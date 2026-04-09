package com.airdefense.game

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.environment.PointLight
import com.badlogic.gdx.graphics.g3d.utils.BaseShaderProvider
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix3

class NightShaderProvider(private val impactLight: PointLight) : BaseShaderProvider() {
    private val whiteDiffuse = createSolidTexture(255)
    private val midRoughness = createSolidTexture(180)

    override fun createShader(renderable: Renderable): Shader =
        NightSceneShader(renderable, impactLight, whiteDiffuse, midRoughness)

    override fun dispose() {
        super.dispose()
        whiteDiffuse.dispose()
        midRoughness.dispose()
    }

    private fun createSolidTexture(value: Int): Texture {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(value / 255f, value / 255f, value / 255f, 1f)
        pixmap.fill()
        val texture = Texture(pixmap)
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        pixmap.dispose()
        return texture
    }
}

private class NightSceneShader(
    renderable: Renderable,
    private val impactLight: PointLight,
    private val fallbackDiffuse: Texture,
    private val fallbackRoughness: Texture
) : Shader {
    private val program = ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER)
    private val normalMatrix = Matrix3()
    private lateinit var context: RenderContext

    init {
        require(program.isCompiled) { "Night shader compilation failed: ${program.log}" }
    }

    override fun init() = Unit

    override fun compareTo(other: Shader?): Int = 0

    override fun canRender(instance: Renderable?): Boolean = true

    override fun begin(camera: Camera, context: RenderContext) {
        this.context = context
        program.bind()
        context.setDepthTest(GL20.GL_LEQUAL)
        context.setDepthMask(true)
        program.setUniformMatrix("u_projViewTrans", camera.combined)
        program.setUniformf("u_cameraPos", camera.position)
        program.setUniformf("u_ambientColor", 0.16f, 0.19f, 0.24f)
        program.setUniformf("u_dirLightDir0", -0.35f, -1f, -0.18f)
        program.setUniformf("u_dirLightColor0", 0.56f, 0.62f, 0.72f)
        program.setUniformf("u_dirLightDir1", 0.35f, -0.24f, 0.42f)
        program.setUniformf("u_dirLightColor1", 0.18f, 0.16f, 0.22f)
        program.setUniformf("u_fogColor", 0.03f, 0.05f, 0.08f)
    }

    override fun render(renderable: Renderable) {
        val material = renderable.material
        val blend = material.get(BlendingAttribute.Type) as? BlendingAttribute
        normalMatrix.set(renderable.worldTransform)
        normalMatrix.inv().transpose()

        val diffuseAttribute = material.get(TextureAttribute.Diffuse) as? TextureAttribute
        val roughnessAttribute = material.get(TextureAttribute.Specular) as? TextureAttribute
        val diffuseColor = (material.get(ColorAttribute.Diffuse) as? ColorAttribute)?.color
        val specularColor = (material.get(ColorAttribute.Specular) as? ColorAttribute)?.color
        val emissiveColor = (material.get(ColorAttribute.Emissive) as? ColorAttribute)?.color
        val shininess = (material.get(FloatAttribute.Shininess) as? FloatAttribute)?.value ?: 24f
        val cullFace = (material.get(IntAttribute.CullFace) as? IntAttribute)?.value?.toInt() ?: GL20.GL_BACK

        val diffuseTexture = diffuseAttribute?.textureDescription?.texture ?: fallbackDiffuse
        val roughnessTexture = roughnessAttribute?.textureDescription?.texture ?: fallbackRoughness

        diffuseTexture.bind(0)
        roughnessTexture.bind(1)

        program.setUniformMatrix("u_worldTrans", renderable.worldTransform)
        program.setUniformMatrix("u_normalMatrix", normalMatrix)
        program.setUniformi("u_diffuseTex", 0)
        program.setUniformi("u_roughnessTex", 1)
        program.setUniformf("u_diffuseColor", diffuseColor ?: com.badlogic.gdx.graphics.Color.WHITE)
        program.setUniformf("u_specularColor", specularColor ?: com.badlogic.gdx.graphics.Color(0.24f, 0.28f, 0.34f, 1f))
        program.setUniformf("u_emissiveColor", emissiveColor ?: com.badlogic.gdx.graphics.Color.CLEAR)
        program.setUniformf("u_roughnessBias", (96f / shininess.coerceAtLeast(8f)).coerceIn(0.35f, 1.6f))
        program.setUniformf("u_pointLightPos", impactLight.position)
        program.setUniformf("u_pointLightColor", impactLight.color)
        program.setUniformf("u_pointLightRadius", impactLight.intensity.coerceAtLeast(0f))
        program.setUniformf("u_opacity", blend?.opacity ?: 1f)
        context.setCullFace(cullFace)
        context.setBlending(blend != null, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        renderable.meshPart.render(program)
    }

    override fun end() {
        program.end()
    }

    override fun dispose() {
        program.dispose()
    }

    companion object {
        private const val VERTEX_SHADER = """
attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec2 a_texCoord0;

uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;
uniform mat3 u_normalMatrix;

varying vec3 v_worldPos;
varying vec3 v_normal;
varying vec2 v_uv;

void main() {
    vec4 worldPos = u_worldTrans * vec4(a_position, 1.0);
    v_worldPos = worldPos.xyz;
    v_normal = normalize(u_normalMatrix * a_normal);
    v_uv = a_texCoord0;
    gl_Position = u_projViewTrans * worldPos;
}
"""

        private const val FRAGMENT_SHADER = """
#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_diffuseTex;
uniform sampler2D u_roughnessTex;
uniform vec4 u_diffuseColor;
uniform vec4 u_specularColor;
uniform vec4 u_emissiveColor;
uniform vec3 u_cameraPos;
uniform vec3 u_ambientColor;
uniform vec3 u_dirLightDir0;
uniform vec3 u_dirLightColor0;
uniform vec3 u_dirLightDir1;
uniform vec3 u_dirLightColor1;
uniform vec3 u_pointLightPos;
uniform vec4 u_pointLightColor;
uniform float u_pointLightRadius;
uniform vec3 u_fogColor;
uniform float u_roughnessBias;
uniform float u_opacity;

varying vec3 v_worldPos;
varying vec3 v_normal;
varying vec2 v_uv;

vec3 toneMap(vec3 color) {
    color = max(color, vec3(0.0));
    color = color / (color + vec3(1.0));
    return pow(color, vec3(1.0 / 2.2));
}

void main() {
    vec3 albedo = texture2D(u_diffuseTex, v_uv).rgb * u_diffuseColor.rgb;
    float roughness = clamp(texture2D(u_roughnessTex, v_uv).r * u_roughnessBias, 0.06, 1.0);
    vec3 N = normalize(v_normal);
    vec3 V = normalize(u_cameraPos - v_worldPos);

    vec3 L0 = normalize(-u_dirLightDir0);
    vec3 L1 = normalize(-u_dirLightDir1);

    float diff0 = max(dot(N, L0), 0.0);
    float diff1 = max(dot(N, L1), 0.0);

    float specExp = mix(160.0, 8.0, roughness);
    float specMul = 1.0 - roughness * 0.82;

    vec3 H0 = normalize(L0 + V);
    vec3 H1 = normalize(L1 + V);
    float spec0 = pow(max(dot(N, H0), 0.0), specExp) * specMul;
    float spec1 = pow(max(dot(N, H1), 0.0), specExp) * specMul;

    vec3 pointVec = u_pointLightPos - v_worldPos;
    float pointDist = length(pointVec);
    vec3 pointDir = pointDist > 0.001 ? pointVec / pointDist : vec3(0.0, 1.0, 0.0);
    float pointAtten = clamp(1.0 - pointDist / max(u_pointLightRadius, 0.001), 0.0, 1.0);
    float pointDiff = max(dot(N, pointDir), 0.0) * pointAtten;
    vec3 pointHalf = normalize(pointDir + V);
    float pointSpec = pow(max(dot(N, pointHalf), 0.0), specExp) * specMul * pointAtten;

    float fresnel = pow(1.0 - max(dot(N, V), 0.0), 5.0);
    vec3 rim = u_specularColor.rgb * fresnel * 0.12;

    vec3 lighting = u_ambientColor;
    lighting += diff0 * u_dirLightColor0;
    lighting += diff1 * u_dirLightColor1;
    lighting += pointDiff * u_pointLightColor.rgb;

    vec3 specular = u_specularColor.rgb * (spec0 * u_dirLightColor0 + spec1 * u_dirLightColor1 + pointSpec * u_pointLightColor.rgb);
    vec3 emissive = u_emissiveColor.rgb;

    float haze = clamp((length(v_worldPos.xz) - 900.0) / 5400.0, 0.0, 0.82);
    vec3 color = albedo * lighting + specular + rim + emissive;
    color = mix(color, u_fogColor, haze);

    gl_FragColor = vec4(toneMap(color), u_opacity);
}
"""
    }
}

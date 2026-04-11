package com.airdefense.game

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class AgentWorkflowDocsTest {
    private data class SurfaceContract(
        val path: String,
        val maxNonEmptyLines: Int,
        val requiredTokens: List<String>,
    )

    private fun repoRoot(): File {
        var current = File(System.getProperty("user.dir")).absoluteFile
        repeat(5) {
            if (File(current, "AGENTS.md").exists()) return current
            current = current.parentFile ?: current
        }
        error("Could not resolve repository root from ${System.getProperty("user.dir")}")
    }

    private fun readRepoFile(path: String): String = File(repoRoot(), path).readText(Charsets.UTF_8)

    @Test
    fun agentInstructionSurfacesStayCompactAndRouted() {
        val contracts =
            listOf(
                SurfaceContract(
                    path = "AGENTS.md",
                    maxNonEmptyLines = 50,
                    requiredTokens =
                        listOf(
                            "docs/reference/ai-agent-context-routing.md",
                            "docs/reference/ai-agent-skills.md",
                            "Open at most 4",
                            "Run at most 2 refine loops",
                            "Add at most 3 files",
                            "Hard stop at 8",
                        ),
                ),
                SurfaceContract(
                    path = "CLAUDE.md",
                    maxNonEmptyLines = 12,
                    requiredTokens = listOf("AGENTS.md", "docs/reference/ai-agent-context-routing.md"),
                ),
                SurfaceContract(
                    path = "GEMINI.md",
                    maxNonEmptyLines = 8,
                    requiredTokens = listOf("AGENTS.md", "docs/reference/ai-agent-context-routing.md"),
                ),
                SurfaceContract(
                    path = ".github/copilot-instructions.md",
                    maxNonEmptyLines = 10,
                    requiredTokens = listOf("AGENTS.md", ".agents/skills/", "android/assets/ATTRIBUTION.md"),
                ),
                SurfaceContract(
                    path = "docs/reference/ai-agent-context-routing.md",
                    maxNonEmptyLines = 28,
                    requiredTokens =
                        listOf(
                            "Open at most 4 task-specific files",
                            "Run at most 2 refine loops",
                            "Add at most 3 files per escalation round",
                            "If you have opened 8 task-specific files",
                        ),
                ),
            )

        contracts.forEach { contract ->
            val text = readRepoFile(contract.path)
            val nonEmptyLines = text.lineSequence().count { it.isNotBlank() }
            assertTrue(
                nonEmptyLines <= contract.maxNonEmptyLines,
                "${contract.path} grew to $nonEmptyLines non-empty lines; keep agent surfaces compact.",
            )
            contract.requiredTokens.forEach { token ->
                assertTrue(text.contains(token), "${contract.path} is missing required token: $token")
            }
        }
    }

    @Test
    fun readmeBadgeBlockStaysGenerated() {
        val readme = readRepoFile("README.md")
        assertTrue(readme.contains("<!-- BEGIN GENERATED BADGES -->"))
        assertTrue(readme.contains("<!-- END GENERATED BADGES -->"))
        assertTrue(readme.contains("ktlint.yml"))
        assertTrue(readme.contains("quality.yml"))
        assertTrue(readme.contains("JuliusBrussee/caveman"))
    }

    @Test
    fun cavemanDocsStayPinnedToUpstreamV150AndLocalOverride() {
        val agents = readRepoFile("AGENTS.md")
        val caveman = readRepoFile(".agents/skills/caveman/SKILL.md")
        val help = readRepoFile(".agents/skills/caveman-help/SKILL.md")
        val doc = readRepoFile("docs/reference/ai-agent-skills.md")

        assertTrue(agents.contains("Never use `caveman`-style writing in repo docs"))
        assertTrue(caveman.contains("v1.5.0"))
        assertTrue(caveman.contains("CAVEMAN_DEFAULT_MODE"))
        assertTrue(caveman.contains("off"))
        assertTrue(caveman.contains("No hooks"))
        assertTrue(caveman.contains("never to repo docs"))
        assertTrue(help.contains("CAVEMAN_DEFAULT_MODE"))
        assertTrue(help.contains("~/.config/caveman/config.json"))
        assertTrue(help.contains("/caveman-help"))
        assertTrue(help.contains("no flag file"))
        assertTrue(doc.contains("function descriptions stay in standard technical English"))
    }

    @Test
    fun assetAndSourceDocsStayConnected() {
        val sourceMap = readRepoFile("docs/level-asset-source-map.md")
        val pipeline = readRepoFile("docs/level-asset-pipeline.md")
        val attribution = readRepoFile("android/assets/ATTRIBUTION.md")
        val repoSkill = readRepoFile("skills/android-3d-air-defense/SKILL.md")

        assertTrue(sourceMap.contains("awesome-citygml"))
        assertTrue(sourceMap.contains("Ladybug Tools / 3D Models"))
        assertTrue(sourceMap.contains("Poly Haven"))
        assertTrue(pipeline.contains("android/assets/ATTRIBUTION.md"))
        assertTrue(attribution.contains("engel_house.obj"))
        assertTrue(repoSkill.contains("docs/level-asset-pipeline.md"))
    }
}

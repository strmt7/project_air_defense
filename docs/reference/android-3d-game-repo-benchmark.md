# Android And 3D Game Repository Benchmark

Measured on 2026-04-17 from public GitHub repository pages/API and targeted web search. This is not a popularity ranking. It is a compact source map for Android 3D games, UE5 mobile work, city rendering, physics, profiling, asset pipelines, and game UI workflows that are relevant or adjacent to this project.

## Selection Rules

- Prefer repositories with direct Android, mobile, 3D, Unreal, geospatial, renderer, physics, profiling, asset-pipeline, or game-production relevance.
- Treat full games, engines, samples, tools, and workflow repos separately; do not pretend a tool repo is a shipped game.
- Copy workflow patterns only after they map to this repo's UE5-only, mobile-first, real-3D-city constraints.

## Repository Scan

| Repo | Stack | Workflow signal | What Project Air Defense should copy |
| --- | --- | --- | --- |
| [CesiumGS/cesium-unreal](https://github.com/CesiumGS/cesium-unreal) | UE5 plugin, C++, 3D Tiles | georeferenced worlds, local/remote tilesets | Keep real city data tiled, streamed, and verifiable. |
| [Esri/arcgis-maps-sdk-unreal-engine-samples](https://github.com/Esri/arcgis-maps-sdk-unreal-engine-samples) | UE5, ArcGIS Maps SDK | GIS layers and sample scenes | Use GIS SDKs for scouting/validation, not gameplay backdrops. |
| [DanialKama/UltimateUI](https://github.com/DanialKama/UltimateUI) | UE5, UMG/Common UI style | main, pause, settings, graphics panels | Structure menus around categories and real settings. |
| [Adriwin06/Ultimate-CommonUI-Menu-System](https://github.com/Adriwin06/Ultimate-CommonUI-Menu-System) | UE5, Common UI | routed settings and graphics-menu patterns | Keep graphics controls backend-owned and testable. |
| [Stajky/CommonUIMenuTemplate](https://github.com/Stajky/CommonUIMenuTemplate) | UE5, Common UI, Enhanced Input | menu layering and input data | Separate player touch UI from hidden debug input. |
| [newrelic/newrelic-unreal-plugin](https://github.com/newrelic/newrelic-unreal-plugin) | UE plugin, Android/iOS agents | crash/error/session telemetry | Add packaged-runtime telemetry before large public testing. |
| [VesCodes/ImGui](https://github.com/VesCodes/ImGui) | UE plugin, ImGui | internal debug panels | Keep debug UI internal and out of the mobile UX. |
| [google/filament](https://github.com/google/filament) | Android/iOS/WebGL PBR renderer | glTF viewer, PBR, IBL | Treat material correctness as an asset-pipeline gate. |
| [google-ar/arcore-android-sdk](https://github.com/google-ar/arcore-android-sdk) | Android AR, Java/Kotlin/C++ | mobile 3D camera/render samples | Respect mobile device lifecycle and camera constraints. |
| [google-ar/sceneform-android-sdk](https://github.com/google-ar/sceneform-android-sdk) | Android 3D scene graph | model loading, placement, bounds | Make scale, placement, and scene hierarchy testable. |
| [googlevr/cardboard](https://github.com/googlevr/cardboard) | Android/iOS VR SDK | low-latency mobile rendering loop | Prioritize frame pacing and input latency. |
| [android/ndk-samples](https://github.com/android/ndk-samples) | Android NDK, C++, Gradle | native graphics/build samples | Keep Android packaging proof deterministic. |
| [android/games-samples](https://github.com/android/games-samples) | Android game APIs | GameActivity and platform samples | Validate UE Android output against Android game expectations. |
| [android/performance-samples](https://github.com/android/performance-samples) | Android performance samples | benchmarks and profiling examples | Measure startup, frame health, and jank separately. |
| [android/testing-samples](https://github.com/android/testing-samples) | Android testing samples | UI/unit/integration coverage | Keep screen-flow proof reproducible. |
| [android/nowinandroid](https://github.com/android/nowinandroid) | Android app architecture | CI, modularity, quality gates | Copy disciplined Android workflow patterns, not UI content. |
| [libgdx/libgdx](https://github.com/libgdx/libgdx) | Java, Android/Desktop/iOS game framework | cross-platform game loop and assets | Preserve deterministic simulation and repeatable tests from the legacy runtime. |
| [Anuken/Mindustry](https://github.com/Anuken/Mindustry) | Java, libGDX, Android/Desktop game | wave combat, mobile UI, content pipeline | Combat readability must beat visual noise. |
| [yairm210/Unciv](https://github.com/yairm210/Unciv) | Kotlin, libGDX, Android/Desktop game | mobile strategy settings and UI | Keep menus text-light and phone-readable. |
| [godotengine/godot](https://github.com/godotengine/godot) | C++, Vulkan/OpenGL, Android export | renderer tiers, editor/runtime split | Separate tooling, runtime config, and quality tiers. |
| [godotengine/godot-demo-projects](https://github.com/godotengine/godot-demo-projects) | Godot sample projects | focused rendering/input/UI demos | Build small proof lanes for major systems. |
| [cocos/cocos-engine](https://github.com/cocos/cocos-engine) | C++/TypeScript mobile engine | mobile-first runtime and asset workflow | Control render cost and package growth aggressively. |
| [defold/defold](https://github.com/defold/defold) | C++/Lua mobile engine | small runtime, build pipeline | Keep the mobile build lean. |
| [MonoGame/MonoGame](https://github.com/MonoGame/MonoGame) | C# game framework | content pipeline and platform abstraction | Treat assets as build products with validation. |
| [stride3d/stride](https://github.com/stride3d/stride) | C# 3D engine | asset pipeline, editor/runtime modularity | Keep renderer, camera, UI, and simulation separable. |
| [supertuxkart/stk-code](https://github.com/supertuxkart/stk-code) | C++ full 3D game | camera, action UI, content-heavy pipeline | Tune camera/readability explicitly, not by default engine behavior. |
| [luanti-org/luanti](https://github.com/luanti-org/luanti) | C++ voxel/world engine | chunking, streaming, content boundaries | Large environments need strict streaming ownership. |
| [KhronosGroup/Vulkan-Samples](https://github.com/KhronosGroup/Vulkan-Samples) | C++, Vulkan | targeted graphics samples | Validate graphics claims with focused render tests. |
| [ARM-software/vulkan_best_practice_for_mobile_developers](https://github.com/ARM-software/vulkan_best_practice_for_mobile_developers) | C++, Vulkan, Android | tile-based mobile GPU practices | Avoid expensive full-screen effects without measured proof. |
| [SnapdragonStudios/adreno-gpu-vulkan-code-sample-framework](https://github.com/SnapdragonStudios/adreno-gpu-vulkan-code-sample-framework) | C++, Vulkan, Android APK | Adreno sample framework and CI | Include device-family profiling when Android cooks are active. |
| [GPUOpen-LibrariesAndSDKs/VulkanMemoryAllocator](https://github.com/GPUOpen-LibrariesAndSDKs/VulkanMemoryAllocator) | C/C++, Vulkan | GPU memory budget discipline | Track city asset memory and texture growth. |
| [ConfettiFX/The-Forge](https://github.com/ConfettiFX/The-Forge) | C++, cross-platform renderer | Android/Vulkan/Metal/DX framework | Treat renderer portability as architecture. |
| [mosra/magnum](https://github.com/mosra/magnum) | C++ graphics middleware | Android, OpenGL, Vulkan, asset examples | Keep graphics systems modular and sample-driven. |
| [vsg-dev/VulkanSceneGraph](https://github.com/vsg-dev/VulkanSceneGraph) | C++17, Vulkan scene graph | scene graph and Vulkan discipline | Separate scene organization from gameplay state. |
| [godlikepanos/anki-3d-engine](https://github.com/godlikepanos/anki-3d-engine) | C++, Vulkan/D3D12 engine | modern renderer, physics, scripting | Keep renderer, physics, and script seams explicit. |
| [Rajawali/Rajawali](https://github.com/Rajawali/Rajawali) | Java, Android OpenGL ES engine | Android-native 3D lifecycle | Respect Android rendering-surface lifecycle. |
| [kool-engine/kool](https://github.com/kool-engine/kool) | Kotlin multiplatform 3D | Android/OpenGL/WebGPU/Vulkan abstraction | Keep game state independent from backend specifics. |
| [bulletphysics/bullet3](https://github.com/bulletphysics/bullet3) | C++ physics | collision and simulation examples | Fast projectiles need continuous/proximity logic. |
| [jrouwe/JoltPhysics](https://github.com/jrouwe/JoltPhysics) | C++ physics | deterministic, performance-conscious APIs | Keep physics stepping independent from rendering. |
| [NVIDIA-Omniverse/PhysX](https://github.com/NVIDIA-Omniverse/PhysX) | C++ physics SDK | scene queries and profiling | Use UE Chaos/scene-query discipline rather than visual hacks. |
| [erincatto/box2d](https://github.com/erincatto/box2d) | C/C++ 2D physics | CCD, shape casts, sensors | Model proximity fuzes like continuous collision checks. |
| [JSBSim-Team/jsbsim](https://github.com/JSBSim-Team/jsbsim) | C++ flight dynamics | guidance/control terminology | Keep missile/interceptor motion physically coherent. |
| [google/agi](https://github.com/google/agi) | Android GPU Inspector | GPU frame capture and counters | Add AGI proof for Android graphics regressions. |
| [google/perfetto](https://github.com/google/perfetto) | Android/system tracing | performance timelines | Use traces for startup, hitches, and streaming issues. |
| [google/oboe](https://github.com/google/oboe) | Android native audio | low-latency audio patterns | Plan missile/explosion audio latency deliberately. |
| [google/gapid](https://github.com/google/gapid) | graphics API debugger | GLES/Vulkan trace/replay | Debug render API behavior with tooling when needed. |
| [KhronosGroup/glTF-Sample-Viewer](https://github.com/KhronosGroup/glTF-Sample-Viewer) | glTF PBR viewer | material/IBL reference | Validate imported city materials outside UE when needed. |
| [KhronosGroup/glTF-Validator](https://github.com/KhronosGroup/glTF-Validator) | glTF validation CLI/library | schema and asset validation | Reject malformed GLB/glTF before import. |
| [KhronosGroup/KTX-Software](https://github.com/KhronosGroup/KTX-Software) | texture tooling | KTX/KTX2/BasisU | Prefer compressed texture strategy for mobile assets. |
| [KhronosGroup/SPIRV-Cross](https://github.com/KhronosGroup/SPIRV-Cross) | shader cross-compilation | shader reflection/portability | Treat shader compatibility as a validation topic. |
| [BinomialLLC/basis_universal](https://github.com/BinomialLLC/basis_universal) | texture compression | transcodable GPU texture codec | Avoid package bloat and runtime texture spikes. |
| [google/draco](https://github.com/google/draco) | mesh compression | mesh/point-cloud compression | Use only after confirming UE/Cesium compatibility. |
| [assimp/assimp](https://github.com/assimp/assimp) | asset importer | many 3D file formats | Normalize imported assets before runtime integration. |
| [CesiumGS/cesium-native](https://github.com/CesiumGS/cesium-native) | C++ geospatial runtime | 3D Tiles/glTF core | Understand Cesium runtime assumptions before changing city data. |
| [CesiumGS/3d-tiles-validator](https://github.com/CesiumGS/3d-tiles-validator) | TypeScript validator | 3D Tiles validation | Validate city tiles before blaming the renderer. |
| [CesiumGS/obj2gltf](https://github.com/CesiumGS/obj2gltf) | JavaScript CLI | OBJ to glTF conversion | Useful for OBJ sources only with validation and material cleanup. |
| [cityjson/cjio](https://github.com/cityjson/cjio) | Python CityJSON CLI | inspect/filter/transform CityJSON | Trim city datasets before engine import. |
| [tudelft3d/3dfier](https://github.com/tudelft3d/3dfier) | C++ geospatial generation | 2D GIS to 3D city model | Fallback only when official textured meshes do not exist. |
| [OSGeo/gdal](https://github.com/OSGeo/gdal) | geospatial toolkit | CRS and raster/vector translation | Do not hand-roll coordinate conversion. |
| [maplibre/maplibre-native](https://github.com/maplibre/maplibre-native) | C++ native map renderer | mobile vector map rendering | Reference mobile map streaming and camera constraints. |
| [Unity-Technologies/BoatAttack](https://github.com/Unity-Technologies/BoatAttack) | Unity URP demo | water, horizon, lighting, camera | Use as a mobile scene-composition reference. |
| [Unity-Technologies/Graphics](https://github.com/Unity-Technologies/Graphics) | Unity SRP/URP/HDRP | quality settings and shader libraries | Graphics settings belong to the render pipeline. |
| [Unity-Technologies/EntityComponentSystemSamples](https://github.com/Unity-Technologies/EntityComponentSystemSamples) | Unity ECS samples | data-oriented simulation/render split | Keep missiles/effects data-driven and measurable. |
| [heroiclabs/nakama](https://github.com/heroiclabs/nakama) | game backend | matchmaking, leaderboards, realtime | Future online features should use proven backend architecture. |
| [AirtestProject/Airtest](https://github.com/AirtestProject/Airtest) | game/app automation | screenshot-driven mobile automation | Use image proof cautiously and record thresholds. |
| [AirtestProject/Poco](https://github.com/AirtestProject/Poco) | UI automation | cross-engine UI inspection | Useful where accessibility/UI-tree proof exists. |
| [adamrehn/ue4-docker](https://github.com/adamrehn/ue4-docker) | Unreal build containers | reproducible UE environments | Future CI should avoid machine-only UE assumptions. |
| [adamrehn/ue4cli](https://github.com/adamrehn/ue4cli) | Unreal CLI tooling | command-line UE automation | Keep build/test/package scriptable. |
| [Allar/ue5-style-guide](https://github.com/Allar/ue5-style-guide) | UE style guide | naming/folder conventions | Enforce asset naming before content multiplies. |

## Prioritized Workflow Changes For This Repo

1. Replace debug-only battle effects with packaged-renderable UE components or Niagara systems.
2. Keep the UE5 headless Monte Carlo lane on `FProjectAirDefenseBattleSimulation`, the same C++ simulation type used by the GUI bridge.
3. Add asset validation gates for 3D Tiles/glTF/KTX before any new city dataset becomes active runtime content.
4. Add Android graphics proof using AGI/Perfetto once UE Android packaging is active, because screenshots cannot prove frame health.
5. Keep player UI touch-first and landscape-only; debug keyboard or ImGui-style tooling must remain hidden and documented.

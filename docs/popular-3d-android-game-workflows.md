# Popular 3D Android Game Workflows

This document summarizes public workflow patterns used by large, high-visibility 3D Android games and translates them into practical guidance for this repository.

It is intentionally evidence-first:

- Direct evidence means the workflow is stated in a public Android, engine-vendor, or studio source.
- Synthesis means the recommendation is inferred from several direct sources.
- If a title is extremely popular but does not publish much technical detail, this document does not invent internal practices for it.

## Scope and selection rule

This document focuses on titles or franchises that are:

- widely recognized 3D Android games or live-service mobile 3D games
- backed by public technical material from Android Developers, Unity, GDC-style slides, or studio engineering posts
- relevant to rendering, performance, asset delivery, input, quality tiers, or live operations

The titles below are not a popularity ranking. They are a practical set of public case studies with usable workflow signals.

## Case studies

### 1. Genshin Impact

Direct evidence:

- A Unity Dojo presentation by miHoYo technical director Zhenzhong Yi describes Genshin as an open-world action RPG that ships across PS4, PC, and mobile and is built for a long update life cycle: [Slideshare deck](https://www.slideshare.net/slideshow/210617-unity-dojo20211mihoyozhenzhongyi/249437133?nway-content_model=A)
- The same talk states that Genshin uses two customized rendering pipelines: one for PC and console, and one for Android and iOS. It also states that mobile is the primary development platform and that console follows closely behind: [Slideshare deck](https://www.slideshare.net/slideshow/210617-unity-dojo20211mihoyozhenzhongyi/249437133?nway-content_model=A)
- The talk also emphasizes practical and mature technology, heavy compute shader usage, stylized PBR rendering, and system-level pipeline work rather than isolated feature additions: [Slideshare deck](https://www.slideshare.net/slideshow/210617-unity-dojo20211mihoyozhenzhongyi/249437133?nway-content_model=A)

Workflow signal:

- mobile-first multiplatform development
- separate renderer strategy per platform tier
- art direction defines renderer priorities
- mature techniques beat flashy but unstable features
- rendering changes are treated as pipeline architecture work

Why it matters here:

- Premium visuals do not start with random effects.
- They start with a stable mobile path and a clear visual contract.

### 2. Wuthering Waves

Direct evidence:

- Android Developers documents that Wuthering Waves used Android Studio Power Profiler and On Device Power Rails Monitor data to study power consumption in a high-fidelity action RPG: [Wuthering Waves story](https://developer.android.com/stories/games/kuro-powerprofiler)
- The same article describes a custom profiling workflow built on Perfetto and ODPM so the team could filter rails, compare test cases across versions, and upload power data into its own QA system: [Wuthering Waves story](https://developer.android.com/stories/games/kuro-powerprofiler)
- Specific optimization areas included CPU core scheduling, PSO precompilation, potentially visible set culling, and baked shadow occlusion culling. The article reports a 9.68% total power reduction between releases under comparable settings: [Wuthering Waves story](https://developer.android.com/stories/games/kuro-powerprofiler)

Workflow signal:

- measure long-session power, not only FPS
- compare the same scene and camera across versions
- put performance data into a QA system that survives individual sessions
- optimize CPU scheduling, shader compilation cost, culling, and shadows as separate levers

Why it matters here:

- Premium mobile visuals are only useful if the power budget is sustainable.
- This repo needs repeatable scene-based profiling, not ad hoc visual guesses.

### 3. Call of Duty: Warzone Mobile

Direct evidence:

- Android Developers states that Call of Duty: Warzone Mobile runs on Vulkan-enabled Android devices and uses rendering technology shared with the console and PC versions: [Games Quarterly](https://developer.android.com/newsletter/games-quarterly/2024/content/quarter3)
- Android Developers also uses Warzone Mobile as a high-end Vulkan example in its graphics guidance: [Android Vulkan stories](https://developer.android.com/games/develop/vulkan/sample-codelab-story)

Workflow signal:

- modern graphics API first
- renderer strategy aligned across platform family
- premium mobile visuals anchored in one modern rendering stack

Synthesis:

- Public workflow detail is limited, but the Vulkan-first positioning strongly suggests deliberate reduction of dual-backend complexity.

Why it matters here:

- High-end ambitions and fragmented renderer strategy usually fight each other.

### 4. Diablo Immortal

Direct evidence:

- Android Developers documents Diablo Immortal's Vulkan hardware ray tracing implementation on Android: [Diablo Immortal ray tracing story](https://developer.android.com/stories/games/diablo-raytracing)
- The same article explains the acceleration structure strategy: TLAS and BLAS separation, asynchronous BLAS construction for static content, incremental updates for animated content, and view-region-based TLAS construction to control cost: [Diablo Immortal ray tracing story](https://developer.android.com/stories/games/diablo-raytracing)

Workflow signal:

- expensive rendering features are built with explicit architecture and budgets
- static and dynamic scene content are handled differently
- costly scene-building work is spread across frames
- only view-relevant content participates in the most expensive structures

Why it matters here:

- The lesson is not "add ray tracing."
- The real lesson is that advanced graphics only work when the scene, update model, and budgets are designed together.

### 5. NEW STATE Mobile

Direct evidence:

- Android Developers documents that NEW STATE Mobile used Android GPU Inspector because long-view scenes and dense vegetation created heavy GPU pressure and overdraw: [NEW STATE Mobile AGI story](https://developer.android.com/stories/games/new-state-mobile)
- The article describes a repeated optimization loop: inspect counters, identify a costly pass, change one rendering decision, and re-measure: [NEW STATE Mobile AGI story](https://developer.android.com/stories/games/new-state-mobile)
- Concrete changes included a depth prepass for dominant geometry, switching scene color from 64-bit to 32-bit, lower-poly shadow LODs, and auto-instancing. Reported wins included a 7.5% GPU utilization drop from the depth prepass, 5.3% from the scene color change, and 3.5% from auto-instancing: [NEW STATE Mobile AGI story](https://developer.android.com/stories/games/new-state-mobile)

Workflow signal:

- profile passes, not only whole frames
- optimize overdraw and bandwidth directly
- treat shadows, color format, and instancing as separate budget levers
- make scene repetition cheap

Why it matters here:

- "Looks bad" and "runs badly" often come from the same scene-budget failure.

### 6. Lineage W

Direct evidence:

- Android Developers documents that Lineage W used the Android Dynamic Performance Framework to maximize graphics quality while preventing thermal-throttling-related performance loss: [Lineage W ADPF story](https://developer.android.com/stories/games/lineagew-adpf)
- The story describes the Unreal Engine ADPF plugin checking thermal status every second, mapping thermal state to quality levels, creating hint sessions for game and render threads, and reporting target and actual frame durations every frame: [Lineage W ADPF story](https://developer.android.com/stories/games/lineagew-adpf)
- The article also reports 30-minute Pixel 6 tests. Default behavior fell from 60 FPS to 32 FPS after thermal stress, while deeper integration between ADPF and Lineage W's own in-game quality settings maintained 60 FPS with thermal headroom below the severe throttling threshold: [Lineage W ADPF story](https://developer.android.com/stories/games/lineagew-adpf)

Workflow signal:

- sustained performance is designed, not hoped for
- thermal response belongs in the runtime strategy
- performance adaptation should be automatic, not only user-driven

Why it matters here:

- For a live 3D Android game, short burst performance is not enough.
- The real target is stable play over time.

### 7. Summoners War: Chronicles

Direct evidence:

- Android Developers documents that Summoners War: Chronicles uses Vulkan exclusively on Android and reports up to 30% performance gains: [Com2uS Vulkan story](https://developer.android.com/stories/games/com2us-vulkan?hl=en)
- The same article describes a custom deferred renderer, indirect instanced rendering, heavy compute shader use, runtime quality adjustment, and Android adaptive performance hooks: [Com2uS Vulkan story](https://developer.android.com/stories/games/com2us-vulkan?hl=en)
- The article also explains why Vulkan-only was chosen: required features depended on compute outputs and SSBO behavior that were unavailable or constrained in the team's OpenGL ES path: [Com2uS Vulkan story](https://developer.android.com/stories/games/com2us-vulkan?hl=en)
- Android Developers separately documents Com2uS shipping the same Android build to Google Play Games on PC, adding keyboard, mouse, and controller support, adjusting UI for larger screens, and validating with emulator plus ADB workflows: [Com2uS Google Play Games on PC story](https://developer.android.com/stories/games/com2us)

Workflow signal:

- choose one modern API when the device floor supports it
- use compute-driven culling and indirect rendering for repeated content
- integrate thermal adaptation into the renderer and runtime
- reuse build outputs across platforms when the architecture allows it
- treat emulator and ADB flows as part of routine QA

Why it matters here:

- This is one of the clearest public examples of renderer strategy, QA strategy, and platform scope being managed as one system.

### 8. Asphalt 9: Legends

Direct evidence:

- Android Developers documents Asphalt 9's in-house game options system and later Game Mode API integration: [Asphalt 9 Game Mode story](https://developer.android.com/stories/games/gameloft-gamemode)
- The documented modes are Standard, Performance, Battery, and Unsupported, with graphics behavior tied directly to the chosen mode: [Asphalt 9 Game Mode story](https://developer.android.com/stories/games/gameloft-gamemode)
- Battery mode reduces environment fidelity, disables or simplifies expensive effects such as reflective calculations and depth of field, and caps frame rate at 30 FPS. Gameloft reports up to 70% lower power consumption and 35% longer play time on some devices: [Asphalt 9 Game Mode story](https://developer.android.com/stories/games/gameloft-gamemode)

Workflow signal:

- quality tiers are part of product design
- battery and thermal behavior are explicit player-facing concerns
- expensive effects are selectively removed, not uniformly degraded

Why it matters here:

- A professional mobile game does not ship one hardcoded fidelity mode.

### 9. Gameloft asset delivery workflow

Direct evidence:

- Android Developers documents Gameloft's migration from APK plus OBB or in-house delivery systems to Play Asset Delivery: [Gameloft PAD story](https://developer.android.com/stories/games/gameloft-pad)
- The article recommends splitting data into three groups: required at first launch, required after a few minutes, and optional content used by only some users: [Gameloft PAD story](https://developer.android.com/stories/games/gameloft-pad)
- Gameloft reports lower CDN costs and a 10% increase in new players completing the secondary download path compared with the old system: [Gameloft PAD story](https://developer.android.com/stories/games/gameloft-pad)

Workflow signal:

- content delivery is architecture, not release plumbing
- boot-critical assets are treated differently from optional assets
- download structure affects conversion and retention

Why it matters here:

- If this project grows its texture and model quality, asset segmentation will stop being optional.

### 10. War Robots

Direct evidence:

- Android Developers documents War Robots adapting to large screens and alternate input modes with device-mode detection, keyboard and mouse support, cursor control, mode-specific tutorials, and resizable windows: [War Robots Android story](https://developer.android.com/stories/games/war-robots)
- Unity's case study describes studio practices such as open knowledge sharing, continuous improvement, designer autonomy, in-engine promo asset creation, and a rule to prototype quickly and focus on engaging core gameplay first: [War Robots Unity case study](https://unity.com/case-study/pixonic-war-robots)

Workflow signal:

- input mode changes should update controls, labels, tutorials, and HUD together
- live games need iteration speed, not only rendering ambition
- designers need enough autonomy to tune content without waiting on engine changes

Why it matters here:

- Giant tightly coupled screen classes slow down gameplay and UI iteration.

### 11. MIR 2: Return of the King

Direct evidence:

- Android Developers documents MIR 2: Return of the King's use of the Android Frame Pacing library, also known as Swappy, to stabilize presentation timing and improve rendering consistency on Android: [MIR 2 frame pacing story](https://developer.android.com/stories/games/swappy)
- The same article explains that Swappy handles multiple refresh rates and correct presentation timing, and reports MIR 2 reducing its slow-session rate from 40% to 10% through Unity's built-in optimized frame pacing integration: [MIR 2 frame pacing story](https://developer.android.com/stories/games/swappy)

Workflow signal:

- frame pacing is separate from raw FPS
- presentation timing needs explicit engineering attention

Why it matters here:

- A missile defense game with camera motion, effects, and HUD overlays needs stable frame delivery to feel correct.

## Cross-title workflow patterns

### Pattern 1: Renderer strategy is chosen early

Observed in:

- Genshin Impact
- Warzone Mobile
- Diablo Immortal
- Summoners War: Chronicles

Direct evidence:

- Top teams decide whether they are Vulkan-first, Vulkan-only, or multi-pipeline early in development.

Synthesis:

- They do not let renderer strategy emerge accidentally from engine defaults late in the project.

### Pattern 2: Frame pacing and thermals are product concerns

Observed in:

- Wuthering Waves
- Lineage W
- Asphalt 9
- MIR 2

Direct evidence:

- These teams track slow frames, frozen frames, thermal pressure, or frame pacing as separate concerns from average FPS.

Synthesis:

- "Runs at 60 sometimes" is not a valid mobile quality bar.

### Pattern 3: Performance work is pass-level, not mystical

Observed in:

- NEW STATE Mobile
- Diablo Immortal
- Summoners War: Chronicles

Direct evidence:

- They profile overdraw, memory bandwidth, shadow cost, acceleration structure cost, instancing, and color format decisions as isolated variables.

Synthesis:

- Real mobile graphics improvement comes from measurable pass and content decisions, not from vague taste.

### Pattern 4: Asset delivery is part of game design

Observed in:

- Gameloft PAD workflows
- Genshin's long-life-cycle structure

Direct evidence:

- Large games explicitly separate first-launch, early-session, and optional assets.

Synthesis:

- Download architecture is retention architecture.

### Pattern 5: Controls and UI adapt to mode and platform

Observed in:

- War Robots
- Summoners War: Chronicles

Direct evidence:

- These games adjust input, tutorials, and UI behavior when the platform or control mode changes.

Synthesis:

- A serious mobile game does not treat HUD, controls, and device mode as unrelated systems.

### Pattern 6: Mature techniques beat feature stacking

Observed in:

- Genshin Impact
- Asphalt 9
- Diablo Immortal

Direct evidence:

- The most polished games consistently use coherent, art-direction-aligned systems rather than enabling every expensive effect at once.

Synthesis:

- Visual quality is mostly the result of a coherent stack: materials, lighting, silhouettes, scale, and frame stability.

## Distilled workflow model for this repository

This section is synthesis. It is a practical operating model derived from the case studies above.

### 1. Preproduction workflow

- lock the visual target first: what kind of night scene, what screen readability target, what device floor
- decide renderer strategy early: baseline Android path, higher tier path, and whether Vulkan-first is a real goal
- define scene scale rules before importing more assets
- define a device tier matrix before chasing premium effects

### 2. Rendering workflow

- keep one safe Android renderer path that always works
- track pass budgets for sky, terrain, buildings, projectiles, particles, shadows, and post-process
- add effects only after the base scene reads correctly
- prefer selective effects over universal effects
- use instancing, repeated-material discipline, and cheap distant geometry

### 3. Content pipeline workflow

- measure imported models on arrival: bounds, orientation, scale, texture resolution, material assumptions
- separate boot-critical assets from optional high-fidelity assets
- avoid introducing large reference assets into shipping bundles without a delivery plan
- treat model provenance and licensing as part of the pipeline, not legal cleanup later

### 4. Performance workflow

- measure startup, battle-start, and live frame pacing separately
- measure frame pacing, not only FPS
- profile one render pass at a time when graphics regress
- keep scene complexity measurable: draw calls, repeated meshes, particle counts, shadow coverage
- validate thermal behavior over longer sessions, not only 30-second runs

### 5. QA workflow

- verify the exact button path visually, not by assumption
- capture menu and in-battle screenshots after each major graphics or UI change
- keep emulator verification, device verification, and headless simulation aligned
- treat process-alive, render-alive, and playable as separate checks

### 6. Live tuning workflow

- keep gameplay simulation shared between headless and GUI paths
- use repeated headless runs for balance tuning
- make quality scaling and battle balance both measurable
- keep docs updated when workflows change so the repo remains operable by another engineer

## What Project Air Defense should copy next

### Immediate

- keep one baseline Android render path and one higher tier, not one unstable path trying to do everything
- add explicit quality modes with clear tradeoffs for frame rate, particles, reflections, and shadows
- measure battle rendering by pass and batch count, not by screenshots alone
- keep using shared simulation between GUI and headless runs
- separate scene readability work from premium-effects work

### Next

- move repeated scenery to instancing-friendly data
- simplify shadows and expensive post effects on lower tiers
- add runtime thermal or sustained-performance adaptation hooks
- formalize a low, medium, and high-end Android device matrix
- prepare asset segmentation before texture and model count grows again

### Long-term

- decide whether the renderer stays OpenGL-safe or becomes Vulkan-first
- build a proper asset delivery plan if environment fidelity rises
- keep benchmark, simulation, rendering, and QA results in one repeatable loop

## What this repo should avoid

- copying desktop rendering ideas directly into the mobile path
- adding premium-looking effects without a measured pass budget
- keeping battle logic, HUD logic, and render logic welded together forever
- assuming "looks better on one device" is a shipping strategy
- assuming average FPS is enough evidence

## Conclusion

The strongest public pattern across successful 3D Android games is not any single feature. It is disciplined workflow:

- choose renderer strategy early
- measure frame pacing and thermal behavior explicitly
- profile at pass level
- segment assets intentionally
- adapt controls and UI to platform mode
- keep live development, tuning, and graphics work in one loop

For Project Air Defense, the direct implication is clear: the next major gains will come from renderer architecture, device-tier strategy, scene-budget control, and stable frame delivery before any attempt at premium effects beyond that baseline.

## Sources

- [Genshin Impact Unity Dojo presentation](https://www.slideshare.net/slideshow/210617-unity-dojo20211mihoyozhenzhongyi/249437133?nway-content_model=A)
- [Wuthering Waves power profiling story](https://developer.android.com/stories/games/kuro-powerprofiler)
- [Call of Duty: Warzone Mobile in Android Games Quarterly](https://developer.android.com/newsletter/games-quarterly/2024/content/quarter3)
- [Android Vulkan stories](https://developer.android.com/games/develop/vulkan/sample-codelab-story)
- [Diablo Immortal ray tracing story](https://developer.android.com/stories/games/diablo-raytracing)
- [NEW STATE Mobile AGI story](https://developer.android.com/stories/games/new-state-mobile)
- [Lineage W ADPF story](https://developer.android.com/stories/games/lineagew-adpf)
- [Com2uS Vulkan story](https://developer.android.com/stories/games/com2us-vulkan?hl=en)
- [Com2uS Google Play Games on PC story](https://developer.android.com/stories/games/com2us)
- [Asphalt 9 Game Mode story](https://developer.android.com/stories/games/gameloft-gamemode)
- [Gameloft Play Asset Delivery story](https://developer.android.com/stories/games/gameloft-pad)
- [War Robots Android story](https://developer.android.com/stories/games/war-robots)
- [War Robots Unity case study](https://unity.com/case-study/pixonic-war-robots)
- [MIR 2 frame pacing story](https://developer.android.com/stories/games/swappy)

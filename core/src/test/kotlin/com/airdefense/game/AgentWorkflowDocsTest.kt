package com.airdefense.game

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class AgentWorkflowDocsTest {
    private val defaultBranchGuard = "github.ref_name == github.event.repository.default_branch"
    private val agentsPath = "AGENTS.md"
    private val attributionPath = "android/assets/ATTRIBUTION.md"
    private val contextRoutingPath = "docs/reference/ai-agent-context-routing.md"
    private val levelAssetPipelinePath = "docs/level-asset-pipeline.md"
    private val skillsPath = "docs/reference/ai-agent-skills.md"
    private val cavemanDefaultMode = "CAVEMAN_DEFAULT_MODE"
    private val refineLoopLimit = "Run at most 2 refine loops"
    private val karpathyBaselineCommit =
        listOf(
            "2c606141",
            "936f1eee",
            "f17fa304",
            "3a72095b",
            "4765b9c2",
        ).joinToString("")

    private data class SurfaceContract(
        val path: String,
        val maxNonEmptyLines: Int,
        val requiredTokens: List<String>,
    )

    private fun repoRoot(): File {
        var current = File(System.getProperty("user.dir")).absoluteFile
        repeat(5) {
            if (File(current, agentsPath).exists()) return current
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
                    path = agentsPath,
                    maxNonEmptyLines = 112,
                    requiredTokens =
                        listOf(
                            "Pinned Karpathy agent baseline",
                            karpathyBaselineCommit,
                            "Compact and efficient code matters",
                            "EXAMPLES.md",
                            contextRoutingPath,
                            skillsPath,
                            "Open at most 4",
                            refineLoopLimit,
                            "Add at most 3 files",
                            "Hard stop at 8",
                        ),
                ),
                SurfaceContract(
                    path = "CLAUDE.md",
                    maxNonEmptyLines = 12,
                    requiredTokens = listOf(agentsPath, contextRoutingPath),
                ),
                SurfaceContract(
                    path = "GEMINI.md",
                    maxNonEmptyLines = 8,
                    requiredTokens = listOf(agentsPath, contextRoutingPath),
                ),
                SurfaceContract(
                    path = ".github/copilot-instructions.md",
                    maxNonEmptyLines = 10,
                    requiredTokens = listOf(agentsPath, ".agents/skills/", attributionPath),
                ),
                SurfaceContract(
                    path = contextRoutingPath,
                    maxNonEmptyLines = 28,
                    requiredTokens =
                        listOf(
                            "Open at most 4 task-specific files",
                            refineLoopLimit,
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
        assertTrue(
            readme.contains(
                "https://img.shields.io/static/v1?label=&message=andrej-karpathy-skills&color=555&logo=github&logoColor=white",
            ),
        )
        assertTrue(!readme.contains("https://img.shields.io/badge/andrej-karpathy-skills"))
        assertTrue(readme.contains("forrestchang/andrej-karpathy-skills"))
    }

    @Test
    fun cavemanDocsStayPinnedToUpstreamV150AndLocalOverride() {
        val agents = readRepoFile(agentsPath)
        val caveman = readRepoFile(".agents/skills/caveman/SKILL.md")
        val help = readRepoFile(".agents/skills/caveman-help/SKILL.md")
        val doc = readRepoFile(skillsPath)

        assertTrue(agents.contains("Never use `caveman`-style writing in repo docs"))
        assertTrue(caveman.contains("v1.5.0"))
        assertTrue(caveman.contains(cavemanDefaultMode))
        assertTrue(caveman.contains("off"))
        assertTrue(caveman.contains("No hooks"))
        assertTrue(caveman.contains("never to repo docs"))
        assertTrue(help.contains(cavemanDefaultMode))
        assertTrue(help.contains("~/.config/caveman/config.json"))
        assertTrue(help.contains("/caveman-help"))
        assertTrue(help.contains("no flag file"))
        assertTrue(doc.contains("function descriptions stay in standard technical English"))
    }

    @Test
    fun assetAndSourceDocsStayConnected() {
        val sourceMap = readRepoFile("docs/level-asset-source-map.md")
        val pipeline = readRepoFile(levelAssetPipelinePath)
        val attribution = readRepoFile(attributionPath)
        val repoSkill = readRepoFile("skills/android-3d-air-defense/SKILL.md")

        assertTrue(sourceMap.contains("awesome-citygml"))
        assertTrue(sourceMap.contains("Ladybug Tools / 3D Models"))
        assertTrue(sourceMap.contains("Poly Haven"))
        assertTrue(pipeline.contains(attributionPath))
        assertTrue(attribution.contains("engel_house.obj"))
        assertTrue(repoSkill.contains(levelAssetPipelinePath))
    }

    @Test
    fun allWorkflowJobsGateRunnerExecutionToDefaultBranch() {
        val workflowPaths =
            listOf(
                ".github/workflows/android-release-apk.yml",
                ".github/workflows/ktlint.yml",
                ".github/workflows/quality.yml",
            )

        val workflowDir = File(repoRoot(), ".github/workflows")
        val actualWorkflowPaths =
            workflowDir
                .listFiles { file -> file.extension == "yml" }
                ?.map { ".github/workflows/${it.name}" }
                ?.sorted()
                ?: error("Could not list workflow files from ${workflowDir.absolutePath}")

        assertTrue(
            actualWorkflowPaths == workflowPaths.sorted(),
            "Workflow file set changed. Update the contract test before editing workflow behavior.",
        )

        workflowPaths.forEach { path ->
            val workflow = readRepoFile(path)
            assertTrue(
                workflow.contains(defaultBranchGuard),
                "$path must gate runner execution to the default branch.",
            )
        }
    }
}

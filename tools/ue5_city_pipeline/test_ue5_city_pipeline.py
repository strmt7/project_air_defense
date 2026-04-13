from __future__ import annotations

import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from build_master_tileset import build_master_tileset
from city_sources import build_manifest, get_source, load_sources, repo_root
from patch_cesium_for_unreal import (
    patch_metadata_statistic_semantic_default,
    patch_generate_material_utility_header,
    strip_deprecated_http_keys,
)


class UE5CityPipelineTest(unittest.TestCase):
    def test_catalog_has_expected_sources(self) -> None:
        ids = {source.id for source in load_sources()}
        self.assertIn("helsinki_kalasatama_3dtiles", ids)
        self.assertIn("helsinki_kalasatama_mesh", ids)
        self.assertIn("berlin_official_city_mesh", ids)
        self.assertIn("rotterdam_3dbag_obj", ids)

    def test_manifest_has_required_policy(self) -> None:
        manifest = build_manifest(get_source("helsinki_kalasatama_3dtiles"))
        self.assertEqual(manifest["required_scene_policy"]["engine"], "Unreal Engine 5")
        self.assertFalse(manifest["required_scene_policy"]["static_gameplay_backdrops_allowed"])
        self.assertIn("zoom", manifest["required_scene_policy"]["camera_controls_required"])
        self.assertEqual(manifest["runtime_strategy"]["renderer"], "Cesium for Unreal")
        self.assertTrue(manifest["runtime_strategy"]["requires_legacy_b3dm_upgrade"])
        self.assertEqual(
            manifest["runtime_strategy"]["upgrade_tool"],
            "CesiumGS/3d-tiles-tools upgrade",
        )

    def test_generator_emits_expected_manifest(self) -> None:
        generator = repo_root() / "tools" / "ue5_city_pipeline" / "generate_import_manifest.py"
        with tempfile.TemporaryDirectory() as temp_dir:
            output = Path(temp_dir) / "manifest.json"
            subprocess.run(
                [
                    sys.executable,
                    str(generator),
                    "--source",
                    "helsinki_kalasatama_3dtiles",
                    "--output",
                    str(output),
                ],
                check=True,
            )
            manifest = json.loads(output.read_text(encoding="utf-8"))
            self.assertEqual(manifest["source_id"], "helsinki_kalasatama_3dtiles")
            self.assertEqual(manifest["ue5_project_root"], "ue5/ProjectAirDefenseUE5")
            self.assertEqual(
                manifest["runtime_strategy"]["entry_tileset_relative_path"],
                "ue5/ProjectAirDefenseUE5/ExternalData/helsinki_kalasatama_3dtiles/tileset.json",
            )

    def test_project_scaffold_exists(self) -> None:
        project_file = repo_root() / "ue5" / "ProjectAirDefenseUE5" / "ProjectAirDefenseUE5.uproject"
        project = json.loads(project_file.read_text(encoding="utf-8"))
        self.assertEqual(project["EngineAssociation"], "5.7")
        self.assertEqual(project["Modules"][0]["Name"], "ProjectAirDefenseUE5")
        plugin_names = {plugin["Name"] for plugin in project["Plugins"]}
        self.assertIn("CesiumForUnreal", plugin_names)
        self.assertIn("EnhancedInput", plugin_names)

    def test_default_map_uses_template_default(self) -> None:
        config_path = repo_root() / "ue5" / "ProjectAirDefenseUE5" / "Config" / "DefaultEngine.ini"
        config_text = config_path.read_text(encoding="utf-8")
        self.assertIn("GameDefaultMap=/Engine/Maps/Entry", config_text)
        self.assertIn("EditorStartupMap=/Engine/Maps/Entry", config_text)

    def test_graphics_user_settings_own_runtime_toggles(self) -> None:
        engine_config = (repo_root() / "ue5" / "ProjectAirDefenseUE5" / "Config" / "DefaultEngine.ini").read_text(
            encoding="utf-8"
        )
        user_settings_config = (
            repo_root() / "ue5" / "ProjectAirDefenseUE5" / "Config" / "DefaultGameUserSettings.ini"
        ).read_text(encoding="utf-8")

        self.assertNotIn("r.AntiAliasingMethod=", engine_config)
        self.assertNotIn("r.DefaultFeature.AmbientOcclusion=", engine_config)
        self.assertIn("PreferredAntiAliasingMethod=TSR", user_settings_config)
        self.assertIn("bAmbientOcclusionEnabled=True", user_settings_config)
        self.assertIn("bMotionBlurEnabled=False", user_settings_config)

    def test_default_game_stages_external_data(self) -> None:
        config_path = repo_root() / "ue5" / "ProjectAirDefenseUE5" / "Config" / "DefaultGame.ini"
        config_text = config_path.read_text(encoding="utf-8")
        self.assertIn('+DirectoriesToAlwaysCook=(Path="/CesiumForUnreal")', config_text)
        self.assertIn("LocalTilesetRootRelativePath=ExternalData/helsinki_kalasatama_3dtiles", config_text)
        self.assertIn('+DirectoriesToAlwaysStageAsNonUFS=(Path="../ExternalData")', config_text)

    def test_master_tileset_builder_emits_parent_tileset(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            self._write_root_tile(root, "Tile_p001_p001", [0.0, 0.0, 0.0, 120.0], 64.0)
            self._write_root_tile(root, "Tile_p001_p002", [600.0, 0.0, 0.0, 140.0], 48.0)

            tileset = build_master_tileset(root)
            self.assertEqual(tileset["asset"]["version"], "1.0")
            self.assertEqual(len(tileset["root"]["children"]), 2)
            self.assertEqual(
                tileset["root"]["children"][0]["content"]["url"],
                "Tile_p001_p001/Tile_p001_p001.json",
            )
            self.assertAlmostEqual(tileset["root"]["boundingVolume"]["sphere"][3], 440.0)

    def test_cesium_patcher_updates_local_plugin_files(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            plugin_root = Path(temp_dir) / "CesiumForUnreal"
            config_dir = plugin_root / "Config"
            public_dir = plugin_root / "Source" / "CesiumRuntime" / "Public"
            source_dir = plugin_root / "Source" / "CesiumRuntime" / "Private"
            config_dir.mkdir(parents=True, exist_ok=True)
            public_dir.mkdir(parents=True, exist_ok=True)
            source_dir.mkdir(parents=True, exist_ok=True)

            (config_dir / "Engine.ini").write_text(
                "[HTTP.HttpThread]\nRunningThreadedRequestLimit=100\n[HTTP]\nHttpMaxConnectionsPerServer=40\n",
                encoding="utf-8",
            )
            (config_dir / "Editor.ini").write_text(
                "[HTTP.HttpThread]\nRunningThreadedRequestLimitEditor=100\n",
                encoding="utf-8",
            )
            header = source_dir / "GenerateMaterialUtility.h"
            header.write_text(
                '#include "CoreMinimal.h"\n'
                "#if WITH_EDITOR\n"
                "enum class ECesiumMetadataStatisticSemantic : uint8;\n"
                "struct FCesiumMetadataValue;\n"
                "struct FCesiumMetadataValueType;\n"
                "#endif\n",
                encoding="utf-8",
            )
            metadata_header = public_dir / "CesiumFeaturesMetadataDescription.h"
            metadata_header.write_text(
                "struct FCesiumMetadataPropertyStatisticValue {\n"
                "  ECesiumMetadataStatisticSemantic Semantic;\n"
                "};\n",
                encoding="utf-8",
            )

            self.assertEqual(strip_deprecated_http_keys(plugin_root), 2)
            self.assertTrue(patch_generate_material_utility_header(plugin_root))
            self.assertTrue(patch_metadata_statistic_semantic_default(plugin_root))

            self.assertNotIn(
                "RunningThreadedRequestLimit",
                (config_dir / "Engine.ini").read_text(encoding="utf-8"),
            )
            patched_header = header.read_text(encoding="utf-8")
            self.assertIn('#include "CesiumFeaturesMetadataDescription.h"', patched_header)
            self.assertIn('#include "CesiumMetadataValue.h"', patched_header)
            self.assertIn('#include "CesiumMetadataValueType.h"', patched_header)
            self.assertNotIn("ECesiumMetadataStatisticSemantic", patched_header)
            self.assertNotIn("struct FCesiumMetadataValue;", patched_header)
            self.assertIn(
                "ECesiumMetadataStatisticSemantic Semantic = ECesiumMetadataStatisticSemantic::None;",
                metadata_header.read_text(encoding="utf-8"),
            )

    @staticmethod
    def _write_root_tile(root: Path, directory_name: str, sphere: list[float], geometric_error: float) -> None:
        tile_dir = root / directory_name
        tile_dir.mkdir(parents=True, exist_ok=True)
        payload = {
            "asset": {"version": "1.0"},
            "geometricError": geometric_error,
            "root": {
                "boundingVolume": {"sphere": sphere},
                "geometricError": geometric_error,
                "refine": "REPLACE",
                "children": [],
            },
        }
        (tile_dir / f"{directory_name}.json").write_text(json.dumps(payload), encoding="utf-8")


if __name__ == "__main__":
    unittest.main()

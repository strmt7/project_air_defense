from __future__ import annotations

import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from build_master_tileset import build_master_tileset
from city_sources import build_manifest, get_source, load_sources, repo_root


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
        self.assertIn("GameDefaultMap=/Engine/Maps/Templates/Template_Default", config_text)
        self.assertIn("EditorStartupMap=/Engine/Maps/Templates/Template_Default", config_text)

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

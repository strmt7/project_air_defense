from __future__ import annotations

import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from city_sources import build_manifest, get_source, load_sources, repo_root


class UE5CityPipelineTest(unittest.TestCase):
    def test_catalog_has_expected_sources(self) -> None:
        ids = {source.id for source in load_sources()}
        self.assertIn("helsinki_kalasatama_mesh", ids)
        self.assertIn("berlin_official_city_mesh", ids)
        self.assertIn("rotterdam_3dbag_obj", ids)

    def test_manifest_has_required_policy(self) -> None:
        manifest = build_manifest(get_source("helsinki_kalasatama_mesh"))
        self.assertEqual(manifest["required_scene_policy"]["engine"], "Unreal Engine 5")
        self.assertFalse(manifest["required_scene_policy"]["static_gameplay_backdrops_allowed"])
        self.assertIn("zoom", manifest["required_scene_policy"]["camera_controls_required"])

    def test_generator_emits_expected_manifest(self) -> None:
        generator = repo_root() / "tools" / "ue5_city_pipeline" / "generate_import_manifest.py"
        with tempfile.TemporaryDirectory() as temp_dir:
            output = Path(temp_dir) / "manifest.json"
            subprocess.run(
                [
                    sys.executable,
                    str(generator),
                    "--source",
                    "helsinki_kalasatama_mesh",
                    "--output",
                    str(output),
                ],
                check=True,
            )
            manifest = json.loads(output.read_text(encoding="utf-8"))
            self.assertEqual(manifest["source_id"], "helsinki_kalasatama_mesh")
            self.assertEqual(manifest["ue5_project_root"], "ue5/ProjectAirDefenseUE5")

    def test_project_scaffold_exists(self) -> None:
        project_file = repo_root() / "ue5" / "ProjectAirDefenseUE5" / "ProjectAirDefenseUE5.uproject"
        project = json.loads(project_file.read_text(encoding="utf-8"))
        self.assertEqual(project["EngineAssociation"], "5.6")
        self.assertEqual(project["Modules"][0]["Name"], "ProjectAirDefenseUE5")


if __name__ == "__main__":
    unittest.main()

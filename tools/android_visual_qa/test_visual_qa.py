from __future__ import annotations

import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("visual_qa.py")
SPEC = importlib.util.spec_from_file_location("visual_qa", MODULE_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError("Failed to load visual_qa.py for tests.")
visual_qa = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = visual_qa
SPEC.loader.exec_module(visual_qa)


class VisualQaTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.temp_dir = Path(tempfile.mkdtemp(prefix="visual-qa-test-"))
        cls.image_path, cls.template_path = visual_qa.build_selftest_fixture(cls.temp_dir)
        cls.image = visual_qa.Image.open(cls.image_path).convert("RGB")
        try:
            visual_qa.resolve_tesseract()
            cls.tesseract_ready = True
        except Exception:
            cls.tesseract_ready = False

    def test_template_match_finds_button(self) -> None:
        match = visual_qa.match_template(self.image, str(self.template_path), threshold=0.9)
        self.assertGreaterEqual(match.score, 0.99)
        self.assertGreater(match.center_x, 950)
        self.assertGreater(match.center_y, 220)

    def test_ocr_finds_project_title(self) -> None:
        if not self.tesseract_ready:
            self.skipTest("Tesseract OCR is not installed in this environment.")
        lines = visual_qa.ocr_lines(self.image)
        match = visual_qa.find_text_match(lines, "PROJECT AIR DEFENSE", exact=False, case_sensitive=False)
        self.assertIsNotNone(match)
        assert match is not None
        self.assertIn("PROJECT", match.text.upper())
        self.assertGreaterEqual(match.confidence, 35.0)


if __name__ == "__main__":
    unittest.main()

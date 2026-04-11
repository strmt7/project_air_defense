---
name: android-visual-qa
description: Use when validating Project Air Defense with screenshots, OCR, template matching, or live emulator navigation, especially when libGDX SurfaceView content is not visible in the Android accessibility tree.
---

# Android Visual QA

Use this skill when the question is "what is actually on screen?" rather than only "did the process survive?"

## Read next

- [`docs/android-visual-qa.md`](../../../docs/android-visual-qa.md)
- [`docs/reference/android-visual-qa-sources.md`](../../../docs/reference/android-visual-qa-sources.md)

## Workflow

1. Probe the toolchain first.
   - `py -3 tools/android_visual_qa/visual_qa.py probe`
2. Resolve and launch the exact installed app variant when package names are uncertain.
   - `py -3 tools/android_visual_qa/visual_qa.py launch --device emulator-5554 --package com.airdefense.game.benchmark`
3. Use the Android UI tree only for native surfaces.
   - system dialogs
   - permission prompts
   - launcher chrome
4. Use screenshot-driven checks for libGDX gameplay surfaces.
   - `find-text` or `tap-text` for readable buttons or HUD labels
   - `match-template` or `tap-template` when OCR is too weak
5. Save hard proof.
   - screenshot
   - OCR or template JSON
   - logcat when the flow matters
6. Distinguish the outcomes.
   - installed
   - process alive
   - correct screen visible
   - correct target tapped

## Rules

- Do not guess tap coordinates from memory when the toolchain can read the screen.
- Prefer OCR over template matching when the control has strong text.
- Prefer template matching over OCR when the target is icon-heavy or stylized.
- Keep one active automation stack in this repo: `adbutils` + `OpenCV` + `Tesseract` + `Pillow`, with `scrcpy` for live confirmation.
- Treat Appium, Maestro, Airtest, and SikuliX as evaluated references unless a future task needs them explicitly.

## Validation

- `py -3 tools/android_visual_qa/test_visual_qa.py`
- `py -3 tools/android_visual_qa/visual_qa.py selftest`
- `py -3 tools/android_visual_qa/visual_qa.py launch --device emulator-5554 --package com.airdefense.game.benchmark`
- `py -3 tools/android_visual_qa/visual_qa.py find-text "ENTER AIRSPACE" --device emulator-5554`

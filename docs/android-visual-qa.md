# Android Visual QA

Project Air Defense mixes native Android chrome with a libGDX `SurfaceView`. That matters:

- use the Android UI tree for native dialogs and system chrome
- use screenshot OCR or template matching for menu, HUD, and battle surfaces

## Active stack

- `adbutils 2.12.0`: device discovery, screenshot capture, taps
- `opencv-python 4.13.0.92`: template matching
- `pytesseract 0.3.13` + Tesseract `5.5.0.20241111`: OCR
- `Pillow 12.2.0`: image conversion and fixtures
- `scrcpy 3.3.4`: live human confirmation

## Why this stack is active

- It works on `SurfaceView` content where accessibility-tree tools lose detail.
- It keeps one deterministic repo-local path instead of mixing several mobile automation frameworks.
- It is simple to verify with saved screenshots and JSON output.

## Commands

Bootstrap on Windows:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\android_visual_qa\bootstrap.ps1
```

Probe the installed stack:

```powershell
py -3 .\tools\android_visual_qa\visual_qa.py probe
```

Launch the exact installed variant without guessing the activity:

```powershell
py -3 .\tools\android_visual_qa\visual_qa.py launch --device emulator-5554 --package com.airdefense.game.benchmark
```

Read live text from the emulator:

```powershell
py -3 .\tools\android_visual_qa\visual_qa.py find-text "ENTER AIRSPACE" --device emulator-5554
```

Tap the detected text instead of guessing coordinates:

```powershell
py -3 .\tools\android_visual_qa\visual_qa.py tap-text "ENTER AIRSPACE" --device emulator-5554
```

Match an icon or cropped button when OCR is weak:

```powershell
py -3 .\tools\android_visual_qa\visual_qa.py match-template --device emulator-5554 --template .\benchmark-results\visual-qa\button.png
```

## Evaluated but not active

- Appium: strong for native accessibility-driven flows, weaker for this repo's primary `SurfaceView` battle surface unless extra image plugins are added.
- Maestro: strong for native mobile flows and readable YAML journeys, but still not the main path for pixel-driven libGDX gameplay validation.
- Airtest: game-friendly image automation, but its own docs explicitly warn that image recognition is probabilistic and threshold-sensitive.
- SikuliX: historically important for image automation, but not selected as the active repo dependency.

## Verification lane

1. `py -3 .\tools\android_visual_qa\test_visual_qa.py`
2. `py -3 .\tools\android_visual_qa\visual_qa.py selftest`
3. live emulator proof with `find-text`, `tap-text`, or `match-template`
4. keep screenshot, OCR/template JSON, and logcat together when reporting a navigation result

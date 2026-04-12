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

## Preferred device lane

- active smoke-test AVD: Android 15 / API 35 / Google APIs / Pixel 9 Pro
- local verified AVD name: `airdefense_android15_pixel9pro`
- active smoke-test package after `:android:installDebug`: `com.airdefense.game.debug`
- benchmark package exists and remains valid for the benchmark build type: `com.airdefense.game.benchmark`

Known bad lane:

- the legacy `airdefense_api36` AVD has already stalled at the Android logo with `adb offline`
- do not use it as proof of game health until it is fixed

## Commands

Bootstrap on Windows:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\android_visual_qa\bootstrap.ps1
```

Probe the installed stack:

```powershell
py -3 .\tools\android_visual_qa\visual_qa.py probe
```

Verify emulator health before app claims:

```powershell
adb devices
adb shell getprop sys.boot_completed
```

Expected result:

- device state is `device`, not `offline`
- boot property is `1`

Launch the exact installed variant without guessing the activity:

```powershell
py -3 .\tools\android_visual_qa\visual_qa.py launch --device emulator-5554 --package com.airdefense.game.debug
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
3. verify emulator health with `adb devices` and `adb shell getprop sys.boot_completed`
4. capture the pre-tap menu state with screenshot + OCR
5. tap through with `tap-text` or `tap-template`
6. capture the post-tap battle state with screenshot + OCR
7. keep screenshot, OCR/template JSON, logcat, and crash buffer together when reporting a navigation result

## Rules that prevent false positives

- Do not call a flow verified from process survival alone.
- Do not call a flow verified from logcat alone.
- Do not call a flow verified from a single screenshot alone.
- A navigation claim is only valid when the pre-tap target exists on screen, the tap tool reports a hit, and the post-tap screen contains battle-only text.
- Fresh Android emulator boots can show the system `Viewing full screen` education overlay. Clear it before OCR-based menu claims.

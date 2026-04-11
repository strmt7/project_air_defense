from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable

import adbutils
import cv2
import numpy as np
import pytesseract
from PIL import Image, ImageDraw, ImageFont

REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_ARTIFACT_DIR = REPO_ROOT / "benchmark-results" / "visual-qa"
COMMON_ADB_PATHS = (
    Path(r"C:\Program Files (x86)\Android\android-sdk\platform-tools\adb.exe"),
    Path(r"C:\Program Files\Android\android-sdk\platform-tools\adb.exe"),
)
COMMON_TESSERACT_PATHS = (
    Path(r"C:\Program Files\Tesseract-OCR\tesseract.exe"),
    Path(r"C:\Program Files (x86)\Tesseract-OCR\tesseract.exe"),
)
COMMON_SCRCPY_PATHS = (
    Path(r"C:\Users\strat\AppData\Local\Microsoft\WinGet\Packages\Genymobile.scrcpy_Microsoft.Winget.Source_8wekyb3d8bbwe\scrcpy-win64-v3.3.4\scrcpy.exe"),
)
COMMON_FONT_PATHS = (
    Path(r"C:\Windows\Fonts\segoeuib.ttf"),
    Path(r"C:\Windows\Fonts\arialbd.ttf"),
    Path(r"C:\Windows\Fonts\arial.ttf"),
)


@dataclass
class TextMatch:
    text: str
    confidence: float
    left: int
    top: int
    width: int
    height: int

    @property
    def center_x(self) -> int:
        return self.left + self.width // 2

    @property
    def center_y(self) -> int:
        return self.top + self.height // 2

    def to_dict(self) -> dict[str, object]:
        payload = asdict(self)
        payload["center_x"] = self.center_x
        payload["center_y"] = self.center_y
        return payload


@dataclass
class TemplateMatch:
    score: float
    left: int
    top: int
    width: int
    height: int

    @property
    def center_x(self) -> int:
        return self.left + self.width // 2

    @property
    def center_y(self) -> int:
        return self.top + self.height // 2

    def to_dict(self) -> dict[str, object]:
        payload = asdict(self)
        payload["center_x"] = self.center_x
        payload["center_y"] = self.center_y
        return payload


def resolve_executable(name: str, env_var: str, extra_paths: Iterable[Path]) -> str | None:
    env_value = os.environ.get(env_var, "").strip().strip('"')
    if env_value:
        env_path = Path(env_value)
        if env_path.exists():
            return str(env_path)
    which_path = shutil.which(name)
    if which_path:
        return which_path
    for candidate in extra_paths:
        if candidate.exists():
            return str(candidate)
    return None


def resolve_tesseract() -> str:
    path = resolve_executable("tesseract", "TESSERACT_CMD", COMMON_TESSERACT_PATHS)
    if path is None:
        raise RuntimeError("tesseract.exe was not found. Install Tesseract OCR or set TESSERACT_CMD.")
    pytesseract.pytesseract.tesseract_cmd = path
    return path


def resolve_scrcpy() -> str | None:
    return resolve_executable("scrcpy", "SCRCPY_PATH", COMMON_SCRCPY_PATHS)


def resolve_adb() -> str | None:
    sdk_root_value = os.environ.get("ANDROID_SDK_ROOT", "").strip().strip('"')
    home_value = os.environ.get("ANDROID_HOME", "").strip().strip('"')
    extra_paths = list(COMMON_ADB_PATHS)
    if sdk_root_value:
        extra_paths.append(Path(sdk_root_value) / "platform-tools" / "adb.exe")
    if home_value:
        extra_paths.append(Path(home_value) / "platform-tools" / "adb.exe")
    return resolve_executable("adb", "ADB", extra_paths)


def adb_device(serial: str | None):
    devices = adbutils.adb.device_list()
    if not devices:
        raise RuntimeError("No ADB devices are connected.")
    if serial:
        for device in devices:
            if device.serial == serial:
                return device
        raise RuntimeError(f"ADB device '{serial}' was not found.")
    return devices[0]


def capture_image(serial: str | None) -> Image.Image:
    return adb_device(serial).screenshot().convert("RGB")


def load_image(image_path: str | None, serial: str | None) -> Image.Image:
    if image_path:
        return Image.open(image_path).convert("RGB")
    return capture_image(serial)


def preprocess_for_ocr(image: Image.Image, scale: float) -> np.ndarray:
    gray = np.array(image.convert("L"))
    if scale != 1.0:
        gray = cv2.resize(gray, None, fx=scale, fy=scale, interpolation=cv2.INTER_CUBIC)
    _, thresholded = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    return thresholded


def _line_key(data: dict[str, list[object]], index: int) -> tuple[int, int, int, int]:
    return (
        int(data["block_num"][index]),
        int(data["par_num"][index]),
        int(data["line_num"][index]),
        int(data["page_num"][index]),
    )


def ocr_lines(
    image: Image.Image,
    *,
    scale: float = 2.0,
    psm: int = 11,
    min_confidence: float = 35.0,
) -> list[TextMatch]:
    resolve_tesseract()
    processed = preprocess_for_ocr(image, scale)
    data = pytesseract.image_to_data(
        processed,
        output_type=pytesseract.Output.DICT,
        config=f"--psm {psm}",
    )

    grouped: dict[tuple[int, int, int, int], list[dict[str, object]]] = {}
    item_count = len(data["text"])
    for index in range(item_count):
        text = str(data["text"][index]).strip()
        if not text:
            continue
        confidence = float(data["conf"][index])
        if confidence < min_confidence:
            continue
        entry = {
            "text": text,
            "confidence": confidence,
            "left": int(int(data["left"][index]) / scale),
            "top": int(int(data["top"][index]) / scale),
            "width": max(1, int(int(data["width"][index]) / scale)),
            "height": max(1, int(int(data["height"][index]) / scale)),
        }
        grouped.setdefault(_line_key(data, index), []).append(entry)

    lines: list[TextMatch] = []
    for entries in grouped.values():
        entries.sort(key=lambda item: int(item["left"]))
        left = min(int(item["left"]) for item in entries)
        top = min(int(item["top"]) for item in entries)
        right = max(int(item["left"]) + int(item["width"]) for item in entries)
        bottom = max(int(item["top"]) + int(item["height"]) for item in entries)
        line_text = " ".join(str(item["text"]) for item in entries)
        line_conf = sum(float(item["confidence"]) for item in entries) / len(entries)
        lines.append(
            TextMatch(
                text=line_text,
                confidence=round(line_conf, 2),
                left=left,
                top=top,
                width=max(1, right - left),
                height=max(1, bottom - top),
            )
        )
    lines.sort(key=lambda match: (match.top, match.left))
    return lines


def normalize_text(text: str, *, case_sensitive: bool) -> str:
    collapsed = " ".join(text.split())
    return collapsed if case_sensitive else collapsed.lower()


def find_text_match(
    lines: list[TextMatch],
    query: str,
    *,
    exact: bool,
    case_sensitive: bool,
) -> TextMatch | None:
    query_norm = normalize_text(query, case_sensitive=case_sensitive)
    candidates: list[TextMatch] = []
    for line in lines:
        haystack = normalize_text(line.text, case_sensitive=case_sensitive)
        matched = haystack == query_norm if exact else query_norm in haystack
        if matched:
            candidates.append(line)
    if not candidates:
        return None
    return sorted(candidates, key=lambda item: (-item.confidence, item.top, item.left))[0]


def match_template(
    image: Image.Image,
    template_path: str,
    *,
    threshold: float = 0.9,
) -> TemplateMatch:
    source = np.array(image.convert("L"))
    template = cv2.imread(str(template_path), cv2.IMREAD_GRAYSCALE)
    if template is None:
        raise RuntimeError(f"Template image '{template_path}' could not be read.")
    if template.shape[0] > source.shape[0] or template.shape[1] > source.shape[1]:
        raise RuntimeError("Template is larger than the source image.")

    result = cv2.matchTemplate(source, template, cv2.TM_CCOEFF_NORMED)
    _, max_value, _, max_location = cv2.minMaxLoc(result)
    if max_value < threshold:
        raise RuntimeError(
            f"Template match score {max_value:.3f} was below threshold {threshold:.3f}."
        )
    height, width = template.shape[:2]
    return TemplateMatch(
        score=round(float(max_value), 4),
        left=int(max_location[0]),
        top=int(max_location[1]),
        width=int(width),
        height=int(height),
    )


def write_image(image: Image.Image, out_path: str) -> Path:
    path = Path(out_path)
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)
    return path


def run_subprocess(command: list[str]) -> str:
    completed = subprocess.run(command, capture_output=True, text=True, check=True)
    return completed.stdout.strip() or completed.stderr.strip()


def probe_payload() -> dict[str, object]:
    tesseract_path = resolve_tesseract()
    scrcpy_path = resolve_scrcpy()
    adb_path = resolve_adb()
    devices = []
    for device in adbutils.adb.device_list():
        devices.append(
            {
                "serial": device.serial,
                "model": device.shell("getprop ro.product.model").strip(),
                "size": device.shell("wm size").strip(),
            }
        )

    scrcpy_version = None
    if scrcpy_path:
        try:
            scrcpy_version = run_subprocess([scrcpy_path, "--version"]).splitlines()[0]
        except Exception:
            scrcpy_version = "unavailable"

    return {
        "python": sys.version.split()[0],
        "adbutils": adbutils.__version__,
        "opencv": cv2.__version__,
        "pytesseract": pytesseract.__version__,
        "adb": adb_path,
        "tesseract": {
            "path": tesseract_path,
            "version": str(pytesseract.get_tesseract_version()),
        },
        "scrcpy": {
            "path": scrcpy_path,
            "version": scrcpy_version,
        },
        "devices": devices,
    }


def print_payload(payload: dict[str, object]) -> None:
    print(json.dumps(payload, indent=2))


def resolve_font(size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    for font_path in COMMON_FONT_PATHS:
        if font_path.exists():
            return ImageFont.truetype(str(font_path), size=size)
    return ImageFont.load_default()


def build_selftest_fixture(out_dir: Path) -> tuple[Path, Path]:
    out_dir.mkdir(parents=True, exist_ok=True)
    image = Image.new("RGB", (1280, 720), (12, 18, 34))
    draw = ImageDraw.Draw(image)
    title_font = resolve_font(48)
    button_font = resolve_font(56)
    body_font = resolve_font(28)

    draw.text((96, 96), "PROJECT AIR DEFENSE", fill=(210, 230, 255), font=title_font)
    draw.text((96, 164), "Touch-first battle validation", fill=(150, 170, 190), font=body_font)

    button_bounds = (740, 180, 1180, 328)
    draw.rounded_rectangle(button_bounds, radius=34, fill=(244, 248, 255), outline=(125, 210, 255), width=6)
    button_text = "ENTER AIRSPACE"
    text_box = draw.textbbox((0, 0), button_text, font=button_font)
    text_width = text_box[2] - text_box[0]
    text_height = text_box[3] - text_box[1]
    text_x = button_bounds[0] + ((button_bounds[2] - button_bounds[0]) - text_width) // 2
    text_y = button_bounds[1] + ((button_bounds[3] - button_bounds[1]) - text_height) // 2 - 6
    draw.text((text_x, text_y), button_text, fill=(14, 24, 38), font=button_font)

    image_path = out_dir / "selftest_screen.png"
    template_path = out_dir / "selftest_button.png"
    image.save(image_path)
    image.crop(button_bounds).save(template_path)
    return image_path, template_path


def command_probe(_: argparse.Namespace) -> int:
    print_payload(probe_payload())
    return 0


def command_devices(_: argparse.Namespace) -> int:
    print_payload({"devices": probe_payload()["devices"]})
    return 0


def command_capture(args: argparse.Namespace) -> int:
    image = capture_image(args.device)
    path = write_image(image, args.out)
    print_payload({"capture": str(path)})
    return 0


def command_launch(args: argparse.Namespace) -> int:
    device = adb_device(args.device)
    resolved = device.shell(
        f"cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER {args.package}"
    )
    component = None
    for line in resolved.splitlines():
        stripped = line.strip()
        if "/" in stripped:
            component = stripped
    if component is None:
        raise RuntimeError(f"No launcher activity was resolved for package '{args.package}'.")
    device.shell(f"am force-stop {args.package}")
    device.shell(f"am start -n {component}")
    print_payload({"package": args.package, "component": component})
    return 0


def command_ocr(args: argparse.Namespace) -> int:
    image = load_image(args.image, args.device)
    lines = [line.to_dict() for line in ocr_lines(image, scale=args.scale, psm=args.psm, min_confidence=args.min_confidence)]
    print_payload({"lines": lines})
    return 0


def command_find_text(args: argparse.Namespace, *, tap: bool) -> int:
    image = load_image(args.image, args.device)
    lines = ocr_lines(image, scale=args.scale, psm=args.psm, min_confidence=args.min_confidence)
    match = find_text_match(lines, args.query, exact=args.exact, case_sensitive=args.case_sensitive)
    if match is None:
        raise RuntimeError(f"Text '{args.query}' was not found.")
    if tap:
        if not args.device:
            raise RuntimeError("--device is required for tap-text.")
        adb_device(args.device).click(match.center_x, match.center_y)
    payload = {"match": match.to_dict(), "tapped": tap}
    print_payload(payload)
    return 0


def command_match_template(args: argparse.Namespace, *, tap: bool) -> int:
    image = load_image(args.image, args.device)
    match = match_template(image, args.template, threshold=args.threshold)
    if tap:
        if not args.device:
            raise RuntimeError("--device is required for tap-template.")
        adb_device(args.device).click(match.center_x, match.center_y)
    print_payload({"match": match.to_dict(), "tapped": tap})
    return 0


def command_selftest(args: argparse.Namespace) -> int:
    out_dir = Path(args.out_dir) if args.out_dir else DEFAULT_ARTIFACT_DIR / "selftest"
    image_path, template_path = build_selftest_fixture(out_dir)
    image = Image.open(image_path).convert("RGB")
    lines = ocr_lines(image)
    text_match = find_text_match(lines, "PROJECT AIR DEFENSE", exact=False, case_sensitive=False)
    if text_match is None:
        raise RuntimeError("Self-test OCR did not find 'PROJECT AIR DEFENSE'.")
    template = match_template(image, str(template_path), threshold=0.9)
    print_payload(
        {
            "probe": probe_payload(),
            "selftest": {
                "image": str(image_path),
                "template": str(template_path),
                "text_match": text_match.to_dict(),
                "template_match": template.to_dict(),
            },
        }
    )
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Android emulator visual QA toolkit for Project Air Defense.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    subparsers.add_parser("probe")
    subparsers.add_parser("devices")

    capture_parser = subparsers.add_parser("capture")
    capture_parser.add_argument("--device", help="ADB serial. Defaults to the first connected device.")
    capture_parser.add_argument("--out", required=True, help="Output image path.")

    launch_parser = subparsers.add_parser("launch")
    launch_parser.add_argument("--device", help="ADB serial. Defaults to the first connected device.")
    launch_parser.add_argument("--package", required=True, help="Installed Android package to launch.")

    ocr_parser = subparsers.add_parser("ocr")
    ocr_parser.add_argument("--device", help="ADB serial. Defaults to the first connected device.")
    ocr_parser.add_argument("--image", help="Existing screenshot path. When omitted, the command captures live.")
    ocr_parser.add_argument("--scale", type=float, default=2.0)
    ocr_parser.add_argument("--psm", type=int, default=11)
    ocr_parser.add_argument("--min-confidence", type=float, default=35.0)

    for name in ("find-text", "tap-text"):
        text_parser = subparsers.add_parser(name)
        text_parser.add_argument("query")
        text_parser.add_argument("--device", help="ADB serial. Required for tap-text.")
        text_parser.add_argument("--image", help="Existing screenshot path. When omitted, the command captures live.")
        text_parser.add_argument("--scale", type=float, default=2.0)
        text_parser.add_argument("--psm", type=int, default=11)
        text_parser.add_argument("--min-confidence", type=float, default=35.0)
        text_parser.add_argument("--exact", action="store_true")
        text_parser.add_argument("--case-sensitive", action="store_true")

    for name in ("match-template", "tap-template"):
        template_parser = subparsers.add_parser(name)
        template_parser.add_argument("--template", required=True)
        template_parser.add_argument("--device", help="ADB serial. Required for tap-template.")
        template_parser.add_argument("--image", help="Existing screenshot path. When omitted, the command captures live.")
        template_parser.add_argument("--threshold", type=float, default=0.9)

    selftest_parser = subparsers.add_parser("selftest")
    selftest_parser.add_argument("--out-dir", help="Artifact directory.")
    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    try:
        if args.command == "probe":
            return command_probe(args)
        if args.command == "devices":
            return command_devices(args)
        if args.command == "capture":
            return command_capture(args)
        if args.command == "launch":
            return command_launch(args)
        if args.command == "ocr":
            return command_ocr(args)
        if args.command == "find-text":
            return command_find_text(args, tap=False)
        if args.command == "tap-text":
            return command_find_text(args, tap=True)
        if args.command == "match-template":
            return command_match_template(args, tap=False)
        if args.command == "tap-template":
            return command_match_template(args, tap=True)
        if args.command == "selftest":
            return command_selftest(args)
    except Exception as exc:
        print(json.dumps({"error": str(exc)}, indent=2))
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

from __future__ import annotations

import argparse
import json
from pathlib import Path

from city_sources import build_manifest, get_source


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate a UE5 city pilot manifest.")
    parser.add_argument("--source", required=True, help="City source id from catalog.json")
    parser.add_argument("--output", required=True, help="Output manifest path")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    source = get_source(args.source)
    manifest = build_manifest(source)
    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

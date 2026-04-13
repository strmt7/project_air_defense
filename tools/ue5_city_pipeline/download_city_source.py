from __future__ import annotations

import argparse
from pathlib import Path
from urllib.request import urlretrieve

from city_sources import get_source


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Download an official UE5 city source.")
    parser.add_argument("--source", required=True, help="City source id from catalog.json")
    parser.add_argument("--dest", required=True, help="Destination directory for raw downloads")
    parser.add_argument("--dry-run", action="store_true", help="Print the planned download only")
    parser.add_argument("--overwrite", action="store_true", help="Allow overwriting an existing file")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    source = get_source(args.source)
    dest_root = Path(args.dest)
    filename = source.download_url.rstrip("/").split("/")[-1] or f"{source.id}.download"
    target = dest_root / source.id / filename
    if args.dry_run:
        print(f"source={source.id}")
        print(f"url={source.download_url}")
        print(f"target={target}")
        return 0
    target.parent.mkdir(parents=True, exist_ok=True)
    if target.exists() and not args.overwrite:
        raise SystemExit(f"Refusing to overwrite existing file: {target}")
    urlretrieve(source.download_url, target)
    print(target)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

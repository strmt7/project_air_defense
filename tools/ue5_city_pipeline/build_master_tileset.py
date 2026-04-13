from __future__ import annotations

import argparse
import json
import math
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate a master 3D Tiles entry point for Helsinki-style tiled exports.")
    parser.add_argument("--root", required=True, help="Root directory containing the extracted 3D Tiles package")
    parser.add_argument("--output", required=True, help="Path to the generated master tileset.json")
    return parser.parse_args()


def _is_root_tile_manifest(path: Path) -> bool:
    if path.suffix.lower() != ".json":
        return False
    parent_name = path.parent.name
    return path.stem == parent_name and parent_name.startswith("Tile_")


def _load_root_tiles(root_dir: Path) -> list[tuple[Path, dict]]:
    manifests: list[tuple[Path, dict]] = []
    for path in sorted(root_dir.rglob("*.json")):
        if _is_root_tile_manifest(path):
            manifests.append((path, json.loads(path.read_text(encoding="utf-8"))))
    if not manifests:
        raise SystemExit(f"No root tile manifests found under {root_dir}")
    return manifests


def _sphere_union(children: list[dict]) -> list[float]:
    spheres = [child["boundingVolume"]["sphere"] for child in children]
    center = [
        sum(sphere[index] for sphere in spheres) / len(spheres)
        for index in range(3)
    ]
    radius = max(
        math.dist(center, sphere[:3]) + float(sphere[3])
        for sphere in spheres
    )
    return [*center, radius]


def build_master_tileset(root_dir: Path) -> dict:
    children: list[dict] = []
    manifests = _load_root_tiles(root_dir)
    asset = manifests[0][1]["asset"]
    for manifest_path, payload in manifests:
        root = payload["root"]
        relative_path = manifest_path.relative_to(root_dir).as_posix()
        children.append(
            {
                "boundingVolume": root["boundingVolume"],
                "geometricError": float(payload.get("geometricError", root.get("geometricError", 0.0))),
                "content": {"url": relative_path},
            }
        )

    return {
        "asset": asset,
        "geometricError": max(child["geometricError"] for child in children),
        "root": {
            "boundingVolume": {"sphere": _sphere_union(children)},
            "refine": "REPLACE",
            "geometricError": max(child["geometricError"] for child in children),
            "children": children,
        },
    }


def main() -> int:
    args = parse_args()
    root_dir = Path(args.root)
    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    tileset = build_master_tileset(root_dir)
    output_path.write_text(json.dumps(tileset, indent=2) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

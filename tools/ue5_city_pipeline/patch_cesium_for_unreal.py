from __future__ import annotations

import argparse
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Patch a local Cesium for Unreal install for the UE5 city pilot.")
    parser.add_argument("--plugin-root", required=True, help="Path to the extracted CesiumForUnreal plugin root")
    return parser.parse_args()


def rewrite_text(path: Path, transform) -> bool:
    original = path.read_text(encoding="utf-8")
    updated = transform(original)
    if updated == original:
        return False
    path.write_text(updated, encoding="utf-8")
    return True


def strip_deprecated_http_keys(plugin_root: Path) -> int:
    updated_count = 0
    for relative in ("Config/Engine.ini", "Config/Editor.ini"):
        path = plugin_root / relative
        if not path.exists():
            continue

        def transform(text: str) -> str:
            lines = [
                line
                for line in text.splitlines()
                if "RunningThreadedRequestLimit" not in line
            ]
            return "\n".join(lines).rstrip() + "\n"

        if rewrite_text(path, transform):
            updated_count += 1
    return updated_count


def patch_generate_material_utility_header(plugin_root: Path) -> bool:
    path = plugin_root / "Source/CesiumRuntime/Private/GenerateMaterialUtility.h"
    if not path.exists():
        return False

    def transform(text: str) -> str:
        text = text.replace(
            '#include "CoreMinimal.h"\n',
            '#include "CoreMinimal.h"\n#include "CesiumFeaturesMetadataDescription.h"\n#include "CesiumMetadataValue.h"\n#include "CesiumMetadataValueType.h"\n',
        )
        text = text.replace(
            "struct FCesiumMetadataValue;\nstruct FCesiumMetadataValueType;\n",
            "",
        )
        text = text.replace(
            "enum class ECesiumMetadataStatisticSemantic : uint8;\n",
            "",
        )
        return text

    return rewrite_text(path, transform)


def patch_metadata_statistic_semantic_default(plugin_root: Path) -> bool:
    path = plugin_root / "Source/CesiumRuntime/Public/CesiumFeaturesMetadataDescription.h"
    if not path.exists():
        return False

    def transform(text: str) -> str:
        return text.replace(
            "  ECesiumMetadataStatisticSemantic Semantic;\n",
            "  ECesiumMetadataStatisticSemantic Semantic = ECesiumMetadataStatisticSemantic::None;\n",
        )

    return rewrite_text(path, transform)


def main() -> int:
    args = parse_args()
    plugin_root = Path(args.plugin_root)
    if not plugin_root.exists():
        raise SystemExit(f"Plugin root not found: {plugin_root}")

    changed = 0
    changed += strip_deprecated_http_keys(plugin_root)
    changed += int(patch_generate_material_utility_header(plugin_root))
    changed += int(patch_metadata_statistic_semantic_default(plugin_root))
    print(f"Patched CesiumForUnreal at {plugin_root} ({changed} file changes)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

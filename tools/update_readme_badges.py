"""Generate the README badge row from canonical repository metadata."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

README_PATH = Path("README.md")
BADGE_METADATA_PATH = Path(".github/readme_badges.json")
BADGE_BLOCK_BEGIN = "<!-- BEGIN GENERATED BADGES -->"
BADGE_BLOCK_END = "<!-- END GENERATED BADGES -->"


def load_metadata(repo_root: Path) -> dict:
    return json.loads((repo_root / BADGE_METADATA_PATH).read_text(encoding="utf-8"))


def render_badge_block(metadata: dict) -> str:
    repo_path = f"{metadata['owner']}/{metadata['repo']}"
    branch = metadata["branch"]
    lines = [BADGE_BLOCK_BEGIN]
    for badge in metadata["badges"]:
        image = badge["image"].format(repo_path=repo_path, branch=branch)
        target = badge["target"].format(repo_path=repo_path, branch=branch)
        lines.append(f"[![{badge['alt']}]({image})]({target})")
    lines.append(BADGE_BLOCK_END)
    return "\n".join(lines)


def extract_badge_block(readme_text: str) -> str:
    start = readme_text.find(BADGE_BLOCK_BEGIN)
    end = readme_text.find(BADGE_BLOCK_END)
    if start == -1 or end == -1:
        raise RuntimeError("README.md is missing generated badge markers.")
    end += len(BADGE_BLOCK_END)
    return readme_text[start:end]


def update_readme(repo_root: Path, write: bool) -> int:
    readme_path = repo_root / README_PATH
    original = readme_path.read_text(encoding="utf-8")
    badge_block = render_badge_block(load_metadata(repo_root))
    updated = original.replace(extract_badge_block(original), badge_block)

    if updated == original:
        return 0

    if not write:
        sys.stderr.write("README.md badge block is out of date. Run `python3 tools/update_readme_badges.py --write`.\n")
        return 1

    readme_path.write_text(updated + ("" if updated.endswith("\n") else "\n"), encoding="utf-8")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Render the generated README badge block.")
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--check", action="store_true", help="Fail if README.md is out of date.")
    mode.add_argument("--write", action="store_true", help="Rewrite README.md in place.")
    args = parser.parse_args()
    repo_root = Path(__file__).resolve().parent.parent
    return update_readme(repo_root, write=args.write)


if __name__ == "__main__":
    raise SystemExit(main())

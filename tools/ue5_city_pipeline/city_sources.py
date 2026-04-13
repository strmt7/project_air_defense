from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any


@dataclass(frozen=True)
class CitySource:
    id: str
    title: str
    official_page: str
    download_url: str
    license_url: str
    license_summary: str
    format: str
    kind: str
    recommended_area_sqkm: float
    preferred_pipeline: str
    notes: tuple[str, ...]

    @classmethod
    def from_dict(cls, payload: dict[str, Any]) -> "CitySource":
        return cls(
            id=payload["id"],
            title=payload["title"],
            official_page=payload["official_page"],
            download_url=payload["download_url"],
            license_url=payload["license_url"],
            license_summary=payload["license_summary"],
            format=payload["format"],
            kind=payload["kind"],
            recommended_area_sqkm=float(payload["recommended_area_sqkm"]),
            preferred_pipeline=payload["preferred_pipeline"],
            notes=tuple(payload["notes"]),
        )


def repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def catalog_path() -> Path:
    return repo_root() / "tools" / "ue5_city_pipeline" / "catalog.json"


def load_sources() -> list[CitySource]:
    payload = json.loads(catalog_path().read_text(encoding="utf-8"))
    return [CitySource.from_dict(item) for item in payload["sources"]]


def get_source(source_id: str) -> CitySource:
    for source in load_sources():
        if source.id == source_id:
            return source
    raise KeyError(f"Unknown source id: {source_id}")


def build_manifest(source: CitySource) -> dict[str, Any]:
    dataset_dir = (
        repo_root()
        / "data"
        / "ue5_city_pilot"
        / source.id.replace("_mesh", "").replace("_official_city", "")
    )
    return {
        "source_id": source.id,
        "title": source.title,
        "official_page": source.official_page,
        "download_url": source.download_url,
        "license_url": source.license_url,
        "license_summary": source.license_summary,
        "format": source.format,
        "kind": source.kind,
        "target_area_sqkm": source.recommended_area_sqkm,
        "preferred_pipeline": source.preferred_pipeline,
        "raw_download_root": "data/external",
        "pilot_output_root": str(dataset_dir.relative_to(repo_root())).replace("\\", "/"),
        "ue5_project_root": "ue5/ProjectAirDefenseUE5",
        "required_scene_policy": {
            "engine": "Unreal Engine 5",
            "nanite": True,
            "world_partition": True,
            "static_gameplay_backdrops_allowed": False,
            "camera_controls_required": [
                "orbit_or_rotate",
                "pan",
                "zoom",
                "reset"
            ]
        },
        "blender_cleanup_steps": [
            "recenter district near local origin",
            "split geometry into streamable blocks",
            "separate buildings, terrain, shoreline, and roads into logical collections",
            "remove broken triangles and duplicate faces",
            "preserve the real skyline silhouette"
        ],
        "notes": list(source.notes),
    }

# Level Asset Source Map

Use this as the compact source map for future levels.

## Ship-safe runtime sources
| Source | License | Best use |
| --- | --- | --- |
| [Ladybug Tools / 3D Models](https://github.com/ladybug-tools/3d-models) | MIT | architectural landmark models |
| [Poly Haven](https://polyhaven.com/license) | CC0 | HDRI, textures, some models |
| [ambientCG](https://docs.ambientcg.com/license/) | CC0 | ground, concrete, asphalt, metal, roughness maps |
| [Kenney](https://kenney.nl/assets?q=city) | CC0 | modular city props and blockout kits |

## City layout and dataset discovery
Use these as layout/reference inputs unless the individual dataset license is confirmed for shipping:
- [awesome-citygml README](https://github.com/OloOcki/awesome-citygml/blob/main/README.md)
- [awesome-citygml software map](https://github.com/OloOcki/awesome-citygml/blob/main/software.md)

High-value references from that catalog for urban-coast levels:
- [Melbourne street-space CityGML](http://www.3dcitydb.net/3dcitydb/fileadmin/public/datasets/Melbourne/Melbourne_Streetspace_CityGML.zip)
- [Victoria buildings](https://www.land.vic.gov.au/maps-and-spatial/spatial-data/vicmap-catalogue/vicmap-buildings)
- [Prague city model](https://geoportalpraha.cz/en/data-and-services/7e6316e95cfe4f36ae06bbfb687bf34b)
- [Montreal LoD2 textured model](https://donnees.montreal.ca/ville-de-montreal/maquette-numerique-plateau-mont-royal-batiments-lod2-avec-textures)
- [Espoo 3D services](https://kartat.espoo.fi/3d/services_en.html)
- [Helsinki 3D city model](https://kartta.hel.fi/3d/#/)
- [Vantaa 3D buildings](https://www.betaavoindata.fi/data/en_GB/dataset/vantaan-3d-rakennukset)
- [Bavaria LoD2](https://geodaten.bayern.de/opengeodata/OpenDataDetail.html?pn=lod2)

## Processing tools from awesome-citygml
- [3DCityDB Web Map](https://github.com/3dcitydb/3dcitydb-web-map): inspect large city datasets fast
- [F3D](https://github.com/f3d-app/f3d): inspect imported mesh formats
- [citygml-tools](https://github.com/citygml4j/citygml-tools): preprocess CityGML files
- [citygml4j](https://github.com/citygml4j/citygml4j): Java CityGML tooling
- [val3dity](https://github.com/tudelft3d/val3dity): geometry validation
- [3dfier](https://github.com/tudelft3d/3dfier): lift 2D GIS to 3D
- [Random3Dcity](https://github.com/tudelft3d/Random3Dcity): procedural massing seeds

## Agent rule
- If the license is not explicitly CC0, MIT, or otherwise clearly shippable for bundled game assets, treat the source as reference-only until verified.

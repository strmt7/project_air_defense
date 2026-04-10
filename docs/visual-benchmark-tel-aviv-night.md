# Tel Aviv Night Visual Benchmark

This document defines the visual direction for the battle scene using real-world Tel Aviv night references.
These sources are references for composition and atmosphere, not assets to copy into the game.

## Reference sources

- Tel Aviv central promenade design notes:
  - https://www.mkarchitects.com/wp-content/uploads/2019/08/promenade-photos-with-credits7-_compressed.pdf
- Tel Aviv skyline at night from Jaffa:
  - https://commons.wikimedia.org/wiki/File%3ATel_Aviv_Skyline_at_night.jpg
- Tel Aviv shoreline skyline by night:
  - https://commons.wikimedia.org/wiki/File%3ASkyline_of_Tel_Aviv_by_night.jpg
- Tel Aviv beach daytime structure reference:
  - https://unsplash.com/photos/people-on-shore-during-daytime-CrH3wCMGeZg
- Tel Aviv night skyline / high-rise references:
  - https://unsplash.com/photos/city-with-high-rise-buildings-during-night-time-FgIJKC4xGS0
  - https://unsplash.com/photos/a-very-tall-building-lit-up-at-night-uPxwPZz1KKU
  - https://unsplash.com/photos/high-rise-buildings-during-night-time-WJyL85xhy4Y
- Tel Aviv beachfront night footage references:
  - https://www.world-footage.com/product/night-view-of-tel-aviv-beachfront-and-skyline/
  - https://www.world-footage.com/product/nighttime-aerial-view-of-tel-aviv-coastline/
  - https://www.world-footage.com/product/night-view-of-vibrant-tel-aviv-beach-and-promenade/

## What the real scene consistently shows

- A readable coast stack:
  - dark sea
  - bright sand band
  - illuminated promenade and road line
  - low beachfront buildings and hotels
  - denser inland towers beyond them
- Strong horizon separation:
  - the skyline sits on a clear horizon band instead of floating in open darkness
- Warm and cool lighting contrast:
  - cool sky and sea
  - warm windows, streetlights, hotel frontage, and traffic light trails
- Dense skyline grouping:
  - towers cluster into districts instead of being scattered as isolated boxes
- A long oblique coastline view:
  - the camera usually reads the city as layered depth, not as a flat frontal wall

## What the game is currently getting wrong

- Buildings still read like floating lit blocks instead of districts with a waterfront edge.
- The coastline stack is too weak, so the sea, beach, promenade, and city do not separate clearly.
- The horizon band is too empty and too dark for the amount of city shown.
- Building silhouettes are too repetitive and too evenly spaced.
- The UI frame still consumes too much of the visual field relative to the scene quality.

## Implementation priorities

1. Strengthen the sea-beach-promenade-city layering before adding more effects.
2. Shift the camera to read the coastline and skyline depth more clearly.
3. Replace evenly scattered towers with clustered waterfront, hotel, and inland-business districts.
4. Add a warm street-and-promenade light rhythm that sits below the cool skyline haze.
5. Keep mobile-safe rendering:
   - emissive windows
   - layered 2D horizon and haze
   - clustered low-poly districts
   - limited real-time lights

## Anti-goals

- Do not chase photo realism by dropping random images into 3D space.
- Do not add expensive effects that hide bad composition.
- Do not treat a running process as visual success.

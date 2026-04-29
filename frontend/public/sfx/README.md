# Sound Effects

Drop short SFX files here, served at `/sfx/<name>.mp3` from the frontend.

Currently used:
- `typing.mp3` — short tick (~30ms) played per non-space character during dialog typewriter reveal.
  If the file is missing, `playSfx` silently no-ops.

Recommended sources: freesound.org (CC0), or any DAW. Keep files under ~10kb so
overlapping playback doesn't spike memory.

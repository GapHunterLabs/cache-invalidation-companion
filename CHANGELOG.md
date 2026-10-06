<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Cache-Invalidation Companion Changelog

## [Unreleased]

### Added

- A description page for the inspection in **Settings | Editor |
  Inspections**, which showed "Under construction".

### Changed

- The rating prompt's local counter keeps one-way fingerprints of findings
  instead of their file paths, and deletes the list that earlier versions
  kept.
- `PRIVACY.md` describes the values the plugin keeps in the IDE's local
  settings.

## [0.1.1]

### Fixed

- Review/star CTA now links to this plugin's own Marketplace
  reviews page instead of the vendor's generic plugin list.

## [0.1.0]

### Added

- Warning on a `.save(...)` call whose class also has a hand-rolled
  `Map`-backed cache field, when that method never touches the cache
  -- stale data can be served after a real update.
- Cache detection covers both `computeIfAbsent` and the manual
  `containsKey`+`put` get-or-compute pattern.

[Unreleased]: https://github.com/GapHunterLabs/cache-invalidation-companion/compare/0.1.1...HEAD
[0.1.1]: https://github.com/GapHunterLabs/cache-invalidation-companion/compare/0.1.0...0.1.1
[0.1.0]: https://github.com/GapHunterLabs/cache-invalidation-companion/commits/0.1.0

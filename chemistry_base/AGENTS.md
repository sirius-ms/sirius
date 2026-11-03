# AGENTS.md — `:chemistry_base`

Central chemistry data model that most other modules depend on: molecular formulas and elements,
(MS/MS) spectra and peaks, ionization/adducts, fingerprint versions, and shared scoring/algorithm
utilities. Root package: `de.unijena.bioinf.ChemistryBase`.

- **Code map**: `build/ai-overview/modules/chemistry_base.md` (a large module — its map may be split
  per package under `build/ai-overview/modules/chemistry_base/`). Regenerate with `./gradlew aiOverview`.
- **Test**: `./gradlew :chemistry_base:test`.
- Changes here ripple widely — prefer additive changes and keep the public API stable. Heavy Lombok
  use; check the map for synthesized `(lombok)` members. See root [AGENTS.md](../AGENTS.md).

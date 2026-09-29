# BioTransformer golden baseline

`golden-baseline.json` records what the **upstream** BioTransformer 3.0 produces, so SIRIUS'
integration can be compared against the reference implementation rather than against itself.

Its main purpose is to gate the CDK work: the BioTransformer jar currently ships a relocated
CDK 2.7.1 whose implementation classes are bound to the project's CDK 2.12 interfaces. This
baseline is the evidence for whether removing that relocation changes the science.

## Provenance

| | |
|---|---|
| Reference | `BioTransformer3.0_20230525.jar` |
| Source | <https://bitbucket.org/wishartlab/biotransformer3.0jar> (master, commit `6432cf887`, 2023-05-25) |
| Data | that repository's `btkb/` and `supportfiles/` folders, same commit |
| CDK | 2.7.1, as bundled in the reference jar |
| HMDB lookup | **off** (`-useDB false`) — the baseline is offline and contacts no server |
| Recorded | 28 substrates x 11 configurations, 331 runs, 9648 product rows |

Substrates are in `substrates.tsv` and cover the cases that actually break this code: tetrahedral
and double-bond stereochemistry, permanent cations, pre-existing conjugates (glucuronide, sulfate),
aromatic heterocycles, steroids, and molecules small enough to have almost no reaction sites.

Each cell stores the product InChIKeys with their reaction ID and precursor, so a mismatch says
which metabolite differs *and* which reaction produced it. An empty product list means the
reference ran and produced nothing — that is a recorded result, not a gap.

## Who reads it

| Test | Task | Scope |
|---|---|---|
| `BioTransformerGoldenSmokeTest` | `test` | 24 cells, ~11s; the per-commit gate |
| `BioTransformerGutDefectTest` | `test` | pins the known human-gut divergence |
| `BioTransformerGoldenAcceptanceTest` | `acceptanceTest` | the full matrix; **still fails**, see below |

## Where the matrix stands

Removing BioTransformer's bundled CDK (`tools/repack-biotransformer`) took the matrix from
**182/308 to 278/308** cells agreeing with the reference. 98 of the 126 disagreements went away and
the stereochemistry failure is gone entirely.

30 cells still differ, in three groups. None of them is a crash; all are chemistry:

| cells | pattern | status |
|---|---|---|
| 22 | human gut: SIRIUS reports **more** products than the reference (e.g. quercetin 8 vs 3, thymol 1 vs 0) | pre-existing, unchanged by the CDK work |
| 5 | morphine: SIRIUS reports **fewer** products (CYP450 10 vs 11, allHuman 111 vs 125, sequence 266 vs 313) | pre-existing |
| 4 | phase II in `COMBINED_RULES` mode: SIRIUS reports exactly **one extra** conjugate (warfarin, tryptophan, estradiol, quercetin) | 2 of these are new since the CDK change |

The baseline has deliberately **not** been re-recorded to make these pass. It is the only evidence
that the CDK migration was safe, and re-recording would destroy it. Deciding whether the reference
or SIRIUS is right for these 30 cells is a scientific question for the maintainers.

## Re-recording it

Only needed when moving to a different BioTransformer release. Takes about 15 minutes.

```bash
mkdir bt && cd bt
# jar + the btkb/ and supportfiles/ folders from the Bitbucket repo above, side by side
cp <this directory>/{make_baseline.py,substrates.tsv} .
python3 make_baseline.py        # writes golden-baseline.json
```

The harness runs the CLI once per substrate and configuration with `-useDB false`, parses the CSV
output and sorts every product list. It records the exit code and stderr tail per run, so a failed
run is distinguishable from one that legitimately produced nothing.

Two configurations were deliberately left out:

- **`superbio`** — the upstream `-b superbio` path. SIRIUS never invokes it: the GUI's "SuperBio"
  entry maps to `ALL_HUMAN` with 4 iterations, and `BioTransformerWrapper.transform` has no superbio
  case. It was also by far the most expensive config (it alone exceeded a 30-minute-per-run limit on
  five substrates).
- **`ABIOTIC`** — exposed by SIRIUS (`MetabolicTransformation.ABIOTIC`) but not reachable through the
  upstream CLI's `-b` options, so it cannot be baselined this way. Covering it needs a small harness
  that calls `AbioticTransformer_rails` directly.

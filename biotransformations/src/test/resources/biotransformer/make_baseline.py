#!/usr/bin/env python3
"""Record a golden baseline from the UPSTREAM BioTransformer 3.0 CLI.

Reference implementation: BioTransformer3.0_20230525.jar from
https://bitbucket.org/wishartlab/biotransformer3.0jar (master, commit 6432cf887),
run against its own CDK 2.7.1 with the HMDB lookup disabled (-useDB false), so the
result is reproducible and offline.

Each configuration mirrors one MetabolicTransformation that SIRIUS exposes.
"""
import concurrent.futures as cf
import csv
import json
import subprocess
import sys
import time
from pathlib import Path

HERE = Path(__file__).parent
JAR = HERE / "BioTransformer3.0_20230525.jar"
OUT = HERE / "out"
TIMEOUT = 1800

# (config id, SIRIUS MetabolicTransformation, extra CLI args)
# cm=3 is the SIRIUS default Cyp450Mode (COMBINED), cm=1 is RULE_BASED;
# pm=1 is the SIRIUS default P2Mode (BT_RULE_BASED).
CONFIGS = [
    ("cyp450_s1_cm1",    "PHASE_1_CYP450",     ["-b", "cyp450",   "-s", "1", "-cm", "1"]),
    ("cyp450_s1_cm3",    "PHASE_1_CYP450",     ["-b", "cyp450",   "-s", "1", "-cm", "3"]),
    ("cyp450_s2_cm3",    "PHASE_1_CYP450",     ["-b", "cyp450",   "-s", "2", "-cm", "3"]),
    ("ecbased_s1",       "EC_BASED",           ["-b", "ecbased",  "-s", "1"]),
    ("ecbased_s2",       "EC_BASED",           ["-b", "ecbased",  "-s", "2"]),
    ("phaseII_s1_pm1",   "PHASE_2",            ["-b", "phaseII",  "-s", "1", "-pm", "1"]),
    ("phaseII_s1_pm3",   "PHASE_2",            ["-b", "phaseII",  "-s", "1", "-pm", "3"]),
    ("hgut_s1",          "HUMAN_GUT",          ["-b", "hgut",     "-s", "1"]),
    ("hgut_s2",          "HUMAN_GUT",          ["-b", "hgut",     "-s", "2"]),
    ("allHuman_s2_cm3",  "ALL_HUMAN",          ["-b", "allHuman", "-s", "2", "-cm", "3", "-pm", "1"]),
    ("seq_cyp450_2_phaseII_1", "HUMAN_CUSTOM_MULTI", ["-q", "cyp450:2; phaseII:1", "-cm", "3", "-pm", "1"]),
]


def run(name, smiles, cfg_id, args):
    out = OUT / f"{name}__{cfg_id}.csv"
    out.unlink(missing_ok=True)
    cmd = ["java", "-jar", str(JAR), "-k", "pred", "-ismi", smiles,
           "-ocsv", str(out), "-useDB", "false", *args]
    t0 = time.time()
    p = subprocess.run(cmd, cwd=HERE, capture_output=True, text=True, timeout=TIMEOUT)
    took = round(time.time() - t0, 1)

    # No output file means the run produced no metabolites; that is a real result, not an error.
    products = []
    if out.exists():
        with out.open() as fh:
            for row in csv.DictReader(fh):
                key = (row.get("InChIKey") or "").strip()
                if key:
                    products.append({
                        "inchikey": key,
                        "reactionId": (row.get("Reaction ID") or "").strip(),
                        "precursorInchikey": (row.get("Precursor InChIKey") or "").strip(),
                    })
    products.sort(key=lambda d: (d["inchikey"], d["reactionId"], d["precursorInchikey"]))
    return {
        "exitCode": p.returncode,
        "seconds": took,
        "productCount": len(products),
        "products": products,
        "stderrTail": p.stderr.strip().splitlines()[-3:] if p.returncode else [],
    }


def main():
    OUT.mkdir(exist_ok=True)
    subs = [l.split("\t") for l in (HERE / "substrates.tsv").read_text().strip().splitlines()]
    jobs = [(n, s, c[0], c[2]) for n, s in subs for c in CONFIGS]
    print(f"{len(subs)} substrates x {len(CONFIGS)} configs = {len(jobs)} runs", flush=True)

    results, done, t0 = {}, 0, time.time()
    with cf.ThreadPoolExecutor(max_workers=8) as ex:
        futs = {ex.submit(run, *j): j for j in jobs}
        for f in cf.as_completed(futs):
            name, smiles, cfg_id, _ = futs[f]
            try:
                results.setdefault(name, {})[cfg_id] = f.result()
            except Exception as e:                      # noqa: BLE001 - recorded, not swallowed
                results.setdefault(name, {})[cfg_id] = {"error": repr(e)}
            done += 1
            if done % 25 == 0 or done == len(jobs):
                print(f"  {done}/{len(jobs)}  ({round(time.time()-t0)}s)", flush=True)

    baseline = {
        "reference": {
            "jar": JAR.name,
            "source": "https://bitbucket.org/wishartlab/biotransformer3.0jar (master, 6432cf887, 2023-05-25)",
            "cdkVersion": "2.7.1 (bundled in the reference jar)",
            "useDB": False,
            "note": "Recorded offline from the upstream CLI. Products are sorted; "
                    "an empty list means the run produced no metabolites.",
        },
        "configs": {c[0]: {"siriusTransformation": c[1], "cliArgs": c[2]} for c in CONFIGS},
        "substrates": {n: {"smiles": s} for n, s in subs},
        "results": results,
    }
    (HERE / "golden-baseline.json").write_text(json.dumps(baseline, indent=1, sort_keys=True) + "\n")

    fails = [(n, c) for n, cs in results.items() for c, r in cs.items()
             if r.get("error") or r.get("exitCode")]
    print(f"\nwrote golden-baseline.json  ({round(time.time()-t0)}s)")
    print(f"non-zero exits/errors: {len(fails)}")
    for n, c in fails[:20]:
        print("   ", n, c, results[n][c].get("stderrTail") or results[n][c].get("error"))
    return 0


if __name__ == "__main__":
    sys.exit(main())

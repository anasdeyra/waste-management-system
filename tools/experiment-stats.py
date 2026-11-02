#!/usr/bin/env python3
"""
Statistics of the seeded MN01 evaluation experiment (Part 6, docs/06-metrics.md).

Reads (EXPERIMENT_DIR is the output of ``tools/run-experiment.sh``, committed as
``docs/results/experiment/``):

* ``results.csv``    one row per run and method (columns of docs/evaluation-csv.md);
* ``run-info.txt``   campaign facts (commit, start and end time, arguments).

Writes into OUT_DIR (all deterministic: sorted keys, fixed formats, no timestamps of its own):

* ``summary.json``                  every aggregate, floats unrounded;
* ``per-instance.csv``              the 20 routing instances (seed, point count) side by side;
* ``per-scenario.csv``              the 60 scenarios, repetition 1;
* ``checks.txt``                    PASS/FAIL/INFO validity checks (exit status 1 if one fails);
* ``summary.md``                    the Markdown tables copied by docs/06-metrics.md and the deck;
* ``distance-change-by-size.svg``   grouped bar chart of the mean distance change.

Definitions
-----------
* Scenario = ``scenario_key`` (profile, seed, size). Instance = (seed, point count). The demand
  profile only changes the demands, so when the capacity never binds the three profiles of an
  instance produce identical routes (``INFO profile_invariant``); paired tests then use the
  instances (n = 20), not the scenarios (n = 60), to avoid pseudo-replication.
* Distances, durations and orders come from repetition 1 (``checks.txt`` proves that all
  repetitions are identical); execution times use every repetition.
* Distance change = stored ``distance_change_pct`` = (distance - input order distance) / input
  order distance x 100. NN + 2-opt versus NN = (opt - nn) / nn x 100. Share of the gain from
  2-opt = (d_nn - d_opt) / (d_input - d_opt) x 100.
* Paired comparison (A baseline, B challenger): d = A - B (positive = B shorter). ``improved`` is
  d > 0.0005, ``worse`` d < -0.0005, ``equal`` the rest (0.0005 m or s is half the CSV precision).
  Exact two-sided Wilcoxon signed-rank test: zero differences are dropped, |d| is ranked with
  average ranks for ties, the null distribution is the 2^n' equally likely sign assignments
  (dynamic programming over doubled integer ranks), p = min(1, 2 x min(P(W+ <= w), P(W+ >= w))).

Usage:
    python3 tools/experiment-stats.py EXPERIMENT_DIR OUT_DIR
    python3 tools/experiment-stats.py --self-test
"""

from __future__ import annotations

import argparse
import csv
import json
import statistics
import sys
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path

METHODS = ["INPUT_ORDER", "NEAREST_NEIGHBOR", "NN_PLUS_2_OPT"]
METHOD_LABEL = {"INPUT_ORDER": "Input order", "NEAREST_NEIGHBOR": "NN", "NN_PLUS_2_OPT": "NN + 2-opt"}
PROFILES = ["UNIFORM_LOW", "UNIFORM_HIGH", "HOTSPOT"]
SIZES = [10, 25, 50, 100]
TOLERANCE = 0.0005


# --------------------------------------------------------------------------------------------
# Statistics helpers
# --------------------------------------------------------------------------------------------

def describe(values: list[float]) -> dict:
    return {
        "mean": statistics.mean(values),
        "median": statistics.median(values),
        "min": min(values),
        "max": max(values),
    }


def wilcoxon(diffs: list[float], tolerance: float = TOLERANCE) -> dict:
    """Exact two-sided Wilcoxon signed-rank test of paired differences ``diffs`` (A - B)."""
    n = len(diffs)
    improved = sum(1 for d in diffs if d > tolerance)
    worse = sum(1 for d in diffs if d < -tolerance)
    nonzero = [d for d in diffs if abs(d) > tolerance]
    result = {
        "n": n,
        "improved": improved,
        "equal": n - improved - worse,
        "worse": worse,
        "n_nonzero": len(nonzero),
    }
    if not nonzero:
        result.update({"w_plus": 0.0, "w_minus": 0.0, "p_exact_two_sided": 1.0})
        return result

    order = sorted(range(len(nonzero)), key=lambda i: abs(nonzero[i]))
    ranks = [0.0] * len(nonzero)
    start = 0
    while start < len(order):
        end = start
        while (
            end + 1 < len(order)
            and abs(nonzero[order[end + 1]]) - abs(nonzero[order[end]]) <= tolerance
        ):
            end += 1
        average = (start + end) / 2 + 1  # average of the 1-based ranks start+1 .. end+1
        for k in range(start, end + 1):
            ranks[order[k]] = average
        start = end + 1

    w_plus = sum(r for r, d in zip(ranks, nonzero) if d > 0)
    w_minus = sum(r for r, d in zip(ranks, nonzero) if d < 0)

    doubled = [int(round(2 * r)) for r in ranks]
    total = sum(doubled)
    counts = [0] * (total + 1)
    counts[0] = 1
    for value in doubled:
        for s in range(total, value - 1, -1):
            counts[s] += counts[s - value]
    all_assignments = 2 ** len(doubled)
    observed = int(round(2 * w_plus))
    p_low = sum(counts[: observed + 1]) / all_assignments
    p_high = sum(counts[observed:]) / all_assignments
    result.update(
        {"w_plus": w_plus, "w_minus": w_minus, "p_exact_two_sided": min(1.0, 2 * min(p_low, p_high))}
    )
    return result


def self_test() -> int:
    r = wilcoxon([1, 2, 3, 4, 5])
    assert r["w_plus"] == 15 and r["p_exact_two_sided"] == 0.0625, r
    r = wilcoxon([1, -2, 3, 4, 5, 6, 7, 8, 9, 10])
    assert r["w_plus"] == 53 and r["w_minus"] == 2 and r["p_exact_two_sided"] == 0.005859375, r
    assert wilcoxon([0, 0])["p_exact_two_sided"] == 1
    r = wilcoxon([1, 1, -1, 2])
    assert r["w_plus"] == 8 and r["w_minus"] == 2, r
    print("self-test ok")
    return 0


# --------------------------------------------------------------------------------------------
# Input
# --------------------------------------------------------------------------------------------

NUMERIC = [
    "seed", "repetition", "point_count", "distance_m", "travel_time_s", "service_time_total_s",
    "duration_s", "execution_time_ms", "input_order_distance_m", "input_order_duration_s",
    "distance_change_pct", "duration_change_pct", "total_demand_kg", "vehicle_capacity_kg",
]


def read_results(path: Path) -> list[dict]:
    rows = []
    with path.open(newline="", encoding="utf-8") as handle:
        for row in csv.DictReader(handle):
            for column in NUMERIC:
                row[column] = float(row[column])
            row["seed"] = int(row["seed"])
            row["repetition"] = int(row["repetition"])
            row["point_count"] = int(row["point_count"])
            rows.append(row)
    return rows


def read_run_info(path: Path) -> dict:
    info = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if ": " in line:
            key, value = line.split(": ", 1)
            info[key.strip()] = value.strip()
    return info


def parse_utc(text: str) -> datetime:
    return datetime.strptime(text, "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=timezone.utc)


# --------------------------------------------------------------------------------------------
# Analysis
# --------------------------------------------------------------------------------------------

def analyse(rows: list[dict], info: dict) -> tuple[dict, list[dict], list[dict], list[str], bool]:
    checks: list[str] = []
    failed = False

    def check(name: str, ok: bool, detail: str = "") -> bool:
        nonlocal failed
        checks.append(f"{'PASS' if ok else 'FAIL'} {name}" + (f": {detail}" if detail else ""))
        failed = failed or not ok
        return ok

    keys = sorted({r["scenario_key"] for r in rows})
    repetitions = sorted({r["repetition"] for r in rows})
    seeds = sorted({r["seed"] for r in rows})
    sizes = sorted({r["point_count"] for r in rows})
    profiles = sorted({r["demand_profile"] for r in rows})
    instances = sorted({(r["seed"], r["point_count"]) for r in rows}, key=lambda t: (t[1], t[0]))
    runs = {(r["scenario_key"], r["repetition"]) for r in rows}

    check("row count = scenarios x repetitions x 3 methods",
          len(rows) == len(keys) * len(repetitions) * 3,
          f"{len(rows)} rows = {len(keys)} x {len(repetitions)} x 3")
    check("scenario grid = 3 profiles x 5 seeds x 4 sizes",
          profiles == sorted(PROFILES) and len(seeds) == 5 and sizes == SIZES and len(keys) == 60,
          f"{len(profiles)} profiles, {len(seeds)} seeds, sizes {sizes}, {len(keys)} scenarios")
    keyed = {(r["scenario_key"], r["repetition"], r["method"]) for r in rows}
    check("one row per scenario, repetition and method", len(keyed) == len(rows))
    check("every method present in every run",
          all(sum(1 for m in METHODS if (k, rep, m) in keyed) == 3 for k, rep in runs))
    check("every run_status is COMPLETED", all(r["run_status"] == "COMPLETED" for r in rows))
    check("every run is feasible, covers every stop and respects the capacity",
          all(r["feasible"] == "true" and r["coverage_ok"] == "true" and r["capacity_ok"] == "true"
              for r in rows))
    versions = sorted({r["road_graph_version"] for r in rows})
    check("one road graph version", len(versions) == 1, ", ".join(versions))
    check("stored distance_change_pct recalculates from distance_m",
          all(abs((r["distance_m"] - r["input_order_distance_m"]) / r["input_order_distance_m"] * 100
                  - r["distance_change_pct"]) <= 0.001 for r in rows))
    check("stored duration_change_pct recalculates from duration_s",
          all(abs((r["duration_s"] - r["input_order_duration_s"]) / r["input_order_duration_s"] * 100
                  - r["duration_change_pct"]) <= 0.001 for r in rows))
    check("duration_s = travel_time_s + service_time_total_s",
          all(abs(r["duration_s"] - r["travel_time_s"] - r["service_time_total_s"]) <= 0.002 for r in rows))
    check("total demand <= vehicle capacity in every row",
          all(r["total_demand_kg"] <= r["vehicle_capacity_kg"] for r in rows))

    by_run: dict[tuple, dict] = {(r["scenario_key"], r["repetition"], r["method"]): r for r in rows}
    deterministic = all(
        by_run[(k, rep, m)]["distance_m"] == by_run[(k, repetitions[0], m)]["distance_m"]
        and by_run[(k, rep, m)]["duration_s"] == by_run[(k, repetitions[0], m)]["duration_s"]
        and by_run[(k, rep, m)]["visit_order"] == by_run[(k, repetitions[0], m)]["visit_order"]
        for k in keys for rep in repetitions for m in METHODS
        if (k, rep, m) in by_run and (k, repetitions[0], m) in by_run
    )
    check("deterministic across repetitions (distance, duration, visit order)", deterministic)

    first = {(r["scenario_key"], r["method"]): r for r in rows if r["repetition"] == repetitions[0]}
    scenario_info = {}
    for r in rows:
        scenario_info[r["scenario_key"]] = (r["demand_profile"], r["seed"], r["point_count"], r["total_demand_kg"])

    profile_invariant = True
    by_instance_profiles: dict[tuple, list[str]] = defaultdict(list)
    for key, (profile, seed, size, _) in scenario_info.items():
        by_instance_profiles[(seed, size)].append(key)
    for members in by_instance_profiles.values():
        for m in METHODS:
            reference = first[(members[0], m)]
            for other in members[1:]:
                row = first[(other, m)]
                if row["distance_m"] != reference["distance_m"] or row["visit_order"] != reference["visit_order"]:
                    profile_invariant = False
    checks.append(f"INFO profile_invariant: {'true' if profile_invariant else 'false'}")

    # Scenario rows (repetition 1), sorted by profile, seed, size.
    scenarios = []
    for key in sorted(keys, key=lambda k: (scenario_info[k][0], scenario_info[k][1], scenario_info[k][2])):
        profile, seed, size, demand = scenario_info[key]
        row = {"scenario_key": key, "demand_profile": profile, "seed": seed, "point_count": size,
               "total_demand_kg": demand}
        for m in METHODS:
            source = first[(key, m)]
            row[m] = {
                "distance_m": source["distance_m"], "duration_s": source["duration_s"],
                "distance_change_pct": source["distance_change_pct"],
                "duration_change_pct": source["duration_change_pct"],
            }
        scenarios.append(row)

    # Instances: repetition 1, profile UNIFORM_LOW when profile invariant, otherwise all scenarios.
    if profile_invariant:
        basis = [s for s in scenarios if s["demand_profile"] == "UNIFORM_LOW"]
        checks.append("INFO paired basis: 20 instances (profile UNIFORM_LOW, repetition 1)"
                      if len(basis) == 20 else f"INFO paired basis: {len(basis)} instances")
    else:
        basis = scenarios
        checks.append(f"INFO paired basis: all {len(scenarios)} scenarios (profiles differ)")
    basis = sorted(basis, key=lambda s: (s["point_count"], s["seed"], s["demand_profile"]))

    inst = []
    for s in basis:
        d_in = s["INPUT_ORDER"]["distance_m"]
        d_nn = s["NEAREST_NEIGHBOR"]["distance_m"]
        d_opt = s["NN_PLUS_2_OPT"]["distance_m"]
        denominator = d_in - d_opt
        inst.append({
            "seed": s["seed"], "point_count": s["point_count"], "profile": s["demand_profile"],
            "input": s["INPUT_ORDER"], "nn": s["NEAREST_NEIGHBOR"], "opt": s["NN_PLUS_2_OPT"],
            "opt_vs_nn_distance_change_pct": (d_opt - d_nn) / d_nn * 100,
            "opt_vs_nn_duration_change_pct": (s["NN_PLUS_2_OPT"]["duration_s"] - s["NEAREST_NEIGHBOR"]["duration_s"])
            / s["NEAREST_NEIGHBOR"]["duration_s"] * 100,
            "share": (d_nn - d_opt) / denominator * 100 if abs(denominator) > TOLERANCE else None,
            "nn_improved": d_nn - d_opt > TOLERANCE,
        })

    # Descriptive groups over the 60 scenarios.
    def group(selected_keys: set[str]) -> dict:
        out = {}
        for m in METHODS:
            sel = [s[m] for s in scenarios if s["scenario_key"] in selected_keys]
            times = [r["execution_time_ms"] for r in rows
                     if r["scenario_key"] in selected_keys and r["method"] == m]
            later = [r["execution_time_ms"] for r in rows
                     if r["scenario_key"] in selected_keys and r["method"] == m
                     and r["repetition"] != repetitions[0]]
            out[m] = {
                "distance_change_pct": describe([x["distance_change_pct"] for x in sel]),
                "duration_change_pct": describe([x["duration_change_pct"] for x in sel]),
                "mean_distance_m": statistics.mean([x["distance_m"] for x in sel]),
                "mean_duration_s": statistics.mean([x["duration_s"] for x in sel]),
                "execution_time_ms": {
                    "median": statistics.median(times),
                    "max": max(times),
                    "median_excl_rep1": statistics.median(later) if later else None,
                },
            }
        return out

    by_size = {str(size): group({s["scenario_key"] for s in scenarios if s["point_count"] == size})
               for size in SIZES}
    by_size["all"] = group({s["scenario_key"] for s in scenarios})
    by_profile = {p: group({s["scenario_key"] for s in scenarios if s["demand_profile"] == p})
                  for p in PROFILES}
    by_profile["all"] = by_size["all"]

    def pair(name_a: str, name_b: str, indicator: str, key_a: str, key_b: str) -> dict:
        field = "distance_m" if indicator == "distance" else "duration_s"
        return wilcoxon([i[key_a][field] - i[key_b][field] for i in inst])

    paired = {
        "opt_vs_input_distance": pair("input", "opt", "distance", "input", "opt"),
        "nn_vs_input_distance": pair("input", "nn", "distance", "input", "nn"),
        "opt_vs_nn_distance": pair("nn", "opt", "distance", "nn", "opt"),
        "opt_vs_input_duration": pair("input", "opt", "duration", "input", "opt"),
        "opt_vs_nn_duration": pair("nn", "opt", "duration", "nn", "opt"),
    }

    def per_size_stats(selector, improved_key=None) -> dict:
        out = {}
        for label in [str(s) for s in SIZES] + ["all"]:
            members = [i for i in inst if label == "all" or i["point_count"] == int(label)]
            values = [selector(i) for i in members if selector(i) is not None]
            entry = describe(values)
            if improved_key:
                entry["improved"] = sum(1 for i in members if i[improved_key])
                entry["instances"] = len(members)
            out[label] = entry
        return out

    campaign = {k: info.get(k) for k in ("commit", "tracked_changes", "exit", "cpus", "java", "os", "args")}
    campaign["started_utc"] = info.get("started_utc")
    campaign["finished_utc"] = info.get("finished_utc")
    if info.get("started_utc") and info.get("finished_utc"):
        campaign["wall_time_s"] = int((parse_utc(info["finished_utc"]) - parse_utc(info["started_utc"])).total_seconds())
    else:
        campaign["wall_time_s"] = None

    summary = {
        "rows": len(rows),
        "runs": len(runs),
        "scenarios": len(keys),
        "instances": len(instances),
        "repetitions": len(repetitions),
        "all_completed": all(r["run_status"] == "COMPLETED" for r in rows),
        "all_feasible": all(r["feasible"] == "true" for r in rows),
        "deterministic_across_repetitions": deterministic,
        "profile_invariant": profile_invariant,
        "road_graph_version": versions[0] if len(versions) == 1 else versions,
        "campaign": campaign,
        "by_size": by_size,
        "by_profile": by_profile,
        "paired": paired,
        "opt_vs_nn_distance_change_pct": per_size_stats(lambda i: i["opt_vs_nn_distance_change_pct"], "nn_improved"),
        "share_of_gain_from_2opt_pct": per_size_stats(lambda i: i["share"]),
    }
    return summary, scenarios, inst, checks, failed


# --------------------------------------------------------------------------------------------
# Output
# --------------------------------------------------------------------------------------------

def write_per_instance(path: Path, inst: list[dict]) -> None:
    header = ["seed", "point_count", "input_order_distance_m", "nearest_neighbor_distance_m",
              "nn_plus_2_opt_distance_m", "input_order_duration_s", "nearest_neighbor_duration_s",
              "nn_plus_2_opt_duration_s", "nn_distance_change_pct", "opt_distance_change_pct",
              "opt_vs_nn_distance_change_pct", "nn_duration_change_pct", "opt_duration_change_pct",
              "opt_vs_nn_duration_change_pct", "share_of_gain_from_2opt_pct"]
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle, lineterminator="\n")
        writer.writerow(header)
        for i in inst:
            writer.writerow([
                i["seed"], i["point_count"],
                f"{i['input']['distance_m']:.3f}", f"{i['nn']['distance_m']:.3f}", f"{i['opt']['distance_m']:.3f}",
                f"{i['input']['duration_s']:.3f}", f"{i['nn']['duration_s']:.3f}", f"{i['opt']['duration_s']:.3f}",
                f"{i['nn']['distance_change_pct']:.4f}", f"{i['opt']['distance_change_pct']:.4f}",
                f"{i['opt_vs_nn_distance_change_pct']:.4f}",
                f"{i['nn']['duration_change_pct']:.4f}", f"{i['opt']['duration_change_pct']:.4f}",
                f"{i['opt_vs_nn_duration_change_pct']:.4f}",
                "" if i["share"] is None else f"{i['share']:.4f}",
            ])


def write_per_scenario(path: Path, scenarios: list[dict]) -> None:
    header = ["scenario_key", "demand_profile", "seed", "point_count", "total_demand_kg"]
    names = {"INPUT_ORDER": "input_order", "NEAREST_NEIGHBOR": "nearest_neighbor", "NN_PLUS_2_OPT": "nn_plus_2_opt"}
    for m in METHODS:
        header += [f"{names[m]}_distance_m", f"{names[m]}_duration_s",
                   f"{names[m]}_distance_change_pct", f"{names[m]}_duration_change_pct"]
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle, lineterminator="\n")
        writer.writerow(header)
        for s in scenarios:
            line = [s["scenario_key"], s["demand_profile"], s["seed"], s["point_count"], f"{s['total_demand_kg']:.2f}"]
            for m in METHODS:
                v = s[m]
                line += [f"{v['distance_m']:.3f}", f"{v['duration_s']:.3f}",
                         f"{v['distance_change_pct']:.4f}", f"{v['duration_change_pct']:.4f}"]
            writer.writerow(line)


def table(headers: list[str], body: list[list[str]]) -> list[str]:
    lines = ["| " + " | ".join(headers) + " |", "|" + "|".join("---" for _ in headers) + "|"]
    lines += ["| " + " | ".join(row) + " |" for row in body]
    return lines


def write_markdown(path: Path, summary: dict, source: str) -> None:
    c = summary["campaign"]
    bs = summary["by_size"]
    labels = [str(s) for s in SIZES] + ["all"]
    out: list[str] = ["# Experiment statistics", "",
                      f"Generated by `python3 tools/experiment-stats.py {source} <out>`; "
                      "indicators come from repetition 1 unless stated.", ""]

    out += ["**(a) Campaign facts.** Source: `run-info.txt`, `results.csv`.", ""]
    out += table(["Fact", "Value"], [
        ["Rows (runs x 3 methods)", str(summary["rows"])],
        ["Runs (scenarios x repetitions)", str(summary["runs"])],
        ["Scenarios", str(summary["scenarios"])],
        ["Distinct routing instances", str(summary["instances"])],
        ["Repetitions", str(summary["repetitions"])],
        ["Campaign wall time (s)", str(c["wall_time_s"])],
        ["Commit", f"`{c['commit']}`"],
    ]) + [""]

    out += ["**(b) Distance change versus input order (%) by size.** Source: `results.csv`, "
            "repetition 1, 60 scenarios; negative = shorter.", ""]
    body = []
    for label in labels:
        for m in ("NEAREST_NEIGHBOR", "NN_PLUS_2_OPT"):
            d = bs[label][m]["distance_change_pct"]
            body.append([label if label != "all" else "all sizes", METHOD_LABEL[m],
                         f"{d['mean']:.2f}", f"{d['median']:.2f}", f"{d['min']:.2f}", f"{d['max']:.2f}"])
    out += table(["Points", "Method", "Mean", "Median", "Min", "Max"], body) + [""]

    out += ["**(c) Duration change versus input order (%) by size.** Source: `results.csv`, "
            "repetition 1, 60 scenarios; duration = travel time + 600 s per stop.", ""]
    body = []
    for label in labels:
        for m in ("NEAREST_NEIGHBOR", "NN_PLUS_2_OPT"):
            d = bs[label][m]["duration_change_pct"]
            body.append([label if label != "all" else "all sizes", METHOD_LABEL[m],
                         f"{d['mean']:.2f}", f"{d['median']:.2f}", f"{d['min']:.2f}", f"{d['max']:.2f}"])
    out += table(["Points", "Method", "Mean", "Median", "Min", "Max"], body) + [""]

    out += ["**(d) Mean route length (km) and duration (h) by size and method.** Source: "
            "`results.csv`, repetition 1, 60 scenarios.", ""]
    body = []
    for label in labels:
        for m in METHODS:
            g = bs[label][m]
            body.append([label if label != "all" else "all sizes", METHOD_LABEL[m],
                         f"{g['mean_distance_m'] / 1000:.2f}", f"{g['mean_duration_s'] / 3600:.2f}"])
    out += table(["Points", "Method", "Mean length (km)", "Mean duration (h)"], body) + [""]

    out += ["**(e) Mean distance change versus input order (%) by demand profile.** Source: "
            "`results.csv`, repetition 1, 20 scenarios per profile.", ""]
    body = []
    for p in PROFILES + ["all"]:
        g = summary["by_profile"][p]
        body.append([p if p != "all" else "all profiles",
                     f"{g['NEAREST_NEIGHBOR']['distance_change_pct']['mean']:.2f}",
                     f"{g['NN_PLUS_2_OPT']['distance_change_pct']['mean']:.2f}"])
    out += table(["Profile", "NN", "NN + 2-opt"], body) + [""]

    out += ["**(f) NN + 2-opt versus NN by size.** Source: `per-instance.csv` (one row per routing "
            "instance, 5 per size, 20 in total); change = (NN + 2-opt - NN) / NN, share = part of the "
            "total gain over input order that 2-opt contributes.", ""]
    body = []
    for label in labels:
        d = summary["opt_vs_nn_distance_change_pct"][label]
        s = summary["share_of_gain_from_2opt_pct"][label]
        body.append([label if label != "all" else "all sizes", f"{d['mean']:.2f}", f"{d['median']:.2f}",
                     f"{d['min']:.2f}", f"{d['max']:.2f}", f"{d['improved']}/{d['instances']}", f"{s['mean']:.2f}"])
    out += table(["Points", "Mean (%)", "Median (%)", "Min (%)", "Max (%)", "Instances improved",
                  "Mean share of gain from 2-opt (%)"], body) + [""]

    out += ["**(g) Paired comparisons.** Source: `per-instance.csv`, 20 instances; d = baseline - "
            "challenger (positive = challenger shorter); exact two-sided Wilcoxon signed-rank test, "
            "n' = instances with a non-zero difference.", ""]
    names = [
        ("opt_vs_input_distance", "Distance: NN + 2-opt vs input order"),
        ("nn_vs_input_distance", "Distance: NN vs input order"),
        ("opt_vs_nn_distance", "Distance: NN + 2-opt vs NN"),
        ("opt_vs_input_duration", "Duration: NN + 2-opt vs input order"),
        ("opt_vs_nn_duration", "Duration: NN + 2-opt vs NN"),
    ]
    body = []
    for key, label in names:
        t = summary["paired"][key]
        body.append([label, str(t["n"]), str(t["improved"]), str(t["equal"]), str(t["worse"]),
                     str(t["n_nonzero"]), f"{t['w_plus']:.1f}", f"{t['w_minus']:.1f}",
                     f"{t['p_exact_two_sided']:.3g}"])
    out += table(["Pair", "n", "Improved", "Equal", "Worse", "n'", "W+", "W-", "p"], body) + [""]

    out += ["**(h) Execution time (ms) by size and method.** Source: `results.csv`, all "
            "repetitions (45 samples per size and method, 180 for all sizes); the timer covers "
            "sequencing only, not the road matrix.", ""]
    body = []
    for label in labels:
        for m in METHODS:
            t = bs[label][m]["execution_time_ms"]
            body.append([label if label != "all" else "all sizes", METHOD_LABEL[m], f"{t['median']:.3f}",
                         f"{t['max']:.3f}", f"{t['median_excl_rep1']:.3f}"])
    out += table(["Points", "Method", "Median", "Max", "Median excluding repetition 1"], body) + [""]
    path.write_text("\n".join(out), encoding="utf-8")


def write_chart(path: Path, summary: dict) -> None:
    colours = {"NEAREST_NEIGHBOR": "#2563eb", "NN_PLUS_2_OPT": "#16a34a"}
    left, top, scale = 80, 90, 2.4  # px per percent; zero line at y = top
    group_w, bar_w, gap = 150, 54, 8
    parts = [
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 720 400" width="720" height="400" '
        'font-family="system-ui, sans-serif">',
        '<rect width="720" height="400" fill="#ffffff"/>',
        '<text x="360" y="26" text-anchor="middle" font-size="18" font-weight="600" fill="#111827">'
        'Mean distance change vs input order (60 scenarios)</text>',
    ]
    for index, m in enumerate(("NEAREST_NEIGHBOR", "NN_PLUS_2_OPT")):
        x = 250 + index * 120
        parts.append(f'<rect x="{x}" y="42" width="14" height="14" fill="{colours[m]}"/>')
        parts.append(f'<text x="{x + 20}" y="54" font-size="14" fill="#111827">{METHOD_LABEL[m]}</text>')
    for tick in range(0, 101, 20):
        y = top + tick * scale
        parts.append(f'<line x1="{left}" y1="{y:.1f}" x2="{left + 4 * group_w}" y2="{y:.1f}" '
                     f'stroke="{"#111827" if tick == 0 else "#d1d5db"}" stroke-width="{1.5 if tick == 0 else 1}"/>')
        parts.append(f'<text x="{left - 8}" y="{y + 4:.1f}" text-anchor="end" font-size="12" fill="#374151">'
                     f'{-tick}</text>')
    parts.append(f'<text transform="translate(18 {top + 120}) rotate(-90)" text-anchor="middle" '
                 f'font-size="12" fill="#374151">Distance change (%)</text>')
    for g, size in enumerate(SIZES):
        x0 = left + g * group_w + (group_w - 2 * bar_w - gap) / 2
        for b, m in enumerate(("NEAREST_NEIGHBOR", "NN_PLUS_2_OPT")):
            value = summary["by_size"][str(size)][m]["distance_change_pct"]["mean"]
            height = -value * scale
            x = x0 + b * (bar_w + gap)
            parts.append(f'<rect x="{x:.1f}" y="{top}" width="{bar_w}" height="{height:.1f}" fill="{colours[m]}"/>')
            parts.append(f'<text x="{x + bar_w / 2:.1f}" y="{top + height + 15:.1f}" text-anchor="middle" '
                         f'font-size="13" font-weight="600" fill="#111827">{value:.1f}</text>')
        parts.append(f'<text x="{left + g * group_w + group_w / 2:.1f}" y="{top - 8}" text-anchor="middle" '
                     f'font-size="14" fill="#111827">{size} points</text>')
    parts.append('<text x="360" y="385" text-anchor="middle" font-size="12" fill="#4b5563">'
                 'Source: docs/results/experiment-stats/summary.json</text>')
    parts.append("</svg>")
    path.write_text("\n".join(parts) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("experiment_dir", nargs="?", help="directory with results.csv and run-info.txt")
    parser.add_argument("out_dir", nargs="?", help="output directory (created)")
    parser.add_argument("--self-test", action="store_true", help="check the Wilcoxon implementation and exit")
    args = parser.parse_args()
    if args.self_test:
        return self_test()
    if not args.experiment_dir or not args.out_dir:
        parser.error("EXPERIMENT_DIR and OUT_DIR are required")

    source = Path(args.experiment_dir)
    out = Path(args.out_dir)
    out.mkdir(parents=True, exist_ok=True)
    rows = read_results(source / "results.csv")
    info = read_run_info(source / "run-info.txt")
    summary, scenarios, inst, checks, failed = analyse(rows, info)

    (out / "summary.json").write_text(json.dumps(summary, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    write_per_instance(out / "per-instance.csv", inst)
    write_per_scenario(out / "per-scenario.csv", scenarios)
    (out / "checks.txt").write_text("\n".join(checks) + "\n", encoding="utf-8")
    write_markdown(out / "summary.md", summary, "docs/results/experiment")
    write_chart(out / "distance-change-by-size.svg", summary)
    print("\n".join(checks))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())

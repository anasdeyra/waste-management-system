# AGENTS.md

Context file for AI agents working in this workspace. **Read this file first**, then
`project-assignment.md`.

---

## 1. Mission

`project-ase` is the workspace for completing the ASE (Advanced Software Engineering)
course project described in [`project-assignment.md`](./project-assignment.md).

The assignment is not just code. It is a full software-engineering deliverable:

1. problem analysis,
2. literature review,
3. UML modeling,
4. a working prototype,
5. Scrum artifacts,
6. software metrics + a research question,
7. a final presentation.

An agent's job is to help produce **all** of these, coherently, not only the code.

## 2. Source of truth (order of precedence)

1. Direct instructions from the user in the current conversation.
2. This file (`AGENTS.md`) — workspace conventions and current status.
3. [`project-assignment.md`](./project-assignment.md)

## 3. Current status

| Item | State |
|---|---|
| Assignment transcribed | ✅ `project-assignment.md` |
| Subject / domain chosen | ✅ Map-centric intelligent waste management; New York City / Lower Manhattan MN01 case study with real DSNY coordinates |
| Tech stack chosen | ✅ Java 21 + Spring Boot 3 (REST API) + React 19 + Vite + TypeScript + Tailwind v4 + shadcn/ui + H2 (file) + Maven; Leaflet/React Leaflet installed (S2); GraphHopper 11.1 embedded behind `routing/` (S5) |
| Project mode | ✅ Individual |
| Prototype scope | ✅ Extended; multiple single-vehicle tours per day |
| Optimization model | ✅ Directed road-network methods: input order (worst case), the sweep baseline of Gillett & Miller (S11, D-052), NN and directed-cost 2-opt; 60 real-location scenarios fixed; NYC traffic overlay only; implemented in S5/S11 (`optimization/`, `routing/`); seeded experiment generator and runner (S9), campaign re-executed with four methods in S11 (`docs/results/`) |
| Repository scaffold | ✅ `backend/`, `frontend/`, `docs/`, `tools/`, `README.md` |
| Toolchain | ✅ JDK 21 + Maven 3.9 + CKJM installed locally by `tools/setup-toolchain.sh` |
| Documentation (Parts 1, 2, 5, 6) | ✅ Parts 1, 2, 5 and 6 complete; Part 5 sprints S1–S11 executed with reviews and retrospectives; FR-to-test matrix in `docs/traceability.md` |
| UML diagrams (Part 3) | ✅ Use case, operational class, optimization sequence, tour-lifecycle activity, and OCL complete; all Mermaid sources re-rendered successfully in S10 and S11 and embedded in the deck |
| Prototype (Part 4) | ✅ S1–S9 and S11 implemented (424 backend tests green): auth/roles/users/categories, collection points + demand on a Leaflet map, vehicles/agents with availability and account links, idempotent MN01 import (449 baskets, 238 DSNY-eligible, MN01G depot, snapshot identity), tour drafts with map/list stop selection, explicit input order, vehicle/crew assignment and capacity validation, directed road routing on the versioned OSM snapshot and the retained four-method evaluation API (sweep baseline, S11), route comparison, proposal acceptance and the planning gate (S6: accept or keep input order, confirm to PLANNED, lock); crew execution (assigned-tour list, start with revalidation, stop outcomes, completion) and the idempotent MN01 demonstration (S7), enriched to 5 vehicles, 7 agents and six tours covering every lifecycle state, with an admin reset that keeps accounts and the MN01 import (D-055); cancellation, retained outcome corrections, operational history and the audit log (S8); dashboards, the evaluation explorer, evaluation CSV import/export, the seeded experiment runner and the display-only NYC DOT traffic overlay (S9) |
| Metrics + research answer (Part 6) | ✅ `docs/06-metrics.md`; measured data under `docs/results/` (`bash tools/metrics.sh --publish`, 60-scenario campaign with 3 repetitions and four methods (720 rows), answer NN + 2-opt vs sweep (S11), `tools/experiment-stats.py`) |
| Presentation | ✅ Self-contained HTML deck `presentation/index.html`, redesigned 2026-11-05 (D-058): Problem → Product → Proof, 15 main slides for a 15-minute slot with a live demo, 13 appendix slides, Apple-style light design with bundled Inter, live image zoom, presenter view (S) with speaker notes, repository QR; 12 screenshots recaptured at 2× with the current interface; presented on 2026-11-10 (D-059) |

**Last updated:** 2026-11-05 (presentation redesigned, D-058; date fixed, D-059)

Update this table whenever the state changes. Do not claim something is done unless the
artifact exists in the repo.

## 4. Open decisions (ask the user before assuming)

These are unresolved and **must not be invented** by an agent:

None. The presentation date is fixed (Tuesday 10 November 2026, D-059).

All implementation-blocking decisions are resolved; see `docs/decisions.md`. Do not reopen
them without the project owner's agreement.

When a decision is made, record it in `docs/decisions.md` with date, decision, and
rationale, then remove it from this list.

## 5. Deliverables checklist

Map every artifact to a part of the assignment. Keep this list honest.

### Part 1 — Problem analysis → `docs/01-analysis.md`
- [x] Context
- [x] Users / stakeholders
- [x] Functional requirements
- [x] Non-functional requirements
- [x] Problem statement

### Part 2 — Literature review → `docs/02-literature-review.md`
- [x] Research question tied to the domain
- [x] 5–10 credible scientific sources (papers, standards, books) — 10 sources, Gillett & Miller (1974) replacing the former source 9 in S11
- [x] Synthesis: what exists, what is missing, what this project borrows
- [x] Citations with links/DOIs

### Part 3 — UML → `docs/03-uml/`
- [x] Use case diagram (+ short descriptions)
- [x] Class diagram
- [x] At least one sequence diagram (core scenario)
- [x] At least one activity diagram
- [x] OCL constraints where relevant

### Part 4 — Prototype → `backend/` + `frontend/`
> Agreed scope: **Extended** (see `docs/decisions.md` D-007). S1 delivered authentication, roles, users and waste categories; S2 added collection points and demand on a Leaflet map.
- [x] Frameworks initialised and runnable locally (backend on 8080, frontend on 5173, Vite proxies `/api`)
- [x] README with setup + run instructions
- [x] Authentication
- [x] Collection points, vehicles, agents, tours, waste categories (CRUD) (waste categories + users done in S1; collection points + demand done in S2; vehicles and agents done in S3; tour drafts done in S4; tour lifecycle: confirm S6, execution S7, cancel S8)
- [x] Intelligent tour optimization on directed NYC road-network costs and impact measurement (engine, routing and retained runs S5; review/acceptance S6; 60-scenario campaign executed in S10, re-run with the sweep baseline in S11)
- [x] Interactive maps for point editing/selection, tour display/comparison, and execution (point display/editing done in S2; stop selection done in S4; route comparison and attached routes done in S6; execution map S7; verified by build/bundle/proxy checks, D-040)
- [x] Versioned OSM/GraphHopper routing and timestamped NYC DOT traffic overlay (routing S5; traffic snapshot overlay S9; verified by the S9 done-check)
- [x] Dashboards, CSV import/export, audit log (dashboards, CSV and traffic snapshot S9; audit S8)
- [x] Import the included real MN01 basket/depot snapshots and add demo operations so a reviewer can try them immediately (snapshot import S3; demo operations S7, `dev` start or `POST /api/admin/demo`; one command from a fresh clone: `tools/demo.sh`, D-056)

### Part 5 — Scrum → `docs/05-scrum.md`
- [x] Product Backlog as user stories ("As a …, I want …, so that …")
- [x] Prioritized, with acceptance criteria
- [x] Sprint Backlog(s) — eleven sprints (S1–S11) planned for AI-agent execution, `docs/05-scrum.md` §3; S1–S11 executed
- [x] Sprint review notes (S1–S11 recorded)
- [x] Retrospective notes (what went well / badly) (S1–S11 recorded)

### Part 6 — Metrology → `docs/06-metrics.md`
> Executed in S10 and re-run in S11 with the sweep baseline: `bash tools/metrics.sh --publish`, the campaign and `tools/experiment-stats.py`; results in `docs/results/`, report in `docs/06-metrics.md`.
- [x] Tooling named and reproducible
- [x] LOC
- [x] Cyclomatic complexity
- [x] Coupling
- [x] Cohesion
- [x] Number of classes / methods
- [x] Depth of inheritance
- [x] Code smells
- [x] Generate and execute the fixed 60 real-location MN01 scenarios, retaining directed-road results and snapshot identifiers (generator and runner ready in S9: `tools/run-experiment.sh`)
- [x] A research question **answered with the measured data**, not just stated

### Presentation → `presentation/`
- [x] Deck following the 6 parts
- [x] Architecture diagram
- [x] Demo script / screenshots
- [x] Metrics table + key finding
- [x] Limitations and future work

## 6. Repository layout

```
project-ase/
├── AGENTS.md                 # this file
├── project-assignment.md     # assignment (canonical)
├── README.md                 # project overview + how to run
├── backend/                  # Spring Boot 3 REST API (Java 21, Maven)
│   ├── mvnw, .mvn/          # Maven wrapper
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/ase/wastems/
│       │   ├── config/        # Spring configuration (security, service area, dev seeds)
│       │   ├── domain/ repository/ service/ web/ security/
│       │   ├── optimization/  # framework-free algorithm core (D-008, D-022)
│       │   ├── routing/       # embedded GraphHopper on the OSM snapshot
│       │   ├── traffic/       # display-only NYC DOT traffic overlay (isolated, D-021)
│       │   └── WasteMsApplication.java
│       ├── main/resources/   # application.yml + real NYC source snapshots in data/ + OSM road snapshot in routing/
│       └── test/             # JUnit 5 tests
├── frontend/                 # React 19 + Vite + TypeScript SPA
│   ├── package.json
│   ├── vite.config.ts        # @ alias, Tailwind plugin, /api proxy
│   ├── components.json       # shadcn/ui configuration
│   └── src/
│       ├── components/ui/    # shadcn/ui components (vendored, not own code)
│       ├── pages/ components/shared/ hooks/ types/
│       ├── components/maps/  # Leaflet base map and layers
│       ├── components/collection-points/  # list, details, form, demand editor
│       ├── components/fleet/  # vehicle form dialog
│       ├── components/tours/  # tour header, stop list, candidates, assignment, capacity
│       ├── components/agents/  # agent form and account-link dialogs
│       ├── lib/              # cn(), API client, auth helpers
│       ├── App.tsx           # routes
│       └── main.tsx          # providers: Query, Router
├── docs/
│   ├── decisions.md          # decision log
│   ├── geospatial-data.md    # NYC sources, snapshots, maps, routing, attribution
│   ├── evaluation-csv.md     # evaluation CSV import/export, seeded experiment, CLI
│   ├── 01-analysis.md
│   ├── 02-literature-review.md
│   ├── 03-uml/
│   ├── 05-scrum.md
│   ├── 06-metrics.md         # Part 6 report and research answer
│   ├── traceability.md       # FR -> user story -> use case -> code -> test
│   └── results/              # committed measured data: experiment/, experiment-stats/, metrics/
├── tools/
│   ├── setup-toolchain.sh    # installs JDK / Maven / CKJM into ~/.local/opt
│   ├── demo.sh               # portable one-command seeded demo for a fresh clone (D-056); --reset rebuilds it
│   ├── dev.sh                # backend + frontend side by side in one tmux session (start/stop/status)
│   ├── fetch-road-snapshot.sh # refreshes the OSM road snapshot (new routing version)
│   ├── fetch-traffic-snapshot.py # refreshes the NYC DOT traffic snapshot (new version)
│   ├── run-experiment.sh     # headless seeded experiment (S10 campaign)
│   ├── toolchain.env
│   ├── metrics.sh            # Part 6 entry point (`--publish` copies summaries to docs/results/metrics/)
│   ├── experiment-stats.py   # checks, statistics, Wilcoxon test and chart of the campaign
│   ├── capture-screenshots.mjs # optional: presentation screenshots (puppeteer outside the repo)
│   ├── count-loc.py          # language-aware LOC counter
│   ├── ckjm-summary.py, jdepend-summary.py, pmd-summary.py, ts-complexity-summary.py
│   ├── oxlint-complexity.json # TypeScript complexity configuration
│   └── pmd/                  # wastems-ruleset.xml (quality) and wastems-cc-all.xml (complexity)
└── presentation/             # index.html (self-contained deck), assets/ (screenshots, diagrams, qr.png, fonts/)
```

The top-level shape is stable: `backend/` and `frontend/` replace the single `src/` folder
because the prototype is two independently built applications (D-018). The domain packages
(`domain/`, `repository/`, `service/`, `web/`, `optimization/`, `routing/`, `traffic/`) are created as the
feature sprints are implemented. D-019–D-022 fix the NYC/MN01, map, and road-network direction.

## 7. Conventions

- **Docs:** Markdown, English unless the user says otherwise. One file per assignment part.
- **Diagrams:** Mermaid inside Markdown by default (renders on GitHub, no toolchain).
  Use PlantUML only if the user prefers it. Always keep the source next to the export.
- **Backend code:** Java 21 idioms. Entities in `domain/`, persistence in `repository/`,
  business rules in `service/`, transport in `web/`, and road-network integration in
  `routing/`. DTOs are Java `record`s. No Lombok: explicit accessors keep the Part 6 metrics
  honest. The intelligent module lives in its own package and must not depend on Spring, the
  persistence layer, or GraphHopper; it consumes a directed cost matrix through a small core
  interface.
- **Frontend code:** TypeScript strict mode. shadcn/ui components are vendored under
  `src/components/ui/` and are **not** counted as own code in Part 6. Pages live in
  `src/pages/`, shared widgets in `src/components/shared/`, server state through TanStack
  Query. Interactive map components live in `src/components/maps/`. The API contract is typed
  in `src/types/api.ts` and must stay in sync with the backend records.
- **Geospatial data:** Use WGS84 and explicit `longitude`/`latitude` fields; GeoJSON coordinate
  arrays are `[longitude, latitude]`. Preserve source IDs and distinguish real locations from
  synthetic demand/operations. Route geometry must follow the versioned road graph, and 2-opt
  must recalculate directed costs for reversed segments. Keep required NYC/OSM attribution.
- **Naming:** kebab-case for files, `PascalCase`/`camelCase` per language norms for code.
- **Traceability:** each functional requirement in Part 1 should be traceable to a use
  case, to code, and (ideally) to a test. Each metric in Part 6 must point to a command
  that reproduces it.
- **Commits:** imperative, scoped, e.g. `docs: add use case diagram`, `feat(auth): add login`.
- **Secrets:** never commit credentials, tokens, or `.env` files. Provide `.env.example`.

## 7.1 Toolchain

The development machine has no system package manager and no root access. The Java
toolchain is therefore installed per user under `~/.local/opt`:

| Tool | Location | Wrapper |
|---|---|---|
| Eclipse Temurin JDK 21 | `~/.local/opt/jdk-21` | `~/.local/opt/ase-env.sh` |
| Apache Maven 3.9 | `~/.local/opt/maven` | `~/.local/opt/ase-env.sh` |
| CKJM | `~/.local/opt/ckjm` | `~/.local/bin/ckjm` |
| Node.js 22 | provided by `fnm` | `fnm use` |

`tools/setup-toolchain.sh` performs the installation idempotently. Source
`~/.local/opt/ase-env.sh` (added to `~/.bashrc`) before running any Maven command.

`tools/metrics.sh` is the single entry point for Part 6: it produces LOC, CKJM, PMD and
JDepend reports under `metrics/`.

## 8. Agent workflow

**Start of task**
1. Read this file and `project-assignment.md`.
2. Check §3 status and §4 open decisions.
3. If the task depends on an open decision, ask — do not guess.
4. State which deliverable you are advancing.

**During work**
- Prefer extending existing artifacts over creating parallel ones.
- Keep the prototype runnable at all times; do not leave the repo in a broken state.
- If you change scope, update `docs/05-scrum.md` and §4/§5 here.

**End of task**
1. Verify what you produced actually works (run it, render it, open it).
2. Update §3 status, tick §5 boxes, and log decisions.
3. Report: what changed, what is still missing, what needs a user decision.

## 9. Definition of done

A deliverable is done when:

- the artifact exists in the repo at the documented path;
- it is internally consistent with the other parts (same actors, same class names,
  same feature scope);
- a reviewer can open/run it without asking the author anything;
- the corresponding box in §5 is ticked and §3 is updated.

## 10. Hard rules

- Never invent the domain, tech stack, team composition, or deadline.
- Never delete or overwrite user work without checking what it is first.
- Never present placeholder or fabricated results as measured data (especially in Part 6).
- Never claim a diagram, metric, or feature exists if it does not.
- Ask before adding heavy dependencies, paid services, or cloud infrastructure.

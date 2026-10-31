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
| Subject / domain chosen | ✅ Intelligent waste management; collection points, vehicles, agents, tours, and waste |
| Tech stack chosen | ✅ Java 21 + Spring Boot 3 (REST API) + React 19 + Vite + TypeScript + Tailwind v4 + shadcn/ui + H2 (file) + Maven |
| Project mode | ✅ Individual |
| Prototype scope | ✅ Extended; multiple single-vehicle tours per day |
| Optimization model | ✅ Input-order baseline, NN + 2-opt; 60-scenario matrix fixed |
| Repository scaffold | ✅ `backend/`, `frontend/`, `docs/`, `tools/`, `README.md` |
| Toolchain | ✅ JDK 21 + Maven 3.9 + CKJM installed locally by `tools/setup-toolchain.sh` |
| Documentation (Parts 1, 2, 5, 6) | 🟡 Part 1 complete; Part 6 has the tooling and a preliminary baseline; Parts 2 and 5 not started |
| UML diagrams (Part 3) | ❌ Not started |
| Prototype (Part 4) | 🟡 Scaffold running: domain model, auth, read APIs, optimizer, seed data, React shell + 7 pages. Write operations, tour execution and evaluation harness not started |
| Metrics + research answer (Part 6) | 🟡 Tooling and baseline done (`tools/metrics.sh`, `docs/06-metrics.md`); the 60-scenario matrix and the final answer are pending |
| Presentation | ❌ Not started |

**Last updated:** 2026-10-31

Update this table whenever the state changes. Do not claim something is done unless the
artifact exists in the repo.

## 4. Open decisions (ask the user before assuming)

These are unresolved and **must not be invented** by an agent:

- **Presentation date and format.** No fixed deadline; the presentation date and deck
  format (PowerPoint vs. HTML) are not chosen.

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
- [ ] Research question tied to the domain
- [ ] 5–10 credible scientific sources (papers, standards, books)
- [ ] Synthesis: what exists, what is missing, what this project borrows
- [ ] Citations with links/DOIs

### Part 3 — UML → `docs/03-uml/`
- [ ] Use case diagram (+ short descriptions)
- [ ] Class diagram
- [ ] At least one sequence diagram (core scenario)
- [ ] At least one activity diagram
- [ ] OCL constraints where relevant

### Part 4 — Prototype → `backend/` + `frontend/`
> Agreed scope: **Extended** (see `docs/decisions.md` D-007). Scaffold in place, features in progress.
- [x] Runnable locally (backend on 8080, frontend on 5173, Vite proxies `/api`)
- [ ] Covers the agreed scope
- [x] Authentication (opaque bearer tokens, BCrypt hashes, role model)
- [x] Seed/demo data so a reviewer can try it immediately
- [x] README with setup + run instructions
- [ ] Write operations (create / update / deactivate) for the five core areas
- [ ] Tour creation, agent execution updates, accepted tours
- [ ] Optimization evaluation harness that persists the 60-scenario matrix
- [ ] CSV import/export and audit log

### Part 5 — Scrum → `docs/05-scrum.md`
- [ ] Product Backlog as user stories ("As a …, I want …, so that …")
- [ ] Prioritized, with acceptance criteria
- [ ] Sprint Backlog(s)
- [ ] Sprint review notes
- [ ] Retrospective notes (what went well / badly)

### Part 6 — Metrology → `docs/06-metrics.md`
> Tooling is in place; the measurements are a preliminary baseline on the scaffold.
- [x] Tooling named and reproducible (`bash tools/metrics.sh`)
- [x] LOC (baseline measured)
- [x] Cyclomatic complexity (baseline measured)
- [x] Coupling (baseline measured)
- [x] Cohesion (baseline measured)
- [x] Number of classes / methods
- [x] Depth of inheritance
- [x] Code smells (0 violations in the baseline)
- [ ] Final measurement on the completed prototype
- [ ] The 60-scenario evaluation matrix executed
- [ ] A research question **answered with the measured data**, not just stated

### Presentation → `presentation/`
- [ ] Deck following the 6 parts
- [ ] Architecture diagram
- [ ] Demo script / screenshots
- [ ] Metrics table + key finding
- [ ] Limitations and future work

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
│       │   ├── domain/       # entities and enums
│       │   ├── repository/   # Spring Data JPA repositories
│       │   ├── service/      # application services
│       │   ├── web/          # REST controllers + DTO records
│       │   ├── web/dto/      # API contract records
│       │   ├── optimization/ # intelligent module
│       │   ├── security/     # token authentication
│       │   ├── config/       # Spring configuration
│       │   ├── common/       # error handling
│       │   └── bootstrap/    # demonstration data
│       ├── main/resources/   # application.yml
│       └── test/             # JUnit 5 tests
├── frontend/                 # React 19 + Vite + TypeScript SPA
│   ├── package.json
│   ├── vite.config.ts        # @ alias, Tailwind plugin, /api proxy
│   ├── components.json       # shadcn/ui configuration
│   └── src/
│       ├── components/ui/    # shadcn/ui components (vendored, not own code)
│       ├── components/layout/
│       ├── components/shared/
│       ├── pages/
│       ├── hooks/
│       ├── lib/              # API client, formatters
│       └── types/            # TypeScript mirror of the API contract
├── docs/
│   ├── decisions.md          # decision log
│   ├── 01-analysis.md
│   ├── 02-literature-review.md
│   ├── 03-uml/
│   ├── 05-scrum.md
│   └── 06-metrics.md
├── tools/
│   ├── setup-toolchain.sh    # installs JDK / Maven / CKJM into ~/.local/opt
│   ├── toolchain.env
│   ├── metrics.sh            # Part 6 entry point
│   ├── count-loc.py          # language-aware LOC counter
│   └── pmd/wastems-ruleset.xml
└── presentation/
```

The top-level shape is stable: `backend/` and `frontend/` replace the single `src/` folder
because the prototype is two independently built applications (D-018).

## 7. Conventions

- **Docs:** Markdown, English unless the user says otherwise. One file per assignment part.
- **Diagrams:** Mermaid inside Markdown by default (renders on GitHub, no toolchain).
  Use PlantUML only if the user prefers it. Always keep the source next to the export.
- **Backend code:** Java 21 idioms. Entities in `domain/`, persistence in `repository/`,
  business rules in `service/`, transport in `web/`. DTOs are Java `record`s. No Lombok:
  explicit accessors keep the Part 6 metrics honest. The intelligent module lives in its own
  package and must not depend on Spring or on the persistence layer.
- **Frontend code:** TypeScript strict mode. shadcn/ui components are vendored under
  `src/components/ui/` and are **not** counted as own code in Part 6. Pages live in
  `src/pages/`, shared widgets in `src/components/shared/`, server state through TanStack
  Query. The API contract is typed in `src/types/api.ts` and must stay in sync with the
  backend records.
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

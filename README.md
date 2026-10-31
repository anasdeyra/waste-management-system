# WasteMS — Intelligent Waste Management System

Management system for municipal waste collection, organised around five core areas —
**collection points, vehicles, agents, tours and waste** — plus an **intelligent
tour-optimization module** and a **reproducible impact evaluation** against a documented
baseline.

This repository is the deliverable of the Advanced Software Engineering course project. It
contains the problem analysis, the literature review, the UML models, the prototype, the
Scrum artifacts and the software metrics.

---

## 1. Technology stack

| Layer | Technology | Version |
|---|---|---|
| Backend | Java (Eclipse Temurin LTS) | 21 |
| Backend framework | Spring Boot (Web, Data JPA, Security, Validation) | 3.5.16 |
| Build tool | Apache Maven (with wrapper) | 3.9.16 |
| Database | H2, file mode | 2.x |
| Frontend | React + TypeScript | 19 / 5.x |
| Build tool | Vite | 8.x |
| Styling | Tailwind CSS | 4.x |
| Components | shadcn/ui (Radix UI) | latest |
| Data fetching | TanStack Query + React Router | 5.x / 7.x |
| Tests | JUnit 5, AssertJ, Spring Security Test | — |
| Metrics | PMD, CKJM, JDepend, `tools/count-loc.py` | — |

The stack is recorded in [`docs/decisions.md`](docs/decisions.md) (D-004, D-013, D-017,
D-018).

## 2. Repository layout

```
project-ase/
├── AGENTS.md                  # workspace conventions and status
├── project-assignment.md      # the assignment (canonical)
├── README.md                  # this file
├── backend/                   # Spring Boot REST API (Java 21, Maven)
│   ├── mvnw                   # Maven wrapper
│   └── src/main/java/com/ase/wastems/
│       ├── domain/            # entities and enums
│       ├── repository/        # Spring Data JPA repositories
│       ├── service/           # application services
│       ├── web/               # REST controllers and DTOs
│       ├── optimization/      # the intelligent module (baseline, NN, NN + 2-opt)
│       ├── security/          # token authentication
│       ├── config/            # Spring configuration
│       └── bootstrap/         # demonstration data
├── frontend/                  # React single-page application (Vite, Tailwind, shadcn/ui)
│   └── src/
│       ├── components/ui/     # shadcn/ui components (vendored)
│       ├── components/layout/ # application shell
│       ├── pages/             # one page per functional area
│       ├── lib/               # API client and formatting helpers
│       └── types/             # TypeScript mirror of the API contract
├── docs/
│   ├── decisions.md           # decision log
│   ├── 01-analysis.md         # Part 1 — problem analysis
│   ├── 02-literature-review.md
│   ├── 03-uml/                # Part 3 — UML models (Mermaid)
│   ├── 05-scrum.md            # Part 5 — Scrum artifacts
│   └── 06-metrics.md          # Part 6 — software metrology
├── tools/                     # toolchain installer and metric scripts
└── presentation/              # final deck
```

## 3. Prerequisites

| Tool | Required version | Notes |
|---|---|---|
| Node.js | 20.19+ / 22.12+ | provided by `fnm` or `nvm` on the development machine |
| npm | 10+ | ships with Node.js |
| JDK 21 | 21 | installed locally by `tools/setup-toolchain.sh` |
| Maven | 3.9+ | installed locally by `tools/setup-toolchain.sh` |

The development machine has no system package manager and no root access, so the Java
toolchain is installed as user-local tarballs. Install it once with:

```bash
bash tools/setup-toolchain.sh
. "$HOME/.local/opt/ase-env.sh"   # sets JAVA_HOME, MAVEN_HOME and PATH
```

The script is idempotent: it skips anything that is already present.

## 4. Running the prototype

### 4.1 Backend

```bash
cd backend
./mvnw spring-boot:run          # or: mvn spring-boot:run
```

The API listens on <http://localhost:8080>. The H2 database is created on first start in
`backend/data/` and is seeded with the demonstration data set.

| User | Password | Role |
|---|---|---|
| `admin` | `admin123` | administrator + manager |
| `manager` | `manager123` | collection manager / planner |
| `agent` | `agent123` | collection agent |

Useful URLs:

- <http://localhost:8080/api/health> — liveness probe
- <http://localhost:8080/h2-console> — database console (JDBC URL
  `jdbc:h2:file:./data/wastems`, user `sa`, empty password)

### 4.2 Frontend

```bash
cd frontend
npm install
npm run dev
```

The application is served on <http://localhost:5173>. The Vite development server proxies
`/api` to `http://localhost:8080`, so no CORS configuration is required.

### 4.3 Production build

```bash
cd backend  && ./mvnw clean package        # -> backend/target/wastems-backend-0.1.0-SNAPSHOT.jar
cd frontend && npm run build               # -> frontend/dist
```

## 5. Tests and metrics

```bash
cd backend && ./mvnw test                  # unit and integration tests
bash tools/metrics.sh                      # LOC, CKJM, PMD, JDepend (Part 6)
```

`tools/metrics.sh` writes the raw reports to `metrics/`. Every number quoted in
`docs/06-metrics.md` must come from that script so the results stay reproducible.

## 6. Documentation

| Part | Document | State |
|---|---|---|
| 1 — Problem analysis | [`docs/01-analysis.md`](docs/01-analysis.md) | complete |
| 2 — Literature review | `docs/02-literature-review.md` | to do |
| 3 — UML modelling | `docs/03-uml/` | to do |
| 4 — Prototype | `backend/`, `frontend/` | scaffold running; features in progress |
| 5 — Scrum | `docs/05-scrum.md` | to do |
| 6 — Metrology | [`docs/06-metrics.md`](docs/06-metrics.md) | tooling + preliminary baseline; final measurement pending |

## 7. Conventions

- Documentation is Markdown, in English, one file per assignment part.
- Diagrams are Mermaid inside Markdown so they render on GitHub without a toolchain.
- Commits are imperative and scoped: `docs: add use case diagram`, `feat(auth): add login`.
- Secrets are never committed; `.env.example` documents the expected variables.

# WasteMS — Intelligent Waste Management System

Map-centric management system for municipal waste collection, organised around five core areas
— **collection points, vehicles, agents, tours and waste** — plus **real street routing**, an
**intelligent tour-optimization module**, and a **reproducible impact evaluation** against a
documented baseline. The worked example uses official DSNY locations in **New York City,
Lower Manhattan (MN01)**, with interactive maps for planning, execution, and comparison.

This repository is being developed as the deliverable of the Advanced Software Engineering
course project. Its intended final contents are the problem analysis, literature review, UML
models, prototype, Scrum artifacts, and software metrics; the status table below distinguishes
completed artifacts from planned work.

> **Current state: Sprint S1 delivered.** Session authentication with CSRF protection, the
> ADMIN / PLANNER / AGENT roles, user administration and waste categories are implemented
> (backend API and React UI). Maps, collection points, fleet, tours and optimization are not
> implemented yet. Real MN01 source extracts are already included under
> `backend/src/main/resources/data/`.

The real-city sources, included snapshots, map interactions, routing rules, and attribution
are documented in [`docs/geospatial-data.md`](docs/geospatial-data.md). Traffic is a timestamped
map overlay; it does not drive dynamic rerouting or the controlled optimization experiment.

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
| Interactive maps (planned) | Leaflet + React Leaflet, OSM-compatible basemap | not installed yet |
| Road routing (planned) | GraphHopper + versioned OpenStreetMap extract | not installed yet |
| Tests | JUnit 5, AssertJ, Spring Security Test | — |
| Metrics | PMD, CKJM, JDepend, `tools/count-loc.py` | — |

The stack and geospatial direction are recorded in
[`docs/decisions.md`](docs/decisions.md) (D-004, D-013, D-017–D-022).

## 2. Repository layout

```
project-ase/
├── AGENTS.md                  # workspace conventions and status
├── project-assignment.md      # the assignment (canonical)
├── README.md                  # this file
├── backend/                   # Spring Boot REST API (Java 21, Maven)
│   ├── mvnw                   # Maven wrapper
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/ase/wastems/
│       │   ├── domain/ repository/ service/ web/ security/ config/
│       │   └── WasteMsApplication.java
│       ├── main/resources/    # application.yml + real NYC snapshots in data/
│       └── test/              # JUnit 5 tests
├── frontend/                  # React single-page application (Vite, Tailwind, shadcn/ui)
│   ├── package.json
│   ├── vite.config.ts         # @ alias, Tailwind plugin, /api proxy
│   ├── components.json        # shadcn/ui configuration
│   └── src/
│       ├── components/ui/     # shadcn/ui components (vendored)
│       ├── components/shared/ # app shell, route guards
│       ├── pages/ hooks/ types/  # pages, auth hooks, typed API contract
│       ├── lib/               # cn(), API client, auth helpers
│       ├── App.tsx            # routes
│       └── main.tsx           # providers: Query, Router
├── docs/
│   ├── decisions.md           # decision log
│   ├── geospatial-data.md     # NYC sources, snapshots, maps, routing, attribution
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

## 4. Running the application

### 4.1 Backend

```bash
cd backend
./mvnw spring-boot:run          # or: mvn spring-boot:run
```

The API listens on <http://localhost:8080>. It starts with the `dev` Spring profile by default
(`spring.profiles.default: dev`), which creates three accounts on first start:

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | ADMIN |
| `planner` | `planner123` | PLANNER |
| `agent` | `agent123` | AGENT |

These are **development credentials only: change or remove them for any shared deployment.**
To start without seeding, use any non-`dev` profile, e.g.
`./mvnw spring-boot:run -Dspring-boot.run.profiles=prod`. To reset the database, stop the
application and delete `backend/data/`.

 The H2 database file is created on first start in
`backend/data/`; the H2 console is available at <http://localhost:8080/h2-console> (JDBC URL
`jdbc:h2:file:./data/wastems`, user `sa`, empty password).

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

### 4.4 API authentication (curl)

The API uses an HttpSession cookie (`JSESSIONID`) plus a CSRF token: `GET /api/auth/csrf` sets
the cookie `XSRF-TOKEN`; send its value back as the header `X-XSRF-TOKEN` on every
POST/PUT/PATCH request. The CSRF cookie is cleared on logout, so fetch a token again afterwards.

```bash
B=http://localhost:8080; J=/tmp/jar
curl -s -c $J -b $J $B/api/auth/csrf                      # sets XSRF-TOKEN
X=$(awk '$6=="XSRF-TOKEN"{print $7}' $J)
curl -s -c $J -b $J -H "X-XSRF-TOKEN: $X" -H 'Content-Type: application/json' \
     -d '{"username":"admin","password":"admin123"}' $B/api/auth/login   # sets JSESSIONID
curl -s -b $J $B/api/auth/me                              # {"id":...,"username":"admin","role":"ADMIN"}
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
| 2 — Literature review | [`docs/02-literature-review.md`](docs/02-literature-review.md) | complete |
| 3 — UML modelling | [`docs/03-uml/README.md`](docs/03-uml/README.md) | four required diagrams + OCL complete |
| 4 — Prototype | `backend/`, `frontend/` | S1: authentication, roles, users, waste categories |
| 5 — Scrum | [`docs/05-scrum.md`](docs/05-scrum.md) | Product Backlog complete; S1 executed, S2–S10 planned |
| 6 — Metrology | `docs/06-metrics.md` | tooling ready, measurements to do |
| Supporting geospatial specification | [`docs/geospatial-data.md`](docs/geospatial-data.md) | NYC snapshots included; maps/routing to do |

## 7. Conventions

- Documentation is Markdown, in English, one file per assignment part.
- Diagrams are Mermaid inside Markdown so they render on GitHub without a toolchain.
- Commits are imperative and scoped: `docs: add use case diagram`, `feat(auth): add login`.
- Secrets are never committed; `.env.example` documents the expected variables.

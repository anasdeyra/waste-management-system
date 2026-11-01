# Part 3 — UML modeling

## Artifacts

| Assignment item | Artifact | Main coverage |
|---|---|---|
| Use-case diagram and short descriptions | [01-use-cases.md](01-use-cases.md) | Grouped system overview, human/supporting actors, 25 use cases traced to all 39 user stories |
| Class diagram | [02-class-diagram.md](02-class-diagram.md) | Operational domain, current/planned demand, resources, tour execution, collection results, provenance |
| Sequence diagram | [03-sequence-optimize-tour.md](03-sequence-optimize-tour.md) | Directed three-method evaluation, retained results, planner review, explicit proposal acceptance |
| Activity diagram | [04-activity-tour-lifecycle.md](04-activity-tour-lifecycle.md) | Draft planning, confirmation, execution, terminal stop outcomes, completion and cancellation (S8: cancellation fields, `OutcomeCorrection` and the correction path) |
| OCL constraints | [02-class-diagram.md §4](02-class-diagram.md#4-ocl-constraints) | Coordinates, positive demand/capacity, agent links, crew/capacity, stop order/outcomes, lifecycle operations |

## Modeling conventions

- Diagram sources are Mermaid blocks inside Markdown (D-032), so the Markdown files are the
  canonical sources and can be rendered by GitHub or a Mermaid-compatible Markdown viewer.
- The use-case overview uses flowchart syntax because Mermaid has no native UML use-case diagram.
  The class, sequence, and activity views use their corresponding Mermaid constructs.
- The main class diagram is operational-domain only (D-033). Optimization/routing participants
  appear in the sequence view without prematurely fixing implementation classes.
- Names and states are shared with `docs/01-analysis.md` and `docs/05-scrum.md`: `ADMIN`,
  `PLANNER`, `AGENT`, `DRAFT`, `PLANNED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `PENDING`,
  `COLLECTED`, and `NOT_COLLECTED`.
- OCL describes intended domain invariants and operation pre/postconditions. It is not a claim
  that these constraints are already implemented in the prototype.

## Traceability rule

The use-case table is the bridge from Product Backlog to UML: each grouped use case names its
atomic `US-xx` stories and `FR-xx` requirements. The sequence diagram refines UC10/UC11, and the
activity diagram refines UC08–UC19. Future implementation classes and tests should retain these
identifiers in their design/test documentation where useful.

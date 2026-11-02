# Part 2 — Literature review

## 1. Research question and scope

> **To what extent does integrating an intelligent tour-optimization module reduce total
> road-network travel distance and estimated collection duration compared with a documented
> baseline, while satisfying the same vehicle-capacity and collection-service constraints?**

This review focuses on the intelligent module of the Intelligent Waste Management System.
The operational prototype also manages collection points, vehicles, agents, tours, and waste,
but those management features are not the subject of this literature question.

The agreed optimization model has the following boundaries:

- one real NYC depot and one vehicle per tour-optimization run;
- selected real MN01 DSNY collection-point coordinates, synthetic waste demand, and service
  time;
- directed road-network travel distance/base travel time and an explicit vehicle-capacity
  check;
- estimated duration equal to road-network travel time plus service time at the selected
  stops;
- three deterministic planning methods: input order, capacity-aware nearest neighbor, and
  nearest neighbor followed by directed-cost 2-opt;
- identical source/road snapshots, scenarios, feasibility rules, and indicator formulas for
  all three methods; and
- real NYC traffic as a display overlay only, not as a varying experimental input.

The model therefore has a **Traveling Salesman Problem (TSP)** sequencing core: find an order
for visiting the selected points and returning to the depot. Because one-way streets and turn
restrictions can make leg costs asymmetric, the operational matrix is a directed/asymmetric
TSP matrix rather than an assumed symmetric Euclidean one. Adding vehicle demand and capacity
places it in the broader family of capacitated-routing problems. It is not a simultaneous
multi-vehicle Capacitated Vehicle Routing Problem (CVRP): the prototype supports multiple
independent single-vehicle tours per day, but does not optimize fleet assignment and all routes
at once.

The literature is examined for four purposes:

1. establishing the routing models and their optimality limits;
2. explaining nearest-neighbor construction and 2-opt local improvement;
3. identifying capacity, service, and evaluation concerns in waste collection;
4. defining what the prototype can legitimately borrow and what its experiments cannot prove.

No numerical improvement is claimed in this review. Experimental results must come from the
reproducible evaluation defined for Part 6.

## 2. Review approach

This is a **focused narrative review**, not a systematic review or meta-analysis. Sources were
selected for their direct relevance to at least one of the four purposes above and for their
academic credibility. The final set contains ten peer-reviewed journal articles:

- five foundational routing and heuristic studies; and
- five waste-collection reviews or applied studies.

Foundational papers are retained because they define the algorithms and their limitations.
Recent waste-collection reviews supply the domain context, while the applied case study shows
how richer operational constraints change the routing problem. Bibliographic details and DOI
links were checked against publisher or Crossref metadata. A citation's age is not treated as a
weakness when its role is historical or algorithmic; conversely, a recent review is not treated
as experimental evidence that one algorithm will outperform another in this project.

## 3. Evidence table

| # | Source and type | Main contribution and evidence | Limitations | Relevance to the project |
|---|---|---|---|---|
| 1 | **Dantzig & Ramser (1959)** — peer-reviewed journal article | Formulated the truck-dispatching problem around customer demand, truck capacity, a terminal, and mileage minimization; proposed a computational procedure for near-optimal routing. | Historical formulation, not a modern benchmark or proof that a particular heuristic is best. | Establishes demand and capacity as intrinsic routing concerns rather than optional display data. |
| 2 | **Clarke & Wright (1964)** — peer-reviewed journal article | Introduced the savings construction method for rapidly combining delivery routes from a central depot when vehicle capacities permit. | “Optimum or near-optimum” is not a general guarantee of global optimality; the multi-route savings method is not the project's implemented algorithm. | Provides a classical constructive-heuristic reference point and situates nearest neighbor within a broader family of fast feasible-route builders. |
| 3 | **Croes (1958)** — peer-reviewed journal article | Presented a mechanized tour-improvement method for the TSP commonly associated with 2-opt edge exchanges. | Concerns the TSP rather than capacitated waste collection; improving exchanges can still end at a local optimum. | Directly supports 2-opt as the local-improvement stage after an initial tour has been constructed. |
| 4 | **Lin (1965)** — peer-reviewed journal article | Distinguished exact solutions for small TSP instances from heuristic local optima for larger instances and discussed repeated starts as a way to encounter better local optima. | TSP-only evidence; reported computing times reflect 1960s hardware and cannot be transferred to this prototype. | Supplies the essential caution that NN + 2-opt is a heuristic and does not certify a globally optimal collection tour. |
| 5 | **Rosenkrantz, Stearns, & Lewis (1977)** — peer-reviewed journal article | Analyzed several polynomial-time TSP heuristics and proved logarithmic worst-case bounds for nearest-neighbor tour quality rather than assuming that greedy choices are reliably near-optimal. | Worst-case theoretical bounds do not predict the average gain on the project's seeded instances. | Gives a rigorous reason to evaluate nearest neighbor empirically and to study the marginal benefit of adding local search. |
| 6 | **Han & Ponce-Cueto (2015)** — peer-reviewed literature review | Classified waste-collection routing research by collection type and by node-routing versus arc-routing models, and reviewed solution methods for each. | An older review that does not cover all recent heuristic, learning-based, emissions, or dynamic-routing work. | Confirms that waste collection contains several distinct routing models and prevents an inappropriate one-to-one claim between this project and every waste-routing variant. |
| 7 | **Sulemana et al. (2018)** — peer-reviewed journal review | Reviewed optimization methods for solid-waste truck routing, particularly mathematical-programming and GIS-based approaches, with attention to implementation conditions. | Broad qualitative synthesis rather than a controlled comparison of input order, nearest neighbor, and 2-opt. | Supports transparent reporting of datasets, constraints, and local assumptions instead of transferring percentage gains between municipalities. |
| 8 | **Babaee Tirkolaee et al. (2019)** — peer-reviewed applied case study | Studied a multi-trip waste-collection VRP with time windows using simulated annealing, a real Iranian case, and comparison with CPLEX; its objective includes traversal, vehicle-use, and time-window costs. | Much richer than the project's model and tied to a particular case; its results do not establish the performance of NN + 2-opt. | Demonstrates how additional service constraints and costs alter the problem and clarifies which operational conclusions are outside this prototype's scope. |
| 9 | **Liang, Minanda, & Gunawan (2022)** — peer-reviewed mini-review | Reviewed recent heuristic approaches and waste-collection routing applications, confirming the continued importance of constructive and improvement heuristics. | A mini-review, not an exhaustive systematic review or a source of directly comparable benchmark values for this project. | Supports a lightweight heuristic prototype while motivating comparison with stronger methods as future work. |
| 10 | **Restrepo-Franco, Valencia-Rodríguez, & Toro-Ocampo (2025)** — peer-reviewed systematic review | Used a systematic protocol to organize residential waste-collection VRP research by variants, objectives, constraints, decision variables, solution methods, and applications. | A research map does not itself measure nearest-neighbor or 2-opt performance on the project's scenarios. | Provides current domain grounding and reinforces the need to state the exact variant, objective, and constraints being evaluated. |

## 4. Thematic synthesis

### 4.1 Exact optimization and heuristic routing

Dantzig and Ramser's original formulation already combined route cost, customer demand, and
vehicle capacity [1]. Subsequent waste-routing reviews show that this basic structure has been
extended into many variants rather than one universal “waste-routing problem” [6, 10]. Exact
methods are valuable when a proof of optimality is required and the instance is tractable, but
they are not the only legitimate way to support operational planning.

The project instead investigates an explainable heuristic pipeline. This is appropriate for a
software-engineering prototype whose research objective is to measure the effect of integrating
an intelligent module, not to advance exact optimization theory. The necessary condition is
honest terminology: input order, nearest neighbor, and NN + 2-opt produce candidate tours under
a documented validation process; the last method is not called globally optimal.

### 4.2 Nearest-neighbor construction

Nearest neighbor builds a sequence by repeatedly choosing the feasible unvisited point with
the lowest directed road distance from the current location. Its advantages are simplicity,
deterministic output, and an easily explained decision at every stop. Clarke and Wright's
savings procedure illustrates the wider
family of constructive methods: rapid feasibility and useful routes are prioritized over an
optimality certificate [2].

Nearest neighbor is nevertheless myopic. A locally short next hop can leave isolated points for
the end of the tour or produce an expensive return toward the depot. Rosenkrantz et al. provide
worst-case theoretical evidence that nearest-neighbor solutions can deteriorate with instance
size [5]. That result does **not** predict a fixed percentage loss or gain in the prototype. It
justifies measuring actual results and adding a separate improvement stage rather than assuming
that greedy construction is sufficient.

Capacity adds a feasibility condition to the construction rule. A candidate point cannot be
accepted merely because it is nearest; the resulting tour must also satisfy the same capacity
validation applied to the baseline and optimized solutions. Every method must cover exactly the
same selected points or be reported as infeasible.

### 4.3 2-opt local improvement

A 2-opt move removes two non-adjacent tour edges and reconnects the tour in the alternative way.
For a symmetric tour this is equivalent to reversing one segment and permits a constant-size
edge-delta calculation. That shortcut is **not valid** for the project's directed road matrix:
reversing a stop segment also reverses its internal legs, whose costs must be recalculated. A
move is accepted only when its exact directed total distance is lower. Repeating accepted moves
until no improving 2-edge exchange remains produces a **2-optimal local optimum** for the
implemented directed-cost neighborhood.

Croes supplies the foundational tour-improvement method [3], while Lin makes the limits of local
search explicit [4]. A 2-optimal tour may still be improved by a different initial tour or by a
higher-order exchange. Consequently, the evaluation must distinguish two questions:

1. How much does capacity-aware nearest neighbor change the input-order result?
2. How much additional improvement does 2-opt obtain from that nearest-neighbor result?

This separation is more informative than comparing only the weakest and strongest pipelines.
Any implementation must also revalidate capacity and point coverage after improvement; a shorter
sequence is not acceptable if it violates the scenario constraints.

### 4.4 Capacity, service time, and duration

Capacity is central to both classical vehicle routing and waste-collection applications [1, 6,
8]. It must therefore be part of scenario generation, feasibility checking, and result reporting,
not added after distance optimization as an informal warning.

The duration model is deliberately simpler than the multi-cost objectives found in applied
studies such as Babaee Tirkolaee et al. [8]. For a method $m$, let $P_m$ be its ordered path,
$t_{ij}$ the directed base road-network travel time from stop $i$ to stop $j$, and $s_i$ the
service time at stop $i$. Then:

$$
T_m = \sum_{(i,j) \in P_m} t_{ij} + \sum_{i=1}^{N} s_i
$$

The routing engine supplies each $t_{ij}$ from the same versioned road graph for every compared
method. For a fixed set of selected stops, total service time is invariant under visit
reordering. Distance and duration must nevertheless both be reported: optimizing directed road
distance does not guarantee an equal change in travel time when road classes and speeds differ.

The model excludes time windows, live-traffic cost changes, road gradients, and variable service
disruptions. NYC DOT traffic is shown as contextual map information, not inserted into $t_{ij}$.
Those exclusions make the computation reproducible but prevent direct operational claims about
real collection shifts or dynamic rerouting.

### 4.5 Waste-collection evaluation practice

The domain literature uses real municipal cases, GIS distances, commercial solvers, and a range
of heuristics and metaheuristics [7–10]. Babaee Tirkolaee et al., for example, evaluate a richer
multi-trip, time-window problem using a real case and CPLEX comparison [8]. Such studies show
operational relevance, but their numerical outcomes are not transferable to a different network,
constraint set, or algorithm.

The reviews by Han and Ponce-Cueto, Sulemana et al., and Restrepo-Franco et al. also show why a
single universal performance figure would be misleading [6, 7, 10]. Model variant, demand,
depot location, capacity tightness, road data, and baseline all affect the result.

Seeded scenarios sampled from real MN01 coordinates are therefore suitable for this project's
**controlled algorithmic comparison**. They hold real geometry, directed road costs, and
synthetic demand constant across methods and avoid live third-party-service variability. This
improves geographic realism without turning the result into a municipal field validation:
actual DSNY demand, official tours, truck-specific restrictions, and operational outcomes are
not observed.

### 4.6 Solution quality, computational effort, and reproducibility

The literature distinguishes finding a useful route quickly from proving that it is optimal
[2–5]. NN + 2-opt performs more work than nearest neighbor alone because it searches for
improving exchanges. Its evaluation should therefore record execution time alongside distance
and duration rather than presenting additional route quality as cost-free.

Reproducibility requires more than naming an algorithm. The scenario seed, demand profile,
point count, real source coordinates, depot, vehicle capacity, road graph and routing-engine
version, directed cost matrix, service times, method, tour order, indicators, and feasibility
checks must be retained. This follows the project's NFR-07 and NFR-09 and addresses the context
sensitivity visible in the waste-routing reviews [7, 10].

## 5. Research gap and project positioning

### 5.1 What the reviewed literature establishes

- Waste collection is an established application of node- and arc-routing models with demands,
  capacities, depots, and route-cost objectives [1, 6, 10].
- Constructive heuristics provide fast candidate routes, but nearest neighbor has demonstrable
  structural and worst-case weaknesses [2, 5].
- 2-opt can improve an existing tour, but a 2-optimal result is only locally optimal [3, 4].
- Applied waste-routing studies often include multiple trips, time windows, GIS data, emissions,
  monetary costs, or metaheuristics beyond this project's scope [7–10].
- Dataset, baseline, objectives, and constraints must be explicit before reported improvements
  can be interpreted [7, 10].

### 5.2 What the reviewed sources do not answer

The selected literature does not provide a direct, controlled answer for the exact comparison
of **input order**, **capacity-aware nearest neighbor**, and **nearest neighbor plus 2-opt** on
this project's small single-vehicle scenarios built from real MN01 coordinates and synthetic
demand. Published results obtained with other road networks, richer constraints, or other
algorithms cannot be inserted as expected results for this experiment.

The project addresses this narrow evidence gap with its own reproducible measurements. This is
a project-specific evaluation gap, not a claim that no broader routing research exists.

### 5.3 What the project borrows

| Borrowed element | Source basis | Project adaptation |
|---|---|---|
| Demand- and capacity-aware routing | Dantzig & Ramser [1] | Explicit scenario demand and identical capacity validation for every compared method. |
| Constructive-heuristic perspective | Clarke & Wright [2] | Deterministic nearest-neighbor construction, without claiming to implement the savings algorithm. |
| Empirical caution about greedy routing | Rosenkrantz et al. [5] | Separate measurement of nearest neighbor instead of assuming its route quality. |
| Local edge-exchange improvement | Croes [3] | Deterministic 2-opt after nearest-neighbor construction. |
| Local-versus-global optimality distinction | Lin [4] | Results are labelled heuristic; no global-optimality claim. |
| Explicit waste-routing variants and constraints | Domain reviews and case study [6–10] | A bounded single-vehicle model, with excluded real-world constraints stated rather than hidden. |

## 6. Consequences for the experimental design

The fixed evaluation matrix contains **60 real-location scenarios**: three synthetic demand
profiles, five seeds, and four sizes (10, 25, 50, and 100 collection points), sampled from the
eligible MN01 DSNY source records as recorded in D-022. A separate hand-crafted Lower Manhattan
dataset supports the demonstration and is not mixed into the seeded aggregate.

For every scenario and method, the evaluation will retain:

- scenario identifier, input point order, seed, profile, and point count;
- source snapshot, coordinates, depot, synthetic demands, vehicle capacity, and service-time
  parameters;
- road graph snapshot, routing-engine version, and directed distance/base-time matrix;
- resulting visit order, road-following geometry reference, and collection-point coverage;
- total road distance, estimated duration, and execution time; and
- capacity and other supported constraint checks.

Distance and duration changes will be calculated relative to the input-order baseline using the
same scenario. The intermediate nearest-neighbor result will show how much of the total change
comes from construction and how much from 2-opt. Execution-time measurements will be reported
with their execution environment; no runtime target or improvement percentage is invented in
advance.

### Supported conclusions after measurement

The experiment can support bounded statements such as:

> On the specified seeded MN01 road-network scenarios, NN + 2-opt reduced or did not reduce
> directed road distance and estimated duration relative to input order, while preserving the
> measured coverage and capacity constraints; its additional computational cost was also
> measured.

The measured answer is in `docs/06-metrics.md` §6.

### Unsupported conclusions

The experiment alone cannot establish that:

- NN + 2-opt is globally optimal or state of the art;
- an observed percentage will generalize to other municipalities, road snapshots, or actual
  DSNY operations;
- the method outperforms exact solvers, commercial systems, or advanced metaheuristics;
- the results prove fleet-level coordination, live-traffic rerouting, or IoT benefits; or
- the base travel-time model is operationally valid without field validation.

## 7. References

1. Dantzig, G. B., & Ramser, J. H. (1959). The truck dispatching problem. *Management Science,
   6*(1), 80–91. <https://doi.org/10.1287/mnsc.6.1.80>
2. Clarke, G., & Wright, J. W. (1964). Scheduling of vehicles from a central depot to a number of
   delivery points. *Operations Research, 12*(4), 568–581.
   <https://doi.org/10.1287/opre.12.4.568>
3. Croes, G. A. (1958). A method for solving traveling-salesman problems. *Operations Research,
   6*(6), 791–812. <https://doi.org/10.1287/opre.6.6.791>
4. Lin, S. (1965). Computer solutions of the traveling salesman problem. *Bell System Technical
   Journal, 44*(10), 2245–2269. <https://doi.org/10.1002/j.1538-7305.1965.tb04146.x>
5. Rosenkrantz, D. J., Stearns, R. E., & Lewis, P. M., II. (1977). An analysis of several
   heuristics for the traveling salesman problem. *SIAM Journal on Computing, 6*(3), 563–581.
   <https://doi.org/10.1137/0206041>
6. Han, H., & Ponce-Cueto, E. (2015). Waste collection vehicle routing problem: Literature
   review. *Promet – Traffic & Transportation, 27*(4), 345–358.
   <https://doi.org/10.7307/ptt.v27i4.1616>
7. Sulemana, A., Donkor, E. A., Forkuo, E. K., & Oduro-Kwarteng, S. (2018). Optimal routing of
   solid waste collection trucks: A review of methods. *Journal of Engineering, 2018*, Article
   4586376. <https://doi.org/10.1155/2018/4586376>
8. Babaee Tirkolaee, E., Abbasian, P., Soltani, M., & Ghaffarian, S. A. (2019). Developing an
   applied algorithm for multi-trip vehicle routing problem with time windows in urban waste
   collection: A case study. *Waste Management & Research, 37*(1 Suppl.), 4–13.
   <https://doi.org/10.1177/0734242X18807001>
9. Liang, Y.-C., Minanda, V., & Gunawan, A. (2022). Waste collection routing problem: A
   mini-review of recent heuristic approaches and applications. *Waste Management & Research,
   40*(5), 519–537. <https://doi.org/10.1177/0734242X211003975>
10. Restrepo-Franco, A. M., Valencia-Rodríguez, O., & Toro-Ocampo, E. M. (2025). The vehicle
    routing problem as applied to residential solid waste collection operations: Systematic
    literature review. *International Journal of Industrial Engineering Computations, 16*(1),
    197–220. <https://doi.org/10.5267/j.ijiec.2024.10.002>

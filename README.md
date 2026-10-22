# DAA Assignment 3: Graphs

**Student:** Temirgaliyev Nurassyl  
**Group:** SE-2523  
**Repository:** https://github.com/barri857/daa-assignment3  
**Release:** branch `main`, tag `v1.0`

This project implements DSU, Kruskal, Prim, Kosaraju, Kahn and shortest paths in a DAG. Java source and tests contain no comments. Explanations are in the report and this README. Optional bonus tasks are not included.

## Requirements

- JDK 17 or newer; set `JAVA_HOME` to the JDK and put its `bin` directory on `PATH`.
- Maven 3.9 or newer, available as `mvn`.
- Python 3.10 or newer for regenerating the plots and report.
- Install the Python dependencies once: `python -m pip install -r requirements.txt`.

The submitted results were checked with OpenJDK 25 and Maven 3.9.12. A Java 8 runtime is not sufficient. In IntelliJ IDEA, open `pom.xml`, select JDK 17 or newer as the project SDK, and use Maven's `test` goal.

## Build and test

Run from the project folder:

```text
mvn test
```

This compiles the project and runs 23 JUnit 5 tests. No tests are skipped. Tests cover random DSU unions, 100 random connected MST graphs, SCCs against BFS reachability, DAG paths against Bellman-Ford with negative weights, graph files, edge cases and an iterative DFS on 100,000 vertices.

## Benchmark

```text
java -Xmx2g -cp target/classes benchmark.Benchmark
```

The benchmark creates 30 inputs: five sizes, two densities and three graph kinds. It runs five algorithms on the appropriate inputs, giving 50 CSV rows and 250 measured runs. Each row contains the median of five times. Each graph starts with a new `Random(42)`. When endpoints coincide, only `b` is redrawn. The DAG permutation gives vertex ranks, and every edge is oriented from lower to higher rank.

The benchmark checks every reference value, MST agreement and edge counts, every topological order, exact scan counters and counter stability between runs. A failure throws an exception and does not replace the final CSV. Only a completely verified run replaces `results/results.csv`. The included `results/benchmark.txt` is the console output of the submitted run; later benchmark executions print new output to the terminal.

## Reproduce the results, plots and report

After installing the requirements above, run this single command in PowerShell:

```powershell
./reproduce.ps1
```

On other systems, or to run each step separately:

```text
mvn test
java -Xmx2g -cp target/classes benchmark.Benchmark
python scripts/plots.py
python scripts/report.py
```

This regenerates the CSV, three PNG plots, fitted slopes, `REPORT.md` and the five-page `REPORT.pdf`. Times change between runs; numeric algorithm results match the reference table. Comparator counts are reproducible with the same implementation and input order, but Java sorting/heap implementation changes can affect them.

## Run an input file and print paths

```text
java -cp target/classes benchmark.Demo data/dag.txt
java -cp target/classes benchmark.Demo data/disconnected.txt
java -cp target/classes benchmark.Demo data/cyclic.txt
```

The first line is `n m directed` or `n m undirected`, followed by exactly `m` lines of `u v w`. Vertices are numbered `0` to `n-1`. Parallel edges and self-loops are accepted. A connected fixture is also in `data/connected.txt`.

For undirected input, the demo prints both minimum spanning forest weights and the number of components. For directed input, it prints the SCC count, a valid topological order and each vertex's distance and restored path. If there is a cycle, it reports the cycle instead of running DAG shortest paths. All indegree-zero vertices start at distance zero, including isolated vertices.

## Layout

```text
src/main/java/graph/          Shared adjacency-list graph and edge
src/main/java/algorithms/     Six algorithms and their result classes
src/main/java/metrics/        Operation counters
src/main/java/benchmark/      Exact generators, benchmark and file demo
src/test/java/               JUnit 5 tests
data/                       Four small graph fixtures used in tests
results/results.csv         50 benchmark rows
results/benchmark.txt       Submitted benchmark console output
results/slopes.csv          Fitted log-log slopes
results/plots/              Three PNG figures
scripts/                    Plot and report generation
REPORT.md                   Report source readable on GitHub
REPORT.pdf                  Five-page formatted report
```

## Metrics and implementation details

- `dsu_ops`: find calls plus successful union calls. Rejected unions still invoke two finds. Path-pointer steps are not counted separately.
- `edge_scans`: Kruskal's examined sorted edges; Prim's adjacency entries; Kosaraju's two DFS passes; Kahn's outgoing-edge pass; DAG's relaxation pass.
- `comparisons`: weight comparisons in sorting and the priority queue. Relaxation comparisons are not weight-to-weight comparisons and are excluded.
- `max_depth`: maximum explicit DFS stack size in Kosaraju; zero for the other methods.

Graph construction maintains indegrees. Kosaraju's transpose construction is excluded from the required `2m` scan count. DAG shortest paths uses Kahn internally but its reported scan count includes only the required `m` relaxations. Both preparation steps are included in measured execution time. Kruskal's early stopping does not eliminate sorting the whole edge list. The report distinguishes scan slopes from sorting complexity instead of attributing a logarithmic sorting cost to edge scans.

Results use `long` for weights and distances. The code uses standard Java collections, sorting and a priority queue; it does not use a graph-algorithm library.

## Git workflow

Feature work is committed on `feature/dsu`, `feature/mst`, `feature/scc`, `feature/paths` and `feature/metrics`, with merges into `main`. Release `v1.0` identifies the final project. To inspect the history:

```text
git log --graph --oneline --all
git tag
```

## AI-use acknowledgement

OpenAI Codex assisted with implementing the Java code, writing tests, checking the benchmark, creating plots, and drafting the report and README. The generated code was compiled, the 23 tests were run successfully, and all benchmark reference values were checked. This acknowledgement describes the assistance used; it does not imply that the student has already reviewed or defended the work.

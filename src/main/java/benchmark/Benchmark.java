package benchmark;

import algorithms.DagShortestPath;
import algorithms.Kahn;
import algorithms.Kosaraju;
import algorithms.Kruskal;
import algorithms.Prim;
import graph.Graph;
import metrics.Metrics;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;

public class Benchmark {
    private static final int RUNS = 5;
    private static final int[] SIZES = {1000, 3162, 10000, 31623, 100000};
    private static final long[] MST = {
        147970, 30363, 468911, 97589, 1486139,
        309550, 4715515, 968517, 15012094, 3056681
    };
    private static final int[] SCC = {27, 1, 137, 1, 373, 1, 1188, 1, 3923, 1};
    private static final long[] DAG_SUM = {
        396274, 252411, 1271911, 723157, 3941828,
        2381342, 12551891, 7376612, 40096539, 23512087
    };

    public static void main(String[] args) throws IOException {
        Path output = Path.of(args.length == 0 ? "results/results.csv" : args[0]);
        Path parent = output.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporary = output.resolveSibling(output.getFileName() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(temporary)) {
            writer.write("algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result");
            writer.newLine();
            int row = 0;
            for (int n : SIZES) {
                for (int factor : new int[] {4, 20}) {
                    int m = factor * n;
                    String density = factor == 4 ? "sparse" : "dense";
                    benchmarkMst(writer, n, m, density, row);
                    System.gc();
                    benchmarkScc(writer, n, m, density, row);
                    System.gc();
                    benchmarkDag(writer, n, m, density, row);
                    System.gc();
                    row++;
                }
            }
        }
        Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Saved 50 verified rows to " + output.toAbsolutePath());
    }

    private static void benchmarkMst(BufferedWriter writer, int n, int m,
                                     String density, int row) throws IOException {
        Graph graph = Generator.undirected(n, m);
        long kruskalWeight = benchmark(writer, "Kruskal", "undirected_" + density,
            graph, m, metrics -> Kruskal.run(graph, metrics), (result, metrics) -> {
                check(result.components == 1, "Kruskal did not produce one tree");
                check(result.edges.size() == n - 1, "Kruskal returned the wrong edge count");
                return result.weight;
            });
        long primWeight = benchmark(writer, "Prim", "undirected_" + density,
            graph, m, metrics -> Prim.run(graph, metrics), (result, metrics) -> {
                check(result.components == 1, "Prim did not produce one tree");
                check(result.edges.size() == n - 1, "Prim returned the wrong edge count");
                return result.weight;
            });
        check(kruskalWeight == primWeight, "Kruskal and Prim disagree for n=" + n + ", m=" + m);
        check(kruskalWeight == MST[row], "MST reference mismatch for n=" + n + ", m=" + m
            + ": expected " + MST[row] + ", got " + kruskalWeight);
    }

    private static void benchmarkScc(BufferedWriter writer, int n, int m,
                                     String density, int row) throws IOException {
        Graph graph = Generator.directed(n, m);
        long count = benchmark(writer, "Kosaraju", "directed_" + density,
            graph, m, metrics -> Kosaraju.run(graph, metrics), (result, metrics) -> {
                check(metrics.edgeScans == 2L * m, "Kosaraju must scan exactly 2m edges");
                return result.count;
            });
        check(count == SCC[row], "SCC reference mismatch for n=" + n + ", m=" + m
            + ": expected " + SCC[row] + ", got " + count);
    }

    private static void benchmarkDag(BufferedWriter writer, int n, int m,
                                     String density, int row) throws IOException {
        Graph graph = Generator.dag(n, m);
        benchmark(writer, "Kahn", "dag_" + density, graph, m,
            metrics -> Kahn.run(graph, metrics), (order, metrics) -> {
            check(Kahn.isValid(graph, order), "Invalid topological order");
            check(metrics.edgeScans == m, "Kahn must scan exactly m edges");
            return order.length;
        });
        long sum = benchmark(writer, "DagShortestPath", "dag_" + density,
            graph, m, metrics -> DagShortestPath.run(graph, metrics), (result, metrics) -> {
                check(metrics.edgeScans == m, "DAG shortest path must scan exactly m edges");
                return result.sum();
            });
        check(sum == DAG_SUM[row], "DAG reference mismatch for n=" + n + ", m=" + m
            + ": expected " + DAG_SUM[row] + ", got " + sum);
    }

    private static <T> long benchmark(BufferedWriter writer, String algorithm,
                                  String name, Graph graph, int m,
                                  Operation<T> operation, ResultCheck<T> resultCheck) throws IOException {
        long[] times = new long[RUNS];
        Metrics firstMetrics = null;
        long firstResult = 0;
        for (int i = 0; i < RUNS; i++) {
            Metrics metrics = new Metrics();
            long start = System.nanoTime();
            T output = operation.run(metrics);
            times[i] = System.nanoTime() - start;
            long result = resultCheck.check(output, metrics);
            if (i == 0) {
                firstMetrics = metrics;
                firstResult = result;
            } else {
                check(result == firstResult, algorithm + " produced inconsistent results");
                check(sameMetrics(firstMetrics, metrics), algorithm + " produced inconsistent metrics");
            }
        }
        Arrays.sort(times);
        double timeMs = times[RUNS / 2] / 1_000_000.0;
        String line = String.format(Locale.ROOT, "%s,%s,%d,%d,%.3f,%d,%d,%d,%d,%d",
            algorithm, name, graph.n, m, timeMs, firstMetrics.dsuOps(),
            firstMetrics.edgeScans, firstMetrics.comparisons,
            firstMetrics.maxDepth, firstResult);
        writer.write(line);
        writer.newLine();
        writer.flush();
        System.out.println(line);
        return firstResult;
    }

    private static boolean sameMetrics(Metrics a, Metrics b) {
        return a.findCalls == b.findCalls && a.unionCalls == b.unionCalls
            && a.edgeScans == b.edgeScans && a.comparisons == b.comparisons
            && a.maxDepth == b.maxDepth;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private interface Operation<T> {
        T run(Metrics metrics);
    }

    private interface ResultCheck<T> {
        long check(T result, Metrics metrics);
    }
}

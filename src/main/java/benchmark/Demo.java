package benchmark;

import algorithms.DagShortestPath;
import algorithms.Kahn;
import algorithms.Kosaraju;
import algorithms.Kruskal;
import algorithms.MstResult;
import algorithms.PathResult;
import algorithms.Prim;
import graph.Graph;
import java.nio.file.Path;
import java.util.Arrays;
import metrics.Metrics;

public class Demo {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: java -cp target/classes benchmark.Demo data/dag.txt");
            return;
        }
        Graph graph = Graph.read(Path.of(args[0]));
        if (!graph.directed) {
            MstResult kruskal = Kruskal.run(graph, new Metrics());
            MstResult prim = Prim.run(graph, new Metrics());
            System.out.println("Kruskal weight: " + kruskal.weight);
            System.out.println("Prim weight: " + prim.weight);
            System.out.println("Components: " + kruskal.components);
            return;
        }
        System.out.println("SCC count: " + Kosaraju.run(graph, new Metrics()).count);
        try {
            int[] order = Kahn.run(graph, new Metrics());
            System.out.println("Topological order: " + Arrays.toString(order));
            PathResult result = DagShortestPath.run(graph, new Metrics());
            for (int vertex = 0; vertex < graph.n; vertex++) {
                System.out.println(vertex + ": distance=" + result.distance[vertex]
                        + ", path=" + result.pathTo(vertex));
            }
        } catch (IllegalArgumentException exception) {
            System.out.println(exception.getMessage());
        }
    }
}

package algorithms;

import graph.Edge;
import graph.Graph;
import java.util.Arrays;
import metrics.Metrics;

public class DagShortestPath {
    public static PathResult run(Graph graph, Metrics metrics) {
        int[] order = Kahn.run(graph, new Metrics());
        long[] distance = new long[graph.n];
        int[] parent = new int[graph.n];
        Arrays.fill(distance, Long.MAX_VALUE);
        Arrays.fill(parent, -1);
        for (int vertex = 0; vertex < graph.n; vertex++) {
            if (graph.indegree[vertex] == 0) {
                distance[vertex] = 0;
            }
        }
        for (int vertex : order) {
            for (Edge edge : graph.adj.get(vertex)) {
                metrics.edgeScans++;
                if (distance[vertex] != Long.MAX_VALUE) {
                    long candidate = distance[vertex] + edge.w;
                    if (candidate < distance[edge.v]) {
                        distance[edge.v] = candidate;
                        parent[edge.v] = vertex;
                    }
                }
            }
        }
        return new PathResult(distance, parent);
    }
}

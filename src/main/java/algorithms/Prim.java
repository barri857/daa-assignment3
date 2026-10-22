package algorithms;

import graph.Edge;
import graph.Graph;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import metrics.Metrics;

public class Prim {
    public static MstResult run(Graph graph, Metrics metrics) {
        if (graph.directed) {
            throw new IllegalArgumentException("Prim requires an undirected graph");
        }
        PriorityQueue<Edge> queue = new PriorityQueue<>((a, b) -> {
            metrics.comparisons++;
            return Integer.compare(a.w, b.w);
        });
        boolean[] visited = new boolean[graph.n];
        List<Edge> selected = new ArrayList<>();
        long weight = 0;
        int components = 0;
        for (int start = 0; start < graph.n; start++) {
            if (visited[start]) {
                continue;
            }
            components++;
            visit(graph, start, visited, queue, metrics);
            while (!queue.isEmpty()) {
                Edge edge = queue.poll();
                if (visited[edge.v]) {
                    continue;
                }
                selected.add(edge);
                weight += edge.w;
                visit(graph, edge.v, visited, queue, metrics);
            }
        }
        return new MstResult(weight, selected, components);
    }

    private static void visit(Graph graph, int vertex, boolean[] visited,
            PriorityQueue<Edge> queue, Metrics metrics) {
        visited[vertex] = true;
        for (Edge edge : graph.adj.get(vertex)) {
            metrics.edgeScans++;
            if (!visited[edge.v]) {
                queue.add(edge);
            }
        }
    }
}

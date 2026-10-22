package algorithms;

import graph.Edge;
import graph.Graph;
import java.util.ArrayList;
import java.util.List;
import metrics.Metrics;

public class Kruskal {
    public static MstResult run(Graph graph, Metrics metrics) {
        if (graph.directed) {
            throw new IllegalArgumentException("Kruskal requires an undirected graph");
        }
        List<Edge> sorted = new ArrayList<>(graph.edges);
        sorted.sort((a, b) -> {
            metrics.comparisons++;
            return Integer.compare(a.w, b.w);
        });
        DSU dsu = new DSU(graph.n, metrics);
        List<Edge> selected = new ArrayList<>();
        long weight = 0;
        for (Edge edge : sorted) {
            if (selected.size() == graph.n - 1) {
                break;
            }
            metrics.edgeScans++;
            if (dsu.union(edge.u, edge.v)) {
                selected.add(edge);
                weight += edge.w;
            }
        }
        return new MstResult(weight, selected, dsu.components());
    }
}

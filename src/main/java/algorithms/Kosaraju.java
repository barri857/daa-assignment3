package algorithms;

import graph.Edge;
import graph.Graph;
import java.util.Arrays;
import metrics.Metrics;

public class Kosaraju {
    public static SccResult run(Graph graph, Metrics metrics) {
        if (!graph.directed) {
            throw new IllegalArgumentException("Kosaraju requires a directed graph");
        }
        boolean[] visited = new boolean[graph.n];
        int[] stack = new int[graph.n];
        int[] nextEdge = new int[graph.n];
        int[] order = new int[graph.n];
        int finished = 0;
        for (int start = 0; start < graph.n; start++) {
            if (visited[start]) {
                continue;
            }
            int depth = 1;
            stack[0] = start;
            visited[start] = true;
            metrics.maxDepth = Math.max(metrics.maxDepth, depth);
            while (depth > 0) {
                int vertex = stack[depth - 1];
                if (nextEdge[vertex] < graph.adj.get(vertex).size()) {
                    Edge edge = graph.adj.get(vertex).get(nextEdge[vertex]++);
                    metrics.edgeScans++;
                    if (!visited[edge.v]) {
                        visited[edge.v] = true;
                        stack[depth++] = edge.v;
                        metrics.maxDepth = Math.max(metrics.maxDepth, depth);
                    }
                } else {
                    order[finished++] = vertex;
                    depth--;
                }
            }
        }
        Graph reversed = graph.reversed();
        int[] component = new int[graph.n];
        Arrays.fill(component, -1);
        Arrays.fill(nextEdge, 0);
        int count = 0;
        for (int i = finished - 1; i >= 0; i--) {
            int start = order[i];
            if (component[start] != -1) {
                continue;
            }
            int depth = 1;
            stack[0] = start;
            component[start] = count;
            metrics.maxDepth = Math.max(metrics.maxDepth, depth);
            while (depth > 0) {
                int vertex = stack[depth - 1];
                if (nextEdge[vertex] < reversed.adj.get(vertex).size()) {
                    Edge edge = reversed.adj.get(vertex).get(nextEdge[vertex]++);
                    metrics.edgeScans++;
                    if (component[edge.v] == -1) {
                        component[edge.v] = count;
                        stack[depth++] = edge.v;
                        metrics.maxDepth = Math.max(metrics.maxDepth, depth);
                    }
                } else {
                    depth--;
                }
            }
            count++;
        }
        return new SccResult(component, count);
    }
}

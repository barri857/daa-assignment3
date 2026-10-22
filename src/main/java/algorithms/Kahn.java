package algorithms;

import graph.Edge;
import graph.Graph;
import java.util.ArrayDeque;
import java.util.Queue;
import metrics.Metrics;

public class Kahn {
    public static int[] run(Graph graph, Metrics metrics) {
        if (!graph.directed) {
            throw new IllegalArgumentException("Kahn requires a directed graph");
        }
        int[] indegree = graph.indegree.clone();
        Queue<Integer> queue = new ArrayDeque<>();
        for (int vertex = 0; vertex < graph.n; vertex++) {
            if (indegree[vertex] == 0) {
                queue.add(vertex);
            }
        }
        int[] order = new int[graph.n];
        int count = 0;
        while (!queue.isEmpty()) {
            int vertex = queue.remove();
            order[count++] = vertex;
            for (Edge edge : graph.adj.get(vertex)) {
                metrics.edgeScans++;
                indegree[edge.v]--;
                if (indegree[edge.v] == 0) {
                    queue.add(edge.v);
                }
            }
        }
        if (count != graph.n) {
            throw new IllegalArgumentException("The graph contains a cycle");
        }
        return order;
    }

    public static boolean isValid(Graph graph, int[] order) {
        if (!graph.directed || order == null || order.length != graph.n) {
            return false;
        }
        int[] position = new int[graph.n];
        boolean[] found = new boolean[graph.n];
        for (int i = 0; i < order.length; i++) {
            int vertex = order[i];
            if (vertex < 0 || vertex >= graph.n || found[vertex]) {
                return false;
            }
            found[vertex] = true;
            position[vertex] = i;
        }
        for (Edge edge : graph.edges) {
            if (position[edge.u] >= position[edge.v]) {
                return false;
            }
        }
        return true;
    }
}

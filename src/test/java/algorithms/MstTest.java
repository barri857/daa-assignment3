package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MstTest {
    @Test
    void connectedExampleHasExpectedWeight() throws Exception {
        Graph graph = Graph.read(Path.of("data/connected.txt"));
        var kruskal = Kruskal.run(graph, new Metrics());
        var prim = Prim.run(graph, new Metrics());
        assertEquals(13, kruskal.weight);
        assertEquals(13, prim.weight);
        assertEquals(1, kruskal.components);
        checkForest(graph, kruskal.edges, kruskal.weight, 1);
        checkForest(graph, prim.edges, prim.weight, 1);
    }

    @Test
    void oneHundredRandomConnectedGraphsAgree() {
        Random random = new Random(2026);
        for (int test = 0; test < 100; test++) {
            int n = 2 + random.nextInt(79);
            Graph graph = new Graph(n, false);
            for (int v = 1; v < n; v++) {
                graph.addEdge(random.nextInt(v), v, random.nextInt(101) - 50);
            }
            for (int i = 0; i < 4 * n; i++) {
                graph.addEdge(random.nextInt(n), random.nextInt(n), random.nextInt(101) - 50);
            }
            var kruskal = Kruskal.run(graph, new Metrics());
            var prim = Prim.run(graph, new Metrics());
            assertEquals(kruskal.weight, prim.weight, "Graph " + test);
            assertEquals(n - 1, kruskal.edges.size());
            assertEquals(n - 1, prim.edges.size());
            assertEquals(1, kruskal.components);
            assertEquals(1, prim.components);
            checkForest(graph, kruskal.edges, kruskal.weight, 1);
            checkForest(graph, prim.edges, prim.weight, 1);
        }
    }

    @Test
    void disconnectedExampleProducesMinimumForest() throws Exception {
        Graph graph = Graph.read(Path.of("data/disconnected.txt"));
        var kruskal = Kruskal.run(graph, new Metrics());
        var prim = Prim.run(graph, new Metrics());
        assertEquals(-3, kruskal.weight);
        assertEquals(-3, prim.weight);
        assertEquals(4, kruskal.components);
        assertEquals(4, prim.components);
        checkForest(graph, kruskal.edges, kruskal.weight, 4);
        checkForest(graph, prim.edges, prim.weight, 4);
    }

    @Test
    void emptyAndIsolatedGraphsHaveNoTreeEdges() {
        for (int n : new int[]{0, 1, 5}) {
            Graph graph = new Graph(n, false);
            var kruskal = Kruskal.run(graph, new Metrics());
            var prim = Prim.run(graph, new Metrics());
            assertEquals(0, kruskal.weight);
            assertEquals(0, prim.weight);
            assertEquals(n, kruskal.components);
            assertEquals(n, prim.components);
            assertTrue(kruskal.edges.isEmpty());
            assertTrue(prim.edges.isEmpty());
        }
    }

    @Test
    void treeWeightsUseLongArithmetic() {
        Graph graph = new Graph(3, false);
        graph.addEdge(0, 1, Integer.MAX_VALUE);
        graph.addEdge(1, 2, Integer.MAX_VALUE);
        assertEquals(2L * Integer.MAX_VALUE, Kruskal.run(graph, new Metrics()).weight);
        assertEquals(2L * Integer.MAX_VALUE, Prim.run(graph, new Metrics()).weight);
    }

    private void checkForest(Graph graph, List<Edge> edges, long weight, int components) {
        int[] labels = new int[graph.n];
        for (int i = 0; i < graph.n; i++) {
            labels[i] = i;
        }
        long total = 0;
        for (Edge edge : edges) {
            assertTrue(graph.edges.stream().anyMatch(e -> e.w == edge.w
                    && ((e.u == edge.u && e.v == edge.v) || (e.u == edge.v && e.v == edge.u))));
            assertNotEquals(labels[edge.u], labels[edge.v]);
            int old = labels[edge.v];
            int next = labels[edge.u];
            for (int i = 0; i < labels.length; i++) {
                if (labels[i] == old) {
                    labels[i] = next;
                }
            }
            total += edge.w;
        }
        for (Edge edge : graph.edges) {
            assertEquals(labels[edge.u], labels[edge.v]);
        }
        assertEquals(graph.n - components, edges.size());
        assertEquals(weight, total);
    }
}

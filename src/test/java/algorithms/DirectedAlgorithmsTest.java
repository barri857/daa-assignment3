package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DirectedAlgorithmsTest {
    @Test
    void cyclicExampleHasFourComponents() throws Exception {
        Graph graph = Graph.read(Path.of("data/cyclic.txt"));
        Metrics metrics = new Metrics();
        var result = Kosaraju.run(graph, metrics);
        assertEquals(4, result.count);
        assertEquals(result.component[0], result.component[1]);
        assertEquals(result.component[1], result.component[2]);
        assertEquals(result.component[3], result.component[4]);
        assertNotEquals(result.component[2], result.component[3]);
        assertNotEquals(result.component[4], result.component[5]);
        assertNotEquals(result.component[5], result.component[6]);
        assertEquals(2L * graph.edges.size(), metrics.edgeScans);
        assertThrows(IllegalArgumentException.class, () -> Kahn.run(graph, new Metrics()));
        assertThrows(IllegalArgumentException.class, () -> DagShortestPath.run(graph, new Metrics()));
    }

    @Test
    void randomSccResultsMatchForwardAndReverseBfs() {
        Random random = new Random(5107);
        for (int test = 0; test < 45; test++) {
            int n = test == 0 ? 200 : 1 + random.nextInt(200);
            Graph graph = new Graph(n, true);
            for (int i = 0; i < 3 * n; i++) {
                graph.addEdge(random.nextInt(n), random.nextInt(n), random.nextInt(21) - 10);
            }
            boolean[][] forward = reachability(graph, false);
            boolean[][] reverse = reachability(graph, true);
            Metrics metrics = new Metrics();
            var result = Kosaraju.run(graph, metrics);
            HashSet<Integer> components = new HashSet<>();
            for (int u = 0; u < n; u++) {
                components.add(result.component[u]);
                for (int v = 0; v < n; v++) {
                    assertEquals(forward[u][v] && reverse[u][v],
                            result.component[u] == result.component[v], "Graph " + test);
                }
            }
            assertEquals(components.size(), result.count);
            assertEquals(2L * graph.edges.size(), metrics.edgeScans);
        }
    }

    @Test
    void oneHundredThousandVertexPathIsStackSafe() {
        int n = 100_000;
        Graph graph = new Graph(n, true);
        for (int v = 1; v < n; v++) {
            graph.addEdge(v - 1, v, 1);
        }
        Metrics metrics = new Metrics();
        var result = Kosaraju.run(graph, metrics);
        assertEquals(n, result.count);
        assertEquals(2L * (n - 1), metrics.edgeScans);
        for (int v = 1; v < n; v++) {
            assertNotEquals(result.component[v - 1], result.component[v]);
        }
        assertTrue(metrics.maxDepth > 0);
        assertTrue(metrics.maxDepth <= n);
    }

    @Test
    void dagExampleHasExpectedDistancesAndPaths() throws Exception {
        Graph graph = Graph.read(Path.of("data/dag.txt"));
        Metrics topologicalMetrics = new Metrics();
        int[] order = Kahn.run(graph, topologicalMetrics);
        assertTrue(Kahn.isValid(graph, order));
        checkOrder(graph, order);
        assertEquals(graph.edges.size(), topologicalMetrics.edgeScans);
        Metrics pathMetrics = new Metrics();
        var result = DagShortestPath.run(graph, pathMetrics);
        assertArrayEquals(new long[]{0, 0, 1, -2, 0, 7, -3, 0}, result.distance);
        assertEquals(List.of(1, 5, 6), result.pathTo(6));
        assertEquals(List.of(1, 2, 3, 4), result.pathTo(4));
        assertEquals(List.of(7), result.pathTo(7));
        assertEquals(3, result.sum());
        assertEquals(graph.edges.size(), pathMetrics.edgeScans);
        checkPaths(graph, result.distance, result.parent, result);
    }

    @Test
    void randomDagsMatchBellmanFord() {
        Random random = new Random(7019);
        for (int test = 0; test < 100; test++) {
            int n = 1 + random.nextInt(90);
            int[] vertices = new int[n];
            for (int i = 0; i < n; i++) {
                vertices[i] = i;
            }
            for (int i = n - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int temp = vertices[i];
                vertices[i] = vertices[j];
                vertices[j] = temp;
            }
            Graph graph = new Graph(n, true);
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (random.nextDouble() < 0.12) {
                        graph.addEdge(vertices[i], vertices[j], random.nextInt(61) - 30);
                    }
                }
            }
            Metrics topologicalMetrics = new Metrics();
            int[] order = Kahn.run(graph, topologicalMetrics);
            checkOrder(graph, order);
            assertTrue(Kahn.isValid(graph, order));
            assertEquals(graph.edges.size(), topologicalMetrics.edgeScans);
            Metrics pathMetrics = new Metrics();
            var result = DagShortestPath.run(graph, pathMetrics);
            assertArrayEquals(bellmanFord(graph), result.distance, "Graph " + test);
            assertEquals(graph.edges.size(), pathMetrics.edgeScans);
            checkPaths(graph, result.distance, result.parent, result);
        }
    }

    @Test
    void selfLoopAndCycleAreRejected() {
        Graph selfLoop = new Graph(1, true);
        selfLoop.addEdge(0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> Kahn.run(selfLoop, new Metrics()));
        Graph cycle = new Graph(4, true);
        cycle.addEdge(0, 1, 1);
        cycle.addEdge(1, 2, 1);
        cycle.addEdge(2, 0, 1);
        assertThrows(IllegalArgumentException.class, () -> Kahn.run(cycle, new Metrics()));
        assertThrows(IllegalArgumentException.class, () -> DagShortestPath.run(cycle, new Metrics()));
    }

    @Test
    void parallelEdgesAreAllProcessed() {
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, 8);
        graph.addEdge(0, 1, -4);
        graph.addEdge(1, 2, 2);
        Metrics metrics = new Metrics();
        int[] order = Kahn.run(graph, metrics);
        checkOrder(graph, order);
        assertEquals(3, metrics.edgeScans);
        var result = DagShortestPath.run(graph, new Metrics());
        assertArrayEquals(new long[]{0, -4, -2}, result.distance);
        assertEquals(List.of(0, 1, 2), result.pathTo(2));
    }

    @Test
    void emptyAndIsolatedDirectedGraphsWork() {
        for (int n : new int[]{0, 1, 6}) {
            Graph graph = new Graph(n, true);
            var scc = Kosaraju.run(graph, new Metrics());
            assertEquals(n, scc.count);
            assertEquals(n, Kahn.run(graph, new Metrics()).length);
            var paths = DagShortestPath.run(graph, new Metrics());
            assertArrayEquals(new long[n], paths.distance);
            assertEquals(0, paths.sum());
            for (int v = 0; v < n; v++) {
                assertEquals(List.of(v), paths.pathTo(v));
            }
        }
    }

    @Test
    void invalidTopologicalOrdersAreDetected() {
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        assertFalse(Kahn.isValid(graph, new int[]{2, 1, 0}));
        assertFalse(Kahn.isValid(graph, new int[]{0, 1}));
        assertFalse(Kahn.isValid(graph, new int[]{0, 0, 2}));
        assertFalse(Kahn.isValid(graph, new int[]{0, 1, 3}));
        assertFalse(Kahn.isValid(graph, new int[]{-1, 1, 2}));
    }

    @Test
    void distancesUseLongArithmetic() {
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, Integer.MIN_VALUE);
        graph.addEdge(1, 2, Integer.MIN_VALUE);
        var result = DagShortestPath.run(graph, new Metrics());
        assertEquals(2L * Integer.MIN_VALUE, result.distance[2]);
        assertEquals(3L * Integer.MIN_VALUE, result.sum());
    }

    private boolean[][] reachability(Graph graph, boolean reversed) {
        List<List<Integer>> adjacency = new ArrayList<>();
        for (int i = 0; i < graph.n; i++) {
            adjacency.add(new ArrayList<>());
        }
        for (Edge edge : graph.edges) {
            adjacency.get(reversed ? edge.v : edge.u).add(reversed ? edge.u : edge.v);
        }
        boolean[][] reached = new boolean[graph.n][graph.n];
        for (int source = 0; source < graph.n; source++) {
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            queue.add(source);
            reached[source][source] = true;
            while (!queue.isEmpty()) {
                int u = queue.remove();
                for (int v : adjacency.get(u)) {
                    if (!reached[source][v]) {
                        reached[source][v] = true;
                        queue.add(v);
                    }
                }
            }
        }
        return reached;
    }

    private long[] bellmanFord(Graph graph) {
        long[] distance = new long[graph.n];
        Arrays.fill(distance, Long.MAX_VALUE);
        for (int v = 0; v < graph.n; v++) {
            if (graph.indegree[v] == 0) {
                distance[v] = 0;
            }
        }
        for (int pass = 1; pass < graph.n; pass++) {
            boolean changed = false;
            for (Edge edge : graph.edges) {
                if (distance[edge.u] != Long.MAX_VALUE && distance[edge.u] + edge.w < distance[edge.v]) {
                    distance[edge.v] = distance[edge.u] + edge.w;
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
        return distance;
    }

    private void checkOrder(Graph graph, int[] order) {
        assertEquals(graph.n, order.length);
        int[] position = new int[graph.n];
        Arrays.fill(position, -1);
        for (int i = 0; i < order.length; i++) {
            assertTrue(order[i] >= 0 && order[i] < graph.n);
            assertEquals(-1, position[order[i]]);
            position[order[i]] = i;
        }
        for (Edge edge : graph.edges) {
            assertTrue(position[edge.u] < position[edge.v]);
        }
    }

    private void checkPaths(Graph graph, long[] distance, int[] parent, PathResult result) {
        for (int v = 0; v < graph.n; v++) {
            List<Integer> path = result.pathTo(v);
            assertFalse(path.isEmpty());
            assertEquals(v, path.get(path.size() - 1).intValue());
            assertEquals(path.size(), new HashSet<>(path).size());
            assertEquals(0, graph.indegree[path.get(0)]);
            assertEquals(-1, parent[path.get(0)]);
            long weight = 0;
            for (int i = 1; i < path.size(); i++) {
                int u = path.get(i - 1);
                int next = path.get(i);
                assertEquals(u, parent[next]);
                int edgeWeight = graph.edges.stream()
                        .filter(e -> e.u == u && e.v == next)
                        .mapToInt(e -> e.w).min().orElseThrow();
                weight += edgeWeight;
            }
            assertEquals(distance[v], weight);
        }
    }
}

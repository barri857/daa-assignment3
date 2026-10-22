package graph;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Graph {
    public final int n;
    public final boolean directed;
    public final List<Edge> edges = new ArrayList<>();
    public final List<List<Edge>> adj = new ArrayList<>();
    public final int[] indegree;

    public Graph(int n, boolean directed) {
        if (n < 0) {
            throw new IllegalArgumentException("Vertex count cannot be negative");
        }
        this.n = n;
        this.directed = directed;
        indegree = new int[n];
        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>());
        }
    }

    public void addEdge(int u, int v, int w) {
        if (u < 0 || u >= n || v < 0 || v >= n) {
            throw new IllegalArgumentException("Vertex is outside the graph");
        }
        Edge edge = new Edge(u, v, w);
        edges.add(edge);
        adj.get(u).add(edge);
        indegree[v]++;
        if (!directed) {
            adj.get(v).add(new Edge(v, u, w));
            indegree[u]++;
        }
    }

    public Graph reversed() {
        Graph result = new Graph(n, directed);
        for (Edge edge : edges) {
            result.addEdge(edge.v, edge.u, edge.w);
        }
        return result;
    }

    public static Graph read(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line = reader.readLine();
            if (line == null) {
                throw new IllegalArgumentException("The graph file is empty");
            }
            String[] header = line.trim().split("\\s+");
            if (header.length != 3) {
                throw new IllegalArgumentException("Expected: n m directed|undirected");
            }
            int n = Integer.parseInt(header[0]);
            int m = Integer.parseInt(header[1]);
            if (m < 0) {
                throw new IllegalArgumentException("Edge count cannot be negative");
            }
            if (!header[2].equals("directed") && !header[2].equals("undirected")) {
                throw new IllegalArgumentException("Graph type must be directed or undirected");
            }
            Graph graph = new Graph(n, header[2].equals("directed"));
            for (int i = 0; i < m; i++) {
                line = reader.readLine();
                if (line == null) {
                    throw new IllegalArgumentException("Fewer edges than declared");
                }
                String[] values = line.trim().split("\\s+");
                if (values.length != 3) {
                    throw new IllegalArgumentException("Expected an edge: u v w");
                }
                graph.addEdge(Integer.parseInt(values[0]), Integer.parseInt(values[1]),
                        Integer.parseInt(values[2]));
            }
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    throw new IllegalArgumentException("More edges than declared");
                }
            }
            return graph;
        }
    }
}

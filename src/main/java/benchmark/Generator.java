package benchmark;

import graph.Graph;
import java.util.Random;

public class Generator {
    public static Graph undirected(int n, int m) {
        checkSize(n, m);
        if (m < n - 1) {
            throw new IllegalArgumentException("A connected graph needs at least n - 1 edges");
        }
        Random random = new Random(42);
        int[] permutation = shuffle(n, random);
        Graph graph = new Graph(n, false);
        for (int i = 1; i < n; i++) {
            int a = permutation[random.nextInt(i)];
            int b = permutation[i];
            graph.addEdge(a, b, 1 + random.nextInt(1000));
        }
        for (int i = n - 1; i < m; i++) {
            int a = random.nextInt(n);
            int b = random.nextInt(n);
            while (a == b) {
                b = random.nextInt(n);
            }
            graph.addEdge(a, b, 1 + random.nextInt(1000));
        }
        return graph;
    }

    public static Graph directed(int n, int m) {
        checkSize(n, m);
        Random random = new Random(42);
        Graph graph = new Graph(n, true);
        for (int i = 0; i < m; i++) {
            int a = random.nextInt(n);
            int b = random.nextInt(n);
            while (a == b) {
                b = random.nextInt(n);
            }
            graph.addEdge(a, b, 1 + random.nextInt(1000));
        }
        return graph;
    }

    public static Graph dag(int n, int m) {
        checkSize(n, m);
        Random random = new Random(42);
        int[] rank = shuffle(n, random);
        Graph graph = new Graph(n, true);
        for (int i = 0; i < m; i++) {
            int a = random.nextInt(n);
            int b = random.nextInt(n);
            while (a == b) {
                b = random.nextInt(n);
            }
            if (rank[a] > rank[b]) {
                int temporary = a;
                a = b;
                b = temporary;
            }
            graph.addEdge(a, b, 1 + random.nextInt(1000));
        }
        return graph;
    }

    private static int[] shuffle(int n, Random random) {
        int[] values = new int[n];
        for (int i = 0; i < n; i++) {
            values[i] = i;
        }
        for (int i = n - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temporary = values[i];
            values[i] = values[j];
            values[j] = temporary;
        }
        return values;
    }

    private static void checkSize(int n, int m) {
        if (n < 1 || m < 0 || n == 1 && m > 0) {
            throw new IllegalArgumentException("Invalid graph size");
        }
    }
}

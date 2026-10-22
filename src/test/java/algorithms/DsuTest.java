package algorithms;

import metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DsuTest {
    @Test
    void randomOperationsMatchSimpleComponentLabels() {
        Random random = new Random(12345);
        for (int test = 0; test < 50; test++) {
            int n = 1 + random.nextInt(80);
            DSU dsu = new DSU(n, new Metrics());
            int[] labels = new int[n];
            for (int i = 0; i < n; i++) {
                labels[i] = i;
            }
            int components = n;
            for (int operation = 0; operation < 500; operation++) {
                int a = random.nextInt(n);
                int b = random.nextInt(n);
                boolean same = labels[a] == labels[b];
                if (random.nextBoolean()) {
                    assertEquals(!same, dsu.union(a, b));
                    if (!same) {
                        int old = labels[b];
                        int next = labels[a];
                        for (int i = 0; i < n; i++) {
                            if (labels[i] == old) {
                                labels[i] = next;
                            }
                        }
                        components--;
                    }
                } else {
                    assertEquals(same, dsu.find(a) == dsu.find(b));
                }
                assertEquals(components, dsu.components());
            }
            for (int a = 0; a < n; a++) {
                for (int b = 0; b < n; b++) {
                    assertEquals(labels[a] == labels[b], dsu.find(a) == dsu.find(b));
                }
            }
        }
    }

    @Test
    void repeatedUnionsAndSelfUnionsDoNotChangeComponentCount() {
        DSU dsu = new DSU(4, new Metrics());
        assertFalse(dsu.union(0, 0));
        assertTrue(dsu.union(0, 1));
        assertFalse(dsu.union(1, 0));
        assertTrue(dsu.union(2, 3));
        assertTrue(dsu.union(1, 3));
        assertEquals(1, dsu.components());
        for (int v = 0; v < 4; v++) {
            assertEquals(dsu.find(0), dsu.find(v));
        }
    }

    @Test
    void makeSetResetsAllComponents() {
        DSU dsu = new DSU(3, new Metrics());
        dsu.union(0, 1);
        dsu.makeSet(5);
        assertEquals(5, dsu.components());
        for (int v = 0; v < 5; v++) {
            assertEquals(v, dsu.find(v));
        }
        dsu.makeSet(0);
        assertEquals(0, dsu.components());
    }

    @Test
    void countersIncludeFindCallsAndOnlySuccessfulUnions() {
        Metrics metrics = new Metrics();
        DSU dsu = new DSU(3, metrics);
        dsu.find(0);
        dsu.union(0, 1);
        dsu.union(1, 0);
        assertEquals(5, metrics.findCalls);
        assertEquals(1, metrics.unionCalls);
        assertEquals(6, metrics.dsuOps());
    }
}

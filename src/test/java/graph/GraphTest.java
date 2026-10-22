package graph;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GraphTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void undirectedEdgesHaveTwoAdjacencyEntries() throws Exception {
        Graph graph = Graph.read(Path.of("data/disconnected.txt"));
        assertEquals(7, graph.n);
        assertFalse(graph.directed);
        assertEquals(7, graph.edges.size());
        assertEquals(14, graph.adj.stream().mapToInt(list -> list.size()).sum());
        assertEquals(0, graph.adj.get(6).size());
        assertEquals(3, graph.adj.get(4).size());
    }

    @Test
    void reversingDirectedGraphPreservesWeightsAndDuplicates() throws Exception {
        Graph graph = Graph.read(Path.of("data/cyclic.txt"));
        Graph reversed = graph.reversed();
        assertTrue(reversed.directed);
        assertEquals(graph.n, reversed.n);
        assertEquals(graph.edges.size(), reversed.edges.size());
        for (int i = 0; i < graph.edges.size(); i++) {
            Edge original = graph.edges.get(i);
            Edge reverse = reversed.edges.get(i);
            assertEquals(original.u, reverse.v);
            assertEquals(original.v, reverse.u);
            assertEquals(original.w, reverse.w);
        }
        for (int v = 0; v < graph.n; v++) {
            assertEquals(graph.adj.get(v).size(), reversed.indegree[v]);
        }
    }

    @Test
    void malformedGraphFilesAreRejected() throws Exception {
        String[] inputs = {
                "",
                "3 1\n0 1 2\n",
                "3 1 unknown\n0 1 2\n",
                "3 -1 directed\n",
                "-1 0 directed\n",
                "3 2 directed\n0 1 2\n",
                "3 0 directed\n0 1 2\n",
                "3 1 directed\n0 3 2\n",
                "3 1 directed\n-1 2 2\n",
                "3 1 directed\n0 1\n",
                "3 1 directed\n0 1 weight\n"
        };
        for (int i = 0; i < inputs.length; i++) {
            Path file = temporaryDirectory.resolve("invalid-" + i + ".txt");
            Files.writeString(file, inputs[i]);
            assertThrows(IllegalArgumentException.class, () -> Graph.read(file));
        }
    }

    @Test
    void whitespaceAndEmptyGraphsAreAccepted() throws Exception {
        Path file = temporaryDirectory.resolve("empty.txt");
        Files.writeString(file, "  0   0   directed  \n\n");
        Graph graph = Graph.read(file);
        assertEquals(0, graph.n);
        assertTrue(graph.edges.isEmpty());
        assertTrue(graph.directed);
    }
}

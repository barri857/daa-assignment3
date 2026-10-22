package algorithms;

import graph.Edge;
import java.util.List;

public class MstResult {
    public final long weight;
    public final List<Edge> edges;
    public final int components;

    public MstResult(long weight, List<Edge> edges, int components) {
        this.weight = weight;
        this.edges = edges;
        this.components = components;
    }
}

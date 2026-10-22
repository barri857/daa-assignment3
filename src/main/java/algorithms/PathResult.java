package algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PathResult {
    public final long[] distance;
    public final int[] parent;

    public PathResult(long[] distance, int[] parent) {
        this.distance = distance;
        this.parent = parent;
    }

    public List<Integer> pathTo(int vertex) {
        if (vertex < 0 || vertex >= distance.length) {
            throw new IllegalArgumentException("Vertex is outside the graph");
        }
        List<Integer> path = new ArrayList<>();
        if (distance[vertex] == Long.MAX_VALUE) {
            return path;
        }
        while (vertex != -1) {
            path.add(vertex);
            vertex = parent[vertex];
        }
        Collections.reverse(path);
        return path;
    }

    public long sum() {
        long total = 0;
        for (long value : distance) {
            if (value != Long.MAX_VALUE) {
                total += value;
            }
        }
        return total;
    }
}

package algorithms;

public class SccResult {
    public final int[] component;
    public final int count;

    public SccResult(int[] component, int count) {
        this.component = component;
        this.count = count;
    }
}

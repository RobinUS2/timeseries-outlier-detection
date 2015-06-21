/**
 * Created by robin on 21/06/15.
 */
public class TimeserieOutlier {
    private long ts;
    private double val;

    public TimeserieOutlier(long ts, double val) {
        this.ts = ts;
        this.val = val;
    }

    public long getTs() {
        return ts;
    }
}

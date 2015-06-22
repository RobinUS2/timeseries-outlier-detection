/**
 * Created by robin on 21/06/15.
 */
public class TimeserieOutlier {
    private final long ts;
    private final double val;
    private final String analyzerName;

    public TimeserieOutlier(String analyzerName, long ts, double val) {
        this.analyzerName = analyzerName;
        this.ts = ts;
        this.val = val;
    }

    public long getTs() {
        return ts;
    }

    public String getAnalyzerName() {
        return analyzerName;
    }

    public double getVal() {
        return val;
    }

}

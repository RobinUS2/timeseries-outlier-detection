/**
 * Created by robin on 21/06/15.
 */
public class TimeserieOutlier {
    private final long ts;
    private final double val;
    private final double valLeftBound;
    private final double valRightBound;
    private final double expectedValue;
    private final String analyzerName;

    public TimeserieOutlier(String analyzerName, long ts, double val, double expectedValue, double valLeftBound, double valRightBound) {
        this.analyzerName = analyzerName;
        this.ts = ts;
        this.val = val;
        this.valLeftBound = valLeftBound;
        this.valRightBound = valRightBound;
        this.expectedValue = expectedValue;
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

    public double getExpectedVal() { return expectedValue; }

    public double getLeftBound() {
        return valLeftBound;
    }

    public double getRightBound() {
        return valRightBound;
    }

}

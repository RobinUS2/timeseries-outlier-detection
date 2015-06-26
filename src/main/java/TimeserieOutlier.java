/**
 * Created by robin on 21/06/15.
 */
public class TimeserieOutlier {
    private final long ts;
    private final double val;
    private final double valLeftBound;
    private final double valRightBound;
    private final double expectedValue;
    private final AbstractTimeserieAnalyzer analyzer;

    public TimeserieOutlier(AbstractTimeserieAnalyzer analyzer, long ts, double val, double expectedValue, double valLeftBound, double valRightBound) {
        this.analyzer = analyzer;
        this.ts = ts;
        this.val = val;
        this.valLeftBound = valLeftBound;
        this.valRightBound = valRightBound;
        this.expectedValue = expectedValue;
    }

    public long getTs() {
        return ts;
    }

    public AbstractTimeserieAnalyzer getAnalyzer() {
        return analyzer;
    }

    public String getAnalyzerName() {
        return analyzer.getClass().getSimpleName();
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

    public double getOutlierMagnitude() {
        if (val < valLeftBound) {
            return 1.0 + Math.max(0.0D, Math.log(Math.abs((valLeftBound - val) / valLeftBound)));
        }
        if (val > valRightBound) {
            return 1.0 + Math.max(0.0D, Math.log(Math.abs((valRightBound - val) / valRightBound)));
        }
        return 0.0D;
    }

}

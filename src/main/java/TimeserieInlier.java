/**
 * Created by robin on 24/06/15.
 */
public class TimeserieInlier extends TimeserieOutlier {
    public TimeserieInlier(String analyzerName, long ts, double val, double expectedValue, double valLeftBound, double valRightBound) {
        super(analyzerName, ts, val, expectedValue, valLeftBound, valRightBound);
    }
}

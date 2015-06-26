package nl.us2.timeseriesoutlierdetection;

/**
 * Created by robin on 24/06/15.
 */
public class TimeserieInlier extends TimeserieOutlier {
    public TimeserieInlier(AbstractTimeserieAnalyzer analyzer, long ts, double val, double expectedValue, double valLeftBound, double valRightBound) {
        super(analyzer, ts, val, expectedValue, valLeftBound, valRightBound);
    }
}

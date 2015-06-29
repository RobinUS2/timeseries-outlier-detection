package nl.us2.timeseriesoutlierdetection;

/**
 * Created by robin on 24/06/15.
 */
public class TimeserieInlier extends TimeserieOutlier {
    public TimeserieInlier(AbstractTimeserieAnalyzer analyzer, String serieName, long ts, double val, double expectedValue, double valLeftBound, double valRightBound) {
        super(analyzer, serieName, ts, val, expectedValue, valLeftBound, valRightBound);
    }
}

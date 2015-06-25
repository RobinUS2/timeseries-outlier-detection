/**
 * Created by robin on 21/06/15.
 */
public class AbstractTimeserieAnalyzer {
    public void log (String msg) {
        System.out.println(msg);
    }

    public TimeserieAnalyzerResult newResultSet() {
        return new TimeserieAnalyzerResult();
    }
    protected static final int DEFAULT_INLIER_SCORE = 1;
    protected static final int DEFAULT_OUTLIER_SCORE = 3;
    protected int INLIER_SCORE = AbstractTimeserieAnalyzer.DEFAULT_INLIER_SCORE; // Decrements the score if an inlier is found
    protected int OUTLIER_SCORE = AbstractTimeserieAnalyzer.DEFAULT_OUTLIER_SCORE; // Increments the score if an outlier is found
}

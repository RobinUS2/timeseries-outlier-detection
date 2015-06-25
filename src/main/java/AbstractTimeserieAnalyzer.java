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

    public int getInlierScore() {
        return DEFAULT_INLIER_SCORE;
    }

    public int getOutlierScore() {
        return DEFAULT_OUTLIER_SCORE;
    }
}

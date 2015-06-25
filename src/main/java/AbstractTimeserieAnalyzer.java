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

    protected int INLIER_SCORE = 1; // Decrements the score if an inlier is found
    protected int OUTLIER_SCORE = 3; // Increments the score if an outlier is found
}

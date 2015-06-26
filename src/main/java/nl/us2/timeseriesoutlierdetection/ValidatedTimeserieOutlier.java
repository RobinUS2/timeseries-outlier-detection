package nl.us2.timeseriesoutlierdetection;

/**
 * Created by robin on 25/06/15.
 */
public class ValidatedTimeserieOutlier {
    private long ts;
    private double score;
    public ValidatedTimeserieOutlier(long ts, double score) {
        this.ts = ts;
        this.score = score;
    }

    public long getTs() {
        return ts;
    }

    public double getScore() {
        return score;
    }
}

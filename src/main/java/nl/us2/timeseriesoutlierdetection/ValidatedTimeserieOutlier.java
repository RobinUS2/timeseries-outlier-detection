package nl.us2.timeseriesoutlierdetection;

import com.google.gson.JsonObject;

/**
 * Created by robin on 25/06/15.
 */
public class ValidatedTimeserieOutlier {
    private long ts;
    private double score;
    private JsonObject details;

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

    public void setDetails(JsonObject x) {
        details = x;
    }

    public JsonObject getDetails() {
        return details;
    }
}

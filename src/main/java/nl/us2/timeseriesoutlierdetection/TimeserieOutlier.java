package nl.us2.timeseriesoutlierdetection;

import com.google.gson.JsonObject;

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
    private final String serieName;
    public static final double DEFAULT_OUTLIER_MAGNITUDE = 1.0D;

    public TimeserieOutlier(AbstractTimeserieAnalyzer analyzer, String serieName, long ts, double val, double expectedValue, double valLeftBound, double valRightBound) {
        this.serieName = serieName;
        this.analyzer = analyzer;
        this.ts = ts;
        this.val = val;
        this.valLeftBound = valLeftBound;
        this.valRightBound = valRightBound;
        this.expectedValue = expectedValue;
    }

    public String getSerieName() { return serieName; }

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
        double magnitude = DEFAULT_OUTLIER_MAGNITUDE;
        if (val < valLeftBound) {
            magnitude = DEFAULT_OUTLIER_MAGNITUDE + Math.max(0.0D, Math.log(Math.abs((valLeftBound - val) / valLeftBound)));
        }
        if (val > valRightBound) {
            magnitude = DEFAULT_OUTLIER_MAGNITUDE + Math.max(0.0D, Math.log(Math.abs((valRightBound - val) / valRightBound)));
        }
        // Default magnitude
        if (Double.isInfinite(magnitude) || Double.isNaN(magnitude)) {
            return DEFAULT_OUTLIER_MAGNITUDE;
        }
        // Magnitude can not be more than the regular score, effectively doubling the score
        magnitude = Double.min(magnitude, getAnalyzer().getOutlierScore());

        // Final score
        return magnitude;
    }
    public JsonObject getJsonObjectWithDetails() {
        JsonObject o = new JsonObject();
        o.addProperty("serie_name", getSerieName());
        o.addProperty("timestamp", getTs());
        o.addProperty("analyzer_name", getAnalyzerName());
        o.addProperty("measured_value", getVal());
        o.addProperty("expected_value", getExpectedVal());
        o.addProperty("expected_value_left_bound", getLeftBound());
        o.addProperty("expected_value_right_bound", getRightBound());
        o.addProperty("outlier_magnitude", getOutlierMagnitude());
        return o;
    }

}

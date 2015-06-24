import java.util.ArrayList;
import java.util.List;

/**
 * Created by robin on 24/06/15.
 */
public class TimeserieAnalyzerResult {
    private List<TimeserieOutlier> outliers; // Weird points
    private List<TimeserieInlier> inliers; // Points that are confirmed to be OK, are used to supress possible warnings

    public TimeserieAnalyzerResult() {
        outliers = new ArrayList<TimeserieOutlier>();
        inliers = new ArrayList<TimeserieInlier>();
    }

    public void addOutlier(TimeserieOutlier x) {
        outliers.add(x);
    }

    public void addInlier(TimeserieInlier x) {
        inliers.add(x);
    }

    public List<TimeserieOutlier> getOutliers() {
        return outliers;
    }

    public List<TimeserieInlier> getInliers() {
        return inliers;
    }

}

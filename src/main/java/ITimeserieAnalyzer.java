import java.util.HashMap;
import java.util.List;

/**
 * Created by robin on 21/06/15.
 */
public interface ITimeserieAnalyzer {
     List<TimeserieOutlier> analyze(HashMap<String, Timeseries> timeseries);
}

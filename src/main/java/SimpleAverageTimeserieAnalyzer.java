import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by robin on 21/06/15.
 */
public class SimpleAverageTimeserieAnalyzer implements ITimeserieAnalyzer {
    public List<TimeserieOutlier> analyze(HashMap<String, Timeseries> timeseries) {
        for (Map.Entry<String, Timeseries> kv : timeseries.entrySet()) {
            double total = 0.0D;
            long count = 0L;
            for (double val : kv.getValue().getData().values()) {
                total += val;
                count++;
            }
            double avg = total / (double)count;
            System.out.println("Average = " + avg);
        }
        return null;
    }
}

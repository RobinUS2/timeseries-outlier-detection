import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by robin on 21/06/15.
 */
public class SimpleAverageTimeserieAnalyzer extends AbstractTimeserieAnalyzer implements ITimeserieAnalyzer {
    public List<TimeserieOutlier> analyze(HashMap<String, Timeseries> timeseries) {
        List<TimeserieOutlier> outliers = new ArrayList<TimeserieOutlier>();
        for (Map.Entry<String, Timeseries> kv : timeseries.entrySet()) {
            // Average
            double total = 0.0D;
            long count = 0L;
            for (double val : kv.getValue().getData().values()) {
                total += val;
                count++;
            }
            double avg = total / (double)count;
            log("Average = " + avg);

            // Stddev
            double msqT = 0.0D;
            for (double val : kv.getValue().getData().values()) {
                double msq = Math.pow(val - avg, 2.0D);
                msqT += msq;
            }
            double msqAvg = msqT / (double)count;
            double stdDev = Math.sqrt(msqAvg);
            log("Stddev = " + stdDev);

            // Detect outliers
            double maxStdDevMp = 1.0D;
            for (Map.Entry<Long, Double> tskv : kv.getValue().getData().entrySet()) {
                if (tskv.getValue() > avg + (maxStdDevMp * stdDev) || tskv.getValue() < avg - (maxStdDevMp * stdDev)) {
                    TimeserieOutlier outlier = new TimeserieOutlier(tskv.getKey(), tskv.getValue());
                    outliers.add(outlier);
                }
            }
        }
        return outliers;
    }
}

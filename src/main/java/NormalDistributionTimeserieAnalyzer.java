import com.sun.javafx.tools.resource.DeployResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by robin on 21/06/15.
 */
public class NormalDistributionTimeserieAnalyzer extends AbstractTimeserieAnalyzer implements ITimeserieAnalyzer {
    public List<TimeserieOutlier> analyze(AbstractDataLoader dataLoader, HashMap<String, Timeseries> timeseries) {
        List<TimeserieOutlier> outliers = new ArrayList<TimeserieOutlier>();
        for (Map.Entry<String, Timeseries> kv : timeseries.entrySet()) {
            // Average
            double total = 0.0D;
            long count = 0L;
            for (double val : kv.getValue().getDataTrain().values()) {
                total += val;
                count++;
            }
            double avg = total / (double)count;
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Average = " + avg);

            // Stddev
            double msqT = 0.0D;
            for (double val : kv.getValue().getDataTrain().values()) {
                double msq = Math.pow(val - avg, 2.0D);
                msqT += msq;
            }
            double msqAvg = msqT / (double)count;
            double stdDev = Math.sqrt(msqAvg);
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Stddev = " + stdDev);

            // Is this filter reliable?
            double stdDevLim = 0.25 * avg; // @todo dynamic
            if (stdDev > stdDevLim || Double.isInfinite(avg) || Double.isNaN(stdDev)) {
                dataLoader.log(dataLoader.LOG_NOTICE, getClass().getSimpleName(), "Unreliable based on standard deviation average crosscheck (is " + stdDev + " exceeds " + stdDevLim + ")");
                return null;
            }

            // Detect outliers
            double maxStdDevMp = 1.0D;
            for (Map.Entry<Long, Double> tskv : kv.getValue().getDataClassify().entrySet()) {
                double val = tskv.getValue();
                double rightBound = avg + (maxStdDevMp * stdDev);
                double leftBound = avg - (maxStdDevMp * stdDev);
                if (val < leftBound || val > rightBound) {
                    TimeserieOutlier outlier = new TimeserieOutlier(this.getClass().getSimpleName(), tskv.getKey(), tskv.getValue(), leftBound, rightBound);
                    outliers.add(outlier);
                }
            }
        }
        return outliers;
    }
}

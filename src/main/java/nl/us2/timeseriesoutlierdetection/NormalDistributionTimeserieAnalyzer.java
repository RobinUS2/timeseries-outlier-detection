package nl.us2.timeseriesoutlierdetection;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by robin on 21/06/15.
 */
public class NormalDistributionTimeserieAnalyzer extends AbstractTimeserieAnalyzer implements ITimeserieAnalyzer {
    public TimeserieAnalyzerResult analyze(AbstractDataLoader dataLoader, HashMap<String, Timeseries> timeseries) {
        TimeserieAnalyzerResult res = new TimeserieAnalyzerResult();

        // Iterate series
        for (Map.Entry<String, Timeseries> kv : timeseries.entrySet()) {
            // Average
            double avg = kv.getValue().getTrainAvg();
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Average = " + avg);

            // Stddev
            double stdDev = kv.getValue().getTrainStdDev();
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Stddev = " + stdDev);

            // Is this filter reliable?
            double stdDevLim = 0.25 * avg; // @todo dynamic
            if (stdDev > stdDevLim || Double.isInfinite(avg) || Double.isNaN(stdDev)) {
                dataLoader.log(dataLoader.LOG_NOTICE, getClass().getSimpleName(), "Unreliable based on standard deviation average crosscheck (is " + stdDev + " exceeds " + stdDevLim + ")");
                continue;
            }

            // Detect outliers
            double maxStdDevMp = 1.0D;
            double maxErr = Math.max(maxStdDevMp * stdDev, 0.05 * avg); // 1x std deviation or 5% of average
            for (Map.Entry<Long, Double> tskv : kv.getValue().getDataClassify().entrySet()) {
                double val = tskv.getValue();
                double rb = avg + maxErr;
                double lb = avg - maxErr;
                if (val < lb || val > rb) {
                    TimeserieOutlier outlier = new TimeserieOutlier(this, tskv.getKey(), tskv.getValue(), avg, lb, rb);
                    if (!kv.getValue().validateOutlier(outlier)) {
                        continue;
                    }
                    res.addOutlier(outlier);
                } else {
                    res.addInlier(new TimeserieInlier(this, tskv.getKey(), tskv.getValue(), avg, lb, rb));
                }
            }
        }
        return res;
    }
}

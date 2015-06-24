import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by robin on 21/06/15.
 */
public class LogNormalDistributionTimeserieAnalyzer extends AbstractTimeserieAnalyzer implements ITimeserieAnalyzer {
    public TimeserieAnalyzerResult analyze(AbstractDataLoader dataLoader, HashMap<String, Timeseries> timeseries) {
        TimeserieAnalyzerResult res = new TimeserieAnalyzerResult();

        // Iterate series
        for (Map.Entry<String, Timeseries> kv : timeseries.entrySet()) {
            // Average
            double total = 0.0D;
            long count = 0L;
            for (double val : kv.getValue().getDataTrain().values()) {
                val = convertValue(val);
                total += val;
                count++;
            }
            double avg = total / (double)count;
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Average = " + avg);

            // Stddev
            double msqT = 0.0D;
            for (double val : kv.getValue().getDataTrain().values()) {
                val = convertValue(val);
                double msq = Math.pow(val - avg, 2.0D);
                msqT += msq;
            }
            double msqAvg = msqT / (double)count;
            double stdDev = Math.sqrt(msqAvg);
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Stddev = " + stdDev);

            // Is this filter reliable?
            double stdDevLim = 0.05 * avg; // @todo dynamic
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Stddev limit = " + stdDevLim);
            if (stdDev > stdDevLim) {
                dataLoader.log(dataLoader.LOG_NOTICE, getClass().getSimpleName(), "Unreliable based on standard deviation average crosscheck (is " + stdDev + " exceeds " + stdDevLim + ")");
                continue;
            }
            if (stdDev < 1 / Double.MAX_VALUE) {
                dataLoader.log(dataLoader.LOG_NOTICE, getClass().getSimpleName(), "Unreliable based on standard deviation crosscheck, deviation too low");
                continue;
            }

            // Detect outliers
            double maxStdDevMp = 1.0D;
            double maxErr = Math.max(maxStdDevMp * stdDev, 0.05 * avg); // 1x std deviation or 5% of average
            for (Map.Entry<Long, Double> tskv : kv.getValue().getDataClassify().entrySet()) {
                double val = convertValue(tskv.getValue());
                double rb = avg + maxErr;
                double lb = avg - maxErr;
                if (val < lb || val > rb) {
                    TimeserieOutlier outlier = new TimeserieOutlier(this, tskv.getKey(), val, avg, lb, rb);
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

    public double convertValue(double in) {
        double out = Math.log(in);
        if (Double.isInfinite(out)) {
            out = 1 / Double.MAX_VALUE; // Very small value
        }
        return out;
    }
}

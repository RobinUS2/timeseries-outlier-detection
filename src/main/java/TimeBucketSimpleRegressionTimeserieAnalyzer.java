import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by robin on 21/06/15.
 */
public class TimeBucketSimpleRegressionTimeserieAnalyzer extends AbstractTimeserieAnalyzer implements ITimeserieAnalyzer {
    public TimeserieAnalyzerResult analyze(AbstractDataLoader dataLoader, HashMap<String, Timeseries> timeseries) {
        TimeserieAnalyzerResult res = new TimeserieAnalyzerResult();

        // Iterate series
        for (Map.Entry<String, Timeseries> kv : timeseries.entrySet()) {
            ArrayList<Long> outliers = new ArrayList<Long>();
            for (int i = 0; i < 4; i++) {
                long windowSeconds = 0;
                long targetSeconds = 0;
                int minBuckets = 0;
                int bucketModulo = 0;
                switch (i) {
                    case 0:
                        // Minutely
                        windowSeconds = 3600;
                        targetSeconds = 60;
                        minBuckets = 60;
                        bucketModulo = 0;
                        break;
                    case 1:
                        // 5-minutely
                        windowSeconds = 3600;
                        targetSeconds = 60;
                        minBuckets = 12;
                        bucketModulo = 5;
                        break;
                    case 2:
                        // 10-minutely
                        windowSeconds = 3600;
                        targetSeconds = 60;
                        minBuckets = 6;
                        bucketModulo = 10;
                        break;
                    case 3:
                        // Hourly
                        windowSeconds = 86400;
                        targetSeconds = 3600;
                        minBuckets = 24;
                        bucketModulo = 0;
                        break;
                }

                // Get slope
                SimpleRegression r = new SimpleRegression();

                // Train regression
                HashMap<Long, Double> bucketTotals = new HashMap<Long, Double>();
                HashMap<Long, Double> bucketCounts = new HashMap<Long, Double>();
                for (Map.Entry<Long, Double> tskv : kv.getValue().getDataTrain().entrySet()) {
                    long ts = transformTime(tskv.getKey(), windowSeconds, targetSeconds, bucketModulo);
                    double val = tskv.getValue();
                    bucketTotals.put(ts, bucketTotals.getOrDefault(ts, 0D) + val);
                    bucketCounts.put(ts, bucketCounts.getOrDefault(ts, 0D) + 1.0D);
                }
                dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Bucketed totals= " + bucketTotals.toString());
                dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Bucketed counts = " + bucketCounts.toString());
                for (Map.Entry<Long, Double> tskv : bucketTotals.entrySet()) {
                    r.addData((double) tskv.getKey(), tskv.getValue() / bucketCounts.get(tskv.getKey()));
                }

                // Enough buckets?
                if (bucketTotals.size() < minBuckets) {
                    dataLoader.log(dataLoader.LOG_NOTICE, getClass().getSimpleName(), "Unreliable based on too few buckets");
                    continue;
                }

                // Reliable?
                double maxMse = 0.05; // 95% = 0.05
                double mse = r.getMeanSquareError();
                if (mse > maxMse) {
                    dataLoader.log(dataLoader.LOG_NOTICE, getClass().getSimpleName(), "Unreliable based on mean square error crosscheck (is " + mse + " exceeds " + maxMse + ")");
                    continue;
                }

                // Predict
                double maxStdDevMp = 1.0D;
                double stdDev = kv.getValue().getTrainStdDev();
                double avg = kv.getValue().getTrainAvg();
                double maxErr = Math.max(maxStdDevMp * stdDev, 0.05 * avg); // 1x std deviation or 5% of average
                for (Map.Entry<Long, Double> tskv : kv.getValue().getDataClassify().entrySet()) {
                    double val = tskv.getValue();
                    double expectedVal = r.predict(transformTime(tskv.getKey(), windowSeconds, targetSeconds, bucketModulo));
                    double rb = expectedVal + maxErr;
                    double lb = expectedVal - maxErr;
                    if (val < lb || val > rb) {
                        // New outlier
                        TimeserieOutlier outlier = new TimeserieOutlier(this, tskv.getKey(), tskv.getValue(), expectedVal, lb, rb);
                        if (!kv.getValue().validateOutlier(outlier)) {
                            continue;
                        }
                        // Only add once, not for every interval we catch it
                        if (outliers.contains(tskv.getKey())) {
                            continue;
                        }
                        outliers.add(tskv.getKey());
                        res.addOutlier(outlier);
                    } else {
                        res.addInlier(new TimeserieInlier(this, tskv.getKey(), tskv.getValue(), expectedVal, lb, rb));
                    }
                }
            }
        }
        return res;
    }

    private int transformTime(long ts, long windowSeconds, long targetSeconds, int bucketModulo) {
        long whole = ts / windowSeconds;
        long rest = ts - (whole * windowSeconds);
        int bucket = (int)Math.floor(rest / targetSeconds);
        if (bucketModulo > 0) {
            bucket = bucket - (bucket % bucketModulo);
        }
        return bucket;
    }
}

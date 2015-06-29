package nl.us2.timeseriesoutlierdetection;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by robin on 21/06/15.
 */
public class RandomWalkRegressionTimeserieAnalyzer extends AbstractTimeserieAnalyzer implements ITimeserieAnalyzer {
    public TimeserieAnalyzerResult analyze(AbstractDataLoader dataLoader, HashMap<String, Timeseries> timeseries) {
        TimeserieAnalyzerResult res = new TimeserieAnalyzerResult();

        // Iterate series
        for (Map.Entry<String, Timeseries> kv : timeseries.entrySet()) {
            TreeMap<Long, Double> deltas = kv.getValue().getTrainDeltas();
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Deltas = " + deltas.toString());

            // Train simple regression based on deltas
            SimpleRegression r = new SimpleRegression();

            // Train regression
            for (Map.Entry<Long, Double> tskv : deltas.entrySet()) {
                long ts = tskv.getKey();
                double val = tskv.getValue();
                r.addData((double)ts, val);
            }

            // Reliable?
            double maxMse = 0.05; // 95% = 0.05
            double relMse = r.getSumSquaredErrors() / r.getTotalSumSquares();
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Relative MSE = " + relMse);
            if (Double.isNaN(relMse)) {
                relMse = 0.0D;
            }
            if (relMse > maxMse) {
                dataLoader.log(dataLoader.LOG_NOTICE, getClass().getSimpleName(), "Unreliable based on relative mean square error crosscheck (is " + relMse + " exceeds " + maxMse + ")");
                continue;
            }

            // Predict
            double maxRelDif = Math.max(0.5 * relMse, 0.02); // Half of the expected error is acceptable, or 5%
            double previousVal = kv.getValue().getDataTrain().get(kv.getValue().getDataTrain().lastKey());
            for (Map.Entry<Long, Double> tskv : kv.getValue().getDataClassify().entrySet()) {
                long ts = tskv.getKey();
                double val = tskv.getValue();
                double expectedVal = previousVal + r.predict(ts);
                previousVal = expectedVal;
                double lb = expectedVal * (1-maxRelDif);
                double rb = expectedVal * (1+maxRelDif);
                dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), ts + " " + val + " " + expectedVal );
                if (val < lb || val > rb) {
                    TimeserieOutlier outlier = new TimeserieOutlier(this, kv.getValue().getSerieName(), tskv.getKey(), tskv.getValue(), expectedVal, lb, rb);
                    if (!kv.getValue().validateOutlier(outlier)) {
                        continue;
                    }
                    res.addOutlier(outlier);
                } else {
                    res.addInlier(new TimeserieInlier(this, kv.getValue().getSerieName(), tskv.getKey(), tskv.getValue(), expectedVal, lb, rb));
                }
            }
        }
        return res;
    }
}

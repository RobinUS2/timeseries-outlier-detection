package nl.us2.timeseriesoutlierdetection;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by robin on 21/06/15.
 */
public class SimpleRegressionTimeserieAnalyzer extends AbstractTimeserieAnalyzer implements ITimeserieAnalyzer {
    public TimeserieAnalyzerResult analyze(AbstractDataLoader dataLoader, HashMap<String, Timeseries> timeseries) {
        TimeserieAnalyzerResult res = new TimeserieAnalyzerResult();

        // Iterate series
        for (Map.Entry<String, Timeseries> kv : timeseries.entrySet()) {
            // Get slope
            SimpleRegression r = new SimpleRegression();

            // Train regression
            for (Map.Entry<Long, Double> tskv : kv.getValue().getDataTrain().entrySet()) {
                long ts = tskv.getKey();
                double val = tskv.getValue();
                r.addData((double)ts, val);
            }

            // Slope
            double slopeTs = 60.0D * r.getSlope(); // @todo dynamic
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Slope per time step = " + slopeTs);
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Slope = " + r.getSlope());
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Slope std err = " + r.getSlopeStdErr());
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Slope confidence interval = " + r.getSlopeConfidenceInterval()); //95% confidence interval
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Mean square err = " + r.getMeanSquareError());
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Sum square err = " + r.getSumSquaredErrors());
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Total sum square = " + r.getTotalSumSquares());

            // Reliable?
            double maxMse = 0.05; // 95% = 0.05
            double relMse =r.getSumSquaredErrors() / r.getTotalSumSquares();
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
            for (Map.Entry<Long, Double> tskv : kv.getValue().getDataClassify().entrySet()) {
                long ts = tskv.getKey();
                double val = tskv.getValue();
                double expectedVal = r.predict(ts);
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

import org.apache.commons.math3.stat.regression.RegressionResults;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by robin on 21/06/15.
 */
public class SimpleRegressionTimeserieAnalyzer extends AbstractTimeserieAnalyzer implements ITimeserieAnalyzer {
    public List<TimeserieOutlier> analyze(AbstractDataLoader dataLoader, HashMap<String, Timeseries> timeseries) {
        List<TimeserieOutlier> outliers = new ArrayList<TimeserieOutlier>();
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
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Total sum square = " + r.getTotalSumSquares());

            // Reliable?
            double maxMse = 0.05; // 95% = 0.05
            double relMse =r.getMeanSquareError() / r.getTotalSumSquares();
            if (relMse > maxMse) {
                dataLoader.log(dataLoader.LOG_NOTICE, getClass().getSimpleName(), "Unreliable based on relative mean square error crosscheck (is " + relMse + " exceeds " + maxMse + ")");
                return null;
            }

            // Predict
            double maxRelDif = 0.5 * relMse; // Half of the expected error is acceptable
            for (Map.Entry<Long, Double> tskv : kv.getValue().getDataClassify().entrySet()) {
                long ts = tskv.getKey();
                double val = tskv.getValue();
                double expectedVal = r.predict(ts);
                double dif = expectedVal / val;
                //dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), ts + " " + val + " " + expectedVal + " (dif " + dif + ")");
                if (Math.abs(dif) < 1 - maxRelDif || Math.abs(dif) > 1 + maxRelDif) {
                    TimeserieOutlier outlier = new TimeserieOutlier(this.getClass().getSimpleName(), tskv.getKey(), tskv.getValue(), -1, -1);
                    outliers.add(outlier);
                }
            }
        }
        return outliers;
    }
}

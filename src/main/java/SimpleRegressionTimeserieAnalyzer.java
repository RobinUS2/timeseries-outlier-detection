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
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Slope confidence interval = " + r.getSlopeConfidenceInterval());

            // Predict
            double maxRelDif = 0.1; // @todo dynamic
            for (Map.Entry<Long, Double> tskv : kv.getValue().getDataClassify().entrySet()) {
                long ts = tskv.getKey();
                double val = tskv.getValue();
                double expectedVal = r.predict(ts);
                double leftBound = expectedVal * (1 - maxRelDif);
                double rightBound = expectedVal * (1 + maxRelDif);
                if (val < leftBound || val > rightBound) {
                    TimeserieOutlier outlier = new TimeserieOutlier(this.getClass().getSimpleName(), tskv.getKey(), tskv.getValue(), leftBound, rightBound);
                    outliers.add(outlier);
                }
                //dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "ts " + ts + " expected " + expectedVal + " is " + val);
            }
        }
        return outliers;
    }
}

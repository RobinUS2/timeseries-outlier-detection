import com.sun.javafx.tools.resource.DeployResource;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by robin on 21/06/15.
 */
public class IntervalInterceptorTimeserieAnalyzer extends AbstractTimeserieAnalyzer implements ITimeserieAnalyzer {
    public List<TimeserieOutlier> analyze(AbstractDataLoader dataLoader, HashMap<String, Timeseries> timeseries) {
        List<TimeserieOutlier> outliers = new ArrayList<TimeserieOutlier>();
        for (Map.Entry<String, Timeseries> kv : timeseries.entrySet()) {
            try {
                // Get slope
                IntervalInterceptorModel r = new IntervalInterceptorModel();

                // Train regression
                for (Map.Entry<Long, Double> tskv : kv.getValue().getDataTrain().entrySet()) {
                    long ts = tskv.getKey();
                    double val = tskv.getValue();
                    r.addData(ts, val);
                }

                // Train
                r.train();

                // Reliable?
                double maxMse = 0.05; // 95% = 0.05
                double relMse = r.getMeanSquareError() / r.getTotalSumSquares();
                if (relMse > maxMse) {
                    dataLoader.log(dataLoader.LOG_NOTICE, getClass().getSimpleName(), "Unreliable based on relative mean square error crosscheck (is " + relMse + " exceeds " + maxMse + ")");
                    return null;
                }

                // Predict
                double maxRelDif = 0.05;
                for (Map.Entry<Long, Double> tskv : kv.getValue().getDataClassify().entrySet()) {
                    long ts = tskv.getKey();
                    double val = tskv.getValue();
                    double expectedVal = r.predict(ts);
                    double dif = expectedVal / val;
                    dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), ts + " " + val + " " + expectedVal + " (dif " + dif + ")");
                    double lb = 1 - maxRelDif;
                    double rb = 1 + maxRelDif;
                    if (Math.abs(dif) < lb || Math.abs(dif) > rb) {
                        TimeserieOutlier outlier = new TimeserieOutlier(this.getClass().getSimpleName(), tskv.getKey(), tskv.getValue(), lb * expectedVal, rb * expectedVal);
                        outliers.add(outlier);
                    }
                }
            } catch (Exception e) {
                dataLoader.log(dataLoader.LOG_ERROR, getClass().getSimpleName(), e.getMessage());
            }
        }
        return outliers;
    }
}

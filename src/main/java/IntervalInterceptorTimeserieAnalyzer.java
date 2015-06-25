import java.util.HashMap;
import java.util.Map;

/**
 * Created by robin on 21/06/15.
 */
public class IntervalInterceptorTimeserieAnalyzer extends AbstractTimeserieAnalyzer implements ITimeserieAnalyzer {
    public TimeserieAnalyzerResult analyze(AbstractDataLoader dataLoader, HashMap<String, Timeseries> timeseries) {
        TimeserieAnalyzerResult res = new TimeserieAnalyzerResult();

        // Iterate series
        for (Map.Entry<String, Timeseries> kv : timeseries.entrySet()) {
            try {
                dataLoader.log(dataLoader.LOG_NOTICE, getClass().getSimpleName(), kv.getKey());
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

                // Patterns?
                if (!r.patternsFound()) {
                    dataLoader.log(dataLoader.LOG_NOTICE, getClass().getSimpleName(), "No patterns found");
                    continue;
                }

                // Reliable?
                double maxMse = 0.05; // 95% = 0.05
                double relMse = r.getMeanSquareError() / r.getTotalSumSquares();
                if (relMse > maxMse) {
                    dataLoader.log(dataLoader.LOG_NOTICE, getClass().getSimpleName(), "Unreliable based on relative mean square error crosscheck (is " + relMse + " exceeds " + maxMse + ")");
                    continue;
                }

                // Predict
                double maxRelDif = 0.4; // Peaks are highly fluctuant
                for (Map.Entry<Long, Double> tskv : kv.getValue().getDataClassify().entrySet()) {
                    long ts = tskv.getKey();
                    double val = tskv.getValue();
                    double expectedVal = r.predict(ts);
                    dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), ts + " " + val + " " + expectedVal);
                    double lb = Math.min(expectedVal - kv.getValue().getTrainStdDev(), expectedVal * (1-maxRelDif));
                    double rb = Math.max(expectedVal + kv.getValue().getTrainStdDev(), expectedVal * (1+maxRelDif));
                    if (val < lb || val > rb) {
                        TimeserieOutlier outlier = new TimeserieOutlier(this, tskv.getKey(), tskv.getValue(), expectedVal, lb, rb);
                        if (!kv.getValue().validateOutlier(outlier)) {
                            continue;
                        }
                        res.addOutlier(outlier);
                    } else {
                        res.addInlier(new TimeserieInlier(this, tskv.getKey(), tskv.getValue(), expectedVal, lb, rb));
                    }
                }
            } catch (Exception e) {
                dataLoader.log(dataLoader.LOG_ERROR, getClass().getSimpleName(), e.getMessage());
            }
        }
        return res;
    }
}

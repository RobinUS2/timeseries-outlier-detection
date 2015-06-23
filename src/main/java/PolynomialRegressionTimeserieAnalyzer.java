import net.sourceforge.openforecast.DataSet;
import net.sourceforge.openforecast.Observation;
import net.sourceforge.openforecast.models.MovingAverageModel;
import net.sourceforge.openforecast.models.PolynomialRegressionModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by robin on 21/06/15.
 */
public class PolynomialRegressionTimeserieAnalyzer extends AbstractTimeserieAnalyzer implements ITimeserieAnalyzer {
    public List<TimeserieOutlier> analyze(AbstractDataLoader dataLoader, HashMap<String, Timeseries> timeseries) {
        List<TimeserieOutlier> outliers = new ArrayList<TimeserieOutlier>();
        for (Map.Entry<String, Timeseries> kv : timeseries.entrySet()) {
            PolynomialRegressionModel m = new PolynomialRegressionModel("ts");

            // Create train dataset
            DataSet dsTrain = new DataSet();
            long count = 0L;
            double total = 0D;
            for (Map.Entry<Long, Double> tskv : kv.getValue().getDataTrain().entrySet()) {
                long ts = tskv.getKey();
                double val = tskv.getValue();
                total += val;
                count++;
                Observation o = new Observation(val);
                o.setIndependentValue("ts", ts);
                dsTrain.add(o);
            }

            // Avg
            double avg = total / (double)count;

            // Total sum of squares
            double tsos = 0D;
            for (Map.Entry<Long, Double> tskv : kv.getValue().getDataTrain().entrySet()) {
                double val = tskv.getValue();
                tsos += Math.pow(val - avg, 2.0D);
            }
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Average = " + avg);
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Total sum squares = " + tsos);

            // Load data
            m.init(dsTrain);

            // Validate, total sum of squares must be bigger than 0 as else there is no delta between avg and data values
            double mse = m.getMSE();
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Mean square err = " + mse);// Reliable?
            double maxMse = 0.05; // 95% = 0.05
            double relMse = mse / tsos;
            if (relMse > maxMse && tsos > 0D) {
                dataLoader.log(dataLoader.LOG_NOTICE, getClass().getSimpleName(), "Unreliable based on relative mean square error crosscheck (is " + relMse + " exceeds " + maxMse + ")");
                return null;
            }

            // Classify
            double maxRelDif = Math.max(0.5 * relMse, 0.05); // Half of the expected error is acceptable, or 5%
            for (Map.Entry<Long, Double> tskv : kv.getValue().getDataClassify().entrySet()) {
                long ts = tskv.getKey();
                double val = tskv.getValue();
                Observation o = new Observation(0.0D); // Fake value
                o.setIndependentValue("ts", ts);
                double expectedVal = m.forecast(o);
                double lb = expectedVal * (1-maxRelDif);
                double rb = expectedVal * (1+maxRelDif);
                dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), ts + " " + val + " " + expectedVal );
                if (val < lb || val > rb) {
                    TimeserieOutlier outlier = new TimeserieOutlier(this.getClass().getSimpleName(), tskv.getKey(), tskv.getValue(), lb, rb);
                    if (!kv.getValue().validateOutlier(outlier)) {
                        continue;
                    }
                    outliers.add(outlier);
                }
            }

        }
        return outliers;
    }
}

import net.sourceforge.openforecast.DataSet;
import net.sourceforge.openforecast.Observation;
import net.sourceforge.openforecast.models.PolynomialRegressionModel;
import net.sourceforge.openforecast.models.SimpleExponentialSmoothingModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by robin on 21/06/15.
 */
public class SimpleExponentialSmoothingTimeserieAnalyzer extends AbstractTimeserieAnalyzer implements ITimeserieAnalyzer {
    public TimeserieAnalyzerResult analyze(AbstractDataLoader dataLoader, HashMap<String, Timeseries> timeseries) {
        TimeserieAnalyzerResult res = new TimeserieAnalyzerResult();

        // Iterate series
        for (Map.Entry<String, Timeseries> kv : timeseries.entrySet()) {// Create train dataset
            DataSet dsTrain = new DataSet();
            for (Map.Entry<Long, Double> tskv : kv.getValue().getDataTrain().entrySet()) {
                long ts = tskv.getKey();
                double val = tskv.getValue();
                Observation o = new Observation(val);
                o.setIndependentValue("ts", ts);
                dsTrain.add(o);
            }

            // Avg
            double avg = kv.getValue().getTrainAvg();

            // Total sum of squares
            double tsos = 0D;
            for (Map.Entry<Long, Double> tskv : kv.getValue().getDataTrain().entrySet()) {
                double val = tskv.getValue();
                tsos += Math.pow(val - avg, 2.0D);
            }
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Average = " + avg);
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Total sum squares = " + tsos);

            // Model
            SimpleExponentialSmoothingModel m = SimpleExponentialSmoothingModel.getBestFitModel(dsTrain);
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Alpha = " + m.getAlpha());// Reliable?

            // Validate, total sum of squares must be bigger than 0 as else there is no delta between avg and data values
            double mse = m.getMSE();
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Mean square err = " + mse);// Reliable?
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Mean absolute deviation = " + m.getMAD());// Reliable?
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Mean absolute percentage error = " + m.getMAPE());// Reliable?
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Akaike Information Criteria = " + m.getAIC());// Reliable? less is better
            double maxMse = 0.05; // 95% = 0.05
            double relMse = mse / tsos;
            if (relMse > maxMse && tsos > 0D) {
                dataLoader.log(dataLoader.LOG_NOTICE, getClass().getSimpleName(), "Unreliable based on relative mean square error crosscheck (is " + relMse + " exceeds " + maxMse + ")");
                continue;
            }
            // Average absolute error bigger than standard deviation is not acceptable
            if (kv.getValue().getTrainStdDev() > 0 && m.getMAD() > kv.getValue().getTrainStdDev()) {
                dataLoader.log(dataLoader.LOG_NOTICE, getClass().getSimpleName(), "Unreliable based on MAD (mean absolute error) / standard deviation crosscheck (MAD " + m.getMAD() + " exceeds stddev " + kv.getValue().getTrainStdDev() + ")");
                continue;
            }
            // Average absolute error bigger than average is not acceptable
            if (m.getMAD() > kv.getValue().getTrainAvg()) {
                dataLoader.log(dataLoader.LOG_NOTICE, getClass().getSimpleName(), "Unreliable based on MAD (mean absolute error) / average crosscheck (MAD " + m.getMAD() + " exceeds avg " + kv.getValue().getTrainAvg() + ")");
                continue;
            }

            // Classify
            double maxRelDif = Math.max(0.5 * relMse, 0.05); // Half of the expected error is acceptable, or 5%
            for (Map.Entry<Long, Double> tskv : kv.getValue().getDataClassify().entrySet()) {
                long ts = tskv.getKey();
                double val = tskv.getValue();
                Observation o = new Observation(0.0D); // Fake value
                o.setIndependentValue("ts", ts);
                double expectedVal = m.forecast(o);
                double lb = Math.min(expectedVal - kv.getValue().getTrainStdDev(), expectedVal * (1-maxRelDif));
                double rb = Math.max(expectedVal + kv.getValue().getTrainStdDev(), expectedVal * (1+maxRelDif));
                dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), ts + " " + val + " " + expectedVal );
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

        }
        return res;
    }
}

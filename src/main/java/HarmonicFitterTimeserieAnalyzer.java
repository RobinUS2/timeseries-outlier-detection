import org.apache.commons.math3.fitting.HarmonicCurveFitter;
import org.apache.commons.math3.fitting.SimpleCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.util.*;

/**
 * Created by robin on 21/06/15.
 */
public class HarmonicFitterTimeserieAnalyzer extends AbstractTimeserieAnalyzer implements ITimeserieAnalyzer {
    public List<TimeserieOutlier> analyze(AbstractDataLoader dataLoader, HashMap<String, Timeseries> timeseries) {
        List<TimeserieOutlier> outliers = new ArrayList<TimeserieOutlier>();
        for (Map.Entry<String, Timeseries> kv : timeseries.entrySet()) {
            // Train set
            Collection<WeightedObservedPoint> train = new ArrayList<WeightedObservedPoint>();
            for (Map.Entry<Long, Double> tskv : kv.getValue().getDataTrain().entrySet()) {
                long ts = tskv.getKey();
                double val = tskv.getValue();
                train.add(new WeightedObservedPoint(1.0, (double)ts, val));
            }

            // Param guesser
            HarmonicCurveFitter.ParameterGuesser pg = new HarmonicCurveFitter.ParameterGuesser(train);
            double[] params = pg.guess();
            double amplitude = params[0];
            double angularFreq = params[1];
            double phase = params[2];
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Amplitude = " + amplitude);
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Angular frequency = " + angularFreq);
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Phase = " + phase);
        }
        return outliers;
    }
}

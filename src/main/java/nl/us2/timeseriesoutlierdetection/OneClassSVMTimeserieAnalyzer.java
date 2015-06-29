package nl.us2.timeseriesoutlierdetection;

import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameterPoint;
import edu.berkeley.compbio.jlibsvm.kernel.GaussianRBFKernel;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.oneclass.MutableOneClassProblemImpl;
import edu.berkeley.compbio.jlibsvm.oneclass.OneClassModel;
import edu.berkeley.compbio.jlibsvm.oneclass.OneClassProblem;
import edu.berkeley.compbio.jlibsvm.oneclass.OneClassSVC;
import edu.berkeley.compbio.jlibsvm.scaler.NoopScalingModelLearner;
import edu.berkeley.compbio.jlibsvm.util.SparseVector;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by robin on 21/06/15.
 */
public class OneClassSVMTimeserieAnalyzer extends AbstractTimeserieAnalyzer implements ITimeserieAnalyzer {
    private static final double MIN_VALIDATION_RATE = 0.9;

    public int getInlierScore() {
        // Overrides the default, this model is not used often, but if it is, it is reliable mostly for inliers
        return DEFAULT_INLIER_SCORE * 3;
    }

    public TimeserieAnalyzerResult analyze(AbstractDataLoader dataLoader, HashMap<String, Timeseries> timeseries) {
        TimeserieAnalyzerResult res = new TimeserieAnalyzerResult();

        // Iterate series
        for (Map.Entry<String, Timeseries> kv : timeseries.entrySet()) {

            // Kernel
            KernelFunction kernel = new GaussianRBFKernel(0.1F);

            // Params: see http://mlpy.sourceforge.net/docs/3.4/svm.html
            ImmutableSvmParameterPoint.Builder paramPointBuilder = new ImmutableSvmParameterPoint.Builder();
            paramPointBuilder.kernel = kernel;
            paramPointBuilder.nu = 0.05F; // The parameter nu is an upper bound on the fraction of margin errors and a lower bound of the fraction of support vectors relative to the total number of training examples. For example, if you set it to 0.05 you are guaranteed to find at most 5% of your training examples being misclassified (at the cost of a small margin, though) and at least 5% of your training examples being support vectors.
            paramPointBuilder.eps = 0.00001F;
            ImmutableSvmParameterPoint param = paramPointBuilder.build();

            // Problem, P = point, L = label
            int trainSamples = (int)Math.floor(kv.getValue().getDataTrain().size() * 0.7);
            MutableOneClassProblemImpl problem = new MutableOneClassProblemImpl(trainSamples, Float.class);
            int samplesProcessed = 0;
            for (Map.Entry<Long, Double> tskv : kv.getValue().getDataTrain().entrySet()) {
                SparseVector v = new SparseVector(1);
                v.indexes[0] = tsToIndex(tskv.getKey());
                v.values[0] = convertVal(tskv.getValue().doubleValue());
                problem.addExampleFloat(v, 1.0F);
                samplesProcessed++;
                if (samplesProcessed == trainSamples) {
                    break;
                }
            }

            // SVM: The range of C is from zero to infinity but nu is always between [0,1]. A nice property of nu is that it is related to the ratio of support vectors and the ratio of the training error.
            OneClassSVC svm = new OneClassSVC();

            // Scale problem, disabled as we only have one real dimension, so there will be no dominant features
            OneClassProblem scaledProblem = problem.getScaledCopy(new NoopScalingModelLearner());

            // Train
            OneClassModel model = (OneClassModel) svm.train(scaledProblem, param);

            // Validate
            SparseVector vv = new SparseVector(1);
            int matched = 0;
            int tested = 0;
            for (Map.Entry<Long, Double> tskv : kv.getValue().getDataTrain().entrySet()) {
                samplesProcessed++;
                if (samplesProcessed < trainSamples) {
                    continue;
                }
                tested++;
                vv.indexes[0] = tsToIndex(tskv.getKey());
                vv.values[0] = convertVal(tskv.getValue().doubleValue());
                double prob = model.predictValue(vv);
                if (prob > 0) {
                    // 1.0 = match
                    matched++;
                }
            }
            double validationRate = (double)matched / (double)tested;
            dataLoader.log(dataLoader.LOG_DEBUG, getClass().getSimpleName(), "Validation rate " + validationRate + " (" + matched + "/" + tested + ")");
            if (validationRate < MIN_VALIDATION_RATE) {
                dataLoader.log(dataLoader.LOG_NOTICE, getClass().getSimpleName(), "Unreliable based on validation rate cross validation (is " + validationRate + " below " + MIN_VALIDATION_RATE + ")");
                continue;
            }

            // Predict
            SparseVector vp = new SparseVector(1);
            for (Map.Entry<Long, Double> tskv : kv.getValue().getDataClassify().entrySet()) {
                vp.indexes[0] = tsToIndex(tskv.getKey());
                vp.values[0] = convertVal(tskv.getValue().doubleValue());
                double prob = model.predictValue(vp);
                if (prob < 0) {
                    // -1 is outlier
                    TimeserieOutlier outlier = new TimeserieOutlier(this, kv.getValue().getSerieName(), tskv.getKey(), tskv.getValue(), Double.NaN, Double.NaN, Double.NaN);
                    if (!kv.getValue().validateOutlier(outlier)) {
                        continue;
                    }
                    res.addOutlier(outlier);
                } else {
                    res.addInlier(new TimeserieInlier(this, kv.getValue().getSerieName(), tskv.getKey(), tskv.getValue(), Double.NaN, Double.NaN, Double.NaN));
                }
            }
        }

        return res;
    }

    private float convertVal(double val) {
        return (float)val;
    }

    private int tsToIndex(long ts) {
        return 1;/*
        int secondsInHour = 3600;
        int secondsInMinute = 60;
        long whole = ts / secondsInHour;
        long rest = ts - (whole * secondsInHour);
        return (int)Math.floor(rest / secondsInMinute);*/
    }
}

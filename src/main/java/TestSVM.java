import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameterPoint;
import edu.berkeley.compbio.jlibsvm.kernel.GaussianRBFKernel;
import edu.berkeley.compbio.jlibsvm.oneclass.MutableOneClassProblemImpl;
import edu.berkeley.compbio.jlibsvm.oneclass.OneClassModel;
import edu.berkeley.compbio.jlibsvm.oneclass.OneClassProblem;
import edu.berkeley.compbio.jlibsvm.oneclass.OneClassSVC;
import edu.berkeley.compbio.jlibsvm.scaler.LinearScalingModelLearner;
import edu.berkeley.compbio.jlibsvm.util.SparseVector;

import java.util.Random;

/**
 * Created by robin on 24/06/15.
 */
public class TestSVM {
    public static void main(String [ ] args) throws Exception
    {
        // Kernel
        GaussianRBFKernel kernel = new GaussianRBFKernel(0.1F);

        // Params: see http://mlpy.sourceforge.net/docs/3.4/svm.html
        ImmutableSvmParameterPoint.Builder paramPointBuilder = new ImmutableSvmParameterPoint.Builder();
        paramPointBuilder.kernel = kernel;
        paramPointBuilder.nu = 0.005F; // The parameter nu is an upper bound on the fraction of margin errors and a lower bound of the fraction of support vectors relative to the total number of training examples. For example, if you set it to 0.05 you are guaranteed to find at most 5% of your training examples being misclassified (at the cost of a small margin, though) and at least 5% of your training examples being support vectors.
        paramPointBuilder.eps = 0.00001F;
        ImmutableSvmParameterPoint param = paramPointBuilder.build();

        // Problem, P = point, L = label
        int samples = 100;
        MutableOneClassProblemImpl problem = new MutableOneClassProblemImpl(samples, Float.class);
        Random rand = new Random();
        for (int i = 0; i < samples; i++) {
            SparseVector v = new SparseVector(1);
            v.indexes[0] = rand.nextInt(500);
            v.values[0] = rand.nextInt(500);
            problem.addExample(v, 1.0F);
        }

        // SVM: The range of C is from zero to infinity but nu is always between [0,1]. A nice property of nu is that it is related to the ratio of support vectors and the ratio of the training error.
        OneClassSVC svm = new OneClassSVC();

        // Scale problem
        OneClassProblem scaledProblem = problem.getScaledCopy(new LinearScalingModelLearner(samples, true));

        // Train
        OneClassModel model = (OneClassModel) svm.train(scaledProblem, param);

        // Predict
        for (int i = 0; i < 100; i++) {
            SparseVector vp = new SparseVector(1);
            vp.indexes[0] = rand.nextInt(500);
            vp.values[0] = rand.nextInt(500);
            double prob = model.predictValue(vp);
            System.out.println(prob);
        }
    }
}

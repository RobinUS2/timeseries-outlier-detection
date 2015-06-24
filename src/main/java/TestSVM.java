import edu.berkeley.compbio.jlibsvm.ContinuousModel;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameterPoint;
import edu.berkeley.compbio.jlibsvm.SolutionModel;
import edu.berkeley.compbio.jlibsvm.SvmProblem;
import edu.berkeley.compbio.jlibsvm.binary.BinaryClassificationProblem;
import edu.berkeley.compbio.jlibsvm.binary.BinaryClassificationSVM;
import edu.berkeley.compbio.jlibsvm.binary.MutableBinaryClassificationProblemImpl;
import edu.berkeley.compbio.jlibsvm.binary.Nu_SVC;
import edu.berkeley.compbio.jlibsvm.kernel.GaussianRBFKernel;
import edu.berkeley.compbio.jlibsvm.multi.MultiClassProblem;
import edu.berkeley.compbio.jlibsvm.multi.MutableMultiClassProblemImpl;
import edu.berkeley.compbio.jlibsvm.oneclass.MutableOneClassProblemImpl;
import edu.berkeley.compbio.jlibsvm.oneclass.OneClassModel;
import edu.berkeley.compbio.jlibsvm.oneclass.OneClassProblem;
import edu.berkeley.compbio.jlibsvm.oneclass.OneClassSVC;
import edu.berkeley.compbio.jlibsvm.scaler.LinearScalingModelLearner;
import edu.berkeley.compbio.jlibsvm.scaler.NoopScalingModelLearner;
import edu.berkeley.compbio.jlibsvm.util.SparseVector;

/**
 * Created by robin on 24/06/15.
 */
public class TestSVM {
    public static void main(String [ ] args) throws Exception
    {
        // Kernel
        GaussianRBFKernel kernel = new GaussianRBFKernel(1.0F);

        // Params: see http://mlpy.sourceforge.net/docs/3.4/svm.html
        ImmutableSvmParameterPoint.Builder paramPointBuilder = new ImmutableSvmParameterPoint.Builder();
        paramPointBuilder.kernel = kernel;
        paramPointBuilder.nu = 0.5F; // The parameter nu is an upper bound on the fraction of margin errors and a lower bound of the fraction of support vectors relative to the total number of training examples. For example, if you set it to 0.05 you are guaranteed to find at most 5% of your training examples being misclassified (at the cost of a small margin, though) and at least 5% of your training examples being support vectors.
        paramPointBuilder.eps = 0.00001F;
        ImmutableSvmParameterPoint param = paramPointBuilder.build();

        // Problem, P = point, L = label
        MutableOneClassProblemImpl problem = new MutableOneClassProblemImpl(3, Float.class);
        SparseVector v = new SparseVector(1);
        v.indexes[0] = 1;
        v.values[0] = 100;
        problem.addExampleFloat(v, 100F);

        SparseVector v2 = new SparseVector(1);
        v2.indexes[0] = 1;
        v2.values[0] = 100;
        problem.addExampleFloat(v2, 100F);

        SparseVector v3 = new SparseVector(1);
        v3.indexes[0] = 1;
        v3.values[0] = 100;
        problem.addExampleFloat(v3, 100F);

        // SVM: The range of C is from zero to infinity but nu is always between [0,1]. A nice property of nu is that it is related to the ratio of support vectors and the ratio of the training error.
        OneClassSVC svm = new OneClassSVC();

        // Scale problem
        OneClassProblem scaledProblem = problem.getScaledCopy(new NoopScalingModelLearner());

        // Train
        OneClassModel model = (OneClassModel) svm.train(scaledProblem, param);

        // Predict
        SparseVector vp = new SparseVector(1);
        vp.indexes[0] = 1;
        vp.values[0] = 100;
        double prob = model.predictValue(vp);
        System.out.println(prob);
    }
}

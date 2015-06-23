import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by robin on 21/06/15.
 */
public class Timeseries {
    private TreeMap<Long, Double> data;
    private TreeMap<Long, Double> trainData;
    private TreeMap<Long, Double> classifyData;
    private double trainAvg;
    private double trainStdDev;
    private long datapoints;
    private final double TRAIN_CLASSIFY_SPLIT = 0.7D;
    private final long maxClassifyPoints = 10;
    private long trainDataPoints;
    private long classifyDataPointsStart;
    private boolean alertOutlierOver = true;
    private boolean alertOutlierUnder = true;

    public boolean validateOutlier(TimeserieOutlier outlier) {
        if (outlier.getVal() < outlier.getLeftBound() && !alertOutlierUnder) {
            // Do not alert if lower than expected
            return false;
        }
        if (outlier.getVal() > outlier.getRightBound() && !alertOutlierOver) {
            // Do not alert if higher than expected
            return false;
        }
        return true;
    }

    public Timeseries() {
        data = new TreeMap<Long, Double>();
    }

    public void setAlertPolicy(boolean over, boolean under) {
        alertOutlierOver = over;
        alertOutlierUnder = under;
    }

    public void setData(TreeMap<Long, Double> d) {
        data = d;
        datapoints = data.size();
        trainDataPoints = classifyDataPointsStart = (long)Math.floor((double)datapoints * TRAIN_CLASSIFY_SPLIT);
        if (datapoints - trainDataPoints > maxClassifyPoints) {
            trainDataPoints = datapoints-maxClassifyPoints;
            classifyDataPointsStart = trainDataPoints;
        }

        // Clear previously cached data sets
        trainAvg = Double.NaN;
        trainStdDev = Double.NaN;
        trainData = null;
        classifyData = null;

        // Reload
        getDataTrain();
        getDataClassify();
    }

    public String toString() {
        return data.toString();
    }

    public TreeMap<Long, Double> getData() {
        return data;
    }

    public TreeMap<Long, Double> getDataTrain() {
        if (trainData != null) {
            return trainData;
        }
        long i = 0L;
        trainData = new TreeMap<Long, Double>();
        for (Map.Entry<Long, Double> kv : data.entrySet()) {
            trainData.put(kv.getKey(), kv.getValue());
            i++;
            if (i == trainDataPoints) {
                break;
            }
        }

        // Stats
        _computeTrainStatics();

        // Sanitize training data
        _sanitizeTrainData();

        return trainData;
    }

    protected void _computeTrainStatics() {
        // Avg
        double total = 0.0D;
        for (double val : trainData.values()) {
            total += val;
        }
        trainAvg = total / (double)trainData.size();

        // Stddev
        double msqT = 0.0D;
        for (double val : trainData.values()) {
            double msq = Math.pow(val - trainAvg, 2.0D);
            msqT += msq;
        }
        double msqAvg = msqT / (double)trainData.size();
        trainStdDev = Math.sqrt(msqAvg);
    }

    protected void _sanitizeTrainData() {
        double outlierStdDevMp = 5.0D; // x times the standard deviation of the average is considered weird datapoint, 5 seems to be a good value "At five-sigma there is only one chance in nearly two million that a random fluctuation would yield the result" - wikipedia
        double min = trainAvg - (trainStdDev * outlierStdDevMp);
        double max = trainAvg + (trainStdDev * outlierStdDevMp);
        double previousValue = trainAvg;
        double replacementValue;
        int replacementCount = 0;
        for (Map.Entry<Long, Double> kv : trainData.entrySet()) {
            // Weird outlier?
            if (kv.getValue() < min || kv.getValue() > max) {
                // Replacement value
                replacementValue = (trainAvg + previousValue) / 2.0D;

                // Log
                System.err.println("Training outlier " + kv.getKey() + " val " + kv.getValue() + " avg " + trainAvg + " stddev " + trainStdDev +  " replacing with " + replacementValue);

                // Replace the value
                trainData.put(kv.getKey(), replacementValue);

                // Count
                replacementCount++;

                // Continue to make sure we do not set this outlier as previous value
                continue;
            }
            previousValue = kv.getValue();
        }

        // Recompute if there were changes
        if (replacementCount > 0) {
            //System.out.println("Stdddev was " + trainStdDev + " avg was " + trainAvg);
            _computeTrainStatics();
            //System.out.println("Stdddev is " + trainStdDev + " avg is " + trainAvg);
        }
    }

    public double getTrainAvg() {
        return trainAvg;
    }

    public double getTrainStdDev() {
        return trainStdDev;
    }

    public SortedMap<Long, Double> getDataClassify() {
        if (classifyData != null) {
            return classifyData;
        }
        long i = 0L;
        classifyData = new TreeMap<Long, Double>();
        for (Map.Entry<Long, Double> kv : data.entrySet()) {
            i++;
            if (i <= classifyDataPointsStart) {
                continue;
            }
            classifyData.put(kv.getKey(), kv.getValue());
        }
        return classifyData;
    }
}

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
        return trainData;
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

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
    private long trainDataPoints;
    private long classifyDataPointsStart;
    public Timeseries() {
        data = new TreeMap<Long, Double>();
    }

    public void setData(TreeMap<Long, Double> d) {
        data = d;
        datapoints = data.size();
        trainDataPoints = (long)Math.floor((double)datapoints * TRAIN_CLASSIFY_SPLIT);
        classifyDataPointsStart = (long)Math.floor((double)datapoints * TRAIN_CLASSIFY_SPLIT);
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
            if (i < classifyDataPointsStart) {
                continue;
            }
            classifyData.put(kv.getKey(), kv.getValue());
        }
        return classifyData;
    }
}

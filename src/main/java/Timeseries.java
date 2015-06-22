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
    public Timeseries() {
        data = new TreeMap<Long, Double>();
    }

    public void setData(TreeMap<Long, Double> d) {
        data = d;
        datapoints = data.size();
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
        long needed = (long)Math.floor((double)datapoints * TRAIN_CLASSIFY_SPLIT);
        long i = 0L;
        trainData = new TreeMap<Long, Double>();
        for (Map.Entry<Long, Double> kv : data.entrySet()) {
            trainData.put(kv.getKey(), kv.getValue());
            i++;
            if (i == needed) {
                break;
            }
        }
        return trainData;
    }

    public SortedMap<Long, Double> getDataClassify() {
        if (classifyData != null) {
            return classifyData;
        }
        long start = (long)Math.floor((double)datapoints * TRAIN_CLASSIFY_SPLIT);
        long i = 0L;
        classifyData = new TreeMap<Long, Double>();
        for (Map.Entry<Long, Double> kv : data.entrySet()) {
            i++;
            if (i < start) {
                continue;
            }
            classifyData.put(kv.getKey(), kv.getValue());
        }
        return classifyData;
    }
}

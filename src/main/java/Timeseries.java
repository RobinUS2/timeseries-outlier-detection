import java.util.TreeMap;

/**
 * Created by robin on 21/06/15.
 */
public class Timeseries {
    private TreeMap<Long, Double> data;
    public Timeseries() {
        data = new TreeMap<Long, Double>();
    }

    public void setData(TreeMap<Long, Double> d) {
        data = d;
    }

    public String toString() {
        return data.toString();
    }
}

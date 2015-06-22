import java.util.TreeMap;

/**
 * Interval based forecasting
 * @author Robin Verlangen
 */
public class IntervalInterceptorModel {
    private TreeMap<Long, Double> data;
    private double maxValue;
    private double minValue;

    public IntervalInterceptorModel() {
        data = new TreeMap<Long, Double>();
        maxValue = Double.MIN_VALUE;
        minValue = Double.MAX_VALUE;
    }

    public void addData(long ts, double val) {
        data.put(ts, val);
        if (val < minValue) {
            minValue = val;
        }
        if (val > maxValue) {
            maxValue = val;
        }
    }

    public void train() {
        // Calculate average and standard deviation, very low stddev will make this unusable algo (as there are no peaks/lows)
        double total = 0.0D;
        long count = 0L;
        for (double val : data.values()) {
            total += val;
            count++;
        }
        double avg = total / (double)count;

        // Standard deviation
        double msqT = 0.0D;
        for (double val : data.values()) {
            double msq = Math.pow(val - avg, 2.0D);
            msqT += msq;
        }
        double msqAvg = msqT / (double)count;
        double stdDev = Math.sqrt(msqAvg);
        System.out.println("avg " + avg);
        System.out.println("stddev " + stdDev);


        // @todo Implement interceptors
    }

    public double predict(long ts) {
        // @todo Implement
        return -1.0D;
    }
}

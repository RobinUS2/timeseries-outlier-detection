import sun.nio.cs.Surrogate;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Interval based forecasting
 * @author Robin Verlangen
 */
public class IntervalInterceptorModel {
    private TreeMap<Long, Double> data;
    private double maxValue;
    private double minValue;
    private boolean debugEnabled;
    private boolean isTrained;
    private double avg;
    private double stdDev;
    private int dataCount;

    public IntervalInterceptorModel() {
        data = new TreeMap<Long, Double>();
        maxValue = Double.MIN_VALUE;
        minValue = Double.MAX_VALUE;
        debugEnabled = true;
    }

    public void addData(long ts, double val) {
        data.put(ts, val);
        if (val < minValue) {
            minValue = val;
        }
        if (val > maxValue) {
            maxValue = val;
        }
        isTrained = false;
    }

    public void train() throws Exception {
        // Calculate average and standard deviation, very low stddev will make this unusable algo (as there are no peaks/lows)
        double total = 0.0D;
        dataCount = 0;
        for (double val : data.values()) {
            total += val;
            dataCount++;
        }
        avg = total / (double)dataCount;

        // Standard deviation
        double msqT = 0.0D;
        for (double val : data.values()) {
            double msq = Math.pow(val - avg, 2.0D);
            msqT += msq;
        }
        double msqAvg = msqT / (double)dataCount;
        stdDev = Math.sqrt(msqAvg);
        debug("avg " + avg);
        debug("stddev " + stdDev);
        if (stdDev == 0.0D) {
            isTrained = true;
            debug("Standard deviation zero, stop training");
            return;
        }

        // Does this suite a simple regression? IF so, it is probably not an interval dataset, throw exception
        // @todo check

        // Scan intervals
        double scanValue = maxValue;
        int maxIterations = 10000;
        double scanStep = (maxValue - minValue) / maxIterations;
        debug("Scan step size " + scanStep);
        HashMap<Long, Double> foundPairs = new HashMap<Long, Double>();
        for (int i = 0; i <maxIterations; i++) {
            scanValue -= scanStep;
            foundPairs.clear();
            for (Map.Entry<Long, Double> kv: data.entrySet()) {
                // Ignore below scan value
                if (kv.getValue() < scanValue) {
                    continue;
                }
                foundPairs.put(kv.getKey(), kv.getValue());
            }
            // Need at least two peaks to establish an interval
            int foundPairCount = foundPairs.size();
            if (foundPairCount < 2) {
                continue;
            }

            // Can not be majority of datapoints
            if (foundPairCount >= (double)dataCount * 0.3) {
                continue;
            }

            // Debug
            debug("Pairs above scan value of " + scanValue + ": " + foundPairs.toString());

            // Regular intervals?
            long previousTs = -1L;
            long previousTsDelta = -1L;
            boolean regularIntervals = true;
            for (Map.Entry<Long, Double> kv: foundPairs.entrySet()) {
                long ts = kv.getKey();
                if (previousTs > -1L) {
                    long tsDelta = ts - previousTs;
                    debug("ts delta " + tsDelta);
                    if (previousTsDelta > -1L) {
                        if (tsDelta != previousTsDelta) {
                            regularIntervals = false;
                            debug("TS deltas are not regular");
                            break;
                        }
                    }
                    previousTsDelta = tsDelta;
                }
                previousTs = ts;
            }

            // Regular?
            if (regularIntervals) {
                debug("Regular intervals " + foundPairs.toString());
            }

            // Skip forward in scanStep to below the lowest value of the current pairs
            double minValFound = Double.MAX_VALUE;
            for (double val : data.values()) {
                if (val < minValFound) {
                    minValFound = val;
                }
            }
            scanValue = minValFound - scanStep;
            debug("Forward scanValue to " + scanValue);

            // Done?
            if (scanValue < minValue) {
                break;
            }
        }


        // @todo Implement interceptors

        // Done
        isTrained = true;
    }

    protected void debug(String msg) {
        if (debugEnabled) {
            System.out.println("[" + getClass().getSimpleName()+"] " + msg);
        }
    }

    public double predict(long ts) throws Exception {
        // Must be trained
        if (!isTrained) {
            throw new Exception("Not trained, call train()");
        }

        // IF there is no standard deviation, the next value is probably the average
        if (stdDev == 0.0D) {
            return avg;
        }

        // @todo Implement
        return -1.0D;
    }
}

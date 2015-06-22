import sun.nio.cs.Surrogate;

import java.util.ArrayList;
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
    private long tsDelta;
    private ArrayList<IntervalPattern> intervalPatterns;

    public IntervalInterceptorModel() {
        data = new TreeMap<Long, Double>();
        maxValue = Double.MIN_VALUE;
        minValue = Double.MAX_VALUE;
        debugEnabled = true;
        intervalPatterns = new ArrayList<IntervalPattern>();
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
        long tsPrev = -1;
        tsDelta = -1L;
        for (Map.Entry<Long, Double> kv:  data.entrySet()) {
            double val = kv.getValue();

            // Current time
            long ts = kv.getKey();

            // Sparse data?
            if (tsPrev > -1L) {
                long nowDelta = ts - tsPrev;
                if (tsDelta > -1 && nowDelta != tsDelta) {
                    throw new Exception("Timeseries time interval not consistent");
                }
                tsDelta = nowDelta;
            }

            // Previous time
            tsPrev = ts;

            // Total
            total += val;

            // Data point count
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

        // Clear found patterns list
        intervalPatterns.clear();

        // Scan intervals
        double scanValue = maxValue;
        int maxIterations = 10000;
        double scanStep = (maxValue - minValue) / maxIterations;
        debug("Scan step size " + scanStep);
        TreeMap<Long, Double> foundPairs = new TreeMap<Long, Double>();
        for (int i = 0; i <maxIterations; i++) {
            scanValue -= scanStep;
            foundPairs.clear();
            for (Map.Entry<Long, Double> kv: data.entrySet()) {
                // Ignore below scan value
                if (kv.getValue() < scanValue) {
                    continue;
                }

                // Ignore any values around avg
                if (kv.getValue() >= avg -(0.1 * stdDev) && kv.getValue() <= avg + (0.1*stdDev)) {
                    continue;
                }

                // Options
                foundPairs.put(kv.getKey(), kv.getValue());
            }
            // Need at least two peaks to establish an interval
            int foundPairCount = foundPairs.size();
            if (foundPairCount < 2) {
                continue;
            }

            // Can not be majority of datapoints
            if (foundPairCount >= (double)dataCount * 0.9) {
                continue;
            }

            // Debug
            debug("Pairs above scan value of " + scanValue + ": " + foundPairs.toString());

            // Regular intervals?
            long previousTs = foundPairs.firstKey() - tsDelta;
            long previousTsDelta = -1L;
            long intervalStartTs = -1L; // Start timestamp
            long intervalLength = 0L; // Steps in the current interval
            long lastIntervalEndTs = -1L;
            String possibleIntervalKey = "";
            HashMap<String, Integer> possibleIntervals = new HashMap<String, Integer>();
            double minVal = Double.MAX_VALUE;
            double maxVal = Double.MIN_VALUE;
            for (Map.Entry<Long, Double> kv: foundPairs.entrySet()) {
                // Store min and max
                double val = kv.getValue();
                if (val < minVal) {
                    minVal = val;
                }
                if (val > maxVal) {
                    maxVal = val;
                }

                // Analyze timestamps
                long ts = kv.getKey();
                if (previousTs > -1L) {
                    // Delta compared to previous entry
                    long nowDelta = ts - previousTs;
                    debug("ts delta " + nowDelta);

                    // Is the previous peak 1 step away, in that case this is a interval spanning multiple points
                    boolean endOfChain = false;
                    if (nowDelta == tsDelta) {
                        debug("chained to prev delta");
                    } else {
                        if (intervalStartTs > -1L) {
                            // End
                            debug("end of chain, started at " + intervalStartTs + " length " + intervalLength);
                            endOfChain = true;

                            // Option
                            if (lastIntervalEndTs > -1L) {
                                long timeBetweenIntervals = ts - intervalStartTs;
                                possibleIntervalKey = "l" + intervalLength + "_i" + timeBetweenIntervals;
                            }

                            // Last end
                            lastIntervalEndTs = ts;

                            // Reset
                            intervalStartTs = -1L;
                            intervalLength = 0L;
                        }
                    }
                    intervalLength++;

                    // Timestamp for spanning points
                    if (intervalStartTs == -1L) {
                        intervalStartTs = ts;
                    }

                    // Interval ts delta
                    long intervalTsDelta = Math.max(nowDelta, ts - intervalStartTs);

                    // Only check intervals if this is the end of a chain
                    if (endOfChain) {
                        if (previousTsDelta > -1L) {
                            // Check for irregular series, only if the serie ended
                            if (intervalTsDelta != previousTsDelta) {
                                debug("TS deltas are not regular, is " + intervalTsDelta + " was " + previousTsDelta);
                            } else {
                                // Regular delta, possible interval
                                int tmp = possibleIntervals.getOrDefault(possibleIntervalKey, 0);
                                tmp++;
                                possibleIntervals.put(possibleIntervalKey, tmp);
                                possibleIntervalKey = "";
                            }
                        }
                        previousTsDelta = intervalTsDelta;
                    }
                }
                previousTs = ts;
            }

            debug("Possible intervals " + possibleIntervals.toString());
            if (possibleIntervals.size() > 0) {
                // Pick the first most occuring one
                int maxOccurence = Integer.MIN_VALUE;
                String maxK = "";
                for (Map.Entry<String, Integer> kv : possibleIntervals.entrySet()) {
                    if (kv.getValue() > maxOccurence) {
                        maxOccurence = kv.getValue();
                        maxK = kv.getKey();
                    }
                }
                String[] split = maxK.split("_");
                int length = Integer.parseInt(split[0].substring(1));
                int interval = Integer.parseInt(split[1].substring(1));
                debug("Pattern found: length " + length + " with interval of " + interval);
                IntervalPattern ip = new IntervalPattern(length, interval, minVal, maxVal, lastIntervalEndTs);
                intervalPatterns.add(ip);
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

        // No patterns?
        if (intervalPatterns.size() < 1) {
            return Double.NaN;
        }

        // Iterate patterns
        for (IntervalPattern ip : intervalPatterns) {
            long tSinceLastOccurrence = ts - ip.lastIntervalEndTs;
            long patternLength = (ip.length * tsDelta) + ip.interval;
            int patternsMatched = (int)Math.floor(tSinceLastOccurrence / patternLength);
            long normalizedTsinceLastOccurence = tSinceLastOccurrence - (patternsMatched * patternLength);
            debug(tSinceLastOccurrence + " tSinceLastOccurrence");
            debug(normalizedTsinceLastOccurence + " normalizedTsinceLastOccurence");
            debug(patternLength + " patternLength");
            debug(patternsMatched + " patternsMatched");
            if (normalizedTsinceLastOccurence >= ip.interval && normalizedTsinceLastOccurence <= patternLength) {
                return (ip.minVal + ip.maxVal)/2; // @todo Store avg in pattern and use that
            }
        }

        // Unable to forecast, not a peak / no peaks detected
        // @todo return value from simple regression without all the peaks
        return avg;
    }

    private class IntervalPattern {
        private final int length;
        private final int interval;
        private final double minVal;
        private final double maxVal;
        private final long lastIntervalEndTs;
        private IntervalPattern(int length, int interval, double minVal, double maxVal, long lastIntervalEndTs) {
            this.length = length;
            this.interval = interval;
            this.minVal = minVal;
            this.maxVal = maxVal;
            this.lastIntervalEndTs = lastIntervalEndTs;
        }


        public String toString() {
            return getClass().getSimpleName()  + " length=" + length + " interval=" +interval + " minval=" + minVal + " maxval=" + maxVal + " lastend=" + lastIntervalEndTs;
        }


    }
}

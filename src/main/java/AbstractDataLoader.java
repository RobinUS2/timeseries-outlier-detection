import java.util.*;

/**
 * Created by robin on 21/06/15.
 */
public abstract class AbstractDataLoader implements IDataLoader {
    private HashMap<String, String> settings;
    private HashMap<String, Timeseries> timeseries;
    private List<Long> expectedErrors;
    private List<TimeserieOutlier> outliers;
    private List<TimeserieInlier> inliers;
    public final int LOG_ERROR = 1;
    public final int LOG_WARN = 2;
    public final int LOG_NOTICE = 3;
    public final int LOG_INFO = 4;
    public final int LOG_DEBUG = 5;
    private final int LOGLEVEL = LOG_INFO;
    private long targetTsStepResolution = 60; // Default, @todo configure
    private long forecastPeriods = 10; // Amount of periods to forecast, @todo configure
    private ValueNormalizationModes valueNormalizationMode = ValueNormalizationModes.NONE; // @todo Configure

    public AbstractDataLoader() {
        settings = new HashMap<String, String>();
        timeseries = new HashMap<String, Timeseries>();
        expectedErrors = new ArrayList<Long>();
        outliers = new ArrayList<TimeserieOutlier>();
        inliers = new ArrayList<TimeserieInlier>();
    }

    public void log(int type, String className, String msg) {
        if (type > LOGLEVEL) {
            return;
        }
        msg = "[" + getConfig("name", "") + "] [" + className + "] " + msg;
        switch(type) {
            case LOG_ERROR:
            case LOG_WARN:
                System.err.println("ERR: " + msg);
                break;
            default:
                System.out.println(msg);
        }
    }

    public void setConfig(String k, String v) {
        settings.put(k, v);
        if (k.equalsIgnoreCase("rollup")) {
            targetTsStepResolution = Long.parseLong(v);
        }
    }

    public void setForecastPeriods(int x) {
        setConfig("forecast_periods", String.valueOf(x));
        forecastPeriods = x;
    }

    public void setDesiredTimeResolution(int x) {
        setConfig("desired_time_resolution", String.valueOf(x));
        targetTsStepResolution = x;
    }

    public String getConfig(String k, String d) {
        return settings.getOrDefault(k, d);
    }

    public List<TimeserieOutlier> analyze(List<ITimeserieAnalyzer> analyzers) {
        outliers.clear();
        int activeAnalyzers = 0;
        for (ITimeserieAnalyzer analyzer : analyzers) {
            TimeserieAnalyzerResult res = analyzer.analyze(this, timeseries);
            List<TimeserieOutlier> analyzerOutliers = res.getOutliers();
            List<TimeserieInlier> analyzerInliers = res.getInliers();
            if (analyzerOutliers.isEmpty() && analyzerInliers.isEmpty()) {
                // Not active
                continue;
            }
            activeAnalyzers++;
            outliers.addAll(analyzerOutliers);
            inliers.addAll(analyzerInliers);
        }
        if (activeAnalyzers < 1) {
            log(LOG_ERROR, getClass().getSimpleName(), "No analyzers were taken into account");
        }
        return outliers;
    }

    // Convert it to a sorted TS (long) Value (double) set, fills gaps with 0's
    protected void processData(HashMap<String, HashMap<String, String>> raw) throws Exception {
        for (Map.Entry<String, HashMap<String, String>> kv : raw.entrySet()) {
            String serieName = kv.getKey();

            // New serie
            Timeseries timeserie = new Timeseries(forecastPeriods);

            // Iterate data points and convert to the right datatypes, while sorting them
            TreeMap<Long, Double> sortedMap = new TreeMap<Long, Double>();
            for (Map.Entry<String, String> tskv : kv.getValue().entrySet()) {
                // TS
                Long ts = Long.parseLong(tskv.getKey());

                // Bucket ts
                ts = ts - (ts % targetTsStepResolution);

                // Val
                Double val = Double.parseDouble(tskv.getValue());
                val = normalizeValue(val);

                // Add
                if (!sortedMap.containsKey(ts)) {
                    sortedMap.put(ts, val);
                } else {
                    // Sum
                    double tmp = sortedMap.get(ts);
                    tmp += val;
                    sortedMap.put(ts, tmp);
                }
            }

            // Fill gaps
            long tsInterval = targetTsStepResolution;
            long tsPrev = 0;
            HashMap<Long, Double> fills = new HashMap<Long, Double>();
            for (Long ts : sortedMap.keySet()) {
                long actualTsInterval = ts - tsPrev;
                if (tsPrev != 0 && tsInterval != actualTsInterval) {
                    //System.err.println("Gap after " + tsPrev + " until " + ts);
                    long gapSize = (actualTsInterval - tsInterval) / tsInterval; // Interval should be 1*tsInterval
                    long gapTs = tsPrev;
                    for (int i = 0; i < gapSize; i++) {
                        gapTs += tsInterval;
                        //System.out.println("Filing gap" + gapTs);
                        fills.put(gapTs, 0D) ;// @todo Configure 0, or average, or previous, or ..
                    }
                }
                tsPrev = ts;
            }
            for (Map.Entry<Long, Double> kvFill : fills.entrySet()) {
                sortedMap.put(kvFill.getKey(), kvFill.getValue());
            }

            // Skip empty datasets
            if (sortedMap.size() == 0) {
                continue;
            }

            // Put in timeserie
            timeserie.setData(sortedMap);

            // Alert policy
            if (serieName.equals("error")) {
                timeserie.setAlertPolicy(true, false); // Do not alert if lower than expected
            }

            // Store result
            timeseries.put(serieName, timeserie);
        }

        // Many datapoints? Auto rollup
        _autoRollup();

        // Derive timeseries
        _deriveErrorRate();

        // Auto normalize data based on best practices
        _autoNormalizeData();
    }

    protected void _autoNormalizeData() throws Exception {
        if (valueNormalizationMode != ValueNormalizationModes.NONE) {
            return;
        }
        for (Timeseries ts : timeseries.values()) {
            double minMaxDelta = ts.getTrainMaxVal() - ts.getTrainMinVal();
            if (minMaxDelta >= 1000D) {
                // More than X absolute difference between min, max, apply log normalization
                log(LOG_INFO, getClass().getSimpleName(), "normalizing data");
                log(LOG_DEBUG, getClass().getSimpleName(), "max-min value delta " + minMaxDelta);
                _printTimeserieDebug(ts);

                // Normalize points
                TreeMap<Long, Double> sortedMap = new TreeMap<Long, Double>();
                for (Map.Entry<Long, Double> rts : ts.getData().entrySet()) {
                    sortedMap.put(rts.getKey(), normalizeValue(ValueNormalizationModes.LOG, rts.getValue()));
                }
                ts.setData(sortedMap);
                _printTimeserieDebug(ts);
            }
        }
    }

    protected void _printTimeserieDebug(Timeseries ts) {
        log(LOG_DEBUG, getClass().getSimpleName(), "min value " + ts.getTrainMinVal());
        log(LOG_DEBUG, getClass().getSimpleName(), "max value " + ts.getTrainMaxVal());
        log(LOG_DEBUG, getClass().getSimpleName(), "avg value " + ts.getTrainAvg());
        log(LOG_DEBUG, getClass().getSimpleName(), "stddev value " + ts.getTrainStdDev());
    }

    protected void _deriveErrorRate() throws Exception {
        if (timeseries.containsKey("regular") && timeseries.containsKey("error")) {
            log(LOG_DEBUG, getClass().getSimpleName(), "Deriving error rate timeseries");
            Timeseries timeserie = new Timeseries(forecastPeriods);
            TreeMap<Long, Double> sortedMap = new TreeMap<Long, Double>();
            for (Map.Entry<Long, Double> rts : timeseries.get("regular").getData().entrySet()) {
                double regular = rts.getValue();
                double errors = timeseries.get("error").getData().get(rts.getKey());
                double rate = 0.0D;
                if (regular > 0 && errors > 0) {
                    rate = errors / regular;
                } else if (errors > 0 && rate == 0) {
                    rate = 1.0; // All errors, prevent infinite
                }
                sortedMap.put(rts.getKey(), rate);
            }
            timeserie.setData(sortedMap);
            timeserie.setAlertPolicy(true, false); // Do not alert if lower than expected
            timeseries.put("error_rate", timeserie);
        }
    }

    protected void _autoRollup() throws Exception {
        for (Timeseries ts : timeseries.values()) {
            while (true) {
                long size = ts.getData().size();
                if (size > 1440 && targetTsStepResolution == 60) {
                    // rollup to 5 minute windows if you have at least a day
                    targetTsStepResolution = 300;
                } else if (size > 864 && targetTsStepResolution == 300) {
                    // rollup to 15 minute windows if you have at least three days
                    targetTsStepResolution = 900;
                } else if (size > 480 && targetTsStepResolution == 900) {
                    // rollup to 30 minute windows if you have at least five days
                    targetTsStepResolution = 1800;
                } else {
                    // No more options
                    break;
                }
                log(LOG_DEBUG, getClass().getSimpleName(), "Rollup resolution to " + targetTsStepResolution);
                for (Timeseries tsR : timeseries.values()) {
                    tsR.rollup(targetTsStepResolution);
                }
            }
            break;
        }
    }

    // Validate
    public ArrayList<ValidatedTimeserieOutlier> validate() {
        return validate(1);
    }

    // Validate with custom minimum score
    public ArrayList<ValidatedTimeserieOutlier> validate(int minScore) {
        ArrayList<ValidatedTimeserieOutlier> validatedOutliers = new ArrayList<ValidatedTimeserieOutlier>();

        // Scored anomalies
        HashMap<Long, Integer> scoredOutliers = new HashMap<Long, Integer>();
        HashMap<Long, Integer> outliersCount = new HashMap<Long, Integer>();
        for (TimeserieOutlier o : outliers) {
            log(LOG_DEBUG, getClass().getSimpleName(), "Outlier at " + o.getTs() + " found by " + o.getAnalyzerName());
            scoredOutliers.put(o.getTs(), scoredOutliers.getOrDefault(o.getTs(), 0) + o.getAnalyzer().getOutlierScore());
            outliersCount.put(o.getTs(), outliersCount.getOrDefault(o.getTs(), 0) + 1);
        }
        for (TimeserieInlier o : inliers) {
            log(LOG_DEBUG, getClass().getSimpleName(), "Inlier at " + o.getTs() + " found by " + o.getAnalyzerName());
            scoredOutliers.put(o.getTs(), scoredOutliers.getOrDefault(o.getTs(), 0) - o.getAnalyzer().getInlierScore());
        }

        // Did we find the expected ones?
        for (Long expectedErr : expectedErrors) {
            int matches = outliersCount.get(expectedErr);
            double score = scoredOutliers.get(expectedErr);
            log(LOG_DEBUG, getClass().getSimpleName(), "Error at " + expectedErr + " found " + matches + " time(s) with score " + score);

            // Not found?
            if (matches < 1) {
                log(LOG_ERROR, getClass().getSimpleName(), "Did not find error on " + expectedErr);
            }
        }

        // Real unexpected errors
        for (Map.Entry<Long, Integer> kv : scoredOutliers.entrySet()) {
            // Minimum score
            if (kv.getValue() < minScore) {
                continue;
            }

            // Validated outlier
            validatedOutliers.add(new ValidatedTimeserieOutlier(kv.getKey(), kv.getValue()));

            // Expected errors are not shown as error message
            if (!expectedErrors.contains(kv.getKey())) {
                log(LOG_ERROR, getClass().getSimpleName(), "Found unexpected error at " + kv.getKey() + " net score " + kv.getValue());
            }
        }

        // List of outliers
        return validatedOutliers;
    }


    // Load data
    public void load() throws Exception {
        // Load settings
        HashMap<String, String> dataSettings = loadSettings();
        for (Map.Entry<String, String> kv : dataSettings.entrySet()) {
            setConfig(kv.getKey(), kv.getValue());
        }

        // Load raw
        HashMap<String, HashMap<String, String>> raw = loadRawData();
        log(LOG_DEBUG, getClass().getSimpleName(), raw.toString());

        // Process
        processData(raw);
        log(LOG_DEBUG, getClass().getSimpleName(), timeseries.toString());

        // Load expected errors
        expectedErrors = loadExpectedErrors();
        ArrayList<Long> tmp = new ArrayList<Long>();
        for (Long l : expectedErrors) {
            long lb = l - (l % targetTsStepResolution);
            if (tmp.contains(lb)) {
                continue;
            }
            tmp.add(lb);
        }
        expectedErrors = tmp;
        log(LOG_DEBUG, getClass().getSimpleName(), expectedErrors.toString());
    }

    public double normalizeValue(ValueNormalizationModes mode, double in) {
        switch (mode) {
            case LOG:
                if (in < 1 / Double.MAX_VALUE) {
                    return 0;
                }
                return Math.log(in);
            case LOG10:
                if (in < 1 / Double.MAX_VALUE) {
                    return 0;
                }
                return Math.log10(in);
            case LOG_NATURAL:
                if (in < 1 / Double.MAX_VALUE) {
                    return 0;
                }
                return Math.log1p(in);
            case SQRT:
                return Math.sqrt(in);
            case NONE:
            default:
                // None
                return in;
        }
    }

    public double normalizeValue(double in) {
        return normalizeValue(valueNormalizationMode, in);
    }

}

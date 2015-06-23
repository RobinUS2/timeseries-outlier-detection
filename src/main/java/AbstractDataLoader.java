import com.sun.org.apache.xalan.internal.lib.ExsltDatetime;
import org.omg.CosNaming.NamingContextExtPackage.AddressHelper;

import java.util.*;

/**
 * Created by robin on 21/06/15.
 */
public abstract class AbstractDataLoader implements IDataLoader {
    private HashMap<String, String> settings;
    private HashMap<String, Timeseries> timeseries;
    private List<Long> expectedErrors;
    private List<TimeserieOutlier> outliers;
    public final int LOG_ERROR = 1;
    public final int LOG_WARN = 2;
    public final int LOG_NOTICE = 3;
    public final int LOG_INFO = 4;
    public final int LOG_DEBUG = 5;
    private final int LOGLEVEL = LOG_DEBUG;
    private long targetTsStepResolution = 60; // Default

    public AbstractDataLoader() {
        settings = new HashMap<String, String>();
        timeseries = new HashMap<String, Timeseries>();
        expectedErrors = new ArrayList<Long>();
        outliers = new ArrayList<TimeserieOutlier>();
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

    public String getConfig(String k, String d) {
        return settings.getOrDefault(k, d);
    }

    public List<TimeserieOutlier> analyze(List<ITimeserieAnalyzer> analyzers) {
        outliers.clear();
        int activeAnalyzers = 0;
        for (ITimeserieAnalyzer analyzer : analyzers) {
            List<TimeserieOutlier> analyzerOutliers = analyzer.analyze(this, timeseries);
            if (analyzerOutliers == null) {
                // Not active
                continue;
            }
            activeAnalyzers++;
            outliers.addAll(analyzerOutliers);
        }
        if (activeAnalyzers < 1) {
            log(LOG_ERROR, getClass().getSimpleName(), "No analyzers were taken into account");
        }
        return outliers;
    }

    // Convert it to a sorted TS (long) Value (double) set, fills gaps with 0's
    protected void processData(HashMap<String, HashMap<String, String>> raw) {
        for (Map.Entry<String, HashMap<String, String>> kv : raw.entrySet()) {
            String serieName = kv.getKey();

            // New serie
            Timeseries timeserie = new Timeseries();

            // Iterate data points and convert to the right datatypes, while sorting them
            TreeMap<Long, Double> sortedMap = new TreeMap<Long, Double>();
            for (Map.Entry<String, String> tskv : kv.getValue().entrySet()) {
                // TS
                Long ts = Long.parseLong(tskv.getKey());

                // Bucket ts
                ts = ts - (ts % targetTsStepResolution);

                // Val
                Double val = Double.parseDouble(tskv.getValue());

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

            // Put in timeserie
            timeserie.setData(sortedMap);

            // Skip empty datasets
            if (timeserie.getData().size() == 0) {
                continue;
            }

            // Alert policy
            if (serieName.equals("error")) {
                timeserie.setAlertPolicy(true, false); // Do not alert if lower than expected
            }

            // Store result
            timeseries.put(serieName, timeserie);
        }

        // Derive timeseries
        if (timeseries.containsKey("regular") && timeseries.containsKey("error")) {
            log(LOG_DEBUG, getClass().getSimpleName(), "Deriving error rate timeseries");
            Timeseries timeserie = new Timeseries();
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

    // Validate
    public void validate() {
        // Did we find the expected ones?
        for (Long expectedErr : expectedErrors) {
            boolean found = false;
            int matches = 0;
            for (TimeserieOutlier o : outliers) {
                if (expectedErr == o.getTs()) {
                    // Found expected
                    matches++;
                }
            }
            log(LOG_DEBUG, getClass().getSimpleName(), "Error at " + expectedErr + " found " + matches + " time(s)");

            // Not found?
            if (matches < 1) {
                log(LOG_ERROR, getClass().getSimpleName(), "Did not find error on " + expectedErr);
            }
        }

        // Unexpected errors?
        HashMap<Long, Integer> unexpectedErrors = new HashMap<Long, Integer>();
        for (TimeserieOutlier o : outliers) {
            if (expectedErrors.contains(o.getTs())) {
                continue;
            }
            if (!unexpectedErrors.containsKey(o.getTs())) {
                unexpectedErrors.put(o.getTs(), 1);
            } else {
                unexpectedErrors.put(o.getTs(), unexpectedErrors.get(o.getTs()) + 1);
            }
            log(LOG_ERROR, getClass().getSimpleName(), "Found unexpected error at " + o.getTs() + " triggered by " + o.getAnalyzerName() + " " + o.getLeftBound() + " > " + o.getVal() + " < " + o.getRightBound());
        }
        if (unexpectedErrors.size() > 0) {
            log(LOG_ERROR, getClass().getSimpleName(), "Unexpected errors " + unexpectedErrors.toString());
        }
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

}

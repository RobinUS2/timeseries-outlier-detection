import com.sun.org.apache.xalan.internal.lib.ExsltDatetime;

import java.util.*;

/**
 * Created by robin on 21/06/15.
 */
public abstract class AbstractDataLoader implements IDataLoader {
    private HashMap<String, String> settings;
    private HashMap<String, Timeseries> timeseries;
    private List<Long> expectedErrors;

    public AbstractDataLoader() {
        settings = new HashMap<String, String>();
        timeseries = new HashMap<String, Timeseries>();
        expectedErrors = new ArrayList<Long>();
    }

    public void setConfig(String k, String v) {
        settings.put(k, v);
    }

    public String getConfig(String k, String d) {
        return settings.getOrDefault(k, d);
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

                // Val
                Double val = Double.parseDouble(tskv.getValue());

                // Add
                sortedMap.put(ts, val);
            }

            // Fill gaps
            long tsInterval = 60; // @todo Dynamic / configurable
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

            timeserie.setData(sortedMap);

            // Store result
            timeseries.put(serieName, timeserie);
        }
    }

    // Load data
    public void load() throws Exception {
        // Load raw
        HashMap<String, HashMap<String, String>> raw = loadRawData();
        //System.out.println(raw);

        // Process
        processData(raw);
        System.out.println(timeseries);

        // Load expected errors
        expectedErrors = loadExpectedErrors();
        //System.out.println(expectedErrors);
    }

}

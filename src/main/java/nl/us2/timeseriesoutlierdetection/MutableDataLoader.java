package nl.us2.timeseriesoutlierdetection;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by robin on 21/06/15.
 */
public class MutableDataLoader extends AbstractDataLoader {
    private ArrayList<Long> expectedErrors;
    private HashMap<String, String> settings;
    private HashMap<String, HashMap<String, String>> rawSeries;

    public MutableDataLoader(String name) {
        setConfig("name", name);
        expectedErrors = new ArrayList<Long>();
        settings = new HashMap<String, String>();
        rawSeries = new HashMap<String, HashMap<String, String>>();
    }

    public ArrayList<Long> loadExpectedErrors() {
        return expectedErrors;
    }

    public void addExpectedError(long timestamp) {
        expectedErrors.add(timestamp);
    }

    public void addExpectedErrors(long[] timestamps) {
        for (long ts : timestamps) {
            expectedErrors.add(ts);
        }
    }

    public HashMap<String, String> loadSettings() {
        return settings;
    }

    public void addData(String serie, HashMap<String, String> values) {
        HashMap<String, String> tmp = rawSeries.getOrDefault(serie, new HashMap<String, String>());
        tmp.putAll(values);
        rawSeries.put(serie, tmp);
    }

    public void addData(String serie, String ts, String val) {
        HashMap<String, String> tmp = rawSeries.getOrDefault(serie, new HashMap<String, String>());
        tmp.put(ts, val);
        rawSeries.put(serie, tmp);
    }

    public HashMap<String, HashMap<String, String>> loadRawData() throws Exception {
        return rawSeries;
    }
}

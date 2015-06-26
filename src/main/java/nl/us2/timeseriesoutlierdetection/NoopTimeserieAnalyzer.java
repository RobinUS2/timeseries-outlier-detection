package nl.us2.timeseriesoutlierdetection;

import java.util.HashMap;

/**
 * Created by robin on 21/06/15.
 */
public class NoopTimeserieAnalyzer extends AbstractTimeserieAnalyzer implements ITimeserieAnalyzer {
    public TimeserieAnalyzerResult analyze(AbstractDataLoader dataLoader, HashMap<String, Timeseries> timeseries) {
        TimeserieAnalyzerResult res = new TimeserieAnalyzerResult();
        // Does absolutely nothing
        return res;
    }
}

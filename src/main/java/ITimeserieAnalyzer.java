import java.util.HashMap;

/**
 * Created by robin on 21/06/15.
 */
public interface ITimeserieAnalyzer {
     TimeserieAnalyzerResult analyze(AbstractDataLoader dataLoader, HashMap<String, Timeseries> timeseries);
}

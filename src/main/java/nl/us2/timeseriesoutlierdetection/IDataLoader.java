package nl.us2.timeseriesoutlierdetection;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by robin on 21/06/15.
 */
public interface IDataLoader {
    public ArrayList<Long> loadExpectedErrors() throws Exception;
    HashMap<String, HashMap<String, String>> loadRawData() throws Exception;
    HashMap<String, String> loadSettings() throws Exception;
}

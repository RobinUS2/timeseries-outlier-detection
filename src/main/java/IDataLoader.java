import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by robin on 21/06/15.
 */
public interface IDataLoader {
    public ArrayList<Long> loadExpectedErrors() throws Exception;
    HashMap<String, HashMap<String, String>> loadRawData() throws Exception;
}

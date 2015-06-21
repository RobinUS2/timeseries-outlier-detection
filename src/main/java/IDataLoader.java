import java.util.HashMap;

/**
 * Created by robin on 21/06/15.
 */
public interface IDataLoader {
    HashMap<String, HashMap<String, String>> loadRawData() throws Exception;
}

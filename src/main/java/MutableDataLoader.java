import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by robin on 21/06/15.
 */
public class MutableDataLoader extends AbstractDataLoader {
    private ArrayList<Long> expectedErrors;
    private HashMap<String, String> settings;

    public MutableDataLoader(String path) {
        setConfig("path", path);
        String[] pSplit = path.split("/");
        setConfig("name", pSplit[pSplit.length - 1]);
        expectedErrors = new ArrayList<Long>();
        settings = new HashMap<String, String>();
    }

    public ArrayList<Long> loadExpectedErrors() {
        return expectedErrors;
    }

    public HashMap<String, String> loadSettings() {
        return settings;
    }


    public HashMap<String, HashMap<String, String>> loadRawData() throws Exception {
        throw new NotImplementedException();
    }
}

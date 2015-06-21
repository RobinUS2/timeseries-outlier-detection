import java.util.HashMap;

/**
 * Created by robin on 21/06/15.
 */
public abstract class AbstractDataLoader implements IDataLoader {
    private HashMap<String, String> settings;

    public AbstractDataLoader() {
        settings = new HashMap<String, String>();
    }

    public void setConfig(String k, String v) {
        settings.put(k, v);
    }

    public String getConfig(String k, String d) {
        return settings.getOrDefault(k, d);
    }

    public void load() throws Exception {
        HashMap<String, HashMap<String, String>> raw = loadRawData();
        System.out.println(raw);
    }

}

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;

/**
 * Created by robin on 21/06/15.
 */
public class FileDataLoader extends AbstractDataLoader {
    public FileDataLoader(String path) {
        setConfig("path", path);
    }
    public HashMap<String, HashMap<String, String>> loadRawData() throws Exception {
        // Series holders
        HashMap<String, HashMap<String, String>> series = new HashMap<String, HashMap<String, String>>();
        series.put("regular", new HashMap<String, String>());
        series.put("error", new HashMap<String, String>());

        // Read file
        List<String> lines = FileUtils.readLines(new File(getConfig("path", null)));

        // Col indices
        int colTs = -1;
        int colRegular = -1;
        int colError = -1;

        // Iterate lines
        long i = 0L;
        for (String line : lines) {
            // Split
            String[] cols = line.split("\\s+");

            // Mapping of series
            if (i == 0L) {
                int colI=0;
                for (String col : cols) {
                    if (col.equals("ts")) {
                        colTs = colI;
                    } else if (col.equals("regular")) {
                        colRegular = colI;
                    } else if (col.startsWith("error")) {
                        colError = colI;
                    }
                    colI++;
                }
            } else {
                String tsStr = cols[colTs];
                if (colRegular != -1) {
                    series.get("regular").put(tsStr, cols[colRegular]);
                }
                if (colError != -1) {
                    series.get("error").put(tsStr, cols[colError]);
                }
            }

            // Next line
            i++;
        }
        return series;
    }
}

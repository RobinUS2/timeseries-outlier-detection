import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by robin on 21/06/15.
 */
public class TestRunner {
    public void run() throws Exception {
        // List files
        URL url = this.getClass().getClassLoader().getResource("testdata");
        File folder = new File(url.toURI());
        File[] listOfFiles = folder.listFiles();
        ArrayList<String> testDataFiles = new ArrayList<String>();
        for (File f : listOfFiles) {
            if (!f.isFile()) {
                continue;
            }
            String p = f.getAbsolutePath();
            if (!p.endsWith(".tsv")) {
                continue;
            }
            // Testing only
//            if (!p.contains("real_test_5")) {
//                continue;
//            }
            testDataFiles.add(p);
        }

        // List of analyzers
        List<ITimeserieAnalyzer> analyzers = new ArrayList<ITimeserieAnalyzer>();
        analyzers.add(new NormalDistributionTimeserieAnalyzer());
        analyzers.add(new LogNormalDistributionTimeserieAnalyzer());
        analyzers.add(new SimpleRegressionTimeserieAnalyzer());
        analyzers.add(new MovingAverageTimeserieAnalyzer());
        analyzers.add(new PolynomialRegressionTimeserieAnalyzer());
        analyzers.add(new IntervalInterceptorTimeserieAnalyzer());

        // Load
        AbstractDataLoader dl;
        for (String p : testDataFiles) {
            dl = new FileDataLoader(p);
            dl.load();
            dl.analyze(analyzers);
            dl.validate();
        }
    }
}

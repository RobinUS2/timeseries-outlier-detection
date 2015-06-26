package nl.us2.timeseriesoutlierdetection;

import java.util.Date;

/**
 * Created by robin on 21/06/15.
 */
public class Main {
    public static void main(String [ ] args) throws Exception
    {
        long start = new Date().getTime();
        TestRunner tr = new TestRunner();
        tr.run();
        long end = new Date().getTime();
        System.out.println("Took " + (end - start) + " ms");
    }
}

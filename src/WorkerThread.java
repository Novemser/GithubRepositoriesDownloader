import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by Novemser on 2016/9/27.
 */
public class WorkerThread implements Runnable {

    public WorkerThread(PrintWriter writer, StringBuilder dir, String fullName, String archive, ThreadPoolExecutor executor) {
        this.writer = writer;
        this.dir = dir;
        this.fullName = fullName;
        this.archiveUrl = archive;
        this.executor = executor;
    }

    private PrintWriter writer;
    private StringBuilder dir;
    private String fullName;
    private String archiveUrl;
    private ThreadPoolExecutor executor;


    @Override
    public void run() {
        try {
            while (!Utils.checkAPIRateLimit(executor)){
                Thread.sleep(1000 * 60 * 5);
            }

        } catch (UnirestException | InterruptedException e) {
            e.printStackTrace();
        }

        try {
            String fileAbsName = dir.append(fullName).append(".zip").toString();

            // If file already exists
            File f = new File(fileAbsName);

            if (f.exists() && !f.isDirectory()) {
                String time = fullName + " already exists! Time:" + Utils.getTimeFormitted();
                Utils.logln(writer, time);
                System.out.println(time);
            }

            // File doesn't exist
            else {
                Main.saveZipToFile(fileAbsName, archiveUrl);
                String time = "Save " + fullName + " successfully! Time:" + Utils.getTimeFormitted();
                Utils.logln(writer, time);
                System.out.println(time);
            }

        } catch (IOException e) {
            e.printStackTrace();
            Utils.logln(writer, "Error in worker at:" + Utils.getTimeFormitted());
            Utils.logln(writer, e.getMessage());
        }
    }

}

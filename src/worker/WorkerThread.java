package worker;

import com.mashape.unirest.http.exceptions.UnirestException;
import util.Utils;

import java.io.File;
import java.io.FileNotFoundException;
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
    private final int RETRY_TIME = 10;

    @Override
    public void run() {
        try {
            while (!Utils.checkAPIRateLimit(executor)) {
                Thread.sleep(1000 * 60 * 5);
            }
        } catch (UnirestException e) {
            Utils.logMsgWithTime(writer, "Check api rate limit failed.");
            System.out.println("Check api rate limit failed.");
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String fileAbsName = dir.append(fullName).append(".zip").toString();

        // If file already exists
        File f = new File(fileAbsName);

        // File already exist and not corrupt
        if (f.exists() && !f.isDirectory() && Utils.isZipFileValid(f)) {
            String time = fullName + " already exists! Time:" + Utils.getTimeFormitted();
            Utils.logMsgWithTime(writer, fullName + " already exists.");
            System.out.println(time);
        }
        // File doesn't exist or corrupted
        else {
            if (f.exists())
                f.delete();
            // Try retryNum times
            int retryNum = RETRY_TIME;
            while (retryNum-- > 0) {
                try {
                    Utils.saveZipToFile(fileAbsName, archiveUrl);

                    Utils.logMsgWithTime(writer, "Save " + fullName + " successfully.");
                    String time = "Save " + fullName + " successfully! Time:" + Utils.getTimeFormitted();
                    System.out.println(time);

                    return;
                } catch (IOException e) {
                    if (e instanceof FileNotFoundException) {
                        Utils.logMsgWithTime(writer, fullName + " does not exist.");
                        e.printStackTrace();
                        return;
                    }

                    Utils.logMsgWithTime(writer, "Save " + fullName + " failed. | " + e);
                    e.printStackTrace();
                }
            }

            Utils.logMsgWithTime(writer, "Save " + fullName + " failed after 10 attempts");
            System.out.println("Save " + fullName + " failed after 10 attempts Time:" + Utils.getTimeFormitted());
        }

    }

}

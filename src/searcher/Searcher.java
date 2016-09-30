package searcher;

import com.mashape.unirest.http.Unirest;
import org.apache.http.HttpHost;
import util.HttpHelper;
import util.Utils;

import java.io.PrintWriter;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Novemser on 2016/9/29.
 */
public class Searcher {
    protected ThreadPoolExecutor fixedThreadPool;
    protected ThreadPoolExecutor fixedThreadPoolMinor;
    protected boolean running;
    final static String rootFolder = "D:/GithubCodes";

    public Searcher(int maxThread) {

        // Init a thread pool
        fixedThreadPool = new ThreadPoolExecutor(maxThread, maxThread,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
        fixedThreadPoolMinor = new ThreadPoolExecutor(maxThread / 2, maxThread / 2,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
        startMonitor(fixedThreadPool);
        startMonitor(fixedThreadPoolMinor);

        // Start...
        HttpHelper.startProducing();

        // Use proxy to craw unimportant information
        HttpHost host = HttpHelper.getNextAvailProxy();
        System.out.println("Init using " + host.getHostName() + ":" + host.getPort());
        Unirest.setProxy(host);

        // Listen that if the proxy is too slow
        // Then change another
        listenAndChange();
    }

    private void listenAndChange() {
        Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            long lastComplete = 0;

            @Override
            public void run() {
                if (lastComplete == 0) {
                    lastComplete = fixedThreadPoolMinor.getCompletedTaskCount();
                } else {
                    long nowComplete = fixedThreadPoolMinor.getCompletedTaskCount();

                    long delta = nowComplete - lastComplete;
                    System.err.println("Last thread pool delta:" + delta);
                    if (delta < 50) {
                        System.err.println("Proxy too slow, changing...");
                        HttpHelper.forceChangeProxy(fixedThreadPoolMinor);
                        System.err.println("Change proxy successfully");
                    }

                    lastComplete = nowComplete;
                }
            }
        }, 0, 1000 * 60);
    }

    protected void startListenExit(PrintWriter writer) {
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            if (scanner.hasNext()) {
                running = false;
                System.err.println("Downloading stopped.");
                Utils.logMsgWithTime(writer, "Manually stop downloading.");
            }
        }).start();
    }

    protected void startMonitor(ThreadPoolExecutor executor) {
        new Thread(() -> {
            while (!executor.isShutdown() || executor.getActiveCount() > 0) {
                synchronized (this) {


                    System.out.print(Utils.getTimeFormitted());
                    System.out.println(Utils.ANSI_YELLOW +
                            String.format(" | [monitor] [%d/%d] Active: %d, Completed: %d, Task: %d, isShutdown: %s, isTerminated: %s",
                                    executor.getPoolSize(),
                                    executor.getCorePoolSize(),
                                    executor.getActiveCount(),
                                    executor.getCompletedTaskCount(),
                                    executor.getTaskCount(),
                                    executor.isShutdown(),
                                    executor.isTerminated())
                            + Utils.ANSI_RESET
                    );
                }

                try {
                    Thread.sleep(1000 * 5);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }).start();

    }

    protected void sleepSome(PrintWriter logFile)  {
        Utils.logMsgWithTime(logFile, "Main loop sleep.");
        System.out.println(Utils.ANSI_RED + "Main loop sleep." + Utils.ANSI_RESET);
        try {
            Thread.sleep(1000 * 60 * 1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(Utils.ANSI_RED + "Main loop resumed." + Utils.ANSI_RESET);
        Utils.logMsgWithTime(logFile, "Main loop resumed.");
    }
}

package searcher;

import com.mashape.unirest.http.Unirest;
import com.sun.org.apache.xerces.internal.impl.xs.identity.UniqueOrKey;
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
 * GithubRepoGetter
 *
 * Created by Novemser on 2016/9/29.
 */
public abstract class Searcher {
    protected ThreadPoolExecutor fixedThreadPool;
    protected ThreadPoolExecutor fixedThreadPoolMinor;
    protected boolean running;
    protected String clientId = "c3cdb3e20ce16b2fe446";
    protected String clientSecret = "9f1ca7fb8181eebcc27c4047f531d810718ba9bd";
    final static String rootFolder = "D:/GithubCodes";
    private boolean isUsingProxy;

    public boolean isUsingProxy() {
        return isUsingProxy;
    }

    public void usingProxy() {
        isUsingProxy = true;
    }

    public Searcher(int maxThread) {

        // Init a thread pool
        fixedThreadPool = new ThreadPoolExecutor(maxThread, maxThread,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
        startMonitor(fixedThreadPool);

        fixedThreadPoolMinor = new ThreadPoolExecutor(maxThread / 2, maxThread / 2,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
        startMonitor(fixedThreadPoolMinor);

        if (isUsingProxy) {
            // Start...
            HttpHelper.startProducing();

            // Use proxy to craw unimportant information
            HttpHost host = HttpHelper.getNextAvailProxy();
            System.out.println("Init using " + host.getHostName() + ":" + host.getPort());
            Unirest.setProxy(host);
            Unirest.setTimeouts(5000, 1000 * 10);

            // Listen that if the proxy is too slow
            // Then change another
            listenAndChange();
        }


    }

    private void listenAndChange() {
        Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            long lastComplete = 0;
            int zeroCnt = 0;

            @Override
            public void run() {
                if (lastComplete == 0 && zeroCnt < 10) {
                    zeroCnt++;
                    lastComplete = fixedThreadPoolMinor.getCompletedTaskCount();
                } else {
                    long nowComplete = fixedThreadPoolMinor.getCompletedTaskCount();

                    long delta = nowComplete - lastComplete;
                    System.err.println("Last thread pool delta:" + delta);
                    if (delta < 50) {
                        System.err.println("Proxy too slow, changing...");
                        HttpHelper.forceChangeProxy(fixedThreadPoolMinor);
                    }

                    lastComplete = nowComplete;
                }
            }
        }, 0, 1000 * 60);
    }

    void startListenExit(PrintWriter writer) {
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            if (scanner.hasNext()) {
                running = false;
                System.err.println("Downloading stopped.");
                fixedThreadPoolMinor.shutdown();
                fixedThreadPool.shutdown();
                Utils.logMsgWithTime(writer, "Manually stop downloading.");
            }
        }).start();
    }

    /**
     * Run the crawler
     *
     */
    public abstract void run();

    private void startMonitor(ThreadPoolExecutor executor) {
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

    void sleepSome(PrintWriter logFile)  {
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

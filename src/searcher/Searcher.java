package searcher;

import util.Utils;

import java.io.PrintWriter;
import java.util.Scanner;
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

                try {
                    Thread.sleep(1000 * 5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }
}

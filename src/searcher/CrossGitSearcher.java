package searcher;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import org.apache.http.HttpHost;
import org.json.JSONArray;
import org.json.JSONObject;
import util.HttpHelper;
import util.Utils;
import worker.WorkerThread;

import java.io.*;
import java.util.Set;

/**
 * Created by Novemser on 2016/9/25.
 */
public class CrossGitSearcher extends Searcher {
    private String since = "2710853";
    private String initRepo = "https://api.github.com/repositories?client_id=" + clientId + "&client_secret=" + clientSecret + "&since=" + since;

//    final static String downloadTest = "https://api.github.com/repos/octokit/octokit.rb/tarball?client_id=c3cdb3e20ce16b2fe446&client_secret=9f1ca7fb8181eebcc27c4047f531d810718ba9bd";

    public CrossGitSearcher(int maxThread) {
        super(maxThread);
    }

    public void run() {
        System.out.println(Utils.ANSI_BLUE + "Start running" + Utils.ANSI_RESET);
        String nextReposList = initRepo;

        FileWriter fw = null, fileWriter = null;
        BufferedWriter bw = null, bufferedWriter = null;
        PrintWriter logFile = null, printWriter = null;

        // Create the root folder,
        // which contains all the zip files
        new File(rootFolder).mkdirs();
        new File(rootFolder + "/Log").mkdirs();

        try {
            String time = Utils.getTimeFormitted();

            fw = new FileWriter(rootFolder + "/Log/log-" + "since-" + since + "-" + time + ".txt", true);
            bw = new BufferedWriter(fw);
            logFile = new PrintWriter(bw);
            Utils.log = logFile;

            fileWriter = new FileWriter(rootFolder + "/Log/Repositories-" + time + ".txt", true);
            bufferedWriter = new BufferedWriter(fileWriter);
            printWriter = new PrintWriter(bufferedWriter);

            String beginTime = "New Crawling begins:" + Utils.getTimeFormitted();

            Utils.logln(logFile, beginTime);
            Utils.logln(printWriter, beginTime);
            Utils.logln(logFile, "===================================================================");
            Utils.logln(printWriter, "===================================================================");
            running = true;

            startListenExit(logFile);

            while (running) {
                // Reach limit?
                HttpHelper.checkAndSet(fixedThreadPool);

                // Get repo body
                GetRequest request = Unirest.get(nextReposList);
                HttpResponse<JsonNode> jsonResponse = null;
                try {
                    jsonResponse = request.asJson();
                } catch (UnirestException e) {
                    System.err.println("Connection time out in getting nextRepolist!");
                    continue;
                }
                JsonNode root = jsonResponse.getBody();
                // Log it to file
                Utils.logMsgWithTime(logFile, "Repo:" + nextReposList);

                // Get header to generate next url
                // Get next page Headers
                nextReposList = HttpHelper.getNextPageFromHeader(jsonResponse.getHeaders());
                if (nextReposList.equals("")) {
                    Utils.logMsgWithTime(logFile, "Search ended.");
                    break;
                }

                if (root.isArray()) {

                    JSONArray array = root.getArray();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject repoObj = array.getJSONObject(i);
                        String fullName = repoObj.getString("full_name").replace("/", "_");
                        StringBuilder lanUrl = new StringBuilder(repoObj.getString("languages_url"));
                        lanUrl.append("?client_id=").append(clientId).append("&client_secret=").append(clientSecret);

                        String archiveUrl = repoObj.getString("archive_url");
                        archiveUrl = archiveUrl.replace("{archive_format}{/ref}", "zipball" + "?client_id=" + clientId + "&client_secret=" + clientSecret);

                        PrintWriter finalPrintWriter = printWriter;
                        String finalArchiveUrl = archiveUrl;
                        fixedThreadPoolMinor.execute(() -> {
                            // Check language
                            HttpResponse<JsonNode> jsonResponse1 = null;
                            // Reach limit?
                            HttpHelper.checkAndSet(fixedThreadPool);

                            try {
                                jsonResponse1 = Unirest.get(lanUrl.toString()).asJson();
                            } catch (UnirestException e) {
                                HttpHelper.checkAndSet(fixedThreadPool);
                                System.err.println("Get lan failed");
                                return;
                            }

                            Set<String> keys = jsonResponse1.getBody().getObject().keySet();
                            StringBuilder dir = new StringBuilder("/");

//                        int len = keys.size();

                            if (keys.size() == 1 && keys.contains("Java"))
                                dir.append("Java");
                            else
                                return;

//                        for (String item : keys) {
//                            len--;
//                            dir.append(item);
//                            if (len > 0)
//                                dir.append("&");
//                        }

                            new File(dir.insert(0, rootFolder).toString()).mkdirs();
                            dir.append("/");

                            Runnable worker = new WorkerThread(finalPrintWriter, dir, fullName, finalArchiveUrl, fixedThreadPool);
                            fixedThreadPool.execute(worker);

                        });
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (logFile != null)
                logFile.flush();
            try {
                if (bw != null)
                    bw.flush();
            } catch (IOException e) {
                //exception handling left as an exercise for the reader
            }
            try {
                if (fw != null)
                    fw.flush();
            } catch (IOException e) {
                //exception handling left as an exercise for the reader
            }
            if (printWriter != null) {
                printWriter.flush();
            }
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (fileWriter != null) {
                    fileWriter.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void main(String... args) {
        CrossGitSearcher main = new CrossGitSearcher(5000);
        main.run();
    }
}

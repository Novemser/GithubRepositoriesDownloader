package searcher;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import util.HttpHelper;
import util.Utils;
import worker.WorkerThread;

import java.io.*;
import java.util.concurrent.*;

/**
 * Created by Novemser on 2016/9/27.
 */
public class SpecLanguageSearcher extends Searcher {

    public static void main(String... args) {
        SpecLanguageSearcher java = new SpecLanguageSearcher("Java", 1000);
        java.run();
    }

    private String clientId = "c3cdb3e20ce16b2fe446";
    private String clientSecret = "9f1ca7fb8181eebcc27c4047f531d810718ba9bd";
    private String initSearchUrl;
    private String rootFolder = "D:/GithubSearchCodes";
    private String language;
    private int maxActiveCnt = 500;

    public SpecLanguageSearcher(String language, int maxThread) {
        super(maxThread);

        initSearchUrl = "https://api.github.com/search/repositories?q=language:"
                + language
                + "&sort=stars&order=desc&client_id="
                + clientId
                + "&client_secret="
                + clientSecret;

        rootFolder += "/" + language;
        this.language = language;
    }

    public boolean checkSearchRateLimit(ThreadPoolExecutor executor) {
        String limit = "https://api.github.com/rate_limit?client_id=" + clientId + "&client_secret=" + clientSecret;

        GetRequest request = Unirest.get(limit);
        HttpResponse<JsonNode> jsonResponse = null;
        try {
            jsonResponse = request.asJson();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        JSONObject root = jsonResponse.getBody().getObject();

        JSONObject rate = root.getJSONObject("rate");
        int remaining = rate.getInt("remaining");
        boolean flag = (executor.getActiveCount() <= remaining) && remaining > 10;

        JSONObject resources = root.getJSONObject("resources");
        JSONObject search = resources.getJSONObject("search");
        int searchRemain = search.getInt("remaining");
        boolean fg = searchRemain > 1;

        boolean ac = executor.getActiveCount() < maxActiveCnt;

        if (!flag) {
            System.err.println("API limit reached." + Utils.getTimeFormitted());
        }
        if (!fg) {
            System.err.println("Search limit reached." + Utils.getTimeFormitted());
        }
        if (!ac) {
            System.err.println("Active limit reached." + Utils.getTimeFormitted());
        }

        return flag && fg && ac;
    }


    public void run() {
        String searchUrl = initSearchUrl;

        FileWriter fw = null, fileWriter = null;
        BufferedWriter bw = null, bufferedWriter = null;
        PrintWriter logFile = null, repoFile = null;

        // Create the root folder,
        // which contains all the zip files
        new File(rootFolder).mkdirs();
        new File(rootFolder + "/Log").mkdirs();

        try {
            String time = Utils.getTimeFormitted();

            fw = new FileWriter(rootFolder + "/Log/log-" + time + ".txt", true);
            bw = new BufferedWriter(fw);
            logFile = new PrintWriter(bw);
            Utils.log = logFile;

            fileWriter = new FileWriter(rootFolder + "/Log/Repos-" + time + ".txt", true);
            bufferedWriter = new BufferedWriter(fileWriter);
            repoFile = new PrintWriter(bufferedWriter);

            String beginStr = "New Crawling begins:" + Utils.getTimeFormitted() + " Language:" + language;

            Utils.logln(logFile, beginStr);
            Utils.logln(repoFile, beginStr);
            Utils.logln(logFile, "===================================================================");
            Utils.logln(repoFile, "===================================================================");
            running = true;

            // Listen for manual shutdown
            startListenExit(logFile);

            while (running) {
                // Reach limit
                if (!checkSearchRateLimit(fixedThreadPool)) {
                    // Sleep 5 minutes
                    sleepSome(logFile);
                }

                HttpResponse<JsonNode> response = null;
                try {
                    response = HttpHelper.getJsonResponse(searchUrl);
                    Utils.logMsgWithTime(logFile, "Searching " + searchUrl);
                } catch (UnirestException e) {
                    Utils.logMsgWithTime(logFile, "Search " + searchUrl + " Failed.");
                    e.printStackTrace();
                    continue;
                }
                // Get next page Headers
                searchUrl = HttpHelper.getNextPageFromHeader(response.getHeaders());
                if (searchUrl.equals("")) {
                    Utils.logMsgWithTime(logFile, "Search ended.");
                    break;
                }

                // Get http response body
                JsonNode rootNode = response.getBody();
                // Get root object
                JSONObject root = rootNode.getObject();
                // Get result list
                JSONArray resultList = root.getJSONArray("items");
                // Iterate all the results in one JsonArray
                for (int i = 0; i < resultList.length(); i++) {
                    // Get one obj
                    JSONObject repoObj = resultList.getJSONObject(i);

                    String fullName = repoObj.getString("full_name").replace("/", "__");
                    String archiveUrl = repoObj.getString("archive_url");
                    archiveUrl = archiveUrl.replace("{archive_format}{/ref}", "zipball" + "?client_id=" + clientId + "&client_secret=" + clientSecret);

                    StringBuilder dir = new StringBuilder(rootFolder);
                    dir.append("/Repositories");
//                    dir.append(fullName);

                    new File(dir.toString()).mkdirs();
                    dir.append("/");

//                    System.out.println("Start worker...");
                    Runnable worker = new WorkerThread(repoFile, dir, fullName, archiveUrl, fixedThreadPool);
                    fixedThreadPool.execute(worker);
                }

            }
        } catch (IOException e) {
            if (logFile != null)
                Utils.logMsgWithTime(logFile, "Error terminated:" + e);
            e.printStackTrace();

            if (logFile != null) {
                logFile.close();
                logFile = null;
            }

            if (bw != null)
                try {
                    bw.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            if (fw != null)
                try {
                    fw.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            if (repoFile != null) {
                repoFile.close();
                repoFile = null;
            }

            if (bufferedWriter != null)
                try {
                    bufferedWriter.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            if (fileWriter != null)
                try {
                    fileWriter.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

        } finally {
            // shut down
            fixedThreadPool.shutdown();
            if (logFile != null) {
                Utils.logMsgWithTime(logFile, "All downloads terminating...");
            }
        }

    }
}

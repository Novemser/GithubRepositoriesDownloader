import com.mashape.unirest.http.Headers;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Novemser on 2016/9/25.
 */
public class Main {
    private String clientId = "c3cdb3e20ce16b2fe446";
    private String clientSecret = "9f1ca7fb8181eebcc27c4047f531d810718ba9bd";
    private String initRepo = "https://api.github.com/repositories?client_id=" + clientId + "&client_secret=" + clientSecret + "&since=50067";

    private ThreadPoolExecutor fixedThreadPool;

    final static String downloadTest = "https://api.github.com/repos/octokit/octokit.rb/tarball?client_id=c3cdb3e20ce16b2fe446&client_secret=9f1ca7fb8181eebcc27c4047f531d810718ba9bd";

    final static String rootFolder = "F:/GithubCodes";

    public void run(int maxThreadNum) {
        String nextReposList = initRepo;

        FileWriter fw = null, fileWriter = null;
        BufferedWriter bw = null, bufferedWriter = null;
        PrintWriter out = null, printWriter = null;

        // Create the root folder,
        // which contains all the zip files
        new File(rootFolder).mkdirs();

        // Init a thread pool
        fixedThreadPool = new ThreadPoolExecutor(maxThreadNum, maxThreadNum,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
        int listCnt = 0;

        try {
            String time = Utils.getTimeFormitted();

            fw = new FileWriter("F:/log-" + time + ".txt", true);
            bw = new BufferedWriter(fw);
            out = new PrintWriter(bw);
            Utils.log = out;

            fileWriter = new FileWriter("F:/Repos-" + time + ".txt", true);
            bufferedWriter = new BufferedWriter(fileWriter);
            printWriter = new PrintWriter(bufferedWriter);

            String beginTime = "New Crawling begins:" + Utils.getTimeFormitted();

            Utils.logln(out, beginTime);
            Utils.logln(printWriter, beginTime);
            Utils.logln(out, "===================================================================");
            Utils.logln(printWriter, "===================================================================");

            while (true) {
                // Reach limit
                try {
                    while (!Utils.checkAPIRateLimit(fixedThreadPool)) {
                        // Sleep 10 minutes
                        Thread.sleep(1000 * 60 * 10);
                    }
                } catch (UnirestException | InterruptedException e) {
                    e.printStackTrace();
                    continue;
                }

                // Get repo body
                GetRequest request = Unirest.get(nextReposList);
                HttpResponse<JsonNode> jsonResponse = null;
                try {
                    jsonResponse = request.asJson();
                } catch (UnirestException e) {
                    e.printStackTrace();
                    continue;
                }
                JsonNode root = jsonResponse.getBody();
                // Log it to file
                Utils.logln(out, Utils.getTimeFormitted()+" | Repo:" + nextReposList);

                // Get header to generate next url
                Headers headers = jsonResponse.getHeaders();
                List list = headers.get("Link");
                String nextUrl = (String) list.get(list.size() - 1);

                Pattern pattern = Pattern.compile("<(.*?)>; rel=\"next\"");
                Matcher matcher = pattern.matcher(nextUrl);
                if (matcher.find()) {
                    nextReposList = matcher.group(1);
                }

                listCnt += 100;

                if (root.isArray()) {

                    JSONArray array = root.getArray();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject repoObj = array.getJSONObject(i);
                        String fullName = repoObj.getString("full_name").replace("/", "_");
                        StringBuilder lanUrl = new StringBuilder(repoObj.getString("languages_url"));
                        lanUrl.append("?client_id=").append(clientId).append("&client_secret=").append(clientSecret);

                        String archiveUrl = repoObj.getString("archive_url");
                        archiveUrl = archiveUrl.replace("{archive_format}{/ref}", "zipball" + "?client_id=" + clientId + "&client_secret=" + clientSecret);

                        // Reach limit?
                        try {
                            while (!Utils.checkAPIRateLimit(fixedThreadPool)) {
                                // Sleep 10 minutes
                                Thread.sleep(1000 * 60 * 10);
                            }
                        } catch (UnirestException | InterruptedException e) {
                            e.printStackTrace();
                            continue;
                        }

                        try {
                            jsonResponse = Unirest.get(lanUrl.toString()).asJson();
                        } catch (UnirestException e) {
                            e.printStackTrace();
                            continue;
                        }

                        Set<String> keys = jsonResponse.getBody().getObject().keySet();
                        StringBuilder dir = new StringBuilder("/");
                        int len = keys.size();

                        for (String item : keys) {
                            len--;
                            dir.append(item);
                            if (len > 0)
                                dir.append("&");
                        }

                        new File(dir.insert(0, rootFolder).toString()).mkdirs();
                        dir.append("/");

                        Runnable worker = new WorkerThread(printWriter, dir, fullName, archiveUrl, fixedThreadPool);
                        fixedThreadPool.execute(worker);
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null)
                out.flush();
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
//        Main main = new Main();
//        main.run(5000);
        String path = "G:/2.zip";
        File file = new File(path);

        boolean flag = file.renameTo(new File("G:/2tmp.zip"));
        System.out.println(flag);
    }
}

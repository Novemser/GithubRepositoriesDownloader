package util;

import com.mashape.unirest.http.Headers;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import org.apache.http.HttpHost;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Novemser on 2016/9/27.
 */
public class HttpHelper {
    private static String clientId = "c3cdb3e20ce16b2fe446";
    private static String clientSecret = "9f1ca7fb8181eebcc27c4047f531d810718ba9bd";
    private static String limit = "https://api.github.com/rate_limit?client_id=" + clientId + "&client_secret=" + clientSecret;
    private static String ipTesting = limit;
    private static String[] initPages = {
            "http://www.xicidaili.com/nt/",
            "http://www.xicidaili.com/nn/",
            "http://www.xicidaili.com/wn/",
            "http://www.xicidaili.com/nt/"
    };

    private static BlockingQueue<HttpHost> availQueue = new LinkedBlockingDeque<>();

    public static HttpResponse<JsonNode> getJsonResponse(String url) throws UnirestException {
        return Unirest.get(url).asJson();
    }

    public static HttpHost getNextAvailProxy() {
        // Get next available proxy
        // Block if none
        while (true) {
            try {
                HttpHost host = availQueue.take();
                System.err.println("Taking:" + host.getHostName() + ":" + host.getPort());
                // Fail
                if (!testProxy(host)) {
                    System.err.println("Dropping:" + host.getHostName() + ":" + host.getPort());
                    continue;
                }
                // Get next
                // Success
                // Add this proxy to the last position
                availQueue.add(host);
                System.err.println("Re-add:" + host.getHostName() + ":" + host.getPort());
                return host;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean testProxy(HttpHost host) {
        try {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host.getHostName(), host.getPort()));

            HttpURLConnection.setFollowRedirects(true);
            HttpURLConnection con = (HttpURLConnection) new URL(limit).openConnection(proxy);
            con.setRequestMethod("GET");

            con.setConnectTimeout(2000); //set timeout to 5 seconds
            con.setReadTimeout(2000);
            con.getContent();

            return true;
        } catch (java.net.SocketTimeoutException e) {
            return false;
        } catch (java.io.IOException e) {
            return false;
        } catch (NoSuchElementException e) {
            return false;
        }

    }

    /**
     * 检查是否达到了API使用最大限制
     *
     * @param executor 当前线程池
     * @return true:可以继续 false:不可以继续
     * @throws UnirestException 谁知道这是啥
     */
    public synchronized static boolean checkAPIRateLimit(ThreadPoolExecutor executor) {
        GetRequest request = Unirest.get(limit);

        try {
            HttpResponse<JsonNode> jsonResponse = request.asJson();
            JSONObject root = jsonResponse.getBody().getObject();

            JSONObject rate = root.getJSONObject("rate");
            int remaining = rate.getInt("remaining");
            boolean flag = (executor.getActiveCount() <= remaining) && remaining > 10;

            if (!flag) {
                System.out.println("API limit reached." + Utils.getTimeFormitted());
                return false;
            }

            return true;
        } catch (UnirestException e) {
            return false;
        }

    }

    private static void setNextProxy() {
        if (HttpHelper.getAvailProxyNum() > 0) {
            HttpHost host = HttpHelper.getNextAvailProxy();
            Unirest.setProxy(host);
            System.err.println("Using another proxy:" + host.getHostName() + ":" + host.getPort());
        }
    }

    public synchronized static void checkAndSet(ThreadPoolExecutor executor) {
        while (!checkAPIRateLimit(executor)) {
            setNextProxy();
        }
    }

    public synchronized static void forceChangeProxy(ThreadPoolExecutor executor) {
        setNextProxy();
        checkAndSet(executor);
    }


    public static int getAvailProxyNum() {
        return availQueue.size();
    }

    public static void startProducing() {
        new Thread(HttpHelper::produceProxy).start();
    }

    public static void main(String... args) {
        HttpHelper.startProducing();
    }

    private static void produceProxy() {
        System.out.println("Start producing proxy...");
        int proxyIndex = 0;
        try {
            for (int j = 1; j <= 2; j++) {
                // Circuit...
                if (j == 2) {
                    j = 1;
                    System.out.println("Finished 2 pages. Circuiting...");
                    proxyIndex++;
                }

                String urlPage = initPages[proxyIndex % initPages.length] + j;
                Document doc = Jsoup.connect(urlPage).header("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64; rv:43.0) Gecko/20100101 Firefox/43.0").get();


                Elements IPs = doc.select("tbody tr:gt(0) td:eq(1)");
                Elements ports = doc.select("tbody tr:gt(0) td:eq(2)");

                if (IPs.size() == ports.size()) {
                    for (int i = 0; i < IPs.size(); i++) {
                        String ip, port;
                        ip = IPs.get(i).html();
                        port = ports.get(i).html();

                        HttpHost tmpHost = new HttpHost(ip, Integer.parseInt(port));

                        // Test failed
                        if (!testProxy(tmpHost)) {
                            System.out.println("Proxy " + ip + ":" + port + " failed.");
                            continue;
                        }

                        availQueue.add(tmpHost);
                        System.out.println(Utils.ANSI_GREEN + "Proxy " + ip + ":" + port + " passed." + Utils.ANSI_RESET);
                    }

                }

            }
            System.out.println("Finished producing.");

            for (HttpHost item : availQueue) {
                System.out.println(item.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从Header中获取下一页的链接
     * 只适用于Github
     *
     * @param headers 返回的response header
     * @return 下一页链接
     */
    public static String getNextPageFromHeader(Headers headers) {
        String result = "";

        List list = headers.get("Link");
        String nextUrl = (String) list.get(list.size() - 1);

        Pattern pattern = Pattern.compile("<(.*?)>; rel=\"next\"");
        Matcher matcher = pattern.matcher(nextUrl);
        if (matcher.find()) {
            result = matcher.group(1);
        }

        return result;
    }
}

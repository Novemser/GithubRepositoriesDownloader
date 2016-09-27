import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by Novemser on 2016/9/27.
 */
public class Utils {
    private static String clientId = "c3cdb3e20ce16b2fe446";
    private static String clientSecret = "9f1ca7fb8181eebcc27c4047f531d810718ba9bd";
    private static String initRepo = "https://api.github.com/repositories?client_id=" + clientId + "&client_secret=" + clientSecret + "&since=99315";
    private static String limit = "https://api.github.com/rate_limit?client_id=" + clientId + "&client_secret=" + clientSecret;
    private static SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
    public static PrintWriter log;

    /**
     * 检查是否达到了API使用最大限制
     * @param executor 当前线程池
     * @return true:可以继续 false:不可以继续
     * @throws UnirestException 谁知道这是啥
     */
    public static boolean checkAPIRateLimit(ThreadPoolExecutor executor) throws UnirestException {
        GetRequest request = Unirest.get(limit);
        HttpResponse<JsonNode> jsonResponse = request.asJson();
        JSONObject root = jsonResponse.getBody().getObject();

        JSONObject rate = root.getJSONObject("rate");
        int remaining = rate.getInt("remaining");
        boolean flag = (executor.getActiveCount() <= remaining) && remaining > 10;

        if (!flag) {
            System.out.println("API limit reached." + getTimeFormitted());
            logln(log, "WARNING:API limit reached." + getTimeFormitted());
        }

        return flag;
    }

    public static String getTimeFormitted() {
        return format.format(new Date());
    }

    public static void logln(PrintWriter writer, String s) {
        writer.println(s);
        writer.flush();
    }
}

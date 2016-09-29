package util;

import com.mashape.unirest.http.Headers;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Novemser on 2016/9/27.
 */
public class HttpHelper {
    public static HttpResponse<JsonNode> getJsonResponse(String url) throws UnirestException {
        return Unirest.get(url).asJson();
    }

    /**
     * 从Header中获取下一页的链接
     * 只适用于Github
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

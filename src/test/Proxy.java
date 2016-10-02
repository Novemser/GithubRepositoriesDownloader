package test;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Novemser on 2016/9/30.
 */
public class Proxy {
    public static void main(String...args) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://www.xicidaili.com/nt/").openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64; rv:43.0) Gecko/20100101 Firefox/43.0");
        final InputStream inputStream = connection.getInputStream();
        final String html = IOUtils.toString(inputStream);
        inputStream.close();

        System.out.println(html);
    }
}

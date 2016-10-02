package test;

import com.mashape.unirest.http.Unirest;
import org.apache.http.HttpHost;
import util.HttpHelper;
import util.Utils;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URLConnection;

/**
 * Created by Novemser on 2016/9/30.
 */
public class FileDownload {
    public static void main(String...args) throws InterruptedException {
//        ProxySelector defaults = ProxySelector.getDefault();
//
//        HttpHost host = new HttpHost("218.102.23.83", 8080);
//        Unirest.setProxy(host);
//        boolean flag = HttpHelper.testProxy(host);
//        System.out.println(flag);


        String name = "D:/Test/karlbennett_debug.zip";
        String url = "https://api.github.com/repos/karlbennett/debug/zipball?client_id=c3cdb3e20ce16b2fe446&client_secret=9f1ca7fb8181eebcc27c4047f531d810718ba9bd";
        try {
            Utils.saveZipToFile(name, url);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

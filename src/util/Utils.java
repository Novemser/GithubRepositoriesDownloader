package util;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHost;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 *
 * Created by Novemser on 2016/9/27.
 */

public class Utils {
    private static String clientId = "c3cdb3e20ce16b2fe446";
    private static String clientSecret = "9f1ca7fb8181eebcc27c4047f531d810718ba9bd";
    private static String initRepo = "https://api.github.com/repositories?client_id=" + clientId + "&client_secret=" + clientSecret + "&since=99315";
    private static String limit = "https://api.github.com/rate_limit?client_id=" + clientId + "&client_secret=" + clientSecret;
    private static SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
    public static PrintWriter log;


    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public static String getTimeFormitted() {
        return format.format(new Date());
    }

    public static void logln(PrintWriter writer, String s) {
        writer.println(s);
        writer.flush();
    }

    public static void logMsgWithTime(PrintWriter writer, String msg) {
        logln(writer, Utils.getTimeFormitted() + " | " + msg);
    }

    public static void saveZipToFile(String fileName, String url) throws IOException {
        URL website = new URL(url);
        // First generate a temp file
        File saveFile = new File(fileName + ".tmp");
//        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
//        FileOutputStream fos = new FileOutputStream(saveFile);


        FileUtils.copyURLToFile(website, saveFile);
//        long offset = 0;
//        long count;
//        while ((count = fos.getChannel().transferFrom(rbc, offset, Long.MAX_VALUE)) > 0)
//        {
//            offset += count;
//        }
        // If succeed, change to the desired name
        if (!saveFile.renameTo(new File(fileName))) {
            saveFile.delete();
            throw new IOException("Rename file failed!");
        }
    }

    public static boolean isZipFileValid(final File file) {
        ZipFile zipfile = null;
        ZipInputStream zis = null;
        try {
            zipfile = new ZipFile(file);
            zis = new ZipInputStream(new FileInputStream(file));
            ZipEntry ze = zis.getNextEntry();
            if(ze == null) {
                return false;
            }
            while(ze != null) {
                // if it throws an exception fetching any of the following then we know the file is corrupted.
                zipfile.getInputStream(ze);
                ze.getCrc();
                ze.getCompressedSize();
                ze.getName();
                ze = zis.getNextEntry();
            }
            return true;
        } catch (ZipException e) {
            return false;
        } catch (IOException e) {
            return false;
        } finally {
            try {
                if (zipfile != null) {
                    zipfile.close();
                }
                if (zis != null) {
                    zis.close();
                }
            } catch (IOException e) {
                return false;
            }

        }
    }
}

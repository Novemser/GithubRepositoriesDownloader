package util.proxyprovider;

import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * GithubRepoGetter
 * <p>
 * Created by Novemser on 2016/10/2.
 */
public class MimiIp implements Provider {

    public MimiIp() {
        initPages.add("http://www.mimiip.com/gngao/");
        initPages.add("http://www.mimiip.com/gnpu/");
        initPages.add("http://www.mimiip.com/gntou/");
    }

    private List<String> initPages = new ArrayList<>();

    @Override
    public Elements getIPs() {
        return null;
    }

    @Override
    public Elements getPorts() {
        return null;
    }
}

package util.proxyprovider;

import org.jsoup.select.Elements;

/**
 * GithubRepoGetter
 * <p>
 * Created by Novemser on 2016/10/2.
 */
public class XiciDaili implements Provider {

    private String[] initPages = {
            "http://www.xicidaili.com/nt/",
            "http://www.xicidaili.com/nn/"
    };

    @Override
    public Elements getIPs() {

        return null;
    }

    @Override
    public Elements getPorts() {
        return null;
    }
}

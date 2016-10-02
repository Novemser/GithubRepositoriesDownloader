package util.proxyprovider;

import org.jsoup.select.Elements;

/**
 * GithubRepoGetter
 * <p>
 * Created by Novemser on 2016/10/2.
 */
interface Provider {
    int cnt = 0;

    Elements getIPs();
    Elements getPorts();
}

package net.sourceforge.docfetcher.util.errorreport;

import io.sentry.Sentry;
import io.sentry.SentryClient;
import io.sentry.SentryClientFactory;
import io.sentry.config.Lookup;
import io.sentry.event.BreadcrumbBuilder;
import net.sourceforge.docfetcher.enums.SystemConf;
import net.sourceforge.docfetcher.util.AppUtil;
import net.sourceforge.docfetcher.util.Util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by huzhengmian on 2018/5/10.
 */
public final class SentryHandler {

    private static SentryClient sentry;
    public static void init(){
        Sentry.init("https://6d2e21d50ec54f3e90d182f135acea84@sentry.io/1204219");
        sentry = SentryClientFactory.sentryClient();

        sentry.setRelease(AppUtil.Const.PROGRAM_VERSION.value);
        sentry.setDist(System.getProperty("os.name"));
        Map<String,String> tagsmap=new HashMap<String, String>();
        tagsmap.put("name",AppUtil.Const.PROGRAM_NAME.value);
        tagsmap.put("build",AppUtil.Const.PROGRAM_BUILD_DATE.value);
        tagsmap.put("portable",AppUtil.Const.IS_PORTABLE.value);
        tagsmap.put("develop", AppUtil.Const.IS_DEVELOPMENT_VERSION.value);
        sentry.setTags(tagsmap);
    }

    public static void capture(Throwable throwable){
        if(sentry != null){
            Sentry.capture(throwable);
        }
    }
}

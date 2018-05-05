package net.sourceforge.docfetcher.python;

import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.enums.SystemConf;
import py4j.GatewayServer;

/**
 * Created by huzhengmian on 2018/5/5.
 */
public class Py4jHandler {

    private static GatewayServer server;
    private static synchronized GatewayServer getServer(){
        if(server==null){
            server = new GatewayServer(new Py4jHandler(), ProgramConf.Int.Py4jPort.get());
        }
        return server;
    }
    public static void openGatewayServer(){
        getServer().start();
    }
    public static void shutdownGatewayServer(){
        getServer().shutdown();
    }
}

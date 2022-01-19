package com.hd.tvpro.webserver;

import com.hd.tvpro.app.App;
import com.yanzhenjie.andserver.AndServer;
import com.yanzhenjie.andserver.Server;

import java.util.concurrent.TimeUnit;

public class WebServer {
    public final static int port = 12345;

    public void start() {
        Server server = AndServer
                .webServer(App.INSTANCE.getApplicationContext())
                .port(port)
//                .inetAddress(new InetSocketAddress(50000).getAddress())
                .timeout(60, TimeUnit.SECONDS)
                .listener(new Server.ServerListener() {
                    @Override
                    public void onStarted() {

                    }

                    @Override
                    public void onStopped() {

                    }

                    @Override
                    public void onException(Exception e) {

                    }
                }).build();
        server.startup();
    }
}

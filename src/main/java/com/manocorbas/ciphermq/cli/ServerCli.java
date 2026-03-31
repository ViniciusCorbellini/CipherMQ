package com.manocorbas.ciphermq.cli;

import com.manocorbas.ciphermq.server.Server;
import com.manocorbas.ciphermq.util.log.Log;

public class ServerCli {
    public static void start(int port){
        Log.debug("SERVERCLI", "Server CLI started");

        Server s = new Server(port);
        s.start();
    }
}

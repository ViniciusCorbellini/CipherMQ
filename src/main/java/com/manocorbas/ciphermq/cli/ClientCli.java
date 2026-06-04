package com.manocorbas.ciphermq.cli;

import com.manocorbas.ciphermq.gui.Dashboard;
import com.manocorbas.ciphermq.util.log.Log;

public class ClientCli {

    public static void start(String host, int port) {
        Log.debug("CLIENTCLI", "Client CLI started");

        Dashboard dashboard = new Dashboard(host, port);
        dashboard.pack();
        dashboard.setVisible(true);
    }
    
}

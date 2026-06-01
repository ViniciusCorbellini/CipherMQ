package com.manocorbas.ciphermq.cli;

import java.util.Scanner;

import com.manocorbas.ciphermq.client.Client;
import com.manocorbas.ciphermq.client.ClientCredentials;
import com.manocorbas.ciphermq.client.ClientSetup;
import com.manocorbas.ciphermq.client.ConnectRequest;
import com.manocorbas.ciphermq.exceptions.HandShakeException;
import com.manocorbas.ciphermq.gui.Dashboard;
import com.manocorbas.ciphermq.util.log.Log;

public class ClientCli {

    private static Scanner input = new Scanner(System.in);

    private final static String COMPONENT = "CLIENTCLI";

    public static void start(String host, int port) {

        Log.debug(COMPONENT, "Client CLI started");

        Client client = null;
        try {
            client = initClient(host, port);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        try {
            Dashboard dashboard = new Dashboard(client);
            dashboard.pack();
            dashboard.setVisible(true);
        } catch (Exception e) {
            Log.error(COMPONENT, "Error in caused in dashboard", e);
        }
        
    }

    private static Client initClient(String host, int port) throws Exception {
        Log.debug(COMPONENT, "Initializing client");

        System.out.print("Username: ");
        String username = input.nextLine().strip();

        ClientCredentials creds = ClientSetup.load(username);
        Client client = new Client();

        ConnectRequest request = new ConnectRequest(host, port, creds);

        try {
            client.connect(request);
        } catch (HandShakeException e) {
            Log.error(COMPONENT, e.getMessage(), e);
            throw new Exception("Error while initializing client");
        }

        return client;
    }
}

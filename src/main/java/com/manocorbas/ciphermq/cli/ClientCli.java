package com.manocorbas.ciphermq.cli;

import java.util.Scanner;

import com.manocorbas.ciphermq.client.Client;
import com.manocorbas.ciphermq.client.ConnectRequest;
import com.manocorbas.ciphermq.exceptions.HandShakeException;
import com.manocorbas.ciphermq.util.log.Log;

// TODO: GUI (URGENT)
public class ClientCli {

    private static Scanner input = new Scanner(System.in);

    private final static String COMPONENT = "CLIENTCLI";

    public static void start(String host, int port) {

        Log.debug(COMPONENT, "Client CLI started");
        boolean running = true;

        Client client = null;
        try {
            client = initClient(host, port);
        } catch (Exception e) {
            e.printStackTrace();
            running = false;
        }

        while (running) {
            try {
                if (!input.hasNextLine()) {
                    Log.warn(COMPONENT, "STDIN closed, exiting...");
                    break;
                }

                String line = input.nextLine();

                Log.debug(COMPONENT, "input read: " + line);

                if (line.startsWith("sub ")) {
                    String topic = line.substring(4);
                    client.subscribe(topic);
                }

                else if (line.startsWith("unsub ")) {
                    String topic = line.substring(6);
                    client.unsubscribe(topic);
                }

                else if (line.startsWith("create ")) {
                    String topic = line.substring(7);
                    client.createTopic(topic);
                }

                else if (line.startsWith("pub ")) {
                    String[] parts = line.split(" ", 3);
                    client.publish(parts[1], parts[2]);
                }

                else if (line.equals("exit")) {
                    running = false;
                    client.close();
                }

                else {
                    printCommands();
                }

            } catch (Exception e) {
                Log.error(COMPONENT, "Exception caught", e);
            }
            System.out.println();
        }
        input.close();
    }

    private static void printCommands() {
        System.out.println("Commands:");
        System.out.println("sub <topic>");
        System.out.println("unsub <topic>");
        System.out.println("create <topic>");
        System.out.println("pub <topic> <msg>");
        System.out.println("exit");
    }

    private static Client initClient(String host, int port) throws Exception {
        Log.debug(COMPONENT, "Initializing client");

        System.out.print("Username: ");
        String username = input.nextLine();

        Client client = new Client();

        ConnectRequest request = new ConnectRequest(host, port, username);

        try {
            client.connect(request);
        }

        catch (HandShakeException e) {
            Log.error(host, username, e);
            throw new Exception("Erro while initializing client");
        }

        return client;
    }
}

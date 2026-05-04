package com.manocorbas.ciphermq.cli;

import java.util.Scanner;

import com.manocorbas.ciphermq.client.Client;
import com.manocorbas.ciphermq.util.log.Log;

// TODO: GUI (URGENT)
public class ClientCli {

    public static void start(String host, int port) {
        String COMPONENT = "CLIENTCLI";

        Log.debug(COMPONENT, "Client CLI started");

        Client client = new Client();
        client.connect(host, port);

        Scanner input = new Scanner(System.in);

        boolean running = true;

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
                    System.out.println("Commands:");
                    System.out.println("sub <topic>");
                    System.out.println("unsub <topic>");
                    System.out.println("create <topic>");
                    System.out.println("pub <topic> <msg>");
                    System.out.println("exit");
                }
            } catch (Exception e) {
                Log.error(COMPONENT, "Exception caught", e);
            }
            System.out.println();
        }
        input.close();
    }
}

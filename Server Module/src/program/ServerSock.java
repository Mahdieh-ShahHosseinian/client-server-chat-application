package Program;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The socket is the interface of sending and receiving data on
 * a specific port while the port is a numerical value assigned to
 * a specific process or an application in the device.
 * <p>
 * A socket is the communication path to a port.
 * <p>
 * If the destination device has three applications running, the port number
 * helps to identify the application that requires the received data.
 * <p>
 * Port helps to identify the process that requires the received data.
 * <p>
 * In a real-world scenario, a port is similar to the apartment number in
 * an apartment building while a socket is similar to the door of that apartment.
 * <p>
 * IP address and the port is also called the socket.
 * <p>
 * A Socket is the way  a server and a client keep track of requests.
 */
public class ServerSock extends Thread {

    private final int serverPort;
    private final List<ServerSockProcessor> sockProcessors;
    private final Set<String> groups;

    {
        sockProcessors = new ArrayList<>();

        // there are only 5 default groups in this server
        groups = new HashSet<>();
        groups.addAll(Set.of("group1", "group2", "group3", "group4", "group5"));
    }

    public ServerSock(int serverPort) {
        this.serverPort = serverPort;
    }

    @Override
    public void run() {

        try {

            ServerSocket serverSocket = new ServerSocket(serverPort);
            while (true) {

                System.out.println("Server is listening on port #" + serverPort);

                Socket clientSocket = serverSocket.accept();
                System.out.println("Server accepted connection from " + clientSocket);

                ServerSockProcessor processor = new ServerSockProcessor(this, clientSocket);
                sockProcessors.add(processor);
                processor.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Getters/Setters
     */
    public List<ServerSockProcessor> getSockProcessors() {
        return sockProcessors;
    }

    public Set<String> getGroups() {
        return groups;
    }
}

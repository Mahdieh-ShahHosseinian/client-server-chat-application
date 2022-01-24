package program;

/**
 * @Date January/15/2022
 * @Author Mahdieh ShahHosseinian
 * SID: 9820093 - Shahrood University of Technology
 * <p>
 * Client: one or multiple clients which request for connection by providing host (ip address) and port number
 * One main thread to run or display the output (window) in the desktop
 * One extra thread to continuously receive the incoming message from the server
 */
public class ClientSideProgram {

    private final String serverIPAddress;
    private final int serverPortNumber;
    private ClientSock clientSocket;

    public ClientSideProgram(String serverIPAddress, int serverPortNumber) {
        this.serverIPAddress = serverIPAddress;
        this.serverPortNumber = serverPortNumber;
        startClientSocket();
    }

    private void startClientSocket() {
        clientSocket = new ClientSock(serverIPAddress, serverPortNumber);
    }

    public ClientSock getClientSocket() {
        return clientSocket;
    }
}

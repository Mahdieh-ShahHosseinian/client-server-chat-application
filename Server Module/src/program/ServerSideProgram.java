package Program;

/**
 * @Date January/15/2022
 * @Author Mahdieh ShahHosseinian
 * SID: 9820093 - Shahrood University of Technology
 * <p>
 * Server: a single server responsible for listening and accepting to multiple connection request at a particular port (number)
 * One main thread (GUI thread) to run or display the output (window) in the desktop
 * One extra thread to continuously listen and accept to multiple connection request and
 * A thread for each and every successful connection (each connection means client connection to the server) to get message from client and send them back
 */
public class ServerSideProgram {

    private final int serverPort;

    public ServerSideProgram(int serverPort) {
        this.serverPort = serverPort;
        startServer();
    }

    public void startServer() {
        ServerSock serverSock = new ServerSock(serverPort);
        serverSock.start();
    }

    public static void main(String[] args) {

        ServerSideProgram MainServer = new ServerSideProgram(50000);
    }
}

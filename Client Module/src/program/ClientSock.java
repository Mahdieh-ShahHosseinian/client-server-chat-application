package program;

import view.ChatController;
import view.Menu;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Client socket and all Client request/Server response handle here
 *
 * This Client Side works along with JavaFX Side to interact with server
 */
public class ClientSock extends Thread {

    private final String IPAddress;
    private final int port;

    private Socket socket;
    //    private DataInputStream in;
    //    private DataOutputStream out;
    private InputStream in;
    private OutputStream out;
    private BufferedReader reader;

    private Menu menu;
    private ChatController chatController;

    private String login = null;

    private String[] allUserSet;

    public ClientSock(String IPAddress, int port) {
        this.IPAddress = IPAddress;
        this.port = port;
    }

    @Override
    public void run() {

        try {
            connect();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * To close the connection
     *
     * @throws IOException
     */
    public void closeConnection() throws IOException {

        in.close();
        out.close();
        socket.close();
    }

    /**
     * To make the connection
     *
     * @throws IOException
     */
    private void connect() throws IOException {

        socket = new Socket(IPAddress, port);
//      in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
//      out = new DataOutputStream(new DataOutputStream(socket.getOutputStream()));
        in = socket.getInputStream();
        out = socket.getOutputStream();
        reader = new BufferedReader(new InputStreamReader(in));
        readMessageLoop();
    }

    /**
     * @param msg
     * @throws IOException
     */
    public void sendMsg(String msg) throws IOException {
        out.write(msg.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @param username
     * @param password
     * @throws IOException
     */
    public void register(String username, String password) throws IOException {

        String msg = "Make -Option user:" + username + " -Option pass:" + password + "\r\n";
        sendMsg(msg);
    }

    /**
     * @param username
     * @param password
     * @throws IOException
     */
    public void login(String username, String password) throws IOException {

        String msg = "Connect -Option user:" + username + " -Option pass:" + password + "\r\n";
        sendMsg(msg);
    }

    /**
     * @param groupName
     * @throws IOException
     */
    public void joinGroup(String groupName) throws IOException {

        String msg = "Group -Option gname:" + groupName + "\r\n";
        sendMsg(msg);
    }

    /**
     * @param groupName
     * @throws IOException
     */
    public void leaveGroup(String groupName) throws IOException {

        String msg = "End -Option gname:" + groupName + "\r\n";
        sendMsg(msg);
    }

    /**
     * @throws IOException
     */
    public void requestAllUserPresence() throws IOException {

        String msg = "Users\r\n";
        sendMsg(msg);
    }

    /**
     * @param username
     * @throws IOException
     */
    public void requestAUserPresence(String username) throws IOException {

        String msg = "Users -Option user:" + username + "\r\n";
        sendMsg(msg);
    }

    /**
     * @param groupName
     * @throws IOException
     */
    public void requestGroupUserPresence(String groupName) throws IOException {

        String msg = "Users -Option gname:" + groupName + "\r\n";
        sendMsg(msg);
    }

    /**
     * @param sendTo
     * @param message
     * @throws IOException
     */
    public void sendGroupMessage(String sendTo, String message) throws IOException {

        String[] split = message.split("\\n");
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : split) {
            stringBuilder.append(s).append("\r\n");
        }

        int length = message.length();
        String msg = "GM -Option to:" + sendTo + " -Option message_len:" + length + " -Option message_body:" + stringBuilder;
        sendMsg(msg);
    }

    /**
     * @param sendTo
     * @param message
     * @throws IOException
     */
    public void sendPrivateMessage(String sendTo, String message) throws IOException {

        String[] split = message.split("\\n");
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : split) {
            stringBuilder.append(s).append("\r\n");
        }

        int length = message.length();
        String msg = "PM -Option to:" + sendTo + " -Option message_len:" + length + " -Option message_body:" + stringBuilder;
        sendMsg(msg);
    }

    /**
     * A Specific Thread always listening to the server
     */
    private void readMessageLoop() {

        new Thread(() -> {

            try {
                String line;
                while ((line = reader.readLine()) != null) {

                    line = line.toLowerCase(Locale.ROOT);
                    String[] tokens = line.split(" -option ");

                    if (tokens.length > 0) {

                        String cmd = tokens[0];
                        switch (cmd) {

                            case "make" -> registerResponse(tokens);
                            case "connect" -> loginResponse(tokens);
                            case "group", "end" -> deliverGroupNotification(tokens);
                            case "gm" -> deliverGroupMessage(tokens);
                            case "users" -> userPresenceResponse(tokens);
                            case "pm" -> deliverPrivateMessage(tokens);
                        }
                    }

                    Thread.sleep(1000);
                }
                closeConnection();
            } catch (IOException | InterruptedException i) {
                try {
                    closeConnection();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
                i.printStackTrace();
            }
        }).start();
    }

    /**
     * @param token
     * @param key
     * @return
     */
    private String getTokenValue(String token, String key) {

        return token.replace(key + ":", "");
    }

    /**
     * @param tokens taken from server
     * @throws IOException
     */
    public void registerResponse(String[] tokens) throws IOException {

        String result = getTokenValue(tokens[2], "res");

        if (result.equals("accepted")) {
            login = getTokenValue(tokens[1], "user");
            menu.getRegisterDialogPane().handleRegisterResponse(result, "");
        } else {
            String reason = getTokenValue(tokens[3], "reason");
            menu.getRegisterDialogPane().handleRegisterResponse(result, reason);
        }
    }

    /**
     * @param tokens taken from server
     * @throws IOException
     */
    public void loginResponse(String[] tokens) throws IOException {

        String result = getTokenValue(tokens[2], "res");

        if (result.equals("connected")) {
            login = getTokenValue(tokens[1], "user");
            menu.handleLoginResponse(result, "");
        } else {
            String reason = getTokenValue(tokens[3], "reason");
            menu.handleLoginResponse(result, reason);
        }
    }

    /**
     * @param tokens taken from server
     * @throws IOException
     */
    private void deliverGroupNotification(String[] tokens) throws IOException {

        String group = getTokenValue(tokens[1], "from");
        String notification = getTokenValue(tokens[2], "message");

        chatController.getGroupTabControllers().get(group).receiveGroupNotification(notification);
    }

    /**
     * @param tokens taken from server
     */
    private void userPresenceResponse(String[] tokens) {

        if (tokens[1].startsWith("user:")) {

            String username = getTokenValue(tokens[1], "user");
            String res = getTokenValue(tokens[2], "res");
            if (tokens[3].startsWith("status:")) {

                String status = getTokenValue(tokens[3], "status");
                chatController.searchIDResponse(username, res, status);
            } else if (tokens[3].startsWith("reason:")) {

                String reason = getTokenValue(tokens[3], "reason");
                chatController.searchIDResponse(username, res, reason);
            }
        } else if (tokens[1].startsWith("gname:")) {

            String group = getTokenValue(tokens[1], "gname");
            String userList = getTokenValue(tokens[2], "user_list");
            String[] users = userList.split("\\|");
            chatController.getGroupTabControllers().get(group).updateOnlineUserList(users);
        }
    }

    /**
     * @param tokens taken from server
     * @throws IOException
     */
    private void deliverGroupMessage(String[] tokens) throws IOException {

        String from = getTokenValue(tokens[1], "from");
        String group = getTokenValue(tokens[2], "to");
        int length = Integer.parseInt(getTokenValue(tokens[3], "message_len"));
        String body = getTokenValue(tokens[4], "message_body");

        body = continueReadingMessage(body.trim(), length);

        chatController.getGroupTabControllers().get(group).receiveGroupMessages(from, body);
    }

    /**
     * @param tokens taken from server
     * @throws IOException
     */
    private void deliverPrivateMessage(String[] tokens) throws IOException {

        String from = getTokenValue(tokens[1], "from");
        String username = getTokenValue(tokens[2], "to");
        int length = Integer.parseInt(getTokenValue(tokens[3], "message_len"));
        String body = getTokenValue(tokens[4], "message_body");

        body = continueReadingMessage(body.trim(), length);

        if (chatController.setupNewChat(from)) {
            chatController.getDirectTabControllers().get(from).receivePrivateMessage(from, body);
        }
    }

    /**
     * ensure the whole message is captured, using message length
     *
     * @param body   message body
     * @param length message length
     * @return the whole message body
     * @throws IOException if any I/O err happened
     */
    private String continueReadingMessage(String body, int length) throws IOException {

        int l = body.length();
        StringBuilder bodyBuilder = new StringBuilder(body);
        while (l < length) {

            String line = reader.readLine();
            bodyBuilder.append("\r\n").append(line);
            l += line.length() + 2;
        }
        body = bodyBuilder.toString();
        return body;
    }

    /**
     * Getters/Setters
     */
    public void setMenu(Menu menu) {
        this.menu = menu;
    }

    public void setChatController(ChatController chatController) {
        this.chatController = chatController;
    }

    public String getLogin() {
        return login;
    }

    public Socket getSocket() {
        return socket;
    }
}

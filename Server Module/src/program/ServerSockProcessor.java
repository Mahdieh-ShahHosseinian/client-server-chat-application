package Program;

import Database.Database;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;

/**
 * For every new client socket, this class will be initiated..
 * As soon as the client socket accepted by server socket.
 * And the instance of this class will do the remain works on the specific client socket.
 */
public class ServerSockProcessor extends Thread {

    private final ServerSock serverSock;
    private final Socket clientSock;

    private OutputStream outputStream;
    private InputStream inputStream;
    private BufferedReader reader;

    private final Database database = new Database();

    private String login = null;

    private final HashSet<String> groupSet = new HashSet<>();
    private final HashSet<String> directSet = new HashSet<>();

    public ServerSockProcessor(ServerSock serverSock, Socket clientSocket) {
        this.serverSock = serverSock;
        this.clientSock = clientSocket;
    }

    @Override
    public void run() {
        try {
            processClientSocket();
        } catch (IOException | SQLException e) {
            try {
                closeClientSocket();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    /**
     * Process the client socket.
     *
     * @throws IOException  if and IO error accrues.
     * @throws SQLException if a SQL error accrues.
     */
    private void processClientSocket() throws IOException, SQLException {

        outputStream = clientSock.getOutputStream();
        inputStream = clientSock.getInputStream();

        // reading the client request
        reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        here:
        while ((line = reader.readLine()) != null) {
            System.out.println("+IN: " + clientSock.getPort() + "<-port login=" + login + ": " + line);

            line = line.toLowerCase(Locale.ROOT);
            String[] tokens = line.split(" -option ");

            if (tokens.length > 0) {

                String cmd = tokens[0];
                if (login == null) {
                    switch (cmd) {
                        case "make" -> processRegister(tokens);
                        case "connect" -> processLogin(tokens);
                        case "end" -> {
                            break here;
                        }
                        default -> {
                            String msg = "Unknown/Unexpected command: " + cmd + "\r\n";
                            sendMsg(msg);
                        }
                    }
                } else {
                    switch (cmd) {
                        case "group" -> processJoiningGroup(tokens);
                        case "users" -> requestUserPresence(tokens);
                        case "gm" -> sendGroupMessage(tokens);
                        case "pm" -> sendPrivateMessage(tokens);
                        case "end" -> {
                            leave(tokens);
                            if (tokens.length == 1) break here;
                        }
                        default -> {
                            String msg = "Unknown command: " + cmd + "\r\n";
                            sendMsg(msg);
                        }
                    }
                }
            }
        }
        closeClientSocket();
    }

    // TODO handle connection lost to notify others the user's status -> Done!
    private void closeClientSocket() throws IOException {

        serverSock.getSockProcessors().remove(this);
        leaveAllGroups();
        leaveAllDirects();
        inputStream.close();
        outputStream.close();
        clientSock.close();
    }

    /**
     * @param msg the message body
     * @throws IOException
     */
    private void sendMsg(String msg) throws IOException {
        System.out.println("-OUT: " + clientSock.getPort() + "<-port login=" + login + ": " + msg);
        outputStream.write(msg.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @param tokens taken from buffer reader
     * @throws SQLException
     * @throws IOException
     */
    private void processRegister(String[] tokens) throws SQLException, IOException {

        if (tokens.length == 3 && tokens[1].startsWith("user:") && tokens[2].startsWith("pass:")) {

            String username = getTokenValue(tokens[1], "user");
            String password = getTokenValue(tokens[2], "pass");
            if (database.doesExist(username)) {
                String msg = "Make -Option user:" + username + " -Option res:not-accepted -Option reason:This username is already occupied.\r\n";
                sendMsg(msg);
            } else {
                if (registerValidation(username, password)) {
                    database.addNewUser(username, password);
                    String msg = "Make -Option user:" + username + " -Option res:accepted\r\n";
                    sendMsg(msg);
                }
            }
        } else {
            String msg = "Wrong command. please follow the protocol." + "\r\n";
            sendMsg(msg);
        }
    }

    /**
     * @param username
     * @param password
     * @return true if the registration was successful
     * @throws IOException
     */
    private boolean registerValidation(String username, String password) throws IOException {

        if (!username.matches("[a-zA-Z0-9_]+") || !password.matches("[a-zA-Z0-9_]+")) {
            String msg = "Make -Option user:" + username + " -Option res:not-accepted -Option reason:To choose username/password, only a-z 0-9 and underscores allowed.\r\n";
            sendMsg(msg);
            return false;
        } else if (username.length() < 6) {
            String msg = "Make -Option user:" + username + " -Option res:not-accepted -Option reason:This username is too short. Minimum length is 6 characters.\r\n";
            sendMsg(msg);
            return false;
        } else if (username.length() > 15) {
            String msg = "Make -Option user:" + username + " -Option res:not-accepted -Option reason:This username is too long. Maximum length is 15 characters.\r\n";
            sendMsg(msg);
            return false;
        } else if (password.length() < 6) {
            String msg = "Make -Option user:" + username + " -Option res:not-accepted -Option reason:The password is too short. Minimum length is 6 characters.\r\n";
            sendMsg(msg);
            return false;
        } else if (password.length() > 15) {
            String msg = "Make -Option user:" + username + " -Option res:not-accepted -Option reason:The password is too long. Maximum length is 15 characters.\r\n";
            sendMsg(msg);
            return false;
        }
        return true;
    }

    /**
     * @param tokens taken from buffer reader
     * @throws SQLException
     * @throws IOException
     */
    private void processLogin(String[] tokens) throws SQLException, IOException {

        if (tokens.length == 3 && tokens[1].startsWith("user:") && tokens[2].startsWith("pass:")) {

            String username = getTokenValue(tokens[1], "user");
            String password = getTokenValue(tokens[2], "pass");

            if (!database.doesExist(username)) {
                String msg = "Connect -Option user:" + username + " -Option res:not-connected -Option reason:This username doesn't exist.\r\n";
                sendMsg(msg);
            } else if (password.equals(database.getUserPass(username))) {

                boolean alreadyLoggedIn = false;
                List<ServerSockProcessor> processors = serverSock.getSockProcessors();
                for (ServerSockProcessor processor : processors) {
                    if (processor.getLogin() != null && processor.getLogin().equals(username)) {
                        alreadyLoggedIn = true;
                        break;
                    }
                }
                if (!alreadyLoggedIn) {
                    login = username;
                    String msg = "Connect -Option user:" + username + " -Option res:connected\r\n";
                    sendMsg(msg);
                } else {
                    String msg = "Connect -Option user:" + username + " -Option res:not-connected -Option reason:User already logged in.\r\n";
                    sendMsg(msg);
                }
            } else {
                String msg = "Connect -Option user:" + username + " -Option res:not-connected -Option reason:The password is incorrect." + "\r\n";
                sendMsg(msg);
            }
        } else {
            String msg = "Wrong command. please follow the protocol." + "\r\n";
            sendMsg(msg);
        }
    }

    /**
     * @param tokens taken from buffer reader
     * @throws IOException
     */
    private void processJoiningGroup(String[] tokens) throws IOException {

        if (tokens.length == 2 && tokens[1].startsWith("gname:")) {

            String groupName = getTokenValue(tokens[1], "gname");

            if (serverSock.getGroups().contains(groupName)) {
                if (!checkMembership(groupName)) {
                    groupSet.add(groupName);

                    // Send other present users on chatroom the new user's status
                    // And a welcome message to new user
                    List<ServerSockProcessor> processors = serverSock.getSockProcessors();
                    for (ServerSockProcessor processor : processors) {

                        if (processor.getLogin() != null && processor.getLogin().equals(login)) {
                            String msg = "Group -Option from:" + groupName + " -Option message:Hi " + login + ", welcome to the chat room.\r\n";
                            processor.sendMsg(msg);
                        } else if (processor.checkMembership(groupName)) {
                            String msg = "Group -Option from:" + groupName + " -Option message:" + login + " joined the chat room.\r\n";
                            processor.sendMsg(msg);
                        }

                    }

                } else { // TODO protocol format option
                    String msg = "Group -Option from:" + groupName + " -Option err:You already joined this group.\r\n";
                    sendMsg(msg);
                }
            } else { // TODO protocol format option
                String msg = "Group -Option from:" + groupName + " -Option err:This group doesn't exist.\r\n";
                sendMsg(msg);
            }

        } else {
            String msg = "Wrong command. please follow the protocol." + "\r\n";
            sendMsg(msg);
        }
    }

    /**
     * @param tokens taken from buffer reader
     * @throws IOException
     * @throws SQLException
     */
    public void requestUserPresence(String[] tokens) throws IOException, SQLException {

        if (tokens.length == 1) {
            requestAllUserPresence();
        } else {
            if (tokens[1].startsWith("user:")) {
                requestAUserPresence(tokens);
            } else if (tokens[1].startsWith("gname:")) {
                requestGroupUserPresence(tokens);
            }
        }
    }

    /**
     * NOT USE ON THIS CLIENT APP VERSION
     *
     * @throws IOException
     */
    private void requestAllUserPresence() throws IOException {

        List<ServerSockProcessor> processors = serverSock.getSockProcessors();
        StringBuilder usersList = new StringBuilder(login);
        for (ServerSockProcessor processor : processors) {

            if (processor.getLogin() != null && !processor.getLogin().equals(login)) {
                usersList.append("|").append(processor.getLogin());
            }
        }
        sendMsg("Users -Option USER_LIST:" + usersList + "\r\n");
    }

    /**
     * @param tokens taken from buffer reader
     * @throws SQLException
     * @throws IOException
     */
    private void requestAUserPresence(String[] tokens) throws SQLException, IOException {

        String username = getTokenValue(tokens[1], "user");

        if (!database.doesExist(username)) {

            String msg = "Users -Option user:" + username + " -Option res:failed -Option reason:Username not found.\r\n";
            sendMsg(msg);
        } else {

            boolean online = false;
            List<ServerSockProcessor> processors = serverSock.getSockProcessors();
            for (ServerSockProcessor processor : processors) {

                if (processor.getLogin() != null && processor.getLogin().equals(username)) {
                    online = true;
                    break;
                }
            }
            String msg = "Users -Option user:" + username + " -Option res:succeed -Option status:" + (online ? "online" : "offline");
            sendMsg(msg + "\r\n");
        }
    }

    /**
     * @param tokens taken from buffer reader
     * @throws IOException
     */
    private void requestGroupUserPresence(String[] tokens) throws IOException {

        if (tokens.length == 2 && tokens[1].startsWith("gname:")) {

            String groupName = getTokenValue(tokens[1], "gname");
            if (serverSock.getGroups().contains(groupName)) {
                if (checkMembership(groupName)) {

                    // Send current user all the other online logins in this particular group
                    List<ServerSockProcessor> processors = serverSock.getSockProcessors();
                    StringBuilder usersList = new StringBuilder(login);
                    for (ServerSockProcessor processor : processors) {

                        if (processor.getLogin() != null && !processor.getLogin().equals(login)
                                && processor.checkMembership(groupName)) {
                            usersList.append("|").append(processor.getLogin());
                        }
                    }
                    sendMsg("Users -Option gname:" + groupName + " -Option USER_LIST:" + usersList + "\r\n");

                } else {

                    String msg = "Users -Option gname:" + groupName + " -Option err:You don't have permission because you are not part of this group.\r\n";
                    sendMsg(msg);
                }
            } else {
                String msg = "Users -Option gname:" + groupName + " -Option err:This group doesn't exist.\r\n";
                sendMsg(msg);
            }
        } else {
            String msg = "Wrong command. please follow the protocol." + "\r\n";
            sendMsg(msg);
        }
    }

    /**
     * @param tokens taken from buffer reader
     * @throws IOException
     */
    public void leave(String[] tokens) throws IOException {

        if (tokens.length == 1) { // already handled but let it be double check LOLs
            closeClientSocket();
        } else if (tokens.length == 2 && tokens[1].startsWith("gname:")) { // leave the group
            String groupName = getTokenValue(tokens[1], "gname");
            leaveGroup(groupName);
        } else {
            String msg = "Wrong command. please follow the protocol." + "\r\n";
            sendMsg(msg);
        }
    }


    /**
     * @throws IOException
     */
    private void leaveAllDirects() throws IOException {

        ArrayList<String> directs = new ArrayList<>(directSet);
        for (String direct : directs) {
            leaveDirect(direct);
        }
    }

    /**
     * @param directName the users that this client is having a private chat with
     * @throws IOException
     */
    private void leaveDirect(String directName) throws IOException {

        directSet.remove(directName);

        List<ServerSockProcessor> processors = serverSock.getSockProcessors();
        for (ServerSockProcessor processor : processors) {

            if (processor.getLogin() != null && !processor.getLogin().equals(login) && processor.getLogin().equals(directName)) {
                String msg = "PM -Option from:" + login + " -Option to:" + directName +
                        " -Option message_len:" + 19 + " -Option message_body:" + "$CLOSE_DIRECT_CHAT$" + "\r\n";
                processor.sendMsg(msg);
                break;
            }
        }
    }

    /**
     * @throws IOException
     */
    private void leaveAllGroups() throws IOException {

        ArrayList<String> groups = new ArrayList<>(groupSet);
        for (String groupName : groups) {
            leaveGroup(groupName);
        }
    }

    /**
     * @param groupName the groups that this client is a member of it
     * @throws IOException
     */
    private void leaveGroup(String groupName) throws IOException {

        if (serverSock.getGroups().contains(groupName)) {
            if (checkMembership(groupName)) {
                groupSet.remove(groupName);

                // Send other present users on chatroom the new user's status
                // And a welcome message to new user
                List<ServerSockProcessor> processors = serverSock.getSockProcessors();
                for (ServerSockProcessor processor : processors) {

                    if (processor.getLogin() != null && processor.getLogin().equals(login)) {
                        String msg = "END -Option from:" + groupName + " -Option message:You left the chat room.\r\n";
                        processor.sendMsg(msg);
                    } else if (processor.checkMembership(groupName)) {
                        String msg = "END -Option from:" + groupName + " -Option message:" + login + " left the chat room.\r\n";
                        processor.sendMsg(msg);
                    }
                }
            } else {
                String msg = "END -Option from:" + groupName + " -Option err:You are not in this group.\r\n";
                sendMsg(msg);
            }
        } else {
            String msg = "END -Option from:" + groupName + " -Option err:This group doesn't exist.\r\n";
            sendMsg(msg);
        }
    }

    /**
     * @param token
     * @param key
     * @return
     */
    private String getTokenValue(String token, String key) {

        return token.replace(key + ":", "");
    }

    public boolean checkMembership(String gname) {
        return groupSet.contains(gname);
    }

    /**
     * TODO encrypted message
     *
     * @param tokens
     * @throws IOException
     */
    public void sendPrivateMessage(String[] tokens) throws IOException {

        if (tokens.length == 4 && tokens[1].startsWith("to:") &&
                tokens[2].startsWith("message_len:") && tokens[3].startsWith("message_body:")) {

            String sendTo = getTokenValue(tokens[1], "to");
            int length = Integer.parseInt(getTokenValue(tokens[2], "message_len"));
            String body = getTokenValue(tokens[3], "message_body");

            body = continueReadingMessage(body.trim(), length);

            List<ServerSockProcessor> processors = serverSock.getSockProcessors();
            for (ServerSockProcessor processor : processors) {

                if (processor.getLogin() != null && processor.getLogin().equals(sendTo)) {
                    String msg = "PM -Option from:" + login + " -Option to:" + sendTo +
                            " -Option message_len:" + length + " -Option message_body:" + body + "\r\n";
                    if (body.equals("$start_direct_chat$")) {
                        directSet.add(sendTo);
                    } else if (body.equals("$close_direct_chat$")) {
                        directSet.remove(sendTo);
                    }
                    processor.sendMsg(msg);
                }
            }

        } else {
            String msg = "Wrong command. please follow the protocol." + "\r\n";
            sendMsg(msg);
        }
    }

    /**
     * TODO encrypted message
     *
     * @param tokens
     * @throws IOException
     */
    public void sendGroupMessage(String[] tokens) throws IOException {

        if (tokens.length == 4 && tokens[1].startsWith("to:") &&
                tokens[2].startsWith("message_len:") && tokens[3].startsWith("message_body:")) {

            String sendTo = getTokenValue(tokens[1], "to");
            int length = Integer.parseInt(getTokenValue(tokens[2], "message_len"));
            String body = getTokenValue(tokens[3], "message_body");

            body = continueReadingMessage(body.trim(), length);

            if (serverSock.getGroups().contains(sendTo)) {
                if (checkMembership(sendTo)) {
                    List<ServerSockProcessor> processors = serverSock.getSockProcessors();
                    for (ServerSockProcessor processor : processors) {

                        if (processor.getLogin() != null && !processor.getLogin().equals(login)
                                && processor.checkMembership(sendTo)) {
                            String msg = "GM -Option from:" + login + " -Option to:" + sendTo +
                                    " -Option message_len:" + length + " -Option message_body:" + body + "\r\n";
                            processor.sendMsg(msg);
                        }
                    }
                } else {
                    String msg = "GM -Option from:" + login + " -Option to:" + sendTo + " -Option err:You are not in this group.\r\n";
                    sendMsg(msg);
                }
            } else {
                String msg = "GM -Option from:" + login + " -Option to:" + sendTo + " -Option err:This group doesn't exist.\r\n";
                sendMsg(msg);
            }
        } else {
            String msg = "Wrong command. please follow the protocol.\r\n";
            sendMsg(msg);
        }
    }

    /**
     * ensure the whole message was captured, using message length
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
            bodyBuilder.append("\r\n").append(line.toLowerCase(Locale.ROOT));
            l += line.length() + 2;
            System.out.println("+IN: " + clientSock.getPort() + "<-port login=" + login + ": " + line);
        }
        body = bodyBuilder.toString();
        return body;
    }

    /**
     * Getters/Setters
     */
    public String getLogin() {
        return login;
    }
}


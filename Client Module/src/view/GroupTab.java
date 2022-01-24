package view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.util.Locale;

/**
 * in charge of group tab
 */
public class GroupTab {

    private String groupName;
    @FXML
    private Button joinLeaveButton;
    @FXML
    private Button showOnlineUsersButton;
    @FXML
    private Button sendButton;
    @FXML
    private TextArea text;
    @FXML
    private HBox sendBox;
    @FXML
    private TextArea textArea;
    @FXML
    public ListView<String> onlineUserList;

    @FXML
    public void sendMessage() {

        if (!text.getText().isEmpty() && !text.getText().isBlank()) {

            String msg = text.getText();

            new Thread(() -> {
                try {
                    Main.clientSocket.sendGroupMessage(groupName, msg);
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }).start();

            textArea.appendText("You: " + msg.toLowerCase(Locale.ROOT) + "\r\n");
        }
        text.setText("");
    }

    @FXML
    public void joinLeave() {

        if (joinLeaveButton.getText().equals("Join group")) {

            new Thread(() -> {
                try {
                    Main.clientSocket.joinGroup(groupName);
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }).start();
            sendBox.setDisable(false);

            showOnlineUsersButton.setDisable(false);
            joinLeaveButton.setText("Leave group");
        } else if (joinLeaveButton.getText().equals("Leave group")) {

            new Thread(() -> {
                try {
                    Main.clientSocket.leaveGroup(groupName);
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }).start();
            joinLeaveButton.setText("Join group");
            sendBox.setDisable(true);
            textArea.setText("");

            onlineUserList.getItems().clear();
            showOnlineUsersButton.setDisable(true);
            showOnlineUsersButton.setText("Show online users");
        }
    }

    @FXML
    public void showOnlineUsers() {

        new Thread(() -> {
            try {
                Main.clientSocket.requestGroupUserPresence(groupName);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }).start();
        showOnlineUsersButton.setText("Refresh online users");
    }

    /**
     * @param from
     * @param msg
     */
    public void receiveGroupMessages(String from, String msg) {

        System.out.println("received group message:: " + msg);
        textArea.appendText(from + ": " + msg + "\r\n");
    }

    /**
     * TODO colorize notification
     *
     * @param notification
     */
    public void receiveGroupNotification(String notification) {

        textArea.appendText(notification + "\r\n");
    }

    /**
     * @param users
     */
    public void updateOnlineUserList(String[] users) {

        Platform.runLater(() -> {
            onlineUserList.getItems().clear();
            onlineUserList.getItems().addAll(users);
        });
    }

    /**
     * Setter/Getter
     */
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}

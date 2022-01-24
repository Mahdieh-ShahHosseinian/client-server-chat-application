package view;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;

/**
 * in charge of direct tab
 */
public class DirectTab {

    private String targetUsername;
    @FXML
    private Button leaveButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button sendButton;
    @FXML
    private TextArea text;
    @FXML
    private HBox sendBox;
    @FXML
    private TextArea textArea;

    /**
     * "$START_DIRECT_CHAT$" is a control message to start the chat, no need to print it out
     * "$CLOSE_DIRECT_CHAT$" is a control message to close/end the chat
     *
     * @param from
     * @param msg
     */
    public void receivePrivateMessage(String from, String msg) {

        System.out.println("received private message:: " + msg);

        if (!Main.clientSocket.getLogin().equals(from)) {

            if (!msg.equals("$start_direct_chat$")) {

                if (!msg.equals("$close_direct_chat$")) {

                    textArea.appendText(from + ": " + msg + "\r\n");
                } else {
                    textArea.appendText(from + ": " + "left the chat. " + new Date() + "\r\n");
                    leaveButton.setDisable(true);
                    deleteButton.setDisable(false);
                    sendBox.setDisable(true);
                }
            } else {
                if (!deleteButton.isDisable()) { // means the target user back to the chat

                    textArea.setText(""); // clear chat history
                    textArea.appendText(from + ": " + "joined the chat. " + new Date() + "\r\n");
                    leaveButton.setDisable(false);
                    deleteButton.setDisable(true);
                    sendBox.setDisable(false);
                }
            }
        }
    }

    @FXML
    private void sendMessage() {

        if (!text.getText().isEmpty() && !text.getText().isBlank()) {

            String msg = text.getText();

            new Thread(() -> {
                try {
                    Main.clientSocket.sendPrivateMessage(targetUsername, msg);
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }).start();

            textArea.appendText("You: " + msg.toLowerCase(Locale.ROOT) + "\r\n");
        }
        text.setText("");
    }

    /**
     * Setter/Getter
     */
    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }

    public Button getLeaveButton() {
        return leaveButton;
    }

    public Button getDeleteButton() {
        return deleteButton;
    }
}

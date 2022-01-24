package view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * The Main Chat Window managing the Group Tabs and Direct Tabs
 * <p>
 * NO HARD CODING PLZ :-)
 */
public class ChatController implements Initializable {

    /**
     * Groups field
     */
    private final Map<String, GroupTab> groupTabControllers = new HashMap<>();
    @FXML
    private TabPane groupTabPane;

    /**
     * Direct field
     */
    private final Map<String, DirectTab> directTabControllers = new HashMap<>();
    @FXML
    private TabPane directTabPane;
    @FXML
    private TextField searchIDTextField;
    @FXML
    private Button searchIDButton;
    @FXML
    private Label searchIDLabel;
    @FXML
    private Button startANewChatButton;
    @FXML
    private Label startANewChatLabel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        createDefaultGroupTabs();
    }

    /**
     * Groups
     */
    private void createDefaultGroupTabs() {

        for (int i = 1; i <= 5; i++) {

            String groupName = "group#" + i;
            FXMLLoader loader = new FXMLLoader();
            AnchorPane tabNode = null;
            try {
                tabNode = loader.load(Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(getClass().getResource("group-tab.fxml")).openStream())));
            } catch (IOException exception) {
                exception.printStackTrace();
            }
            GroupTab groupTab = loader.getController();
            groupTab.setGroupName(groupName.replace("#", ""));
            groupTabControllers.put(groupName.replace("#", ""), groupTab);

            Tab tab = new Tab();
            tab.setContent(tabNode);
            tab.setText(groupName);
            groupTabPane.getTabs().add(tab);
        }
    }

    /**
     * Direct
     * <p>
     * To find a username and check if the username status (online/offline) to start a chat with
     */
    @FXML
    private void searchID() {

        if (!searchIDTextField.getText().isBlank() && !searchIDTextField.getText().isEmpty()) {
            new Thread(() -> {
                try {
                    Main.clientSocket.requestAUserPresence(searchIDTextField.getText());
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }).start();
        }
    }

    /**
     * @param username
     * @param result
     * @param additionalInfo
     */
    public void searchIDResponse(String username, String result, String additionalInfo) {

        if (result.equals("succeed")) { // OFFLINE - ONLINE

            if (additionalInfo.equals("online")) {
                startANewChatButton.setDisable(false);
            }
            Platform.runLater(() -> searchIDLabel.setText(username + " is " + additionalInfo + "!"));
        } else { // NOT FOUND - BLOCK
            Platform.runLater(() -> searchIDLabel.setText(additionalInfo));
        }
    }

    @FXML
    private void handleOnMouseClickedIDLabel() {

        searchIDLabel.setText("");
        startANewChatButton.setDisable(true);
        startANewChatLabel.setText("");
        searchIDLabel.setText("");
    }

    /**
     * If the searched usr ID was found and it was Online, a new tab will be created for these two users
     */
    @FXML
    private void startNewChat() throws IOException {

        String targetUser = searchIDTextField.getText();
        startANewChatButton.setDisable(true);
        searchIDTextField.setText("");
        searchIDLabel.setText("");

        if (directTabPaneIterator(targetUser)) {
            startANewChatLabel.setText("Chat already opened!");
        } else {
            setupNewChat(targetUser);
        }
    }

    /**
     * Iterating the direct tabs to check if the tab already exist or not
     *
     * @param targetUser
     * @return
     */
    private boolean directTabPaneIterator(String targetUser) {

        for (Tab t : directTabPane.getTabs()) {
            if (t.getText().equals(targetUser)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Setup new direct tab
     * <p>
     * add the new direct tab to direct tab pane
     * and if the chat ended, remove the tab from the direct tab pane
     *
     * @param targetUser
     * @return
     * @throws IOException
     */
    public boolean setupNewChat(String targetUser) throws IOException {

        if (!directTabPaneIterator(targetUser)) {

//            if (!Main.clientSocket.getLogin().equals(targetUser)) {
            new Thread(() -> {
                try {
                    Main.clientSocket.sendPrivateMessage(targetUser, "$START_DIRECT_CHAT$");
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }).start();
//            }

            // create a new tab
            FXMLLoader loader = new FXMLLoader();
            AnchorPane tabNode = loader.load(Objects.requireNonNull(Objects.requireNonNull(getClass().getResource("direct-tab.fxml").openStream())));
            DirectTab directTab = loader.getController();
            directTab.setTargetUsername(targetUser);
            directTabControllers.put(targetUser, directTab);

            Tab tab = new Tab();
            tab.setContent(tabNode);
            tab.setText(targetUser);
            Platform.runLater(() -> {
                if (!directTabPaneIterator(targetUser)) {
                    directTabPane.getTabs().add(tab);
                }
            });

            /**
             * close the tab if chat deleted
             * handle Delete button on direct
             */
            directTab.getLeaveButton().setOnAction(actionEvent -> {
                if (!Main.clientSocket.getLogin().equals(targetUser)) {
                    new Thread(() -> {
                        try {
                            Main.clientSocket.sendPrivateMessage(targetUser, "$CLOSE_DIRECT_CHAT$");
                        } catch (IOException exception) {
                            exception.printStackTrace();
                        }
                    }).start();
                }
                directTabPane.getTabs().remove(tab);
                directTabControllers.remove(targetUser);
            });
            directTab.getDeleteButton().setOnAction(actionEvent -> {
                directTabPane.getTabs().remove(tab);
                directTabControllers.remove(targetUser);
            });
            return false;
        }
        return true;
    }

    /**
     * Setter/Getter
     */
    public Map<String, GroupTab> getGroupTabControllers() {
        return groupTabControllers;
    }

    public Map<String, DirectTab> getDirectTabControllers() {
        return directTabControllers;
    }
}
package view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

public class Menu implements Initializable {

    private RegisterDialogPane registerDialogPane;

    @FXML
    private Pane rootPane;

    @FXML
    private ImageView image;

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;

    @FXML
    private Label alreadyLoggedInLabel;
    @FXML
    private Label invalidUsernameLabel;
    @FXML
    private Label wrongPasswordLabel;

    @FXML
    private Label registerLabel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        try {
            image.setImage(new Image(new FileInputStream("Client Module\\src\\view\\icon.png")));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void setLabelsNull() {
        invalidUsernameLabel.setText("");
        alreadyLoggedInLabel.setText("");
        wrongPasswordLabel.setText("");
    }

    @FXML
    private void login() {

        if (usernameField.getText().isEmpty() || usernameField.getText().isBlank()) {

            setLabelsNull();
            invalidUsernameLabel.setText("Enter username");
            passwordField.setText("");
        } else if (passwordField.getText().isEmpty() || passwordField.getText().isBlank()) {

            setLabelsNull();
            wrongPasswordLabel.setText("Enter password");
        } else {

            new Thread(() -> {
                try {
                    Main.clientSocket.login(usernameField.getText(), passwordField.getText());
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }).start();
            setLabelsNull();
        }
    }

    public void handleLoginResponse(String result, String reason) {


        if (result.equals("connected")) {

            Platform.runLater(() -> {
                StackPane pane = null;
                try {
                    FXMLLoader fxmlLoader = new FXMLLoader();
                    pane = fxmlLoader.load(Objects.requireNonNull(Objects.requireNonNull(getClass().getResource("chat.fxml")).openStream()));
                    Main.clientSocket.setChatController(fxmlLoader.getController());
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
                rootPane.getChildren().clear();
                rootPane.getChildren().addAll(pane);
            });
        } else {

            switch (reason) {
                case "this username doesn't exist." -> Platform.runLater(() -> {
                    invalidUsernameLabel.setText("Username does not exist!");
                    passwordField.setText("");
                });
                case "user already logged in." -> Platform.runLater(() -> {
                    alreadyLoggedInLabel.setText("Username already logged in!");
                    passwordField.setText("");
                });
                case "the password is incorrect." -> Platform.runLater(() -> {
                    wrongPasswordLabel.setText("Password wrong!");
                    passwordField.setText("");
                });
            }
        }
    }

    @FXML
    public void showRegisterDialog() throws IOException {

        FXMLLoader loader = new FXMLLoader();
        DialogPane dialogPane = loader.load(Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(getClass().getResource("register-dialog-pane.fxml")).openStream())));
        this.registerDialogPane = loader.getController();
        dialogPane.getButtonTypes().addAll(ButtonType.OK);
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setDialogPane(dialogPane);
        dialog.setTitle("Registration");
        dialog.showAndWait();
    }

    // getters & setters
    public RegisterDialogPane getRegisterDialogPane() {
        return registerDialogPane;
    }
}

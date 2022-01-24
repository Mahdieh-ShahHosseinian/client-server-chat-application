package view;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.io.IOException;
import java.util.Objects;

public class RegisterDialogPane {

    @FXML
    public TextField usernameField;
    @FXML
    public Label errorLabel;
    @FXML
    public Label invalidUsernameLabel;
    @FXML
    public PasswordField passwordField;
    @FXML
    public Label invalidPasswordLabel;
    @FXML
    public Button registerButton;

    private void setLabelsNull() {
        errorLabel.setText("");
        invalidUsernameLabel.setText("");
        invalidPasswordLabel.setText("");
    }

    public void register() {

        if (usernameField.getText().isEmpty() || usernameField.getText().isBlank()) {

            setLabelsNull();
            invalidUsernameLabel.setText("Enter username");
            passwordField.setText("");
        } else if (passwordField.getText().isEmpty() || passwordField.getText().isBlank()) {

            setLabelsNull();
            invalidPasswordLabel.setText("Enter password");
        } else {

            new Thread(() -> {
                try {
                    Main.clientSocket.register(usernameField.getText(), passwordField.getText());
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }).start();
            setLabelsNull();
        }
    }

    public void handleRegisterResponse(String result, String reason) {


        if (result.equals("accepted")) {

            Platform.runLater(() -> {

                Text text = new Text("Username successfully registered!");
                errorLabel.setText(text.getText());
                errorLabel.setStyle("-fx-text-fill : #4BB543;");
                registerButton.setDisable(true);
            });
        } else {

            switch (reason) {
                case "this username is already occupied." -> Platform.runLater(() -> {
                    errorLabel.setText("This username is already occupied.");
                    passwordField.setText("");
                });
                case "to choose username/password, only a-z 0-9 and underscores allowed." -> Platform.runLater(() -> {
                    errorLabel.setText("To choose username/password, only a-z 0-9 and underscores allowed.");
                    passwordField.setText("");
                });
                case "this username is too short. minimum length is 6 characters." -> Platform.runLater(() -> {
                    errorLabel.setText("This username is too short. Minimum length is 6 characters.");
                    passwordField.setText("");
                });
                case "this username is too long. maximum length is 15 characters." -> Platform.runLater(() -> {
                    errorLabel.setText("The username is too long. Maximum length is 15 characters.");
                    passwordField.setText("");
                });
                case "the password is too short. minimum length is 6 characters." -> Platform.runLater(() -> {
                    errorLabel.setText("The password is too short. Minimum length is 6 characters.");
                    passwordField.setText("");
                });
                case "the password is too long. maximum length is 15 characters." -> Platform.runLater(() -> {
                    errorLabel.setText("The password is too long. Maximum length is 15 characters.");
                    passwordField.setText("");
                });
            }
        }
    }
}

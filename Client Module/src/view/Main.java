package view;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import program.ClientSideProgram;
import program.ClientSock;

import java.io.IOException;
import java.util.Objects;

/**
 * The Application for Client-Side
 */
public class Main extends Application {

    public static Scene scene;
    public static ClientSock clientSocket;

    static {
        ClientSideProgram clientSideProgram = new ClientSideProgram("localhost", 50000);
        clientSocket = clientSideProgram.getClientSocket();
        clientSocket.start();
    }

    @Override
    public void start(Stage stage) throws Exception {

        /**
         * If the server was off, connection will be refused
         */
        if (clientSocket.getSocket() != null) {

            FXMLLoader fxmlLoader = new FXMLLoader();
            Parent root = fxmlLoader.load(Objects.requireNonNull(getClass().getResource("menu.fxml")).openStream());
            clientSocket.setMenu(fxmlLoader.getController());
            scene = new Scene(root);
            stage.setTitle("Simple Chat Messenger");
            stage.getIcons().add(new Image("view/icon.png"));
            stage.setResizable(false);
            stage.setScene(scene);
            stage.show();

            stage.setOnCloseRequest(event -> {

                try {
                    clientSocket.getSocket().close();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            });
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Oops! Server Down. Try later");
            alert.showAndWait();
            Platform.exit();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

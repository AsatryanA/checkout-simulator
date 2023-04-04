package application;

import controller.ShellController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class MainApp extends Application {
    private static Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        setPrimaryStage(stage);
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/shell.fxml"));
        Parent root = fxmlLoader.load();
        ShellController shellController = fxmlLoader.getController();
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/icon.png"))));

        Scene scene = new Scene(root);

        stage.setTitle("Դրամարկղերի սիմուլյատոր");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();

        primaryStage.setOnCloseRequest(windowEvent -> {
            shellController.exit();
            System.exit(0);
        });
    }
    public static Stage getPrimaryStage() {
        return primaryStage;
    }
    private void setPrimaryStage(Stage p) {
        primaryStage = p;
    }
}

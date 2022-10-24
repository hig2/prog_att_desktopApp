import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.Objects;


public class ProgrammingAttenuator extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass()
                    .getResource("gui.fxml")));
            primaryStage.setTitle("Аттенюатор v1.0");
            primaryStage.setScene(new Scene(root, 278, 280, Color.web("#3C3F41")));
            //primaryStage.getIcons().add(new Image("picture/favicon.png"));
            primaryStage.setResizable(false);
            primaryStage.show();

            primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    Platform.exit();
                    System.exit(0);
                }
            });
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
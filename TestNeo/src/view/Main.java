package view;

import java.io.File;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Main extends Application {
	
		@Override
		public void start(Stage primaryStage) {
			try {
				Pane root = (Pane) FXMLLoader.load(getClass().getResource("GUIOfNeo4jTextSearch.fxml"));
				Scene scene = new Scene(root,942,636);
				File imageFile = new File("JavaFXApplicationIcon.jpg");
				Image image = new Image(imageFile.toURI().toString());
				primaryStage.getIcons().add(image);
				primaryStage.setTitle("NEO4J TEXT SEARCH");
				primaryStage.setScene(scene);
				primaryStage.show();
				root.requestFocus();
				primaryStage.setOnCloseRequest((EventHandler<WindowEvent>) new EventHandler<WindowEvent>() {
				    @Override
				    public void handle(WindowEvent t) {
				        Platform.exit();
				        System.exit(0);
				    }
				});
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		public static void main(String[] args) {
			launch(args);
		}
	}
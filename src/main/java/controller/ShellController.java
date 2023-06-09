package controller;

import application.MainApp;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.MainModel;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ShellController extends Controller {
    public AnchorPane mainContainer;
    public AnchorPane sideContainer;
    public AnchorPane preferenceContainer;
    public Tab preferencesTab;
    public Tab simulationTab;
    public Tab statisticsTab;
    public TabPane stepTabPane;
    public AnchorPane statisticsContainer;

    private Stage popup;
    private Pane popupContainer;


    public void initialize(URL location, ResourceBundle resources) {
        model = MainModel.getInstance();
        model.shellController = this;
        setStep(0);
        stepTabPane.getSelectionModel().select(preferencesTab);
        this.popup = new Stage();
        this.popup.initModality(Modality.APPLICATION_MODAL);
        this.popup.initOwner(MainApp.getPrimaryStage());
        this.popupContainer = new Pane();
        Scene popupScene = new Scene(popupContainer);
        this.popup.setScene(popupScene);
        this.popup.setResizable(false);
    }

    public void setStep(int i) {
        switch (i) {
            case 0 -> {
                try {
                    loadView("preference.fxml", preferenceContainer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    loadView("simulator.fxml", mainContainer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    loadView("output.fxml", sideContainer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    loadView("statistics.fxml", statisticsContainer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                simulationTab.setDisable(true);
                statisticsTab.setDisable(true);
                preferencesTab.setDisable(false);
            }
            case 1 -> {
                simulationTab.setDisable(false);
                statisticsTab.setDisable(false);
                preferencesTab.setDisable(true);
                model.simulatorController.initSimulator();
            }
        }

    }

    public void setPopup(String viewName, int width, int height) {
        String title = viewName.substring(0, 1).toUpperCase() + viewName.substring(1).replace(".fxml", "");
        this.popup.setTitle(title);

        try {
            loadView(viewName, popupContainer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        popup.setHeight(height + 30);
        popup.setWidth(width);
        popup.show();
    }

    public void exit() {
        System.exit(0);
    }
}

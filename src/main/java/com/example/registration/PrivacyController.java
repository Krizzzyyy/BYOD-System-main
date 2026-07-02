package com.example.registration;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;

public class PrivacyController {

    @FXML private CheckBox consentCheck;
    @FXML private Button okBtn;
    @FXML private Button cancelBtn;

    private boolean agreed = false;

    @FXML
    public void initialize() {
        consentCheck.selectedProperty().addListener((obs, o, n) -> {
            okBtn.setDisable(!n);
            okBtn.getStyleClass().removeAll("btn-disabled", "btn-save");
            okBtn.getStyleClass().add(n ? "btn-save" : "btn-disabled");
        });
    }

    @FXML
    private void handleAgree() {
        agreed = true;
        closeStage();
    }

    @FXML
    private void handleCancel() {
        agreed = false;
        closeStage();
    }

    public boolean isAgreed() {
        return agreed;
    }

    private void closeStage() {
        Stage stage = (Stage) okBtn.getScene().getWindow();
        stage.close();
    }
}
package com.example.monitoring;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

public class QRRegistrationSuccessWindow {

    public static void show(Stage ownerStage, String formId, String qrFilePath) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.WINDOW_MODAL);
        popupStage.initOwner(ownerStage);
        popupStage.setTitle("Registration Successful");
        popupStage.setResizable(false);

        Label headerLabel = new Label("Registration Successful");
        headerLabel.setWrapText(true);
        headerLabel.setTextOverrun(OverrunStyle.CLIP);
        headerLabel.setMaxWidth(Double.MAX_VALUE);
        headerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");

        Label messageLabel = new Label("Item registered successfully.\n\nPlease take a clear photo of this QR code and scan it when exiting the campus.");
        messageLabel.setWrapText(true);
        messageLabel.setTextOverrun(OverrunStyle.CLIP);
        messageLabel.setMaxWidth(Double.MAX_VALUE);
        messageLabel.setMinHeight(Region.USE_PREF_SIZE);
        messageLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #333333; -fx-line-spacing: 5px;");

        Label idLabel = new Label("Form ID: " + formId);
        idLabel.setWrapText(true);
        idLabel.setTextOverrun(OverrunStyle.CLIP);
        idLabel.setMaxWidth(Double.MAX_VALUE);
        idLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #666666;");

        Button closeButton = new Button("Close");
        closeButton.setMinWidth(110);
        closeButton.setStyle(
                "-fx-background-color: #2e7d32; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 10 24 10 24; " +
                        "-fx-background-radius: 5; " +
                        "-fx-cursor: hand;"
        );
        closeButton.setOnAction(e -> popupStage.close());

        VBox leftColumn = new VBox(18, headerLabel, messageLabel, idLabel, closeButton);
        leftColumn.setAlignment(Pos.CENTER_LEFT);
        leftColumn.setPrefWidth(380);
        leftColumn.setMinWidth(380);

        ImageView qrImageView = new ImageView();
        qrImageView.setFitWidth(260);
        qrImageView.setFitHeight(260);
        qrImageView.setPreserveRatio(true);

        try {
            File file = new File(qrFilePath);
            if (file.exists()) {
                Image qrImage = new Image(file.toURI().toString());
                qrImageView.setImage(qrImage);
            }
        } catch (Exception ex) {
            System.err.println("Failed to render QR image element: " + ex.getMessage());
        }

        VBox rightColumn = new VBox(qrImageView);
        rightColumn.setAlignment(Pos.CENTER);
        rightColumn.setStyle(
                "-fx-background-color: #ffffff; " +
                        "-fx-padding: 15; " +
                        "-fx-border-color: #e0e0e0; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8;"
        );

        HBox mainLayout = new HBox(30, leftColumn, rightColumn);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setPadding(new Insets(30));
        mainLayout.setStyle("-fx-background-color: #fcfbf7;");

        Scene scene = new Scene(mainLayout, 740, 360);
        popupStage.setScene(scene);
        popupStage.centerOnScreen();
        popupStage.showAndWait();
    }
}

package com.example.monitoring;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

public class QRScannerWindow {

    public interface QRScanListener {
        void onQRScanned(String qrPayload);
    }

    public static void openScanner(Stage ownerStage, QRScanListener listener) {
        Webcam webcam = Webcam.getDefault();
        if (webcam == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "No camera device detected on this system.");
            alert.showAndWait();
            return;
        }

        Stage scannerStage = new Stage();
        scannerStage.initModality(Modality.WINDOW_MODAL);
        scannerStage.initOwner(ownerStage);
        scannerStage.setTitle("Scan Student QR Code");

        ImageView videoContainer = new ImageView();
        Label statusLabel = new Label("Align QR Code within the camera frame...");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-padding: 10; -fx-text-fill: #333;");

        VBox layout = new VBox(10, videoContainer, statusLabel);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #f7f3ec; -fx-padding: 20;");

        // Enforce smooth resolution aspect ratios
        Dimension size = WebcamResolution.QVGA.getSize(); // 320x240
        webcam.setViewSize(size);

        Scene scene = new Scene(layout, 360, 320);
        scannerStage.setScene(scene);

        AtomicBoolean isRunning = new AtomicBoolean(true);

        // Background thread dedicated to capturing video frames and reading layout data
        Thread captureThread = new Thread(() -> {
            try {
                webcam.open();
                while (isRunning.get()) {
                    if (!webcam.isOpen()) break;

                    BufferedImage image = webcam.getImage();
                    if (image != null) {
                        // Cast frame to JavaFX canvas rendering element safely
                        WritableImage fxImage = SwingFXUtils.toFXImage(image, null);
                        Platform.runLater(() -> videoContainer.setImage(fxImage));

                        try {
                            // Run frame matrix payload detection via ZXing
                            LuminanceSource source = new BufferedImageLuminanceSource(image);
                            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                            Result result = new MultiFormatReader().decode(bitmap);

                            if (result != null && !result.getText().isEmpty()) {
                                isRunning.set(false); // Stop processing frames instantly
                                Platform.runLater(() -> {
                                    scannerStage.close();
                                    listener.onQRScanned(result.getText());
                                });
                                break;
                            }
                        } catch (NotFoundException ignored) {
                            // QR code wasn't found in this specific frame; continue loop safely
                        }
                    }
                    Thread.sleep(60); // Roughly ~15-18 FPS processing to preserve CPU overhead
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                webcam.close();
            }
        });

        // Ensure safe container closing events safely release hardware hooks
        scannerStage.setOnCloseRequest(event -> {
            isRunning.set(false);
        });

        scannerStage.show();
        captureThread.setDaemon(true);
        captureThread.start();
    }
}
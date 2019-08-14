package com.xhxj.ocr.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;

@Component
public class OcrTxtController {
    private Stage ocrTxtBox;
    @FXML
    TextField grayLeve;
    @FXML
    TextField threadSleep;
    @FXML
    Button submit;

    public void showOcrTxtBox() {

        AnchorPane anchorPane = null;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader();
            URL url = fxmlLoader.getClassLoader().getResource("view/ocrTxt.fxml");
            fxmlLoader.setLocation(url);
            anchorPane = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }


        Scene scene = new Scene(anchorPane);
        ocrTxtBox = new Stage();
        ocrTxtBox.setTitle("选项");
        ocrTxtBox.setScene(scene);
        ocrTxtBox.show();
    }

    @FXML
    private void initialize() {
        submit.setOnAction(event -> {
            String grayLeveText = grayLeve.getText();
            String threadSleepText = threadSleep.getText();
            MainController.grayLeve = Double.parseDouble(grayLeveText);
            MainController.threadSleep = Long.parseLong(threadSleepText);
            grayLeve.setText(String.valueOf(MainController.grayLeve));
            threadSleep.setText(String.valueOf(MainController.threadSleep));
        });
    }
}

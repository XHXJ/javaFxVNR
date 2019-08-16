package com.xhxj.ocr.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import net.sourceforge.tess4j.util.LoggHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;

@Component
/**
 * @description:
 * @author: zdthm2010@gmail.com
 * @date: 2019-08-15 19:34
 */
public class ShowOptionController {
    private Stage ocrTxtBox;
    @FXML
    private TextField grayLeve;
    @FXML
    private TextField threadSleep;
    @FXML
    private Button submit;
    @FXML
    private CheckBox color;
    @FXML
    private TextField ocrLanguage;
    @FXML
    private TextField tessdataPath;
    private static final Logger logger = LoggerFactory.getLogger(new LoggHelper().toString());
    public void showOption() {
        AnchorPane anchorPane = null;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader();
            URL url = fxmlLoader.getClassLoader().getResource("view/OcrTxt.fxml");
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
        grayLeve.setText(String.valueOf(MainController.grayLeve));
        threadSleep.setText(String.valueOf(MainController.threadSleep));
        ocrLanguage.setText(MainController.ocrLanguage);
        tessdataPath.setText(MainController.tessdataPath);

        submit.setOnAction(event -> {
            String grayLeveText = grayLeve.getText();
            String threadSleepText = threadSleep.getText();
            MainController.grayLeve = Double.parseDouble(grayLeveText);
            MainController.threadSleep = Long.parseLong(threadSleepText);
            grayLeve.setText(String.valueOf(MainController.grayLeve));
            threadSleep.setText(String.valueOf(MainController.threadSleep));

            MainController.ocrLanguage = ocrLanguage.getText();
            ocrLanguage.setText(MainController.ocrLanguage);
            MainController.tessdataPath = tessdataPath.getText();
            tessdataPath.setText(MainController.tessdataPath);
        });
        color.setSelected(MainController.colorBoolean);
        color.selectedProperty().addListener((observable, oldValue, newValue) -> {
            MainController.colorBoolean = newValue;
            logger.info("二值化处理 :"+newValue);
        });
    }
}

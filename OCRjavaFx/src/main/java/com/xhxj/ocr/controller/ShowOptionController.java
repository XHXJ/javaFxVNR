package com.xhxj.ocr.controller;

import com.xhxj.ocr.SysConfig;
import de.felixroske.jfxsupport.FXMLController;
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


/**
 设置选项
 @description:
  * @author: zdthm2010@gmail.com
 * @date: 2019-08-15 19:34
 */
@FXMLController
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
        grayLeve.setText(String.valueOf(SysConfig.grayLeve));
        threadSleep.setText(String.valueOf(SysConfig.threadSleep));
        ocrLanguage.setText(SysConfig.ocrLanguage);
        tessdataPath.setText(SysConfig.tessdataPath);

        submit.setOnAction(event -> {
            String grayLeveText = grayLeve.getText();
            SysConfig.grayLeve = Double.parseDouble(grayLeveText);
            grayLeve.setText(String.valueOf(SysConfig.grayLeve));

            String threadSleepText = threadSleep.getText();
            SysConfig.threadSleep = Long.parseLong(threadSleepText);
            threadSleep.setText(String.valueOf(SysConfig.threadSleep));

            SysConfig.ocrLanguage = ocrLanguage.getText();
            ocrLanguage.setText(SysConfig.ocrLanguage);
            SysConfig.tessdataPath = tessdataPath.getText();
            tessdataPath.setText(SysConfig.tessdataPath);

            //写出配置文件
            new SysConfig().outSysConfig();
        });
        color.setSelected(SysConfig.colorBoolean);
        color.selectedProperty().addListener((observable, oldValue, newValue) -> {
            SysConfig.colorBoolean = newValue;
            logger.info("二值化处理 :"+newValue);
        });
    }
}



package com.xhxj.ocr.controller;

import com.xhxj.ocr.dao.SceneDao;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.sourceforge.tess4j.util.LoggHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.List;


/**
 * @description:
 * @author: zdthm2010@gmail.com
 * @date: 2019-08-15 19:34
 */
@Component
public class ListController {
    private static final Logger logger = LoggerFactory.getLogger(new LoggHelper().toString());
    @Autowired
    MainController mainController;
    //保存获取到的选取名字
    private String selectName = "";

    private Stage mainMarquee;

    private Stage interfaceBox;
    @FXML
    private ListView listView;
    @FXML
    private Button delete;

    List<SceneDao> daos = mainController.sceneDaos;

    @FXML
    private void initialize() {

        ObservableList<String> objects = FXCollections.observableArrayList();
//        List<String> collect = sceneDaos.parallelStream().map(sceneDao -> sceneDao.getName()).collect(Collectors.toList());
//        logger.info("拉姆达 :"+collect);
        daos.forEach(sceneDao -> objects.add(sceneDao.getName()));
        listView.setItems(objects);
        listView.getSelectionModel().selectedItemProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
            if (!"".equals(newValue) && null != newValue) {

                logger.info("获取选框数据" + newValue);
                selectName = newValue;
                if (interfaceBox!=null){
                    interfaceBox.close();
                }
                //选取的时候应该显示该窗口大小
                getMainInterfaceBox();
            }
        });

        delete.setOnAction(event -> {
            daos.removeIf(sceneDao -> sceneDao.getName().equals(selectName));
            logger.info("删除成功!剩余 :" + mainController.sceneDaos);
            ObservableList<String> list = FXCollections.observableArrayList();
            daos.forEach(sceneDao -> list.add(sceneDao.getName()));
            listView.setItems(list);
            if (interfaceBox!=null){
                interfaceBox.close();
            }
        });


    }


    /**
     * 选择框窗口
     */
    private void getMainInterfaceBox() {
        AnchorPane an = new AnchorPane();
        an.setStyle("-fx-background-color: #00000000");
        Scene scene = new Scene(an);
        scene.setFill(Paint.valueOf("#00000000"));
        interfaceBox = new Stage();
        interfaceBox.initStyle(StageStyle.TRANSPARENT);
        interfaceBox.setScene(scene);
        interfaceBox.setFullScreen(true);
        interfaceBox.setFullScreenExitHint("");
        interfaceBox.show();
        //esc退出
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                interfaceBox.close();
//                mainMarquee.setIconified(false);
            }
        });

        SceneDao sceneDao = new SceneDao();
        for (SceneDao dao : daos) {
            if (dao.getName().equals(selectName)) {
                sceneDao = dao;
            }
        }
        double sceneY_start = sceneDao.getSceneY_start();
        double sceneX_start = sceneDao.getSceneX_start();
        double sceney_end = sceneDao.getSceney_End();
        double sceneX_end = sceneDao.getSceneX_End();

        HBox hBox = new HBox();

        hBox.setBackground(null);
        hBox.setBorder(new Border(new BorderStroke(Paint.valueOf("#FB7299"), BorderStrokeStyle.SOLID, null, new BorderWidths(2))));


        AnchorPane.setLeftAnchor(hBox, sceneX_start);
        AnchorPane.setTopAnchor(hBox, sceneY_start);

        double width = sceneX_end - sceneX_start;
        double height = sceney_end - sceneY_start;
        hBox.setPrefWidth(width);
        hBox.setPrefHeight(height);

        an.getChildren().add(hBox);

    }


    /**
     * 显示选框管理
     */
    public void showMainMarquee() {
        AnchorPane anchorPane = null;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader();
            URL url = fxmlLoader.getClassLoader().getResource("view/MainInterface.fxml");
            fxmlLoader.setLocation(url);
            anchorPane = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }


        Scene scene = new Scene(anchorPane);
        mainMarquee = new Stage();
        mainMarquee.setTitle("翻译区域管理");
        mainMarquee.setScene(scene);
        mainMarquee.show();


    }


}

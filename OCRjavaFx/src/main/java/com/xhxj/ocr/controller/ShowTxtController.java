package com.xhxj.ocr.controller;

import com.xhxj.ocr.ShowTxtTaskExecutePool;
import com.xhxj.ocr.TaskExecutePool;
import com.xhxj.ocr.dao.SceneDao;
import com.xhxj.ocr.tool.TimingShowServiceTask;
import javafx.application.Platform;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import net.sourceforge.tess4j.util.LoggHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * @description:
 * @author: zdthm2010@gmail.com
 * @date: 2019-08-15 19:38
 */
@Component
public class ShowTxtController {
    @Autowired
    ShowTxtTaskExecutePool showTxtTaskExecutePool;
    @Autowired
    TaskExecutePool taskExecutePool;


    //保存所有的窗口
    static List<Stage> stageList = new ArrayList<>();
    static List<TimingShowServiceTask> timingShowServiceTasks = new ArrayList<>();

    private double xOffset = 0;
    private double yOffset = 0;

    private static final Logger logger = LoggerFactory.getLogger(new LoggHelper().toString());

    /**
     * 显示透明可拖拽的窗口在上面显示文字
     */
    public void startShowText() {
        Executor executor = showTxtTaskExecutePool.myTaskAsyncPool();
//        Platform.setImplicitExit(false);

        //把所有的sceneDaos显示出来
        for (SceneDao sceneDao : MainController.sceneDaos) {
            double sceneY_start = sceneDao.getSceneY_start();
            double sceneX_start = sceneDao.getSceneX_start();
            double sceney_end = sceneDao.getSceney_End();
            double sceneX_end = sceneDao.getSceneX_End();

            double width = sceneX_end - sceneX_start;
            double height = sceney_end - sceneY_start;

            Stage showTxtStage = new Stage();
            showTxtStage.initStyle(StageStyle.TRANSPARENT);
            BorderPane root = new BorderPane();
//            root.setStyle("-fx-background-color: #000000");

            root.setStyle("-fx-background-color: #00000000");
            /*
             * 鼠标按下时，记下相对于 root左上角(0,0) 的 x, y坐标, 也就是x偏移量 = x - 0, y偏移量 = y - 0
             */
            root.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });
            /*
             * 根据偏移量设置primaryStage新的位置
             */
            root.setOnMouseDragged(event -> {
                showTxtStage.setX(event.getScreenX() - xOffset);
                showTxtStage.setY(event.getScreenY() - yOffset);
            });

            TextArea textArea = new TextArea(sceneDao.getTranslation());
            textArea.setMaxWidth(width/2 * 0.8);
            textArea.setMaxHeight(height/2 * 0.8);
            textArea.setWrapText(true);
            textArea.setEditable(false);
            textArea.getStylesheets().add("css/area.css");

            Label label1 = new Label(sceneDao.getName());
            label1.setTextFill(Color.web("#FFFFFF"));
            textArea.setId(sceneDao.getName());


            root.setCenter(textArea);
            root.setLeft(label1);


            Scene scene = new Scene(root, width/2, height/2);
            scene.setFill(Paint.valueOf("#00000020"));
            showTxtStage.setAlwaysOnTop(true);
            showTxtStage.setScene(scene);
            showTxtStage.setY(sceneY_start);
            showTxtStage.setX(sceneX_start);
            showTxtStage.show();

            TimingShowServiceTask timingShowServiceTask = new TimingShowServiceTask();
            //启用更新文字线程
            timingShowServiceTask.setName(sceneDao.getName());
            timingShowServiceTask.setDelay(Duration.seconds(1));
            timingShowServiceTask.setPeriod(Duration.millis(100));
            timingShowServiceTask.setExecutor(executor);
            timingShowServiceTask.start();

            timingShowServiceTask.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (StringUtils.isNotEmpty(newValue)){
                    textArea.setText(newValue);
                }
            });

            timingShowServiceTasks.add(timingShowServiceTask);
            stageList.add(showTxtStage);

            //esc退出
            scene.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    showTxtStage.close();
                }
            });
        }
    }

    /**
     * 关闭所有文本框
     */
    public void offShowText() {
        timingShowServiceTasks.forEach(Service::cancel);
        stageList.forEach(Stage::close);
    }

}

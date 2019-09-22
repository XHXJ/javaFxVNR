package com.xhxj.ocr.controller;

import com.xhxj.ocr.dao.SceneDao;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
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

    //范围框
    private Stage interfaceBox;
    @FXML
    private ListView listView;
    //原文
    @FXML
    public TextArea originalText;
    //译文
    @FXML
    public TextArea translationText;

    @FXML
    public TextField maxWhiteSpaceText;
    @FXML
    public TextField maxFontLineWidthText;
    @FXML
    public TextField grayScaleThresholdText;
    @FXML
    public TextField minTextWidthText;
    //测试
    @FXML
    public Button testButton;
    //确认
    @FXML
    public Button confirm;
    //删除
    @FXML
    private Button delete;
    //文本框识别
    @FXML
    public CheckBox ocrmodelTextBox;
    //阀值滑动条
    @FXML
    public Slider grayLeveSlider;
    //启用二值化
    @FXML
    public CheckBox colorBooleanBox;
    //排除空格
    @FXML
    public CheckBox ruleOutSpaceBox;
    //输出到文件
    @FXML
    public CheckBox outputFileBox;
    //二值化后的图片
    @FXML
    public ImageView outImageAllView;


    @FXML
    public ImageView ImageVi;
    @FXML
    public ImageView outImageVi;

    List<SceneDao> daos = mainController.sceneDaos;
    SceneDao sceneDao;

    @FXML
    private void initialize() {

        ObservableList<String> objects = FXCollections.observableArrayList();
//        List<String> collect = sceneDaos.parallelStream().map(sceneDao -> sceneDao.getName()).collect(Collectors.toList());
//        logger.info("拉姆达 :"+collect);
        daos.forEach(sceneDao -> objects.add(sceneDao.getName()));
        listView.setItems(objects);
        listView.getSelectionModel().selectedItemProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
            if (!"".equals(newValue) && null != newValue) {
                selectName = newValue;
                if (interfaceBox != null) {
                    interfaceBox.close();
                }
                sceneDao = daos.parallelStream().filter(s -> s.getName().equals(selectName)).findFirst().get();
                logger.info("获取选框数据" + sceneDao);
                //选取的时候应该显示该窗口大小
                getMainInterfaceBox(sceneDao);
                //刷新所有信息并显示
                refreshAll();
            }
        });

        ocrmodelTextBox.selectedProperty().addListener((observable, oldValue, newValue) -> sceneDao.setOcrModelText(newValue));
        colorBooleanBox.selectedProperty().addListener((observable, oldValue, newValue) -> sceneDao.setColorBoolean(newValue));
        ruleOutSpaceBox.selectedProperty().addListener((observable, oldValue, newValue) -> sceneDao.setRuleOutSpace(newValue));
        outputFileBox.selectedProperty().addListener((observable, oldValue, newValue) -> sceneDao.setOutputFile(newValue));
        grayLeveSlider.valueProperty().addListener((observable, oldValue, newValue) -> sceneDao.setGrayLeve((Double) newValue));

        //确定键更新值并刷新所有值
        confirm.setOnAction(event -> {
//            maxWhiteSpaceText.getText()
////                    maxFontLineWidthText
////            grayScaleThresholdText
////                    minTextWidthText
            sceneDao.setMaxWhiteSpace(Integer.parseInt(maxWhiteSpaceText.getText()));
            sceneDao.setMaxFontLineWidth(Integer.parseInt(maxFontLineWidthText.getText()));
            sceneDao.setGrayScaleThreshold(Integer.parseInt(grayScaleThresholdText.getText()));
            sceneDao.setMinTextWidth(Integer.parseInt(minTextWidthText.getText()));
            refreshAll();
        });

        delete.setOnAction(event -> {
            daos.removeIf(sceneDao -> sceneDao.getName().equals(selectName));
            logger.info("删除成功!剩余 :" + mainController.sceneDaos);
            ObservableList<String> list = FXCollections.observableArrayList();
            daos.forEach(sceneDao -> list.add(sceneDao.getName()));
            listView.setItems(list);
            if (interfaceBox != null) {
                interfaceBox.close();
            }
        });


    }

    /**
     * 刷新所有信息
     */
    private void refreshAll(){
        //显示所有信息
        originalText.setText(sceneDao.getOriginal());
        translationText.setText(sceneDao.getTranslation());
        maxWhiteSpaceText.setText(String.valueOf(sceneDao.getMaxWhiteSpace()));
        maxFontLineWidthText.setText(String.valueOf(sceneDao.getMaxFontLineWidth()));
        grayScaleThresholdText.setText(String.valueOf(sceneDao.getGrayScaleThreshold()));
        minTextWidthText.setText(String.valueOf(sceneDao.getMinTextWidth()));
        //第一次开启截图时没有运行识别程序时没有图片
        if (sceneDao.getImage() != null) {
            ImageVi.setImage(SwingFXUtils.toFXImage(sceneDao.getImage(), null));
        }
        if (sceneDao.getOutImage() != null) {
            outImageVi.setImage(SwingFXUtils.toFXImage(sceneDao.getOutImage(), null));
        }
        if (sceneDao.getOutImageAll().size()>0){
            outImageAllView.setImage(SwingFXUtils.toFXImage(sceneDao.getOutImageAll().get(0),null));
        }
        ocrmodelTextBox.setSelected(sceneDao.isOcrModelText());
        colorBooleanBox.setSelected(sceneDao.isColorBoolean());
        ruleOutSpaceBox.setSelected(sceneDao.isRuleOutSpace());
        outputFileBox.setSelected(sceneDao.isOutputFile());
        grayLeveSlider.setValue(sceneDao.getGrayLeve());

    }

    /**
     * 显示选择框
     *
     * @param sceneDao 需要显示的对象
     */
    private void getMainInterfaceBox(SceneDao sceneDao) {
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
        BorderPane anchorPane = null;
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

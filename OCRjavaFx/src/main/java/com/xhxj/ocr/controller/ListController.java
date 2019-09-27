package com.xhxj.ocr.controller;

import com.xhxj.ocr.OcrApplication;
import com.xhxj.ocr.View.ListStageView;
import com.xhxj.ocr.View.SelectBoxView;
import com.xhxj.ocr.dao.SceneDao;
import de.felixroske.jfxsupport.FXMLController;
import de.felixroske.jfxsupport.GUIState;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.sourceforge.tess4j.util.LoggHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @description:
 * @author: zdthm2010@gmail.com
 * @date: 2019-08-15 19:34
 */
@FXMLController
public class ListController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(new LoggHelper().toString());


    @Autowired
    MainController mainController;
    @Autowired
    ListStageView listStageView;

    //保存获取到的选取名字
    private String selectName = "";

    private Stage mainMarquee;

    //范围框
    public Stage interfaceBox;
    @FXML
    public ListView listView;
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
    public Button delete;
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
    //下一张图
    @FXML
    public Button nextButton;

    @FXML
    public ImageView ImageVi;
    @FXML
    public ImageView outImageVi;
    @FXML
    public Label cpLabel;

    public HBox imageBox;
    private SceneDao sceneDao;

    private AtomicInteger count = new AtomicInteger();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ObservableList<String> objects = FXCollections.observableArrayList();
        MainController.sceneDaos.forEach(sceneDao -> objects.add(sceneDao.getName()));
        listView.setItems(objects);
        listView.getSelectionModel().selectedItemProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
            if (!"".equals(newValue) && null != newValue) {
                selectName = newValue;
                if (interfaceBox != null) {
                    interfaceBox.close();
                }
                sceneDao = MainController.sceneDaos.parallelStream().filter(s -> s.getName().equals(selectName)).findFirst().get();
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
            sceneDao.setMaxWhiteSpace(Integer.parseInt(maxWhiteSpaceText.getText()));
            sceneDao.setMaxFontLineWidth(Integer.parseInt(maxFontLineWidthText.getText()));
            sceneDao.setGrayScaleThreshold(Integer.parseInt(grayScaleThresholdText.getText()));
            sceneDao.setMinTextWidth(Integer.parseInt(minTextWidthText.getText()));
            refreshAll();
        });

        nextButton.setOnAction(event -> {
            if (sceneDao.getOutImageAll().size()>0){
                if (count.get()>=sceneDao.getOutImageAll().size()){
                    count.set(0);
                }
                outImageAllView.setImage(SwingFXUtils.toFXImage(sceneDao.getOutImageAll().get(count.get()),null));
                count.incrementAndGet();
            }
        });
        testButton.setOnAction(event -> {
            class myTask extends Task<Number> {
                @Override
                protected void updateValue(Number value) {
                    WritableImage writableImage = SwingFXUtils.toFXImage(sceneDao.getOutImage(), null);
                    //复制到剪切板
                    Clipboard cp = Clipboard.getSystemClipboard();
                    ClipboardContent content = new ClipboardContent();
                    content.putImage(writableImage);
                    cp.setContent(content);
                    cpLabel.setText("处理后的完整图片已保存至剪切板....");
                    super.updateValue(value);
                }

                @Override
                protected Number call() throws Exception {
                    mainController.getScreenImg();
                    mainController.orcStart();
                    refreshAll();
                    return null;
                }
            }
            myTask myTask = new myTask();

            new Thread(myTask).start();

        });

        delete.setOnAction(event -> {
            MainController.sceneDaos.removeIf(sceneDao -> sceneDao.getName().equals(selectName));
            logger.info("删除成功!剩余 :" + MainController.sceneDaos);
            ObservableList<String> list = FXCollections.observableArrayList();
            MainController.sceneDaos.forEach(sceneDao -> list.add(sceneDao.getName()));
            listView.setItems(list);
            if (sceneDao != null) {
                deleteAll();
            }

            if (interfaceBox != null) {
                interfaceBox.close();
            }
        });

    }

    /**
     * 刷新所有信息
     */
    private void refreshAll() {
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
        if (sceneDao.getOutImageAll().size() > 0) {
            outImageAllView.setImage(SwingFXUtils.toFXImage(sceneDao.getOutImageAll().get(0), null));
        }
        ocrmodelTextBox.setSelected(sceneDao.isOcrModelText());
        colorBooleanBox.setSelected(sceneDao.isColorBoolean());
        ruleOutSpaceBox.setSelected(sceneDao.isRuleOutSpace());
        outputFileBox.setSelected(sceneDao.isOutputFile());
        grayLeveSlider.setValue(sceneDao.getGrayLeve());

    }

    /**
     * 删除后应该清空全部
     */
    private void deleteAll() {
        originalText.setText("");
        translationText.setText("");
        maxWhiteSpaceText.setText("");
        maxFontLineWidthText.setText("");
        grayScaleThresholdText.setText("");
        minTextWidthText.setText("");
        //第一次开启截图时没有运行识别程序时没有图片
        ImageVi.setImage(null);
        outImageVi.setImage(null);
        outImageAllView.setImage(null);
        ocrmodelTextBox.setSelected(sceneDao.isOcrModelText());
        colorBooleanBox.setSelected(sceneDao.isColorBoolean());
        ruleOutSpaceBox.setSelected(sceneDao.isRuleOutSpace());
        outputFileBox.setSelected(sceneDao.isOutputFile());
        grayLeveSlider.setValue(sceneDao.getGrayLeve());
        sceneDao = null;
    }

    @Autowired
    SelectBoxView selectBoxView;

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
}

package com.xhxj.ocr.controller;

import com.alibaba.fastjson.JSONObject;
import com.xhxj.ocr.TaskExecutePool;
import com.xhxj.ocr.dao.FanyiBaiduDao;
import com.xhxj.ocr.dao.FanyiBaiduTxtDao;
import com.xhxj.ocr.dao.SceneDao;
import com.xhxj.ocr.tool.ImagePicture;
import com.xhxj.ocr.tool.baidu.TransApi;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.sourceforge.tess4j.util.LoggHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

@Controller
public class MainController {
    @Autowired
    TaskExecutePool taskExecutePool;
    @Autowired
    ListController listController;
    @Autowired
    OcrTxtController ocrTxtController;


    //文字存取图片
    ImageView iv;
    BufferedImage bufferedImage;

    Stage primary;
    Stage stage;
    Stage ocrTxt;
    Stage ocrTxtBox;

    HBox view;

    //截图坐标
    double sceneX_start;
    double sceneY_start;
    double sceneX_End;
    double sceney_End;

    //所有的选择框
    static List<SceneDao> sceneDaos = new ArrayList<>();

    //定义变量
    //识别间隔
    public static long threadSleep = 1000;
    public static double grayLeve = 9.0;


    //输出路径
    File out = new File("out");

    //开始关闭
    private static int startInt = 0;


    private static final Logger logger = LoggerFactory.getLogger(new LoggHelper().toString());

    private static void handle(ActionEvent event) {
        startInt = 0;
    }


    public void start(Stage primaryStage) {
        primary = primaryStage;
        //顶级
        AnchorPane root = new AnchorPane();
        //定义截图后的图片
        iv = new ImageView();
        iv.setFitWidth(400);
        iv.setPreserveRatio(true);
        root.getChildren().add(iv);
        AnchorPane.setTopAnchor(iv, 100.0);
        AnchorPane.setLeftAnchor(iv, 30.0);

        Scene scene = new Scene(root);
        primaryStage.setTitle("下划线君的OCR翻译机 v0.1");
        primaryStage.setScene(scene);
        primaryStage.setHeight(500);
        primaryStage.setWidth(500);
        primaryStage.show();

        //按钮
        Button button = new Button("选取翻译位置");
        root.getChildren().add(button);
        AnchorPane.setTopAnchor(button, 25.0);
        AnchorPane.setLeftAnchor(button, 25.0);

        //测试按钮
        Button buttonTest = new Button("翻译测试");
        root.getChildren().add(buttonTest);
        AnchorPane.setTopAnchor(buttonTest, 25.0);
        AnchorPane.setRightAnchor(buttonTest, 100.0);
        buttonTest.setOnAction(event -> showOcrTxt());

        //管理选框按钮
        Button buttonMarquee = new Button("翻译位置管理");
        root.getChildren().add(buttonMarquee);
        AnchorPane.setTopAnchor(buttonMarquee, 25.0);
        AnchorPane.setLeftAnchor(buttonMarquee, 150.0);
        buttonMarquee.setOnAction(event -> listController.showMainMarquee());


        //定义按钮事件
        button.setOnAction(event -> choose());
        KeyCombination keyCombination = KeyCombination.valueOf("alt+a");
        //定义快捷键
        Mnemonic mnemonic = new Mnemonic(button, keyCombination);
        scene.addMnemonic(mnemonic);
    }


    /**
     * 截图功能
     */
    public void choose() {
        //最小化主窗口
        primary.setIconified(true);

        //截图窗口
        AnchorPane an = new AnchorPane();
        //设置主节点半透明
        an.setStyle("-fx-background-color: #00000050");
        //节点
        Scene scene = new Scene(an);
        //节点无颜色全透明
        scene.setFill(Paint.valueOf("#ffffff00"));

        stage = new Stage();
        stage.setFullScreenExitHint("");
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();
        //选取框
        drag(an);


        //esc退出
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                stage.close();
                primary.setIconified(false);
            }
        });
    }

    /**
     * 截图选择框
     *
     * @param an
     */
    public void drag(AnchorPane an) {
        an.setOnMousePressed(event -> {
            //清空之前的选择框
            an.getChildren().clear();
            //创建选择框
            view = new HBox();
            view.setBackground(null);
            view.setBorder(new Border(new BorderStroke(Paint.valueOf("#FB7299"), BorderStrokeStyle.SOLID, null, new BorderWidths(2))));
            an.getChildren().add(view);

            sceneX_start = event.getSceneX();
            sceneY_start = event.getSceneY();

            AnchorPane.setLeftAnchor(view, sceneX_start);
            AnchorPane.setTopAnchor(view, sceneY_start);
        });

        //设置拖拽
        an.setOnDragDetected(event -> an.startFullDrag());
        an.setOnMouseDragOver(event -> {
            //宽高标题
            Label label = new Label();
            label.setAlignment(Pos.CENTER);
            label.setPrefWidth(160);
            label.setPrefHeight(15);
            an.getChildren().add(label);
            AnchorPane.setLeftAnchor(label, sceneX_start);
            AnchorPane.setTopAnchor(label, sceneY_start - label.getPrefHeight());
            label.setTextFill(Paint.valueOf("#ffffff"));
            label.setStyle("-fx-background-color: #FB7299");

            double sceneX = event.getSceneX();
            double sceneY = event.getSceneY();
//            System.out.println(sceneX+"\n"+sceneY);
            double width = sceneX - sceneX_start;
            double height = sceneY - sceneY_start;
            view.setPrefWidth(width);
            view.setPrefHeight(height);

            label.setText("宽度: " + width + " 高度: " + height);
        });
        an.setOnMouseDragExited(event -> {
            sceneX_End = event.getSceneX();
            sceney_End = event.getSceneY();

            Button button = new Button("ok");
            view.getChildren().add(button);
            view.setAlignment(Pos.BOTTOM_RIGHT);
            //点击截图
            button.setOnAction(e -> {
//                getScreenImg();
                //创建选框对象保存
                getSceneDaoImg();
            });
        });

    }

    /**
     * 保存选择框
     */
    private void getSceneDaoImg() {
        /**
         * 获取截图
         */
        stage.close();
        int w = (int) (sceneX_End - sceneX_start);
        int h = (int) (sceney_End - sceneY_start);

        try {
            Robot robot = new Robot();
            Rectangle rectangle = new Rectangle((int) sceneX_start, (int) sceneY_start, w, h);
            //获取到截图
            bufferedImage = robot.createScreenCapture(rectangle);
            WritableImage writableImage = SwingFXUtils.toFXImage(bufferedImage, null);
            iv.setImage(writableImage);
            primary.setIconified(false);

            //复制到剪切板
            Clipboard cp = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putImage(writableImage);
            cp.setContent(content);

            SceneDao sceneDao = new SceneDao();
            sceneDao.setSceneX_start(sceneX_start);
            sceneDao.setSceneY_start(sceneY_start);
            sceneDao.setSceneX_End(sceneX_End);
            sceneDao.setSceney_End(sceney_End);
            sceneDao.setName(UUID.randomUUID().toString());
            sceneDaos.add(sceneDao);
            logger.info("当前拥有选择框对象:\n" + sceneDaos.toString());

        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    /**
     * 截图并保存到文件
     *
     * @param
     */
    private void getScreenImg() {

        sceneDaos.forEach(sceneDao -> {
            double sceneX_start = sceneDao.getSceneX_start();
            double sceneY_start = sceneDao.getSceneY_start();
            double sceneX_End = sceneDao.getSceneX_End();
            double sceney_End = sceneDao.getSceney_End();
            if (stage != null && sceneX_start != 0.0d && sceneY_start != 0.0d) {
                int w = (int) (sceneX_End - sceneX_start);
                int h = (int) (sceney_End - sceneY_start);

                try {
                    Robot robot = new Robot();
                    Rectangle rectangle = new Rectangle((int) sceneX_start, (int) sceneY_start, w, h);
                    //获取到截图
                    bufferedImage = robot.createScreenCapture(rectangle);
                    WritableImage writableImage = SwingFXUtils.toFXImage(bufferedImage, null);
                    iv.setImage(writableImage);


                    //复制到剪切板
//                    Clipboard cp = Clipboard.getSystemClipboard();
//                    ClipboardContent content = new ClipboardContent();
//                    content.putImage(writableImage);
//                    cp.setContent(content);


                    //写出到文件
                    if (!out.exists()) {
                        out.mkdirs();
                    }


                    File file = new File("out/" + sceneDao.getName() + ".png");
                    logger.info("写出的图片文件路径: " + file.getCanonicalPath());
//                ImageIO.write(bufferedImage, "png", out);

                    ImagePicture imagePicture = new ImagePicture();
                    //把图像二值后输出
                    imagePicture.getImagePicture(bufferedImage, file);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                logger.info("没有打开过截图");
            }
        });

    }


    /**
     * 翻译测试按钮界面
     */
    private void showOcrTxt() {

        Scene scene = null;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader();
            URL url = fxmlLoader.getClassLoader().getResource("view/ORCMenu.fxml");
            fxmlLoader.setLocation(url);
            scene = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }


        Button ocrStart = (Button) scene.lookup("#ocrStart");
        Button ocrStop = (Button) scene.lookup("#ocrStop");
        MenuBar menuBar = (MenuBar) scene.lookup("#ocrTxtOption");
        Menu menu = new Menu("选项");
        Menu menu1 = new Menu("关于");
        MenuItem menuItem = new MenuItem("识别设置");
        MenuItem menuItem1 = new MenuItem("作者qq782221265");
        menu.getItems().add(menuItem);
        menu1.getItems().add(menuItem1);
        menuBar.getMenus().add(menu);
        menuBar.getMenus().add(menu1);
        menuItem.setOnAction(event -> {
            ocrTxtController.showOcrTxtBox();
        });


        ocrStop.setOnAction(MainController::handle);
        Scene finalScene = scene;
        Executor executor = taskExecutePool.myTaskAsyncPool();
        ocrStart.setOnAction(event -> {
            executor.execute(() -> {
                if (sceneDaos.size() > 0) {
                    logger.info("开始整个识别流程!识别间隔 :" + threadSleep);
                    startInt = 1;
                    while (startInt == 1) {
                        timingGetScreenImg(finalScene);
                        try {
                            Thread.sleep(threadSleep);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                logger.info("识别已停止!");
            });

        });


        ocrTxt = new Stage();
        ocrTxt.setTitle("翻译测试");
        ocrTxt.setScene(scene);
        ocrTxt.show();


    }


    /**
     * 开始截图翻译
     */
    public void timingGetScreenImg(Scene scene) {
        if (sceneDaos.size() > 0) {


            //每两秒截图
            //这里应该开启多线程去维护更新
            //重复截图
            getScreenImg();
            //获取识别后的文字
            orcIdentify();

            TextArea txt_original = (TextArea) scene.lookup("#ocr1");
            TextArea txt_origina2 = (TextArea) scene.lookup("#ocr2");

            StringBuilder txt1 = new StringBuilder();


            //获得翻译文本
            sceneDaos.forEach(sceneDao -> {
                txt1.append(sceneDao.getOriginal());
                txt1.append(" \n");
            });

            if (!"error".equals(txt1) && txt1 != null) {
                List<String> baiduTxt = getBaiduApi(new String(txt1));
                logger.info("百度翻译 : " + baiduTxt);
                String join = StringUtils.join(Arrays.asList(baiduTxt.toArray()), "\n").replace("12;", "");

                txt_origina2.setText(join);
            }
            //重新复制给文本
            txt_original.setText(new String(txt1));
        } else {
            logger.info("没有选框对象");
        }
    }

    @Value("${APP_ID}")
    private String APP_ID;
    @Value("${SECURITY_KEY}")
    private String SECURITY_KEY;

    /**
     * 获取翻译后的文字
     *
     * @return
     */
    private List<String> getBaiduApi(String query) {
        TransApi api = new TransApi(APP_ID, SECURITY_KEY);

//        String query = "生活の文化を笑颜であるし、何もない滞纳私たちは、常にする必要があるので苦い。";
        String transResult = api.getTransResult(query, "jp", "zh");

        FanyiBaiduDao fyBaidu = JSONObject.parseObject(transResult, FanyiBaiduDao.class);
        List<FanyiBaiduTxtDao> trans_result = fyBaidu.getTrans_result();
        List<String> txt = new ArrayList<>();

        trans_result.forEach(v -> txt.add(v.getDst()));
        return txt;
    }

    /**
     * ocr识别方法
     *
     * @return
     */
    public void orcIdentify() {
        final CountDownLatch latch = new CountDownLatch(sceneDaos.size());
        Executor executor = taskExecutePool.myTaskAsyncPool();
        //判断是否执行了之前的截图方法,并且截图文件保存了
        sceneDaos.forEach(sceneDao -> {
            //开启多线程
            executor.execute(() -> {
                logger.info("OCR识别开始");
                Date stardata = new Date();
                Runtime runtime = Runtime.getRuntime();
                StringBuilder sumtxt = new StringBuilder();

                try {

                    Process md_test = runtime.exec("tesseract " + sceneDao.getName() + ".png  " + sceneDao.getName() + " -l jpn+osd", null, out);
                    InputStream inputStream = md_test.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    String s;

                    while ((s = reader.readLine()) != null) {
                        logger.info("cmd返回信息: " + s);
                    }

                    //读取识别后的文本
                    File txtFile = new File("out/" + sceneDao.getName() + ".txt");
                    BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(txtFile), "UTF-8"));
                    String txt;
                    while ((txt = bf.readLine()) != null) {
                        sumtxt.append(txt);
                    }
                    bf.close();
                    long sum = new Date().getTime() - stardata.getTime();
                    logger.info("完成识别 : \n" + sumtxt + "\n共耗时 : " + sum);
                    String replace = new String(sumtxt).replace(" ", "");
                    sceneDao.setOriginal(replace);
                    latch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }

//        ITesseract instance;
//        instance = new Tesseract();
//        //开始识别
////        logger.info("doOCR on a jpg image");
//        File file = new File("");
//        try {
//            System.out.println("当前运行目录 :+\n"+file.getCanonicalPath());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
////        instance.setDatapath("/tessdata");
//        instance.setLanguage("jpn");
//        String result = "没有获取到游戏文本";
//        try {
//            if (bufferedImage != null) {
//                result = instance.doOCR(bufferedImage);
//            }
//        } catch (TesseractException e) {
//            e.printStackTrace();
//        }

            });

        });
        try {
            latch.await();
            logger.info("全部识别完成,继续运行");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}


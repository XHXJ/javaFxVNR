package com.xhxj.ocr.controller;

import com.alibaba.fastjson.JSONObject;
import com.xhxj.ocr.ShowTxtTaskExecutePool;
import com.xhxj.ocr.TaskExecutePool;
import com.xhxj.ocr.dao.FanyiBaiduDao;
import com.xhxj.ocr.dao.FanyiBaiduTxtDao;
import com.xhxj.ocr.dao.SceneDao;
import com.xhxj.ocr.tool.ImagePicture;
import com.xhxj.ocr.tool.baidu.TransApi;
import de.felixroske.jfxsupport.GUIState;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
import net.sourceforge.tess4j.*;
import net.sourceforge.tess4j.util.ImageHelper;
import net.sourceforge.tess4j.util.LoggHelper;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
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

import static net.sourceforge.tess4j.ITessAPI.TessParagraphJustification.JUSTIFICATION_LEFT;


/**
 * @description:
 * @author: zdthm2010@gmail.com
 * @date: 2019-08-15 19:34
 */
@Controller
public class MainController {
    @Autowired
    TaskExecutePool taskExecutePool;
    @Autowired
    ShowTxtTaskExecutePool showTxtTaskExecutePool;
    @Autowired
    ListController listController;
    @Autowired
    ShowOptionController showOptionController;
    @Autowired
    ShowTxtController showTxtController;


    //文字存取图片
    @FXML
    private ImageView iv;
    @FXML
    private ImageView iv2;

    BufferedImage bufferedImage;

    Stage primary;
    Stage stage;
    Stage ocrTxt;

    HBox view;

    //baiduapi
    @Value("${APP_ID}")
    private String APP_ID;
    @Value("${SECURITY_KEY}")
    private String SECURITY_KEY;


    //截图坐标
    double sceneX_start;
    double sceneY_start;
    double sceneX_End;
    double sceney_End;

    //所有的选择框
    public static List<SceneDao> sceneDaos = new ArrayList<>();

    //识别间隔
    public static long threadSleep = 0;
    //二值化阀值
    public static double grayLeve = 170;
    //启用二值化输出
    public static Boolean colorBoolean = true;
    //tessdata路径设置
    public static String tessdataPath = "C:\\Program Files (x86)\\Tesseract-OCR\\tessdata";
    //翻译语言设置
    public static String ocrLanguage = "jpn";

    //保存所有翻译过的原文
    static HashMap<String, Date> allTxtMap = new HashMap<>();

    //开始关闭
    private static boolean startInt = true;
    //是否开启二值化处理

    //给与是否要更新的旗帜
    public static boolean flag = true;
    public static Object lock = new Object();
    public static Object lock2 = new Object();


    private static final Logger logger = LoggerFactory.getLogger(new LoggHelper().toString());

    /**
     * 关闭识别线程
     */
    private static void ocrStop() {
        startInt = false;
    }


    //开始
    @FXML
    private Button mainStart;
    @FXML
    private Button mainStop;
    @FXML
    private MenuItem showOptionMenuItem;
    @FXML
    private MenuItem showOcrTxtMenu;
    @FXML
    private Button getSceneDaoImgButton;
    @FXML
    private Button showMainMarqueeButton;
    @FXML
    private Button startShowTextButton;
    @FXML
    private Button offShowTextButton;

    @Value("${version}")
    private String version;

    @FXML
    public void initialize() {

        primary = GUIState.getStage();
        primary.setTitle("下划线君的OCR翻译机 v" + version);
        Executor executor = taskExecutePool.myTaskAsyncPool();
        //开始识别
        mainStart.setOnAction(event -> executor.execute(() -> {
            if (sceneDaos.size() > 0) {
                logger.info("开始整个识别流程!识别间隔 :" + threadSleep);
                startInt = true;
                while (startInt) {
                    timingGetScreenImg(null);
                    try {
                        Thread.sleep(threadSleep);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            logger.info("识别已停止!");
        }));
        //停止
        mainStop.setOnAction(event -> ocrStop());
        //打开选项
        showOptionMenuItem.setOnAction(event -> showOptionController.showOption());
        //测试
        showOcrTxtMenu.setOnAction(event -> showOcrTxt());
        //选取翻译位置
        getSceneDaoImgButton.setOnAction(event -> choose());
        //管理选择框
        showMainMarqueeButton.setOnAction(event -> listController.showMainMarquee());
        //显示文本框
        startShowTextButton.setOnAction(event -> showTxtController.startShowText());
        //关闭文本框
        offShowTextButton.setOnAction(event -> showTxtController.offShowText());
        executor.execute(() -> {
            while (true) {
                try {
                    //每一秒去切换主界面的图片
                    Thread.sleep(1000);
                    if (sceneDaos.size() > 0 && sceneDaos.get(0).getImage() != null) {
                        WritableImage writableImage = SwingFXUtils.toFXImage(sceneDaos.get(0).getImage(), null);
                        iv2.setImage(writableImage);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }
        });

    }


    public void start(Stage primaryStage) {

        BorderPane anchorPane = null;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader();
            URL url = fxmlLoader.getClassLoader().getResource("view/Main.fxml");
            fxmlLoader.setLocation(url);
            anchorPane = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }


        Scene scene = new Scene(anchorPane);
        primaryStage.setTitle("下划线君的OCR翻译机 v0.4");
        primaryStage.setScene(scene);
        primaryStage.show();


//
//        primary = primaryStage;
//        //顶级
//        AnchorPane root = new AnchorPane();
//        //定义截图后的图片
//        iv = new ImageView();
//        iv.setFitWidth(400);
//        iv.setPreserveRatio(true);
//        iv2 = new ImageView();
//        iv2.setFitWidth(400);
//        iv2.setPreserveRatio(true);
//
//        root.getChildren().add(iv);
//        root.getChildren().add(iv2);
//        AnchorPane.setTopAnchor(iv, 100.0);
//        AnchorPane.setTopAnchor(iv2, 300.0);
//        AnchorPane.setLeftAnchor(iv, 30.0);
//        AnchorPane.setLeftAnchor(iv2, 30.0);
//
//        Scene scene = new Scene(root);
//        primaryStage.setTitle("下划线君的OCR翻译机 v0.4");
//        primaryStage.setScene(scene);
//        primaryStage.setHeight(500);
//        primaryStage.setWidth(500);
//        primaryStage.show();
//
//        //按钮
//        Button button = new Button("选取翻译位置");
//        root.getChildren().add(button);
//        AnchorPane.setTopAnchor(button, 25.0);
//        AnchorPane.setLeftAnchor(button, 25.0);
//
//        //测试按钮
//        Button buttonTest = new Button("翻译测试");
//        root.getChildren().add(buttonTest);
//        AnchorPane.setTopAnchor(buttonTest, 25.0);
//        AnchorPane.setRightAnchor(buttonTest, 80.0);
//        buttonTest.setOnAction(event -> showOcrTxt());
//
//        //翻译位置管理按钮
//        Button buttonMarquee = new Button("翻译位置管理");
//        root.getChildren().add(buttonMarquee);
//        AnchorPane.setTopAnchor(buttonMarquee, 25.0);
//        AnchorPane.setLeftAnchor(buttonMarquee, 120.0);
//        buttonMarquee.setOnAction(event -> listController.showMainMarquee());
//        //显示文本框
//        Button showTxt = new Button("显示文本框");
//        root.getChildren().add(showTxt);
//        AnchorPane.setTopAnchor(showTxt, 25.0);
//        AnchorPane.setLeftAnchor(showTxt, 250.0);
//        showTxt.setOnAction(event -> showTxtController.startShowText());
//        //关闭所有文本
//        Button offShowTex = new Button("关闭文本框");
//        root.getChildren().add(offShowTex);
//        AnchorPane.setTopAnchor(offShowTex, 50.0);
//        AnchorPane.setLeftAnchor(offShowTex, 250.0);
//        offShowTex.setOnAction(event -> showTxtController.offShowTxt());
//
//
//        //定义按钮事件
//        button.setOnAction(event -> choose());
//        KeyCombination keyCombination = KeyCombination.valueOf("alt+a");
//        //定义快捷键
//        Mnemonic mnemonic = new Mnemonic(button, keyCombination);
//        scene.addMnemonic(mnemonic);
//
//        Executor executor = taskExecutePool.myTaskAsyncPool();

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
//            sceneDao.setName(UUID.randomUUID().toString());
            sceneDaos.add(sceneDao);
            logger.info("当前拥有选择框对象:\n" + sceneDaos.toString());

        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    /**
     * 截图
     *
     * @param
     */
    public void getScreenImg() {
        Executor executor = taskExecutePool.myTaskAsyncPool();
        //文本窗口最小化

//        Task<Integer> task = new Task<Integer>() {
//            @Override
//            protected void updateValue(Integer value) {
//                synchronized (lock2) {
//                    super.updateValue(value);
//                    logger.info("最小化文本窗口");
//                    ShowTxtController.stageList.forEach(s ->
//                                    s.setIconified(true)
////                            s.close()
//                    );
//                    lock2.notifyAll();
//                }
//            }
//
//            @Override
//            protected Integer call() throws Exception {
//                return null;
//            }
//        };
//        executor.execute(task);
//        synchronized (lock2) {
//            try {
//                lock2.wait();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
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
                    BufferedImage screenCapture = robot.createScreenCapture(rectangle);
                    //把截图保存到对象
                    sceneDao.setImage(screenCapture);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                logger.info("没有打开过截图");
            }
        });
//        }
//        Task<Integer> task2 = new Task<Integer>() {
//            @Override
//            protected void updateValue(Integer value) {
//                super.updateValue(value);
//                logger.info("最大化文本窗口");
//                ShowTxtController.stageList.forEach(s ->
//                                s.setIconified(false)
////                        s.show()
//                );
//
//            }
//
//            @Override
//            protected Integer call() throws Exception {
//                return null;
//            }
//        };
//        //文本窗口最大化
//        executor.execute(task2);
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
        menuItem.setOnAction(event -> showOptionController.showOption());
        ocrStop.setOnAction(event -> ocrStop());
        Scene finalScene = scene;
        Executor executor = taskExecutePool.myTaskAsyncPool();
        ocrStart.setOnAction(event -> {
            executor.execute(() -> {
                if (sceneDaos.size() > 0) {
                    logger.info("开始整个识别流程!识别间隔 :" + threadSleep);
                    startInt = true;
                    while (startInt) {
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
     * 开始截图翻译流程
     *
     * @param scene null 表示不用赋值给界面文本档
     */
    public void timingGetScreenImg(Scene scene) {
        if (sceneDaos.size() > 0) {


            //每两秒截图
            //这里应该开启多线程去维护更新
            //把sceneDaos中的对象全部拿去截图
            getScreenImg();
            //把sceneDaos中的对象全部识别
            orcIdentify();

            StringBuilder txt1 = new StringBuilder();

            //获得翻译文本
            sceneDaos.forEach(sceneDao -> {
                txt1.append(sceneDao.getName() + "\"IcDc\"  ");
                txt1.append(sceneDao.getOriginal());
                txt1.append("\n");
            });
            String allTxt = new String(txt1);

            //判断文本是否之前就存在注意控制数量

            //赋值给sceneDaos译文这里需要注意根据#id来区分翻译选择框,进入百度api之后如果识别为乱码#id可能不会返回!
            if (!allTxtMap.containsKey(allTxt)) {

                List<String> baiduTxt = getBaiduApi(allTxt);
                logger.info("百度翻译 : " + baiduTxt);

                synchronized (lock) {
                    baiduTxt.forEach(s -> {
                        String[] split = s.split("“ICDC”");
                        sceneDaos.forEach(sceneDao -> {
                            if (sceneDao.getName().equals(split[0]) && split.length > 1) {
                                sceneDao.setTranslation(split[1]);
                            }
                        });
                    });
                    //去通知更新showTxt显示的文本框
                    lock.notifyAll();
                    logger.info("发出通知要求更新文本框");
//                    flag =true;
                }


                if (scene != null) {
                    TextArea txt_original = (TextArea) scene.lookup("#ocr1");
                    TextArea txt_origina2 = (TextArea) scene.lookup("#ocr2");

                    String join = StringUtils.join(Arrays.asList(baiduTxt.toArray()), "\n\n");
                    //重新复制给文本
                    txt_origina2.setText(join);
                    txt_original.setText(allTxt);
                }


            } else {
                //没有也要更新
                logger.info("baiduApi没有调用,已翻译过");
                //超过自动清空
                if (allTxtMap.size() > 200) {
                    logger.info("已清空翻译过的原文_allTxtMap");
                    allTxtMap.clear();
                }
            }

            allTxtMap.put(allTxt, new Date());
        } else {
            logger.info("没有选框对象");
        }
    }


    /**
     * 获取翻译后的文字
     *
     * @return 翻译后的文字
     */
    private List<String> getBaiduApi(String query) {
        TransApi api = new TransApi(APP_ID, SECURITY_KEY);

//        String query = "生活の文化を笑颜であるし、何もない滞纳私たちは、常にする必要があるので苦い。";
        String transResult = api.getTransResult(query, "auto", "zh");
        logger.info("baiduApi message :" + transResult);
        FanyiBaiduDao fyBaidu = JSONObject.parseObject(transResult, FanyiBaiduDao.class);
        List<FanyiBaiduTxtDao> trans_result = fyBaidu.getTrans_result();
        List<String> txt = new ArrayList<>();
        if (trans_result != null) {
            trans_result.forEach(v -> txt.add(v.getDst()));
        }
        return txt;
    }

    ITesseract instance;

    /**
     * ocr识别方法
     */
    private void orcIdentify() {
        final CountDownLatch latch = new CountDownLatch(sceneDaos.size());
        Executor executor = taskExecutePool.myTaskAsyncPool();
        //判断是否执行了之前的截图方法,并且截图文件保存了
        sceneDaos.forEach(sceneDao -> {
            //开启多线程
            executor.execute(() -> {
                BufferedImage image = sceneDao.getImage();
//                Runtime runtime = Runtime.getRuntime();
//                StringBuilder sumtxt = new StringBuilder();
                logger.info("OCR识别开始");
                Date stardata = new Date();
                try {
                    if (colorBoolean) {
                        logger.info("启用二值化输出");
                        //把图像二值后输出
                        ImagePicture imagePicture = new ImagePicture();
//                        BufferedImage image = sceneDao.getImage();
//                        image = ImageHelper.getScaledInstance(image, (int) (image.getWidth() * 1.5), (int) (image.getHeight() * 1.5));
                        image = imagePicture.cleanLinesInImage(image);
                        sceneDao.setImage(image);
                    }


//                    String s = iTesseract.doOCR(sceneDao.getImage());
                    StringBuilder s = new StringBuilder();
                    instance = new Tesseract();
                    instance.setDatapath(tessdataPath);
                    instance.setLanguage(ocrLanguage);
//                    instance.setPageSegMode(ITessAPI.TessPageSegMode.PSM_AUTO_OSD);
                    List<Word> words = instance.getWords(image, TessAPI.TessPageIteratorLevel.RIL_PARA);
                    words.forEach(word -> {
                        if (word.getConfidence() > 70) {
                            s.append(word.getText());
                        }
                    });

                    long sum = new Date().getTime() - stardata.getTime();
                    logger.info("完成识别 : \n" + s + "\n共耗时 : " + sum);


//                    //读取识别后的文本
//                    File txtFile = new File("out/" + sceneDao.getName() + ".txt");
//                    BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(txtFile), "UTF-8"));
//                    String txt;
//                    while ((txt = bf.readLine()) != null) {
//                        sumtxt.append(txt);
//                    }
//                    bf.close();

                    String replace = new String(s).replace(" ", "").replace("\n", "");
                    sceneDao.setOriginal(replace);
                    latch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
        try {
            latch.await();
            logger.info("全部识别完成,继续运行");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断是否为纯色
     *
     * @param bu      图片源
     * @param percent 纯色百分比，即大于此百分比为同一种颜色则判定为纯色,范围[0-1]
     * @return
     * @throws IOException
     */
    public boolean isSimpleColorImg(BufferedImage bu, float percent) throws IOException {
        int height = bu.getHeight();
        int width = bu.getWidth();
        int count = 0, pixTemp = 0, pixel = 0;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                pixel = bu.getRGB(i, j);
                if (pixel == pixTemp) //如果上一个像素点和这个像素点颜色一样的话，就判定为同一种颜色
                    count++;
                else
                    count = 0;
                if ((float) count / (height * width) >= percent) //如果连续相同的像素点大于设定的百分比的话，就判定为是纯色的图片
                    return true;
                pixTemp = pixel;
            }
        }
        return false;
    }

}
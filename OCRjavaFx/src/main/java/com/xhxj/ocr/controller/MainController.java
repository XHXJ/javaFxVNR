package com.xhxj.ocr.controller;

import com.alibaba.fastjson.JSONObject;
import com.xhxj.ocr.*;
import com.xhxj.ocr.View.ListStageView;
import com.xhxj.ocr.dao.FanyiBaiduDao;
import com.xhxj.ocr.dao.FanyiBaiduTxtDao;
import com.xhxj.ocr.dao.SceneDao;
import com.xhxj.ocr.tool.baidu.TransApi;
import com.xhxj.ocr.tool.imageUtil.BinaryTest;
import de.felixroske.jfxsupport.FXMLController;
import de.felixroske.jfxsupport.GUIState;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import marvin.MarvinDefinitions;
import marvin.image.MarvinImage;
import marvin.image.MarvinSegment;
import net.sourceforge.tess4j.*;
import net.sourceforge.tess4j.util.LoggHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

import static com.xhxj.ocr.SysConfig.*;
import static marvin.MarvinPluginCollection.findTextRegions;

/**
 * @description:
 * @author: zdthm2010@gmail.com
 * @date: 2019-08-15 19:34
 */
@FXMLController
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
    //文字存取图片
    @FXML
    private ImageView iv;
    @FXML
    private ImageView iv2;

    @Value("${version}")
    private String version;

    @FXML
    public void initialize() {
        logger.info("下划线君的OCR翻译机 v" + version);
        //读取配置文件
        new SysConfig().readSysConfig();
        primary = GUIState.getStage();
        primary.setTitle("下划线君的OCR翻译机 v" + version);
        Executor executor = taskExecutePool.myTaskAsyncPool();

        //识别任务
        class MyOcrThread extends ScheduledService<Boolean> {
            @Override
            protected Task<Boolean> createTask() {
                return new Task<Boolean>() {
                    @Override
                    protected Boolean call() throws Exception {
                        timingGetScreenImg(null);
                        return null;
                    }
                };
            }
        }
        MyOcrThread myOcrThread = new MyOcrThread();
        mainStart.setOnAction(event -> {
            myOcrThread.setPeriod(Duration.millis(threadSleep));
            myOcrThread.setExecutor(executor);
            myOcrThread.restart();
            primary.setTitle("下划线君的OCR翻译机 v" + version + " 状态:识别中...");
            logger.info("开始整个识别流程!识别间隔 :" + threadSleep);
        });
        mainStop.setOnAction(event -> {
            myOcrThread.cancel();
            primary.setTitle("下划线君的OCR翻译机 v" + version);
            logger.info("识别停止!");
        });

        //打开选项
        showOptionMenuItem.setOnAction(event -> showOptionController.showOption());
        //测试
        showOcrTxtMenu.setOnAction(event -> showOcrTxt());
        //选取翻译位置
        getSceneDaoImgButton.setOnAction(event -> choose());
        //管理选择框
        showMainMarqueeButton.setOnAction(event -> OcrApplication.showView(ListStageView.class, Modality.NONE));
        //显示文本框
        startShowTextButton.setOnAction(event -> showTxtController.startShowText());
        //关闭文本框
        offShowTextButton.setOnAction(event -> showTxtController.offShowText());

        class CyclicGraph extends ScheduledService<Number> {
            @Override
            protected Task<Number> createTask() {
                return new Task<Number>() {
                    @Override
                    protected Number call() throws Exception {
                        if (sceneDaos.size() > 0 && sceneDaos.get(0).getOutImage() != null) {
                            WritableImage writableImage = SwingFXUtils.toFXImage(sceneDaos.get(0).getOutImage(), null);
                            iv2.setImage(writableImage);
                        }
                        return null;
                    }
                };
            }
        }
        CyclicGraph cyclicGraph = new CyclicGraph();

        //启用图片更换线程
        cyclicGraph.setDelay(Duration.seconds(1));
        cyclicGraph.setPeriod(Duration.millis(1000));
        cyclicGraph.setExecutor(executor);
        cyclicGraph.start();
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
                //更新翻译管理
                ObservableList<String> objects = FXCollections.observableArrayList();
                MainController.sceneDaos.forEach(sceneDao -> objects.add(sceneDao.getName()));
                if (listController.listView != null) {
                    listController.listView.setItems(objects);
                }
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
//            Clipboard cp = Clipboard.getSystemClipboard();
//            ClipboardContent content = new ClipboardContent();
//            content.putImage(writableImage);
//            cp.setContent(content);

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
            orcStart();

            StringBuilder txt1 = new StringBuilder();

            //获得翻译文本
            sceneDaos.forEach(sceneDao -> {
                txt1.append(sceneDao.getOriginal());
                txt1.append("\n");
//                txt1.append("\n");
            });
            String allTxt = new String(txt1);

            //判断文本是否之前就存在注意控制数量

            //赋值给sceneDaos译文这里需要注意根据#id来区分翻译选择框,进入百度api之后如果识别为乱码#id可能不会返回!
            if (!allTxtMap.containsKey(allTxt)) {

                List<String> baiduTxt = getBaiduApi(allTxt);

                logger.info("百度翻译 : " + baiduTxt);

                synchronized (lock) {
                    if (baiduTxt.size() > 0) {
                        //顺序取出赋值
                        for (int i = 0; i < sceneDaos.size(); i++) {
                            sceneDaos.get(i).setTranslation(baiduTxt.get(i));
                        }
                    }
//                    baiduTxt.forEach(s -> {
//                        String[] split = s.split("\n");
//                        sceneDaos.forEach(sceneDao -> {
//                            if (sceneDao.getName().equals(split[0]) && split.length > 1) {
//                                sceneDao.setTranslation(split[1]);
//                            }
//                        });
//                    });

                    //去通知更新showTxt显示的文本框
                    lock.notifyAll();
                    logger.info("发出通知要求更新文本框");
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

    /**
     * ocr识别方法
     */
    public void orcStart() {
        final CountDownLatch latch = new CountDownLatch(sceneDaos.size());
        Executor executor = taskExecutePool.myTaskAsyncPool();
        //判断是否执行了之前的截图方法,并且截图文件保存了
        sceneDaos.forEach(sceneDao -> {
            //开启多线程
            executor.execute(() -> {
                //选择区域保存的截图
                BufferedImage image = sceneDao.getImage();

                //所有识别后的文字
                String ocr;
                //清空所有之前识别的outImageAll
                sceneDao.getOutImageAll().clear();

                try {
                    //识别的时候先去识别文本框,如果有文本框就分别识别文本框
                    //开启文本框识别
                    Map<Integer, BufferedImage> allBufferedImage = new HashMap<>();
                    if (sceneDao.isOcrModelText()) {
                        //设置marvin路径
                        MarvinDefinitions.setImagePluginPath(new File("marvin/plugins/image").getAbsolutePath() + "/");
                        MarvinImage marvinImage = new MarvinImage(image);
                        //识别到的文本框
                        List<MarvinSegment> segments = findTextRegions(marvinImage, sceneDao.getMaxWhiteSpace(), sceneDao.getMaxFontLineWidth(), sceneDao.getMinTextWidth(), sceneDao.getGrayScaleThreshold());
//                        按照顺序给文本框截图并put到map
                        for (int i = 0; i < segments.size(); i++) {
                            MarvinSegment s = segments.get(i);
                            if (s.height >= 5) {
                                s.y1 -= 5;
                                s.y2 += 5;
                                if (s.y1 >= 0 && (s.x2 - s.x1) > 0 && (s.y2 - s.y1) > 0) {
                                    try {
                                        marvinImage.drawRect(s.x1 - 2, s.y1 - 5, s.x2 - s.x1, s.y2 - s.y1 +5, Color.red);
//                                        marvinImage.drawRect(s.x1 - 2, s.y1 - 2, (s.x2 - s.x1) - 2, (s.y2 - s.y1) - 2, Color.red);
                                        //分为大小文本截图
                                        BufferedImage subimage = image.getSubimage(s.x1 -2 , s.y1 -5, s.x2 - s.x1, s.y2 - s.y1+5);
                                        allBufferedImage.put(i, subimage);
                                    } catch (Exception e) {
                                        //这是一个因为坐标没有正确获取报的错,可能是位置被减到了负数...如果对准确性有要求可以处理一下这个bug
                                        //需要看用户设置的参数
                                        logger.warn("orcStart() 标记文本框时报错 :" + e.getMessage());
                                    }
                                }
                            }
                        }
                        //保存处理后的图像
                        BufferedImage newImageInstance = marvinImage.getBufferedImageNoAlpha();
                        sceneDao.setOutImage(newImageInstance);

                        //每一个文本框单独识别
                        //所有识别后的文本map
                        Map<Integer, String> allTxtMap = new TreeMap<>();
                        //多线程翻译每一个识别后的文本框
                        final CountDownLatch latch2 = new CountDownLatch(allBufferedImage.size());
                        allBufferedImage.forEach((k, y) -> {
                            executor.execute(() -> {
                                allTxtMap.put(k, ocr(y, sceneDao));
                                latch2.countDown();
                            });
                        });
                        latch2.await();
                        //按顺序合并翻译后的文本框
                        StringBuilder stringBuilder = new StringBuilder();
                        allTxtMap.forEach((k, y) -> stringBuilder.append(y));
                        ocr = new String(stringBuilder);
                        logger.info("文本框识别 :" + allTxtMap.toString());


                    } else {
                        //正常识别流程开始
                        ocr = ocr(image, sceneDao);

                    }
                    //将识别完成后的文字赋值
                    if (StringUtils.isNotEmpty(ocr)) {
                        sceneDao.setOriginal(ocr);
                    } else {
                        //没有识别出任何文字
                        sceneDao.setOriginal("NULL");
                    }

                    //输出到文件测试
                    if (sceneDao.isOutputFile()) {
                        File file = new File("out/" + sceneDao.getName() + "/separated/");
                        if (!file.exists()) {
                            file.mkdirs();
                        }
                        File file2 = new File("out/" + sceneDao.getName() + "/binarization/");
                        if (!file2.exists()) {
                            file2.mkdirs();
                        }
                        File file3 = new File("out/" + sceneDao.getName() + "/image/");
                        if (!file3.exists()) {
                            file3.mkdirs();
                        }
                        File file4 = new File("out/" + sceneDao.getName() + "/outImage/");
                        if (!file4.exists()) {
                            file4.mkdirs();
                        }
                        //输出分隔后的图片
                        allBufferedImage.forEach((k, y) -> {
                            try {
                                ImageIO.write(y, "png", new File(file.getAbsolutePath() + "/" + k + ".png"));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                        //输出二值化后的图片
                        for (int i = 0; i < sceneDao.getOutImageAll().size(); i++) {
                            BufferedImage bufferedImage = sceneDao.getOutImageAll().get(i);
                            ImageIO.write(bufferedImage, "png", new File(file2.getAbsolutePath() + "/" + i + ".png"));
                        }
                        //输出原图
                        ImageIO.write(sceneDao.getImage(), "png", new File(file3.getAbsolutePath() + "/" + sceneDao.getName() + ".png"));
                        //输出处理后的图
                        ImageIO.write(sceneDao.getOutImage(), "png", new File(file4.getAbsolutePath() + "/" + sceneDao.getName() + ".png"));
                    }


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
     * 文字识别
     *
     * @param image    需要识别的图片
     * @param sceneDao 设置识别后的sceneDao对象
     */
    public String ocr(BufferedImage image, SceneDao sceneDao) {
        //正常识别流程
//        logger.info("OCR识别开始");
        Date stardata = new Date();
        if (sceneDao.isColorBoolean()) {
//            logger.info("启用二值化输出");
            //把图像二值后输出
            try {
                image = BinaryTest.getImagePicture(image, sceneDao.getGrayLeve());
            } catch (Exception e) {
                logger.error("图像二值化时报错 :" + e.toString());
            }
            //将二值化后的图像保存
            sceneDao.getOutImageAll().add(image);
        }
        ITesseract instance = new Tesseract();
        instance.setDatapath(SysConfig.tessdataPath);
        instance.setLanguage(SysConfig.ocrLanguage);
//                    instance.setPageSegMode(ITessAPI.TessPageSegMode.PSM_AUTO_OSD);
        //识别后的文字
        StringBuilder ocrString = new StringBuilder();
        List<Word> words = instance.getWords(image, TessAPI.TessPageIteratorLevel.RIL_PARA);
        words.forEach(word -> {
            if (word.getConfidence() > 70) {
                String text = word.getText();
                //开启去除空格
                if (sceneDao.isRuleOutSpace()) {
                    text = text.replace(" ", "");
                }
                ocrString.append(text);
            }
        });
//                    String replace = new String(orc).replace(" ", "").replace("\n", "");

        long sum = new Date().getTime() - stardata.getTime();
        logger.info("完成识别 : \n" + ocrString + "\n共耗时 : " + sum);

        return new String(ocrString).replace("\n", "");
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
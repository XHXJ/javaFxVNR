package com.xhxj.ocr;

import com.xhxj.ocr.controller.ListController;
import com.xhxj.ocr.controller.MainController;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * @description: 
 * @author: zdthm2010@gmail.com
 * @date: 2019-08-12 21:40
 */
@Configuration
@EnableAutoConfiguration //自动加载配置信息
@ComponentScan("com")//使包路径下带有注解的类可以使用@Autowired自动注入
@EnableAsync
public class OrcApplication extends Application implements CommandLineRunner, Consumer<Stage> {

    /**
     * 窗口启动接口，原理：
     * 1. run(String... args)中给springStartMain赋值
     * 2. start(Stage primaryStage)中调用了springStartMain来操作primaryStage
     * 3. 而springStartMain实际上是spring管理的StartMain一个对象，因此accept方法中可以操作spring管理的任何对象
     */
    private static Consumer<Stage> springStartMain;

    @Override
    public void run(String... args) throws Exception {
        //将springStartMain赋值为spring管理的实例
        springStartMain = Objects.requireNonNull(this);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        //使用spring管理的实例操作primaryStage
        springStartMain.accept(primaryStage);
    }


    @Autowired
	private MainController mainController;
    @Autowired
    private ListController listController;

    @Override
    public void accept(Stage stage) {
			mainController.start(stage);


	}

    public static void main(String[] args) {

        SpringApplicationBuilder builder = new SpringApplicationBuilder(OrcApplication.class);
         builder.headless(false).run(args);

        //启动窗口
        Application.launch(args);
    }
}

package com.xhxj.ocr;

import com.xhxj.ocr.controller.ListController;
import com.xhxj.ocr.controller.MainController;
import de.felixroske.jfxsupport.AbstractJavaFxApplicationSupport;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class OrcApplication  extends AbstractJavaFxApplicationSupport {

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        launch(OrcApplication.class, MainStageView.class, args);
    }
    /**
     * Start.
     *
     * @param stage the stage
     *
     * @exception Exception the exception
     */
    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);
    }
}

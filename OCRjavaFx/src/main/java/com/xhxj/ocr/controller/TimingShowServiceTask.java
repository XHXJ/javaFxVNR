package com.xhxj.ocr.controller;

import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import net.sourceforge.tess4j.util.LoggHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @description:
 * @author: zdthm2010@gmail.com
 * @date: 2019-08-16 00:40
 */
public class TimingShowServiceTask extends ScheduledService<String> {

    private static final Logger logger = LoggerFactory.getLogger(new LoggHelper().toString());
    private String name;

    public void setName(String name) {
        this.name = name;
    }

    @Override
    protected Task createTask() {
        Task<String> task = new Task<String>() {
            private String translation;
            @Override
            protected String call() throws Exception {
                synchronized (MainController.lock){
                    MainController.lock.wait();
                    logger.info("文本更新线程执行");
                    translation = MainController.sceneDaos.parallelStream().filter(sceneDao -> sceneDao.getName().equals(name)).findFirst().get().getTranslation();
                }
                return translation;
            }
        };
        return task;
    }
}
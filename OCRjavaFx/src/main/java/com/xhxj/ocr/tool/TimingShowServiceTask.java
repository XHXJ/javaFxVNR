package com.xhxj.ocr.tool;

import com.xhxj.ocr.controller.MainController;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import net.sourceforge.tess4j.util.LoggHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @description:
 * @author: zdthm2010@gmail.com
 * @date: 2019-08-16 00:40
 */
@Component
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
                    MainController.sceneDaos.forEach(sceneDao -> {
                        if (name.equals(sceneDao.getName())) {
                            translation = sceneDao.getTranslation();
                        }
                    });
                }
                return translation;
            }
        };
        return task;
    }
}

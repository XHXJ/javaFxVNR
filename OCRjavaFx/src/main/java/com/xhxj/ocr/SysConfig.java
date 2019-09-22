package com.xhxj.ocr;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import net.sourceforge.tess4j.util.LoggHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * @description:结合fastjson使用
 * @author: zdthm2010@gmail.com
 * @date: 2019-09-20 23:38
 */
public class SysConfig {

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

    /**
     * fastjson通过get方法才能获取到成员变量,方便之后输出配置文件
     *
     * @return
     */
    @Override
    public String toString() {
        return "SysConfig{" +
                "threadSleep=" + threadSleep +
                ", grayLeve=" + grayLeve +
                ", colorBoolean=" + colorBoolean +
                ", tessdataPath='" + tessdataPath + '\'' +
                ", ocrLanguage='" + ocrLanguage + '\'' +
                '}';
    }

    public long getThreadSleep() {
        return threadSleep;
    }

    public double getGrayLeve() {
        return grayLeve;
    }

    public Boolean getColorBoolean() {
        return colorBoolean;
    }

    public String getTessdataPath() {
        return tessdataPath;
    }

    public String getOcrLanguage() {
        return ocrLanguage;
    }

    public void setThreadSleep(long threadSleep) {
        SysConfig.threadSleep = threadSleep;
    }

    public void setGrayLeve(double grayLeve) {
        SysConfig.grayLeve = grayLeve;
    }

    public void setColorBoolean(Boolean colorBoolean) {
        SysConfig.colorBoolean = colorBoolean;
    }

    public void setTessdataPath(String tessdataPath) {
        SysConfig.tessdataPath = tessdataPath;
    }

    public void setOcrLanguage(String ocrLanguage) {
        SysConfig.ocrLanguage = ocrLanguage;
    }


    private static final Logger logger = LoggerFactory.getLogger(new LoggHelper().toString());

    /**
     * 写出配置文件
     */
    public void outSysConfig() {
        //fastjson基于get方法转换json格式
        String sysConfig = JSON.toJSONString(this);

        File file = new File("config.json");
        try {
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            logger.info("写出配置文件 :" + sysConfig + " 文件路径 :" + file.getAbsolutePath());
            bufferedWriter.write(sysConfig);
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取配置文件
     */
    public void readSysConfig() {
        try {
            File file = new File("config.json");
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(
                        new FileReader(file)
                );
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                reader.close();
                logger.info("读取配置文件 :" + stringBuilder);
                //fastjson本身获取值使用的反射set方法,所以这里不用手动去赋值
                JSONArray.parseObject(stringBuilder.toString(), SysConfig.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
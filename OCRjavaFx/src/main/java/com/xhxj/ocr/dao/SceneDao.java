package com.xhxj.ocr.dao;

import lombok.Data;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 节点对象
 */
@Data
public class SceneDao {
    private static AtomicInteger count = new AtomicInteger();

    {
        name = String.valueOf(count.incrementAndGet());
    }

    private double sceneX_start;
    private double sceneY_start;
    private double sceneX_End;
    private double sceney_End;
    private String name;

    //译文
    private String translation;
    //原文
    private String original;
    //截取的图片
    private BufferedImage Image;
    //文本识别后处理的图片
    private BufferedImage outImage;
    //二值化后的图片集合
    List<BufferedImage> outImageAll = new ArrayList<>();
    //文字识别框设置参数
    private int maxWhiteSpace = 10;
    private int maxFontLineWidth = 35;
    private int minTextWidth = 65;
    private int grayScaleThreshold = 210;

    //二值化阀值
    private double grayLeve = 170;
    //启用二值化输出
    private boolean colorBoolean = true;
    //排除空格
    private boolean ruleOutSpace = true;
    //识别文本框
    private boolean ocrModelText = true;
    //输出到文件夹
    private boolean outputFile = false;
}

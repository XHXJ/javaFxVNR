package com.xhxj.ocr.dao;

import lombok.Data;

import java.awt.image.BufferedImage;


/**
 * 节点对象
 */
@Data
public class SceneDao {
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
}

package com.xhxj.ocr.tool;

import com.xhxj.ocr.controller.MainController;
import net.sourceforge.tess4j.util.LoggHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImagePicture {

    private final Logger logger = LoggerFactory.getLogger(new LoggHelper().toString());


    /**
     * @param bufferedImage   需要去噪的图像
     * @param destdir 去噪后的图像保存地址
     * @throws IOException
     */
    public BufferedImage cleanLinesInImage(BufferedImage bufferedImage) throws IOException {
//        File destF = new File(destDir);
//        if (!destF.exists())
//        {
//            destF.mkdirs();
//        }
//
//        BufferedImage bufferedImage = ImageIO.read(sfile);
        int h = bufferedImage.getHeight();
        int w = bufferedImage.getWidth();

        // 灰度化
        int[][] gray = new int[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int argb = bufferedImage.getRGB(x, y);
                // 图像加亮（调整亮度识别率非常高）
                int r = (int) (((argb >> 16) & 0xFF) * 1.1 + 30);
                int g = (int) (((argb >> 8) & 0xFF) * 1.1 + 30);
                int b = (int) (((argb >> 0) & 0xFF) * 1.1 + 30);
                if (r >= 255) {
                    r = 255;
                }
                if (g >= 255) {
                    g = 255;
                }
                if (b >= 255) {
                    b = 255;
                }
                gray[x][y] = (int) Math
                        .pow((Math.pow(r, 2.2) * 0.2973 + Math.pow(g, 2.2)
                                * 0.6274 + Math.pow(b, 2.2) * 0.0753), 1 / 2.2);
            }
        }

        // 二值化
        int threshold = ostu(gray, w, h);
        BufferedImage binaryBufferedImage = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (gray[x][y] > threshold) {
                    gray[x][y] |= 0x00FFFF;
                } else {
                    gray[x][y] &= 0xFF0000;
                }
                binaryBufferedImage.setRGB(x, y, gray[x][y]);
            }
        }

//        //去除干扰线条
//        for(int y = 1; y < h-1; y++){
//            for(int x = 1; x < w-1; x++){
//                boolean flag = false ;
//                if(isBlack(binaryBufferedImage.getRGB(x, y))){
//                    //左右均为空时，去掉此点
//                    if(isWhite(binaryBufferedImage.getRGB(x-1, y)) && isWhite(binaryBufferedImage.getRGB(x+1, y))){
//                        flag = true;
//                    }
//                    //上下均为空时，去掉此点
//                    if(isWhite(binaryBufferedImage.getRGB(x, y+1)) && isWhite(binaryBufferedImage.getRGB(x, y-1))){
//                        flag = true;
//                    }
//                    //斜上下为空时，去掉此点
//                    if(isWhite(binaryBufferedImage.getRGB(x-1, y+1)) && isWhite(binaryBufferedImage.getRGB(x+1, y-1))){
//                        flag = true;
//                    }
//                    if(isWhite(binaryBufferedImage.getRGB(x+1, y+1)) && isWhite(binaryBufferedImage.getRGB(x-1, y-1))){
//                        flag = true;
//                    }
//                    if(flag){
//                        binaryBufferedImage.setRGB(x,y,-1);
//                    }
//                }
//            }
//        }


//        // 矩阵打印
//        for (int y = 0; y < h; y++)
//        {
//            for (int x = 0; x < w; x++)
//            {
//                if (isBlack(binaryBufferedImage.getRGB(x, y)))
//                {
//                    System.out.print("*");
//                } else
//                {
//                    System.out.print(" ");
//                }
//            }
//            System.out.println();
//        }

//        ImageIO.write(binaryBufferedImage, "jpg", new File(destDir, sfile
//                .getName()));

        return binaryBufferedImage;
    }

    private boolean isBlack(int colorInt) {
        Color color = new Color(colorInt);
        if (color.getRed() + color.getGreen() + color.getBlue() <= 300) {
            return true;
        }
        return false;
    }

    private boolean isWhite(int colorInt) {
        Color color = new Color(colorInt);
        if (color.getRed() + color.getGreen() + color.getBlue() > 300) {
            return true;
        }
        return false;
    }

    private int isBlackOrWhite(int colorInt) {
        if (getColorBright(colorInt) < 30 || getColorBright(colorInt) > 730) {
            return 1;
        }
        return 0;
    }

    private int getColorBright(int colorInt) {
        Color color = new Color(colorInt);
        return color.getRed() + color.getGreen() + color.getBlue();
    }

    private int ostu(int[][] gray, int w, int h) {
        int[] histData = new int[w * h];
        // Calculate histogram
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int red = 0xFF & gray[x][y];
                histData[red]++;
            }
        }

        // Total number of pixels
        int total = w * h;

        float sum = 0;
        for (int t = 0; t < 256; t++)
            sum += t * histData[t];

        float sumB = 0;
        int wB = 0;
        int wF = 0;

        float varMax = 0;
        int threshold = 0;

        for (int t = 0; t < 256; t++) {
            wB += histData[t]; // Weight Background
            if (wB == 0)
                continue;

            wF = total - wB; // Weight Foreground
            if (wF == 0)
                break;

            sumB += (float) (t * histData[t]);

            float mB = sumB / wB; // Mean Background
            float mF = (sum - sumB) / wF; // Mean Foreground

            // Calculate Between Class Variance
            float varBetween = (float) wB * (float) wF * (mB - mF) * (mB - mF);

            // Check if new maximum found
            if (varBetween > varMax) {
                varMax = varBetween;
                threshold = t;
            }
        }

        return threshold;
    }


    private BufferedImage getImagePicture(BufferedImage image) {
//        BufferedImage image = ImageIO.read(new File("C:\\Users\\78222\\Desktop\\测试\\Annotation 2019-08-14 230558.png"));
        int w = image.getWidth();
        int h = image.getHeight();
        float[] rgb = new float[3];
        double[][] zuobiao = new double[w][h];
        int R = 0;
        float red = 0;
        float green = 0;
        float blue = 0;
        BufferedImage bi = new BufferedImage(w, h,
                BufferedImage.TYPE_BYTE_BINARY);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int pixel = image.getRGB(x, y);
                rgb[0] = (pixel & 0xff0000) >> 16;
                rgb[1] = (pixel & 0xff00) >> 8;
                rgb[2] = (pixel & 0xff);
                red += rgb[0];
                green += rgb[1];
                blue += rgb[2];
                float avg = (rgb[0] + rgb[1] + rgb[2]) / 3;
                zuobiao[x][y] = avg;

            }
        }
        double SW = MainController.grayLeve;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (zuobiao[x][y] <= SW) {
                    int max = new Color(0, 0, 0).getRGB();
                    bi.setRGB(x, y, max);
                } else {
                    int min = new Color(255, 255, 255).getRGB();
                    bi.setRGB(x, y, min);
                }
            }
        }


        return bi;
    }

}
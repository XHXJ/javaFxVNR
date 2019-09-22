package com.xhxj.ocr.tool.imageUtil;

import net.sourceforge.tess4j.util.ImageHelper;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;

import javax.imageio.ImageIO;


public class BinaryTest {
    public static BufferedImage getImagePicture(BufferedImage image,double sw) throws Exception {
        image = ImageHelper.getScaledInstance(image, (int) (image.getWidth() * 2), (int) (image.getHeight() * 2));
        image = grayImage(image);
        image = binaryImage(image,sw);
        image = denoise(image);
        return image;
    }
    /**
     *
     * @param sourcePath 全路径，包括文件名
     * @param targetDir 目标目录，不包括文件名，最后面不要斜杠(数字+binary)
     * @throws Exception
     */
    public static void binaryAndCutImage(String sourcePath, String targetDir) throws Exception{
        File f = new File(sourcePath);
        BufferedImage input = ImageIO.read(f);
        input = ImageHelper.getScaledInstance(input, (int) (input.getWidth() * 2), (int) (input.getHeight() * 2));
        input = grayImage(input);
        input = binaryImage(input,180);
        input = denoise(input);
        String formatName = sourcePath.substring(sourcePath.lastIndexOf(".")+1, sourcePath.length());
        File destF = new File(targetDir);
        if (!destF.exists())
        {
            destF.mkdirs();
        }
        String targetPath = targetDir+File.separator+"binary."+formatName;
        ImageIO.write(input, formatName, new File(targetPath));
        int[] xpro = CutImg.xpro(input, input.getWidth(), input.getHeight());
        Rectangle[] r = CutImg.xproSegment(input, xpro, 4);
        for(int i=0; i<r.length; i++){
            int[] ypro = CutImg.ypro(input, r[i]);
            Rectangle y = CutImg.yproSegment(ypro, r[i], 2);
            ImageCutUtil imageCut = new ImageCutUtil(targetPath, targetDir+File.separator+i+"."+formatName, (int)y.getX(), (int)y.getY(), (int)y.getWidth(), (int)y.getHeight());
            imageCut.cut(imageCut.getSrcpath(), imageCut.getSubpath());
        }
    }

    public static void grayAndBinaryImage(String sourcePath, String targetPath) throws Exception {
        BufferedImage input = ImageIO.read(new FileInputStream(sourcePath));
        input = grayImage(input);
        input = binaryImage(input,180);
        input = denoise(input);
        String formatName = sourcePath.substring(sourcePath.lastIndexOf(".")+1, sourcePath.length());
        ImageIO.write(input, formatName, new File(targetPath));
    }

    /**
     * 图片灰化（参考：http://www.codeceo.com/article/java-image-gray.html）
     *
     * @param bufferedImage
     *            待处理图片
     * @return
     * @throws Exception
     */
    public static BufferedImage grayImage(BufferedImage bufferedImage) throws Exception {

        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        BufferedImage grayBufferedImage = new BufferedImage(width, height, bufferedImage.getType());
        for (int i = 0; i < bufferedImage.getWidth(); i++) {
            for (int j = 0; j < bufferedImage.getHeight(); j++) {
                final int color = bufferedImage.getRGB(i, j);
                final int r = (color >> 16) & 0xff;
                final int g = (color >> 8) & 0xff;
                final int b = color & 0xff;
                int gray = (int) (0.3 * r + 0.59 * g + 0.11 * b);
                int newPixel = colorToRGB(255, gray, gray, gray);
                grayBufferedImage.setRGB(i, j, newPixel);
            }
        }

        return grayBufferedImage;

    }

    /**
     * 颜色分量转换为RGB值
     *
     * @param alpha
     * @param red
     * @param green
     * @param blue
     * @return
     */
    private static int colorToRGB(int alpha, int red, int green, int blue) {

        int newPixel = 0;
        newPixel += alpha;
        newPixel = newPixel << 8;
        newPixel += red;
        newPixel = newPixel << 8;
        newPixel += green;
        newPixel = newPixel << 8;
        newPixel += blue;

        return newPixel;

    }

    /**
     * 二值化
     * @param image
     * @return
     * @throws Exception
     */
    public static BufferedImage binaryImage(BufferedImage image,double sw) throws Exception {
        int w = image.getWidth();
        int h = image.getHeight();
        float[] rgb = new float[3];
        double[][] zuobiao = new double[w][h];
        int black = new Color(0, 0, 0).getRGB();
        int white = new Color(255, 255, 255).getRGB();
        BufferedImage bi= new BufferedImage(w, h,
                BufferedImage.TYPE_BYTE_BINARY);;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int pixel = image.getRGB(x, y);
                rgb[0] = (pixel & 0xff0000) >> 16;
                rgb[1] = (pixel & 0xff00) >> 8;
                rgb[2] = (pixel & 0xff);
                float avg = (rgb[0]+rgb[1]+rgb[2])/3;
                zuobiao[x][y] = avg;

            }
        }
        //阀值
        double SW = sw;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (zuobiao[x][y] < SW) {
                    bi.setRGB(x, y, black);
                }else{
                    bi.setRGB(x, y, white);
                }
            }
        }



        return bi;
    }

    /**
     * 降噪，以1个像素点为单位
     * @param image
     * @param
     * @return
     */
    public static BufferedImage denoise(BufferedImage image){
        int w = image.getWidth();
        int h = image.getHeight();
        int white = new Color(255, 255, 255).getRGB();

        if(isWhite(image.getRGB(1, 0)) && isWhite(image.getRGB(0, 1)) && isWhite(image.getRGB(1, 1))){
            image.setRGB(0,0,white);
        }
        if(isWhite(image.getRGB(w-2, 0)) && isWhite(image.getRGB(w-1, 1)) && isWhite(image.getRGB(w-2, 1))){
            image.setRGB(w-1,0,white);
        }
        if(isWhite(image.getRGB(0, h-2)) && isWhite(image.getRGB(1, h-1)) && isWhite(image.getRGB(1, h-2))){
            image.setRGB(0,h-1,white);
        }
        if(isWhite(image.getRGB(w-2, h-1)) && isWhite(image.getRGB(w-1, h-2)) && isWhite(image.getRGB(w-2, h-2))){
            image.setRGB(w-1,h-1,white);
        }

        for(int x = 1; x < w-1; x++){
            int y = 0;
            if(isBlack(image.getRGB(x, y))){
                int size = 0;
                if(isWhite(image.getRGB(x-1, y))){
                    size++;
                }
                if(isWhite(image.getRGB(x+1, y))){
                    size++;
                }
                if(isWhite(image.getRGB(x, y+1))){
                    size++;
                }
                if(isWhite(image.getRGB(x-1, y+1))){
                    size++;
                }
                if(isWhite(image.getRGB(x+1, y+1))){
                    size++;
                }
                if(size>=5){
                    image.setRGB(x,y,white);
                }
            }
        }
        for(int x = 1; x < w-1; x++){
            int y = h-1;
            if(isBlack(image.getRGB(x, y))){
                int size = 0;
                if(isWhite(image.getRGB(x-1, y))){
                    size++;
                }
                if(isWhite(image.getRGB(x+1, y))){
                    size++;
                }
                if(isWhite(image.getRGB(x, y-1))){
                    size++;
                }
                if(isWhite(image.getRGB(x+1, y-1))){
                    size++;
                }
                if(isWhite(image.getRGB(x-1, y-1))){
                    size++;
                }
                if(size>=5){
                    image.setRGB(x,y,white);
                }
            }
        }

        for(int y = 1; y < h-1; y++){
            int x = 0;
            if(isBlack(image.getRGB(x, y))){
                int size = 0;
                if(isWhite(image.getRGB(x+1, y))){
                    size++;
                }
                if(isWhite(image.getRGB(x, y+1))){
                    size++;
                }
                if(isWhite(image.getRGB(x, y-1))){
                    size++;
                }
                if(isWhite(image.getRGB(x+1, y-1))){
                    size++;
                }
                if(isWhite(image.getRGB(x+1, y+1))){
                    size++;
                }
                if(size>=5){
                    image.setRGB(x,y,white);
                }
            }
        }

        for(int y = 1; y < h-1; y++){
            int x = w - 1;
            if(isBlack(image.getRGB(x, y))){
                int size = 0;
                if(isWhite(image.getRGB(x-1, y))){
                    size++;
                }
                if(isWhite(image.getRGB(x, y+1))){
                    size++;
                }
                if(isWhite(image.getRGB(x, y-1))){
                    size++;
                }
                //斜上下为空时，去掉此点
                if(isWhite(image.getRGB(x-1, y+1))){
                    size++;
                }
                if(isWhite(image.getRGB(x-1, y-1))){
                    size++;
                }
                if(size>=5){
                    image.setRGB(x,y,white);
                }
            }
        }

        //降噪，以1个像素点为单位
        for(int y = 1; y < h-1; y++){
            for(int x = 1; x < w-1; x++){
                if(isBlack(image.getRGB(x, y))){
                    int size = 0;
                    //上下左右均为空时，去掉此点
                    if(isWhite(image.getRGB(x-1, y))){
                        size++;
                    }
                    if(isWhite(image.getRGB(x+1, y))){
                        size++;
                    }
                    //上下均为空时，去掉此点
                    if(isWhite(image.getRGB(x, y+1))){
                        size++;
                    }
                    if(isWhite(image.getRGB(x, y-1))){
                        size++;
                    }
                    //斜上下为空时，去掉此点
                    if(isWhite(image.getRGB(x-1, y+1))){
                        size++;
                    }
                    if(isWhite(image.getRGB(x+1, y-1))){
                        size++;
                    }
                    if(isWhite(image.getRGB(x+1, y+1))){
                        size++;
                    }
                    if(isWhite(image.getRGB(x-1, y-1))){
                        size++;
                    }
                    if(size>=8){
                        image.setRGB(x,y,white);
                    }
                }
            }
        }

        return image;
    }

    public static boolean isBlack(int colorInt)
    {
        Color color = new Color(colorInt);
        if (color.getRed() + color.getGreen() + color.getBlue() <= 300)
        {
            return true;
        }
        return false;
    }

    public static boolean isWhite(int colorInt)
    {
        Color color = new Color(colorInt);
        if (color.getRed() + color.getGreen() + color.getBlue() > 300)
        {
            return true;
        }
        return false;
    }

    public static int isBlack(int colorInt, int whiteThreshold) {
        final Color color = new Color(colorInt);
        if (color.getRed() + color.getGreen() + color.getBlue() <= whiteThreshold) {
            return 1;
        }
        return 0;
    }

    // 自己加周围8个灰度值再除以9，算出其相对灰度值
    public static double getGray(double[][] zuobiao, int x, int y, int w, int h) {
        double rs = zuobiao[x][y] + (x == 0 ? 255 : zuobiao[x - 1][y]) + (x == 0 || y == 0 ? 255 : zuobiao[x - 1][y - 1])
                + (x == 0 || y == h - 1 ? 255 : zuobiao[x - 1][y + 1]) + (y == 0 ? 255 : zuobiao[x][y - 1])
                + (y == h - 1 ? 255 : zuobiao[x][y + 1]) + (x == w - 1 ? 255 : zuobiao[x + 1][y])
                + (x == w - 1 || y == 0 ? 255 : zuobiao[x + 1][y - 1])
                + (x == w - 1 || y == h - 1 ? 255 : zuobiao[x + 1][y + 1]);
        return rs / 9;
    }
}
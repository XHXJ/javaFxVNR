package com.xhxj.ocr.tool;

        import com.xhxj.ocr.controller.MainController;
        import net.sourceforge.tess4j.util.LoggHelper;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;

        import java.awt.Color;
        import java.awt.image.BufferedImage;
        import java.io.File;

public class ImagePicture {

    private static final Logger logger = LoggerFactory.getLogger(new LoggHelper().toString());



//    public static void main(String[] args) throws IOException {
//        BufferedImage bi=ImageIO.read(new File("C:\\Users\\78222\\Desktop\\测试\\Annotation 2019-08-14 230558.png"));//通过imageio将图像载入
//        int h=bi.getHeight();//获取图像的高
//        int w=bi.getWidth();//获取图像的宽
//        int rgb=bi.getRGB(0, 0);//获取指定坐标的ARGB的像素值
//        int[][] gray=new int[w][h];
//        for (int x = 0; x < w; x++) {
//            for (int y = 0; y < h; y++) {
//                gray[x][y]=getGray(bi.getRGB(x, y));
//            }
//        }
//
//        BufferedImage nbi=new BufferedImage(w,h,BufferedImage.TYPE_BYTE_BINARY);
//        int SW=150;
//        for (int x = 0; x < w; x++) {
//            for (int y = 0; y < h; y++) {
//                if(getAverageColor(gray, x, y, w, h)>SW){
//                    int max=new Color(255,255,255).getRGB();
//                    nbi.setRGB(x, y, max);
//                }else{
//                    int min=new Color(0,0,0).getRGB();
//                    nbi.setRGB(x, y, min);
//                }
//            }
//        }
//
//        ImageIO.write(nbi, "png", new File("C:\\Users\\78222\\Desktop\\测试\\test.png"));
//    }
//
//    public static int getGray(int rgb){
//        String str=Integer.toHexString(rgb);
//        int r=Integer.parseInt(str.substring(2,4),16);
//        int g=Integer.parseInt(str.substring(4,6),16);
//        int b=Integer.parseInt(str.substring(6,8),16);
//        //or 直接new个color对象
//        Color c=new Color(rgb);
//        r=c.getRed();
//        g=c.getGreen();
//        b=c.getBlue();
//        int top=(r+g+b)/3;
//        return (int)(top);
//    }
//
//    /**
//     * 自己加周围8个灰度值再除以9，算出其相对灰度值
//     * @param gray
//     * @param x
//     * @param y
//     * @param w
//     * @param h
//     * @return
//     */
//    public static int  getAverageColor(int[][] gray, int x, int y, int w, int h)
//    {
//        int rs = gray[x][y]
//                + (x == 0 ? 255 : gray[x - 1][y])
//                + (x == 0 || y == 0 ? 255 : gray[x - 1][y - 1])
//                + (x == 0 || y == h - 1 ? 255 : gray[x - 1][y + 1])
//                + (y == 0 ? 255 : gray[x][y - 1])
//                + (y == h - 1 ? 255 : gray[x][y + 1])
//                + (x == w - 1 ? 255 : gray[x + 1][ y])
//                + (x == w - 1 || y == 0 ? 255 : gray[x + 1][y - 1])
//                + (x == w - 1 || y == h - 1 ? 255 : gray[x + 1][y + 1]);
//        return rs / 9;
//    }


    public BufferedImage getImagePicture(BufferedImage image) {
//        BufferedImage image = ImageIO.read(new File("C:\\Users\\78222\\Desktop\\测试\\Annotation 2019-08-14 230558.png"));
        int w = image.getWidth();
        int h = image.getHeight();
        float[] rgb = new float[3];
        double[][] zuobiao = new double[w][h];
        int R = 0;
        float red = 0;
        float green = 0;
        float blue = 0;
        BufferedImage bi= new BufferedImage(w, h,
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
                float avg = (rgb[0]+rgb[1]+rgb[2])/3;
                zuobiao[x][y] = avg;

            }
        }
        double SW = MainController.grayLeve;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (zuobiao[x][y] <= SW) {
                    int max = new Color(0, 0, 0).getRGB();
                    bi.setRGB(x, y, max);
                }else{
                    int min = new Color(255, 255, 255).getRGB();
                    bi.setRGB(x, y, min);
                }
            }
        }


        return bi;
    }

}
package com.xhxj.ocr.tool;

        import com.xhxj.ocr.controller.MainController;
        import net.sourceforge.tess4j.util.LoggHelper;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;
        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.stereotype.Component;

        import java.awt.Color;
        import java.awt.image.BufferedImage;
        import java.io.File;
        import java.io.IOException;

        import javax.imageio.ImageIO;
public class ImagePicture {

    private static final Logger logger = LoggerFactory.getLogger(new LoggHelper().toString());

    /**
     *
     * @param bi 处理的图像
     * @param file 输出位置
     */
    public void getImagePicture(BufferedImage bi,File file) {
//        String filename = "C:\\Users\\78222\\IdeaProjects\\xhxjVNR\\out\\1565648909909.png";// separator是File里的一个常量,由于java历史遗留问题故为小写
//        File file = new File(filename);
//        BufferedImage bi = ImageIO.read(file);
        logger.info("当前灰度值设置为 :"+MainController.grayLeve);
        // 获取当前图片的高,宽,ARGB
        int h = bi.getHeight();
        int w = bi.getWidth();
        int rgb = bi.getRGB(0, 0);
        int arr[][] = new int[w][h];

        // 获取图片每一像素点的灰度值
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                // getRGB()返回默认的RGB颜色模型(十进制)
                arr[i][j] = getImageRgb(bi.getRGB(i, j));//该点的灰度值
            }

        }

        BufferedImage bufferedImage=new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);//  构造一个类型为预定义图像类型之一的 BufferedImage，TYPE_BYTE_BINARY（表示一个不透明的以字节打包的 1、2 或 4 位图像。）
        int FZ=130;
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                if(getGray(arr,i,j,w,h)>FZ){
                    int black=new Color(255,255,255).getRGB();
                    bufferedImage.setRGB(i, j, black);
                }else{
                    int white=new Color(0,0,0).getRGB();
                    bufferedImage.setRGB(i, j, white);
                }
            }

        }
        try {
            ImageIO.write(bufferedImage, "png", file.getCanonicalFile());
        } catch (IOException e) {
            logger.info("图像处理时写出文件出现异常");
            e.printStackTrace();
        }
    }

    private static int getImageRgb(int i) {
        String argb = Integer.toHexString(i);// 将十进制的颜色值转为十六进制
        // argb分别代表透明,红,绿,蓝 分别占16进制2位
        int r = Integer.parseInt(argb.substring(2, 4),16);//后面参数为使用进制
        int g = Integer.parseInt(argb.substring(4, 6),16);
        int b = Integer.parseInt(argb.substring(6, 8),16);
        int result=(int)((r+g+b)/3);
        return result;
    }



    //自己加周围8个灰度值再除以9，算出其相对灰度值
    public static double  getGray(int gray[][], int x, int y, int w, int h)
    {
        double rs = gray[x][y]
                + (x == 0 ? 255 : gray[x - 1][y])
                + (x == 0 || y == 0 ? 255 : gray[x - 1][y - 1])
                + (x == 0 || y == h - 1 ? 255 : gray[x - 1][y + 1])
                + (y == 0 ? 255 : gray[x][y - 1])
                + (y == h - 1 ? 255 : gray[x][y + 1])
                + (x == w - 1 ? 255 : gray[x + 1][ y])
                + (x == w - 1 || y == 0 ? 255 : gray[x + 1][y - 1])
                + (x == w - 1 || y == h - 1 ? 255 : gray[x + 1][y + 1]);
        return rs / MainController.grayLeve;
    }
}
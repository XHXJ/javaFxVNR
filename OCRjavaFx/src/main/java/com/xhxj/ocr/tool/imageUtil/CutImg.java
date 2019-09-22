package com.xhxj.ocr.tool.imageUtil;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * 已经二值化后的图片通过x,y投影实现切割，无粘连情况下效果很好
 * @ClassName:  CutImg
 * @Description:TODO(这里用一句话描述这个类的作用)
 * @author: chenyang
 * @date:   2017年9月15日 下午5:02:25
 *
 */
public class CutImg {

    /**
     * 图像向x轴做投影后的数组
     *
     * @param imagedata
     * @param w
     * @param h
     * @return
     */
    public static int[] xpro(BufferedImage input, int w, int h) {
        int xpro[] = new int[w];
        for (int j = 0; j < w; j++) {
            for (int i = 0; i < h; i++) {
                if (input.getRGB(j, i) !=-1)
                    xpro[j]++;
            }
        }
        return xpro;
    }

    /**
     * 基于x投影后再进行y轴投影
     *
     * @param imagedata
     * @param w
     * @param h
     * @return
     */
    public static int[] ypro(BufferedImage input, Rectangle xRectangle) {
        int ypro[] = new int[(int) xRectangle.getHeight()];
        int w = (int) xRectangle.getWidth();
        int h = (int) xRectangle.getHeight();
        for (int y = (int) xRectangle.getY(); y < h; y++) {
            for (int x = (int) xRectangle.getX(); x < xRectangle.getX()+w; x++) {
                if (input.getRGB(x, y) !=-1)
                    ypro[y]++;
            }
        }
        return ypro;
    }

    public static BufferedImage verticalProjection(int[] xpro, BufferedImage input){
        BufferedImage out = new BufferedImage(input.getWidth(), input.getHeight(), input.getType());
        for (int i = 0; i < input.getHeight(); i++)
        {
            for (int j = 0; j < input.getWidth(); j++)
            {
                int color = new Color(255, 255, 255).getRGB();  //背景设置为白色。
                out.setRGB(j, i, color);
            }
        }

        /*将直方图的曲线设为黑色*/
        for (int i = 0; i < input.getWidth(); i++)
        {
            for (int j = 0; j < xpro[i]; j++)
            {
                int color = new Color(0, 0, 0).getRGB();  //直方图设置为黑色
                out.setRGB(i, j, color);
            }
        }

        return out;
    }

    public static BufferedImage yVerticalProjection(int[] ypro, BufferedImage input){
        BufferedImage out = new BufferedImage(input.getWidth(), input.getHeight(), input.getType());
        for (int i = 0; i < input.getHeight(); i++)
        {
            for (int j = 0; j < input.getWidth(); j++)
            {
                int color = new Color(255, 255, 255).getRGB();  //背景设置为白色。
                out.setRGB(j, i, color);
            }
        }

        /*将直方图的曲线设为黑色*/
        for (int y = 0; y < input.getHeight(); y++)
        {
            for (int x = 0; x < ypro[y]; x++)
            {
                int color = new Color(0, 0, 0).getRGB();  //直方图设置为黑色
                out.setRGB(x, y, color);
            }
        }

        return out;
    }

    /**
     * 简单的基于投影的图像分割
     * @param xpro
     * @param w
     * @param h
     * @return
     */
    public static Rectangle[] xproSegment(BufferedImage input, int[] xpro, int FZ) {

        List<Rectangle> c = new ArrayList<Rectangle>();//用于储存分割出来的每个字符
        int startIndex = 0;//记录进入字符区的索引
        int endIndex = 0;//记录进入空白区域的索引
        boolean inBlock = false;//是否遍历到了字符区内
//        int totalWidth = 0;
        for (int i = 0; i < input.getWidth(); ++i)
        {
            if (!inBlock && xpro[i] != 0)//进入字符区了
            {
                inBlock = true;
                startIndex = i;
            }
            else if (xpro[i] == 0 && inBlock)//进入空白区了
            {
                endIndex = i;
                inBlock = false;
                int width = endIndex-startIndex;
                if(width>=FZ){
                    Rectangle rectangle = new Rectangle(startIndex, 0, width, input.getHeight());
                    c.add(rectangle);
//                    totalWidth+=width;
                }
            }
        }

        //预处理一下，先将间隔很小的rectangle合并
        List<Rectangle> cc = new ArrayList<Rectangle>();//用于储存分割出来的每个字符
        for (int i = 1; i <c.size() ; i++){
            Rectangle pre = c.get(i-1);
            Rectangle now = c.get(i);
            int xspace = (int) now.getX() - (int)(pre.getX()+pre.getWidth());
            if(xspace<2 && (now.getWidth()<=FZ || pre.getWidth()<=4)){
                //合并
                int x = (int)(pre.getX());
                int w = (int)(now.getX()-pre.getX()+now.getWidth());
                Rectangle newR = new Rectangle(x, 0, w, input.getHeight());
                cc.add(newR);
            }else{
                if(i==1){
                    cc.add(pre);
                }
                if(i!=c.size()-1){
                    Rectangle next = c.get(i+1);
                    int space = (int) next.getX() - (int)(now.getX()+now.getWidth());
                    if(space>=2 || (now.getWidth()>FZ && next.getWidth()>4)){
                        cc.add(now);
                    }
                }else{
                    cc.add(now);
                }
            }
        }

        Rectangle result[] = cc.toArray(new Rectangle[]{});

        //如果有三个，找出最多的再分割一次
        if(result.length==3){
            int maxIndex = 0;
            int maxWidth = (int) result[0].getWidth();
            for(int i=0;i<result.length;i++){
                Rectangle r = result[i];
                if(maxWidth<r.getWidth()){
                    maxWidth = (int) r.getWidth();
                    maxIndex = i;
                }
            }

            Rectangle r = result[maxIndex];
            int newXpro[] = new int[(int) r.getWidth()];
            for(int i=0; i<r.getWidth(); i++){
                newXpro[i] = xpro[(int) (i+r.getX())];
            }

            List<Bottom> bottoms = findXproBottom(newXpro,1);
            if(bottoms!=null && bottoms.size()>0){
                Bottom bottom = bottoms.get(0);
                Rectangle finalResult[] = new Rectangle[4];
                Rectangle new1 = new Rectangle((int)result[maxIndex].getX(), 0, bottom.getStartIndex()+1, input.getHeight());
                Rectangle new2 = new Rectangle((int)result[maxIndex].getX()+bottom.getStartIndex(), 0, (int)r.getWidth()-bottom.getStartIndex()-1, input.getHeight());
                for(int i=0; i<maxIndex; i++){
                    finalResult[i] = result[i];
                }
                finalResult[maxIndex] = new1;
                finalResult[maxIndex+1] = new2;
                for(int i=maxIndex+2; i<4; i++){
                    finalResult[i] = result[i-1];
                }

                return finalResult;
            }
        }

        //多了一个，将最少那个和旁边的合并
        if(result.length==5){
            int minIndex = 0;
            int minWidth = (int) result[0].getWidth();
            for(int i=0;i<result.length;i++){
                Rectangle r = result[i];
                if(minWidth>r.getWidth()){
                    minWidth = (int) r.getWidth();
                    minIndex = i;
                }
            }
            //第一个，直接和后面一个合并
            if(minIndex==0){
                Rectangle r1 = result[0];
                Rectangle r2 = result[1];
                int newWidth = (int) ((r2.getX()-r1.getX())+r2.getWidth());
                Rectangle newr = new Rectangle((int)r1.getX(), 0, newWidth, (int)r1.getHeight());
                Rectangle finalResult[] = new Rectangle[4];
                finalResult[0] = newr;
                finalResult[1] = result[2];finalResult[2] = result[3];finalResult[3] = result[4];
                return finalResult;
            }else
                //最后一个，直接和前面一个合并
                if(minIndex==4){
                    Rectangle r1 = result[3];
                    Rectangle r2 = result[4];
                    int newWidth = (int) ((r2.getX()-r1.getX())+r2.getWidth());
                    Rectangle newr = new Rectangle((int)r1.getX(), 0, newWidth, (int)r1.getHeight());
                    Rectangle finalResult[] = new Rectangle[4];
                    finalResult[0] = result[0];
                    finalResult[1] = result[1];finalResult[2] = result[2];finalResult[3] = newr;
                    return finalResult;
                } else
                //中间，和前后的比较一下
                {
                    Rectangle r = result[minIndex];
                    Rectangle newr;
                    Rectangle pre = result[minIndex-1];
                    Rectangle next = result[minIndex+1];
                    //前一个
                    if(pre.getWidth()<next.getWidth()){
                        int newWidth = (int) ((r.getX()-pre.getX())+r.getWidth());
                        newr = new Rectangle((int)pre.getX(), 0, newWidth, (int)r.getHeight());
                        Rectangle finalResult[] = new Rectangle[4];
                        for(int i=0; i<minIndex-1; i++){
                            finalResult[i] = result[i];
                        }
                        finalResult[minIndex-1] = newr;
                        for(int i=minIndex+1; i<5; i++){
                            finalResult[i-1] = result[i];
                        }
                        return finalResult;
                    }
                    //后一个
                    else{
                        int newWidth = (int) ((next.getX()-r.getX())+r.getWidth());
                        newr = new Rectangle((int)r.getX(), 0, newWidth, (int)r.getHeight());
                        Rectangle finalResult[] = new Rectangle[4];
                        for(int i=0; i<minIndex; i++){
                            finalResult[i] = result[i];
                        }
                        finalResult[minIndex] = newr;
                        for(int i=minIndex+2; i<5; i++){
                            finalResult[i-1] = result[i];
                        }
                        return finalResult;
                    }
                }
        }

        return result;
    }

    /**
     * 简单的基于投影的图像分割
     * @param xpro
     * @param w
     * @param h
     * @return
     */
    public static Rectangle yproSegment(int[] ypro, Rectangle xRectangle, int FZ) {

        List<Rectangle> c = new ArrayList<Rectangle>();//用于储存分割出来的每个字符
        int startIndex = 0;//记录进入字符区的索引
        int endIndex = 0;//记录进入空白区域的索引
        boolean inBlock = false;//是否遍历到了字符区内
        for (int y = 0; y < xRectangle.getHeight(); y++)
        {
            if (!inBlock && ypro[y] != 0)//进入字符区了
            {
                inBlock = true;
                startIndex = y;
            }
            else if (ypro[y] == 0 && inBlock)//进入空白区了
            {
                endIndex = y;
                inBlock = false;
                int height = endIndex-startIndex;
                if(height>=FZ){
                    Rectangle rectangle = new Rectangle((int)xRectangle.getX(), (int)(startIndex+xRectangle.getY()), (int)xRectangle.getWidth(), height);
                    c.add(rectangle);
                }
            }
        }
        //可能出现触底的情况,有start没有end
        if(startIndex>0 && endIndex<=startIndex){
            int height = (int)xRectangle.getHeight()-startIndex;
            if(height>FZ){
                return new Rectangle((int)xRectangle.getX(), (int)(startIndex+xRectangle.getY()), (int)xRectangle.getWidth(), height);
            }
        }

        //没有的话，在检查一下是不是图片断断续续的,直接合并
        if(c.size()==0){
            List<Integer> cc = new ArrayList<Integer>();//用于储存分割出来的每个字符
            int startIndex2 = 0;//记录进入字符区的索引
            int endIndex2 = 0;//记录进入空白区域的索引
            boolean inBlock2 = false;//是否遍历到了字符区内
            for (int y = 0; y < xRectangle.getHeight(); y++)
            {
                if (!inBlock2 && ypro[y] != 0)//进入字符区了
                {
                    inBlock2 = true;
                    startIndex2 = y;
                }
                else if (ypro[y] == 0 && inBlock2)//进入空白区了
                {
                    endIndex2 = y;
                    inBlock2 = false;
                    int height = endIndex2-startIndex2;
                    if(height>=1){
                        Rectangle rectangle = new Rectangle((int)xRectangle.getX(), (int)(startIndex2+xRectangle.getY()), (int)xRectangle.getWidth(), height);
                        c.add(rectangle);
                    }
                }
            }

            if(c.size()>0){
                if(c.size()>=2){
                    for(int i=1; i<c.size(); i++){
                        Rectangle pre = c.get(i-1);
                        Rectangle now = c.get(i);
                        int space = (int) now.getY() - (int)(pre.getY()+pre.getHeight());
                        if(space<2){
                            cc.add(i);
                        }
                    }
                }

                int firstIndex = cc.get(0)-1;
                int lastIndex = cc.get(cc.size()-1);
                Rectangle first = c.get(firstIndex);
                Rectangle last = c.get(lastIndex);

                int x = (int)xRectangle.getX();
                int y = (int) (xRectangle.getY() + first.getY());
                int w = (int)xRectangle.getWidth();
                int h = (int) (last.getY() - y+last.getHeight());
                Rectangle rectangle = new Rectangle(x, y, w, h);
                return rectangle;
            }
        }

        if(c.size()==1){
            return c.get(0);
        }else{
            //多个直接合并吧
            Rectangle first = c.get(0);
            Rectangle last = c.get(c.size()-1);
            int x = (int)xRectangle.getX();
            int y = (int) (xRectangle.getY() + first.getY());
            int w = (int)xRectangle.getWidth();
            int h = (int) (last.getY() - y+last.getHeight());
            Rectangle rectangle = new Rectangle(x, y, w, h);
            return rectangle;
        }
    }

    /**
     * n表示想要获取的个数
     * @param xpro
     * @param n
     * @return
     */
    public static List<Bottom> findXproBottom(int xpro[], int n){
        //好像不可取，暂时不管了
        List<Integer[]> bottom2 = new ArrayList<>();
        int startIndex = 0;
        int endIndex = 0;
        for(int i=1; i<xpro.length-1; i++){
            int thisPoint = xpro[i];
            int prePoint = xpro[i-1];
            int nextPoint = xpro[i+1];
            if(prePoint>thisPoint && thisPoint<=nextPoint){
                startIndex = i;
//    			System.out.println("startIndex："+i+"  prePoint:"+prePoint+",thisPoint:"+thisPoint+",nextPoint"+nextPoint);
                bottom2.add(new Integer[]{0,startIndex});
            }else
            if(prePoint>=thisPoint && thisPoint<nextPoint){
                endIndex = i;
                if(endIndex>startIndex){
                    boolean check = true;
                    for(int j=startIndex; j<=endIndex; j++){
                        if(xpro[j]!=xpro[startIndex]){
                            check = false;
                            break;
                        }
                    }
                    if(check){
//    					System.out.println("endIndex："+i+"  prePoint:"+prePoint+",thisPoint:"+thisPoint+",nextPoint"+nextPoint);
                        bottom2.add(new Integer[]{1,endIndex});
                    }
                }
            }
        }

        if(bottom2.size()<2 ){
            return null;
        }

        Integer[][] bottom3 = new Integer[bottom2.size()][2];
        for(int i=0; i<bottom2.size(); i++){
            bottom3[i] = bottom2.get(i);
        }

        List<Bottom> bottoms = new ArrayList<>();
        for(int i=0; i<bottom3.length; i++){
            if(bottom3[i][0]==1 && i!=0){
                Integer[] start = bottom3[i-1];
                Integer[] end = bottom3[i];
                if(start[0]==0){
                    Bottom b = new Bottom(start[1], end[1], xpro[start[1]]);
                    bottoms.add(b);
                }
            }
        }
        if(bottoms==null || bottoms.size()==0){
            return null;
        }
        if(n==1){
            Bottom real = bottoms.get(0);
            for(Bottom b : bottoms){
                if(real.getHeight()>b.getHeight()){
                    real = b;
                }
            }
            List<Bottom> bb = new ArrayList<>();
            bb.add(real);
            return bb;
        }else{

            /*排序*/
            for(int i=0;i<bottoms.size()-1;i++){
                for(int j=0;j<bottoms.size()-i-1;j++){//比较两个整数
                    if(bottoms.get(j).getHeight()>bottoms.get(j+1).getHeight()){
                        /*交换*/
                        Bottom temp=bottoms.get(j);
                        bottoms.set(j, bottoms.get(j+1));
                        bottoms.set(j+1, temp);
                    }
                }
            }

            if(n<bottoms.size()){
                for(int i=n; i<bottoms.size(); i++){
                    bottoms.remove(i);
                }
            }
            return bottoms;

        }
    }
}

class Bottom{
    private int startIndex;
    private int endIndex;
    private int height;

    public Bottom() {
    }

    public Bottom(int startIndex, int endIndex, int height) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.height = height;
    }

    public int getStartIndex() {
        return startIndex;
    }
    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }
    public int getEndIndex() {
        return endIndex;
    }
    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }
    public int getHeight() {
        return height;
    }
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public String toString() {
        return "Bottom [startIndex=" + startIndex + ", endIndex=" + endIndex + ", height=" + height + "]";
    }

}
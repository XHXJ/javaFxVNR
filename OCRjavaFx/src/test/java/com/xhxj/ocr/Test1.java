package com.xhxj.ocr;

import com.xhxj.ocr.controller.OcrTxtController;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
@RunWith(SpringRunner.class)
@SpringBootTest(classes = OcrTxtController.class)
public class Test1 {
    @Test
    public void Test() {
        File imageFile = new File("C:\\Users\\78222\\IdeaProjects\\javaFxVNR\\out\\d03f939d-3a83-40f4-b852-ae3d4d847e41.png");
        ITesseract instance = new Tesseract();  // JNA Interface Mapping
        // ITesseract instance = new Tesseract1(); // JNA Direct Mapping
        instance.setDatapath("C:\\Program Files (x86)\\Tesseract-OCR\\tessdata"); // path to tessdata directory
        instance.setLanguage("jpn");

        try {
            String result = instance.doOCR(imageFile);
            System.out.println(result);
        } catch (TesseractException e) {
            System.err.println(e.getMessage());
        }
    }
    @Test
    public void testQuery(){
        HashMap<String, String> objectObjectHashMap = new HashMap<>();

        System.out.println(objectObjectHashMap);

    }
}

package com.xhxj.ocr;

import java.util.concurrent.CountDownLatch;

public class Test1 {
    public static void main(String[] args) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("子线程运行!");
                latch.countDown();
            }).start();
        }
        latch.await();
        System.out.println("主线程运行");
    }
}

package org.jetlinks.cloud.test;

import co.paralleluniverse.fibers.Fiber;
import lombok.SneakyThrows;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public class FiberTest {

    @SneakyThrows
    public static void main(String[] args) {
//        ExecutorService executorService = Executors.newFixedThreadPool(32);
//
//
//        {
//            new Fiber<>("test", () -> {
//
//
//            }).start();
//            long time = System.currentTimeMillis();
//            CountDownLatch latch = new CountDownLatch(1000);
//            for (int i = 0; i < 1000; i++) {
//                new Fiber<>("test2", () -> {
//                    Fiber.sleep(1000);
//                    latch.countDown();
//                }).start();
//            }
//            latch.await();
//            System.out.println(System.currentTimeMillis() - time);
//        }
//
//        {
//
            long time = System.currentTimeMillis();
            CountDownLatch latch = new CountDownLatch(1000);
        RestTemplate restTemplate=new RestTemplate();

            for (int i = 0; i < 1000; i++) {
                CompletableFuture.runAsync(() -> {
                    try {
                        restTemplate.getForObject("http://www.baidu.com",String.class);

                    } catch (Exception e) {
                    } finally {
                        latch.countDown();
                    }
                }, run->new Fiber<>("test",run::run).start());
            }
            latch.await();
            System.out.println(System.currentTimeMillis() - time);
//            executorService.shutdown();
//        }

//        AtomicLong counter=new AtomicLong();
//        for (int i = 0; i < 100000; i++) {
//            new Fiber<>("test2", () -> {
//               for (;;){
//                   Fiber.sleep(1000);
//                   System.out.println(counter.incrementAndGet());
//               }
//            }).start();
//        }
//
//        System.in.read();
    }

}

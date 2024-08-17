package io.github.enerccio.rensu.app;

import io.github.enerccio.rensu.ui.MainWindow;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.swing.*;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Main extends EventQueue {

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    static ApplicationContext applicationContext;

    public static void main(String[] args) {
        log.info("Starting rensu-ui with arguments {}", Arrays.toString(args));
        try {
            log.info("Starting application context.");
            try (ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("META-INF/spring/applicationContext.xml")) {
                Main.applicationContext = applicationContext;

                applicationContext.getBean(Args.class).setArgs(List.of(args));
                CountDownLatch waitLatch = new CountDownLatch(1);

                SwingUtilities.invokeLater(() -> {
                    try {
                        MainWindow mainWindow = new MainWindow();
                        mainWindow.create();
                        mainWindow.setVisible(true);
                        mainWindow.addWindowListener(new WindowAdapter() {
                            @Override
                            public void windowClosed(WindowEvent e) {
                                waitLatch.countDown();
                            }
                        });
                    } catch (Exception e) {
                        log.error(e.getMessage());
                        log.debug(e.getMessage(), e);
                    }
                });

                waitLatch.await();
            } finally {
                log.info("Closing application context.");
            }
        } catch (Exception e) {
            log.error("Main failed with {}", e.getMessage());
            log.debug(e.getMessage(), e);
        }
    }


    static {
        try {
            ClassPool classPool = ClassPool.getDefault();
            CtClass ctClass = classPool.get("sun.awt.screencast.ScreencastHelper");
            ctClass.stopPruning(true);

            if (ctClass.isFrozen())
                ctClass.defrost();

            CtMethod method = ctClass.getDeclaredMethod("timerCloseSessionRestart");
            ctClass.removeMethod(method);

            method = CtMethod.make("private static void timerCloseSessionRestart() {}", ctClass);
            ctClass.addMethod(method);
            ctClass.toClass();

            classPool.toClass(ctClass, Thread.currentThread().getContextClassLoader().getParent(), null);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

}

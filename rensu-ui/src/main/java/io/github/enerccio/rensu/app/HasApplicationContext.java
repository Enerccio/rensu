package io.github.enerccio.rensu.app;

import org.springframework.context.ApplicationContext;

public interface HasApplicationContext {

    default ApplicationContext getApplicationContext() {
        return Main.applicationContext;
    }

}

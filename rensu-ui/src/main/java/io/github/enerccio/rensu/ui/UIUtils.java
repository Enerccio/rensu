package io.github.enerccio.rensu.ui;

import org.slf4j.Logger;

public class UIUtils {

    public static void onError(Logger logger, Throwable t, String text) {
        logger.error(t.getMessage());
        logger.debug(t.getMessage(), t);
    }

}

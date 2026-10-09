package cn.itcraft.cl4g4.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

/**
 * Created by Helly on 2017/05/17.
 */
public final class ForbiddenClassInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForbiddenClassInitializer.class);

    private ForbiddenClassInitializer() {
    }

    public static <T> T newAction(PrivilegedAction<T> action) {
        try {
            return AccessController.doPrivileged(action);
        } catch (Exception e) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(e.getMessage(), e);
            }
            return null;
        }
    }

    public static <T> T newExAction(PrivilegedExceptionAction<T> action) {
        try {
            return AccessController.doPrivileged(action);
        } catch (Exception e) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(e.getMessage(), e);
            }
            return null;
        }
    }
}

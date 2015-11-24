package com.insweat.hssd.lib.interop;

import com.insweat.hssd.lib.util.logging.Logger;

public class Logging {
    private final static com.insweat.hssd.lib.util.logging.package$
        pkgLogging = Helper.pkgLogging();
    public final static int LEVEL_MIN = pkgLogging.LEVEL_MIN();
    public final static int LEVEL_DEBUG = pkgLogging.LEVEL_DEBUG();
    public final static int LEVEL_INFO = pkgLogging.LEVEL_INFO();
    public final static int LEVEL_NOTICE = pkgLogging.LEVEL_NOTICE();
    public final static int LEVEL_WARNING = pkgLogging.LEVEL_WARNING();
    public final static int LEVEL_ERROR = pkgLogging.LEVEL_ERROR();
    public final static int LEVEL_CRITICAL = pkgLogging.LEVEL_CRITICAL();
    public final static int LEVEL_MAX = pkgLogging.LEVEL_MAX();

    public static Logger getRoot() {
        return pkgLogging.root();
    }

    public static String getLevelName(int level) {
        return pkgLogging.getLevelName(level);
    }

    public static Logger getChild(
            Logger logger,
            String name,
            int level,
            boolean propagate) {
        return logger.getChild(name, Interop.opt(Integer.valueOf(level)), propagate);
    }

    protected static void logf(
            Logger logger, int level, String fmt, Object[] args) {
        try {
            logger.log(level, String.format(fmt, args));
        }
        catch(Exception e) {
            getRoot().log(Logging.LEVEL_ERROR, String.format(
                    "Error logging %s %s", fmt, args));
        }
    }
    
    public static void debugf(Logger logger, String fmt, Object ... args) {
    	logf(logger, LEVEL_DEBUG, fmt, args);
    }
    
    public static void infof(Logger logger, String fmt, Object ... args) {
    	logf(logger, LEVEL_INFO, fmt, args);
    }

    public static void noticef(Logger logger, String fmt, Object ... args) {
    	logf(logger, LEVEL_NOTICE, fmt, args);
    }

    public static void warnf(Logger logger, String fmt, Object ... args) {
    	logf(logger, LEVEL_WARNING, fmt, args);
    }

    public static void errorf(Logger logger, String fmt, Object ... args) {
    	logf(logger, LEVEL_ERROR, fmt, args);
    }

    public static void criticalf(Logger logger, String fmt, Object ... args) {
    	logf(logger, LEVEL_CRITICAL, fmt, args);
    }
    
    public static void exceptionf(Logger logger, int level, Throwable t, String fmt, Object ... args) {
        logger.log(level,
                String.format(fmt, args),
                Interop.list(Interop.tuple("exception", t)));
    }

}

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
        return logger.getChild(name, Interop.opt((Object)level), propagate);
    }
}

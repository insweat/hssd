package com.insweat.hssd.editor.util;

import com.insweat.hssd.lib.interop.Logging;
import com.insweat.hssd.lib.util.logging.Handler;
import com.insweat.hssd.lib.util.logging.Logger;
import com.insweat.hssd.lib.util.logging.Record;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import java.text.SimpleDateFormat;

public class LogSupport {

    private final String qname;
    private Logger logger;

    public LogSupport(String qname) {
        this.qname = qname;
    }

    public Logger getLogger() {
        if(logger == null) {
            logger = getLogger(qname);
        }
        return logger;
    }
    
    
    public void debugf(String fmt, Object ... args) {
    	Logging.debugf(logger, fmt, args);
    }

    
    public void infof(String fmt, Object ... args) {
    	Logging.infof(logger, fmt, args);
    }

    
    public void noticef(String fmt, Object ... args) {
    	Logging.noticef(logger, fmt, args);
    }

    
    public void warnf(String fmt, Object ... args) {
    	Logging.warnf(logger, fmt, args);
    }

    
    public void errorf(String fmt, Object ... args) {
    	Logging.errorf(logger, fmt, args);
    }

    
    public void criticalf(String fmt, Object ... args) {
    	Logging.criticalf(logger, fmt, args);
    }

    
    private static MessageConsole findConsole(String name) {
        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsoleManager conMan = plugin.getConsoleManager();
        IConsole[] existing = conMan.getConsoles();
        for (int i = 0; i < existing.length; i++)
            if (name.equals(existing[i].getName()))
                return (MessageConsole) existing[i];
        MessageConsole myConsole = new MessageConsole(name, null);
        conMan.addConsoles(new IConsole[]{myConsole});
        return myConsole;
    }

    public static Logger getLogger(String qname) {
        return getLogger(qname, Logging.LEVEL_MIN);
    }

    public static Logger getLogger(String qname, int level) {
        final String[] names = qname.split("\\.");
        final Logger rootLogger = Logging.getRoot();
        if(0 == rootLogger.handlerCount()) {
            final SimpleDateFormat dateFmt = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss");
            final Display d = Display.getCurrent();
            final MessageConsole console = findConsole("HSSD");
            final MessageConsoleStream debug = console.newMessageStream();
            final MessageConsoleStream info = console.newMessageStream();
            final MessageConsoleStream notice = console.newMessageStream();
            final MessageConsoleStream warning = console.newMessageStream();
            final MessageConsoleStream error = console.newMessageStream();
            final MessageConsoleStream critical = console.newMessageStream();

            notice.setColor(d.getSystemColor(SWT.COLOR_BLUE));
            warning.setColor(d.getSystemColor(SWT.COLOR_DARK_YELLOW));
            error.setColor(d.getSystemColor(SWT.COLOR_MAGENTA));
            critical.setColor(d.getSystemColor(SWT.COLOR_RED));

            rootLogger.addHandler(new Handler(Logging.LEVEL_MIN) {
                @Override
                public void emit(Record record) {
                    final int level = record.level();
                    final String timestamp = dateFmt.format(record.timestamp());
                    final String levelStr = Logging.getLevelName(level);
                    final MessageConsoleStream stream;
                    if(level >= Logging.LEVEL_CRITICAL) {
                        stream = critical;
                    }
                    else if(level >= Logging.LEVEL_ERROR) {
                        stream = error;
                    }
                    else if(level >= Logging.LEVEL_WARNING) {
                        stream = warning;
                    }
                    else if(level >= Logging.LEVEL_NOTICE) {
                        stream = notice;
                    }
                    else if(level >= Logging.LEVEL_INFO) {
                        stream = info;
                    }
                    else if(level >= Logging.LEVEL_DEBUG){
                        stream = debug;
                    }
                    else {
                        stream = error;
                    }
                    if(record.exception().isDefined()) {
                        stream.println(String.format(
                                "%s [%s] <%s>: %s %s",
                                timestamp,
                                levelStr,
                                record.name(),
                                record.msg(),
                                record.exception().get()));
                    }
                    else {
                        stream.println(String.format(
                                "%s [%s] <%s>: %s",
                                timestamp,
                                levelStr,
                                record.name(),
                                record.msg()));
                    }
                }
            });

            IWorkbenchPage page = Helper.getActiveWBPage();
            String id = IConsoleConstants.ID_CONSOLE_VIEW;
            try {
			    final IViewPart view = page.showView(id);
	            IConsoleView consoleView = (IConsoleView) view;
	            consoleView.display(console);
			} catch (PartInitException e) {
				e.printStackTrace();
			}
        }
        Logger rv = rootLogger;
        for(String name : names) {
            rv = Logging.getChild(rv, name, level, true);
        }
        return rv;
    }
}

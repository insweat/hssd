package com.insweat.hssd.editor.services;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.services.AbstractServiceFactory;
import org.eclipse.ui.services.IServiceLocator;

import com.insweat.hssd.editor.util.Helper;

public class ServiceFactory extends AbstractServiceFactory {
    
    private boolean disposed = false;
    private final HashMap<Class<?>, Object> serviceRegistry = new HashMap<>();

    public ServiceFactory() {
        Helper.getWB().addWorkbenchListener(new IWorkbenchListener() {
            
            @Override
            public boolean preShutdown(IWorkbench workbench, boolean forced) {
                dispose();
                return true;
            }
            
            @Override
            public void postShutdown(IWorkbench workbench) {
            }
        });
    }
    
    public void dispose() {
        if(disposed) {
            return;
        }
        disposed = true;
        for(Object svc: serviceRegistry.values()) {
            final Method dispose;
            try {
                dispose = svc.getClass().getMethod("dispose");
            } catch (NoSuchMethodException e) {
                continue;
            } catch (SecurityException e) {
                e.printStackTrace();
                continue;
            }
            
            try {
                dispose.invoke(svc);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        serviceRegistry.clear();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object create(Class serviceInterface, IServiceLocator parentLocator,
            IServiceLocator locator) {
        if(disposed) {
            return null;
        }
        Object rv = serviceRegistry.get(serviceInterface);
        if(rv == null) {
            if(serviceInterface == IDService.class) {
                rv = new IDService();
                serviceRegistry.put(serviceInterface, rv);
            }
        }
        return rv;
    }

}

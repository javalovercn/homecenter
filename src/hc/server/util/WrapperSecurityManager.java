package hc.server.util;

import hc.util.ClassUtil;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;

public class WrapperSecurityManager extends SecurityManager {
	private final SecurityManager old;
	private final Class[] nullParaTypes = {};
	private final Object[] nullParas = {};
	
	public WrapperSecurityManager(SecurityManager sm){
		old = sm;
	}
	
    private boolean hasAllPermission(){
    	if(old != null){
    		Object out = ClassUtil.invoke(SecurityManager.class, old, "hasAllPermission", nullParaTypes, nullParas, true);
            if(out != null && out instanceof Boolean){
            	return ((Boolean)out);
            }
    	}
    	
        return true;
    }

    public final boolean getInCheck() {
    	if(old == null){
    		return false;
    	}
    	
        return old.getInCheck();
    }

    protected final Class[] getClassContext(){
    	if(old != null){
	    	Object out = ClassUtil.invoke(SecurityManager.class, old, "getClassContext", nullParaTypes, nullParas, true);
	    	if(out != null && out instanceof Class[]){
	    		return (Class[])out;
	    	}
    	}
    	Class[] back = {};
    	return back;
    }

    protected final ClassLoader currentClassLoader()
    {
    	if(old != null){
	        Object out = ClassUtil.invoke(SecurityManager.class, old, "currentClassLoader", nullParaTypes, nullParas, true);
	        if(out != null && out instanceof ClassLoader){
	        	return (ClassLoader)out;
	        }
    	}
    	
		return null;
    }

    protected final Class<?> currentLoadedClass() {
    	if(old != null){
	    	Object out = ClassUtil.invoke(SecurityManager.class, old, "currentLoadedClass", nullParaTypes, nullParas, true);
	        if(out != null && out instanceof Class<?>){
	        	return (Class<?>)out;
	        }
    	}
    	
		return null;
    }

    protected final int classDepth(String name){
    	if(old != null){
	    	Class[] paraTypes = {String.class};
	    	Object[] para = {name};
	    	
	    	Object out = ClassUtil.invoke(SecurityManager.class, old, "classDepth", paraTypes, para, true);
	        if(out != null && out instanceof Integer){
	        	return (Integer)out;
	        }
    	}
    	
		return 0;
    }

    protected final int classLoaderDepth()
    {
    	if(old != null){
	    	Object out = ClassUtil.invoke(SecurityManager.class, old, "classLoaderDepth", nullParaTypes, nullParas, true);
	        if(out != null && out instanceof Integer){
	        	return (Integer)out;
	        }
    	}
    	
    	return 0;
    }

    protected final boolean inClass(String name) {
    	if(old != null){
	    	Class[] paraTypes = {String.class};
	    	Object[] para = {name};
	    	
	    	Object out = ClassUtil.invoke(SecurityManager.class, old, "inClass", paraTypes, para, true);
	        if(out != null && out instanceof Boolean){
	        	return (Boolean)out;
	        }
    	}
    		
    	return false;
    }

    protected final boolean inClassLoader() {
    	if(old != null){
	    	Object out = ClassUtil.invoke(SecurityManager.class, old, "inClassLoader", nullParaTypes, nullParas, true);
	    	if(out != null && out instanceof Boolean){
	        	return (Boolean)out;
	        }
    	}
    		
    	return false;
    }

    public final Object getSecurityContext() {
    	if(old != null){
    		final Object securityContext = old.getSecurityContext();
    		if(securityContext != null){
    			return securityContext;
    		}
    	}
    		
    	return null;
    }

    public void checkPermission(Permission perm) {
    	if(old != null){
    		old.checkPermission(perm);
    	}
    		
//    	super.checkPermission(perm);//否则导致异常
    }

    public void checkPermission(Permission perm, Object context) {
    	if(old != null){
    		old.checkPermission(perm, context);
    	}
    		
//    	super.checkPermission(perm, context);//否则导致异常
    }

    public void checkCreateClassLoader() {
    	if(old != null){
    		old.checkCreateClassLoader();
    	}
    		
    	super.checkCreateClassLoader();
    }

    public void checkAccess(Thread t) {
    	if(old != null){
    		old.checkAccess(t);
    	}
    		
    	super.checkAccess(t);
    }
    
    public void checkAccess(ThreadGroup g) {
    	if(old != null){
    		old.checkAccess(g);
    	}
    		
//    	super.checkAccess(g);//否则导致异常
    }

    public void checkExit(int status) {
    	if(old != null){
    		old.checkExit(status);
    	}
    		
//    	super.checkExit(status);
    }

    public void checkExec(String cmd) {
    	if(old != null){
    		old.checkExec(cmd);
    	}
    	
//    	super.checkExec(cmd);
    }

    public void checkLink(String lib) {
    	if(old != null){
    		old.checkLink(lib);
    	}
    		
//    	super.checkLink(lib);
    }

    public void checkRead(FileDescriptor fd) {
    	if(old != null){
    		old.checkRead(fd);
    	}
    	
//    	super.checkRead(fd);
    }

    public void checkRead(String file) {
    	if(old != null){
    		old.checkRead(file);
    	}

//    	super.checkRead(file);
    }

    public void checkRead(String file, Object context) {
    	if(old != null){
    		old.checkRead(file, context);
    	}
    	
//    	super.checkRead(file, context);
    }

    public void checkWrite(FileDescriptor fd) {
    	if(old != null){
    		old.checkWrite(fd);
    	}
    	
//    	super.checkWrite(fd);
    }

    public void checkWrite(String file) {
    	if(old != null){
    		old.checkWrite(file);
    	}
    	
//    	super.checkWrite(file);
    }

    public void checkDelete(String file) {
    	if(old != null){
    		old.checkDelete(file);
    	}
    	
//    	super.checkDelete(file);
    }

    public void checkConnect(String host, int port) {
    	if(old != null){
    		old.checkConnect(host, port);
    	}
    	
    	super.checkConnect(host, port);
    }

    public void checkConnect(String host, int port, Object context) {
    	if(old != null){
    		old.checkConnect(host, port, context);
    	}
    	
    	super.checkConnect(host, port, context);
    }

    public void checkListen(int port) {
    	if(old != null){
    		old.checkListen(port);
    	}
    	
    	super.checkListen(port);
    }

    public void checkAccept(String host, int port) {
    	if(old != null){
    		old.checkAccept(host, port);
    	}
    	
    	super.checkAccept(host, port);
    }

    public void checkMulticast(InetAddress maddr) {
    	if(old != null){
    		old.checkMulticast(maddr);
    	}
    	
    	super.checkMulticast(maddr);
    }

    public void checkMulticast(InetAddress maddr, byte ttl) {
    	if(old != null){
    		old.checkMulticast(maddr, ttl);
    	}
    	
    	super.checkMulticast(maddr, ttl);
    }

    public void checkPropertiesAccess() {
    	if(old != null){
    		old.checkPropertiesAccess();
    	}
    	
//    	super.checkPropertiesAccess();//否则导致异常
    }

    public void checkPropertyAccess(String key) {
    	if(old != null){
    		old.checkPropertyAccess(key);
    	}
    	
    	super.checkPropertyAccess(key);
    }

    public boolean checkTopLevelWindow(Object window) {
    	boolean out = false;
    	if(old != null){
    		out = old.checkTopLevelWindow(window);
    	}
    	return out || super.checkTopLevelWindow(window);
    }

    public void checkPrintJobAccess() {
    	if(old != null){
    		old.checkPrintJobAccess();
    	}
    	
    	super.checkPrintJobAccess();
    }

    public void checkSystemClipboardAccess() {
    	if(old != null){
    		old.checkSystemClipboardAccess();
    	}
    	
    	super.checkSystemClipboardAccess();
    }

    public void checkAwtEventQueueAccess() {
    	if(old != null){
    		old.checkAwtEventQueueAccess();
    	}
    	
    	super.checkAwtEventQueueAccess();
    }

    private static String[] getPackages(String p) {
    	Class[] paraTypes = {String.class};
    	Object[] para = {p};
    	
    	Object out = ClassUtil.invoke(SecurityManager.class, SecurityManager.class, "getPackages", paraTypes, para, true);//静态方法，不用old，而用SecurityManager.class
    	if(out != null && out instanceof String[]){
    		return (String[])out;
    	}
    	
    	String[] back = {};
    	return back;
    }

    public void checkPackageAccess(String pkg) {
    	if(old != null){
    		old.checkPackageAccess(pkg);
    	}
    	
    	super.checkPackageAccess(pkg);
    }

    public void checkPackageDefinition(String pkg) {
    	if(old != null){
    		old.checkPackageDefinition(pkg);
    	}
    	
    	super.checkPackageDefinition(pkg);
    }

    public void checkSetFactory() {
    	if(old != null){
    		old.checkSetFactory();
    	}
    	
    	super.checkSetFactory();
    }

    public void checkMemberAccess(Class<?> clazz, int which) {
    	if(old != null){
    		old.checkMemberAccess(clazz, which);
    	}
    	
//    	super.checkMemberAccess(clazz, which);
    }

    public void checkSecurityAccess(String target) {
    	if(old != null){
    		old.checkSecurityAccess(target);
    	}
    	
//    	super.checkSecurityAccess(target);
    }

    public ThreadGroup getThreadGroup() {
    	if(old != null){
    		return old.getThreadGroup();
    	}
    	return Thread.currentThread().getThreadGroup();
    }
}

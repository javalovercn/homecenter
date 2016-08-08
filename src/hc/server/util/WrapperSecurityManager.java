package hc.server.util;

import hc.util.ClassUtil;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;

public class WrapperSecurityManager extends SecurityManager {
	private final SecurityManager old;
	private final Class[] nullParaTypes = {};
	private final Object[] nullParas = {};
	
	public WrapperSecurityManager(final SecurityManager sm){
		old = sm;
	}
	
    private boolean hasAllPermission(){
    	if(old != null){
    		final Object out = ClassUtil.invoke(SecurityManager.class, old, "hasAllPermission", nullParaTypes, nullParas, true);
            if(out != null && out instanceof Boolean){
            	return ((Boolean)out);
            }
    	}
    	
        return true;
    }

    @Override
	public final boolean getInCheck() {
    	if(old == null){
    		return false;
    	}
    	
        return old.getInCheck();
    }

    @Override
	protected final Class[] getClassContext(){
    	return super.getClassContext();
//    	if(old != null){
//	    	Object out = ClassUtil.invoke(SecurityManager.class, old, "getClassContext", nullParaTypes, nullParas, true);
//	    	if(out != null && out instanceof Class[]){
//	    		return (Class[])out;
//	    	}
//    	}
//    	Class[] back = {};
//    	return back;
    }

    @Override
	protected final ClassLoader currentClassLoader()
    {
    	if(old != null){
	        final Object out = ClassUtil.invoke(SecurityManager.class, old, "currentClassLoader", nullParaTypes, nullParas, true);
	        if(out != null && out instanceof ClassLoader){
	        	return (ClassLoader)out;
	        }
    	}
    	
		return null;
    }

    @Override
	protected final Class<?> currentLoadedClass() {
    	if(old != null){
	    	final Object out = ClassUtil.invoke(SecurityManager.class, old, "currentLoadedClass", nullParaTypes, nullParas, true);
	        if(out != null && out instanceof Class<?>){
	        	return (Class<?>)out;
	        }
    	}
    	
		return null;
    }

    @Override
	protected final int classDepth(final String name){
    	if(old != null){
	    	final Class[] paraTypes = {String.class};
	    	final Object[] para = {name};
	    	
	    	final Object out = ClassUtil.invoke(SecurityManager.class, old, "classDepth", paraTypes, para, true);
	        if(out != null && out instanceof Integer){
	        	return (Integer)out;
	        }
    	}
    	
		return 0;
    }

    @Override
	protected final int classLoaderDepth()
    {
    	if(old != null){
	    	final Object out = ClassUtil.invoke(SecurityManager.class, old, "classLoaderDepth", nullParaTypes, nullParas, true);
	        if(out != null && out instanceof Integer){
	        	return (Integer)out;
	        }
    	}
    	
    	return 0;
    }

    @Override
	protected final boolean inClass(final String name) {
    	if(old != null){
	    	final Class[] paraTypes = {String.class};
	    	final Object[] para = {name};
	    	
	    	final Object out = ClassUtil.invoke(SecurityManager.class, old, "inClass", paraTypes, para, true);
	        if(out != null && out instanceof Boolean){
	        	return (Boolean)out;
	        }
    	}
    		
    	return false;
    }

    @Override
	protected final boolean inClassLoader() {
    	if(old != null){
	    	final Object out = ClassUtil.invoke(SecurityManager.class, old, "inClassLoader", nullParaTypes, nullParas, true);
	    	if(out != null && out instanceof Boolean){
	        	return (Boolean)out;
	        }
    	}
    		
    	return false;
    }

    @Override
	public final Object getSecurityContext() {
    	if(old != null){
    		final Object securityContext = old.getSecurityContext();
    		if(securityContext != null){
    			return securityContext;
    		}
    	}
    		
    	return null;
    }

    @Override
	public void checkPermission(final Permission perm) {
    	if(old != null){
    		old.checkPermission(perm);
    	}
    		
//    	super.checkPermission(perm);//否则导致异常
    }

    @Override
	public void checkPermission(final Permission perm, final Object context) {
    	if(old != null){
    		old.checkPermission(perm, context);
    	}
    		
//    	super.checkPermission(perm, context);//否则导致异常
    }

    @Override
	public void checkCreateClassLoader() {
    	if(old != null){
    		old.checkCreateClassLoader();
    	}
    		
    	super.checkCreateClassLoader();
    }

    @Override
	public void checkAccess(final Thread t) {
    	if(old != null){
    		old.checkAccess(t);
    	}
    		
    	super.checkAccess(t);
    }
    
    @Override
	public void checkAccess(final ThreadGroup g) {
    	if(old != null){
    		old.checkAccess(g);
    	}
    		
//    	super.checkAccess(g);//否则导致异常
    }

    @Override
	public void checkExit(final int status) {
    	if(old != null){
    		old.checkExit(status);
    	}
    		
//    	super.checkExit(status);
    }

    @Override
	public void checkExec(final String cmd) {
    	if(old != null){
    		old.checkExec(cmd);
    	}
    	
//    	super.checkExec(cmd);
    }

    @Override
	public void checkLink(final String lib) {
    	if(old != null){
    		old.checkLink(lib);
    	}
    		
//    	super.checkLink(lib);
    }

    @Override
	public void checkRead(final FileDescriptor fd) {
    	if(old != null){
    		old.checkRead(fd);
    	}
    	
//    	super.checkRead(fd);
    }

    @Override
	public void checkRead(final String file) {
    	if(old != null){
    		old.checkRead(file);
    	}

//    	super.checkRead(file);
    }

    @Override
	public void checkRead(final String file, final Object context) {
    	if(old != null){
    		old.checkRead(file, context);
    	}
    	
//    	super.checkRead(file, context);
    }

    @Override
	public void checkWrite(final FileDescriptor fd) {
    	if(old != null){
    		old.checkWrite(fd);
    	}
    	
//    	super.checkWrite(fd);
    }

    @Override
	public void checkWrite(final String file) {
    	if(old != null){
    		old.checkWrite(file);
    	}
    	
//    	super.checkWrite(file);
    }

    @Override
	public void checkDelete(final String file) {
    	if(old != null){
    		old.checkDelete(file);
    	}
    	
//    	super.checkDelete(file);
    }

    @Override
	public void checkConnect(final String host, final int port) {
    	if(old != null){
    		old.checkConnect(host, port);
    	}
    	
    	super.checkConnect(host, port);
    }

    @Override
	public void checkConnect(final String host, final int port, final Object context) {
    	if(old != null){
    		old.checkConnect(host, port, context);
    	}
    	
    	super.checkConnect(host, port, context);
    }

    @Override
	public void checkListen(final int port) {
    	if(old != null){
    		old.checkListen(port);
    	}
    	
    	super.checkListen(port);
    }

    @Override
	public void checkAccept(final String host, final int port) {
    	if(old != null){
    		old.checkAccept(host, port);
    	}
    	
    	super.checkAccept(host, port);
    }

    @Override
	public void checkMulticast(final InetAddress maddr) {
    	if(old != null){
    		old.checkMulticast(maddr);
    	}
    	
    	super.checkMulticast(maddr);
    }

    @Override
	public void checkMulticast(final InetAddress maddr, final byte ttl) {
    	if(old != null){
    		old.checkMulticast(maddr, ttl);
    	}
    	
    	super.checkMulticast(maddr, ttl);
    }

    @Override
	public void checkPropertiesAccess() {
    	if(old != null){
    		old.checkPropertiesAccess();
    	}
    	
//    	super.checkPropertiesAccess();//否则导致异常
    }

    @Override
	public void checkPropertyAccess(final String key) {
    	if(old != null){
    		old.checkPropertyAccess(key);
    	}
    	
    	super.checkPropertyAccess(key);
    }

    @Override
	public boolean checkTopLevelWindow(final Object window) {
    	boolean out = false;
    	if(old != null){
    		out = old.checkTopLevelWindow(window);
    	}
    	return out || super.checkTopLevelWindow(window);
    }

    @Override
	public void checkPrintJobAccess() {
    	if(old != null){
    		old.checkPrintJobAccess();
    	}
    	
    	super.checkPrintJobAccess();
    }

    @Override
	public void checkSystemClipboardAccess() {
    	if(old != null){
    		old.checkSystemClipboardAccess();
    	}
    	
    	super.checkSystemClipboardAccess();
    }

    @Override
	public void checkAwtEventQueueAccess() {
    	if(old != null){
    		old.checkAwtEventQueueAccess();
    	}
    	
    	super.checkAwtEventQueueAccess();
    }

    private static String[] getPackages(final String p) {
    	final Class[] paraTypes = {String.class};
    	final Object[] para = {p};
    	
    	final Object out = ClassUtil.invoke(SecurityManager.class, SecurityManager.class, "getPackages", paraTypes, para, true);//静态方法，不用old，而用SecurityManager.class
    	if(out != null && out instanceof String[]){
    		return (String[])out;
    	}
    	
    	final String[] back = {};
    	return back;
    }

    @Override
	public void checkPackageAccess(final String pkg) {
    	if(old != null){
    		old.checkPackageAccess(pkg);
    	}
    	
    	super.checkPackageAccess(pkg);
    }

    @Override
	public void checkPackageDefinition(final String pkg) {
    	if(old != null){
    		old.checkPackageDefinition(pkg);
    	}
    	
    	super.checkPackageDefinition(pkg);
    }

    @Override
	public void checkSetFactory() {
    	if(old != null){
    		old.checkSetFactory();
    	}
    	
    	super.checkSetFactory();
    }

    @Override
	public void checkMemberAccess(final Class<?> clazz, final int which) {
    	if(old != null){
    		old.checkMemberAccess(clazz, which);
    	}
    	
//    	super.checkMemberAccess(clazz, which);
    }

    @Override
	public void checkSecurityAccess(final String target) {
    	if(old != null){
    		old.checkSecurityAccess(target);
    	}
    	
//    	super.checkSecurityAccess(target);
    }

    @Override
	public ThreadGroup getThreadGroup() {
    	if(old != null){
    		return old.getThreadGroup();
    	}
    	return Thread.currentThread().getThreadGroup();
    }
}

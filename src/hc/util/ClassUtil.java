package hc.util;

import hc.App;
import hc.core.ContextManager;
import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.server.HCException;

import java.awt.Component;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;

public class ClassUtil {
	public static final Class[] nullParaTypes = {};
	public static final Object[] nullParas = {};
	final static float jreVersion = App.getJREVer();
	
	public final static void revalidate(final Component comp){
		if(jreVersion >= 1.7){
			invoke(Component.class, comp, "revalidate", nullParaTypes, nullParas, true);
		}
	}
	
	public final static void changeField(final Class clazz, final Object obj, final String name, final Object value) throws Exception{
		try{
		    final Field field = clazz.getDeclaredField(name);
		    field.setAccessible(true);
		    field.set(obj, value);
		    field.setAccessible(false);
		}catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
			throw new HCException(e.toString());
		}
	}

	public final static void changeParentToNull(final ThreadGroup threadGroup){
		try{
			ClassUtil.changeField(ThreadGroup.class, threadGroup, "parent", null);
		}catch (final Exception e) {
			ContextManager.getThreadPool().run(new Runnable() {
				@Override
				public void run() {//重要，请勿在Event线程中调用，
					App.showOptionDialog(null, "Fail to modify ThreadGroup.parent to null", "JVM Error");
				}
			});
		}
	}
	
	public static final Object NO_MATCH_FIELD = new Object();

	/**
	 * 
	 * @param clazz
	 * @param obj
	 * @param matchClass
	 * @param needModiAccessible
	 * @param isReportException
	 * @return if not matched, then return {@link ClassUtil#NO_MATCH_FIELD}
	 */
	public final static Object getFieldMatch(final Class clazz, final Object obj, final Class matchClass, final boolean needModiAccessible, final boolean isReportException) {
		try{
		    final Field[] fields = clazz.getDeclaredFields();
		    
		    Field field = null;
		    for (int i = 0; i < fields.length; i++) {
				final Field f = fields[i];
				if(f.getType() == matchClass){
					field = f;
					break;
				}
			}
		    
		    if(field == null){
		    	return NO_MATCH_FIELD;
		    }
		    
		    if(needModiAccessible){
		    	field.setAccessible(true);
		    }
		    final Object out = field.get(obj);
		    if(needModiAccessible){
		    	field.setAccessible(false);
		    }
		    return out;
		}catch (final Exception e) {
			if(isReportException){
				ExceptionReporter.printStackTrace(e);
			}
		}
		return null;
	}
	
	public final static Object getFieldMatch(final Class clazz, final Object obj, final String hideClassName, final boolean needModiAccessible, final boolean isReportException) {
		try{
		    final Field[] fields = clazz.getDeclaredFields();
		    
		    Field field = null;
		    for (int i = 0; i < fields.length; i++) {
				final Field f = fields[i];
				if(f.getType().getCanonicalName() == hideClassName){
					field = f;
					break;
				}
			}
		    
		    if(field == null){
		    	return NO_MATCH_FIELD;
		    }
		    
		    if(needModiAccessible){
		    	field.setAccessible(true);
		    }
		    final Object out = field.get(obj);
		    if(needModiAccessible){
		    	field.setAccessible(false);
		    }
		    return out;
		}catch (final Exception e) {
			if(isReportException){
				ExceptionReporter.printStackTrace(e);
			}
		}
		return null;
	}
	
	public final static Object getField(final Class clazz, final Object obj, final String name, final boolean needModiAccessible, final boolean isReportException) {
		try{
		    final Field field = clazz.getDeclaredField(name);
		    if(needModiAccessible){
		    	field.setAccessible(true);
		    }
		    final Object out = field.get(obj);
		    if(needModiAccessible){
		    	field.setAccessible(false);
		    }
		    return out;
		}catch (final Exception e) {
			if(isReportException){
				ExceptionReporter.printStackTrace(e);
			}
		}
		return null;
	}

	public static final Object construct(final String clazName, final Class[] paraTypes, final Object[] para, final boolean needModiAccessible) throws Exception{
		final Class clas = Class.forName(clazName);
		final Constructor c = clas.getDeclaredConstructor(paraTypes);
		return c.newInstance(para);
	}
	
	public static final Object invoke(final Class claz, final Object obj, final String methodName, final Class[] paraTypes, final Object[] para, final boolean needModiAccessible){
		try{
		    return invokeWithExceptionOut(claz, obj, methodName, paraTypes,
					para, needModiAccessible);
		}catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}

	public static final Object invokeWithExceptionOut(final Class claz,
			final Object obj, final String methodName, final Class[] paraTypes,
			final Object[] para, final boolean needModiAccessible) throws Exception{
		final Method addPath = claz.getDeclaredMethod(methodName, paraTypes);
		if(needModiAccessible){
			addPath.setAccessible(true);
		}
		final Object out = addPath.invoke(obj, para);
		if(needModiAccessible){
			addPath.setAccessible(false);
		}
		return out;
	}
	
	public static void printCurrentThreadStack(final String lineInfo, final String delLine){
		printCurrentThreadStack(lineInfo, delLine, false);
	}
	
	public static void printCurrentThreadStack(final String lineInfo, final String delLine, final boolean isError){
		final StringBuilder sb = StringBuilderCacher.getFree();
		sb.append(lineInfo);
		sb.append("\n");
		
		final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		final int size = ste.length;
		boolean searchDelLine = (delLine==null||delLine.length()==0)?true:false;
		for (int i = 0; i < size; i++) {
			final String stackLine = ste[i].toString();
			
			if(searchDelLine == false){
				if(stackLine.indexOf(delLine) > 0){
					searchDelLine = true;
				}
				continue;
			}
			sb.append("\tat : ");
			sb.append(stackLine);
			sb.append("\n");
		}
		
		if(isError){
			LogManager.errToLog(sb.toString());
		}else{
			L.V = L.O ? false : LogManager.log(sb.toString());
		}
		
		StringBuilderCacher.cycle(sb);
	}

	public static void printCurrentThreadStack(final String lineInfo){
		printCurrentThreadStack(lineInfo, false);
	}
	
	public static void printCurrentThreadStack(final String lineInfo, final boolean isError){
		printCurrentThreadStack(lineInfo, "", isError);
	}

	public static void printThreadStack(final String name) {
		final StringBuilder sb = StringBuilderCacher.getFree();
		if(name != null){
			sb.append("\n");
		}else{
			sb.append("\n------------------------------------All Non-Daemon StackTraces---------------------------------\n");
		}
		final Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
		final Iterator<Thread> it = map.keySet().iterator();
		while(it.hasNext()){
			final Thread t = it.next();
			final String threadName = t.getName();
			final boolean isDaemon = t.isDaemon();
			if((name == null || name.equals(threadName)) && isDaemon == false){
				sb.append("--------------- Thread Name : ");
				sb.append(t.toString());
//				sb.append("@:");
//				sb.append(Integer.toHexString(t.hashCode()));//非final，
				sb.append(", isDaemon : ");
				sb.append(isDaemon);
				sb.append("--------------------\n");
				final StackTraceElement[] ste = map.get(t);
				final int size = ste.length;
				for (int i = 0; i < size; i++) {
					sb.append("\tat : ");
					sb.append(ste[i]);
					sb.append("\n");
				}
			}
		}
		
		L.V = L.O ? false : LogManager.log(sb.toString());
		StringBuilderCacher.cycle(sb);
	}

}

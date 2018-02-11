package third.hc.server.util.lucene;

import java.lang.reflect.Method;

public class MethodHandle {
	public static final Object[] nullParas = {};
	
	final Method method;
	
	public MethodHandle(final Method method){
		method.setAccessible(true);
		this.method = method;
	}
	
	public final Object invokeExact(Object instance) throws Throwable{
		return method.invoke(instance, nullParas);
	}
	
}

package hc.server.ui.design.code;

import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.AbstractMap;

public class AutoJRubyObject {
	public final Class javaClass;
	public final String methodName;
	public final Class returnType;
	
	public AutoJRubyObject(final Class jClass, final String method, final Class rt){
		this.javaClass = jClass;
		this.methodName = method;
		this.returnType = rt;
	}
	
	final static AutoJRubyObject[] list = init();
	final static int size = list.length;
	
	private static AutoJRubyObject[] init(){
		final AutoJRubyObject[] result = {
				new AutoJRubyObject(AbstractMap.class, "values", RubyHelper.JRUBY_ARRAY_CLASS)
				};
		return result;
	}
	
	/**
	 * 自动将Java返回对象，转换为JRuby对象
	 * @param method
	 * @return
	 */
	static Type replace(final Method method){
		final Class javaClass = method.getDeclaringClass();
		for (int i = 0; i < size; i++) {
			final AutoJRubyObject item = list[i];
			if(item.methodName.equals(method.getName()) && CodeHelper.isExtendsOrImplements(javaClass, item.javaClass)){
				final Type[] typeVar = {new TypeVariable() {
					@Override
					public Type[] getBounds() {
						return null;
					}

					@Override
					public GenericDeclaration getGenericDeclaration() {
						return null;
					}

					@Override
					public String getName() {
						return "V";
					}
				}};
				return new HCParameterizedType(typeVar, item.returnType, null);
			}
		}
		final Type rType = method.getGenericReturnType();
		return rType;
	}
}
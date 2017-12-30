package hc.server.ui.design.code;

import hc.util.StringBuilderCacher;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Vector;

import org.jrubyparser.ast.ArrayNode;
import org.jrubyparser.ast.ClassNode;
import org.jrubyparser.ast.Node;

public class ReturnType {
	private final Type type;
	public Node superNode;
	public ArrayNode structDefArrayNode;
	public ArrayNode structInstArrayNode;
	public boolean isDefClass;
	public Vector<ClassNode> extChains;
	
	public final void addClassDefNode(final ClassNode classNode){
		if(extChains == null){
			extChains = new Vector<ClassNode>(2);
		}
		isDefClass = true;
		extChains.add(classNode);
	}
	
	public final Object[] toClassDefNodeArray(){
		return extChains.toArray();
	}
	
	public ReturnType(Type baseType){
		baseType = RubyHelper.j2seClassToRubyClass(baseType);
		this.type = baseType;
	}

	public final Type getArrayRawType(){
		return getArrayRowTypeImpl(type);
	}
	
	@Override
	public final String toString(){
		return type.toString();
	}

	private final Type getArrayRowTypeImpl(final Type type) {
		if(type instanceof GenericArrayType){
			final GenericArrayType gat = (GenericArrayType)type;
			return getArrayRowTypeImpl(gat.getGenericComponentType());
		}else{
			return type;
		}
	}
	
	/**
	 * true means GenericArray或一般Array
	 * @return
	 */
	public final boolean isGenericArrayType(){
		if (type instanceof GenericArrayType){
			return true;
		}else if(type instanceof Class){
			return ((Class)type).isArray();
		}else{
			return false;
		}
	}
	
	public final Type getType(){
		return type;
	}
	
	public final Class getRawClass(){
		return getRawClass(type);
	}
	
	private static final Class getRawClass(final Type type){
		if(type instanceof Class){
			return (Class)type;
		}else if(type instanceof TypeVariable){
			return Object.class;
		}else if(type instanceof ParameterizedType){
			return (Class)((ParameterizedType)type).getRawType();
		}else if(type instanceof WildcardType){
			final Type[] ub = ((WildcardType)type).getUpperBounds();
			if(ub.length > 0){
				return getRawClass(ub[0]);
			}
		}
		return Object.class;
	}
	
	public final Field getField(final String name) throws NoSuchFieldException, SecurityException {
		return getRawClass().getField(name);
	}
	
	public final boolean isEquals(final Class claz){
		return getRawClass() == claz;
	}
	
	public final ReturnType getSuperclass(){
		return new ReturnType(getRawClass().getGenericSuperclass());
	}
	
	public final ReturnType getReturnType(final Method method){
//		final Type rType = method.getGenericReturnType();
		final Type rType = AutoJRubyObject.replace(method);
		
		if(rType instanceof TypeVariable){//E
			final String name = ((TypeVariable) rType).getName();
			final Class rawClass = getRawClass(type);
			return new ReturnType(matchTypeDeepToSuper(name, rawClass, type));
		}else if(rType instanceof ParameterizedType){
//				final ParameterizedType pt = searchParameterizedTypeFromSuperOrInterfaces(type, fromClass);
//				final HCParameterizedType hcpt = new HCParameterizedType(pt.getActualTypeArguments(), ((ParameterizedType) rType).getActualTypeArguments()[0]);
				final ParameterizedType pt = replace(method, (ParameterizedType)rType, type);
				return new ReturnType(pt);
		}
		return new ReturnType(rType);
	}
	
	private final ParameterizedType replace(final Method method, final ParameterizedType rType, final Type type){
		final Type rawType = rType.getRawType();
		ParameterizedType out = null;
		if(rawType != null){
			out = searchParameterizedTypeFromSuperOrInterfaces(type, (Class)rawType);
		}
		if(out != null){
			return new HCParameterizedType(out.getActualTypeArguments(), rawType, null);
		}
		
		final Type ownType = rType.getOwnerType();
		if(ownType != null){
			out = searchParameterizedTypeFromSuperOrInterfaces(type, (Class)ownType);
		}
		if(out != null){
			return new HCParameterizedType(out.getActualTypeArguments(), ownType, null);
		}
		
		final Type[] aca = rType.getActualTypeArguments();
		for (int i = 0; i < aca.length; i++) {
			final Type acaItem = aca[i];
			if(acaItem instanceof ParameterizedType){
				final ParameterizedType acapt = (ParameterizedType)acaItem;
				out = replace(null, acapt, type);
				if(out != null){
					aca[i] = new HCParameterizedType(out.getActualTypeArguments(), acapt.getRawType(), acapt.getOwnerType());
				}
			}else if(acaItem instanceof TypeVariable){
				final String varName = ((TypeVariable) acaItem).getName();
				final Class fromClass = method.getDeclaringClass();
				aca[i] = matchTypeDeepToSuper(varName, fromClass, type);
			}
		}
		return new HCParameterizedType(aca, rawType, ownType);
	}
	
	private final ParameterizedType searchParameterizedTypeFromSuperOrInterfaces(final Type searchClass, final Class targetClass){
		ParameterizedType out;
		
		if(searchClass instanceof Class){
			out = searchParameterizedTypeFromSuperOrInterfaces(((Class)searchClass).getGenericSuperclass(), targetClass);
			if(out != null){
				return out;
			}
			final Type[] types = ((Class)searchClass).getGenericInterfaces();
			for (int i = 0; i < types.length; i++) {
				out = searchParameterizedTypeFromSuperOrInterfaces(types[i], targetClass);
				if(out != null){
					return out;
				}
			}
		}
		
		if(searchClass instanceof ParameterizedType){
			final ParameterizedType pt = (ParameterizedType)searchClass;
			if(pt.getRawType() == targetClass || CodeHelper.isExtendsOrImplements(pt.getRawType(), targetClass)){
				return pt;
			}
		}
		
		return null;
	}
	
	private final TypeVariable hasTypeVar(final ParameterizedType pt1){
		final Type[] actualTypes = pt1.getActualTypeArguments();
		for (int i = 0; i < actualTypes.length; i++) {
			final Type oneType = actualTypes[i];
			if(oneType instanceof TypeVariable){
				return (TypeVariable)oneType;
			}
		}
		return null;
	}

	private static final Type matchTypeDeepToSuper(final String name, final Class rawClass, final Type type) {
		Class deepClass = null;
		if(type instanceof ParameterizedType){
			final Type rawType = ((ParameterizedType) type).getRawType();
			if(rawType == rawClass){
				final TypeVariable[] tv = rawClass.getTypeParameters();
				for (int i = 0; i < tv.length; i++) {
					final TypeVariable next = tv[i];
					if(next.getName().equals(name)){
						return ((ParameterizedType)type).getActualTypeArguments()[i];
					}
				}
			}
			if(rawType instanceof Class){
				deepClass = (Class)rawType;
			}
		}else if(type instanceof Class){
			deepClass = (Class)type;
		}
		
		if(deepClass != null){
			final Type superClass = deepClass.getGenericSuperclass();
			if(superClass != null){
				final Type out = matchTypeDeepToSuper(name, rawClass, superClass);
				if(out != null){
					return out;
				}
			}
			final Type[] interfaces = rawClass.getGenericInterfaces();
			for (int i = 0; i < interfaces.length; i++) {
				final Type item = interfaces[i];
				final Type out = matchTypeDeepToSuper(name, rawClass, item);
				if(out != null){
					return out;
				}
			}
		}
		return null;
	}
	
	public static String getGenericReturnTypeDesc(final Type type){
		if(type instanceof Class){
			return ((Class)type).getSimpleName();
		}else if(type instanceof ParameterizedType){
			final ParameterizedType pType = (ParameterizedType)type;
			final Type[] innerParas = pType.getActualTypeArguments();
			final StringBuilder sb = StringBuilderCacher.getFree();
			sb.append(getGenericReturnTypeDesc(pType.getRawType()));
			sb.append("<");
			for (int i = 0; i < innerParas.length; i++) {
				if(i > 0){
					sb.append(", ");
				}
				sb.append(getGenericReturnTypeDesc(innerParas[i]));
			}
			sb.append(">");
			final String result = sb.toString();
			StringBuilderCacher.cycle(sb);
			return result;
		}else if(type instanceof TypeVariable){
			return ((TypeVariable)type).getName();
		}else if(type instanceof WildcardType){
			final WildcardType wt = (WildcardType)type;
			final Type[] ub = wt.getUpperBounds();
			final Type[] lb = wt.getLowerBounds();
			if(type.toString().indexOf("super") > 0){
				return "? super " + getGenericReturnTypeDesc(lb[0]);
			}else{
				return "? extends " + getGenericReturnTypeDesc(ub[0]);
			}
		}else if(type instanceof GenericArrayType){
			final GenericArrayType arrType = (GenericArrayType)type;
			return getGenericReturnTypeDesc(arrType.getGenericComponentType()) + "[]";
		}else{
			return "?";//CodeHelper.OBJECT_STR;
		}
	}
	
	public final Method[] getMethods(){
		return getRawClass().getMethods();
	}
	
	public final Method getMethod(final String name, final Class[] para) throws NoSuchMethodException, SecurityException {
		return getRawClass().getMethod(name, para);
	}
	
	public final Method[] getDeclaredMethods(){
		return getRawClass().getDeclaredMethods();
	}
	
	public final Class[] getInterfaces(){
		return getRawClass().getInterfaces();
	}
}

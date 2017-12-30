package hc.server.ui.design.code;

import java.util.Vector;

import org.jrubyparser.ast.CallNode;
import org.jrubyparser.ast.IterNode;
import org.jrubyparser.ast.SClassNode;

public class JRubyClassDesc {
	private final ReturnType type;
	
	public JRubyClassDesc(final ReturnType type){
		this.type = type;
	}
	
	public final boolean isJRubyStruct(){
		return type.getType() == RubyHelper.JRUBY_STRUCT_CLASS;
	}
	
	public final Class getClassForDoc(){
		if(type != null){
			return type.getRawClass();
		}else{
			return null;
		}
	}
	
	public final Class getClassForSuper(){
		if(type != null){
			return type.getRawClass();
		}else{
			return null;
		}
	}
	
	public final Class getClassForParameter(){
		if(type != null){
			return type.getRawClass();
		}else{
			return null;
		}
	}
	
	public final ReturnType getReturnType(){
		return type;
	}
	
	@Override
	public final String toString(){
		return type.toString() + ", isInstance : " + isInstance;
	}
	
	public boolean isInstance;
	public boolean isInExtend;

	public IterNode defIterNode;
	public Vector<Class> include;//implements;
	
	public final boolean hasExtChain(){
		return type.isDefClass;
	}
	
	public final void appendInterface(final Class claz){
		if(include == null){
			include = new Vector<Class>(2);
		}
		include.add(claz);
	}
	
	public SClassNode defSClassNode;
	public CallNode innerDefNode;//to getIter
}

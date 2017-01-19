package hc.server.ui.design.code;

import java.util.Vector;

import org.jrubyparser.ast.CallNode;
import org.jrubyparser.ast.ClassNode;
import org.jrubyparser.ast.SClassNode;

public class JRubyClassDesc {
	public JRubyClassDesc(){
	}
	
	public Class baseClass;
	public boolean isInstance;
	public boolean isInExtend;
	public boolean isDefClass;
	public ClassNode defNode;
	public Vector<Class> include;//implements;
	
	public final void appendInterface(final Class claz){
		if(include == null){
			include = new Vector<Class>(2);
		}
		include.add(claz);
	}
	
	public SClassNode defSClassNode;
	public CallNode innerDefNode;//to getIter
}

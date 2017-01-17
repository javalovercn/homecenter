package hc.server.ui.design.code;

import org.jrubyparser.ast.CallNode;
import org.jrubyparser.ast.ClassNode;

public class JRubyClassDesc {
	public JRubyClassDesc(){
	}
	
	public Class baseClass;
	public boolean isInstance;
	public boolean isInExtend;
	public boolean isDefClass;
	public ClassNode defNode;
	public CallNode innerDefNode;//to getIter
}

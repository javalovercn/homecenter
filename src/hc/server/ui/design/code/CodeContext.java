package hc.server.ui.design.code;

import java.util.List;

import org.jrubyparser.ast.ClassNode;
import org.jrubyparser.ast.DefnNode;
import org.jrubyparser.ast.Node;

public class CodeContext {
	public final CodeHelper codeHelper;
	
	/**
	 * 
	 * @param contextNode
	 * @param rowIdx 光标所在行号
	 */
	public CodeContext(final CodeHelper codeHelper, final Node contextNode, final int rowIdx){
		this.codeHelper = codeHelper;
		this.contextNode = contextNode;
		this.rowIdx = rowIdx;
		
		this.bottomNode = searchBottomNode(contextNode, rowIdx);
	}
	
	/**
	 * 如果不是在defClass内，返回null
	 * @return
	 */
	public final ClassNode getDefClassNode(){
		final JRubyClassDesc desc = getDefJRubyClassDesc();
		return (desc==null)?null:desc.defNode;
	}
	
	public final JRubyClassDesc getDefJRubyClassDesc(){
		if(isSearchedDefClass == false){
			jrubyClassDesc = CodeHelper.isInDefClass(contextNode, this, rowIdx);
			isSearchedDefClass = true;
		}
		return jrubyClassDesc;
	}
	
	private final Node searchBottomNode(final Node startNode, final int rowIdxAtScript){
		if(startNode == null){
			return null;
		}
		
		final List<Node> list = startNode.childNodes();
		if(list == null){
			return null;
		}
		final int size = list.size();
		for (int i = size - 1; i >= 0; i--) {
			final Node sub = list.get(i);
			if(sub instanceof ClassNode){
				final ClassNode classNode = (ClassNode)sub;
				final Node body = classNode.getBody();
				if(body == null){
					return classNode;
				}else{
					return searchBottomNode(body, rowIdxAtScript);
				}
			}else if(sub instanceof DefnNode){
				final DefnNode defNode = (DefnNode)sub;
				final Node body = defNode.getBody();
				if(body == null){
					return defNode;
				}else{
					return searchBottomNode(body, rowIdxAtScript);
				}
			}
			if(i > 0 && sub.getPosition().getStartLine() >= rowIdxAtScript){//必须>=，否则当前编辑行会置于bottom，可能导致循环。比如i=100\ni=100+i<edit>
				continue;
			}else{
				final Node deepSub = searchBottomNode(sub, rowIdxAtScript);
				if(deepSub == null){
					return sub;
				}else{
					return deepSub;
				}
			}
		}
		return null;
	}
	public Node contextNode;
	private boolean isSearchedDefClass = false;
	private JRubyClassDesc jrubyClassDesc;
	public Node bottomNode;
	public int rowIdx;
}

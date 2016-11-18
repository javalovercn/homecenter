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
	 * @param scriptIdx 光标所在index
	 * @param rowIdxAtScript 光标所在行号
	 */
	public CodeContext(final CodeHelper codeHelper, final Node contextNode, final int scriptIdx, final int rowIdxAtScript){
		this.codeHelper = codeHelper;
		this.contextNode = contextNode;
		this.scriptIdx = scriptIdx;
		this.rowIdxAtScript = rowIdxAtScript;
		
		this.bottomNode = searchBottomNode(contextNode, rowIdxAtScript);
	}
	
	/**
	 * 如果不是在defClass内，返回null
	 * @return
	 */
	public final ClassNode getDefClassNode(){
		if(isSearchedDefClass == false){
			defClassNode = CodeHelper.isInDefClass(contextNode, rowIdxAtScript);
			isSearchedDefClass = true;
		}
		return defClassNode;
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
				return searchBottomNode(classNode.getBody(), rowIdxAtScript);
			}else if(sub instanceof DefnNode){
				final DefnNode defNode = (DefnNode)sub;
				return searchBottomNode(defNode.getBody(), rowIdxAtScript);
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
	private ClassNode defClassNode;
	public Node bottomNode;
	public int scriptIdx;
	public int rowIdxAtScript;
}

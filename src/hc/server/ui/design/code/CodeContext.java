package hc.server.ui.design.code;

import java.util.List;

import org.jrubyparser.ast.ClassNode;
import org.jrubyparser.ast.DefnNode;
import org.jrubyparser.ast.Node;

public class CodeContext {
	public final CodeHelper codeHelper;
	public boolean isParseScripts;
	/**
	 * 
	 * @param contextNode
	 * @param rowIdx 光标所在行号
	 */
	public CodeContext(final CodeHelper codeHelper, final Node contextNode, final int rowIdx){
		this(codeHelper, contextNode, rowIdx, false);
	}
	
	public CodeContext(final CodeHelper codeHelper, final Node contextNode, final int rowIdx, final boolean isParseScripts){
		this.codeHelper = codeHelper;
		this.contextNode = contextNode;
		this.rowIdx = rowIdx;
		this.isParseScripts = isParseScripts;
		this.bottomNode = searchBottomNode(contextNode, rowIdx, false);
	}
	
	/**
	 * import Java::hc.server.util.json.JSONObject
	 * $aa = JSONObject.new()
	 * 
	 * $aa = $aa.put("aa", "")
	 * $str = "World"
	 * $str = $str
	 * $i = 100
	 * $aa.put($str, "").<edit>
	 * 获得某个变量Node的上一行Node，以免进入循环search。
	 * @param node
	 * @return
	 */
	public final Node getUpperRowNode(final Node node){
		int searchRow = getUpperRowIdx(node);
		if(isParseScripts && searchRow == -1){//-1表示isParseScripts段的上一行即为-1
			searchRow = rowIdx - 1;
		}
		return searchBottomNode(contextNode, searchRow, true);
	}

	private final int getUpperRowIdx(final Node node) {
		return node.getPosition().getStartLine() - 1;
	}
	
	/**
	 * 如果不是在defClass内，返回null
	 * @return
	 */
	public final ClassNode getDefClassNode(){
		final JRubyClassDesc desc = getDefJRubyClassDesc();
		return (desc==null)?null:desc.defNode;//注意：有可能返回null，但desc.baseClass不为null
	}
	
	public final JRubyClassDesc getDefJRubyClassDesc(){
		if(isSearchedDefClass == false){
			jrubyClassDesc = CodeHelper.isInDefClass(contextNode, this, rowIdx);
			isSearchedDefClass = true;
		}
		return jrubyClassDesc;
	}
	
	private final Node searchBottomNode(final Node startNode, final int rowIdxAtScript, final boolean isForRow){
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
					return searchBottomNode(body, rowIdxAtScript, isForRow);
				}
			}else if(sub instanceof DefnNode){
				final DefnNode defNode = (DefnNode)sub;
				final Node body = defNode.getBody();
				if(body == null){
					return defNode;
				}else{
					return searchBottomNode(body, rowIdxAtScript, isForRow);
				}
			}
			
			if(isForRow && 
					((sub.getPosition().getEndLine() < rowIdxAtScript)
					||
					(sub.getPosition().getStartLine() == rowIdxAtScript))){//有可能跨多行且当前编辑行初始为空，所以不能使用&& sub.getPosition().getEndLine() == rowIdxAtScript + 1，比如：a=1\n\na = 1 + a<edit>
				return sub;
			}
			
			if(i > 0 && sub.getPosition().getStartLine() >= rowIdxAtScript){//必须>=，否则当前编辑行会置于bottom，可能导致循环。比如i=100\ni=100+i<edit>
				continue;
			}else{
				final Node deepSub = searchBottomNode(sub, rowIdxAtScript, isForRow);
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
	Node bottomNode;
	public int rowIdx;
}

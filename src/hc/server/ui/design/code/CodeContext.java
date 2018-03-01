package hc.server.ui.design.code;

import java.util.List;
import java.util.Vector;

import org.jrubyparser.SourcePosition;
import org.jrubyparser.ast.ClassNode;
import org.jrubyparser.ast.MethodDefNode;
import org.jrubyparser.ast.Node;
import org.jrubyparser.ast.TrueNode;

public class CodeContext {
	public final CodeHelper codeHelper;
	public boolean isParseScripts;
	public Vector<MethodDefNode> appendDefMethods = new Vector<MethodDefNode>(8);

	/**
	 * 
	 * @param contextNode
	 * @param rowIdx
	 *            光标所在行号
	 */
	public CodeContext(final CodeHelper codeHelper, final Node contextNode, final int rowIdx) {
		this(codeHelper, contextNode, rowIdx, false, true);
	}

	public CodeContext(final boolean isForRow, final CodeHelper codeHelper, final Node contextNode,
			final int rowIdx) {
		this(codeHelper, contextNode, rowIdx, false, isForRow);
	}

	public CodeContext(final CodeHelper codeHelper, final Node contextNode, final int rowIdx,
			final boolean isParseScripts) {
		this(codeHelper, contextNode, rowIdx, isParseScripts, false);
	}

	public CodeContext(final CodeHelper codeHelper, final Node contextNode, final int rowIdx,
			final boolean isParseScripts, final boolean isForRow) {
		this.codeHelper = codeHelper;
		this.contextNode = contextNode;
		this.rowIdx = rowIdx;
		this.isParseScripts = isParseScripts;
		this.bottomNode = serachLimitBottomNode(isForRow, rowIdx);
	}

	private final Node serachLimitBottomNode(final boolean isForRow, final int rowIdx) {
		Node out = searchBottomNode(contextNode, rowIdx, isForRow);
		if (out == null) {
			return contextNode;
		}
		if (isForRow && out.getPosition().getStartLine() > rowIdx) {
			out = getUpperRowNode(out, rowIdx);
		}
		return out;
	}

	/**
	 * import Java::hc.server.util.json.JSONObject $aa = JSONObject.new()
	 * 
	 * $aa = $aa.put("aa", "") $str = "World" $str = $str $i = 100 $aa.put($str,
	 * "").<edit> 获得某个变量Node的上一行Node，以免进入循环search。
	 * 
	 * @param node
	 * @return
	 */
	public final Node getUpperRowNode(final Node node) {
		return getUpperRowNode(node.getParent(), getUpperRowIdx(node));
	}

	private final Node getUpperRowNode(final Node node, final int rowIdx) {
		if (node.getPosition().getStartLine() > rowIdx) {
			final Node parent = node.getParent();
			if (parent == null) {
				try {
					return serachLimitBottomNode(true, rowIdx);
				} catch (final Throwable e) {
					return new TrueNode(new SourcePosition("", rowIdx, rowIdx));
				}
			} else {
				return getUpperRowNode(parent, rowIdx);// 临时构造结点，可能parent=null
			}
		}
		final List<Node> childs = node.childNodes();
		final int size = childs.size();
		for (int i = size - 1; i >= 0; i--) {
			final Node sub = childs.get(i);
			if (sub.getPosition().getStartLine() > rowIdx) {
				continue;
			} else {
				return sub;
			}
		}
		return node;
	}

	private final int getUpperRowIdx(final Node node) {
		return node.getPosition().getStartLine() - 1;
	}

	/**
	 * 如果不是在defClass内，返回null
	 * 
	 * @return
	 */
	public final Vector<ClassNode> getDefClassNode() {
		final JRubyClassDesc desc = getDefJRubyClassDesc();
		return (desc == null) ? null : desc.getReturnType().extChains;// 注意：有可能返回null，但desc.baseClass不为null
	}

	/**
	 * 注意不是全部继承链
	 * 
	 * @return
	 */
	public final ClassNode getTopDefClassNode() {
		final JRubyClassDesc desc = getDefJRubyClassDesc();
		if (desc == null) {
			return null;
		} else {
			final Vector<ClassNode> array = desc.getReturnType().extChains;
			if (array == null) {
				return null;
			} else {
				return array.elementAt(array.size() - 1);
			}
		}
	}

	public final JRubyClassDesc getDefJRubyClassDesc() {
		if (isSearchedDefClass == false) {
			jrubyClassDesc = CodeHelper.isInDefClass(contextNode, this, rowIdx);
			isSearchedDefClass = true;
		}
		return jrubyClassDesc;
	}

	private final Node searchBottomNode(final Node startNode, final int rowIdxAtScript,
			final boolean isForRow) {
		if (startNode == null) {
			return null;
		}

		final List<Node> list = startNode.childNodes();
		if (list == null) {
			return null;
		}
		final int size = list.size();
		for (int i = size - 1; i >= 0; i--) {
			final Node sub = list.get(i);
			if (sub instanceof ClassNode) {
				final ClassNode classNode = (ClassNode) sub;
				final Node body = classNode.getBody();
				if (body == null) {
					return classNode;
				} else {
					return searchBottomNode(body, rowIdxAtScript, isForRow);
				}
			} else if (sub instanceof MethodDefNode) {
				final MethodDefNode defNode = (MethodDefNode) sub;
				final Node body = defNode.getBody();
				if (body == null) {
					return defNode;
				} else {
					return searchBottomNode(body, rowIdxAtScript, isForRow);
				}
			}

			if (isForRow && ((sub.getPosition().getEndLine() < rowIdxAtScript)
					|| (sub.getPosition().getStartLine() == rowIdxAtScript))) {// 有可能跨多行且当前编辑行初始为空，所以不能使用&&
																				// sub.getPosition().getEndLine()
																				// ==
																				// rowIdxAtScript
																				// +
																				// 1，比如：a=1\n\na
																				// =
																				// 1
																				// +
																				// a<edit>
				final Node deepSub = searchBottomNode(sub, rowIdxAtScript, isForRow);
				if (deepSub == null) {
					return sub;
				} else {
					return deepSub;
				}
			}

			if (i > 0 && sub.getPosition().getStartLine() >= rowIdxAtScript) {// 必须>=，否则当前编辑行会置于bottom，可能导致循环。比如i=100\ni=100+i<edit>
				continue;
			} else {
				final Node deepSub = searchBottomNode(sub, rowIdxAtScript, isForRow);
				if (deepSub == null) {
					return sub;
				} else {
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

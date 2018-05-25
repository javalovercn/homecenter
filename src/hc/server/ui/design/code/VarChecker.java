package hc.server.ui.design.code;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.jrubyparser.CompatVersion;
import org.jrubyparser.Parser;
import org.jrubyparser.ast.ArgsNode;
import org.jrubyparser.ast.ArrayNode;
import org.jrubyparser.ast.AssignableNode;
import org.jrubyparser.ast.CallNode;
import org.jrubyparser.ast.ClassNode;
import org.jrubyparser.ast.DefnNode;
import org.jrubyparser.ast.GlobalAsgnNode;
import org.jrubyparser.ast.IClassVariable;
import org.jrubyparser.ast.IGlobalVariable;
import org.jrubyparser.ast.IInstanceVariable;
import org.jrubyparser.ast.ILocalVariable;
import org.jrubyparser.ast.InstAsgnNode;
import org.jrubyparser.ast.InstVarNode;
import org.jrubyparser.ast.ListNode;
import org.jrubyparser.ast.MethodDefNode;
import org.jrubyparser.ast.NewlineNode;
import org.jrubyparser.ast.Node;
import org.jrubyparser.ast.VCallNode;
import org.jrubyparser.parser.ParserConfiguration;

import hc.core.L;
import hc.core.util.LogManager;
import hc.server.ui.design.engine.HCJRubyEngine;

public class VarChecker {
	final ArrayList<Node> result = new ArrayList<Node>(8);
	final HashMap<ClassNode, ClassNodeInfoForChecker> classNodeInstanceMap = new HashMap<ClassNode, ClassNodeInfoForChecker>(2);
	final ArrayList<DefnNode> scriptLevelDefMethods = new ArrayList<DefnNode>(8);
	
	public final ArrayList<Node> check(final Node root) {
		result.clear();
		classNodeInstanceMap.clear();
		scriptLevelDefMethods.clear();
		
		scanScriptLevelDefMethods(root);
		firstScanForDefInstanceOfClassNode(root);
		checkInstanceVar(root);
		checkVCallNode(root);
		
		return result;
	}
	
	private final void checkVCallNode(final Node parent) {
		final List<Node> childs = parent.childNodes();
		final int size = childs.size();
		for (int i = 0; i < size; i++) {
			final Node child = childs.get(i);
			if(child instanceof VCallNode) {
				final String name = ((VCallNode)child).getName();
				L.V = L.WShop ? false : LogManager.log("[CodeTip] VCallNode : " + name);

				if(parent instanceof CallNode) {
					if(size > 1) {
						final Node nextSecond = childs.get(1);
						if(nextSecond instanceof ListNode && ((nextSecond instanceof ArrayNode) == false)) {
							//排除 (CallNode:ProjectContext, (CallNode:ui, (CallNode:server, (VCallNode:hc), (ListNode)), (ListNode))
							continue;
						}
						//保留 (CallNode:+, (VCallNode:msgErr), (ArrayNode, (StrNode)))
					}
				}
				
				final ClassNode cNode = searchClassScopeNodeFromInstVarNode(child);
				if(cNode != null) {
					final ClassNodeInfoForChecker cni = classNodeInstanceMap.get(cNode);
					if(cni != null) {
						//去掉自定义且不带参数的方法(VCallNode:doFuncNoPara)
						if(cni.vcallNames.contains(name)) {
							L.V = L.WShop ? false : LogManager.log("[CodeTip] class defined method : " + name);
							continue;
						}
					}
				}else {
//					sb.append("def myFunc\n");
//					sb.append("end\n");
//					sb.append("b = a + myFunc + cErr\n");
					final int topLevelDefMethodSize = scriptLevelDefMethods.size();
					boolean isDefed = false;
					for (int j = 0; j < topLevelDefMethodSize; j++) {
						final DefnNode oneNode = scriptLevelDefMethods.get(j);
						if(name.equals(oneNode.getName()) 
								&& oneNode.getPosition().getStartLine() < child.getPosition().getStartLine()) {
							L.V = L.WShop ? false : LogManager.log("[CodeTip] top level defined method : " + name);
							isDefed = true;
							break;
						}
					}
					if(isDefed) {
						continue;
					}
				}
				addResultNode(child);
			}else{//Case VCallNode
				checkVCallNode(child);
			}
		}
	}
	
	private final void addResultNode(final Node n) {
		result.add(n);
	}
	
	private final void checkInstanceVar(final Node parent) {
		final List<Node> childs = parent.childNodes();
		final int size = childs.size();
		for (int i = 0; i < size; i++) {
			final Node child = childs.get(i);
			if (child instanceof IInstanceVariable) {
				if(child instanceof InstVarNode) {//IInstanceVariable : (InstVarNode:ctx)
					final ClassNode cNode = searchClassScopeNodeFromInstVarNode(child);
					if(cNode != null) {
						final ClassNodeInfoForChecker classNodeInfoForChecker = classNodeInstanceMap.get(cNode);
						if(classNodeInfoForChecker == null) {
							L.V = L.WShop ? false : LogManager.log("[CodeTip] checkInstanceVar undefined ClassNode : " + cNode);
							continue;
						}
						final ArrayList<String> instLists = classNodeInfoForChecker.defedInstVarNames;
						if(instLists != null) {
							final String instVarName = ((InstVarNode)child).getName();
							if(instLists.contains(instVarName)) {
								continue;
							}
						}
					}
					addResultNode(child);
					continue;
				}
			} else if (child instanceof IClassVariable) {
			} else if (child instanceof IGlobalVariable) {
			} else {
			}
			checkInstanceVar(child);
		}
	}

	private final ClassNode searchClassScopeNodeFromInstVarNode(Node child) {
		Node parent;
		while((parent = child.getParent()) != null) {
			if(parent instanceof ClassNode) {
				return (ClassNode)parent;
			}
			child = parent;
		};
		return null;
	}
	
	private final void scanScriptLevelDefMethods(final Node root) {//(NewlineNode, (DefnNode:myFunc, (MethodNameNode:myFunc), (ArgsNode)))
		final List<Node> rootBlock = root.childNodes();
		if(rootBlock.size() > 0) {
			final Node block = rootBlock.get(0);
			final List<Node> blockList = block.childNodes();
			final int size = blockList.size();
			for (int i = 0; i < size; i++) {
				final Node child = blockList.get(i);
				if(child instanceof NewlineNode) {
					final NewlineNode nln = (NewlineNode)child;
					final List<Node> nlnList = nln.childNodes();
					if(nlnList.size() > 0) {
						final Node defNode = nlnList.get(0);
						if(defNode instanceof DefnNode) {
							scriptLevelDefMethods.add((DefnNode)defNode);
						}
					}
				}
			}
		}
	}
	
	private final void firstScanForDefInstanceOfClassNode(final Node root) {
		final List<Node> childs = root.childNodes();
		final int size = childs.size();
		for (int i = 0; i < size; i++) {
			final Node child = childs.get(i);
			if(child instanceof ClassNode) {
				scanForClassNode((ClassNode)child);
			}else {
				firstScanForDefInstanceOfClassNode(child);
			}
		}
	}

	private final void scanForClassNode(final ClassNode cNode) {
		ClassNodeInfoForChecker cni = classNodeInstanceMap.get(cNode);
		if(cni == null) {
			cni = new ClassNodeInfoForChecker();
			classNodeInstanceMap.put(cNode, cni);
		}
		
		scanDefInstanceForClassNode(cNode, cni);
		scanVCallNamesForClassNode(cNode, cni);
	}
	
	final void scanVCallNamesForClassNode(final ClassNode cNode, final ClassNodeInfoForChecker cni) {
		final List<MethodDefNode> lists = cNode.getMethodDefs();
		final int methodSize = lists.size();

		//不带参数的VCall
		for (int j = 0; j < methodSize; j++) {
			final MethodDefNode aMethod = lists.get(j);
			final ArgsNode argsNode = aMethod.getArgs();
			if(argsNode.getMaxArgumentsCount() == 0) {
				cni.vcallNames.add(aMethod.getName());
			}
		}
		
		//不检索父类的不带参数
	}
	
	final void scanDefInstanceForClassNode(final ClassNode cNode, final ClassNodeInfoForChecker cni) {
		final List<MethodDefNode> lists = cNode.getMethodDefs();
		final int methodSize = lists.size();

		//从initialize中找
		for (int j = 0; j < methodSize; j++) {
			final MethodDefNode aMethod = lists.get(j);
			if (aMethod.getName().equals(CodeHelper.JRUBY_CLASS_INITIALIZE_DEF)) {
				searchVarDefInInitializeMethod(aMethod.getBody(), cni);
			}
		}
	}
	
	private final void searchVarDefInInitializeMethod(final Node methodBody, final ClassNodeInfoForChecker cni) {
		if (methodBody == null) {
			return;
		}
		
		if(methodBody instanceof ClassNode) {
			scanForClassNode((ClassNode)methodBody);
			return;
		}

		final List<Node> list = methodBody.childNodes();
		final int size = list.size();

		for (int i = 0; i < size; i++) {
			Node varDefNode = list.get(i);
			if (varDefNode instanceof AssignableNode) {
				do {
					String searchParaName;
					if (varDefNode instanceof InstAsgnNode) {
						searchParaName = ((InstAsgnNode) varDefNode).getName();
						if(cni.defedInstVarNames.contains(searchParaName) == false) {
							cni.defedInstVarNames.add(searchParaName);
						}
					} else if (varDefNode instanceof GlobalAsgnNode) {
						searchParaName = ((GlobalAsgnNode) varDefNode).getName();
					} else {
						break;
					}

					varDefNode = ((AssignableNode) varDefNode).getValue();
				} while (varDefNode != null && varDefNode instanceof AssignableNode);
				continue;
			}else if(varDefNode instanceof InstVarNode) {
				final InstVarNode ivn = (InstVarNode)varDefNode;
				if(cni.defedInstVarNames.contains(ivn.getName()) == false) {
					addResultNode(varDefNode);//在定义之前，被引用
				}
			} else {
				// 有可能位于嵌入中
				searchVarDefInInitializeMethod(varDefNode, cni);
			}
		}
	}
	
	public static void checkVar(final Node root, final Node node, final Vector<Node> result) {
		if(node instanceof ILocalVariable) {
			final ILocalVariable lv = (ILocalVariable)node;
			System.out.println("ILocalVariable : " + lv.toString());
			System.out.println("\tgetDefinedScope : " + lv.getDefinedScope().toString());
			System.out.println("\tgetDeclaration : " + lv.getDeclaration().toString());
		}else if(node instanceof IInstanceVariable) {
			final IInstanceVariable iv = (IInstanceVariable)node;
			System.out.println("IInstanceVariable : " + iv.toString());
			
//			final List<IInstanceVariable> list = IInstanceVariableVisitor.findOccurrencesIn(root, iv.getName());
//			final int size = list.size();
//			for (int i = 0; i < size; i++) {
//				System.out.println("\tIInstanceVariable : " + list.get(i).toString());
//			}
		}else if(node instanceof IGlobalVariable) {
			final IGlobalVariable gv = (IGlobalVariable)node;
			System.out.println("IGlobalVariable : " + gv.toString());
		}else if(node instanceof IClassVariable) {
			final IClassVariable cv = (IClassVariable)node;
			System.out.println("IClassVariable : " + cv.toString());
		}
		
		final List<Node> childs = node.childNodes();
		final int size = childs.size();
		for (int i = 0; i < size; i++) {
			checkVar(root, childs.get(i), result);
		}
	}
	
	public static void main(final String[] args) {
		final Node root = getTestScritpNode();
		checkScripts(root);
	}

	public static Node getTestScritpNode() {
		final StringBuilder sb = new StringBuilder(3300);
		sb.append("#encoding:utf-8\n");
		sb.append("\n");
		sb.append("import javax.swing.JLabel\n");
		sb.append("import hc.server.ui.ProjectContext\n");
		sb.append("import javax.swing.JButton\n");
		sb.append("import java.awt.BorderLayout\n");
		sb.append("import javax.swing.JPanel\n");
		sb.append("import java.awt.GridLayout\n");
		sb.append("import java.awt.Dimension\n");
		sb.append("import java.lang.Thread\n");
		sb.append("\n");
		sb.append("class MyQuesDialog < Java::hc.server.ui.Dialog\n");
		sb.append("	def initialize\n");
		sb.append("		super\n");
		sb.append("		@ctx.sendMovingMsg(\"choose YES in dialog\")\n");
		sb.append("		@ctx = getProjectContext()\n");
		sb.append("		@selfDialog = self\n");
		sb.append("		dimension = Dimension.new(dialogWidthErr, hc.server.ui.ProjectContext::MESSAGE_ERROR)\n");
		sb.append("		dimension = dimension + 1\n");
		sb.append("		\n");
		sb.append("		@okErr.addActionListener {|e|\n");
		sb.append("			@selfDialog.dismiss()\n");
		sb.append("			@ctx.sendMovingMsg(\"choose YES in dialog\")\n");
		sb.append("			@contextErr.sendMovingMsg(\"choose YES in dialog\")\n");
		sb.append("		}\n");
		sb.append("	end\n");
		sb.append("	def doFunc(msg)\n");
		sb.append("		@ctx.sendMovingMsg(msg + \"choose YES in dialog\")\n");
		sb.append("		doFunc(msg)\n");
		sb.append("		@contextErr.sendMovingMsg(msgErr + \"choose YES in dialog\")\n");
		sb.append("	end\n");
		sb.append("	def doFuncNoPara\n");
		sb.append("		@ctx.sendMovingMsg(doFuncNoPara + \"choose YES in dialog\")\n");
		sb.append("		doFuncNoPara\n");
		sb.append("	end\n");
		sb.append("end\n");
		sb.append("\n");
		sb.append("a = 100\n");
		sb.append("def myFunc\n");
		sb.append("end\n");
		sb.append("b = a + myFunc + cErr\n");
		
		final String scripts = sb.toString();
		
		final Parser rubyParser = new Parser();
		final ParserConfiguration config = new ParserConfiguration(0,
				CompatVersion.getVersionFromString(HCJRubyEngine.JRUBY_PARSE_VERSION));
		
		final StringReader in = new StringReader(scripts);
		final Node root = rubyParser.parse("<code>", in, config);
		return root;
	}
	
	private static void checkScripts(final Node root) {
		final Vector<Node> errUndefined = new Vector<Node>();
		checkVar(root, root, errUndefined);
		
		final int size = errUndefined.size();
		for (int i = 0; i < size; i++) {
			final Node errNode = errUndefined.elementAt(i);
			System.out.println();
		}
	}
}

class ClassNodeInfoForChecker {
	final ArrayList<String> defedInstVarNames = new ArrayList<String>(32);
	final ArrayList<String> vcallNames = new ArrayList<String>(16);
}

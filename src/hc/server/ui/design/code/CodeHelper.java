package hc.server.ui.design.code;

import hc.core.L;
import hc.server.data.KeyComperPanel;
import hc.server.data.StoreDirManager;
import hc.server.ui.design.hpj.HPShareJar;
import hc.server.ui.design.hpj.RubyExector;
import hc.server.ui.design.hpj.ScriptEditPanel;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

import java.awt.Event;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.script.ScriptException;
import javax.swing.JTextPane;
import javax.swing.text.Document;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jrubyparser.CompatVersion;
import org.jrubyparser.Parser;
import org.jrubyparser.SourcePosition;
import org.jrubyparser.ast.ArgsNode;
import org.jrubyparser.ast.ArgumentNode;
import org.jrubyparser.ast.ArrayNode;
import org.jrubyparser.ast.AssignableNode;
import org.jrubyparser.ast.BignumNode;
import org.jrubyparser.ast.CallNode;
import org.jrubyparser.ast.ClassNode;
import org.jrubyparser.ast.Colon2Node;
import org.jrubyparser.ast.ConstDeclNode;
import org.jrubyparser.ast.ConstNode;
import org.jrubyparser.ast.DAsgnNode;
import org.jrubyparser.ast.DefnNode;
import org.jrubyparser.ast.FCallNode;
import org.jrubyparser.ast.FixnumNode;
import org.jrubyparser.ast.FloatNode;
import org.jrubyparser.ast.GlobalAsgnNode;
import org.jrubyparser.ast.GlobalVarNode;
import org.jrubyparser.ast.ILiteralNode;
import org.jrubyparser.ast.InstAsgnNode;
import org.jrubyparser.ast.InstVarNode;
import org.jrubyparser.ast.ListNode;
import org.jrubyparser.ast.LocalAsgnNode;
import org.jrubyparser.ast.LocalVarNode;
import org.jrubyparser.ast.MethodDefNode;
import org.jrubyparser.ast.NewlineNode;
import org.jrubyparser.ast.NilNode;
import org.jrubyparser.ast.Node;
import org.jrubyparser.ast.StrNode;
import org.jrubyparser.ast.SymbolNode;
import org.jrubyparser.ast.VCallNode;
import org.jrubyparser.parser.ParserConfiguration;

public class CodeHelper {
	private static final String TO_S = "to_s";
	private static final String TO_F = "to_f";
	private static final String TO_I = "to_i";
	private static final String JRUBY_CLASS_INITIALIZE_DEF = "initialize";
	private static final String JRUBY_NEW = "new";
	private static final String JRUBY_CLASS_FOR_NEW = "Class";

	private final HashMap<String, String[]> addedLibClassesAndRes = new HashMap<String, String[]>();
	private final HashMap<String, CodeItem[]> classCacheMethodAndPropForClass = new HashMap<String, CodeItem[]>();
	private final HashMap<String, CodeItem[]> classCacheMethodAndPropForInstance = new HashMap<String, CodeItem[]>();
	
	private final CodeWindow window = new CodeWindow();
	public final ArrayList<CodeItem> out = new ArrayList<CodeItem>();

	public int wordCompletionModifyMaskCode;
	public int wordCompletionModifyCode;
	public int wordCompletionCode;
	public char wordCompletionChar;
	
	private final void buildMethodAndProp(final Class c, final boolean isForClass, final ArrayList<CodeItem> list, final boolean needNewMethod){
		if(c == null){
			return;
		}
		
		final Field[] allFields;
		if(isForClass){
			allFields = c.getFields();//public
		}else{
			allFields = c.getDeclaredFields();
		}
		final Method[] allMethods;
		if(isForClass){
			allMethods = c.getMethods();//public
		}else{
			allMethods = c.getDeclaredMethods();
		}
		
		final int fieldSize = allFields.length;
		final int methodSize = allMethods.length;

		if(c == String.class){
			addToMethod(list, TO_I, "int", String.class.getSimpleName());
			addToMethod(list, TO_F, "float", String.class.getSimpleName());
		}else if(c == int.class){
			addToMethod(list, TO_S, "String", "int");
		}else if(c == float.class){
			addToMethod(list, TO_S, "String", "float");
		}
		
		for (int i = 0; i < fieldSize; i++) {
			final Field field = allFields[i];
			final int modifiers = field.getModifiers();
			if(isForClass){
				if(Modifier.isStatic(modifiers) == false){
					continue;
				}
			}
			
			final boolean isPublic = Modifier.isPublic(modifiers);
			if(isPublic || Modifier.isProtected(modifiers)){
				final String fieldName = field.getName();
				
				//属性相同名，只保留最外层
				if(CodeItem.contains(list, fieldName)){
					continue;
				}
				
				final CodeItem item = CodeItem.getFree();
				item.code = fieldName;
				final Class<?> fieldClass = field.getDeclaringClass();
				item.fmClass = fieldClass.getName();
				item.codeDisplay = item.code + " : " + field.getType().getSimpleName() + " - " + fieldClass.getSimpleName();
				item.codeLowMatch = item.code.toLowerCase();
				item.isPublic = isPublic;
				item.isForMaoHaoOnly = Modifier.isStatic(modifiers);
				item.type = CodeItem.TYPE_FIELD;
				
				list.add(item);
			}
		}
		
		for (int i = 0; i < methodSize; i++) {
			final Method method = allMethods[i];
			final int modifiers = method.getModifiers();
			if(isForClass){
				if(Modifier.isStatic(modifiers) == false){
					continue;
				}
			}
			
			final boolean isPublic = Modifier.isPublic(modifiers);
			if(isPublic || Modifier.isProtected(modifiers)){
				final Class[] paras = method.getParameterTypes();
				String paraStr = "";
				for (int j = 0; j < paras.length; j++) {
					if(paraStr.length() > 0){
						paraStr += ", ";
					}
					paraStr += paras[j].getSimpleName();
				}
				
				final String codeMethodStr = method.getName() + (paraStr.length()==0?"()":"(" + paraStr + ")");
				
				final boolean findSameName = CodeItem.contains(list, codeMethodStr);
				if(findSameName){
					continue;
				}
				
				final CodeItem item = CodeItem.getFree();
				item.code = codeMethodStr;
				final Class<?> methodClass = method.getDeclaringClass();
				item.fmClass = methodClass.getName();
				item.codeDisplay = item.code + " : " + method.getReturnType().getSimpleName() + " - " + methodClass.getSimpleName();
				item.codeLowMatch = item.code.toLowerCase();
				
				item.isPublic = isPublic;
				item.isForMaoHaoOnly = false;//!Modifier.isStatic(modifiers);
				item.type = CodeItem.TYPE_METHOD;
				list.add(item);
			}
		}
		
		if(isForClass == false){
			final Class superclass = c.getSuperclass();
			if(superclass != null){
				buildMethodAndProp(superclass, isForClass, list, false);
			}
			final Class[] interfaces = c.getInterfaces();
			for (int i = 0; i < interfaces.length; i++) {
				buildMethodAndProp(interfaces[i], isForClass, list, false);
			}
		}else if(needNewMethod){
			final Constructor[] cons = c.getDeclaredConstructors();
			final int size = cons.length;
			if(size == 0){
				final CodeItem item = CodeItem.getFree();
				item.code = JRUBY_NEW + "()";
				item.codeDisplay = item.code + " - " + c.getSimpleName();
				item.codeLowMatch = item.code.toLowerCase();
				
				item.isPublic = true;
				item.isForMaoHaoOnly = false;
				item.type = CodeItem.TYPE_METHOD;
				list.add(item);
			}else{
				for (int i = 0; i < size; i++) {
					final Constructor con = cons[i];
					
					final Class[] paras = con.getParameterTypes();
					String paraStr = "";
					for (int j = 0; j < paras.length; j++) {
						if(paraStr.length() > 0){
							paraStr += ", ";
						}
						paraStr += paras[j].getSimpleName();
					}
					
					final CodeItem item = CodeItem.getFree();
					item.code = JRUBY_NEW + (paraStr.length()==0?"()":"(" + paraStr + ")");
					item.codeDisplay = item.code + " - " + c.getSimpleName();
					item.codeLowMatch = item.code.toLowerCase();
					
					item.isPublic = true;
					item.isForMaoHaoOnly = false;
					item.type = CodeItem.TYPE_METHOD;
					list.add(item);
				}
			}
		}
	}

	private void addToMethod(final ArrayList<CodeItem> list, final String methodName, final String resultType, final String baseClassName) {
		final CodeItem item = CodeItem.getFree();
		item.code = methodName + "()";
		item.fmClass = int.class.getName();
		item.codeDisplay = item.code + " : " + resultType + " - " + baseClassName;
		item.codeLowMatch = item.code.toLowerCase();
		item.isPublic = true;
		item.isForMaoHaoOnly = false;
		item.type = CodeItem.TYPE_METHOD;
		
		list.add(item);
	}

	private final void buildMethodAndField(final Class c, final JRubyClassDesc jcd, final boolean isForClass, final HashMap<String, CodeItem[]> set){
		final ArrayList<CodeItem> list = new ArrayList<CodeItem>();
		
		buildMethodAndProp(c, isForClass, list, true);
		
		//将定义体的类的常量和构造方法提取出来，装入代码提示表中
		if(jcd.defNode != null){
			
			final ClassNode classNode = jcd.defNode;
			final String className = getDefClassName(classNode);
			final Node body = classNode.getBody();
			final List<Node> listNodes = body.childNodes();
			final int size = listNodes.size();
			for (int i = 0; i < size; i++) {
				Node sub = listNodes.get(i);
				if(sub instanceof NewlineNode){
					sub = ((NewlineNode)sub).getNextNode();
				}
				if(sub == null){
					continue;
				}
				if(sub instanceof ConstDeclNode){
					final ConstDeclNode constNode = (ConstDeclNode)sub;
					
					final CodeItem item = CodeItem.getFree();
					item.code = constNode.getName();
					item.fmClass = Object.class.getName();
					item.codeDisplay = item.code + " : " + Object.class.getSimpleName() + " - " + className;
					item.codeLowMatch = item.code.toLowerCase();
					item.isPublic = true;
					item.isForMaoHaoOnly = false;
					item.type = CodeItem.TYPE_FIELD;
					
					list.add(item);
				}else if(sub instanceof DefnNode){
					final DefnNode defN = (DefnNode)sub;
					appendMethod(isForClass, className, defN, list);
				}
			}
		}
		
		final Object[] objs = list.toArray();
		final CodeItem[] out = new CodeItem[objs.length];
		System.arraycopy(objs, 0, out, 0, objs.length);
		Arrays.sort(out);
		
		if(jcd.defNode == null){
			final String className = c.getName();
			set.put(className, out);
		}else{
			set.put(getDefClassName(jcd.defNode), out);
		}
	}

	private final void appendMethod(final boolean isForClass, final String className, final DefnNode defN, final ArrayList<CodeItem> list) {
		String methodName = defN.getName();
		if(methodName.equals(JRUBY_CLASS_INITIALIZE_DEF)){
			methodName = JRUBY_NEW;
		}else{
			if(isForClass){
				return;
			}
		}
		
		final CodeItem item = CodeItem.getFree();
		
		final ArgsNode argsNode = defN.getArgs();
		final List<Node> paraList = argsNode.childNodes();
		final int parameterNum = paraList.size();
		
		final StringBuilder sb = new StringBuilder();
		if(parameterNum == 0){
			sb.append(methodName + "()");
		}else{
			sb.append(methodName);
			sb.append("(");

			final int parameterStartIdx = sb.length();

			final ArrayNode parametersNode = (ArrayNode)paraList.get(0);

			for (int j = 0; j < parameterNum; j++) {
				final ArgumentNode parameterNode = (ArgumentNode)parametersNode.get(j);
				final String name = parameterNode.getName();
				if(sb.length() > parameterStartIdx){
					sb.append(", ");
				}
				sb.append(name);
			}
			sb.append(")");
		}
		
		item.code = sb.toString();
		item.codeDisplay = item.code + " - " + className;
		item.codeLowMatch = item.code.toLowerCase();
		
		item.isPublic = true;
		item.isForMaoHaoOnly = false;//仅限new
		item.type = CodeItem.TYPE_METHOD;
		
		list.add(item);
	}

	private static String getDefClassName(final ClassNode classNode) {
		return classNode.getCPath().getName();
	}
	
	/**
	 * 为实例查找可用的方法和属性
	 * @param backgroundOrPreVar
	 * @param preName
	 * @param needPublic
	 * @param out
	 * @return
	 */
	public final ArrayList<CodeItem> getMethodAndFieldForInstance(final Class backgroundOrPreVar, final ClassNode defNode, 
			final JRubyClassDesc jdc, final boolean needPublic, final ArrayList<CodeItem> out){
		return getMethodAndField(backgroundOrPreVar, defNode, jdc, needPublic, out, classCacheMethodAndPropForInstance);
	}
	
	/**
	 * 为类查找可用的静态方法和属性
	 * @param c
	 * @param preName
	 * @param out
	 * @return
	 */
	public final ArrayList<CodeItem> getMethodAndFieldForClass(final Class c, final ClassNode defNode, final ArrayList<CodeItem> out){
		return getMethodAndField(c, defNode, null, true, out, classCacheMethodAndPropForClass);
	}
	
	private final ArrayList<CodeItem> getMethodAndField(final Class backgroundOrPreVar, final ClassNode defNode, 
			final JRubyClassDesc jdc, final boolean needPublic, final ArrayList<CodeItem> out, 
			final HashMap<String, CodeItem[]> set){
		freeArray(out);
		
		if(jdc != null && jdc.defNode != null){
			CodeItem[] methods = set.get(getDefClassName(jdc.defNode));
			if(methods == null && jdc != null){
				buildForClass(jdc);
				methods = set.get(getDefClassName(jdc.defNode));
			}
			if(methods != null){
				return appendToOut(methods, needPublic, out);
			}
		}
		
		//仅取定义体中的
		if(defNode != null){
			CodeItem[] methods = set.get(getDefClassName(defNode));
			if(methods == null && jdc != null){
				buildForClass(jdc);
				methods = set.get(getDefClassName(defNode));
			}
			if(methods != null){
				return appendToOut(methods, needPublic, out);
			}
		}
		
		final String className = backgroundOrPreVar.getName();
		
		CodeItem[] methods = set.get(className);
		if(methods == null){
			buildForClass(buildJRubyClassDesc(backgroundOrPreVar, false));
			methods = set.get(className);
			DocHelper.processDoc(backgroundOrPreVar, true);
		}
		
		return appendToOut(methods, needPublic, out);
	}

	private ArrayList<CodeItem> appendToOut(final CodeItem[] methods,
			final boolean needPublic, final ArrayList<CodeItem> out) {
		final int size = methods.length;
		for (int i = 0; i < size; i++) {
			final CodeItem tryMethod = methods[i];
			if(needPublic){
				if(tryMethod.isPublic){
				}else{
					continue;
				}
			}
			if(preCodeSplitIsDot){
				if(tryMethod.isForMaoHaoOnly){
					continue;
				}
			}
			{
				final CodeItem item = CodeItem.getFree();
				
				item.type = tryMethod.type;
				item.code = tryMethod.code;
				item.fmClass = tryMethod.fmClass;
				item.codeDisplay = tryMethod.codeDisplay;
				item.codeLowMatch = tryMethod.codeLowMatch;
				item.isPublic = tryMethod.isPublic;
				out.add(item);
			}
		}
		
		return out;
	}

	private final void buildForClass(final JRubyClassDesc jcd) {
		buildMethodAndField(jcd.baseClass, jcd, true, classCacheMethodAndPropForClass);
		buildMethodAndField(jcd.baseClass, jcd, false, classCacheMethodAndPropForInstance);
	}
	
	public static ArrayList<String> getRequireLibs(final Node node, final ArrayList<String> out){
		out.clear();
		
		final List<Node> childNodes = node.childNodes();
		if(childNodes.size() == 0){
			return out;
		}
		final List<Node> blockNode = childNodes.get(0).childNodes();
		final int size = blockNode.size();
		for (int i = 0; i < size; i++) {
			final Node cmdNode = blockNode.get(i).childNodes().get(0);
			if(cmdNode instanceof FCallNode){
				final FCallNode fCallNode = (FCallNode) cmdNode;
				if(fCallNode.getName().equals("require")){
					final Node args = fCallNode.getArgs();
					final Node firstArg = args.childNodes().get(0);
					if(firstArg instanceof StrNode){
						final String firstStr = ((StrNode) firstArg).getValue();
						if(firstStr.equals("java")){
						}else{
							out.add(firstStr);
						}
					}
				}
			}
		}
		return out;
	}
	
	public ArrayList<CodeItem> getSubPackageAndClasses(final ArrayList<CodeItem> out, final ArrayList<String> requireLibs, final boolean isJavaLimited, final boolean isAppend){
		if(isAppend == false){
			freeArray(out);
		}
		
		if(isJavaLimited){
			appendPackageAndClass(out, CodeStaticHelper.J2SE_CLASS_SET, CodeStaticHelper.J2SE_CLASS_SET_SIZE, false);
		}
		if(isJavaLimited == false){
			appendPackageAndClass(out, CodeStaticHelper.HC_CLASS_SET, CodeStaticHelper.HC_CLASS_SET_SIZE, false);
		}
		
		if(isJavaLimited == false && requireLibs != null){
			final int size = requireLibs.size();
			for (int i = 0; i < size; i++) {
				final String libName = requireLibs.get(i);
				final String[] classAndRes = addedLibClassesAndRes.get(libName);
				
//				if(classAndRes != null){//有可能添加后，又删除了库，但代码仍保留，不拦截此错误，脚本自动编译时，会产生错误提示
					appendPackageAndClass(out, classAndRes, classAndRes.length, false);
//				}else{
//					throw new Error("No found or removed lib : " + libName);
//				}
			}
		}
		
		Collections.sort(out);
		
		return out;
	}

	public final ArrayList<CodeItem> getResources(final ArrayList<CodeItem> out, final ArrayList<String> requireLibs, final boolean isAppend){
		if(isAppend == false){
			freeArray(out);
		}
		
		if(requireLibs != null){
			final int size = requireLibs.size();
			for (int i = 0; i < size; i++) {
				final String libName = requireLibs.get(i);
				final String[] classAndRes = addedLibClassesAndRes.get(libName);
				appendPackageAndClass(out, classAndRes, classAndRes.length, true);
			}
		}
		
		Collections.sort(out);
		
		return out;
	}

	public static void freeArray(final ArrayList<CodeItem> out) {
		final int out_size = out.size();
		for (int i = out_size - 1; i >= 0; i--) {
			CodeItem.cycle(out.get(i));
		}
		out.clear();
	}
	
	private static void appendPackageAndClass(final ArrayList<CodeItem> out, final String[] set, final int size, final boolean isResourceOnly) {
//		final int nextSplitIdx = preName.length();
		
		for (int i = 0; i < size; i++) {
			final String pkg = set[i];
			final int pathIdx = pkg.indexOf("/");
			if(pathIdx == 0){
				if(isResourceOnly){
					final CodeItem item = CodeItem.getFree();
					item.type = CodeItem.TYPE_RESOURCES;
					item.code = pkg;
					item.codeDisplay = pkg;
					item.codeLowMatch = pkg.toLowerCase();
					out.add(item);
				}
			}else if(isResourceOnly == false){
				final CodeItem item = CodeItem.getFree();
				item.type = CodeItem.TYPE_CLASS;
				item.code = pkg;
				item.codeDisplay = pkg;
				item.codeLowMatch = pkg.toLowerCase();
				out.add(item);
			}
		}
	}
	
	public final void loadLibToCodeHelper(final DefaultMutableTreeNode node){
		final HPShareJar jar = (HPShareJar)node.getUserObject();
		final byte[] fileContent = jar.content;
		final String fileShortName = jar.name;
		
		final String tmpFileName = ResourceUtil.createRandomFileNameWithExt(StoreDirManager.TEMP_DIR, StoreDirManager.HCTMP_EXT);
		final File tmpFile = new File(StoreDirManager.TEMP_DIR, tmpFileName);
		
		if(ResourceUtil.writeToFile(fileContent, tmpFile)){
			final ArrayList<String> classAndRes = J2SEClassBuilder.getClassAndResByJar(tmpFile, true);
			final String[] cr = CodeStaticHelper.convertArray(classAndRes);
			addedLibClassesAndRes.put(fileShortName, cr);
		}
				
		tmpFile.delete();
	}
	
	public final void loadLibForTest(final String libName){
		addedLibClassesAndRes.put(libName, null);
	}

//	private final String getLowerLibName(final String fileShortName) {
//		return fileShortName + EXT_LOWER;
//	}
	
	private final static String EXT_LOWER = "_lOwEr";
	
	public final void unloadLibFromCodeHelper(final String fileShortName){
		addedLibClassesAndRes.remove(fileShortName);
	}
	
	public CodeHelper(){
		initShortCutKeys();
		
		buildForClass(buildJRubyClassDesc(int.class, false));
		buildForClass(buildJRubyClassDesc(float.class, false));
	}

	/**
	 * loadNodeFromMap时调用一次
	 */
	public final void initCodeHelper(final DefaultMutableTreeNode jarFolder){
		initRequire(jarFolder);
	}

	public final void initShortCutKeys() {
		refreshShortCutKeys(getWordCompletionKeyChar(), getWordCompletionKeyText(), getWordCompletionModifierCode());
	}
	
	public final static ClassNode isInDefClass(final Node node, final int lineIdxAtScript){
		final List<Node> childNodes = node.childNodes();
		if(childNodes.size() > 0){
			final List<Node> list = childNodes.get(0).childNodes();
			final int size = list.size();
			for (int i = 0; i < size; i++) {
				final List<Node> childNodes2 = list.get(i).childNodes();
				if(childNodes2.size() == 0){
					continue;
				}
				final Node classNode = childNodes2.get(0);
				if(classNode instanceof ClassNode){
					final SourcePosition position = classNode.getPosition();
					final int startLine = position.getStartLine();
					final int endLine = position.getEndLine();
					if(startLine < lineIdxAtScript && lineIdxAtScript < endLine){
						return (ClassNode)classNode;
					}
				}
			}
		}
		return null;
	}
	
	public final static Class getDefSuperClass(final Node node, final int lineIdxAtScript){
		final ClassNode classNode = isInDefClass(node, lineIdxAtScript);
		if(classNode != null){
			return getDefSuperClass(classNode);
		}
		return null;
	}
	
	public final void refreshShortCutKeys(final String keyChar, final String key, final int modiCode){
		wordCompletionChar = keyChar.length()==0?0:keyChar.toCharArray()[0];
		wordCompletionCode = KeyComperPanel.getCharKeyCode(key.toCharArray()[0]);
		final int[] actionEvents = {Event.META_MASK, Event.CTRL_MASK, Event.ALT_MASK};
		final int[] keyEvents = {KeyEvent.VK_META, KeyEvent.VK_CONTROL, KeyEvent.VK_ALT};
		for (int i = 0; i < keyEvents.length; i++) {
			if(modiCode == keyEvents[i]){
				wordCompletionModifyMaskCode = actionEvents[i];
				break;
			}
		}
		wordCompletionModifyCode = modiCode;
	}
	
	public final void release(){
		window.release();
	}
	
	private final void initRequire(final DefaultMutableTreeNode jarFolder){
		final Enumeration enumeration = jarFolder.children();
		while(enumeration.hasMoreElements()){
			final DefaultMutableTreeNode node = (DefaultMutableTreeNode)enumeration.nextElement();
			loadLibToCodeHelper(node);
		}
	}
	
    public static Node parseScripts(final String cmds) {
        final Parser rubyParser = new Parser();
        final StringReader in = new StringReader(cmds);
        final CompatVersion version = CompatVersion.RUBY2_0;
        final ParserConfiguration config = new ParserConfiguration(0, version);
    	return rubyParser.parse("<code>", in, config);
    }
    
    private static final Class searchOverrideMethodParameterType(final Class claz, final String methodName, final int parameterIdx){
    	final Method[] methods = claz.getDeclaredMethods();
    	final int size = methods.length;
    	for (int i = 0; i < size; i++) {
			final Method method = methods[i];
			final int modifiers = method.getModifiers();
			if((modifiers & Modifier.STATIC) != 0){
				continue;
			}
			if(((modifiers & Modifier.PUBLIC) != 0) || ((modifiers & Modifier.PROTECTED) != 0)){
				if(method.getName().equals(methodName)){
					final Class[] paraTypes = method.getParameterTypes();
					if(paraTypes.length > parameterIdx){
						return paraTypes[parameterIdx];
					}
				}				
			}
		}
    	
    	final Class[] interfaces = claz.getInterfaces();
    	final int interfaceLen = interfaces.length;
    	for (int i = 0; i < interfaceLen; i++) {
			final Class out = searchOverrideMethodParameterType(interfaces[i], methodName, parameterIdx);
			if(out != null){
				return out;
			}
		}
    	
    	final Class superClaz = claz.getSuperclass();
    	if(superClaz != claz && superClaz != null){
    		return searchOverrideMethodParameterType(superClaz, methodName, parameterIdx);
    	}
    	return null;
    }
    
    /**
     * 没有找到，返回null
     * @param rootNode
     * @param parameter
     * @return
     */
    public final JRubyClassDesc findParaClass(final CodeContext contextNode, final String parameter, final int typeParameter){
    	final Node subNode = contextNode.bottomNode;//searchParaCallingNodeByIdx(contextNode.contextNode, contextNode.scriptIdx);
    	
    	if(subNode != null){
    		final ClassNode defClassNode = contextNode.getDefClassNode();
			final Node varDefNode = findVarDefNode(subNode, parameter, typeParameter, null, false, defClassNode != null);
    		if(varDefNode != null){
    			if(varDefNode instanceof AssignableNode){
    				return findClassFromRightAssign(((AssignableNode)varDefNode).getValue(), contextNode);
    			}else if(varDefNode instanceof FCallNode){
    				final ArrayNode importArgs = (ArrayNode)((FCallNode)varDefNode).getArgs();
					final CallNode firstPackageNode = (CallNode)importArgs.childNodes().get(0);
					final String className = buildFullClassNameFromReceiverNode(firstPackageNode);
					return buildJRubyClassDesc(findClassByName(className, true), false);
    			}else if(varDefNode instanceof ClassNode){
    				final JRubyClassDesc classDesc = buildJRubyClassDesc(getDefSuperClass((ClassNode)varDefNode), false);
					classDesc.isDefClass = true;
					classDesc.defNode = (ClassNode)varDefNode;
					
					buildForClass(classDesc);//直接在MyDefClass::进行代码提示要求
					return classDesc;
    			}else if(varDefNode instanceof DefnNode){//方法体定义
    				if(defClassNode != null){
        				final DefnNode defNode = (DefnNode)varDefNode;
        				final String methodName = defNode.getName();
        				final ArgsNode argsNode = defNode.getArgs();
        				final List<Node> list = argsNode.childNodes();
        				final ArrayNode parametersNode = (ArrayNode)list.get(0);
        				final int parameterNum = parametersNode.size();
        				int parameterIdx = -1;
        				for(int i = 0; i<parameterNum; i++){
        					final ArgumentNode parameterNode = (ArgumentNode)parametersNode.get(i);
        					final String name = parameterNode.getName();
        					if(name.equals(parameter)){
        						parameterIdx = i;
        						break;
        					}
        				}
        				
        				if(parameterIdx >= 0){
	        				final Class superClass = getDefSuperClass(defClassNode);
	        				final Class methodParaType = searchOverrideMethodParameterType(superClass, methodName, parameterIdx);
	        				if(methodParaType != null){
	        					return buildJRubyClassDesc(methodParaType, true);
	        				}
        				}
    				}
    				return buildJRubyClassDesc(Object.class, true);
    			}
    		}
    	}
    	
    	return null;
    }
    
    private static Node searchParaCallingNodeByIdx(final Node node, final int inputIdx){
    	if(node == null){
    		return null;
    	}
    	
    	if(node instanceof ClassNode){
    		final ClassNode clazNode = (ClassNode)node;
    		return searchParaCallingNodeByIdx(clazNode.getBody(), inputIdx);
    	}else if(node instanceof DefnNode){//方法定义
    		final DefnNode defNode = (DefnNode)node;
    		final Node out = searchParaCallingNodeByIdx(defNode.getBody(), inputIdx);
    		return ((out == null)?defNode:out);
    	}
		final List<Node> list = node.childNodes();
		final int size = list.size();
		
		if(size == 1){
			final SourcePosition position = node.getPosition();
			if(position.getEndOffset() < inputIdx || position.isWithin(inputIdx)){//如最后行是注释，可能导致endIdx < inputIdx
				return searchParaCallingNodeByIdx(list.get(0), inputIdx);
			}else{
				return node;
			}
		}else{
			for (int i = size - 1; i >= 0; i--) {
				final Node subNode = list.get(i);
				final SourcePosition position = subNode.getPosition();
				if(position.getEndOffset() < inputIdx || position.isWithin(inputIdx)){//如最后行是注释，可能导致endIdx < inputIdx
					return searchParaCallingNodeByIdx(subNode, inputIdx);
				}
			}
			
			return node;
		}
    }
    
    public final static int TYPE_VAR_UNKNOW = 1;
    public final static int TYPE_VAR_LOCAL = 1 << 1;
    public final static int TYPE_VAR_GLOBAL = 1 << 2;
    public final static int TYPE_VAR_INSTANCE = 1 << 3;
    
    private static Node findVarDefNode(final Node bottomNode, final String parameter, final int typeParameter, 
    		final Node fromNode, final boolean downForward, final boolean isInDefClass){
    	final Node parent = bottomNode.getParent();
    	if(parent == null){
    		return null;
    	}
    	
    	if(typeParameter == TYPE_VAR_LOCAL && parent instanceof DefnNode){//方法定义体
    		final DefnNode defNode = (DefnNode)parent;
			final ArgsNode argsNode = defNode.getArgs();
			final List<Node> list = argsNode.childNodes();
			if(list.size() > 0){
				final ArrayNode parametersNode = (ArrayNode)list.get(0);
				final int size = parametersNode.size();
				for(int i = 0; i<size; i++){
					final ArgumentNode parameterNode = (ArgumentNode)parametersNode.get(i);
					final String name = parameterNode.getName();
					
					if(name.equals(parameter)){
						return defNode;
					}
				}
			}
    	}
    	
    	final List<Node> list = parent.childNodes();
    	final int size = list.size();
    	int bottomIdx = size - 1;
    	for (int i = bottomIdx; i >= 0; i--) {
			if(list.get(i) == fromNode){
				bottomIdx = i;
				break;
			}
		}
    	for (int i = bottomIdx; i >= 0; i--) {
			final Node varDefNode = list.get(i);
			
			if(varDefNode instanceof AssignableNode){
				String searchParaName;
				if(typeParameter == TYPE_VAR_LOCAL && varDefNode instanceof LocalAsgnNode){
					searchParaName = ((LocalAsgnNode)varDefNode).getName();
				}else if(typeParameter == TYPE_VAR_LOCAL && varDefNode instanceof DAsgnNode){
					searchParaName = ((DAsgnNode)varDefNode).getName();
				}else if(typeParameter == TYPE_VAR_INSTANCE && varDefNode instanceof InstAsgnNode){
					searchParaName = ((InstAsgnNode)varDefNode).getName();
				}else if(typeParameter == TYPE_VAR_GLOBAL && varDefNode instanceof GlobalAsgnNode){
					searchParaName = ((GlobalAsgnNode)varDefNode).getName();
				}else if(varDefNode instanceof ConstDeclNode){
					searchParaName = ((ConstDeclNode)varDefNode).getName();
				}else{
					continue;
				}
				
				if(searchParaName.equals(parameter)){
					return varDefNode;
				}
			}else if(typeParameter == TYPE_VAR_LOCAL && varDefNode instanceof ClassNode){
				//class MyMlet < Java::hc.server.ui.Mlet\n end\n
				//(ClassNode, (Colon2ImplicitNode:MyMlet), (CallNode:Mlet, (CallNode:ui, (CallNode:server, (CallNode:hc, (ConstNode:Java), 
				final ClassNode cNode = (ClassNode)varDefNode;
				if(getDefClassName(cNode).equals(parameter)){
					return varDefNode;
				}
			}else if(varDefNode instanceof FCallNode){
				if(typeParameter == TYPE_VAR_LOCAL){
					final FCallNode callNode = (FCallNode)varDefNode;
					if(callNode.getName().equals("import")){
						final ArrayNode importArgs = (ArrayNode)callNode.getArgs();
						final CallNode firstPackageNode = (CallNode)importArgs.childNodes().get(0);
						if(firstPackageNode.getName().equals(parameter)){
							return varDefNode;
						}
					}
				}
				continue;
			}else if(varDefNode instanceof NewlineNode){
				final Node node = varDefNode.childNodes().get(0);
				if(node != fromNode){
					final Node aNode = findVarDefNode(node, parameter, typeParameter, null, true, isInDefClass);
					if(aNode != null){
						return aNode;
					}
				}
//			}else if(varDefNode instanceof LocalVarNode){
//				return findVarDefNode(varDefNode, ((LocalVarNode)varDefNode).getName(), TYPE_LOCAL_VAR, null, true);
//			}else if(varDefNode instanceof InstVarNode){
//				return findVarDefNode(varDefNode, ((InstVarNode)varDefNode).getName(), TYPE_INSTANCE_VAR, null, true);
//			}else if(varDefNode instanceof GlobalVarNode){
//				return findVarDefNode(varDefNode, ((GlobalVarNode)varDefNode).getName(), TYPE_GLOBAL_VAR, null, true);
			}else if(isInDefClass && (typeParameter == TYPE_VAR_INSTANCE || typeParameter == TYPE_VAR_GLOBAL) && varDefNode instanceof ClassNode){
				//(ClassNode, (Colon2ImplicitNode:MyClass), (BlockNode, (NewlineNode, (DefnNode:initialize, (MethodNameNode:initialize), (ArgsNode), 
				final ClassNode defClass = (ClassNode)varDefNode;
				final List<MethodDefNode> lists = defClass.getMethodDefs();
				final int methodSize = lists.size();
				
				//优先从initialize中找
				for (int j = 0; j < methodSize; j++) {
					final MethodDefNode aMethod = lists.get(j);
					if(aMethod.getName().equals(JRUBY_CLASS_INITIALIZE_DEF)){
						final Node defNode = searchVarDefInInitializeMethod(aMethod.getBody(), parameter, typeParameter);
						if(defNode != null){
							return defNode;
						}
						break;
					}
				}
				
				//从非initialize中找
				for (int j = 0; j < methodSize; j++) {
					final MethodDefNode aMethod = lists.get(j);
					if(aMethod.getName().equals(JRUBY_CLASS_INITIALIZE_DEF) == false){
						final Node defNode = searchVarDefInInitializeMethod(aMethod.getBody(), parameter, typeParameter);
						if(defNode != null){
							return defNode;
						}
					}
				}
			}
    	}
    	
    	if(downForward){
    		return null;
    	}else{
    		return findVarDefNode(parent, parameter, typeParameter, bottomNode, downForward, isInDefClass);
    	}
    }
    
    private static final Node searchVarDefInInitializeMethod(final Node methodBody, final String parameter, final int typeParameter){
    	final List<Node> list = methodBody.childNodes();
    	final int size = list.size();
    	for (int i = 0; i < size; i++) {
			final Node varDefNode = list.get(i);
			if(varDefNode instanceof AssignableNode){
				String searchParaName;
				if(typeParameter == TYPE_VAR_INSTANCE && varDefNode instanceof InstAsgnNode){
					searchParaName = ((InstAsgnNode)varDefNode).getName();
				}else if(typeParameter == TYPE_VAR_GLOBAL && varDefNode instanceof GlobalAsgnNode){
					searchParaName = ((GlobalAsgnNode)varDefNode).getName();
				}else{
					continue;
				}
				
				if(searchParaName.equals(parameter)){
					return varDefNode;
				}
			}else{
				//有可能位于嵌入中
				final Node result = searchVarDefInInitializeMethod(varDefNode, parameter, typeParameter);
				if(result != null){
					return result;
				}
			}
		}
    	return null;
    }
    
    private static final void appendVarDefInInitializeMethod(final Node methodBody, final int typeParameter, final ArrayList<CodeItem> out){
    	final List<Node> list = methodBody.childNodes();
    	final int size = list.size();
    	for (int i = 0; i < size; i++) {
			final Node varDefNode = list.get(i);
			if(varDefNode instanceof AssignableNode){
				String searchParaName;
				if(typeParameter == TYPE_VAR_INSTANCE && varDefNode instanceof InstAsgnNode){
					searchParaName = ((InstAsgnNode)varDefNode).getName();
				}else if(typeParameter == TYPE_VAR_GLOBAL && varDefNode instanceof GlobalAsgnNode){
					searchParaName = ((GlobalAsgnNode)varDefNode).getName();
				}else{
					continue;
				}
				
				if(CodeItem.contains(out, searchParaName)){
					continue;
				}
				
				final CodeItem item = CodeItem.getFree();
				
				item.type = CodeItem.TYPE_FIELD;
				item.code = searchParaName;
				item.codeDisplay = searchParaName;
				item.codeLowMatch = searchParaName.toLowerCase();
				item.isPublic = false;
				out.add(item);
			}else{
				//有可能位于嵌入中
				appendVarDefInInitializeMethod(varDefNode, typeParameter, out);
			}
		}
    }
    
    /**
     * 从应用变量作用域中提取可用变量
     */
    private static final void appendVarDefInScope(final Node execNode, final int typeParameter, final ArrayList<CodeItem> out){
    	final List<Node> list = execNode.childNodes();
    	final int size = list.size();
    	for (int i = 0; i < size; i++) {
			final Node varDefNode = list.get(i);
			if(varDefNode instanceof AssignableNode){
				String searchParaName;
				if(typeParameter == TYPE_VAR_INSTANCE && varDefNode instanceof InstAsgnNode){
					searchParaName = ((InstAsgnNode)varDefNode).getName();
				}else if(typeParameter == TYPE_VAR_GLOBAL && varDefNode instanceof GlobalAsgnNode){
					searchParaName = ((GlobalAsgnNode)varDefNode).getName();
				}else if(typeParameter == TYPE_VAR_LOCAL && varDefNode instanceof LocalAsgnNode){
					searchParaName = ((LocalAsgnNode)varDefNode).getName();
				}else{
					continue;
				}
				
				if(CodeItem.contains(out, searchParaName)){
					continue;
				}
				
				final CodeItem item = CodeItem.getFree();
				
				item.type = CodeItem.TYPE_FIELD;
				item.code = searchParaName;
				item.codeDisplay = searchParaName;
				item.codeLowMatch = searchParaName.toLowerCase();
				item.isPublic = false;
				out.add(item);
			}else{
				//有可能位于嵌入中
				appendVarDefInScope(varDefNode.getParent(), typeParameter, out);
			}
		}
    }
    
    private final JRubyClassDesc findClassFromRightAssign(final Node value, final CodeContext codeContext){
    	if(value instanceof CallNode){
    		return findClassFromCallNode((CallNode)value, codeContext);
    	}else if(value instanceof ILiteralNode){
    		return buildJRubyClassDesc(getClassFromLiteral(value), true);
    	}else if(value instanceof LocalVarNode){
    		final LocalVarNode lvn = (LocalVarNode)value;
    		final CodeContext newCodeCtx = new CodeContext(codeContext.contextNode, lvn.getPosition().getStartOffset(), lvn.getPosition().getStartLine());
    		return findParaClass(newCodeCtx, ((LocalVarNode)value).getName(), CodeHelper.TYPE_VAR_LOCAL);
    	}
    	
    	return findClassFromReceiverNode(value, true, codeContext);
    }
    
    private static final Class[] AUTO_BOX_CLASS1 = {boolean.class, byte.class, char.class, short.class, int.class, long.class, float.class, double.class};
    private static final Class[] AUTO_BOX_CLASS2 = {Boolean.class, Byte.class, Character.class, Short.class, Integer.class, Long.class, Float.class, Double.class};
    
    private static boolean isAutoBoxClass(final Class class1, final Class class2){
    	final int size = AUTO_BOX_CLASS1.length;
    	for (int i = 0; i < size; i++) {
			if(class1 == AUTO_BOX_CLASS1[i] && class2 == AUTO_BOX_CLASS2[i]){
				return true;
			}
		}
    	
    	for (int i = 0; i < size; i++) {
			if(class1 == AUTO_BOX_CLASS2[i] && class2 == AUTO_BOX_CLASS1[i]){
				return true;
			}
		}
    	
    	return false;
    }

	private final JRubyClassDesc findClassFromCallNode(final CallNode call, final CodeContext codeContext) {
		final String methodName = call.getName();
		
		//可能 
		//1.以属性访问的方式 
		//2.方法
		//来创建实例
		if(methodName.equals(JRUBY_NEW)){
			final JRubyClassDesc classDesc = findClassFromReceiverNode(call.getReceiver(), false, codeContext);
			if(classDesc != null){
				classDesc.isInstance = true;
			}
			return classDesc;
		}else if(methodName.equals(TO_S)){
			return buildJRubyClassDesc(String.class, true);
		}else if(methodName.equals(TO_I)){
			return buildJRubyClassDesc(int.class, true);
		}else if(methodName.equals(TO_F)){
			return buildJRubyClassDesc(float.class, true);
		}
		
		final Node parametersNode = call.getArgs();
		
		if(parametersNode instanceof ArrayNode){//方法或+-*/
			//(CallNode:to_java, (ArrayNode, (StrNode), (StrNode)), (ArrayNode, (SymbolNode:string))) ==> {"", ""}
			if(methodName.equals("to_java")){
				final ArrayNode arrayNode = (ArrayNode)parametersNode;
				final SymbolNode synNode = (SymbolNode)arrayNode.get(0);//[(SymbolNode:string)]
				final String name = synNode.getName();
				if(name.equals("string")){
					return buildJRubyClassDesc(String[].class, true);
				}else if(name.equals("int")){
					return buildJRubyClassDesc(int[].class, true);
				}else if(name.equals("float")){
					return buildJRubyClassDesc(float[].class, true);
				}
				return buildJRubyClassDesc(Object[].class, true);
			}
			if(methodName.equals("+") || methodName.equals("-") || methodName.equals("*") || methodName.equals("/")){
				return findClassFromRightAssign(call.getReceiver(), codeContext);
			}
			
			final Class[] paraClass = findClasssFromArgs((ArrayNode)parametersNode, codeContext);

			return findMethodReturnByCallNode(call, methodName, paraClass, codeContext);
		}else if(parametersNode instanceof ListNode){//属性
			final String className = buildFullClassNameFromReceiverNode(call);//jop = Java::javax.swing.JOptionPane
			final Class classOut = findClassByName(className, false);
			if(classOut != null){
				return buildJRubyClassDesc(classOut, false);
			}
			
			final JRubyClassDesc out = findClassFromReceiverNode(call.getReceiver(), false, codeContext);
			if(out == null){
				return null;
			}
			
			final String propertyName = call.getName();
			try {
				final Field field = out.baseClass.getField(propertyName);
				return buildJRubyClassDesc(field.getType(), true);
			} catch (final Exception e) {
			}
			
			//仿属性的方法，即隐式方法调用
			return findMethodReturnByCallNode(call, methodName, NULL_PARAMETER, codeContext);
		}
		
		return null;
	}
	
	private final JRubyClassDesc findMethodReturnByCallNode(final CallNode call, final String methodName, final Class[] paraClass, final CodeContext codeContext) {
		final Node receiveNode = call.getReceiver();

		final JRubyClassDesc receiverClass = findClassFromReceiverNode(receiveNode, false, codeContext);
		try {
			final Class baseClass = receiverClass.baseClass;
			return findMethodReturn(baseClass, methodName, paraClass);
		} catch (final Exception e) {
		}
		
		return null;
	}

	public static JRubyClassDesc findMethodReturn(final Class baseClass, final String methodName, final Class[] paraClass)
			throws NoSuchMethodException {
		final Method[] methods = baseClass.getMethods();
		final int methodSize = methods.length;
		int matchCount = 0;
		Method firstMatch = null;
		for (int i = 0; i < methodSize; i++) {
			final Method tempMethod = methods[i];
			if(tempMethod.getName().equals(methodName)){
				final Class[] parameterTypes = tempMethod.getParameterTypes();
				if(paraClass.length == parameterTypes.length){
					boolean isMatch = true;
					for (int j = 0; j < parameterTypes.length; j++) {
						final Class ptc = parameterTypes[j];
						final Class iptc = paraClass[j];
						if(iptc == ptc || ptc.isAssignableFrom(iptc) || iptc.isAssignableFrom(ptc) || isAutoBoxClass(ptc, iptc)){
						}else{
							isMatch = false;
							break;
						}
					}
					
					if(isMatch == false){
						continue;
					}
					
					if(matchCount == 0){
						firstMatch = tempMethod;
					}
					matchCount++;
				}
			}
		}
		
		if(matchCount == 1){
			return buildJRubyClassDesc(firstMatch.getReturnType(), true);
		}
		
		final Method jmethod = baseClass.getMethod(methodName, paraClass);
		return buildJRubyClassDesc(jmethod.getReturnType(), true);
	}
	
	private final static JRubyClassDesc buildJRubyClassDesc(final Class basClass, final boolean isInstance){
		final JRubyClassDesc classDesc = new JRubyClassDesc();
		classDesc.baseClass = basClass;
		classDesc.isInstance = isInstance;
		
		return classDesc;
	}
    
    private final JRubyClassDesc findClassFromReceiverNode(final Node receiverNode, final boolean isTry, final CodeContext codeContext){
    	if(receiverNode instanceof CallNode){
    		final CallNode callNode = (CallNode)receiverNode;
    		if(callNode.getName().equals(JRUBY_NEW)){
    			final Node receiver = callNode.getReceiver();
    			if(receiver != null && receiver instanceof ConstNode && ((ConstNode)receiver).getName().equals(JRUBY_CLASS_FOR_NEW)){
    				//Class.new(Java::hc.a.b){}.new
    				final Node args = callNode.getArgs();//(CallNode:new, (ConstNode:Class), (ArrayNode, (CallNode:DeviceCompatibleDescription, (CallNode:msb, (CallNode:server, (CallNode:hc, (ConstNode:Java), (ListNode)), (ListNode))
					final CallNode firstPackageNode = (CallNode)args.childNodes().get(0);
					final String className = buildFullClassNameFromReceiverNode(firstPackageNode);
					return buildJRubyClassDesc(findClassByName(className, true), true);
    			}else{
    				return findClassFromReceiverNode(receiver, false, codeContext);//直接new
    			}
    		}else{
    			final JRubyClassDesc out = findClassFromCallNode(callNode, codeContext);
    			
				if(out != null){
					return out;
				}
				final String className = buildFullClassNameFromReceiverNode(callNode);
				return buildJRubyClassDesc(findClassByName(className, isTry), false);
			}
    	}else if(receiverNode instanceof FCallNode){//在定义内getProjectContext()
    		final ClassNode defClassNode = codeContext.getDefClassNode();
    		if(defClassNode != null){
	    		final FCallNode fcallNode = (FCallNode)receiverNode;
	    		final Node parametersNode = fcallNode.getArgs();
	    		
	    		if(parametersNode instanceof ArrayNode){//方法或+-*/
	//    			if(methodName.equals("+") || methodName.equals("-") || methodName.equals("*") || methodName.equals("/")){
	//    				return findClassFromRightAssign(call.getReceiver(), codeContext);
	//    			}
	    			final Class[] paraClass = findClasssFromArgs((ArrayNode)parametersNode, codeContext);
	    			try{
	    				return findMethodReturn(getDefSuperClass(defClassNode), fcallNode.getName(), paraClass);
	    			}catch (final Exception e) {
	    				if(L.isInWorkshop){
	    					e.printStackTrace();
	    				}
	    			}
	    		}
    		}
    		return null;
    	}else if(receiverNode instanceof LocalVarNode){
    		final LocalVarNode lvn = (LocalVarNode)receiverNode;
    		final AssignableNode varDefNode = (AssignableNode)findVarDefNode(codeContext.bottomNode, lvn.getName(), TYPE_VAR_LOCAL, null, false, codeContext.getDefClassNode() != null);
    		if(varDefNode != null){
    			return findClassFromReceiverNode(varDefNode.childNodes().get(0), false, codeContext);
    		}
    	}else if(receiverNode instanceof VCallNode){
    		final VCallNode vcn = (VCallNode)receiverNode;
    		final AssignableNode varDefNode = (AssignableNode)findVarDefNode(codeContext.bottomNode, vcn.getName(), TYPE_VAR_LOCAL, null, false, codeContext.getDefClassNode() != null);
    		if(varDefNode != null){
    			return findClassFromReceiverNode(varDefNode.childNodes().get(0), false, codeContext);
    		}
    	}else if(receiverNode instanceof InstVarNode){
    		final InstVarNode ivn = (InstVarNode)receiverNode;
    		final AssignableNode varDefNode = (AssignableNode)findVarDefNode(codeContext.bottomNode, ivn.getName(), TYPE_VAR_INSTANCE, null, false, codeContext.getDefClassNode() != null);
    		if(varDefNode != null){
    			return findClassFromReceiverNode(varDefNode.childNodes().get(0), false, codeContext);
    		}
    	}else if(receiverNode instanceof GlobalVarNode){
    		final GlobalVarNode gvn = (GlobalVarNode)receiverNode;
    		final AssignableNode varDefNode = (AssignableNode)findVarDefNode(codeContext.bottomNode, gvn.getName(), TYPE_VAR_GLOBAL, null, false, codeContext.getDefClassNode() != null);
    		if(varDefNode != null){
    			return findClassFromReceiverNode(varDefNode.childNodes().get(0), false, codeContext);
    		}
    	}else if(receiverNode instanceof ConstNode){
    		if(((ConstNode) receiverNode).getName().equals("Java")){//Java::xxxx
    			return null;
    		}
    		return findClassFromConstByImportOrDefClass(((ConstNode)receiverNode).getName(), codeContext);
    	}else if(receiverNode instanceof Colon2Node){
    		//(Colon2ConstNode:AA, (ConstNode:TestCodeHelper))
    		final Colon2Node colon2Node = (Colon2Node)receiverNode;
    		final String propertyName = colon2Node.getName();
    		final JRubyClassDesc classDesc = findClassFromReceiverNode(colon2Node.childNodes().get(0), false, codeContext);
    		try {
				final Field field = classDesc.baseClass.getField(propertyName);
				return buildJRubyClassDesc(field.getType(), true);
			} catch (final Exception e) {
			}
    	}else if(receiverNode instanceof ILiteralNode){
    		return buildJRubyClassDesc(getClassFromLiteral(receiverNode), true);
    	}else if(receiverNode instanceof NilNode){
    		return buildJRubyClassDesc(Object.class, true);
    	}
    	return null;
    }

	private static Class findClassByName(final String className, final boolean printError) {
		try {
			return getClassLoader().loadClass(className);
		} catch (final ClassNotFoundException e) {
			if(printError){
				e.printStackTrace();
			}
		}
		return null;
	}
    
    private static JRubyClassDesc findClassFromConstByImportOrDefClass(final String varName, final CodeContext codeContext){
    	//import javax.swing.JLabel\n
    	//JLabel便是constNode, 找到这个import行
    	//FCallNode (import|require)
    	if(codeContext != null){
    		final List<Node> childNodes = codeContext.contextNode.childNodes();
    		if(childNodes.size() > 0){
				final List<Node> list = childNodes.get(0).childNodes();
	    		final int size = list.size();
	    		for (int i = 0; i < size; i++) {
	    			Node fcallNode = list.get(i);
	    			if(fcallNode instanceof NewlineNode){
						fcallNode = fcallNode.childNodes().get(0);
	    			}
					if(fcallNode instanceof FCallNode){
						final FCallNode callNode = (FCallNode)fcallNode;
						if(callNode.getName().equals("import")){
							final ArrayNode importArgs = (ArrayNode)callNode.getArgs();
							final CallNode firstPackageNode = (CallNode)importArgs.childNodes().get(0);
							if(firstPackageNode.getName().equals(varName)){
								final String className = buildFullClassNameFromReceiverNode(firstPackageNode);
								return buildJRubyClassDesc(findClassByName(className, true), false);
							}
						}
					}else if(fcallNode instanceof ClassNode){
						//class MyMlet < Java::hc.server.ui.Mlet\n end\n
						//(ClassNode, (Colon2ImplicitNode:MyMlet), (CallNode:Mlet, (CallNode:ui, (CallNode:server, (CallNode:hc, (ConstNode:Java), 
						final ClassNode cNode = (ClassNode)fcallNode;
						if(getDefClassName(cNode).equals(varName)){
							final JRubyClassDesc classDesc = buildJRubyClassDesc(getDefSuperClass(cNode), false);
							classDesc.isDefClass = true;
							classDesc.defNode = cNode;
							
							return classDesc;
						}
	    			}
				}
    		}
    	}
    		
//    	if(parent instanceof BlockNode){
//        	final List<Node> list = parent.childNodes();
//        	final int size = list.size();
//			final String varName = constNode.getName();
//			int bottomIdx = size - 1;
//			for (int i = bottomIdx; i >= 0; i--) {
//				if(list.get(i) == bottomNode){
//					bottomIdx = i;
//					break;
//				}
//			}
//        	for (int i = bottomIdx; i >= 0; i--) {
//				final Node newlineNode = list.get(i);
//				if(newlineNode instanceof NewlineNode){
//					final Node fcallNode = newlineNode.childNodes().get(0);
//					if(fcallNode instanceof FCallNode){
//						FCallNode callNode = (FCallNode)fcallNode;
//						if(callNode.getName().equals("import")){
//							ArrayNode importArgs = (ArrayNode)callNode.getArgs();
//							final CallNode firstPackageNode = (CallNode)importArgs.childNodes().get(0);
//							if(firstPackageNode.getName().equals(varName)){
//								String className = buildFullClassNameFromReceiverNode(firstPackageNode);
//								return buildJRubyClassDesc(findClassByName(className, true), false);
//							}
//						}
//					}else if(fcallNode instanceof ClassNode){
//						//class MyMlet < Java::hc.server.ui.Mlet\n end\n
//						//(ClassNode, (Colon2ImplicitNode:MyMlet), (CallNode:Mlet, (CallNode:ui, (CallNode:server, (CallNode:hc, (ConstNode:Java), 
//						final ClassNode cNode = (ClassNode)fcallNode;
//						Colon3Node c3Node = cNode.getCPath();
//						if(c3Node.getName().equals(varName)){
//							JRubyClassDesc classDesc = buildJRubyClassDesc(getDefSuperClass(cNode), false);
//							classDesc.isDefClass = true;
//							classDesc.defNode = cNode;
//							
//							return classDesc;
//						}
//					}
//				}
//			}
//        	
		return null;
    }

	private static Class getDefSuperClass(final ClassNode cNode) {
		final CallNode superClass = (CallNode)cNode.getSuper();
		
		Class baseClass;
		if(superClass == null){//没有继承
			baseClass = Object.class;
		}else{
			final String className = buildFullClassNameFromReceiverNode(superClass);
			baseClass = findClassByName(className, true);
		}
		return baseClass;
	}
    
    private static final String buildFullClassNameFromReceiverNode(final CallNode receiverNode){
    	final Node receiver = receiverNode.getReceiver();
    	if(receiver instanceof CallNode){
    		final String parentClass = buildFullClassNameFromReceiverNode((CallNode)receiver);
    		if(parentClass.length() == 0){
    			return receiverNode.getName();
    		}else{
    			return parentClass + "." + receiverNode.getName();
    		}
    	}else if(receiver instanceof ConstNode){
    		return receiverNode.getName();
    	}else if(receiver instanceof VCallNode){
    		return ((VCallNode)receiver).getName() + "." + receiverNode.getName();
    	}
    	return "";
    }
    
    private static final Class[] NULL_PARAMETER = new Class[0];
    
    private final Class[] findClasssFromArgs(final ArrayNode argsNode, final CodeContext context){
    	if(argsNode.size() == 0){
    		return NULL_PARAMETER;
    	}else{
    		final int size = argsNode.size();
    		final Class[] out = new Class[size];
    		for (int i = 0; i < size; i++) {
				final Node node = argsNode.get(i);
//				Class c;
//				if(node instanceof CallNode){
//					c = findClassFromCallNode((CallNode)node, context).baseClass;
//				}else if(true){
//					c = 
//				}else{
//					c = getClassFromLiteral(node);
//				}
				out[i] = findClassFromReceiverNode(node, false, context).baseClass;
			}
    		return out;
    	}
    }

	private static Class getClassFromLiteral(final Node node) {
		if(node instanceof StrNode){
			return String.class;
		}else if(node instanceof FloatNode){
			return float.class;
		}else if(node instanceof BignumNode){
			return BigInteger.class;
		}else if(node instanceof FixnumNode){
			return int.class;
//				}else if(node instanceof ArrayNode){
		}else{
			return Object.class;
		}
	}
	
	private Node root = parseScripts("");
	
	boolean isScreeASTError = false;
	
	/**
	 * 将指定行填充为whitespace
	 * @param script
	 * @param lineNo
	 * @return
	 */
	public static final String whiteLine(final String script, final int lineNo){
		int startIdx = 0;
		int oldStartIdx = -1;
		int countLineNo = 0;
		for (int i = 0; i < lineNo && startIdx >= 0; i++) {
			countLineNo++;
			oldStartIdx = startIdx;
			startIdx = script.indexOf("\n", startIdx + 1);
		}
		
		final int scriptLength = script.length();
		
		if(startIdx >= 0){
			final StringBuilder sb = new StringBuilder(scriptLength);
			sb.append(script.substring(0, oldStartIdx));
			final int whiteStartIdx = (oldStartIdx==0?0:oldStartIdx + 1);
			final int whiteLen = startIdx - whiteStartIdx;
			if(countLineNo > 1){
				sb.append("\n");
			}
			for (int i = 0; i < whiteLen; i++) {
				sb.append(' ');
			}
			sb.append(script.substring(startIdx, scriptLength));
			return sb.toString();
		}else{
			if(lineNo == countLineNo){
				final StringBuilder sb = new StringBuilder(scriptLength);
				if(oldStartIdx > 0){
					sb.append(script.substring(0, oldStartIdx));
				}
				final int whiteLen = (scriptLength > oldStartIdx && lineNo > 1?(scriptLength - oldStartIdx - 1):scriptLength - oldStartIdx);
				if(oldStartIdx > 0 && (whiteLen > 0 || scriptLength == oldStartIdx + 1)){
					sb.append("\n");
				}
				for (int i = 0; i < whiteLen; i++) {
					sb.append(' ');
				}
				return sb.toString();
			}else{
				return script;
			}
		}
	}
	
	private final int getErrorLineNO(final ScriptException evalException){
		//(SyntaxError) <script>:3: syntax error, unexpected end-of-file
		final String evalStr = evalException.getMessage();
		int startMaoHao = evalStr.indexOf(":");
		startMaoHao = evalStr.indexOf(":", startMaoHao + 1);
		if(startMaoHao >= 0){
			final int endMaoHao = evalStr.indexOf(":", startMaoHao + 1);
			if(endMaoHao > 0){
				return Integer.parseInt(evalStr.substring(startMaoHao + 1, endMaoHao));
			}
		}
		return -1;
	}
	
	private final Node buildMiniNode(final String script, final ScriptEditPanel sep){
		RubyExector.parse(script, sep.getRunTestEngine());
		final ScriptException evalException = sep.getRunTestEngine().getEvalException();
		if(evalException == null){
			return parseScripts(script);
		}else{
			int lineNo = getErrorLineNO(evalException);
			do{
				try{
					if(lineNo > 0){
						return parseScripts(whiteLine(script, lineNo));
					}
				}catch (final Throwable e) {
					if(lineNo > 1){
						lineNo--;
					}else{
						return parseScripts("");
					}
				}
			}while(true);//必须要循环以发现多个bug
		}
	}
	
	public final boolean updateScriptASTNode(final ScriptEditPanel sep, final String scripts, final boolean isModifySource){
		if(isModifySource == false){
			return true;
		}
		
		try{
			final Node temp = parseScripts(scripts);
			root = temp;
			return true;
		}catch (final Throwable e) {
			if(L.isInWorkshop){
				e.printStackTrace();
			}
			root = buildMiniNode(scripts, sep);
		}
		return false;
	}
	
	private final static String PRE_REQUIRE = "require";
	
	public final ArrayList<CodeItem> getReqAndImp(final ArrayList<CodeItem> out){
		freeArray(out);
		{
			CodeItem item = CodeItem.getFree();
			item.code = PRE_IMPORT;
			item.codeDisplay = PRE_IMPORT;
			item.codeLowMatch = PRE_IMPORT.toLowerCase();
			item.type = CodeItem.TYPE_IMPORT;
			
			out.add(item);
			
			item = CodeItem.getFree();
			item.code = PRE_IMPORT_JAVA;
			item.codeDisplay = PRE_IMPORT_JAVA;
			item.codeLowMatch = PRE_IMPORT_JAVA.toLowerCase();
			item.type = CodeItem.TYPE_IMPORT;
			
			out.add(item);
		}
		
		{
			{
				final CodeItem item = CodeItem.getFree();
				item.code = PRE_REQUIRE + " 'java'";
				item.codeDisplay = item.code;
				item.codeLowMatch = item.codeDisplay.toLowerCase();
				item.type = CodeItem.TYPE_IMPORT;
				
				out.add(item);
			}
			
			final Set<String> keySet = addedLibClassesAndRes.keySet();
			final String[] keyStrs = new String[keySet.size()];
			
			keySet.toArray(keyStrs);
			Arrays.sort(keyStrs);
			
			for (int i = 0; i < keyStrs.length; i++) {
				final String libName = keyStrs[i];
				if(libName.endsWith(EXT_LOWER)){
					continue;
				}
				
				final CodeItem item = CodeItem.getFree();
				item.code = PRE_REQUIRE + " '" + libName + "'";
				item.codeDisplay = item.code;
				item.codeLowMatch = item.codeDisplay.toLowerCase();
				item.type = CodeItem.TYPE_IMPORT;
				
				out.add(item);
			}
		}

		return out;
	}
	
	/**
	 * 如果处理了，返回true；否则返回false
	 * @param key
	 * @param lineHeader
	 * @param lineIdx input focus index at current line
	 * @param scriptIdx input focus index at total script
	 * @return
	 */
	public final boolean input(final ScriptEditPanel sep, final JTextPane jtaScript, final Document doc, 
			final int fontHeight, final boolean isForcePopup, final Point caretPosition, final int scriptIdx) throws Exception{
		//1：行首时，requ
		//2：行首时，impo
		//3：def initialize|可重载的方法
		//4：空格后 import|Java::|getProjectContext(class xx < abc)
		//5：::后 Java::|Font::
		//6：.后 JButton.new|ImageIO.read
		//3：resource("时，lib资源
		
		final Point win_loc = jtaScript.getLocationOnScreen();
		
		final int line = ScriptEditPanel.getLineOfOffset(doc, scriptIdx);
        final int editLineStartIdx = ScriptEditPanel.getLineStartOffset(doc, line);
        final int lineIdx = scriptIdx - editLineStartIdx;
        final char[] lineChars = doc.getText(editLineStartIdx, lineIdx).toCharArray();
        
		initPreCode(lineChars, lineIdx, scriptIdx, line);
		if(isForcePopup == false && out.size() == 0){
			return false;
		}
		final Class codeClass = (preClass==null?null:preClass.baseClass);
		
		final int input_x = win_loc.x + ((caretPosition==null)?0:caretPosition.x);
		final int input_y = win_loc.y + ((caretPosition==null)?0:caretPosition.y);
		window.toFront(codeClass, sep, jtaScript, input_x, input_y, out, preCode.toLowerCase(), scriptIdx, fontHeight);
		return true;
	}
	
	public final void inputVariableForCSS(final JTextPane jtaScript, final Point caretPosition, final int fontHeight,
			final int scriptIdx){
		final Point win_loc = jtaScript.getLocationOnScreen();
		out.clear();
		
		final String[] variables = StyleManager.variables;
		
		for (int i = 0; i < variables.length; i++) {
			final CodeItem item = CodeItem.getFree();
			item.type = CodeItem.TYPE_RESOURCES;
			item.code = variables[i];
			item.codeDisplay = variables[i];
			item.codeLowMatch = variables[i];
			
			out.add(item);
		}
		
		final int input_x = win_loc.x + ((caretPosition==null)?0:caretPosition.x);
		final int input_y = win_loc.y + ((caretPosition==null)?0:caretPosition.y);
		window.toFront(Object.class, null, jtaScript, input_x, input_y, out, "", scriptIdx, fontHeight);
	}
	
//	/**
//	 * @param lineHeader
//	 * @param offLineIdx
//	 * @return return 0 if not instance or global var.
//	 */
//	private final int isInsOrGlobalVar(final char[] lineHeader, final int offLineIdx){
//		return 0;
//	}
	
	private final void getInsOrGloablVar(final ClassNode defClass, final ArrayList<CodeItem> out){
		freeArray(out);
		
		final List<MethodDefNode> lists = defClass.getMethodDefs();
		final int methodSize = lists.size();
		
		//优先从initialize中找
		for (int j = 0; j < methodSize; j++) {
			final MethodDefNode aMethod = lists.get(j);
			if(aMethod.getName().equals(JRUBY_CLASS_INITIALIZE_DEF)){
				appendVarDefInInitializeMethod(aMethod.getBody(), pre_var_tag_ins_or_global, out);
				break;
			}
		}
		
		//从非initialize中找
//		for (int j = 0; j < methodSize; j++) {
//			final MethodDefNode aMethod = lists.get(j);
//			if(aMethod.getName().equals(JRUBY_CLASS_INITIALIZE_DEF) == false){
//				final Node defNode = searchVarDefInInitializeMethod(aMethod.getBody(), parameter, typeParameter);
//				if(defNode != null){
//					return defNode;
//				}
//			}
//		}
		
		Collections.sort(out);
	}
	
	public void initPreCode(final char[] lineHeader, final int offLineIdx, final int scriptIdx, final int lineIdxAtScript) {
		freeArray(out);
		
		final int preCodeType = getPreCodeType(lineHeader, offLineIdx, scriptIdx, lineIdxAtScript);
		if(preCodeType == PRE_TYPE_NEWLINE){
			getVariables(root, true, "", out, scriptIdx, TYPE_VAR_LOCAL | TYPE_VAR_INSTANCE | TYPE_VAR_GLOBAL);
		}else if(preCodeType == PRE_TYPE_RESOURCES){
			getResources(out, getRequireLibs(root, outRequireLibs), true);
		}else if(preCodeType == PRE_TYPE_AFTER_IMPORT){
			getSubPackageAndClasses(out, getRequireLibs(root, outRequireLibs), true, true);
		}else if(preCodeType == PRE_TYPE_AFTER_JAVA){
			getSubPackageAndClasses(out, getRequireLibs(root, outRequireLibs), false, true);
		}else if(preCodeType == PRE_TYPE_IN_DEF_CLASS_FOR_METHOD_FIELD_ONLY){
			getMethodAndFieldForInstance(getDefSuperClass(backgroundDefClassNode), backgroundDefClassNode, null, false, out);
		}else if(preCodeType == PRE_TYPE_BEFORE_INSTANCE){
			getVariables(root, true, "", out, scriptIdx, pre_var_tag_ins_or_global);//情形：在行首输入im，可能后续为import或ImageIO
		}else if(preCodeType == PRE_TYPE_AFTER_INSTANCE){
			if(preClass != null){
				if(preClass.isInstance){
					getMethodAndFieldForInstance(preClass.baseClass, preClass.defNode, preClass, true, out);
				}else{
					getMethodAndFieldForClass(preClass.baseClass, preClass.defNode, out);
				}
			}else{//直接从背景中取类，会出现preClass==null
				if(pre_var_tag_ins_or_global == TYPE_VAR_INSTANCE || pre_var_tag_ins_or_global == TYPE_VAR_GLOBAL){
					getInsOrGloablVar(backgroundDefClassNode, out);
				}else{
					getMethodAndFieldForInstance(getDefSuperClass(backgroundDefClassNode), backgroundDefClassNode, null, false, out);
					getVariables(root, true, "", out, scriptIdx, pre_var_tag_ins_or_global);
				}
			}
		}
	}
	
	/**
	 * 可选项：require xxx, import
	 */
	private final static int PRE_TYPE_NEWLINE = 1;
	/**
	 * 可选项：package/subPackage/class。位于import之后
	 */
	private final static int PRE_TYPE_AFTER_IMPORT = 2;
	private final static int PRE_TYPE_AFTER_JAVA = 3;//import Java:: 或 abc = Java:: 之后
	private final static int PRE_TYPE_IN_DEF_CLASS_FOR_METHOD_FIELD_ONLY = 4;//class abc \n getProXXX end\n 1.在类定义内，进行方法或属性访问 2.访问变量
	private final static int PRE_TYPE_BEFORE_INSTANCE = 5;//JLab myLa $my @my
	private final static int PRE_TYPE_AFTER_INSTANCE = 6;//JLable. myLabel. $myLabel. @my.
	private final static int PRE_TYPE_RESOURCES = 7;//"/test/res/hc_16.png"
	
	public String preCode;
	private int pre_var_tag_ins_or_global;
	public boolean preCodeSplitIsDot;
	private JRubyClassDesc preClass;
	private ClassNode backgroundDefClassNode;
	
	private final ArrayList<String> outRequireLibs = new ArrayList<String>();
	
	private final String PRE_IMPORT = "import ";
	private final String PRE_IMPORT_JAVA = "import Java::";
	private final char[] import_chars = PRE_IMPORT.toCharArray();
	private final int import_chars_len = import_chars.length;
	private final char[] import_java_chars = PRE_IMPORT_JAVA.toCharArray();
	private final int import_java_chars_len = import_java_chars.length;
	
	private final boolean matchChars(final char[] search, final char[] chars, final int offset){
		if(search.length < (chars.length - offset)){
			return false;
		}
		
		final int endIdx = offset + chars.length;
		for (int i = offset; i < endIdx; i++) {
			if(chars[i - offset] != search[i]){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 如果是在方法体定义内，return true
	 */
	private final static boolean isInDefnNode(final Node node){
		final Node parent = node.getParent();
		if(parent == null){
			return false;
		}else{
			if(parent instanceof DefnNode){
				return true;
			}
			return isInDefnNode(parent);
		}
	}
	
	private final void getVariablesUpperForward(final Node node, final String preCode, final ArrayList<CodeItem> out, final int scriptIdx, 
			final int type_var){
		if(node == null){
			return;
		}
		final List<Node> childNodes = node.childNodes();
		
		if(node instanceof DefnNode){//将方法定义的参数取出来。如：opeator(id, para)，则增加id, para
			final DefnNode defNode = (DefnNode)node;
			final ArgsNode argsNode = defNode.getArgs();
			final List<Node> list = argsNode.childNodes();
			if (list.size() > 0){//除无参数，如opera()的情形
				final ArrayNode parametersNode = (ArrayNode)list.get(0);
				final int size = parametersNode.size();
				for(int i = 0; i<size; i++){
					final ArgumentNode parameterNode = (ArgumentNode)parametersNode.get(i);
					final String name = parameterNode.getName();
					
					final CodeItem item = CodeItem.getFree();
					item.code = name;
					item.codeDisplay = name;
					item.codeLowMatch = name.toLowerCase();
					item.type = CodeItem.TYPE_VARIABLE;
					out.add(item);
				}
			}
		}else if(node instanceof ConstDeclNode){
			final ConstDeclNode constDecl = (ConstDeclNode)node;
			final String name = constDecl.getName();
			
			final CodeItem item = CodeItem.getFree();
			item.code = name;
			item.codeDisplay = name;
			item.codeLowMatch = name.toLowerCase();
			item.type = CodeItem.TYPE_VARIABLE;
			out.add(item);
		}
		
		final int size = childNodes.size();
		for (int i = 0; i < size; i++) {
			Node one = childNodes.get(i);
			final SourcePosition position = one.getPosition();
			if(position.getStartOffset() > scriptIdx){
				break;
			}else if(position.getEndOffset() < scriptIdx){
				if(one instanceof NewlineNode){
					one = ((NewlineNode)one).childNodes().get(0);
				}
				if(((type_var & CodeHelper.TYPE_VAR_UNKNOW) != 0 || (type_var & CodeHelper.TYPE_VAR_LOCAL) != 0) 
						&& one instanceof FCallNode){//(FCallNode:import, (ArrayNode, (CallNode:JLabel, (CallNode:swing,
					final FCallNode callNode = (FCallNode)one;
					if(callNode.getName().equals("import")){
						final ArrayNode importArgs = (ArrayNode)callNode.getArgs();
						final CallNode firstPackageNode = (CallNode)importArgs.childNodes().get(0);
						final String name = firstPackageNode.getName();
						if(name.startsWith(preCode) && (CodeItem.contains(out, name) == false)){//由于是从下向上，可能被添加了一次
							final CodeItem item = CodeItem.getFree();
							item.code = name;
							item.codeDisplay = name;
							item.codeLowMatch = name.toLowerCase();
							item.type = CodeItem.TYPE_CLASS;
							out.add(item);
							continue;
						}
					}
				}
				
				if((type_var & CodeHelper.TYPE_VAR_INSTANCE) != 0 
						&& one instanceof InstAsgnNode){//CodeItem.TYPE_CLASS
					final String name = ((InstAsgnNode)one).getName();
					if(name.startsWith(preCode) && (CodeItem.contains(out, name) == false)){
						final CodeItem item = CodeItem.getFree();
						item.code = name;
						item.codeDisplay = name;
						item.codeLowMatch = name.toLowerCase();
						item.type = CodeItem.TYPE_VARIABLE;
						out.add(item);
						continue;
					}
				}
				
				if(((type_var & CodeHelper.TYPE_VAR_UNKNOW) != 0 || (type_var & CodeHelper.TYPE_VAR_LOCAL) != 0)
						&& one instanceof LocalAsgnNode){
					final String name = ((LocalAsgnNode)one).getName();
					if(name.startsWith(preCode) && (CodeItem.contains(out, name) == false)){
						final CodeItem item = CodeItem.getFree();
						item.code = name;
						item.codeDisplay = name;
						item.codeLowMatch = name.toLowerCase();
						if(isInDefnNode(one)){//方法定义内
							item.type = CodeItem.TYPE_VARIABLE;
						}else{
							item.type = CodeItem.TYPE_CLASS;
						}
						out.add(item);
						continue;
					}
				}
				
				if((type_var & CodeHelper.TYPE_VAR_GLOBAL) == CodeHelper.TYPE_VAR_GLOBAL 
						&& one instanceof GlobalAsgnNode){
					final String name = ((GlobalAsgnNode)one).getName();
					if(name.startsWith(preCode) && (CodeItem.contains(out, name) == false)){
						final CodeItem item = CodeItem.getFree();
						item.code = name;
						item.codeDisplay = name;
						item.codeLowMatch = name.toLowerCase();
						item.type = CodeItem.TYPE_VARIABLE;
						out.add(item);
						continue;
					}
				}
				
				if(((type_var & CodeHelper.TYPE_VAR_UNKNOW) != 0 || (type_var & CodeHelper.TYPE_VAR_LOCAL) != 0) 
						&& one instanceof ClassNode){
					final ClassNode cNode = (ClassNode)one;
					final String name = getDefClassName(cNode);
					if(name.startsWith(preCode) && (CodeItem.contains(out, name) == false)){
						final CodeItem item = CodeItem.getFree();
						item.code = name;
						item.codeDisplay = name;
						item.codeLowMatch = name.toLowerCase();
						item.type = CodeItem.TYPE_CLASS;
						out.add(item);
						continue;
					}
				}
				
			}
		}
		
		final Node parentNode = node.getParent();
		if(parentNode != null){
			getVariablesUpperForward(parentNode, preCode, out, scriptIdx, type_var);
		}
	}
	/**
	 * 从脚本中提取import 的classes，因为它们如变量般访问或new实例。
	 * 如果代码行前有def class，则可能新定义的类也会添加到结果集
	 * 如果是在方法定义体内，则可能方法参数亦是添加到结果集 
	 * @param node
	 * @param preCode
	 * @param out
	 * @param isAppend 是否添加到结果集尾部。false:先清空旧内容
	 * @param type_var 寻找指定类型变量，CodeHelper.TYPE_LOCAL_VAR,...
	 */
	public final void getVariables(final Node node, final boolean isAppend, final String preCode, final ArrayList<CodeItem> out, final int scriptIdx, final int type_var){
		if(isAppend == false){
			freeArray(out);
		}
		
		final Node paraNode = searchParaCallingNodeByIdx(node, scriptIdx);
		
		getVariablesUpperForward(paraNode.getParent(), preCode, out, scriptIdx, type_var);
		
		Collections.sort(out);
		return;
	}
	
	private final int searchJavaMaoHao(final char[] lines, final char[] searchChars, final int fromIdx){
		final int size = lines.length;
		final char firstChar = searchChars[0];
		final int javaMHlength = searchChars.length;
		for (int i = size - javaMHlength; i >= fromIdx; i--) {
			if(lines[i] == firstChar){
				if(matchChars(lines, searchChars, i)){
					return i;
				}
			}
		}
		return -1;
	}
	
	public final int getPreCodeType(final char[] lineHeader, final int columnIdx, final int scriptIdx, final int rowIdxAtScript){
		final int lineLen = lineHeader.length;
		if(lineLen == 0){
			preCode = "";
			getReqAndImp(out);
			return PRE_TYPE_NEWLINE;
		}
		
		{
			int countYinHao = 0;
			int lastYinHaoIdx = 0;
			for (int i = columnIdx - 1; i >= 0; i--) {
				if(lineHeader[i] == '\"' && i > 0 && lineHeader[i - 1] != '\\'){
					if(countYinHao == 0){
						lastYinHaoIdx = i;
					}
					countYinHao++;
				}
			}
	        if(countYinHao % 2 == 1){//countYinHao > 0 && 
	        	preCode = String.valueOf(lineHeader, lastYinHaoIdx + 1, columnIdx - (lastYinHaoIdx + 1));
	        	return PRE_TYPE_RESOURCES;
	        }
		}
		
		//因为此情形可也可能出现在首字母非空格非tab的情形，所以要提前
		final int idxJavaMaoHao = searchJavaMaoHao(lineHeader, JAVA_MAO_MAO, 0);
		if(idxJavaMaoHao>=0){
			//int i = 100 + Java::mypackage.sub::
			final int cutClassIdx = idxJavaMaoHao + JAVA_MAO_MAO.length;
			final int idxMaoHaoAgainForField = searchJavaMaoHao(lineHeader, MAO_HAO_ONLY, cutClassIdx);
			if(idxMaoHaoAgainForField < 0){
				//int i = 100 + Java::mypackage.su
				preCode = String.valueOf(lineHeader, cutClassIdx, columnIdx - cutClassIdx);
				return PRE_TYPE_AFTER_JAVA;
			}else{
				////int i = 100 + Java::mypackage.sub::myfield
				final String classPreName = String.valueOf(lineHeader, cutClassIdx, idxMaoHaoAgainForField - cutClassIdx);
				Class tryClass;
				try{
					tryClass = getClassLoader().loadClass(classPreName);
				}catch (final Exception e) {
					tryClass = Object.class;
				}
				final JRubyClassDesc jcd = buildJRubyClassDesc(tryClass, false);
				preClass = jcd;
				
				final int cutPreIdx = idxMaoHaoAgainForField + MAO_HAO_ONLY.length;
				preCodeSplitIsDot = false;
				preCode = String.valueOf(lineHeader, cutPreIdx, columnIdx - cutPreIdx);
				return PRE_TYPE_AFTER_INSTANCE;
			}
		}
		
		final char firstChar = lineHeader[0];
		if(firstChar != '\t' && firstChar != ' '){
			if(matchChars(lineHeader, import_chars, 0)){
				if(columnIdx < import_chars_len){
					preCode = "";
				}else if(matchChars(lineHeader, import_java_chars, 0)){
					preCode = String.valueOf(lineHeader, import_java_chars_len, columnIdx - import_java_chars_len);
					return PRE_TYPE_AFTER_JAVA;//import Java::ab
				}else{
					preCode = String.valueOf(lineHeader, import_chars_len, columnIdx - import_chars_len);
				}
				return PRE_TYPE_AFTER_IMPORT;
			}else{
				getReqAndImp(out);
			}
		}
		 
		final ClassNode classNode = isInDefClass(root, rowIdxAtScript);
		preClass = findPreCodeAfterVar(lineHeader, columnIdx, scriptIdx, rowIdxAtScript);
		if(classNode != null && preClass == null){
			backgroundDefClassNode = classNode;
			return PRE_TYPE_AFTER_INSTANCE;
		}else{
			if(preClass == null){
				return PRE_TYPE_BEFORE_INSTANCE;
			}else{
				return PRE_TYPE_AFTER_INSTANCE;
			}
		}
	}

	private final static ClassLoader getClassLoader() {
		return ScriptEditPanel.getRunTestEngine().getProjClassLoader();
	}
	
	static final char[] JAVA_MAO_MAO = "Java::".toCharArray();
	static final char[] MAO_HAO_ONLY = "::".toCharArray();
	
	/**
	 * 寻找abc.metho中的abc(return)和metho(preCode)。
	 * 有可能没有return部分，即返回null
	 * @param lineHeader
	 * @param columnIdx
	 * @return
	 */
	private final JRubyClassDesc findPreCodeAfterVar(final char[] lineHeader, final int columnIdx, final int scriptIdx, final int rowIdxAtScript){
		preCode = "";
		pre_var_tag_ins_or_global = TYPE_VAR_UNKNOW;
		
		for (int i = columnIdx - 1; i >= 0; i--) {
			final char c = lineHeader[i];
			if((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '1' && c <= '0') || c == '_'){
				continue;
			}else if(c == '.'){
				final int offset = i + 1;
				preCode = String.valueOf(lineHeader, offset, columnIdx - offset);
				preCodeSplitIsDot = true;
				return findPreVariableOrMethodOut(lineHeader, i, scriptIdx, rowIdxAtScript);
			}else if(c == '"'){
				while(i>=0 && lineHeader[--i] != '"'){
				}
				continue;
			}else if(c == ':'){
				if(i > 0 && lineHeader[i - 1] == ':'){
					//a = JLable::PARA_1
					final int offset = i + 1;
					preCode = String.valueOf(lineHeader, offset, columnIdx - offset);
					preCodeSplitIsDot = false;
					return findPreVariableOrMethodOut(lineHeader, i - 1, scriptIdx, rowIdxAtScript);
				}
			}else{
				if(c == '@'){
					pre_var_tag_ins_or_global = TYPE_VAR_INSTANCE;
				}else if(c == '$'){
					pre_var_tag_ins_or_global = TYPE_VAR_GLOBAL;
				}
				final int offset = i + 1;
				preCode = String.valueOf(lineHeader, offset, columnIdx - offset);
				return null;
			}
		}
		
		preCode = String.valueOf(lineHeader, 0, columnIdx - 0);
		
		return null;
	}
	
	/**
	 * 从表达式中搜索如：+ {@abc.m()} + {$efg.kk(a, b)} + {edf::efg()} + {abc.efg()}
	 * @param lineHeader
	 * @param rightKuoIdx 后端)所在index
	 * @return
	 */
	private final static String searchLeftAssign(final char[] lineHeader, final int rightKuoIdx){
		int leftKuoIdx = searchLeftMethodStartIdx(lineHeader, rightKuoIdx);
		
		//变量名段
		if(leftKuoIdx > 0){
			for (; leftKuoIdx >= 0; leftKuoIdx--) {
				final char c = lineHeader[leftKuoIdx];
				if(c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '1' && c <= '0' || c == '_' || c == ':' || c == '$' || c == '@'){
					continue;
				}else{
					break;
				}
			}
			final int startIdx = leftKuoIdx + 1;
			return String.valueOf(lineHeader, startIdx, rightKuoIdx + 1 - startIdx);
		}
		
		return "";
	}

	private static int searchLeftMethodStartIdx(final char[] lineHeader,
			final int rightKuoIdx) {
		int kuoHaoDeep = 1;
		int leftKuoIdx = rightKuoIdx - 1;
		
		//()段
		for (; leftKuoIdx >= 0; leftKuoIdx--) {
			final char c = lineHeader[leftKuoIdx];
			if(c == '('){
				if(--kuoHaoDeep == 0){
					leftKuoIdx--;
					break;
				}
			}else if(c == ')'){
				kuoHaoDeep++;
			}else if(c == '"'){
				while(leftKuoIdx >=1 && lineHeader[--leftKuoIdx] != '"' && lineHeader[leftKuoIdx - 1] != '\\'){
				}
				continue;
			}
		}
		
		//方法名段
		for (; leftKuoIdx >= 0; leftKuoIdx--) {
			final char c = lineHeader[leftKuoIdx];
			if(c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '1' && c <= '0' || c == '_'){
				continue;
			}
			if(c == '.'){
				leftKuoIdx--;
				
				if(lineHeader[leftKuoIdx] == ')'){
					//pp.add().add()
					return searchLeftMethodStartIdx(lineHeader, leftKuoIdx);
				}else{
					break;
				}
			}else if(c == ':'){
				leftKuoIdx -= 2;
			}else{
				break;
			}
		}
		
		return leftKuoIdx;
	}
	
	/**
	 * 行中出现：f = Java::javax.swing.JLabel::PROP，中出现Java::不由本逻辑处理，由外部逻辑判断提示
	 * @param lineHeader
	 * @param columnIdx
	 * @param scriptIdx
	 * @return
	 */
	private final JRubyClassDesc findPreVariableOrMethodOut(final char[] lineHeader, final int columnIdx, final int scriptIdx, final int rowIdxAtScript){
		for (int i = columnIdx - 1; i >= 0; i--) {
			final char c = lineHeader[i];
			if((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '1' && c <= '0') || c == '_'){
				continue;
			}else if(c == '@'){
				final int offset = i + 1;
				final String v = String.valueOf(lineHeader, offset, columnIdx - offset);
				final CodeContext context = new CodeContext(root, scriptIdx, rowIdxAtScript);
				return findParaClass(context, v, CodeHelper.TYPE_VAR_INSTANCE);
			}else if(c == '$'){
				final int offset = i + 1;
				final String v = String.valueOf(lineHeader, offset, columnIdx - offset);
				final CodeContext context = new CodeContext(root, scriptIdx, rowIdxAtScript);
				return findParaClass(context, v, CodeHelper.TYPE_VAR_GLOBAL);
			}else if(c == ')'){
				final String assignStr = searchLeftAssign(lineHeader, i);//从左向提取类
				final List<Node> childNodes = parseScripts(assignStr).childNodes();
				if(childNodes.size() > 0){
					final Node node = childNodes.get(0).childNodes().get(0);//(RootNode, (NewlineNode, (CallNode:add,
					final CodeContext codeContext = new CodeContext(root, scriptIdx, rowIdxAtScript);
					return findClassFromRightAssign(node, codeContext);
				}
			}else{
				final int offset = i + 1;
				final String v = String.valueOf(lineHeader, offset, columnIdx - offset);
				final CodeContext context = new CodeContext(root, scriptIdx, rowIdxAtScript);
				return findParaClass(context, v, CodeHelper.TYPE_VAR_LOCAL);
			}
		}
		final String v = String.valueOf(lineHeader, 0, columnIdx - 0);
		final CodeContext context = new CodeContext(root, scriptIdx, rowIdxAtScript);
		return findParaClass(context, v, CodeHelper.TYPE_VAR_LOCAL);
	}
	
	/**
	 * 没有，则返回null
	 * @param preClass
	 * @param set
	 * @param setSize
	 * @return
	 */
	public final List<String> getMatchClasses(final String preClass){
		return getMatch(preClass, CodeStaticHelper.J2SE_CLASS_SET, CodeStaticHelper.J2SE_CLASS_SET_SIZE);
	}
	
	/**
	 * 没有，则返回null
	 * @param preClass
	 * @return
	 */
	public final List<String> getMatchPkg(final String preClass){
		return getMatch(preClass, CodeStaticHelper.J2SE_PACKAGE_SET, CodeStaticHelper.J2SE_PACKAGE_SET_SIZE);
	}
	
	public static final String getWordCompletionKeyText(){
		return PropertiesManager.getValue(PropertiesManager.p_wordCompletionKeyCode, "/");
	}
	
	public static final String getWordCompletionKeyChar(){
		return PropertiesManager.getValue(PropertiesManager.p_wordCompletionKeyChar, ResourceUtil.isMacOSX()?"÷":"");
	}
	
	public static final String getWordCompletionModifierText(){
		return KeyComperPanel.getHCKeyText(getWordCompletionModifierCode());
	}

	private static int getWordCompletionModifierCode() {
		return Integer.parseInt(PropertiesManager.getValue(PropertiesManager.p_wordCompletionModifierCode, 
		String.valueOf(KeyEvent.VK_ALT)));
	}
	
	private final List<String> getMatch(final String preClass, final String[] set, final int setSize){
		if(preClass == null || preClass.length() == 0){
			return null;
		}
		
		int lastPreIdx = 0;
		int lastMatchIdx = setSize;
		boolean matched = false;
		int matchIdx = setSize >> 1;
		while(true){
			final String item = set[matchIdx];
			final int compareTo = item.compareTo(preClass);
			if(compareTo >= 0){
				if(item.startsWith(preClass)){
					matched = true;
				}
				lastMatchIdx = matchIdx;
				matchIdx = lastPreIdx + ((matchIdx - lastPreIdx) >> 1);
				if(matchIdx == lastPreIdx){
					break;
				}
			}else{
				lastPreIdx = matchIdx;
				matchIdx = lastPreIdx + ((lastMatchIdx - lastPreIdx) >> 1);
				if(matchIdx == lastPreIdx){
					if(matched){
						matchIdx++;
					}
					break;
				}
			}
		}
		
		if(matched){
			final ArrayList<String> list = new ArrayList<String>();
			
			while(true){
				final String item = set[matchIdx++];
				if(item.startsWith(preClass)){
					list.add(item);
					if(matchIdx == setSize){
						return list;
					}
				}else if(list.size() == 0){
					continue;
				}else{
					return list;
				}
			}
		}else{
			return null;
		}
	}
	
}
package hc.server.ui.design.code;

import hc.App;
import hc.core.HCTimer;
import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.server.CallContext;
import hc.server.PlatformManager;
import hc.server.data.KeyComperPanel;
import hc.server.data.StoreDirManager;
import hc.server.ui.Mlet;
import hc.server.ui.SimuMobile;
import hc.server.ui.design.engine.HCJRubyEngine;
import hc.server.ui.design.engine.RubyExector;
import hc.server.ui.design.hpj.HCTextPane;
import hc.server.ui.design.hpj.HPShareJar;
import hc.server.ui.design.hpj.MouseExitHideDocForMouseMovTimer;
import hc.server.ui.design.hpj.ScriptEditPanel;
import hc.util.ClassUtil;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;
import hc.util.StringBuilderCacher;

import java.awt.Event;
import java.awt.Point;
import java.awt.Rectangle;
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
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jrubyparser.CompatVersion;
import org.jrubyparser.Parser;
import org.jrubyparser.SourcePosition;
import org.jrubyparser.ast.ArgsNode;
import org.jrubyparser.ast.ArgumentNode;
import org.jrubyparser.ast.ArrayNode;
import org.jrubyparser.ast.AssignableNode;
import org.jrubyparser.ast.BignumNode;
import org.jrubyparser.ast.BlockNode;
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
import org.jrubyparser.ast.INameNode;
import org.jrubyparser.ast.InstAsgnNode;
import org.jrubyparser.ast.InstVarNode;
import org.jrubyparser.ast.IterNode;
import org.jrubyparser.ast.ListNode;
import org.jrubyparser.ast.LocalAsgnNode;
import org.jrubyparser.ast.LocalVarNode;
import org.jrubyparser.ast.MethodDefNode;
import org.jrubyparser.ast.MethodNameNode;
import org.jrubyparser.ast.NewlineNode;
import org.jrubyparser.ast.NilNode;
import org.jrubyparser.ast.Node;
import org.jrubyparser.ast.NodeType;
import org.jrubyparser.ast.SClassNode;
import org.jrubyparser.ast.SelfNode;
import org.jrubyparser.ast.StrNode;
import org.jrubyparser.ast.SymbolNode;
import org.jrubyparser.ast.VCallNode;
import org.jrubyparser.lexer.SyntaxException;
import org.jrubyparser.parser.ParserConfiguration;

public class CodeHelper {
	public static final String JAVA_PACKAGE_CLASS_PREFIX = "java";
	public static final String HC_PACKAGE_CLASS_PREFIX = "hc.";
	private static final String CLASS_STATIC = "class";//ctx.class.getProjectContext()
	private static final String CLASS_STATIC_PREFIX = CLASS_STATIC + ".";//ctx.class.getProjectContext()
	private static final String TO_UPPER_CASE = "upcase";
	private static final String TO_DOWN_CASE = "downcase";
	private static final String TO_S = "to_s";
	private static final String TO_F = "to_f";
	private static final String TO_I = "to_i";
	private static final String NIL = "nil";
	private static final String JRUBY_CLASS_INITIALIZE_DEF = "initialize";
	private static final String JRUBY_INCLUDE = "include";
	public static final String JRUBY_NEW = "new";
	public static final String JRUBY_NEW_METHOD = JRUBY_NEW + "(";
	private static final String JRUBY_CLASS_FOR_NEW = "Class";
	public static final String JRUBY_JAVA_CLASS = "java_class";
	private static final Class JRUBY_JAVA_CLASS_AGENT = JavaClass.class;
	static final String IterNodeClass = IterNode.class.getName();
	private static final Class JRUBY_CLASS_FOR_BULDER = ClassBulder.class;
	public static final String DEF_MEMBER = "def";
	private static final char[] DEF_MEMBER_BS = DEF_MEMBER.toCharArray();
	static boolean isDisplayOverrideMethodAndDoc = false;
	
	private final HashMap<String, String[]> addedLibClassesAndRes = new HashMap<String, String[]>();
	final HashMap<String, ArrayList<CodeItem>> classCacheMethodAndPropForClass = new HashMap<String, ArrayList<CodeItem>>();
	final HashMap<String, ArrayList<CodeItem>> classCacheMethodAndPropForInstance = new HashMap<String, ArrayList<CodeItem>>();
	final Vector<String> classCacheMethodAndPropForProject = new Vector<String>(30);
	
	public final CodeWindow window;
	private final ArrayList<CodeItem> autoTipOut = new ArrayList<CodeItem>();
	public final ArrayList<CodeItem> outAndCycle = new ArrayList<CodeItem>();

	public int wordCompletionModifyMaskCode;
	public int wordCompletionModifyCode;
	public int wordCompletionCode;
	public char wordCompletionChar;
	public final MouseExitHideDocForMouseMovTimer mouseExitHideDocForMouseMovTimer = new MouseExitHideDocForMouseMovTimer(
			"MouseExitHideDocForMouseMovTimer", HCTimer.HC_INTERNAL_MS * 4, false) {
		@Override
		public void doBiz() {
			synchronized (ScriptEditPanel.scriptEventLock) {
				hideByMouseEvent();
				reset();
			}
		}
		
		@Override
		public void setEnable(final boolean enable){
			if(enable){
				if(isUsingByCode == false && isUsingByDoc == false){
					super.setEnable(true);
				}else{
					super.setEnable(false);
				}
			}else{
				super.setEnable(false);
			}
		}
	};
	
	public final void reset(){
		PlatformManager.getService().resetClassPool();
		
		final int size = classCacheMethodAndPropForProject.size();
		for (int i = 0; i < size; i++) {
			final String key = classCacheMethodAndPropForProject.get(i);
			classCacheMethodAndPropForClass.remove(key);
			classCacheMethodAndPropForInstance.remove(key);
		}
		
		classCacheMethodAndPropForProject.clear();
	}
	
	/**
	 * 鼠标使用DocWindown或CodeWindow
	 */
	public final void notifyUsingByDoc(final boolean isUsing){
		if(L.isInWorkshop){
			LogManager.log("AutoCodeTip notifyUsingByDoc : " + isUsing);
		}
		if(mouseExitHideDocForMouseMovTimer.isTriggerOn()){
			mouseExitHideDocForMouseMovTimer.isUsingByDoc = isUsing;
			mouseExitHideDocForMouseMovTimer.setEnable(!isUsing);
		}
	}
	
	public final void notifyUsingByCode(final boolean isUsing){
		if(L.isInWorkshop){
			LogManager.log("[CodeTip] start notifyUsingByCode : " + isUsing);
		}
		
		if(mouseExitHideDocForMouseMovTimer.isTriggerOn()){
			mouseExitHideDocForMouseMovTimer.isUsingByCode = isUsing;
			mouseExitHideDocForMouseMovTimer.setEnable(!isUsing);
		}
		
		if(L.isInWorkshop){
			LogManager.log("[CodeTip] done notifyUsingByCode : " + isUsing);
		}
	}
	
//	public final void hideAfterMouse(final boolean isForceHideDoc){
//		if(isForceHideDoc){
//			hideByMouseEvent();
//			return;
//		}
////		if(mouseExitHideDocForMouseMovTimer.isTriggerOn()){
////			mouseExitHideDocForMouseMovTimer.setEnable(true);
////		}
//	}
	
	//TODO str.each(separator=$/) { |substr| block }  ;;; each_byte { |fixnum| block } ;;; each_line(separator=$/) { |substr| block }
	private static final String[] rubyStrMethods = {"capitalize", "capitalize!", "casecmp(str)", "center(width)", "chomp", "chomp!", 
		"chop", "chop!", "concat(other_str)", "count(str)", "crypt(other_str)", "delete(other_str)", "delete!(other_str)",
		"downcase", "downcase!", "dump", "empty?", "eql?(other)", "gsub(pattern, replacement)", "gsub!(pattern, replacement)",
		"hash", "hex", "include?(other_str)", "index(substring)", "insert(index, other_str)", "inspect", "intern", "to_sym",
		"length", "ljust(integer, padstr)", "lstrip", "lstrip!", "match(pattern)", "oct", "partition(sep)", "replace(other_str)", "reverse", 
		"reverse!", "rindex(substring)", "rjust(integer, padstr)", "rpartition(sep)", "rstrip", "rstrip!", "scan(pattern)", 
		"size", "slice(fixnum)", "slice!(fixnum)", "split(pattern)", "squeeze(other_str)", "squeeze!(other_str)", "start_with?(prefix)", 
		"strip", "strip!", "sub(pattern, replacement)", "sub!(pattern, replacement)", "succ", "succ!", "next", "next!", "sum()", 
		"swapcase", "swapcase!", "to_f", "to_i()",
		"to_s", "to_str", "tr(from_str, to_str)", "tr!(from_str, to_str)", "tr_s(from_str, to_str)", "tr_s!(from_str, to_str)",
		"unpack(format)", "upcase", "upcase!", "upto(other_str)"};
	
	private static final Class[] rubyClass = {String.class};
	
	public static final boolean isRubyClass(final Class c){
		for (int i = 0; i < rubyClass.length; i++) {
			if(c == rubyClass[i]){
				return true;
			}
		}
		return false;
	}
	
	private final static String getMethodNameFromRubyMethod(final String method){
		int endIdx = method.indexOf('!');
		if(endIdx > 0){
			return method.substring(0, endIdx);
		}
		endIdx = method.indexOf('(');
		if(endIdx > 0){
			return method.substring(0, endIdx);
		}
		
		return method;
	}
	
	private final static void buildMethodForRubyClass(final Class c, final String[] methods, final ArrayList<CodeItem> list){
		final int methodSize = methods.length;
		
		for (int i = 0; i < methodSize; i++) {
			final String codeMethod = methods[i];
			
			final boolean findSameName = CodeItem.contains(list, codeMethod);
			if(findSameName){
				continue;
			}
			
			final CodeItem item = CodeItem.getFree();
			item.fieldOrMethodOrClassName = getMethodNameFromRubyMethod(codeMethod);
			item.code = codeMethod;//一般方法直接输出，静态方法加.class.转换
			final int cuohaoIdx = codeMethod.indexOf('(');
			item.codeForDoc = cuohaoIdx>0?(codeMethod.substring(0, cuohaoIdx)):codeMethod;
			item.fmClass = c.getName();
			item.codeDisplay = codeMethod;
			item.codeLowMatch = codeMethod;
			
			item.isPublic = true;
			item.modifiers = Modifier.PUBLIC;
			item.isForMaoHaoOnly = false;//!Modifier.isStatic(modifiers);
			item.type = CodeItem.TYPE_METHOD;
			item.setAnonymouseClassType(null);
			
			list.add(item);
		}
	}
	
	private final static void buildMethodAndProp(final Class c, final boolean isForClass, final ArrayList<CodeItem> out, 
			final boolean needNewMethod, final CodeHelper codeHelper, final HashMap<String, ArrayList<CodeItem>> set, final String key){
		if(c == null){
			return;
		}
		
		{
			final ArrayList<CodeItem> list = set.get(key);
			if(list != null){
				out.addAll(list);
				return;
			}
		}
		
		final ArrayList<CodeItem> list = new ArrayList<CodeItem>(100);
		try{
			
			if(isRubyClass(c)){
				buildMethodForRubyClass(c, rubyStrMethods, list);
				return;
			}
			
			appendSuperOrNewMethod(c, isForClass, needNewMethod, codeHelper, set, list);

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
				addToMethod(c, list, TO_DOWN_CASE, "String", String.class.getSimpleName());
				addToMethod(c, list, TO_UPPER_CASE, "String", String.class.getSimpleName());
				addToMethod(c, list, TO_I, "int", String.class.getSimpleName());
				addToMethod(c, list, TO_F, "float", String.class.getSimpleName());
			}else if(c == int.class){
				addToMethod(c, list, TO_S, "String", "int");
			}else if(c == float.class){
				addToMethod(c, list, TO_S, "String", "float");
			}else if(c == Object.class){
				addToField(c, list, NIL, "Object", "Object");//is Nil
			}else if(isForClass){//一般用于用户Jar库的类
				if(c != JRUBY_JAVA_CLASS_AGENT){
					addToField(c, list, JRUBY_JAVA_CLASS, "Class", "JRubyClass");//后两参数仅表示，没有实际相关
				}
	//			item.isForMaoHaoOnly = true;
			}
			
			for (int i = 0; i < fieldSize; i++) {
				final Field field = allFields[i];
				final int modifiers = field.getModifiers();
				final boolean isStatic = Modifier.isStatic(modifiers);
				
				if(isForClass){
					if(isStatic == false){
						continue;
					}
				}
				
				final boolean isPublic = Modifier.isPublic(modifiers);
				if(isPublic || Modifier.isProtected(modifiers)){
					final String codeField = field.getName();
					final String codeFieldForInput = (isForClass==false && isStatic)?(CLASS_STATIC_PREFIX + codeField):codeField;
					
//					//属性相同名，只保留最外层
//					if(CodeItem.contains(list, codeFieldForInput)){
//						continue;
//					}
					
					final CodeItem item = CodeItem.getFree();
					item.fieldOrMethodOrClassName = codeField;
					item.code = codeFieldForInput;
					item.codeForDoc = item.code;
					final Class<?> fieldClass = field.getDeclaringClass();
					item.fmClass = fieldClass.getName();
					item.codeDisplay = codeField + " : " + field.getType().getSimpleName() + " - " + fieldClass.getSimpleName();
					item.codeLowMatch = codeField.toLowerCase();
					item.isPublic = isPublic;
					item.modifiers = modifiers;
					item.isForMaoHaoOnly = isStatic;//不能如方法改为false，因为import Java::HTMLMlet\n HTMLMlet.URL_EXIT会出错，这是由于JRuby语法所限，1.7.3如此
					item.type = CodeItem.TYPE_FIELD;
					
					list.add(item);
				}
			}
			
			for (int i = 0; i < methodSize; i++) {
				final Method method = allMethods[i];
				final int modifiers = method.getModifiers();
				final boolean isStatic = Modifier.isStatic(modifiers);
				
				if(isForClass){
					if(isStatic == false){
						continue;
					}
				}
				
				final boolean isPublic = Modifier.isPublic(modifiers);
				if(isPublic || Modifier.isProtected(modifiers)){
					final Class[] paras = method.getParameterTypes();
					final String[] codeParas;
					if(key.startsWith(HC_PACKAGE_CLASS_PREFIX) || key.startsWith(JAVA_PACKAGE_CLASS_PREFIX)){
						codeParas = null;//provides from Java doc
					}else{
						codeParas = PlatformManager.getService().getMethodCodeParameter(method);
					}
					String paraStr = "";
					String paraStrForDisplay = "";
					for (int j = 0; j < paras.length; j++) {
						if(paraStr.length() > 0){
							paraStr += ", ";
							paraStrForDisplay += ", ";
						}
						final String simpleName = paras[j].getSimpleName();
						if(codeParas != null){
							paraStr += codeParas[j];
						}else{
							paraStr += ResourceUtil.toLowerCaseFirstChar(simpleName);
						}
						paraStrForDisplay += simpleName;
					}
					
					final String codeMethod = method.getName() + (paraStr.length()==0?"()":"(" + paraStr + ")");
					final String codeMethodForDisplay = method.getName() + (paraStrForDisplay.length()==0?"()":"(" + paraStrForDisplay + ")");
					final String codeMethodForInput = (isForClass==false && isStatic)?(CLASS_STATIC_PREFIX + codeMethod):codeMethod;
					final String codeMethodForDoc = (isForClass==false && isStatic)?(CLASS_STATIC_PREFIX + codeMethodForDisplay):codeMethodForDisplay;
					
					final CodeItem item = CodeItem.getFree();
					item.fieldOrMethodOrClassName = method.getName();
					item.code = codeMethodForInput;//一般方法直接输出，静态方法加.class.转换
					item.codeForDoc = codeMethodForDoc;
					final Class<?> methodClass = method.getDeclaringClass();
					item.fmClass = methodClass.getName();
					item.codeDisplay = codeMethodForDisplay + " : " + method.getReturnType().getSimpleName() + " - " + methodClass.getSimpleName();
					item.codeLowMatch = codeMethod.toLowerCase();
					
					item.isPublic = isPublic;
					item.modifiers = modifiers;
					item.isForMaoHaoOnly = false;//!Modifier.isStatic(modifiers);
					item.type = CodeItem.TYPE_METHOD;
					item.setAnonymouseClassType(paras);
					
					CodeItem.overrideMethod(item, list);
					
					list.add(item);
				}
			}
		}finally{
			set.put(key, list);
			
			if(isForClass == false){//注意：等待静态先处理，其次才是for instance
				DocHelper.processDoc(codeHelper, c, false);
			}
			
			out.addAll(list);
		}
	}

	private static void appendSuperOrNewMethod(final Class c, final boolean isForClass,
			final boolean needNewMethod, final CodeHelper codeHelper,
			final HashMap<String, ArrayList<CodeItem>> set, final ArrayList<CodeItem> list) {
		if(isForClass == false){
			final Class superclass = c.getSuperclass();
			if(superclass != null){
				buildMethodAndProp(superclass, isForClass, list, false, codeHelper, set, superclass.getName());
			}
			final Class[] interfaces = c.getInterfaces();
			for (int i = 0; i < interfaces.length; i++) {
				final Class face1 = interfaces[i];
				buildMethodAndProp(face1, isForClass, list, false, codeHelper, set, face1.getName());
			}
		}else if(needNewMethod){
			if(c == Mlet.class){//强制不出现Mlet的new方法
				return;
			}
			
			final Constructor[] cons = c.getDeclaredConstructors();
			final int size = cons.length;
			if(size == 0){
				final CodeItem item = CodeItem.getFree();
				item.fieldOrMethodOrClassName = JRUBY_NEW;
				item.code = JRUBY_NEW + "()";
				item.codeForDoc = item.code;
				item.codeDisplay = item.code + " - " + c.getSimpleName();
				item.codeLowMatch = item.code.toLowerCase();
				
				item.isPublic = true;
				item.isForMaoHaoOnly = false;
				item.type = CodeItem.TYPE_METHOD;
				item.fmClass = c.getName();
				
				list.add(item);
			}else{
				for (int i = 0; i < size; i++) {
					final Constructor con = cons[i];
					final int conModifier = con.getModifiers();
					if(Modifier.isProtected(conModifier) || Modifier.isPublic(conModifier)){
					}else{
						continue;
					}
					
					final Class[] paras = con.getParameterTypes();
					String paraStr = "";
					for (int j = 0; j < paras.length; j++) {
						if(paraStr.length() > 0){
							paraStr += ", ";
						}
						paraStr += paras[j].getSimpleName();
					}
					
					final CodeItem item = CodeItem.getFree();
					item.fieldOrMethodOrClassName = JRUBY_NEW;
					item.code = JRUBY_NEW + (paraStr.length()==0?"()":"(" + paraStr + ")");
					item.codeForDoc = item.code;
					item.codeDisplay = item.code + " - " + c.getSimpleName();
					item.codeLowMatch = item.code.toLowerCase();
					
					item.isPublic = true;
					item.isForMaoHaoOnly = false;
					item.type = CodeItem.TYPE_METHOD;
					item.fmClass = c.getName();
					
					list.add(item);
				}
			}
		}
	}

	private static CodeItem addToField(final Class claz, final ArrayList<CodeItem> list, final String methodName, final String resultType, final String baseClassName) {
		return addToMethodOrField(claz, CodeItem.TYPE_FIELD, list, methodName, resultType, baseClassName);
	}
	
	private static CodeItem addToMethod(final Class claz, final ArrayList<CodeItem> list, final String methodName, final String resultType, final String baseClassName) {
		return addToMethodOrField(claz, CodeItem.TYPE_METHOD, list, methodName, resultType, baseClassName);
	}
	
	private static CodeItem addToMethodOrField(final Class claz, final int type, final ArrayList<CodeItem> list, final String methodName, final String resultType, final String baseClassName) {
		final CodeItem item = CodeItem.getFree();
		item.fieldOrMethodOrClassName = methodName;
		if(type == CodeItem.TYPE_METHOD){
			item.code = methodName + "()";
		}else{
			item.code = methodName;
		}
		item.codeForDoc = item.code;
		item.fmClass = claz.getName();
		item.codeDisplay = item.code + " : " + resultType + " - " + baseClassName;
		item.codeLowMatch = item.code.toLowerCase();
		item.isPublic = true;
		item.isForMaoHaoOnly = false;
		item.type = type;
		
		list.add(item);
		
		return item;
	}

	private final static CodeItem[] buildMethodAndField(final Class c, final JRubyClassDesc jcd, final boolean isForClass, 
			final HashMap<String, ArrayList<CodeItem>> set, final CodeHelper codeHelper){
		final ArrayList<CodeItem> list = new ArrayList<CodeItem>();
		
		boolean isBuildForClass = false;
		
		if(c != null){
			buildMethodAndProp(c, isForClass, list, true, codeHelper, set, c.getName());
			isBuildForClass = true;
		}
		
		if(jcd == null || jcd.defNode == null){
		}else{
			final String key = getDefClassName(jcd.defNode);
			if(key.startsWith(HC_PACKAGE_CLASS_PREFIX) || key.startsWith(JAVA_PACKAGE_CLASS_PREFIX)){
			}else{
				codeHelper.classCacheMethodAndPropForProject.add(key);
			}
			if(isBuildForClass == false){
				buildMethodAndProp(c, isForClass, list, true, codeHelper, set, key);
			}else{
				list.addAll(set.get(c.getName()));
			}
		}
		
		final Object[] objs = list.toArray();
		final CodeItem[] out = new CodeItem[objs.length];
		System.arraycopy(objs, 0, out, 0, objs.length);
		Arrays.sort(out);
		
		return out;
	}

	private final void appendMethodFromDef(final boolean isForClass, final String className, final DefnNode defN, 
			final ArrayList<CodeItem> list, final boolean isNeedAppendInit) {
		String methodName = defN.getName();
		if(methodName.equals(JRUBY_CLASS_INITIALIZE_DEF)){
			if(isNeedAppendInit == false){
				return;
			}
			methodName = JRUBY_NEW;
		}else{
			if(isForClass){
				return;
			}
		}
		
		final CodeItem item = CodeItem.getFree();
		
		final ArgsNode argsNode = defN.getArgs();
		final int parameterNum = argsNode.getMaxArgumentsCount();//获得参数个数
		
		final StringBuilder sb = StringBuilderCacher.getFree();
		if(parameterNum == 0){
			sb.append(methodName).append("()");
		}else{
			sb.append(methodName);
			sb.append("(");

			final int parameterStartIdx = sb.length();

			final List<Node> paraList = argsNode.childNodes();
			final ListNode parametersNode = (ListNode)paraList.get(0);//注意：org.jrubyparser.ast.ArrayNode extends org.jrubyparser.ast.ListNode

			for (int j = 0; j < parameterNum; j++) {
				try{
					final ArgumentNode parameterNode = (ArgumentNode)parametersNode.get(j);
					final String name = parameterNode.getName();
					if(sb.length() > parameterStartIdx){
						sb.append(", ");
					}
					sb.append(name);
				}catch (final Throwable e) {
					e.printStackTrace();
				}
			}
			sb.append(")");
		}
		
		item.code = sb.toString();
		StringBuilderCacher.cycle(sb);
		item.codeForDoc = item.code;
		item.fieldOrMethodOrClassName = methodName;
		item.codeDisplay = item.code + " - " + className;
		item.codeLowMatch = item.code.toLowerCase();
		item.fmClass = IterNodeClass;
		
		item.modifiers = Modifier.PUBLIC | Modifier.FINAL;//假定如此，以不能让其被def override
		item.isPublic = true;
		item.isDefed = true;
		item.isForMaoHaoOnly = false;//仅限new
		item.type = CodeItem.TYPE_METHOD;
		
		final int size = list.size();
		for (int i = 0; i < size; i++) {
//			final CodeItem matchItem = list.get(i);
//			if(matchItem.isDefed == false && matchItem.fieldOrMethodOrClassName.equals(item.fieldOrMethodOrClassName)){//不能使用fieldOrMethodOrClassName，因为可能参数不一
			if(list.get(i).codeDisplay.equals(item.codeDisplay)){//由于run() - YesRun VS run() - Runnable，故改为
				if(L.isInWorkshop){
					LogManager.log("item.codeDisplay = " + item.codeDisplay + " is added in list, skip add.");
				}
				return;
			}
		}
		
		list.add(item);
	}

	private static String getDefClassName(final ClassNode classNode) {
		return classNode.getCPath().getName();
	}
	
	/**
	 * 为实例查找可用的方法和属性
	 */
	public final ArrayList<CodeItem> getMethodAndFieldForInstance(final JRubyClassDesc jdc, final boolean needPublic, 
			final ArrayList<CodeItem> out, final boolean isAppend, final boolean isNeedAppendInit){
		return getMethodAndField(jdc, needPublic, out, classCacheMethodAndPropForInstance, false, isAppend, isNeedAppendInit);
	}
	
	/**
	 * 为类查找可用的静态方法和属性
	 * @param c
	 * @param preName
	 * @param out
	 * @return
	 */
	public final ArrayList<CodeItem> getMethodAndFieldForClass(final JRubyClassDesc jcd, final ArrayList<CodeItem> out,
			final boolean isAppend, final boolean isNeedAppendInit){
		return getMethodAndField(jcd, true, out, classCacheMethodAndPropForClass, true, isAppend, isNeedAppendInit);
	}
	
	private final void appendInterfaces(final Vector<Class> includes, final ArrayList<CodeItem> out){
		if(includes == null){
			return;
		}
		
		final int size = includes.size();
		for (int i = 0; i < size; i++) {
			final Class claz = includes.get(i);
			final boolean needPublic = false;
			final boolean needNewMethod = false;
			append(claz, classCacheMethodAndPropForInstance, out, needPublic, needNewMethod);
		}
	}
	
	private final void append(final Class claz, final HashMap<String, ArrayList<CodeItem>> set, final ArrayList<CodeItem> out, 
			final boolean needPublic, final boolean needNewMethod){
		final String className = claz.getName();
		ArrayList<CodeItem> methods = set.get(className);
		if(methods == null){
			buildForClass(this, null, claz);
			methods = set.get(className);
		}
		if(methods != null){
			appendToOut(methods, needPublic, out, needNewMethod);
		}
	}
	
	private final ArrayList<CodeItem> getMethodAndField(final JRubyClassDesc jdc, final boolean needPublic, 
			final ArrayList<CodeItem> out, final HashMap<String, ArrayList<CodeItem>> set, final boolean isForClass,
			final boolean isAppend, final boolean isNeedAppendInit){
		if(isAppend == false){
			clearArray(out);
		}
		
		final ClassNode defNode = jdc.defNode;
		
		//仅取定义体中的
		final boolean isDefNode = defNode != null;

		{
			final Class backgroundOrPreVar = jdc.baseClass;
			final String className = backgroundOrPreVar.getName();
			
			ArrayList<CodeItem> methods = set.get(className);
			if(methods == null){
				buildForClass(this, buildJRubyClassDesc(backgroundOrPreVar, false));
				methods = set.get(className);
			}
			
			appendToOut(methods, needPublic, out, isDefNode?false:true);
		}
		
		if(jdc != null && jdc.defNode != null){
			ArrayList<CodeItem> methods = set.get(getDefClassName(jdc.defNode));
			if(methods == null){
				buildForClass(this, jdc);
				methods = set.get(getDefClassName(jdc.defNode));
			}
			if(methods != null){
				appendToOut(methods, needPublic, out, true);
			}
			if(jdc != null && jdc.defNode != null){
				appendDef(jdc.defNode, isForClass, out, isNeedAppendInit);
			}
			if(out.size() > 0){
				return out;
			}
		}
		
		if(isDefNode){
			ArrayList<CodeItem> methods = set.get(getDefClassName(defNode));
			if(methods == null && jdc != null){
				if(methods == null){
					buildForClass(this, jdc);
					methods = set.get(getDefClassName(defNode));
				}
				if(methods != null){
					appendToOut(methods, needPublic, out, true);
				}
				if(jdc != null && jdc.defNode != null){
					appendDef(jdc.defNode, isForClass, out, isNeedAppendInit);
				}
			}
			if(out.size() > 0){
				return out;
			}
		}
		
		if(isDefNode){
			appendDef(defNode, isForClass, out, isNeedAppendInit);
		}else if(jdc != null && jdc.innerDefNode != null){
			appendDef(jdc.innerDefNode, false, out);
		}
		return out;
	}

	private ArrayList<CodeItem> appendToOut(final ArrayList<CodeItem> methods,
			final boolean needPublic, final ArrayList<CodeItem> out, final boolean needNewMethod) {
		final int size = methods.size();
		for (int i = 0; i < size; i++) {
			final CodeItem tryMethod = methods.get(i);
			if(needPublic){
				if(tryMethod.isPublic){
				}else{
					continue;
				}
			}
			if(needNewMethod == false && JRUBY_NEW == tryMethod.fieldOrMethodOrClassName){//defined JRuby class is NOT required super class new methods.
				continue;
			}
			
			if(preCodeSplitIsDot){
				if(tryMethod.isForMaoHaoOnly){
					continue;
				}
			}
			{
//				final CodeItem item = CodeItem.getFree();
//				item.copyFrom(tryMethod);
//				out.add(item);
				
				out.add(tryMethod);
			}
		}
		
		return out;
	}

	private final static void buildForClass(final CodeHelper codeHelper, final JRubyClassDesc jcd) {
		buildForClass(codeHelper, jcd, jcd.baseClass);
	}
	
	public static void buildForClass(final CodeHelper codeHelper, final JRubyClassDesc jcd, final Class claz) {
		//注意：以下两行，次序不能变动，因docHelper.processDoc依赖最后一个
		buildMethodAndField(claz, jcd, true, codeHelper.classCacheMethodAndPropForClass, codeHelper);
		buildMethodAndField(claz, jcd, false, codeHelper.classCacheMethodAndPropForInstance, codeHelper);
	}

	private final void appendDef(final CallNode innerNode, final boolean isForClass, final ArrayList<CodeItem> list) {
		final Node body = ((IterNode)innerNode.getIter()).getBody();
		appendFromDefBody(body, DEF_MEMBER, isForClass, list, false);
	}
	
	private final void appendDef(final ClassNode defNode, final boolean isForClass, final ArrayList<CodeItem> list,
			final boolean isNeedAppendInit) {
		//将定义体的类的常量和构造方法提取出来，装入代码提示表中
		if(defNode != null){
			
			final ClassNode classNode = defNode;
			final String className = getDefClassName(classNode);
			final Node body = classNode.getBody();//注意：有可能只定义类，而没有定义体，导致body为null
			appendFromDefBody(body, className, isForClass, list, isNeedAppendInit);
		}
	}

	private final void appendFromDefBody(final Node body, final String className,
			final boolean isForClass, final ArrayList<CodeItem> list, final boolean isNeedAppendInit) {
		if(body == null){
			if(isNeedAppendInit){
				appendDefaultInitialize(isForClass, list, className);
			}
			return;
		}
		
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
				item.fieldOrMethodOrClassName = constNode.getName();
				item.code = constNode.getName();
				item.codeForDoc = item.code;
				final Class classOfConst = getClassFromLiteral(constNode.getValue());
				item.fmClass = classOfConst.getName();//Object.class.getName();
				item.codeDisplay = item.code + " : " + classOfConst.getSimpleName() + " - " + className;
				item.codeLowMatch = item.code.toLowerCase();
				item.isPublic = true;
				item.isDefed = true;
				item.isForMaoHaoOnly = false;
				item.type = CodeItem.TYPE_FIELD;
				
				list.add(item);
			}else if(sub instanceof DefnNode){
				appendMethodFromDef(isForClass, className, (DefnNode)sub, list, isNeedAppendInit);
			}
		}
		
		if(isNeedAppendInit){
			appendDefaultInitialize(isForClass, list, className);
		}
	}

	public final void appendDefaultInitialize(final boolean isForClass,
			final ArrayList<CodeItem> list, final String className) {
		final ArgsNode args = new ArgsNode(null, null, null, null, null, null, null, null);
		final MethodNameNode methodName = new MethodNameNode(null, JRUBY_CLASS_INITIALIZE_DEF);
		final DefnNode defaultNew = new DefnNode(null, methodName, args, null, null);
		appendMethodFromDef(isForClass, className, defaultNew, list, true);
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
			clearArray(out);
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
				
				if(classAndRes != null){//有可能添加后，又删除了库，但代码仍保留，不拦截此错误，脚本自动编译时，会产生错误提示
					appendPackageAndClass(out, classAndRes, classAndRes.length, false);
				}else{
					App.showMessageDialog(null, "NOT found lib '" + libName + "', but it is required in scripts!", ResourceUtil.getErrorI18N(), 
							JOptionPane.ERROR_MESSAGE, App.getSysIcon(App.SYS_ERROR_ICON));
					throw new Error("No found or removed lib : " + libName);
				}
			}
		}
		
		Collections.sort(out);
		
		return out;
	}

	public final ArrayList<CodeItem> getResources(final ArrayList<CodeItem> out, final ArrayList<String> requireLibs, final boolean isAppend){
		if(isAppend == false){
			clearArray(out);
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

	public static void clearArray(final ArrayList<CodeItem> out) {
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
					item.codeForDoc = item.code;
					item.codeDisplay = pkg;
					item.codeLowMatch = pkg.toLowerCase();
					out.add(item);
				}
			}else if(isResourceOnly == false){
				final CodeItem item = CodeItem.getFree();
				item.type = CodeItem.TYPE_CLASS;
				item.fieldOrMethodOrClassName = pkg;
				item.code = pkg;
				item.codeForDoc = item.code;
				item.codeDisplay = pkg;
				item.codeLowMatch = pkg.toLowerCase();
				item.isFullPackageAndClassName = true;
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
		window = new CodeWindow(this);
		buildForClass(this, null, int.class);
		buildForClass(this, null, float.class);
		
		final JRubyClassDesc jcd = buildJRubyClassDesc(JRUBY_CLASS_FOR_BULDER, false);
		final CodeItem[] out = buildMethodAndField(JRUBY_CLASS_FOR_BULDER, jcd, true, classCacheMethodAndPropForClass, this);
		for (int i = 0; i < out.length; i++) {
			out[i].isInnerClass = true;
		}
		
		buildJRubyClassDesc(JRUBY_JAVA_CLASS_AGENT, false);
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
	
	private final static void printNode(final Node node){
		final StringBuilder sb = StringBuilderCacher.getFree();
		buildNodeString(node, 0, sb);
		LogManager.log(sb.toString());
		StringBuilderCacher.cycle(sb);
	}
	
	private final static void buildNodeString(final Node node, final int deep, final StringBuilder sb){
		if(sb.length() > 0){
			sb.append("\n");
		}
		for (int i = 0; i < deep; i++) {
			sb.append(" ");
		}
		sb.append(ClassUtil.invoke(Node.class, node, "getNodeName", ClassUtil.nullParaTypes, ClassUtil.nullParas, true));

        if (node instanceof INameNode) {
            sb.append(":").append(((INameNode)node).getName());
        }
        
        for (final Node child: node.childNodes()) {
        	buildNodeString(child, deep+1, sb);
        }
        sb.append("\n");
        for (int i = 0; i < deep; i++) {
			sb.append(" ");
		}
        sb.append(")");
	}
	
	public final static JRubyClassDesc isInDefClass(final Node node, final CodeContext codeContext, final int rowIdx){
//		if(L.isInWorkshop){
//			final StringBuilder sb = new StringBuilder(1024 * 10);
//			buildNodeString(node, 0, sb);
//			LogManager.log(sb.toString());
//		}
//		final List<Node> childNodes = node.childNodes();
//		if(childNodes.size() > 0){
//			final List<Node> list = childNodes.get(0).childNodes();
//			final int size = list.size();
//			for (int i = 0; i < size; i++) {
//				final List<Node> childNodes2 = list.get(i).childNodes();
//				if(childNodes2.size() == 0){
//					continue;
//				}
//				final Node classNode = childNodes2.get(0);
//				if(classNode instanceof ClassNode){
//					final SourcePosition position = classNode.getPosition();
//					if(position.getStartLine() < rowIdx && rowIdx < position.getEndLine()){
//						return buildJRubyClassDescForDef(codeContext, (ClassNode)classNode);
////						return (ClassNode)classNode;
//					}
//				}else if(classNode instanceof SClassNode){//class << noRun 
//					//(SClassNode, (LocalVarNode:noRun), (NewlineNode, (DefnNode:run, 
//					final SourcePosition position = classNode.getPosition();
//					if(position.getStartLine() < rowIdx && rowIdx < position.getEndLine()){
//						return buildClassDescFromSClassNode(codeContext, classNode);
//					}
//				}else{
					Node idxNode = searchClassNodeWrapRowIdx(node, rowIdx);
					boolean isSearchIter = false;
					while(idxNode != null){
						
						if(idxNode instanceof ClassNode){
							return buildJRubyClassDescForDef(codeContext, (ClassNode)idxNode);
						}else if(idxNode instanceof SClassNode){
							return buildClassDescFromSClassNode(codeContext, idxNode);
						}
						
						if(isSearchIter == false){//DefnNode
							isSearchIter = true;
							final JRubyClassDesc out = searchIterNode(idxNode, codeContext);//注意：优先查找
							if(out != null){
								return out;
							}
						}
						
						idxNode = idxNode.getParent();//注意：查找最内层的ClassNode和SClassNode
					}
//				}
//			}
//		}
		return null;
	}

	private static JRubyClassDesc buildClassDescFromSClassNode(final CodeContext codeContext,
			final Node classNode) {
		final SClassNode sClassNode = (SClassNode)classNode;
		final JRubyClassDesc base = findClassFromRightAssign(sClassNode.getReceiver(), codeContext);//LocalVarNode:noRun
		final JRubyClassDesc classDesc = buildJRubyClassDesc(base.baseClass, true);
		classDesc.defSClassNode = sClassNode;
		return classDesc;
	}
	
	public final static Node searchClassNodeWrapRowIdx(final Node contain, final int rowIdx){
		if(isClassNodeWrapRowIdx(contain, rowIdx) == false){
			return null;
		}
		
		final List<Node> list = contain.childNodes();
		final int size = list.size();
		
		for (int i = size - 1; i >= 0; i--) {
			final Node subNode = list.get(i);
			if(isClassNodeWrapRowIdx(subNode, rowIdx)){
				return searchClassNodeWrapRowIdx(subNode, rowIdx);
			}
		}
		
		return contain;
	}
	
	private static boolean isClassNodeWrapRowIdx(final Node contain, final int rowIdx){
		final SourcePosition position = contain.getPosition();
		if(position.getStartLine() < rowIdx && rowIdx < position.getEndLine()){
			return true;
		}
		return false;
	}
	
	private final static JRubyClassDesc searchIterNode(Node node, final CodeContext codeContext){
		while(true){
			if(node instanceof IterNode){
				break;
			}
			
			node = node.getParent();
			if(node == null){
				return null;
			}
		}
		
		final Node parentNode = node.getParent();
		
		final JRubyClassDesc out = buildDescFromClassNewCallNode(parentNode, codeContext);
		if(out == null){
			return null;
		}
		
		out.defIterNode = (IterNode)node;
		return out;
	}

	private static JRubyClassDesc buildDescFromClassNewCallNode(final Node callNode,
			final CodeContext codeContext) {
		if((callNode instanceof CallNode) == false){
			return null;
		}
		
		final CallNode innerClass = (CallNode)callNode;
		if(JRUBY_NEW.equals(innerClass.getName()) == false){
			return null; 
		}
		
		final Node receive = innerClass.getReceiver();
		if(receive instanceof ConstNode){
			final ConstNode cNode = (ConstNode)receive;
			if(JRUBY_CLASS_FOR_NEW.equals(cNode.getName())){
				final Node aNode = innerClass.getArgs();
				final Node constNode = aNode.childNodes().get(0);
				final Class claz = getClassByNode(constNode, codeContext);
				final JRubyClassDesc desc = buildJRubyClassDesc(claz, true);
				desc.innerDefNode = innerClass;
				return desc;
			}
		}
		return null;
//		CallNode:new, (ConstNode:Class), 
//		(ArrayNode, 
//				(*)
//		), 
//		(IterNode, 
//				(NewlineNode, 
//						(DefnNode:onEvent
//						)
//				)
//		)
	}
	
	public final static Class getDefSuperClass(final Node node, final int scriptIdx, final int lineIdxAtScript, final CodeContext codeContext){
		final JRubyClassDesc classNode = isInDefClass(node, codeContext, lineIdxAtScript);
		if(classNode != null){
			return classNode.baseClass;
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
		HCTimer.remove(mouseExitHideDocForMouseMovTimer);
	}
	
	private final void initRequire(final DefaultMutableTreeNode jarFolder){
		final Enumeration enumeration = jarFolder.children();
		while(enumeration.hasMoreElements()){
			final DefaultMutableTreeNode node = (DefaultMutableTreeNode)enumeration.nextElement();
			loadLibToCodeHelper(node);
		}
	}
	
	static final Parser rubyParser = new Parser();
	static final ParserConfiguration config = new ParserConfiguration(0, CompatVersion.getVersionFromString(HCJRubyEngine.JRUBY_VERSION));
	static final Node emptyStringNode = parseScripts("");
	
    public static Node parseScripts(final String cmds) {
        final StringReader in = new StringReader(cmds);
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
    public final static JRubyClassDesc findParaClass(final CodeContext contextNode, final String parameter, final int typeParameter){
    	final Node subNode = contextNode.bottomNode;//searchParaCallingNodeByIdx(contextNode.contextNode, contextNode.scriptIdx);
    	
    	if(subNode != null){
    		final ClassNode defClassNode = contextNode.getDefClassNode();
			final Node varDefNode = findVarDefNode(subNode, parameter, typeParameter, null, false, defClassNode != null, contextNode);
    		if(varDefNode != null){
    			final NodeType nodeType = varDefNode.getNodeType();
    			if(varDefNode instanceof AssignableNode){
    				return findClassFromRightAssign(((AssignableNode)varDefNode).getValue(), contextNode);
    			}else if(nodeType == NodeType.FCALLNODE){
    				final ListNode importArgs = (ListNode)((FCallNode)varDefNode).getArgs();//注意：org.jrubyparser.ast.ArrayNode extends org.jrubyparser.ast.ListNode
					final CallNode firstPackageNode = (CallNode)importArgs.childNodes().get(0);
					final String className = buildFullClassNameFromReceiverNode(firstPackageNode);
					return buildJRubyClassDesc(findClassByName(className, true), false);
    			}else if(nodeType == NodeType.CLASSNODE){
    				final JRubyClassDesc classDesc = buildJRubyClassDesc(getDefSuperClass((ClassNode)varDefNode, contextNode), false);
					classDesc.isDefClass = true;
					classDesc.defNode = (ClassNode)varDefNode;
					buildForClass(contextNode.codeHelper, classDesc);//直接在MyDefClass::进行代码提示要求
//					appendDefForClass(jcd, isForClass, list);
					return classDesc;
    			}else if(nodeType == NodeType.DEFNNODE){//方法体定义
    				if(defClassNode != null){
        				final JRubyClassDesc cd = findParaClassFromArgu(varDefNode, null, defClassNode, parameter, contextNode);
        				if(cd != null){
        					return cd;
        				}
    				}else{
    					//Class.new
    					final Class superClass = searchInnerParentClassFromDef(varDefNode, contextNode);
    					if(superClass != null){
	    					final JRubyClassDesc cd = findParaClassFromArgu(varDefNode, superClass, defClassNode, parameter, contextNode);
	        				if(cd != null){
	        					return cd;
	        				}
    					}
    				}
    				return buildJRubyClassDesc(Object.class, true);
    			}else if(nodeType == NodeType.CALLNODE && JRUBY_NEW.equals(((CallNode)varDefNode).getName())){
    				return buildDescFromCallNodeNew((CallNode)((CallNode)varDefNode).getReceiver(), contextNode);
    			}
    		}
    	}
    	
    	return null;
    }
    
    private static Class searchInnerParentClassFromDef(final Node def, final CodeContext contextNode){
    	try{
	    	final Node callNode = def.getParent().getParent().getParent();
	    	
	    	if(callNode instanceof CallNode){
	    		final JRubyClassDesc desc = buildDescFromClassNewCallNode(callNode, contextNode);
				if(desc != null){
					return desc.baseClass;
				}
	    	}else if(callNode instanceof IterNode){
	    		final JRubyClassDesc desc = searchIterNode(callNode, contextNode);
	    		if(desc != null){
					return desc.baseClass;
				}
	    	}
    	}catch (final Throwable e) {
		}
    	
    	return null;
    }

	private final static JRubyClassDesc findParaClassFromArgu(final Node varDefNode,
			Class superClass, final ClassNode defClassNode, final String parameter, final CodeContext codeContext) {
		final DefnNode defNode = (DefnNode)varDefNode;
		final String methodName = defNode.getName();
		final ArgsNode argsNode = defNode.getArgs();
		final int parameterNum = argsNode.getMaxArgumentsCount();//获得参数个数
		int parameterIdx = -1;
		if(parameterNum > 0){
			final List<Node> list = argsNode.childNodes();
			final ListNode parametersNode = (ListNode)list.get(0);//注意：org.jrubyparser.ast.ArrayNode extends org.jrubyparser.ast.ListNode

			for(int i = 0; i<parameterNum; i++){
					try{
					final ArgumentNode parameterNode = (ArgumentNode)parametersNode.get(i);
					final String name = parameterNode.getName();
					if(name.equals(parameter)){
						parameterIdx = i;
						break;
					}
				}catch (final Throwable e) {
					e.printStackTrace();
				}
			}
		}
		
		if(parameterIdx >= 0){
			if(superClass == null){
				superClass = getDefSuperClass(defClassNode, codeContext);
			}
			{
				final Class methodParaType = searchOverrideMethodParameterType(superClass, methodName, parameterIdx);
				if(methodParaType != null){
					return buildJRubyClassDesc(methodParaType, true);
				}
			}
			
			final JRubyClassDesc desc = buildJRubyClassDescForDef(codeContext, defClassNode);
			if(desc != null){
				//可能是仅实现接口的类型
				final Vector<Class> v = desc.include;
				if(v != null){
					final int size = v.size();
					for (int i = 0; i < size; i++) {
						final Class impl = v.get(i);
						final Class methodParaType = searchOverrideMethodParameterType(impl, methodName, parameterIdx);
						if(methodParaType != null){
							return buildJRubyClassDesc(methodParaType, true);
						}
					}
				}
			}
		}
		return null;
	}
    
    private static Node searchParaCallingNodeByIdx(final Node node, final int rowIdx){
    	if(node == null){
    		return null;
    	}
    	
    	if(node instanceof ClassNode){
    		final ClassNode clazNode = (ClassNode)node;
    		final Node body = clazNode.getBody();
    		if(body != null){
    			return searchParaCallingNodeByIdx(body, rowIdx);
    		}else{
    			return node;//class ProjMlet < Java::hc.server.ui.Mlet\nend\n
    		}
    	}else if(node instanceof DefnNode){//方法定义
    		final DefnNode defNode = (DefnNode)node;
    		final Node out = searchParaCallingNodeByIdx(defNode.getBody(), rowIdx);
    		return ((out == null)?defNode:out);
    	}
		final List<Node> list = node.childNodes();
		final int size = list.size();
		
		if(size == 1){
			final SourcePosition position = node.getPosition();
			final int endLine = position.getEndLine();
			if(rowIdx <= endLine){//最后一行为==
				return searchParaCallingNodeByIdx(list.get(0), rowIdx);
			}else{
				return node;
			}
		}else{
			for (int i = size - 1; i >= 0; i--) {
				final Node subNode = list.get(i);
				final SourcePosition position = subNode.getPosition();//注意：回车归属于下行，位于行尾的光标与回车getStartOffset相等
				if(position.getStartLine() < rowIdx && rowIdx <= position.getEndLine()){//所以，此处position.getStartOffset() < inputIdx，不能用<=
					return searchParaCallingNodeByIdx(subNode, rowIdx);
				}
			}
			
			return node;
		}
    }
    
    public final static int TYPE_VAR_UNKNOW = 1;
    public final static int TYPE_VAR_LOCAL = 1 << 1;
    public final static int TYPE_VAR_GLOBAL = 1 << 2;
    public final static int TYPE_VAR_INSTANCE = 1 << 3;
    
    private static boolean isInnerClassDefForNew(final Node node){
    	return node.getNodeType() == NodeType.CALLNODE && JRUBY_NEW.equals(((CallNode)node).getName()) &&  isInnerClassDef(((CallNode)node).getReceiver());
    }
    
    private static boolean isInnerClassDef(final Node node){
    	if(node == null || node.getNodeType() != NodeType.CALLNODE){
    		return false;
    	}
    	
    	final CallNode callNode = (CallNode)node;
    	
		if(callNode.getName().equals(JRUBY_NEW)){
			final Node receiver = callNode.getReceiver();
			if(receiver != null && receiver instanceof ConstNode && ((ConstNode)receiver).getName().equals(JRUBY_CLASS_FOR_NEW)){
				return true;
			}
		}
		
    	return false;
    }
    
    private static Node isAssignableNode(final AssignableNode varDefNode, final String parameter, final int typeParameter){
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
			return null;
		}
		
		if(searchParaName.equals(parameter)){
			return varDefNode;
		}
		return null;
    }
    
    private static Node findVarDefNode(final Node bottomNode, final String parameter, final int typeParameter, 
    		final Node fromNode, final boolean downForward, final boolean isInDefClass, final CodeContext codeContext){
    	if(bottomNode == null){
    		return null;
    	}
    	final NodeType nodeType = bottomNode.getNodeType();
    	if(typeParameter == TYPE_VAR_LOCAL && nodeType == NodeType.DEFNNODE){//方法定义体
    		final DefnNode defNode = (DefnNode)bottomNode;
			final ArgsNode argsNode = defNode.getArgs();
			final int parameterNum = argsNode.getMaxArgumentsCount();//获得参数个数
			if(parameterNum > 0){
				final List<Node> list = argsNode.childNodes();
				final ListNode parametersNode = (ListNode)list.get(0);//注意：org.jrubyparser.ast.ArrayNode extends org.jrubyparser.ast.ListNode
				for(int i = 0; i<parameterNum; i++){
					try{
						final ArgumentNode parameterNode = (ArgumentNode)parametersNode.get(i);
						final String name = parameterNode.getName();
						
						if(name.equals(parameter)){
							return defNode;
						}
					}catch (final Throwable e) {
						e.printStackTrace();
					}
				}
			}
    	}
    	
    	final List<Node> list = bottomNode.childNodes();
    	final int size = list.size();
    	int bottomIdx = size - 1;
    	if(fromNode != null){
	    	for (int i = bottomIdx; i >= 0; i--) {
				if(list.get(i).comparePositionWith(fromNode) >= 0){
					bottomIdx = i;
					break;
				}
			}
	    }
    	for (int i = bottomIdx; i >= 0; i--) {
			Node varDefNode = list.get(i);
			if(varDefNode instanceof NewlineNode){
				varDefNode = varDefNode.childNodes().get(0);
			}
			
			if(varDefNode instanceof AssignableNode){
				final Node out = isAssignableNode((AssignableNode)varDefNode, parameter, typeParameter);
				if(out != null){
					return out;
				}else{
					continue;
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
						final ListNode importArgs = (ListNode)callNode.getArgs();//注意：org.jrubyparser.ast.ArrayNode extends org.jrubyparser.ast.ListNode
						final CallNode firstPackageNode = (CallNode)importArgs.childNodes().get(0);
						if(firstPackageNode.getName().equals(parameter)){
							return varDefNode;
						}
					}
				}
				continue;
//			}else if(varDefNode instanceof NewlineNode){
//				final Node node = varDefNode.childNodes().get(0);
//				
//				if(node instanceof AssignableNode){
//					final Node out = isAssignableNode((AssignableNode)node, parameter, typeParameter);
//					if(out != null){
//						return out;
//					}
//				}
//				
//				if(node != fromNode){
//					if(node instanceof DefnNode){
//						final Node out = searchVarDefInUserDefMethod((DefnNode)node, parameter);
//						if(out != null){
//							return out;
//						}
//					}
//					final Node aNode = findVarDefNode(node, parameter, typeParameter, null, true, isInDefClass, codeContext);
//					if(aNode != null){
//						return aNode;
//					}
//				}
			}else if(isInDefClass && (typeParameter == TYPE_VAR_INSTANCE || typeParameter == TYPE_VAR_GLOBAL) && varDefNode instanceof ClassNode){
				//(ClassNode, (Colon2ImplicitNode:MyClass), (BlockNode, (NewlineNode, (DefnNode:initialize, (MethodNameNode:initialize), (ArgsNode), 
				final ClassNode defClass = (ClassNode)varDefNode;
				final List<MethodDefNode> lists = defClass.getMethodDefs();
				final int methodSize = lists.size();
				
				//优先从initialize中找
				for (int j = 0; j < methodSize; j++) {
					final MethodDefNode aMethod = lists.get(j);
					if(aMethod.getName().equals(JRUBY_CLASS_INITIALIZE_DEF)){
						final Node defNode = searchVarDefInInitializeMethod(aMethod.getBody(), parameter, typeParameter, codeContext);
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
						final Node defNode = searchVarDefInInitializeMethod(aMethod.getBody(), parameter, typeParameter, codeContext);
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
    		final Node out = findVarDefNode(bottomNode.getParent(), parameter, typeParameter, bottomNode, downForward, isInDefClass, codeContext);
    		if(out != null){
    			return out;
    		}
    	}
    	
    	if(nodeType == NodeType.CALLNODE && JRUBY_NEW.equals(((CallNode)bottomNode).getName()) && isInnerClassDefForNew(bottomNode)){
     		//(CallNode:new, (CallNode:new, (ConstNode:Class), (ArrayNode, (ConstNode:Object)), (IterNode, 
     		return bottomNode;
     	}
    	
    	return null;
    }
    
    private static Node searchVarDefInUserDefMethod(final DefnNode defNode, final String parameter){
    	try{
			final ArgsNode argsNode = defNode.getArgs();
			final int parameterNum = argsNode.getMaxArgumentsCount();//获得参数个数
			if(parameterNum > 0){
				final List<Node> list = argsNode.childNodes();
				final ListNode parametersNode = (ListNode)list.get(0);//注意：org.jrubyparser.ast.ArrayNode extends org.jrubyparser.ast.ListNode

				for(int i = 0; i<parameterNum; i++){
					try{
						final ArgumentNode parameterNode = (ArgumentNode)parametersNode.get(i);
						final String name = parameterNode.getName();
						if(name.equals(parameter)){
							return defNode;
						}
					}catch (final Throwable e) {
						e.printStackTrace();
					}
				}
			}
    	}catch (final Throwable e) {
    		e.printStackTrace();
    	}
    	return null;
    }
    
    private static final Node searchVarDefInInitializeMethod(final Node methodBody, final String parameter, 
    		final int typeParameter, final CodeContext codeContext){
    	if(methodBody == null){
    		return null;
    	}
    	
    	final List<Node> list = methodBody.childNodes();
    	final int size = list.size();
    	Node out = null;
    	
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
				final Node result = searchVarDefInInitializeMethod(varDefNode, parameter, typeParameter, codeContext);
				if(result != null){
					if(result instanceof AssignableNode){
	    				final JRubyClassDesc classDesc = findClassFromRightAssign(((AssignableNode)result).getValue(), codeContext);
	    				if(classDesc == NIL_CLASS_DESC){//被赋于nil
	    					out = result;//备选项
	    					continue;//查找更优定义，比如@ctx = getProjectContext()
	    				}
	    			}
					
					return result;
				}
			}
		}
    	return out;
    }
    
    private static final void appendVarDefInInitializeMethod(final Node methodBody, final int typeParameter, final ArrayList<CodeItem> out){
    	if(methodBody == null){
    		return;
    	}
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
				item.fieldOrMethodOrClassName = searchParaName;
				item.code = searchParaName;
				item.codeForDoc = item.code;
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
				item.fieldOrMethodOrClassName = searchParaName;
				item.code = searchParaName;
				item.codeForDoc = item.code;
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
    
    private final static JRubyClassDesc findClassFromRightAssign(final Node value, final CodeContext codeContext){
    	if(value instanceof CallNode){
    		return findClassFromCallNode((CallNode)value, codeContext);
    	}else if(value instanceof ILiteralNode){
    		return buildJRubyClassDesc(getClassFromLiteral(value), true);
    	}else if(value instanceof LocalVarNode){
    		final LocalVarNode lvn = (LocalVarNode)value;
    		final CodeContext newCodeCtx = new CodeContext(codeContext.codeHelper, codeContext.contextNode, lvn.getPosition().getStartLine());
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

	private final static JRubyClassDesc findClassFromCallNode(final CallNode call, final CodeContext codeContext) {
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
				final ListNode arrayNode = (ListNode)parametersNode;//注意：org.jrubyparser.ast.ArrayNode extends org.jrubyparser.ast.ListNode
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
			
			final Class[] paraClass = findClasssFromArgs((ListNode)parametersNode, codeContext);//注意：org.jrubyparser.ast.ArrayNode extends org.jrubyparser.ast.ListNode

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
	
	private final static JRubyClassDesc findMethodReturnByCallNode(final CallNode call, final String methodName, final Class[] paraClass, final CodeContext codeContext) {
		final Node receiveNode = call.getReceiver();

		final JRubyClassDesc receiverClass = findClassFromReceiverNode(receiveNode, false, codeContext);
		final Class baseClass = receiverClass.baseClass;
		return findMethodReturn(baseClass, methodName, paraClass);
	}

	public static JRubyClassDesc findMethodReturn(Class baseClass, final String methodName, final Class[] paraClass) {
		JRubyClassDesc out = findMatchMehtodForReturn(baseClass.getMethods(), methodName, paraClass);
		
		if(out != null){
			return out;
		}
		
		Method jmethod = null;
		try{
			jmethod = baseClass.getMethod(methodName, paraClass);
		}catch (final Exception e) {
		}
		
		if(jmethod == null){
			out = findMatchMehtodForReturn(baseClass.getDeclaredMethods(), methodName, paraClass);
			if(out != null){
				return out;
			}else{
				while(true){
					if(baseClass == Object.class){
						return null;
					}
					baseClass = baseClass.getSuperclass();
					out = findMatchMehtodForReturn(baseClass.getDeclaredMethods(), methodName, paraClass);
					if(out != null){
						return out;
					}
				}
			}
		}else{
			return buildJRubyClassDesc(jmethod.getReturnType(), true);
		}
	}

	private static JRubyClassDesc findMatchMehtodForReturn(final Method[] methods,
			final String methodName, final Class[] paraClass) {
		if(JRUBY_JAVA_CLASS.equals(methodName)){
			return buildJRubyClassDesc(JRUBY_JAVA_CLASS_AGENT, false);
		}
		
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
		return null;
	}
	
	public final static boolean IS_EXTEND = true;
	
	private final static JRubyClassDesc buildJRubyClassDesc(final Class basClass, final boolean isInstance){
		final JRubyClassDesc classDesc = new JRubyClassDesc();
		classDesc.baseClass = basClass;
		classDesc.isInstance = isInstance;
		
		return classDesc;
	}
	
	private final static JRubyClassDesc preFindClassFromReceiverNode(final Node node, final CodeContext codeContext, 
			final String paraName){
		if(node != null){
			if(node instanceof AssignableNode){
				return findClassFromReceiverNode(node.childNodes().get(0), false, codeContext);
			}else if(node instanceof DefnNode){
				final Class superClass = searchInnerParentClassFromDef(node, codeContext);
				if(superClass != null){
					final JRubyClassDesc cd = findParaClassFromArgu(node, superClass, null, paraName, codeContext);
					if(cd != null){
						return cd;
					}
				}
				
				final ClassNode cNode = codeContext.getDefClassNode();
				final JRubyClassDesc cd = findParaClassFromArgu(node, null, cNode, paraName, codeContext);
				if(cd != null){
					return cd;
				}
			}
		}
		return null;
	}
	
	private final static Class searchSelfParentDefNode(final Node node, final CodeContext codeContext){
		try{
			//class abc < exttend {
			//  def initialize
			//     myA = self
			//  end
			//}
			final Node defNode = node.getParent().getParent().getParent().getParent().getParent().getParent();//只能结构尝试
			if(defNode instanceof ClassNode){
				return getDefSuperClass((ClassNode)defNode, codeContext);
			}
			
			//Class.new(){
			//}
			final Node callNode = defNode.getParent();//.getParent();
			if(callNode instanceof CallNode){
				final JRubyClassDesc desc = buildDescFromClassNewCallNode(callNode, null);
				if(desc != null){
					return desc.baseClass;
				}
			}
		}catch (final Throwable e) {
		}
		return null;
	}
    
    private final static JRubyClassDesc findClassFromReceiverNode(final Node receiverNode, final boolean isTry, final CodeContext codeContext){
    	if(receiverNode instanceof CallNode){
    		final CallNode callNode = (CallNode)receiverNode;
    		if(callNode.getName().equals(JRUBY_NEW)){
    			final Node receiver = callNode.getReceiver();
    			if(receiver != null && receiver instanceof ConstNode && ((ConstNode)receiver).getName().equals(JRUBY_CLASS_FOR_NEW)){
    				//Class.new(Java::hc.a.b){}.new
    				return buildDescFromCallNodeNew(callNode, codeContext);
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
	    			final Class[] paraClass = findClasssFromArgs((ListNode)parametersNode, codeContext);//注意：org.jrubyparser.ast.ArrayNode extends org.jrubyparser.ast.ListNode
    				return findMethodReturn(getDefSuperClass(defClassNode, codeContext), fcallNode.getName(), paraClass);
	    		}
    		}
    		return null;
    	}else if(receiverNode instanceof SelfNode){
    		final ClassNode defNode = codeContext.getDefClassNode();
    		Class baseClass = null;
    		if(defNode == null){
    			baseClass = searchSelfParentDefNode(receiverNode, codeContext);
    		}else{
    			baseClass = getDefSuperClass(defNode, codeContext);
    		}
    		if(baseClass != null){
    			final JRubyClassDesc cd = buildJRubyClassDesc(baseClass, true);
    			cd.isInExtend = IS_EXTEND;//for example, extend device, assign self to instance field A, access field A then need protected method in code show.
    			return cd;
    		}
    	}else if(receiverNode instanceof LocalVarNode){
    		final LocalVarNode lvn = (LocalVarNode)receiverNode;
    		final Node node = findVarDefNode(codeContext.bottomNode, lvn.getName(), TYPE_VAR_LOCAL, 
    				null, false, codeContext.getDefClassNode() != null, codeContext);
			return preFindClassFromReceiverNode(node, codeContext, lvn.getName());
    	}else if(receiverNode instanceof VCallNode){
    		final VCallNode vcn = (VCallNode)receiverNode;
    		final Node node = findVarDefNode(codeContext.bottomNode, vcn.getName(), TYPE_VAR_LOCAL, 
    				null, false, codeContext.getDefClassNode() != null, codeContext);
			return preFindClassFromReceiverNode(node, codeContext, vcn.getName());
    	}else if(receiverNode instanceof InstVarNode){
    		final InstVarNode ivn = (InstVarNode)receiverNode;
    		final Node node = findVarDefNode(codeContext.bottomNode, ivn.getName(), TYPE_VAR_INSTANCE, 
    				null, false, codeContext.getDefClassNode() != null, codeContext);
			return preFindClassFromReceiverNode(node, codeContext, ivn.getName());
    	}else if(receiverNode instanceof GlobalVarNode){
    		final GlobalVarNode gvn = (GlobalVarNode)receiverNode;
    		final Node node = findVarDefNode(codeContext.bottomNode, gvn.getName(), TYPE_VAR_GLOBAL, 
    				null, false, codeContext.getDefClassNode() != null, codeContext);
			return preFindClassFromReceiverNode(node, codeContext, gvn.getName());
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
    		return NIL_CLASS_DESC;
    	}
    	return null;
    }

	private static JRubyClassDesc buildDescFromCallNodeNew(final CallNode callNode,
			final CodeContext codeContext) {
		final Node args = callNode.getArgs();//(CallNode:new, (ConstNode:Class), (ArrayNode, (CallNode:DeviceCompatibleDescription, (CallNode:msb, (CallNode:server, (CallNode:hc, (ConstNode:Java), (ListNode)), (ListNode))
		final Node node = args.childNodes().get(0);
		final JRubyClassDesc desc = buildJRubyClassDesc(getClassByNode(node, codeContext), true);
		desc.innerDefNode = callNode;
		return desc;
	}
    
    private static final JRubyClassDesc NIL_CLASS_DESC = buildJRubyClassDesc(Object.class, true);

	private static Class findClassByName(final String className, final boolean printError) {
		try {
			return getClassLoader().loadClass(className);
		} catch (final ClassNotFoundException e) {
			if(printError){
				ExceptionReporter.printStackTrace(e);
			}
		}
		return null;
	}
	
	private static Class getClassFromFCallNode(final FCallNode fcallNode, final CodeContext codeContext){
		final FCallNode callNode = fcallNode;
		if(callNode.getName().equals(JRUBY_INCLUDE)){
			final ListNode importArgs = (ListNode)callNode.getArgs();//注意：org.jrubyparser.ast.ArrayNode extends org.jrubyparser.ast.ListNode
			final Node node = importArgs.childNodes().get(0);
			return getClassByNode(node, codeContext);
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
							final ListNode importArgs = (ListNode)callNode.getArgs();//注意：org.jrubyparser.ast.ArrayNode extends org.jrubyparser.ast.ListNode
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
							final JRubyClassDesc classDesc = buildJRubyClassDescForDef(codeContext, cNode);
							
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
//							ListNode importArgs = (ListNode)callNode.getArgs();//注意：org.jrubyparser.ast.ArrayNode extends org.jrubyparser.ast.ListNode
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

	private static JRubyClassDesc buildJRubyClassDescForDef(final CodeContext codeContext, final ClassNode cNode) {
		final JRubyClassDesc classDesc = buildJRubyClassDesc(getDefSuperClass(cNode, codeContext), false);
		classDesc.isDefClass = true;
		classDesc.defNode = cNode;
//		(ClassNode, (Colon2ImplicitNode:YesRun), (BlockNode, (NewlineNode, (FCallNode:include, (ArrayNode, (CallNode:Runnable, (CallNode:lang, (VCallNode:java), 
		
		//提取include中的接口
		final Node body = cNode.getBody();
		if(body != null){
			final List<Node> childs = body.childNodes();
			final int size = childs.size();
			for (int i = 0; i < size; i++) {
				Node sub = childs.get(i);
				
				if(sub.getNodeType() == NodeType.NEWLINENODE){
					sub = ((NewlineNode)sub).getNextNode();
				}
				if(sub == null){
					break;
				}
				
				if(sub.getNodeType() == NodeType.FCALLNODE){//可以提取Runnable/java.lang.Runnable/Java::hc.core.util.IEncryptor
					final Class one = getClassFromFCallNode((FCallNode)sub, codeContext);
					if(one != null){
						classDesc.appendInterface(one);
					}
				}else{
					break;
				}
			}
		}
		return classDesc;
	}
	
	private static Class getClassByNode(final Node node, final CodeContext codeContext){
		if(node == null){
			return null;
		}
		
		final NodeType nodeType = node.getNodeType();
		if(nodeType == NodeType.CONSTNODE){
			return findClassFromConstByImportOrDefClass(((ConstNode)node).getName(), codeContext).baseClass;
		}else if(nodeType == NodeType.CALLNODE){
			final String className = buildFullClassNameFromReceiverNode((CallNode)node);
			return findClassByName(className, true);
		}
		
		return null;
	}

	private static Class getDefSuperClass(final ClassNode cNode, final CodeContext codeContext) {
		final Node superNode = cNode.getSuper();
		if(superNode != null && superNode instanceof ConstNode){
			return findClassFromConstByImportOrDefClass(((ConstNode)superNode).getName(), codeContext).baseClass;
		}
		
		final CallNode superClass = (CallNode)superNode;
		
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
    
    /**
     * 
     * @param argsNode 注意：org.jrubyparser.ast.ArrayNode extends org.jrubyparser.ast.ListNode
     * @param context
     * @return
     */
    private final static Class[] findClasssFromArgs(final ListNode argsNode, final CodeContext context){
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
	
	private Node backRoot;
	private Node root = emptyStringNode;
	
	public final void resetASTRoot(){
		backRoot = null;
		root = emptyStringNode;
	}
	
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
			final StringBuilder sb = StringBuilderCacher.getFree();
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
			final String returnStr = sb.toString();
			StringBuilderCacher.cycle(sb);
			return returnStr;
		}else{
			if(lineNo == countLineNo){
				final StringBuilder sb = StringBuilderCacher.getFree();
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
				final String returnStr = sb.toString();
				StringBuilderCacher.cycle(sb);
				return returnStr;
			}else{
				return script;
			}
		}
	}
	
	private final int getErrorLineNO(final String evalStr){
		//(SyntaxError) <script>:3: syntax error, unexpected end-of-file
		final int startMaoHao = evalStr.indexOf(":");
		if(startMaoHao >= 0){
			final int endMaoHao = evalStr.indexOf(":", startMaoHao + 1);
			if(endMaoHao > 0){
				return Integer.parseInt(evalStr.substring(startMaoHao + 1, endMaoHao));
			}
		}
		return -1;
	}
	
	final CallContext callCtxNeverCycle = CallContext.getFree();

	private final Node buildNodeRemoveCurrEditErrLine(final String script, final ScriptEditPanel sep){
		final int idx = sep.jtaScript.getCaretPosition();
		try{
			final AbstractDocument jtaDocment = sep.jtaDocment;
			final int line = ScriptEditPanel.getLineOfOffset(jtaDocment, idx);
			final StringBuilder sb = StringBuilderCacher.getFree();
			final int startOff = ScriptEditPanel.getLineStartOffset(jtaDocment, line);
			final int endOff = ScriptEditPanel.getLineEndOffset(jtaDocment, line);
			sb.append(jtaDocment.getText(0, startOff));
			final int docLen = jtaDocment.getLength();
			sb.append(jtaDocment.getText(endOff, docLen - endOff));
			final String newScript = sb.toString();
			StringBuilderCacher.cycle(sb);
			
			return parseScripts(newScript);
		}catch (final Throwable e) {
		}
		return null;
	}
	
	private final Node buildMiniNode(final String script, final ScriptEditPanel sep){
		callCtxNeverCycle.reset();
		RubyExector.parse(callCtxNeverCycle, script, null, SimuMobile.getRunTestEngine(), false);
		
		if(callCtxNeverCycle.isError == false){
			return parseScripts(script);
		}else{
			int lineNo = getErrorLineNO(callCtxNeverCycle.getMessage());
			do{
				try{
					if(lineNo > 0){
						return parseScripts(whiteLine(script, lineNo));
					}
				}catch (final Throwable e) {
					if(lineNo > 1){
						lineNo--;
					}else{
						return emptyStringNode;
					}
				}
			}while(true);//必须要循环以发现多个bug
		}
	}
	
	private Object errorHighlighter;
	
	public final boolean updateScriptASTNode(final ScriptEditPanel sep, final String scripts, final boolean isModifySource){
		if(isModifySource == false){
			return true;
		}
		
		try{
			final Node oldRoot = root;
			root = parseScripts(scripts);
			if(errorHighlighter != null){
				final Highlighter highlighter = sep.jtaScript.getHighlighter();
				highlighter.removeHighlight(errorHighlighter);
				errorHighlighter = null;
			}
//			if(L.isInWorkshop){
//				printNode(root);
//			}
			backRoot = oldRoot;
			return true;
		}catch (final Throwable e) {
			if(e instanceof SyntaxException){
				final StyledDocument document = (StyledDocument)sep.jtaDocment;
				
				if(errorHighlighter != null){
					final Highlighter highlighter = sep.jtaScript.getHighlighter();
					highlighter.removeHighlight(errorHighlighter);
					errorHighlighter = null;
				}
				
				final SyntaxException se = (SyntaxException)e;
				final SourcePosition sp = se.getPosition();
				try{
					int errStartOff = sp.getStartOffset();
					int errEndOff = sp.getEndOffset();
					
					if(errStartOff == errEndOff){
						final int lineNo = sp.getStartLine();
						errStartOff = ScriptEditPanel.getLineStartOffset(document, lineNo);
						errEndOff = ScriptEditPanel.getLineEndOffset(document, lineNo);
					}
					
					final Highlighter highlighter = sep.jtaScript.getHighlighter();
					try {
						errorHighlighter = highlighter.addHighlight(errStartOff, errEndOff, ScriptEditPanel.SYNTAX_ERROR_PAINTER);
					} catch (final BadLocationException ex) {
					}
				}catch (final Throwable ex) {
				}
			}
			
			final Node node = buildNodeRemoveCurrEditErrLine(scripts, sep);
			if(node != null){
				backRoot = root;
				root = node;
				return true;
			}
			
			if(L.isInWorkshop){
				LogManager.log("[============>workbench]:");
				ExceptionReporter.printStackTrace(e);
			}
			if(backRoot == null){
//				if(sep.isInited == false){
//					root = parseScripts("");
//				}else{
					root = buildMiniNode(scripts, sep);//UI迟滞，采用backRoot方式
//				}
			}else{
				root = backRoot;
			}
		}
		return false;
	}
	
	private final static String PRE_REQUIRE = "require";
	
	public final ArrayList<CodeItem> getReqAndImp(final ArrayList<CodeItem> out){
		clearArray(out);
		{
			CodeItem item = CodeItem.getFree();
			item.code = PRE_IMPORT;
			item.codeForDoc = item.code;
			item.codeDisplay = PRE_IMPORT;
			item.codeLowMatch = PRE_IMPORT.toLowerCase();
			item.type = CodeItem.TYPE_IMPORT;
			
			out.add(item);
			
			item = CodeItem.getFree();
			item.code = PRE_IMPORT_JAVA;
			item.codeForDoc = item.code;
			item.codeDisplay = PRE_IMPORT_JAVA;
			item.codeLowMatch = PRE_IMPORT_JAVA.toLowerCase();
			item.type = CodeItem.TYPE_IMPORT;
			
			out.add(item);
		}
		
		{
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
				item.codeForDoc = item.code;
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
	public final boolean input(final ScriptEditPanel sep, final HCTextPane textPane, final Document doc, 
			final int fontHeight, final boolean isForcePopup, final int scriptIdx, final boolean isForceResTip) throws Exception{
		//1：行首时，requ
		//2：行首时，impo
		//3：def initialize|可重载的方法
		//4：空格后 import|Java::|getProjectContext(class xx < abc)
		//5：::后 Java::|Font::
		//6：.后 JButton.new|ImageIO.read
		//3：resource("时，lib资源
		this.isForceResTip = isForceResTip;
		
		final int rowIdx = ScriptEditPanel.getLineOfOffset(doc, scriptIdx);
        final int editLineStartIdx = ScriptEditPanel.getLineStartOffset(doc, rowIdx);
        final int lineIdx = scriptIdx - editLineStartIdx;
        final char[] lineChars = doc.getText(editLineStartIdx, lineIdx).toCharArray();
        
        if(inputCSSClassOrPropInDesigner(0, lineChars, lineIdx, scriptIdx, sep, textPane, doc, fontHeight)){
        	return true;
        }
        
		initPreCode(lineChars, lineIdx, rowIdx);
		if(isForcePopup == false && outAndCycle.size() == 0){
			return false;
		}
		if(isForceResTip == false && preCodeType == PRE_TYPE_RESOURCES){
			if(matchRes(outAndCycle, preCode) == false){
				return false;
			}
		}
		
		final Class codeClass = (preClass==null?null:preClass.baseClass);
		
		final Point win_loc = textPane.getLocationOnScreen();
		final Rectangle caretRect=textPane.modelToView(scriptIdx - preCode.length());
		final Point caretPointer = new Point(caretRect.x, caretRect.y);
		final int input_x = win_loc.x + ((caretPointer==null)?0:caretPointer.x);
		final int input_y = win_loc.y + ((caretPointer==null)?0:caretPointer.y);
		window.toFront(preCodeType, codeClass, sep, textPane, input_x, input_y, outAndCycle, preCode, scriptIdx, fontHeight);//JRuby代码
		return true;
	}

	private static boolean matchRes(final ArrayList<CodeItem> out, final String pre) {
		if(out.size() == 0 || pre.length() == 0){
			return false;
		}
		
		final int size = out.size();
		for (int i = 0; i < size; i++) {
			if(out.get(i).code.startsWith(pre)){
				return true;
			}
		}
		
		return false;
	}
	
	private final static int checkInSetCSSStylePropParameter(final char[] lineChars, final int methodIdx, final int endIdx){
		int i = methodIdx;
		boolean isFind = false;
		int isInKuoHao = 0;
		for (; i < lineChars.length && i < endIdx; i++) {
			final char c = lineChars[i];
			if(isInKuoHao > 0){
				if(c == ')'){
					isInKuoHao--;
				}else if(c == '('){
					isInKuoHao++;
				}
				continue;
			}
			
			if(c == ','){
				isFind = true;
				break;
			}else if(c == '('){
				isInKuoHao++;
			}
		}
		
		if(isFind == false){
			return -1;
		}

		//----------------------以上完成第一个参数段
		
		i++;
		isFind = false;
		isInKuoHao = 0;
		for (; i < lineChars.length && i < endIdx; i++) {
			final char c = lineChars[i];
			if(isInKuoHao > 0){
				if(c == ')'){
					isInKuoHao--;
				}else if(c == '('){
					isInKuoHao++;
				}
				continue;
			}
			
			if(c == ','){
				isFind = true;
				break;
			}else if(c == '('){
				isInKuoHao++;
			}
		}
		
		if(isFind == false){
			return -1;
		}
		
		//----------------------以上完成第二个参数段
		
		int yinHaoBeforeIdx = -1, propFenHaoNextIdx = -1;
		i++;
		
		for (; i < lineChars.length && i < endIdx; i++) {
			final char c = lineChars[i];
			if(c == ','){
				return -1;
			}else if(c == ';'){
				if(yinHaoBeforeIdx >= 0){
					propFenHaoNextIdx = i;
				}
			}else if(c == '\"'){
				if(yinHaoBeforeIdx == -1){
					yinHaoBeforeIdx = i;
				}else{//"abc:efg;" + "hij:kmn;"
					yinHaoBeforeIdx = -1;
					propFenHaoNextIdx = -1;
				}
			}
		}
		
		if(yinHaoBeforeIdx != -1){
			if(propFenHaoNextIdx == -1){
				return yinHaoBeforeIdx + 1;
			}else{
				return propFenHaoNextIdx + 1;
			}
		}
		return -1;
	}
	
	/**
	 * 
	 * @param lineChars
	 * @param methodIdx 即(所在index
	 * @return >0 表示处于classParameter段
	 */
	private final static int checkInSetCSSClassParameter(final char[] lineChars, final int methodIdx, final int endIdx){
		int i = methodIdx;
		boolean isFind = false;
		int isInKuoHao = 0;
		for (; i < lineChars.length && i < endIdx; i++) {
			final char c = lineChars[i];
			if(isInKuoHao > 0){
				if(c == ')'){
					isInKuoHao--;
				}else if(c == '('){
					isInKuoHao++;
				}
				continue;
			}
			
			if(c == ','){
				isFind = true;
				break;
			}else if(c == '\"'){
				return -1;
			}else if(c == '('){
				isInKuoHao++;
			}
		}
		
		if(isFind == false){
			return -1;
		}
		
		int yinHaoBeforeIdx = -1, classSpaceNextIdx = -1;
		i++;
		
		for (; i < lineChars.length && i < endIdx; i++) {
			final char c = lineChars[i];
			if(c == ','){
				return -1;
			}else if(c == ' '){
				if(yinHaoBeforeIdx >= 0){
					classSpaceNextIdx = i;
				}
			}else if(c == '\"'){
				if(yinHaoBeforeIdx == -1){
					yinHaoBeforeIdx = i;
				}else{
					yinHaoBeforeIdx = -1;
					classSpaceNextIdx = -1;
				}
			}
		}
		
		if(yinHaoBeforeIdx != -1){
			if(classSpaceNextIdx == -1){
				return yinHaoBeforeIdx + 1;
			}else{
				return classSpaceNextIdx + 1;
			}
		}
		return -1;
	}
	
	public static final CSSIdx getCSSIdx(final char[] lineChars, final int lineIdx, final int endIdx){
		int cssMethodIdx;
		int cssPropIdx = -1;
		int classIdx = -1;
		if((cssMethodIdx = searchSubChars(lineChars, SET_CSS_FOR_TOGGLE, 0)) >= 0 && cssMethodIdx < lineIdx){
			final int startIdx = cssMethodIdx + SET_CSS_FOR_TOGGLE.length + 1;
			cssPropIdx = checkInSetCSSStylePropParameter(lineChars, startIdx, endIdx);
			if(cssPropIdx == -1){
				classIdx = checkInSetCSSClassParameter(lineChars, startIdx, endIdx);
			}
		}else if((cssMethodIdx = searchSubChars(lineChars, SET_CSS_FOR_DIV, 0)) >= 0 && cssMethodIdx < lineIdx){
			final int startIdx = cssMethodIdx + SET_CSS_FOR_DIV.length + 1;
			cssPropIdx = checkInSetCSSStylePropParameter(lineChars, startIdx, endIdx);
			if(cssPropIdx == -1){
				classIdx = checkInSetCSSClassParameter(lineChars, startIdx, endIdx);
			}
		}else if((cssMethodIdx = searchSubChars(lineChars, SET_CSS_BY_CLASS, 0)) >= 0 && cssMethodIdx < lineIdx){
			final int startIdx = cssMethodIdx + SET_CSS_BY_CLASS.length + 1;
			classIdx = checkInSetCSSClassParameter(lineChars, startIdx, endIdx);
		}else if((cssMethodIdx = searchSubChars(lineChars, SET_CSS, 0)) >= 0 && cssMethodIdx < lineIdx){
			final int startIdx = cssMethodIdx + SET_CSS.length + 1;
			cssPropIdx = checkInSetCSSStylePropParameter(lineChars, startIdx, endIdx);
			if(cssPropIdx == -1){
				classIdx = checkInSetCSSClassParameter(lineChars, startIdx, endIdx);
			}
		}else{
			return null;
		}
		
		if(cssPropIdx == -1 && classIdx == -1){
			return null;
		}
		
		return new CSSIdx(cssPropIdx, classIdx);
	}
	
	public static final boolean searchGoGoExternalURL(final char[] lineChars){
		if(searchSubChars(lineChars, MLET_DIALOG_GOEXTERNALURL, 0) >= 0){
			return true;
		}else if(searchSubChars(lineChars, MLET_DIALOG_GO, 0) >= 0){
			return true;
		}else{
			return false;
		}
	}

	private final boolean inputCSSClassOrPropInDesigner(final int preCodeType, final char[] lineChars, final int lineIdx, final int scriptIdx, final ScriptEditPanel sep, 
			final HCTextPane textPane, final Document doc, final int fontHeight){
		final CSSIdx cssIdx = getCSSIdx(lineChars, lineIdx, lineChars.length);
		
		if(cssIdx == null){
			return false;
		}
		
		if(cssIdx.cssPropIdx != -1){
			preCode = String.valueOf(lineChars, cssIdx.cssPropIdx, lineIdx - cssIdx.cssPropIdx).trim();//因为;号可能有空格，所以要trim
			inputPropertyForCSSInCSSEditor(preCodeType, preCode, textPane, fontHeight, scriptIdx);
			return true;
		}
		
		try{
			final Rectangle caretRect = sep.jtaScript.modelToView(scriptIdx - lineIdx + cssIdx.classIdx);
			final Point caretPointer = new Point(caretRect.x, caretRect.y);
			preCode = String.valueOf(lineChars, cssIdx.classIdx, lineIdx - cssIdx.classIdx);
			clearArray(outAndCycle);
			
			CodeItem.append(outAndCycle, sep.designer.cssClassesOfProjectLevel);
			
			final Point win_loc = textPane.getLocationOnScreen();
			final int input_x = win_loc.x + caretPointer.x;
			final int input_y = win_loc.y + caretPointer.y;
			window.toFront(preCodeType, null, sep, textPane, input_x, input_y, outAndCycle, preCode, scriptIdx, fontHeight);//CSS class
			return true;
		}catch (final Throwable e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public final boolean mouseMovOn(final ScriptEditPanel sep, final HCTextPane textPane, final Document doc, 
			final int fontHeight, final boolean isForcePopup, final int scriptIdx) throws Exception{
		if(L.isInWorkshop){
			LogManager.log("[CodeTip] start mouseMovOn from HCTimer.");
		}
		
		final int rowIdx = ScriptEditPanel.getLineOfOffset(doc, scriptIdx);
        final int editLineStartIdx = ScriptEditPanel.getLineStartOffset(doc, rowIdx);
        final int lineIdx = scriptIdx - editLineStartIdx;
        final int lineEndOffset = ScriptEditPanel.getLineEndOffset(doc, rowIdx);
        if(scriptIdx == lineEndOffset || scriptIdx + 1 == lineEndOffset){//鼠标处于行尾时，不处理
        	return false;
        }
        
		char[] lineChars = doc.getText(editLineStartIdx, lineEndOffset - editLineStartIdx).toCharArray();
		{
			int i = lineIdx;
			for (; i < lineChars.length; i++) {//补全完整的方法名
				final char nextChar = lineChars[i];
				if(nextChar >= 'a' && nextChar <= 'z'
						|| nextChar >= 'A' && nextChar <= 'Z'
						|| nextChar >= '0' && nextChar <= '9'
						|| nextChar == '_'){
				}else{
					break;
				}
			}
			if(i > lineIdx){
				final char[] newLineChars = new char[i];
				System.arraycopy(lineChars, 0, newLineChars, 0, i);//去掉方法后段部分，仅保留此之前
				lineChars = newLineChars;
			}
		}
		
		initPreCode(lineChars, lineIdx, rowIdx);
		if(isForcePopup == false && outAndCycle.size() == 0){
			return false;
		}
		final Class codeClass = (preClass==null?null:preClass.baseClass);
		
		int endIdx = lineIdx;
		for (; endIdx < lineChars.length; endIdx++) {//补全完整的方法名
			final char nextChar = lineChars[endIdx];
			if(nextChar >= 'a' && nextChar <= 'z'
					|| nextChar >= 'A' && nextChar <= 'Z'
					|| nextChar >= '0' && nextChar <= '9'
					|| nextChar == '_'){
			}else{
				break;
			}
		}
		int startIdx = lineIdx - 1;
		for (; startIdx < lineChars.length; startIdx--) {//补全完整的方法名
			final char nextChar = lineChars[startIdx];
			if(nextChar >= 'a' && nextChar <= 'z'
					|| nextChar >= 'A' && nextChar <= 'Z'
					|| nextChar >= '0' && nextChar <= '9'
					|| nextChar == '_'){
			}else{
				break;
			}
		}
		startIdx++;
		preCode = new String(lineChars, startIdx, endIdx - startIdx);
		
		final Point win_loc = textPane.getLocationOnScreen();
		final Rectangle caretRect = textPane.modelToView(scriptIdx - (lineIdx - startIdx));//与方法段齐
		final int input_x = win_loc.x + caretRect.x;
		final int input_y = win_loc.y + caretRect.y;
		

		if(L.isInWorkshop){
			LogManager.log("[CodeTip] AutoCodeTip : " + preCode + ", codeItem : " + outAndCycle.size());
		}
		if(preCode.length() == 0){
			return false;
		}
		
		CodeWindow.fillForAutoTip(outAndCycle, autoTipOut, preCode);
		
		final int matchSize = autoTipOut.size();
		if(matchSize == 0){
			return false;
		}else if(matchSize == 1){
			window.setMouseOverAutoTipLoc(input_x, input_y, fontHeight);
			window.startAutoPopTip(autoTipOut.get(0), textPane);
		}else{
			window.toFront(preCodeType, codeClass, sep, textPane, input_x, input_y, autoTipOut, preCode, scriptIdx, fontHeight);
		}
		return true;
	}

	public final void inputForCSSInCSSEditor(final int preCodeType, final HCTextPane textPane, final Document cssDocument, final Point caretPosition, 
			final int fontHeight, final int scriptIdx){
		String preCode = null;
		
		try{
			final int line = ScriptEditPanel.getLineOfOffset(cssDocument, scriptIdx);
	        final int editLineStartIdx = ScriptEditPanel.getLineStartOffset(cssDocument, line);
	        final int lineIdx = scriptIdx - editLineStartIdx;
	        final int lineEndOffset = ScriptEditPanel.getLineEndOffset(cssDocument, line);
	        
			final char[] lineChars = cssDocument.getText(editLineStartIdx, lineEndOffset - editLineStartIdx).toCharArray();
			
			for (int i = lineIdx; i >= 0; i--) {
				final char oneChar = lineChars[i];
				if(oneChar == ';' || oneChar == '{'){
					preCode = String.valueOf(lineChars, i, lineIdx);
					break;
				}
				if(oneChar == ':'){
					break;
				}
			}
			
			if(preCode == null){
				if(lineChars[0] == ' ' || lineChars[0] == '\t'){
					preCode = String.valueOf(lineChars, 0, lineIdx);
				}
			}
		}catch (final Throwable e) {
			e.printStackTrace();
			return;
		}

		if(preCode != null){
			inputPropertyForCSSInCSSEditor(preCodeType, preCode.trim(), textPane, fontHeight, scriptIdx);
		}else{
			inputVariableForCSSInCSSEditor(preCodeType, textPane, caretPosition, fontHeight, scriptIdx);
		}
	}
	
	private final void inputPropertyForCSSInCSSEditor(final int preCodeType, final String preCode, final HCTextPane textPane, final int fontHeight,
			final int scriptIdx){
		try{
			final Point win_loc = textPane.getLocationOnScreen();
			final Rectangle caretRect = textPane.modelToView(scriptIdx - preCode.length());//与方法段齐
			final int input_x = win_loc.x + caretRect.x;
			final int input_y = win_loc.y + caretRect.y;
			
			CSSHelper.getProperties();
			
			clearArray(outAndCycle);
			CodeItem.append(outAndCycle, DocHelper.cssCodeItems);
			
	//		final int input_x = win_loc.x + ((caretPosition==null)?0:caretPosition.x);
	//		final int input_y = win_loc.y + ((caretPosition==null)?0:caretPosition.y);
			window.toFront(preCodeType, Object.class, null, textPane, input_x, input_y, outAndCycle, preCode, scriptIdx, fontHeight);
		}catch (final Throwable e) {
			e.printStackTrace();
		}
	}
	
	private final void inputVariableForCSSInCSSEditor(final int preCodeType, final HCTextPane textPane, final Point caretPosition, 
			final int fontHeight, final int scriptIdx){
		final Point win_loc = textPane.getLocationOnScreen();
		clearArray(outAndCycle);
		
		final String[] variables = StyleManager.variables;
		
		for (int i = 0; i < variables.length; i++) {
			final CodeItem item = CodeItem.getFree();
			final String var = variables[i];
			
			item.type = CodeItem.TYPE_RESOURCES;
			item.code = var;
			item.codeForDoc = item.code;
			item.codeDisplay = var;
			item.codeLowMatch = var.toLowerCase();
			
			outAndCycle.add(item);
		}
		
		final int input_x = win_loc.x + ((caretPosition==null)?0:caretPosition.x);
		final int input_y = win_loc.y + ((caretPosition==null)?0:caretPosition.y);
		window.toFront(preCodeType, Object.class, null, textPane, input_x, input_y, outAndCycle, "", scriptIdx, fontHeight);
	}
	
//	/**
//	 * @param lineHeader
//	 * @param offLineIdx
//	 * @return return 0 if not instance or global var.
//	 */
//	private final int isInsOrGlobalVar(final char[] lineHeader, final int offLineIdx){
//		return 0;
//	}
	
	private final void getInsOrGloablVar(final Node defClass, final ArrayList<CodeItem> out){
		if(defClass == null){
			return;
		}
		
		clearArray(out);
		
		if(defClass instanceof ClassNode){
			final List<MethodDefNode> lists = ((ClassNode)defClass).getMethodDefs();
			final int methodSize = lists.size();
			
			//优先从initialize中找
			for (int j = 0; j < methodSize; j++) {
				final MethodDefNode aMethod = lists.get(j);
				if(aMethod.getName().equals(JRUBY_CLASS_INITIALIZE_DEF)){
					appendVarDefInInitializeMethod(aMethod.getBody(), pre_var_tag_ins_or_global, out);
					break;
				}
			}
		}else if(defClass instanceof IterNode){
			final IterNode iterNode = (IterNode)defClass;
			final Node body = iterNode.getBody();
			if(body != null){
				final BlockNode blockNode = (BlockNode)body;
				final int size = blockNode.size();
				for (int i = 0; i < size; i++) {
					final NewlineNode newline = (NewlineNode)blockNode.get(i);
					final DefnNode aMethod = (DefnNode)newline.childNodes().get(0);
					if(aMethod.getName().equals(JRUBY_CLASS_INITIALIZE_DEF)){
						appendVarDefInInitializeMethod(aMethod.getBody(), pre_var_tag_ins_or_global, out);
						break;
					}
				}
			}
		}
		
		Collections.sort(out);
	}
	
	public void initPreCode(final char[] lineHeader, final int offLineIdx, final int rowIdx) {
		clearArray(outAndCycle);
		preCodeSplitIsDot = false;
		
		codeContext = new CodeContext(this, root, rowIdx);

		preCodeType = getPreCodeType(lineHeader, offLineIdx, rowIdx);
		if(preCodeType == PRE_TYPE_NEWLINE){
			getVariables(rowIdx, root, true, "", outAndCycle, TYPE_VAR_LOCAL | TYPE_VAR_INSTANCE | TYPE_VAR_GLOBAL);
		}else if(preCodeType == PRE_TYPE_RESOURCES){
			getResources(outAndCycle, getRequireLibs(root, outRequireLibs), true);
		}else if(preCodeType == PRE_TYPE_AFTER_IMPORT_ONLY){
			getSubPackageAndClasses(outAndCycle, getRequireLibs(root, outRequireLibs), true, true);
		}else if(preCodeType == PRE_TYPE_AFTER_IMPORTJAVA){
			getSubPackageAndClasses(outAndCycle, getRequireLibs(root, outRequireLibs), false, true);
		}else if(preCodeType == PRE_TYPE_IN_DEF_CLASS_FOR_METHOD_FIELD_ONLY){
			getMethodAndFieldForInstance(backgroundDefClassNode, false, outAndCycle, false, true);
		}else if(preCodeType == PRE_TYPE_OVERRIDE_METHOD){
//			final int shiftBackLen = DEF_MEMBER.length() + 1 + preCode.length();
			final JRubyClassDesc desc = codeContext.getDefJRubyClassDesc();//isInDefClass(root, codeContext, rowIdx);
			if(desc != null){
				buildItem(outAndCycle, JRUBY_CLASS_INITIALIZE_DEF, CodeItem.TYPE_METHOD);
				appendInterfaces(desc.include, outAndCycle);//要先执行，这样def因相同不会再次添加
				getMethodAndFieldForInstance(desc, false, outAndCycle, true, false);
				Collections.sort(outAndCycle);
			}
		}else if(preCodeType == PRE_TYPE_BEFORE_INSTANCE){
			getVariables(rowIdx, root, true, "", outAndCycle, pre_var_tag_ins_or_global);//情形：在行首输入im，可能后续为import或ImageIO
		}else if(preCodeType == PRE_TYPE_AFTER_INSTANCE_OR_CLASS){
			if(preClass != null){
				if(preClass.isInstance){
					clearArray(outAndCycle);
					appendInterfaces(preClass.include, outAndCycle);
					getMethodAndFieldForInstance(preClass, preClass.isInExtend?false:true, outAndCycle, true, false);
				}else{
					getMethodAndFieldForClass(preClass, outAndCycle, false, true);
				}
			}else{//直接从背景中取类，会出现preClass==null
				if(pre_var_tag_ins_or_global == TYPE_VAR_INSTANCE || pre_var_tag_ins_or_global == TYPE_VAR_GLOBAL){
					final JRubyClassDesc desc = codeContext.getDefJRubyClassDesc();
					if(desc != null && desc.defNode != null){
						getInsOrGloablVar(desc.defNode, outAndCycle);
					}else if(desc != null && desc.defIterNode != null){
						getInsOrGloablVar(desc.defIterNode, outAndCycle);
					}else{
						final ClassNode classNode = backgroundDefClassNode.defNode;
						getInsOrGloablVar(classNode, outAndCycle);
					}
				}else{
					if(preCodeSplitIsDot == false){
						getMethodAndFieldForInstance(backgroundDefClassNode, false, outAndCycle, false, true);
//						getVariables(root, true, "", out, scriptIdx, pre_var_tag_ins_or_global);//改为，增加preCode过滤
//						final int startTipIdx = scriptIdx - preCode.length();
						getVariables(rowIdx, root, true, preCode, outAndCycle, pre_var_tag_ins_or_global);
					}
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
	private final static int PRE_TYPE_AFTER_IMPORT_ONLY = 2;
	private final static int PRE_TYPE_AFTER_IMPORTJAVA = 3;//import Java:: 或 abc = Java:: 之后
	private final static int PRE_TYPE_IN_DEF_CLASS_FOR_METHOD_FIELD_ONLY = 4;//class abc \n getProXXX end\n 1.在类定义内，进行方法或属性访问 2.访问变量
	private final static int PRE_TYPE_BEFORE_INSTANCE = 5;//JLab myLa $my @my
	private final static int PRE_TYPE_AFTER_INSTANCE_OR_CLASS = 6;//JLable. myLabel. $myLabel. @my.
	private final static int PRE_TYPE_RESOURCES = 7;//"/test/res/hc_16.png"
	public final static int PRE_TYPE_OVERRIDE_METHOD = 8;
	
	public String preCode;
	public boolean isForceResTip = false;
	private int pre_var_tag_ins_or_global;
	public boolean preCodeSplitIsDot;
	public int preCodeType;
	private JRubyClassDesc preClass;
	private JRubyClassDesc backgroundDefClassNode;
	
	private final ArrayList<String> outRequireLibs = new ArrayList<String>();
	
	private final String PRE_IMPORT = "import ";
	private final String PRE_IMPORT_JAVA = "import Java::";
	private final char[] java_dot_chars = "java.".toCharArray();
	private final char[] javax_dot_chars = "javax.".toCharArray();
	private final char[] import_chars = PRE_IMPORT.toCharArray();
	private final int import_chars_len = import_chars.length;
	private final char[] import_java_chars = PRE_IMPORT_JAVA.toCharArray();
	private final int import_java_chars_len = import_java_chars.length;
	
	private final static boolean matchChars(final char[] search, final char[] chars, final int offset){
		if(search.length < (chars.length - offset)){
			return false;
		}
		if(search.length - offset < chars.length){
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
	
	private final void getVariablesUpperForward(final int rowIdx, final Node node, final String preCode, final ArrayList<CodeItem> out, 
			final int type_var){
		if(node == null){
			return;
		}
		final String lowerPreCode = preCode.toLowerCase();
		
		if(node instanceof DefnNode){// || isDefNodeUnderNewlineNode(node)){//将方法定义的参数取出来。如：opeator(id, para)，则增加id, para
			final DefnNode defNode = (DefnNode)node;
//			if(isDefNode){
//				defNode = (DefnNode)node;
//			}else{
//				defNode = (DefnNode)((NewlineNode)node).childNodes().get(0);//会导致被添加两次，所以调用处，不从parent开始
//			}
			final ArgsNode argsNode = defNode.getArgs();
			final int parameterNum = argsNode.getMaxArgumentsCount();//获得参数个数
			if (parameterNum > 0){//除无参数，如opera()的情形
				final List<Node> list = argsNode.childNodes();
				final ListNode parametersNode = (ListNode)list.get(0);//注意：org.jrubyparser.ast.ArrayNode extends org.jrubyparser.ast.ListNode
				for(int i = 0; i<parameterNum; i++){
					try{
						final ArgumentNode parameterNode = (ArgumentNode)parametersNode.get(i);
						final String name = parameterNode.getName();
						
						buildVarItem(out, name);
					}catch (final Throwable e) {
						e.printStackTrace();
					}
				}
			}
		}else if(node instanceof ConstDeclNode){
			final ConstDeclNode constDecl = (ConstDeclNode)node;
			final String name = constDecl.getName();
			
			buildVarItem(out, name);
		}
		
		final List<Node> childNodes = node.childNodes();
		final int size = childNodes.size();
		for (int i = 0; i < size; i++) {
			Node one = childNodes.get(i);
			final SourcePosition position = one.getPosition();
			if(position.getStartLine() > rowIdx){
				break;
			}else if(position.getEndLine() < rowIdx){
				if(one instanceof NewlineNode){
					one = ((NewlineNode)one).childNodes().get(0);
				}
				if(((type_var & CodeHelper.TYPE_VAR_UNKNOW) != 0 || (type_var & CodeHelper.TYPE_VAR_LOCAL) != 0) 
						&& one instanceof FCallNode){//(FCallNode:import, (ArrayNode, (CallNode:JLabel, (CallNode:swing,
					final FCallNode callNode = (FCallNode)one;
					if(callNode.getName().equals("import")){
						final ListNode importArgs = (ListNode)callNode.getArgs();//注意：org.jrubyparser.ast.ArrayNode extends org.jrubyparser.ast.ListNode
						final CallNode firstPackageNode = (CallNode)importArgs.childNodes().get(0);
						final String name = firstPackageNode.getName();
						final String nameLower = name.toLowerCase();
						if(nameLower.startsWith(lowerPreCode) && (CodeItem.contains(out, name) == false)){//由于是从下向上，可能被添加了一次
							buildClassItem(out, name, nameLower);
							continue;
						}
					}
				}
				
				if((type_var & CodeHelper.TYPE_VAR_UNKNOW) != 0
						&& one instanceof DAsgnNode){//run{while{...}}
					final DAsgnNode dasgnNode = (DAsgnNode)one;
					final String name = dasgnNode.getName();
					final String nameLower = name.toLowerCase();
					if(nameLower.startsWith(lowerPreCode) && (CodeItem.contains(out, name) == false)){//由于是从下向上，可能被添加了一次
						final CodeItem item = CodeItem.getFree();
						item.code = name;
						item.codeForDoc = item.code;
						item.codeDisplay = name;
						item.codeLowMatch = nameLower;
						item.type = CodeItem.TYPE_VARIABLE;
						out.add(item);
						continue;
					}
				}

				if((type_var & CodeHelper.TYPE_VAR_INSTANCE) != 0 
						&& one instanceof InstAsgnNode){//CodeItem.TYPE_CLASS
					final String name = ((InstAsgnNode)one).getName();
					final String nameLower = name.toLowerCase();
					if(nameLower.startsWith(lowerPreCode) && (CodeItem.contains(out, name) == false)){
						final CodeItem item = CodeItem.getFree();
						item.code = name;
						item.codeForDoc = item.code;
						item.codeDisplay = name;
						item.codeLowMatch = nameLower;
						item.type = CodeItem.TYPE_VARIABLE;
						out.add(item);
						continue;
					}
				}
				
				if(((type_var & CodeHelper.TYPE_VAR_UNKNOW) != 0 || (type_var & CodeHelper.TYPE_VAR_LOCAL) != 0)
						&& one instanceof LocalAsgnNode){
					final String name = ((LocalAsgnNode)one).getName();
					final String nameLower = name.toLowerCase();
					if(nameLower.startsWith(lowerPreCode) && (CodeItem.contains(out, name) == false)){
						final CodeItem item = CodeItem.getFree();
						item.code = name;
						item.codeForDoc = item.code;
						item.codeDisplay = name;
						item.codeLowMatch = nameLower;
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
					final String nameLower = name.toLowerCase();
					if(nameLower.startsWith(lowerPreCode) && (CodeItem.contains(out, name) == false)){
						final CodeItem item = CodeItem.getFree();
						item.code = name;
						item.codeForDoc = item.code;
						item.codeDisplay = name;
						item.codeLowMatch = nameLower;
						item.type = CodeItem.TYPE_VARIABLE;
						out.add(item);
						continue;
					}
				}
				
				if(((type_var & CodeHelper.TYPE_VAR_UNKNOW) != 0 || (type_var & CodeHelper.TYPE_VAR_LOCAL) != 0) 
						&& one instanceof ClassNode){
					final ClassNode cNode = (ClassNode)one;
					final String name = getDefClassName(cNode);
					final String nameLower = name.toLowerCase();
					if(nameLower.startsWith(lowerPreCode) && (CodeItem.contains(out, name) == false)){
						buildClassItem(out, name, nameLower);
						continue;
					}
				}
				
			}
		}
		
		final Node parentNode = node.getParent();
		if(parentNode != null){
			getVariablesUpperForward(rowIdx, parentNode, preCode, out, type_var);
		}
	}

	private final void buildClassItem(final ArrayList<CodeItem> out, final String name, final String nameLower) {
		final CodeItem item = CodeItem.getFree();
		item.code = name;
		item.codeForDoc = item.code;
		item.codeDisplay = name;
		item.codeLowMatch = nameLower;
		item.type = CodeItem.TYPE_CLASS;
		out.add(item);
	}

	private final void buildVarItem(final ArrayList<CodeItem> out, final String name) {
		final CodeItem item = CodeItem.getFree();
		item.code = name;
		item.codeForDoc = item.code;
		item.codeDisplay = name;
		item.codeLowMatch = name.toLowerCase();
		item.type = CodeItem.TYPE_VARIABLE;
		out.add(item);
	}
	
	private final void buildItem(final ArrayList<CodeItem> out, final String name, final int type) {
		final CodeItem item = CodeItem.getFree();
		if(type == CodeItem.TYPE_METHOD){
			item.code = name + "()";
		}else{
			item.code = name;
		}
		item.codeForDoc = item.code;
		item.codeDisplay = name;
		item.codeLowMatch = name.toLowerCase();
		item.type = type;
		out.add(item);
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
	public final void getVariables(final int rowIdx, final Node node, final boolean isAppend, final String preCode, final ArrayList<CodeItem> out, final int type_var){
		if(isAppend == false){
			clearArray(out);
		}
		
		final Node paraNode = searchParaCallingNodeByIdx(node, rowIdx);
		
		getVariablesUpperForward(rowIdx, paraNode, preCode, out, type_var);
		
		buildClassItem(out, JRUBY_CLASS_FOR_NEW, JRUBY_CLASS_FOR_NEW.toLowerCase());
		
		Collections.sort(out);
		return;
	}
	
	private static final int searchSubChars(final char[] lines, final char[] searchChars, final int fromIdx){
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
	
	final static char[] partSpliter = {' ', '\t', ',', '[', '(', '+', '-', '*', '/'};
	
	public final int getPreCodeType(final char[] lineHeader, final int columnIdx, final int rowIdx){
		final int lineLen = lineHeader.length;
		if(lineLen == 0){
			preCode = "";
			getReqAndImp(outAndCycle);
			return PRE_TYPE_NEWLINE;
		}
		
		{
			//重载方法的代码提示
			//  def my
			int i = columnIdx - 1;
			for (; i >= 0; i--) {
				final char oneChar = lineHeader[i];
				if(oneChar >= 'a' && oneChar <= 'z'
						|| oneChar >= 'A' && oneChar <= 'Z'
						|| oneChar >= '0' && oneChar <= '9'
						|| oneChar == '_'){
				}else{
					break;
				}
			}
			
			if(i > 0 && lineHeader[i] == ' ' && matchChars(lineHeader, DEF_MEMBER_BS, i - DEF_MEMBER_BS.length)){
				final int preIdx = i + 1;
				i -= (DEF_MEMBER_BS.length + 1);
				for (; i >= 0; i--) {
					final char oneChar = lineHeader[i];
					if(oneChar == ' ' || oneChar == '\t'){
					}else{
						break;
					}
				}
				if(i < 0){
					preCode = new String(lineHeader, preIdx, columnIdx - preIdx);
					return PRE_TYPE_OVERRIDE_METHOD;
				}
			}
		}
		
		{
			// "#@valuable~~#@volatile~~#@precious"
			//"Before: obj = #{obj}"
			final char var_char = '#';
			int varCharIdx = -1;
			int yinhaoIdx = -1;
			for (int i = 0; i < columnIdx; i++) {
				final char oneChar = lineHeader[i];
				if(oneChar == '\"'){
					if(yinhaoIdx >=0){
						yinhaoIdx = -1;
						varCharIdx = -1;
						continue;
					}
					
					yinhaoIdx = i;
					continue;
				}else if(oneChar == '#'){
					varCharIdx = i;
					continue;
				}
			}
			
//			if(varCharIdx > 0 && (varCharIdx + 1) < lineHeader.length){
//				preCode = String.valueOf(lineHeader, varCharIdx + 2, columnIdx - (varCharIdx + 2));
//				if(lineHeader[varCharIdx + 1] == '@'){
//					return PRE_TYPE_BEFORE_INSTANCE
//					
//					zzz
//				}
//	        	return PRE_TYPE_RESOURCES;
//			}
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
		
		final int partStartIdx = 0;
		final char firstChar = lineHeader[partStartIdx];
		if(firstChar != '\t' && firstChar != ' '){
			if(matchChars(lineHeader, import_chars, partStartIdx)){
				if(columnIdx < import_chars_len){
					preCode = "";
				}else if(matchChars(lineHeader, import_java_chars, partStartIdx)){
					preCode = String.valueOf(lineHeader, import_java_chars_len, columnIdx - import_java_chars_len);
					return PRE_TYPE_AFTER_IMPORTJAVA;//import Java::ab
				}else{
					preCode = String.valueOf(lineHeader, import_chars_len, columnIdx - import_chars_len);
				}
				return PRE_TYPE_AFTER_IMPORT_ONLY;
			}else{
				getReqAndImp(outAndCycle);
			}
		}
		
		int partSplitIdx = -1;
		for (int i = columnIdx - 1; i >= 0; i--) {
			final char checkSplitChar = lineHeader[i];
			for (int j = 0; j < partSpliter.length; j++) {
				if(checkSplitChar == partSpliter[j]){
					partSplitIdx = i;
					break;
				}
			}
			if(partSplitIdx >= 0){
				break;
			}
		}
		
		return searchCodePart(lineHeader, columnIdx, rowIdx, (partSplitIdx != -1)?(partSplitIdx + 1):0);
	}

	private final int searchCodePart(final char[] lineHeader, final int columnIdx, final int rowIdx, final int partStartIdx) {
		final int idxJavaClass = searchSubChars(lineHeader, JRUBY_JAVA_CLASS_CHARS, partStartIdx);
		if(idxJavaClass >= partStartIdx && idxJavaClass + JRUBY_JAVA_CLASS_CHARS.length + 1 <= columnIdx){
			final JRubyClassDesc jcd = buildJRubyClassDesc(JRUBY_JAVA_CLASS_AGENT, false);
			preClass = jcd;
			
			final int cutPreIdx = idxJavaClass + JRUBY_JAVA_CLASS_CHARS.length + 1;
			preCode = String.valueOf(lineHeader, cutPreIdx, columnIdx - cutPreIdx);
			return PRE_TYPE_AFTER_INSTANCE_OR_CLASS;
		}
		//因为此情形可也可能出现在首字母非空格非tab的情形，所以要提前
		final int idxJavaMaoHao = searchSubChars(lineHeader, JAVA_MAO_MAO, partStartIdx);
		if(idxJavaMaoHao>=partStartIdx){
			//int i = 100 + Java::mypackage.sub::
			final int cutClassIdx = idxJavaMaoHao + JAVA_MAO_MAO.length;
			final int idxMaoHaoAgainForField = searchSubChars(lineHeader, MAO_HAO_ONLY, cutClassIdx);
			if(idxMaoHaoAgainForField < 0){
				//int i = 100 + Java::mypackage.su
				preCode = String.valueOf(lineHeader, cutClassIdx, columnIdx - cutClassIdx);
				return PRE_TYPE_AFTER_IMPORTJAVA;
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
				return PRE_TYPE_AFTER_INSTANCE_OR_CLASS;
			}
		}
		
		//java.lang.Strin or 1 + java.lang.Integer::v
		if(matchChars(lineHeader, java_dot_chars, partStartIdx) || matchChars(lineHeader, javax_dot_chars, partStartIdx)){
			final int maohaoIdx = searchSubChars(lineHeader, MAO_HAO_ONLY, partStartIdx);
			if(maohaoIdx < 0){
				preCode = String.valueOf(lineHeader, partStartIdx, columnIdx - partStartIdx);
				return PRE_TYPE_AFTER_IMPORT_ONLY;
			}else{
				//::段后的处理
				Class tryClass;
				final String className = String.valueOf(lineHeader, partStartIdx, maohaoIdx - partStartIdx);
				try{
					tryClass = getClassLoader().loadClass(className);
				}catch (final Exception e) {
					if(L.isInWorkshop){
						LogManager.errToLog("fail to find or initialize class : " + className);
					}
					tryClass = Object.class;
				}
				final JRubyClassDesc jcd = buildJRubyClassDesc(tryClass, false);
				preClass = jcd;
				
				preCodeSplitIsDot = false;
				final int maohaoCutIdx = maohaoIdx + MAO_HAO_ONLY.length;
				preCode = String.valueOf(lineHeader, maohaoCutIdx, columnIdx - maohaoCutIdx);
				return PRE_TYPE_AFTER_INSTANCE_OR_CLASS;
			}
		}

		final JRubyClassDesc desc = codeContext.getDefJRubyClassDesc();//isInDefClass(root, codeContext, rowIdx);
		preClass = findPreCodeAfterVar(lineHeader, columnIdx, rowIdx, partStartIdx);
		if(desc != null && preClass == null){
			backgroundDefClassNode = desc;
			return PRE_TYPE_AFTER_INSTANCE_OR_CLASS;
		}else{
			if(preClass == null){
				return PRE_TYPE_BEFORE_INSTANCE;
			}else{
				return PRE_TYPE_AFTER_INSTANCE_OR_CLASS;
			}
		}
	}

	private final static ClassLoader getClassLoader() {
		return SimuMobile.getRunTestEngine().getProjClassLoader();
	}
	
	static final char[] JRUBY_JAVA_CLASS_CHARS = JRUBY_JAVA_CLASS.toCharArray();
	static final char[] JAVA_MAO_MAO = "Java::".toCharArray();
	static final char[] MAO_HAO_ONLY = "::".toCharArray();
	static final char[] SET_CSS = "setCSS".toCharArray();
	static final char[] SET_CSS_BY_CLASS = "setCSSByClass".toCharArray();
	static final char[] SET_CSS_FOR_DIV = "setCSSForDiv".toCharArray();
	static final char[] SET_CSS_FOR_TOGGLE = "setCSSForToggle".toCharArray();
	
	static final char[] MLET_DIALOG_GO = "go(\"".toCharArray();
	static final char[] MLET_DIALOG_GOEXTERNALURL = "goExternalURL(\"".toCharArray();
	
	/**
	 * 寻找abc.metho中的abc(return)和metho(preCode)。
	 * 有可能没有return部分，即返回null
	 * @param lineHeader
	 * @param columnIdx
	 * @return
	 */
	private final JRubyClassDesc findPreCodeAfterVar(final char[] lineHeader, final int columnIdx, 
			final int rowIdx, final int partStartIdx){
		preCode = "";
		pre_var_tag_ins_or_global = TYPE_VAR_UNKNOW;
		
		for (int i = columnIdx - 1; i >= partStartIdx; i--) {
			final char c = lineHeader[i];
			if((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_'){
				continue;
			}else if(c == '.'){
				final int offset = i + 1;
				preCode = String.valueOf(lineHeader, offset, columnIdx - offset);
				preCodeSplitIsDot = true;
				return findPreVariableOrMethodOut(lineHeader, i, rowIdx, partStartIdx);
			}else if(c == '"'){
				while(i>=partStartIdx && lineHeader[--i] != '"'){
				}
				continue;
			}else if(c == ':'){
				if(i > partStartIdx && lineHeader[i - 1] == ':'){
					//a = JLable::PARA_1
					final int offset = i + 1;
					preCode = String.valueOf(lineHeader, offset, columnIdx - offset);
					preCodeSplitIsDot = false;
					return findPreVariableOrMethodOut(lineHeader, i - 1, rowIdx, partStartIdx);
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
		
		preCode = String.valueOf(lineHeader, partStartIdx, columnIdx - partStartIdx);
		
		return null;
	}
	
	/**
	 * 从表达式中搜索如：+ {@abc.m()} + {$efg.kk(a, b)} + {edf::efg()} + {abc.efg()}
	 * @param lineHeader
	 * @param rightKuoIdx 后端)所在index
	 * @return
	 */
	private final static String searchLeftAssign(final char[] lineHeader, final int rightKuoIdx, final int partStartIdx){
		int leftKuoIdx = searchLeftMethodStartIdx(lineHeader, rightKuoIdx, partStartIdx);
		
		//变量名段
		if(leftKuoIdx > partStartIdx){
			for (; leftKuoIdx >= partStartIdx; leftKuoIdx--) {
				final char c = lineHeader[leftKuoIdx];
				if(c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == ':' || c == '$' || c == '@'){
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
			final int rightKuoIdx, final int partStartIdx) {
		int kuoHaoDeep = 1;
		int leftKuoIdx = rightKuoIdx - 1;
		
		//()段
		for (; leftKuoIdx >= partStartIdx; leftKuoIdx--) {
			final char c = lineHeader[leftKuoIdx];
			if(c == '('){
				if(--kuoHaoDeep == partStartIdx){
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
		for (; leftKuoIdx >= partStartIdx; leftKuoIdx--) {
			final char c = lineHeader[leftKuoIdx];
			if(c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_'){
				continue;
			}
			if(c == '.'){
				leftKuoIdx--;
				
				if(lineHeader[leftKuoIdx] == ')'){
					//pp.add().add()
					return searchLeftMethodStartIdx(lineHeader, leftKuoIdx, partStartIdx);
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
	
	private final JRubyClassDesc searchVarInIterNode(final IterNode iterNode, final String parameter, 
			final int typeParameter, final CodeContext codeContext){
		final Node body = iterNode.getBody();
		if(body != null){
			final BlockNode blockNode = (BlockNode)body;
			final int size = blockNode.size();
			for (int i = 0; i < size; i++) {
				final NewlineNode newline = (NewlineNode)blockNode.get(i);
				final DefnNode aMethod = (DefnNode)newline.childNodes().get(0);
				if(aMethod.getName().equals(JRUBY_CLASS_INITIALIZE_DEF)){
					final Node out = searchVarDefInInitializeMethod(aMethod.getBody(), parameter, typeParameter, codeContext);
					if(out != null){
						return preFindClassFromReceiverNode(out, codeContext, parameter);
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * 行中出现：f = Java::javax.swing.JLabel::PROP，中出现Java::不由本逻辑处理，由外部逻辑判断提示
	 * @param lineHeader
	 * @param columnIdx
	 * @return
	 */
	private final JRubyClassDesc findPreVariableOrMethodOut(final char[] lineHeader, final int columnIdx, 
			final int rowIdx, final int partStartIdx){
		for (int i = columnIdx - 1; i >= partStartIdx; i--) {
			final char c = lineHeader[i];
			if((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_'){
				continue;
			}else if(c == '@'){
				final int offset = i + 1;
				final String v = String.valueOf(lineHeader, offset, columnIdx - offset);
				final JRubyClassDesc desc = codeContext.getDefJRubyClassDesc();
				if(desc != null && desc.defIterNode != null){
					final JRubyClassDesc out = searchVarInIterNode(desc.defIterNode, v, CodeHelper.TYPE_VAR_INSTANCE, codeContext);
					if(out != null){
						return out;
					}
				}
				return findParaClass(codeContext, v, CodeHelper.TYPE_VAR_INSTANCE);
			}else if(c == '$'){
				final int offset = i + 1;
				final String v = String.valueOf(lineHeader, offset, columnIdx - offset);
				return findParaClass(codeContext, v, CodeHelper.TYPE_VAR_GLOBAL);
			}else if(c == ')'){
				final String assignStr = searchLeftAssign(lineHeader, i, 0);//从左向提取类，注意：不能用partStartIdx
				final List<Node> childNodes = parseScripts(assignStr).childNodes();
				if(childNodes.size() > 0){
					final Node node = childNodes.get(0).childNodes().get(0);//(RootNode, (NewlineNode, (CallNode:add,
					final CodeContext codeContext = new CodeContext(this, root, rowIdx);
					return findClassFromRightAssign(node, codeContext);
				}
			}else{
				final int offset = i + 1;
				final String v = String.valueOf(lineHeader, offset, columnIdx - offset);
				if(CLASS_STATIC.equals(v) && i >= partStartIdx && lineHeader[i] == '.'){//ctx.class.get()调用静态方法
					final JRubyClassDesc jcd = findPreCodeAfterVar(lineHeader, offset, rowIdx, partStartIdx);
					jcd.isInstance = false;//供静态专用
					
					//重新计算preCode
					final int preCodeIdx = columnIdx + 1;
					preCode = String.valueOf(lineHeader, preCodeIdx, lineHeader.length - preCodeIdx);
					preCodeSplitIsDot = true;
					return jcd;
				}else if(JRUBY_JAVA_CLASS.equals(v) && i >= partStartIdx && lineHeader[i] == '.'){//clazInJar.java_class.resource()
					final JRubyClassDesc jcd = buildJRubyClassDesc(JRUBY_JAVA_CLASS_AGENT, false);
					return jcd;
				}else{
					return findParaClass(codeContext, v, CodeHelper.TYPE_VAR_LOCAL);
				}
			}
		}
		final String v = String.valueOf(lineHeader, partStartIdx, columnIdx - partStartIdx);
		if(JRUBY_CLASS_FOR_NEW.equals(v)){
			final JRubyClassDesc desc = buildJRubyClassDesc(JRUBY_CLASS_FOR_BULDER, false);
			return desc;
		}else{
			return findParaClass(codeContext, v, CodeHelper.TYPE_VAR_LOCAL);
		}
	}
	
	private CodeContext codeContext;
	
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
	
//	/**
//	 * 没有，则返回null
//	 * @param preClass
//	 * @return
//	 */
//	public final List<String> getMatchPkg(final String preClass){
//		return getMatch(preClass, CodeStaticHelper.J2SE_PACKAGE_SET, CodeStaticHelper.J2SE_PACKAGE_SET_SIZE);
//	}
	
	public static final String getWordCompletionKeyText(){
		return PropertiesManager.getValue(PropertiesManager.p_wordCompletionKeyCode, "/");
	}
	
	public static final String getWordCompletionKeyChar(){
		return PropertiesManager.getValue(PropertiesManager.p_wordCompletionKeyChar, ResourceUtil.getDefaultWordCompletionKeyChar());
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

	private final void hideByMouseEvent() {
		if(L.isInWorkshop){
			LogManager.log("[CodeTip] start hideByMouseEvent");
		}
		window.hide(true);
		if(L.isInWorkshop){
			LogManager.log("[CodeTip] done hideByMouseEvent");
		}
	}
	
}
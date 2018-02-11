package hc.server.ui.design.code;

import java.awt.Event;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jrubyparser.CompatVersion;
import org.jrubyparser.Parser;
import org.jrubyparser.SourcePosition;
import org.jrubyparser.ast.AliasNode;
import org.jrubyparser.ast.ArgsNode;
import org.jrubyparser.ast.ArgumentNode;
import org.jrubyparser.ast.ArrayNode;
import org.jrubyparser.ast.AssignableNode;
import org.jrubyparser.ast.BignumNode;
import org.jrubyparser.ast.BlockArgNode;
import org.jrubyparser.ast.BlockNode;
import org.jrubyparser.ast.CallNode;
import org.jrubyparser.ast.ClassNode;
import org.jrubyparser.ast.ClassVarDeclNode;
import org.jrubyparser.ast.Colon2Node;
import org.jrubyparser.ast.ConstDeclNode;
import org.jrubyparser.ast.ConstNode;
import org.jrubyparser.ast.DAsgnNode;
import org.jrubyparser.ast.DStrNode;
import org.jrubyparser.ast.DefnNode;
import org.jrubyparser.ast.DefsNode;
import org.jrubyparser.ast.FCallNode;
import org.jrubyparser.ast.FixnumNode;
import org.jrubyparser.ast.FloatNode;
import org.jrubyparser.ast.GlobalAsgnNode;
import org.jrubyparser.ast.GlobalVarNode;
import org.jrubyparser.ast.ILiteralNode;
import org.jrubyparser.ast.INameNode;
import org.jrubyparser.ast.IfNode;
import org.jrubyparser.ast.InstAsgnNode;
import org.jrubyparser.ast.InstVarNode;
import org.jrubyparser.ast.IterNode;
import org.jrubyparser.ast.ListNode;
import org.jrubyparser.ast.LocalAsgnNode;
import org.jrubyparser.ast.LocalVarNode;
import org.jrubyparser.ast.MethodDefNode;
import org.jrubyparser.ast.MethodNameNode;
import org.jrubyparser.ast.MultipleAsgnNode;
import org.jrubyparser.ast.NewlineNode;
import org.jrubyparser.ast.Node;
import org.jrubyparser.ast.NodeType;
import org.jrubyparser.ast.OptArgNode;
import org.jrubyparser.ast.RegexpNode;
import org.jrubyparser.ast.SClassNode;
import org.jrubyparser.ast.StrNode;
import org.jrubyparser.ast.SymbolNode;
import org.jrubyparser.ast.VAliasNode;
import org.jrubyparser.ast.VCallNode;
import org.jrubyparser.ast.ZArrayNode;
import org.jrubyparser.lexer.SyntaxException;
import org.jrubyparser.parser.ParserConfiguration;

import hc.App;
import hc.core.HCTimer;
import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.StringValue;
import hc.server.CallContext;
import hc.server.PlatformManager;
import hc.server.data.KeyComperPanel;
import hc.server.data.StoreDirManager;
import hc.server.ui.Mlet;
import hc.server.ui.SimuMobile;
import hc.server.ui.design.engine.HCJRubyEngine;
import hc.server.ui.design.engine.RubyExector;
import hc.server.ui.design.hpj.HCTextPane;
import hc.server.ui.design.hpj.HPNode;
import hc.server.ui.design.hpj.HPShareJar;
import hc.server.ui.design.hpj.MouseExitHideDocForMouseMovTimer;
import hc.server.ui.design.hpj.ScriptEditPanel;
import hc.server.util.IDEUtil;
import hc.util.ClassUtil;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;
import hc.util.StringBuilderCacher;

public class CodeHelper {
	public static final String OBJECT_STR = "Object";
	public static final String JAVA_PACKAGE_CLASS_PREFIX = "java";
	public static final String HC_PACKAGE_CLASS_PREFIX = "hc.";
	private static final String CLASS_STATIC = "class";//ctx.class.getProjectContext()
	private static final String CLASS_STATIC_PREFIX = CLASS_STATIC + ".";//ctx.class.getProjectContext()
	private static final String TO_UPPER_CASE = "upcase";
	private static final String TO_DOWN_CASE = "downcase";
	private static final String JAVA_ARRAY_LENGTH = "length";//array.length #=> 5
	private static final String F_ARRAY_LENGTH = "length";//array.length #=> 5
	private static final String F_ARRAY_COUNT = "count";//array.count #=> 5
	private static final String F_ARRAY_EMPTY = "empty?";//array.empty? #=> false
	private static final String TO_S = "to_s";
	private static final String REGEXP_MATCH = "match";
	private static final String TO_F = "to_f";
	private static final String TO_A = "to_a";
	private static final String ARRAY_FLAG = "[]";
	private static final String TO_I = "to_i";
	private static final int ACC_PRIVATE = 1;
	private static final int ACC_PROTECTED = 2;
	private static final int ACC_PUBLIC = 3;
	
	private static final char PRE_GLOBAL_DALLOR_CHAR = '$';
	private static final String PRE_GLOBAL_DALLOR_STR = "$";
	private static final char PRE_CLASS_VAR_OR_INS_CHAR = '@';
	private static final String PRE_CLASS_VARIABLE_STR = "@@";
	private static final String PRE_CLASS_INSTANCE_STR = "@";
	
	static final Class J2SE_STRING_CLASS = String.class;
	static final Class J2SE_ARRAY_CLASS = J2SEArrayID.class;
	
	/**
	 * Kernel方法与用户自定义方法大小写相同
	 */
	public static final boolean DISABLE_SAME_METHOD_NAME = true;

	private static final String JRUBY_CLASS_INITIALIZE_DEF = "initialize";
	private static final String JRUBY_INCLUDE = "include";
	public static final String JRUBY_NEW = "new";
	public static final String JRUBY_NEW_METHOD = JRUBY_NEW + "(";
	private static final String JRUBY_CLASS_FOR_NEW = "Class";
	public static final String JRUBY_JAVA_CLASS = "java_class";
	public static final char RUBY_METHOD_BOOL_CHAR = '?';//str.nil??case1:case2
	public static final char RUBY_METHOD_MODI_CHAR = '!';
	private static final Class JRUBY_JAVA_CLASS_AGENT = JavaClass.class;
	static final String IterNodeClass = IterNode.class.getName();
	private static final Class JRUBY_CLASS_FOR_BULDER = ClassBulder.class;
	public static final String DEF_MEMBER = "def";
	private static final char[] DEF_MEMBER_BS = DEF_MEMBER.toCharArray();
	private static final JRubyClassDesc objectJCD = buildJRubyClassDesc(Object.class, true);
	private static final JRubyClassDesc rubyKernelJCD = buildJRubyClassDesc(RubyHelper.JRUBY_KERNEL_CLASS, true);
	private static final JRubyClassDesc rubyClassJCD = buildJRubyClassDesc(RubyHelper.JRUBY_CLASS_CLASS, true);
	private static final JRubyClassDesc rubyModuleJCD = buildJRubyClassDesc(RubyHelper.JRUBY_MODULE_CLASS, true);
	private static final JRubyClassDesc rubyEnumerableJCD = buildJRubyClassDesc(RubyHelper.JRUBY_ENUMERABLE_CLASS, true);
	private static final JRubyClassDesc rubyObjectJCD = buildJRubyClassDesc(RubyHelper.JRUBY_OBJECT_CLASS, true);
	private static final JRubyClassDesc rubyArrayJCD = buildJRubyClassDesc(RubyHelper.JRUBY_ARRAY_CLASS, true);
	static boolean isDisplayOverrideMethodAndDoc = false;
	
	private final NodeLibClassesAndResMap nodeLibClassesAndResMap = new NodeLibClassesAndResMap();
	
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
			}
		}
	};
	
	public final void resetAll(){
		window.codeInvokeCounter.reset();
		window.docHelper.resetCache();
		synchronized (classCacheMethodAndPropForProject) {
			PlatformManager.getService().resetClassPool();
			classCacheMethodAndPropForClass.clear();
			classCacheMethodAndPropForInstance.clear();
			classCacheMethodAndPropForProject.clear();
		}
	}
	
	public final void reset(){
		synchronized (classCacheMethodAndPropForProject) {
			PlatformManager.getService().resetClassPool();
			
			final int size = classCacheMethodAndPropForProject.size();
			for (int i = 0; i < size; i++) {
				final String key = classCacheMethodAndPropForProject.get(i);
				classCacheMethodAndPropForClass.remove(key);
				classCacheMethodAndPropForInstance.remove(key);
			}
			
			classCacheMethodAndPropForProject.clear();
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
	
	private final static String getMethodNameFromRubyMethod(final String method){
		int endIdx = method.indexOf(RUBY_METHOD_MODI_CHAR);
		if(endIdx > 0){
			return method.substring(0, endIdx);
		}
		endIdx = method.indexOf('(');
		if(endIdx > 0){
			return method.substring(0, endIdx);
		}
		
		return method;
	}
	
	private final static void buildMethodForRubyClass(final RubyClassAndDoc clazDoc, final ArrayList<CodeItem> list,
			final boolean isForClass){
		buildMethodForRubyClass(clazDoc, list, isForClass?clazDoc.staticMethods:clazDoc.insMethods, isForClass);
	}
	
	private final static void buildMethodForRubyClass(final RubyClassAndDoc clazDoc, final ArrayList<CodeItem> list, final Vector<RubyMethodItem> methods,
			final boolean isForClass){
		final int methodSize = methods.size();
		
		for (int i = 0; i < methodSize; i++) {
			final RubyMethodItem rubyMethodItem = methods.get(i);
			final String codeMethod = rubyMethodItem.methodOrField;
			final RubyClassAndDoc rt = rubyMethodItem.returnType;
			final String returnType = rt==null?"":rt.displayClassName;
			
			final boolean findSameName = CodeItem.contains(list, (isForClass?RubyHelper.RUBY_STATIC_MEMBER_DOC:"")+codeMethod);
			if(findSameName){
				continue;
			}
			
			final CodeItem item = CodeItem.getFree();
			item.fieldOrMethodOrClassName = getMethodNameFromRubyMethod(codeMethod);
			item.code = codeMethod;//一般方法直接输出，静态方法加.class.转换
			{
				final String codeForDoc = rubyMethodItem.methodOrFieldForDoc;
				final int cuohaoIdx = codeForDoc.indexOf('(');
				item.codeForDoc = (isForClass?RubyHelper.RUBY_STATIC_MEMBER_DOC:"") + (cuohaoIdx>0?(codeForDoc.substring(0, cuohaoIdx)):codeForDoc);
			}
			item.fmClass = clazDoc.claz.getName();
			item.codeDisplay = codeMethod + (returnType==null?"":(" : " + returnType)) + " - " + clazDoc.displayClassName;
			item.codeLowMatch = codeMethod.toLowerCase();
			item.isRubyClass = true;
			
			item.isPublic = rubyMethodItem.isPublic;
			item.modifiers = Modifier.PUBLIC;
			item.isForMaoHaoOnly = false;//!Modifier.isStatic(modifiers);
			if(rubyMethodItem.isConstant){
				item.type = CodeItem.TYPE_FIELD;
			}else{
				item.type = CodeItem.TYPE_METHOD;
			}
			item.setAnonymouseClassType(null);
			
			list.add(item);
		}
	}
	
	private final static void buildMethodAndProp(final Class c, final boolean isForClass, final ArrayList<CodeItem> out, 
			final boolean needNewMethod, final CodeHelper codeHelper, final HashMap<String, ArrayList<CodeItem>> set, final String classFullName,
			final Vector<String> addedInterface){
		if(c == null){
			return;
		}
		
		{
			if(c.isInterface()){
				if(addedInterface.contains(classFullName)){
					return;
				}
				addedInterface.add(classFullName);
			}
			final ArrayList<CodeItem> list = set.get(classFullName);
			if(list != null){
				out.addAll(list);
				return;
			}
		}
		
		final ArrayList<CodeItem> list = new ArrayList<CodeItem>(100);
		try{
			
			final RubyClassAndDoc clazDoc = RubyHelper.searchRubyClass(c);
			if(clazDoc != null){
				codeHelper.window.docHelper.processDoc(codeHelper, clazDoc.claz, false);//要先行处理，以获得methods
				
				appendSuperOrNewMethod(clazDoc.claz, isForClass, needNewMethod, codeHelper, set, list, addedInterface);
				buildMethodForRubyClass(clazDoc, list, isForClass);
				return;
			}
			
			if(c != J2SE_ARRAY_CLASS){
				appendSuperOrNewMethod(c, isForClass, needNewMethod, codeHelper, set, list, addedInterface);
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
	
			if(isForClass == false){
				if(c == J2SE_STRING_CLASS){
					addToMethod(c, list, TO_DOWN_CASE, "String", J2SE_STRING_CLASS.getSimpleName());
					addToMethod(c, list, TO_UPPER_CASE, "String", J2SE_STRING_CLASS.getSimpleName());
					addToMethod(c, list, TO_I, "int", J2SE_STRING_CLASS.getSimpleName());
					addToMethod(c, list, TO_F, "float", J2SE_STRING_CLASS.getSimpleName());
				}else if(c == byte.class){
					addToMethod(c, list, TO_S, "String", "byte");
				}else if(c == char.class){
					addToMethod(c, list, TO_S, "String", "char");
				}else if(c == short.class){
					addToMethod(c, list, TO_S, "String", "short");
				}else if(c == int.class){
					addToMethod(c, list, TO_S, "String", "int");
				}else if(c == long.class){
					addToMethod(c, list, TO_S, "String", "long");
				}else if(c == float.class){
					addToMethod(c, list, TO_S, "String", "float");
				}else if(c == double.class){
					addToMethod(c, list, TO_S, "String", "double");
				}else if(c == boolean.class){
					addToMethod(c, list, TO_S, "String", "boolean");
				}else if(c == J2SE_ARRAY_CLASS){
					codeHelper.getMethodAndFieldForInstance(false, rubyArrayJCD, true, list, true, false, false);
//					addToField(c, list, JAVA_ARRAY_LENGTH, "int", "Object[]");
//					addToField(c, list, TO_A, "JRuby[]", "Object[]");
					return;
				}else if(c == Object.class){
					codeHelper.getMethodAndFieldForInstance(false, rubyObjectJCD, true, list, true, false, false);
//					addToField(c, list, NIL, "boolean", OBJECT_STR);//is Nil
	//				addToField(c, list, TO_A, "JRuby[]", "Object[]");//因上段j2seArrayClass
				}
			}
			
			if(isForClass){//一般用于用户Jar库的类
				if(c != JRUBY_JAVA_CLASS_AGENT){
					addToField(c, list, JRUBY_JAVA_CLASS, "Class", "RubyClass");//后两参数仅表示，没有实际相关
				}
	//			item.isForMaoHaoOnly = true;
			}
			
			final String simpleClassName = c.getSimpleName();
			
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
					item.codeForDoc = codeField;//item.code; 后者可能含有class.getProjectContext()，导致不能打开Doc
					item.fmClass = classFullName;
					item.codeDisplay = codeField + " : " + ReturnType.getGenericReturnTypeDesc(field.getGenericType()) + " - " + simpleClassName;
					item.codeLowMatch = codeField.toLowerCase();
					item.isPublic = isPublic;
					item.modifiers = modifiers;
					item.isForMaoHaoOnly = isStatic;//不能如方法改为false，因为import Java::HTMLMlet\n HTMLMlet.URL_EXIT会出错，这是由于JRuby语法所限，1.7.3如此
					item.type = CodeItem.TYPE_FIELD;
					
					list.add(item);
				}
			}
			
			final Vector<String> deprecatedMethods = CodeStaticHelper.deprecatedMethodsAndFields.get(c);
			
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
					final Type[] paras = method.getGenericParameterTypes();
					final String[] codeParas;
					if(classFullName.startsWith(HC_PACKAGE_CLASS_PREFIX) || classFullName.startsWith(JAVA_PACKAGE_CLASS_PREFIX)){
						codeParas = null;//provides from Java doc
					}else{
						codeParas = PlatformManager.getService().getMethodCodeParameter(method);
					}
					String paraStr = "";
					String paraStrForDisplay = "";
					final int paraNum = paras.length;
					for (int j = 0; j < paraNum; j++) {
						if(paraStr.length() > 0){
							paraStr += ", ";
							paraStrForDisplay += ", ";
						}
						final Type paraClass = paras[j];
						final String simpleName = ReturnType.getGenericReturnTypeDesc(paraClass);
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
					final String codeMethodForDoc = codeMethodForDisplay;//[(isForClass==false && isStatic)?(CLASS_STATIC_PREFIX + codeMethodForDisplay):codeMethodForDisplay] 后者可能含有class.getProjectContext()，导致不能打开Doc
					
					if(deprecatedMethods != null && deprecatedMethods.contains(codeMethodForDoc)){
						continue;
					}
					
					final CodeItem item = CodeItem.getFree();
					item.fieldOrMethodOrClassName = method.getName();
					item.code = codeMethodForInput;//一般方法直接输出，静态方法加.class.转换
					item.codeForDoc = codeMethodForDoc;
					item.fmClass = classFullName;
					item.codeDisplay = codeMethodForDisplay + " : " + ReturnType.getGenericReturnTypeDesc(method.getGenericReturnType()) + " - " + simpleClassName;
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
			codeHelper.window.codeInvokeCounter.initCounter(list);
			
			set.put(classFullName, list);
			
			if(isForClass == false){//注意：等待静态先处理，其次才是for instance
				codeHelper.window.docHelper.processDoc(codeHelper, c, false);
			}
			
			out.addAll(list);
		}
	}
	
	private static void appendSuperOrNewMethod(final Class c, final boolean isForClass,
			final boolean needNewMethod, final CodeHelper codeHelper,
			final HashMap<String, ArrayList<CodeItem>> set, final ArrayList<CodeItem> list, final Vector<String> addedInterface) {
		if(isForClass == false){
			final Class superclass = c.getSuperclass();
			if(superclass != null){
				if(RubyHelper.searchRubyClass(c) != null && superclass == Object.class){
					return;
				}
				
				buildMethodAndProp(superclass, isForClass, list, false, codeHelper, set, superclass.getName(), addedInterface);
			}
			
			final Class[] interfaces = c.getInterfaces();
			for (int i = 0; i < interfaces.length; i++) {
				final Class face1 = interfaces[i];
				final String interfaceFullName = face1.getName();
				if(addedInterface.contains(interfaceFullName)) {
					continue;
				}
				buildMethodAndProp(face1, isForClass, list, false, codeHelper, set, interfaceFullName, addedInterface);
			}
		}else if(needNewMethod){
			if(c == Mlet.class || c == JavaClass.class){//强制不出现Mlet的new方法
				return;
			}
			
			final Constructor[] cons = c.getDeclaredConstructors();
			final int size = cons.length;
			final String fullClassName = c.getName();
			final String simpleClassName = c.getSimpleName();
			if(size == 0){
				final CodeItem item = CodeItem.getFree();
				item.fieldOrMethodOrClassName = JRUBY_NEW;
				item.code = JRUBY_NEW + "()";
				item.codeForDoc = item.code;
				item.codeDisplay = item.code + " - " + simpleClassName;
				item.codeLowMatch = item.code.toLowerCase();
				
				item.isPublic = true;
				item.isForMaoHaoOnly = false;
				item.type = CodeItem.TYPE_METHOD;
				item.fmClass = fullClassName;
				
				list.add(item);
			}else{
				for (int i = 0; i < size; i++) {
					final Constructor con = cons[i];
					final int conModifier = con.getModifiers();
					if(Modifier.isProtected(conModifier) || Modifier.isPublic(conModifier)){
					}else{
						continue;
					}
					
					final Type[] paras = con.getGenericParameterTypes();
					String paraStr = "";
					for (int j = 0; j < paras.length; j++) {
						if(paraStr.length() > 0){
							paraStr += ", ";
						}
						paraStr += ReturnType.getGenericReturnTypeDesc(paras[j]);
					}
					
					final CodeItem item = CodeItem.getFree();
					item.fieldOrMethodOrClassName = JRUBY_NEW;
					item.code = JRUBY_NEW + (paraStr.length()==0?"()":"(" + paraStr + ")");
					item.codeForDoc = item.code;
					item.codeDisplay = item.code + " - " + simpleClassName;
					item.codeLowMatch = item.code.toLowerCase();
					
					item.isPublic = true;
					item.isForMaoHaoOnly = false;
					item.type = CodeItem.TYPE_METHOD;
					item.fmClass = fullClassName;
					
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
	
	private static CodeItem addStructFields(final ArrayList<CodeItem> list, final String methodName, final String resultType) {
		final CodeItem item = CodeItem.getFree();
		item.fieldOrMethodOrClassName = methodName;
		item.code = methodName;
		
		item.codeForDoc = item.code;
		item.fmClass = CodeInvokeCounter.CLASS_UN_INVOKE_COUNT_STRUCT;
		item.codeDisplay = item.code + " : " + resultType;
		item.codeLowMatch = item.code.toLowerCase();
		item.isPublic = true;
		item.isForMaoHaoOnly = false;
		item.type = CodeItem.TYPE_FIELD;
		
		list.add(item);
		
		return item;
	}
	
	private static CodeItem addToMethodOrField(final Class claz, final int type, final ArrayList<CodeItem> list, final String methodName, final String resultType, final String baseClassName) {
		final CodeItem item = CodeItem.getFree();
		item.fieldOrMethodOrClassName = methodName;
//		if(type == CodeItem.TYPE_METHOD){
//			item.code = methodName + "()";
//		}else{
			item.code = methodName;
//		}
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

	private final static CodeItem[] buildMethodAndField(final Class c, final boolean isForClass, 
			final HashMap<String, ArrayList<CodeItem>> set, final CodeHelper codeHelper){
		final ArrayList<CodeItem> list = new ArrayList<CodeItem>();
		
		if(c != null){
			buildMethodAndProp(c, isForClass, list, true, codeHelper, set, c.getName(), new Vector<String>());
		}
		
//		if(defNode == null){
//		}else{
//			final String key = getDefClassName(defNode);
//			if(key.startsWith(HC_PACKAGE_CLASS_PREFIX) || key.startsWith(JAVA_PACKAGE_CLASS_PREFIX)){
//			}else{
//				codeHelper.classCacheMethodAndPropForProject.add(key);
//			}
//			if(isBuildForClass == false){
//				buildMethodAndProp(c, isForClass, list, true, codeHelper, set, key);
//			}else{
//				list.addAll(set.get(c.getName()));
//			}
//		}
		
		final Object[] objs = list.toArray();
		final CodeItem[] out = new CodeItem[objs.length];
		System.arraycopy(objs, 0, out, 0, objs.length);
		Arrays.sort(out);
		
		return out;
	}
	
//	Method-related hooks
//	method_added, method_missing, method_removed, method_undefined, singleton_method_added, singleton_method_removed, singleton_method_undefined
//	Class and module-related hooks
//	append_features, const_missing, extend_object, extended, included, inherited, initialize_clone, initial- ize_copy, initialize_dup
//	Object marshaling hooks marshal_dump, marshal_load
//	Coercion hooks
//	coerce, induced_from, to_xxx
	static final String[] methodHooks = {"method_added", "method_missing", "method_removed", "method_undefined", 
		"singleton_method_added", "singleton_method_removed", "singleton_method_undefined"	};

	private final void appendMethodFromDef(final boolean isForClass, final String className, final MethodDefNode defN, 
			final ArrayList<CodeItem> list, final boolean isNeedAppendInit) {
		String methodName = defN.getName();
		
		final int methodHookSize = methodHooks.length;
		for (int i = 0; i < methodHookSize; i++) {
			if(methodName.equals(methodHooks[i])){
				return;
			}
		}
		
		if(methodName.equals(JRUBY_CLASS_INITIALIZE_DEF)){
			if(isNeedAppendInit == false){
				return;
			}
			methodName = JRUBY_NEW;
		}else{
			if(isForClass && defN instanceof DefnNode){
				//注意：DefnNode for instance, 
				return;
			}
			if(isForClass == false && defN instanceof DefsNode){
				//注意：DefsNode for class
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
	public final ArrayList<CodeItem> getMethodAndFieldForInstance(final boolean needNewMethod, final JRubyClassDesc jdc, final boolean needPublic, 
			final ArrayList<CodeItem> out, final boolean isAppend, final boolean isNeedAppendInit, final boolean dontAppendIfSameMethodName){
		return getMethodAndField(needNewMethod, jdc, needPublic, out, classCacheMethodAndPropForInstance, false, isAppend, isNeedAppendInit, dontAppendIfSameMethodName);
	}
	
	/**
	 * 为类查找可用的静态方法和属性
	 * @param c
	 * @param preName
	 * @param out
	 * @return
	 */
	public final ArrayList<CodeItem> getMethodAndFieldForClass(final JRubyClassDesc jcd, final ArrayList<CodeItem> out,
			final boolean isAppend, final boolean isNeedAppendInit, final boolean dontAppendIfSameMethodName){
		return getMethodAndField(true, jcd, true, out, classCacheMethodAndPropForClass, true, isAppend, isNeedAppendInit, dontAppendIfSameMethodName);
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
			buildForClass(this, claz);
			methods = set.get(className);
		}
		if(methods != null){
			appendToOut(methods, needPublic, out, needNewMethod, false);
		}
	}
	
	private final ArrayList<CodeItem> getMethodAndField(final boolean needNewMethod, final JRubyClassDesc jdc, final boolean needPublic, 
			final ArrayList<CodeItem> out, final HashMap<String, ArrayList<CodeItem>> set, final boolean isForClass,
			final boolean isAppend, final boolean isNeedAppendInit, final boolean dontAppendIfSameMethodName){
		if(isAppend == false){
			clearArray(out);
		}
		
		{
			final Class backgroundOrPreVar = jdc.getClassForDoc();
			final String className = backgroundOrPreVar.getName();
			
			ArrayList<CodeItem> methods = set.get(className);
			if(methods == null){
				buildForClass(this, buildJRubyClassDesc(backgroundOrPreVar, false));
				methods = set.get(className);
			}
			
			appendToOut(methods, needPublic, out, needNewMethod && (jdc.hasExtChain()?false:true), dontAppendIfSameMethodName);
		}
		
		if(jdc.hasExtChain()){
			final Object[] defNodes = jdc.getReturnType().toClassDefNodeArray();
			boolean isInDefClass = false;
			final int topClassIdx = defNodes.length - 1;
			for (int i = topClassIdx; i >= 0; i--) {
				final ClassNode defNode = (ClassNode)defNodes[i];
				
//				ArrayList<CodeItem> methods = set.get(getDefClassName(defNode));
//				if(methods == null){
//					buildForClass(this, jdc);
//					methods = set.get(getDefClassName(defNode));
//				}
//				if(methods != null){
//					appendToOut(methods, needPublic, out, true);
//				}
				if(defNode != null){
					final int reqAccessLevelOrAbove;
					if(i == topClassIdx){//后来者居上
						if(isInClassDefNode(codeContext.bottomNode) == defNode){
							if(preCodeType == PRE_TYPE_BEFORE_INSTANCE){
								reqAccessLevelOrAbove = ACC_PRIVATE;
							}else{
								reqAccessLevelOrAbove = ACC_PROTECTED;
							}
							isInDefClass = true;
						}else{
							reqAccessLevelOrAbove = ACC_PUBLIC;
						}
					}else{
						if(isInDefClass){
							if(preCodeType == PRE_TYPE_BEFORE_INSTANCE){
								reqAccessLevelOrAbove = ACC_PRIVATE;
							}else{
								reqAccessLevelOrAbove = ACC_PROTECTED;
							}
						}else{
							reqAccessLevelOrAbove = ACC_PUBLIC;
						}
					}
					appendDef(reqAccessLevelOrAbove, defNode, isForClass, out, isNeedAppendInit);
				}
			}
		}else if(jdc != null && jdc.innerDefNode != null){
			appendDef(ACC_PRIVATE, jdc.innerDefNode, isForClass, out);
		}
		
		return out;
	}
	
//	private final ArrayList<CodeItem> getMethodAndFieldItem(final JRubyClassDesc jdc, final ClassNode defNode, final boolean needPublic, 
//			final ArrayList<CodeItem> out, final HashMap<String, ArrayList<CodeItem>> set, final boolean isForClass,
//			final boolean isAppend, final boolean isNeedAppendInit){
//		//仅取定义体中的
//		final boolean isDefNode = defNode != null;
//		
//		if(defNode != null){
//			ArrayList<CodeItem> methods = set.get(getDefClassName(defNode));
//			if(methods == null){
//				buildForClass(this, jdc);
//				methods = set.get(getDefClassName(defNode));
//			}
//			if(methods != null){
//				appendToOut(methods, needPublic, out, true);
//			}
//			if(defNode != null){
//				appendDef(defNode, isForClass, out, isNeedAppendInit);
//			}
//			if(out.size() > 0){
//				return out;
//			}
//		}
//		
//		if(isDefNode){
//			ArrayList<CodeItem> methods = set.get(getDefClassName(defNode));
//			if(methods == null && jdc != null){
//				if(methods == null){
//					buildForClass(this, jdc);
//					methods = set.get(getDefClassName(defNode));
//				}
//				if(methods != null){
//					appendToOut(methods, needPublic, out, true);
//				}
//				if(defNode != null){
//					appendDef(defNode, isForClass, out, isNeedAppendInit);
//				}
//			}
//			if(out.size() > 0){
//				return out;
//			}
//		}
//		
//		if(isDefNode){
//			appendDef(defNode, isForClass, out, isNeedAppendInit);
//		}else if(jdc != null && jdc.innerDefNode != null){
//			appendDef(jdc.innerDefNode, false, out);
//		}
//		return out;
//	}

	private final ArrayList<CodeItem> appendToOut(final ArrayList<CodeItem> methods,
			final boolean needPublic, final ArrayList<CodeItem> out, final boolean needNewMethod, final boolean dontAppendIfSameMethodName) {
		final int size = methods.size();
		for (int i = 0; i < size; i++) {
			final CodeItem tryMethod = methods.get(i);
			if(needPublic){
				if(tryMethod.isPublic){
				}else{
					continue;
				}
			}
			if(needNewMethod == false && JRUBY_NEW.equals(tryMethod.fieldOrMethodOrClassName)){//defined JRuby class is NOT required super class new methods.
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
				
				if(dontAppendIfSameMethodName && CodeItem.containsSameFieldMethodName(out, tryMethod.fieldOrMethodOrClassName)){
					continue;
				}
				
				out.add(tryMethod);
			}
		}
		
		return out;
	}

	private final static void buildForClass(final CodeHelper codeHelper, final JRubyClassDesc jcd) {
		buildForClass(codeHelper, jcd.getClassForDoc());
	}
	
	public static void buildForClass(final CodeHelper codeHelper, final Class claz) {
		//注意：以下两行，次序不能变动，因docHelper.processDoc依赖最后一个
		buildMethodAndField(claz, true, codeHelper.classCacheMethodAndPropForClass, codeHelper);
		buildMethodAndField(claz, false, codeHelper.classCacheMethodAndPropForInstance, codeHelper);
	}

	private final void appendDef(final int reqAccessLevelOrAbove, final CallNode innerNode, final boolean isForClass, final ArrayList<CodeItem> list) {
		final Node body = ((IterNode)innerNode.getIter()).getBody();
		appendFromDefBody(reqAccessLevelOrAbove, body, DEF_MEMBER, isForClass, list, false);
	}
	
	private final void appendDef(final int reqAccessLevelOrAbove, final ClassNode defNode, final boolean isForClass, final ArrayList<CodeItem> list,
			final boolean isNeedAppendInit) {
		//将定义体的类的常量和构造方法提取出来，装入代码提示表中
		if(defNode != null){
			
			final ClassNode classNode = defNode;
			final String className = getDefClassName(classNode);
			final Node body = classNode.getBody();//注意：有可能只定义类，而没有定义体，导致body为null
			appendFromDefBody(reqAccessLevelOrAbove, body, className, isForClass, list, isNeedAppendInit);
		}
	}

	private final void appendFromDefBody(final int reqAccessLevelOrAbove, final Node body, final String className, final boolean isForClass, 
			final ArrayList<CodeItem> list, final boolean isNeedAppendInit) {
		if(body == null){
			if(isNeedAppendInit){
				appendDefaultInitialize(isForClass, list, className);
			}
			return;
		}
		
		//(NewlineNode, (VCallNode:private))
		
		final List<Node> listNodes = body.childNodes();
		final int size = listNodes.size();
		int accLevel = ACC_PUBLIC;
		
		for (int i = 0; i < size; i++) {
			Node sub = listNodes.get(i);
			if(sub instanceof NewlineNode){
				sub = ((NewlineNode)sub).getNextNode();
			}
			if(sub == null){
				continue;
			}
			if(sub instanceof VCallNode){
				final VCallNode vc = (VCallNode)sub;
				final String name = vc.getName();
				if(name.equals("private")){
					accLevel = ACC_PRIVATE;
				}else if(name.equals("protected")){
					accLevel = ACC_PROTECTED;
				}else if(name.equals("public")){
					accLevel = ACC_PUBLIC;
				}
				continue;
			}
			
			if(reqAccessLevelOrAbove > accLevel){
				continue;
			}
			
			if(sub instanceof ConstDeclNode){//实例方法可以访问它
				do{
					final ConstDeclNode constNode = (ConstDeclNode)sub;
					final String name = constNode.getName();
					if(CodeItem.contains(list, name) == false){
						final CodeItem item = CodeItem.getFree();
						item.fieldOrMethodOrClassName = name;
						item.code = name;
						item.codeForDoc = item.code;
						final Class classOfConst = getClassFromLiteral(searchValueNodeFromAssignableNode(constNode));
						item.fmClass = classOfConst.getName();//Object.class.getName();
						item.codeDisplay = item.code + " : " + classOfConst.getSimpleName() + " - " + className;
						item.codeLowMatch = item.code.toLowerCase();
						item.isPublic = true;
						item.isDefed = true;
						item.isForMaoHaoOnly = true;
						item.type = CodeItem.TYPE_FIELD;
						
						list.add(item);
					}
					sub = constNode.getValue();
				}while(sub instanceof ConstDeclNode);
//			}else if(sub instanceof InstAsgnNode){//@var in body, not in init，所以isForClass必须
//				final InstAsgnNode constNode = (InstAsgnNode)sub;
//				
//				final CodeItem item = CodeItem.getFree();
//				final String name = constNode.getName();
//				item.fieldOrMethodOrClassName = name;
//				item.code = PRE_CLASS_INSTANCE_STR + name;
//				item.codeForDoc = name;
//				final Class classOfConst = getClassFromLiteral(constNode.getValue());
//				item.fmClass = classOfConst.getName();//Object.class.getName();
//				item.codeDisplay = item.code + " : " + classOfConst.getSimpleName() + " - " + className;
//				item.codeLowMatch = item.code.toLowerCase();
////				item.isPublic = true;
////				item.isDefed = true;
////				item.isForMaoHaoOnly = true;
//				item.type = CodeItem.TYPE_FIELD;
//				
//				list.add(item);
			}else if((isForClass && (sub instanceof DefsNode)) || ((isForClass == false) && (sub instanceof DefnNode))){
				appendMethodFromDef(isForClass, className, (MethodDefNode)sub, list, isNeedAppendInit);
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
	
	public ArrayList<CodeItem> getSubPackageAndClasses(final ArrayList<CodeItem> out, 
			final ArrayList<String> requireLibs, final boolean isJavaClassSetNotHcClass, final boolean isAppend){
		if(isAppend == false){
			clearArray(out);
		}
		
		if(isJavaClassSetNotHcClass){
			appendPackageAndClass(out, CodeStaticHelper.J2SE_CLASS_SET, CodeStaticHelper.J2SE_CLASS_SET_SIZE, false);
		}
		if(isJavaClassSetNotHcClass == false){
			appendPackageAndClass(out, CodeStaticHelper.HC_CLASS_SET, CodeStaticHelper.HC_CLASS_SET_SIZE, false);
		}
		
		if(isJavaClassSetNotHcClass == false && requireLibs != null){
			final int size = requireLibs.size();
			for (int i = 0; i < size; i++) {
				final String libName = requireLibs.get(i);
				final String[] classAndRes = nodeLibClassesAndResMap.searchLibClassesAndRes(libName);
				
				if(classAndRes != null){//有可能添加后，又删除了库，但代码仍保留，不拦截此错误，脚本自动编译时，会产生错误提示
					appendPackageAndClass(out, classAndRes, classAndRes.length, false);
				}else{
					App.showMessageDialog(null, "NOT found lib '" + libName + "', but it is required in scripts!", ResourceUtil.getErrorI18N(), 
							JOptionPane.ERROR_MESSAGE, App.getSysIcon(App.SYS_ERROR_ICON));
					throw new Error("No found or removed lib : " + libName);
				}
			}
		}
		
		window.codeInvokeCounter.initCounter(out);
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
				final String[] classAndRes = nodeLibClassesAndResMap.searchLibClassesAndRes(libName);
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
				final CodeItem item = buildClassItemForDocClass(pkg);
				out.add(item);
			}
		}
	}
	
	static final String javaLangSystem = "java.lang.System";
	static final String wrapJavaLangSystem = "Java::hc.server.util.JavaLangSystemAgent";
	
	/**
	 * 该item应用于显示类的java doc。
	 */
	private static CodeItem buildClassItemForDocClass(final String pkg) {
		final boolean isJavaLangSystem = pkg.equals(javaLangSystem);
		
		final CodeItem item = CodeItem.getFree();
		item.type = CodeItem.TYPE_CLASS;
		item.fieldOrMethodOrClassName = pkg;
		item.code = isJavaLangSystem?wrapJavaLangSystem:pkg;
		item.codeForDoc = item.code;
		item.codeDisplay = pkg;
		item.codeLowMatch = pkg.toLowerCase();
		item.isFullPackageAndClassName = true;
		item.fmClass = CodeItem.FM_CLASS_PACKAGE;
		item.isRubyClass = RubyHelper.searchRubyClassByFullName(pkg) != null;
		
		return item;
	}
	
	public final void loadLibToCodeHelper(final DefaultMutableTreeNode node){
		final HPShareJar jar = (HPShareJar)node.getUserObject();
		final byte[] fileContent = jar.content;
		
		final File tmpFile = ResourceUtil.createRandomFileWithExt(StoreDirManager.TEMP_DIR, StoreDirManager.HCTMP_EXT);
		
		if(ResourceUtil.writeToFile(fileContent, tmpFile)){
			final ArrayList<String> classAndRes = J2SEClassBuilder.getClassAndResByJar(tmpFile, true);
			final String[] cr = CodeStaticHelper.convertArray(classAndRes);
			nodeLibClassesAndResMap.put(jar, cr);
		}
				
		tmpFile.delete();
	}
	
	public final void loadLibForTest(final HPNode node){
		nodeLibClassesAndResMap.put(node, null);
	}

//	private final String getLowerLibName(final String fileShortName) {
//		return fileShortName + EXT_LOWER;
//	}
	
	private final static String EXT_LOWER = "_lOwEr";
	
	public final void unloadLibFromCodeHelper(final HPNode node){
		nodeLibClassesAndResMap.remove(node);
	}
	
	public CodeHelper(){
		initShortCutKeys();
		window = new CodeWindow(this);
		buildForClass(this, Object.class);//因为appendObjectForInterfaceOnly，所以提前
		buildForClass(this, int.class);
		buildForClass(this, float.class);
		RubyHelper.codeHelper = this;
		buildForClass(this, RubyHelper.JRUBY_KERNEL_CLASS);
		
		final CodeItem[] out = buildMethodAndField(JRUBY_CLASS_FOR_BULDER, true, classCacheMethodAndPropForClass, this);
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
		sb.append(ClassUtil.invoke(Node.class, node, "getNodeName", ClassUtil.NULL_PARA_TYPES, ClassUtil.NULL_PARAS, true));

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
		return null;
	}

	private static JRubyClassDesc buildClassDescFromSClassNode(final CodeContext codeContext,
			final Node classNode) {
		final SClassNode sClassNode = (SClassNode)classNode;
		final JRubyClassDesc base = findClassFromRightAssign(sClassNode.getReceiver(), codeContext);//LocalVarNode:noRun
		final JRubyClassDesc classDesc = buildJRubyClassDesc(base.getReturnType(), true);
		classDesc.defSClassNode = sClassNode;
		return classDesc;
	}

	private static CodeContext buildNewCodeContext(final CodeContext codeContext,
			final int startLine) {
		return new CodeContext(codeContext.codeHelper, codeContext.contextNode, startLine);
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
				final ReturnType claz = getClassByNode(constNode, codeContext);
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
	
	public final static ReturnType getDefSuperClass(final Node node, final int scriptIdx, final int lineIdxAtScript, final CodeContext codeContext){
		final JRubyClassDesc classNode = isInDefClass(node, codeContext, lineIdxAtScript);
		if(classNode != null){
			return classNode.getReturnType();
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
	
	public final void flipTipStop() {
		if(mouseExitHideDocForMouseMovTimer.isEnable() == false){
			mouseExitHideDocForMouseMovTimer.resetTimerCount();
			mouseExitHideDocForMouseMovTimer.setEnable(true);
		}
	}

	public final void flipTipKeepOn() {
		if(mouseExitHideDocForMouseMovTimer.isEnable()){
			mouseExitHideDocForMouseMovTimer.setEnable(false);
			mouseExitHideDocForMouseMovTimer.resetTimerCount();
		}
		if(window.scriptEditPanel != null){
			window.scriptEditPanel.autoCodeTip.setEnable(false);
		}
	}
	
	private final void initRequire(final DefaultMutableTreeNode jarFolder){
		final Enumeration enumeration = jarFolder.children();
		while(enumeration.hasMoreElements()){
			final DefaultMutableTreeNode node = (DefaultMutableTreeNode)enumeration.nextElement();
			loadLibToCodeHelper(node);
		}
	}
	
	static final Parser rubyParser = new Parser();
	static final ParserConfiguration config = new ParserConfiguration(0, CompatVersion.getVersionFromString(HCJRubyEngine.JRUBY_PARSE_VERSION));
	static final Node emptyStringNode = parseScripts("");
	
    public static Node parseScripts(final String cmds) {
        final StringReader in = new StringReader(cmds);
    	return rubyParser.parse("<code>", in, config);
    }
    
    private static void setPosition(final Node node, final int startLine){
    	setPositionDeep(node, startLine);
    	node.setParent(null);
    }
    
    private static void setPositionDeep(final Node node, final int startLine){
    	node.setPosition(new SourcePosition("", startLine, startLine));
    	
    	final List<Node> childs = node.childNodes();
    	final int size = childs.size();
    	for (int i = 0; i < size; i++) {
    		setPositionDeep(childs.get(i), startLine);
		}
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
    
    public final static JRubyClassDesc findParaClass(final CodeContext contextNode, final String parameter, int typeParameter){
    	if((typeParameter & VAR_LOCAL) == VAR_LOCAL){
    		if(parameter.length() > 0){
    			final char firstChar = parameter.charAt(0);
				if(firstChar >= 'A' && firstChar <= 'Z'){
	    			typeParameter ^= VAR_LOCAL;
	    			typeParameter |= VAR_CONST;
	    		}
    		}
    	}
    	
    	return findParaClassImpl(contextNode, parameter, typeParameter);
    }
    
    /**
     * 没有找到，返回null
     * @param rootNode
     * @param parameter
     * @return
     */
    private final static JRubyClassDesc findParaClassImpl(final CodeContext contextNode, final String parameter, final int typeParameter){
    	final Node subNode = contextNode.bottomNode;//searchParaCallingNodeByIdx(contextNode.contextNode, contextNode.scriptIdx);
    	
    	if(subNode != null){
    		final Vector<ClassNode> defClassNode = contextNode.getDefClassNode();
			final Node varDefNode = findVarDefNode(subNode, parameter, typeParameter, null, false, defClassNode != null, contextNode);
    		if(varDefNode != null){
    			final NodeType nodeType = varDefNode.getNodeType();
    			if(varDefNode instanceof AssignableNode){
    				if(varDefNode instanceof MultipleAsgnNode){
    					final MultipleAsgnNode multiNode = (MultipleAsgnNode)varDefNode;
						final ListNode valueList = (ListNode)multiNode.getValue();
						final JRubyClassDesc result = findVarDefFromMultiAsgn(multiNode, valueList, contextNode, parameter, typeParameter);
						if(result != null){
							return result;
						}
    				}
    				final JRubyClassDesc jcd = checkInRescueException(varDefNode);
    				if(jcd != null){
    					return jcd;
    				}
    				final Node value = ((AssignableNode)varDefNode).getValue();
    				if(varDefNode instanceof ConstDeclNode && value instanceof CallNode){//(ConstDeclNode:JSlider, (CallNode:JSlider, (CallNode:swing, (VCallNode:javax), (ListNode)), (ListNode)))
						final String className = buildFullClassNameFromReceiverNode((CallNode)value);
						final ReturnType result = findClassByName(className, true);
						if(result != null){
							return buildJRubyClassDesc(result, false);
						}
    				}
    				if(value != null){
    					return findClassFromRightAssign(value, contextNode);
    				}else{
    					//addActionListener{|e|}
    					//(CallNode:addChangeListener, (LocalVarNode:slider), (ListNode), (IterNode, (DAsgnNode:e), (BlockNode, ...))), 
    					if(varDefNode instanceof DAsgnNode){
    						final Node iterNode = varDefNode.getParent();
    						if(iterNode instanceof IterNode){
    							final Node callNode = iterNode.getParent();
    							if(callNode instanceof CallNode){
    								final CallNode cNode = (CallNode)callNode;
    								final int rowIdx = cNode.getPosition().getStartLine();
    								final CodeContext callerCodeContext = buildNewCodeContext(contextNode, rowIdx);
									final JRubyClassDesc objClass = findClassFromReceiverNode(cNode.getReceiver(), false, callerCodeContext);
    								if(objClass != null){
    									return findListenerMethodParameterTypeForJava(objClass.getReturnType(), cNode.getName());
    								}
    							}
    						}
    					}
    				}
    				return null;
    			}else if(nodeType == NodeType.FCALLNODE){
    				final ListNode importArgs = (ListNode)((FCallNode)varDefNode).getArgs();//注意：org.jrubyparser.ast.ArrayNode extends org.jrubyparser.ast.ListNode
					final CallNode firstPackageNode = (CallNode)importArgs.childNodes().get(0);
					final String className = buildFullClassNameFromReceiverNode(firstPackageNode);
					return buildJRubyClassDesc(findClassByName(className, true), false);
    			}else if(nodeType == NodeType.CLASSNODE){
    				final JRubyClassDesc classDesc = buildJRubyClassDesc(getDefSuperClass((ClassNode)varDefNode, contextNode), false);
//					classDesc.addClassDefNode((ClassNode)varDefNode);
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
    			}else{
    				return findClassFromRightAssign(varDefNode, contextNode);
    			}
    		}
    	}
    	
    	return null;
    }

	private static JRubyClassDesc findVarDefFromMultiAsgn(final MultipleAsgnNode multiNode,
			final ListNode valueList, final CodeContext contextNode, final String parameter,
			final int typeParameter) {
		final ListNode array = multiNode.getPre();
		final int size = array.size();
		for (int i = 0; i < size; i++) {
			final AssignableNode sub = (AssignableNode)array.get(i);
			if(sub instanceof MultipleAsgnNode){
				final JRubyClassDesc result = findVarDefFromMultiAsgn((MultipleAsgnNode)sub, (ListNode)valueList.get(i), contextNode, parameter, typeParameter);
				if(result != null){
					return result;
				}
			}
			final String name = getAsgnNodeVarName(sub, typeParameter);
			if(name != null && name.equals(parameter)){
				if(valueList != null){
					return findClassFromRightAssign(valueList.get(i), contextNode);
				}else{
					return null;//addActionListener{|e|}
				}
			}
		}
		final Node rest = multiNode.getRest();//a,*b=1,2,3 # a=1, b=[2,3]
		if(rest != null && rest instanceof AssignableNode){
			final String name = getAsgnNodeVarName((AssignableNode)rest, typeParameter);
			if(name != null && name.equals(parameter)){
				return buildJRubyClassDesc(RubyHelper.JRUBY_ARRAY_CLASS, true);
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
					return desc.getClassForSuper();
				}
	    	}else if(callNode instanceof IterNode){
	    		final JRubyClassDesc desc = searchIterNode(callNode, contextNode);
	    		if(desc != null){
					return desc.getClassForSuper();
				}
	    	}
    	}catch (final Throwable e) {
		}
    	
    	return null;
    }

	private final static JRubyClassDesc findParaClassFromArgu(final Node varDefNode,
			final Class superClass, final Vector<ClassNode> defClassNodes, final String parameter, final CodeContext codeContext) {
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
		
		if(parameterIdx >= 0 && defClassNodes != null){
			final int size = defClassNodes.size();
			for (int i = 0; i < size; i++) {
				final ClassNode defClassNode = defClassNodes.elementAt(i);
				final JRubyClassDesc jcdOut = findParaClassFromArguFromDefClassNode(superClass, codeContext, methodName, parameterIdx, defClassNode);
				if(jcdOut != null){
					return jcdOut;
				}
			}
		}else if(parameterIdx >= 0 && superClass != null){
			final JRubyClassDesc jcdOut = findParaClassFromArguFromDefClassNode(superClass, codeContext, methodName, parameterIdx, null);
			if(jcdOut != null){
				return jcdOut;
			}
		}
		
		return null;
	}

	private static JRubyClassDesc findParaClassFromArguFromDefClassNode(Class superClass,
			final CodeContext codeContext, final String methodName, final int parameterIdx,
			final ClassNode defClassNode) {
		if(superClass == null){
			superClass = getDefSuperClass(defClassNode, codeContext).getRawClass();
		}
		{
			final Class methodParaType = searchOverrideMethodParameterType(superClass, methodName, parameterIdx);
			if(methodParaType != null){
				return buildJRubyClassDesc(methodParaType, true);
			}
		}
		
		if(defClassNode != null){
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
    
    public final static int VAR_LOCAL = 1 << 1;
    public final static int VAR_GLOBAL = 1 << 2;
    public final static int VAR_INSTANCE = 1 << 3;
    public final static int VAR_CLASS = 1 << 4;
    public final static int VAR_CONST = 1 << 5;
    public final static int VAR_UNKNOW = VAR_LOCAL | VAR_GLOBAL | VAR_INSTANCE | VAR_CLASS | VAR_CONST;
    
    private static boolean isInnerClassDefForNew(final Node node){
    	return node.getNodeType() == NodeType.CALLNODE && JRUBY_NEW.equals(((CallNode)node).getName()) &&  isInnerClassDef(((CallNode)node).getReceiver());
    }
    
    //abc = Class.new(base){}.new
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
    
    private static Node searchValueNodeFromAssignableNode(Node node){
    	do{
    		node = ((AssignableNode)node).getValue();
    	}while(node instanceof AssignableNode);
    	return node;
	}
    
    private static Node isAssignableNode(final AssignableNode varDefNode, final String parameter, final int typeParameter){
    	if(varDefNode instanceof MultipleAsgnNode){//a, b, c = 1, 2, 3
			final MultipleAsgnNode multi = (MultipleAsgnNode)varDefNode;
			final Node result = isAsgnFromMultiAsgn(multi, parameter, typeParameter);
			if(result != null){
				return result;
			}
    	}
    	
    	final String searchParaName = getAsgnNodeVarName(varDefNode, typeParameter);
		if(searchParaName == null){
			return null;
		}
		
		if(searchParaName.equals(parameter)){
			return varDefNode;
		}
		
		final Node nestAsgnNode = varDefNode.getValue();
		if(nestAsgnNode != null && nestAsgnNode instanceof AssignableNode){
			return isAssignableNode((AssignableNode)nestAsgnNode, parameter, typeParameter);
		}
		return null;
    }

	private static Node isAsgnFromMultiAsgn(final MultipleAsgnNode multi, final String parameter, final int typeParameter) {
		final ListNode array = multi.getPre();
		final int size = array.size();
		for (int i = 0; i < size; i++) {
			final AssignableNode sub = (AssignableNode)array.get(i);
			if(sub instanceof MultipleAsgnNode){
				final Node result = isAsgnFromMultiAsgn((MultipleAsgnNode)sub, parameter, typeParameter);
				if(result != null){
					return multi;
				}
			}
			final String name = getAsgnNodeVarName(sub, typeParameter);
			if(name != null && name.equals(parameter)){
				return multi;
			}
		}
		final Node rest = multi.getRest();
		if(rest != null && rest instanceof AssignableNode){
			final String name = getAsgnNodeVarName((AssignableNode)rest, typeParameter);
			if(name != null && name.equals(parameter)){
				return multi;
			}
		}
		return null;
	}
    
	private static String getVarNodeName(final Node varDefNode, final int typeParameter) {
		String searchParaName;
		if((typeParameter & VAR_LOCAL) != 0 && varDefNode instanceof LocalVarNode){
			searchParaName = ((LocalVarNode)varDefNode).getName();
//		}else if((typeParameter & VAR_LOCAL) != 0 && varDefNode instanceof DAsgnNode){//{|e|
//			searchParaName = ((DAsgnNode)varDefNode).getName();
		}else if((typeParameter & VAR_INSTANCE) != 0 && varDefNode instanceof InstVarNode){
			searchParaName = ((InstVarNode)varDefNode).getName();
		}else if((typeParameter & VAR_GLOBAL) != 0 && varDefNode instanceof GlobalVarNode){
			searchParaName = ((GlobalVarNode)varDefNode).getName();
//		}else if((typeParameter & VAR_CLASS) != 0 && varDefNode instanceof ClassVarDeclNode){
//			searchParaName = ((ClassVarDeclNode)varDefNode).getName();
//		}else if(varDefNode instanceof ConstDeclNode){
//			searchParaName = ((ConstDeclNode)varDefNode).getName();
		}else{
			return null;
		}
		return searchParaName;
	}

	private static String getAsgnNodeVarName(final AssignableNode varDefNode, final int typeParameter) {
		String searchParaName;
		if((typeParameter & VAR_LOCAL) != 0 && varDefNode instanceof LocalAsgnNode){
			searchParaName = ((LocalAsgnNode)varDefNode).getName();
		}else if((typeParameter & VAR_LOCAL) != 0 && varDefNode instanceof DAsgnNode){//{|e|
			searchParaName = ((DAsgnNode)varDefNode).getName();
		}else if((typeParameter & VAR_INSTANCE) != 0 && varDefNode instanceof InstAsgnNode){
			searchParaName = ((InstAsgnNode)varDefNode).getName();
		}else if((typeParameter & VAR_GLOBAL) != 0 && varDefNode instanceof GlobalAsgnNode){
			searchParaName = ((GlobalAsgnNode)varDefNode).getName();
		}else if((typeParameter & VAR_CLASS) != 0 && varDefNode instanceof ClassVarDeclNode){
			searchParaName = ((ClassVarDeclNode)varDefNode).getName();
		}else if(varDefNode instanceof ConstDeclNode){
			searchParaName = ((ConstDeclNode)varDefNode).getName();
		}else{
			return null;
		}
		return searchParaName;
	}
    
    private static Node traversConditionNode(final Node node, final String parameter, final int typeParameter){
    	final List<Node> list = node.childNodes();
    	final int size = list.size();
    	for (int i = 0; i < size; i++) {
			final Node sub = list.get(i);
			if(sub instanceof AssignableNode){
				final Node out = isAssignableNode((AssignableNode)sub, parameter, typeParameter);
				if(out != null){
					return out;
				}
			}
			final Node out = traversConditionNode(sub, parameter, typeParameter);
			if(out != null){
				return out;
			}
		}
    	return null;
    }
    
    private static final Node searchMethodParameterFromList(final String paraName, final ListNode list) {
		if(list == null){
			return null;
		}
		
		final int size = list.size();
		for(int i = 0; i<size; i++){
			try{
				final Node node = list.get(i);
				if(node instanceof ArgumentNode){
					final ArgumentNode parameterNode = (ArgumentNode)node;
					if(parameterNode.getName().equals(paraName)){
						return parameterNode;
					}
				}else if(node instanceof OptArgNode){
					final OptArgNode parameterNode = (OptArgNode)node;
					if(parameterNode.getName().equals(paraName)){
						return parameterNode;
					}
				}else{
					continue;
				}
			}catch (final Throwable e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}
    
    private static Node findVarDefNode(final Node bottomNode, String parameter, int typeParameter, 
    		final Node fromNode, final boolean downForward, final boolean isInDefClass, final CodeContext codeContext){
    	if(bottomNode == null){
    		return null;
    	}
    	final NodeType nodeType = bottomNode.getNodeType();
    	
    	if((typeParameter & VAR_LOCAL) != 0 && nodeType == NodeType.DEFNNODE){//方法定义体
    		final ArgsNode argsNode = ((MethodDefNode)bottomNode).getArgs();
    		
    		Node result = null;
    		result = searchMethodParameterFromList(parameter, argsNode.getPre());
    		if(result != null){
    			return bottomNode;//注意：是java需要的方法，而不是Optional的可能类型
    		}
    		result = searchMethodParameterFromList(parameter, argsNode.getPost());
    		if(result != null){
    			return bottomNode;
    		}
    		final ArgumentNode restArgu = argsNode.getRest();//实际为RestArgNode
    		if(restArgu != null && restArgu.getName().equals(parameter)){
				return restArgu;
			}
    		final BlockArgNode blockArgu = argsNode.getBlock();
    		if(blockArgu != null && blockArgu.getName().equals(parameter)){
    			return blockArgu;
    		}
    		result = searchMethodParameterFromList(parameter, argsNode.getOptional());
    		if(result != null){
    			return ((OptArgNode)result).getValue();
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
			//(DefsNode:each, (MethodNameNode:each), (ArgsNode), (BlockNode, (NewlineNode, (FCallNode:puts, (ArrayNode, (YieldNode)))), (NewlineNode, (FCallNode:puts, (ArrayNode, (YieldNode, (FixnumNode))))), (NewlineNode, (FCallNode:puts, (ArrayNode, (YieldNode, (ArrayNode, (FixnumNode), (FixnumNode)))))), (NewlineNode, (FixnumNode))), (LocalVarNode:o))
			if(varDefNode instanceof AssignableNode){
				final Node out = isAssignableNode((AssignableNode)varDefNode, parameter, typeParameter);
				if(out != null){
					return out;
				}else{
					continue;
				}
			}
			
			final NodeType subType = varDefNode.getNodeType();
			if(subType == NodeType.IFNODE || subType == NodeType.WHILENODE 
    			|| subType == NodeType.UNTILNODE){//unless ==> if
	    		final Node out = traversConditionNode(varDefNode, parameter, typeParameter);
	    		if(out != null){
	    			return out;
	    		}
    		}else if(subType == NodeType.DEFSNODE){
				final Node receiveNode = ((DefsNode)varDefNode).getReceiver();
				if(parameter.equals(getVarNodeName(receiveNode, typeParameter))){
					codeContext.appendDefMethods.add((DefsNode)varDefNode);
				}
				//parameter.equals(getAsgnNodeVarName(varDefNode, typeParameter)
				continue;
    		}else if(subType == NodeType.SCLASSNODE){//class << animal\n def
    			final SClassNode sclassNode = (SClassNode)varDefNode;
    			if(parameter.equals(getVarNodeName(sclassNode.getReceiver(), typeParameter))){
    				final List<MethodDefNode> methods = sclassNode.getMethodDefs();
    				for (int j = 0; j < methods.size(); j++) {
    					codeContext.appendDefMethods.add(methods.get(j));
					}
				}
    			continue;
			}else if((typeParameter & VAR_CONST) != 0 && (subType == NodeType.CLASSNODE)){
				//class MyMlet < Java::hc.server.ui.Mlet\n end\n
				//(ClassNode, (Colon2ImplicitNode:MyMlet), (CallNode:Mlet, (CallNode:ui, (CallNode:server, (CallNode:hc, (ConstNode:Java), 
				final ClassNode cNode = (ClassNode)varDefNode;
				if(getDefClassName(cNode).equals(parameter)){
					return varDefNode;
				}
			}else if(subType == NodeType.ALIASNODE || subType == NodeType.VALIASNODE){
				if(subType == NodeType.ALIASNODE){
					final AliasNode aliasNode = (AliasNode)varDefNode;
					final String newName = aliasNode.getNewNameString();
					if(newName.equals(parameter)){
						parameter = aliasNode.getOldNameString();
						continue;
					}
				}else if(subType == NodeType.VALIASNODE){
					final VAliasNode aliasNode = (VAliasNode)varDefNode;
					final String newName = aliasNode.getNewName();//返回如：$ctx
					if(matchCodeParameter(newName, parameter, typeParameter)){
						parameter = aliasNode.getOldName();
				    	if(parameter.startsWith(PRE_CLASS_VARIABLE_STR, 0)){
				    		typeParameter = VAR_CLASS;
				    		parameter = parameter.substring(PRE_CLASS_VARIABLE_STR.length());
				    	}else if(parameter.startsWith(PRE_GLOBAL_DALLOR_STR, 0)){
				    		typeParameter = VAR_GLOBAL;
				    		parameter = parameter.substring(PRE_GLOBAL_DALLOR_STR.length());
				    	}else if(parameter.startsWith(PRE_CLASS_INSTANCE_STR, 0)){
				    		typeParameter = VAR_INSTANCE;
				    		parameter = parameter.substring(PRE_CLASS_INSTANCE_STR.length());
				    	}else{
				    		typeParameter = VAR_UNKNOW;
				    	}
						continue;
					}
				}
			}else if(subType == NodeType.FCALLNODE){
				if((typeParameter & VAR_CONST) != 0){
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
			}else if(isInDefClass && ((typeParameter & VAR_INSTANCE) != 0 || (typeParameter & VAR_GLOBAL) != 0) && (subType == NodeType.CLASSNODE)){
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
    		final Node parent = bottomNode.getParent();
    		if(parent == null){
    			return null;
    		}
    		if((typeParameter & VAR_LOCAL) != 0){
	    		final NodeType parentNodeType = parent.getNodeType();
	        	if((parentNodeType == NodeType.CLASSNODE || parentNodeType == NodeType.SCLASSNODE|| isInnerClassDef(parent))){
	        		if(bottomNode.getPosition().getStartLine() == parent.getPosition().getStartLine()){
	        			//class << animal {}, animal is localvarNode
	        		}else{
		        		typeParameter ^= VAR_LOCAL;
		        		if(typeParameter == 0){
		        			return null;
		        		}
	        		}
	        	}
    		}
    		
			final Node out = findVarDefNode(parent, parameter, typeParameter, bottomNode, downForward, isInDefClass, codeContext);
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
    
    private static final boolean matchCodeParameter(final String newName, final String parameter, final int typeParameter){
    	if(newName.endsWith(parameter)){
    		final int lenNew = newName.length();
    		final int lenOld = parameter.length();
    		if(lenNew == lenOld){
    			return true;
    		}
    		final int preFixLen = lenNew - lenOld;
    		final String preFix = newName.substring(0, preFixLen);
    		if(preFixLen == 2){
    			if(preFix.equals(PRE_CLASS_VARIABLE_STR) && (typeParameter & VAR_CLASS) != 0){
    				return true;
    			}
    		}else if(preFixLen == 1){
    			if(preFix.equals(PRE_GLOBAL_DALLOR_STR) && (typeParameter & VAR_GLOBAL) != 0){
    				return true;
    			}else if(preFix.equals(PRE_CLASS_INSTANCE_STR) && (typeParameter & VAR_INSTANCE) != 0){
    				return true;
    			}
    		}
    	}
    	return false;
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
			Node varDefNode = list.get(i);
			if(varDefNode instanceof AssignableNode){
				do{
					String searchParaName;
					if((typeParameter & VAR_INSTANCE) != 0 && varDefNode instanceof InstAsgnNode){
						searchParaName = ((InstAsgnNode)varDefNode).getName();
					}else if((typeParameter & VAR_GLOBAL) != 0 && varDefNode instanceof GlobalAsgnNode){
						searchParaName = ((GlobalAsgnNode)varDefNode).getName();
					}else{
						break;
					}
					
					if(searchParaName.equals(parameter)){
						return varDefNode;
					}
					varDefNode = ((AssignableNode) varDefNode).getValue();
				}while(varDefNode != null && varDefNode instanceof AssignableNode);
				continue;
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
    
    private static final boolean isAttrAssistant(final Node node){
		final FCallNode callNode = (FCallNode)node;
		final String callName = callNode.getName();
		if(callName.equals("attr_reader") || callName.equals("attr_accessor") || callName.equals("attr_writer")){
			return true;
		}
		return false;
    }
    
    private static final void appendVarDefInInitializeMethod(final Node methodBody, final int typeParameter, final ArrayList<CodeItem> out,
    		final CodeContext codeContext, final boolean isInInitMethod){
    	if(methodBody == null){
    		return;
    	}
    	final List<Node> list = methodBody.childNodes();
    	final int size = list.size();
    	for (int i = 0; i < size; i++) {
			Node varDefNode = list.get(i);
			if(isInInitMethod && varDefNode.getPosition().getStartLine() > codeContext.rowIdx){
				return;
			}
			
			if(varDefNode instanceof NewlineNode){
				varDefNode = varDefNode.childNodes().get(0);
			}
			
			if(varDefNode instanceof MethodDefNode){
				continue;
			}else if((typeParameter & VAR_INSTANCE) != 0 && varDefNode instanceof FCallNode 
					&& isAttrAssistant(varDefNode)){//(FCallNode:attr_reader, (ArrayNode, (SymbolNode:field1), (SymbolNode:field2)))
				final ArrayNode arrayNode = (ArrayNode)((FCallNode)varDefNode).getArgs();
				final int arrSize = arrayNode.size();
				for (int j = 0; j < arrSize; j++) {
					final SymbolNode symNode = (SymbolNode)arrayNode.get(j);
					addFieldForDefClass(out, PRE_CLASS_INSTANCE_STR + symNode.getName(), true);
				}
			}else if(varDefNode instanceof AssignableNode){
				do{
					String searchParaName;
					boolean isPublic = false;
					if((typeParameter & VAR_INSTANCE) != 0 && varDefNode instanceof InstAsgnNode){
						searchParaName = PRE_CLASS_INSTANCE_STR + ((InstAsgnNode)varDefNode).getName();
						isPublic = true;
					}else if((typeParameter & VAR_GLOBAL) != 0 && varDefNode instanceof GlobalAsgnNode){
						searchParaName = PRE_GLOBAL_DALLOR_STR + ((GlobalAsgnNode)varDefNode).getName();
						isPublic = true;
//					}else if((typeParameter & VAR_LOCAL) != 0 && varDefNode instanceof LocalAsgnNode){
//						searchParaName = ((LocalAsgnNode)varDefNode).getName();
					}else if((typeParameter & VAR_CLASS) != 0 && varDefNode instanceof ClassVarDeclNode){//(ClassVarDeclNode:class_variable, (FixnumNode))
						searchParaName = PRE_CLASS_VARIABLE_STR + ((ClassVarDeclNode)varDefNode).getName();
						isPublic = false;
					}else{
						break;
					}
					
					addFieldForDefClass(out, searchParaName, isPublic);
					varDefNode = ((AssignableNode)varDefNode).getValue();
				}while(varDefNode instanceof AssignableNode);//(InstAsgnNode:x, (InstAsgnNode:y, (FixnumNode))) => @x = @y = 1
				continue;
			}else{
				//有可能位于嵌入中
				appendVarDefInInitializeMethod(varDefNode, typeParameter, out, codeContext, isInInitMethod);
			}
		}
    }

	private static void addFieldForDefClass(final ArrayList<CodeItem> out, final String searchParaName, final boolean isPublic) {
		if(CodeItem.contains(out, searchParaName)){
		}else{
			final CodeItem item = CodeItem.getFree();
			
			item.type = CodeItem.TYPE_FIELD;
			item.fieldOrMethodOrClassName = searchParaName;
			item.code = searchParaName;
			item.codeForDoc = item.code;
			item.codeDisplay = searchParaName;
			item.codeLowMatch = searchParaName.toLowerCase();
			item.isPublic = isPublic;
			out.add(item);
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
    		return findParaClass(newCodeCtx, ((LocalVarNode)value).getName(), CodeHelper.VAR_LOCAL);
    	}
    	
    	return findClassFromReceiverNode(value, true, codeContext);
    }
    
    private static final Class[] AUTO_BOX_CLASS1 = {boolean.class, byte.class, char.class, short.class, int.class, long.class, float.class, double.class};
    private static final Class[] AUTO_BOX_CLASS2 = {Boolean.class, Byte.class, Character.class, Short.class, Integer.class, Long.class, Float.class, Double.class};

    private static boolean isAutoBoxClass(final Class class1, final Class class2){
    	final int size = AUTO_BOX_CLASS1.length;
    	for (int i = 0; i < size; i++) {
			final Class dc1 = AUTO_BOX_CLASS1[i];
			final Class dc2 = AUTO_BOX_CLASS2[i];
			if(class1 == dc1 && class2 == dc2 || class1 == dc2 && class2 == dc1){
				return true;
			}
		}
    	
    	return false;
    }
    
    private final static boolean isIDEUtilReceive(final Node node, final CodeContext codeContext){
		final JRubyClassDesc classdesc = findClassFromReceiverNode(node, false, codeContext);
		if(classdesc != null && classdesc.getReturnType().getRawClass() == IDEUtil.class){
			return true;
		}
    	
    	return false;
    }
    
    private final static Node getFinedReceiver(Node receiver){
    	while(receiver instanceof NewlineNode){
    		final NewlineNode newlineNode = (NewlineNode)receiver;
			final SourcePosition position = newlineNode.getPosition();
			if(position.getStartLine() == position.getEndLine()){
    			receiver = newlineNode.getNextNode();
    		}else{
    			break;
    		}
    	}
    	return receiver;
    }

	private final static JRubyClassDesc findClassFromCallNode(final CallNode call, final CodeContext codeContext) {
		final String methodName = call.getName();
		
		//可能 
		//1.以属性访问的方式 
		//2.方法
		//来创建实例
		final Node receiver = getFinedReceiver(call.getReceiver());
		
		if(methodName.equals(JRUBY_NEW)){
			final JRubyClassDesc classDesc = findClassFromReceiverNode(receiver, false, codeContext);
			if(classDesc != null){
				classDesc.isInstance = true;
			}
			if(classDesc.isJRubyStruct()){
				classDesc.getReturnType().structInstArrayNode = (ArrayNode)call.getArgs();//(ArrayNode, (StrNode), (StrNode), (FixnumNode))
			}
			return classDesc;
		}else if(methodName.equals("asClass") && isIDEUtilReceive(receiver, codeContext)){
			//(CallNode:asClass, (ConstNode:IDEUtil), (ArrayNode, (LocalVarNode:ctrler), (ConstNode:CtrlResponse)))
			//(CallNode:asClass, (CallNode:IDEUtil, (CallNode:util, (CallNode:server, (CallNode:hc, (ConstNode:Java), (ListNode)), (ListNode)), (ListNode)), (ListNode)), (ArrayNode, (LocalVarNode:ctrler), (CallNode:CtrlResponse, (CallNode:ui, (CallNode:server, (CallNode:hc, (ConstNode:Java), (ListNode)), (ListNode)), (ListNode)), (ListNode))))
			final Node parametersNode = call.getArgs();
			if(parametersNode instanceof ArrayNode){
				final Node classNode = ((ArrayNode)parametersNode).get(1);
				final JRubyClassDesc classDesc = findClassFromReceiverNode(classNode, false, codeContext);
				if(classDesc != null){
					classDesc.isInstance = true;
					return classDesc;
				}
			}
			return null;
		}else if(methodName.equals(ARRAY_FLAG)){
			final Node receive = receiver;
			final JRubyClassDesc receiverClass = findClassFromRightAssign(receive, codeContext);
			final ReturnType type = new ReturnType(deepArray(receiverClass.getReturnType().getType(), 1));
			return buildJRubyClassDesc(type, receiverClass.isInstance);
		}else if(methodName.equals(TO_S)){
			return buildJRubyClassDesc(RubyHelper.JRUBY_STRING_CLASS, true);
		}else if(methodName.equals(TO_I)){
			return buildJRubyClassDesc(int.class, true);
		}else if(methodName.equals(TO_F)){
			return buildJRubyClassDesc(float.class, true);
		}else if(methodName.equals(TO_A)){
			final JRubyClassDesc jcd = findClassFromRightAssign(receiver, codeContext);
			final Type[] typeVar = {deepArray(jcd.getReturnType().getType(), 1)};//去掉一个维度
			final HCParameterizedType hcpt = new HCParameterizedType(typeVar, RubyHelper.JRUBY_ARRAY_CLASS, null);
			return buildJRubyClassDesc(new ReturnType(hcpt), jcd.isInstance);
		}else if(methodName.equals(CLASS_STATIC)){//(CallNode:class, (LocalVarNode:ctrl), (ListNode))
			final JRubyClassDesc classDesc = findClassFromRightAssign(receiver, codeContext);
			return classDesc;
		}else if(methodName.equals(REGEXP_MATCH) && receiver != null && receiver instanceof RegexpNode){
			return buildJRubyClassDesc(RubyHelper.JRUBY_MATCHDATA_CLASS, true);
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
				return findClassFromRightAssign(receiver, codeContext);
			}
			
			final Class[] paraClass = findClasssFromArgs((ListNode)parametersNode, codeContext);//注意：org.jrubyparser.ast.ArrayNode extends org.jrubyparser.ast.ListNode

			return findMethodReturnByCallNode(receiver, methodName, paraClass, codeContext);
		}else if(parametersNode instanceof ListNode){//属性
			final String className = buildFullClassNameFromReceiverNode(call);//jop = Java::javax.swing.JOptionPane
			final ReturnType classOut = findClassByName(className, false);
			if(classOut != null){
				return buildJRubyClassDesc(classOut, false);
			}
			
			final JRubyClassDesc out = findClassFromReceiverNode(receiver, false, codeContext);
			if(out == null){
				return null;
			}
			final ReturnType returnType = out.getReturnType();
			
			if(out.isJRubyStruct()){
				//(CallNode:name, (LocalVarNode:joe), (ListNode)) => joe.name (joe : Struct.new)
				try{
					final ArrayNode defArray = returnType.structDefArrayNode;
					final String fieldName = call.getName();
					for (int i = 0; i < defArray.size(); i++) {
						final SymbolNode defNode = (SymbolNode)defArray.get(i);
						if(defNode.getName().equals(fieldName)){
							final ArrayNode insArray = returnType.structInstArrayNode;
							if(insArray == null || insArray.size() < i + 1){
								return objectJCD;//Person.new('Dave', 'Texas'), but Person = Struct.new(:name, :address, :likes)
							}
							return findClassFromReceiverNode(insArray.get(i), false, codeContext);
						}
					}
				}catch (final Throwable e) {
					e.printStackTrace();
				}
			}
			
			if(methodName.equals(JAVA_ARRAY_LENGTH) && returnType.isGenericArrayType()){
				return buildJRubyClassDesc(int.class, true);
			}
			if(returnType.getType() == RubyHelper.JRUBY_ARRAY_CLASS){
				if(methodName.equals(F_ARRAY_COUNT) || methodName.equals(F_ARRAY_LENGTH)){
					return buildJRubyClassDesc(int.class, true);
				}else if(methodName.equals(F_ARRAY_EMPTY)){
					return buildJRubyClassDesc(boolean.class, true);
				}
			}
		
			try {
				final Field field = returnType.getField(methodName);
				return buildJRubyClassDesc(new ReturnType(field.getGenericType()), true);
			} catch (final Exception e) {
			}
			
			//仿属性的方法，即隐式方法调用
			return findMethodReturnByCallNode(receiver, methodName, NULL_PARAMETER, codeContext);
		}
		
		return null;
	}
	
	private final static JRubyClassDesc findMethodReturnByCallNode(final Node receiveNode, final String methodName, final Class[] paraClass, final CodeContext codeContext) {
		JRubyClassDesc receiverClass = findClassFromReceiverNode(receiveNode, false, codeContext);
		final ReturnType returnType = receiverClass.getReturnType();
		
		final RubyClassAndDoc rcd = RubyHelper.searchRubyClass(returnType.getRawClass());
		if(rcd != null){
			//Ruby
			return searchMethodForRuby(rcd, methodName, receiverClass.isInstance);
		}else{
			//Java
			receiverClass = findMethodReturnForJava(returnType, methodName, paraClass);
			return receiverClass;
		}
	}
	
	private static JRubyClassDesc searchMethodForRuby(RubyClassAndDoc rcd, final String methodName, final boolean isInstance){
			//Ruby
		final RubyMethodItem rmi;
		if(isInstance){
			rmi = rcd.searchInstanceMethod(methodName);
		}else{
			rmi = rcd.searchStaticMethod(methodName);
		}
		if(rmi != null){
			return buildJRubyClassDesc(rmi.returnType.claz, true);
		}else{
			final Class superclass = rcd.claz.getSuperclass();
			if(superclass != null){
				rcd = RubyHelper.searchRubyClass(superclass);
				if(rcd != null){
					return searchMethodForRuby(rcd, methodName, isInstance);
				}
			}
		}
		return null;
	}

	/**
	 * 获得e的类型，从addActionListener(e)
	 * @param baseClass
	 * @param methodName
	 * @return
	 */
	private static JRubyClassDesc findListenerMethodParameterTypeForJava(final ReturnType baseClass, final String methodName) {
		final Method[] methods = baseClass.getMethods();
		final int methodSize = methods.length;
		for (int i = 0; i < methodSize; i++) {
			final Method tempMethod = methods[i];
			if(tempMethod.getName().equals(methodName)){
				final Type[] parameterTypes = tempMethod.getGenericParameterTypes();
				return buildJRubyClassDesc(new ReturnType(parameterTypes[0]), true);
			}
		}
		
		return null;
	}
	
	private static JRubyClassDesc findMethodReturnForJava(ReturnType baseClass, final String methodName, final Class[] paraClass) {
		Method jmethod = null;
		try{
			jmethod = baseClass.getMethod(methodName, paraClass);
		}catch (final Exception e) {
		}
		
		if(jmethod == null){
			JRubyClassDesc out = findMatchMehtodForReturnViaClassAndInterfaces(methodName, paraClass, baseClass);
			if(out != null){
				return out;
			}else{
				while(true){
					if(baseClass.isEquals(Object.class)){
						return null;
					}
					baseClass = baseClass.getSuperclass();
					out = findMatchMehtodForReturnViaClassAndInterfaces(methodName, paraClass, baseClass);
					if(out != null){
						return out;
					}
				}
			}
		}else{
			return buildJRubyClassDesc(baseClass.getReturnType(jmethod), true);
		}
	}
	
	private static JRubyClassDesc findMatchMehtodForReturnViaClassAndInterfaces(final String methodName, final Class[] paraClass, final ReturnType baseClass){
		JRubyClassDesc out = findMatchMehtodForReturn(baseClass.getDeclaredMethods(), methodName, paraClass, baseClass);
		if(out != null){
			return out;
		}
		
		final Class[] interfaces = baseClass.getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			final Class iface = interfaces[i];
			out = findMatchMehtodForReturn(iface.getDeclaredMethods(), methodName, paraClass, new ReturnType(iface));
			if(out != null){
				return out;
			}
		}
		return null;
	}

	private static JRubyClassDesc findMatchMehtodForReturn(final Method[] methods,
			final String methodName, final Class[] paraClass, final ReturnType invokeClass) {
		if(JRUBY_JAVA_CLASS.equals(methodName)){
			return buildJRubyClassDesc(JRUBY_JAVA_CLASS_AGENT, false);
		}
		
		final int methodSize = methods.length;
		int matchCount = 0;
		Method firstMatch = null;
		Method firstMatchParaNumOnly = null;
		for (int i = 0; i < methodSize; i++) {
			final Method tempMethod = methods[i];
			if(tempMethod.getName().equals(methodName)){
				final Class[] parameterTypes = tempMethod.getParameterTypes();
				if(paraClass.length == parameterTypes.length){
					if(firstMatchParaNumOnly == null){
						firstMatchParaNumOnly = tempMethod;
					}
					
					boolean isMatch = true;
					for (int j = 0; j < parameterTypes.length; j++) {
						final Class ptc = parameterTypes[j];
						final Class iptc = paraClass[j];
						if(iptc == ptc || isAutoBoxClass(ptc, iptc)
								|| ptc.isAssignableFrom(iptc) || iptc.isAssignableFrom(ptc) 
								|| (RubyHelper.j2seClassToRubyClass(ptc) == RubyHelper.j2seClassToRubyClass(iptc))){
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
		
		Method returnMethod = null;
		
		if(matchCount >= 1){
			returnMethod = firstMatch;
		}else if(firstMatchParaNumOnly != null){
			returnMethod = firstMatchParaNumOnly;
		}
		
		if(returnMethod != null){
			return buildJRubyClassDesc(invokeClass.getReturnType(returnMethod), true);
		}
		
		return null;
	}
	
	public final static boolean IS_EXTEND = true;
	
	private final static JRubyClassDesc buildJRubyClassDesc(final Class baseClass, final boolean isInstance){
		return buildJRubyClassDesc(new ReturnType(baseClass), isInstance);
	}
	
	private final static JRubyClassDesc buildJRubyClassDesc(final ReturnType basClass, final boolean isInstance){
		final JRubyClassDesc classDesc = new JRubyClassDesc(basClass);
		classDesc.isInstance = isInstance;
		
		return classDesc;
	}
	
	/**
	 * (RescueBodyNode, (LocalAsgnNode:detail, (GlobalVarNode:!)))
	 * 
	 */ 
	private final static JRubyClassDesc checkInRescueException(final Node node){//(LocalAsgnNode:detail, (GlobalVarNode:!))
		if(node instanceof LocalAsgnNode){
			final Node value = ((LocalAsgnNode)node).getValue();
			if(value != null && value instanceof GlobalVarNode && ((GlobalVarNode)value).getName().equals("!")){
//				final Node parent = node.getParent();
//				if(parent != null && parent instanceof RescueBodyNode){
					return buildJRubyClassDesc(RubyHelper.JRUBY_EXCEPTION_CLASS, true);
//				}
			}
		}
		return null;
	}
	
	private final static JRubyClassDesc preFindClassFromReceiverNode(final Node node, final CodeContext codeContext, 
			final String paraName){
		if(node != null){
			if(node instanceof AssignableNode){
				//rescue => detail ===>  (RescueBodyNode, (LocalAsgnNode:detail, (GlobalVarNode:!)))
				JRubyClassDesc jcd = checkInRescueException(node);
				if(jcd != null){
					return jcd;
				}
				
				jcd = findClassFromReceiverNode(node.childNodes().get(0), false, codeContext);//xxx
				if(jcd.isJRubyStruct()){
					final AssignableNode asgnNode = (AssignableNode)node;
					final CallNode newNode = (CallNode)asgnNode.getValue();
					jcd.getReturnType().structInstArrayNode = (ArrayNode)newNode.getArgs();//(LocalAsgnNode:joe, (CallNode:new, (ConstNode:Customer), (ArrayNode, (StrNode), (StrNode), (FixnumNode))))
				}
				return jcd;
			}else if(node instanceof DefnNode){
				final Class superClass = searchInnerParentClassFromDef(node, codeContext);
				if(superClass != null){
					final JRubyClassDesc cd = findParaClassFromArgu(node, superClass, null, paraName, codeContext);
					if(cd != null){
						return cd;
					}
				}
				
				final JRubyClassDesc cd = findParaClassFromArgu(node, null, codeContext.getDefClassNode(), paraName, codeContext);
				if(cd != null){
					return cd;
				}
			}
		}
		return null;
	}
	
	private final static ReturnType searchSelfParentDefNode(final Node node, final CodeContext codeContext){
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
					return desc.getReturnType();
				}
			}
		}catch (final Throwable e) {
		}
		return null;
	}
    
    private final static JRubyClassDesc findClassFromReceiverNode(Node receiverNode, final boolean isReportError, final CodeContext codeContext){
    	receiverNode = getFinedReceiver(receiverNode);
    	final NodeType nodeType = receiverNode.getNodeType();
    	
    	if(nodeType == NodeType.CALLNODE){
    		final CallNode callNode = (CallNode)receiverNode;
    		if(callNode.getName().equals(JRUBY_NEW)){
    			JRubyClassDesc out;
    			final Node receiver = callNode.getReceiver();
    			if(receiver != null && receiver instanceof ConstNode && ((ConstNode)receiver).getName().equals(JRUBY_CLASS_FOR_NEW)){
    				//Class.new(Java::hc.a.b){}.new
    				out = buildDescFromCallNodeNew(callNode, codeContext);
    			}else{
    				out = findClassFromReceiverNode(receiver, false, codeContext);//直接new
    			}
    			if(out != null && out.isInstance == false){
    				out.isInstance = true;
    			}
    			return out;
    		}else{
    			final JRubyClassDesc out = findClassFromCallNode(callNode, codeContext);
    			
				if(out != null){
					return out;
				}
				final String className = buildFullClassNameFromReceiverNode(callNode);
				return buildJRubyClassDesc(findClassByName(className, isReportError), false);
			}
    	}else if(nodeType == NodeType.FCALLNODE){//在定义内getProjectContext()
    		final ClassNode defClassNode = codeContext.getTopDefClassNode();
    		
    		ReturnType superClass = null;
    		if(defClassNode != null){
    			superClass = getDefSuperClass(defClassNode, codeContext);
    		}
    		if(superClass == null){
    			final JRubyClassDesc defJRubyClassDesc = codeContext.getDefJRubyClassDesc();
    			if(defJRubyClassDesc != null){
    				superClass = defJRubyClassDesc.getReturnType();
    			}
    		}
    		
    		if(superClass != null){
	    		final FCallNode fcallNode = (FCallNode)receiverNode;
	    		final Node parametersNode = fcallNode.getArgs();
	    		
	    		if(parametersNode instanceof ArrayNode){//方法或+-*/
	//    			if(methodName.equals("+") || methodName.equals("-") || methodName.equals("*") || methodName.equals("/")){
	//    				return findClassFromRightAssign(call.getReceiver(), codeContext);
	//    			}
	    			final Class[] paraClass = findClasssFromArgs((ListNode)parametersNode, codeContext);//注意：org.jrubyparser.ast.ArrayNode extends org.jrubyparser.ast.ListNode
    				final JRubyClassDesc jcd = findMethodReturnForJava(superClass, fcallNode.getName(), paraClass);
    				if(jcd != null){
    					return jcd;
    				}
	    		}
    		}
    		
    		//调用Kernel的内置方法，比如puts
    		final RubyClassAndDoc rcd = RubyHelper.searchRubyClass(RubyHelper.JRUBY_KERNEL_CLASS);
    		final RubyMethodItem rmi = rcd.searchInstanceMethod(((FCallNode)receiverNode).getName());
    		if(rmi != null && rmi.returnType != null){
    			return buildJRubyClassDesc(rmi.returnType.claz, true);
    		}
    		
    		return null;
    	}else if(nodeType == NodeType.SELFNODE){
    		final ClassNode defNode = codeContext.getTopDefClassNode();
    		ReturnType baseClass = null;
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
    	}else if(nodeType == NodeType.LOCALVARNODE){
    		final LocalVarNode lvn = (LocalVarNode)receiverNode;
    		final Node upperRowNode = codeContext.getUpperRowNode(receiverNode);//codeContext.bottomNode
    		final Node node = findVarDefNode(upperRowNode, lvn.getName(), VAR_LOCAL, 
    				null, false, codeContext.getDefClassNode() != null, codeContext);
			return preFindClassFromReceiverNode(node, codeContext, lvn.getName());
    	}else if(nodeType == NodeType.VCALLNODE){
    		final VCallNode vcn = (VCallNode)receiverNode;
    		final Node upperRowNode = codeContext.getUpperRowNode(receiverNode);//codeContext.bottomNode
    		final Node node = findVarDefNode(upperRowNode, vcn.getName(), VAR_LOCAL, 
    				null, false, codeContext.getDefClassNode() != null, codeContext);
			return preFindClassFromReceiverNode(node, codeContext, vcn.getName());
    	}else if(nodeType == NodeType.INSTVARNODE){
    		final InstVarNode ivn = (InstVarNode)receiverNode;
    		final Node upperRowNode = codeContext.getUpperRowNode(receiverNode);//codeContext.bottomNode
    		final Node node = findVarDefNode(upperRowNode, ivn.getName(), VAR_INSTANCE, 
    				null, false, codeContext.getDefClassNode() != null, codeContext);
			return preFindClassFromReceiverNode(node, codeContext, ivn.getName());
    	}else if(nodeType == NodeType.GLOBALVARNODE){
    		final GlobalVarNode gvn = (GlobalVarNode)receiverNode;
    		final Node upperRowNode = codeContext.getUpperRowNode(receiverNode);//codeContext.bottomNode
    		final Node node = findVarDefNode(upperRowNode, gvn.getName(), VAR_GLOBAL, 
    				null, false, codeContext.getDefClassNode() != null, codeContext);
			return preFindClassFromReceiverNode(node, codeContext, gvn.getName());
    	}else if(receiverNode instanceof AssignableNode){//@x = @y = 1
			return findClassFromRightAssign(((AssignableNode)receiverNode).getValue(), codeContext);
    	}else if(nodeType == NodeType.CONSTNODE){
    		final String name = ((ConstNode) receiverNode).getName();
			if(name.equals("Java")){//Java::xxxx
    			return null;
    		}
			
			final Node defNode = findVarDefNode(receiverNode, name, VAR_CONST, null, false, false, codeContext);
			if(defNode instanceof ConstDeclNode && ((ConstDeclNode)defNode).getValue() instanceof CallNode){//(ConstDeclNode:JSlider, (CallNode:JSlider, (CallNode:swing, (VCallNode:javax), (ListNode)), (ListNode)))
				final String className = buildFullClassNameFromReceiverNode((CallNode)((ConstDeclNode)defNode).getValue());
				final ReturnType result = findClassByName(className, true);
				if(result != null){
					return buildJRubyClassDesc(result, false);
				}
			}
			if(defNode != null){
				//Customer = Struct.new(:name, :address, :zip)
				//(ConstDeclNode:Customer, (CallNode:new, (ConstNode:Struct), (ArrayNode, (SymbolNode:name), (SymbolNode:address), (SymbolNode:zip))))
				final ArrayNode fields = isStructDefNode(defNode);
				if(fields != null){
					final JRubyClassDesc jcd;
					if(defNode instanceof ClassNode){
						jcd = buildJRubyClassDescForDef(codeContext, (ClassNode)defNode);
					}else{
						jcd = buildJRubyClassDesc(RubyHelper.JRUBY_STRUCT_CLASS, true);
						jcd.getReturnType().structDefArrayNode = fields;
					}
					return jcd;
				}
			}
    		final JRubyClassDesc jcd = findClassFromConstByImportOrDefClass(name, codeContext);
    		if(jcd != null){
    			return jcd;
    		}
    		
			final String[] rubyMap = RubyHelper.JRUBY_STR_MAP;
			final Class[] rubyClassMap = RubyHelper.JRUBY_CLASS_MAP;
			final int rubyLength = rubyMap.length;
			for (int i = 0; i < rubyLength; i++) {
				if(name.equals(rubyMap[i])){
					final Class baseClass = rubyClassMap[i];
					codeContext.codeHelper.window.docHelper.processDoc(codeContext.codeHelper, baseClass, false);
					return buildJRubyClassDesc(baseClass, false);
				}
			}
			
    		return null;
    	}else if(receiverNode instanceof Colon2Node){
    		//(Colon2ConstNode:AA, (ConstNode:TestCodeHelper))
    		final Colon2Node colon2Node = (Colon2Node)receiverNode;
    		final String propertyName = colon2Node.getName();
    		final JRubyClassDesc classDesc = findClassFromReceiverNode(colon2Node.childNodes().get(0), false, codeContext);
    		try {
				final Field field = classDesc.getReturnType().getField(propertyName);
				return buildJRubyClassDesc(new ReturnType(field.getGenericType()), true);
			} catch (final Exception e) {
			}
    	}else if(receiverNode instanceof ILiteralNode){
    		return buildJRubyClassDesc(getClassFromLiteral(receiverNode), true);
    	}else if(nodeType == NodeType.NILNODE){
    		return NIL_CLASS_DESC;
    	}else if(nodeType == NodeType.TRUENODE || nodeType == NodeType.FALSENODE){
    		return BOOLEAN_CLASS_DESC;
    	}else if(nodeType == NodeType.HASHNODE){
    		return buildJRubyClassDesc(RubyHelper.JRUBY_HASH_CLASS, true);
    	}else if(nodeType == NodeType.DOTNODE){//1..10, 0..."cat"
    		return buildJRubyClassDesc(RubyHelper.JRUBY_RANGE_CLASS, true);
    	}else if(nodeType == NodeType.MATCH2NODE){///cat/ =~ "dog and cat"
    		return buildJRubyClassDesc(RubyHelper.JRUBY_FIXNUM_CLASS, true);
    	}else if(nodeType == NodeType.NOTNODE || nodeType == NodeType.ANDNODE || nodeType == NodeType.ORNODE){//false && 99 # => false\n "cat" && 99 # => 99
    		return buildJRubyClassDesc(RubyHelper.JRUBY_TRUE_CLASS, true);
    	}else if(nodeType == NodeType.REGEXPNODE){
    		return buildJRubyClassDesc(RubyHelper.JRUBY_REGEXP_CLASS, true);
    	}else if(nodeType == NodeType.RESTARG){
    		return buildJRubyClassDesc(RubyHelper.JRUBY_ARRAY_CLASS, true);
    	}else if(nodeType == NodeType.BLOCKARGNODE){
    		return buildJRubyClassDesc(RubyHelper.JRUBY_PROC_CLASS, true);
    	}else if(nodeType == NodeType.IFNODE){//含：boolean-expression ? expr1 : expr2
    		final IfNode ifNode = (IfNode)receiverNode;
    		final Node thenNode = ifNode.getThenBody();
    		if(thenNode != null){
    			return findClassFromReceiverNode(thenNode, isReportError, codeContext);
    		}
    		final Node elseNode = ifNode.getElseBody();
    		if(elseNode != null){
    			return findClassFromReceiverNode(elseNode, isReportError, codeContext);
    		}
    	}
    	return null;
    }

	//(ConstDeclNode:Customer, (CallNode:new, (ConstNode:Struct), (ArrayNode, (SymbolNode:name), (SymbolNode:address), (SymbolNode:zip))))
	private static ArrayNode isStructDefNode(final Node defNode){
		try{
			if(defNode instanceof ConstDeclNode){
				final Node newNode = ((ConstDeclNode)defNode).childNodes().get(0);
				return isStructDefNode(newNode);
			}else if(defNode instanceof ClassNode){
				final ClassNode defClass = (ClassNode)defNode;
				return isStructDefNode(defClass.getSuper());
			}else if(defNode instanceof CallNode){
				final CallNode newMethod = (CallNode)defNode;
				final Node arrayNode = newMethod.getArgs();
				final Node structNode = newMethod.childNodes().get(0);
				if(structNode instanceof ConstNode && ((ConstNode)structNode).getName().equals("Struct") && arrayNode instanceof ArrayNode){
					return (ArrayNode)arrayNode;
				}
			}
		}catch (final Throwable e) {
		}
		return null;
	}
	
	private static Vector<String> toVector(final ArrayNode arrayNode){
		//(ArrayNode, (SymbolNode:name), (SymbolNode:address), (SymbolNode:zip))
		try{
			final int size = arrayNode.size();
			final Vector<String> out = new Vector<String>(size);
			for (int i = 0; i < size; i++) {
				final SymbolNode node = (SymbolNode)arrayNode.get(i);
				out.add(node.getName());
			}
			return out;
		}catch (final Throwable e) {
			e.printStackTrace();
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
    private static final JRubyClassDesc BOOLEAN_CLASS_DESC = buildJRubyClassDesc(Boolean.class, true);

	private static ReturnType findClassByName(final String className, final boolean isReportError) {
		try {
			return new ReturnType(getClassLoader().loadClass(className));
		} catch (final ClassNotFoundException e) {
			if(isReportError){
				ExceptionReporter.printStackTrace(e);
			}
		}
		return null;
	}
	
	private static ReturnType getClassFromFCallNode(final FCallNode fcallNode, final CodeContext codeContext){
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

    /**
     * 
     * @param codeContext
     * @param cNode
     * @param superClass null表示未先知
     * @return
     */
	private static JRubyClassDesc buildJRubyClassDescForDef(final CodeContext codeContext, final ClassNode cNode) {
		final JRubyClassDesc classDesc = buildJRubyClassDesc(getDefSuperClass(cNode, codeContext), false);
//		classDesc.getReturnType().addClassDefNode(cNode);
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
					final Class one = getClassFromFCallNode((FCallNode)sub, codeContext).getRawClass();
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
	
	private static ReturnType getClassByNode(final Node node, final CodeContext codeContext){
		if(node == null){
			return null;
		}
		
		final NodeType nodeType = node.getNodeType();
		if(nodeType == NodeType.CONSTNODE){
			return findClassFromReceiverNode(node, false, codeContext).getReturnType();
//			return findClassFromConstByImportOrDefClass(((ConstNode)node).getName(), codeContext).getReturnType();
		}else if(nodeType == NodeType.CALLNODE){
			final String className = buildFullClassNameFromReceiverNode((CallNode)node);
			return findClassByName(className, true);
		}
		
		return null;
	}
	
	private static ReturnType getDefSuperClass(final ClassNode cNode, final CodeContext codeContext) {
		final ReturnType rt = getDefSuperClassImpl(cNode, codeContext);
		if(rt != null){
			rt.addClassDefNode(cNode);
		}
		return rt;
	}

	private static ReturnType getDefSuperClassImpl(final ClassNode cNode, final CodeContext codeContext) {
		final Node superNode = cNode.getSuper();
		if(superNode != null && superNode instanceof ConstNode){
			return findClassFromConstByImportOrDefClass(((ConstNode)superNode).getName(), codeContext).getReturnType();
		}
		
		final CallNode superClass = (CallNode)superNode;
		
		final ArrayNode arrayNode = isStructDefNode(superNode);
		if(arrayNode != null){
			final ReturnType rt = new ReturnType(RubyHelper.JRUBY_STRUCT_CLASS);
			rt.structDefArrayNode = arrayNode;
			return rt;
		}
		
		if(superClass == null){//没有继承
			return new ReturnType(Object.class);
		}else{
			final String className = buildFullClassNameFromReceiverNode(superClass);
			return findClassByName(className, true);
		}
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
				final JRubyClassDesc findClassFromReceiverNode = findClassFromReceiverNode(node, false, context);
				out[i] = findClassFromReceiverNode.getClassForParameter();
			}
    		return out;
    	}
    }

	private static Class getClassFromLiteral(final Node node) {
		if(node instanceof StrNode || node instanceof DStrNode){
			return RubyHelper.JRUBY_STRING_CLASS;//J2SE_STRING_CLASS
		}else if(node instanceof FloatNode){
			return RubyHelper.JRUBY_FLOAT_CLASS;
		}else if(node instanceof BignumNode){
			return RubyHelper.JRUBY_BIGNUM_CLASS;
		}else if(node instanceof FixnumNode){
			return RubyHelper.JRUBY_FIXNUM_CLASS;
		}else if(node instanceof ArrayNode || node instanceof ZArrayNode){//ZArrayNode : []
			return RubyHelper.JRUBY_ARRAY_CLASS;
		}else if(node instanceof RegexpNode){
			return RubyHelper.JRUBY_REGEXP_CLASS;
		}else{
			if(L.isInWorkshop){
				LogManager.errToLog("unknow node type : " + node.toString());
			}
			return Object.class;
		}
	}
	
	private Node backRoot;
	public Node root = emptyStringNode;
	
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
		RubyExector.parse(callCtxNeverCycle, new StringValue(script), null, SimuMobile.getRunTestEngine(), false);
		
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
				final StyledDocument document = sep.jtaStyledDocment;
				
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
			item.codeDisplay = PRE_IMPORT + " {a class of JVM}";
			item.codeLowMatch = PRE_IMPORT.toLowerCase();
			item.type = CodeItem.TYPE_IMPORT;
			
			out.add(item);
			
			item = CodeItem.getFree();
			item.code = PRE_IMPORT_JAVA;
			item.codeForDoc = item.code;
			item.codeDisplay = PRE_IMPORT_JAVA + " {a class of API and third library}";
			item.codeLowMatch = PRE_IMPORT_JAVA.toLowerCase();
			item.type = CodeItem.TYPE_IMPORT;
			
			out.add(item);
		}
		
		{
			final String[] keyStrs = nodeLibClassesAndResMap.getNodeNames();
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
        
        synchronized (classCacheMethodAndPropForProject) {
        	initPreCode(lineChars, lineIdx, rowIdx);
        }
        if(isForcePopup == false && outAndCycle.size() == 0){
			return false;
		}
        if(preCodeSplitIsDot){
        	final int size = outAndCycle.size();
        	for (int i = size - 1; i >= 0; i--) {
				if(outAndCycle.get(i).isForMaoHaoOnly){
					outAndCycle.remove(i);
				}
			}
        }
		if(isForceResTip == false && preCodeType == PRE_TYPE_RESOURCES){
			if(matchRes(outAndCycle, preCode) == false){
				return false;
			}
		}
		
		final Class codeClass = (preClass==null?null:preClass.getClassForDoc());
		
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
        int columnIdx = lineIdx;
        final int lineEndOffset = ScriptEditPanel.getLineEndOffset(doc, rowIdx);
        if(scriptIdx == lineEndOffset || scriptIdx + 1 == lineEndOffset){//鼠标处于行尾时，不处理
        	return false;
        }
        
		boolean isRubyCharInMethod = false;
		char[] lineChars = doc.getText(editLineStartIdx, lineEndOffset - editLineStartIdx).toCharArray();
		{
			int i = lineIdx;
			int charCount = 0;
			for (; i < lineChars.length; i++) {//补全完整的方法名
				final char nextChar = lineChars[i];
				if(nextChar >= 'a' && nextChar <= 'z'
						|| nextChar >= 'A' && nextChar <= 'Z'
						|| nextChar >= '0' && nextChar <= '9'
						|| nextChar == '_'
						){
					charCount++;
				}else{
					if(nextChar == RUBY_METHOD_BOOL_CHAR || nextChar == RUBY_METHOD_MODI_CHAR){
						isRubyCharInMethod = true;
					}
					if(charCount == 0){
						if(isVarChar(lineChars[i - 1])){//只有一个字符，且偏右时
							columnIdx--;
						}
					}
					break;
				}
			}
			if(i > columnIdx){
				final char[] newLineChars = new char[i];
				System.arraycopy(lineChars, 0, newLineChars, 0, i);//去掉方法后段部分，仅保留此之前
				lineChars = newLineChars;
				columnIdx = i;
			}
		}
		
		synchronized (classCacheMethodAndPropForProject) {
			initPreCode(lineChars, columnIdx, rowIdx);
		}
		if(isForcePopup == false && outAndCycle.size() == 0){
			return false;
		}
		final Class codeClass = (preClass==null?null:preClass.getClassForDoc());
		
		int endIdx = lineIdx;
		for (; endIdx < lineChars.length; endIdx++) {//补全完整的方法名
			final char nextChar = lineChars[endIdx];
			if(nextChar >= 'a' && nextChar <= 'z'
					|| nextChar >= 'A' && nextChar <= 'Z'
					|| nextChar >= '0' && nextChar <= '9'
					|| nextChar == '_'
					){
			}else{
				break;
			}
		}
		int startIdx = lineIdx - 1;
		for (; startIdx < lineChars.length && startIdx >= 0; startIdx--) {//补全完整的方法名
			final char nextChar = lineChars[startIdx];
			if(nextChar >= 'a' && nextChar <= 'z'
					|| nextChar >= 'A' && nextChar <= 'Z'
					|| nextChar >= '0' && nextChar <= '9'
					|| nextChar == '_'
					){
			}else{
				break;
			}
		}
		startIdx++;
		
		L.V = L.WShop ? false : LogManager.log("codeClass : " + codeClass + ", preCode : " + preCode);
		
		if(codeClass == null && 
				(preCodeType == PRE_TYPE_AFTER_IMPORT_ONLY
				|| preCodeType == PRE_TYPE_AFTER_IMPORTJAVA
				|| preCodeType == PRE_TYPE_BEFORE_INSTANCE
				|| preCodeType == PRE_TYPE_AFTER_INCLUDE
				|| preCodeType == PRE_TYPE_AFTER_INSTANCE_OR_CLASS
				) && 
				preCode != null && preCode.length() > 0){
			//focus for class define，如落焦在java.lang.Thread或Thread
			final String className = preCode;
			preCode = new String(lineChars, startIdx, endIdx - startIdx);
			if(preCode != null && preCode.length() > 0){
				final int lastDotIdx = className.lastIndexOf('.');
				if(lastDotIdx > 0){
					preCode = className.substring(0, lastDotIdx + 1) + preCode;
				}else{
					//比如Thread，需转为java.lang.Thread
		    		final CodeContext newCodeCtx = new CodeContext(true, codeContext.codeHelper, codeContext.contextNode, rowIdx);
		    		JRubyClassDesc jcd;
		    		if((pre_var_tag_ins_or_global & VAR_INSTANCE) != 0 || (pre_var_tag_ins_or_global & VAR_GLOBAL) != 0){
		    			jcd = findParaClass(newCodeCtx, preCode, pre_var_tag_ins_or_global);
		    		}else{
		    			jcd = findParaClass(newCodeCtx, preCode, VAR_LOCAL);
		    		}
		    		 
		    		if(jcd == null){
		    			final RubyClassAndDoc rcd = RubyHelper.searchRubyClassByShortName(preCode);
		    			if(rcd != null){
		    				jcd = buildJRubyClassDesc(rcd.claz, false);
		    			}
		    		}
		    		if(jcd != null && jcd.getClassForDoc() != null){
		    			preCode = jcd.getClassForDoc().getName();
						final CodeItem item = buildClassItemForDocClass(preCode);
						window.codeInvokeCounter.initCounter(item);
		    			if(outAndCycle.contains(item) == false) {
							outAndCycle.add(item);
						}
		    		}
				}
			}
		}else{
			preCode = new String(lineChars, startIdx, endIdx - startIdx);
		}
		
		if(L.isInWorkshop){
			LogManager.log("[CodeTip] AutoCodeTip : " + preCode + ", codeItem : " + outAndCycle.size());
		}
		if(preCode.length() == 0){
			return false;
		}
		
		CodeWindow.fillForAutoTip(outAndCycle, autoTipOut, isRubyCharInMethod, preCode);
		
		final int matchSize = autoTipOut.size();
		if(matchSize == 0){
			return false;
		}
		
		if(lastMousePreCode != null && lastMousePreCode.equals(preCode)){
			return false;
		}
		lastMousePreCode = preCode;
	
		final Point win_loc = textPane.getLocationOnScreen();
		final Rectangle caretRect = textPane.modelToView(scriptIdx - (lineIdx - startIdx));//与方法段齐
		final int input_x = win_loc.x + caretRect.x;
		final int input_y = win_loc.y + caretRect.y;

		if(matchSize == 1){
			window.setMouseOverAutoTipLoc(input_x, input_y, fontHeight);
			window.startAutoPopTip(autoTipOut.get(0), textPane, sep);
		}else{
			window.toFront(preCodeType, codeClass, sep, textPane, input_x, input_y, autoTipOut, preCode, scriptIdx, fontHeight);
		}
		return true;
	}
	
	public static boolean isVarChar(final char nextChar){
		return nextChar >= 'a' && nextChar <= 'z'
				|| nextChar >= 'A' && nextChar <= 'Z'
				|| nextChar >= '0' && nextChar <= '9'
				|| nextChar == '_';
	}

	public final void inputForCSSInCSSEditor(final int preCodeType, final HCTextPane textPane, final Document cssDocument, final Point caretPosition, 
			final int fontHeight, final int scriptIdx){
		String preCode = null;
		
		try{
			final int line = ScriptEditPanel.getLineOfOffset(cssDocument, scriptIdx);
	        final int editLineStartIdx = ScriptEditPanel.getLineStartOffset(cssDocument, line);
	        final int codeLen = scriptIdx - editLineStartIdx;
	        
			final char[] lineChars = cssDocument.getText(editLineStartIdx, codeLen).toCharArray();
			
			for (int i = codeLen - 1; i >= 0; i--) {
				final char oneChar = lineChars[i];
				if(oneChar == ';' || oneChar == '{' || oneChar == ' ' || oneChar == '\t'){
					if(preCode == null){
						final int off = i + 1;
						preCode = String.valueOf(lineChars, off, codeLen - off);
					}
					if(oneChar == ';' || oneChar == '{'){
						break;
					}
				}
				if(oneChar == ':'){//值区，只能输入变量
					preCode = null;//无效
					break;
				}
			}
			
			if(lineChars.length == 0){//行首时
				preCode = "";
			}
			
			if(preCode != null){
				inputPropertyForCSSInCSSEditor(preCodeType, preCode.trim(), textPane, fontHeight, scriptIdx);
			}else{
				inputVariableForCSSInCSSEditor(preCodeType, textPane, caretPosition, fontHeight, scriptIdx);
			}
		}catch (final Throwable e) {
			e.printStackTrace();
			return;
		}
	}
	
	private final void inputPropertyForCSSInCSSEditor(final int preCodeType, final String preCode, final HCTextPane textPane, final int fontHeight,
			final int scriptIdx){
		try{
			final Point win_loc = textPane.getLocationOnScreen();
			final Rectangle caretRect = textPane.modelToView(scriptIdx - preCode.length());//与方法段齐
			final int input_x = win_loc.x + caretRect.x;
			final int input_y = win_loc.y + caretRect.y;
			
			window.docHelper.getProperties();
			
			clearArray(outAndCycle);
			synchronized (window.docHelper.cssCodeItems) {
				CodeItem.append(outAndCycle, window.docHelper.cssCodeItems);
			}
			
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
			
			item.type = CodeItem.TYPE_CSS_VAR;
			item.code = var;
			item.codeForDoc = item.code;
			item.codeDisplay = var;
			item.codeLowMatch = var.toLowerCase();
			item.fmClass = CodeItem.FM_CLASS_CSS_VAR;
			
			outAndCycle.add(item);
		}
		
		window.codeInvokeCounter.initCounter(outAndCycle);
		
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
	
	private final void getInsOrGloablVarForClass(final Object[] defClassArray, final ArrayList<CodeItem> out){
		if(defClassArray == null){
			return;
		}
		
		for (int i = 0; i < defClassArray.length; i++) {
			getInsOrGloablVarForClassItem((Node)defClassArray[i], out);
		}

		return;
	}
	
	private final void getInsOrGloablVarForClassItem(final Node defClass, final ArrayList<CodeItem> out){
		if(defClass instanceof ClassNode){
	    	final List<Node> list = ((ClassNode)defClass).getBody().childNodes();
	    	final int size = list.size();
	    	for (int i = 0; i < size; i++) {
				Node varDefNode = list.get(i);
				if(varDefNode instanceof NewlineNode){
					varDefNode = ((NewlineNode)varDefNode).getNextNode();
				}
				
				do{
					String searchParaName;
					boolean isInstance = false;
					if(varDefNode instanceof InstAsgnNode && (pre_var_tag_ins_or_global & VAR_INSTANCE) != 0){//@classVar = 100
						searchParaName = ((InstAsgnNode)varDefNode).getName();
						isInstance = true;
					}else if(varDefNode instanceof ConstDeclNode && (pre_var_tag_ins_or_global & VAR_LOCAL) != 0){//(ConstDeclNode:CLASS_CONST, (FixnumNode))
						searchParaName = ((ConstDeclNode)varDefNode).getName();
					}else{
						break;
					}
	
					if(CodeItem.contains(out, searchParaName)){
						continue;
					}
					
					final CodeItem item = CodeItem.getFree();
					
					item.fmClass = CodeInvokeCounter.CLASS_UN_INVOKE_COUNT_DYN_STATIC;
					item.type = CodeItem.TYPE_FIELD;
					item.code = (isInstance?PRE_CLASS_INSTANCE_STR:"") + searchParaName;
					item.fieldOrMethodOrClassName = item.code;
					item.codeForDoc = item.code;
					item.codeDisplay = item.code;
					item.codeLowMatch = item.code.toLowerCase();
					item.isPublic = false;
					out.add(item);
					varDefNode = ((AssignableNode)varDefNode).getValue();
				}while(varDefNode instanceof AssignableNode);
	    	}
//			appendVarDefInInitializeMethod(, pre_var_tag_ins_or_global, out);
		}else if(defClass instanceof IterNode){
			final IterNode iterNode = (IterNode)defClass;
			final Node body = iterNode.getBody();
			if(body != null){
//				final BlockNode blockNode = (BlockNode)body;
//				final int size = blockNode.size();
//				for (int i = 0; i < size; i++) {
//					final NewlineNode newline = (NewlineNode)blockNode.get(i);
//					final DefnNode aMethod = (DefnNode)newline.childNodes().get(0);
//					if(aMethod.getName().equals(JRUBY_CLASS_INITIALIZE_DEF)){
//						appendVarDefInInitializeMethod(aMethod.getBody(), pre_var_tag_ins_or_global, out);
//						break;
//					}
//				}
			}
		}
	}
	
	private final void getInsOrGloablVar(final Object[] defClassArr, final ArrayList<CodeItem> out){
		if(defClassArr == null){
			return;
		}
		
		for (int i = 0; i < defClassArr.length; i++) {
			getInsOrGloablVarItem((Node)defClassArr[i], out);
		}
		
		return;
	}
	
	private final void getInsOrGloablVarItem(final Node defClass, final ArrayList<CodeItem> out){
		if(defClass instanceof ClassNode){
			final List<MethodDefNode> lists = ((ClassNode)defClass).getMethodDefs();
			final int methodSize = lists.size();
			
			//优先从initialize中找
			for (int j = 0; j < methodSize; j++) {
				final MethodDefNode aMethod = lists.get(j);
				if(aMethod.getName().equals(JRUBY_CLASS_INITIALIZE_DEF)){
					final SourcePosition position = aMethod.getPosition();
					final boolean isInInitMethod = position.getStartLine() < codeContext.rowIdx && codeContext.rowIdx < position.getEndLine() ;
					appendVarDefInInitializeMethod(aMethod.getBody(), pre_var_tag_ins_or_global, out, codeContext, isInInitMethod);
//					break;//可能有多个init
				}
			}
			
			if((pre_var_tag_ins_or_global & VAR_CLASS) != 0 || (pre_var_tag_ins_or_global & VAR_INSTANCE) != 0){
				//提取class var
				final Node body = ((ClassNode)defClass).getBody();
				appendVarDefInInitializeMethod(body, pre_var_tag_ins_or_global, out, codeContext, false);
			}
		}else if(defClass instanceof IterNode){
			final IterNode iterNode = (IterNode)defClass;
			final Node body = iterNode.getBody();
			if(body != null && body instanceof BlockNode){//有可能为newline/defnNode，而非BlockNode
				final BlockNode blockNode = (BlockNode)body;
				final int size = blockNode.size();
				for (int i = 0; i < size; i++) {
					final NewlineNode newline = (NewlineNode)blockNode.get(i);
					final DefnNode aMethod = (DefnNode)newline.childNodes().get(0);
					if(aMethod.getName().equals(JRUBY_CLASS_INITIALIZE_DEF)){
						final SourcePosition position = aMethod.getPosition();
						final boolean isInInitMethod = position.getStartLine() < codeContext.rowIdx && codeContext.rowIdx < position.getEndLine() ;
						appendVarDefInInitializeMethod(aMethod.getBody(), pre_var_tag_ins_or_global, out, codeContext, isInInitMethod);
						break;
					}
				}
			}
		}
	}
	
	public final static Type deepArray(final Type type, final int level){
		if(level == 0){
			return type;
		}
		
		if(type instanceof GenericArrayType){
			return deepArray(((GenericArrayType)type).getGenericComponentType(), level - 1);
		}else if(type instanceof Class){
			return buildArrayClass((Class)type, level);
		}else if(type instanceof ParameterizedType){//本处为特例处理
			if(((ParameterizedType) type).getRawType() == RubyHelper.JRUBY_ARRAY_CLASS){
				return ((ParameterizedType) type).getActualTypeArguments()[0];
			}
		}
		
		return type;
	}

	public static Class buildArrayClass(final Class claz, final int level) {
		//[[Ljava.lang.String; => String[][]
		final byte[] bs = claz.getName().getBytes();
		
		Class rawClass = null;
		for (int i = 0; i < bs.length; i++) {
			final byte b = bs[i];
			if(b == '[' || b == 'L'){
				continue;
			}
			try{
				rawClass = claz.forName(new String(bs, i, bs.length - 1 - i));
			}catch (final Exception e) {
				return Object.class;
			}
			break;
		}
		if(rawClass != null){
			for (int i = level; i < bs.length; i++) {
				final byte b = bs[i];
				if(b == '['){
					rawClass = Array.newInstance(rawClass, 0).getClass();
				}else{
					break;
				}
			}
			return rawClass;
		}
		return Object.class;
	}
	
	private final void appendDefInstanceMethods(final ArrayList<CodeItem> out){
		final Vector<MethodDefNode> appendDefMethods = codeContext.appendDefMethods;
		final int size = appendDefMethods.size();
		for (int i = 0; i < size; i++) {
			final MethodDefNode method = appendDefMethods.get(i);
			final String name = method.getName();
			if(CodeItem.containsSameFieldMethodName(out, name) == false) {
				addToDefMethod(out, name);
			}
		}
	}
	
	private static CodeItem addToDefMethod(final ArrayList<CodeItem> list, final String methodName) {
		final CodeItem item = CodeItem.getFree();
		item.fieldOrMethodOrClassName = methodName;
		item.code = methodName;
		item.codeForDoc = item.code;
		item.fmClass = "def";
		item.codeDisplay = item.code;// + " : " + resultType + " - " + baseClassName;
		item.codeLowMatch = item.code.toLowerCase();
		item.isPublic = true;
		item.isForMaoHaoOnly = false;
		item.type = CodeItem.TYPE_METHOD;
		
		list.add(item);
		
		return item;
	}
	
	private final void appendEnumerableMethods(final Type type, final ArrayList<CodeItem> out){
		if(RubyHelper.isEnumerable(type)){
			//The class must provide a method each, which yields successive members of the collection.
			getMethodAndFieldForInstance(false, rubyEnumerableJCD, true, outAndCycle, true, false, false);
		}
	}

	private final void appendStructFields(final JRubyClassDesc jcd, final ArrayList<CodeItem> out){
		if(jcd.isJRubyStruct()){
			try{
				final ReturnType rt = jcd.getReturnType();
				final ArrayNode defArray = rt.structDefArrayNode;
				final ArrayNode insArray = rt.structInstArrayNode;
				final int size = defArray.size();
				for (int i = 0; i < size; i++) {
					final SymbolNode symNode = (SymbolNode)defArray.get(i);
					final Node insNode = insArray.get(i);//StrNode or FixnumNode  ...
					addStructFields(out, symNode.getName(), getClassFromLiteral(insNode).getSimpleName());
				}
			}catch (final Throwable e) {
				e.printStackTrace();
			}
		}
	}
	
	private final void appendAttrAssistantForDefClass(final JRubyClassDesc jcd, final ArrayList<CodeItem> out){
		if(jcd.hasExtChain()){
			final Object[] defClassArr = jcd.getReturnType().toClassDefNodeArray();
			for (int i = 0; i < defClassArr.length; i++) {
				final Node body = ((ClassNode)defClassArr[i]).getBody();
				final List<Node> list = body.childNodes();
		    	final int size = list.size();
		    	for (int j = 0; j < size; j++) {
					Node varDefNode = list.get(j);
					if(varDefNode instanceof NewlineNode){
						varDefNode = ((NewlineNode)varDefNode).getNextNode();
					}
					if(varDefNode instanceof FCallNode && isAttrAssistant(varDefNode)){//(FCallNode:attr_reader, (ArrayNode, (SymbolNode:field1), (SymbolNode:field2)))
						final ArrayNode arrayNode = (ArrayNode)((FCallNode)varDefNode).getArgs();
						final int arrSize = arrayNode.size();
						for (int k = 0; k < arrSize; k++) {
							final SymbolNode symNode = (SymbolNode)arrayNode.get(k);
							addFieldForDefClass(out, symNode.getName(), true);
						}
					}
		    	}
			}
		}
	}
	
	private final void appendObjectForInterfaceOnly(final JRubyClassDesc jcd, final ArrayList<CodeItem> out){
		final Type type = jcd.getReturnType().getType();
		if(type != null && type instanceof Class && ((Class)type).isInterface()){
			final ArrayList<CodeItem> methods = classCacheMethodAndPropForInstance.get(Object.class.getName());
			appendToOut(methods, true, out, false, false);
		}
	}
	
	public final void initPreCode(final char[] lineHeader, final int columnIdx, final int rowIdx) {
		clearArray(outAndCycle);
		preCodeSplitIsDot = false;
		varIsNotValid = false;
		preClass = null;
		backgroundDefClassNode = null;
		codeContext = new CodeContext(this, root, rowIdx);

		preCodeType = getPreCodeType(lineHeader, columnIdx, rowIdx);
		if(preCodeType == PRE_TYPE_NEWLINE){
			pre_var_tag_ins_or_global = VAR_UNKNOW;//VAR_LOCAL | VAR_INSTANCE | VAR_GLOBAL;
			getVariables(rowIdx, root, true, "", outAndCycle, pre_var_tag_ins_or_global);
		}else if(preCodeType == PRE_TYPE_RESOURCES){
			getResources(outAndCycle, getRequireLibs(root, outRequireLibs), true);
		}else if(preCodeType == PRE_TYPE_AFTER_IMPORT_ONLY){
			getSubPackageAndClasses(outAndCycle, getRequireLibs(root, outRequireLibs), true, true);
		}else if(preCodeType == PRE_TYPE_AFTER_IMPORTJAVA){
			getSubPackageAndClasses(outAndCycle, getRequireLibs(root, outRequireLibs), false, true);
		}else if(preCodeType == PRE_TYPE_AFTER_INCLUDE){
			pre_var_tag_ins_or_global = VAR_CLASS;
			getVariables(rowIdx, root, false, preCode, outAndCycle, pre_var_tag_ins_or_global);
			getSubPackageAndClasses(outAndCycle, getRequireLibs(root, outRequireLibs), true, true);
		}else if(preCodeType == PRE_TYPE_IN_DEF_CLASS_FOR_METHOD_FIELD_ONLY){
			getMethodAndFieldForInstance(true, backgroundDefClassNode, false, outAndCycle, false, true, false);
		}else if(preCodeType == PRE_TYPE_OVERRIDE_METHOD){
//			final int shiftBackLen = DEF_MEMBER.length() + 1 + preCode.length();
			final JRubyClassDesc desc = codeContext.getDefJRubyClassDesc();//isInDefClass(root, codeContext, rowIdx);
			if(desc != null){
				buildItem(outAndCycle, JRUBY_CLASS_INITIALIZE_DEF, CodeItem.TYPE_METHOD);
				appendInterfaces(desc.include, outAndCycle);//要先执行，这样def因相同不会再次添加
				getMethodAndFieldForInstance(true, desc, false, outAndCycle, true, false, false);
				Collections.sort(outAndCycle);
			}
		}else if(preCodeType == PRE_TYPE_BEFORE_INSTANCE){
			getVariables(rowIdx, root, true, "", outAndCycle, pre_var_tag_ins_or_global);//情形：在行首输入im，可能后续为import或ImageIO
		}else if(preCodeType == PRE_TYPE_AFTER_INSTANCE_OR_CLASS_BUT_NOT_VALID){
			outAndCycle.clear();
		}else if(preCodeType == PRE_TYPE_AFTER_INSTANCE_OR_CLASS){
			if(preClass != null){
				if(preClass.isInstance){
					clearArray(outAndCycle);
					final ReturnType rt = preClass.getReturnType();
					final Type type = rt.getType();
					if(type != RubyHelper.JRUBY_ARRAY_CLASS && rt.isGenericArrayType()){
						preClass = buildJRubyClassDesc(J2SE_ARRAY_CLASS, true);
					}else{
						appendInterfaces(preClass.include, outAndCycle);
					}
					getMethodAndFieldForInstance(true, preClass, preClass.isInExtend?false:true, outAndCycle, true, false, false);
					appendObjectForInterfaceOnly(preClass, outAndCycle);
					appendAttrAssistantForDefClass(preClass, outAndCycle);
					appendDefInstanceMethods(outAndCycle);
					appendStructFields(preClass, outAndCycle);
					appendEnumerableMethods(type, outAndCycle);//应置于append最后
				}else{
					getMethodAndFieldForClass(preClass, outAndCycle, false, true, false);
					getMethodAndFieldForInstance(false, rubyClassJCD, true, outAndCycle, true, false, false);//如superclass
					getMethodAndFieldForInstance(false, rubyModuleJCD, true, outAndCycle, true, false, false);//如remove_class_variable
				}
				Collections.sort(outAndCycle);
			}else{//直接从背景中取类，会出现preClass==null
				if((pre_var_tag_ins_or_global & VAR_INSTANCE) != 0 
						|| (pre_var_tag_ins_or_global & VAR_GLOBAL) != 0){
					clearArray(outAndCycle);

					appendInsOrGlobalFromDef();

					Collections.sort(outAndCycle);
				}else{
					if(preCodeSplitIsDot == false){
//						getVariables(root, true, "", out, scriptIdx, pre_var_tag_ins_or_global);//改为，增加preCode过滤
//						final int startTipIdx = scriptIdx - preCode.length();
						getVariables(rowIdx, root, true, preCode, outAndCycle, pre_var_tag_ins_or_global);
					}
				}
			}
		}
	}

	/**
	 * true means in class def, local var is not required
	 * @return
	 */
	private final boolean appendInsOrGlobalFromDef() {
		final JRubyClassDesc desc = codeContext.getDefJRubyClassDesc();
		if(desc != null && desc.hasExtChain()){
			final MethodDefNode mNode = isInMethodDefNode(codeContext.bottomNode);
			if(mNode != null){
				if(mNode instanceof DefnNode){
					getInsOrGloablVar(desc.getReturnType().toClassDefNodeArray(), outAndCycle);
				}else if(mNode instanceof DefsNode){
					getInsOrGloablVarForClass(desc.getReturnType().toClassDefNodeArray(), outAndCycle);
				}
			}
			return true;
		}else if(desc != null && desc.defIterNode != null){
			final Node[] nodes = {desc.defIterNode};
			getInsOrGloablVar(nodes, outAndCycle);//可以接受外围的local变量
			return false;
		}else{
			if(backgroundDefClassNode != null && backgroundDefClassNode.hasExtChain()){
				getInsOrGloablVar(backgroundDefClassNode.getReturnType().toClassDefNodeArray(), outAndCycle);
				return true;
			}
		}
		return false;
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
	private final static int PRE_TYPE_AFTER_INSTANCE_OR_CLASS_BUT_NOT_VALID = 7;
	private final static int PRE_TYPE_RESOURCES = 8;//"/test/res/hc_16.png"
	public final static int PRE_TYPE_OVERRIDE_METHOD = 9;
	private final static int PRE_TYPE_AFTER_INCLUDE = 10;
	
	public String preCode, lastMousePreCode;
	public boolean isForceResTip = false;
	private int pre_var_tag_ins_or_global;
	public boolean preCodeSplitIsDot;
	public boolean varIsNotValid;
	public int preCodeType;
	private JRubyClassDesc preClass;
	private JRubyClassDesc backgroundDefClassNode;
	
	private final ArrayList<String> outRequireLibs = new ArrayList<String>();
	
	private final static String PRE_IMPORT = "import ";
	private final static String PRE_IMPORT_JAVA = "import Java::";
	private final static char[] java_dot_chars = "java.".toCharArray();
	private final static char[] javax_dot_chars = "javax.".toCharArray();
	private final static char[] import_chars = PRE_IMPORT.toCharArray();
	private final static char[] include_chars = (JRUBY_INCLUDE + " ").toCharArray();
	private final static int include_chars_len = include_chars.length;
	private final static int import_chars_len = import_chars.length;
	private final static char[] import_java_chars = PRE_IMPORT_JAVA.toCharArray();
	private final static int import_java_chars_len = import_java_chars.length;
	
	public final static boolean matchChars(final char[] lineChars, final int offset, final char[] matchChars){
		if(offset < 0){
			return false;
		}
		
		if(lineChars.length < (matchChars.length - offset)){
			return false;
		}
		if(lineChars.length - offset < matchChars.length){
			return false;
		}
		
		final int endIdx = offset + matchChars.length;
		for (int i = offset; i < endIdx; i++) {
			if(matchChars[i - offset] != lineChars[i]){
				return false;
			}
		}
		return true;
	}
	
	private final static ClassNode isInClassDefNode(final Node node){
		if(node instanceof ClassNode){
			return (ClassNode)node;
		}
		
		final Node parent = node.getParent();
		if(parent == null){
			return null;
		}else{
			return isInClassDefNode(parent);
		}
	}
	
	private final static MethodDefNode isInMethodDefNode(final Node node){
		if(node instanceof MethodDefNode){
			return (MethodDefNode)node;
		}
		
		final Node parent = node.getParent();
		if(parent == null){
			return null;
		}else{
			return isInMethodDefNode(parent);
		}
	}
	
	private final void getVariablesUpperForward(final int rowIdx, final Node node, final String lowerPreCode, final ArrayList<CodeItem> out, 
			int type_var){
		if(node == null){
			return;
		}
		
		if(node instanceof MethodDefNode){// || isDefNodeUnderNewlineNode(node)){//将方法定义的参数取出来。如：opeator(id, para)，则增加id, para
			getParameterFromMethod((MethodDefNode)node, out);
		}else if(node instanceof ClassNode || node instanceof SClassNode || isInnerClassDef(node)){
			if((type_var & VAR_LOCAL) != 0){
				type_var ^= VAR_LOCAL;
			}
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
				
				final NodeType nodeType = one.getNodeType();
				
				if(((type_var & CodeHelper.VAR_CONST) != 0 || (type_var & CodeHelper.VAR_CLASS) != 0) 
						&& nodeType == NodeType.FCALLNODE){//(FCallNode:import, (ArrayNode, (CallNode:JLabel, (CallNode:swing,
					final FCallNode callNode = (FCallNode)one;
					if(callNode.getName().equals("import")){
						final ListNode importArgs = (ListNode)callNode.getArgs();//注意：org.jrubyparser.ast.ArrayNode extends org.jrubyparser.ast.ListNode
						final CallNode firstPackageNode = (CallNode)importArgs.childNodes().get(0);
						final String name = firstPackageNode.getName();
						final String nameLower = name.toLowerCase();
						if(nameLower.startsWith(lowerPreCode) && (CodeItem.contains(out, name) == false)){//由于是从下向上，可能被添加了一次
							final CodeItem item = buildClassItem(out, name, nameLower);
							item.codeForDoc = buildFullClassNameFromReceiverNode(firstPackageNode);
							continue;
						}
					}
				}
				
				if((type_var & CodeHelper.VAR_UNKNOW) != 0 && nodeType == NodeType.DASGNNODE){//run{while{...}}
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

				if((type_var & CodeHelper.VAR_CONST) != 0 && nodeType == NodeType.CLASSNODE){
					final ClassNode cNode = (ClassNode)one;
					final String name = getDefClassName(cNode);
					final String nameLower = name.toLowerCase();
					if(nameLower.startsWith(lowerPreCode) && (CodeItem.contains(out, name) == false)){
						buildClassItem(out, name, nameLower);
						continue;
					}
				}
				
				if(nodeType == NodeType.IFNODE || nodeType == NodeType.WHILENODE 
		    			|| nodeType == NodeType.UNTILNODE){//unless ==> if
	    	    	searchAllAsgnFromCond(one, lowerPreCode, out, type_var);
		    		continue;
				}
				
				if(nodeType == NodeType.MULTIPLEASGNNODE){
					final MultipleAsgnNode multi = (MultipleAsgnNode)one;
					addAsgnNodeForMultiAsgn(multi, out, lowerPreCode, type_var);
				}
				
				addAsgnNode(one, nodeType, lowerPreCode, out, type_var);
			}
		}
		
		final Node parentNode = node.getParent();
		if(parentNode != null){
			getVariablesUpperForward(rowIdx, parentNode, lowerPreCode, out, type_var);
		}
	}

	private final void addAsgnNodeForMultiAsgn(final MultipleAsgnNode multi, final ArrayList<CodeItem> out, final String lowerPreCode, final int type_var) {
		final ListNode list = multi.getPre();
		final int listSize = list.size();
		for (int j = 0; j < listSize; j++) {
			final Node sub = list.get(j);
			if(sub instanceof MultipleAsgnNode){
				addAsgnNodeForMultiAsgn((MultipleAsgnNode)sub, out, lowerPreCode, type_var);
			}else{
				addAsgnNode(sub, sub.getNodeType(), lowerPreCode, out, type_var);
			}
		}
		
		final Node rest = multi.getRest();
		if(rest != null){
			addAsgnNode(rest, rest.getNodeType(), lowerPreCode, out, type_var);
		}
	}

	public final void getParameterFromMethod(final MethodDefNode defNode, final ArrayList<CodeItem> out) {
		final ArgsNode argsNode = defNode.getArgs();
		
		addMethodParameterFromList(out, argsNode.getPre());
		addMethodParameterFromList(out, argsNode.getPost());
		final ArgumentNode restArgu = argsNode.getRest();
		if(restArgu != null){
			buildVarItem(out, restArgu.getName());
		}
		final BlockArgNode blockArgu = argsNode.getBlock();
		if(blockArgu != null){
			buildVarItem(out, blockArgu.getName());
		}
		addMethodParameterFromList(out, argsNode.getOptional());
	}
	
	public static final Node searchNode(final Node node, final Class instanceClass){
		final List<Node> childNodes = node.childNodes();
		final int size = childNodes.size();
		for (int i = 0; i < size; i++) {
			final Node sub = childNodes.get(i);
			if(instanceClass.isInstance(sub)){
				return sub;
			}
			final Node result = searchNode(sub, instanceClass);
			if(result != null){
				return result;
			}
		}
		return null;
	}

	private final void addMethodParameterFromList(final ArrayList<CodeItem> out, final ListNode list) {
		if(list == null){
			return;
		}
		
		final int size = list.size();
		for(int i = 0; i<size; i++){
			try{
				final Node node = list.get(i);
				final String name;
				if(node instanceof ArgumentNode){
					final ArgumentNode parameterNode = (ArgumentNode)node;
					name = parameterNode.getName();
				}else if(node instanceof OptArgNode){
					final OptArgNode parameterNode = (OptArgNode)node;
					name = parameterNode.getName();
				}else{
					continue;
				}
				buildVarItem(out, name);
			}catch (final Throwable e) {
				e.printStackTrace();
			}
		}
	}

	private final void searchAllAsgnFromCond(final Node cond, final String lowerPreCode, final ArrayList<CodeItem> out, final int type_var) {
		final List<Node> list = cond.childNodes();
		final int size = list.size();
		for (int i = 0; i < size; i++) {
			final Node sub = list.get(i);
			if(sub instanceof AssignableNode){
				addAsgnNode(sub, sub.getNodeType(), lowerPreCode, out, type_var);
			}else{
				searchAllAsgnFromCond(sub, lowerPreCode, out, type_var);
			}
		}
	}
	
	private final void addAsgnNode(Node one, NodeType nodeType, final String lowerPreCode, final ArrayList<CodeItem> out, 
			final int type_var){
		boolean isInWhile = false;
		
		while(nodeType == NodeType.CONSTDECLNODE){//Customer = Struct.new(:name, :address, :zip)
			//注意：不加(type_var & CodeHelper.VAR_LOCAL) != 0
			final ConstDeclNode constDecl = (ConstDeclNode)one;
			final String name = constDecl.getName();
			if(CodeItem.contains(out, name) == false){
				final CodeItem item = buildVarItem(out, name);
				final JRubyClassDesc jcd = findClassFromRightAssign(constDecl, codeContext);
				if(jcd != null){
					item.codeForDoc = jcd.getReturnType().getRawClass().getName();
					item.type = CodeItem.TYPE_CLASS;
				}
			}
			isInWhile = true;
			one = constDecl.getValue();
			if(one == null){
				break;
			}
			nodeType = one.getNodeType();
		}
		if(isInWhile){
			return;
		}
		
		while((type_var & CodeHelper.VAR_INSTANCE) != 0 	&& nodeType == NodeType.INSTASGNNODE){//CodeItem.TYPE_CLASS
			final InstAsgnNode insNode = (InstAsgnNode)one;
			final String name = PRE_CLASS_INSTANCE_STR + insNode.getName();
			final String nameLower = name.toLowerCase();
			if(nameLower.startsWith(lowerPreCode) && (CodeItem.contains(out, name) == false)){
				final CodeItem item = CodeItem.getFree();
				item.code = name;
				item.codeForDoc = item.code;
				item.codeDisplay = item.code;
				item.codeLowMatch = nameLower;
				item.type = CodeItem.TYPE_VARIABLE;
				out.add(item);
			}
			isInWhile = true;
			one = insNode.getValue();
			if(one == null){
				break;
			}
			nodeType = one.getNodeType();
		}
		if(isInWhile){
			return;
		}
		
		while((type_var & CodeHelper.VAR_LOCAL) != 0 && nodeType == NodeType.LOCALASGNNODE){
			final LocalAsgnNode localNode = (LocalAsgnNode)one;
			final String name = localNode.getName();
			final String nameLower = name.toLowerCase();
			if(nameLower.startsWith(lowerPreCode) && (CodeItem.contains(out, name) == false)){
				final CodeItem item = CodeItem.getFree();
				item.code = name;
				item.codeForDoc = item.code;
				item.codeDisplay = name;
				item.codeLowMatch = nameLower;
//				if(isInDefnNode(one)){//方法定义内
//					item.type = CodeItem.TYPE_VARIABLE;
//				}else{
					item.type = CodeItem.TYPE_VARIABLE;//原值CodeItem.TYPE_CLASS;
//				}
				out.add(item);
			}
			isInWhile = true;
			one = localNode.getValue();			
			if(one == null){
				break;
			}
			nodeType = one.getNodeType();
		}
		if(isInWhile){
			return;
		}
		
		while((type_var & CodeHelper.VAR_GLOBAL) != 0 && nodeType == NodeType.GLOBALASGNNODE){
			final GlobalAsgnNode globalNode = (GlobalAsgnNode)one;
			final String name = PRE_GLOBAL_DALLOR_STR + globalNode.getName();
			final String nameLower = name.toLowerCase();
			if(nameLower.startsWith(lowerPreCode) && (CodeItem.contains(out, name) == false)){
				final CodeItem item = CodeItem.getFree();
				item.code = name;
				item.codeForDoc = item.code;
				item.codeDisplay = name;
				item.codeLowMatch = nameLower;
				item.type = CodeItem.TYPE_VARIABLE;
				out.add(item);
			}
			isInWhile = true;
			one = globalNode.getValue();
			if(one == null){
				break;
			}
			nodeType = one.getNodeType();
		}
		if(isInWhile){
			return;
		}
	}

	private final CodeItem buildClassItem(final ArrayList<CodeItem> out, final String name, final String nameLower) {
		final CodeItem item = CodeItem.getFree();
		item.code = name;
		item.codeForDoc = item.code;
		item.codeDisplay = name;
		item.fieldOrMethodOrClassName = name;
		item.codeLowMatch = nameLower;
		item.type = CodeItem.TYPE_CLASS;
		out.add(item);
		
		return item;
	}

	/**
	 * 注意：可能返回null
	 * @param out
	 * @param name
	 * @return
	 */
	private final CodeItem buildVarItem(final ArrayList<CodeItem> out, final String name) {
		if(CodeItem.contains(out, name)){
			return null;
		}
		
		final CodeItem item = CodeItem.getFree();
		item.code = name;
		item.codeForDoc = item.code;
		item.codeDisplay = name;
		item.fieldOrMethodOrClassName = name;
		item.codeLowMatch = name.toLowerCase();
		item.type = CodeItem.TYPE_VARIABLE;
		out.add(item);
		
		return item;
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
		item.fieldOrMethodOrClassName = name;
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
		
		appendInsOrGlobalFromDef();
		
		final Node paraNode = searchParaCallingNodeByIdx(node, rowIdx);
		getVariablesUpperForward(rowIdx, paraNode, preCode.toLowerCase(), out, type_var);
		
		buildClassItem(out, JRUBY_CLASS_FOR_NEW, JRUBY_CLASS_FOR_NEW.toLowerCase());
		
		if(preCodeSplitIsDot == false && backgroundDefClassNode != null){
			final MethodDefNode method = isInMethodDefNode(codeContext.bottomNode);

			if(method == null){
				getMethodAndFieldForInstance(false, rubyModuleJCD, false, outAndCycle, true, false, false);//如attr_reader
			}
			
			if(method != null){
				if(method instanceof DefnNode){
					getMethodAndFieldForInstance(true, backgroundDefClassNode, false, out, true, false, false);
				}else if(method instanceof DefsNode){
					getMethodAndFieldForClass(backgroundDefClassNode, out, true, false, false);
				}
			}
		}
		for (int i = 0; i < RubyHelper.JRUBY_STR_MAP.length; i++) {
			final String name = RubyHelper.JRUBY_STR_MAP[i];
			if(CodeItem.contains(out, name)){//如果有import，则不收录Ruby的
				continue;
			}
			final CodeItem item = buildClassItem(out, name, name.toLowerCase());
			if(item != null){
				item.codeForDoc = RubyHelper.JRUBY_CLASS_MAP[i].getName();
				item.isRubyClass = true;
			}
		}
		
		//添加puts, 等kernel方法
		getMethodAndFieldForInstance(false, rubyKernelJCD, true, out, true, false, DISABLE_SAME_METHOD_NAME);
		addToMethod(RubyHelper.JRUBY_ANONYMOUS_CLASS, out, "defined?", "description", RubyHelper.ANONYMOUS_CLASS_BASE_NAME);
		
		Collections.sort(out);
		return;
	}
	
	private static final int searchSubChars(final char[] lines, final char[] searchChars, final int fromIdx){
		final int size = lines.length;
		final char firstChar = searchChars[0];
		final int javaMHlength = searchChars.length;
		for (int i = size - javaMHlength; i >= fromIdx; i--) {
			if(lines[i] == firstChar){
				if(matchChars(lines, i, searchChars)){
					return i;
				}
			}
		}
		return -1;
	}
	
	final static char[] partSpliter = {' ', '\t', ',', '(', '+', '-', '*', '/'};
	
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
			
			if(i >= DEF_MEMBER_BS.length && lineHeader[i] == ' ' && matchChars(lineHeader, i - DEF_MEMBER_BS.length, DEF_MEMBER_BS)){
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
			//puts " #{word} "
			int partSplitIdx = -1;
			char isInStr = 0;
			for (int i = columnIdx - 1; i >= 0; i--) {
				final char checkSplitChar = lineHeader[i];
				if((checkSplitChar == '"' || checkSplitChar == '\'') && (i == 0 || ((i - 1) >= 0 && lineHeader[i - 1] != '\\'))){//array[var.indexOf("Hello")]
					if(isInStr != 0){
						if(isInStr == checkSplitChar){
							isInStr = 0;
						}else{
							continue;
						}
					}else{
						isInStr = checkSplitChar;
					}
				}
				if(isInStr != 0){
					continue;
				}
				if(i > 1 && checkSplitChar == '{' && lineHeader[i - 1] == '#'){
					partSplitIdx = i;
					break;
				}
			}
			
			if(partSplitIdx > 0){
				return searchCodePart(lineHeader, columnIdx, rowIdx, partSplitIdx + 1);
			}
		}
		
		{
			char isInStr = 0;
			int lastYinHaoIdx = -1;
			for (int i = 0; i < columnIdx; i++) {
				final char checkSplitChar = lineHeader[i];
				if((checkSplitChar == '"' || checkSplitChar == '\'') && (i == 0 || (i > 0 && lineHeader[i - 1] != '\\'))){
					if(isInStr != 0){
						if(isInStr == checkSplitChar){
							isInStr = 0;
							lastYinHaoIdx = -1;
						}else{
							continue;
						}
					}else{
						isInStr = checkSplitChar;
						if(lastYinHaoIdx == -1){
							lastYinHaoIdx = i;
						}
					}
				}
			}
	        if(isInStr != 0){ 
	        	preCode = String.valueOf(lineHeader, lastYinHaoIdx + 1, columnIdx - (lastYinHaoIdx + 1));
	        	return PRE_TYPE_RESOURCES;//Java::test.TestClass::java_class.resource("/test/press_on_64.png")
	        }
		}
		
		final int partStartIdx = 0;
		final char firstChar = lineHeader[partStartIdx];
		if(firstChar != '\t' && firstChar != ' '){
			if(matchChars(lineHeader, partStartIdx, import_chars)){
				if(columnIdx < import_chars_len){
					preCode = "";
				}else if(matchChars(lineHeader, partStartIdx, import_java_chars)){
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
		{
			char isInStr = 0;
			for (int i = columnIdx - 1; i >= 0; i--) {
				final char checkSplitChar = lineHeader[i];
				if((checkSplitChar == '"' || checkSplitChar == '\'') && (i == 0 || ((i - 1) >= 0 && lineHeader[i - 1] != '\\'))){//array[var.indexOf("Hello")]
					if(isInStr != 0){
						if(isInStr == checkSplitChar){
							isInStr = 0;
						}else{
							continue;
						}
					}else{
						isInStr = checkSplitChar;
					}
				}
				if(isInStr != 0){
					continue;
				}
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
		if(matchChars(lineHeader, partStartIdx, java_dot_chars) || matchChars(lineHeader, partStartIdx, javax_dot_chars)){
			final int maohaoIdx = searchSubChars(lineHeader, MAO_HAO_ONLY, partStartIdx);
			if(maohaoIdx < 0){
				//java.lang.StringBuilder.new() => preCode:new, preClass = StringBuilder
				for (int i = columnIdx - 1; i >= partStartIdx; i--) {
					final char c = lineHeader[i];
					if(c == '.'){
						final String assignStr = searchLeftAssign(lineHeader, i - 1, 0, false);
						final List<Node> childNodes = parseScripts(assignStr).childNodes();
						if(childNodes.size() > 0){
							final Node node = childNodes.get(0).childNodes().get(0);
							setPosition(node, rowIdx);
							final CodeContext codeContext = new CodeContext(this, root, rowIdx, true);//parseScripts(assignStr)
							final JRubyClassDesc out = findClassFromRightAssign(node, codeContext);
							if(out != null){
								preClass = out;
								preCodeSplitIsDot = true;
								preCode = String.valueOf(lineHeader, i + 1, columnIdx - i - 1);
								return PRE_TYPE_AFTER_INSTANCE_OR_CLASS;
							}
						}
					}
				}
				
				//java.lang.Strin
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

		{//注意：要置于Java::之后。语法class ABC \ninclude java.lang.Runnable
			for (int i = 0; i < lineHeader.length; i++) {
				final char c = lineHeader[i];
				if(c != '\t' && c != ' '){
					if(matchChars(lineHeader, i, include_chars)){
						final int cutIdx = i + include_chars_len;
						preCode = String.valueOf(lineHeader, cutIdx, columnIdx - cutIdx);
						return PRE_TYPE_AFTER_INCLUDE;
					}
				}
			}
		}
		
		final JRubyClassDesc desc = codeContext.getDefJRubyClassDesc();//isInDefClass(root, codeContext, rowIdx);
		//LocPreClass
		preClass = findPreCodeAfterVar(lineHeader, columnIdx, rowIdx, partStartIdx);
		if(desc != null){
			backgroundDefClassNode = desc;
		}

		if(varIsNotValid){
			return PRE_TYPE_AFTER_INSTANCE_OR_CLASS_BUT_NOT_VALID;
		}
		
		if(preClass == null){
			return PRE_TYPE_BEFORE_INSTANCE;
		}else{
			return PRE_TYPE_AFTER_INSTANCE_OR_CLASS;
		}
	}

	private final static ClassLoader getClassLoader() {
		return SimuMobile.getRunTestEngine().getProjClassLoader();
	}
	
	static final char[] JRUBY_JAVA_CLASS_CHARS = JRUBY_JAVA_CLASS.toCharArray();
	static final char[] JAVA_MAO_MAO = RubyExector.JAVA_MAO_MAO.toCharArray();
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
		pre_var_tag_ins_or_global = VAR_UNKNOW;
		
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
				while(i>=partStartIdx && lineHeader[--i] != '"' && lineHeader[i - 1] != '\\'){
				}
				continue;
			}else if(c == '\''){
				while(i>=partStartIdx && lineHeader[--i] != '\'' && lineHeader[i - 1] != '\\'){
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
				final int offset;
				if(c == PRE_CLASS_VAR_OR_INS_CHAR){//@
					if(isPreClassChar(lineHeader, i)){
						pre_var_tag_ins_or_global = VAR_CLASS;
						offset = i - 1;
					}else{
						pre_var_tag_ins_or_global = VAR_INSTANCE;
						offset = i;
					}
				}else if(c == PRE_GLOBAL_DALLOR_CHAR){//$
					pre_var_tag_ins_or_global = VAR_GLOBAL;
					offset = i;
				}else{
					offset = i + 1;
				}
				preCode = String.valueOf(lineHeader, offset, columnIdx - offset);
				if(pre_var_tag_ins_or_global == VAR_INSTANCE && PRE_CLASS_INSTANCE_STR.equals(preCode)){
					pre_var_tag_ins_or_global |= VAR_CLASS;//增补，有可能用户还需输入VAR_CLASS
				}
				return null;
			}
		}
		
		preCode = String.valueOf(lineHeader, partStartIdx, columnIdx - partStartIdx);
		
		return null;
	}
	
	private static boolean isPreClassChar(final char[] chars, int currentIdx){
		if(--currentIdx >= 0){
			return chars[currentIdx] == PRE_CLASS_VAR_OR_INS_CHAR;
		}
		return false;
	}
	
	/**
	 * 从表达式中搜索如：+ {@abc.m()} + {$efg.kk(a, b)} + {edf::efg()} + {abc.efg()}
	 * @param lineHeader
	 * @param rightKuoIdx 后端)所在index
	 * @return
	 */
	private final static String searchLeftAssign(final char[] lineHeader, final int rightKuoIdx, final int partStartIdx, final boolean isFromRightKuo){
		int leftKuoIdx;
		if(isFromRightKuo){
			leftKuoIdx = searchLeftMethodStartIdx(lineHeader, rightKuoIdx, partStartIdx);
		}else{
			leftKuoIdx = rightKuoIdx;
		}
		
		//变量名段
		if(leftKuoIdx >= partStartIdx 
				|| 
				(partStartIdx == 0 && leftKuoIdx == -1)){//行首ProjectContext::getProjectContext().
			for (; leftKuoIdx >= partStartIdx; leftKuoIdx--) {
				final char c = lineHeader[leftKuoIdx];
				//必须使用包含 . ，否则出现swing.JLable::getLocale()
				if(c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9' 
						|| c == '_' || c == ':' || c == PRE_GLOBAL_DALLOR_CHAR || c == PRE_CLASS_VAR_OR_INS_CHAR || c == '.'){
					continue;
				}else if(c == ']'){//可能包含数组
					leftKuoIdx = stopToLeftFang(lineHeader, leftKuoIdx, partStartIdx);
					continue;
				}else if(c == ')'){
					leftKuoIdx = searchLeftMethodStartIdx(lineHeader, leftKuoIdx, partStartIdx);
					continue;
				}else if(c == '"'){//Ruby String表达式，需要弹出方法时
					leftKuoIdx--;
					for (; leftKuoIdx >= partStartIdx; leftKuoIdx--) {
						final char cc = lineHeader[leftKuoIdx];
						if(cc == '"' && (leftKuoIdx - 1) >= partStartIdx && lineHeader[leftKuoIdx - 1] != '\\'){
							break;
						}
					}
					if(leftKuoIdx >= partStartIdx){
						continue;
					}else{
						break;
					}
				}else if(c == '\''){
					leftKuoIdx--;
					for (; leftKuoIdx >= partStartIdx; leftKuoIdx--) {
						final char cc = lineHeader[leftKuoIdx];
						if(cc == '\'' && (leftKuoIdx - 1) >= partStartIdx && lineHeader[leftKuoIdx - 1] != '\\'){
							break;
						}
					}
					if(leftKuoIdx >= partStartIdx){
						continue;
					}else{
						break;
					}
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
			}else if(c == ']'){
				leftKuoIdx = stopToLeftFang(lineHeader, leftKuoIdx, partStartIdx);
				continue;
			}else if(c == '"'){
				while(leftKuoIdx >=1 && lineHeader[--leftKuoIdx] != '"' && lineHeader[leftKuoIdx - 1] != '\\'){
				}
				continue;
			}else if(c == '\''){
				while(leftKuoIdx >=1 && lineHeader[--leftKuoIdx] != '\'' && lineHeader[leftKuoIdx - 1] != '\\'){
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
				
				if(lineHeader[leftKuoIdx] == ']'){
					leftKuoIdx = stopToLeftFang(lineHeader, leftKuoIdx, partStartIdx) - 1;
				}
				
				if(lineHeader[leftKuoIdx] == ')'){
					//pp.add().add()
					return searchLeftMethodStartIdx(lineHeader, leftKuoIdx, partStartIdx);
				}else{
					break;
				}
			}else if(c == ':'){
				leftKuoIdx -= 1;//注意：不是2
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
	 * 返回[的idx
	 * @param lineHeader
	 * @param i
	 * @param partStartIdx
	 * @return
	 */
	private final static int stopToLeftFang(final char[] lineHeader, int i, final int partStartIdx){
		i--;
		int deep = 0;
		char isInStr = 0;
		for (; i >= partStartIdx; i--) {
			final char cc = lineHeader[i];
			if((cc == '"' || cc == '\'') && (i - 1) >= partStartIdx && lineHeader[i - 1] != '\\'){
				if(isInStr != 0){
					if(isInStr == cc){
						isInStr = 0;
					}
				}else{
					isInStr = cc;
				}
				continue;
			}
			if(isInStr != 0){
				continue;
			}else{
				if(cc == '['){
					if(deep > 0){
						deep--;
						continue;
					}
					return i;
				}else if(cc == ']'){
					deep++;
				}
			}
		};
		return -1;
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
			}else if(c == PRE_CLASS_VAR_OR_INS_CHAR){//@
				final int offset = i + 1;
				final int var_tag;
				if(isPreClassChar(lineHeader, i)){
//					offset = i - 1;
					var_tag = VAR_CLASS;
				}else{
//					offset = i;
					var_tag = VAR_INSTANCE;
				}
				final String v = String.valueOf(lineHeader, offset, columnIdx - offset);
				final JRubyClassDesc desc = codeContext.getDefJRubyClassDesc();
				if(desc != null && desc.defIterNode != null){
					final JRubyClassDesc out = searchVarInIterNode(desc.defIterNode, v, var_tag, codeContext);
					if(out != null){
						return out;
					}
				}
				return findParaClass(codeContext, v, var_tag);
			}else if(c == PRE_GLOBAL_DALLOR_CHAR){//$
				final int offset = i + 1;
				final String v = String.valueOf(lineHeader, offset, columnIdx - offset);
				return findParaClass(codeContext, v, CodeHelper.VAR_GLOBAL);
			}else if(c == ')' || c == ']' || c == '.'){
				final String assignStr = searchLeftAssign(lineHeader, columnIdx - 1, 0, c == ')');//从左向提取类，注意：不能用partStartIdx
				//assignStr可能为arr[0][0].get(0) 或 ProjectContext.getProjectContext()
				final List<Node> childNodes = parseScripts(assignStr).childNodes();
				if(childNodes.size() > 0){
					//共有以下两种情形
					//[(NewlineNode, (CallNode:put, (GlobalVarNode:aa), (ArrayNode, (StrNode), (StrNode))))]
					//(RootNode, (NewlineNode, (CallNode:add,
					final Node node = childNodes.get(0).childNodes().get(0);
					setPosition(node, rowIdx);
					final CodeContext codeContext = new CodeContext(this, root, rowIdx, true);//parseScripts(assignStr)
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
					JRubyClassDesc out = findParaClass(codeContext, v, CodeHelper.VAR_LOCAL);
					if(out != null){
						return out;
					}
					
					//Ruby表达式时，比如"Hello".时需要弹出方法(含数组)
					final String assignStr = searchLeftAssign(lineHeader, columnIdx - 1, 0, false);//从左向提取类，注意：不能用partStartIdx
					if(v.equals(assignStr) == false){
						final List<Node> childNodes = parseScripts(assignStr).childNodes();
						if(childNodes.size() > 0){
							final Node node = childNodes.get(0).childNodes().get(0);
							setPosition(node, rowIdx);
							final CodeContext codeContext = new CodeContext(this, root, rowIdx, true);//parseScripts(assignStr)
							out = findClassFromRightAssign(node, codeContext);
							if(out != null){
								return out;
							}
						}
					}
					
	    			final RubyClassAndDoc rcd = RubyHelper.searchRubyClassByShortName(v);
	    			if(rcd != null){
	    				return buildJRubyClassDesc(rcd.claz, false);
	    			}
					
					return null;
				}
			}
		}
		final String v = String.valueOf(lineHeader, partStartIdx, columnIdx - partStartIdx);
		if(JRUBY_CLASS_FOR_NEW.equals(v)){
			final JRubyClassDesc desc = buildJRubyClassDesc(JRUBY_CLASS_FOR_BULDER, false);
			return desc;
		}else{
			{
				final JRubyClassDesc out = findParaClass(codeContext, v, CodeHelper.VAR_LOCAL);
				if(out != null){
					return out;
				}
			}
			
			//Ruby表达式时，比如"File."时需要弹出方法(含数组)
			final String assignStr = searchLeftAssign(lineHeader, columnIdx - 1, 0, false);//从左向提取类，注意：不能用partStartIdx
			if(v.equals(assignStr) == false){//不再搜索localVar
				final List<Node> childNodes = parseScripts(assignStr).childNodes();
				if(childNodes.size() > 0){
					final Node node = childNodes.get(0).childNodes().get(0);
					setPosition(node, rowIdx);
					final CodeContext codeContext = new CodeContext(this, root, rowIdx, true);//parseScripts(assignStr)
					final JRubyClassDesc out = findClassFromRightAssign(node, codeContext);
					if(out != null){
						return out;
					}
				}
			}
			
			final RubyClassAndDoc rcd = RubyHelper.searchRubyClassByShortName(v);
			if(rcd != null){
				return buildJRubyClassDesc(rcd.claz, false);
			}
			
			varIsNotValid = true;
			return null;
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
		window.hide();
		if(L.isInWorkshop){
			LogManager.log("[CodeTip] done hideByMouseEvent");
		}
	}

	public static final void buildListenerForScroll(final JScrollPane scrollPanel, final CodeHelper codeHelper) {
		final MouseMotionListener motionListern = new MouseMotionListener() {
			
			@Override
			public void mouseMoved(final MouseEvent e) {
				codeHelper.flipTipKeepOn();
			}
			
			@Override
			public void mouseDragged(final MouseEvent e) {
				
			}
		};
		scrollPanel.getHorizontalScrollBar().addMouseMotionListener(motionListern);
		scrollPanel.getVerticalScrollBar().addMouseMotionListener(motionListern);
	}

	/**
	 * for example, type1 [HashMap], type2 [Map], then return true. 
	 * @param type1
	 * @param type2
	 * @return
	 */
	public static final boolean isExtendsOrImplements(final Type type1, final Class type2){
		if(type1 == type2){
			return true;
		}
		
		boolean out;
		if(type1 instanceof Class){
			out = isExtendsOrImplements(((Class) type1).getSuperclass(), type2);
			if(out){
				return true;
			}
			
			final Class[] interfaces = ((Class) type1).getInterfaces();
			for (int i = 0; i < interfaces.length; i++) {
				if(isExtendsOrImplements(interfaces[i], type2)){
					return true;
				}
			}
		}
		
		return false;
	}
	
}

class J2SEArrayID {
}
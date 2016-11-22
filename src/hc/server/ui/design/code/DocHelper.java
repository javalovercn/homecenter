package hc.server.ui.design.code;

import hc.core.ContextManager;
import hc.core.HCTimer;
import hc.core.IConstant;
import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.ThreadPriorityManager;
import hc.server.DefaultManager;
import hc.server.ui.LinkProjectStatus;
import hc.util.ClassUtil;
import hc.util.HttpUtil;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

public class DocHelper {
	private static final HashMap<String, HashMap<String, String>> cache = new HashMap<String, HashMap<String,String>>();
	private static final HCTimer resetTimer = new HCTimer("", 1000 * 60 * 30, false) {
		@Override
		public final void doBiz() {
			if(LinkProjectStatus.getStatus() == LinkProjectStatus.MANAGER_DESIGN){
				resetTimerCount();
			}else{
				cache.clear();
				setEnable(false);
			}
		}
	};
	
	static final String bodyRule = "body { font-family:Arial, Helvetica, sans-serif; font-size: " + DefaultManager.getDesignerDocFontSize() + "pt; }";//font-family: " + font.getFamily() + ";
	static final String strongRule = "strong { font-size: " + DefaultManager.getDesignerDocFontSize() + "pt; }";//font-family: " + font.getFamily() + ";
	static final String aRule = "a { font-family:Arial, Helvetica, sans-serif; text-decoration:underline; color:blue; font-size: " + DefaultManager.getDesignerDocFontSize() + "pt; }";//font-family: " + font.getFamily() + ";
	static final String codeRule = "code { font-size: " + DefaultManager.getDesignerDocFontSize() + "pt; }";//font-family: " + font.getFamily() + ";

	private JFrame codeFrame;
	private DocLayoutLimit layoutLimit;
	private final JEditorPane docPane = new JEditorPane();
	private final JScrollPane scrollPanel = new JScrollPane(docPane);
	private final JFrame docFrame = new JFrame("");
	private final CodeHelper codeHelper;
	public boolean isForMouseOverTip;
	public int mouseOverX, mouseOverY, mouseOverFontHeight;
	
	DocHelper(final CodeHelper codeHelper, final CodeWindow codeWindow) {
		this.codeHelper = codeHelper;
		
		docPane.addKeyListener(codeWindow.keyListener);
		
		docPane.setContentType("text/html");
	    final StyleSheet styleSheet = ((HTMLDocument)docPane.getDocument()).getStyleSheet();
		docPane.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(final MouseEvent e) {
			}
			
			@Override
			public void mousePressed(final MouseEvent e) {
			}
			
			@Override
			public void mouseExited(final MouseEvent e) {
				codeHelper.notifyUsingByDoc(false);
			}
			
			@Override
			public void mouseEntered(final MouseEvent e) {
				codeHelper.notifyUsingByDoc(true);
			}
			
			final HTMLDocument hDoc = (HTMLDocument)docPane.getDocument();
    		final String classStartType1 = "../";
    		final String classStartType2 = "/api/";
    		final char methodSpliter = '#';
    		final int methodSpliterLength = 1;
    		final char parameterSpliter = '-';
    		final String strParameterSpliter = String.valueOf(parameterSpliter);
    		final int parameterSpliterLength = 1;
    		final Pattern removePackageName = Pattern.compile("\\b(\\w+\\.)");
			final ClassLoader classLoader = DocHelper.class.getClassLoader();

			/**
			 * 从一个java doc页面跳到另一个java doc页面
			 */
			@Override
			public void mouseClicked(final MouseEvent e) {
				final int pos = docPane.viewToModel(e.getPoint());
		        if (pos >= 0) {
		            final javax.swing.text.Element element = hDoc.getCharacterElement(pos);
		            final AttributeSet a = element.getAttributes();

		            final SimpleAttributeSet value = (SimpleAttributeSet) a.getAttribute(HTML.Tag.A);
		            if (value != null) {
		                final String href = (String) value.getAttribute(HTML.Attribute.HREF);
		                if (href != null) {
		                	//../../javax/swing/JComponent.html#getAutoscrolls--
		                	//click href : ../../../hc/server/ui/HTMLMlet.html
		                	//click href : http://docs.oracle.com/javase/8/docs/api/javax/swing/JComponent.html?is-external=true
//		                	click href : ../../../hc/server/ui/Mlet.html#resizeImage(java.awt.image.BufferedImage, int, int)
//		                	click href : ../../../hc/server/ui/ProjectContext.html#getMobileOS()
//		                	click href : ../../java/awt/Toolkit.html#createCustomCursor-java.awt.Image-java.awt.Point-java.lang.String-
		                	
		                	final int classEndIdx = href.lastIndexOf(".html");
		                	if(classEndIdx >= 0){
		                		int classStartIdx = href.lastIndexOf(classStartType1);
		                		if(classStartIdx >= 0){
		                			classStartIdx += classStartType1.length();
		                		}else{
		                			classStartIdx = href.lastIndexOf(classStartType2);
		                			if(classStartIdx >= 0){
		                				classStartIdx += classStartType2.length();
		                			}
		                		}
		                		
		                		final String fmClass = href.substring(classStartIdx, classEndIdx).replace('/', '.');
		                		
		                		String fieldOrMethodName = CLASS_DESC;
		                		final int methodSplitIdx = href.indexOf(methodSpliter, classEndIdx);
		                		if(methodSplitIdx >= 0){
		                			final String methodPart = href.substring(methodSplitIdx + methodSpliterLength);
		                			final int firstParameterSpliterIdx = methodPart.indexOf(parameterSpliter);
		                			if(firstParameterSpliterIdx < 0){
		                				fieldOrMethodName = removePackageName.matcher(methodPart).replaceAll("");
		                			}else{
		                				final String methodName = methodPart.substring(0, firstParameterSpliterIdx);
		                				String parameter = methodPart.substring(firstParameterSpliterIdx + parameterSpliterLength, methodPart.length() - parameterSpliterLength);
		                				parameter = parameter.replaceAll(strParameterSpliter, ", ");
		                				parameter = removePackageName.matcher(parameter).replaceAll("");
		                				fieldOrMethodName = methodName + "(" + parameter + ")";
		                			}
		                			
	                				final int classIdx = fmClass.lastIndexOf('.');
	                				if(classIdx > 0){
	                					final String className = fmClass.substring(classIdx + 1);
	                					if(fieldOrMethodName.startsWith(className)){//将构造方法转为new()
	                						fieldOrMethodName = CodeHelper.JRUBY_NEW + fieldOrMethodName.substring(className.length());
	                					}
	                				}
		                		}
		                		
								try {
									processDoc(Class.forName(fmClass, false, classLoader), false);
								} catch (final ClassNotFoundException e1) {
								}
								
			            		final String doc = getDoc(fmClass, fieldOrMethodName);
			            		if(doc != null && doc.length() > 0){
			            			SwingUtilities.invokeLater(new Runnable() {
										@Override
										public void run() {
					            			docPane.setText("<html><body style=\"background-color:#FAFBC5\">" + doc +"</body></html>");
					            			docPane.setCaretPosition(0);
										}
									});
			            		}else{
			            			if(href.startsWith("http")){
			            				HttpUtil.browse(href);
			            			}
			            		}
		                	}
//		                    L.V = L.O ? false : LogManager.log("click href : " + href);
		                }
		            }
		        }
			}
		});
		
		styleSheet.addRule(bodyRule);
		styleSheet.addRule(strongRule);
		styleSheet.addRule(aRule);
		styleSheet.addRule(codeRule);
		
		docPane.setEditable(false);
		docFrame.setVisible(false);
		docFrame.setUndecorated(true);
		
		docFrame.setLayout(new BorderLayout());
		docFrame.add(scrollPanel, BorderLayout.CENTER);
		docFrame.setPreferredSize(new Dimension(CodeWindow.MAX_WIDTH, CodeWindow.MAX_HEIGHT));
	}
	
	final Dimension docFrameSize = new Dimension();
	final Dimension codeFrameSize = new Dimension();
	final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	boolean isWillShowDoc;//用于invokeLater与setInvisible进行同步通信
	
	private final Runnable repainRunnable = new Runnable() {
		@Override
		public void run() {
			docFrame.validate();
			ClassUtil.revalidate(docFrame);
			docFrame.pack();

			docFrame.getSize(docFrameSize);
			
			int showX, showY;
			if(isForMouseOverTip){
				showX = mouseOverX;
				showY = mouseOverY + mouseOverFontHeight;
				if(showY + docFrameSize.height > screenSize.height){
					showY = mouseOverY - docFrameSize.height;
				}
				if(showX + docFrameSize.width > screenSize.width){
					showX = mouseOverX - docFrameSize.width;
				}
			}else{
				codeFrame.getSize(codeFrameSize);
				showX = codeFrame.getX();
				showY = codeFrame.getY();
				if(showX + codeFrameSize.width + docFrameSize.width > screenSize.width){
					if(showX - docFrameSize.width < 0){
						if (layoutLimit.isNotUpLayout == false 
								&& (layoutLimit.isNotDownLayout 
										|| showY + codeFrameSize.height + docFrameSize.height > screenSize.height)
							){
							showY -= docFrameSize.height;//上置
						}else{
							showY += codeFrameSize.height;//下置
						}
					}else{
						showX -= docFrameSize.width;//左置
					}
				}else{
					showX += codeFrameSize.width;//自然右置
				}
			}
			docFrame.setLocation(showX, showY);
			synchronized (docFrame) {
				if(isWillShowDoc){
					docFrame.setVisible(true);
				}
			}
		}
	};
	
	public final void release(){
		docFrame.dispose();
	}
	
	public final boolean isShowing(){
		return docFrame.isVisible();
	}
	
	public final void setInvisible(){
		synchronized (docFrame) {
			
			if(docFrame.isVisible()){
				docFrame.setVisible(false);
			}
			isWillShowDoc = false;
		}
	}
	
	public final void popDocTipWindow(final JFrame codeWindow, String fmClass, String fieldOrMethodName, final int type,
			final DocLayoutLimit layoutLimit){
		final boolean isForClassDoc = (type==CodeItem.TYPE_CLASS);
		
		//支持类的doc描述
		fmClass = isForClassDoc?fieldOrMethodName:fmClass;
		if(fmClass == null){
			setInvisible();
		}

		fieldOrMethodName = isForClassDoc?CLASS_DESC:fieldOrMethodName;
		
		this.codeFrame = codeWindow;
		this.layoutLimit = layoutLimit;
		
		final String doc = getDoc(fmClass, fieldOrMethodName);
		if(doc != null && doc.length() > 0){
			docPane.setText("<html><body style=\"background-color:#FAFBC5\">" + doc +"</body></html>");
			isWillShowDoc = true;
			SwingUtilities.invokeLater(repainRunnable);
		}else{
			if(L.isInWorkshop){
				L.V = L.O ? false : LogManager.log("fail to get doc about : " + fmClass + "/" + fieldOrMethodName);
			}
			setInvisible();
		}
	}

	public final boolean acceptType(final int type) {
		return type == CodeItem.TYPE_FIELD || type == CodeItem.TYPE_METHOD || type == CodeItem.TYPE_CLASS;
	}
	
	/**
	 * 如果没有相应文档，则返回null或空串
	 * @param claz
	 * @param fieldOrMethod 如：getFreeMessage(String)，没有实参
	 * @return
	 */
	public final String getDoc(final String claz, final String fieldOrMethod){
//		L.V = L.O ? false : LogManager.log("class : " + claz + ", fieldOrMethod : " + fieldOrMethod);
		
		HashMap<String, String> map = cache.get(claz);
		
		if(map == null){
			try{
				Thread.sleep(ThreadPriorityManager.UI_WAIT_MS);//不能采用Notify技术，因为有可能不是目标claz装入
			}catch (final Exception e) {
			}
			map = cache.get(claz);//等待异步线程完成doc内容
			if(map == null){
				return null;
			}
		}
		
		if(L.isInWorkshop){
			final Set<String> set = map.keySet();
			final Iterator<String> it = set.iterator();
			while(it.hasNext()){
				L.V = L.O ? false : LogManager.log("==>" + it.next());
			}
		}
		
		return map.get(fieldOrMethod);
	}
	
	public static void processDoc(final Class c, final boolean processInDelay){
		if(resetTimer.isEnable()){
		}else{
			resetTimer.setEnable(true);
		}
		resetTimer.resetTimerCount();
		
		final String clasName = c.getName();
		if(clasName.indexOf('$') > 0){
			return;
		}
		if(cache.containsKey(clasName)){
			return;
		}
		if(processInDelay){
			ContextManager.getThreadPool().run(new Runnable() {
				@Override
				public void run() {
					processDoc(c, clasName, false);
				}
			});
		}else{
			processDoc(c, clasName, true);
		}
	}
	
	private static void processDocForOneLevel(final String clasName){
//		System.out.println("-----processDocForOneLevel : " + claz.getName());

		if(clasName.startsWith("java")){
			read(clasName, "hc/res/docs/jdk_docs/api/" + clasName.replace('.', '/') + ".html");
		}else if(clasName.startsWith("hc.")){
			read(clasName, "hc/res/docs/" + clasName.replace('.', '/') + ".html");
		}
	}

	private static void read(final String clasName, final String docPath) {
		final InputStream in = ResourceUtil.getResourceAsStream(docPath);
		if(in == null){
			synchronized (cache) {
				cache.put(clasName, new HashMap<String, String>());
			}
			return;
		}
		final ByteArrayOutputStream outStream = new ByteArrayOutputStream();  
		final int BUFFER_SIZE = 4096;
		final byte[] data = new byte[BUFFER_SIZE];  
		int count = -1;  
		try{
		    while((count = in.read(data,0,BUFFER_SIZE)) != -1)  
		        outStream.write(data, 0, count);  
		    final String docContent = new String(outStream.toByteArray(), IConstant.UTF_8);
		    toCache(clasName, docContent);
		}catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
			synchronized (cache) {
		    	cache.put(clasName, new HashMap<String, String>());
			}
		}
		return;
	}
	

	private static final int GROUP_IDX = 2;
	private static final String blockListLast = "<ul class=\"blockListLast\">";
	private static final Pattern jianKuoHao = Pattern.compile("\\<(.*?)\\>");
	private static final Pattern generics_e_type = Pattern.compile("&lt;(.*?)&gt;");//不能(.*)
	private static final Pattern generics_e_to_object_pattern = Pattern.compile("\\b([A-Z]{1})\\b");
	private static final Pattern blockPattern = Pattern.compile("<ul class=\"(blockListLast|blockList)\">\n<li class=\"blockList\">\n(.*?)</li>\n</ul>\n", Pattern.DOTALL);
	private static final Pattern fieldOrMethodNamePattern = Pattern.compile("<pre>(.*?)</pre>", Pattern.DOTALL);//javaDoc method throws Exception有换行现象
	private static final Pattern classDescPattern = Pattern.compile(
			"<div class=\"description\">\n<ul class=\"blockList\">\n<li class=\"blockList\">\n" +//<hr>\n<br>\n
			"(.*?)" +
			"\n</li>\n</ul>\n</div>\n", Pattern.DOTALL);
	
	public static final String CLASS_DESC = "CLASS_DESC";
	
	public static final String NULL_CONSTRUCTOR_SIMPLE_CLASS_NAME = null;
	
	private static void processDoc(final HashMap<String, String> docMap, final String docContent, final String simpleClassName){
		{
			final Matcher matcher = classDescPattern.matcher(docContent);
			if (matcher.find()) {
				String doc = matcher.group(1);
				final String hr = "<hr>\n<br>\n";
				final int hrIdx = doc.indexOf(hr, 0);
				if(hrIdx >= 0){
					doc = doc.substring(hrIdx + hr.length());
				}
				docMap.put(CLASS_DESC, doc);
			}
		}
		
		{
			final int constructorDocIdx = docContent.indexOf("<h3>Constructor Detail</h3>");
			if(constructorDocIdx > 0){
				processListBlock(docContent.substring(constructorDocIdx), docMap, true, simpleClassName);
			}
		}
		
		{
			final int detailDocIdx = docContent.indexOf("<h3>Field Detail</h3>");
			if(detailDocIdx > 0){
				processListBlock(docContent.substring(detailDocIdx), docMap, false, NULL_CONSTRUCTOR_SIMPLE_CLASS_NAME);
			}
		}
		
		{
			final int detailDocIdx = docContent.indexOf("<h3>Method Detail</h3>");
			if(detailDocIdx > 0){
				processListBlock(docContent.substring(detailDocIdx), docMap, true, NULL_CONSTRUCTOR_SIMPLE_CLASS_NAME);
			}
		}
	}

	private static void processListBlock(final String docContent, final HashMap<String, String> docMap, final boolean isForMethod,
			final String simpleClassName) {
		{
			final Matcher matcher = blockPattern.matcher(docContent);
			while (matcher.find()) {
				final String match = matcher.group(GROUP_IDX);
				processItem(docMap, match, isForMethod, simpleClassName);
				if(matcher.group().startsWith(blockListLast)){
					return;
				}
			}
		}
	}

	private static void processItem(final HashMap<String, String> docMap, final String item, final boolean isForMethod,
			final String simpleClassName) {
		final Matcher matchFieldOrMethodName = fieldOrMethodNamePattern.matcher(item);
		if(matchFieldOrMethodName.find()){
			String fieldOrMethodName = matchFieldOrMethodName.group(1);
			fieldOrMethodName = jianKuoHao.matcher(fieldOrMethodName).replaceAll("");
			fieldOrMethodName = generics_e_type.matcher(fieldOrMethodName).replaceAll("");
			fieldOrMethodName = generics_e_to_object_pattern.matcher(fieldOrMethodName).replaceAll("Object");
			
			if(isForMethod){
				final int kuohaoRightIdx = fieldOrMethodName.indexOf(")");
				if(kuohaoRightIdx != fieldOrMethodName.length() - 1){
					//右括号之后是回车，及throws XXXException
					fieldOrMethodName = fieldOrMethodName.substring(0, kuohaoRightIdx + 1);
				}
			}
			//将参数实名去掉
			fieldOrMethodName = fieldOrMethodName.replace("&nbsp;", " ");
			fieldOrMethodName = fieldOrMethodName.replace("\n", "");
			
			if(isForMethod){
				if(isForMethod && simpleClassName != NULL_CONSTRUCTOR_SIMPLE_CLASS_NAME){
					fieldOrMethodName = fieldOrMethodName.replace(simpleClassName, CodeHelper.JRUBY_NEW);//将构造方法转为new()方法
				}
				
				final int kuohaoLeftIdx = fieldOrMethodName.indexOf("(");
				String parameter = fieldOrMethodName.substring(kuohaoLeftIdx);
				parameter = parameter.replaceAll(" \\w+,", ",");//method(int hello, boolean yes) => method(int, boolean yes)
				parameter = parameter.replaceAll(" \\w+\\)", ")");//method(int, boolean yes) => method(int, boolean)//有可能boolean另起一行
				parameter = parameter.replace(" ", "");
				parameter = parameter.replace(",", ", ");
				final String frontPartWithName = fieldOrMethodName.substring(0, kuohaoLeftIdx);
				final int nameStartIdx = frontPartWithName.lastIndexOf(' ') + 1;
				
				fieldOrMethodName = frontPartWithName.substring(nameStartIdx) + parameter;
			}else{
				//去掉public final static...
				fieldOrMethodName = fieldOrMethodName.substring(fieldOrMethodName.lastIndexOf(" ") + 1);
			}
			
			if(docMap.containsKey(fieldOrMethodName) == false){
				final int indexOfDoc = item.indexOf("<div class=\"block\">");
				String apiDoc = "";
				if(indexOfDoc >= 0){
					apiDoc = item.substring(indexOfDoc);
				}
//				if(L.isInWorkshop){
//					System.out.println("item    :    " + fieldOrMethodName);
//					System.out.println(apiDoc + "\n\n");
//				}
				//getFreeMessage(String)
				docMap.put(fieldOrMethodName, apiDoc);
			}
		}
	}

	private static void toCache(final String clasName, final String docContent) {
		final HashMap<String, String> docMap = new HashMap<String, String>();
		if(docContent != null){
			final int classIdx = clasName.lastIndexOf(".");
			if(classIdx > 0){
				processDoc(docMap, docContent, clasName.substring(classIdx + 1));
			}else{
				processDoc(docMap, docContent, NULL_CONSTRUCTOR_SIMPLE_CLASS_NAME);
			}
		}
		synchronized (cache) {
			cache.put(clasName, docMap);
		}
	}

	/**
	 * 
	 * @param c
	 * @param clasName
	 * @param isNeedShiftToBackground true:superAndInterface must process in background; false: current thread
	 */
	private static void processDoc(final Class c, final String clasName, final boolean isNeedShiftToBackground) {
		if(cache.containsKey(clasName)){
			return;
		}
		
		processDocForOneLevel(clasName);

		if(isNeedShiftToBackground){
			ContextManager.getThreadPool().run(new Runnable() {
				@Override
				public void run() {
					processSuperAndInterfaces(c);
				}
			});
		}else{
			processSuperAndInterfaces(c);
		}
	}

	private static void processSuperAndInterfaces(final Class c) {
		final Class superclass = c.getSuperclass();
		if(superclass != null){
			processDoc(superclass, superclass.getName(), false);
		}
		final Class[] interfaces = c.getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			final Class claz = interfaces[i];
			processDoc(claz, claz.getName(), false);
		}
	}

}

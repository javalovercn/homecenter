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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

public class DocHelper {
	private static final int cssSize = 120;
	private static final HashMap<String, HashMap<String, String>> cache = new HashMap<String, HashMap<String,String>>();
	static final Vector<CodeItem> cssCodeItems = new Vector<CodeItem>(cssSize);
	static final HashMap<String, String> cssDocs = new HashMap<String,String>(cssSize);
	static final HashMap<String, String> cssProperties = new HashMap<String,String>(cssSize);
	static final Pattern douhaoSpaces = Pattern.compile(", {2,}");
	private static final HCTimer resetTimer = new HCTimer("", 1000 * 60 * 30, false) {
		@Override
		public final void doBiz() {
			if(LinkProjectStatus.getStatus() == LinkProjectStatus.MANAGER_DESIGN){
				resetTimerCount();
			}else{
				resetCache();
				setEnable(false);
			}
		}
	};

	static void resetCache() {
		synchronized (cache) {
			cache.clear();
		}
		synchronized (cssCodeItems) {
			CodeItem.cycle(cssCodeItems);
		}
		synchronized (cssDocs) {
			cssDocs.clear();
		}
		synchronized(cssProperties){
			cssProperties.clear();
		}
	}
	
	static final String bodyRule = "body { font-family:Dialog, Arial, Helvetica, sans-serif; font-size: " + DefaultManager.getDesignerDocFontSize() + "pt; }";//font-family: " + font.getFamily() + ";
	static final String strongRule = ".strong { font-weight:bold; font-size: " + DefaultManager.getDesignerDocFontSize() + "pt; }";//font-family: " + font.getFamily() + ";
	static final String aRule = "a { font-family:Dialog, Arial, Helvetica, sans-serif; text-decoration:underline; color:blue; font-size: " + DefaultManager.getDesignerDocFontSize() + "pt; }";//font-family: " + font.getFamily() + ";
	static final String codeRule = "code { font-size: " + DefaultManager.getDesignerDocFontSize() + "pt; }";//font-family: " + font.getFamily() + ";
	static final String preRule = "pre { font-style:italic; font-family:Dialog, Arial, Helvetica, sans-serif; font-size: " + DefaultManager.getDesignerDocFontSize() + "pt; }";//font-family: " + font.getFamily() + ";
	static final String background_color = "#FAFBC5";
	static final Color bg_color = Color.decode(background_color);
	
	private JFrame codeFrame;
	private DocLayoutLimit layoutLimit;
	private final JEditorPane docPane = new JEditorPane();
	private final JScrollPane scrollPanel = new JScrollPane(docPane);//不能使用NEVER，因为内容可能含图片
	private final JFrame docFrame = new JFrame("");
	public final CodeHelper codeHelper;
	public boolean isForMouseOverTip;
	public int mouseOverX, mouseOverY, mouseOverFontHeight;
	private CodeItem currItem;
	boolean isEntered = false;

	private final HCTimer readyMouseExitTimer = new HCTimer("", HCTimer.HC_INTERNAL_MS * 2, false) {
		@Override
		public void doBiz() {
			isEntered = false;
			codeHelper.notifyUsingByDoc(false);
			setEnable(false);
		}
	};
	
	private final void disableReadyMouseExitTimer(){
		readyMouseExitTimer.setEnable(false);
	}
	
	private final void enableReadyMouseExitTimer(){
		readyMouseExitTimer.resetTimerCount();
		readyMouseExitTimer.setEnable(true);
	}
	
	DocHelper(final CodeHelper codeHelper, final CodeWindow codeWindow) {
		this.codeHelper = codeHelper;
		
		docPane.addKeyListener(codeWindow.keyListener);
		docPane.setBorder(new EmptyBorder(4, 4, 4, 4));
		docPane.setBackground(bg_color);
		docPane.setContentType("text/html");
	    final StyleSheet styleSheet = ((HTMLDocument)docPane.getDocument()).getStyleSheet();
	    final MouseListener scrollBarListener = new MouseListener() {
			@Override
			public void mouseReleased(final MouseEvent e) {
			}
			
			@Override
			public void mousePressed(final MouseEvent e) {
			}
			
			@Override
			public void mouseExited(final MouseEvent e) {
				enableReadyMouseExitTimer();
			}
			
			@Override
			public void mouseEntered(final MouseEvent e) {
				disableReadyMouseExitTimer();
			}
			
			@Override
			public void mouseClicked(final MouseEvent e) {
			}
		};
		scrollPanel.getHorizontalScrollBar().addMouseListener(scrollBarListener);
		scrollPanel.getVerticalScrollBar().addMouseListener(scrollBarListener);
		
		docPane.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(final MouseEvent e) {
			}
			
			@Override
			public void mousePressed(final MouseEvent e) {
			}
			
			@Override
			public void mouseExited(final MouseEvent e) {
				enableReadyMouseExitTimer();
			}
			
			@Override
			public void mouseEntered(final MouseEvent e) {
				if(isEntered == false){
					codeHelper.notifyUsingByDoc(true);
					isEntered = true;
				}
				disableReadyMouseExitTimer();
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
			
			private static final String PROPDEF = "propdef-";

			private final void processCssDoc(final SimpleAttributeSet value){
				 final String href = (String) value.getAttribute(HTML.Attribute.HREF);
	                if (href != null) {//media.html#visual-media-group 或 colors.html#propdef-background-color
	                	try {
	                		final int sectIdx = href.indexOf('#');
	                		final boolean hasSect = sectIdx >= 0;
//	                		final String fileName = hasSect?href.substring(0, sectIdx):href;
							if(hasSect){
								final String fragment = href.substring(sectIdx + 1);
								if(fragment.startsWith(PROPDEF)){
									final String propName = fragment.substring(PROPDEF.length());
									final String doc = CSSHelper.getDocs(propName);
									if(doc != null){
										SwingUtilities.invokeLater(new Runnable() {
											@Override
											public void run() {
							        			loadDoc(doc);
							        			docPane.setCaretPosition(0);
											}
										});
										return;
									}
								}
							}
         				HttpUtil.browse(CSSHelper.getCSSDocWebURL(href));
						} catch (final Exception e1) {
							e1.printStackTrace();
						}
//	                    LogManager.log("click href : " + href);
	                }
			}
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
		            	if(currItem.isCSSProperty){
		            		processCssDoc(value);
		            	}else{
		            		processJavaDoc(value);
		            	}
		            }
		        }
			}

			final void processJavaDoc(final SimpleAttributeSet value) {
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
						if(fmClass.equals(CodeHelper.j2seStringClass.getName())){
							return;
						}
						
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
							CodeHelper.buildForClass(codeHelper, null, Class.forName(fmClass, false, classLoader));
						} catch (final ClassNotFoundException e1) {
						}
						
						final String doc = getDoc(fmClass, fieldOrMethodName);
						if(doc != null && doc.length() > 0){
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
				        			loadDoc(doc);
				        			docPane.setCaretPosition(0);
								}
							});
						}else{
							if(href.startsWith("http")){
								HttpUtil.browse(href);
							}
						}
					}
//		                    LogManager.log("click href : " + href);
				}
			}
		});
		
		styleSheet.addRule(bodyRule);
		styleSheet.addRule(strongRule);
		styleSheet.addRule(aRule);
		styleSheet.addRule(preRule);
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
					if(L.isInWorkshop){
						LogManager.log("[CodeTip] docFrame.setVisible(true).");
					}
				}else{
					if(L.isInWorkshop){
						LogManager.log("[CodeTip] keep docFrame visible as old.");
					}
				}
			}
		}
	};
	
	public final void release(){
		docFrame.dispose();
		HCTimer.remove(readyMouseExitTimer);
	}
	
	public final boolean isShowing(){
		return docFrame.isVisible();
	}
	
	public final void setInvisible(){
		synchronized (docFrame) {
			if(docFrame.isVisible()){
				docFrame.setVisible(false);
				if(L.isInWorkshop){
					ClassUtil.printCurrentThreadStack("[CodeTip] docFrame.setVisible(false)");
				}
			}
			isWillShowDoc = false;
		}
	}
	
	public final void popDocTipWindow(final CodeItem item, final JFrame codeWindow, String fmClass, String fieldOrMethodName, final int type,
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
		this.currItem = item;
		
		final String doc = getDoc(fmClass, fieldOrMethodName);
		if(doc != null && doc.length() > 0){
			loadDoc(doc);
			isWillShowDoc = true;
			SwingUtilities.invokeLater(repainRunnable);
		}else{
			if(L.isInWorkshop){
				LogManager.log("[CodeTip] fail to get doc and setInvisible() about : " + fmClass + "/" + fieldOrMethodName);
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
//		LogManager.log("class : " + claz + ", fieldOrMethod : " + fieldOrMethod);
		if(currItem.isCSSProperty){
			return CSSHelper.getDocs(fieldOrMethod);
		}
		
		synchronized (cache) {
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
					LogManager.log("==>" + it.next());
				}
			}
			
			return map.get(fieldOrMethod);
		}
	}
	
	private final void loadDoc(final String doc) {
		docPane.setText("<html><body style=\"background-color:" + background_color + "\">" + doc +"</body></html>");
	}

	public static void processDoc(final CodeHelper codeHelper, final Class c, final boolean processInDelay){
		resetClearTimer();
		
		final String clasName = c.getName();
		if(clasName.indexOf('$') > 0){
			return;
		}
		synchronized (cache) {
			if(cache.containsKey(clasName)){
				return;
			}
		}
//		if(processInDelay){
//			ContextManager.getThreadPool().run(new Runnable() {
//				@Override
//				public void run() {
//					processDoc(codeHelper, c, clasName, false);
//				}
//			});
//		}else{
			processDoc(codeHelper, c, clasName, false);
//		}
	}

	static void resetClearTimer() {
		if(resetTimer.isEnable()){
		}else{
			resetTimer.setEnable(true);
		}
		resetTimer.resetTimerCount();
	}
	
	private static void processDocForOneLevel(final CodeHelper codeHelper, final Class c, final String clasName){
//		System.out.println("-----processDocForOneLevel : " + claz.getName());

		if(CodeHelper.isRubyClass(c)){
			if(clasName.equals(CodeHelper.j2seStringClass.getName())){
				read(codeHelper, true, clasName, ResourceUtil.getResourceAsStream("hc/res/docs/ruby/RubyString2_2_0.htm"));
			}
			return;
		}
		
		final String className = clasName.replace('.', '/');
		
		if(clasName.startsWith(CodeHelper.JAVA_PACKAGE_CLASS_PREFIX)){
			read(codeHelper, false, clasName, J2SEDocHelper.getDocStream(buildClassDocPath(className)));
		}else if(clasName.startsWith(CodeHelper.HC_PACKAGE_CLASS_PREFIX)){
			read(codeHelper, false, clasName, ResourceUtil.getResourceAsStream("hc/res/docs/" + className + ".html"));
		}
	}
	
	public static String buildClassDocPath(final String clasName) {
		return "hc/res/docs/jdk_docs/api/" + clasName + ".html";
	}

	private static void read(final CodeHelper codeHelper, final boolean isRubyClass, final String clasName, final InputStream in) {
		
		if(in == null){
			if(J2SEDocHelper.isBuildIn() == false && J2SEDocHelper.isJ2SEDocReady() == false){
				return;
			}
			
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
		    toCache(codeHelper, isRubyClass, clasName, docContent);
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
	private static final Pattern rubyStringMethodsList = Pattern.compile("<div id=\"method-list-section\" class=\"section\">" +
			"(.*?)" +
			"</div>", Pattern.DOTALL);
	private static final Pattern rubyStringMethodItem = Pattern.compile("<li><a href=\"#(.*?)\">#(.*?)</a></li>");
	
	public static final String CLASS_DESC = "CLASS_DESC";
	
	public static final String NULL_CONSTRUCTOR_SIMPLE_CLASS_NAME = null;
	
	private static void processRubyDoc(final HashMap<String, String> docMap, final String docContent, final String simpleClassName){
		final Matcher matcher = rubyStringMethodsList.matcher(docContent);
		if (matcher.find()) {
			final String method_i = "method-i-";
			final String removeTail = "<div class=\"method-source-code\"";

			final String match = matcher.group();
			final Matcher itemMatcher = rubyStringMethodItem.matcher(match);
			while(itemMatcher.find()){
				final String locateID = itemMatcher.group(1);
				final String fieldOrMethodName = itemMatcher.group(2);
				
				final String startStr = "<a name=\"" + locateID + "\"></a>";
				final String endStr = "</div><!-- " + locateID.substring(method_i.length()) + "-method -->";
				
				final int cutStartIdx = docContent.indexOf(startStr) + startStr.length();
				final int cutEndIdx = docContent.indexOf(removeTail, cutStartIdx);
				final int cutMaxEndIdx = docContent.indexOf(endStr, cutStartIdx);
				
				final String doc = docContent.substring(cutStartIdx, cutMaxEndIdx<cutEndIdx?cutMaxEndIdx:cutEndIdx);
//				System.out.println("\n\nruby method : " + fieldOrMethodName + "\n" + doc);
				docMap.put(fieldOrMethodName, doc);
			}
		}
	}
	
	private static void processDoc(final CodeHelper codeHelper, final String clasName, final HashMap<String, String> docMap, 
			final String docContent, final String simpleClassName){
		{
			final Matcher matcher = classDescPattern.matcher(docContent);
			if (matcher.find()) {
				String doc = matcher.group(1);
				final String hr = "<hr>\n<br>\n";
				final int hrIdx = doc.indexOf(hr, 0);
				if(hrIdx >= 0){
					doc = doc.substring(hrIdx + hr.length());
				}
				doc = doc.replaceFirst("<pre>", "<strong>");
				doc = doc.replaceFirst("</pre>", "</strong><BR><BR>");
				docMap.put(CLASS_DESC, doc);
			}
		}
		
		{
			final int constructorDocIdx = docContent.indexOf("<h3>Constructor Detail</h3>");
			if(constructorDocIdx > 0){
				processListBlock(codeHelper, clasName, docContent.substring(constructorDocIdx), docMap, true, simpleClassName);
			}
		}
		
		{
			final int detailDocIdx = docContent.indexOf("<h3>Field Detail</h3>");
			if(detailDocIdx > 0){
				processListBlock(codeHelper, clasName, docContent.substring(detailDocIdx), docMap, false, NULL_CONSTRUCTOR_SIMPLE_CLASS_NAME);
			}
		}
		
		{
			final int detailDocIdx = docContent.indexOf("<h3>Method Detail</h3>");
			if(detailDocIdx > 0){
				processListBlock(codeHelper, clasName, docContent.substring(detailDocIdx), docMap, true, NULL_CONSTRUCTOR_SIMPLE_CLASS_NAME);
			}
		}
	}

	private static void processListBlock(final CodeHelper codeHelper, final String clasName, final String docContent, 
			final HashMap<String, String> docMap, final boolean isForMethod, final String simpleClassName) {
		{
			final Matcher matcher = blockPattern.matcher(docContent);
			while (matcher.find()) {
				final String match = matcher.group(GROUP_IDX);
				processItem(codeHelper, clasName, docMap, match, isForMethod, simpleClassName);
				if(matcher.group().startsWith(blockListLast)){
					return;
				}
			}
		}
	}
	
	/**
	 * (java.lang.String hello, int j, boolean[] yes) => (hello, j, yes)
	 * () => ()
	 * @param method
	 * @return
	 */
	private static String buildCodeParameterList(String parameter){
		parameter = parameter.replaceAll(", [\\w\\.\\[\\]]+ ", ", ");//method(java.lang.String hello, boolean[] yes) => method(String, boolean yes)
		parameter = parameter.replaceAll("\\([\\w\\.\\[\\]]+ ", "(");//method(int, boolean yes) => method(int, boolean)//有可能boolean另起一行
		parameter = parameter.replace(" ", "");//去掉多余空格
		return parameter.replace(",", ", ");
	}
	
	private static void replaceCodeParameter(final CodeHelper codeHelper, final String clasName, final String methodForDoc, 
			final String codeParameterList){
		ArrayList<CodeItem> list = codeHelper.classCacheMethodAndPropForClass.get(clasName);
		 if(list != null && replaceCode(methodForDoc, codeParameterList, list)){
			 return;
		 }
		 
		 list = codeHelper.classCacheMethodAndPropForInstance.get(clasName);
		 if(list != null){
			 replaceCode(methodForDoc, codeParameterList, list);
		 }
	}

	private static boolean replaceCode(final String methodForDoc, final String codeParameterList, final ArrayList<CodeItem> list) {
		final int size = list.size();
		for (int i = 0; i < size; i++) {
			final CodeItem item = list.get(i);
			if(methodForDoc.equals(item.codeForDoc)){
				item.code = item.fieldOrMethodOrClassName + codeParameterList;
				return true;
			}
		}
		return false;
	}

	private static void processItem(final CodeHelper codeHelper, final String clasName, final HashMap<String, String> docMap, 
			final String item, final boolean isForMethod, final String simpleClassName) {
		final Matcher matchFieldOrMethodName = fieldOrMethodNamePattern.matcher(item);
		if(matchFieldOrMethodName.find()){
			String fieldOrMethodName = matchFieldOrMethodName.group(1);
			final String fieldOrMethodNameWithBR = "<strong>" + fieldOrMethodName + "</strong><BR><BR>";
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
			fieldOrMethodName = douhaoSpaces.matcher(fieldOrMethodName).replaceAll(", ");//wait(int a,                long ms) => wait(int a, long ms)
			
			if(isForMethod){
				if(isForMethod && simpleClassName != NULL_CONSTRUCTOR_SIMPLE_CLASS_NAME){
					fieldOrMethodName = fieldOrMethodName.replace(simpleClassName, CodeHelper.JRUBY_NEW);//将构造方法转为new()方法
				}
				
				final int kuohaoLeftIdx = fieldOrMethodName.indexOf("(");
				String parameter = fieldOrMethodName.substring(kuohaoLeftIdx);
				final String codeParameterList = buildCodeParameterList(parameter);
				parameter = parameter.replaceAll("[\\w]+\\.", "");//method(java.lang.String hello, boolean yes) => method(String hello, boolean yes)
				parameter = parameter.replaceAll(" \\w+,", ",");//method(int hello, boolean yes) => method(int, boolean yes)
				parameter = parameter.replaceAll(" \\w+\\)", ")");//method(int, boolean yes) => method(int, boolean)//有可能boolean另起一行
				parameter = parameter.replace(" ", "");
				parameter = parameter.replace(",", ", ");
				final String frontPartWithName = fieldOrMethodName.substring(0, kuohaoLeftIdx);
				final int nameStartIdx = frontPartWithName.lastIndexOf(' ') + 1;
				
				fieldOrMethodName = frontPartWithName.substring(nameStartIdx) + parameter;
				
				replaceCodeParameter(codeHelper, clasName, fieldOrMethodName, codeParameterList);
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
				String formatMethodDocContent = fieldOrMethodNameWithBR.replace("\n", " ").replace("&nbsp;", " ").replaceAll("[ ]{2,}", " ");//消除换行和多个空格
				formatMethodDocContent = formatMethodDocContent.replaceAll("java\\.lang\\.String", "String");
				formatMethodDocContent = formatMethodDocContent.replaceAll("java\\.lang\\.Object", "Object");
				docMap.put(fieldOrMethodName, formatMethodDocContent + apiDoc);
			}
		}
	}

	private static void toCache(final CodeHelper codeHelper, final boolean isRubyClass, final String clasName, final String docContent) {
		final HashMap<String, String> docMap = new HashMap<String, String>();
		if(docContent != null){
			final int classIdx = clasName.lastIndexOf(".");
			if(isRubyClass){
				processRubyDoc(docMap, docContent, clasName.substring(classIdx + 1));
			}else{
				if(classIdx > 0){
					processDoc(codeHelper, clasName, docMap, docContent, clasName.substring(classIdx + 1));
				}else{
					processDoc(codeHelper, clasName, docMap, docContent, NULL_CONSTRUCTOR_SIMPLE_CLASS_NAME);
				}
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
	private static void processDoc(final CodeHelper codeHelper, final Class c, final String clasName, 
			final boolean isNeedShiftToBackground) {
		synchronized (cache) {
			if(cache.containsKey(clasName)){
				return;
			}
		}
		
		processDocForOneLevel(codeHelper, c, clasName);
		
		if(CodeHelper.isRubyClass(c)){
			return;
		}

		if(isNeedShiftToBackground){
			ContextManager.getThreadPool().run(new Runnable() {
				@Override
				public void run() {
					processSuperAndInterfaces(codeHelper, c);
				}
			});
		}else{
			processSuperAndInterfaces(codeHelper, c);
		}
	}

	private static void processSuperAndInterfaces(final CodeHelper codeHelper, final Class c) {
		final Class superclass = c.getSuperclass();
		if(superclass != null){
			processDoc(codeHelper, superclass, superclass.getName(), false);
		}
		final Class[] interfaces = c.getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			final Class claz = interfaces[i];
			processDoc(codeHelper, claz, claz.getName(), false);
		}
	}

}

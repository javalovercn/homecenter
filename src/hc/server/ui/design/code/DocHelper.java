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
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
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
	
	static final String bodyRule = "body { font-size: " + DefaultManager.getDesignerDocFontSize() + "pt; }";//font-family: " + font.getFamily() + ";
	static final String strongRule = "strong { font-size: " + DefaultManager.getDesignerDocFontSize() + "pt; }";//font-family: " + font.getFamily() + ";
	static final String aRule = "a { text-decoration:underline; color:blue; font-size: " + DefaultManager.getDesignerDocFontSize() + "pt; }";//font-family: " + font.getFamily() + ";
	static final String codeRule = "code { font-size: " + DefaultManager.getDesignerDocFontSize() + "pt; }";//font-family: " + font.getFamily() + ";

	private JFrame codeFrame;
	private DocLayoutLimit layoutLimit;
	private final JEditorPane docPane = new JEditorPane();
	private final JScrollPane scrollPanel = new JScrollPane(docPane);
	private final JFrame docFrame = new JFrame("");
	
	DocHelper(final CodeWindow codeWindow) {
		docPane.addKeyListener(codeWindow.keyListener);
		
		docPane.setContentType("text/html");
	    final StyleSheet styleSheet = ((HTMLDocument)docPane.getDocument()).getStyleSheet();
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

	private final Runnable repainRunnable = new Runnable() {
		@Override
		public void run() {
			docFrame.validate();
			ClassUtil.revalidate(docFrame);
			docFrame.pack();

			docFrame.getSize(docFrameSize);
			codeFrame.getSize(codeFrameSize);
			
					
			int showX = codeFrame.getX();
			int showY = codeFrame.getY();
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
			
			docFrame.setLocation(showX, showY);
			docFrame.setVisible(true);
		}
	};
	
	public final void release(){
		docFrame.dispose();
	}
	
	public final boolean isShowing(){
		return docFrame.isVisible();
	}
	
	public final void setInvisible(){
		if(docFrame.isVisible()){
			docFrame.setVisible(false);
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
			read(clasName, "hc/server/docs/jdk_docs/api/" + clasName.replace('.', '/') + ".html");
		}else if(clasName.startsWith("hc.")){
			read(clasName, "hc/server/docs/" + clasName.replace('.', '/') + ".html");
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
	private static final Pattern blockPattern = Pattern.compile("<ul class=\"(blockListLast|blockList)\">\n<li class=\"blockList\">\n(.*?)</dl>\n</li>\n</ul>\n", Pattern.DOTALL);
	private static final Pattern blockPatternWithoutDL = Pattern.compile("<ul class=\"(blockListLast|blockList)\">\n<li class=\"blockList\">\n(.*?)</li>\n</ul>\n", Pattern.DOTALL);
	private static final Pattern fieldOrMethodNamePattern = Pattern.compile("<pre>(.*?)</pre>", Pattern.DOTALL);//javaDoc method throws Exception有换行现象
	private static final Pattern classDescPattern = Pattern.compile(
			"<div class=\"description\">\n<ul class=\"blockList\">\n<li class=\"blockList\">\n" +//<hr>\n<br>\n
			"(.*?)" +
			"\n</li>\n</ul>\n</div>\n", Pattern.DOTALL);
	
	public static final String CLASS_DESC = "CLASS_DESC";
	
	private static void processDoc(final HashMap<String, String> docMap, String docContent){
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
		
//		<ul class="blockList|Last">
//		<li class="blockList">
//		</li>
//		</ul>
		final int detailDocIdx = docContent.indexOf("<h3>Method Detail</h3>");
		if(detailDocIdx < 0){
			//没有方法和属性
			return;
		}
		
		docContent = docContent.substring(detailDocIdx);//否则会从Method Summary段开始，导致第一个不正确
		{
			final Matcher matcher = blockPattern.matcher(docContent);
			while (matcher.find()) {
				processItem(docMap, matcher.group(GROUP_IDX) + "</dl>");
			}
		}
		{
			final Matcher matcher = blockPatternWithoutDL.matcher(docContent);
			while (matcher.find()) {
				processItem(docMap, matcher.group(GROUP_IDX));
			}
		}
	}

	private static void processItem(final HashMap<String, String> docMap,
			final String item) {
		final Matcher matchFieldOrMethodName = fieldOrMethodNamePattern.matcher(item);
		if(matchFieldOrMethodName.find()){
			String fieldOrMethodName = matchFieldOrMethodName.group(1);
			fieldOrMethodName = fieldOrMethodName.replaceAll("\\<(.*?)\\>", "");
			final int kuohaoRightIdx = fieldOrMethodName.indexOf(")");
			if(kuohaoRightIdx != fieldOrMethodName.length() - 1){
				//右括号之后是回车，及throws XXXException
				fieldOrMethodName = fieldOrMethodName.substring(0, kuohaoRightIdx + 1);
			}
			//将参数实名去掉
			fieldOrMethodName = fieldOrMethodName.replace("&nbsp;", " ");
			fieldOrMethodName = fieldOrMethodName.replace("\n", "");
			final int kuohaoLeftIdx = fieldOrMethodName.indexOf("(");
			String parameter = fieldOrMethodName.substring(kuohaoLeftIdx);
			parameter = parameter.replaceAll(" \\w+,", ",");//method(int hello, boolean yes) => method(int, boolean yes)
			parameter = parameter.replaceAll(" \\w+\\)", ")");//method(int, boolean yes) => method(int, boolean)//有可能boolean另起一行
			parameter = parameter.replace(" ", "");
			parameter = parameter.replace(",", ", ");
			final String frontPartWithName = fieldOrMethodName.substring(0, kuohaoLeftIdx);
			final int nameStartIdx = frontPartWithName.lastIndexOf(' ') + 1;
			
			fieldOrMethodName = frontPartWithName.substring(nameStartIdx) + parameter;
		
			if(docMap.containsKey(fieldOrMethodName) == false){
				final int indexOfDoc = item.indexOf("<div class=\"block\">");
				String apiDoc = "";
				if(indexOfDoc >= 0){
					apiDoc = item.substring(indexOfDoc);
				}
//				if(L.isInWorkshop){
//					System.out.println("method    :    " + fieldOrMethodName);
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
			processDoc(docMap, docContent);
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

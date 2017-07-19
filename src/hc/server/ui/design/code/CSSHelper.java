package hc.server.ui.design.code;

import hc.core.util.LogManager;
import hc.util.ResourceUtil;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CSSHelper {
	private static final String CSS_BASE = "hc/res/docs/css2/";
	
	final static HashMap<String, String> getProperties(){
		synchronized (DocHelper.cssProperties) {
			final HashMap<String, String> props = DocHelper.cssProperties;
			if(props.size() != 0){
				return props;
			}
			
			props.put("azimuth", "aural.html");
			props.put("background", "colors.html");
			props.put("background-attachment", "colors.html");
			props.put("background-color", "colors.html");
			props.put("background-image", "colors.html");
			props.put("background-position", "colors.html");
			props.put("background-repeat", "colors.html");
			props.put("border", "box.html");
			props.put("border-bottom", "box.html");
			props.put("border-bottom-color", "box.html");
			props.put("border-bottom-style", "box.html");
			props.put("border-bottom-width", "box.html");
			props.put("border-collapse", "tables.html");
			props.put("border-color", "box.html");
			props.put("border-left", "box.html");
			props.put("border-left-color", "box.html");
			props.put("border-left-style", "box.html");
			props.put("border-left-width", "box.html");
			props.put("border-right", "box.html");
			props.put("border-right-color", "box.html");
			props.put("border-right-style", "box.html");
			props.put("border-right-width", "box.html");
			props.put("border-spacing", "tables.html");
			props.put("border-style", "box.html");
			props.put("border-top", "box.html");
			props.put("border-top-color", "box.html");
			props.put("border-top-style", "box.html");
			props.put("border-top-width", "box.html");
			props.put("border-width", "box.html");
			props.put("bottom", "visuren.html");
			props.put("caption-side", "tables.html");
			props.put("clear", "visuren.html");
			props.put("clip", "visufx.html");
			props.put("color", "colors.html");
			props.put("content", "generate.html");
			props.put("counter-increment", "generate.html");
			props.put("counter-reset", "generate.html");
			props.put("cue", "aural.html");
			props.put("cue-after", "aural.html");
			props.put("cue-before", "aural.html");
			props.put("cursor", "ui.html");
			props.put("direction", "visuren.html");
			props.put("display", "visuren.html");
			props.put("elevation", "aural.html");
			props.put("empty-cells", "tables.html");
			props.put("float", "visuren.html");
			props.put("font", "fonts.html");
			props.put("font-family", "fonts.html");
			props.put("font-size", "fonts.html");
			props.put("font-style", "fonts.html");
			props.put("font-variant", "fonts.html");
			props.put("font-weight", "fonts.html");
			props.put("height", "visudet.html");
			props.put("left", "visuren.html");
			props.put("letter-spacing", "text.html");
			props.put("line-height", "visudet.html");
			props.put("list-style", "generate.html");
			props.put("list-style-image", "generate.html");
			props.put("list-style-position", "generate.html");
			props.put("list-style-type", "generate.html");
			props.put("margin", "box.html");
			props.put("margin-bottom", "box.html");
			props.put("margin-left", "box.html");
			props.put("margin-right", "box.html");
			props.put("margin-top", "box.html");
			props.put("max-height", "visudet.html");
			props.put("max-width", "visudet.html");
			props.put("min-height", "visudet.html");
			props.put("min-width", "visudet.html");
			props.put("orphans", "page.html");
			props.put("outline", "ui.html");
			props.put("outline-color", "ui.html");
			props.put("outline-style", "ui.html");
			props.put("outline-width", "ui.html");
			props.put("overflow", "visufx.html");
			props.put("padding", "box.html");
			props.put("padding-bottom", "box.html");
			props.put("padding-left", "box.html");
			props.put("padding-right", "box.html");
			props.put("padding-top", "box.html");
			props.put("page-break-after", "page.html");
			props.put("page-break-before", "page.html");
			props.put("page-break-inside", "page.html");
			props.put("pause", "aural.html");
			props.put("pause-after", "aural.html");
			props.put("pause-before", "aural.html");
			props.put("pitch", "aural.html");
			props.put("pitch-range", "aural.html");
			props.put("play-during", "aural.html");
			props.put("position", "visuren.html");
			props.put("quotes", "generate.html");
			props.put("richness", "aural.html");
			props.put("right", "visuren.html");
			props.put("speak", "aural.html");
			props.put("speak-header", "aural.html");
			props.put("speak-numeral", "aural.html");
			props.put("speak-punctuation", "aural.html");
			props.put("speech-rate", "aural.html");
			props.put("stress", "aural.html");
			props.put("table-layout", "tables.html");
			props.put("text-align", "text.html");
			props.put("text-decoration", "text.html");
			props.put("text-indent", "text.html");
			props.put("text-transform", "text.html");
			props.put("top", "visuren.html");
			props.put("unicode-bidi", "visuren.html");
			props.put("vertical-align", "visudet.html");
			props.put("visibility", "visufx.html");
			props.put("voice-family", "aural.html");
			props.put("volume", "aural.html");
			props.put("white-space", "text.html");
			props.put("widows", "page.html");
			props.put("width", "visudet.html");
			props.put("word-spacing", "text.html");
			props.put("z-index", "visuren.html");
	
			buildPropCodeItems(props);
			
			return props;
		}
	}
	
	public static final String getCSSDocWebURL(final String fileNameWithFragment){
		return "https://www.w3.org/TR/CSS22/" + fileNameWithFragment;
	}
	
	final static void buildPropCodeItems(final HashMap<String, String> props){
		final Object[] keys = props.keySet().toArray();
		Arrays.sort(keys);
		
		final int size = keys.length;
		for (int j = 0; j < size; j++) {
			final CodeItem item = CodeItem.getFree();
			item.type = CodeItem.TYPE_FIELD;
			
			final String prop = (String)keys[j];
			item.code = prop;
			item.codeForDoc = item.code;
			item.codeDisplay = prop;
			item.codeLowMatch = prop.toLowerCase();
			item.isCSSProperty = true;
			
			synchronized (DocHelper.cssCodeItems) {
				DocHelper.cssCodeItems.add(item);
			}
		}
	}
	
	final static String getDocs(final String prop){
		DocHelper.resetClearTimer();
		
		synchronized (DocHelper.cssDocs) {
			final String doc = DocHelper.cssDocs.get(prop);
			if(doc != null){
				return doc;
			}
		}
		
		return processDoc(prop);
	}
	
	private static String processDoc(final String prop){
		final HashMap<String, String> map = getProperties();
		final String fileName = map.get(prop);
		if(fileName == null){
			LogManager.errToLog("[CSS] property [" + prop + "] is not exits!");
			return null;
		}
		
		final String data = loadData(ResourceUtil.getResourceAsStream(buildCSSFilePath(fileName)));
		synchronized (DocHelper.cssDocs) {
			readCssDoc(data, DocHelper.cssDocs);
			return DocHelper.cssDocs.get(prop);
		}
	}

	public static void main(final String[] args){
//		generateProperties();
		testCssDoc();
	}

	private static void testCssDoc() {
		final HashMap<String, String> cssDocs = new HashMap<String, String>(120);
		final String data = loadData(ResourceUtil.getResourceAsStream(buildCSSFilePath("box.html")));
		readCssDoc(data, cssDocs);
		System.out.println(cssDocs.get("margin"));
	}
	
	public static void generateProperties(){
		final Vector<String> fileNames = getAllFiles("indexlist.html");
		final int size = fileNames.size();
		final HashMap<String, String> propVector = new HashMap<String, String>(2048);
		
		for (int i = 0; i < size; i++) {
			final String fileName = fileNames.elementAt(i);
			final String data = loadData(ResourceUtil.getResourceAsStream(buildCSSFilePath(fileName)));
			readTabel(data, fileName, propVector);
		}
		
		final Object[] array = propVector.keySet().toArray();
		Arrays.sort(array);
		final int propSize = array.length;
		final HashMap<String, String> propDocVector = new HashMap<String, String>(propSize);
		System.out.println("total size : " + propSize);
		for (int i = 0; i < propSize; i++) {
			final Object item = array[i];
			final String fileName = propVector.get(item);
			System.out.println("props.put(\"" + item + "\", \"" + fileName + "\");");
			
			if(propDocVector.get(item) == null){
				final String data = loadData(ResourceUtil.getResourceAsStream(buildCSSFilePath(fileName)));
				readCssDoc(data, propDocVector);
			}
			
			if(propDocVector.get(item) == null){
				System.err.println("fail to get doc for : " + item);
				System.exit(0);
			}
		}
	}
	
	public static URL getCSSResource(final String fileName){
		return ResourceUtil.getResource(buildCSSFilePath(fileName));
	}
	
	private static String buildCSSFilePath(final String fileName){
		return CSS_BASE + fileName;
	}
	
	final static String defTable = "<table class=\"def propdef\">";
	final static Pattern pattern = Pattern.compile(defTable + "(.*?)</table>", Pattern.MULTILINE | Pattern.DOTALL);//注意：margin-top, margin-bottom共用一个
	final static Pattern prop_pattern = Pattern.compile("<dfn id=\"propdef-(.*?)\">(.*?)</dfn>");//相关联的多个可能排列在一起(比如cue-before, cue-after)，共用参数描述和example
//	final static Pattern item_pattern = Pattern.compile("<tr><th>(.*?):<td>(.*?)\n");
	final static Pattern patternDoc = Pattern.compile(defTable + "(.*?)(?=(<hr|\n\n<H2>|\n\n<h2>|\n\n<H3>|\n\n<h3>|" + defTable + "))", Pattern.MULTILINE | Pattern.DOTALL);//注意：margin-top, margin-bottom共用一个

	final static void readCssDoc(final String docContent, final HashMap<String, String> propVector){
		final Matcher matcher = patternDoc.matcher(docContent);
		while (matcher.find()) {
			final String doc = defTable + matcher.group(1);
			final Matcher p = prop_pattern.matcher(doc);
			
			while(p.find()){
				final String propertyName = p.group(2);
//				System.out.println("=>Property-name : " + propertyName + ", in " + fileName);
				if(propVector.containsKey(propertyName) == false){
					propVector.put(propertyName, doc);
				}
			}
		}
	}
	
	final static void readTabel(final String docContent, final String fileName, final HashMap<String, String> propVector){
		final Matcher matcher = pattern.matcher(docContent);
		while (matcher.find()) {
			final String prop = matcher.group(1);
			final Matcher p = prop_pattern.matcher(prop);
			
			while(p.find()){//注意：margin-top, margin-bottom共用一个
				final String propertyName = p.group(2);
//				System.out.println("=>Property-name : " + propertyName + ", in " + fileName);
				if(propVector.containsKey(propertyName) == false){
					propVector.put(propertyName, fileName);
				}
			}
//			final Matcher item = item_pattern.matcher(prop);
//			while(item.find()){
//				System.out.println("    " + item.group(1) + " : " + item.group(2));
//			}
		}
	}
	
	private static String loadData(final InputStream in) {
		try {
			final ByteArrayOutputStream outStream = new ByteArrayOutputStream(200 * 1024);  
			final int BUFFER_SIZE = 4096;
			final byte[] data = new byte[BUFFER_SIZE];  
			int count = -1;  
		    while((count = in.read(data,0,BUFFER_SIZE)) != -1)  
		        outStream.write(data, 0, count);  
		    return new String(outStream.toByteArray(), "UTF-8");
		} catch (final Throwable e1) {
			e1.printStackTrace();
		}finally{
			try{
				in.close();
			}catch (final Throwable e) {
			}
		}

		return null;
	}
	
	private static Vector<String> getAllFiles(final String fileName) {
		final InputStream in = ResourceUtil.getResourceAsStream(buildCSSFilePath(fileName));
		
		final Vector<String> out = new Vector<String>(50);

		final Pattern IDX_FILE_LIST = Pattern.compile("<a href=\"(.*?).html");
		final String[] excludeFile = {"about", "Overview", "sample", "intro"};
		
	    final String data = loadData(in);
	    final Matcher m = IDX_FILE_LIST.matcher(data);
	    while(m.find()){
	    	final String item = m.group(1);
	    	boolean isExclude = false;
	    	for (int i = 0; i < excludeFile.length; i++) {
				if(item.equals(excludeFile[i])){
					isExclude = true;
					break;
				}
			}
	    	if(isExclude == false){
	    		final String itemName = item + ".html";
	    		if(out.contains(itemName) == false){
	    			out.add(itemName);
	    		}
	    	}
	    }
	    
	    return out;
	}
	
}

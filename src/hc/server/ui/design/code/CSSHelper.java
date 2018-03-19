package hc.server.ui.design.code;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hc.core.util.ByteUtil;
import hc.util.ResourceUtil;

public class CSSHelper {
	public static final String CSS_BASE = "hc/res/docs/css2/";

	public static final String getCSSDocWebURL(final String fileNameWithFragment) {
		return "https://www.w3.org/TR/CSS22/" + fileNameWithFragment;
	}

	public static void main(final String[] args) {
		// generateProperties();
		testCssDoc();
	}

	private static void testCssDoc() {
		final HashMap<String, String> cssDocs = new HashMap<String, String>(120);
		final String data = loadData(ResourceUtil.getResourceAsStream(DocHelper.buildCSSFilePath("box.html")));
		readCssDoc(data, cssDocs);
		System.out.println(cssDocs.get("margin"));
	}

	public static void generateProperties() {
		final Vector<String> fileNames = getAllFiles("indexlist.html");
		final int size = fileNames.size();
		final HashMap<String, String> propVector = new HashMap<String, String>(2048);

		for (int i = 0; i < size; i++) {
			final String fileName = fileNames.elementAt(i);
			final String data = loadData(ResourceUtil.getResourceAsStream(DocHelper.buildCSSFilePath(fileName)));
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

			if (propDocVector.get(item) == null) {
				final String data = loadData(ResourceUtil.getResourceAsStream(DocHelper.buildCSSFilePath(fileName)));
				readCssDoc(data, propDocVector);
			}

			if (propDocVector.get(item) == null) {
				System.err.println("fail to get doc for : " + item);
				System.exit(0);
			}
		}
	}

	final static String defTable = "<table class=\"def propdef\">";
	final static Pattern pattern = Pattern.compile(defTable + "(.*?)</table>", Pattern.MULTILINE | Pattern.DOTALL);// 注意：margin-top,
																													// margin-bottom共用一个
	final static Pattern prop_pattern = Pattern.compile("<dfn id=\"propdef-(.*?)\">(.*?)</dfn>");// 相关联的多个可能排列在一起(比如cue-before,
																									// cue-after)，共用参数描述和example
																									// final static Pattern item_pattern =
																									// Pattern.compile("<tr><th>(.*?):<td>(.*?)\n");
	final static Pattern patternDoc = Pattern.compile(defTable + "(.*?)(?=(<hr|\n\n<H2>|\n\n<h2>|\n\n<H3>|\n\n<h3>|" + defTable + "))",
			Pattern.MULTILINE | Pattern.DOTALL);// 注意：margin-top,
																																													// margin-bottom共用一个

	final static void readCssDoc(final String docContent, final HashMap<String, String> propVector) {
		final Matcher matcher = patternDoc.matcher(docContent);
		while (matcher.find()) {
			final String doc = defTable + matcher.group(1);
			final Matcher p = prop_pattern.matcher(doc);

			while (p.find()) {
				final String propertyName = p.group(2);
				// System.out.println("=>Property-name : " + propertyName + ",
				// in " + fileName);
				if (propVector.containsKey(propertyName) == false) {
					propVector.put(propertyName, doc);
				}
			}
		}
	}

	final static void readTabel(final String docContent, final String fileName, final HashMap<String, String> propVector) {
		final Matcher matcher = pattern.matcher(docContent);
		while (matcher.find()) {
			final String prop = matcher.group(1);
			final Matcher p = prop_pattern.matcher(prop);

			while (p.find()) {// 注意：margin-top, margin-bottom共用一个
				final String propertyName = p.group(2);
				// System.out.println("=>Property-name : " + propertyName + ",
				// in " + fileName);
				if (propVector.containsKey(propertyName) == false) {
					propVector.put(propertyName, fileName);
				}
			}
			// final Matcher item = item_pattern.matcher(prop);
			// while(item.find()){
			// System.out.println(" " + item.group(1) + " : " + item.group(2));
			// }
		}
	}

	static String loadData(final InputStream in) {
		final byte[] bs = ResourceUtil.getContent(in, 0);
		if (bs == null) {
			return null;
		} else {
			return ByteUtil.bytesToStr(bs, 0, bs.length);
		}
	}

	private static Vector<String> getAllFiles(final String fileName) {
		final InputStream in = ResourceUtil.getResourceAsStream(DocHelper.buildCSSFilePath(fileName));

		final Vector<String> out = new Vector<String>(50);

		final Pattern IDX_FILE_LIST = Pattern.compile("<a href=\"(.*?).html");
		final String[] excludeFile = { "about", "Overview", "sample", "intro" };

		final String data = loadData(in);
		final Matcher m = IDX_FILE_LIST.matcher(data);
		while (m.find()) {
			final String item = m.group(1);
			boolean isExclude = false;
			for (int i = 0; i < excludeFile.length; i++) {
				if (item.equals(excludeFile[i])) {
					isExclude = true;
					break;
				}
			}
			if (isExclude == false) {
				final String itemName = item + ".html";
				if (out.contains(itemName) == false) {
					out.add(itemName);
				}
			}
		}

		return out;
	}

}

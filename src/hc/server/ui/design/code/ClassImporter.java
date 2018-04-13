package hc.server.ui.design.code;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import hc.core.util.StringUtil;
import hc.util.ResourceUtil;

public class ClassImporter {
	final char[][] lowNames;
	final ClassImportItem[] nameItems;
	final int size;
	final int[] firstCharStartIdx = new int[26];
	final String thirdLibNameMaybeNull;//null means J2SE or HC
	
	public static final int MIN_PRE_LEN = 2;
	
	public ClassImporter(final HashMap<String, ClassImportItem> map, final Vector<String> keyList, final String libName) {
		this.thirdLibNameMaybeNull = libName;
		
		{
			int idx = 0;
			for (char i = 'a'; i <= 'z'; i++) {
				firstCharStartIdx[idx++] = -1;
			}
		}
		
		Collections.sort(keyList);
		
		final int keySize = keyList.size();
		lowNames = new char[keySize][];
		nameItems = new ClassImportItem[keySize];
		size = keySize;
		
		final char charA = 'a';
		char firstChar = charA - 1;
		for (int i = 0; i < keySize; i++) {
			final String k = keyList.elementAt(i);
			final ClassImportItem ci = map.get(k);
			
			final char[] charNames = StringUtil.toLowerCase(k.toCharArray());
			final char c = charNames[0];
			if(c > firstChar) {
				firstCharStartIdx[c - charA] = i;
				firstChar = c;
			}
			lowNames[i] = charNames;
			nameItems[i] = ci;
		}
	}
	
	public final void appendClassImport(final CodeWindow windowMaybeNull, 
			final String preCodeLower, final char[] preCodeLowerChars, final int preCodeLowerCharsLen, 
			final ArrayList<CodeItem> vector, final HashSet<String> excludeImports) {
		if(preCodeLowerCharsLen < MIN_PRE_LEN) {
			return;
		}
		
		final char firstChar = preCodeLowerChars[0];
		
		if(firstChar >= 'a' && firstChar <= 'z') {//有可能输入@ctx
		}else {
			return;
		}
		
		try {
			final int startIdx = firstCharStartIdx[firstChar - 'a'];
			if(startIdx < 0) {
				return;
			}
			
			synchronized (excludeImports) {
				for (int i = startIdx; i < size; i++) {
					final char[] charNames = lowNames[i];
					
					if(charNames.length < preCodeLowerCharsLen) {
						continue;
					}
					
					int isBigger = 0;
					for (int j = 0; j < preCodeLowerCharsLen; j++) {
						final char c = preCodeLowerChars[j];
						isBigger = c - charNames[j];
						if(isBigger != 0) {
							break;
						}
					}
					
					if(isBigger == 0) {
						final ClassImportItem ci = nameItems[i];
						final String name = ci.name;
						if(name != null) {
							if(excludeImports.contains(name) == false) {
								buildClassImportItem(windowMaybeNull, vector, name).similarity(preCodeLower, preCodeLowerChars, preCodeLowerCharsLen);//计算相似度，为排序
							}
						}else {
							final int ciSize = ci.size;
							for (int j = 0; j < ciSize; j++) {
								final String oneName = ci.names[j];
								if(excludeImports.contains(oneName) == false) {
									buildClassImportItem(windowMaybeNull, vector, oneName).similarity(preCodeLower, preCodeLowerChars, preCodeLowerCharsLen);
								}
							}
						}
					}else if(isBigger < 0) {
						return;
					}
				}
			}
		}catch (final Throwable e) {
		}
	}
	
	private final CodeItem buildClassImportItem(final CodeWindow windowMaybeNull, final ArrayList<CodeItem> out, final String name) {
		final CodeItem item = CodeItem.getFree();
		item.code = getShortName(name);
		item.codeForDoc = name;//注意：该值在插入后，用于记录到已imports列表
		item.fmClass = CodeItem.FM_CLASS_IMPORT + name;
		
		item.codeDisplay = item.code + " - " + name;//ScriptPanel - hc.server.ui.ScriptPanel
		if(thirdLibNameMaybeNull != null) {
			item.codeDisplay += " : " + thirdLibNameMaybeNull;
		}
//		if(name.startsWith(CodeHelper.JAVA_PACKAGE_CLASS_PREFIX, 0)) {
//			item.codeDisplay = RubyExector.IMPORT + name;
//		}else {
//			item.codeDisplay = RubyExector.IMPORT + RubyExector.JAVA_MAO_MAO + name;//可能hc.或用户级
//		}
		
		item.thirdLibNameMaybeNull = thirdLibNameMaybeNull;
		item.type = CodeItem.TYPE_CLASS_IMPORT;
		
		out.add(item);
		if(windowMaybeNull != null) {
			windowMaybeNull.codeInvokeCounter.initCounter(item);
		}
		return item;
	}
	
	public static ClassImporter buildJ2SEHCImporter() {
		final int totalSize = CodeStaticHelper.J2SE_CLASS_SET_SIZE + CodeStaticHelper.HC_CLASS_SET_SIZE;
		final HashMap<String, ClassImportItem> map = new HashMap<String, ClassImportItem>(totalSize);
		final Vector<String> keyList = new Vector<String>(totalSize);
		
		ClassImporter.loadSet(map, keyList, CodeStaticHelper.J2SE_CLASS_SET, CodeStaticHelper.J2SE_CLASS_SET_SIZE, false);
		ClassImporter.loadSet(map, keyList, CodeStaticHelper.HC_CLASS_SET, CodeStaticHelper.HC_CLASS_SET_SIZE, false);
		return new ClassImporter(map, keyList, null);
	}

	public static void loadSet(final HashMap<String, ClassImportItem> map, final Vector<String> keyList, 
			final String[] set, final int setSize, final boolean isIncludeRes) {
		for (int i = 0; i < setSize; i++) {
			final String item = set[i];
			if(isIncludeRes && ResourceUtil.isResPath(item)) {
				continue;
			}
			final String shortName = getShortName(item).toLowerCase();
			ClassImportItem cItem = map.get(shortName);
			if(cItem == null) {
				cItem = new ClassImportItem();
				map.put(shortName, cItem);
				keyList.add(shortName);
			}
			cItem.addCName(item);
		}
	}
	
	public static String getShortName(final String fullName) {
		final int lastDotIdx = fullName.lastIndexOf('.');
		return fullName.substring(lastDotIdx + 1);
	}
	
}

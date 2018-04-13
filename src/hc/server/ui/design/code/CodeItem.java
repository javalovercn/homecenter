package hc.server.ui.design.code;

import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.event.ChangeListener;

import hc.core.util.IntValue;
import hc.core.util.StringUtil;

public class CodeItem implements Comparable<CodeItem> {
	// private final static Stack free = new Stack(1024);

	public final static CodeItem getFree() {
		return new CodeItem();
	}
	// public final static CodeItem getFree(){
	// final CodeItem out = (CodeItem)free.pop();
	// if(out != null){
	// return out;
	// }else{
	// return new CodeItem();
	// }
	// }

	public static void append(final ArrayList<CodeItem> target, final AbstractList<CodeItem> set) {
		target.addAll(set);
	}

	// public static void copy(final AbstractList<CodeItem> src, final
	// ArrayList<CodeItem> target){
	// final int size = src.size();
	// for (int i = 0; i < size; i++) {
	// final CodeItem item = CodeItem.getFree();
	// item.copyFrom(src.get(i));
	// target.add(item);
	// }
	// }

	public CodeItem() {
		this.reset();
	}

	@Override
	public final String toString() {
		String typeStr = "";
		if (type == TYPE_CLASS) {
			typeStr = "class";
		}else if (type == TYPE_CLASS_IMPORT) {
				typeStr = "class_import";
		} else if (type == TYPE_RESOURCES) {
			typeStr = "resources";
		} else if (type == TYPE_METHOD) {
			typeStr = "method";
		} else if (type == TYPE_FIELD) {
			typeStr = "field";
		} else if (type == TYPE_CSS) {
			typeStr = "css";
		} else if (type == TYPE_CSS_VAR) {
			typeStr = "cssvar";
		}
		return code + ", type : " + typeStr;
	}

	public final static void cycle(final Vector<CodeItem> vector) {
		vector.clear();
	}

	public final static void cycle(final ArrayList<CodeItem> vector) {
		vector.clear();
	}
	// public final static void cycle(final Vector<CodeItem> vector){
	// final int size = vector.size();
	// for (int i = 0; i < size; i++) {
	// cycle(vector.elementAt(i));
	// }
	// vector.clear();
	// }

	// public final static void cycle(final CodeItem item){
	// reset(item);
	//
	// free.push(item);
	// }

	private final void reset() {
		type = 0;
		isPublic = false;
		modifiers = 0;
		isForMaoHaoOnly = false;
		isFullPackageAndClassName = false;
		code = "";
		codeForDoc = "";
		fmClass = Object.class.getName();
		codeDisplay = "";
		fieldOrMethodOrFullClassName = "";
		anonymousClass = null;
		isCSSProperty = false;
		isCSSClass = false;
		isRubyClass = false;
		userObject = null;
		isInnerClass = false;
		isDefed = false;
		overrideMethodLevel = 0;
		overrideMethodItem = null;
		invokeCounter = new IntValue();
		matchSimilarValue = 0;
		codeMatch = null;
		codeMatchLower = null;
		codeLowMatchSubStrings = null;
		thirdLibNameMaybeNull = null;
	}
	
	/**
	 * 完全匹配前缀
	 * @param preCodeLower
	 * @return
	 */
	public final boolean matchPreCode(final String preCodeLower) {
		initCodeMatchAndLower();
		
		return codeMatchLower.indexOf(preCodeLower) >= 0;
	}
	
	private final static char[][] subStringParts = new char[64][];
	private static int subStringPartsSize;
	
	public static final int MATCH_MAX = Integer.MAX_VALUE;
	public static final int MATCH_HALF = MATCH_MAX / 2;
	private static final int SIMILAR_TYPE = CodeItem.TYPE_METHOD;
	
	public final void initForSimilar() {
		initCodeMatchAndLower();
		
		if(type == SIMILAR_TYPE) {
			initCodeLowMatchSubStrings();
		}
	}
	
	public final int similarity(final String preCodeLower, final char[] preCodeLowerChars, final int preCodeLowerCharsLen) {
		matchSimilarValue = 0;
		
		if(preCodeLowerCharsLen == 0) {
			matchSimilarValue = 1;
			return matchSimilarValue;
		}
		
		initCodeMatchAndLower();
		
		{
			final int matchIdx = codeMatchLower.indexOf(preCodeLower, 0);
			if(matchIdx == 0) {
				matchSimilarValue = MATCH_MAX;
				return matchSimilarValue;
			}
			if(matchIdx > 0) {//可以匹配如XX_MESSAGE
				if(type == CodeItem.TYPE_FIELD 
						|| type == CodeItem.TYPE_CLASS_IMPORT
						|| type == CodeItem.TYPE_CSS) {
					matchSimilarValue = MATCH_HALF;
					return matchSimilarValue;
				}
			}
		}

		if(type != SIMILAR_TYPE) {//不做相似度匹配
			matchSimilarValue = 0;
			return matchSimilarValue;
		}

		initCodeLowMatchSubStrings();
		
		final int partNum = codeLowMatchSubStrings.length;
		
		for (int j = 0; j < preCodeLowerCharsLen; j++) {
			for (int i = 0; i < partNum; i++) {
				final char[] parts = codeLowMatchSubStrings[i];
				
				for (int k = 0; k < parts.length; k++) {
					if(parts[k] == preCodeLowerChars[j + k]) {
						matchSimilarValue++;
					}else {
						break;
					}
				}
			}
		}
		
		if(matchSimilarValue < 4 && matchSimilarValue < preCodeLowerCharsLen) {//滤掉如：addContainerListener jch => matchSimilarValue == 1
			matchSimilarValue = 0;
		}
		return matchSimilarValue;
	}

	private final void initCodeLowMatchSubStrings() {
		//以下进入相似度匹配
		if(codeLowMatchSubStrings != null) {
			return;
		}
		
		synchronized (subStringParts) {
			final char[] codeLowMatchChars = codeMatch.toCharArray();
			subStringPartsSize = 0;
			
			int serialHighCharCount = 0;
			boolean isLastCharHigh = true;
			int nextCutIdx = 0;
			for (int i = 0, len = codeLowMatchChars.length; i < len; i++) {
				final char oneChar = codeLowMatchChars[i];
				if(oneChar >= 'A' && oneChar <= 'Z') {
					codeLowMatchChars[i] = (char)(oneChar + StringUtil.DISTANCE_CHAR_A);//转小写
					if(isLastCharHigh == false) {
						final int subPartsLen = i - nextCutIdx;
						final char[] subParts = new char[subPartsLen];
						for (int j = 0; j < subPartsLen; j++) {
							subParts[j] = codeLowMatchChars[nextCutIdx + j];
						}
						subStringParts[subStringPartsSize++] = subParts;
						serialHighCharCount = 1;
					}else {
						if(serialHighCharCount++ > 0) {
							continue;
						};
					}
					isLastCharHigh = true;
					nextCutIdx = i;
				}else {
					if(isLastCharHigh == true) {
						if(i == 0) {
							isLastCharHigh = false;
							continue;
						}else {
							if(serialHighCharCount > 1) {
								final int subPartsLen = i - 1 - nextCutIdx;
								final char[] subParts = new char[subPartsLen];
								for (int j = 0; j < subPartsLen; j++) {
									subParts[j] = codeLowMatchChars[nextCutIdx + j];
								}
								subStringParts[subStringPartsSize++] = subParts;
								serialHighCharCount = 0;
								nextCutIdx = i - 1;
							}
						}
					}
					isLastCharHigh = false;
				}
			}
			
			final int subPartsLen = codeLowMatchChars.length - nextCutIdx;
			final char[] subParts = new char[subPartsLen];
			for (int j = 0; j < subPartsLen; j++) {
				subParts[j] = codeLowMatchChars[nextCutIdx + j];
			}
			subStringParts[subStringPartsSize++] = subParts;
			
			codeLowMatchSubStrings = new char[subStringPartsSize][];
			for (int i = 0; i < subStringPartsSize; i++) {
				codeLowMatchSubStrings[i] = subStringParts[i];
			}
		}
	}

	private final void initCodeMatchAndLower() {
		if(codeMatch == null) {
			if(fieldOrMethodOrFullClassName.length() > 0 && (type != CodeItem.TYPE_CLASS_IMPORT)) {
				codeMatch = fieldOrMethodOrFullClassName;
			}else {
				final int idx = code.indexOf('(');
				if(idx >= 0) {
					codeMatch = code.substring(0, idx);
				}else {
					codeMatch = code;
				}
			}
		}
		
		if(codeMatchLower == null) {
			codeMatchLower = codeMatch.toLowerCase();
		}
	}

	public final boolean isOverrideable() {
		return type == TYPE_METHOD && Modifier.isFinal(modifiers) == false && Modifier.isStatic(modifiers) == false
				&& Modifier.isPrivate(modifiers) == false;
	}

	public final static int TYPE_VARIABLE = 1;// 优先排序
	public final static int TYPE_FIELD = 2;
	public final static int TYPE_METHOD = 3;
	public final static int TYPE_PACKAGE = 4;// Deprecated
	public final static int TYPE_IMPORT = 5;
	public final static int TYPE_CLASS = 6;
	public final static int TYPE_RESOURCES = 7;
	public final static int TYPE_CSS_VAR = 8;
	public final static int TYPE_CSS = 9;
	//注意：直接在代码中输入类名，自动添加import语句；不同于TYPE_IMPORT手动添加import语句
	public final static int TYPE_CLASS_IMPORT = 10;

	public final static Class[] AnonymousClass = { Runnable.class, ActionListener.class,
			// FocusListener.class,
			// MouseListener.class, //Release, mousePressed, mouseExited,
			// mouseEntered, mouseClicked, mouseDragged
			// KeyListener.class,
			ItemListener.class, // select JComboBox
			ChangeListener.class, // JSlider setValue
			// DocumentListener.class, //JTextField setText
			// JRadioButton doClick()
	};
	public final static int AnonymousClassSize = AnonymousClass.length;

	public final void setAnonymouseClassType(final Type[] paras) {
		if (paras == null) {
			return;
		}

		if (paras.length != 1) {
			return;
		}

		final Type claz = paras[0];

		for (int i = 0; i < AnonymousClassSize; i++) {
			final Class item = AnonymousClass[i];
			if (claz == item) {
				anonymousClass = item;
				return;
			}
		}
	}

	// public final void copyFrom(final CodeItem from){
	// type = from.type;
	// code = from.code;
	// codeForDoc = from.codeForDoc;
	// fmClass = from.fmClass;
	// codeDisplay = from.codeDisplay;
	// codeMatch = from.codeLowMatch;
	// fieldOrMethodOrFullClassName = from.fieldOrMethodOrClassName;
	// isPublic = from.isPublic;
	// modifiers = from.modifiers;
	// anonymousClass = from.anonymousClass;
	// isCSSProperty = from.isCSSProperty;
	// isCSSClass = from.isCSSClass;
	// userObject = from.userObject;
	// isInnerClass = from.isInnerClass;
	// isDefed = from.isDefed;
	// overrideMethodLevel = from.overrideLevel;
	// }

	public static final String FM_CLASS_CSS_VAR = "fm_class_css_var";
	public static final String FM_CLASS_CSS = "fm_class_css";
	public static final String FM_CLASS_PACKAGE = "fm_class_package";
	public static final String FM_CLASS_IMPORT = "fm_class_import_";

	public int type;
	public boolean isPublic;
	public int modifiers;
	public boolean isForMaoHaoOnly;
	public boolean isFullPackageAndClassName;
	public String code;
	public String codeForDoc;
	public String fieldOrMethodOrFullClassName;
	public String fmClass;
	public String codeDisplay;
	public Class anonymousClass;
	public boolean isCSSProperty;
	public boolean isCSSClass;
	public boolean isRubyClass;
	public boolean isInnerClass;
	public Object userObject;
	public boolean isDefed;
	public int overrideMethodLevel;// 数字越大，居上(子类)
	public CodeItem overrideMethodItem;
	public IntValue invokeCounter = new IntValue();
	public int matchSimilarValue;
	public String codeMatch;
	private String codeMatchLower;
	public char[][] codeLowMatchSubStrings;
	public String thirdLibNameMaybeNull;//null means J2SE or HC, not third lib

	@Override
	public final int compareTo(final CodeItem o) {// 注意：如果增加field，请同步到equals()
		final double matchSimilarDiff = o.matchSimilarValue - this.matchSimilarValue;
		if(matchSimilarDiff != 0) {
			return (matchSimilarDiff>0?1:-1);
		}
		
		if (fmClass == CodeInvokeCounter.CLASS_UN_INVOKE_COUNT_STRUCT && o.fmClass != CodeInvokeCounter.CLASS_UN_INVOKE_COUNT_STRUCT) {
			return -1;// 优先显示JRuby Struct枚举属性
		}

		final int invokeCount = o.invokeCounter.value - this.invokeCounter.value;
		if (invokeCount != 0) {
			return invokeCount;
		}

		int result = this.type - o.type;
		if (result == 0) {
			result = codeDisplay.compareToIgnoreCase(o.codeDisplay);// 可将bor/Border规在一块
			if (result == 0) {
				return o.overrideMethodLevel - overrideMethodLevel;// 子类居上
			} else {
				return result;
			}
		} else {
			return result;
		}
	}

	@Override
	public final boolean equals(final Object o) {// 不重复添加
		if (o == null) {
			return false;
		}

		if (this == o) {
			return true;
		}

		if (o instanceof CodeItem) {
			final CodeItem ci = (CodeItem) o;
			if (this.type == ci.type && this.fmClass.equals(ci.fmClass) && this.fieldOrMethodOrFullClassName.equals(ci.fieldOrMethodOrFullClassName)
					&& this.codeDisplay.equals(ci.codeDisplay)) {
				return true;
			}
			return false;
		} else {
			return false;
		}
	}

	public final static boolean contains(final ArrayList<CodeItem> list, final String code) {
		final int sizeList = list.size();
		boolean findSameName = false;
		for (int j = 0; j < sizeList; j++) {
			if (list.get(j).code.equals(code)) {
				findSameName = true;
				break;
			}
		}
		return findSameName;
	}

	public final static boolean containsSameFieldMethodName(final ArrayList<CodeItem> list, final String fieldOrMethodOrClassName) {
		final int sizeList = list.size();
		for (int j = 0; j < sizeList; j++) {
			if (list.get(j).fieldOrMethodOrFullClassName.equals(fieldOrMethodOrClassName)) {
				return true;
			}
		}
		return false;
	}

	public final static boolean overrideMethod(final CodeItem item, final ArrayList<CodeItem> list) {
		final int sizeList = list.size();
		boolean findSameName = false;
		for (int j = sizeList - 1; j >= 0; j--) {
			final CodeItem codeItem = list.get(j);
			if (item.isOverrideItem(codeItem)) {
				// item.overrideMethodLevel += (codeItem.overrideMethodLevel +
				// 1);
				item.overrideMethodItem = codeItem;
				codeItem.pushDownOverrideMethod();
				findSameName = true;
				break;
			}
		}
		return findSameName;
	}

	final boolean isOverrideItem(final CodeItem codeItem) {
		// isRubyClass == fals，因为Object方法eql?和equal?使用相同的codeForDoc
		return type == TYPE_METHOD && isRubyClass == false && codeItem.type == TYPE_METHOD && codeItem.codeForDoc.equals(codeForDoc);
	}

	public final void pushDownOverrideMethod() {
		overrideMethodLevel--;
		if (overrideMethodItem != null) {
			overrideMethodItem.pushDownOverrideMethod();
		}
	}
}

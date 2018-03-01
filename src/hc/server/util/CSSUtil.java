package hc.server.util;

import hc.core.util.StringUtil;
import hc.server.ui.design.hpj.CSSClassIndex;
import hc.server.ui.design.hpj.ScriptEditPanel;
import hc.util.StringBuilderCacher;

import java.util.Collections;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

public class CSSUtil {
	private static Pattern commentPattern = Pattern.compile("/\\*(.*?)\\*/", Pattern.DOTALL);

	/**
	 * 注意：暂不支持.class1.class2 {font:red}的定义
	 */
	private static Pattern cssClassPattern = Pattern
			.compile("(?<=(\\s|\\}|\\b))(\\w*)\\.(\\w+)\\s*?\\{(.*?)\\}", Pattern.DOTALL);// 含p.right{text-align:right}

	/**
	 * 
	 * @param cssScriptsWithRem
	 * @param cssProjectClasses
	 *            如果只有一个，则存储为CSSClassIndex；多个，则为Vector<CSSClassIndex>
	 * @param docMaybeNull
	 * @return
	 */
	public static String updateCSSClass(final String cssScriptsWithRem,
			final Vector<Object> cssProjectClasses, final Document docMaybeNull) {
		String sameFullName = null;

		final Matcher m = cssClassPattern
				.matcher(commentPattern.matcher(cssScriptsWithRem).replaceAll(""));
		int stringLineNoIdx = 0;
		int stringLineNo = 0;
		while (m.find()) {
			final String html = m.group(2);
			final String cssClass = m.group(3);
			final String checkFullName = html.toUpperCase() + "." + cssClass;
			final String matchItem = m.group(0);
			int lineNo = 0;
			final int startIdx = cssScriptsWithRem.indexOf(matchItem, m.start(0));// 注：须包括可能的/**/
			if (docMaybeNull != null) {
				try {
					lineNo = ScriptEditPanel.getLineOfOffset(docMaybeNull, startIdx) + 1;
				} catch (final BadLocationException e) {
					e.printStackTrace();
				}
			} else {
				lineNo = StringUtil.getLineNo(cssScriptsWithRem, stringLineNoIdx, stringLineNo,
						startIdx);
				stringLineNoIdx = startIdx;
				stringLineNo = lineNo;
			}
			final CSSClassIndex idx = new CSSClassIndex(checkFullName, cssClass, startIdx, lineNo);

			boolean isAdded = false;
			final int size = cssProjectClasses.size();
			for (int i = 0; i < size; i++) {
				final Object item = cssProjectClasses.elementAt(i);
				if (item instanceof CSSClassIndex) {
					final CSSClassIndex compIdx = (CSSClassIndex) item;
					if (compIdx.className.equals(cssClass)) {
						final Vector<CSSClassIndex> v = new Vector<CSSClassIndex>(2);

						cssProjectClasses.remove(i);
						cssProjectClasses.add(i, v);

						v.add(compIdx);
						v.add(idx);
						isAdded = true;

						if (sameFullName == null && checkFullName.equals(compIdx.fullName)) {
							sameFullName = "<html>Error same CSS define [<strong>" + checkFullName
									+ "</strong>]." + "<BR><BR>"
									+ "It is defined at line : <strong>" + compIdx.lineNo
									+ "</strong> and line : <strong>" + idx.lineNo
									+ "</strong></html>";
						}
						break;
					}
				} else {
					final Vector<CSSClassIndex> v = (Vector<CSSClassIndex>) item;

					// 检查fullName
					final int subSize = v.size();
					for (int j = 0; j < subSize; j++) {
						final CSSClassIndex compIdx = v.elementAt(j);
						if (sameFullName == null && checkFullName.equals(compIdx.fullName)) {
							sameFullName = "<html>Error same CSS define [<strong>" + checkFullName
									+ "</strong>]." + "<BR><BR>"
									+ "It is defined at line : <strong>" + compIdx.lineNo
									+ "</strong> and line : <strong>" + idx.lineNo
									+ "</strong></html>";
							break;
						}
					}

					if (v.elementAt(0).className.equals(cssClass)) {
						isAdded = true;
						v.add(idx);
					}
				}
			}

			if (isAdded == false) {
				cssProjectClasses.add(idx);
			}
		}

		final int cssSize = cssProjectClasses.size();
		for (int i = 0; i < cssSize; i++) {
			final Object element = cssProjectClasses.elementAt(i);
			if (element instanceof Vector) {
				Collections.sort((Vector<CSSClassIndex>) element);
			}
		}

		return sameFullName;
	}

	public static boolean comment(final JTextPane pane) {
		return checkPane(pane);
	}

	private static boolean checkPane(final JTextPane pane) {
		final int startIdx = pane.getSelectionStart();
		final int endIdx = pane.getSelectionEnd();
		if (endIdx > 0) {
			comment(pane, startIdx, endIdx);
		} else {
			final String text = pane.getText();
			if (text.length() == 0) {
				return false;
			}
			comment(pane, startIdx, endIdx);
		}
		return true;
	}

	private static void comment(final JTextPane pane, final int startIdx, final int endIdx) {
		final Document doc = pane.getDocument();
		try {
			final int startNo = ScriptEditPanel.getLineOfOffset(doc, startIdx);
			final int endNo = ScriptEditPanel.getLineOfOffset(doc, endIdx);
			boolean isComment = true;
			for (int i = startNo; i <= endNo; i++) {
				final int lineStartIdx = ScriptEditPanel.getLineStartOffset(doc, i);
				final int lineEndIdx = ScriptEditPanel.getLineEndOffset(doc, i);
				final String text = doc.getText(lineStartIdx, lineEndIdx - lineStartIdx);
				final char[] textChars = text.toCharArray();
				int commentIdx = -1;
				int unSpaceIdx = -1;
				int commentEndIdx = -1;// 注意：*/ the star index
				final int textCharLen = textChars.length;
				for (int j = 0; j < textCharLen; j++) {
					final char currChar = textChars[j];
					if (unSpaceIdx == -1 && (currChar == ' ' || currChar == '\t') == false) {
						unSpaceIdx = j;
					}
					if (commentIdx == -1 && j + 1 < textCharLen && currChar == '/'
							&& textChars[j + 1] == '*') {
						commentIdx = j;
						if (i == startNo) {
							isComment = false;// 以首行来判断是否进行comment/uncomment
						}
						if (isComment) {
							break;
						}
					}
					if (isComment == false && j + 1 < textCharLen && currChar == '*'
							&& textChars[j + 1] == '/') {
						commentEndIdx = j;
						break;
					}
				}

				if (isComment && commentIdx >= 0) {
					continue;// 当前行已注释
				} else if (isComment == false && commentIdx == -1) {
					continue;// 当前行已无注释
				}

				if (isComment) {
					doc.insertString(lineEndIdx - 1, "*/", null);
					doc.insertString(lineStartIdx + unSpaceIdx, "/*", null);
				} else {
					if (commentEndIdx > 0) {
						doc.remove(lineStartIdx + commentEndIdx, 2);
					}
					if (commentIdx >= 0) {
						doc.remove(lineStartIdx + commentIdx, 2);
					}
				}
			}
		} catch (final Throwable e) {
			e.printStackTrace();
		}
	}

	private final static char removeBackSpace(final StringBuilder sb) {
		// font-color:red \t} => font-color:red\n}
		int size = sb.length();
		char backChar = 0;
		while (true) {
			if (size > 0) {
				final int tailIdx = size - 1;
				backChar = sb.charAt(tailIdx);
				if (backChar == ' ' || backChar == '\t') {
					sb.deleteCharAt(tailIdx);
					size--;
					continue;
				}
			}
			return backChar;
		}
	}

	/**
	 * 本方法测试用例为TestFormatCSS.java
	 * 
	 * @param text
	 */
	public static final String format(final String text) {
		// System.out.println("format content : \n" + jtaScript.getText());
		final char[] textChars = text.toCharArray();
		final StringBuilder sb = StringBuilderCacher.getFree();

		boolean isNewLine = false;
		boolean isIndent = false;

		final int charSize = textChars.length;
		char preChar = ' ';
		for (int i = 0; i < charSize;) {
			final char oneChar = textChars[i];
			if (oneChar == ' ' || oneChar == '\t') {
				if (preChar == ' ' || preChar == '\t') {
					i++;
					continue;
				}
			}
			// if (isIndent == false && (oneChar == ' ' || oneChar == '\t')) {
			// i++;
			// continue;
			// }
			if (isNewLine == false && oneChar == '\n') {
				i++;
				continue;
			}
			// if (isIndent && (oneChar == ' ' || oneChar == '\t')) {
			// i++;
			// continue;// 忽略{之后的空格和Tab
			// }
			if (isNewLine && (oneChar == '\n' || oneChar == ' ' || oneChar == '\t')) {
				i++;
				continue;// 忽略\n之后的空格和Tab
			}

			if (isNewLine) {
				sb.append('\n');
				if (isIndent) {
					sb.append('\t');
					preChar = '\t';
				}

				final int oldI = i;
				i = appendRemMaybe(sb, textChars, i - 1, charSize, false);
				if (oldI != i) {
					continue;// 添加一个rem段
				}
				isNewLine = false;
			}

			if (oneChar == '{') {
				if (preChar == '\t' || preChar == ' ') {
					removeBackSpace(sb);
				}
				sb.append(' ');
				preChar = ' ';
				sb.append(oneChar);
				i = appendRemMaybe(sb, textChars, i, charSize, true);

				isNewLine = true;
				isIndent = true;
				continue;
			} else if (oneChar == '}') {
				char isNewLineChar = 0;
				if (preChar == '\t' || preChar == ' ') {
					isNewLineChar = removeBackSpace(sb);
				}
				// font-color:red \t} => font-color:red\n}
				// removeBackSpace(sb);
				// sb.append(oneChar);
				// i = appendRemMaybe(sb, textChars, i, charSize);
				if (isNewLineChar != '\n') {
					sb.append('\n');
				}
				sb.append(oneChar);
				sb.append('\n');
				preChar = oneChar;
				isNewLine = true;
				isIndent = false;
				i++;
				continue;
			} else if (oneChar == '/') {
				if (preChar == '*') {
					isNewLine = true;
				}
			} else if (oneChar == ';') {
				if (preChar == '\t' || preChar == ' ') {
					removeBackSpace(sb);
				}
				preChar = 0;
				sb.append(oneChar);
				i = appendRemMaybe(sb, textChars, i, charSize, true);

				isNewLine = true;
				continue;
			} else if (oneChar == ':') {
				if (preChar == '\t' || preChar == ' ') {
					removeBackSpace(sb);
				}
				sb.append(oneChar);
				sb.append(' ');
				preChar = ' ';
				i = skipSpace(textChars, i, charSize);
				continue;
			}
			sb.append(oneChar);
			preChar = oneChar;
			i++;
		}

		final String out = sb.toString();
		StringBuilderCacher.cycle(sb);

		return out;
	}

	private static int skipSpace(final char[] textChars, int i, final int charSize) {
		while (i + 1 < charSize) {
			final char nextChar = textChars[++i];
			if (nextChar == ' ' || nextChar == '\t') {
				continue;
			}
			break;
		}
		return i;
	}

	/**
	 * 并移去前面可能的空格。返回index为下一个有效字符。 如果有注释段，则前面加空格
	 * 
	 * @param sb
	 * @param textChars
	 * @param i
	 * @param charSize
	 * @return
	 */
	private static int appendRemMaybe(final StringBuilder sb, final char[] textChars, int i,
			final int charSize, final boolean needSpace) {
		// 每个property的行尾;号，可能后挂/**/，中间可能有空格
		while (++i < charSize) {
			final char nextChar = textChars[i];
			if (nextChar == ' ' || nextChar == '\t') {
				continue;
			}
			if (nextChar == '/' && i + 1 < charSize && textChars[i + 1] == '*') {
				if (needSpace) {
					sb.append(' ');// 注释之前加一个空格
				}
				final int endRemIdx = CSSUtil.searchRem(i + 1, textChars, charSize);
				sb.append(textChars, i, endRemIdx - (i - 1));
				i = endRemIdx + 1;
				break;
			} else {
				break;
			}
		}
		return i;
	}

	private static int searchRem(int startIdx, final char[] chars, final int charLen) {
		while (true) {
			if (startIdx + 1 >= charLen) {
				return startIdx;
			}
			if (chars[startIdx++] == '*' && chars[startIdx] == '/') {
				return startIdx;
			}
		}
	}

}

package hc.server.util;

import java.util.Locale;

/**
 * the voice which user speaks in client.
 */
public class VoiceCommand {
	final String text;
	
	public VoiceCommand(final String text){
		this.text = text;
	}
	
	/**
	 * return the text of voice command.<BR><BR>
	 * <font color="red"><STRONG>Important</STRONG> :</font><BR>
	 * in JRuby, the object String will be converted to JRuby string, if you are familiar with Java String, please use methods in this class, for example {@link #indexOfText(String)}.
	 * @return
	 * @since 7.47
	 */
	public final String getText(){
		return this.text;
	}
	
	/**
	 * see {@link String#indexOf(String, int)}
	 * @param subStr
	 * @param fromIdx
	 * @return
	 */
	public final int indexOfText(final String subStr, final int fromIdx){
		return this.text.indexOf(subStr, fromIdx);
	}
	
	/**
	 * see {@link String#indexOf(String)}
	 * @param str
	 * @return
	 */
	public final int indexOfText(final String str) {
		return text.indexOf(str);
	}
	
	/**
	 * see {@link String#lastIndexOf(String)}
	 * @param str
	 * @return
	 */
	public final int lastIndexOfText(final String str) {
		return text.lastIndexOf(str);
	}
	
	/**
	 * see {@link String#contains(CharSequence)}
	 * @param s
	 * @return
	 */
	public final boolean containsText(final CharSequence s) {
		return this.text.contains(s);
	}
	
	/**
	 * see {@link String#startsWith(String, int)}
	 * @param prefix
	 * @param toffset
	 * @return
	 */
	public final boolean startsWithText(final String prefix, final int toffset) {
		return this.text.startsWith(prefix, toffset);
	}
	
	/**
	 * see {@link String#startsWith(String)}
	 * @param prefix
	 * @return
	 */
	public final boolean startsWithText(final String prefix) {
		return this.text.startsWith(prefix);
	}
	
	/**
	 * see {@link String#endsWith(String)}
	 * @param suffix
	 * @return
	 */
	public final boolean endsWithText(final String suffix) {
		return this.text.endsWith(suffix);
	}
	
	/**
	 * see {@link String#replaceFirst(String, String)}
	 * @param regex
	 * @param replacement
	 * @return
	 */
	public final String replaceFirstText(final String regex, final String replacement) {
		return text.replaceFirst(regex, replacement);
	}
	
	/**
	 * see {@link String#replaceAll(String, String)}
	 * @param regex
	 * @param replacement
	 * @return
	 */
	public final String replaceAllText(final String regex, final String replacement) {
		return text.replaceAll(regex, replacement);
	}
	
	/**
	 * see {@link String#replace(CharSequence, CharSequence)}
	 * @param target
	 * @param replacement
	 * @return
	 */
	public final String replaceText(final CharSequence target, final CharSequence replacement) {
		return text.replace(target, replacement);
	}
	
	/**
	 * see {@link String#replace(char, char)}
	 * @param oldChar
	 * @param newChar
	 * @return
	 */
	public final String replaceText(final char oldChar, final char newChar) {
		return text.replace(oldChar, newChar);
	}
	
	/**
	 * see {@link String#substring(int)}
	 * @param beginIndex
	 * @return
	 */
	public final String substringText(final int beginIndex) {
		return text.substring(beginIndex);
	}
	
	/**
	 * see {@link String#substring(int, int)}
	 * @param beginIndex
	 * @param endIndex
	 * @return
	 */
	public final String substringText(final int beginIndex, final int endIndex) {
		return text.substring(beginIndex, endIndex);
	}
	
	/**
	 * see {@link String#equalsIgnoreCase(String)}
	 * @param anotherString
	 * @return
	 */
	public final boolean equalsIgnoreCaseText(final String anotherString) {
		return this.text.equalsIgnoreCase(anotherString);
	}
	
	/**
	 * see {@link String#contentEquals(CharSequence)}
	 * @param cs
	 * @return
	 */
	public final boolean contentEqualsText(final CharSequence cs) {
		return this.text.contentEquals(cs);
	}
	
	/**
	 * see {@link String#regionMatches(int, String, int, int)}
	 * @param toffset
	 * @param other
	 * @param ooffset
	 * @param len
	 * @return
	 */
	public final boolean regionMatchesText(final int toffset, final String other, final int ooffset,
            final int len) {
		return this.text.regionMatches(toffset, other, ooffset, len);
	}
	
	/**
	 * see {@link String#regionMatches(boolean, int, String, int, int)}
	 * @param ignoreCase
	 * @param toffset
	 * @param other
	 * @param ooffset
	 * @param len
	 * @return
	 */
	public final boolean regionMatchesText(final boolean ignoreCase, final int toffset,
            final String other, final int ooffset, final int len) {
		return text.regionMatches(ignoreCase, toffset, other, ooffset, len);
	}
	
	/**
	 * see {@link String#lastIndexOf(String, int)}
	 * @param str
	 * @param fromIndex
	 * @return
	 */
	public final int lastIndexOfText(final String str, final int fromIndex) {
		return text.lastIndexOf(str, fromIndex);
	}
	
	/**
	 * see {@link String#matches(String)}
	 * @param regex
	 * @return
	 */
	public final boolean matchesText(final String regex) {
		return text.matches(regex);
	}
	
	/**
	 * see {@link String#split(String, int)}
	 * @param regex
	 * @param limit
	 * @return
	 */
	public final String[] splitText(final String regex, final int limit) {
		return text.split(regex, limit);
	}
	
	/**
	 * see {@link String#split(String)}
	 * @param regex
	 * @return
	 */
	public final String[] splitText(final String regex) {
		return text.split(regex);
	}
	
	/**
	 * see {@link String#toLowerCase()}
	 * @return
	 */
	public final String toLowerCaseText() {
		return text.toLowerCase();
	}
	
	/**
	 * see {@link String#toLowerCase(Locale)}
	 * @param locale
	 * @return
	 */
	public final String toLowerCaseText(final Locale locale) {
		return text.toLowerCase(locale);
	}
	
	/**
	 * see {@link String#toUpperCase(Locale)}
	 * @param locale
	 * @return
	 */
	public final String toUpperCaseText(final Locale locale) {
		return text.toUpperCase(locale);
	}
	
	/**
	 * see {@link String#toUpperCase()}
	 * @return
	 */
	public final String toUpperCaseText() {
		return text.toUpperCase();
	}
}

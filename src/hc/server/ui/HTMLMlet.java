package hc.server.ui;

import java.awt.Color;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;

/**
 * {@link HTMLMlet} is a JPanel, which contains JComponents and present them as HTML on mobile (not snapshot), you can set CSS for these JComponents.<BR>
 * <BR>Tip : to load and execute JavaScript, please use {@link ScriptPanel}.
 * <BR><BR>
 * Not all HTML tag are supported by mobile runtime environment.
 * <BR>To support HTML5, Android 4.4 (or above) / iPhone 4s (or above) is required.
 * <BR>
 * To synchronize status with mobile, AWT/Swing listeners are added to these JComponents, please don't remove them.
 * <BR><BR>
 * <STRONG>Important :</STRONG>
 * <BR>it is considered as {@link Mlet} and presents as snapshot to mobile, when one of the following conditions is met:
 * <BR>1. there is no sub component in {@link HTMLMlet} and it overrides {@link JComponent#paint(java.awt.Graphics)} method, 
 * <BR>2. the mobile client is J2ME (Java phone) mobile.
 * @see Mlet
 * @since 7.0
 */
@SuppressWarnings("deprecation")
public class HTMLMlet extends Mlet {
	private static final long serialVersionUID = 1234L;
	
	final ScriptCSSSizeHeight sizeHeightForXML;

	/**
	 * @deprecated
	 */
	@Deprecated
	public HTMLMlet(){
		sizeHeightForXML = new ScriptCSSSizeHeight(coreSS, __context);
	}
	
	/**
	 * load special styles for current {@link HTMLMlet}, it must be invoked before {@link #setCSS(JComponent, String, String)} which refer to these styles.
	 * <BR><BR>
	 * Network connection permission : <BR>
	 * if there is a <code>url()</code> in CSS, it is required to add domain of it to socket/connect permission or disable limit socket/connect.
	 * <BR><BR>
	 * More about CSS styles : 
	 * <BR>
	 * 1. the <i>CSS Styles</i> tree node in designer is shared to all {@link HTMLMlet}/{@link Dialog}s in same project.
	 * In other words, it will be loaded automatically by server for each HTMLMlet/Dialog.
	 * <BR>
	 * 2. this method can be invoked as many times as you want.
	 * <BR>
	 * 3. this method can be invoked also in constructor method (the initialize method in JRuby).
	 * <BR><BR>About cache :<BR>
	 * don't worry about styles too large for re-translating to mobile, <BR>
	 * the cache subsystem of HomeCenter will intelligence analysis to determine whether transmission or loading cache from mobile (if styles is too small, it will not be cached).
	 * What you should do is put more data into one style file, because if there is too much pieces of cache in a project, system will automatically clear the cache and restart caching.
	 * <BR><BR>
	 * to disable cache for current styles, see {@link #loadCSS(String, boolean)}.
	 * @param styles for example, "<i>h1 {color:red} p {color:blue}</i>".
	 * @see #setCSS(JComponent, String, String)
	 * @see #setCSSForToggle(JToggleButton, String, String)
	 * @see #setCSSForDiv(JComponent, String, String)
	 * @since 7.0
	 */
	public void loadCSS(final String styles){
		loadCSS(styles, true);
	}
	
	/**
	 * load special styles for current {@link HTMLMlet}.
	 * @param styles
	 * @param enableCache true means this styles may be cached if it is too large.
	 * @see #loadCSS(String)
	 */
	public void loadCSS(final String styles, final boolean enableCache){
		sizeHeightForXML.loadCSSImpl(this, __context, styles, enableCache);
	}
	
	/**
	 * set CSS <i>class</i> and/or CSS <i>style</i> for HTML div tag of {@link JComponent}.
	 * <BR><BR>
	 * for more, see {@link #setCSS(JComponent, String, String)}.
	 * <BR><BR>
	 * <STRONG>Important :</STRONG><BR>
	 * CSS box model of HomeCenter is <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/box-sizing">border-box</a> default (quirks mode), NOT the w3c <code>content-box</code>.<BR>
	 * most browsers use a DOCTYPE to decide whether to handle it in quirks mode or standards mode, so there is no DOCTYPE in HTML.
	 * @param component the JComponent to set style.
	 * @param className the class name of styles defined <i>Resources/CSS Styles</i> in designer or {@link #loadCSS(String)}. Null for ignore and keep old value. Empty string for clear.
	 * @param styles the styles defined <i>Resources/CSS Styles</i> in designer or {@link #loadCSS(String)}. Null for ignore and keep old value. Empty string for clear.
	 * @see #setCSS(JComponent, String, String)
	 * @see #setCSSForToggle(JToggleButton, String, String)
	 * @since 7.0
	 */
	public void setCSSForDiv(final JComponent component, final String className, final String styles){//in user thread
		sizeHeightForXML.setCSSForDivImpl(this, __context, component, className, styles);
	}
	
	/**
	 * set CSS <i>class</i> for {@link JComponent}, it's <code>div</code> and it's <code>toggle</code> (if exists) with one step.
	 * <BR><BR>
	 * for example, defines CSS for JButton as following :<BR><BR>
	 * <code>div.<STRONG>btnStyle</STRONG> {<BR>
	 * &nbsp;&nbsp;padding:3px;<BR>
	 * }<BR>
	 * <BR>
	 * button.<STRONG>btnStyle</STRONG> {<BR>
	 * &nbsp;&nbsp;color:blue;<BR>
	 * }</code>
	 * <BR><BR>
	 * invoking <code>setCSSByClass(btn, "<STRONG>btnStyle</STRONG>")</code> is same with invoking <code>setCSSForDiv(btn, "<STRONG>btnStyle</STRONG>", null)</code> and <code>setCSS(btn, "<STRONG>btnStyle</STRONG>", null)</code>.
	 * @param component the JComponent to set CSS class.
	 * @param className the class name of styles defined <i>Resources/CSS Styles</i> in designer or {@link #loadCSS(String)}. Do nothing if Null. Empty string for clear.
	 * @see #setCSSForDiv(JComponent, String, String)
	 * @see #setCSS(JComponent, String, String)
	 * @see #setCSSForToggle(JToggleButton, String, String)
	 * @since 7.32
	 */
	public void setCSSByClass(final JComponent component, final String className){
		if(className == null || component == null){
			return;
		}
		
		if(component instanceof JPanel){
			setCSSForDiv(component, className, null);
			return;
		}
		
		setCSSForDiv(component, className, null);
		setCSS(component, className, null);
		
		if(component instanceof JToggleButton){
			setCSSForToggle((JToggleButton)component, className, null);
		}
	}
	
	/**
	 * set CSS <i>class</i> and/or CSS <i>style</i> for {@link JComponent}.
	 * <BR><BR>
	 * it is effective immediately to mobile.<BR>
	 * it is allowed to invoke this method in constructor of {@link HTMLMlet}.
	 * <BR><BR>
	 * <STRONG>Important :</STRONG><BR>
	 * CSS box model of HomeCenter is <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/box-sizing">border-box</a> default (quirks mode), NOT the w3c <code>content-box</code>.<BR>
	 * most browsers use a DOCTYPE to decide whether to handle it in quirks mode or standards mode, so there is no DOCTYPE in HTML.
	 * <BR><BR>
	 * Network connection permission : <BR>
	 * if there is a <code>url()</code> in CSS, it is required to add domain of it to socket/connect permission or disable limit socket/connect.
	 * <BR><BR>
	 * Know more CSS : 
	 * <BR>
	 * 1. the effect of CSS depends on the run-time environment of mobile client.
	 * <BR>
	 * 2. to get environment information about mobile, please invoke {@link ProjectContext#getMobileOS()} and {@link ProjectContext#getMobileOSVer()}.
	 * <BR>
	 * 3. please resize image and save them in jar first, or invoke {@link #resizeImage(java.awt.image.BufferedImage, int, int)}. It is not recommend to resize image by your implementation, because the HAR project may be executed on Android server which is NOT standard J2SE.
	 * <BR>
	 * 4. the best practice is <STRONG>JComponent + LayoutManager + Listener + CSS</STRONG>. (Note : the implementation of Swing/J2SE for Android is differentiated from Oracle J2SE, if your HAR runs in Android server).
	 * <BR>
	 * 5. if your UI is ugly, please ask your CSS artist for pleasantly surprised!
	 * <BR><BR>
	 * Swing {@link JComponent}s are translated to HTML as following:
	 * <table border='1'>
	 * <tr>
	 * <th>JComponent</th><th>translated HTML</th><th>available</th><th>note</th>
	 * </tr>
	 * 
	 * <tr>
	 * <td>{@link JPanel}</td><td>&lt;div&gt;&lt;/div&gt;</td>
	 * <td>setCSSForDiv => div<BR>setCSS => div<BR><font style="text-decoration:line-through">setCSSForToggle</font>
	 * </td>
	 * <td>&nbsp;</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>{@link JButton}</td><td>&lt;div&gt;
	 * 		<BR>&nbsp;&nbsp;&lt;button type='button'&gt;
	 * 			<BR>&nbsp;&nbsp;&nbsp;&nbsp;&lt;img /&gt;
	 * 			<BR>&nbsp;&nbsp;&lt;/button&gt;
	 * 		<BR>&lt;/div&gt;</td>
	 * <td>setCSSForDiv => div<BR>setCSS => button<BR><font style="text-decoration:line-through">setCSSForToggle</font>
	 * </td>
	 * <td>the image of JButton is optional</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>{@link JCheckBox}</td><td style='white-space:nowrap'>&lt;div&gt;
	 * 		<BR>&nbsp;&nbsp;&lt;input type='checkbox'/&gt;
	 * 		<BR>&nbsp;&nbsp;&lt;label /&gt;
	 * 		<BR>&lt;/div&gt;</td>
	 * <td style='white-space:nowrap'>setCSSForDiv => div<BR><STRONG>setCSSForToggle => input</STRONG><BR>setCSS => label
	 * </td>
	 * <td>&nbsp;</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>{@link JRadioButton}</td><td>&lt;div&gt;
	 * 		<BR>&nbsp;&nbsp;&lt;input type='radio'/&gt;
	 * 		<BR>&nbsp;&nbsp;&lt;label /&gt;
	 * 		<BR>&lt;/div&gt;</td>
	 * <td>setCSSForDiv => div<BR><STRONG>setCSSForToggle => input</STRONG><BR>setCSS => label
	 * </td>
	 * <td>&nbsp;</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>{@link JLabel}</td><td>&lt;div&gt;
	 * 		<BR>&nbsp;&nbsp;&lt;img /&gt;
	 * 		<BR>&nbsp;&nbsp;&lt;label /&gt;
	 * 		<BR>&lt;/div&gt;</td>
	 * <td>setCSSForDiv => div<BR>setCSS => label<BR><font style="text-decoration:line-through">setCSSForToggle</font>
	 * </td>
	 * <td>the image of JLable is optional</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>{@link JTextField}</td><td>&lt;div&gt;
	 * 		<BR>&nbsp;&nbsp;&lt;input type='text|password'/&gt;
	 * 		<BR>&lt;/div&gt;</td>
	 * <td>setCSSForDiv => div<BR>setCSS => input<BR><font style="text-decoration:line-through">setCSSForToggle</font>
	 * </td>
	 * <td>&nbsp;</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>{@link JTextArea}<BR>{@link JTextPane}</td><td>&lt;div&gt;
	 * 		<BR>&nbsp;&nbsp;&lt;textarea/&gt;
	 * 		<BR>&lt;/div&gt;</td>
	 * <td>setCSSForDiv => div<BR>setCSS => textarea<BR><font style="text-decoration:line-through">setCSSForToggle</font>
	 * </td>
	 * <td>&nbsp;</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>{@link JComboBox}</td><td>&lt;div&gt;
	 * 		<BR>&nbsp;&nbsp;&lt;select &gt;
	 * 		<BR>&nbsp;&nbsp;&nbsp;&nbsp;&lt;/option&gt;&lt;/option&gt;
	 * 		<BR>&nbsp;&nbsp;&lt;/select&gt;
	 * 		<BR>&lt;/div&gt;</td>
	 * <td>setCSSForDiv => div<BR>setCSS => select<BR><font style="text-decoration:line-through">setCSSForToggle</font>
	 * </td>
	 * <td>&nbsp;</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>{@link JProgressBar}</td><td>&lt;div&gt;
	 * 		<BR>&nbsp;&nbsp;&lt;progress /&gt;
	 * 		<BR>&lt;/div&gt;</td>
	 * <td>setCSSForDiv => div<BR>setCSS => progress<BR><font style="text-decoration:line-through">setCSSForToggle</font>
	 * </td>
	 * <td>
	 * <code>progress</code> is tag in HTML5, so <br>Android 4.4 (or above) or iPhone 4s (or above) is required.
	 * <BR><STRONG>CAUTION : </STRONG>there is no 'min' attribute in <code>progress</code>.
	 * </td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>{@link JSlider}</td><td>&lt;div&gt;
	 * 		<BR>&nbsp;&nbsp;&lt;input type='range'/&gt;
	 * 		<BR>&lt;/div&gt;</td>
	 * <td>setCSSForDiv => div<BR>setCSS => input<BR><font style="text-decoration:line-through">setCSSForToggle</font>
	 * </td>
	 * <td>the 'range' is tag in HTML5, <br>Android 4.4 (or above) or iPhone 4s (or above) is required.
	 * <BR><STRONG>CAUTION : </STRONG>for Android Server (NOT Android client), <code>SeekBar</code> is used to render JSlider and there is no 'min' field in <code>SeekBar</code>.
	 * </td>
	 * </tr>
	 * 
	 * </table>
	 * <BR>in general, <BR>
	 * 1. <code>setCSSForDiv</code> is for the <code>div</code> tag of <code>JComponent</code>, there is a <code>div</code> for each <code>JComponent</code> for location and size.<BR>
	 * 2. the location is set to the <code>left</code>(relative to the parent div),<code>top</code>(relative to the parent div), <code>width</code>, <code>height</code> of <code>div</code>.<BR>
	 * 3. <code>setCSSForToggle</code> is for the <code>input</code> tag of <code>JCheckBox</code> or <code>JRadioButton</code>,<BR>
	 * 4. <code>setCSS</code> is just for <code>JComponent</code>, maybe for <code>div</code> if it is JPanel, maybe for <code>label</code> if it is JLabel.<BR>
	 * 5. visible : <i>getElementById({div}).style.visibility='visible'</i>;<BR>
	 * 6. invisible : <i>getElementById({div}).style.visibility='hidden'</i>;<BR>
	 * 7. enable : <i>getElementById({input|label|selection|progress}).disabled = false</i>;<BR>
	 * 8. disable : <i>getElementById({input|label|selection|progress}).disabled = true</i>;<BR>
	 * 9. readonly : <i>getElementById({input|label|selection|progress}).setAttribute('readonly', 'readonly')</i>;<BR>
	 * 10. editable : <i>getElementById({input|label|selection|progress}).removeAttribute('readonly')</i>;<BR>
	 * 11. NOT all JComponents are supported.
	 * @param component the JComponent to set style.
	 * @param className the class name of styles defined <i>Resources/CSS Styles</i> in designer or {@link #loadCSS(String)}. Null for ignore and keep old value. Empty string for clear.
	 * @param styles the styles defined <i>Resources/CSS Styles</i> in designer or {@link #loadCSS(String)}. Null for ignore and keep old value. Empty string for clear.
	 * @see #setCSSForDiv(JComponent, String, String)
	 * @see #setCSSForToggle(JToggleButton, String, String)
	 * @since 7.0
	 */
	public void setCSS(final JComponent component, final String className, final String styles){//in user thread
		sizeHeightForXML.setCSSImpl(this, __context, component, className, styles);
	}
	
	/**
	 * set CSS <i>class</i> and/or CSS <i>style</i> for the HTML input tag of {@link JCheckBox} and {@link JRadioButton}.
	 * <BR><BR>
	 * for more, see {@link #setCSS(JComponent, String, String)}.
	 * @param togButton the JComponent (JCheckBox or JRadioButton) to set style.
	 * @param className the class name of styles defined <i>Resources/CSS Styles</i> in designer or {@link #loadCSS(String)}. Null for ignore and keep old value. Empty string for clear.
	 * @param styles the styles defined <i>Resources/CSS Styles</i> in designer or {@link #loadCSS(String)}. Null for ignore and keep old value. Empty string for clear.
	 * @see #setCSSForDiv(JComponent, String, String)
	 * @see #setCSS(JComponent, String, String)
	 * @since 7.0
	 */
	public void setCSSForToggle(final JToggleButton togButton, final String className, final String styles){
		sizeHeightForXML.setCSSForToggleImpl(this, __context, togButton, className, styles);
	}
	
	/**
	 * set attribute 'dir' for a DIV of JComponent.<BR><BR>
	 * Know more :<BR>
	 * 1. a {@link HTMLMlet} will be set RTL by server for the entire page if language of mobile is RTL.<BR>
	 * 2. in some case, a JComponent may be set RTL/LTR different from the entire page.
	 * @param component
	 * @param isRTL true means right to left, false means left to right.
	 * @since 7.40
	 */
	public void setRTL(final JComponent component, final boolean isRTL){
		sizeHeightForXML.setRTLForDivImpl(this, __context, component, isRTL);
	}

	/**
	 * get normal font size in pixels of current session mobile.<BR>
	 * user may change default font size in optional from mobile.
	 * @return the recommended normal font size in pixels, it is normal used for CSS <code>font-size</code>.
	 * @since 7.0
	 * @see #getFontSizeForSmall()
	 * @see #getFontSizeForLarge()
	 */
	public int getFontSizeForNormal(){
		return sizeHeightForXML.getFontSizeForNormal();
	}
	
	/**
	 * get small font size in pixels of current session mobile.<BR>
	 * user may change small font size in optional from mobile.
	 * @return the recommended small font size in pixels, it is normal used for CSS <code>font-size</code>.
	 * @since 7.0
	 * @see #getFontSizeForNormal()
	 * @see #getFontSizeForLarge()
	 */
	public int getFontSizeForSmall(){
		return sizeHeightForXML.getFontSizeForSmall();
	}
	
	/**
	 * get large font size in pixels of current session mobile.<BR>
	 * user may change large font size in optional from mobile.
	 * @return the recommended large font size in pixels, it is normal used for CSS <code>font-size</code>.
	 * @since 7.0
	 * @see #getFontSizeForSmall()
	 * @see #getFontSizeForNormal()
	 */
	public int getFontSizeForLarge(){
		return sizeHeightForXML.getFontSizeForLarge();
	}
	
	/**
	 * get button font size in pixels of current session mobile.<BR>
	 * user may change default font size in optional from mobile.
	 * @return the recommended button font size in pixels, it is normal used for CSS <code>font-size</code>.
	 * @since 7.0
	 * @see #getButtonHeight()
	 */
	public int getFontSizeForButton(){
		return sizeHeightForXML.getFontSizeForButton();
	}
	
	/**
	 * get button height in pixels of current session mobile.
	 * @return the recommended button height in pixels, it is normal used for the size of button area on bottom of mobile.
	 * @since 7.0
	 * @see #getFontSizeForButton()
	 */
	public int getButtonHeight(){
		return sizeHeightForXML.getButtonHeight();
	}
	
	/**
	 * the width pixel of login mobile.
	 * <BR>it is equals with <code>getProjectContext().getMobileWidth()</code>
	 * @return
	 * @since 7.3
	 */
	public int getMobileWidth(){
		return sizeHeightForXML.getMobileWidth(coreSS);
	}
	
	/**
	 * the height pixel of login mobile.
	 * <BR>it is equals with <code>getProjectContext().getMobileHeight()</code>
	 * @return
	 * @since 7.3
	 */
	public int getMobileHeight(){
		return sizeHeightForXML.getMobileHeight(coreSS);
	}
	
	/**
	 * return integer value of color of font, for example : 0x00FF00.
	 * <BR><BR>
	 * <STRONG>Important : </STRONG>the color may be changed in different implementation or version.
	 * @return
	 * @see #getColorForFontByHexString()
	 * @see #getDarkerColor(int)
	 * @see #getBrighterColor(int)
	 * @see #toHexColor(int, boolean)
	 * @since 7.9
	 */
	public static final int getColorForFontByIntValue(){
		return ScriptCSSSizeHeight.getColorForFontByIntValue();
	}

	/**
	 * return hex format string of color of font, for example : "00FF00".
	 * <BR><BR>
	 * <STRONG>Important : </STRONG>the color may be changed in different implementation or version.
	 * @return
	 * @see #getColorForFontByIntValue()
	 * @see #toHexColor(int, boolean)
	 * @since 7.9
	 */
	public static final String getColorForFontByHexString(){
		return ScriptCSSSizeHeight.getColorForFontByHexString();
	}

	/**
	 * convert a int color to hex string.
	 * <BR><BR>
	 * for example :
	 * <BR>
	 * 1. toHexColor(0x0000AABB, false) returns "00aabb",
	 * <BR>
	 * 2. toHexColor(0x0000AABB, true) returns "0000aabb",
	 * <BR>
	 * 3. toHexColor(0xAABBCCDD, false) returns "bbccdd",
	 * <BR>
	 * 4. toHexColor(0xAABBCCDD, true) returns "aabbccdd",
	 * @param color
	 * @param useAlpha true, use the alpha channel.
	 * @return
	 * @since 7.9
	 * @see #getDarkerColor(int)
	 * @see #getBrighterColor(int)
	 */
	public static final String toHexColor(final int color, final boolean useAlpha){
		return ScriptCSSSizeHeight.toHexColor(color, useAlpha);
	}

	/**
	 * return integer value of color of body, for example : 0x00FF00.
	 * <BR><BR>
	 * <STRONG>Important : </STRONG>the color may be changed in different implementation or version.
	 * @return
	 * @see #getColorForBodyByHexString()
	 * @see #getDarkerColor(int)
	 * @see #toHexColor(int, boolean)
	 * @since 7.9
	 */
	public static final int getColorForBodyByIntValue(){
		return ScriptCSSSizeHeight.getColorForBodyByIntValue();
	}
	
	/**
	 * Creates a new Color that is a darker version of this <code>rgbColorWithAlpha</code>.
	 * @param rgbColorWithAlpha
	 * @return
	 * @see #getBrighterColor(int)
	 * @see Color#darker()
	 * @see #toHexColor(int, boolean)
	 */
	public static final int getDarkerColor(final int rgbColorWithAlpha){
		return new Color(rgbColorWithAlpha, true).darker().getRGB();
	}
	
	/**
	 * Creates a new Color that is a brighter version of this <code>rgbColorWithAlpha</code>.
	 * @param rgbColorWithAlpha
	 * @return
	 * @see #getDarkerColor(int)
	 * @see Color#brighter()
	 */
	public static final int getBrighterColor(final int rgbColorWithAlpha){
		return new Color(rgbColorWithAlpha, true).brighter().getRGB();
	}

	/**
	 * return hex format string of color of body, for example : "00FF00".
	 * <BR><BR>
	 * <STRONG>Important : </STRONG>the color may be changed in different implementation or version.
	 * @return
	 * @see #getColorForBodyByIntValue()
	 * @see #toHexColor(int, boolean)
	 * @since 7.9
	 */
	public static final String getColorForBodyByHexString(){
		return ScriptCSSSizeHeight.getColorForBodyByHexString();
	}

}

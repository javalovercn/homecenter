package hc.server.ui;

import hc.core.util.StringUtil;
import hc.core.util.UIUtil;
import hc.server.html5.syn.DifferTodo;
import hc.server.msb.UserThreadResourceUtil;

import java.util.Vector;

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
 * <BR>Not all HTML tag are supported by mobile runtime environment.
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
	/**
	 * @deprecated
	 */
	@Deprecated
	Vector<StyleItem> styleItemToDeliver;
	/**
	 * @deprecated
	 */
	@Deprecated
	Vector<String> stylesToDeliver;
	/**
	 * @deprecated
	 */
	@Deprecated
	Vector<String> jsToDeliver;
	
	/**
	 * @deprecated
	 */
	@Deprecated
	boolean isFlushCSS = false;
	/**
	 * @deprecated
	 */
	@Deprecated
	DifferTodo diffTodo;

	/**
	 * load special styles for current {@link HTMLMlet}, it must be invoked before {@link #setCSS(JComponent, String, String)} which refer to these styles.
	 * <BR><BR>More about CSS styles : 
	 * <BR>
	 * 1. the <i>CSS Styles</i> tab in designer for {@link HTMLMlet} is shared to all {@link HTMLMlet}s in same project.
	 * In other words, it will be loaded automatically by server for each HTMLMlet.
	 * <BR>
	 * 2. this method can be invoked as many times as you want.
	 * <BR>
	 * 3. this method can be invoked also in constructor method (the initialize method in JRuby).
	 * <BR><BR>About cache :<BR>
	 * don't worry about styles too large for re-translating to mobile, <BR>
	 * the cache subsystem of HomeCenter will intelligence analysis to determine whether transmission or loading cache from mobile (if styles is too small, it will not be cached).
	 * What you should do is put more data into one style file, because if there is too much pieces of cache in a project, the system will automatically clear the cache and restart caching.
	 * @param styles for example, "<i>h1 {color:red} p {color:blue}</i>".
	 * @see #setCSS(JComponent, String, String)
	 * @see #setCSSForToggle(JToggleButton, String, String)
	 * @see #setCSSForDiv(JComponent, String, String)
	 * @since 7.0
	 */
	public final void loadCSS(final String styles){
		if(SimuMobile.checkSimuProjectContext(__context)){
			return;
		}
		
		synchronized(this){
			if(status == STATUS_INIT){
				if(stylesToDeliver == null){
					stylesToDeliver = new Vector<String>();
				}
				stylesToDeliver.add(styles);
				return;
			}
			if(diffTodo != null && status < STATUS_EXIT){//由于本方法可能在构造中被调用，而无法确定是否需要后期转发，所以diffTofo条件限于此。
				diffTodo.loadStyles(styles);
			}
		}
	}
	
	/**
	 * 
	 * @param js
	 * @since 7.7
	 */
	final void loadJS(final String js){
		synchronized(this){
			if(status == STATUS_INIT){
				if(jsToDeliver == null){
					jsToDeliver = new Vector<String>();
				}
				jsToDeliver.add(js);
				return;
			}
			if(diffTodo != null && status < STATUS_EXIT){//由于本方法可能在构造中被调用，而无法确定是否需要后期转发，所以diffTofo条件限于此。
				diffTodo.loadJS(js);
			}
		}
	}
	
	/**
	 * set CSS <i>class</i> and/or CSS <i>style</i> for <code>div</code> of {@link JComponent}.
	 * <BR><BR>
	 * for more, see {@link #setCSS(JComponent, String, String)}.
	 * @param component the JComponent to set style.
	 * @param className the class name of styles defined <i>CSS Styles</i> tab in designer or {@link #loadCSS(String)}. Null for ignore and keep old value. Empty string for clear.
	 * @param styles the styles defined <i>CSS Styles</i> tab in designer or {@link #loadCSS(String)}. Null for ignore and keep old value. Empty string for clear.
	 * @see #setCSS(JComponent, String, String)
	 * @see #setCSSForToggle(JToggleButton, String, String)
	 * @since 7.0
	 */
	public final void setCSSForDiv(final JComponent component, String className, final String styles){//in user thread
		if(SimuMobile.checkSimuProjectContext(__context)){
			return;
		}
		
		if(diffTodo == null && status > STATUS_INIT){
			//MletSnapCanvas模式
			return;
		}
		
		if(className != null && className.length() == 0){
			className = null;
		}
		
		synchronized(this){
			if(status == STATUS_INIT){
				if(styleItemToDeliver == null){
					styleItemToDeliver = new Vector<StyleItem>();
				}
				styleItemToDeliver.add(new StyleItem(StyleItem.FOR_DIV, component, className, styles));
				return;
			}
			if(diffTodo != null && status < STATUS_EXIT){//由于本方法可能在构造中被调用，而无法确定是否需要后期转发，所以diffTofo条件限于此。
				diffTodo.setStyleForDiv(diffTodo.buildHcCode(component), className, styles);//in user thread
			}
		}
	}
	
	/**
	 * set CSS <i>class</i> and/or CSS <i>style</i> for {@link JComponent}.
	 * <BR><BR>
	 * it is effective immediately to mobile.<BR>
	 * it is allowed to invoke this method in constructor of {@link HTMLMlet}.
	 * <BR><BR>
	 * know more : 
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
	 * Swing {@link JComponent}s are translated to HTML like following:
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
	 * @param className the class name of styles defined <i>CSS Styles</i> tab in designer or {@link #loadCSS(String)}. Null for ignore and keep old value. Empty string for clear.
	 * @param styles the styles defined <i>CSS Styles</i> tab in designer or {@link #loadCSS(String)}. Null for ignore and keep old value. Empty string for clear.
	 * @see #setCSSForDiv(JComponent, String, String)
	 * @see #setCSSForToggle(JToggleButton, String, String)
	 * @since 7.0
	 */
	public final void setCSS(final JComponent component, String className, final String styles){//in user thread
		if(SimuMobile.checkSimuProjectContext(__context)){
			return;
		}
		
		if(diffTodo == null && status > STATUS_INIT){
			//MletSnapCanvas模式
			return;
		}
		
		if(className != null && className.length() == 0){
			className = null;
		}
		
		if(component instanceof JPanel){
			setCSSForDiv(component, className, styles);//in user thread
			return;
		}else if(component instanceof JToggleButton){
			doForLabelTag((JToggleButton)component, className, styles);//in user thread
			return;
		}
		
		doForInputTag(StyleItem.FOR_JCOMPONENT, component, className, styles);//in user thread
	}

	/**
	 * @deprecated
	 * @param component
	 * @param className
	 * @param styles
	 */
	@Deprecated
	private final void doForInputTag(final int forType, final JComponent component, final String className,
			final String styles) {//in user thread
		synchronized(this){
			if(status == STATUS_INIT){
				if(styleItemToDeliver == null){
					styleItemToDeliver = new Vector<StyleItem>();
				}
				styleItemToDeliver.add(new StyleItem(forType, component, className, styles));
				return;
			}
			if(diffTodo != null && status < STATUS_EXIT){
				diffTodo.setStyleForInputTag(diffTodo.buildHcCode(component), className, styles);//in user thread
			}
		}
	}
	
	/**
	 * set CSS <i>class</i> and/or CSS <i>style</i> for the <STRONG>input</STRONG> tag of {@link JCheckBox} and {@link JRadioButton}.
	 * <BR><BR>
	 * for more, see {@link #setCSS(JComponent, String, String)}.
	 * @param togButton the JComponent (JCheckBox or JRadioButton) to set style.
	 * @param className the class name of styles defined <i>CSS Styles</i> tab in designer or {@link #loadCSS(String)}. Null for ignore and keep old value. Empty string for clear.
	 * @param styles the styles defined <i>CSS Styles</i> tab in designer or {@link #loadCSS(String)}. Null for ignore and keep old value. Empty string for clear.
	 * @see #setCSSForDiv(JComponent, String, String)
	 * @see #setCSS(JComponent, String, String)
	 * @since 7.0
	 */
	public final void setCSSForToggle(final JToggleButton togButton, String className, final String styles){
		if(SimuMobile.checkSimuProjectContext(__context)){
			return;
		}
		
		if(diffTodo == null && status > STATUS_INIT){
			//MletSnapCanvas模式
			return;
		}
		
		if(className != null && className.length() == 0){
			className = null;
		}
		
		doForInputTag(StyleItem.FOR_JTOGGLEBUTTON, togButton, className, styles);//in user thread
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	private final void doForLabelTag(final JToggleButton togButton, final String className, final String styles) {//in user thread
		synchronized(this){
			if(status == STATUS_INIT){
				if(styleItemToDeliver == null){
					styleItemToDeliver = new Vector<StyleItem>();
				}
				styleItemToDeliver.add(new StyleItem(StyleItem.FOR_JCOMPONENT, togButton, className, styles));
				return;
			}
			if(diffTodo != null && status < STATUS_EXIT){
				diffTodo.setStyleForJCheckBoxText(diffTodo.buildHcCode(togButton), className, styles);//in user thread
			}
		}
	}
	
	int fontSizeForNormal, fontSizeForSmall, fontSizeForLarge, fontSizeForButton, buttonHeight;
	
	/**
	 * get normal font size in pixels of current login mobile.<BR>
	 * user may change default font size in optional from mobile.
	 * @return the recommended normal font size in pixels, it is normal used for CSS <code>font-size</code>.
	 * @since 7.0
	 * @see #getFontSizeForSmall()
	 * @see #getFontSizeForLarge()
	 */
	public final int getFontSizeForNormal(){
		if(fontSizeForNormal == 0){
			initFontSize();
		}
		return fontSizeForNormal;
	}
	
	/**
	 * get small font size in pixels of current login mobile.<BR>
	 * user may change small font size in optional from mobile.
	 * @return the recommended small font size in pixels, it is normal used for CSS <code>font-size</code>.
	 * @since 7.0
	 * @see #getFontSizeForNormal()
	 * @see #getFontSizeForLarge()
	 */
	public final int getFontSizeForSmall(){
		if(fontSizeForSmall == 0){
			initFontSize();
		}
		return fontSizeForSmall;
	}
	
	/**
	 * get large font size in pixels of current login mobile.<BR>
	 * user may change large font size in optional from mobile.
	 * @return the recommended large font size in pixels, it is normal used for CSS <code>font-size</code>.
	 * @since 7.0
	 * @see #getFontSizeForSmall()
	 * @see #getFontSizeForNormal()
	 */
	public final int getFontSizeForLarge(){
		if(fontSizeForLarge == 0){
			initFontSize();
		}
		return fontSizeForLarge;
	}
	
	/**
	 * get button font size in pixels of current login mobile.<BR>
	 * user may change default font size in optional from mobile.
	 * @return the recommended button font size in pixels, it is normal used for CSS <code>font-size</code>.
	 * @since 7.0
	 * @see #getButtonHeight()
	 */
	public final int getFontSizeForButton(){
		if(fontSizeForButton == 0){
			initFontSize();
		}
		return fontSizeForButton;
	}
	
	/**
	 * get button height in pixels of current login mobile.
	 * @return the recommended button height in pixels, it is normal used for the size of button area on bottom of mobile.
	 * @since 7.0
	 * @see #getFontSizeForButton()
	 */
	public final int getButtonHeight(){
		if(buttonHeight == 0){
			initFontSize();
		}
		return buttonHeight;
	}
	
	/**
	 * the width pixel of login mobile.
	 * <BR>it is equals with <code>getProjectContext().getMobileWidth()</code>
	 * @return
	 * @since 7.3
	 */
	public final int getMobileWidth(){
		if(coreSS == SimuMobile.SIMU_NULL){
			return SimuMobile.MOBILE_WIDTH;
		}else{
			return UserThreadResourceUtil.getMobileWidthFrom(coreSS);
		}
	}
	
	/**
	 * the height pixel of login mobile.
	 * <BR>it is equals with <code>getProjectContext().getMobileHeight()</code>
	 * @return
	 * @since 7.3
	 */
	public final int getMobileHeight(){
		if(coreSS == SimuMobile.SIMU_NULL){
			return SimuMobile.MOBILE_HEIGHT;
		}else{
			return UserThreadResourceUtil.getMobileHeightFrom(coreSS);
		}
	}
	
	private final void initFontSize(){
		final int width = getMobileWidth();
		final int height = getMobileHeight();
		
		final int maxWH = Math.max(width, height);
		
		fontSizeForNormal = maxWH / 60;
		if(fontSizeForNormal < 14){
			fontSizeForNormal = 14;
		}
		
		fontSizeForSmall = (int)Math.floor(fontSizeForNormal * 0.8);
		if(fontSizeForSmall == fontSizeForNormal){
			fontSizeForSmall = fontSizeForNormal - 1;
		}
		fontSizeForButton = (int)Math.ceil(fontSizeForNormal * 1.2);
		if(fontSizeForButton == fontSizeForNormal){
			fontSizeForButton = fontSizeForNormal + 1;
		}
		fontSizeForLarge = fontSizeForButton;
		
		buttonHeight = fontSizeForButton * 2;//原 * 3在800X600的机器上偏大
	}

	private static final int INT_COLOR_BODY = UIUtil.DEFAULT_COLOR_BACKGROUND;
	private static final String COLOR_BODY = StringUtil.toARGB(INT_COLOR_BODY, false);
	
	private static final int INT_COLOR_FONT = UIUtil.TXT_FONT_COLOR_INT_FOR_ANDROID & 0x00FFFFFF;
	private static final String COLOR_FONT = StringUtil.toARGB(INT_COLOR_FONT, false);
	
	/**
	 * return integer value of color of font, for example : 0x00FF00.
	 * <BR><BR>
	 * <STRONG>Important : </STRONG>the color may be changed in different implementation or version.
	 * @return
	 * @see #getColorForFontByHexString()
	 * @see #toHexColor(int, boolean)
	 * @since 7.9
	 */
	public static final int getColorForFontByIntValue(){
		return INT_COLOR_FONT;
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
		return COLOR_FONT;
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
	 */
	public static final String toHexColor(final int color, final boolean useAlpha){
		return StringUtil.toARGB(color, useAlpha);
	}

	/**
	 * return integer value of color of body, for example : 0x00FF00.
	 * <BR><BR>
	 * <STRONG>Important : </STRONG>the color may be changed in different implementation or version.
	 * @return
	 * @see #getColorForBodyByHexString()
	 * @see #toHexColor(int, boolean)
	 * @since 7.9
	 */
	public static final int getColorForBodyByIntValue(){
		return INT_COLOR_BODY;
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
		return COLOR_BODY;
	}

}

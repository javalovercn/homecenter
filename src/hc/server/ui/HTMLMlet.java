package hc.server.ui;

import hc.server.html5.syn.DifferTodo;

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
 * {@link HTMLMlet} displays JComponents as HTML on mobile (not snapshot), you can set CSS for these JComponents.<BR>
 * <BR>Not all HTML tag are supported in mobile runtime environment.
 * <BR>To support HTML5, Android 4.4 (or later) / iPhone 4s (or later) is required.
 * <BR>
 * To synchronize status with mobile, server will add AWT/Swing listeners for your JComponents, please don't remove them.
 * <BR><BR>
 * <STRONG>Important :</STRONG><BR>when following one of condition is meet, it regards as {@link Mlet} and presents as snapshot to mobile:
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
	boolean isFlushCSS = false;
	/**
	 * @deprecated
	 */
	@Deprecated
	DifferTodo diffTodo;

	/**
	 * invoke this method to load special styles for current {@link HTMLMlet} before invoke {@link #setCSS(JComponent, String, String)}.
	 * <BR><BR><STRONG>Important</STRONG> : The <i>CSS Styles</i> tab in designer for {@link HTMLMlet} is shared to all {@link HTMLMlet}s in current project.
	 * In other words, it will be loaded automatically by server for <STRONG>each</STRONG> HTMLMlet in current project.
	 * <BR><BR>
	 * you can load multiple CSS styles for a {@link HTMLMlet}, in other words, this method can be invoked as many times as you want.
	 * <BR><BR>
	 * this method can be invoked in constructor method (initialize method in JRuby).
	 * <BR><BR>
	 * <STRONG>Tip : </STRONG>you don't worry about styles too large for re-translating to mobile, 
	 * the cache subsystem of HomeCenter will intelligence analysis to determine whether transmission or loading cache from mobile (if styles is too small, it will not be cached).
	 * What you should do is put more data into one style file, because if there is too much pieces of cache in a project, the system will automatically clear the cache and restart caching.
	 * @param styles for example, "<i>h1 {color:red} p {color:blue}</i>".
	 * @see #setCSSForDiv(JComponent, String, String)
	 */
	public final void loadCSS(final String styles){
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
	 * set <i>class</i> and <i>style</i> for <code>div</code> of {@link JComponent}.
	 * <BR><BR>
	 * the <i>class</i> and <i>style</i> will be translated to mobile immediately.<BR>
	 * you can also use this method in constructor of {@link HTMLMlet}.
	 * <BR><BR>
	 * for more, see {@link #setCSS(JComponent, String, String)}.
	 * @param component the JComponent to set style.
	 * @param className the class name of styles defined in designer or {@link #loadCSS(String)}. null to ignore.
	 * @param styles the new styles of HTML tag for the current JComponent. null to ignore.
	 * @see #setCSS(JComponent, String, String)
	 * @see #setCSSForToggle(JToggleButton, String, String)
	 * @since 7.0
	 */
	public final void setCSSForDiv(final JComponent component, String className, final String styles){//in user thread
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
	 * set <i>class</i> and/or <i>style</i> for your {@link JComponent}.
	 * <BR><BR>
	 * the <i>class</i> and/or <i>style</i> will be translated to mobile immediately.<BR>
	 * invoking this method in constructor of {@link HTMLMlet} is allowed.
	 * <BR><BR>
	 * <STRONG>Important</STRONG> : 
	 * <BR>
	 * 1. The effect of CSS depends on the run-time environment of mobile client. Some CSS may be ignored!
	 * <BR>
	 * 2. To get environment of mobile, please reference {@link ProjectContext#getMobileOS()} and {@link ProjectContext#getMobileOSVer()}.
	 * <BR>
	 * 3. If you need resize image for mobile, please resize them first and save them into a jar library, or invoke {@link #resizeImage(java.awt.image.BufferedImage, int, int)}.
	 * <BR>It is not recommend to resize image by your implementation, because the HAR project may be executed in Swing/J2SE environment for Android which is NOT standard J2SE.
	 * <BR>
	 * 4. The best practice is <STRONG>JComponent + LayoutManager + Event + CSS</STRONG>(Note : the implementation of Swing/J2SE for Android is differentiated from Oracle J2SE, if your HAR runs in Android server).
	 * <BR>
	 * 5. If your UI is ugly, please ask your CSS artist for pleasantly surprised!
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
	 * <td>&nbsp;</td>
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
	 * <td>&nbsp;</td>
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
	 * <code>progress</code> is tag in HTML5, so <br>Android 4.4 (or later) or iPhone 4s (or later) is required.
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
	 * <td>the 'range' is tag in HTML5, <br>Android 4.4 (or later) or iPhone 4s (or later) is required.
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
	 * @param className the class name of styles defined in designer (see HTMLMlet panel in designer) or {@link #loadCSS(String)}. Null for ignore and keep old value. Empty string for clear.
	 * @param styles the new styles for HTML tag of the current JComponent. Null for ignore and keep old value. Empty string for clear.
	 * @see #setCSSForDiv(JComponent, String, String)
	 * @see #setCSSForToggle(JToggleButton, String, String)
	 * @since 7.0
	 */
	public final void setCSS(final JComponent component, String className, final String styles){//in user thread
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
	 * set <i>class</i> and <i>style</i> for the <STRONG>input</STRONG> tag of {@link JCheckBox} and {@link JRadioButton}.
	 * <BR><BR>
	 * the <i>class</i> and <i>style</i> will be translated to mobile immediately.<BR>
	 * you can also use this method in constructor of {@link HTMLMlet}.
	 * <BR><BR>
	 * for more, see {@link #setCSS(JComponent, String, String)}.
	 * @param togButton the JComponent (JCheckBox or JRadioButton) to set style.
	 * @param className the class name of styles defined in designer or {@link #loadCSS(String)}. Null for ignore and keep old value. Empty string for clear.
	 * @param styles the new styles for HTML tag of the current JComponent. Null for ignore and keep old value. Empty string for clear.
	 * @see #setCSSForDiv(JComponent, String, String)
	 * @see #setCSS(JComponent, String, String)
	 * @since 7.0
	 */
	public final void setCSSForToggle(final JToggleButton togButton, String className, final String styles){
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
	 * user may change default font size in optional in mobile.
	 * @return recommended normal font size in pixels, it is normal used for CSS <code>font-size</code>.
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
	 * user may change small font size in optional in mobile.
	 * @return recommended small font size in pixels, it is normal used for CSS <code>font-size</code>.
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
	 * user may change large font size in optional in mobile.
	 * @return recommended large font size in pixels, it is normal used for CSS <code>font-size</code>.
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
	 * user may change default font size in optional in mobile.
	 * @return recommended button font size in pixels, it is normal used for CSS <code>font-size</code>.
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
	 * @return recommended button height in pixels, it is normal used for the size of button area on bottom of mobile.
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
		return __context.getMobileWidth();
	}
	
	/**
	 * the height pixel of login mobile.
	 * <BR>it is equals with <code>getProjectContext().getMobileHeight()</code>
	 * @return
	 * @since 7.3
	 */
	public final int getMobileHeight(){
		return __context.getMobileHeight();
	}
	
	private final void initFontSize(){
		final int width = __context.getMobileWidth();
		final int height = __context.getMobileHeight();
		
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
		
		buttonHeight = fontSizeForButton * 3;
	}

}

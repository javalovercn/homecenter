package hc.server.ui;

import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.core.util.UIUtil;
import hc.server.html5.syn.DifferTodo;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.design.J2SESession;

import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

public class SizeHeightForXML {
	int fontSizeForNormal, fontSizeForSmall, fontSizeForLarge, fontSizeForButton, buttonHeight;
	final J2SESession coreSS;
	public DifferTodo diffTodo;
	Vector<StyleItem> styleItemToDeliver;
	Vector<String> stylesToDeliver;
	Vector<String> scriptToDeliver;
	Vector<String> jsToDeliver;
	boolean isFlushCSS = false;
	
	public final void loadCSSImpl(final Mlet mlet, final ProjectContext ctx, final String styles){
		if(SimuMobile.checkSimuProjectContext(ctx)){
			return;
		}
		
		synchronized(mlet.synLock){
			if(mlet.status == Mlet.STATUS_INIT){
				if(stylesToDeliver == null){
					stylesToDeliver = new Vector<String>();
				}
				stylesToDeliver.add(styles);
				return;
			}
			if(diffTodo != null && mlet.status < Mlet.STATUS_EXIT){//由于本方法可能在构造中被调用，而无法确定是否需要后期转发，所以diffTofo条件限于此。
				diffTodo.loadStyles(styles);
			}
		}
	}
	
	final void setInnerHTML(final Mlet mlet, final ScriptPanel scriptPanel, final String innerHTML){
		synchronized(mlet.synLock){
			if(mlet.status == Mlet.STATUS_INIT){
				LogManager.errToLog("invalid status to setInnerHTML : \n" + innerHTML);
				return;
			}
			if(diffTodo != null && mlet.status < Mlet.STATUS_EXIT){//由于本方法可能在构造中被调用，而无法确定是否需要后期转发，所以diffTofo条件限于此。
				final int hashID = diffTodo.buildHcCode(scriptPanel);
				diffTodo.setDivInnerHTML(hashID, innerHTML);
			}
		}
	}
	
	final void executeScript(final Mlet mlet, final String js){
		synchronized(mlet.synLock){
			if(mlet.status == Mlet.STATUS_INIT){
				if(jsToDeliver == null){
					jsToDeliver = new Vector<String>();
				}
				jsToDeliver.add(js);
				return;
			}
			if(diffTodo != null && mlet.status < Mlet.STATUS_EXIT){//由于本方法可能在构造中被调用，而无法确定是否需要后期转发，所以diffTofo条件限于此。
				diffTodo.executeJS(js);
			}
		}
	}
	
	/**
	 * 
	 * @param js
	 * @since 7.7
	 */
	final void loadScript(final Mlet mlet, final String js){
		synchronized(mlet.synLock){
			if(mlet.status == Mlet.STATUS_INIT){
				if(scriptToDeliver == null){
					scriptToDeliver = new Vector<String>();
				}
				scriptToDeliver.add(js);
				return;
			}
			if(diffTodo != null && mlet.status < Mlet.STATUS_EXIT){//由于本方法可能在构造中被调用，而无法确定是否需要后期转发，所以diffTofo条件限于此。
				diffTodo.loadScript(js);
			}
		}
	}
	
	public final void setCSSImpl(final Mlet mlet, final ProjectContext ctx, final JComponent component, final String className, final String styles){//in user thread
		if(SimuMobile.checkSimuProjectContext(ctx) || component == null){
			return;
		}
		
		if(diffTodo == null && mlet.status > Mlet.STATUS_INIT){
			//MletSnapCanvas模式
			return;
		}
		
		if(className == null && styles == null){
			return;
		}
		
		if(component instanceof JPanel){
			if(mlet instanceof HTMLMlet){
				((HTMLMlet)mlet).setCSSForDiv(component, className, styles);//in user thread
			}
			return;
		}else if(component instanceof JToggleButton){
			doForLabelTag(mlet, (JToggleButton)component, className, styles);//in user thread
			return;
		}
		
		doForInputTag(mlet, StyleItem.FOR_JCOMPONENT, component, className, styles);//in user thread
	}
	
	public final void setCSSForDivImpl(final Mlet mlet, final ProjectContext ctx, final JComponent component, final String className, final String styles){//in user thread
		if(SimuMobile.checkSimuProjectContext(ctx) || component == null){
			return;
		}
		
		if(diffTodo == null && mlet.status > Mlet.STATUS_INIT){
			//MletSnapCanvas模式
			return;
		}
		
		if(className == null && styles == null){
			return;
		}
		
		synchronized(mlet.synLock){
			if(mlet.status == Mlet.STATUS_INIT){
				if(styleItemToDeliver == null){
					styleItemToDeliver = new Vector<StyleItem>();
				}
				styleItemToDeliver.add(new StyleItem(StyleItem.FOR_DIV, component, className, styles));
				return;
			}
			if(diffTodo != null && mlet.status < Mlet.STATUS_EXIT){//由于本方法可能在构造中被调用，而无法确定是否需要后期转发，所以diffTofo条件限于此。
				diffTodo.setStyleForDiv(diffTodo.buildHcCode(component), className, styles);//in user thread
			}
		}
	}
	
	public final void setCSSForToggleImpl(final Mlet mlet, final ProjectContext ctx, final JToggleButton togButton, final String className, final String styles) {
		if(SimuMobile.checkSimuProjectContext(ctx) || togButton == null){
			return;
		}
		
		if(diffTodo == null && mlet.status > Mlet.STATUS_INIT){
			//MletSnapCanvas模式
			return;
		}
		
		if(className == null && styles == null){
			return;
		}
		
		doForInputTag(mlet, StyleItem.FOR_JTOGGLEBUTTON, togButton, className, styles);//in user thread
	}
	
	private final void doForLabelTag(final Mlet mlet, final JToggleButton togButton, final String className, final String styles) {//in user thread
		synchronized(mlet.synLock){
			if(mlet.status == Mlet.STATUS_INIT){
				if(styleItemToDeliver == null){
					styleItemToDeliver = new Vector<StyleItem>();
				}
				styleItemToDeliver.add(new StyleItem(StyleItem.FOR_JCOMPONENT, togButton, className, styles));
				return;
			}
			if(diffTodo != null && mlet.status < Mlet.STATUS_EXIT){
				diffTodo.setStyleForJCheckBoxText(diffTodo.buildHcCode(togButton), className, styles);//in user thread
			}
		}
	}
	
	private final void doForInputTag(final Mlet mlet, final int forType, final JComponent component, final String className,
			final String styles) {//in user thread
		synchronized(mlet.synLock){
			if(mlet.status == Mlet.STATUS_INIT){
				if(styleItemToDeliver == null){
					styleItemToDeliver = new Vector<StyleItem>();
				}
				styleItemToDeliver.add(new StyleItem(forType, component, className, styles));
				return;
			}
			if(diffTodo != null && mlet.status < Mlet.STATUS_EXIT){
				diffTodo.setStyleForInputTag(diffTodo.buildHcCode(component), className, styles);//in user thread
			}
		}
	}
	
	public SizeHeightForXML(final J2SESession coreSS){
		this.coreSS = coreSS;
	}
	
	public final int getFontSizeForNormal(){
		if(fontSizeForNormal == 0){
			initFontSize();
		}
		return fontSizeForNormal;
	}
	
	public final int getFontSizeForSmall(){
		if(fontSizeForSmall == 0){
			initFontSize();
		}
		return fontSizeForSmall;
	}
	
	public final int getFontSizeForLarge(){
		if(fontSizeForLarge == 0){
			initFontSize();
		}
		return fontSizeForLarge;
	}
	
	public final int getFontSizeForButton(){
		if(fontSizeForButton == 0){
			initFontSize();
		}
		return fontSizeForButton;
	}
	
	public final int getButtonHeight(){
		if(buttonHeight == 0){
			initFontSize();
		}
		return buttonHeight;
	}
	
	public final int getMobileWidth(final J2SESession coreSS){
		if(coreSS == SimuMobile.SIMU_NULL){
			return SimuMobile.MOBILE_WIDTH;
		}else{
			return UserThreadResourceUtil.getMobileWidthFrom(coreSS);
		}
	}
	
	public final int getMobileHeight(final J2SESession coreSS){
		if(coreSS == SimuMobile.SIMU_NULL){
			return SimuMobile.MOBILE_HEIGHT;
		}else{
			return UserThreadResourceUtil.getMobileHeightFrom(coreSS);
		}
	}
	
	private final void initFontSize(){
		final int width = getMobileWidth(coreSS);
		final int height = getMobileHeight(coreSS);
		
		final int maxWH = Math.max(width, height);
		if(maxWH < 800){
			fontSizeForNormal = maxWH / 45;
		}else if(maxWH < 1200){
			fontSizeForNormal = maxWH / 50;
		}else if(maxWH < 1600){
			fontSizeForNormal = maxWH / 55;
		}else if(maxWH < 2000){
			fontSizeForNormal = maxWH / 60;
		}else{
			fontSizeForNormal = maxWH / 65;
		}

		if(fontSizeForNormal < 14){
			fontSizeForNormal = 14;
		}
		
		fontSizeForSmall = (int)Math.floor(fontSizeForNormal * 0.7);
		if(fontSizeForSmall == fontSizeForNormal){
			fontSizeForSmall = fontSizeForNormal - 1;
		}
		fontSizeForButton = (int)Math.ceil(fontSizeForNormal * 1.3);
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

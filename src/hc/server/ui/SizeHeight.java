package hc.server.ui;

import hc.core.util.StringUtil;
import hc.core.util.UIUtil;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.design.J2SESession;

public class SizeHeight {

	int fontSizeForNormal;
	int fontSizeForSmall;
	int fontSizeForLarge;
	int fontSizeForButton;
	int buttonHeight;
	int dialogBorderRadius;
	protected final J2SESession coreSS;
	private static final int INT_COLOR_BODY = UIUtil.DEFAULT_COLOR_BACKGROUND;

	public final int getFontSizeForNormal() {
		if(fontSizeForNormal == 0){
			initFontSize();
		}
		return fontSizeForNormal;
	}

	public final int getFontSizeForSmall() {
		if(fontSizeForSmall == 0){
			initFontSize();
		}
		return fontSizeForSmall;
	}

	public final int getFontSizeForLarge() {
		if(fontSizeForLarge == 0){
			initFontSize();
		}
		return fontSizeForLarge;
	}

	public final int getFontSizeForButton() {
		if(fontSizeForButton == 0){
			initFontSize();
		}
		return fontSizeForButton;
	}
	
	public final int getDialogBorderRadius(){
		if(dialogBorderRadius == 0){
			initFontSize();
		}
		return dialogBorderRadius;
	}

	public final int getButtonHeight() {
		if(buttonHeight == 0){
			initFontSize();
		}
		return buttonHeight;
	}

	public final int getMobileWidth(final J2SESession coreSS) {
		if(coreSS == SimuMobile.SIMU_NULL){
			return SimuMobile.MOBILE_WIDTH;
		}else{
			return UserThreadResourceUtil.getMletWidthFrom(coreSS);
		}
	}

	public final int getMobileHeight(final J2SESession coreSS) {
		if(coreSS == SimuMobile.SIMU_NULL){
			return SimuMobile.MOBILE_HEIGHT;
		}else{
			return UserThreadResourceUtil.getMletHeightFrom(coreSS);
		}
	}

	private final void initFontSize() {
		final int width = getMobileWidth(coreSS);
		final int height = getMobileHeight(coreSS);
		
		final int maxWH = Math.max(width, height);
		
		dialogBorderRadius = Math.round(maxWH / 100f);
		
		if(maxWH < 800){
			fontSizeForNormal = maxWH / 44;//45
		}else if(maxWH < 1200){
			fontSizeForNormal = maxWH / 48;//50
		}else if(maxWH < 1600){
			fontSizeForNormal = maxWH / 50;//55
		}else if(maxWH < 2000){
			fontSizeForNormal = maxWH / 60;
		}else{
			fontSizeForNormal = maxWH / 65;
		}
	
		final int baseFontSize = 14;
		
		if(fontSizeForNormal < baseFontSize){
			fontSizeForNormal = baseFontSize;
		}
		
		fontSizeForSmall = (int)Math.floor(fontSizeForNormal * 0.7);
		if(fontSizeForNormal == baseFontSize){
			fontSizeForSmall+=2;
		}
		fontSizeForButton = (int)Math.ceil(fontSizeForNormal * 1.3);
		if(fontSizeForNormal == baseFontSize){
			fontSizeForButton-=2;
		}
		fontSizeForLarge = fontSizeForButton;
		
		buttonHeight = fontSizeForButton * 2;//原 * 3在800X600的机器上偏大
	}

	private static final String COLOR_BODY = StringUtil.toARGB(INT_COLOR_BODY, false);
	private static final int INT_COLOR_FONT = UIUtil.TXT_FONT_COLOR_INT_FOR_MLET & 0x00FFFFFF;
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
	public static final int getColorForFontByIntValue() {
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
	public static final String getColorForFontByHexString() {
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
	public static final String toHexColor(final int color, final boolean useAlpha) {
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
	public static final int getColorForBodyByIntValue() {
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
	public static final String getColorForBodyByHexString() {
		return COLOR_BODY;
	}

	public SizeHeight(final J2SESession coreSS) {
		this.coreSS = coreSS;
	}

}
package hc.server.ui;

import hc.core.util.StringUtil;
import hc.core.util.UIUtil;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.design.J2SESession;

public class SizeHeight {
	protected final J2SESession coreSS;
	private static final int INT_COLOR_BODY = UIUtil.DEFAULT_COLOR_BACKGROUND;

	public SizeHeight(final J2SESession coreSS) {
		this.coreSS = coreSS;
	}

	public final int getFontSizeForNormal() {
		if (coreSS == SimuMobile.SIMU_NULL) {
			return SimuMobile.MOBILE_FONT_SIZE_FOR_BUTTON;
		} else {
			return coreSS.clientFontSize.fontSizeForNormal;
		}
	}

	public final int getFontSizeForSmall() {
		if (coreSS == SimuMobile.SIMU_NULL) {
			return SimuMobile.MOBILE_FONT_SIZE_FOR_BUTTON;
		} else {
			return coreSS.clientFontSize.fontSizeForSmall;
		}
	}

	public final int getFontSizeForLarge() {
		if (coreSS == SimuMobile.SIMU_NULL) {
			return SimuMobile.MOBILE_FONT_SIZE_FOR_BUTTON;
		} else {
			return coreSS.clientFontSize.fontSizeForLarge;
		}
	}

	public final int getFontSizeForButton() {
		if (coreSS == SimuMobile.SIMU_NULL) {
			return SimuMobile.MOBILE_FONT_SIZE_FOR_BUTTON;
		} else {
			return coreSS.clientFontSize.fontSizeForButton;
		}
	}

	public final int getDialogBorderRadius() {
		if (coreSS == SimuMobile.SIMU_NULL) {
			return SimuMobile.MOBILE_BORDER_RADIUS;
		} else {
			return coreSS.clientFontSize.dialogBorderRadius;
		}
	}

	public final int getButtonHeight() {
		if (coreSS == SimuMobile.SIMU_NULL) {
			return SimuMobile.MOBILE_BUTTON_HEIGHT;
		} else {
			return coreSS.clientFontSize.buttonHeight;
		}
	}

	public final int getMobileWidth() {
		if (coreSS == SimuMobile.SIMU_NULL) {
			return SimuMobile.MOBILE_WIDTH;
		} else {
			return UserThreadResourceUtil.getMletWidthFrom(coreSS);
		}
	}

	public final int getMobileHeight() {
		if (coreSS == SimuMobile.SIMU_NULL) {
			return SimuMobile.MOBILE_HEIGHT;
		} else {
			return UserThreadResourceUtil.getMletHeightFrom(coreSS);
		}
	}

	private static final String COLOR_BODY = StringUtil.toARGB(INT_COLOR_BODY, false);
	private static final int INT_COLOR_FONT = UIUtil.TXT_FONT_COLOR_INT_FOR_MLET & 0x00FFFFFF;
	private static final String COLOR_FONT = StringUtil.toARGB(INT_COLOR_FONT, false);

	/**
	 * return integer value of color of font, for example : 0x00FF00. <BR>
	 * <BR>
	 * <STRONG>Important : </STRONG>the color may be changed in different implementation or version.
	 * 
	 * @return
	 * @see #getColorForFontByHexString()
	 * @see #toHexColor(int, boolean)
	 * @since 7.9
	 */
	public static final int getColorForFontByIntValue() {
		return INT_COLOR_FONT;
	}

	/**
	 * return hex format string of color of font, for example : "00FF00". <BR>
	 * <BR>
	 * <STRONG>Important : </STRONG>the color may be changed in different implementation or version.
	 * 
	 * @return
	 * @see #getColorForFontByIntValue()
	 * @see #toHexColor(int, boolean)
	 * @since 7.9
	 */
	public static final String getColorForFontByHexString() {
		return COLOR_FONT;
	}

	/**
	 * convert a int color to hex string. <BR>
	 * <BR>
	 * for example : <BR>
	 * 1. toHexColor(0x0000AABB, false) returns "00aabb", <BR>
	 * 2. toHexColor(0x0000AABB, true) returns "0000aabb", <BR>
	 * 3. toHexColor(0xAABBCCDD, false) returns "bbccdd", <BR>
	 * 4. toHexColor(0xAABBCCDD, true) returns "aabbccdd",
	 * 
	 * @param color
	 * @param useAlpha
	 *            true, use the alpha channel.
	 * @return
	 * @since 7.9
	 */
	public static final String toHexColor(final int color, final boolean useAlpha) {
		return StringUtil.toARGB(color, useAlpha);
	}

	/**
	 * return integer value of color of body, for example : 0x00FF00. <BR>
	 * <BR>
	 * <STRONG>Important : </STRONG>the color may be changed in different implementation or version.
	 * 
	 * @return
	 * @see #getColorForBodyByHexString()
	 * @see #toHexColor(int, boolean)
	 * @since 7.9
	 */
	public static final int getColorForBodyByIntValue() {
		return INT_COLOR_BODY;
	}

	/**
	 * return hex format string of color of body, for example : "00FF00". <BR>
	 * <BR>
	 * <STRONG>Important : </STRONG>the color may be changed in different implementation or version.
	 * 
	 * @return
	 * @see #getColorForBodyByIntValue()
	 * @see #toHexColor(int, boolean)
	 * @since 7.9
	 */
	public static final String getColorForBodyByHexString() {
		return COLOR_BODY;
	}

}
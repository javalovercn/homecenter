package hc.server.ui;

public class ClientFontSize {
	int fontSizeForNormal;
	int fontSizeForSmall;
	int fontSizeForLarge;
	int fontSizeForButton;
	int buttonHeight;
	int dialogBorderRadius;

	public final boolean updateFontSize(final int maxWH, final int fontSizeForLarge,
			final int fontSizeForNormal, final int fontSizeForSmall, final int fontSizeForButton) {
		boolean isEquals = true;
		// isEquals &= (this.fontSizeForLarge == fontSizeForLarge);
		isEquals &= (this.fontSizeForNormal == fontSizeForNormal);
		// isEquals &= (this.fontSizeForSmall == fontSizeForSmall);
		isEquals &= (this.fontSizeForButton == fontSizeForButton);

		this.fontSizeForLarge = fontSizeForLarge;
		this.fontSizeForNormal = fontSizeForNormal;
		this.fontSizeForSmall = fontSizeForSmall;
		this.fontSizeForButton = fontSizeForButton;

		dialogBorderRadius = Math.round(maxWH / 300f);//原/100，在iPhone X偏大
		buttonHeight = fontSizeForButton * 2;// 原 * 3在800X600的机器上偏大

		return isEquals;
	}

}

package hc.server.ui.design;

import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.Dialog;
import hc.server.ui.HTMLMlet;
import hc.server.ui.ProjectContext;
import hc.util.ResourceUtil;

import java.awt.Color;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public class SystemHTMLMlet extends HTMLMlet {
	public static final String CENTER_FOR_DIV = "display: -webkit-box; display: -moz-box; display: -ms-flexbox; display: -webkit-flex;" +
			"display: flex; -webkit-box-align: center; -moz-box-align: center; -ms-flex-align: center;" +
			"-webkit-align-items: center; align-items: center; justify-content: center; -webkit-justify-content: center;" +
			"-webkit-box-pack: center; -moz-box-pack: center; -ms-flex-pack: center;";
	
	static final String BTN_STYLE = "btnStyle";
	static final String BTN_FOR_DIV = "btnForDiv";
	static final String BTN_DISABLE_STYLE = "btnDisableStyle";

	protected final void setButtonStyle(final JButton button){
		setCSSForDiv(button, BTN_FOR_DIV, null);
		setCSS(button, BTN_STYLE, null);
	}
	
	protected final J2SESession localCoreSS;
	
	public SystemHTMLMlet(){
		this.localCoreSS = getCoreSS();
	}

	final static J2SESession getCoreSS() {
		return UserThreadResourceUtil.getCoreSSFromCtx(ProjectContext.getProjectContext());
	}
	
	public static int getButtonHeight(final int height1, final int height2){
		return Math.max(height1, height2);
	}
	
	public final void setButtonEnable(final JButton button, final boolean isEnable){
		button.setEnabled(isEnable);
		if(isEnable){
			setCSS(button, BTN_STYLE, null);
		}else{
			setCSS(button, BTN_DISABLE_STYLE, null);
		}
	}
	
	public final String getRes(final int id){
		return (String)ResourceUtil.get(localCoreSS, id);
	}
	
	public final void setComboBoxEnable(final JComboBox comboBox, final boolean isEnable){
		comboBox.setEnabled(isEnable);
	}

	public static void setAreaCSS(final HTMLMlet mlet, final JTextArea area, final int normalFontSize) {
		final int areaBackColor = new Color(HTMLMlet.getColorForBodyByIntValue(), true).darker().getRGB();
		final int areaFontColor = new Color(HTMLMlet.getColorForFontByIntValue(), true).darker().getRGB();
		mlet.setCSSForDiv(area, "areaForDiv", null);
		mlet.setCSS(area, null, "width:100%;height:100%;" +
				"overflow:scroll;font-size:" + normalFontSize + "px;" +
				"background-color:#" + HTMLMlet.toHexColor(areaBackColor, false) + ";color:#" + HTMLMlet.toHexColor(areaFontColor, false) + ";");
	}
	
	public static void setLabelCSS(final HTMLMlet mlet, final JLabel label){
		mlet.setCSSForDiv(label, null, "display: table-cell;vertical-align: middle;");
	}
	
	public static void setButtonPanelCSS(final Dialog dialog, final JPanel panel){
		dialog.setCSSForDiv(panel, null, "display: flex;align-items: center;");
	}

	public static String buildCSS(final int buttonHeight, final int fontSize, final int fontColor, final int bgColor) {
//		final String strHexFontColor = HTMLMlet.toHexColor(fontColor, false);
		final int darkerFontColor = HTMLMlet.getDarkerColor(fontColor);
		final String strHexFontColor = HTMLMlet.toHexColor(HTMLMlet.getDarkerColor(darkerFontColor), false);
		final String fontDisableColor = HTMLMlet.toHexColor(darkerFontColor, false);
		final String buttonBGColor = HTMLMlet.toHexColor(HTMLMlet.getBrighterColor(bgColor), false);
		final String buttonDisableBGColor = HTMLMlet.toHexColor(HTMLMlet.getDarkerColor(bgColor), false);
		
		return ".btnForDiv {padding:0.2em;" +
				"-webkit-box-align: center;" +//display: -webkit-box;display: -moz-box;display: -ms-flexbox;display: -webkit-flex;display: flex;
				"-moz-box-align: center;-ms-flex-align: center;-webkit-align-items: center;align-items: center;justify-content: center;" +
				"-webkit-justify-content: center;-webkit-box-pack: center;-moz-box-pack: center;-ms-flex-pack: center;display: table-cell;}" +
				
				".areaForDiv {padding:0.4em;}" +
				
				".btnStyle {width:100%;height:100%;border-radius: " + buttonHeight + "px;display: block;transition: all 0.15s ease;" +
				"border: 0em solid #"+ buttonBGColor + ";background-color: #" + buttonBGColor + ";color: #"+ strHexFontColor + ";" +
				"font-size:" + fontSize + "px;}" +
				
				".btnDisableStyle {width:100%;height:100%;border-radius: " + buttonHeight + "px;display: block;transition: all 0.15s ease;" +
				"border: 0em solid #"+ buttonDisableBGColor + ";background-color: #" + buttonDisableBGColor + ";" +
				"color: #"+ fontDisableColor + ";font-size:" + fontSize + "px;}";
	}
}

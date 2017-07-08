package hc.server.ui.design;

import hc.server.ui.Dialog;
import hc.util.ResourceUtil;

import javax.swing.JButton;
import javax.swing.JComboBox;

public class SystemDialog extends Dialog {
	protected final J2SESession localCoreSS;
	
	public SystemDialog(){
		this.localCoreSS = getCoreSS();
	}

	public final void setButtonEnable(final JButton button, final boolean isEnable){
		button.setEnabled(isEnable);
		if(isEnable){
			setCSS(button, SystemHTMLMlet.BTN_STYLE, null);
		}else{
			setCSS(button, SystemHTMLMlet.BTN_DISABLE_STYLE, null);
		}
	}
	
	protected static J2SESession getCoreSS() {
		return SystemHTMLMlet.getCoreSS();
	}
	
	protected final void setButtonStyle(final JButton button){
		setCSSForDiv(button, SystemHTMLMlet.BTN_FOR_DIV, null);
		setCSS(button, SystemHTMLMlet.BTN_STYLE, null);
	}
	
	public final String getRes(final int id){
		return (String)ResourceUtil.get(localCoreSS, id);
	}
	
	public final void setComboBoxEnable(final JComboBox comboBox, final boolean isEnable){
		comboBox.setEnabled(isEnable);
	}
}

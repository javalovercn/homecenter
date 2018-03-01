package hc.server.ui.design;

import hc.server.ui.Dialog;
import hc.util.ResourceUtil;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class SystemDialog extends Dialog {
	private static final long serialVersionUID = 1009823211182676192L;
	protected final J2SESession localCoreSS;
	private boolean isCanceled;

	public SystemDialog() {
		this.localCoreSS = getCoreSS();
	}

	public final void setButtonEnable(final JButton button, final boolean isEnable) {
		button.setEnabled(isEnable);
		if (isEnable) {
			setCSS(button, SystemHTMLMlet.BTN_STYLE, null);
		} else {
			setCSS(button, SystemHTMLMlet.BTN_DISABLE_STYLE, null);
		}
	}

	public final void setCanceled() {
		isCanceled = true;
	}

	public final boolean isCanceled() {
		return isCanceled;
	}

	protected static J2SESession getCoreSS() {
		return SystemHTMLMlet.getCoreSS();
	}

	protected final void setButtonStyle(final JButton button) {
		setCSSForDiv(button, SystemHTMLMlet.BTN_FOR_DIV, null);
		setCSS(button, SystemHTMLMlet.BTN_STYLE, null);
	}

	protected final void setFieldCSS(final JTextField field) {
		setCSSForDiv(field, null, SystemHTMLMlet.CENTER_FOR_DIV);
		setCSS(field, null, SystemHTMLMlet.FIELD_STYLE);
		field.setColumns(18);
	}

	protected final void setLabelCSS(final JLabel label, final boolean isAutoNewLine) {
		setCSSForDiv(label, null, isAutoNewLine ? SystemHTMLMlet.LABEL_FOR_DIV_AUTO_NEW_LINE
				: SystemHTMLMlet.LABEL_FOR_DIV);
		setCSS(label, null, SystemHTMLMlet.LABEL_STYLE);
	}

	public final String getRes(final int id) {
		return ResourceUtil.get(localCoreSS, id);
	}

	public final void setComboBoxEnable(final JComboBox comboBox, final boolean isEnable) {
		comboBox.setEnabled(isEnable);
	}
}

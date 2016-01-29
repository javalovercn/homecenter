package hc.server.ui.design.hpj;

import java.awt.Color;

import javax.swing.JButton;
import javax.swing.JLabel;

import hc.core.HCTimer;

public class JRubyErrorHCTimer extends HCTimer {
	JLabel errRunInfo = null;
	JButton testBtn = null;
	
	public JRubyErrorHCTimer(String name, int ms, boolean enable) {
		super(name, ms, enable);
	}
	
	public void setErrorLable(final JLabel info, final JButton button){
		errRunInfo = info;
		testBtn = button;
	}
	
	@Override
	public final void doBiz() {
		errRunInfo.setBackground(testBtn.getBackground());
		errRunInfo.setForeground(testBtn.getForeground());
		
		errRunInfo.setText(" ");
		
		setEnable(false);
	}

}

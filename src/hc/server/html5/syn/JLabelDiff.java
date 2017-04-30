package hc.server.html5.syn;

import javax.swing.Icon;
import javax.swing.JLabel;

public class JLabelDiff extends JComponentDiff{
	
	@Override
	public void diff(final int hcCode, final Object src, final DifferTodo todo) {
		super.diff(hcCode, src, todo);
		
		final JLabel labelSrc = (JLabel)src;
		
		{
			final String value = labelSrc.getText();
			if(value != null && value.length() > 0){
				todo.notifyModifyLabelText(labelSrc, hcCode, value);
			}
		}
		
		{
			if(labelSrc.isEnabled()){
				final Icon iconValue = labelSrc.getIcon();
				if(iconValue != null){
					todo.notifyModifyIcon(hcCode, iconValue);
				}
			}else{
				final Icon iconValue = labelSrc.getDisabledIcon();
				if(iconValue != null){
					todo.notifyModifyDisabledIcon(hcCode, iconValue);
				}
			}
		}
		
	}
}

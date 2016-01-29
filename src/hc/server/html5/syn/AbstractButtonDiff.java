package hc.server.html5.syn;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JToggleButton;

public class AbstractButtonDiff extends JComponentDiff{
	
	@Override
	public void diff(final int hcCode, Object src, DifferTodo todo) {
		super.diff(hcCode, src, todo);
		
		AbstractButton btnSrc = (AbstractButton)src;
		
		todo.notifyModifyAbstractButtonText(src);
		
		{
			final boolean isSelected = btnSrc.isSelected();
			if(isSelected){
				todo.notifyModifyButtonSelected(hcCode, isSelected);
			}
		}
		
		{
			if(btnSrc.isEnabled()){
				final Icon iconValue = btnSrc.getIcon();
				if(iconValue != null){
					todo.notifyModifyIcon(hcCode, iconValue);
				}
			}else{
				final Icon iconValue = btnSrc.getDisabledIcon();
				if(iconValue != null){
					todo.notifyModifyDisabledIcon(hcCode, iconValue);
				}
			}
		}
		
	}
	
}

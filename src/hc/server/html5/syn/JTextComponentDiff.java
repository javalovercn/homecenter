package hc.server.html5.syn;

import hc.util.JSUtil;

import javax.swing.text.JTextComponent;

public class JTextComponentDiff extends JComponentDiff{
	
	@Override
	public void diff(final int hcCode, final Object src, final DifferTodo todo) {
		super.diff(hcCode, src, todo);
		
		final JTextComponent textCompSrc = (JTextComponent)src;
		
		{
			final String value = textCompSrc.getText();
			if(value != null && value.length() > 0){
				sendModifyText(hcCode, todo, textCompSrc, value);
			}
		}
		
		{
			final boolean isEditable = textCompSrc.isEditable();
			if(isEditable == false){
				todo.notifyModifyTextComponentEditable(hcCode, isEditable);
			}
		}
		
	}

	public static void sendModifyText(final int hcCode, final DifferTodo todo,
			final JTextComponent textCompSrc, String value) {
		value = JSUtil.replaceShuanYinHao(value);
		
		if(JPanelDiff.isTextMultLinesEditor(textCompSrc)){
			value = JSUtil.replaceNewLine(value);
			todo.notifyModifyTextAreaText(hcCode, value);
		}else{
			todo.notifyModifyTextComponentText(hcCode, value);
		}
	}
	
}

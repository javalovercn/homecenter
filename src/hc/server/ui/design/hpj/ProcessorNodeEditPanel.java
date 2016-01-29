package hc.server.ui.design.hpj;

public class ProcessorNodeEditPanel extends JRubyNodeEditPanel {
	public ProcessorNodeEditPanel(){
		super();
		nameLabel.setText("Name :");
//		nameField.setColumns(20);
	}
	
	@Override
	public String getListener() {
		return ((HPProcessor)currItem).listener;
	}
	
	@Override
	public void updateScript(final String script) {
		((HPProcessor)currItem).listener = script;
		
		designer.setNeedRebuildTestJRuby(true);
	}
	
}

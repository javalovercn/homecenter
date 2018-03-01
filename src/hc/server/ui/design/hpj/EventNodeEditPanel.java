package hc.server.ui.design.hpj;

public class EventNodeEditPanel extends JRubyNodeEditPanel {
	public EventNodeEditPanel() {
		super();
		nameLabel.setText("Event :");
		nameField.setColumns(20);
		nameField.setEnabled(false);
	}
}

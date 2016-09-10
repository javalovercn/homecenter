package hc.server.ui.design.hpj;

import hc.App;
import hc.server.HCActionListener;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIUtil;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class NativeNodeEditPanel extends NameEditPanel {
	final VerTextPanel verPanel = new VerTextPanel("native file");

	public NativeNodeEditPanel() {
		super();

		final VerTextField verTextField = verPanel.verTextField;
		verTextField.getDocument().addDocumentListener(new DocumentListener() {
			private void modify() {
				((HCShareFileResource) currItem).ver = verTextField.getText();
				notifyModified(true);
			}

			@Override
			public void removeUpdate(final DocumentEvent e) {
				modify();
			}

			@Override
			public void insertUpdate(final DocumentEvent e) {
				modify();
			}

			@Override
			public void changedUpdate(final DocumentEvent e) {
				modify();
			}
		});
		verTextField.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HCShareFileResource) currItem).ver = verTextField.getText();
				App.invokeLaterUI(updateTreeRunnable);
				notifyModified(true);
			}
		}, threadPoolToken));

		final JPanel center = new JPanel();
		center.setLayout(new BorderLayout());
		center.add(verPanel, BorderLayout.NORTH);

		final JLabel noteLabel = new JLabel("<html><STRONG>Note :</STRONG>" +
				"<BR>1. the native lib will be automatically loaded (System.load) by server before event <STRONG>" + ProjectContext.EVENT_SYS_PROJ_STARTUP + "</STRONG>." +
				"<BR>2. they are loaded in accordance with the sequence of the tree nodes." +
				"<BR>3. if native lib is changed and upgraded, restarting server may be required after upgrading HAR package." +
				"<BR>4. they will be loaded in all operation system (Windows, Mac, Linux...). If you want load they by yourself, please put them as resources in jar.</html>");
		final JComponent[] components = {center, //new JSeparator(SwingConstants.HORIZONTAL), 
				noteLabel};		
		add(ServerUIUtil.buildNorthPanel(components, 0, BorderLayout.CENTER), BorderLayout.CENTER);
	}

	@Override
	public void extendInit() {
		verPanel.verTextField.setText(((HCShareFileResource) currItem).ver);
	}
}
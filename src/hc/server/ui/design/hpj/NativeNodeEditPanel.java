package hc.server.ui.design.hpj;

import hc.App;
import hc.server.HCActionListener;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.NativeOSManager;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class NativeNodeEditPanel extends NameEditPanel {
	final VerTextPanel verPanel = new VerTextPanel("native file", true, false);
	
	final JCheckBox cb_window = new JCheckBox("Windows");
	final JCheckBox cb_linux = new JCheckBox("Linux");
	final JCheckBox cb_mac = new JCheckBox("Mac OS X");
	final JCheckBox cb_android = new JCheckBox("Android");
	
	public NativeNodeEditPanel() {
		super();
		
		cb_window.setToolTipText("current lib is valid for Windows if checked!");
		cb_linux.setToolTipText("current lib is valid for Linux if checked!");
		cb_mac.setToolTipText("current lib is valid for Mac OS X if checked!");
		cb_android.setToolTipText("current lib is valid for Android if checked!");
		
		cb_window.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				tog(NativeOSManager.OS_WINDOW, cb_window.isSelected());
			}
		});
		cb_linux.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				tog(NativeOSManager.OS_LINUX, cb_linux.isSelected());
			}
		});
		cb_mac.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				tog(NativeOSManager.OS_MAC_OSX, cb_mac.isSelected());
			}
		});
		cb_android.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				tog(NativeOSManager.OS_ANDROID, cb_android.isSelected());
			}
		});
		
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
		
		final JPanel osPanel = new JPanel(new BorderLayout());
		osPanel.setBorder(new TitledBorder("For OS"));
		{
			final JPanel osListPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
			osListPanel.add(cb_android);
			osListPanel.add(cb_window);
			osListPanel.add(cb_linux);
			osListPanel.add(cb_mac);
			
			osPanel.add(osListPanel, BorderLayout.CENTER);
			osPanel.add(new JLabel("<html>" +
					"1. select the OS that the local library can run.<BR>" +
					"2. if hosting OS is not selected, the current library will not be loaded in that OS." +
					"</html>"), BorderLayout.SOUTH);
		}
		
		center.add(osPanel, BorderLayout.CENTER);
		
		final JLabel noteLabel = new JLabel("<html><STRONG>Note :</STRONG>" +
				"<BR>1. the native lib will be automatically loaded (System.load) by server before event <STRONG>" + ProjectContext.EVENT_SYS_PROJ_STARTUP + "</STRONG>." +
				"<BR>2. they are loaded in accordance with the sequence of the tree nodes." +
				"<BR>3. if native lib is changed and upgraded, restarting server may be required after upgrading HAR package." +
				"<BR>4. in order to meet the complex loading situation, put them as resources in jar or download it online." +
				"<BR>5. permission [" + ResourceUtil.LOAD_NATIVE_LIB + "] is required.</html>");
		final JComponent[] components = {center, //new JSeparator(SwingConstants.HORIZONTAL), 
				noteLabel};		
		add(ServerUIUtil.buildNorthPanel(components, 0, BorderLayout.CENTER), BorderLayout.CENTER);
	}

	@Override
	public void extendInit() {
		final HPShareNative hcNative = (HPShareNative)currItem;
		verPanel.verTextField.setText(hcNative.ver);
		
		final int osMask = hcNative.osMask;
		
		final boolean isWindow = NativeOSManager.isMatchOS(NativeOSManager.OS_WINDOW, osMask);
		cb_window.setSelected(isWindow);
		final boolean isLinux = NativeOSManager.isMatchOS(NativeOSManager.OS_LINUX, osMask);
		cb_linux.setSelected(isLinux);
		final boolean isMac = NativeOSManager.isMatchOS(NativeOSManager.OS_MAC_OSX, osMask);
		cb_mac.setSelected(isMac);
		final boolean isAndroid = NativeOSManager.isMatchOS(NativeOSManager.OS_ANDROID, osMask);
		cb_android.setSelected(isAndroid);
	}

	private final void tog(final int os, final boolean isSelected) {
		notifyModified(true);
		
		final HPShareNative hcNative = (HPShareNative)currItem;
		if(isSelected){
			hcNative.osMask |= os;
		}else{
			hcNative.osMask ^= os;
		}
	}
}
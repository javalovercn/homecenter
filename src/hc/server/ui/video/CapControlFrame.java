package hc.server.ui.video;

import hc.App;
import hc.res.ImageSrc;
import hc.server.SingleJFrame;
import hc.server.ThirdlibManager;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.TitledBorder;

public class CapControlFrame extends SingleJFrame implements ActionListener{
	final JPanel checkInstall = new JPanel();
	final String NO_JMF_INSTALL = "No Java Media Framework Installed";
	final JTextField installField = new JTextField();
	final CapturePanel cp;
	final JTabbedPane tabbedPanel = new JTabbedPane();
	final CapPreviewPane previewPane;
	final CapViewer searchPane;
	final JButton saveButton, exitButton;
	
	@Override
	public void dispose(){
		cp.dispose();
		searchPane.frame = null;
		super.dispose();
	}
	
	private String getJMFInstallDir(){
		Vector<String> v = ThirdlibManager.getClassPath();
		final String jfmjar = "jmf.jar"; 
		for (int i = 0; i < v.size(); i++) {
			final java.lang.String item = v.elementAt(i);
			if(item.endsWith(jfmjar)){
				return item.substring(0, item.length() - 4 - jfmjar.length());//lib\ 
			}
		}
		return "";
	}

	public CapControlFrame() {
		setTitle((String)ResourceUtil.get(9048));
		setIconImage(App.SYS_LOGO);

		previewPane = new CapPreviewPane(this);
		searchPane = new CapViewer(this);
		
		saveButton = new JButton((String) ResourceUtil.get(1017), new ImageIcon(ImageSrc.OK_ICON));
		exitButton = new JButton((String) ResourceUtil.get(1011), new ImageIcon(ImageSrc.CANCEL_ICON));
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new GridLayout(1, 2, 5, 5));
		bottomPanel.add(saveButton);
		bottomPanel.add(exitButton);
		
		saveButton.addActionListener(this);
		exitButton.addActionListener(this);
		
		cp = new CapturePanel(CaptureConfig.getInstance(), this, previewPane);
		final JFrame self = this;

		JPanel centerPane = new JPanel();
		this.setContentPane(centerPane);
//		Container container = getContentPane();
//		container.add(centerPanel);
		
		{
			JLabel installLabel = new JLabel("JMF (Java Media Framework) install directory :");
			
			installField.setEditable(false);
			installField.setColumns(40);
			JPanel installStatusP = new JPanel(new GridBagLayout());
			installStatusP.add(installLabel, new GridBagConstraints(0, 0, 1, 1, 1, 1,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(5, 10, 5, 10), 0, 0));
			installStatusP.add(installField, new GridBagConstraints(0, 1, 1, 1, 1, 1,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(5, 10, 5, 10), 0, 0));
			
			checkInstall.setLayout(new BorderLayout());
			checkInstall.add(installStatusP, BorderLayout.NORTH);
		}
		Icon icon = null;
		try {
			icon = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/setup_22.png")));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		tabbedPanel.addTab("1.  ", icon, checkInstall);
		try {
			icon = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/configure_22.png")));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		tabbedPanel.addTab("2.  ", icon, cp);
		try {
			icon = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/camera_22.png")));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		tabbedPanel.addTab("3.  ", icon, previewPane);
		try {
			icon = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/search_22.png")));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		tabbedPanel.addTab("4.  ", icon, searchPane);
		
		Insets insets = new Insets(5, 5, 5, 5);
		centerPane.setLayout(new GridBagLayout());
		tabbedPanel.setBorder(new TitledBorder(""));
		centerPane.add(tabbedPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0,
				0));
		centerPane.add(bottomPanel, new GridBagConstraints(0, 1, 1, 1, 0.0,
				0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
				insets, 0, 0));
		
		getRootPane().registerKeyboardAction(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}},
	            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
	            JComponent.WHEN_IN_FOCUSED_WINDOW);

		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
			}
		});
		pack();
		setResizable(false);
		App.showCenter(this);	

		rescan(installField);
	}

	private boolean rescan(final JTextField installField) {
		boolean hasJMF = false;
		String dir = getJMFInstallDir();
		if(dir.length() == 0){
			dir = NO_JMF_INSTALL;
		}else{
			hasJMF = true;
		}
		installField.setText(dir);
		if(hasJMF){
			ThirdlibManager.loadClassPath();
			if(checkHasCap()){
				cp.init();
				tabbedPanel.setSelectedIndex(1);
			}
		}
		return hasJMF;
	}
	
	private boolean checkHasCap(){
		try{
			Vector v = CaptureDeviceManager.getDeviceList(null);
			if ((v == null) || (v.size() < 1)) {
				return false;
			}
			for (int i = 0; i < v.size(); i++) {
				String deviceName = ((CaptureDeviceInfo)v.elementAt(i)).getName();
				if(deviceName.indexOf("vfw") >= 0){
					return true;
				}
			}
		}catch (Throwable e) {
		}
		return false;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		final Object source = e.getSource();
		if(source == saveButton){
			cp.save();
			previewPane.setCtrlEnabled(cp.checkUseVideo.isSelected());
			previewPane.save();
			searchPane.save();
			
			PropertiesManager.saveFile();

			CapManager.notifyMsg(cp.checkUseVideo.isSelected()?CapStream.CAP_ENABLE:CapStream.CAP_DISABLE);

			JPanel panel = new JPanel();
			panel.add(new JLabel((String)ResourceUtil.get(9056)));
			App.showCenterPanel(panel, 200, 150, "Save, OK", false, null, null, null, null, this, true, false, null, false, false);
		}else if(source == exitButton){
			previewPane.dispose();
			dispose();
		}
	}
}

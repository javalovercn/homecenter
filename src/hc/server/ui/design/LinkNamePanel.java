package hc.server.ui.design;

import hc.App;
import hc.server.HCActionListener;
import hc.server.ui.ServerUIUtil;
import hc.server.util.ContextSecurityConfig;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;
import hc.util.SocketEditPanel;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class LinkNamePanel  extends JPanel {
	final ThreadGroup threadPoolToken = App.getThreadPoolToken();
	final JTextField linkNameField = new JTextField(20);
	final JTextField projRemarkField = new JTextField(20);
	final JCheckBox perm_write = new JCheckBox("write, exclude private file");
	final JCheckBox perm_exec = new JCheckBox("execute");
	final JCheckBox perm_del = new JCheckBox("delete, exclude private file");
	final JCheckBox perm_exit = new JCheckBox("exit");

	private final JCheckBox checkReadProperty = new JCheckBox(HCPermissionConstant.READ_SYSTEM_PROPERTIES);
	private final JCheckBox checkWriteProperty = new JCheckBox(HCPermissionConstant.WRITE_SYSTEM_PROPERTIES);
	final JCheckBox perm_memAccessSystem = new JCheckBox(HCPermissionConstant.MEMBER_ACCESS_SYSTEM);
	
	private final JCheckBox checkLoadLib = new JCheckBox(ResourceUtil.LOAD_NATIVE_LIB);
	private final JCheckBox checkRobot = new JCheckBox("create java.awt.Robot");
//	private final JCheckBox checkListenAllAWTEvents = new JCheckBox("listen all AWT events");
//	private final JCheckBox checkAccessClipboard = new JCheckBox("access clipboard");
	private final JCheckBox checkShutdownHooks = new JCheckBox("access shutdown hooks");
	private final JCheckBox checkSetIO = new JCheckBox("set system IO");
	private final JCheckBox checkSetFactory = new JCheckBox("set Factory");
	
	public SocketEditPanel perm_sock_panel;
	boolean isModiPermission = false;
	final ContextSecurityConfig csc;
	final LinkProjectStore lps;
	public final String CANCLE = "-1";
	final JButton resetPermission = new JButton((String)ResourceUtil.get(9090));
	
	public LinkNamePanel(final String linkName, final String desc, final ContextSecurityConfig csconfig, 
			final LinkProjectStore lpstore) {
		this.csc = csconfig;
		this.lps = lpstore;
		
		perm_sock_panel = new SocketEditPanel() {
			@Override
			public void notifyModify() {
				isModiPermission = true;
			}
			
			@Override
			public final ContextSecurityConfig getCSCSource(){
				return csc;
			}
		};
		
		resetPermission.setToolTipText("reset permissions to the initial status of the design.");
		resetPermission.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				ContextSecurityConfig.resetPermission(csc, lps);
				
				refreshUI(csc);
				isModiPermission = true;
			}
		}, App.getThreadPoolToken()));
		
		if(linkName != null){
			linkNameField.setText(linkName);
		}
		if(desc != null){
			projRemarkField.setText(desc);
		}
		
		perm_write.setToolTipText(HCPermissionConstant.WRITE_TIP);
		perm_exec.setToolTipText(HCPermissionConstant.EXECUTE_TIP);
		perm_del.setToolTipText(HCPermissionConstant.DELETE_TIP);
		perm_exit.setToolTipText(HCPermissionConstant.EXIT_TIP);
		
		checkReadProperty.setToolTipText(HCPermissionConstant.READ_PROP_TIP);
		checkWriteProperty.setToolTipText(HCPermissionConstant.WRITE_PROP_TIP);
		perm_memAccessSystem.setToolTipText(HCPermissionConstant.MEMBER_ACCESS_SYSTEM_TIP);
		
		checkLoadLib.setToolTipText(HCPermissionConstant.LOAD_LIB_TIP);
		checkRobot.setToolTipText(HCPermissionConstant.ROBOT_TIP);
//		checkListenAllAWTEvents.setToolTipText(HCPermissionConstant.LISTEN_ALL_AWT_EVENTS_TIP);
//		checkAccessClipboard.setToolTipText(HCPermissionConstant.ACCESS_CLIPBOARD_TIP);
		checkShutdownHooks.setToolTipText(HCPermissionConstant.SHUTDOWN_HOOKS_TIP);
		checkSetIO.setToolTipText(HCPermissionConstant.SETIO_TIP);
		checkSetFactory.setToolTipText(HCPermissionConstant.SET_FACTORY_TIP);
		
		refreshUI(csc);
		
		checkReadProperty.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				csc.setSysPropRead(checkReadProperty.isSelected());
				isModiPermission = true;
			}
		}, threadPoolToken));			
		checkWriteProperty.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				csc.setSysPropWrite(checkWriteProperty.isSelected());
				isModiPermission = true;
			}
		}, threadPoolToken));	
		checkLoadLib.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				csc.setLoadLib(checkLoadLib.isSelected());
				isModiPermission = true;
			}
		}, threadPoolToken));	
		checkRobot.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				csc.setRobot(checkRobot.isSelected());
				isModiPermission = true;
			}
		}, threadPoolToken));	
//		checkListenAllAWTEvents.addItemListener(new HCActionListener(new Runnable() {
//			@Override
//			public void run() {
//				csc.setListenAllAWTEvents(checkListenAllAWTEvents.isSelected());
//				isModiPermission = true;
//			}
//		}, threadPoolToken));	
//		checkAccessClipboard.addItemListener(new HCActionListener(new Runnable() {
//			@Override
//			public void run() {
//				csc.setAccessClipboard(checkAccessClipboard.isSelected());
//				isModiPermission = true;
//			}
//		}, threadPoolToken));	
		checkShutdownHooks.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				csc.setShutdownHooks(checkShutdownHooks.isSelected());
				isModiPermission = true;
			}
		}, threadPoolToken));	
		checkSetIO.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				csc.setSetIO(checkSetIO.isSelected());
				isModiPermission = true;
			}
		}, threadPoolToken));	
		checkSetFactory.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				csc.setSetFactory(checkSetFactory.isSelected());
				isModiPermission = true;
			}
		}, threadPoolToken));	
		
		perm_write.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				csc.setWrite(perm_write.isSelected());
				isModiPermission = true;
			}
		}, threadPoolToken));	
		perm_exec.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				csc.setExecute(perm_exec.isSelected());
				isModiPermission = true;
			}
		}, threadPoolToken));	
		perm_del.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				csc.setDelete(perm_del.isSelected());
				isModiPermission = true;
			}
		}, threadPoolToken));	
		perm_exit.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				csc.setExit(perm_exit.isSelected());
				isModiPermission = true;
			}
		}, threadPoolToken));	
		perm_memAccessSystem.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				csc.setMemberAccessSystem(perm_memAccessSystem.isSelected());
				isModiPermission = true;
			}
		}, threadPoolToken));	
		
		final JPanel composeLinkName = new JPanel(new GridLayout(2, 1));
		final JPanel composeComment = new JPanel(new GridLayout(2, 1));
		{
			final JPanel panel = new JPanel(new BorderLayout());
			
			final JLabel nameLabel = new JLabel((String)ResourceUtil.get(8021));
			final Font oldfont = nameLabel.getFont();
			nameLabel.setFont(new Font(oldfont.getFontName(), Font.BOLD, oldfont.getSize()));
			
			panel.add(nameLabel, BorderLayout.LINE_START);
			panel.add(linkNameField, BorderLayout.CENTER);
			
			composeLinkName.add(panel);
			composeLinkName.add(new JLabel("<html>it is folder name for the entrance to current project in your mobile menu.</html>"));
			
			composeLinkName.setBorder(new TitledBorder((String)ResourceUtil.get(8021)));
		}
		{
			final JPanel panel = new JPanel(new BorderLayout());
			panel.add(new JLabel((String)ResourceUtil.get(8022)), BorderLayout.LINE_START);
			panel.add(projRemarkField, BorderLayout.CENTER);
			
			composeComment.add(panel);
			composeComment.add(new JLabel("comment information of the project"));
			
			composeComment.setBorder(new TitledBorder((String)ResourceUtil.get(8022)));
		}
		
		setLayout(new BorderLayout());
		final JTabbedPane tabbedPane = new JTabbedPane();
		{
			final JComponent[] components = {composeLinkName, composeComment};
			tabbedPane.addTab((String)ResourceUtil.get(9095), ServerUIUtil.buildNorthPanel(components, 0, BorderLayout.CENTER));
		}
		
		final JPanel osPermPanel = new JPanel(new GridLayout(2, 2));
		osPermPanel.add(perm_write);
		osPermPanel.add(perm_exec);
		osPermPanel.add(perm_del);
		osPermPanel.add(perm_exit);
		final JPanel sysPropPanel = new JPanel(new GridLayout(1, 4));
		sysPropPanel.add(checkReadProperty);
		sysPropPanel.add(checkWriteProperty);
		
		sysPropPanel.add(perm_memAccessSystem);
		
		final JPanel sysOtherPropPanel = new JPanel(new GridLayout(2, 3));
		sysOtherPropPanel.add(checkLoadLib);
		sysOtherPropPanel.add(checkRobot);
		sysOtherPropPanel.add(checkSetIO);
		sysOtherPropPanel.add(checkShutdownHooks);
		sysOtherPropPanel.add(checkSetFactory);
//		sysOtherPropPanel.add(checkListenAllAWTEvents);
//		sysOtherPropPanel.add(checkAccessClipboard);
		final JComponent[] components = {osPermPanel, new JSeparator(SwingConstants.HORIZONTAL), 
				sysPropPanel, new JSeparator(SwingConstants.HORIZONTAL), 
				perm_sock_panel, new JSeparator(SwingConstants.HORIZONTAL),
				sysOtherPropPanel
				};
		final JPanel buildNorthPanel =  ServerUIUtil.buildNorthPanel(components, 0, BorderLayout.CENTER);
		
		{
			final JPanel permission = new JPanel();
			permission.setLayout(new BorderLayout());
			permission.add(buildNorthPanel, BorderLayout.CENTER);
			permission.add(resetPermission, BorderLayout.SOUTH);

			if(ResourceUtil.isJ2SELimitFunction()){
				tabbedPane.addTab((String)ResourceUtil.get(9094), permission);
			}
		}
		
		final int defaultIdx = Integer.parseInt(PropertiesManager.getValue(PropertiesManager.p_ProjModiTabIdx, "0"));
		tabbedPane.setSelectedIndex(defaultIdx);
		
		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				final String newTabIdx = String.valueOf(tabbedPane.getSelectedIndex());
				PropertiesManager.setValue(PropertiesManager.p_ProjModiTabIdx, newTabIdx);
			}
		});
		
		add(tabbedPane, BorderLayout.CENTER);		
	}

	private void refreshUI(final ContextSecurityConfig contextSecurityConfig) {
		perm_write.setSelected(contextSecurityConfig.isWrite());
		perm_exec.setSelected(contextSecurityConfig.isExecute());
		perm_del.setSelected(contextSecurityConfig.isDelete());
		perm_exit.setSelected(contextSecurityConfig.isExit());
		
		checkReadProperty.setSelected(contextSecurityConfig.isSysPropRead());
		checkWriteProperty.setSelected(contextSecurityConfig.isSysPropWrite());
		perm_memAccessSystem.setSelected(contextSecurityConfig.isMemberAccessSystem());
		
		checkLoadLib.setSelected(contextSecurityConfig.isLoadLib());
		checkRobot.setSelected(contextSecurityConfig.isRobot());
//		checkListenAllAWTEvents.setSelected(contextSecurityConfig.isListenAllAWTEvents());
//		checkAccessClipboard.setSelected(contextSecurityConfig.isAccessClipboard());
		checkShutdownHooks.setSelected(contextSecurityConfig.isShutdownHooks());
		checkSetIO.setSelected(contextSecurityConfig.isSetIO());
		checkSetFactory.setSelected(contextSecurityConfig.isSetFactory());
		
		perm_sock_panel.refresh(contextSecurityConfig);
	}
}
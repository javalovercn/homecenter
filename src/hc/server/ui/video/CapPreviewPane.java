package hc.server.ui.video;

import hc.App;
import hc.core.IConstant;
import hc.core.util.IMsgNotifier;
import hc.server.AbstractDelayBiz;
import hc.server.DelayServer;
import hc.server.ui.NumberFormatTextField;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

public class CapPreviewPane extends JPanel implements ActionListener{
	private JButton startButton;
	private JButton stopButton;
	private JCheckBox previewCheck;
	CapControlFrame frame;
	private JPanel previewPane;
	private Icon recordIcon, pauseIcon, resumeIcon, stopIcon;
	private final JCheckBox enableSnap = new JCheckBox();
	private JLabel snapLabelW = new JLabel("snap condtion: width >= "), snapLabelH = new JLabel(" and height >=");
	private final NumberFormatTextField diffWidth = new NumberFormatTextField(), 
			diffHeight = new NumberFormatTextField(), snapMS = new NumberFormatTextField();
	private final CapPreviewNotify notify = new CapPreviewNotify();
	private final String CMD_RECORD = (String)ResourceUtil.get(9049);
	private final String CMD_PAUSE = (String)ResourceUtil.get(9050);
	private final String CMD_RESUME = (String)ResourceUtil.get(9051);
	final JLabel msLabel = new JLabel(" millisecond.");
	final JLabel backSlash = new JLabel(", / ");
	
	public void save(){
		PropertiesManager.setValue(PropertiesManager.p_CapPreview, 
				previewCheck.isSelected()?IConstant.TRUE:IConstant.FALSE);
		PropertiesManager.setValue(PropertiesManager.p_CapSnapWidth, diffWidth.getText());
		PropertiesManager.setValue(PropertiesManager.p_CapSnapHeight, diffHeight.getText());
		String ms = snapMS.getText();
		if(Integer.parseInt(ms) < 200){
			ms = "200";
		}
		PropertiesManager.setValue(PropertiesManager.p_CapSnapMS, ms);
		
		MovDetector.updateDiffMin(diffWidth.getText(), diffHeight.getText());
	}
	
	public void dispose(){
		notify.setCapPreviewPane(null);
		CapManager.removeListener(notify);
	}
	
	private final AbstractDelayBiz actionDelay;
	
	public void setCtrlEnabled(boolean enable){
		startButton.setEnabled(enable);
		stopButton.setEnabled(enable);
		previewCheck.setEnabled(enable);
	}
	
	private static JPanel addFourPanel(JPanel center, Component component, String fourDirctory){
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(center, BorderLayout.CENTER);
		panel.add(component, fourDirctory);
		return panel;
	}

	public CapPreviewPane(final CapControlFrame frame){
		this.frame = frame;
		
		startButton = new JButton(CMD_RECORD);
		stopButton = new JButton((String)ResourceUtil.get(9052));
		
		try{
			recordIcon = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/record_22.png")));
			pauseIcon = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/pause_22.png")));
			resumeIcon = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/play_22.png")));
			stopIcon = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/stop_22.png")));
		}catch (Exception e) {
		}
		
		startButton.setIcon(recordIcon);
		stopButton.setIcon(stopIcon);
		
		diffWidth.setText(CaptureConfig.getSnapWidth());
		diffHeight.setText(CaptureConfig.getSnapHeight());
		snapMS.setText(CaptureConfig.getSnapMS());
		
		previewPane = new JPanel();
		
		previewPane.setLayout(new BorderLayout());
		
		setLayout(new BorderLayout());
		add(previewPane, BorderLayout.CENTER);
		
		previewPane.add(new JLabel("No preview", SwingConstants.CENTER), BorderLayout.CENTER);
		
		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		previewCheck = new JCheckBox((String)ResourceUtil.get(9053), 
				PropertiesManager.isTrue(PropertiesManager.p_CapPreview));
		
		previewCheck.addActionListener(this);
		
		buttonsPanel.add(previewCheck);
		buttonsPanel.add(startButton);
		buttonsPanel.add(stopButton);
		
		startButton.addActionListener(this);
		stopButton.addActionListener(this);
		
		//in arbic , order will be from rtl, so force from west to east
		JPanel snapPanel = new JPanel(new BorderLayout());
		snapPanel.add(msLabel, BorderLayout.WEST);
		snapPanel = addFourPanel(snapPanel, snapMS, BorderLayout.WEST);
		snapPanel = addFourPanel(snapPanel, backSlash, BorderLayout.WEST);
		snapPanel = addFourPanel(snapPanel, diffHeight, BorderLayout.WEST);
		snapPanel = addFourPanel(snapPanel, snapLabelH, BorderLayout.WEST);
		snapPanel = addFourPanel(snapPanel, diffWidth, BorderLayout.WEST);
		snapPanel = addFourPanel(snapPanel, snapLabelW, BorderLayout.WEST);
		
		diffHeight.setEnabled(false);
		
		final String tip = "When detect moving object on min block pixel size, " +
				"system snapshot and save for query";
		enableSnap.setText("detect and snap when recording");
		enableSnap.setToolTipText(tip);
		snapLabelW.setToolTipText(tip);
		enableSnap.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean enable = !diffHeight.isEnabled();
				
				enableCap(enable);
				
				PropertiesManager.setValue(PropertiesManager.p_CapNotSnap, (!enable)?IConstant.TRUE:IConstant.FALSE);
			}
		});
		{
			final boolean SnapSelected = PropertiesManager.isTrue(PropertiesManager.p_CapNotSnap) == false;
			enableCap(SnapSelected);
			enableSnap.setSelected(SnapSelected);
		}
		
		msLabel.setToolTipText("detect moving object on each this millisecond.");
		
		JPanel composePanel = new JPanel(new BorderLayout());
		composePanel.add(buttonsPanel, BorderLayout.NORTH);
		{
			JPanel snapCompPanel = new JPanel(new BorderLayout());
			JPanel snapFlowPanle = new JPanel(new FlowLayout(FlowLayout.LEFT));
			snapFlowPanle.add(enableSnap);
			snapCompPanel.add(snapFlowPanle, BorderLayout.NORTH);
			snapCompPanel.add(snapPanel, BorderLayout.SOUTH);
			composePanel.add(snapCompPanel, BorderLayout.SOUTH);
		}
		
		composePanel.setBorder(new TitledBorder(""));
		add(composePanel, BorderLayout.NORTH);
		
		refreshCapStatus();
		
		notify.setCapPreviewPane(this);
		CapManager.addListener(notify);
		
		final JPanel self = this;
		actionDelay = new AbstractDelayBiz(null) {
			@Override
			public void doBiz() {
				Object source = getPara();
				if(source == startButton){
					startButton.setEnabled(false);
					self.repaint();
					doStart();
					startButton.setEnabled(true);
					if(PropertiesManager.isTrue(PropertiesManager.p_IsReadedCAPCrash) == false){
						new Thread(){
							public void run(){
								JPanel panel = new JPanel(new BorderLayout());
								panel.add(new JLabel("<html>" +
										"More bigger capture size and more snapshot frequently maybe cause crash !!!<BR><BR>" +
										"please chose best capture size and snap frequence,<BR>" +
										"and test it for 10 minutes." +
										"<BR><BR></html>", App.getSysIcon(App.SYS_WARN_ICON), SwingConstants.LEFT),
										BorderLayout.CENTER);
								final JCheckBox checkBox = new JCheckBox("don't display me");
								checkBox.addActionListener(new ActionListener() {
									@Override
									public void actionPerformed(ActionEvent e) {
										String isReaded = checkBox.isSelected()?IConstant.TRUE:IConstant.FALSE;
										PropertiesManager.setValue(PropertiesManager.p_IsReadedCAPCrash, isReaded);
										PropertiesManager.saveFile();
									}
								});
								panel.add(checkBox, BorderLayout.SOUTH);
								App.showCenterPanel(panel, 0, 0, "NOTE", false, null, null, null, null, frame, true, false, null, false, false);
							}
						}.start();
					}
				}else if(source == stopButton){
					stopButton.setEnabled(false);
					startButton.setEnabled(false);
					self.repaint();
					doStop();
					stopButton.setEnabled(true);
					startButton.setEnabled(true);
					isAddedPreview = false;
				}else if(source == previewCheck){
					previewCheck.setEnabled(false);
					self.repaint();
					refreshPreviewPane();
					
					previewCheck.setEnabled(true);
				}
				refreshCapStatus();
			}
		};
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		actionDelay.setPara(e.getSource());
		DelayServer.getInstance().addDelayBiz(actionDelay);
	}

	private boolean isAddedPreview = false;
	
	private void refreshPreviewPane() {
		boolean needPreview = false;
		
		if(previewCheck.isSelected()){
			CapStream cs = CapStream.getInstance(false);
			final int status = cs.getCapStatus();
			if(status == CapStream.CAP_RECORDING){
				needPreview = true;
			}
		}
		
		if(needPreview != isAddedPreview){
			previewPane.removeAll();
			if(needPreview){
//				final Component monitor = CapStream.getInstance(false).getMonitor();
				previewPane.add(CapStream.getInstance(false).getMonitor(), BorderLayout.CENTER);
				frame.pack();
//				App.showCenter(frame);
			}else{
				previewPane.add(new JLabel("No preview", SwingConstants.CENTER), BorderLayout.CENTER);
			}
			isAddedPreview = needPreview;
		}
	}

	void refreshCapStatus(){
		
		CapStream cs = CapStream.getInstance(true);
		final int status = (cs==null?CapStream.CAP_NO_WORKING:cs.getCapStatus());
		if(status == CapStream.CAP_NO_WORKING){
			stopButton.setVisible(false);
			startButton.setText(CMD_RECORD);
			startButton.setIcon(recordIcon);
			startButton.setVisible(true);
//			previewCheck.setEnabled(false);
		}else if(status == CapStream.CAP_RECORDING){
			stopButton.setVisible(true);
			startButton.setText(CMD_PAUSE);
			startButton.setIcon(pauseIcon);
			startButton.setVisible(true);
//			previewCheck.setEnabled(true);
			
			refreshPreviewPane();
		}else if(status == CapStream.CAP_PAUSEING){
			stopButton.setVisible(true);
			startButton.setText(CMD_RESUME);
			startButton.setIcon(resumeIcon);
			startButton.setVisible(true);
		}
	}

	private void enableCap(boolean enable) {
		msLabel.setEnabled(enable);
		snapMS.setEnabled(enable);
		backSlash.setEnabled(enable);
		diffHeight.setEnabled(enable);
		snapLabelH.setEnabled(enable);
		diffWidth.setEnabled(enable);
		snapLabelW.setEnabled(enable);
	}

	public static void doStop() {
		CapStream.getInstance(false).actionPerformed(ProgressThread.ACTION_STOP);
	}

	public static void doStart() {
		CapStream cs = CapStream.getInstance(false);
		final int status = cs.getCapStatus();
		if(status == CapStream.CAP_NO_WORKING){
			if(cs.actionPerformed(ProgressThread.ACTION_RECORD) == false){
				final String msg = "Capture device fail : " + CaptureConfig.getInstance().strDeviceName;
				cs.stop();
				JPanel errPanel = new JPanel();
				errPanel.add(new JLabel("<html>" + msg + 
						"<BR>This device maybe be used or release error." +
						"<BR><BR>please try restart pc.</html>", 
						App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEFT));
				App.showCenterPanel(errPanel, 0, 0, "Camera Error", false, null, null, null, null, null, true, false, null, false, false);
			}
		}else if(status == CapStream.CAP_RECORDING){
			CapStream.getInstance(false).actionPerformed(ProgressThread.ACTION_PAUSE);
		}else if(status == CapStream.CAP_PAUSEING){
			CapStream.getInstance(false).actionPerformed(ProgressThread.ACTION_RESUME);
		}
	}
	
}

class CapPreviewNotify implements IMsgNotifier {

	@Override
	public void notifyNewMsg(String msg) {
		if(this.pane != null){
			this.pane.refreshCapStatus();
		}
	}

	@Override
	public String getNextMsg() {
		return null;
	}
	
	private CapPreviewPane pane;
	
	public void setCapPreviewPane(CapPreviewPane pane){
		this.pane = pane;
	}
	
}

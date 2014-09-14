package hc.server.ui.video;

import hc.core.IConstant;
import hc.server.FileSelector;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Vector;

import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jmapps.util.JMAppsCfg;

import com.sun.media.ui.AudioFormatChooser;

public class CapturePanel extends JPanel implements ItemListener, ActionListener {
	private Vector vectorDevices = null;
	private Vector vectorAudioDevices = null;
	private Vector vectorVideoDevices = null;
	private JPanel panelDevices;
	public JCheckBox checkUseVideo = null;
	private JCheckBox checkUseAudio = null;
	private JComboBox comboVideoDevice = null;
	private JComboBox comboAudioDevice = null;
	private JPanel panelVideoFormat = null;
	private JPanel panelAudioFormat = null;
	private AudioFormatChooser chooserAudio = null;
	private CapFormatChooser chooserVideo = null;
	private JCheckBox autoRecord;
	private JTextField storeDirField;
	private JButton dirButton;
	private final CaptureConfig config;
	private CapControlFrame control;
	private CapPreviewPane previewPane;
	
	public void dispose(){
		control = null;
		previewPane.frame = null;
	}
	
	JMAppsCfg cfgJMApps;
	public CapturePanel(CaptureConfig cc, CapControlFrame controlPane, CapPreviewPane preview) {
		this.config = cc;
		this.control = controlPane;
		this.previewPane = preview;
		setLayout(new BorderLayout());
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public CaptureConfig getConfig(){
		config.useVideo = checkUseVideo.isSelected();
		config.strDeviceName = (String)comboVideoDevice.getSelectedItem();
		config.autoRecord = String.valueOf(autoRecord.isSelected());
		config.capSaveDir = storeDirField.getText();
		config.toSerialize((VideoFormat)chooserVideo.getFormat());
		return config;
	}
	
	public boolean isVideoDeviceUsed() {
		boolean boolUsed = false;

		if (this.checkUseVideo != null)
			boolUsed = this.checkUseVideo.isSelected();
		return boolUsed;
	}

	public boolean isAudioDeviceUsed() {
		boolean boolUsed = false;

		if (this.checkUseAudio != null)
			boolUsed = this.checkUseAudio.isSelected();
		return boolUsed;
	}

	public CaptureDeviceInfo getVideoDevice() {
		CaptureDeviceInfo infoCaptureDevice = null;

		if ((this.comboVideoDevice != null) && (isVideoDeviceUsed())) {
			int i = this.comboVideoDevice.getSelectedIndex();
			infoCaptureDevice = (CaptureDeviceInfo) this.vectorVideoDevices
					.elementAt(i);
		}
		return infoCaptureDevice;
	}

	public CaptureDeviceInfo getAudioDevice() {
		CaptureDeviceInfo infoCaptureDevice = null;

		if ((this.comboAudioDevice != null) && (isAudioDeviceUsed())) {
			int i = this.comboAudioDevice.getSelectedIndex();
			infoCaptureDevice = (CaptureDeviceInfo) this.vectorAudioDevices
					.elementAt(i);
		}
		return infoCaptureDevice;
	}

	public VideoFormat getVideoFormat() {
		VideoFormat format = null;

		if ((this.chooserVideo != null) && (isVideoDeviceUsed()))
			format = (VideoFormat) this.chooserVideo.getFormat();
		return format;
	}

	public AudioFormat getAudioFormat() {
		AudioFormat format = null;

		if ((this.chooserAudio != null) && (isAudioDeviceUsed()))
			format = (AudioFormat) this.chooserAudio.getFormat();
		return format;
	}

	public void init(){
		removeAll();

		this.vectorDevices = CaptureDeviceManager.getDeviceList(null);
		if ((this.vectorDevices == null) || (this.vectorDevices.size() < 1)) {
			JLabel label = new JLabel("No capture devices found in JMF registry!");
			add(label, BorderLayout.CENTER);
			return;
		} else {
			this.panelDevices = new JPanel(new GridLayout(1, 0, 6, 6));
			add(this.panelDevices, BorderLayout.CENTER);

			JPanel panel = createVideoPanel(config);
			if (panel != null)
				this.panelDevices.add(panel);
//			panel = createAudioPanel();
//			if (panel != null)
//				this.panelDevices.add(panel);
		}

		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		String isAutoRecord = CaptureConfig.getAutoRecord();
		autoRecord = new JCheckBox("auto capture, when start", isAutoRecord.equals(IConstant.TRUE));
		autoRecord.setToolTipText("It will auto start record, when HomeCenter Server startup.");
		
		buttonsPanel.add(autoRecord);
		
		JPanel dirPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		storeDirField = new JTextField(CaptureConfig.getSaveDir(), 40);
		storeDirField.setEditable(false);
		dirButton = new JButton("Change dir");
		dirButton.addActionListener(this);
		dirPanel.add(storeDirField);
		dirPanel.add(dirButton);
		
		JPanel ctrlPanel = new JPanel(new BorderLayout());
		ctrlPanel.add(dirPanel, BorderLayout.SOUTH);
		ctrlPanel.add(buttonsPanel, BorderLayout.NORTH);
		
		add(ctrlPanel, BorderLayout.SOUTH);
		
		this.checkUseVideo.setSelected(!config.useVideo);
		this.checkUseVideo.doClick();
		this.previewPane.setCtrlEnabled(config.useVideo);
	}
	
	public void save(){
		getConfig().save();
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if(source == dirButton){
			File dir = FileSelector.selectImageFile(dirButton, FileSelector.FOLDER_FILTER, true);
			if(dir != null){
				storeDirField.setText(dir.getAbsolutePath());
			}
			return;
		}
	}

	private JPanel createVideoPanel(CaptureConfig cc){
		CaptureDeviceInfo infoCaptureDevice;
		boolean boolState = false;
		VideoFormat formatDefault = null;

		int nCount = this.vectorDevices.size();
		this.vectorVideoDevices = new Vector();
		for (int i = 0; i < nCount; ++i) {
			infoCaptureDevice = (CaptureDeviceInfo) this.vectorDevices
					.elementAt(i);
			Format[] arrFormats = infoCaptureDevice.getFormats();
			for (int j = 0; j < arrFormats.length; ++j){
				if (arrFormats[j] instanceof VideoFormat) {
					this.vectorVideoDevices.addElement(infoCaptureDevice);
					break;
				}
			}
		}

		if (this.vectorVideoDevices.isEmpty())
			return null;

//		if (this.cfgJMApps != null)
//			dataCapture = this.cfgJMApps.getLastCaptureVideoData();
		if (cc != null) {
			boolState = cc.useVideo;
			if (cc.formatDefault instanceof VideoFormat)
				formatDefault = (VideoFormat) cc.formatDefault;
		}
		JPanel panelVideo = new JPanel(new BorderLayout(6, 6));
//		panelVideo.setEtchedBorder();

		JPanel panelContent = new JPanel(new BorderLayout(6, 6));
//		panelContent.setEmptyBorder(6, 6, 6, 6);
		panelVideo.add(panelContent, BorderLayout.CENTER);
		JPanel panel = panelContent;

		JPanel panelTemp = new JPanel(new BorderLayout(6, 6));
		panel.add(panelTemp, BorderLayout.NORTH);
		this.checkUseVideo = new JCheckBox("Use video device");
		this.checkUseVideo.addItemListener(this);
		JPanel savePane = new JPanel(new FlowLayout());
		savePane.add(checkUseVideo);
		panelTemp.add(savePane, BorderLayout.WEST);
		panelTemp = new JPanel(new BorderLayout(6, 6));
		panel.add(panelTemp, BorderLayout.CENTER);
		panel = panelTemp;

		panelTemp = new JPanel(new BorderLayout(6, 6));
		panel.add(panelTemp, BorderLayout.NORTH);
		this.comboVideoDevice = new JComboBox();
		panelTemp.add(this.comboVideoDevice, BorderLayout.CENTER);
		nCount = this.vectorVideoDevices.size();
		boolean boolContains = false;
		for (int i = 0; i < nCount; ++i) {
			infoCaptureDevice = (CaptureDeviceInfo) this.vectorVideoDevices
					.elementAt(i);
			String strDeviceName = infoCaptureDevice.getName();
			this.comboVideoDevice.addItem(strDeviceName);
			if ((!(boolContains)) && (cc != null)
					&& (cc.strDeviceName != null)) {
				boolContains = cc.strDeviceName.equals(strDeviceName);
			}
		}
		if (boolContains == true)
			this.comboVideoDevice.setSelectedItem(cc.strDeviceName);
		this.comboVideoDevice.addItemListener(this);
		this.comboVideoDevice.setEnabled(boolState);

		this.panelVideoFormat = new JPanel(new BorderLayout(6, 6));
		panel.add(this.panelVideoFormat, BorderLayout.CENTER);
		createVideoChooser(formatDefault);
		if (this.chooserVideo != null)
			this.chooserVideo.setEnabled(boolState);

		return panelVideo;
	}

	private void createVideoChooser(VideoFormat formatDefault) {
		if (this.panelVideoFormat == null)
			return;

		this.panelVideoFormat.removeAll();

		int i = this.comboVideoDevice.getSelectedIndex();
		CaptureDeviceInfo infoCaptureDevice = (CaptureDeviceInfo) this.vectorVideoDevices
				.elementAt(i);
		Format[] arrFormats = infoCaptureDevice.getFormats();
		this.chooserVideo = new CapFormatChooser(arrFormats, formatDefault,
				false, null, true);
		this.panelVideoFormat.add(this.chooserVideo, BorderLayout.CENTER);
	}

//	private JPanel createAudioPanel() throws Exception {
//		CaptureDeviceInfo infoCaptureDevice;
//		boolean boolState = true;
//		AudioFormat formatDefault = null;
//
//		JMAppsCfg.CaptureDeviceData dataCapture = null;
//
//		int nCount = this.vectorDevices.size();
//		this.vectorAudioDevices = new Vector();
//		for (int i = 0; i < nCount; ++i) {
//			infoCaptureDevice = (CaptureDeviceInfo) this.vectorDevices
//					.elementAt(i);
//			Format[] arrFormats = infoCaptureDevice.getFormats();
//			for (int j = 0; j < arrFormats.length; ++j)
//				if (arrFormats[j] instanceof AudioFormat) {
//					this.vectorAudioDevices.addElement(infoCaptureDevice);
//					break;
//				}
//
//		}
//
//		if (this.vectorAudioDevices.isEmpty())
//			return null;
//
////		if (this.cfgJMApps != null)
////			dataCapture = this.cfgJMApps.getLastCaptureAudioData();
//		if (dataCapture != null) {
//			boolState = dataCapture.boolUse;
//			if (dataCapture.format instanceof AudioFormat)
//				formatDefault = (AudioFormat) dataCapture.format;
//		}
//
//		JPanel panelAudio = new JPanel(new BorderLayout(6, 6));
////		panelAudio.setEtchedBorder();
//
//		JPanel panelContent = new JPanel(new BorderLayout(6, 6));
////		panelContent.setEmptyBorder(6, 6, 6, 6);
//		panelAudio.add(panelContent, "Center");
//		JPanel panel = panelContent;
//
//		JPanel panelTemp = new JPanel(new BorderLayout(6, 6));
//		panel.add(panelTemp, BorderLayout.NORTH);
//		this.checkUseAudio = new JCheckBox("Use audio device", boolState);
//		this.checkUseAudio.addItemListener(this);
//		panelTemp.add(this.checkUseAudio, BorderLayout.WEST);
//		panelTemp = new JPanel(new BorderLayout(6, 6));
//		panel.add(panelTemp, BorderLayout.CENTER);
//		panel = panelTemp;
//
//		panelTemp = new JPanel(new BorderLayout(6, 6));
//		panel.add(panelTemp, BorderLayout.NORTH);
//		this.comboAudioDevice = new JComboBox();
//		panelTemp.add(this.comboAudioDevice, BorderLayout.CENTER);
//		nCount = this.vectorAudioDevices.size();
//		boolean boolContains = false;
//		for (int i = 0; i < nCount; ++i) {
//			infoCaptureDevice = (CaptureDeviceInfo) this.vectorAudioDevices
//					.elementAt(i);
//			String strDeviceName = infoCaptureDevice.getName();
//			this.comboAudioDevice.addItem(strDeviceName);
//			if ((!(boolContains)) && (dataCapture != null)
//					&& (dataCapture.strDeviceName != null)) {
//				boolContains = dataCapture.strDeviceName.equals(strDeviceName);
//			}
//		}
//		if (boolContains == true)
//			this.comboAudioDevice.setSelectedItem(dataCapture.strDeviceName);
//		this.comboAudioDevice.addItemListener(this);
//		this.comboAudioDevice.setEnabled(boolState);
//
//		this.panelAudioFormat = new JPanel(new BorderLayout(6, 6));
//		panel.add(this.panelAudioFormat, BorderLayout.CENTER);
//		createAudioChooser(formatDefault);
//		if (this.chooserAudio != null)
//			this.chooserAudio.setEnabled(boolState);
//
//		return panelAudio;
//	}

	private void createAudioChooser(AudioFormat formatDefault) {
		if (this.panelAudioFormat == null)
			return;

		this.panelAudioFormat.removeAll();

		int i = this.comboAudioDevice.getSelectedIndex();
		CaptureDeviceInfo infoCaptureDevice = (CaptureDeviceInfo) this.vectorAudioDevices
				.elementAt(i);
		Format[] arrFormats = infoCaptureDevice.getFormats();
		this.chooserAudio = new AudioFormatChooser(arrFormats, formatDefault,
				false, null);
		this.panelAudioFormat.add(this.chooserAudio, "Center");
	}

	public void itemStateChanged(ItemEvent event) {
		boolean boolEnable;
		Object objectSource = event.getSource();

		if (objectSource == this.checkUseVideo) {
			boolEnable = this.checkUseVideo.isSelected();
			this.comboVideoDevice.setEnabled(boolEnable);
			this.chooserVideo.setEnabled(boolEnable);
			this.dirButton.setEnabled(boolEnable);
			this.autoRecord.setEnabled(boolEnable);
		} else if (objectSource == this.checkUseAudio) {
			boolEnable = this.checkUseAudio.isSelected();
			this.comboAudioDevice.setEnabled(boolEnable);
			this.chooserAudio.setEnabled(boolEnable);
		} else if (objectSource == this.comboVideoDevice) {
			createVideoChooser(null);
			validate();
		} else if (objectSource == this.comboAudioDevice) {
			createAudioChooser(null);
			validate();
		}
	}
}
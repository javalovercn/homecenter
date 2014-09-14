package hc.server.ui.video;

import hc.server.ui.NumberFormatTextField;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.media.Player;
import javax.media.bean.playerbean.MediaPlayer;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jmapps.ui.VideoPanel;
import jmapps.util.JMFUtils;

public class CapViewer extends JPanel{
	private final JList capList = new JList();
	private final JRadioButton capCheck, snapCheck;
	private final JPanel viewPane;
	public CapControlFrame frame;
	MediaPlayer mediaPlayer;
	private final JButton refresh = new JButton((String)ResourceUtil.get(1026));
	final NumberFormatTextField delDay;
	final boolean[] isInited = {true};
	
	public CapViewer(CapControlFrame frame){
		this.frame = frame;
		setLayout(new BorderLayout());
		
		capCheck = new JRadioButton((String)ResourceUtil.get(9054), false);
		snapCheck = new JRadioButton((String)ResourceUtil.get(9055), false);
		
		ButtonGroup bg = new ButtonGroup();
		bg.add(capCheck);
		bg.add(snapCheck);
		
		JPanel groupPane = new JPanel(new GridLayout(1, 2));
		groupPane.add(capCheck);
		groupPane.add(snapCheck);
		
		JPanel listPanel = new JPanel(new BorderLayout());
		capList.setVisibleRowCount(10);
		capList.setAutoscrolls(true);
		JScrollPane capListPane = new JScrollPane(capList);
		listPanel.add(groupPane, BorderLayout.NORTH);
		listPanel.add(capListPane, BorderLayout.CENTER);
		listPanel.add(refresh, BorderLayout.SOUTH);
		listPanel.setBorder(new TitledBorder(""));
		
		viewPane = new JPanel(new BorderLayout());
		
		add(listPanel, BorderLayout.WEST);
		
		
		delDay = new NumberFormatTextField();
		delDay.setText(PropertiesManager.getValue(PropertiesManager.p_CapDelDays, "3"));
		
		JPanel flowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		Icon icon = null;
		try{
			icon = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/remove_22.png")));
		}catch (Exception e) {
		}
		flowPanel.add(new JLabel("del files before ", icon, SwingConstants.LEFT));
		flowPanel.add(delDay);
		flowPanel.add(new JLabel(" days."));
		flowPanel.setBorder(new TitledBorder(""));
		
		JPanel composePane = new JPanel(new BorderLayout());
		composePane.add(flowPanel, BorderLayout.NORTH);
		composePane.add(viewPane, BorderLayout.CENTER);
		add(composePane, BorderLayout.CENTER);
		
		capList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				//
				if(isInited[0] == true && e.getValueIsAdjusting() == false){
					try {
						boolean isVideo = capCheck.isSelected();
						String fileName = (isVideo?CapStream.VIDEO_PREFIX:CapStream.SNAP_PREFIX) + 
								capList.getSelectedValue() + 
								(isVideo?CapStream.VIDEO_END:CapStream.SNAP_END);
						final String url = new File(CaptureConfig.getInstance().capSaveDir, fileName).toURL().toString();
						if(isVideo){
							open(url);
						}else{
							openSnap(url);
						}
					} catch (Throwable e1) {
					}
				}
			}
		});
		
		capCheck.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				list(true);
			}
		});
		snapCheck.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				list(false);
			}
		});
		capCheck.doClick();
		
		try {
			refresh.setIcon(new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/refres_22.png"))));
		} catch (IOException e1) {
		}
		refresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				list(capCheck.isSelected());
			}
		});
	}
	
	private void open(String url){
		clear();
		
		mediaPlayer = JMFUtils.createMediaPlayer(url, frame, null, null);
        mediaPlayer.setPlaybackLoop(false);
//        mediaPlayer.setFixedAspectRatio(menuKeepAspect.getState());
        mediaPlayer.setPopupActive(false);
        mediaPlayer.setControlPanelVisible(true);//显示进度及控制条
        mediaPlayer.realize();
        CapStream.waitForStatePlayer(mediaPlayer, Player.Realized);
        mediaPlayer.prefetch();
        CapStream.waitForStatePlayer(mediaPlayer, Player.Prefetched);
        mediaPlayer.start();
        CapStream.waitForStatePlayer(mediaPlayer, Player.Started);
        viewPane.add(new VideoPanel(mediaPlayer), BorderLayout.CENTER);
        frame.pack();
	}
	
	private void openSnap(String url){
		clear();

		try {
			JLabel icon = new JLabel(new ImageIcon(ImageIO.read(new URL(url))));
	        viewPane.add(icon, BorderLayout.CENTER);
	        frame.pack();
	        System.gc();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void clear() {
		if(mediaPlayer != null){
			try{
				mediaPlayer.stopAndDeallocate();
			}catch (Exception e) {
			}
			mediaPlayer = null;
		}
		viewPane.removeAll();
	}

	private void list(boolean isVideo){
		isInited[0] = false;
		File dir = new File(CaptureConfig.getInstance().capSaveDir);
		Vector<String> list = new Vector<String>();

		String currRecordFileName = "____________";
		final CapStream cs = CapStream.getInstance(false);
		if(cs != null){
			String fn = cs.getCurrRecordFileNameNoExt();
			if(fn != null){
				currRecordFileName = fn;
			}
		}
		
		String[] files = dir.list();
		for (int i = 0; i < files.length; i++) {
			final String aFile = files[i];
			if(isVideo){
				if(aFile.startsWith(CapStream.VIDEO_PREFIX) && (aFile.startsWith(currRecordFileName) == false)){
					addFileNameToList(list, aFile, true);
				}
			}else if(aFile.startsWith(CapStream.SNAP_PREFIX)){
				addFileNameToList(list, aFile, false);
			}
		}
		capList.setListData(list);
		frame.pack();
		isInited[0] = true;
	}

	public void save(){
		PropertiesManager.setValue(PropertiesManager.p_CapDelDays, delDay.getText());
	}
	
	private void addFileNameToList(Vector<String> list, final String aFile, final boolean isVideo) {
		int endIdx =  aFile.indexOf(".");
		int prefixLen = isVideo?CapStream.VIDEO_PREFIX.length():CapStream.SNAP_PREFIX.length();
		list.add(aFile.substring(prefixLen, endIdx));
	}
}

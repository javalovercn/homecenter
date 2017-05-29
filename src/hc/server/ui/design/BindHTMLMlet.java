package hc.server.ui.design;

import hc.core.ContextManager;
import hc.core.L;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.server.msb.ConverterInfo;
import hc.server.msb.DataDeviceCapDesc;
import hc.server.msb.DeviceBindInfo;
import hc.server.msb.DeviceCompatibleDescription;
import hc.server.msb.RealDeviceInfo;
import hc.server.ui.HTMLMlet;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.event.ListDataListener;

public class BindHTMLMlet extends SystemHTMLMlet {
	final JRadioButton listNotBindBtn, listAllBtn;
	final JComboBox robotBox, converterBox, deviceBox;//java 6不支持<String>
//	final JButton doneBut, cancelBtn;
//	final JTextArea descRobot, descConv, descDev;
	final Boolean[] waitLock;
	final ArrayList<RealDeviceInfo> allDevices;
	final ArrayList<ConverterInfo> allConverters;
	
	final ArrayList<String> projectIDList;
	final HashMap<String, Vector<BindDeviceNode[]>> robotsOfProj = new HashMap<String, Vector<BindDeviceNode[]>>(2);
	final BindRobotSource source;
	final Vector<BindDeviceNode> bindedOrNotList = new Vector<BindDeviceNode>(20);
	final BindDeviceNode[] bdnArrayType = new BindDeviceNode[0];
	Vector<Integer> bindedOrNotListIdxForBox;
	final JTextArea robotArea;
	final JTextArea converterArea;
	final JTextArea deviceArea;
	Vector<BindDeviceNode> robotRefList;
	final Object threadToken;
	final String clearDesc;
	JButton nextUnbindOneBtn;

	final int emptyItem = 1;
	
	public BindHTMLMlet(final BindRobotSource source, final Object token, final String nextOne,
			final BufferedImage okImage, final BufferedImage cancelImage,
			final String okDesc, final String cancelDesc, final boolean isUnbindDefault,
			final String robotsDesc, final String convDesc, final String devDesc, final Boolean[] waitLock, 
			final String empty){
		final boolean isEnableSelectList = false;
		clearDesc = "[        " + empty + "        ]";
		this.waitLock = waitLock;
		this.threadToken = token;
		this.source = source;
		
		ArrayList<RealDeviceInfo> allDevices = null;
		ArrayList<ConverterInfo> allConverters = null;
		try{
			allDevices = source.getRealDeviceInAllProject();
			allConverters = source.getConverterInAllProject();
		}catch (final Throwable e) {
			e.printStackTrace();
		}
		
		projectIDList = source.getProjectList();
		
		{
			final int size = projectIDList.size();
			
			for (int i = 0; i < size; i++) {
				final String projID = projectIDList.get(i);
				final ArrayList<String> robotsList = source.getRobotsByProjectID(projID);

				final int robotSize = robotsList.size();
				final Vector<BindDeviceNode[]> robotsBDN = new Vector<BindDeviceNode[]>();
				
				for (int j = 0; j < robotSize; j++) {
					final String robotID = robotsList.get(j);
					final Vector<BindDeviceNode> oneRobotBDN = new Vector<BindDeviceNode>();
					
					try{
					final ArrayList<DeviceBindInfo> refList = BindManager.getReferenceDeviceListByRobotName(source, projID, robotID);
					final int refSize = refList.size();
					
					for (int k = 0; k < refSize; k++) {
						final DeviceBindInfo di = refList.get(k);
						
						final BindDeviceNode bdn = BindManager.buildDataNodeForRefDevInRobot(source.respo, projID, robotID, di, source);
						oneRobotBDN.add(bdn);
						bindedOrNotList.add(bdn);
					}
					}catch (final Throwable e) {
						e.printStackTrace();
					}
					
					robotsBDN.add(oneRobotBDN.toArray(bdnArrayType));
				}
				
				if(robotSize > 0){
					robotsOfProj.put(projID, robotsBDN);
				}
			}
		}
		
		this.allDevices = allDevices;
		this.allConverters = allConverters;
		
		final Vector<String> robotsList = isUnbindDefault?getRobotsInUT(false):getAllRobotsInUT();
		robotBox = new JComboBox(robotsList);//java 6不支持<String>
		converterBox = new JComboBox(getAllConverterInUT());
		deviceBox = new JComboBox(getAllDeviceInUT());
		
		final int gapPixel = 0;
		
		listNotBindBtn = new JRadioButton("list unbind");
		listAllBtn = new JRadioButton("list all");
		
		final int fontSizePX = okImage.getHeight();
		final int labelHeight = (int)(fontSizePX * 1.4);

		{
			final int checkBoxHeight = (int)(labelHeight * 0.95);
			final String checkStyle = "vertical-align:middle;font-weight:bold;font-size:" + fontSizePX + "px;";
			
			setCSS(listNotBindBtn, null, checkStyle);
			setCSSForToggle(listNotBindBtn, null, "width: " + checkBoxHeight + "px; height: " + checkBoxHeight + "px;");
			setCSS(listAllBtn, null, checkStyle);
			setCSSForToggle(listAllBtn, null, "width: " + checkBoxHeight + "px; height: " + checkBoxHeight + "px;");
		}
		
		listNotBindBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final Vector<String> robotsInUT = getRobotsInUT(false);
				robotBox.setModel(new BindComboBoxModel(BindComboBoxModel.TYPE_ROBOT, robotsInUT));
				updateBoxs(robotsInUT);
			}
		});
		listAllBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final Vector<String> allRobotsInUT = getAllRobotsInUT();
				robotBox.setModel(new BindComboBoxModel(BindComboBoxModel.TYPE_ROBOT, allRobotsInUT));
				updateBoxs(allRobotsInUT);
			}
		});
		
		final ButtonGroup bg = new ButtonGroup();
		bg.add(listNotBindBtn);
		bg.add(listAllBtn);
		
		final JPanel listSelectPanel = new JPanel(new GridLayout(1, 2));
		listSelectPanel.add(listNotBindBtn);
		listSelectPanel.add(listAllBtn);
		
		if(isUnbindDefault){
			listNotBindBtn.setSelected(true);
		}else{
			listAllBtn.setSelected(true);
		}

		final JPanel robotsPanel = new JPanel(new BorderLayout());
		final JPanel convPanel = new JPanel(new BorderLayout());
		final JPanel devPanel = new JPanel(new BorderLayout());
		
		final String buttonStyle = "text-align:center;vertical-align:middle;width:100%;height:100%;font-size:" + fontSizePX + "px;";
		final JButton ok = new JButton(okDesc, new ImageIcon(okImage));
		final JButton cancel = new JButton(cancelDesc, new ImageIcon(cancelImage));

		final String labelDivStyle = "overflow:hidden;";
		final String labelStyle = "display:block;vertical-align:middle;font-weight:bold;font-size:" + fontSizePX + "px;";

		final int areaBackColor = new Color(HTMLMlet.getColorForBodyByIntValue(), true).darker().getRGB();
		final int areaFontColor = new Color(HTMLMlet.getColorForFontByIntValue(), true).darker().getRGB();
		final String areaStyle = "width:100%;height:100%;" +
				"overflow-y:auto;font-size:" + (int)(fontSizePX * 0.7) + "px;" +
				"background-color:#" + HTMLMlet.toHexColor(areaBackColor, false) + ";color:#" + HTMLMlet.toHexColor(areaFontColor, false) + ";";

		final int mobileWidth = getMobileWidth();

		robotArea = new JTextArea(30, 30);
		{
			final JLabel label = buildLabel(robotsDesc, labelDivStyle, labelStyle);
			
			label.setPreferredSize(new Dimension(mobileWidth, labelHeight));
			robotArea.setEditable(false);
			setCSS(robotArea, null, areaStyle);
			
			final JPanel areaPanel = new JPanel(new BorderLayout());
			robotBox.setPreferredSize(new Dimension(getMobileWidth(), labelHeight));
			areaPanel.add(robotBox, BorderLayout.NORTH);
			areaPanel.add(robotArea, BorderLayout.CENTER);
			
			robotBox.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(final ItemEvent e) {
					refreshForRobot();
				}
			});
			
			robotsPanel.add(areaPanel, BorderLayout.CENTER);
			robotsPanel.add(label, BorderLayout.NORTH);
		}
		
		{
			final JLabel label = buildLabel(convDesc, labelDivStyle, labelStyle);
	
			label.setPreferredSize(new Dimension(mobileWidth, labelHeight));
			converterArea = new JTextArea(30, 30);
			converterArea.setEditable(false);
			setCSS(converterArea, null, areaStyle);
			
			final JPanel areaPanel = new JPanel(new BorderLayout());
			converterBox.setPreferredSize(new Dimension(getMobileWidth(), labelHeight));
			areaPanel.add(converterBox, BorderLayout.NORTH);
			areaPanel.add(converterArea, BorderLayout.CENTER);
			
			converterBox.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(final ItemEvent e) {
					try{
						converterArea.setText(selectConverter());
					}catch (final Throwable ex) {
						ex.printStackTrace();
					}
				}
			});
			
			convPanel.add(areaPanel, BorderLayout.CENTER);
			convPanel.add(label, BorderLayout.NORTH);
		}
		
		{
			final JLabel label = buildLabel(devDesc, labelDivStyle, labelStyle);
	
			label.setPreferredSize(new Dimension(mobileWidth, labelHeight));
			deviceArea = new JTextArea(30, 30);
			deviceArea.setEditable(false);
			setCSS(deviceArea, null, areaStyle);
			
			final JPanel areaPanel = new JPanel(new BorderLayout());
			deviceBox.setPreferredSize(new Dimension(getMobileWidth(), labelHeight));
			areaPanel.add(deviceBox, BorderLayout.NORTH);
			areaPanel.add(deviceArea, BorderLayout.CENTER);
			
			deviceBox.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(final ItemEvent e) {
					try{
						deviceArea.setText(selectDevice());
						final boolean allBinded = isAllBinded();
						
						ok.setEnabled(allBinded);
						if(nextUnbindOneBtn != null){
							nextUnbindOneBtn.setEnabled(!allBinded);
						}
					}catch (final Throwable ex) {
						ex.printStackTrace();
					}
				}
			});
			
			devPanel.add(areaPanel, BorderLayout.CENTER);
			devPanel.add(label, BorderLayout.NORTH);
		}
		
		ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				submitBind();//要在前执行
				BindHTMLMlet.this.back();
			}
		});
		setCSS(ok, null, buttonStyle);
		
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				BindHTMLMlet.this.back();
			}
		});
		setCSS(cancel, null, buttonStyle);
		
		if(isUnbindDefault){
			nextUnbindOneBtn = new JButton(nextOne);
			setCSS(nextUnbindOneBtn, null, buttonStyle);
			
			nextUnbindOneBtn.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					final int listSize = bindedOrNotListIdxForBox.size();
					for (int i = 0; i < listSize; i++) {
						final int itemIdx = bindedOrNotListIdxForBox.get(i);
						final BindDeviceNode bdn = bindedOrNotList.get(itemIdx);
						if(bdn.isBindedRealDevice() == false){
							robotBox.setSelectedIndex(i);
							return;
						}
					}
				}
			});
		}
		
		final JPanel bottomButtonPanel = new JPanel();
		bottomButtonPanel.setLayout(new GridLayout(1, 2, gapPixel, gapPixel));
		
		if(nextUnbindOneBtn != null){
			bottomButtonPanel.add(nextUnbindOneBtn);
		}else{
			bottomButtonPanel.add(cancel);
		}
		bottomButtonPanel.add(ok);
		final int buttonPanelHeight = Math.max(fontSizePX + getFontSizeForNormal(), getButtonHeight());
		bottomButtonPanel.setPreferredSize(new Dimension(mobileWidth, buttonPanelHeight));
		
		cancel.setEnabled(!isUnbindDefault);//必须绑定
		
		ok.setEnabled(isAllBinded());
		
		final JPanel listPanel = new JPanel(new GridLayout(3, 1));
		listPanel.add(robotsPanel);
		listPanel.add(convPanel);
		listPanel.add(devPanel);
		
		setLayout(new BorderLayout(gapPixel, gapPixel));
		if(isEnableSelectList){
			add(listSelectPanel, BorderLayout.NORTH);
		}
		add(listPanel, BorderLayout.CENTER);
		add(bottomButtonPanel, BorderLayout.SOUTH);
		
		robotArea.setEditable(false);
		converterArea.setEditable(false);
		deviceArea.setEditable(false);
		
		updateBoxs(robotsList);
	}
	
	private final void updateBoxs(final Vector<String> list){
		final boolean isZero = list.size() == 0;
		
		converterBox.setEnabled(!isZero);
		deviceBox.setEnabled(!isZero);
		
		if(isZero == false){
			refreshForRobot();
		}else{
			converterArea.setText("");
			deviceArea.setText("");
		}
	}

	private JLabel buildLabel(final String robotsDesc, final String labelDivStyle,
			final String labelStyle) {
		final JLabel label = new JLabel(robotsDesc);
		setCSSForDiv(label, null, labelDivStyle);
		setCSS(label, null, labelStyle);
		return label;
	}
	
	@Override
	public void onExit() {
		L.V = L.WShop ? false : LogManager.log("BindHTMLMlet onExit and notify waitLock.");
		
		synchronized (waitLock) {
			waitLock.notify();
		}
	}
	
	final Vector<String> getAllRobotsInUT(){
		return getRobotsInUT(true);
	}

	final Vector<String> getRobotsInUT(final boolean isAll){
		final Vector<String> out = new Vector<String>(5);
		bindedOrNotListIdxForBox = new Vector<Integer>();
		
		final int size = bindedOrNotList.size();
		for (int i = 0; i < size; i++) {
			final BindDeviceNode bdn = bindedOrNotList.get(i);
			if(isAll || bdn.isBindedRealDevice() == false){
				final String bindID = bdn.projID + "/" + bdn.lever2Name + "/" + bdn.ref_dev_ID;
				out.add(bindID);
				bindedOrNotListIdxForBox.add(i);
			}
		}
		
		return out;
	}

	final void submitBind(){
		ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
			@Override
			public Object run() {
				final Iterator<String> proj = robotsOfProj.keySet().iterator();
				while(proj.hasNext()){
					final String projID = proj.next();
					AddHarHTMLMlet.updateProjectBindsToLPS(projID, robotsOfProj.get(projID));
				}
				
				LinkProjectManager.updateToLinkProject();
				waitLock[0] = true;
				return null;
			}
		}, threadToken);
	}

	final String selectRobotInUT(){
		final int bdnIdx = bindedOrNotListIdxForBox.get(robotBox.getSelectedIndex());
		final BindDeviceNode bdn = bindedOrNotList.get(bdnIdx);
		final ProjResponser pr = source.respo.getProjResponser(bdn.projID);
		
		final ConverterInfo c = bdn.convBind;
		if(c == null){
			converterBox.setSelectedIndex(emptyItem - 1);
		}else{
			final int selectIdx = getComboIdx(c);
			if(selectIdx < 0){
				converterBox.setSelectedIndex(emptyItem - 1);
			}else{
				converterBox.setSelectedIndex(selectIdx + emptyItem);
			}
		}
		
		final RealDeviceInfo rdi = bdn.realDevBind;
		if(rdi == null){
			deviceBox.setSelectedIndex(emptyItem - 1);
		}else{
			final int selectIdx = getComboIdx(rdi);
			if(selectIdx < 0){
				deviceBox.setSelectedIndex(emptyItem - 1);
			}else{
				deviceBox.setSelectedIndex(selectIdx + emptyItem);
			}
		}
		
		return (String)ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
			@Override
			public Object run() {
				DeviceCompatibleDescription compDesc;
				try {
					compDesc = source.getDeviceCompatibleDescByRobotName(bdn.projID, bdn.lever2Name, bdn.ref_dev_ID);
					final DataDeviceCapDesc devDesc = source.getDataForDeviceCompatibleDesc(pr, compDesc);
					return buildStrForCap(devDesc);
				} catch (final Exception e) {
					e.printStackTrace();
				}
				return "fail to get description";
			}
		}, threadToken);

	}

	final String selectConverter(){
		final int bindIdx = bindedOrNotListIdxForBox.get(robotBox.getSelectedIndex());
		final BindDeviceNode bdn = bindedOrNotList.get(bindIdx);
		
		final int selectedIndex = converterBox.getSelectedIndex();
		if(selectedIndex < emptyItem){
			bdn.convBind = null;
			return "";
		}
		
		final ConverterInfo cInfo = allConverters.get(selectedIndex - emptyItem);
		final ProjResponser pr = source.respo.getProjResponser(cInfo.proj_id);
		source.getConverterDescUpDownToUserThread(pr, cInfo);
		
		bdn.convBind = cInfo;
		
		final DataDeviceCapDesc upDesc = source.getDataForDeviceCompatibleDesc(pr, cInfo.upDeviceCompatibleDescriptionCache);
		final DataDeviceCapDesc downDesc = source.getDataForDeviceCompatibleDesc(pr, cInfo.downDeviceCompatibleDescriptionCache);
		
		return buildStrForCap(upDesc, downDesc);
	}

	final String selectDevice(){
		final int bindIdx = bindedOrNotListIdxForBox.get(robotBox.getSelectedIndex());
		final BindDeviceNode bdn = bindedOrNotList.get(bindIdx);
		
		final int selectedIndex = deviceBox.getSelectedIndex();
		if(selectedIndex < emptyItem){
			bdn.realDevBind = null;
			return "";
		}
		
		final RealDeviceInfo rdi = allDevices.get(selectedIndex - emptyItem);
		final ProjResponser pr = source.respo.getProjResponser(rdi.proj_id);
		source.getDeviceCompatibleDescByDevice(pr, rdi);
		
		bdn.realDevBind = rdi;
		
		final DataDeviceCapDesc dev = source.getDataForDeviceCompatibleDesc(pr, rdi.deviceCompatibleDescriptionCache);
		return buildStrForCap(dev);
	}

	private final String buildStrForCap(final DataDeviceCapDesc devDesc) {
		return "Description :\n" + devDesc.desc +
			"\nVersion :\n" + devDesc.ver + 
			"\nCompatible :\n" + devDesc.capList;
	}
	
	private final String buildStrForCap(final DataDeviceCapDesc upDataDCD, final DataDeviceCapDesc downDataDCD){
		return "Up Description :\n" + upDataDCD.desc +
			"\nUp Version :\n" + upDataDCD.ver + 
			"\nUp Compatible :\n" + upDataDCD.capList +
			"\n\nDown Description :\n" + downDataDCD.desc +
			"\nDown Version :\n" + downDataDCD.ver + 
			"\nDown Compatible :\n" + downDataDCD.capList;
	}
	
	final boolean isAllBinded(){
		final Iterator<String> proj = robotsOfProj.keySet().iterator();
		while(proj.hasNext()){
			final String projID = proj.next();
			final Vector<BindDeviceNode[]> vector = robotsOfProj.get(projID);
			final int size = vector.size();
			for (int i = 0; i < size; i++) {
				final BindDeviceNode[] bdn = vector.elementAt(i);
				for (int j = 0; j < bdn.length; j++) {
					if(bdn[j].isBindedRealDevice() == false){
						return false;
					}
				}
			}
		}
		
		return true;
	}
	
	final int getComboIdx(final ConverterInfo cc){
		final String stringValue = cc.toString();
		final int size = allConverters.size();
		
		for (int i = 0; i < size; i++) {
			final ConverterInfo c = allConverters.get(i);
			if(stringValue.equals(c.toString())){
				return i;
			}
		}
		
		return -1;
	}
	
	final int getComboIdx(final RealDeviceInfo cc){
		final String stringValue = cc.toString();
		final int size = allDevices.size();
		
		for (int i = 0; i < size; i++) {
			final RealDeviceInfo c = allDevices.get(i);
			if(stringValue.equals(c.toString())){
				return i;
			}
		}
		
		return -1;
	}

	final Vector<String> getAllConverterInUT(){
		final int size = allConverters.size();
		final Vector<String> out = new Vector<String>(size);

		out.add(clearDesc);
		for (int i = 0; i < size; i++) {
			final ConverterInfo c = allConverters.get(i);
			out.add(c.toString());
		}
		
		return out; 
	}

	final Vector<String> getAllDeviceInUT(){
		final int size = allDevices.size();
		final Vector<String> out = new Vector<String>(size);

		out.add(clearDesc);
		for (int i = 0; i < size; i++) {
			final RealDeviceInfo c = allDevices.get(i);
			out.add(c.toString());
		}
		
		return out; 
	}

	private final void refreshForRobot() {
		try{
			robotArea.setText(selectRobotInUT());
		}catch (final Throwable ex) {
			ex.printStackTrace();
		}
	}
}

class BindComboBoxModel implements ComboBoxModel {//java 6不支持ComboBoxModel<String>
	public static final int TYPE_ROBOT = 1;
	public static final int TYPE_CONV = 2;
	public static final int TYPE_DEV = 3;
	
	final int type;
	final Vector<String> list;
	
	BindComboBoxModel(final int type, final Vector<String> list){
		this.type = type;
		this.list = list;
	}
	
	@Override
	public int getSize() {
		return list.size();
	}

	@Override
	public String getElementAt(final int index) {
		return list.get(index);
	}

	@Override
	public void addListDataListener(final ListDataListener l) {
	}

	@Override
	public void removeListDataListener(final ListDataListener l) {
	}
	
	Object selectedItem;

	@Override
	public void setSelectedItem(final Object anItem) {
		selectedItem = anItem;
	}

	@Override
	public Object getSelectedItem() {
		if(type == TYPE_ROBOT){
			return selectedItem==null?(list.size() > 0?list.get(0):null):selectedItem;
		}else{
			return selectedItem;
		}
	}
	
}
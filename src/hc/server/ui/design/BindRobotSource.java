package hc.server.ui.design;

import hc.App;
import hc.core.L;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.server.ProcessingWindowManager;
import hc.server.msb.Converter;
import hc.server.msb.ConverterInfo;
import hc.server.msb.DataDeviceCapDesc;
import hc.server.msb.Device;
import hc.server.msb.DeviceBindInfo;
import hc.server.msb.DeviceCompatibleDescription;
import hc.server.msb.IoTSource;
import hc.server.msb.MSBAgent;
import hc.server.msb.RealDeviceInfo;
import hc.server.msb.Robot;
import hc.server.ui.ClientDesc;
import hc.server.ui.design.hpj.HCjarHelper;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class BindRobotSource extends IoTSource{
	final MobiUIResponsor respo;
	
	public BindRobotSource(final MobiUIResponsor r) {
		this.respo = r;
		this.respo.bindRobotSource = this;
	}

	@Override
	public ArrayList<String> getProjectList() {
		final ArrayList<String> list = new ArrayList<String>();
		
		final String[] projs = respo.projIDs;
		final int size = respo.responserSize;
		
		for (int i = 0; i < size; i++) {
			list.add(projs[i]);
		}
		
		return list;
	}

	@Override
	public ArrayList<String> getRobotsByProjectID(final String projID) {
		final ArrayList<String> list = new ArrayList<String>();
		try{
			final Vector<String>[] vectors = HCjarHelper.getRobotsSrc(respo.getHarMap(projID));
			if(vectors != null){
				final Vector<String> names = vectors[0];
				
				list.addAll(names);
			}
		}catch (final Throwable e) {
		}
		return list;
	}

	@Override
	public ArrayList<DeviceBindInfo> getReferenceDeviceListByRobotName(
			final String projID, final String robotName) throws Exception {
		final ArrayList<DeviceBindInfo> list = new ArrayList<DeviceBindInfo>();
		
		final ProjResponser pr = respo.getProjResponser(projID);
		if(pr == null){
			return list;
		}
		
		final Robot[] robots = pr.getRobots();
		if(robots == null){
			return list;
		}
		
		for (int i = 0; i < robots.length; i++) {
			final Robot r = robots[i];
			if(MSBAgent.getName(r).equals(robotName)){
				
				L.V = L.O ? false : LogManager.log("try [declareReferenceDeviceID] for Robot [" + robotName + "] in project [" + pr.context.getProjectID() + "]...");
				final String[] referID = (String[])pr.threadPool.runAndWait(new ReturnableRunnable() {
					@Override
					public Object run() {
						return r.declareReferenceDeviceID();
					}
				});
				
				if(referID != null){
					L.V = L.O ? false : LogManager.log("successful [declareReferenceDeviceID] for Robot [" + robotName + "] in project [" + pr.context.getProjectID() + "]...");
					for (int j = 0; j < referID.length; j++) {
						final DeviceBindInfo dbi = new DeviceBindInfo();
						dbi.ref_dev_id = referID[j];
						dbi.bind_id = DeviceBindInfo.buildStandardBindID(projID, robotName, dbi.ref_dev_id);
						
						list.add(dbi);
					}
				}else{
					L.V = L.O ? false : LogManager.log("NO [declareReferenceDeviceID] for Robot [" + robotName + "] in project [" + pr.context.getProjectID() + "]...");
				}
				return list;
			}
		}
		
		return list;
	}

	@Override
	public DeviceCompatibleDescription getDeviceCompatibleDescByRobotName(final String projID, final String robotName, 
			final String referenceDeviceID) throws Exception{
		final ProjResponser pr = respo.getProjResponser(projID);
		if(pr == null){
			return null;
		}
		
		final Robot[] robots = pr.getRobots();
		if(robots == null){
			return null;
		}
		
		for (int i = 0; i < robots.length; i++) {
			final Robot r = robots[i];
			if(MSBAgent.getName(r).equals(robotName)){
				L.V = L.O ? false : LogManager.log("try [getDeviceCompatibleDescription] for Robot [" + robotName + "] in project [" + pr.context.getProjectID() + "]...");
				final DeviceCompatibleDescription out = getDeviceCompatibleDescByRobot(pr, r, referenceDeviceID);
				if(out != null){
					L.V = L.O ? false : LogManager.log("successful [getDeviceCompatibleDescription] for Robot [" + robotName + "] in project [" + pr.context.getProjectID() + "]...");
				}
				return out;
			}
		}
		return null;
	}

	@Override
	public DeviceCompatibleDescription getDeviceCompatibleDescByRobot(final ProjResponser pr, final Robot r, final String referenceDeviceID) {
		return (DeviceCompatibleDescription)pr.threadPool.runAndWait(new ReturnableRunnable() {
			@Override
			public Object run() {
				return r.getDeviceCompatibleDescription(referenceDeviceID);
			}
		});
	}
	
	@Override
	public DataDeviceCapDesc getDataForDeviceCompatibleDesc(final ProjResponser pr, final DeviceCompatibleDescription devCompDesc) {
		if(devCompDesc == null){
			return new DataDeviceCapDesc("", "", "1.0");
		}else{
			return (DataDeviceCapDesc)pr.threadPool.runAndWait(new ReturnableRunnable() {
				@Override
				public Object run() {
					return new DataDeviceCapDesc(devCompDesc.getDescription(), devCompDesc.getCompatibleStringList(), devCompDesc.getVersion());
				}
			});
		}
	}
	
	@Override
	public void getDeviceCompatibleDescByDevice(final ProjResponser pr, final RealDeviceInfo deviceInfo) {
		if(deviceInfo.deviceCompatibleDescriptionCache != null){
			return;
		}
		
		pr.threadPool.runAndWait(new ReturnableRunnable() {
			@Override
			public Object run() {
				deviceInfo.deviceCompatibleDescriptionCache = deviceInfo.device.getDeviceCompatibleDescription();
				MSBAgent.getCompatibleItem(deviceInfo.deviceCompatibleDescriptionCache);
				return null;
			}
		});
	}
	
	@Override
	public void getConverterDescUpDown(final ProjResponser pr, final ConverterInfo converterInfo) {
		if(converterInfo.upDeviceCompatibleDescriptionCache != null || converterInfo.downDeviceCompatibleDescriptionCache != null){
			return;
		}
		
		pr.threadPool.runAndWait(new ReturnableRunnable() {
			@Override
			public Object run() {
				converterInfo.upDeviceCompatibleDescriptionCache = converterInfo.converter.getUpDeviceCompatibleDescription();
				converterInfo.downDeviceCompatibleDescriptionCache = converterInfo.converter.getDownDeviceCompatibleDescription();
				
				MSBAgent.getCompatibleItem(converterInfo.upDeviceCompatibleDescriptionCache);
				MSBAgent.getCompatibleItem(converterInfo.downDeviceCompatibleDescriptionCache);
				
				return converterInfo;
			}
		});
	}
	
	@Override
	public ArrayList<ConverterInfo> getConverterInAllProject() throws Exception{
		final ArrayList<ConverterInfo> list = new ArrayList<ConverterInfo>();
		
		for (int i = 0; i < respo.responserSize; i++) {
			final ProjResponser pr = respo.responsors[i];
			if(pr != null){
				final Converter[] convs = pr.getConverters();
				if(convs != null){
					for (int j = 0; j < convs.length; j++) {
						final ConverterInfo cbi = new ConverterInfo();
						cbi.proj_id = pr.context.getProjectID();
						cbi.name = MSBAgent.getName(convs[j]);
						cbi.converter = convs[j];
						list.add(cbi);
					}
				}
			}
		}

		return list;
	}

	@Override
	public ArrayList<RealDeviceInfo> getRealDeviceInAllProject() throws Exception {
		final ArrayList<RealDeviceInfo> list = new ArrayList<RealDeviceInfo>();
		for (int i = 0; i < respo.responserSize; i++) {
			final ProjResponser pr = respo.responsors[i];
			if(pr != null){
				final Device[] devices = pr.getDevices();
				final String projectID = pr.context.getProjectID();
				if(devices != null){
					for (int j = 0; j < devices.length; j++) {
						final Device device = devices[j];
						final String dev_name = MSBAgent.getName(device);
						
						final String[] devRealIDS = (String[])pr.threadPool.runAndWait(new ReturnableRunnable() {
							@Override
							public Object run() {
								L.V = L.O ? false : LogManager.log("try [connect] for real device IDs of Device ["+ dev_name + "] in project [" + projectID + "]...");
								final String[] out = MSBAgent.getRegisterDeviceID(device);
								L.V = L.O ? false : LogManager.log("successful [connect] for real device IDs of Device ["+ dev_name + "] in project [" + projectID + "].");
								return out;
							}
						});
						
						if(devRealIDS != null){
							for (int k = 0; k < devRealIDS.length; k++) {
								final RealDeviceInfo rdbi = new RealDeviceInfo();
								rdbi.proj_id = projectID;
								rdbi.dev_name = dev_name;
								rdbi.dev_id = devRealIDS[k];
								rdbi.device = device;
								list.add(rdbi);
							}
						}
					}
				}
			}
		}
		
		return list;
	}

	@Override
	public RealDeviceInfo getRealDeviceBindInfo(final String bind_id) {
		//由于全内存运算，所以不计性能，不用cache到变量
		final Iterator<LinkProjectStore> it = LinkProjectManager.getLinkProjsIterator(true);
		while(it.hasNext()){
			final LinkProjectStore lps = it.next();
			if(lps.isActive() == false){
				continue;
			}
			final Object[] objs = lps.getDevBindMap();
			try{
				if(objs != null){
					final String[] tmpBindIDs = (String[])objs[0];
					final RealDeviceInfo[] tmpRealDevs = (RealDeviceInfo[])objs[1];
					
					for (int i = 0; i < tmpBindIDs.length; i++) {
						final String tmp_bind_id = tmpBindIDs[i];
						if(tmp_bind_id.equals(bind_id)){
							return tmpRealDevs[i];
						}
					}
				}
			}catch (final Throwable e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}

	@Override
	public ConverterInfo getConverterBindInfo(final String bind_id) {
		//由于全内存运算，所以不计性能，不用cache到变量
		final Iterator<LinkProjectStore> it = LinkProjectManager.getLinkProjsIterator(true);
		while(it.hasNext()){
			final LinkProjectStore lps = it.next();
			if(lps.isActive() == false){
				continue;
			}
			final Object[] objs = lps.getConvBindMap();
			try{
				if(objs != null){
					final String[] tmpBindIDs = (String[])objs[0];
					final ConverterInfo[] tmpConv = (ConverterInfo[])objs[1];
					
					for (int i = 0; i < tmpBindIDs.length; i++) {
						final String tmp_bind_id = tmpBindIDs[i];
						if(tmp_bind_id.equals(bind_id)){
							return tmpConv[i];
						}
					}
				}
			}catch (final Throwable e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}

	/**
	 * 
	 * @param totalDevice
	 * @param descLabel
	 * @param back
	 * @param isCancel true:use click 'Cancel' to connect all devices
	 * @return
	 */
	public static JProgressBar showProgressBar(final int totalDevice, final int finishedDevice, final JLabel descLabel, 
			final Window[] back, final boolean[] isCancel){
		final JProgressBar bar = new JProgressBar(finishedDevice, totalDevice);
		
		final AddHarHTMLMlet currMlet = AddHarHTMLMlet.getCurrAddHarHTMLMlet();
		if(currMlet != null){
			return currMlet.showProgressBar(bar, totalDevice, finishedDevice, descLabel, isCancel);
		}
		
		bar.setStringPainted(true);
		if(totalDevice > 0){
			bar.setPreferredSize(new Dimension(500, bar.getPreferredSize().height));
			final JPanel panel = new JPanel(new GridLayout(3, 1, 0, 0));
			panel.add(descLabel);
			panel.add(bar);
			final JButton btn = App.buildDefaultCancelButton();
			{
				final JPanel btnPanel = new JPanel(new GridBagLayout());
				final GridBagConstraints c = new GridBagConstraints();
				c.anchor = GridBagConstraints.CENTER;
				c.fill = GridBagConstraints.NONE;
				btnPanel.add(btn, c);
				panel.add(btnPanel);
			}
			btn.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(final ActionEvent e) {
					back[0].dispose();
					isCancel[0] = true;
				}
			});
			
			final JPanel gapPanel = new JPanel(new GridBagLayout());
			final GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets(ClientDesc.hgap, ClientDesc.hgap, ClientDesc.vgap, ClientDesc.vgap);
			gapPanel.add(panel, c);
			
			ProcessingWindowManager.showCenterMessageOnTop(null, false, gapPanel, back);
		}
		return bar;
	}

}

package hc.server.ui.design;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import hc.App;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.server.HCActionListener;
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
import hc.server.msb.WorkingDeviceList;
import hc.server.ui.ClientDesc;
import hc.server.ui.design.hpj.HCjarHelper;

public class BindRobotSource extends IoTSource {
	final MobiUIResponsor respo;
	final ConverterInfoMap converterInfoMap = new ConverterInfoMap();

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
		try {
			final Vector<String>[] vectors = HCjarHelper.getRobotsSrc(respo.getHarMap(projID));
			if (vectors != null) {
				final Vector<String> names = vectors[0];

				list.addAll(names);
			}
		} catch (final Throwable e) {
		}
		return list;
	}

	@Override
	public ArrayList<DeviceBindInfo> getReferenceDeviceListByRobotName(final String projID, final String robotName) throws Exception {
		final ArrayList<DeviceBindInfo> list = new ArrayList<DeviceBindInfo>();

		final ProjResponser pr = respo.getProjResponser(projID);
		if (pr == null) {
			return list;
		}

		final Robot[] robots = pr.getRobots();
		if (robots == null) {
			return list;
		}

		for (int i = 0; i < robots.length; i++) {
			final Robot r = robots[i];
			if (MSBAgent.getName(r).equals(robotName)) {

				LogManager.log(
						"try [declareReferenceDeviceID] for Robot [" + robotName + "] in project [" + pr.context.getProjectID() + "]...");
				final String[] referID = (String[]) pr.recycleRes.threadPool.runAndWait(new ReturnableRunnable() {
					@Override
					public Object run() throws Throwable {
						return r.declareReferenceDeviceID();
					}
				});

				if (referID != null) {
					LogManager.log("successful [declareReferenceDeviceID] for Robot [" + robotName + "] in project ["
							+ pr.context.getProjectID() + "]...");
					for (int j = 0; j < referID.length; j++) {
						final DeviceBindInfo dbi = new DeviceBindInfo(projID, robotName);
						dbi.ref_dev_id = referID[j];
						dbi.bind_id = DeviceBindInfo.buildStandardBindID(projID, robotName, dbi.ref_dev_id);

						list.add(dbi);
					}
				} else {
					LogManager.log("NO [declareReferenceDeviceID] for Robot [" + robotName + "] in project [" + pr.context.getProjectID()
							+ "]...");
				}
				return list;
			}
		}

		return list;
	}

	@Override
	public final DeviceCompatibleDescription getDeviceCompatibleDescByRobotName(final String projID, final String robotName,
			final String referenceDeviceID) throws Exception {
		final ProjResponser pr = respo.getProjResponser(projID);
		if (pr == null) {
			return null;
		}

		final Robot[] robots = pr.getRobots();
		if (robots == null) {
			return null;
		}

		for (int i = 0; i < robots.length; i++) {
			final Robot r = robots[i];
			if (MSBAgent.getName(r).equals(robotName)) {
				LogManager.log("try [getDeviceCompatibleDescription] for Robot [" + robotName + "] in project [" + pr.context.getProjectID()
						+ "]...");
				final DeviceCompatibleDescription out = getDeviceCompatibleDescByRobotToUserThread(pr, r, referenceDeviceID);
				if (out != null) {
					LogManager.log("successful [getDeviceCompatibleDescription] for Robot [" + robotName + "] in project ["
							+ pr.context.getProjectID() + "]...");
				}
				return out;
			}
		}
		return null;
	}

	@Override
	public final DeviceCompatibleDescription getDeviceCompatibleDescByRobotToUserThread(final ProjResponser pr, final Robot r,
			final String referenceDeviceID) {
		return (DeviceCompatibleDescription) pr.recycleRes.threadPool.runAndWait(new ReturnableRunnable() {
			@Override
			public Object run() throws Throwable {
				return r.getDeviceCompatibleDescription(referenceDeviceID);
			}
		});
	}

	@Override
	public final DataDeviceCapDesc getDataForDeviceCompatibleDesc(final ProjResponser pr, final DeviceCompatibleDescription devCompDesc) {
		if (devCompDesc == null) {
			return new DataDeviceCapDesc("", "", "1.0");
		} else {
			return (DataDeviceCapDesc) pr.recycleRes.threadPool.runAndWait(new ReturnableRunnable() {
				@Override
				public Object run() throws Throwable {
					return new DataDeviceCapDesc(devCompDesc.getDescription(), devCompDesc.getCompatibleStringList(),
							devCompDesc.getVersion());
				}
			});
		}
	}
	
	public static String buildDevCapDescStr(final DataDeviceCapDesc desc) {
		return "<html>" + "<STRONG>Description : </STRONG>" + desc.desc + "<BR><STRONG>Version : </STRONG>" + desc.ver
				+ "<BR><STRONG>Compatible : </STRONG>" + desc.capList + "</html>";
	}
	
	private String buildConverterCapDescStr(final ProjResponser pr, final DeviceCompatibleDescription upDCD,
			final DeviceCompatibleDescription downDCD) {
		final DataDeviceCapDesc upDataDCD = getDataForDeviceCompatibleDesc(pr, upDCD);
		final DataDeviceCapDesc downDataDCD = getDataForDeviceCompatibleDesc(pr, downDCD);

		return "<html>" + "<STRONG>Up Description : </STRONG>" + upDataDCD.desc + "<BR><STRONG>Up Version : </STRONG>" + upDataDCD.ver
				+ "<BR><STRONG>Up Compatible : </STRONG>" + upDataDCD.capList + "<BR><BR><STRONG>Down Description : </STRONG>"
				+ downDataDCD.desc + "<BR><STRONG>Down Version : </STRONG>" + downDataDCD.ver
				+ "<BR><STRONG>Down Compatible : </STRONG>" + downDataDCD.capList + "</html>";
	}

	private void getDeviceCompatibleDescByDevice(final ProjResponser pr, final DeviceAndExt deviceAndExt, final RealDeviceInfo deviceInfo) {
		pr.recycleRes.threadPool.runAndWait(new ReturnableRunnable() {
			@Override
			public Object run() throws Throwable {
				deviceAndExt.deviceCompatibleDescription = deviceAndExt.device.getDeviceCompatibleDescription();
				deviceInfo.compatibleItem = MSBAgent.getCompatibleItemToUserThread(pr, true, deviceAndExt.deviceCompatibleDescription);
				return null;
			}
		});
	}

	private final void getConverterDescUpDownToUserThread(final ProjResponser pr, final ConverterAndExt converterAndExt, final ConverterInfo converterInfo) {
		pr.recycleRes.threadPool.runAndWait(new ReturnableRunnable() {
			@Override
			public Object run() throws Throwable {
				converterAndExt.upDeviceCompatibleDescriptionCache = converterAndExt.converter.getUpDeviceCompatibleDescription();
				converterAndExt.downDeviceCompatibleDescriptionCache = converterAndExt.converter.getDownDeviceCompatibleDescription();

				converterInfo.upCompItems = MSBAgent.getCompatibleItemToUserThread(pr, true, converterAndExt.upDeviceCompatibleDescriptionCache);
				converterInfo.downCompItems = MSBAgent.getCompatibleItemToUserThread(pr, true, converterAndExt.downDeviceCompatibleDescriptionCache);

				return converterInfo;
			}
		});
	}
	
	@Override
	public final BDNTree buildTree(final MobiUIResponsor mobiResp) throws Exception {
		final BDNTree tree = new BDNTree();
		
		final ArrayList<String> list = BindManager.getProjectIDList(this);
		for (int i = 0; i < list.size(); i++) {
			final String projID = list.get(i);

			if (BindManager.getTotalReferenceDeviceNumByProject(this, projID) == 0) {
				continue;
			}

			final ArrayList<String> robotList = BindManager.getRobotsByProjectID(this, projID);
			
			final BDNTreeNode bdnTreeNode = new BDNTreeNode();
			tree.addProjectAndRobotList(projID, robotList, bdnTreeNode);
			
			bdnTreeNode.projectNode = BindManager.buildDataNodeForProject(mobiResp, projID);

			for (int j = 0; j < robotList.size(); j++) {
				final String robotID = robotList.get(j);
				final ArrayList<DeviceBindInfo> refList = BindManager.getReferenceDeviceListByRobotName(this, projID, robotID);
				bdnTreeNode.addRefList(refList);
				
				final int refSize = refList.size();

				if (refSize == 0) {
					continue;
				}

				bdnTreeNode.addBDNForRobot(BindManager.buildDataNodeForRobot(mobiResp, projID, robotID));

				final ArrayList<BindDeviceNode> devBelowRobot = new ArrayList<BindDeviceNode>();
				for (int k = 0; k < refSize; k++) {
					final DeviceBindInfo di = refList.get(k);

					devBelowRobot.add(BindManager.buildDataNodeForRefDevInRobot(mobiResp, projID, robotID, di, this));
				}
				bdnTreeNode.addDevBelowRobot(devBelowRobot);
			}
		}
		
		return tree;
	}
	
	@Override
	public final ConverterTree buildConverterTree(final MobiUIResponsor mobiResp) throws Exception{
		final ConverterTree converterTree = new ConverterTree();
		
		final ArrayList<ConverterInfo> list = getConverterInAllProject();
		if (list == null || list.size() == 0) {
			return converterTree;
		}

		final HashSet<String> proj_map = new HashSet<String>();

		for (int i = 0; i < list.size(); i++) {
			final ConverterInfo cbi = list.get(i);
			if (proj_map.contains(cbi.proj_id) == false) {
				converterTree.addProjectNode(new BindDeviceNode(mobiResp, BindDeviceNode.PROJ_NODE, cbi.proj_id, "", null, null));
				proj_map.add(cbi.proj_id);
			}

			final BindDeviceNode userObject = new BindDeviceNode(mobiResp, BindDeviceNode.CONVERTER_NODE, cbi.proj_id, cbi.name, null,
					null);
			userObject.convBind = cbi;
			converterTree.addConverterNode(userObject);
		}
		
		return converterTree;
	}
	
	@Override
	public final DevTree buildDevTree(final MobiUIResponsor mobiResp) throws Exception{
		final DevTree devTree = new DevTree();
		
		final ArrayList<RealDeviceInfo> list = getRealDeviceInAllProject();
		if (list == null || list.size() == 0) {
			return devTree;
		}

		final HashSet<String> proj_map = new HashSet<String>();
		final HashSet<String> dev_name_map = new HashSet<String>();
		
		for (int i = 0; i < list.size(); i++) {
			final RealDeviceInfo cbi = list.get(i);
			if (proj_map.contains(cbi.proj_id) == false) {
				devTree.addProjectNode(new BindDeviceNode(mobiResp, BindDeviceNode.PROJ_NODE, cbi.proj_id, "", null, null));
				proj_map.add(cbi.proj_id);
			}

			if (dev_name_map.contains(cbi.proj_id + cbi.dev_name) == false) {
				devTree.addDevNode(new BindDeviceNode(mobiResp, BindDeviceNode.DEV_NODE, cbi.proj_id, cbi.dev_name, null, null));
				dev_name_map.add(cbi.proj_id + cbi.dev_name);
			}

			final DeviceBindInfo devBindInfo = new DeviceBindInfo(cbi.proj_id, cbi.dev_name);
			devBindInfo.ref_dev_id = cbi.dev_id;
			final BindDeviceNode userObject = new BindDeviceNode(mobiResp, BindDeviceNode.REAL_DEV_ID_NODE, cbi.proj_id, cbi.dev_name,
					devBindInfo, null);
			userObject.realDevBind = cbi;
			devTree.addRealDevNode(userObject);
		}
		
		return devTree;
	}

	@Override
	public ArrayList<ConverterInfo> getConverterInAllProject() throws Exception {
		final ArrayList<ConverterInfo> list = new ArrayList<ConverterInfo>();

		for (int i = 0; i < respo.responserSize; i++) {
			final ProjResponser pr = respo.responsors[i];
			if (pr != null) {
				final Converter[] convs = pr.getConverters();
				if (convs != null) {
					for (int j = 0; j < convs.length; j++) {
						final ConverterInfo cbi = new ConverterInfo();
						cbi.proj_id = pr.context.getProjectID();
						final Converter converter = convs[j];
						cbi.name = MSBAgent.getName(converter);
						list.add(cbi);
						
						final ConverterAndExt converterAndExt = new ConverterAndExt(converter);
						getConverterDescUpDownToUserThread(pr, converterAndExt, cbi);
						cbi.converterInfoDesc = buildConverterCapDescStr(pr, converterAndExt.upDeviceCompatibleDescriptionCache,
								converterAndExt.downDeviceCompatibleDescriptionCache);
						converterInfoMap.addConverterInfo(converterAndExt, cbi);
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
			if (pr != null) {
				final Device[] devices = pr.getDevices(WorkingDeviceList.ALL_DEVICES);
				final String projectID = pr.context.getProjectID();
				if (devices != null) {
					for (int j = 0; j < devices.length; j++) {
						final Device device = devices[j];
						final String dev_name = MSBAgent.getName(device);

						final String[] devRealIDS = (String[]) pr.recycleRes.threadPool.runAndWait(new ReturnableRunnable() {
							@Override
							public Object run() throws Throwable {
								LogManager.log(
										"try [connect] for real device IDs of Device [" + dev_name + "] in project [" + projectID + "]...");
								final String[] out = MSBAgent.getRegisterDeviceID(device, respo.msbAgent.workbench);
								LogManager.log("successful [connect] for real device IDs of Device [" + dev_name + "] in project ["
										+ projectID + "].");
								return out;
							}
						});

						if (devRealIDS != null) {
							for (int k = 0; k < devRealIDS.length; k++) {
								final RealDeviceInfo rdbi = new RealDeviceInfo();
								rdbi.proj_id = projectID;
								rdbi.dev_name = dev_name;
								rdbi.dev_id = devRealIDS[k];
								list.add(rdbi);
								
								final DeviceAndExt deviceAndExt = new DeviceAndExt(device);
								
								getDeviceCompatibleDescByDevice(pr, deviceAndExt, rdbi);
								rdbi.deviceCapDesc = getDataForDeviceCompatibleDesc(pr, deviceAndExt.deviceCompatibleDescription);
//								realDeviceInfoMap.addRealDeviceInfo(deviceAndExt, rdbi);
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
		// 由于全内存运算，所以不计性能，不用cache到变量
		final Iterator<LinkProjectStore> it = LinkProjectManager.getLinkProjsIteratorInUserSysThread(true);
		while (it.hasNext()) {
			final LinkProjectStore lps = it.next();
			if (lps.isActive() == false) {
				continue;
			}
			final Object[] objs = lps.getDevBindMap();
			try {
				if (objs != null) {
					final String[] tmpBindIDs = (String[]) objs[0];
					final RealDeviceInfo[] tmpRealDevs = (RealDeviceInfo[]) objs[1];

					for (int i = 0; i < tmpBindIDs.length; i++) {
						final String tmp_bind_id = tmpBindIDs[i];
						if (tmp_bind_id.equals(bind_id)) {
							return tmpRealDevs[i];
						}
					}
				}
			} catch (final Throwable e) {
				ExceptionReporter.printStackTrace(e);
			}
		}

		return null;
	}

	@Override
	public ConverterInfo getConverterBindInfo(final String bind_id) {
		// 由于全内存运算，所以不计性能，不用cache到变量
		final Iterator<LinkProjectStore> it = LinkProjectManager.getLinkProjsIteratorInUserSysThread(true);
		while (it.hasNext()) {
			final LinkProjectStore lps = it.next();
			if (lps.isActive() == false) {
				continue;
			}
			final Object[] objs = lps.getConvBindMap();
			try {
				if (objs != null) {
					final String[] tmpBindIDs = (String[]) objs[0];
					final ConverterInfo[] tmpConv = (ConverterInfo[]) objs[1];

					for (int i = 0; i < tmpBindIDs.length; i++) {
						final String tmp_bind_id = tmpBindIDs[i];
						if (tmp_bind_id.equals(bind_id)) {
							return tmpConv[i];
						}
					}
				}
			} catch (final Throwable e) {
				ExceptionReporter.printStackTrace(e);
			}
		}

		return null;
	}

	/**
	 * 
	 * @param totalDevice
	 * @param descLabel
	 * @param back
	 * @param isCancel
	 *            true:use click 'Cancel' to connect all devices
	 * @return
	 */
	public static JProgressBar showProgressBar(final int totalDevice, final int finishedDevice, final JLabel descLabel, final Window[] back,
			final boolean[] isCancel) {
		final JProgressBar bar = new JProgressBar(finishedDevice, totalDevice);

		final AddHarHTMLMlet currMlet = AddHarHTMLMlet.getCurrAddHarHTMLMlet();
		if (currMlet != null) {
			return currMlet.showProgressBar(bar, totalDevice, finishedDevice, descLabel, isCancel);
		}

		bar.setStringPainted(true);
		if (totalDevice > 0) {
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
			btn.addActionListener(new HCActionListener(new Runnable() {
				@Override
				public void run() {
					back[0].dispose();
					isCancel[0] = true;
				}
			}));

			final JPanel gapPanel = new JPanel(new GridBagLayout());
			final GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets(ClientDesc.hgap, ClientDesc.hgap, ClientDesc.vgap, ClientDesc.vgap);
			gapPanel.add(panel, c);

			ProcessingWindowManager.showCenterMessageOnTop(null, false, gapPanel, back);
		}
		return bar;
	}

}

package hc.server.ui.design;

import java.util.Vector;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import hc.server.msb.DeviceBindInfo;
import hc.server.msb.IoTSource;
import hc.server.util.SafeDataManager;

public class DesktopDeviceBinderWizSource implements DeviceBinderWizSource {
	final IoTSource rs;
	final MobiUIResponsor mobiResp;
	
	public DesktopDeviceBinderWizSource(final IoTSource rs, final MobiUIResponsor respo) {
		this.rs = rs;
		this.mobiResp = respo;
	}

	@Override
	public DevTree buildDevTree() throws Exception {
		return rs.buildDevTree(mobiResp);
	}

	@Override
	public ConverterTree buildConverterTree() throws Exception {
		return rs.buildConverterTree(mobiResp);
	}
	
	BDNTree bdnTree;

	@Override
	public BDNTree buildTree() throws Exception {
		if(bdnTree == null) {
			bdnTree = rs.buildTree(mobiResp);
		}
		return bdnTree;
	}
	
	private static final void saveTree(final BDNTree bdnTree) {
		final Vector<String> list = bdnTree.projectList;
		for (int i = 0; i < list.size(); i++) {
			final String projID = list.get(i);

			final Vector<String> robotList = bdnTree.bdnRobotList.get(i).robotIDList;
			final BDNTreeNode treeNode = bdnTree.treeNodeList.get(i);

			final Vector<BindDeviceNode[]> bdns = new Vector<BindDeviceNode[]>();
			for (int j = 0; j < robotList.size(); j++) {
				final String robotID = robotList.get(j);
				
				final DeviceBindInfo[] refList = treeNode.refList.get(j);
				final int refSize = refList.length;

				if (refSize == 0) {
					continue;
				}

				final BindDeviceNode[] devBelowRobot = treeNode.devBelowRobotList.get(j);
				bdns.add(devBelowRobot);
			}
			
			AddHarHTMLMlet.updateProjectBindsToLPS(projID, bdns);
		}
		
		LinkProjectManager.updateToLinkProject();
	}
	
	public static void saveImpl(final BDNTree bdnTree, final MobiUIResponsor mobiResp) {
		saveTree(bdnTree);
		
		mobiResp.msbAgent.workbench.reloadMap();
		SafeDataManager.startSafeBackupProcess(true, false);
	}

	@Override
	public void save() {
		saveImpl(bdnTree, mobiResp);
		
		if(false) {
			final JTree tree = new JTree();
			final TreeNode root = (TreeNode) tree.getModel().getRoot();
			final int rootChildCount = root.getChildCount();
			if (rootChildCount > 0) {
				for (int i = 0; i < rootChildCount; i++) {
					final DefaultMutableTreeNode projNode = (DefaultMutableTreeNode) root.getChildAt(i);
					final BindDeviceNode projBDN = (BindDeviceNode) projNode.getUserObject();
					final String projID = projBDN.projID;
	
					AddHarHTMLMlet.updateProjectBindsToLPS(projID, getBindDeviceNodesofProject(projNode));
				}
			}
	
			LinkProjectManager.updateToLinkProject();
		}
	}
	
	private Vector<BindDeviceNode[]> getBindDeviceNodesofProject(final DefaultMutableTreeNode projNode) {
		final Vector<BindDeviceNode[]> bdns = new Vector<BindDeviceNode[]>();

		final int countProjChild = projNode.getChildCount();

		if (countProjChild > 0) {
			for (int i = 0; i < countProjChild; i++) {
				final DefaultMutableTreeNode robotNode = (DefaultMutableTreeNode) projNode.getChildAt(i);
				bdns.add(getBindDeviceNodesOfRobot(robotNode));
			}
		}

		return bdns;
	}
	
	private BindDeviceNode[] getBindDeviceNodesOfRobot(final DefaultMutableTreeNode robotNode) {
		final int size = robotNode.getChildCount();
		final BindDeviceNode[] bdn = new BindDeviceNode[size];
		if (size > 0) {
			for (int i = 0; i < size; i++) {
				final DefaultMutableTreeNode referNode = (DefaultMutableTreeNode) robotNode.getChildAt(i);
				bdn[i] = (BindDeviceNode) referNode.getUserObject();
			}
		}
		return bdn;
	}

	@Override
	public void cancel() {
		cancelImpl();
	}
	
	public static void cancelImpl() {
		// 恢复原状态
		LinkProjectManager.reloadLinkProjects();
	}
}

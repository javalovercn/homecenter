package hc.server.ui.design.hpj;

import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.server.ConfigPane;
import hc.server.ui.design.Designer;

import java.util.HashMap;
import java.util.Iterator;

public class NodeEditPanelManager {
	public final void updateSkinUI(){
		final Iterator<NodeEditPanel> it = map.values().iterator();
		while(it.hasNext()){
			ConfigPane.updateComponentUI(it.next());
		}
	}
	
	public static boolean isMletNode(final HPNode node){
		if(node != null && node instanceof HPMenuItem){
			final HPMenuItem item = (HPMenuItem)node;
			return (meetHTMLMletLimit(node) || node.type == HPNode.TYPE_MENU_ITEM_SCREEN) 
					&& (item.url.equals(HCURL.URL_HOME_SCREEN) == false);
		}
		return false;
	}

	/**
	 * 满足额外符合HTMLMlet的条件，须基于isMletNode
	 * @param node
	 * @return
	 */
	public static boolean meetHTMLMletLimit(final HPNode node) {
		return node.type == HPNode.TYPE_MENU_ITEM_FORM;
	}
	
	public final NodeEditPanel switchNodeEditPanel(final int nodeType, final HPNode hpnode, final Designer designer){
		NodeEditPanel nep = null;
		if(HPNode.isNodeType(nodeType, HPNode.MASK_MENU)){
			nep = getInstance(MenuListEditPanel.class);
		}else if(HPNode.isNodeType(nodeType, HPNode.MASK_MENU_ITEM)){
			if(nodeType == HPNode.TYPE_MENU_ITEM_CONTROLLER){
				nep = getInstance(CtrlMenuItemNodeEditPanel.class);
			}else if(isMletNode(hpnode)){//isMlet
				nep = getInstance(MletNodeEditPanel.class);
			}else{
				nep = getInstance(DefaultMenuItemNodeEditPanel.class);
			}
		}else if(HPNode.isNodeType(nodeType, HPNode.MASK_ROOT)){
			nep = getInstance(ProjectNodeEditPanel.class);
		}else if(HPNode.isNodeType(nodeType, HPNode.MASK_SHARE_RB)){
			nep = getInstance(JRubyNodeEditPanel.class);
		}else if(nodeType == HPNode.MASK_RESOURCE_JAR){
			nep = getInstance(JarNodeEditPanel.class);
		}else if(nodeType == HPNode.MASK_SHARE_NATIVE){
			nep = getInstance(NativeNodeEditPanel.class);
		}else if(nodeType == HPNode.MASK_EVENT_ITEM){
			nep = getInstance(EventNodeEditPanel.class);
		}else if(nodeType == HPNode.MASK_MSB_ROBOT 
				|| nodeType == HPNode.MASK_MSB_CONVERTER 
				|| nodeType == HPNode.MASK_MSB_DEVICE){
			nep = getInstance(ProcessorNodeEditPanel.class);
		}else{
			nep = noneNodeEditPanel;
		}
		nep.designer = designer;
		return nep;
	}

	public final synchronized NodeEditPanel getInstance(final Class c){
		final String className = c.getName();
		NodeEditPanel nep = map.get(className);
		if(nep != null){
			return nep;
		}else{
			try {
				nep = (NodeEditPanel)c.newInstance();
				map.put(className, nep);
				
				return nep;
			} catch (final Exception e) {
				ExceptionReporter.printStackTrace(e);
			}
			return null;
		}
	}

	final NodeEditPanel noneNodeEditPanel = new NodeEditPanel();
	final HashMap<String, NodeEditPanel> map = new HashMap<String, NodeEditPanel>();

}

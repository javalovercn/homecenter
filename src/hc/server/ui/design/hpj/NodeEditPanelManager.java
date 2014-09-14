package hc.server.ui.design.hpj;

import hc.server.ConfigPane;

import java.util.HashMap;
import java.util.Iterator;

public class NodeEditPanelManager {
	public void updateSkinUI(){
		Iterator<NodeEditPanel> it = map.values().iterator();
		while(it.hasNext()){
			ConfigPane.updateComponentUI(it.next());
		}
	}
	
	public NodeEditPanel switchNodeEditPanel(final int nodeType){
		NodeEditPanel nep = null;
		if(HPNode.isNodeType(nodeType, HPNode.MASK_MENU)){
			nep = getInstance(MenuListEditPanel.class);
		}else if(HPNode.isNodeType(nodeType, HPNode.MASK_MENU_ITEM)){
			if(nodeType == HPNode.TYPE_MENU_ITEM_CONTROLLER){
				nep = getInstance(CtrlMenuItemNodeEditPanel.class);
			}else{
				nep = getInstance(DefaultMenuItemNodeEditPanel.class);
			}
		}else if(HPNode.isNodeType(nodeType, HPNode.MASK_ROOT)){
			nep = getInstance(ProjectNodeEditPanel.class);
		}else if(HPNode.isNodeType(nodeType, HPNode.MASK_SHARE_RB)){
			nep = getInstance(JRubyNodeEditPanel.class);
		}else if(nodeType == HPNode.MASK_RESOURCE_JAR){
			nep = getInstance(JarNodeEditPanel.class);
		}else if(nodeType == HPNode.MASK_EVENT_ITEM){
			nep = getInstance(EventNodeEditPanel.class);
		}else{
			nep = noneNodeEditPanel;
		}
		return nep;
	}

	public NodeEditPanel getInstance(Class c){
		final String className = c.getName();
		NodeEditPanel nep = map.get(className);
		if(nep != null){
			return nep;
		}else{
			try {
				nep = (NodeEditPanel)c.newInstance();
				map.put(className, nep);
				return nep;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	NodeEditPanel noneNodeEditPanel = new NodeEditPanel();
	HashMap<String, NodeEditPanel> map = new HashMap<String, NodeEditPanel>();

}

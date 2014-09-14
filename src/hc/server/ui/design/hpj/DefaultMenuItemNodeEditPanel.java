package hc.server.ui.design.hpj;

import hc.core.util.HCURL;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JPanel;

public class DefaultMenuItemNodeEditPanel extends BaseMenuItemNodeEditPanel {
	
	private JPanel myCommand_Panel = new JPanel();
	public DefaultMenuItemNodeEditPanel() {
		super();
		
		addTargetURLPanel();
		
		myCommand_Panel.setLayout(new BorderLayout());
		myCommand_Panel.add(jtascriptPanel, BorderLayout.CENTER);

		setLayout(new BorderLayout());
		add(iconPanel, BorderLayout.NORTH);
		add(myCommand_Panel, BorderLayout.CENTER);
	}
	
	private void flip_cmd_screen(int type){
		final String element = hcurl.elementID;
		
		if(type == HPNode.TYPE_MENU_ITEM_SCREEN){
			if(element.equals(HCURL.REMOTE_HOME_SCREEN)){
				myCommand_Panel.setVisible(false);
				cmd_url_panel.setVisible(false);
			}else{
				myCommand_Panel.setVisible(true);
				cmd_url_panel.setVisible(true);
			}
		}else if(type == HPNode.TYPE_MENU_ITEM_CMD){
			if(element.equals(HCURL.DATA_CMD_EXIT)){
				myCommand_Panel.setVisible(false);
				cmd_url_panel.setVisible(false);
			}else if(element.equals(HCURL.DATA_CMD_CONFIG)){
				myCommand_Panel.setVisible(false);
				cmd_url_panel.setVisible(false);
			}else{
				myCommand_Panel.setVisible(true);
				cmd_url_panel.setVisible(true);
			}
		}
		
		if(myCommand_Panel.isVisible()){
			initScript();
		}
	}
	
	public void addTargetURLPanel(){
		cmd_url_panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		cmd_url_panel.add(targetLoca);
		cmd_url_panel.add(jtfMyCommand);
		cmd_url_panel.add(testBtn);
		cmd_url_panel.add(errCommandTip);
	}

	protected void extInit(){
		flip_cmd_screen(currItem.type);
	}
}
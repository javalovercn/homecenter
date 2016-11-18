package hc.server.ui.design.hpj;

import hc.core.IConstant;
import hc.core.util.CtrlMap;
import hc.server.HCActionListener;
import hc.server.ui.CtrlResponse;
import hc.server.ui.design.hpj.ctrl.CtrlTotalPanel;
import hc.util.PropertiesManager;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

public class CtrlMenuItemNodeEditPanel extends BaseMenuItemNodeEditPanel {
	private final JPanel design_panel = new JPanel();
		private CtrlTotalPanel ctrl_panel;
		private final JPanel script_panel = new JPanel();
		private final JPanel hv_panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		
	public CtrlMenuItemNodeEditPanel() {
		super();
		
		addTargetURLPanel();
		
		JRadioButton h_button, v_button;

		{
			final JComboBox reponsor = new JComboBox();
			reponsor.setVisible(false);
			final String jruby_script = "JRuby script responsor";
			reponsor.addItem(jruby_script);
			
			reponsor.addActionListener(new HCActionListener(new Runnable() {
				@Override
				public void run() {
					script_panel.setVisible(reponsor.getSelectedItem().equals(jruby_script));
				}
			}, threadPoolToken));
			
			final JButton search = new JButton("search responsor");
			search.setVisible(false);
			search.setToolTipText("<html>search responsor class(es) in current project jar files, " +
					"<BR>responsor is a class to do response biz at server side when some key is pressed. " +					
					"<BR>which is a sub class of '"+CtrlResponse.class.getName()+"'</html>");
			search.addActionListener(new HCActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					System.out.println("do search...");
				}
			});
			
			cmd_url_panel.add(search);
			cmd_url_panel.add(reponsor);
			
			{
				final ButtonGroup bg = new ButtonGroup();
				h_button = new JRadioButton("Horizontal");//, Designer.loadImg("horizontal.png")
				bg.add(h_button);
				v_button = new JRadioButton("Vertical");//, Designer.loadImg("vertical.png")
				bg.add(v_button);
				h_button.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(final ItemEvent e) {
						if(e.getStateChange() == ItemEvent.SELECTED){
							ctrl_panel.buildSplitPanel(JSplitPane.HORIZONTAL_SPLIT);
							ctrl_panel.updateUI();
							PropertiesManager.setValue(PropertiesManager.p_DesignerCtrlHOrV, IConstant.TRUE);
							PropertiesManager.saveFile();
						}
					}
				});
				v_button.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(final ItemEvent e) {
						if(e.getStateChange() == ItemEvent.SELECTED){
							ctrl_panel.buildSplitPanel(JSplitPane.VERTICAL_SPLIT);
							ctrl_panel.updateUI();
							PropertiesManager.setValue(PropertiesManager.p_DesignerCtrlHOrV, IConstant.FALSE);
							PropertiesManager.saveFile();
						}
					}
				});
				hv_panel.add(h_button);
				hv_panel.add(v_button);
				hv_panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
				
				cmd_url_panel.add(hv_panel);
			}
		}
		
		design_panel.setLayout(new BorderLayout());
		{
			script_panel.setLayout(new BorderLayout());
			final JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
			btnPanel.add(testBtn);
			btnPanel.add(formatBtn);
			script_panel.add(btnPanel, BorderLayout.NORTH);
			script_panel.add(jtascriptPanel, BorderLayout.CENTER);
			script_panel.setBorder(new TitledBorder(""));
			
			ctrl_panel = new CtrlTotalPanel(script_panel, this, h_button, v_button);

			design_panel.add(ctrl_panel, BorderLayout.CENTER);
		}

		setLayout(new BorderLayout());
		add(iconPanel, BorderLayout.NORTH);
		add(design_panel, BorderLayout.CENTER);
		
		final String isV = PropertiesManager.getValue(PropertiesManager.p_DesignerCtrlHOrV, IConstant.TRUE);
		if(isV.equals(IConstant.TRUE)){
			h_button.setSelected(true);
		}else{
			v_button.setSelected(true);
		}
	}
	
	@Override
	public void addTargetURLPanel(){
		cmd_url_panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		cmd_url_panel.add(targetLoca);
		cmd_url_panel.add(targetLocator);
		cmd_url_panel.add(errCommandTip);
	}

	@Override
	public void extInit() {
		super.extInit();
		
		ctrl_panel.loadMap(new CtrlMap(((HPMenuItem)currItem).extendMap));
		
		ctrl_panel.repainCanvasInit();
		
		ctrl_panel.panel_canvas.findInitSize();
		
		initScript();
	}

}

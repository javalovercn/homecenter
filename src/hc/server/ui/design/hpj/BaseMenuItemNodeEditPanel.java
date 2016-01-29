package hc.server.ui.design.hpj;

import hc.App;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.core.util.ThreadPriorityManager;
import hc.core.util.UIUtil;
import hc.res.ImageSrc;
import hc.server.FileSelector;
import hc.server.HCActionListener;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.Designer;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.MutableTreeNode;

public abstract class BaseMenuItemNodeEditPanel extends ScriptEditPanel {
	final int cornDegree = 30;
	private final JPanel localnamePanel = new JPanel();
	protected HCURL hcurl;
	protected JFormattedTextField jtfMyCommand = new JFormattedTextField();
	protected JLabel errCommandTip = new JLabel();
	private final JLabel iconLabel = new JLabel();
	protected final JButton browIconBtn = new JButton("Change Icon [" + UIUtil.ICON_MAX + " X " + UIUtil.ICON_MAX + "]");
	protected final ImageIcon sys_icon = Designer.loadImg("hc_" + UIUtil.ICON_DESIGN_SHOW_SIZE + ".png");
	final JPanel jtascriptPanel = new JPanel();
	final JLabel targetLoca = new JLabel("target locator :");
	final JPanel cmd_url_panel = new JPanel();
	final JPanel iconPanel = new JPanel();

	private static final int[] URL_PROTOCAL_CODE = {HPNode.TYPE_MENU_ITEM_CMD, HPNode.TYPE_MENU_ITEM_SCREEN, 
			HPNode.TYPE_MENU_ITEM_CONTROLLER, HPNode.TYPE_MENU_ITEM_FORM, HPNode.TYPE_MENU_ITEM_CFG,
			HPNode.TYPE_MENU_ITEM_SUB_MENU};

	public abstract void addTargetURLPanel();
	
	private final void setItemIcon(final ImageIcon icon){
		iconLabel.setIcon(icon);
	}
	
	public boolean verify(final boolean refresh) {
			final String text = jtfMyCommand.getText();
			if (text.length() < 1) {
				errCommandTip.setVisible(true);
				errCommandTip.setText("error empty");
				return false;
			}
			if (text.startsWith("_")) {
				errCommandTip.setVisible(true);
				errCommandTip.setText("start with char '_'");
				return false;
			}
			if (text.indexOf("/") >= 0) {
				errCommandTip.setVisible(true);
				errCommandTip.setText("invalid char /");
				return false;
			}
			errCommandTip.setVisible(false);
			((HPMenuItem)currItem).url = hcurl.protocal + "://" + text;
			try{
				hcurl = HCURLUtil.extract(((HPMenuItem)currItem).url);
			}catch (final Exception e) {
				
			}
			if(refresh){
	////		if(searchSameElementID(tree, currItem.type, hcurl.elementID) > 1){
	//			errCommandTip.setVisible(true);
	//			errCommandTip.setText("'" + hcurl.elementID + "' is used by item");
	//			return false;
	//		}
				App.invokeLaterUI(updateTreeRunnable);
			}
			notifyModified();
			return true;
		}

	public static String getProtocal(final int code) {
		for (int i = 0; i < URL_PROTOCAL_CODE.length; i++) {
			if(code == URL_PROTOCAL_CODE[i]){
				return HCURL.URL_PROTOCAL[i];
			}
		}
		return "unkown";
	}

	public BaseMenuItemNodeEditPanel() {
		super();
		
		errCommandTip.setVisible(false);
		errCommandTip.setForeground(Color.RED);
		
		jtfMyCommand.setColumns(20);
		jtfMyCommand.setFocusLostBehavior(JFormattedTextField.COMMIT);
		jtfMyCommand.getDocument().addDocumentListener(new DocumentListener() {
			private void modify(){
				verify(true);
			}
			@Override
			public void removeUpdate(final DocumentEvent e) {
				modify();
			}
			
			@Override
			public void insertUpdate(final DocumentEvent e) {
				modify();
			}
			
			@Override
			public void changedUpdate(final DocumentEvent e) {
				modify();
			}
		});
		jtfMyCommand.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				verify(true);
			}
		}, threadPoolToken));

		jtascriptPanel.setBorder(new TitledBorder(
				"JRuby script :"));
		jtascriptPanel.setLayout(new BorderLayout());
		jtascriptPanel.add(scrollpane, BorderLayout.CENTER);
		jtascriptPanel.add(errRunInfo, BorderLayout.SOUTH);
		
		iconPanel.setLayout(new BorderLayout());
		{
			final GridBagLayout gridbag = new GridBagLayout();  
			final JPanel iPanel = new JPanel(gridbag);
			final GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;  
	        c.gridy = 0;  
	        c.gridheight = 1;
	        c.gridwidth = 1;  
	        c.weightx = 0.0;  
	        c.weighty = 0.0;  
	        c.insets = new Insets(0, 10, 0, 0);
	        c.fill = GridBagConstraints.NONE;  
	        
	        gridbag.setConstraints(iconLabel, c);  
	        
			setItemIcon(sys_icon);
			iPanel.add(iconLabel);
			iconLabel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
			
			iconPanel.add(iPanel, BorderLayout.WEST);
		}
		
		localnamePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		localnamePanel.add(new JLabel("Display Name :"));
		nameField.setColumns(10);
		localnamePanel.add(nameField);
		browIconBtn.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				BufferedImage bi;
				try {
					final File selectImageFile = FileSelector.selectImageFile(browIconBtn, FileSelector.IMAGE_FILTER, true);
					if(selectImageFile == null){
						return;
					}
					bi = ImageIO.read(selectImageFile);
				} catch (final IOException e2) {
					App.showMessageDialog(tree, e2.toString(), "Error select resource!", JOptionPane.ERROR_MESSAGE);
					return;
				}
				final BufferedImage oriImage = bi;
				if(bi != null){
					//32 X 32
					if(bi.getWidth() != UIUtil.ICON_MAX || bi.getHeight() != UIUtil.ICON_MAX){
						bi = ResourceUtil.resizeImage(bi, UIUtil.ICON_MAX, UIUtil.ICON_MAX);
					}
//					if(light == null){
//						light = (BufferedImage)Designer.loadImg("lightfil_32.png").getImage();
//					}
					bi = ImageSrc.makeRoundedCorner(bi, cornDegree);//composeImage(bi, light)
					
					final String strImageData = ServerUIUtil.imageToBase64(bi, iconBsArrayos);
					if(strImageData == null){
						return;
					}
					((HPMenuItem)currItem).imageData = strImageData;
					notifyModified();
					
					setItemIcon64(bi);
				}
			}
		}, threadPoolToken));
		localnamePanel.add(browIconBtn);
		
		{
			final JPanel iconTotal = new JPanel(new BorderLayout());
			iconTotal.add(localnamePanel, BorderLayout.CENTER);
			iconTotal.add(cmd_url_panel, BorderLayout.SOUTH);
			
			iconPanel.add(iconTotal, BorderLayout.CENTER);
		}
	}

	@Override
	Map<String, String> buildMapScriptParameter(){
		return RubyExector.toMap(hcurl);
	}
	
	@Override
	public void init(final MutableTreeNode data, final JTree tree) {
		super.init(data, tree);
	
		hcurl = HCURLUtil.extract(((HPMenuItem)currItem).url, false);
	
		currItem.type = (HCURL.getURLProtocalIdx(hcurl.protocal) + HPNode.TYPE_MENU_ITEM_CMD);
	
		if(((HPMenuItem)currItem).imageData.equals(UIUtil.SYS_DEFAULT_ICON)){
			setItemIcon(sys_icon);
		}else{
			setItemIcon64(ServerUIUtil.base64ToBufferedImage(((HPMenuItem)currItem).imageData));
		}
		
		extInit();
		
		try{
			Thread.sleep(ThreadPriorityManager.UI_WAIT_MS);
		}catch (final Exception e) {
		}
		
		this.isInited = true;
		
	}

	protected void initScript() {
		final int pnum = hcurl.getParaSize();
		String pv = "";
		for (int i = 0; i < pnum; i++) {
			if (pv.length() > 0) {
				pv += "&";
			}
			final String key = hcurl.getParaAtIdx(i);
			pv += key + "=" + hcurl.getValueofPara(key);
		}
		jtfMyCommand.setText(hcurl.elementID
				+ ((pv.length() > 0) ? ("?" + pv) : ""));
		final String listener = ((HPMenuItem)currItem).listener;
		jtaScript.setText(listener == null ? "" : listener);
		
		//代码很长时，置于首行
		jtaScript.setCaretPosition(0);
		
		initColor(true);
		updateScriptInInitProcess();
	}
	
	@Override
	public void updateScript(final String script) {
		((HPMenuItem)currItem).listener = script;
	}

	private final void setItemIcon64(BufferedImage oriImage) {
		if(oriImage.getWidth() != UIUtil.ICON_DESIGN_SHOW_SIZE || oriImage.getHeight() != UIUtil.ICON_DESIGN_SHOW_SIZE){
			oriImage = ResourceUtil.resizeImage(oriImage, UIUtil.ICON_DESIGN_SHOW_SIZE, UIUtil.ICON_DESIGN_SHOW_SIZE);
			oriImage = ImageSrc.makeRoundedCorner(oriImage, cornDegree);
		}
		setItemIcon(new ImageIcon(oriImage));
	}

}
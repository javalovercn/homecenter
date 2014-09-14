package hc.server.ui.design.hpj;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import hc.core.data.ServerConfig;
import hc.core.util.ByteUtil;
import hc.core.util.CCoreUtil;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.res.ImageSrc;
import hc.server.FileSelector;
import hc.server.ui.design.Designer;
import hc.util.ResourceUtil;

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
	private final JPanel localnamePanel = new JPanel();
	protected HCURL hcurl;
	protected JFormattedTextField jtfMyCommand = new JFormattedTextField();
	protected JLabel errCommandTip = new JLabel();
	protected final JLabel iconLabel = new JLabel();
	protected final JButton browIconBtn = new JButton("Change Icon [64 X 64]");
	protected final ImageIcon sys_icon = Designer.loadImg("hc_64.png");
	final JPanel jtascriptPanel = new JPanel();
	final JLabel targetLoca = new JLabel("target locator :");
	final JPanel cmd_url_panel = new JPanel();
	final JPanel iconPanel = new JPanel();

	protected byte[] iconBytes = new byte[1024 * 20];
	private static final int[] URL_PROTOCAL_CODE = {HPNode.TYPE_MENU_ITEM_CMD, HPNode.TYPE_MENU_ITEM_SCREEN, 
			HPNode.TYPE_MENU_ITEM_CONTROLLER, HPNode.TYPE_MENU_ITEM_FORM, HPNode.TYPE_MENU_ITEM_SUB_MENU};

	public abstract void addTargetURLPanel();
	
	public boolean verify(boolean refresh) {
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
			}catch (Exception e) {
				
			}
			if(refresh){
	////		if(searchSameElementID(tree, currItem.type, hcurl.elementID) > 1){
	//			errCommandTip.setVisible(true);
	//			errCommandTip.setText("'" + hcurl.elementID + "' is used by item");
	//			return false;
	//		}
				tree.updateUI();
			}
			notifyModified();
			return true;
		}

	public static String getProtocal(int code) {
		for (int i = 0; i < URL_PROTOCAL_CODE.length; i++) {
			if(code == URL_PROTOCAL_CODE[i]){
				return HCURL.URL_PROTOCAL[i];
			}
		}
		return "";
	}

	public BaseMenuItemNodeEditPanel() {
		super();
		iconByteArrayos.reset(iconBytes, 0);
		errCommandTip.setVisible(false);
		errCommandTip.setForeground(Color.RED);
		
		jtfMyCommand.setColumns(20);
		jtfMyCommand.setFocusLostBehavior(JFormattedTextField.COMMIT);
		jtfMyCommand.getDocument().addDocumentListener(new DocumentListener() {
			private void modify(){
				verify(true);
			}
			@Override
			public void removeUpdate(DocumentEvent e) {
				modify();
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				modify();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				modify();
			}
		});
		jtfMyCommand.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				verify(true);
			}
		});

		jtascriptPanel.setBorder(new TitledBorder(
				"JRuby script for my biz :"));
		jtascriptPanel.setLayout(new BorderLayout());
		jtascriptPanel.add(scrollpane, BorderLayout.CENTER);
		jtascriptPanel.add(errRunInfo, BorderLayout.SOUTH);
		
		iconPanel.setLayout(new BorderLayout());
		{
			GridBagLayout gridbag = new GridBagLayout();  
			JPanel iPanel = new JPanel(gridbag);
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;  
	        c.gridy = 0;  
	        c.gridheight = 1;
	        c.gridwidth = 1;  
	        c.weightx = 0.0;  
	        c.weighty = 0.0;  
	        c.insets = new Insets(0, 10, 0, 0);
	        c.fill = GridBagConstraints.NONE;  
	        
	        gridbag.setConstraints(iconLabel, c);  
	        
			iconLabel.setIcon(sys_icon);
			iPanel.add(iconLabel);
			iconLabel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
			
			iconPanel.add(iPanel, BorderLayout.WEST);
		}
		
		localnamePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		localnamePanel.add(new JLabel("Display Name :"));
		nameField.setColumns(10);
		localnamePanel.add(nameField);
		browIconBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				BufferedImage bi;
				try {
					final File selectImageFile = FileSelector.selectImageFile(browIconBtn, FileSelector.IMAGE_FILTER, true);
					if(selectImageFile == null){
						return;
					}
					bi = ImageIO.read(selectImageFile);
				} catch (IOException e2) {
					JOptionPane.showMessageDialog(tree, e.toString(), "Error select resource!", JOptionPane.ERROR_MESSAGE);
					return;
				}
				if(bi != null){
					//32 X 32
					if(bi.getWidth() != CCoreUtil.ICON_WIDTH){
						bi = ResourceUtil.resizeImage(bi, CCoreUtil.ICON_WIDTH, CCoreUtil.ICON_WIDTH);
					}
//					if(light == null){
//						light = (BufferedImage)Designer.loadImg("lightfil_32.png").getImage();
//					}

					bi = ImageSrc.makeRoundedCorner(bi, 20);//composeImage(bi, light)
					
					int doubleSize = bi.getHeight() * bi.getWidth() * 2;
					if(iconBytes.length < doubleSize){
						iconBytes = new byte[doubleSize];
						iconByteArrayos.reset(iconBytes, 0);
					}else{
						iconByteArrayos.reset();
					}
					try {
						ImageIO.write(bi, "png", iconByteArrayos);
					} catch (Exception e1) {
						return;
					}
					final int pngDataLen = iconByteArrayos.size();
					byte[] data = new byte[pngDataLen];
					System.arraycopy(iconBytes, 0, data, 0, pngDataLen);
					
					((HPMenuItem)currItem).imageData = ByteUtil.encodeBase64(data);
					notifyModified();
					
					iconLabel.setIcon(new ImageIcon(bi));
				}
			}
		});
		localnamePanel.add(browIconBtn);
		
		{
			JPanel iconTotal = new JPanel(new BorderLayout());
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
	public void init(MutableTreeNode data, JTree tree) {
		super.init(data, tree);
	
		hcurl = HCURLUtil.extract(((HPMenuItem)currItem).url, false);
	
		currItem.type = (HCURL.getURLProtocalIdx(hcurl.protocal) + HPNode.TYPE_MENU_ITEM_CMD);
	
		if(((HPMenuItem)currItem).imageData.equals(ServerConfig.SYS_DEFAULT_ICON)){
			iconLabel.setIcon(sys_icon);
		}else{
			byte[] bs = ByteUtil.decodeBase64(((HPMenuItem)currItem).imageData);
			ImageIcon icon = new ImageIcon(bs);
			iconLabel.setIcon(icon);
		}
		
		extInit();
		
		this.isInited = true;
		
	}
	
	protected abstract void extInit();

	protected void initScript() {
		int pnum = hcurl.getParaSize();
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
	}
	
	@Override
	public void updateScript(String script) {
		((HPMenuItem)currItem).listener = script;
	}

}
package hc.server.ui;

import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.LicenseHTMLMlet;
import hc.server.ui.design.ProjResponser;
import hc.util.HttpUtil;
import hc.util.ResourceUtil;
import hc.util.StringBuilderCacher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ProjectInputDialog extends Dialog{
	public ProjectInputDialog(final String title,	final String[] fieldNames, final String[] fieldDescs, 
			final String[] out){
		final int fieldNum = fieldNames.length;
		
		for (int i = 0; i < fieldNum; i++) {
			out[i] = "";
		}
		
		final ProjResponser pr = ServerUIAPIAgent.getProjResponserMaybeNull(getProjectContext());
		final BufferedImage okImage = (BufferedImage)ServerUIAPIAgent.getClientSessionAttributeForSys(UserThreadResourceUtil.getCoreSSFromCtx(getProjectContext()), pr, ClientSessionForSys.CLIENT_SESSION_ATTRIBUTE_OK_ICON);
		final int fontSizePX = okImage.getHeight();//不能采用此作为check字号，iPhone下过大
		final int areaFontSize = (int)(fontSizePX * 0.7);
		final int labelHeight = (int)(fontSizePX * 1.4);
		
		final String fieldStyle = "display: block;box-sizing: border-box;width: 100%;vertical-align:middle;font-size:" + areaFontSize + "px;";
		final int halfMobileW = getMobileWidth() / 2;
		
		final JTextField[] fields = new JTextField[fieldNum];
		for (int i = 0; i < fieldNum; i++) {
			final JTextField field = new JTextField("");
			field.setPreferredSize(new Dimension(halfMobileW, labelHeight));
			
			fields[i] = field;
			setCSSForDiv(field, null, LicenseHTMLMlet.BUTTON_FOR_DIV);
			setCSS(field, null, fieldStyle);
			
			fields[i].setColumns(18);
		}
		
		final JPanel tablePanel = new JPanel(new GridLayout(fieldNum * 2 + 1, 1));
		final J2SESession coreSSFromCtx = UserThreadResourceUtil.getCoreSSFromCtx(getProjectContext());
		{
			final String labelDivStyle = "overflow:hidden;";
			final String labelStyle = "font-weight:bold;font-size:" + areaFontSize + "px;";
			
			for (int i = 0; i < fieldNum; i++) {
				final String text = fieldNames[i];
				final JLabel label = new JLabel(text);
				
				label.setPreferredSize(new Dimension(halfMobileW, labelHeight));
				setCSSForDiv(label, null, labelDivStyle);
				setCSS(label, null, labelStyle);

				tablePanel.add(label);
				tablePanel.add(fields[i]);
			}
			
			final String descI18N = (String)ResourceUtil.get(coreSSFromCtx, 9095);
			final JLabel label = new JLabel(descI18N);
			
			label.setPreferredSize(new Dimension(halfMobileW, labelHeight));
			setCSSForDiv(label, null, labelDivStyle);
			setCSS(label, null, labelStyle);
			
			tablePanel.add(label);
		}
		
		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(tablePanel, BorderLayout.NORTH);
		final StringBuilder sb = StringBuilderCacher.getFree();
		{
			for (int i = 0; i < fieldNum; i++) {
				if(i != 0){
					sb.append("\n\n");
				}
				sb.append(fieldNames[i]);
				sb.append(" : ");
				sb.append("\n");
				if(fieldDescs != null && fieldDescs[i] != null){
					sb.append(HttpUtil.removeHtmlTag(fieldDescs[i], true));
				}else{
					sb.append("");
				}
			}
		}
		final String sbStr = sb.toString();
		StringBuilderCacher.cycle(sb);

		{
			final JTextArea area = new JTextArea();
			area.setEditable(false);
			
			area.setPreferredSize(new Dimension(halfMobileW, labelHeight * 5));
			
			area.setText(sbStr);
			final int areaBackColor = new Color(HTMLMlet.getColorForBodyByIntValue(), true).darker().getRGB();
			final int areaFontColor = new Color(HTMLMlet.getColorForFontByIntValue(), true).darker().getRGB();
			setCSS(area, null, "width:100%;height:100%;" +
					"overflow-y:auto;font-size:" + areaFontSize + "px;" +
					"background-color:#" + HTMLMlet.toHexColor(areaBackColor, false) + ";color:#" + HTMLMlet.toHexColor(areaFontColor, false) + ";");

			panel.add(area, BorderLayout.CENTER);
		}
		
		final JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.add(panel, BorderLayout.CENTER);
		
		final J2SESession coreSS = coreSSFromCtx;
		final JButton ok = new JButton(ResourceUtil.getOKI18N(coreSS), new ImageIcon(okImage));
		
		final int buttonPanelHeight = Math.max(fontSizePX + getFontSizeForNormal(), getButtonHeight());
		final String buttonStyle = "text-align:center;vertical-align:middle;display: block;width:100%;height:100%;font-size:" + fontSizePX + "px;";
		setCSS(ok, null, buttonStyle);
//		setCSSForDiv(ok, null, LicenseHTMLMlet.BUTTON_FOR_DIV);
		centerPanel.add(ok, BorderLayout.SOUTH);
		
		ok.setMinimumSize(new Dimension(10, buttonPanelHeight));
		ok.setMaximumSize(new Dimension(getMobileWidth(), buttonPanelHeight));
		
		ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				for (int i = 0; i < fieldNum; i++) {
					out[i] = fields[i].getText();
				}
				
				ProjectInputDialog.this.dismiss();
				
				synchronized (out) {
					out.notify();
				}
			}
		});
		
		addCenterPanel(this, centerPanel);
	}

	public static final void addCenterPanel(final JPanel mainPanel, final JPanel centerPanel) {
		final int max = Integer.MAX_VALUE;
		mainPanel.setLayout(new BorderLayout());
		final int emptyWidth = 10;

		{
			final JPanel leftPanel = new JPanel();
			leftPanel.setMinimumSize(new Dimension(emptyWidth, emptyWidth));
			leftPanel.setMaximumSize(new Dimension(emptyWidth, max));
			
			final JPanel rightPanel = new JPanel();
			rightPanel.setMinimumSize(new Dimension(emptyWidth, emptyWidth));
			rightPanel.setMaximumSize(new Dimension(emptyWidth, max));
			
			final JPanel top = new JPanel();
			top.setMinimumSize(new Dimension(emptyWidth, emptyWidth));
			top.setMaximumSize(new Dimension(max, emptyWidth));
			
			final JPanel bottom = new JPanel();
			bottom.setMinimumSize(new Dimension(emptyWidth, emptyWidth));
			bottom.setMaximumSize(new Dimension(max, emptyWidth));
			
			mainPanel.add(leftPanel, BorderLayout.WEST);
			mainPanel.add(rightPanel, BorderLayout.EAST);
			mainPanel.add(top, BorderLayout.NORTH);
			mainPanel.add(bottom, BorderLayout.SOUTH);
		}
		mainPanel.add(centerPanel, BorderLayout.CENTER);
	}
}	

package hc.server.ui.design;

import hc.core.util.StringUtil;
import hc.server.ui.HTMLMlet;

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

public class LicenseHTMLMlet extends HTMLMlet {
	final ActionListener acceptListener, cancelListener;
	final JTextArea area;
	
	public LicenseHTMLMlet(final String license, final ActionListener acceptListener, final ActionListener cancelListener,
			final BufferedImage okImage, final BufferedImage cancelImage,
			final String iagreeStr, final String acceptStr, final String cancelStr){
		final int gapPixel = 0;
		
		setLayout(new BorderLayout(gapPixel, gapPixel));
		
		final int fontSizePX = okImage.getHeight();

		final String accept = StringUtil.replace(acceptStr, "{iagree}", iagreeStr);
		final JLabel label = new JLabel(accept);
		setCSSForDiv(label, null, "overflow:hidden;");
		setCSS(label, null, "display:block;vertical-align:middle;font-weight:bold;font-size:" + fontSizePX + "px;");
		
		final int labelHeight = (int)(fontSizePX * 1.4);
		final int buttonPanelHeight = Math.max(fontSizePX + getFontSizeForNormal(), getButtonHeight());
		final int areaHeight = getMobileHeight() - labelHeight - buttonPanelHeight;

		final int mobileWidth = getMobileWidth();
		label.setPreferredSize(new Dimension(mobileWidth, labelHeight));
		add(label, BorderLayout.NORTH);
		
		area = new JTextArea(30, 30);
		area.setEditable(false);
		add(area, BorderLayout.CENTER);
//		setCSSForDiv(area, null, "overflow:hidden; resize: both;");
		area.setPreferredSize(new Dimension(mobileWidth, areaHeight));
		
		this.acceptListener = acceptListener;
		this.cancelListener = cancelListener;
		area.setText(license);

		final int areaBackColor = new Color(HTMLMlet.getColorForBodyByIntValue(), true).darker().getRGB();
		final int areaFontColor = new Color(HTMLMlet.getColorForFontByIntValue(), true).darker().getRGB();
		setCSS(area, null, "width:" + mobileWidth+ "px;height:" + areaHeight + "px;" +
				"overflow-y:auto;font-size:" + (int)(fontSizePX * 0.7) + "px;" +
				"background-color:#" + HTMLMlet.toHexColor(areaBackColor, false) + ";color:#" + HTMLMlet.toHexColor(areaFontColor, false) + ";");
		
		final String buttonStyle = "text-align:center;vertical-align:middle;width:100%;height:100%;font-size:" + fontSizePX + "px;";
		final JButton ok = new JButton(iagreeStr, new ImageIcon(okImage));
		ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				go(URL_EXIT);
				if(acceptListener != null){
					acceptListener.actionPerformed(e);
				}
			}
		});
		setCSS(ok, null, buttonStyle);
		
		final JButton cancel = new JButton(cancelStr, new ImageIcon(cancelImage));
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				go(URL_EXIT);
				if(cancelListener != null){
					cancelListener.actionPerformed(e);
				}
			}
		});
		setCSS(cancel, null, buttonStyle);
		
		final JPanel btnPanel = new JPanel();
		btnPanel.setLayout(new GridLayout(1, 2, gapPixel, gapPixel));
		btnPanel.add(cancel);
		btnPanel.add(ok);
		btnPanel.setPreferredSize(new Dimension(mobileWidth, buttonPanelHeight));
		
		add(btnPanel, BorderLayout.SOUTH);
	}
}

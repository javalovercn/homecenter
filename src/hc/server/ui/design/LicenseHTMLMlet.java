package hc.server.ui.design;

import hc.core.IWatcher;
import hc.core.util.StringUtil;
import hc.server.ui.HTMLMlet;
import hc.server.ui.Mlet;
import hc.server.ui.ProjectContext;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public class LicenseHTMLMlet extends SystemHTMLMlet {
	final IWatcher acceptListener, cancelListener;
	final JTextArea area;
	
	public LicenseHTMLMlet(final String license, final String acceptAll, final IWatcher acceptListener, final IWatcher cancelListener,
			final BufferedImage okImage, final BufferedImage cancelImage,
			final String iagreeStr, final String acceptStr, final String cancelStr){
		final int gapPixel = 0;
		
		acceptListener.setPara(this);
		cancelListener.setPara(this);
		
		setLayout(new BorderLayout(gapPixel, gapPixel));
		
		final int fontSizePX = okImage.getHeight();//不能采用此作为check字号，iPhone下过大
		final int areaFontSize = (int)(fontSizePX * 0.7);

		final String accept = StringUtil.replace(acceptStr, "{iagree}", iagreeStr);
		final JLabel label = new JLabel(accept);
		final String labelDivStyle = "overflow:hidden;";
		setCSSForDiv(label, null, labelDivStyle);
		final String labelStyle = "display:block;vertical-align:middle;font-weight:bold;font-size:" + areaFontSize + "px;";
		setCSS(label, null, labelStyle);
		
		final int labelHeight = (int)(fontSizePX * 1.4);
		final int buttonPanelHeight = Math.max(fontSizePX + getFontSizeForNormal(), getButtonHeight());
		final int areaHeight = getMobileHeight() - labelHeight * 2 - buttonPanelHeight;

		final int mobileWidth = getMobileWidth();
		label.setPreferredSize(new Dimension(mobileWidth, labelHeight));
		add(label, BorderLayout.NORTH);
		
		final JCheckBox acceptAllCheck = new JCheckBox(acceptAll);
		setCSSForDiv(acceptAllCheck, null, labelDivStyle);
		final String checkStyle = "vertical-align:middle;font-weight:bold;font-size:" + areaFontSize + "px;";
		setCSS(acceptAllCheck, null, checkStyle);
		final int checkBoxHeight = (int)(labelHeight * 0.8);
		setCSSForToggle(acceptAllCheck, null, "vertical-align:middle;width: " + checkBoxHeight + "px; height: " + checkBoxHeight + "px;");
		acceptAllCheck.setPreferredSize(new Dimension(mobileWidth, labelHeight));
		acceptAllCheck.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				AddHarHTMLMlet.acceptAllHARLicenseInUT(acceptAllCheck.isSelected());
			}
		});
		
		area = new JTextArea(30, 30);
		area.setEditable(false);
//		setCSSForDiv(area, null, "overflow:hidden; resize: both;");
		area.setPreferredSize(new Dimension(mobileWidth, areaHeight));
		
		final JPanel areaPanel = new JPanel(new BorderLayout());
		areaPanel.add(area, BorderLayout.CENTER);
		areaPanel.add(acceptAllCheck, BorderLayout.SOUTH);
		
		add(areaPanel, BorderLayout.CENTER);

		this.acceptListener = acceptListener;
		this.cancelListener = cancelListener;
		area.setText(license);

		final int areaBackColor = new Color(HTMLMlet.getColorForBodyByIntValue(), true).darker().getRGB();
		final int areaFontColor = new Color(HTMLMlet.getColorForFontByIntValue(), true).darker().getRGB();
		setCSS(area, null, "width:" + mobileWidth+ "px;height:" + areaHeight + "px;" +
				"overflow-y:auto;font-size:" + areaFontSize + "px;" +
				"background-color:#" + HTMLMlet.toHexColor(areaBackColor, false) + ";color:#" + HTMLMlet.toHexColor(areaFontColor, false) + ";");
		
		final String buttonStyle = "text-align:center;vertical-align:middle;width:100%;height:100%;font-size:" + fontSizePX + "px;";
		final JButton ok = new JButton(iagreeStr, new ImageIcon(okImage));
		final JButton cancel = new JButton(cancelStr, new ImageIcon(cancelImage));

		final ProjectContext ctx = getProjectContext();
		ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				ok.setEnabled(false);
				cancel.setEnabled(false);
				ctx.run(new Runnable() {
					@Override
					public void run() {
						acceptListener.watch();//由是否需要绑定，来决定back/goMlet
					}
				});
			}
		});
		setCSS(ok, null, buttonStyle);
		
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				cancelListener.watch();//由是否需要绑定，来决定back/goMlet
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
	
	@Override
	public void goMlet(final Mlet toMlet, final String targetOfMlet, final boolean isAutoReleaseCurrentMlet){
		setAutoReleaseAfterGo(isAutoReleaseCurrentMlet);
		super.goMlet(toMlet, targetOfMlet, isAutoReleaseCurrentMlet);
	}
	
	@Override
	public void back(){
		if(isAutoReleaseAfterGo() == false){
			super.back();
		}
	}
}

package hc.server.ui;

import hc.server.ui.design.ProjResponser;
import hc.server.ui.design.SystemDialog;
import hc.server.ui.design.SystemHTMLMlet;
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

public class ProjectInputDialog extends SystemDialog {
	public ProjectInputDialog(final String title, final String[] fieldNames,
			final String[] fieldDescs, final String[] out) {
		final int fieldNum = fieldNames.length;

		for (int i = 0; i < fieldNum; i++) {
			out[i] = "";
		}

		final ProjResponser pr = ServerUIAPIAgent.getProjResponserMaybeNull(getProjectContext());
		final BufferedImage okImage = (BufferedImage) ServerUIAPIAgent
				.getClientSessionAttributeForSys(localCoreSS, pr,
						ClientSessionForSys.STR_CLIENT_SESSION_ATTRIBUTE_OK_ICON);
		final int fontSizePX = okImage.getHeight();// 不能采用此作为check字号，iPhone下过大
		loadCSS(SystemHTMLMlet.buildCSS(getButtonHeight(), getFontSizeForButton(),
				getColorForFontByIntValue(), getColorForBodyByIntValue()));

		final int areaFontSize = (int) (fontSizePX * 0.7);
		final int labelHeight = (int) (fontSizePX * 1.4);

		final int halfMobileW = getMobileWidth() / 2;

		final JTextField[] fields = new JTextField[fieldNum];
		for (int i = 0; i < fieldNum; i++) {
			final JTextField field = new JTextField("");
			field.setPreferredSize(new Dimension(halfMobileW, labelHeight));

			fields[i] = field;

			setFieldCSS(field);
		}

		final JPanel tablePanel = new JPanel(new GridLayout(fieldNum * 2 + 1, 1));
		{
			for (int i = 0; i < fieldNum; i++) {
				final String text = fieldNames[i];
				final JLabel label = new JLabel(text);

				label.setPreferredSize(new Dimension(halfMobileW, labelHeight));
				setLabelCSS(label, false);

				tablePanel.add(label);
				tablePanel.add(fields[i]);
			}

			final String descI18N = getRes(9095);
			final JLabel label = new JLabel(descI18N);

			label.setPreferredSize(new Dimension(halfMobileW, labelHeight));
			setLabelCSS(label, false);

			tablePanel.add(label);
		}

		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(tablePanel, BorderLayout.NORTH);
		final StringBuilder sb = StringBuilderCacher.getFree();
		{
			for (int i = 0; i < fieldNum; i++) {
				if (i != 0) {
					sb.append("\n\n");
				}
				sb.append(fieldNames[i]);
				sb.append(" : ");
				sb.append("\n");
				if (fieldDescs != null && fieldDescs[i] != null) {
					sb.append(HttpUtil.removeHtmlTag(fieldDescs[i], true));
				} else {
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
			final int areaBackColor = new Color(HTMLMlet.getColorForBodyByIntValue(), true).darker()
					.getRGB();
			final int areaFontColor = new Color(HTMLMlet.getColorForFontByIntValue(), true).darker()
					.getRGB();
			setCSS(area, null, "width:100%;height:100%;" + "overflow-y:auto;" +
			// "font-size:" + areaFontSize + "px;" +
					"background-color:#" + HTMLMlet.toHexColor(areaBackColor, false) + ";color:#"
					+ HTMLMlet.toHexColor(areaFontColor, false) + ";");

			panel.add(area, BorderLayout.CENTER);
		}

		final JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.add(panel, BorderLayout.CENTER);

		final JButton ok = new JButton(ResourceUtil.getOKI18N(localCoreSS), new ImageIcon(okImage));
		setButtonStyle(ok);

		final int buttonPanelHeight = Math.max(fontSizePX + getFontSizeForButton(),
				getButtonHeight());
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

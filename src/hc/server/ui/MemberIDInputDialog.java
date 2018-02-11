package hc.server.ui;

import hc.core.L;
import hc.core.util.BooleanValue;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.core.util.StringUtil;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.design.ProjResponser;
import hc.server.ui.design.SystemHTMLMlet;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class MemberIDInputDialog extends BlockSystemDialog{
	public MemberIDInputDialog(final BooleanValue memberIDSetStatus, final Object threadWait){
		super(threadWait);
		
		final ProjResponser pr = ServerUIAPIAgent.getProjResponserMaybeNull(getProjectContext());
		final BufferedImage okImage = (BufferedImage)ServerUIAPIAgent.getClientSessionAttributeForSys(localCoreSS, pr, ClientSessionForSys.STR_CLIENT_SESSION_ATTRIBUTE_OK_ICON);
		final int fontSizePX = okImage.getHeight();//不能采用此作为check字号，iPhone下过大
		loadCSS(SystemHTMLMlet.buildCSS(getButtonHeight(), getFontSizeForButton(), getColorForFontByIntValue(), getColorForBodyByIntValue()));
		
		final int areaFontSize = (int)(fontSizePX * 0.7);
		final int labelHeight = (int)(fontSizePX * 1.4);
		
		final JLabel memberID = new JLabel(ResourceUtil.get(localCoreSS, 1039));//1039=Member ID
		final String descMember = ResourceUtil.buildDescPrefix(localCoreSS, ResourceUtil.get(localCoreSS, 1040));//1040=a member of family or group
		final ScriptPanel desc = new ScriptPanel();
		desc.setInnerHTML("<div style=\"padding:0.1em;"
				+ SystemHTMLMlet.LABEL_FOR_DIV_AUTO_NEW_LINE + "\">" + descMember + "</div>");

		final Dimension itemDimension = new Dimension(getMobileWidth() / 2, labelHeight);
		
		final JTextField inputField = new JTextField("");
		inputField.setPreferredSize(itemDimension);
		setFieldCSS(inputField);
		desc.setPreferredSize(new Dimension(itemDimension.width, labelHeight * 2));

		memberID.setPreferredSize(itemDimension);
		setLabelCSS(memberID, false);
		
		final JPanel inputPanel = new JPanel(new GridLayout(2, 1));
		inputPanel.add(memberID);
		inputPanel.add(inputField);
		
		final JButton ok = new JButton(ResourceUtil.getOKI18N(localCoreSS), new ImageIcon(okImage));
		setButtonStyle(ok);
		
		final int buttonPanelHeight = Math.max(fontSizePX + getFontSizeForButton(), getButtonHeight());
		
		ok.setMinimumSize(new Dimension(10, buttonPanelHeight));
		ok.setMaximumSize(new Dimension(getMobileWidth(), buttonPanelHeight));
		
		ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final String memID = inputField.getText().trim();
				
				if(memID.length() == 0){
					final String emptyStr = ResourceUtil.get(localCoreSS, 1037);//1037=can not be empty string!
					getProjectContext().sendMovingMsg(emptyStr);
					return;
				}
				
				L.V = L.WShop ? false : LogManager.log("set Member ID : " + memID);
				final char illChar = StringUtil.isValidID(memID);
				if(illChar != 0){
					getProjectContext().sendMovingMsg(ResourceUtil.get(localCoreSS, 1038));//1038=contains illegal characters!
					return;
				}
				
				MemberIDInputDialog.this.dismiss();
				
				final boolean hasSame = (Boolean)ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
					@Override
					public Object run() throws Throwable {
						return localCoreSS.checkSameMemberIDInSys(memID, false);
					}
				});
				if(hasSame){
					String msg = ResourceUtil.get(localCoreSS, 9272);
					msg = StringUtil.replace(msg, "{memID}", memID);
					final Runnable yesRun = new Runnable() {
						@Override
						public void run() {
							updateMemberID(memberIDSetStatus, threadWait, memID);
						}
					};
					final Runnable noRun = new Runnable() {
						@Override
						public void run() {
							final MemberIDInputDialog inputMemberIDDialog = new MemberIDInputDialog(memberIDSetStatus, threadWait);
							getProjectContext().sendDialogWhenInSession(inputMemberIDDialog);
						}
					};
					getProjectContext().sendQuestion(ResourceUtil.getWarnI18N(localCoreSS), msg, null, yesRun, noRun, null);
				}else{
					updateMemberID(memberIDSetStatus, threadWait, memID);
				}
			}

			protected final void updateMemberID(final BooleanValue memberIDSetStatus, final Object threadWait, final String text) {
				ServerUIAPIAgent.runInSysThread(new Runnable() {
					@Override
					public void run() {
						HCURLUtil.sendCmd(localCoreSS, HCURL.DATA_CMD_SendPara, HCURL.DATA_PARA_CHANGE_MEMBER_ID, text);
					}
				});
				UserThreadResourceUtil.getMobileAgent(localCoreSS).setMemberID(text);
				memberIDSetStatus.value = true;
				
				synchronized (threadWait) {
					threadWait.notifyAll();
				}
			}
		});
		
		final JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.add(inputPanel, BorderLayout.NORTH);
		centerPanel.add(desc, BorderLayout.CENTER);
		centerPanel.add(ok, BorderLayout.SOUTH);
		
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
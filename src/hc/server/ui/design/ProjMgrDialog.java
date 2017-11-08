package hc.server.ui.design;

import hc.core.ContextManager;
import hc.core.HCTimer;
import hc.core.util.ByteUtil;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.server.ui.ClientSessionForSys;
import hc.server.ui.HTMLMlet;
import hc.server.ui.LinkProjectStatus;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIAPIAgent;
import hc.util.HttpUtil;
import hc.util.ResourceUtil;
import hc.util.StringBuilderCacher;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

public class ProjMgrDialog extends SystemDialog {
	final String ACTIVE_PREFIX = "active_";
	final String ROOT_PREFIX = "root_";
	final String SELECTED_PREFIX = "selected_";

	final String[] colNames;
	
	final BufferedImage okImage, cancelImage, moveUpImage, moveDownImage, removeImage;
	final ProjectContext ctx;
	final ThreadGroup token;
	final ProjResponser pr;
	final JButton okButton, cancelButton, moveUpButton, moveDownButton, removeButton;
	final JPanel buttonList, bottomPanel;
	final ProjListScriptPanel projListPanel;
	Vector<String> removedProjects;
	int currentSelectedIdx = -1;

	final void refreshToolButtons(){
		if(currentSelectedIdx == -1){
			setButtonEnable(moveUpButton, false);
			setButtonEnable(moveDownButton, false);
			setButtonEnable(removeButton, false);
		}else{
			setButtonEnable(removeButton, true);
			
			if(currentSelectedIdx >= 1){
				setButtonEnable(moveUpButton, true);
			}else{
				setButtonEnable(moveUpButton, false);
			}
			
			if(currentSelectedIdx < (projList.dataRowNum - 1)){
				setButtonEnable(moveDownButton, true);
			}else{
				setButtonEnable(moveDownButton, false);
			}
		}
	}
	
	private final BaseProjList projList = new BaseProjList() {
		@Override
		public void saveUpgradeOptions() {
		}
		
		@Override
		public void restartServer() {
			
			final ProjectContext ctx = getProjectContext();
			final String reload = getRes(9261);

			LogManager.log(reload);
			ctx.showTipOnTray(reload);
			
			final String caption = ResourceUtil.getInfoI18N(localCoreSS);
			ServerUIAPIAgent.runInProjContext(ctx, new Runnable() {
				@Override
				public void run() {
					ctx.sendMessage(caption, reload, ProjectContext.MESSAGE_INFO, null, 0);
				}
			});
			
			ContextManager.getThreadPool().run(new Runnable() {
				@Override
				public void run() {
					try{
						Thread.sleep(10 * HCTimer.ONE_SECOND);
					}catch (final Exception e) {
					}
					restartServerImpl(false, null);
				}
			});
		}
		
		@Override
		public void showReferencedDependencyError(final String text) {
			ServerUIAPIAgent.runInSessionThreadPool(localCoreSS, pr, new Runnable() {
				@Override
				public void run() {
					ctx.sendMovingMsg(HttpUtil.removeHtmlTag(text, false));
				}
			});
		}

		@Override
		public void notifyNeedToSave() {
			setButtonEnable(okButton, true);
		}

		@Override
		void showNoMenuInRootError() {
			final String errMsg = LinkProjectManager.getNoMenuItemInRoot();
			ServerUIAPIAgent.runInSessionThreadPool(localCoreSS, pr, new Runnable() {
				@Override
				public void run() {
					ctx.sendMovingMsg(errMsg);
				}
			});
		}

		@Override
		void resetSave() {
			//由于关闭，无需实现
		}
	};
	
	boolean isDismissed = false;
	
	@Override
	public void dismiss(){
		synchronized (this) {
			if(isDismissed == false){
				isDismissed = true;
				ContextManager.getThreadPool().run(new Runnable() {
					@Override
					public void run() {
						LinkProjectManager.reloadLinkProjects();
						LinkProjectStatus.exitStatus();
					}
				}, token);
			}
		}
		
		super.dismiss();
	}
	
	private final String buildScriptsForListPanel(){
		final StringBuilder sb = StringBuilderCacher.getFree();
		
		sb.append("function deleteRow(row){");
		sb.append("\n");
		sb.append("document.getElementById('projListTable').deleteRow(row + 1);");
		sb.append("\n");
		sb.append("}");
		sb.append("\n");
		
		sb.append("function insRow(rowIdx, col1){");
		sb.append("\n");
		sb.append("var x=document.getElementById('projListTable').insertRow(rowIdx + 1);");
		sb.append("\n");
		sb.append("var c1=x.insertCell(0);");
		sb.append("\n");
		sb.append("c1.innerHTML=col1;");
		sb.append("\n");
		sb.append("}");
		sb.append("\n");
		
		sb.append("function movUpRow(rowIdx){");
		sb.append("\n");
		sb.append("var table=document.getElementById('projListTable');");
		sb.append("\n");
		sb.append("table.rows[rowIdx + 1].parentNode.insertBefore(table.rows[rowIdx + 1], table.rows[rowIdx]);");
		sb.append("\n");
		sb.append("}");
		
		final String out = sb.toString();
		StringBuilderCacher.cycle(sb);
		return out;
	}
	
	private final String buildInnerHTMLForListPanel(final int fontHeight){
		final StringBuilder sb = StringBuilderCacher.getFree();
		final String borderColor = HTMLMlet.toHexColor(HTMLMlet.getDarkerColor(getColorForBodyByIntValue()), false);
		sb.append("<div style=\"padding:0em 0.2em;\">");//上下有按钮边距
		sb.append("<table id=\"projListTable\" border=\"1\" style=\"width:100%;border-color:#").append(borderColor).append(";border-collapse:collapse;\">");
		sb.append("\n");
		sb.append("<thead style=\"text-align:center;\">");
		sb.append("\n");
		sb.append("<tr style=\"background-color:#").append(borderColor).append(";\">");
		
		for (int i = 0; i < colNames.length; i++) {
			final String titleRow = colNames[i];
			sb.append("\n");
			sb.append("<td>");
			sb.append(titleRow);
			sb.append("</td>");
		}
		
		sb.append("\n");
		sb.append("</tr>");
		sb.append("\n");
		sb.append("</thead>");
		sb.append("\n");
		sb.append("<tbody>");
		sb.append("\n");
		
		for (int i = 0; i < projList.dataRowNum; i++) {
			final LinkEditData led = (LinkEditData)projList.data.elementAt(i)[projList.IDX_OBJ_STORE];
			final LinkProjectStore lps = led==null?null:led.lps;
			appendRow(fontHeight, sb, i == 0, lps.isRoot(), lps.getProjectID(), lps.isActive(), lps.getProjectRemark(), lps.getProjectUpgradeURL());
		}
		
		sb.append("</tbody>");
		sb.append("\n");
		sb.append("</table>");
		sb.append("<div>");
		
		final String out = sb.toString();
		StringBuilderCacher.cycle(sb);
		return out;
	}
	
	private final void appendRow(final int fontHeight, final StringBuilder sb, final boolean selected, final boolean isRoot, final String projectID, 
			final boolean isActive, final String comment, final String upgradeURL){
		final String base64ProjID = ByteUtil.serialHexString(projectID);
		
		final String selectedID = SELECTED_PREFIX + base64ProjID;
		final String rootID = ROOT_PREFIX + base64ProjID;
		final String activeID = ACTIVE_PREFIX + base64ProjID;
		
		final String td_style = "<td style=\"text-align:center;vertical-align:middle;\">";

		sb.append("<tr>");
		sb.append(td_style);
		buildCheckCell(fontHeight, sb, selectedID, selected, "radiobutton");
		sb.append("</td>");//selected
		sb.append(td_style);
		buildCheckCell(fontHeight, sb, rootID, isRoot, null);
		sb.append("</td>");//isRoot
		sb.append("<td>").append(projectID).append("</td>");//projectID
		sb.append(td_style);
		buildCheckCell(fontHeight, sb, activeID, isActive, null);
		sb.append("</td>");//isActive
		sb.append("<td>").append(comment).append("</td>");//comment
		sb.append("<td>").append(upgradeURL).append("</td>");//upgradeURL
		sb.append("</tr>");
//		<tr>
//	        <td>1</td>
//	        <td><final input size=5 type="text" id="latbox" readonly=true/></td>
//	        <td><input size=5 type="text" id="lngbox" readonly=true/></td>
//	        <td><input type="button" id="delPOIbutton" value="Delete" onclick="deleteRow(this)"/></td>
//	        <td><input type="button" id="addmorePOIbutton" value="Add More POIs" onclick="insRow()"/></td>
//	    </tr>
	}

	private void buildCheckCell(final int fontHeight, final StringBuilder sb, final String id, final boolean selected, final String groupName) {
		final String radionOrCheckBox = groupName!=null?"radio":"checkbox";
		
		sb.append("<input id=\"").append(id).append("\" type=\"").append(radionOrCheckBox).append("\"");
		if(groupName != null){
			sb.append(" name=\"").append(groupName).append("\"");//name='radiobutton'则为一组，以实现单选
		}
		if(selected){
			sb.append(" checked");
		}
//		final float zoom = fontHeight / 8.0F;
//		sb.append(" style=\"zoom:").append(zoom)
//			.append(";-ms-transform:scale(").append(zoom).append(")")
//			.append(";-moz-transform:scale(").append(zoom).append(")")
//			.append(";-webkit-transform:scale(").append(zoom).append(")")
//			.append(";-o-transform:scale(").append(zoom).append(")")
//			.append(";transform:scale(").append(zoom).append(")")
//			.append(";\"");
		sb.append(" style=\"width:").append(fontHeight).append("px;height:").append(fontHeight).append("px;\"");
		
		sb.append(" onclick=\"window.hcserver.click('").append(id).append("');\"></input>");
	}
	
	public ProjMgrDialog(final J2SESession coreSS, final ThreadGroup token){
		ctx = getProjectContext();
		this.token = token;
		
		final String[] colNamesTemp = {getRes(7003),//Selected, 选中 
				getRes(8017),//"is Root", 
				getRes(8018),//"Project ID", 
//				getRes(8019),//"Version", 
				getRes(8020),//"Active", 
//				getRes(8021),//"Link Name", 
				getRes(8022),//"Comment", 
				getRes(8023),//upgradeURL
				};
		colNames = colNamesTemp;
		
		pr = ServerUIAPIAgent.getProjResponserMaybeNull(ctx);
		
		{
			okImage = (BufferedImage)ServerUIAPIAgent.getClientSessionAttributeForSys(localCoreSS, pr, ClientSessionForSys.STR_CLIENT_SESSION_ATTRIBUTE_OK_ICON);
			cancelImage = (BufferedImage)ServerUIAPIAgent.getClientSessionAttributeForSys(localCoreSS, pr, ClientSessionForSys.STR_CLIENT_SESSION_ATTRIBUTE_CANCEL_ICON);
			moveUpImage = (BufferedImage)ServerUIAPIAgent.getClientSessionAttributeForSys(localCoreSS, pr, ClientSessionForSys.STR_CLIENT_SESSION_ATTRIBUTE_MOV_UP_ICON);
			moveDownImage = (BufferedImage)ServerUIAPIAgent.getClientSessionAttributeForSys(localCoreSS, pr, ClientSessionForSys.STR_CLIENT_SESSION_ATTRIBUTE_MOV_DOWN_ICON);
			removeImage = (BufferedImage)ServerUIAPIAgent.getClientSessionAttributeForSys(localCoreSS, pr, ClientSessionForSys.STR_CLIENT_SESSION_ATTRIBUTE_REMOVE_ICON);
		}
		
		final int fontSizePX = okImage.getHeight();
		
		loadCSS(SystemHTMLMlet.buildCSS(getButtonHeight(), getFontSizeForButton(), getColorForFontByIntValue(), getColorForBodyByIntValue()));

		final int dialogWidth = (int)(getMobileWidth() * 0.96);
		final int dialogHeight = (int)(getMobileHeight() * 0.8);
		
		buttonList = new JPanel(new GridLayout(1, 3));
		moveUpButton = new JButton(getRes(9019), new ImageIcon(moveUpImage));
		moveDownButton = new JButton(getRes(9020), new ImageIcon(moveDownImage));
		removeButton = new JButton(getRes(9018), new ImageIcon(removeImage));
		
		buttonList.add(moveUpButton);
		buttonList.add(moveDownButton);
		buttonList.add(removeButton);
		
		final int buttonToolHeight = Math.max(fontSizePX + getFontSizeForNormal(), getButtonHeight());

		buttonList.setPreferredSize(new Dimension(dialogWidth, buttonToolHeight));

		bottomPanel = new JPanel(new GridLayout(1, 2));
		okButton = new JButton(ResourceUtil.getSaveAndApply(localCoreSS), new ImageIcon(okImage));
		cancelButton = new JButton(getRes(1018), new ImageIcon(cancelImage));
		
		bottomPanel.add(okButton);
		bottomPanel.add(cancelButton);
		
		buttonList.setPreferredSize(new Dimension(dialogWidth, buttonToolHeight));
		bottomPanel.setPreferredSize(new Dimension(dialogWidth, buttonToolHeight));
		
		projListPanel = new ProjListScriptPanel(projList, this);
		projListPanel.setInnerHTML(buildInnerHTMLForListPanel(getFontSizeForNormal()), false);
		projListPanel.loadScript(buildScriptsForListPanel(), false);
		
		final int listPanelWidth = dialogWidth;
		final int listPanelHeight = dialogHeight - buttonToolHeight * 2;
		projListPanel.setPreferredSize(new Dimension(listPanelWidth, listPanelHeight));
		setCSS(projListPanel, null, "overflow:scroll;max-width:" + listPanelWidth + "px;max-height:" + listPanelHeight + "px;");
		
		setLayout(new BorderLayout());
		
		add(buttonList, BorderLayout.NORTH);
		add(projListPanel, BorderLayout.CENTER);
		add(bottomPanel, BorderLayout.SOUTH);
		
//		setCSS(this, null, "font-size:" + getFontSizeForNormal() + "px;");
		
		setPreferredSize(new Dimension(dialogWidth, dialogHeight));
		
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final Boolean out = (Boolean)ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
					@Override
					public Object run() {
						return projList.saveAndApply();
					}
				}, token);
				
				if(out == false){
					return;
				}
				dismiss();
			}
		});
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				dismiss();
			}
		});
		
		moveUpButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				projListPanel.executeScript("movUpRow(" + currentSelectedIdx + ");");
				final Object[] items = projList.data.remove(currentSelectedIdx);
				currentSelectedIdx--;
				projList.data.add(currentSelectedIdx, items);
				
				projList.notifyNeedToSave();
				refreshToolButtons();
			}
		});
		
		moveDownButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				projListPanel.executeScript("movUpRow(" + (currentSelectedIdx + 1) + ");");
				final Object[] items = projList.data.remove(currentSelectedIdx + 1);
				projList.data.add(currentSelectedIdx, items);
				currentSelectedIdx++;
				
				projList.notifyNeedToSave();
				refreshToolButtons();
			}
		});
		
		removeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				if (projList.getActiveProjectNum() == 1 && projList.isActive(currentSelectedIdx)){
					getProjectContext().sendMovingMsg(projList.getKeepOneProjWarn(coreSS));
					return;
				}
				
				final LinkEditData led = (LinkEditData)projList.data.elementAt(currentSelectedIdx)[projList.IDX_OBJ_STORE];
				projList.delProjInList(led);
				projList.data.remove(currentSelectedIdx);
				
				projListPanel.executeScript("deleteRow(" + currentSelectedIdx + ");");
				currentSelectedIdx = -1;
				
				projList.notifyNeedToSave();
				refreshToolButtons();
				if(led.lps.isRoot()){
					projList.transRootToOtherActive(led.lps);
					projListPanel.refreshRootColumns();
				}
			}
		});
		
		setButtonStyle(okButton);
		setButtonStyle(cancelButton);
		setButtonStyle(moveDownButton);
		setButtonStyle(moveUpButton);
		setButtonStyle(removeButton);
		
		currentSelectedIdx = 0;
		refreshToolButtons();
		setButtonEnable(okButton, false);
	}

	final int searchIdxByProjectID(final String projectID){
		final int length = projList.data.size();
		for (int i = 0; i < length; i++) {
			final LinkEditData led = (LinkEditData)projList.data.elementAt(i)[projList.IDX_OBJ_STORE];
			if(led != null){
				if(led.lps.getProjectID().equals(projectID)){
					return i;
				}
			}
		}
		return -1;
	}
}

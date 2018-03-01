package hc.server.ui.design;

import hc.core.util.ByteUtil;
import hc.server.ui.ScriptPanel;
import hc.util.StringBuilderCacher;

public class ProjListScriptPanel extends ScriptPanel {
	final BaseProjList projList;
	final ProjMgrDialog projMgrDialog;

	public ProjListScriptPanel(final BaseProjList projList, final ProjMgrDialog projMgrDialog) {
		this.projList = projList;
		this.projMgrDialog = projMgrDialog;
	}

	private final void refreshActiveColumns() {
		final int length = projList.dataRowNum;
		for (int i = 0; i < length; i++) {
			final LinkEditData led = (LinkEditData) projList.data
					.elementAt(i)[projList.IDX_OBJ_STORE];
			final String base64ProjID = ByteUtil.serialHexString(led.lps.getProjectID());
			refreshCheckBox(projMgrDialog.ACTIVE_PREFIX + base64ProjID, led.lps.isActive());
		}
	}

	final void refreshRootColumns() {
		final int length = projList.dataRowNum;
		for (int i = 0; i < length; i++) {
			final LinkEditData led = (LinkEditData) projList.data
					.elementAt(i)[projList.IDX_OBJ_STORE];
			final String base64ProjID = ByteUtil.serialHexString(led.lps.getProjectID());
			refreshCheckBox(projMgrDialog.ROOT_PREFIX + base64ProjID, led.lps.isRoot());
		}
	}

	private final void refreshCheckBox(final String id, final boolean isTrue) {
		final StringBuilder sb = StringBuilderCacher.getFree();
		sb.append("document.getElementById('").append(id).append("')");
		if (isTrue) {
			sb.append(".checked=true;");
		} else {
			sb.append(".checked=false;");
			// sb.append(".removeAttribute('checked');");//此法不行，改为checked=false
		}
		final String scripts = sb.toString();
		StringBuilderCacher.cycle(sb);

		executeScript(scripts);
	}

	@Override
	public boolean onEvent(final String id, final String action, final String[] values) {
		if (ScriptPanel.CLICK.equals(action)) {
			if (id.startsWith(projMgrDialog.SELECTED_PREFIX, 0)) {
				String projID = id.substring(projMgrDialog.SELECTED_PREFIX.length());
				projID = ByteUtil.unserialHexString(projID);
				final int selectedIdx = projMgrDialog.searchIdxByProjectID(projID);
				if (selectedIdx >= 0 && selectedIdx != projMgrDialog.currentSelectedIdx) {
					projMgrDialog.currentSelectedIdx = selectedIdx;
					projMgrDialog.refreshToolButtons();
				}
				return true;
			} else if (id.startsWith(projMgrDialog.ROOT_PREFIX, 0)) {
				String projID = id.substring(projMgrDialog.ROOT_PREFIX.length());
				projID = ByteUtil.unserialHexString(projID);
				final int selectedIdx = projMgrDialog.searchIdxByProjectID(projID);
				if (selectedIdx >= 0) {
					final LinkEditData led = (LinkEditData) projList.data
							.elementAt(selectedIdx)[projList.IDX_OBJ_STORE];
					final LinkProjectStore lps = led.lps;
					projList.clickOnRoot(led, lps, !lps.isRoot());

					refreshActiveColumns();
					refreshRootColumns();
				}
				return true;
			} else if (id.startsWith(projMgrDialog.ACTIVE_PREFIX, 0)) {
				String projID = id.substring(projMgrDialog.ACTIVE_PREFIX.length());
				projID = ByteUtil.unserialHexString(projID);
				final int selectedIdx = projMgrDialog.searchIdxByProjectID(projID);
				if (selectedIdx >= 0) {
					final LinkEditData led = (LinkEditData) projList.data
							.elementAt(selectedIdx)[projList.IDX_OBJ_STORE];
					final LinkProjectStore lps = led.lps;
					projList.clickOnActive(led, lps, !lps.isActive());

					refreshActiveColumns();
					refreshRootColumns();
				}
				return true;
			}
		}
		return false;
	};
}

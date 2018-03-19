package hc.server.ui.design.hpj;

import java.util.Map;

import hc.core.HCConfig;
import hc.core.util.CCoreUtil;
import hc.core.util.HCURL;
import hc.server.ui.design.LinkProjectManager;
import hc.server.util.ContextSecurityConfig;
import hc.util.PropertiesMap;

public class HPProject extends HPNode {
	public static final String DEFAULT_VER = "1.0";
	public static final String HAR_EXT = "har";
	public static final String HAD_EXT = "had";

	public String id, ver, lastSignedVer, upgradeURL = "", exceptionURL = "", contact = "", compactDays = "", copyright = "", desc = "",
			license = "", styles = "";
	public ContextSecurityConfig csc;

	public static String convertProjectIDFromName(final String name) {
		return LinkProjectManager.buildSysProjID();
	}

	public HPProject(final int type, final String name, final String id, final String ver, final ContextSecurityConfig csc) {
		this(type, name, "", id, ver, csc, null);
	}

	public HPProject(final int type, final String name, final String i18nName, final String id, final String ver,
			final ContextSecurityConfig csc, final Map<String, Object> map) {
		super(type, name);
		this.i18nMap = HCjar.buildI18nMapFromSerial(i18nName);
		this.id = id;
		this.ver = ver;
		this.csc = csc;

		if (map != null) {
			// 注意：请初始其值为""
			this.lastSignedVer = (String) map.get(HCjar.PROJ_LAST_SIGNED_VER);
			this.upgradeURL = (String) map.get(HCjar.PROJ_UPGRADE_URL);
			this.exceptionURL = (String) map.get(HCjar.PROJ_EXCEPTION_REPORT_URL);
			this.contact = (String) map.get(HCjar.PROJ_CONTACT);
			this.copyright = (String) map.get(HCjar.PROJ_COPYRIGHT);
			this.desc = (String) map.get(HCjar.PROJ_DESC);
			this.license = (String) map.get(HCjar.PROJ_LICENSE);
			this.styles = (String) map.get(HCjar.PROJ_STYLES);

			this.compactDays = (String) map.get(HCjar.PROJ_COMPACT_DAYS);
		}
		if (compactDays == null || compactDays.length() == 0) {
			this.compactDays = String.valueOf(365 / 2);
		}
	}

	@Override
	public String toString() {
		return name + ", ver:" + ver;// , ID : " + id + "
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof HPProject) {
			final HPProject cp = (HPProject) obj;
			return cp.id.toLowerCase().equals(id.toLowerCase());
		}
		return false;
	}

	@Override
	public String validate() {
		if (id.equals(HCURL.ROOT_MENU)) {
			return "<strong>root</strong> is system reserved ID.";
		} else if (id.indexOf(PropertiesMap.EQUAL) >= 0) {
			return "invalid char [" + PropertiesMap.EQUAL + "].";
		} else if (id.indexOf(HCConfig.CFG_SPLIT) >= 0) {
			return "invalid string [" + HCConfig.CFG_SPLIT + "].";
		} else if (id.startsWith(CCoreUtil.SYS_PREFIX)) {
			return "Error project ID [" + id + "] : <strong>" + CCoreUtil.SYS_PREFIX + "</strong> is system reserved prefix.";
		}

		upgradeURL = upgradeURL.trim();

		if (upgradeURL.length() > 0) {
			if (!upgradeURL.endsWith(HAD_EXT)) {
				return "upgrade url must end with <strong>" + HAD_EXT + "</strong>, not " + HAR_EXT + " file or other.";
			} else if (!upgradeURL.startsWith("http")) {// 支持https
				return "upgrade url must start with <strong>http</strong>.";
			}
		}

		return super.validate();
	}
}

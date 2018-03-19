package hc.server.ui.design.hpj;

import hc.core.util.CCoreUtil;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.core.util.StoreableHashMap;
import hc.core.util.UIUtil;
import hc.util.I18NStoreableHashMapWithModifyFlag;

public class HPMenuItem extends HPNode {
	public static final String TARGET_LOCATOR = "Target Locator";

	public String url = "";
	public String imageData = UIUtil.SYS_DEFAULT_ICON;
	public String listener = "";
	public StoreableHashMap extendMap = new StoreableHashMap();

	public HPMenuItem(final int type, final String name) {
		this(type, name, new I18NStoreableHashMapWithModifyFlag(), HCURL.URL_CMD_EXIT, UIUtil.SYS_DEFAULT_ICON);
	}

	public HPMenuItem(final int type, final String name, final I18NStoreableHashMapWithModifyFlag i18nMap, final String url,
			final String imageData) {
		super(type, name);
		this.i18nMap = i18nMap;
		this.url = url;
		this.imageData = imageData;
	}

	@Override
	public String toString() {
		return name + " [" + (url == null ? "" : url) + "]";
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof HPMenuItem) {
			final HPMenuItem cp = (HPMenuItem) obj;
			final HCURL hcurl1 = HCURLUtil.extract(url);
			final HCURL hcurl2 = HCURLUtil.extract(cp.url);
			if (name.toLowerCase().equals(cp.name.toLowerCase()) || hcurl1.elementID.toLowerCase().equals(hcurl2.elementID.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String validate() {
		final HCURL hcurl1 = HCURLUtil.extract(url);
		final String elementID = hcurl1.elementID;
		if (elementID.startsWith(CCoreUtil.SYS_PREFIX)) {
			return "Error " + TARGET_LOCATOR + " [" + elementID + "] : <strong>" + CCoreUtil.SYS_PREFIX
					+ "</strong> is system reserved prefix.";
		} else if (elementID.indexOf(" ") >= 0) {
			return "Error " + TARGET_LOCATOR + " [" + elementID + "] : contains illegal character ' '";
		}

		return super.validate();
	}
}

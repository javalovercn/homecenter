package hc.server.ui.design;

import hc.server.util.SignHelper;
import hc.server.util.SignItem;
import hc.util.ResourceUtil;
import hc.util.SecurityDataProtector;

import java.io.File;
import java.util.Vector;

import javax.swing.JTable;

public class CertListPanel {
	public static final int COL_ALIAS = 0, COL_EXPIRES = 1;

	final Vector<SignItem> items = new Vector<SignItem>();

	final static File fileDevCert = SecurityDataProtector.getDevCertFile();

	final int COL_NUM = 2;
	final int IDX_OBJ_STORE = 1;

	public static final String COL_NAME_ALIAS = (String) ResourceUtil.get(9224);
	public static final String COL_NAME_EXPIRES = (String) ResourceUtil.get(9225);

	final Object[] colNames = { COL_NAME_ALIAS, COL_NAME_EXPIRES };

	public final boolean hasItem() {
		return items.size() > 0;
	}

	String password;

	public CertListPanel(final String pwd) {
		this.password = pwd;

		if (password != null) {
			try {
				final SignItem[] certs = SignHelper.getContentformPfx(fileDevCert, password);
				if (certs != null) {

					for (int i = 0; i < certs.length; i++) {
						final SignItem item = certs[i];
						items.add(new SignItem(item.alias, item.chain, item.privateKey));
					}
				}
			} catch (final Throwable e) {
			}
		}

	}

	public void initTable(final JTable table) {
		// 装入用户调整后的列宽度
	}
}

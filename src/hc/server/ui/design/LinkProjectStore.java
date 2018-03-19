package hc.server.ui.design;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Vector;

import hc.core.IConstant;
import hc.core.util.ByteUtil;
import hc.core.util.StoreableHashMap;
import hc.core.util.StringUtil;
import hc.server.msb.ConverterInfo;
import hc.server.msb.RealDeviceInfo;
import hc.server.ui.design.hpj.HCjar;
import hc.server.util.ContextSecurityConfig;

public class LinkProjectStore extends StoreableHashMap {
	public static final String DEL_VERSION = "0.0";
	public static final String NO_DEPLOY_TMP_DIR = "";
	private static final String NULL_REFERENCE = "null";

	private static final String FIELD_PROJ_ID = "id";
	private static final String FIELD_PROJ_REMARK = "Comment";
	private static final String FIELD_PROJ_LINK_NAME = "linkName";
	private static final String FIELD_PROJ_IS_ACTIVE = "isActive";
	private static final String FIELD_PROJ_UPGRADE_URL = "url";

	@Deprecated
	private static final String FIELD_PROJ_IMPORT_FILE_PATH = "filePath";
	@Deprecated
	private static final String FIELD_PROJ_STATUS = "status";
	@Deprecated
	private static final String FIELD_PROJ_OP = "op";

	private static final String FIELD_HAR_FILE = "harFile";
	private static final String FIELD_HAR_FILE_PARENT = "harParent";
	private static final String FIELD_DEPLOY_TMP_DIR = "dDir";
	private static final String FIELD_VERSION = "ver";
	private static final String FIELD_DOWNLOADING_VERSION = "download_ver";
	private static final String FIELD_DOWNLOADING_POSITION = "download_position";
	private static final String FIELD_DOWNLOADING_ERR = "download_err";
	private static final String FIELD_PROJ_IS_ROOT = "isRoot";
	private static final String FIELD_MENU_NAME = "menuName";
	private static final String FIELD_DONE_BIND = "isDoneBind";

	private static final String FIELD_CERT_NUM = "certNum";
	private static final String FIELD_CERT_AT_IDX = "certAtIdx";

	/**
	 * 注意：如果增加相关bind数组结构，请同步更新到copyBindTo
	 */
	private static final String FIELD_BIND_DEV_MAP = "bindDevMap";
	private static final String FIELD_BIND_CONV_MAP = "bindConvMap";

	/**
	 * 用于升级时，复制bind
	 * 
	 * @param fromLps
	 * @param toLps
	 */
	public final void copyBindTo(final LinkProjectStore toLps) {
		final String[] binds = { FIELD_BIND_CONV_MAP, FIELD_BIND_DEV_MAP };

		for (int i = 0; i < binds.length; i++) {
			final String item = binds[i];
			toLps.put(item, get(item));
		}
	}

	// 发生了升级，且升级前该工程的editBackFile被修改过。该标识用于是否使用旧编辑或新升级的版本
	private static final String FIELD_IS_CHANGED_BEFORE_UPGRADE = "chgBfrUpgrade";

	public static final String FIELD_HAS_MENU_ITEM_FOR_INSTALL = "hasMenuItemForInstall";

	public final void setBoolean(final String key, final boolean b) {
		put(key, IConstant.toString(b));
	}

	public final boolean isBoolean(final String key) {
		final Object obj = get(key);
		return IConstant.TRUE.equals(obj);
	}

	public static final String DEFAULT_UNKOWN_VER = "0.0.1";

	public final boolean isChangedBeforeUpgrade() {
		if (getValueDefault(FIELD_IS_CHANGED_BEFORE_UPGRADE, IConstant.FALSE).equals(IConstant.TRUE)) {
			return true;
		}
		return false;
	}

	public void setChangedBeforeUpgrade(final boolean changed) {
		put(FIELD_IS_CHANGED_BEFORE_UPGRADE, changed ? IConstant.TRUE : IConstant.FALSE);
	}

	public boolean isDoneBind() {
		if (getValueDefault(FIELD_DONE_BIND, IConstant.TRUE).equals(IConstant.TRUE)) {
			return true;
		}
		return false;
	}

	/**
	 * 新添加或升级的工程DoneBind=false，以便自动绑定或人工绑定
	 * 
	 * @param done
	 */
	public void setDoneBind(final boolean done) {
		put(FIELD_DONE_BIND, done ? IConstant.TRUE : IConstant.FALSE);
	}

	public String getMenuName() {
		return getValueDefault(FIELD_MENU_NAME, "Menu");
	}

	private final int getCertificateNumber() {
		return Integer.parseInt(getValueDefault(FIELD_CERT_NUM, "0"));
	}

	private final void setCertificateNumber(final int num) {
		put(FIELD_CERT_NUM, String.valueOf(num));
	}

	/**
	 * 
	 * @param idx
	 *            the start idx is zero
	 * @return
	 */
	private final X509Certificate getCertificateAtIdx(final int idx) {
		final String serial = (String) get(FIELD_CERT_AT_IDX + idx);
		return deserialX509Certificate(serial);
	}

	/**
	 * 
	 * @param idx
	 *            the start idx is zero
	 * @param cert
	 */
	private final void setCertificateAtIdx(final int idx, final X509Certificate cert) {
		final String serail = serialX509Certificate(cert);
		put(FIELD_CERT_AT_IDX + idx, serail);
	}

	/**
	 * 如果没有，则返回null
	 * 
	 * @return
	 */
	public final X509Certificate[] getCertificates() {
		final int num = getCertificateNumber();
		if (num == 0) {
			return null;
		} else {
			final X509Certificate[] out = new X509Certificate[num];
			for (int i = 0; i < num; i++) {
				out[i] = getCertificateAtIdx(i);
			}
			return out;
		}
	}

	/**
	 * 
	 * @param certs
	 *            可以为null，0长度或其它
	 */
	public final void setCertificates(final X509Certificate[] certs) {
		// remove old data
		{
			final int num = getCertificateNumber();
			for (int i = 0; i < num; i++) {
				remove(FIELD_CERT_AT_IDX + i);
			}
		}

		if (certs == null) {
			setCertificateNumber(0);
		} else {
			final int num = certs.length;
			setCertificateNumber(num);
			for (int i = 0; i < num; i++) {
				setCertificateAtIdx(i, certs[i]);
			}
		}
	}

	/**
	 * 如果没有用户指定自定义链接名，则采用被链接工程的主菜单名
	 * 
	 * @param mName
	 */
	public void setMenuName(final String mName) {
		put(FIELD_MENU_NAME, mName);
	}

	public String getDownloadingErr() {
		return getValueDefault(FIELD_DOWNLOADING_ERR, "");
	}

	public void resetDownloading() {
		setDownloadingErr("");
		setDownloadingPosition(0);
		setDownloadingVersion(DEFAULT_UNKOWN_VER);
	}

	public void setDevBindMap(final String[] bind_id, final RealDeviceInfo[] real, final int length) {
		final StringBuffer sb = new StringBuffer();

		for (int i = 0; i < length; i++) {
			if (i != 0) {
				sb.append(DOUBLE_ARRAY_SPLIT);
			}

			sb.append(bind_id[i]);

			sb.append(ARRAY_SPLIT);

			final RealDeviceInfo rdbi = real[i];

			String realValue = rdbi.proj_id;
			sb.append((realValue == null || realValue.length() == 0) ? NULL_REFERENCE : realValue);

			sb.append(ARRAY_SPLIT);

			realValue = rdbi.dev_name;
			sb.append((realValue == null || realValue.length() == 0) ? NULL_REFERENCE : realValue);

			sb.append(ARRAY_SPLIT);

			realValue = rdbi.dev_id;
			sb.append((realValue == null || realValue.length() == 0) ? NULL_REFERENCE : realValue);
		}

		put(FIELD_BIND_DEV_MAP, sb.toString());
	}

	public void setConvBindMap(final String[] bind_id, final ConverterInfo[] cbi, final int length) {
		final StringBuffer sb = new StringBuffer();

		for (int i = 0; i < length; i++) {
			if (i != 0) {
				sb.append(DOUBLE_ARRAY_SPLIT);
			}

			sb.append(bind_id[i]);

			sb.append(ARRAY_SPLIT);

			final ConverterInfo rdbi = cbi[i];

			String realValue = rdbi.proj_id;
			sb.append((realValue == null || realValue.length() == 0) ? NULL_REFERENCE : realValue);

			sb.append(ARRAY_SPLIT);

			realValue = rdbi.name;
			sb.append((realValue == null || realValue.length() == 0) ? NULL_REFERENCE : realValue);
		}

		put(FIELD_BIND_CONV_MAP, sb.toString());
	}

	public void clearBindMap() {
		remove(FIELD_BIND_DEV_MAP);
		remove(FIELD_BIND_CONV_MAP);
	}

	/**
	 * 如果未指定real时，相应的real值为空串。
	 * 
	 * @return the array of {str_bind_id[], RealDeviceInfo[]}。如果没有，则返回null
	 */
	public Object[] getDevBindMap() {
		final String serail = getValueDefault(FIELD_BIND_DEV_MAP, null);
		if (serail == null) {
			return null;
		}

		final String[] blocks = StringUtil.splitToArray(serail, DOUBLE_ARRAY_SPLIT);

		final Vector<String> p1 = new Vector<String>();
		final Vector<RealDeviceInfo> p2 = new Vector<RealDeviceInfo>();

		try {
			for (int index = 0; index < blocks.length;) {
				final String[] block = StringUtil.splitToArray(blocks[index++], ARRAY_SPLIT);

				int i = 0;
				p1.add(block[i++]);

				final RealDeviceInfo rdbi = new RealDeviceInfo();
				p2.add(rdbi);

				String _real = block[i++];
				_real = _real.equals(NULL_REFERENCE) ? "" : _real;
				rdbi.proj_id = _real;

				_real = block[i++];
				_real = _real.equals(NULL_REFERENCE) ? "" : _real;
				rdbi.dev_name = _real;

				_real = block[i++];
				_real = _real.equals(NULL_REFERENCE) ? "" : _real;
				rdbi.dev_id = _real;
			}
		} catch (final Throwable e) {
		}
		final String[] o1 = new String[p1.size()];
		final RealDeviceInfo[] o2 = new RealDeviceInfo[p1.size()];

		for (int i = 0; i < o2.length; i++) {
			o1[i] = p1.elementAt(i);
			o2[i] = p2.elementAt(i);
		}
		final Object[] out = { o1, o2 };
		return out;
	}

	/**
	 * 如果未指定real时，相应的real值为空串。
	 * 
	 * @return the array of {str_bind_id[], ConverterInfo[]}。如果没有，则返回null
	 */
	public Object[] getConvBindMap() {
		final String serail = getValueDefault(FIELD_BIND_CONV_MAP, null);
		if (serail == null) {
			return null;
		}

		final String[] blocks = StringUtil.splitToArray(serail, DOUBLE_ARRAY_SPLIT);

		final Vector<String> p1 = new Vector<String>();
		final Vector<ConverterInfo> p2 = new Vector<ConverterInfo>();

		try {
			for (int index = 0; index < blocks.length;) {
				final String[] block = StringUtil.splitToArray(blocks[index++], ARRAY_SPLIT);

				int i = 0;
				p1.add(block[i++]);

				final ConverterInfo cbi = new ConverterInfo();
				p2.add(cbi);

				String _real = block[i++];
				_real = _real.equals(NULL_REFERENCE) ? "" : _real;
				cbi.proj_id = _real;

				_real = block[i++];
				_real = _real.equals(NULL_REFERENCE) ? "" : _real;
				cbi.name = _real;
			}
		} catch (final Throwable e) {
		}

		final String[] o1 = new String[p1.size()];
		final ConverterInfo[] o2 = new ConverterInfo[p1.size()];

		for (int i = 0; i < o2.length; i++) {
			o1[i] = p1.elementAt(i);
			o2[i] = p2.elementAt(i);
		}

		final Object[] out = { o1, o2 };
		return out;
	}

	public void setDownloadingErr(final String err) {
		put(FIELD_DOWNLOADING_ERR, err);
	}

	public int getDownloadingPosition() {
		return Integer.parseInt(getValueDefault(FIELD_DOWNLOADING_POSITION, "0"));
	}

	public void setDownloadingPosition(final int position) {
		put(FIELD_DOWNLOADING_POSITION, String.valueOf(position));
	}

	public String getDownloadingVer() {
		return getValueDefault(FIELD_DOWNLOADING_VERSION, DEFAULT_UNKOWN_VER);
	}

	public void setDownloadingVersion(final String version) {
		put(FIELD_DOWNLOADING_VERSION, version);
	}

	/**
	 * 工程的版本
	 * 
	 * @return
	 */
	public String getVersion() {
		return getValueDefault(FIELD_VERSION, DEFAULT_UNKOWN_VER);
	}

	public void setVersion(final String version) {
		put(FIELD_VERSION, version);
	}

	public String getHarParentDir() {
		return getValueDefault(FIELD_HAR_FILE_PARENT, LinkProjectManager.CURRENT_DIR);
	}

	public void setHarParentDir(final String parentDir) {
		put(FIELD_HAR_FILE_PARENT, parentDir);
	}

	public String getDeployTmpDir() {
		return getValueDefault(FIELD_DEPLOY_TMP_DIR, NO_DEPLOY_TMP_DIR);
	}

	/**
	 * 存放供JRuby使用的rb文件和jar文件的数字型随机目录
	 * 
	 * @param dirName
	 */
	public void setDeployTmpDir(final String dirName) {
		put(FIELD_DEPLOY_TMP_DIR, dirName);
	}

	public String getHarFile() {
		return getValueDefault(FIELD_HAR_FILE, "");
	}

	public void setHarFile(final String harFile) {
		put(FIELD_HAR_FILE, harFile);
	}

	public String getProjectUpgradeURL() {
		return getValueDefault(FIELD_PROJ_UPGRADE_URL, "");
	}

	public void setProjectUpgradeURL(final String url) {
		put(FIELD_PROJ_UPGRADE_URL, url);
	}

	public String getProjectID() {
		return getValueDefault(FIELD_PROJ_ID, "");
	}

	public void setProjectID(final String id) {
		put(FIELD_PROJ_ID, id);
	}

	public String getProjectRemark() {
		return getValueDefault(FIELD_PROJ_REMARK, "");
	}

	public void setProjectRemark(final String remark) {
		put(FIELD_PROJ_REMARK, remark);
	}

	public String getLinkName() {
		return getValueDefault(FIELD_PROJ_LINK_NAME, "");
	}

	public void setLinkName(final String name) {
		put(FIELD_PROJ_LINK_NAME, name);
	}

	public boolean isRoot() {
		return isTrue(FIELD_PROJ_IS_ROOT);
	}

	public void setRoot(final boolean main) {
		put(FIELD_PROJ_IS_ROOT, (main ? IConstant.TRUE : IConstant.FALSE));
	}

	public boolean isActive() {
		return isTrue(FIELD_PROJ_IS_ACTIVE);
	}

	public void setActive(final boolean active) {
		put(FIELD_PROJ_IS_ACTIVE, (active ? IConstant.TRUE : IConstant.FALSE));
	}

	// public String getLinkScriptName() {
	// String link_Name = getLinkName();
	// if(link_Name.length() == 0){
	// link_Name = getMenuName();
	// }
	// return link_Name;
	// }

	public void copyFrom(final Map<String, Object> map, final boolean isForceUpdatePermission) {
		setVersion((String) map.get(HCjar.PROJ_VER));
		setProjectUpgradeURL((String) map.get(HCjar.PROJ_UPGRADE_URL));
		setMenuName(HCjar.getMenuName(map, 0));

		ContextSecurityConfig.copyMapsToLPS(this, ContextSecurityConfig.getPermissionFromHARMap(map), isForceUpdatePermission);
	}

	public static X509Certificate deserialX509Certificate(final String src) {
		final byte[] bs = ByteUtil.decodeBase64(src);
		final ByteArrayInputStream bis = new ByteArrayInputStream(bs);
		try {
			final ObjectInput in = new ObjectInputStream(bis);
			final X509Certificate cert = (X509Certificate) in.readObject();
			return cert;
		} catch (final Throwable e) {
			e.printStackTrace();
		} finally {
			try {
				bis.close();
			} catch (final Exception e) {
			}
		}
		return null;
	}

	public static String serialX509Certificate(final X509Certificate cert) {
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			final ObjectOutputStream out = new ObjectOutputStream(bos);
			out.writeObject(cert);
			final byte[] bs = bos.toByteArray();
			return ByteUtil.encodeBase64(bs);
		} catch (final Throwable e) {
			e.printStackTrace();
		} finally {
			try {
				bos.close();
			} catch (final Exception e) {
			}
		}
		return null;
	}
}

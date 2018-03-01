package hc.server.util;

import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.cache.CacheManager;
import hc.core.cache.PendStore;
import hc.core.data.DataCache;
import hc.core.util.ByteUtil;
import hc.core.util.LogManager;
import hc.server.rms.RMSLastAccessTimeManager;
import hc.server.ui.design.J2SESession;
import hc.util.PropertiesManager;

import java.util.Vector;

public abstract class CacheComparator {
	// 仅禁用cache检查在服务器端（即：不发送E_LOAD_CACHE标识信息体），手机端仍会进行cache操作。
	// 更改后，需要重启。
	static final boolean enableCache = PropertiesManager.isTrue(PropertiesManager.p_enableCache,
			true);
	static final boolean isSimu = PropertiesManager.isSimu();

	public static Vector<PendStore> getPendStoreVector(final J2SESession coreSS) {
		return coreSS.pendStoreVector;
	}

	private final byte[] code = new byte[CacheManager.CACHE_CODE_LEN];

	private final DataCache dataCache = new DataCache();

	final String projID;
	final String softUID;
	final String urlID;
	final byte[] projIDbs;
	final byte[] softUidBS;
	final byte[] urlIDbs;

	private final void pendStore(final J2SESession coreSS, final PendStore ps) {
		final Vector<PendStore> pendStoreVector = coreSS.pendStoreVector;
		synchronized (pendStoreVector) {
			pendStoreVector.addElement(ps);
		}
	}

	public CacheComparator(final String projID, final String softUID, final String urlID,
			final byte[] projIDbs, final byte[] softUidBS, final byte[] urlIDbs) {
		this.projID = projID;
		this.softUID = softUID;
		this.urlID = urlID;
		this.projIDbs = projIDbs;
		this.softUidBS = softUidBS;
		this.urlIDbs = urlIDbs;

		dataCache.setBytes(new byte[2048]);
	}

	/**
	 * 
	 * @param coreSS
	 * @param enableCacheForProc
	 *            此次调用是否使用cache，一般是true；AddHAR时menu为false
	 * @param noCycleBS
	 * @param data_idx
	 * @param data_len
	 * @param paras
	 */
	public final synchronized void encodeGetCompare(final J2SESession coreSS,
			final boolean enableCacheForProc, final byte[] noCycleBS, final int data_idx,
			final int data_len, final Object[] paras) {
		// LogManager.log("encodeGetCompare project ID : " + projID);
		// LogManager.log("encodeGetCompare mobile UID : " + uuid);
		// LogManager.log("encodeGetCompare url ID : " + urlID);

		// boolean isJ2ME = false;
		if (CacheManager.isMeetCacheLength(data_len) == false
		// || (isJ2ME =
		// .ServerUIAPIAgent.getMobileAgent().getOS().equals(ProjectContext.OS_J2ME))//由于j2me有最长rmsName限制32。
		) {
			// if(isJ2ME){
			// LogManager.warning("close cache for J2ME mobile");
			// }
			sendData(paras);
			return;
		}

		// 可能存在bug，比如sendData(paras)实现可能含有是否cache逻辑
		// if(enableCache == false){
		// sendData(paras);
		// return;
		// }

		ByteUtil.encodeFileXOR(noCycleBS, data_idx, data_len, code);
		// LogManager.log("encodeGetCompare cache code : " +
		// ByteUtil.encodeBase64(code));

		RMSLastAccessTimeManager.notifyAccess(projID, softUID);

		final byte[] cacheScriptBS;

		boolean isNewCacheItem = false;

		if (enableCacheForProc && (cacheScriptBS = CacheManager.getCacheFileBS(projID, softUID,
				urlID, code)) != null) {
			final int cacheBSLen = cacheScriptBS.length;

			if (cacheBSLen != data_len) {
				isNewCacheItem = true;
			} else {
				for (int i = 0; i < cacheBSLen; i++) {
					if (noCycleBS[data_idx + i] != cacheScriptBS[i]) {
						isNewCacheItem = true;
						break;
					}
				}
			}
		} else {
			isNewCacheItem = true;
		}

		if (isNewCacheItem || enableCache == false) {
			// sendJSBytes(scriptBS, 0, scriptBS.length, needGzip,
			// true);//注意：需要通知进行cache
			sendData(paras);

			pendStore(coreSS, new PendStore(projID, softUID, urlID, projIDbs, softUidBS, urlIDbs,
					code, noCycleBS));
		} else {
			final int dataLen = dataCache.setCacheInfo(projIDbs, 0, projIDbs.length, urlIDbs, 0,
					urlIDbs.length, code);

			// 服务端发送
			coreSS.context.sendWrap(MsgBuilder.E_LOAD_CACHE, dataCache.bs,
					MsgBuilder.INDEX_MSG_DATA, dataLen);
			L.V = L.WShop ? false
					: LogManager.log("[cache] find match cache item for [ProjID/SoftUID/urlID] : ["
							+ projID + "/" + softUID + "/" + urlID + "]");
		}
	}

	public abstract void sendData(Object[] paras);
}

package hc.server.util;

import hc.core.ContextManager;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.cache.CacheManager;
import hc.core.cache.PendStore;
import hc.core.data.DataCache;
import hc.core.util.ByteUtil;
import hc.core.util.CCoreUtil;
import hc.core.util.LogManager;
import hc.server.rms.RMSLastAccessTimeManager;
import hc.util.PropertiesManager;

import java.util.Vector;

public abstract class CacheComparator {
	static final Vector<PendStore> pendStoreVector = new Vector(32);
	
	//仅禁用cache检查在服务器端（即：不发送E_LOAD_CACHE标识信息体），手机端仍会进行cache操作。
	//更改后，需要重启。
	static final boolean enableCache = PropertiesManager.isTrue(PropertiesManager.p_enableCache, true);
	static final boolean isSimu = PropertiesManager.isSimu();
	
	public static Vector<PendStore> getPendStoreVector(){
		CCoreUtil.checkAccess();
		return pendStoreVector;
	}
	
	private final byte[] code = new byte[CacheManager.CODE_LEN];
	private final int codeLen = code.length;

	private final DataCache dataCache = new DataCache();

	final String projID; final String uuid; final String urlID;
	final byte[] projIDbs; final byte[] uuidBS; final byte[] urlIDbs;
	private final void pendStore(final PendStore ps){
		synchronized(pendStoreVector){
			pendStoreVector.addElement(ps);
		}
	}
	
	public CacheComparator(final String projID, final String uuid, final String urlID, 
			final byte[] projIDbs, final byte[] uuidBS, final byte[] urlIDbs){
		this.projID = projID;
		this.uuid = uuid;
		this.urlID = urlID;
		this.projIDbs = projIDbs;
		this.uuidBS = uuidBS;
		this.urlIDbs = urlIDbs;
		
		dataCache.setBytes(new byte[2048]);
	}
	
	public final synchronized void encodeGetCompare(final byte[] data, final int data_idx, final int data_len, final Object[] paras) {
//		L.V = L.O ? false : LogManager.log("encodeGetCompare project ID : " + projID);
//		L.V = L.O ? false : LogManager.log("encodeGetCompare mobile UID : " + uuid);
//		L.V = L.O ? false : LogManager.log("encodeGetCompare url ID : " + urlID);
		
//		boolean isJ2ME = false;
		if(CacheManager.isMeetCacheLength(data_len) == false 
//				|| (isJ2ME = ClientDesc.getAgent().getOS().equals(ProjectContext.OS_J2ME))//由于j2me有最长rmsName限制32。
				){
//			if(isJ2ME){
//				LogManager.warning("close cache for J2ME mobile");
//			}
			sendData(paras);
			return;
		}

//		可能存在bug，比如sendData(paras)实现可能含有是否cache逻辑
//		if(enableCache == false){
//			sendData(paras);
//			return;
//		}
		
		boolean isNeedCache = false;
		
		ByteUtil.encodeFileXOR(data, data_idx, data_len, code, 0, codeLen);
//		L.V = L.O ? false : LogManager.log("encodeGetCompare cache code : " + ByteUtil.encodeBase64(code));
		
		RMSLastAccessTimeManager.notifyAccess(projID, uuid);
		
		final byte[] cacheScriptBS = CacheManager.getCacheFileBS(projID, uuid, urlID, code, 0, codeLen);
		
		if(cacheScriptBS != null){
			final int cacheBSLen = cacheScriptBS.length;
			
			if(cacheBSLen != data_len){
				isNeedCache = true;
			}else{
				for (int i = 0; i < cacheBSLen; i++) {
					if(data[data_idx + i] != cacheScriptBS[i]){
						isNeedCache = true;
						break;
					}
				}
			}
		}else{
			isNeedCache = true;
		}
		
		if(isNeedCache || enableCache == false){
//			sendJSBytes(scriptBS, 0, scriptBS.length, needGzip, true);//注意：需要通知进行cache
			sendData(paras);
			
			pendStore(new PendStore(projID, uuid, urlID, 
					projIDbs, uuidBS, urlIDbs, 
					code, data));
		}else{
			final int dataLen = dataCache.setCacheInfo(projIDbs, 0, projIDbs.length, urlIDbs, 0, urlIDbs.length, code, 0, codeLen);
			
			//服务端发送
			ContextManager.getContextInstance().sendWrap(MsgBuilder.E_LOAD_CACHE, dataCache.bs, MsgBuilder.INDEX_MSG_DATA, dataLen);		
			if(isSimu){
				L.V = L.O ? false : LogManager.log("[cache] find match cache item for [" + projID + "/" + uuid + "/" + urlID + "]");
			}
		}
	}

	public abstract void sendData(Object[] paras);
}

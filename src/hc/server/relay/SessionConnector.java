package hc.server.relay;

import hc.core.HCTimer;
import hc.core.IConstant;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.RemoveableHCTimer;
import hc.core.util.CCoreUtil;
import hc.core.util.LinkedSet;
import hc.core.util.LogManager;
import hc.server.nio.ByteBufferCacher;
import hc.server.nio.UDPPair;
import hc.server.util.ByteArr;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Random;

public class SessionConnector {
	//服务器端接入Channel
	public SocketChannel serverSide;
	public long firstServerRegMS;
	//手机端端接入Channel
	public SocketChannel clientSide;
	public boolean isDelTDN = false;
	
	public SelectionKey serverKey, clientKey;
	public final LinkedSet writeToServerBackSet, writeToClientBackSet;
	private int sizeWriteToServerBackSet, sizeWriteToClientBackSet;
	public final ByteBufferCacher bbCache;
	public ByteArr uuidbs;
	public String token;
	private boolean isClientReset = false, isServerReset = false;
	public UDPPair udpPair;
	private HCTimer resetTimer = null;
	public final byte[] randomUDPHead = new byte[MsgBuilder.LEN_UDP_HEADER];
	
	public void buildRandomUDPHeader(final byte[] bs, final int fillStartIdx){
		final Random r = new Random(System.currentTimeMillis());

		for (int i = 0; i < randomUDPHead.length; i++) {
			randomUDPHead[i] = (byte) (r.nextInt() & 0xFF);
			bs[fillStartIdx + i] = randomUDPHead[i];
		}
	}
	
	//投入使用，则状态为false；回收后，则状态为true
	public boolean isNewStatus = true;
	
	public static boolean resetXXSideUDPAddressNull(final byte[] bs, final int offset, final int len, final boolean isServer,
			final byte udpRandomHead0, final byte udpRandomHead1){
		final SessionConnector sc;
		
		synchronized (RelayManager.tdn) {
			sc = RelayManager.tdn[len].getNodeData(bs, offset, offset + len);
		}
		
		if(sc != null){
			if(sc.randomUDPHead[0] == udpRandomHead0 && sc.randomUDPHead[1] == udpRandomHead1){
				L.V = L.O ? false : LogManager.log("SetUDPAddrNull match the randomUDPHeader");
				final UDPPair udpPair = sc.udpPair;
				if(udpPair == null){
					return false;
				}
				if(udpPair.isServerPort == isServer){
					udpPair.addr = null;
					return true;
				}if(udpPair.target != null && (udpPair.target.isServerPort == isServer)){
					udpPair.target.addr = null;
					return true;
				}
			}
		}
		return false;
	}
	
	public SessionConnector(final ByteBufferCacher bbCache) {
		writeToServerBackSet = new LinkedSet();
		writeToClientBackSet = new LinkedSet();
		
		this.bbCache = bbCache;
	}
	
	public boolean isReset(final boolean serverOrClient){
		return serverOrClient?isServerReset:isClientReset;
	}
	
	public final void setReset(final boolean serverOrClient, final boolean isLineOff){
//		改为仅TCP模式
//		if((serverOrClient == false)//手机端 
//				&& isLineOff){//产生断线事件
//			//手机端不能启动重置连接逻辑，因为手机端的环境复杂性，将保持手机端支持无TCP下，仅UDP的工作状态。
//			L.V = L.O ? false : LogManager.log("mobile offline, skip resetTimer");
//			return;
//		}
		
		if(isLineOff){
			if(resetTimer == null){
				final SessionConnector self = this;
				resetTimer = new RemoveableHCTimer("ResetTimer", CCoreUtil.WAIT_MS_FOR_NEW_CONN - 1000, true) {//20秒改为3秒
					@Override
					public final void doBiz() {
						L.V = L.O ? false : LogManager.log("ResetTimer : Server/Client not reconnect, so close session pair.");
						RelayManager.closePare(self, true);
						setEnable(false);
						L.V = L.O ? false : LogManager.log("ResetTimer : Close Pare");
					}
					
					@Override
					public void setEnable(final boolean enable){
						L.V = L.O ? false : LogManager.log("ResetTimer : " + enable);
						super.setEnable(enable);
					}
				};
			}
			
			//开启,有可能是重用，故调用一次setEnable
			resetTimer.resetTimerCount();
			synchronized (this) {
				if(isNewStatus == false){
//					L.V = L.O ? false : LogManager.log("Enable ResetTimer");
					resetTimer.setEnable(true);
				}else{
//					L.V = L.O ? false : LogManager.log("Try Enable ResetTimer, but isNewStatus, Skip Enable.");
				}
			}
		}
		
		if(serverOrClient){
			isServerReset = isLineOff;	
		}else{
			isClientReset = isLineOff;
		}
		
		if(isClientReset == false && isServerReset == false){
			resetTimer.setEnable(false);
		}
	}
	
	/**
	 * 将一个数据体加挂到输出Channel的后备队列
	 * @param writetarget
	 * @param bb
	 * @param serverOrClientReset 如果writetarget为空，则通过本参数来指明为输出到服务端或客户端
	 */
	public void appendWriteSet(final SocketChannel writetarget, final ByteBuffer bb, final boolean serverOrClientReset){
		if(serverSide == writetarget){
			sizeWriteToServerBackSet++;
			writeToServerBackSet.addTail(bb);
		}else if(clientSide == writetarget){
			sizeWriteToClientBackSet++;
			writeToClientBackSet.addTail(bb);
		}else if(writetarget == null){
			//因为ClientReset，可能使writetarget为null，而进行数据暂存
			if(serverOrClientReset){
				sizeWriteToServerBackSet++;
				writeToServerBackSet.addTail(bb);
			}else{
				sizeWriteToClientBackSet++;
				writeToClientBackSet.addTail(bb);
			}
		}
	}
	
	public void setRewriteSet(final SocketChannel writetarget, final ByteBuffer bb){
		if(serverSide == writetarget){
			sizeWriteToServerBackSet++;
			writeToServerBackSet.addToFirst(bb);
		}else if(clientSide == writetarget){
			sizeWriteToClientBackSet++;
			writeToClientBackSet.addToFirst(bb);
		}
	}
	
	/**
	 * 
	 * @param writetarget
	 * @param serverOrClientReset
	 * @return 返回999999999，表示错误
	 */
	public int getWriteSetSize(final SocketChannel writetarget, final boolean serverOrClientReset){
		if(serverSide == writetarget){
			return sizeWriteToServerBackSet;
		}else if(clientSide == writetarget){
			return sizeWriteToClientBackSet;
		}else if(writetarget == null){
			//检查重连模式前，是否存在缓存数据
			if(serverOrClientReset){
				return sizeWriteToServerBackSet;
			}else{
				return sizeWriteToClientBackSet;
			}
		}
		return 999999999;
	}
	
	/**
	 * 从输出Channel的取出一个数据体
	 * @param writetarget
	 * @return
	 */
	public ByteBuffer getWriteSet(final SocketChannel writetarget){
		if(serverSide == writetarget){
			if(sizeWriteToServerBackSet > 0){
				sizeWriteToServerBackSet--;
				return (ByteBuffer)writeToServerBackSet.getFirst();
			}
		}else if(clientSide == writetarget){
			if(sizeWriteToClientBackSet > 0){
				sizeWriteToClientBackSet--;
				return (ByteBuffer)writeToClientBackSet.getFirst();
			}
		}
		
		return null;
	}
	
	public String getUUIDString(){
		try {
			return new String(uuidbs.bytes, 0, uuidbs.len, IConstant.UTF_8);
		} catch (final UnsupportedEncodingException e) {
			return new String(uuidbs.bytes, 0, uuidbs.len);
		}
	}
	
	/**
	 * 如果是ServerChannel返回true；如果是ClientChannel返回false。否则返回异常
	 * @param channel
	 * @return
	 * @throws Exception
	 */
	public boolean isServerChannel(final SocketChannel channel) {
		if(serverSide == channel){
			return true;
		}else {//if(fromClient == channel){
			return false;
//		}else{
//			throw new Exception("No server or client channel");
		}
	}
	
	public SocketChannel getTarget(final SocketChannel channel){
		if(serverSide == channel){
			return clientSide;
		}else if(clientSide == channel){
			return serverSide;
		}else{
			return null;
		}
	}
	
	public void setKey(final SocketChannel channel, final SelectionKey sk, final boolean isFromServer){
		if(isFromServer){
			if(serverSide != null){
				L.V = L.O ? false : LogManager.log("override old Server channel:" + serverSide.hashCode());
			}
			serverSide = channel;
			serverKey = sk;
		}else{
			if(clientSide != null){
				L.V = L.O ? false : LogManager.log("override old Client channel:" + clientSide.hashCode());
			}
			clientSide = channel;
			clientKey = sk;
		}
	}
	
	public void reset(){
		clientSide = null;
		serverSide = null;
		clientKey = null;
		serverKey = null;
		uuidbs = null;
		token = null;
		isDelTDN = false;
		
		if(resetTimer != null){
			resetTimer.resetTimerCount();
			resetTimer.setEnable(false);
		}

		isServerReset = false;
		isClientReset = false;
		
		if(udpPair != null){
			udpPair.reset();
			udpPair = null;
		}
		
		cyc(writeToServerBackSet);
		sizeWriteToServerBackSet = 0;
		
		cyc(writeToClientBackSet);
		sizeWriteToClientBackSet = 0;
	}

	private void cyc(final LinkedSet linkedSet) {
		bbCache.cycleSet(linkedSet);
	}
	
	/**
	 * 如果另块也为空，则返回true
	 * @param channel
	 * @return
	 */
	public void setNullKey(final SocketChannel channel){
		if(channel == serverSide){
			serverSide = null;
		}else if(channel == clientSide){
			clientSide = null;
		}
	}
	
}

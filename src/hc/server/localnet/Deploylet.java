package hc.server.localnet;

import hc.core.IConstant;
import hc.core.L;
import hc.core.util.ByteArrayCacher;
import hc.core.util.ByteUtil;
import hc.core.util.LogManager;
import hc.server.ui.design.LinkProjectManager;
import hc.util.ResourceUtil;

import java.io.IOException;

public class Deploylet {
	private static final ByteArrayCacher cache = ByteUtil.byteArrayCacher;
	private final DeploySocket socket;
	private byte[] passwordBS;
	private int errAuthCount;
	
	public final int getErrAuthCount(){
		return errAuthCount;
	}
	
	public Deploylet(final DeploySocket socket){
		this.socket = socket;
	}
	
	private final byte[] getPasswordBS(){
		if(passwordBS == null){
			passwordBS = IConstant.getPasswordBS();
		}
		return passwordBS;
	}
	
	public final void processOneClient(){
		try{
			processLoop();
		}catch (final Throwable e) {
		}finally{
			if(socket != null && socket.socket != null){
				L.V = L.WShop ? false : LogManager.log("[Deploy] close a deploy session at " + socket.socket.hashCode());
			}
			try{
				socket.close();
			}catch (final Throwable e) {
			}
		}
		
	}

	String projectID;
	
	private final void processLoop() throws IOException {
		while(true){
			if(socket != null && socket.socket != null){
				L.V = L.WShop ? false : LogManager.log("[Deploy] ready to receive at " + socket.socket.hashCode());
			}
			final byte header = socket.receive();
			final int headerLen = socket.receiveDataLen();
			
			if(header == DeploySocket.H_BYE){
				if(socket != null && socket.socket != null){
					L.V = L.WShop ? false : LogManager.log("[Deploy] receive bye at " + socket.socket.hashCode());
				}
				break;
			}else if(header == DeploySocket.H_HELLO){
				final byte[] projIDbs = socket.receiveData(headerLen, null);
				final String projID = ByteUtil.buildString(projIDbs, 0, headerLen, IConstant.UTF_8);
				
				if(LinkProjectManager.checkActiveProject(projID)){
					projectID = projID;
					socket.sendHeader(DeploySocket.H_HELLO, headerLen);
					socket.sendData(projIDbs, 0, headerLen, false, false, null);
				}else{
					socket.sendHeader(DeploySocket.H_HELLO, 0);
				}

				cache.cycle(projIDbs);
			}else if(header == DeploySocket.H_REQ_AUTH_FROM_CLIENT){
				L.V = L.WShop ? false : LogManager.log("[Deploy] receive H_REQ_CHECK_FROM_CLIENT.");
				
				final int authLen = DeploySocket.AUTH_LEN;
				
				final byte[] random = cache.getFree(authLen);
				ResourceUtil.buildRandom(random);
				final byte[] cloneRandom = cache.getFree(authLen);
				for (int i = 0; i < authLen; i++) {
					cloneRandom[i] = random[i];
				}
				DeploySender.flipRandom(cloneRandom, authLen);
				
				socket.sendHeader(DeploySocket.H_AUTH, authLen);
				
				socket.sendData(random, 0, authLen, true, true, getPasswordBS());
				
				if(socket.receive() != DeploySocket.H_AUTH){
					throw new IOException("should be H_AUTH");
				}
				
				final int receiveDataLen = socket.receiveDataLen();
				if(receiveDataLen != authLen){
					throw new IOException("should be " + authLen + ", but " + receiveDataLen);
				}
				
				socket.receiveData(random, authLen, getPasswordBS());
				
				boolean isFailAuth = false;
				for (int i = 0; i < authLen; i++) {
					if(cloneRandom[i] != random[i]){
						LogManager.errToLog("receive-deploy service receive a error password connection!");
						isFailAuth = true;
						socket.sendError(DeploySocket.ERR_PASSWORD);
						break;
					}
				}

				cache.cycle(cloneRandom);
				cache.cycle(random);
				
				if(isFailAuth){
					L.V = L.WShop ? false : LogManager.log("[Deploy] fail to auth.");
					errAuthCount++;
					if(errAuthCount == 3){
						throw new IOException("Error password!");
					}else{
						continue;
					}
				}
				
				socket.sendHeader(DeploySocket.H_OK, 0);
				L.V = L.WShop ? false : LogManager.log("[Deploy] succesful auth OK.");
			}else if(header == DeploySocket.H_MD5){
				final int md5Len = headerLen;
				final byte[] md5bs = socket.receiveData(md5Len, getPasswordBS());
				final String md5 = ByteUtil.buildString(md5bs, 0, md5Len, IConstant.UTF_8);
				cache.cycle(md5bs);
				
				if(socket.receive() != DeploySocket.H_TRANS_VER){
					throw new IOException("should be H_TRANS_VER");
				}
				final int transVerBSLen = socket.receiveDataLen();
				final byte[] transVerBS = socket.receiveData(transVerBSLen, null);
				
				if(ByteUtil.isSame(transVerBS, 0, transVerBSLen, DeploySocket.VERSION_1_0, 0, DeploySocket.VERSION_1_0.length)){
					cache.cycle(transVerBS);
					
					if(socket.receive() != DeploySocket.H_TRANS){
						throw new IOException("should be H_TRANS");
					}
					
					final int transBSLen = socket.receiveDataLen();
					final byte[] dataBS = socket.receiveData(transBSLen, getPasswordBS());
					
					final String realMd5 = ResourceUtil.getMD5(dataBS, 0, transBSLen);
					if(realMd5.equals(md5)){
						byte[] errorBS = null;
						
						L.V = L.WShop ? false : LogManager.log("[Deploy] succesful receive HAR data.");
						if(LinkProjectManager.deployInLocalNetwork(dataBS, transBSLen, projectID) == -1){
							errorBS = DeploySocket.ERR_IS_BUSY;
						}
						
						cache.cycle(dataBS);

						if(errorBS == null){
							socket.sendHeader(DeploySocket.H_OK, 0);
						}else{
							socket.sendError(errorBS);
						}
					}else{
						final byte[] errBS = DeploySocket.ERR_VERIFY_MD5;
						socket.sendError(errBS);
					}
				}else{
					throw new IOException("unknow trans version");
				}
			}
		}
	}
}

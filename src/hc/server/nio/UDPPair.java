package hc.server.nio;

import hc.core.L;
import hc.core.RootConfig;
import hc.core.util.LinkedSet;
import hc.core.util.LogManager;
import hc.core.util.Stack;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

public class UDPPair {
	public int port;
	public SocketAddress addr;
	public SelectionKey selectionKey;
	public DatagramChannel channel;
	public boolean isServerPort;
	public final LinkedSet writeToBackSet = new LinkedSet();
	private static final ByteBufferCacher bbCache = new ByteBufferCacher() {
		public ByteBuffer buildOne() {
			return ByteBuffer.allocateDirect(
				RootConfig.getInstance().getIntProperty(RootConfig.p_DefaultUDPSize));
		}
	};
	private final static Stack pareCache = new Stack();
	
	public static UDPPair getOneInstance(){
		if(pareCache.isEmpty()){
			return new UDPPair();
		}else{
			return (UDPPair)pareCache.pop();
		}
	}
	
	public UDPPair target;
	
	public void reset(){
		port = 0;
		addr = null;
		
		if(selectionKey != null){
			try{
				selectionKey.cancel();
			}catch (Exception e) {
				
			}
			selectionKey = null;
		}
		
		if(channel != null){
			try{
				channel.disconnect();
			}catch (Exception e) {
				L.V = L.O ? false : LogManager.log("Fail disconnect udp channel");
				e.printStackTrace();
			}
			try{
				channel.close();
			}catch (Exception e) {
				
			}
			channel = null;
		}
		
		bbCache.cycleSet(writeToBackSet);
		
		if(target != null){
			UDPPair temp = target;
			
			target.target = null;
			target = null;
			
			temp.reset();
		}
		pareCache.push(this);
	}
}

package hc.server.nio;

import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.RootConfig;
import hc.core.UDPController;
import hc.core.util.ByteUtil;
import hc.core.util.LinkedSet;
import hc.core.util.LogManager;
import hc.core.util.ThreadPriorityManager;
import hc.server.KeepaliveManager;
import hc.server.relay.RelayManager;
import hc.server.relay.SessionConnector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class AcceptReadThread extends Thread {
	public static final ByteBufferCacher bufferDirectCacher = new ByteBufferCacher() {
		@Override
		public ByteBuffer buildOne() {
			//不能合并与udpBBCache
			//因为此可能遇到大TCP包，如超过10K以上，为了获得性能而保留。
			return ByteBuffer.allocateDirect(RootConfig.getInstance().getIntProperty(RootConfig.p_RelayDirectBFSize));
		}
	};
	
	public static final ByteBufferCacher udpBBCache = new ByteBufferCacher() {
		@Override
		public ByteBuffer buildOne() {
			return ByteBuffer.allocateDirect(
				RootConfig.getInstance().getIntProperty(RootConfig.p_DefaultUDPSize));
		}
	};
	
	private final static ByteBuffer udpBB = udpBBCache.getFree();
	private final static ByteBuffer udpCtrlBB = ByteBuffer.allocate(1024);

	private final ServerSocketChannel ssc;
	public static final Selector connectSelector = buildSelector();
	private final String ip;
	private final ActionRead read;
	private final SelectionKey acceptKey;
	private final SelectionKey udpSpeedKey;

	private static Selector buildSelector(){
		try {
			return Selector.open();
		} catch (final IOException e) {
			return null;
		}
	}
	
	public AcceptReadThread(final String ip, final int localPort, final int udpSpeedPort, final ActionRead ar) throws Exception {
		super("Acceptor");
		this.read = ar;

		ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
		this.ip = ip;
		final InetSocketAddress address = new InetSocketAddress(ip, localPort);
		ssc.socket().setPerformancePreferences(5, 2, 1);
		ssc.socket().setReuseAddress(true);
		ssc.socket().bind(address);
		if(localPort == 0){
			KeepaliveManager.relayServerLocalPort = ssc.socket().getLocalPort();//Port();
		}
		L.V = L.O ? false : LogManager.log("Build Relay Server at localPort:" + localPort);

		acceptKey = ssc.register(connectSelector, SelectionKey.OP_ACCEPT);
		
		udpSpeedChannel = DatagramChannel.open();
		udpSpeedChannel.configureBlocking(false);
		udpSpeedChannel.socket().bind(new InetSocketAddress(ip, udpSpeedPort));
		udpSpeedKey = udpSpeedChannel.register(connectSelector, SelectionKey.OP_READ, udpManagerAtt);

		super.setPriority(ThreadPriorityManager.DATA_TRANS_PRIORITY);
		
		start();
	}
	
	final String udpManagerAtt = "UMA";
	final DatagramChannel udpSpeedChannel;
	
	/**
	 * 如果失败，则返回null
	 * @return
	 */
	public UDPPair buildUDPPortPair(){
		UDPPair p1 = null;
		try{
			p1 = UDPPair.getOneInstance();
			buildUDPPort(ip, p1);
		}catch (final Exception e) {
			e.printStackTrace();
			if(p1 != null){
				p1.reset();
			}
			return null;
		}
			
		UDPPair p2 = null;
		try{
			p2 = UDPPair.getOneInstance();
			buildUDPPort(ip, p2);
		}catch (final Exception e) {
			e.printStackTrace();
			if(p2 != null){
				p2.reset();
			}
			if(p1 != null){
				p1.reset();
			}
			return null;
		}
			
		p1.target = p2;
		p2.target = p1;
		
		return p1;
	}
	

	private void buildUDPPort(final String ip, final UDPPair up) throws Exception {
		final DatagramChannel channel = DatagramChannel.open();
		channel.configureBlocking(false);
		channel.socket().bind(new InetSocketAddress(ip, 0));
//		if(selector == null){
//			selector = Selector.open(); 
//		}
		up.selectionKey = channel.register(connectSelector, SelectionKey.OP_READ, up);
		up.channel = channel;
		up.port = channel.socket().getLocalPort();
	}

	public static void handleWrite(final SelectionKey key, final UDPPair up) throws IOException {
		final DatagramChannel channel = (DatagramChannel) key.channel();
		final LinkedSet writeToBackSet = up.writeToBackSet;
		final SocketAddress writeAddr = up.addr;

		ByteBuffer byteBuf = null;
		try{
			while(true){
				byteBuf = (ByteBuffer)writeToBackSet.getFirst();
				if(byteBuf == null){
					key.interestOps(SelectionKey.OP_READ);
					return;
				}
				
				L.V = L.O ? false : LogManager.log("UDP handleWrite");
				channel.send(byteBuf, writeAddr);
				
				if(byteBuf.hasRemaining()){
					writeToBackSet.addToFirst(byteBuf);
					return;
				}else{
					byteBuf.clear();
					udpBBCache.cycle(byteBuf);
				}
			}
		}catch (final IOException e) {
			//发生异常时，clear并回收以防泄漏。当进行interestOps时，可能出现为null情形
			if(byteBuf != null){
				byteBuf.clear();
				udpBBCache.cycle(byteBuf);
			}
			throw e;
		}
	}

	
	public boolean isOpen(){
		return ssc.isOpen();
	}
	
	public void close(){
		try{
			ssc.close();
		}catch (final Exception e) {
			e.printStackTrace();
			L.V = L.O ? false : LogManager.log("close:" + e.getMessage());
		}
	}

	
	@Override
	public void run() {
		try{
			while (true)
			{	
				if(connectSelector.select() == 0){
					continue;
				}
				
				try {
				final Iterator<SelectionKey> it = connectSelector.selectedKeys().iterator();

				while (it.hasNext()) {
					final SelectionKey key = it.next();
					it.remove();
					
					final int op = key.readyOps();
					
					final Object attach = key.attachment();
					if(attach instanceof UDPPair){
						if (op == SelectionKey.OP_READ) {
							final UDPPair up = (UDPPair)attach;

							try{
								final SocketAddress sa = ((DatagramChannel)key.channel()).receive(udpBB);
								if(sa == null){
									//没有数据，返回
									continue;
								}
								if(up.addr == null){
									//收到第一个包，表明通道建立。以标识或检查非法数据包
									L.V = L.O ? false : LogManager.log("UDP incoming / rebuild at port : " + ((DatagramChannel)key.channel()).socket().getLocalPort());
									up.addr = sa;
//								}else if(writeToAddr == null){
//									//接收方无的情形，或者接收方没建立通道，不能进行下一下的数据包转发，
//									//关闭，因为如果真为null，也会自动抛出java.lang.NullPointerException，与人为检查效果相同
//									bb.clear();
//									throw targetAddNullException;
								}else{
									udpBB.flip();
									up.target.channel.send(udpBB, up.target.addr);
								}
							}catch (final Exception e) {
								//复用对象，不回收，遇异常进行clear
								
								//此处不能回收
//								udpBBCache.cycle(udpBB);
//								throw e;
							}
							udpBB.clear();
							continue;
						}else if ((op & SelectionKey.OP_WRITE) != 0) {
							handleWrite(key, (UDPPair)attach);
						}
						continue;
					}else if(attach == udpManagerAtt){
						try{
							final SocketAddress sa = udpSpeedChannel.receive(udpCtrlBB);
							if(sa == null){
								//没有数据，
								udpCtrlBB.clear();
								continue;
							}
							
							udpCtrlBB.flip();
							final byte[] bs = udpCtrlBB.array();
							final int tag = ByteUtil.twoBytesToInteger(bs, 0);
							if(tag == MsgBuilder.E_UDP_CONTROLLER_TEST_SPEED){
								//回应udp测速
								udpSpeedChannel.send(udpCtrlBB, sa);
							}else if(tag == MsgBuilder.E_UDP_CONTROLLER_SET_ADDR_NULL){
								final int bufferDatalen = udpCtrlBB.remaining();
								final int startIdxUUID = UDPController.UUID_STARD_IDX;
								
								final boolean isServer = (bs[MsgBuilder.LEN_UDP_CONTROLLER_HEAD] == 1);
								final boolean result = SessionConnector.resetXXSideUDPAddressNull(bs, 
										startIdxUUID, bufferDatalen - startIdxUUID, isServer,
										bs[UDPController.UDP_RANDOM_HEADER_STARD_IDX], bs[UDPController.UDP_RANDOM_HEADER_STARD_IDX + 1]);
								
								//回应成功setNullAddr
								udpCtrlBB.limit(1);
								bs[0] = (result
											?MsgBuilder.DATA_UDP_CONTROLLER_SET_ADDR_NULL_SUCC
											:MsgBuilder.DATA_UDP_CONTROLLER_SET_ADDR_NULL_NOT_FOUND);
								udpSpeedChannel.send(udpCtrlBB, sa);
								
								L.V = L.O ? false : LogManager.log("UDP setAddrNull for uuid[" + new String(bs, startIdxUUID, bufferDatalen - startIdxUUID) + "], from " + sa.toString());
							}
						}catch (final Throwable e) {
							e.printStackTrace();
						}
						udpCtrlBB.clear();
						continue;
					}
					
					if(key.isValid() == false){
//						L.V = L.O ? false : LogManager.log("Invalid SelectionKey, skip and continue.");
						continue;
					}
					
					if ((op & SelectionKey.OP_READ) != 0) {
						read.action(key);
					}
					if ((op & SelectionKey.OP_WRITE) != 0) {
						final SessionConnector sc = (SessionConnector)key.attachment();
						final SocketChannel currChannel = (SocketChannel)key.channel();
						
						while(true){
							final ByteBuffer writeBB = sc.getWriteSet(currChannel);
							if(writeBB == null){
//								L.V = L.O ? false : LogManager.log("[RelayCache] Close OP_WRITE");
								
								//关闭OP_WRITE
								final SelectionKey currChannelkey = currChannel.keyFor(connectSelector);
								currChannelkey.interestOps(SelectionKey.OP_READ);
							}else{
								//准备输出数据
								try{
									currChannel.write(writeBB);
									if(writeBB.hasRemaining()){
//										L.V = L.O ? false : LogManager.log("[RelayCache] Write , but has remaining");
										sc.setRewriteSet(currChannel, writeBB);
									}else{
//										L.V = L.O ? false : LogManager.log("[RelayCache] Write , clear , try next write");
										writeBB.clear();
										bufferDirectCacher.cycle(writeBB);
										
										continue;
										//继续，所以不用break;
									}
								}catch (final Exception e) {
									e.printStackTrace();
									writeBB.clear();
									bufferDirectCacher.cycle(writeBB);
									
									RelayManager.closePare(sc, true);
									try{
										//特殊情形下，调用本行是有益的。
										key.cancel();
									}catch (final Exception e1) {
									}
									
								}
							}
							break;
						}
					}
					if ((op & SelectionKey.OP_ACCEPT) != 0) {
						acceptSession(key);
					}
				}
				} catch (final Exception ex) {
					//有可能被Cancle的到此
//					ContextManager.getContextInstance().shutDown();
				}
			}
		}catch (final Exception e) {
			L.V = L.O ? false : LogManager.log("Exception : connectSelector.select()");
//			e.printStackTrace();
		}
	}
	
	private void acceptSession(final SelectionKey key){
		try{
			final ServerSocketChannel readyChannel = (ServerSocketChannel) key.channel();
			final SocketChannel incomingChannel = readyChannel.accept();
			
			if(read.isTop(incomingChannel)){
				try {
					incomingChannel.socket().close();
				} catch (final IOException e) {
				}
				
				try{
					incomingChannel.close();
				}catch (final Exception e) {
					
				}
				L.V = L.O ? false : LogManager.log("Max channel , close connection!");
			}else{
	//			incomingChannel.socket().setSoTimeout(0);
	
				incomingChannel.configureBlocking(false);
				
	//			 Socket 类用 4 个整数表示服务类型.
	//			 低成本: 0x02 (二进制的倒数第二位为1)
	//			 高可靠性: 0x04 (二进制的倒数第三位为1)
	//			 最高吞吐量: 0x08 (二进制的倒数第四位为1)
	//			 最小延迟: 0x10 (二进制的倒数第五位为1)
				final int tc = RootConfig.getInstance().getIntProperty(RootConfig.p_TrafficClass);
				if(tc != 0){
					try{
						incomingChannel.socket().setTrafficClass(tc);
					}catch (final Exception e) {
						//部分虚拟机可能不支持该参数
					}
				}
	//			例如, 如果参数 connectionTime 为 2, 参数 latency 为 1, 而参数bandwidth 为 3, 就表示最高带宽最重要, 其次是最少连接时间, 最后是最小延迟.
	//			参数 connectionTime: 表示用最少时间建立连接.
	//			参数 latency: 表示最小延迟.
	//			参数 bandwidth: 表示最高带宽.
				
				incomingChannel.socket().setPerformancePreferences(5, 2, 1);
				
								//.设置发送逗留时间 socket.setSoLinger(true, 2); 
				//这个参数是socket发送数据时的超时，如果对方在固定时间内不接受，则关闭socket。
				//与socket.setSoTimeout(2000)不同，这个是设置InputStream上调用 read()阻塞超时时间。
				
				incomingChannel.socket().setSoLinger(true, 3);
				
				L.V = L.O ? false : LogManager.log("Accept new SocketChannel socket:" + incomingChannel.socket().hashCode() + ", remotePort:" + incomingChannel.socket().getPort());
				
				//KeepAlive_Tag
				incomingChannel.socket().setKeepAlive(true);
				incomingChannel.socket().setTcpNoDelay(true);
				final int sendBuff = RootConfig.getInstance().getIntProperty(RootConfig.p_ServerSendBufferSize);
				if(sendBuff != 0){
					incomingChannel.socket().setSendBufferSize(sendBuff);
				}
				final int receiveBuff = RootConfig.getInstance().getIntProperty(RootConfig.p_ServerReceiveBufferSize);
				if(receiveBuff != 0){
					incomingChannel.socket().setReceiveBufferSize(receiveBuff);
				}
				
				//如将本方法置于AcceptThread中，会导致下行代码假死，估计是没有read数据
				incomingChannel.register(connectSelector, SelectionKey.OP_READ);
	
			}
		}catch (final Exception e) {
			e.printStackTrace();
		}
	}
	
//	private boolean isShutdown = false;
	
	public void shutDown(){
//		isShutdown = true;
		try {
			acceptKey.cancel();
		} catch (final Exception e) {
		}
		try {
			udpSpeedKey.cancel();
		} catch (final Exception e) {
		}
		try{
		connectSelector.close();
		}catch (final Exception e) {
			
		}
		try {
			ssc.close();
		} catch (final IOException e) {
		}
		try{
			udpSpeedChannel.close();
		}catch (final Exception e) {
		}
	}

}
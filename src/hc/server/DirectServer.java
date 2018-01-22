package hc.server;

import hc.App;
import hc.core.ContextManager;
import hc.core.HCTimer;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.data.DataReg;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.ThreadPriorityManager;
import hc.server.ui.J2SESessionManager;
import hc.server.ui.design.J2SESession;
import hc.server.util.StarterParameter;
import hc.util.HttpUtil;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class DirectServer extends Thread {
	ServerSocket server = null;
	InetAddress ia;
	final String networkAddressName;
	
	private final InetAddress buildDemo(final String ip){
		try{
			return InetAddress.getByName(ip);
		}catch (final Exception e) {
		}
		return null;
	}
	
	public DirectServer(final InetAddress ia, final String naName) {
		super("DirectServer");
		setPriority(ThreadPriorityManager.DIRECT_SERVER_RECEIVE_PRIORITY);
		if(ResourceUtil.isNonUIServer()){
			final String demoIP = PropertiesManager.getValue(PropertiesManager.p_NonUIServerIP);//由于searchReachable，所以几乎不会使用p_DemoServerIP
			if(demoIP != null){
				this.ia = buildDemo(demoIP);
			}
		}
		
		if(this.ia == null){
			this.ia = ia;
		}
		
		this.networkAddressName = naName;
	}
	
	public void buildServer(){
		try{
			synchronized (LOCK) {
				server = new ServerSocket();
				
//				不能重用端口，因为有可能会与其它系统产生共用该端口
//				server.setReuseAddress(true);
				
				final String defaultPort = DefaultManager.getDirectServerPort();
				int directServerPort = DefaultManager.DEFAULT_DIRECT_SERVER_PORT;
				try{
					directServerPort = Integer.parseInt(defaultPort);
				}catch (final Throwable e) {
					LogManager.errToLog("[direct server] invalid port for direct server, use zero as port.");
				}
				final int backlog = 2;
				boolean isAutoSelectPort = false;
				try{
					server.bind(new InetSocketAddress(ia, directServerPort), backlog);
				}catch (final Throwable e) {
					isAutoSelectPort = true;
					server.bind(new InetSocketAddress(ia, 0), backlog);
				}
				//供家庭环境内无线网，快捷访问
				StarterParameter.setHomeWirelessIPPort(server.getInetAddress().getHostAddress(), server.getLocalPort());
				
				if(isAutoSelectPort){
					final JPanel panel = new JPanel(new BorderLayout());
					final String msg = "[direct server] port:" + directServerPort + " is used, select port:" + server.getLocalPort();
					panel.add(new JLabel(msg, App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEADING), BorderLayout.CENTER);
					App.showCenterPanelMain(panel, 0, 0, (String)ResourceUtil.get(IContext.ERROR), false, 
							(JButton)null, (String)null, (ActionListener)null, (ActionListener)null, (JFrame)null, 
							false, true, null, false, false);
					LogManager.err(msg);
				}
				
				LogManager.log("Build direct server at " + server.getLocalSocketAddress().toString()); 
				LogManager.log("  on network interface [" + networkAddressName + "]");
				LogManager.log("  for home wirless network");

				LOCK.notify();
			}
		}catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
		
	}

	public void closeSessionNotServerSocket() {
		if(server != null){
			final ServerSocket serverSnap = server;
			server = null;
			try{
				serverSnap.close();
				LogManager.log("[direct server] successful close old Home Wireless Server");
			}catch (final Throwable e) {
				LogManager.log("[direct server] Error close home wireless server : " + e.toString());
			}
			
			while(true){
				try{
					if(serverSnap.isClosed()){
						break;
					}
					Thread.sleep(IConstant.THREAD_WAIT_INNER_MS);
					if(L.isInWorkshop){
						LogManager.log("[direct server] waiting direct server close...");
					}
				}catch (final Throwable e) {
					LogManager.log("[direct server] Error check isClosed : " + e.toString());
				}
			}
		}
	}
	
	final private Object LOCK = new Object();
	final boolean isDemoServer = ResourceUtil.isDemoServer();
	
	@Override
	public void run(){
		while(!isShutdown){
			if(server == null){
				synchronized (LOCK) {
					if(isShutdown){
						break;
					}
					if(server == null){
						try{
							LOCK.wait();
						}catch (final Exception e) {
						}
					}
					continue;
				}
			}
			
			Socket socket = null;
			try{
				LogManager.log("[direct server] ready for new client.");
				socket = server.accept();
				
				processOneClient(socket, J2SESessionManager.lockIdelSession());//不用threadPool，以减少时间
			}catch (final Throwable e) {
				LogManager.log("[direct server] Exception : " + e.toString());
				if(isDemoServer){
					ExceptionReporter.printStackTrace(e);//必须，在OpenJDK DemoServer
				}
				
				final Socket snapSocket = socket;
				if(snapSocket != null){
					try{
						snapSocket.close();
					}catch (final Exception e1) {
					}
				}
				//此处不做server的close逻辑
//					if(SIPManager.getSIPContext().isNearDeployTime()){
//						continue;
//					}
//					
//					final ServerSocket snapServerSocket = server;
//					server = null;
//					try{
//						snapServerSocket.close();
//					}catch (Exception e1) {
//						
//					}
			}
		}//end while
		LogManager.log("[direct server] shutdown old Home Wireless Server");		
	}

	private final void processOneClient(final Socket socket, J2SESession coreSSMaybeNull) {
		try{
//					LogManager.log("Server is build conn for other, cancle the coming");
//					LogManager.log("  Coming, " + socket.getInetAddress().getHostAddress() + 
//							":" + socket.getPort());
//					try{
//						socket.close();
//					}catch (final Exception e) {
//					}
			LogManager.log("[direct server] client line on:[" + 
					socket.getInetAddress().getHostAddress() + 
					":" + socket.getPort() + "]");
			
			try{
				socket.setKeepAlive(true);//由于某此环境下，节能可能关闭应用，而导致连接空挂。
			}catch (final Throwable ex) {
				ex.printStackTrace();
			}
			try{
				socket.setTcpNoDelay(false);//sendStringJSOrCache会产生大量小块数据，故开启Delay
			}catch (final Throwable ex) {
				ex.printStackTrace();
			}
			try{
				socket.setSoLinger(true, 3);
			}catch (final Throwable ex) {
				ex.printStackTrace();
			}
			
			HttpUtil.initReceiveSendBufferForSocket(socket);

			final int maxTryCount = 5;
			final HCTimer watcher = new HCTimer("", maxTryCount * 1000, true) {
				@Override
				public final void doBiz() {
					synchronized (this) {
						if(isEnable()){
							try {
								socket.close();
							} catch (final Throwable e) {
							}
						}
					}
				}
			};
				
			try{
				//echo back reg tag
				final int BYTE_LEN = DataReg.LEN_DATA_REG + MsgBuilder.MIN_LEN_MSG;

				final byte[] bs = new byte[BYTE_LEN];//DatagramPacketCacher.getInstance().getFree();
				new DataInputStream(socket.getInputStream()).readFully(bs, 0, BYTE_LEN);
				socket.getOutputStream().write(bs, 0, BYTE_LEN);
				socket.getOutputStream().flush();
				
				int tryCount = 0;
				while(coreSSMaybeNull == null && tryCount < 40){
					coreSSMaybeNull = J2SESessionManager.lockIdelSession();
					if(coreSSMaybeNull == null){
						tryCount++;
						try{
							Thread.sleep(100);//有可能多个同时访问directServer
						}catch (final Exception e) {
						}
					}
				}
				if(coreSSMaybeNull == null){
					LogManager.errToLog("no idle session for new client.");
					return;
				}

				try{
					coreSSMaybeNull.context.sendWithoutLockForKeepAliveOnly(null, MsgBuilder.E_TAG_ROOT, MsgBuilder.DATA_ROOT_DIRECT_CONN_OK);
				}catch (final Throwable e) {
				}
				
				coreSSMaybeNull.deploySocket(socket);
//				try{
//					Thread.sleep(ThreadPriorityManager.NET_FLUSH_DELAY);//override same token，所以关闭
//				}catch (final Exception e) {
//				}
				
				setServerConfigPara(coreSSMaybeNull, true, false);
				coreSSMaybeNull.context.setConnectionModeStatus(ContextManager.MODE_CONNECTION_HOME_WIRELESS);
				
				final byte subTag = bs[MsgBuilder.INDEX_CTRL_SUB_TAG];
				if(subTag == MsgBuilder.DATA_E_TAG_RELAY_REG_SUB_FIRST){
					coreSSMaybeNull.context.setStatus(ContextManager.STATUS_READY_MTU);
				}else if(subTag == MsgBuilder.DATA_E_TAG_RELAY_REG_SUB_BUILD_NEW_CONN){
					//注意：这是DATA_E_TAG_RELAY_REG_SUB_BUILD_NEW_CONN与DATA_E_TAG_RELAY_REG_SUB_FIRST的唯一区别
					return;
				}
				
				//以下不应有代码
			}catch (final Throwable e) {
				throw e;
			}finally{
				synchronized (watcher) {
					watcher.setEnable(false);
					HCTimer.remove(watcher);
				}
			}
//				LogManager.info("Succ connect target");
		}catch (final Throwable e) {
			LogManager.log("[direct server] Exception : " + e.toString());
			ExceptionReporter.printStackTrace(e);//必须 在OpenJDK DemoServer
		}
	}

	public static void setServerConfigPara(final J2SESession coreSS, final boolean keepalive, final boolean isRelay) {
		//家庭直联模式下，开启keepalive，不关闭KeepAlive
		coreSS.keepaliveManager.keepalive.setEnable(keepalive);
		coreSS.keepaliveManager.keepalive.resetTimerCount();
		
		coreSS.setOnRelay(isRelay);
	}
	
	private boolean isShutdown = false;
	
	public void shutdown(){
		isShutdown = true;
		
		closeSessionNotServerSocket();

		synchronized (LOCK) {
			LOCK.notify();
		}
		
		StarterParameter.dServer = null;
	}
}

package hc.server;

import hc.App;
import hc.core.ContextManager;
import hc.core.HCTimer;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.data.DataReg;
import hc.core.sip.SIPManager;
import hc.core.util.LogManager;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.IOException;
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
	final InetAddress ia;
	final String networkAddressName;
	
	public DirectServer(final InetAddress ia, final String naName) {
		super("DirectServer");
		this.ia = ia;
		this.networkAddressName = naName;
	}
	
	public void buildServer(){
		try{
			synchronized (LOCK) {
				server = new ServerSocket();
				
//				不能重用端口，因为有可能会与其它系统产生共用该端口
//				server.setReuseAddress(true);
				
				final int directServerPort = Integer.parseInt(
						PropertiesManager.getValue(PropertiesManager.p_selectedNetworkPort, "0"));
				final int backlog = 2;
				boolean isAutoSelect = false;
				try{
					server.bind(new InetSocketAddress(ia, directServerPort), backlog);
				}catch (final Throwable e) {
					isAutoSelect = true;
					server.bind(new InetSocketAddress(ia, 0), backlog);
				}
				//供家庭环境内无线网，快捷访问
				KeepaliveManager.homeWirelessIpPort.ip = server.getInetAddress().getHostAddress();
				KeepaliveManager.homeWirelessIpPort.port = server.getLocalPort();
				
				if(isAutoSelect){
					final JPanel panel = new JPanel(new BorderLayout());
					final String msg = "[Direct Home Server] port:" + directServerPort + " is used, select port:" + server.getLocalPort();
					panel.add(new JLabel(msg, App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEADING), BorderLayout.CENTER);
					App.showCenterPanel(panel, 0, 0, (String)ResourceUtil.get(IContext.ERROR), false, 
							(JButton)null, (String)null, (ActionListener)null, (ActionListener)null, (JFrame)null, 
							false, true, null, false, false);
					LogManager.err(msg);
				}
				
				L.V = L.O ? false : LogManager.log("Build direct server at " + server.getLocalSocketAddress().toString()); 
				L.V = L.O ? false : LogManager.log("  on network interface [" + networkAddressName + "]");
				L.V = L.O ? false : LogManager.log("  for home wirless network");

				LOCK.notify();
			}
		}catch (final Exception e) {
			e.printStackTrace();
		}
		
	}

	public void closeSessionNotServerSocket() {
		if(socket != null){
			final Socket snapSocket = socket;
			socket = null;
			try{
				snapSocket.close();
				L.V = L.O ? false : LogManager.log("Close client Session");
			}catch (final Throwable e) {
				e.printStackTrace();
			}
		}
		
		if(server != null){
			final ServerSocket serverSnap = server;
			server = null;
			try{
				serverSnap.close();
				L.V = L.O ? false : LogManager.log("successful close old Home Wireless Server");
			}catch (final Throwable e) {
				L.V = L.O ? false : LogManager.log("Error close home wireless server : " + e.toString());
			}
			
			while(true){
				try{
					if(serverSnap.isClosed()){
						break;
					}
					Thread.sleep(IConstant.THREAD_WAIT_INNER_MS);
				}catch (final Throwable e) {
					L.V = L.O ? false : LogManager.log("Error check isClosed : " + e.toString());
				}
			}
		}
	}
	
	final private Object LOCK = new Object();
	private Socket socket;
	
	@Override
	public void run(){
		while(!isShutdown){
			if(server == null){
				synchronized (LOCK) {
					if(server == null){
						try{
							LOCK.wait();
						}catch (final Exception e) {
						}
					}
					continue;
				}
			}
			
			try{
				final Socket temp = server.accept();
				if(socket != null){
					L.V = L.O ? false : LogManager.log("Server is build conn for other, cancle the coming");
					L.V = L.O ? false : LogManager.log("  Coming, " + temp.getInetAddress().getHostAddress() + 
							":" + temp.getPort());
					try{
						temp.close();
					}catch (final Exception e) {
						
					}
				}else{
					socket = temp;
					
					L.V = L.O ? false : LogManager.log("client line on:[" + 
							socket.getInetAddress().getHostAddress() + 
							":" + socket.getPort() + "]");
					
					socket.setKeepAlive(true);
					socket.setTcpNoDelay(true);
					
//					try{
//						final int ServerSndBF = RootConfig.getInstance().getIntProperty(RootConfig.p_ClientServerSendBufferSize);
//						socket.setSendBufferSize(ServerSndBF);
//						
//						socket.setReceiveBufferSize(1024 * 25);
//					}catch (Exception e) {
//						
//					}
					final HCTimer watcher = new HCTimer("", 3000, true) {
						@Override
						public final void doBiz() {
							try {
								temp.close();
							} catch (final IOException e) {
							}
						}
					};
					//echo back reg tag
					final int BYTE_LEN = DataReg.LEN_DATA_REG + MsgBuilder.MIN_LEN_MSG;

					final byte[] bs = new byte[BYTE_LEN];//DatagramPacketCacher.getInstance().getFree();
					new DataInputStream(socket.getInputStream()).readFully(bs, 0, BYTE_LEN);
//					if(len != BYTE_LEN){
//						L.V = L.O ? false : LogManager.log("Unknow Reg");
//						throw new Exception();
//					}
					socket.getOutputStream().write(bs, 0, BYTE_LEN);
					socket.getOutputStream().flush();
//					DatagramPacketCacher.getInstance().cycle(bs);
					
					SIPManager.getSIPContext().deploySocket(socket);
					
					//家庭直联模式下，关闭KeepAlive
					setServerConfigPara(false, false);
					ContextManager.setConnectionModeStatus(ContextManager.MODE_CONNECTION_HOME_WIRELESS);
					
					ContextManager.setStatus(ContextManager.STATUS_READY_MTU);
					
					HCTimer.remove(watcher);
//					LogManager.info("Succ connect target");
				}
			}catch (final Exception e) {
				//L.V = L.O ? false : LogManager.log("DirectServer Exception : " + e.toString());
//				e.printStackTrace();

				final Socket snapSocket = socket;
				socket = null;
				try{
					snapSocket.close();
				}catch (final Exception e1) {
					
				}
				
				//此处不做server的close逻辑
//				if(SIPManager.getSIPContext().isNearDeployTime()){
//					continue;
//				}
//				
//				final ServerSocket snapServerSocket = server;
//				server = null;
//				try{
//					snapServerSocket.close();
//				}catch (Exception e1) {
//					
//				}
			}
		}//end while
		L.V = L.O ? false : LogManager.log("shutdown old Home Wireless Server");		
	}

	public static void setServerConfigPara(final boolean keepalive, final boolean isRelay) {
		//家庭直联模式下，关闭KeepAlive
		KeepaliveManager.keepalive.setEnable(keepalive);
		KeepaliveManager.keepalive.resetTimerCount();
		
		SIPManager.setOnRelay(isRelay);
	}
	
	private boolean isShutdown = false;
	
	public void shutdown(){
		isShutdown = true;
		
		closeSessionNotServerSocket();

		synchronized (LOCK) {
			LOCK.notify();
		}
		
		KeepaliveManager.dServer = null;
	}
}

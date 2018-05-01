package hc.server.ui.design;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import hc.core.L;
import hc.core.util.LogManager;
import hc.server.localnet.DeploySocket;

public class LocalnetDeviceBinderWizSource implements DeviceBinderWizSource {
	final DevTree devTree;
	final ConverterTree converterTree;
	final BDNTree bdnTree;
	final DeploySocket socket;
	
	public LocalnetDeviceBinderWizSource(final DevTree devTree, final ConverterTree converterTree, final BDNTree bdnTree,
			final DeploySocket socket) {
		this.devTree = devTree;
		this.converterTree = converterTree;
		this.bdnTree = bdnTree;
		this.socket = socket;
	}
	
	@Override
	public DevTree buildDevTree() throws Exception {
		return devTree;
	}

	@Override
	public ConverterTree buildConverterTree() throws Exception {
		return converterTree;
	}
	
	@Override
	public BDNTree buildTree() throws Exception {
		return bdnTree;
	}

	@Override
	public void save() {
		try {
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final ObjectOutputStream oos = new ObjectOutputStream(baos);
			
			oos.writeObject(bdnTree);
			oos.flush();
			
			final byte[] objsBS = baos.toByteArray();
			socket.sendHeader(DeploySocket.H_SAVE_BIND_OBJS, objsBS.length);
			socket.sendData(objsBS, 0, objsBS.length, false, false, null);
			
			oos.close();
			baos.close();
		}catch (final Exception e) {
			e.printStackTrace();
		}
		L.V = L.WShop ? false : LogManager.log("[Deploy] successful send H_SAVE_BIND_OBJS");
	}

	@Override
	public void cancel() {
		try {
			socket.sendHeader(DeploySocket.H_SAVE_BIND_OBJS, 0);
		}catch (final Exception e) {
		}
		L.V = L.WShop ? false : LogManager.log("[Deploy] successful cancel H_SAVE_BIND_OBJS");
	}

}

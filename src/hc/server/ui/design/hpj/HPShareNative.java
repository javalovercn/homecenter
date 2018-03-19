package hc.server.ui.design.hpj;

import hc.server.ui.design.NativeOSManager;

import java.io.File;

public class HPShareNative extends HCShareFileResource {
	public int osMask;

	public HPShareNative(final int type, final String name, final int osMask) {
		super(type, name);
		this.osMask = osMask;
	}

	public HPShareNative(final int type, final String name, final File file) throws Throwable {
		super(type, name, file);
		osMask = NativeOSManager.OS_UNKNOWN;
	}

	@Override
	public String toString() {
		return name;// 由于要比较同名，所以不加ver
	}

	@Override
	public String getNodeDisplayString() {
		return name + ", version:" + ver;
	}

	@Override
	public String validate() {
		if (osMask == NativeOSManager.OS_UNKNOWN) {
			return "choose at least one operation system for native lib [<strong>" + name + "</strong>] !";
		}

		return null;
	}
}

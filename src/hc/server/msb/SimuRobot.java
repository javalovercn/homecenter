package hc.server.msb;

import hc.core.util.LogManager;

public class SimuRobot extends Robot {
	
	@Override
	public void startup() {
	}
	
	@Override
	public void shutdown() {
	}
	
	@Override
	public void response(final Message msg) {
	}
	
	@Override
	public Object operate(final long functionID, final Object parameter) {
		LogManager.err("In designer panel, create simu result object (empty string) for method [operate] of simu Robot.");
		return "";
	}
	
	@Override
	public DeviceCompatibleDescription getDeviceCompatibleDescription(
			final String referenceDeviceID) {
		return null;
	}
	
	@Override
	public String[] declareReferenceDeviceID() {
		return null;
	}

}

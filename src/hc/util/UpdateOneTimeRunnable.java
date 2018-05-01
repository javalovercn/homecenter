package hc.util;

import hc.core.HCConnection;
import hc.core.MsgBuilder;
import hc.core.util.CCoreUtil;
import hc.server.ui.J2SESessionManager;
import hc.server.ui.design.J2SESession;
import hc.server.util.ServerCUtil;

public class UpdateOneTimeRunnable implements Runnable {
	final J2SESession j2seCoreSS;
	final HCConnection hcConnection;

	public UpdateOneTimeRunnable(final J2SESession ss, final HCConnection hcConnection) {
		this.j2seCoreSS = ss;
		this.hcConnection = hcConnection;
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(1000 * 60 * hcConnection.getUpdateMinMinutes());
			} catch (final Exception e) {
			}
			if (hcConnection.isStopRunning) {
				break;
			}

			// if(IOSBackgroundManager.isIOSForBackgroundCond()){
			// if(.ServerUIAPIAgent.getMobileAgent(.isBackground()){
			// LogManager.log("skip trans one time for iOS in background
			// mode.");
			// continue;
			// }
			// }

			CCoreUtil.generateRandomKey(ResourceUtil.getStartMS(), hcConnection.oneTime, 0, CCoreUtil.CERT_KEY_LEN);
			// LogManager.log("OneTime:" +
			// CUtil.toHexString(CUtil.OneTimeCertKey));

			// LogManager.log("transport new one time certification key to
			// client");
			synchronized (j2seCoreSS.context.sendLock) {
				hcConnection.isReceivedOneTimeInSecuChannalFromMobile = false;

				ServerCUtil.transCertKey(j2seCoreSS, hcConnection, hcConnection.oneTime,
						MsgBuilder.E_TRANS_ONE_TIME_CERT_KEY_IN_SECU_CHANNEL, true);

				hcConnection.waitOneTimeReceiveNotifyLock();

				if (hcConnection.isReceivedOneTimeInSecuChannalFromMobile == false) {
					hcConnection.isStopRunning = true;
				}

			} // end synchronized

			if (hcConnection.isStopRunning) {
				break;
			}
		} // end while(true)

		if (hcConnection.isReceivedOneTimeInSecuChannalFromMobile == false) {
			// LogManager.log("timeout for
			// ReceivedOneTimeInSecuChannalFromMobile");
			J2SESessionManager.stopSession(j2seCoreSS, true, false);
		}

		// System.out.println("--------done
		// UpdateOneTimeRunnable--------------");
	}
}

package hc.core;

public interface FastSender {
	public void sendWrapAction(final byte ctrlTag, final byte[] jcip_bs,
			final int offset, final int len);
}

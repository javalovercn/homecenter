package hc.core;

public interface NestAction {
	public static final int POINTER_EVENT = 1;
	public static final int HCTIMER = 2;

	public void action(final byte ctrlTag, byte[] event);

	public void action(final int actionID, final Object obj);
}

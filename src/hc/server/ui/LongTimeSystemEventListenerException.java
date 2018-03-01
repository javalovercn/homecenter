package hc.server.ui;

/**
 * if system event listener is processing too long time to finish, for example,
 * add session level mobile menu in listener, the user may be wait too long for
 * mobile menu, then this exception is raised.
 *
 */
public class LongTimeSystemEventListenerException extends Exception {
	public LongTimeSystemEventListenerException(final String desc) {
		super(desc);
	}
}

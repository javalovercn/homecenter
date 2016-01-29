package hc.core.util.io;

import java.io.IOException;

public interface IHCStream {
	public void notifyExceptionAndCycle(final IOException exp);
	
	public void notifyClose();
}

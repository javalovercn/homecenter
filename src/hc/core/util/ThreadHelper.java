package hc.core.util;


public class ThreadHelper {
	final static VectorMap table = new VectorMap(16);
	
//	public static final void setImageNoCopy(final boolean isNoCopy){
//		final Thread currentThread = Thread.currentThread();
//		ThreadBinder binder = (ThreadBinder)table.get(currentThread, null);
//		if(binder == null){
//			binder = new ThreadBinder();
//			table.set(currentThread, binder);
//		}
//		binder.isImageNoCopy = isNoCopy;
//	}
	
//	public static final boolean isImageNoCopy(){
//		final Thread currentThread = Thread.currentThread();
//		ThreadBinder binder = (ThreadBinder)table.get(currentThread, null);
//		if(binder == null){
//			return ThreadBinder.DEFAULT_IMAGE_NO_COPY;
//		}else{
//			return binder.isImageNoCopy;
//		}
//	}
}

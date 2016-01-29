package hc.server.msb;

import hc.server.ui.ProjectContext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

/**
 * {@link Message} is medium between {@link Robot} and {@link Device}. 
 * <br><br>
 * {@link Message} is never used outside of {@link Robot}, {@link Converter} and {@link Device}. 
 * To drive {@link Device}, please via {@link Robot}. 
 * To get {@link Robot} instance and operate it in HAR project, please invoke {@link ProjectContext#getRobot(String)} and {@link Robot#operate(long, Object)}.
 * <br><br>There are two parts of a {@link Message}, <i>header</i> is for control data and <i>body</i> is for business data.
 * <BR><BR>To get an instance of {@link Message}, invoke {@link Robot#getFreeMessage(String)} or {@link Device#getFreeMessage(String)}.
 * <br><br><STRONG>Important</STRONG> : <BR>Don't keep any reference of {@link Message} in any threads and instances, it will be auto recycled and cleaned after being consumed.
 */
public final class Message {
	
	/**
	 * @deprecated
	 */
	@Deprecated
	protected Message(){
	}
	
	private final HashMap<String, Object> header_table = new HashMap<String, Object>();
	private final HashMap<String, Object> body_table = new HashMap<String, Object>();
	
	int ctrl_level;
	int ctrl_sync_id;
	boolean ctrl_isInWorkbench = false;
	boolean ctrl_isInitiative = true;
	long ctrl_dispatch_thread_id = 0;
	boolean ctrl_is_downward = true;
	long ctrl_cycle_get_thread_id = 0;
	
	String ctrl_bind_id;
	Vector<String> ctrl_bind_ids;
	
	/**
	 * ID of the device in IoT(Internet of Things) network to indicate from other same model devices. If unknown, please keep empty string.
	 */
	String ctrl_dev_id;
	
	/**
	 * 
	 * @return In {@link Robot}, it means <i>Reference Device ID</i>; In {@link Device}, it means real device ID.
	 */
	public final String getDevID(){
		return ctrl_dev_id;
	}
	
	final void tryRecycle(final Workbench workbench, final boolean isShutdown){
//		Workbench.V = Workbench.O ? false : Workbench.log("try recycle message [" + this.toString() + "]");
		if(isShutdown){
			if(ctrl_sync_id != 0){
				final WaitingForMessage oldwm = workbench.waiting.remove(ctrl_sync_id);
				if(oldwm != null){
					workbench.V = workbench.O ? false : workbench.log("force stop waitFor task on thread :" + oldwm.dispatch_thread_id);
					oldwm.wakeUp();
				}
			}
		}
		Workbench.messagePool.recycle(this, workbench);
	}

	final boolean checkWaitFor(final Workbench workbench) {
		if(ctrl_sync_id != 0){
			final WaitingForMessage wm = workbench.waiting.remove(ctrl_sync_id);
			if(wm != null){
				if(wm.dispatch_thread_id != ctrl_dispatch_thread_id){//由于可能存在异常，增加header_dispatch_thread_id比对
//					workbench.V = workbench.O ? false : workbench.log("WaitingForMessage result (=header_dispatch_thread_id)");
					wm.result = this;
				}else{
					workbench.V = workbench.O ? false : workbench.log("WaitingForMessage result (!=header_dispatch_thread_id)");
				}
				wm.wakeUp();
				return true;
			}
		}
		return false;
	}
	
	final void cloneHeaderTo(final Message target){
		target.ctrl_level = ctrl_level;
		target.ctrl_is_downward = ctrl_is_downward;
//		target.header_sync_id = header_sync_id;//本项不能被复制
//		target.header_isInWorkbench = header_isInWorkbench;//本项不能被复制
//		target.header_dispatch_thread_id = header_dispatch_thread_id;//本项不能被复制
		target.ctrl_cycle_get_thread_id = ctrl_cycle_get_thread_id;
		
		target.ctrl_isInitiative = ctrl_isInitiative;
		
		target.ctrl_dev_id = ctrl_dev_id;
		target.ctrl_bind_id = ctrl_bind_id;
	}
	
	final void clear(){
		//重要：如果下列添加，请同步到cloneMe中
		ctrl_level = 0;
		ctrl_is_downward = true;
		ctrl_sync_id = 0;
		
		ctrl_isInitiative = true;
		
		ctrl_isInWorkbench = false;
		ctrl_dispatch_thread_id = 0;
		ctrl_cycle_get_thread_id = 0;
		
		ctrl_dev_id = "";
		ctrl_bind_id = "";
		ctrl_bind_ids = null;
		
		header_table.clear();
		body_table.clear();
	}
	
	/**
	 * 
	 * @param name
	 * @return if name is not exists or not Boolean object, then return false
	 * @see #getBooleanHeader(String, boolean)
	 * @see #getObjectHeader(String)
	 */
	public final boolean getBooleanHeader(final String name){
		final Object obj = header_table.get(name);
		if(obj != null && obj instanceof Boolean){
			return ((Boolean)obj).booleanValue();
		}
		return false;
	}
	
	/**
	 * 
	 * @param name
	 * @param defaultValue if name is not exists or not Boolean object, then return defaultValue.
	 * @return the value of Header <code>name</code>
	 * @see #getBooleanHeader(String)
	 * @see #getObjectHeader(String)
	 */
	public final boolean getBooleanHeader(final String name, final boolean defaultValue){
		final Object obj = header_table.get(name);
		if(obj != null){
			if(obj instanceof Boolean){
				return ((Boolean)obj).booleanValue();
			}
		}
			
		return defaultValue;
	}
	
	/**
	 * 
	 * @param name
	 * @return if name is not exists or not convertible object, then return 0
	 * @see #getByteHeader(String, byte)
	 * @see #getObjectHeader(String)
	 */
	public final byte getByteHeader(final String name){
		final Object obj = header_table.get(name);
		if(obj != null){
			if(obj instanceof Byte){
				return ((Byte)obj).byteValue();
			}else if(obj instanceof Double){
				return ((Double)obj).byteValue();
			}else if(obj instanceof Float){
				return ((Float)obj).byteValue();
			}else if(obj instanceof Long){
				return ((Long)obj).byteValue();
			}else if(obj instanceof Integer){
				return ((Integer)obj).byteValue();
			}else if(obj instanceof Short){
				return ((Short)obj).byteValue();
			}
		}
		return 0;
	}
	
	/**
	 * 
	 * @param name
	 * @param defaultValue if name is not exists or not convertible object, then return defaultValue.
	 * @return the value of Header <code>name</code>
	 * @see #getByteHeader(String)
	 * @see #getObjectHeader(String)
	 */
	public final byte getByteHeader(final String name, final byte defaultValue){
		final Object obj = header_table.get(name);
		if(obj != null){
			if(obj instanceof Byte){
				return ((Byte)obj).byteValue();
			}else if(obj instanceof Double){
				return ((Double)obj).byteValue();
			}else if(obj instanceof Float){
				return ((Float)obj).byteValue();
			}else if(obj instanceof Long){
				return ((Long)obj).byteValue();
			}else if(obj instanceof Integer){
				return ((Integer)obj).byteValue();
			}else if(obj instanceof Short){
				return ((Short)obj).byteValue();
			}
		}
			
		return defaultValue;
	}
	
	public final byte[] getByteArrayHeader(final String name){
		final Object obj = header_table.get(name);
		if(obj != null && obj instanceof byte[]){
			return (byte[])obj;
		}
			
		return null;
	}
	
	public final byte[] getByteArrayHeader(final String name, final byte[] defaultValue){
		final Object obj = header_table.get(name);
		if(obj != null && obj instanceof byte[]){
			return (byte[])obj;
		}
			
		return defaultValue;
	}
	
	/**
	 * 
	 * @param name
	 * @return if name is not exists or not Character object, then return 0
	 * @see #getCharHeader(String, char)
	 * @see #getObjectHeader(String)
	 */
	public final char getCharHeader(final String name){
		final Object obj = header_table.get(name);
		if(obj != null){
			if(obj instanceof Character){
				return ((Character)obj);
			}
		}
		return 0;
	}
	
	/**
	 * 
	 * @param name
	 * @param defaultValue if name is not exists or not Character object, then return defaultValue.
	 * @return the value of Header <code>name</code>
	 * @see #getCharHeader(String)
	 * @see #getObjectHeader(String)
	 */
	public final char getCharHeader(final String name, final char defaultValue){
		final Object obj = header_table.get(name);
		if(obj != null){
			if(obj instanceof Character){
				return ((Character)obj);
			}
		}
			
		return defaultValue;
	}
	
	/**
	 * 
	 * @param name
	 * @return if name is not exists or not convertible object, then return 0
	 * @see #getShortHeader(String, short)
	 * @see #getObjectHeader(String)
	 */
	public final short getShortHeader(final String name){
		final Object obj = header_table.get(name);
		if(obj != null){
			if(obj instanceof Short){
				return ((Short)obj).shortValue();
			}else if(obj instanceof Double){
				return ((Double)obj).shortValue();
			}else if(obj instanceof Float){
				return ((Float)obj).shortValue();
			}else if(obj instanceof Long){
				return ((Long)obj).shortValue();
			}else if(obj instanceof Integer){
				return ((Integer)obj).shortValue();
			}else if(obj instanceof Byte){
				return ((Byte)obj).shortValue();
			}
		}
		return 0;
	}
	
	/**
	 * 
	 * @param name
	 * @param defaultValue if name is not exists or not convertible object, then return defaultValue.
	 * @return the value of Header <code>name</code>
	 * @see #getShortHeader(String)
	 * @see #getObjectHeader(String)
	 */
	public final short getShortHeader(final String name, final short defaultValue){
		final Object obj = header_table.get(name);
		if(obj != null){
			if(obj instanceof Short){
				return ((Short)obj).shortValue();
			}else if(obj instanceof Double){
				return ((Double)obj).shortValue();
			}else if(obj instanceof Float){
				return ((Float)obj).shortValue();
			}else if(obj instanceof Long){
				return ((Long)obj).shortValue();
			}else if(obj instanceof Integer){
				return ((Integer)obj).shortValue();
			}else if(obj instanceof Byte){
				return ((Byte)obj).shortValue();
			}
		}
			
		return defaultValue;
	}
	
	/**
	 * 
	 * @param name
	 * @return if name is not exists or not convertible object, then return 0
	 * @see #getIntHeader(String, int)
	 * @see #getObjectHeader(String)
	 */
	public final int getIntHeader(final String name){
		final Object obj = header_table.get(name);
		if(obj != null){
			if(obj instanceof Integer){
				return ((Integer)obj).intValue();
			}else if(obj instanceof Double){
				return ((Double)obj).intValue();
			}else if(obj instanceof Float){
				return ((Float)obj).intValue();
			}else if(obj instanceof Long){
				return ((Long)obj).intValue();
			}else if(obj instanceof Short){
				return ((Short)obj).intValue();
			}else if(obj instanceof Byte){
				return ((Byte)obj).intValue();
			}
		}
		return 0;
	}
	
	/**
	 * 
	 * @param name
	 * @param defaultValue if name is not exists or not convertible object, then return defaultValue.
	 * @return the value of Header <code>name</code>
	 * @see #getIntHeader(String)
	 * @see #getObjectHeader(String)
	 */
	public final int getIntHeader(final String name, final int defaultValue){
		final Object obj = header_table.get(name);
		if(obj != null){
			if(obj instanceof Integer){
				return ((Integer)obj).intValue();
			}else if(obj instanceof Double){
				return ((Double)obj).intValue();
			}else if(obj instanceof Float){
				return ((Float)obj).intValue();
			}else if(obj instanceof Long){
				return ((Long)obj).intValue();
			}else if(obj instanceof Short){
				return ((Short)obj).intValue();
			}else if(obj instanceof Byte){
				return ((Byte)obj).intValue();
			}
		}
			
		return defaultValue;
	}
	
	/**
	 * 
	 * @param name
	 * @return if name is not exists or not convertible object, then return 0
	 * @see #getLongHeader(String, long)
	 * @see #getObjectHeader(String)
	 */
	public final long getLongHeader(final String name){
		final Object obj = header_table.get(name);
		if(obj != null){
			if(obj instanceof Long){
				return ((Long)obj).longValue();
			}else if(obj instanceof Double){
				return ((Double)obj).longValue();
			}else if(obj instanceof Float){
				return ((Float)obj).longValue();
			}else if(obj instanceof Integer){
				return ((Integer)obj).longValue();
			}else if(obj instanceof Short){
				return ((Short)obj).longValue();
			}else if(obj instanceof Byte){
				return ((Byte)obj).longValue();
			}
		}
		return 0;
	}
	
	/**
	 * 
	 * @param name
	 * @param defaultValue if name is not exists or not convertible object, then return defaultValue.
	 * @return the value of Header <code>name</code>
	 * @see #getLongHeader(String)
	 * @see #getObjectHeader(String)
	 */
	public final long getLongHeader(final String name, final long defaultValue){
		final Object obj = header_table.get(name);
		if(obj != null){
			if(obj instanceof Long){
				return ((Long)obj).longValue();
			}else if(obj instanceof Double){
				return ((Double)obj).longValue();
			}else if(obj instanceof Float){
				return ((Float)obj).longValue();
			}else if(obj instanceof Integer){
				return ((Integer)obj).longValue();
			}else if(obj instanceof Short){
				return ((Short)obj).longValue();
			}else if(obj instanceof Byte){
				return ((Byte)obj).longValue();
			}
		}
		
		return defaultValue;
	}
	
	/**
	 * 
	 * @param name
	 * @return if name is not exists or not convertible object, then return 0
	 * @see #getFloatHeader(String, float)
	 * @see #getObjectHeader(String)
	 */
	public final float getFloatHeader(final String name){
		final Object obj = header_table.get(name);
		if(obj != null){
			if(obj instanceof Float){
				return ((Float)obj).floatValue();
			}else if(obj instanceof Double){
				return ((Double)obj).floatValue();
			}else if(obj instanceof Long){
				return ((Long)obj).floatValue();
			}else if(obj instanceof Integer){
				return ((Integer)obj).floatValue();
			}else if(obj instanceof Short){
				return ((Short)obj).floatValue();
			}else if(obj instanceof Byte){
				return ((Byte)obj).floatValue();
			}
		}
		return 0;
	}
	
	/**
	 * 
	 * @param name
	 * @param defaultValue if name is not exists or not convertible object, then return defaultValue.
	 * @return the value of Header <code>name</code>
	 * @see #getFloatHeader(String)
	 * @see #getObjectHeader(String)
	 */
	public final float getFloatHeader(final String name, final float defaultValue){
		final Object obj = header_table.get(name);
		if(obj != null){
			if(obj instanceof Float){
				return ((Float)obj).floatValue();
			}else if(obj instanceof Double){
				return ((Double)obj).floatValue();
			}else if(obj instanceof Long){
				return ((Long)obj).floatValue();
			}else if(obj instanceof Integer){
				return ((Integer)obj).floatValue();
			}else if(obj instanceof Short){
				return ((Short)obj).floatValue();
			}else if(obj instanceof Byte){
				return ((Byte)obj).floatValue();
			}
		}
			
		return defaultValue;
	}
	
	/**
	 * 
	 * @param name
	 * @return if name is not exists or not convertible object, then return 0
	 * @see #getDoubleHeader(String, double)
	 * @see #getObjectHeader(String)
	 */
	public final double getDoubleHeader(final String name){
		final Object obj = header_table.get(name);
		if(obj != null){
			if(obj instanceof Double){
				return ((Double)obj).doubleValue();
			}else if(obj instanceof Float){
				return ((Float)obj).doubleValue();
			}else if(obj instanceof Long){
				return ((Long)obj).doubleValue();
			}else if(obj instanceof Integer){
				return ((Integer)obj).doubleValue();
			}else if(obj instanceof Short){
				return ((Short)obj).doubleValue();
			}else if(obj instanceof Byte){
				return ((Byte)obj).doubleValue();
			}
		}
		return 0;
	}
	
	/**
	 * 
	 * @param name
	 * @param defaultValue if name is not exists or not convertible object, then return defaultValue.
	 * @return the value of Header <code>name</code>
	 * @see #getDoubleHeader(String)
	 * @see #getObjectHeader(String)
	 */
	public final double getDoubleHeader(final String name, final double defaultValue){
		final Object obj = header_table.get(name);
		if(obj != null){
			if(obj instanceof Double){
				return ((Double)obj).doubleValue();
			}else if(obj instanceof Float){
				return ((Float)obj).doubleValue();
			}else if(obj instanceof Long){
				return ((Long)obj).doubleValue();
			}else if(obj instanceof Integer){
				return ((Integer)obj).doubleValue();
			}else if(obj instanceof Short){
				return ((Short)obj).doubleValue();
			}else if(obj instanceof Byte){
				return ((Byte)obj).doubleValue();
			}
		}
		return defaultValue;
	}
	
	/**
	 * 
	 * @param name
	 * @return if name is not exists, return null; if name is not String object, return obj.toString()
	 * @see #getStringHeader(String, String)
	 * @see #getObjectHeader(String)
	 */
	public final String getStringHeader(final String name){
		final Object obj = header_table.get(name);
		if(obj != null){
			if(obj instanceof String){
				return (String)obj;
			}else{
				return obj.toString();
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param name
	 * @param defaultValue if name is not exists, then return defaultValue.
	 * @return the value of Header <code>name</code>
	 * @see #getStringHeader(String)
	 * @see #getObjectHeader(String)
	 */
	public final String getStringHeader(final String name, final String defaultValue){
		final Object obj = header_table.get(name);
		if(obj != null){
			if(obj instanceof String){
				return (String)obj;
			}else{
				return obj.toString();
			}
		}else{
			return defaultValue;
		}
	}
	
	/**
	 * get Header by name; check name is exists or not.
	 * @param name
	 * @return if name is not exists, then return null
	 * @see #getObjectHeader(String, Object)
	 */
	public final Object getObjectHeader(final String name){
		return header_table.get(name);
	}
	
	/**
	 * 
	 * @param name
	 * @param defaultValue if name is not exists, then return defaultValue.
	 * @return the value of Header <code>name</code>
	 * @see #getObjectHeader(String)
	 */
	public final Object getObjectHeader(final String name, final Object defaultValue){
		final Object obj = header_table.get(name);
		if(obj != null){
			return obj;
		}else{
			return defaultValue;
		}
	}
	
	/**
	 * 
	 * @param name
	 */
	public final void removeHeader(final String name){
		header_table.remove(name);
	}
	
	public final Set<String> getHeaderNames(){
		return header_table.keySet();
	}
	
	/**
	 * 
	 * @param name
	 * @param value
	 */
	public final void setBooleanHeader(final String name, final boolean value){
		if(ctrl_isInWorkbench){
			throw new MSBException(MSBException.UN_MODIFIED, this, null);
		}
		header_table.put(name, Boolean.valueOf(value));
	}
	
	/**
	 * 
	 * @param name
	 * @param value
	 */
	public final void setByteHeader(final String name, final byte value){
		if(ctrl_isInWorkbench){
			throw new MSBException(MSBException.UN_MODIFIED, this, null);
		}
		header_table.put(name, Byte.valueOf(value));
	}
	
	/**
	 * set <code>value</code> for the <code>name</code> in header.<BR>
	 * to copy byte array , please invoke {@link #setByteArrayHeader(String, byte[], int, int)}.
	 * @param name
	 * @param value is NOT copied.
	 */
	public final void setByteArrayHeader(final String name, final byte[] value){
		if(ctrl_isInWorkbench){
			throw new MSBException(MSBException.UN_MODIFIED, this, null);
		}
		header_table.put(name, value);
	}

	/**
	 * set name with new byte array, which copy values from bs.
	 * @param name
	 * @param bs is copied to new byte array.
	 * @param offset
	 * @param length
	 */
	public final void setByteArrayHeader(final String name, final byte[] bs, final int offset, final int length){
		if(ctrl_isInWorkbench){
			throw new MSBException(MSBException.UN_MODIFIED, this, null);
		}
		final byte[] outbs = new byte[length];
		System.arraycopy(bs, offset, outbs, 0, length);
		header_table.put(name, outbs);
	}
	
	/**
	 * 
	 * @param name
	 * @param value
	 */
	public final void setCharHeader(final String name, final char value){
		if(ctrl_isInWorkbench){
			throw new MSBException(MSBException.UN_MODIFIED, this, null);
		}
		header_table.put(name, Character.valueOf(value));
	}
	
	/**
	 * 
	 * @param name
	 * @param value
	 */
	public final void setShortHeader(final String name, final short value){
		if(ctrl_isInWorkbench){
			throw new MSBException(MSBException.UN_MODIFIED, this, null);
		}
		header_table.put(name, Short.valueOf(value));
	}
	
	/**
	 * 
	 * @param name
	 * @param value
	 */
	public final void setIntHeader(final String name, final int value){
		if(ctrl_isInWorkbench){
			throw new MSBException(MSBException.UN_MODIFIED, this, null);
		}
		header_table.put(name, Integer.valueOf(value));
	}
	
	/**
	 * 
	 * @param name
	 * @param value
	 */
	public final void setLongHeader(final String name, final long value){
		if(ctrl_isInWorkbench){
			throw new MSBException(MSBException.UN_MODIFIED, this, null);
		}
		header_table.put(name, Long.valueOf(value));
	}
	
	/**
	 * 
	 * @param name
	 * @param value
	 */
	public final void setFloatHeader(final String name, final float value){
		if(ctrl_isInWorkbench){
			throw new MSBException(MSBException.UN_MODIFIED, this, null);
		}
		header_table.put(name, Float.valueOf(value));
	}
	
	/**
	 * 
	 * @param name
	 * @param value
	 */
	public final void setDoubleHeader(final String name, final double value){
		if(ctrl_isInWorkbench){
			throw new MSBException(MSBException.UN_MODIFIED, this, null);
		}
		header_table.put(name, Double.valueOf(value));
	}
	
	/**
	 * 
	 * @param name
	 * @param value
	 */
	public final void setStringHeader(final String name, final String value){
		if(ctrl_isInWorkbench){
			throw new MSBException(MSBException.UN_MODIFIED, this, null);
		}
		header_table.put(name, value);
	}
	
	/**
	 * 
	 * @param name
	 * @param value
	 */
	public final void setObjectHeader(final String name, final Object value){
		if(ctrl_isInWorkbench){
			throw new MSBException(MSBException.UN_MODIFIED, this, null);
		}
		header_table.put(name, value);
	}
	
	/**
	 * 
	 * @param name
	 * @return if name is not exists or not Boolean object, then return false
	 * @see #getBooleanBody(String, boolean)
	 * @see #getObjectBody(String)
	 */
	public final boolean getBooleanBody(final String name){
		final Object obj = body_table.get(name);
		if(obj != null && obj instanceof Boolean){
			return ((Boolean)obj).booleanValue();
		}
		return false;
	}

	/**
	 * 
	 * @param name
	 * @param defaultValue if name is not exists or not Boolean object, then return defaultValue.
	 * @return the value of body <code>name</code>
	 * @see #getBooleanBody(String)
	 * @see #getObjectBody(String)
	 */
	public final boolean getBooleanBody(final String name, final boolean defaultValue){
		final Object obj = body_table.get(name);
		if(obj != null){
			if(obj instanceof Boolean){
				return ((Boolean)obj).booleanValue();
			}
		}
			
		return defaultValue;
	}

	/**
	 * 
	 * @param name
	 * @return if name is not exists or not Character object, then return 0
	 * @see #getCharBody(String, char)
	 * @see #getObjectBody(String)
	 */
	public final char getCharBody(final java.lang.String name){
		final Object obj = body_table.get(name);
		if(obj != null && obj instanceof Character){
			return ((Character)obj).charValue();
		}
		return 0;
	}
	
	/**
	 * 
	 * @param name
	 * @param defaultValue if name is not exists or not Character object, then return defaultValue.
	 * @return the value of body <code>name</code>
	 * @see #getCharBody(String)
	 * @see #getObjectBody(String)
	 */
	public final char getCharBody(final java.lang.String name, final char defaultValue){
		final Object obj = body_table.get(name);
		if(obj != null){
			if(obj instanceof Character){
				return ((Character)obj).charValue();
			}
		}
			
		return defaultValue;
	}
	
	/**
	 * 
	 * @param name
	 * @return if name is not exists or not convertible object, then return 0
	 * @see #getByteBody(String, byte)
	 * @see #getObjectBody(String)
	 */
	public final byte getByteBody(final String name){
		final Object obj = body_table.get(name);
		if(obj != null){
			if(obj instanceof Byte){
				return ((Byte)obj).byteValue();
			}else if(obj instanceof Double){
				return ((Double)obj).byteValue();
			}else if(obj instanceof Float){
				return ((Float)obj).byteValue();
			}else if(obj instanceof Long){
				return ((Long)obj).byteValue();
			}else if(obj instanceof Integer){
				return ((Integer)obj).byteValue();
			}else if(obj instanceof Short){
				return ((Short)obj).byteValue();
			}
		}
		return 0;
	}

	/**
	 * 
	 * @param name
	 * @param defaultValue if name is not exists or not convertible object, then return defaultValue.
	 * @return the value of body <code>name</code>
	 * @see #getByteBody(String)
	 * @see #getObjectBody(String)
	 */
	public final byte getByteBody(final String name, final byte defaultValue){
		final Object obj = body_table.get(name);
		if(obj != null){
			if(obj instanceof Byte){
				return ((Byte)obj).byteValue();
			}else if(obj instanceof Double){
				return ((Double)obj).byteValue();
			}else if(obj instanceof Float){
				return ((Float)obj).byteValue();
			}else if(obj instanceof Long){
				return ((Long)obj).byteValue();
			}else if(obj instanceof Integer){
				return ((Integer)obj).byteValue();
			}else if(obj instanceof Short){
				return ((Short)obj).byteValue();
			}
		}
			
		return defaultValue;
	}

	/**
	 * 
	 * @param name
	 * @return if name is not exists or not byte array object, then return null
	 * @see #getByteArrayBody(String, byte[])
	 * @see #getObjectBody(String)
	 */
	public final byte[] getByteArrayBody(final String name){
		final Object obj = body_table.get(name);
		if(obj != null && obj instanceof byte[]){
			return (byte[])obj;
		}
		return null;
	}
	
	/**
	 * 
	 * @param name
	 * @param defaultValue if name is not exists or not byte array object, then return defaultValue.
	 * @return the value of body <code>name</code>
	 * @see #getByteArrayBody(String)
	 * @see #getObjectBody(String)
	 */
	public final byte[] getByteArrayBody(final String name, final byte[] defaultValue){
		final Object obj = body_table.get(name);
		if(obj != null){
			if(obj instanceof byte[]){
				return (byte[])obj;
			}
		}
			
		return defaultValue;
	}
	
	/**
	 * 
	 * @param name
	 * @return if name is not exists or not convertible object, then return 0
	 * @see #getShortBody(String, short)
	 * @see #getObjectBody(String)
	 */
	public final short getShortBody(final String name){
		final Object obj = body_table.get(name);
		if(obj != null){
			if(obj instanceof Short){
				return ((Short)obj).shortValue();
			}else if(obj instanceof Double){
				return ((Double)obj).shortValue();
			}else if(obj instanceof Float){
				return ((Float)obj).shortValue();
			}else if(obj instanceof Long){
				return ((Long)obj).shortValue();
			}else if(obj instanceof Integer){
				return ((Integer)obj).shortValue();
			}else if(obj instanceof Byte){
				return ((Byte)obj).shortValue();
			}
		}
		return 0;
	}

	/**
	 * 
	 * @param name
	 * @param defaultValue if name is not exists or not convertible object, then return defaultValue.
	 * @return the value of body <code>name</code>
	 * @see #getShortBody(String)
	 * @see #getObjectBody(String)
	 */
	public final short getShortBody(final String name, final short defaultValue){
		final Object obj = body_table.get(name);
		if(obj != null){
			if(obj instanceof Short){
				return ((Short)obj).shortValue();
			}else if(obj instanceof Double){
				return ((Double)obj).shortValue();
			}else if(obj instanceof Float){
				return ((Float)obj).shortValue();
			}else if(obj instanceof Long){
				return ((Long)obj).shortValue();
			}else if(obj instanceof Integer){
				return ((Integer)obj).shortValue();
			}else if(obj instanceof Byte){
				return ((Byte)obj).shortValue();
			}
		}
			
		return defaultValue;
	}

	/**
	 * 
	 * @param name
	 * @return if name is not exists or not convertible object, then return 0
	 * @see #getIntBody(String, int)
	 * @see #getObjectBody(String)
	 */
	public final int getIntBody(final String name){
		final Object obj = body_table.get(name);
		if(obj != null){
			if(obj instanceof Integer){
				return ((Integer)obj).intValue();
			}else if(obj instanceof Double){
				return ((Double)obj).intValue();
			}else if(obj instanceof Float){
				return ((Float)obj).intValue();
			}else if(obj instanceof Long){
				return ((Long)obj).intValue();
			}else if(obj instanceof Short){
				return ((Short)obj).intValue();
			}else if(obj instanceof Byte){
				return ((Byte)obj).intValue();
			}
		}
		return 0;
	}

	/**
	 * 
	 * @param name
	 * @param defaultValue if name is not exists or not convertible object, then return defaultValue.
	 * @return the value of body <code>name</code>
	 * @see #getIntBody(String)
	 * @see #getObjectBody(String)
	 */
	public final int getIntBody(final String name, final int defaultValue){
		final Object obj = body_table.get(name);
		if(obj != null){
			if(obj instanceof Integer){
				return ((Integer)obj).intValue();
			}else if(obj instanceof Double){
				return ((Double)obj).intValue();
			}else if(obj instanceof Float){
				return ((Float)obj).intValue();
			}else if(obj instanceof Long){
				return ((Long)obj).intValue();
			}else if(obj instanceof Short){
				return ((Short)obj).intValue();
			}else if(obj instanceof Byte){
				return ((Byte)obj).intValue();
			}
		}
			
		return defaultValue;
	}

	/**
	 * 
	 * @param name
	 * @return if name is not exists or not convertible object, then return 0
	 * @see #getLongBody(String, long)
	 * @see #getObjectBody(String)
	 */
	public final long getLongBody(final String name){
		final Object obj = body_table.get(name);
		if(obj != null){
			if(obj instanceof Long){
				return ((Long)obj).longValue();
			}else if(obj instanceof Double){
				return ((Double)obj).longValue();
			}else if(obj instanceof Float){
				return ((Float)obj).longValue();
			}else if(obj instanceof Integer){
				return ((Integer)obj).longValue();
			}else if(obj instanceof Short){
				return ((Short)obj).longValue();
			}else if(obj instanceof Byte){
				return ((Byte)obj).longValue();
			}
		}
		return 0;
	}

	/**
	 * 
	 * @param name
	 * @param defaultValue if name is not exists or not convertible object, then return defaultValue.
	 * @return the value of body <code>name</code>
	 * @see #getLongBody(String)
	 * @see #getObjectBody(String)
	 */
	public final long getLongBody(final String name, final long defaultValue){
		final Object obj = body_table.get(name);
		if(obj != null){
			if(obj instanceof Long){
				return ((Long)obj).longValue();
			}else if(obj instanceof Double){
				return ((Double)obj).longValue();
			}else if(obj instanceof Float){
				return ((Float)obj).longValue();
			}else if(obj instanceof Integer){
				return ((Integer)obj).longValue();
			}else if(obj instanceof Short){
				return ((Short)obj).longValue();
			}else if(obj instanceof Byte){
				return ((Byte)obj).longValue();
			}
		}
			
		return defaultValue;
	}

	/**
	 * 
	 * @param name
	 * @return if name is not exists or not convertible object, then return 0
	 * @see #getFloatBody(String, float)
	 * @see #getObjectBody(String)
	 */
	public final float getFloatBody(final String name){
		final Object obj = body_table.get(name);
		if(obj != null){
			if(obj instanceof Float){
				return ((Float)obj).floatValue();
			}else if(obj instanceof Double){
				return ((Double)obj).floatValue();
			}else if(obj instanceof Long){
				return ((Long)obj).floatValue();
			}else if(obj instanceof Integer){
				return ((Integer)obj).floatValue();
			}else if(obj instanceof Short){
				return ((Short)obj).floatValue();
			}else if(obj instanceof Byte){
				return ((Byte)obj).floatValue();
			}
		}
		return 0;
	}

	/**
	 * 
	 * @param name
	 * @param defaultValue if name is not exists or not convertible object, then return defaultValue.
	 * @return the value of body <code>name</code>
	 * @see #getFloatBody(String)
	 * @see #getObjectBody(String)
	 */
	public final float getFloatBody(final String name, final float defaultValue){
		final Object obj = body_table.get(name);
		if(obj != null){
			if(obj instanceof Float){
				return ((Float)obj).floatValue();
			}else if(obj instanceof Double){
				return ((Double)obj).floatValue();
			}else if(obj instanceof Long){
				return ((Long)obj).floatValue();
			}else if(obj instanceof Integer){
				return ((Integer)obj).floatValue();
			}else if(obj instanceof Short){
				return ((Short)obj).floatValue();
			}else if(obj instanceof Byte){
				return ((Byte)obj).floatValue();
			}
		}
			
		return defaultValue;
	}

	/**
	 * 
	 * @param name
	 * @return if name is not exists or not convertible object, then return 0
	 * @see #getDoubleBody(String, double)
	 * @see #getObjectBody(String)
	 */
	public final double getDoubleBody(final String name){
		final Object obj = body_table.get(name);
		if(obj != null){
			if(obj instanceof Double){
				return ((Double)obj).doubleValue();
			}else if(obj instanceof Float){
				return ((Float)obj).doubleValue();
			}else if(obj instanceof Long){
				return ((Long)obj).doubleValue();
			}else if(obj instanceof Integer){
				return ((Integer)obj).doubleValue();
			}else if(obj instanceof Short){
				return ((Short)obj).doubleValue();
			}else if(obj instanceof Byte){
				return ((Byte)obj).doubleValue();
			}
		}
		return 0;
	}

	/**
	 * 
	 * @param name
	 * @param defaultValue if name is not exists or not convertible object, then return defaultValue.
	 * @return the value of body <code>name</code>
	 * @see #getDoubleBody(String)
	 * @see #getObjectBody(String)
	 */
	public final double getDoubleBody(final String name, final double defaultValue){
		final Object obj = body_table.get(name);
		if(obj != null){
			if(obj instanceof Double){
				return ((Double)obj).doubleValue();
			}else if(obj instanceof Float){
				return ((Float)obj).doubleValue();
			}else if(obj instanceof Long){
				return ((Long)obj).doubleValue();
			}else if(obj instanceof Integer){
				return ((Integer)obj).doubleValue();
			}else if(obj instanceof Short){
				return ((Short)obj).doubleValue();
			}else if(obj instanceof Byte){
				return ((Byte)obj).doubleValue();
			}
		}
			
		return defaultValue;
	}

	/**
	 * 
	 * @param name
	 * @return if name is not exists, return null; otherwise return obj.toString()
	 * @see #getStringBody(String, String)
	 * @see #getObjectBody(String)
	 */
	public final String getStringBody(final String name){
		final Object obj = body_table.get(name);
		if(obj != null){
			if(obj instanceof String){
				return (String)obj;
			}else{
				return obj.toString();
			}
		}
		return null;
	}

	/**
	 * 
	 * @param name
	 * @param defaultValue if name is not exists, then return defaultValue.
	 * @return the value of body <code>name</code>
	 * @see #getStringBody(String)
	 * @see #getObjectBody(String)
	 */
	public final String getStringBody(final String name, final String defaultValue){
		final Object obj = body_table.get(name);
		if(obj != null){
			if(obj instanceof String){
				return (String)obj;
			}else{
				return obj.toString();
			}
		}else{
			return defaultValue;
		}
	}

	/**
	 * get body by name; check name is exists or not.
	 * @param name
	 * @return if name is not exists, then return null
	 * @see #getObjectBody(String, Object)
	 */
	public final Object getObjectBody(final String name){
		return body_table.get(name);
	}

	/**
	 * 
	 * @param name
	 * @param defaultValue if name is not exists, then return defaultValue.
	 * @return the value of body <code>name</code>
	 * @see #getObjectBody(String)
	 */
	public final Object getObjectBody(final String name, final Object defaultValue){
		final Object obj = body_table.get(name);
		if(obj != null){
			return obj;
		}else{
			return defaultValue;
		}
	}

	/**
	 * 
	 * @param name
	 */
	public final void removeBody(final String name){
		body_table.remove(name);
	}

	public final Set<String> getBodyNames(){
		return body_table.keySet();
	}

	/**
	 * 
	 * @param name
	 * @param value
	 */
	public final void setBooleanBody(final String name, final boolean value){
		if(ctrl_isInWorkbench){
			throw new MSBException(MSBException.UN_MODIFIED, this, null);
		}
		body_table.put(name, Boolean.valueOf(value));
	}

	/**
	 * 
	 * @param name
	 * @param value
	 */
	public final void setCharBody(final String name, final char value){
		if(ctrl_isInWorkbench){
			throw new MSBException(MSBException.UN_MODIFIED, this, null);
		}
		body_table.put(name, Character.valueOf(value));
	}
	
	/**
	 * 
	 * @param name
	 * @param value
	 */
	public final void setByteBody(final String name, final byte value){
		if(ctrl_isInWorkbench){
			throw new MSBException(MSBException.UN_MODIFIED, this, null);
		}
		body_table.put(name, Byte.valueOf(value));
	}

	/**
	 * set <code>value</code> for the <code>name</code> in body.<BR>
	 * to copy byte array , please invoke {@link #setByteArrayBody(String, byte[], int, int)}.
	 * @param name
	 * @param value is NOT copied.
	 */
	public final void setByteArrayBody(final String name, final byte[] value){
		if(ctrl_isInWorkbench){
			throw new MSBException(MSBException.UN_MODIFIED, this, null);
		}
		body_table.put(name, value);
	}

	/**
	 * set name with new bytes array, which copy values from bs.
	 * @param name
	 * @param bs is copied to new byte array.
	 * @param offset
	 * @param length
	 */
	public final void setByteArrayBody(final String name, final byte[] bs, final int offset, final int length){
		if(ctrl_isInWorkbench){
			throw new MSBException(MSBException.UN_MODIFIED, this, null);
		}
		final byte[] outbs = new byte[length];
		System.arraycopy(bs, offset, outbs, 0, length);
		body_table.put(name, outbs);
	}

	/**
	 * 
	 * @param name
	 * @param value
	 */
	public final void setShortBody(final String name, final short value){
		if(ctrl_isInWorkbench){
			throw new MSBException(MSBException.UN_MODIFIED, this, null);
		}
		body_table.put(name, Short.valueOf(value));
	}

	/**
	 * 
	 * @param name
	 * @param value
	 */
	public final void setIntBody(final String name, final int value){
		if(ctrl_isInWorkbench){
			throw new MSBException(MSBException.UN_MODIFIED, this, null);
		}
		body_table.put(name, Integer.valueOf(value));
	}

	/**
	 * 
	 * @param name
	 * @param value
	 */
	public final void setLongBody(final String name, final long value){
		if(ctrl_isInWorkbench){
			throw new MSBException(MSBException.UN_MODIFIED, this, null);
		}
		body_table.put(name, Long.valueOf(value));
	}

	/**
	 * 
	 * @param name
	 * @param value
	 */
	public final void setFloatBody(final String name, final float value){
		if(ctrl_isInWorkbench){
			throw new MSBException(MSBException.UN_MODIFIED, this, null);
		}
		body_table.put(name, Float.valueOf(value));
	}

	/**
	 * 
	 * @param name
	 * @param value
	 */
	public final void setDoubleBody(final String name, final double value){
		if(ctrl_isInWorkbench){
			throw new MSBException(MSBException.UN_MODIFIED, this, null);
		}
		body_table.put(name, Double.valueOf(value));
	}

	/**
	 * 
	 * @param name
	 * @param value
	 */
	public final void setStringBody(final String name, final String value){
		if(ctrl_isInWorkbench){
			throw new MSBException(MSBException.UN_MODIFIED, this, null);
		}
		body_table.put(name, value);
	}

	/**
	 * 
	 * @param name
	 * @param value
	 */
	public final void setObjectBody(final String name, final Object value){
		if(ctrl_isInWorkbench){
			throw new MSBException(MSBException.UN_MODIFIED, this, null);
		}
		body_table.put(name, value);
	}
	
	@Override
	public final String toString() {
		final StringBuffer sb = new StringBuffer();
		
		sb.append(" header:[");
		
		{
			final Set<String> set = getHeaderNames();
			boolean second = false;
			final Iterator<String> it = set.iterator();
			while(it.hasNext()){
				final String p = it.next();
				if(second == false){
					second = true;
				}else{
					sb.append(",");
				}
				final Object objectHeader = getObjectHeader(p);
				sb.append(p + "=" + objectHeader + "(" + objectHeader.getClass().getSimpleName() + ")");
			}
		}
		sb.append("], body:[");
		{
			final Set<String> set = getBodyNames();
			boolean second = false;
			final Iterator<String> it = set.iterator();
			while(it.hasNext()){
				final String p = it.next();
				if(second == false){
					second = true;
				}else{
					sb.append(",");
				}
				final Object objectBody = getObjectBody(p);
				sb.append(p + "=" + objectBody + "(" + objectBody.getClass().getSimpleName() + ")");
			}
		}
		sb.append("]");
		
		return "\n\t{" + super.toString() 
				+ ", dev_id=" + ctrl_dev_id
				+ sb.toString() + "}";
	}
}

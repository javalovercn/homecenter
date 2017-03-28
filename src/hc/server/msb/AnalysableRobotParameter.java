package hc.server.msb;

import hc.server.util.Assistant;

import java.util.HashMap;
import java.util.Iterator;

/**
 * this class is used to pass parameter between Robot and UI or {@link Assistant}.<BR>
 * it is helpful to server to analyze the parameter and know more about the relationship between business and user.
 * <BR><BR>
 * it is recommended used in {@link Robot#operate(long, Object)} and the return object.
 */
public class AnalysableRobotParameter {

	public AnalysableRobotParameter() {
	}

	final HashMap<String, Object> table = new HashMap<String, Object>();
	int level;
	
	/**
	 * if name is not exists or not Boolean object, then return false
	 * 
	 * @param name
	 * @return
	 */
	public final boolean getBoolean(final String name) {
		final Object obj = table.get(name);
		if (obj != null && obj instanceof Boolean) {
			return ((Boolean) obj).booleanValue();
		}
		return false;
	}

	// /**
	// * if name is not exists or not Boolean object, then return defaultValue.
	// * @param name
	// * @param defaultValue
	// * @return the value of <code>name</code>
	// * @see #getBoolean(String)
	// */
	// public final boolean getBoolean(final String name, final boolean
	// defaultValue){
	// final Object obj = body_table.get(name);
	// if(obj != null){
	// if(obj instanceof Boolean){
	// return ((Boolean)obj).booleanValue();
	// }
	// }
	//
	// return defaultValue;
	// }

	/**
	 * if name is not exists or not Character object, then return 0
	 * 
	 * @param name
	 * @return
	 */
	public final char getChar(final java.lang.String name) {
		final Object obj = table.get(name);
		if (obj != null && obj instanceof Character) {
			return ((Character) obj).charValue();
		}
		return 0;
	}

	// /**
	// * if name is not exists or not Character object, then return
	// defaultValue.
	// * @param name
	// * @param defaultValue
	// * @return the value of <code>name</code>
	// * @see #getChar(String)
	// */
	// public final char getChar(final java.lang.String name, final char
	// defaultValue){
	// final Object obj = body_table.get(name);
	// if(obj != null){
	// if(obj instanceof Character){
	// return ((Character)obj).charValue();
	// }
	// }
	//
	// return defaultValue;
	// }

	/**
	 * if name is not exists or not convertible object, then return 0
	 * 
	 * @param name
	 * @return
	 */
	public final byte getByte(final String name) {
		final Object obj = table.get(name);
		if (obj != null) {
			if (obj instanceof Byte) {
				return ((Byte) obj).byteValue();
			} else if (obj instanceof Double) {
				return ((Double) obj).byteValue();
			} else if (obj instanceof Float) {
				return ((Float) obj).byteValue();
			} else if (obj instanceof Long) {
				return ((Long) obj).byteValue();
			} else if (obj instanceof Integer) {
				return ((Integer) obj).byteValue();
			} else if (obj instanceof Short) {
				return ((Short) obj).byteValue();
			}
		}
		return 0;
	}

	// /**
	// * if name is not exists or not convertible object, then return
	// defaultValue.
	// * @param name
	// * @param defaultValue
	// * @return the value of <code>name</code>
	// * @see #getByte(String)
	// */
	// public final byte getByte(final String name, final byte defaultValue){
	// final Object obj = body_table.get(name);
	// if(obj != null){
	// if(obj instanceof Byte){
	// return ((Byte)obj).byteValue();
	// }else if(obj instanceof Double){
	// return ((Double)obj).byteValue();
	// }else if(obj instanceof Float){
	// return ((Float)obj).byteValue();
	// }else if(obj instanceof Long){
	// return ((Long)obj).byteValue();
	// }else if(obj instanceof Integer){
	// return ((Integer)obj).byteValue();
	// }else if(obj instanceof Short){
	// return ((Short)obj).byteValue();
	// }
	// }
	//
	// return defaultValue;
	// }

	/**
	 * if name is not exists or not byte array object, then return null
	 * 
	 * @param name
	 * @return
	 */
	public final byte[] getByteArray(final String name) {
		final Object obj = table.get(name);
		if (obj != null && obj instanceof byte[]) {
			return (byte[]) obj;
		}
		return null;
	}

	// /**
	// * if name is not exists or not byte array object, then return
	// defaultValue.
	// * @param name
	// * @param defaultValue
	// * @return the value of <code>name</code>
	// * @see #getByteArray(String)
	// */
	// public final byte[] getByteArray(final String name, final byte[]
	// defaultValue){
	// final Object obj = body_table.get(name);
	// if(obj != null){
	// if(obj instanceof byte[]){
	// return (byte[])obj;
	// }
	// }
	//
	// return defaultValue;
	// }

	/**
	 * if name is not exists or not convertible object, then return 0
	 * 
	 * @param name
	 * @return
	 */
	public final short getShort(final String name) {
		final Object obj = table.get(name);
		if (obj != null) {
			if (obj instanceof Short) {
				return ((Short) obj).shortValue();
			} else if (obj instanceof Double) {
				return ((Double) obj).shortValue();
			} else if (obj instanceof Float) {
				return ((Float) obj).shortValue();
			} else if (obj instanceof Long) {
				return ((Long) obj).shortValue();
			} else if (obj instanceof Integer) {
				return ((Integer) obj).shortValue();
			} else if (obj instanceof Byte) {
				return ((Byte) obj).shortValue();
			}
		}
		return 0;
	}

	// /**
	// * if name is not exists or not convertible object, then return
	// defaultValue.
	// * @param name
	// * @param defaultValue
	// * @return the value of <code>name</code>
	// * @see #getShort(String)
	// */
	// public final short getShort(final String name, final short defaultValue){
	// final Object obj = body_table.get(name);
	// if(obj != null){
	// if(obj instanceof Short){
	// return ((Short)obj).shortValue();
	// }else if(obj instanceof Double){
	// return ((Double)obj).shortValue();
	// }else if(obj instanceof Float){
	// return ((Float)obj).shortValue();
	// }else if(obj instanceof Long){
	// return ((Long)obj).shortValue();
	// }else if(obj instanceof Integer){
	// return ((Integer)obj).shortValue();
	// }else if(obj instanceof Byte){
	// return ((Byte)obj).shortValue();
	// }
	// }
	//
	// return defaultValue;
	// }

	/**
	 * if name is not exists or not convertible object, then return 0
	 * 
	 * @param name
	 * @return
	 */
	public final int getInt(final String name) {
		final Object obj = table.get(name);
		if (obj != null) {
			if (obj instanceof Integer) {
				return ((Integer) obj).intValue();
			} else if (obj instanceof Double) {
				return ((Double) obj).intValue();
			} else if (obj instanceof Float) {
				return ((Float) obj).intValue();
			} else if (obj instanceof Long) {
				return ((Long) obj).intValue();
			} else if (obj instanceof Short) {
				return ((Short) obj).intValue();
			} else if (obj instanceof Byte) {
				return ((Byte) obj).intValue();
			}
		}
		return 0;
	}

	// /**
	// * if name is not exists or not convertible object, then return
	// defaultValue.
	// * @param name
	// * @param defaultValue
	// * @return the value of <code>name</code>
	// * @see #getInt(String)
	// */
	// public final int getInt(final String name, final int defaultValue){
	// final Object obj = body_table.get(name);
	// if(obj != null){
	// if(obj instanceof Integer){
	// return ((Integer)obj).intValue();
	// }else if(obj instanceof Double){
	// return ((Double)obj).intValue();
	// }else if(obj instanceof Float){
	// return ((Float)obj).intValue();
	// }else if(obj instanceof Long){
	// return ((Long)obj).intValue();
	// }else if(obj instanceof Short){
	// return ((Short)obj).intValue();
	// }else if(obj instanceof Byte){
	// return ((Byte)obj).intValue();
	// }
	// }
	//
	// return defaultValue;
	// }

	/**
	 * if name is not exists or not convertible object, then return 0
	 * 
	 * @param name
	 * @return
	 */
	public final long getLong(final String name) {
		final Object obj = table.get(name);
		if (obj != null) {
			if (obj instanceof Long) {
				return ((Long) obj).longValue();
			} else if (obj instanceof Double) {
				return ((Double) obj).longValue();
			} else if (obj instanceof Float) {
				return ((Float) obj).longValue();
			} else if (obj instanceof Integer) {
				return ((Integer) obj).longValue();
			} else if (obj instanceof Short) {
				return ((Short) obj).longValue();
			} else if (obj instanceof Byte) {
				return ((Byte) obj).longValue();
			}
		}
		return 0;
	}

	// /**
	// * if name is not exists or not convertible object, then return
	// defaultValue.
	// * @param name
	// * @param defaultValue
	// * @return the value of <code>name</code>
	// * @see #getLong(String)
	// */
	// public final long getLong(final String name, final long defaultValue){
	// final Object obj = body_table.get(name);
	// if(obj != null){
	// if(obj instanceof Long){
	// return ((Long)obj).longValue();
	// }else if(obj instanceof Double){
	// return ((Double)obj).longValue();
	// }else if(obj instanceof Float){
	// return ((Float)obj).longValue();
	// }else if(obj instanceof Integer){
	// return ((Integer)obj).longValue();
	// }else if(obj instanceof Short){
	// return ((Short)obj).longValue();
	// }else if(obj instanceof Byte){
	// return ((Byte)obj).longValue();
	// }
	// }
	//
	// return defaultValue;
	// }

	/**
	 * if name is not exists or not convertible object, then return 0
	 * 
	 * @param name
	 * @return
	 */
	public final float getFloat(final String name) {
		final Object obj = table.get(name);
		if (obj != null) {
			if (obj instanceof Float) {
				return ((Float) obj).floatValue();
			} else if (obj instanceof Double) {
				return ((Double) obj).floatValue();
			} else if (obj instanceof Long) {
				return ((Long) obj).floatValue();
			} else if (obj instanceof Integer) {
				return ((Integer) obj).floatValue();
			} else if (obj instanceof Short) {
				return ((Short) obj).floatValue();
			} else if (obj instanceof Byte) {
				return ((Byte) obj).floatValue();
			}
		}
		return 0;
	}

	// /**
	// * if name is not exists or not convertible object, then return
	// defaultValue.
	// * @param name
	// * @param defaultValue
	// * @return the value of <code>name</code>
	// * @see #getFloat(String)
	// */
	// public final float getFloat(final String name, final float defaultValue){
	// final Object obj = body_table.get(name);
	// if(obj != null){
	// if(obj instanceof Float){
	// return ((Float)obj).floatValue();
	// }else if(obj instanceof Double){
	// return ((Double)obj).floatValue();
	// }else if(obj instanceof Long){
	// return ((Long)obj).floatValue();
	// }else if(obj instanceof Integer){
	// return ((Integer)obj).floatValue();
	// }else if(obj instanceof Short){
	// return ((Short)obj).floatValue();
	// }else if(obj instanceof Byte){
	// return ((Byte)obj).floatValue();
	// }
	// }
	//
	// return defaultValue;
	// }

	/**
	 * if name is not exists or not convertible object, then return 0
	 * 
	 * @param name
	 * @return
	 */
	public final double getDouble(final String name) {
		final Object obj = table.get(name);
		if (obj != null) {
			if (obj instanceof Double) {
				return ((Double) obj).doubleValue();
			} else if (obj instanceof Float) {
				return ((Float) obj).doubleValue();
			} else if (obj instanceof Long) {
				return ((Long) obj).doubleValue();
			} else if (obj instanceof Integer) {
				return ((Integer) obj).doubleValue();
			} else if (obj instanceof Short) {
				return ((Short) obj).doubleValue();
			} else if (obj instanceof Byte) {
				return ((Byte) obj).doubleValue();
			}
		}
		return 0;
	}

	// /**
	// * if name is not exists or not convertible object, then return
	// defaultValue.
	// * @param name
	// * @param defaultValue
	// * @return the value of <code>name</code>
	// * @see #getDouble(String)
	// */
	// public final double getDouble(final String name, final double
	// defaultValue){
	// final Object obj = body_table.get(name);
	// if(obj != null){
	// if(obj instanceof Double){
	// return ((Double)obj).doubleValue();
	// }else if(obj instanceof Float){
	// return ((Float)obj).doubleValue();
	// }else if(obj instanceof Long){
	// return ((Long)obj).doubleValue();
	// }else if(obj instanceof Integer){
	// return ((Integer)obj).doubleValue();
	// }else if(obj instanceof Short){
	// return ((Short)obj).doubleValue();
	// }else if(obj instanceof Byte){
	// return ((Byte)obj).doubleValue();
	// }
	// }
	//
	// return defaultValue;
	// }

	/**
	 * if name is not exists, return null; if the map of name is not String
	 * object, return obj.toString()
	 * 
	 * @param name
	 * @return
	 */
	public final String getString(final String name) {
		final Object obj = table.get(name);
		if (obj != null) {
			if (obj instanceof String) {
				return (String) obj;
			} else {
				return obj.toString();
			}
		}
		return null;
	}

	// /**
	// * if name is not exists, then return defaultValue; if the map of name is
	// not String object, return obj.toString()
	// * @param name
	// * @param defaultValue
	// * @return the value of <code>name</code>
	// * @see #getString(String)
	// */
	// public final String getString(final String name, final String
	// defaultValue){
	// final Object obj = body_table.get(name);
	// if(obj != null){
	// if(obj instanceof String){
	// return (String)obj;
	// }else{
	// return obj.toString();
	// }
	// }else{
	// return defaultValue;
	// }
	// }

	// /**
	// * returns the value to which the specified name is mapped, or null if
	// this map contains no mapping for the name.
	// * @param name
	// * @return
	// * @see #getObject(String, Object)
	// */
	// public final Object getObject(final String name){
	// return body_table.get(name);
	// }

	// /**
	// * if name is not exists, then return defaultValue.
	// * @param name
	// * @param defaultValue
	// * @return the value of <code>name</code>
	// * @see #getObject(String)
	// */
	// public final Object getObject(final String name, final Object
	// defaultValue){
	// final Object obj = body_table.get(name);
	// if(obj != null){
	// return obj;
	// }else{
	// return defaultValue;
	// }
	// }

	/**
	 * remove name.
	 * 
	 * @param name
	 */
	public final void remove(final String name) {
		table.remove(name);
	}

	// /**
	// * return a set of all names.
	// * @return
	// */
	// public final Set<String> getBodyNames(){
	// return body_table.keySet();
	// }

	/**
	 * set name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setBoolean(final String name, final boolean value) {
		table.put(name, Boolean.valueOf(value));
	}

	/**
	 * set name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setChar(final String name, final char value) {
		table.put(name, Character.valueOf(value));
	}

	/**
	 * set name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setByte(final String name, final byte value) {
		table.put(name, Byte.valueOf(value));
	}

	/**
	 * set <code>value</code> for the <code>name</code>.<BR>
	 * to copy to new byte array , please invoke
	 * {@link #setByteArray(String, byte[], int, int)}.
	 * 
	 * @param name
	 * @param value
	 *            is NOT copied.
	 */
	public final void setByteArray(final String name, final byte[] value) {
		table.put(name, value);
	}

	/**
	 * set name with new bytes array, which copy values from bs.
	 * 
	 * @param name
	 * @param bs
	 *            is copied to new byte array.
	 * @param offset
	 * @param length
	 * @see #setByteArray(String, byte[])
	 */
	public final void setByteArray(final String name, final byte[] bs, final int offset,
			final int length) {
		final byte[] outbs = new byte[length];
		System.arraycopy(bs, offset, outbs, 0, length);
		table.put(name, outbs);
	}

	/**
	 * set name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setShort(final String name, final short value) {
		table.put(name, Short.valueOf(value));
	}

	/**
	 * set name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setInt(final String name, final int value) {
		table.put(name, Integer.valueOf(value));
	}

	/**
	 * set name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setLong(final String name, final long value) {
		table.put(name, Long.valueOf(value));
	}

	/**
	 * set name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setFloat(final String name, final float value) {
		table.put(name, Float.valueOf(value));
	}

	/**
	 * set name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setDouble(final String name, final double value) {
		table.put(name, Double.valueOf(value));
	}

	/**
	 * set name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setString(final String name, final String value) {
		table.put(name, value);
	}

	/**
	 * set <code>value</code> for the <code>name</code>.<BR>
	 * to copy to new short array , please invoke
	 * {@link #setShortArray(String, short[], int, int)}.
	 * 
	 * @param name
	 * @param value
	 *            is NOT copied.
	 */
	public final void setShortArray(final String name, final short[] value) {
		table.put(name, value);
	}

	/**
	 * set name with new short array, which copy values from bs.
	 * 
	 * @param name
	 * @param bs
	 *            is copied to new short array.
	 * @param offset
	 * @param length
	 * @see #setShortArray(String, short[])
	 */
	public final void setShortArray(final String name, final short[] bs, final int offset,
			final int length) {
		final short[] outbs = new short[length];
		System.arraycopy(bs, offset, outbs, 0, length);
		table.put(name, outbs);
	}

	/**
	 * set <code>value</code> for the <code>name</code>.<BR>
	 * to copy to new int array , please invoke
	 * {@link #setIntArray(String, int[], int, int)}.
	 * 
	 * @param name
	 * @param value
	 *            is NOT copied.
	 */
	public final void setIntArray(final String name, final int[] value) {
		table.put(name, value);
	}

	/**
	 * set name with new int array, which copy values from bs.
	 * 
	 * @param name
	 * @param bs
	 *            is copied to new int array.
	 * @param offset
	 * @param length
	 * @see #setIntArray(String, int[])
	 */
	public final void setIntArray(final String name, final int[] bs, final int offset,
			final int length) {
		final int[] outbs = new int[length];
		System.arraycopy(bs, offset, outbs, 0, length);
		table.put(name, outbs);
	}

	/**
	 * set <code>value</code> for the <code>name</code>.<BR>
	 * to copy to new float array , please invoke
	 * {@link #setFloatArray(String, float[], int, int)}.
	 * 
	 * @param name
	 * @param value
	 *            is NOT copied.
	 */
	public final void setFloatArray(final String name, final float[] value) {
		table.put(name, value);
	}

	/**
	 * set name with new float array, which copy values from bs.
	 * 
	 * @param name
	 * @param bs
	 *            is copied to new float array.
	 * @param offset
	 * @param length
	 * @see #setFloatArray(String, float[])
	 */
	public final void setFloatArray(final String name, final float[] bs, final int offset,
			final int length) {
		final float[] outbs = new float[length];
		System.arraycopy(bs, offset, outbs, 0, length);
		table.put(name, outbs);
	}

	/**
	 * set <code>value</code> for the <code>name</code>.<BR>
	 * to copy to new char array , please invoke
	 * {@link #setCharArray(String, char[], int, int)}.
	 * 
	 * @param name
	 * @param value
	 *            is NOT copied.
	 */
	public final void setCharArray(final String name, final char[] value) {
		table.put(name, value);
	}

	/**
	 * set name with new char array, which copy values from bs.
	 * 
	 * @param name
	 * @param bs
	 *            is copied to new char array.
	 * @param offset
	 * @param length
	 * @see #setCharArray(String, char[])
	 */
	public final void setCharArray(final String name, final char[] bs, final int offset,
			final int length) {
		final char[] outbs = new char[length];
		System.arraycopy(bs, offset, outbs, 0, length);
		table.put(name, outbs);
	}

	/**
	 * set <code>value</code> for the <code>name</code>.<BR>
	 * to copy to new long array , please invoke
	 * {@link #setLongArray(String, long[], int, int)}.
	 * 
	 * @param name
	 * @param value
	 *            is NOT copied.
	 */
	public final void setLongArray(final String name, final long[] value) {
		table.put(name, value);
	}

	/**
	 * set name with new long array, which copy values from bs.
	 * 
	 * @param name
	 * @param bs
	 *            is copied to new long array.
	 * @param offset
	 * @param length
	 * @see #setLongArray(String, long[])
	 */
	public final void setLongArray(final String name, final long[] bs, final int offset,
			final int length) {
		final long[] outbs = new long[length];
		System.arraycopy(bs, offset, outbs, 0, length);
		table.put(name, outbs);
	}

	/**
	 * set <code>value</code> for the <code>name</code>.<BR>
	 * to copy to new double array , please invoke
	 * {@link #setDoubleArray(String, double[], int, int)}.
	 * 
	 * @param name
	 * @param value
	 *            is NOT copied.
	 */
	public final void setDoubleArray(final String name, final double[] value) {
		table.put(name, value);
	}

	/**
	 * set name with new double array, which copy values from bs.
	 * 
	 * @param name
	 * @param bs
	 *            is copied to new double array.
	 * @param offset
	 * @param length
	 * @see #setDoubleArray(String, double[])
	 */
	public final void setDoubleArray(final String name, final double[] bs, final int offset,
			final int length) {
		final double[] outbs = new double[length];
		System.arraycopy(bs, offset, outbs, 0, length);
		table.put(name, outbs);
	}

	/**
	 * set <code>value</code> for the <code>name</code>.<BR>
	 * to copy to new String array , please invoke
	 * {@link #setStringArray(String, String[], int, int)}.
	 * 
	 * @param name
	 * @param value
	 *            is NOT copied.
	 */
	public final void setStringArray(final String name, final String[] value) {
		table.put(name, value);
	}

	/**
	 * set name with new String array, which copy values from bs.
	 * 
	 * @param name
	 * @param bs
	 *            is copied to new String array.
	 * @param offset
	 * @param length
	 * @see #setStringArray(String, String[])
	 */
	public final void setStringArray(final String name, final String[] bs, final int offset,
			final int length) {
		final String[] outbs = new String[length];
		System.arraycopy(bs, offset, outbs, 0, length);
		table.put(name, outbs);
	}

	/**
	 * set <code>value</code> for the <code>name</code>.<BR>
	 * to copy to new boolean array , please invoke
	 * {@link #setBooleanArray(String, boolean[], int, int)}.
	 * 
	 * @param name
	 * @param value
	 *            is NOT copied.
	 */
	public final void setBooleanArray(final String name, final boolean[] value) {
		table.put(name, value);
	}

	/**
	 * set name with new boolean array, which copy values from bs.
	 * 
	 * @param name
	 * @param bs
	 *            is copied to new boolean array.
	 * @param offset
	 * @param length
	 * @see #setBooleanArray(String, boolean[])
	 */
	public final void setBooleanArray(final String name, final boolean[] bs, final int offset,
			final int length) {
		final boolean[] outbs = new boolean[length];
		System.arraycopy(bs, offset, outbs, 0, length);
		table.put(name, outbs);
	}

	/**
	 * returns a boolean array by name.
	 * 
	 * @param name
	 * @return null if not boolean array
	 */
	public final boolean[] getBooleanArray(final String name) {
		final Object obj = table.get(name);
		if (obj != null && obj instanceof boolean[]) {
			return (boolean[]) obj;
		}
		return null;
	}

	/**
	 * returns a char array by name.
	 * 
	 * @param name
	 * @return null if not char array
	 */
	public final char[] getCharArray(final String name) {
		final Object obj = table.get(name);
		if (obj != null && obj instanceof char[]) {
			return (char[]) obj;
		}
		return null;
	}

	/**
	 * returns a short array by name.
	 * 
	 * @param name
	 * @return null if not short array
	 */
	public final short[] getShortArray(final String name) {
		final Object obj = table.get(name);
		if (obj != null && obj instanceof short[]) {
			return (short[]) obj;
		}
		return null;
	}

	/**
	 * returns a int array by name.
	 * 
	 * @param name
	 * @return null if not int array
	 */
	public final int[] getIntArray(final String name) {
		final Object obj = table.get(name);
		if (obj != null && obj instanceof int[]) {
			return (int[]) obj;
		}
		return null;
	}

	/**
	 * returns a float array by name.
	 * 
	 * @param name
	 * @return null if not float array
	 */
	public final float[] getFloatArray(final String name) {
		final Object obj = table.get(name);
		if (obj != null && obj instanceof float[]) {
			return (float[]) obj;
		}
		return null;
	}

	/**
	 * returns a long array by name.
	 * 
	 * @param name
	 * @return null if not long array
	 */
	public final long[] getLongArray(final String name) {
		final Object obj = table.get(name);
		if (obj != null && obj instanceof long[]) {
			return (long[]) obj;
		}
		return null;
	}

	/**
	 * returns a double array by name.
	 * 
	 * @param name
	 * @return null if not double array
	 */
	public final double[] getDoubleArray(final String name) {
		final Object obj = table.get(name);
		if (obj != null && obj instanceof double[]) {
			return (double[]) obj;
		}
		return null;
	}

	/**
	 * returns a String array by name.
	 * 
	 * @param name
	 * @return null if not String array
	 */
	public final String[] getStringArray(final String name) {
		final Object obj = table.get(name);
		if (obj != null && obj instanceof String[]) {
			return (String[]) obj;
		}
		return null;
	}

	/**
	 * set name with a sub analysable object.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setAnalysableRobotObject(final String name, final AnalysableRobotParameter value) {
		if(this.level == 0){
			this.level = 1;
		}
		
		if(value.level != 0){
			throw new IllegalArgumentException(this.getClass().getSimpleName() + " can't be cycled or referenced by twice or more, maybe you need clone() method.");
		}else{
			value.level = this.level + 1;
		}
		table.put(name, value);
	}

	/**
	 * returns a sub analysable object by name.
	 * @param name
	 * @return
	 */
	public final AnalysableRobotParameter getAnalysableRobotObject(final String name) {
		return (AnalysableRobotParameter) table.get(name);
	}
	
	/**
	 * clone a analysable object.
	 */
	@Override
	public final AnalysableRobotParameter clone(){
		final AnalysableRobotParameter out = new AnalysableRobotParameter();
		final HashMap<String, Object> targetTable = out.table;
		
		final Iterator<String> it = table.keySet().iterator();
		while(it.hasNext()){
			final String key = it.next();
			Object value = table.get(key);
			if(value != null && value instanceof AnalysableRobotParameter){
				value = ((AnalysableRobotParameter)value).clone();
			}
			targetTable.put(key, value);
		}
		
		return out;
	}

}

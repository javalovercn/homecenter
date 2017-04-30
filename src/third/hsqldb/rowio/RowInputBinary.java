/* Copyright (c) 2001-2016, The HSQL Development Group
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the HSQL Development Group nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL HSQL DEVELOPMENT GROUP, HSQLDB.ORG,
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package third.hsqldb.rowio;


import java.io.EOFException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import third.hsqldb.error.Error;
import third.hsqldb.error.ErrorCode;
import third.hsqldb.lib.StringConverter;
import third.hsqldb.map.ValuePool;
import third.hsqldb.types.BinaryData;
import third.hsqldb.types.BlobData;
import third.hsqldb.types.BlobDataID;
import third.hsqldb.types.ClobData;
import third.hsqldb.types.ClobDataID;
import third.hsqldb.types.IntervalMonthData;
import third.hsqldb.types.IntervalSecondData;
import third.hsqldb.types.IntervalType;
import third.hsqldb.types.JavaObjectData;
import third.hsqldb.types.TimeData;
import third.hsqldb.types.TimestampData;
import third.hsqldb.types.Type;
import third.hsqldb.types.Types;


/**
 *  Provides methods for reading the data for a row from a
 *  byte array. The format of data is that used for storage of cached
 *  tables by v.1.6.x databases, apart from strings.
 *
 * @author Bob Preston (sqlbob@users dot sourceforge.net)
 * @author Fred Toussi (fredt@users dot sourceforge.net)
 * @version 2.3.4
 * @since 1.7.0
 */
public class RowInputBinary extends RowInputBase implements RowInputInterface {

    public boolean          ignoreDataErrors;
    private RowOutputBinary out;

    public RowInputBinary() {
        this(64);
    }

    public RowInputBinary(int size) {
        super(size);
    }

    public RowInputBinary(byte[] buf) {
        super(buf);
    }

    /**
     * uses the byte[] buffer from out. At each reset, the buffer is set
     * to the current one for out.
     */
    public RowInputBinary(RowOutputBinary out) {

        super(out.getBuffer());

        this.out = out;
    }

    public void readFully(byte[] b) {

        try {
            super.readFully(b);
        } catch (IOException e) {
            throw Error.error(e, ErrorCode.GENERAL_IO_ERROR, "RowInputBinary");
        }
    }

    public long readLong() {

        try {
            return super.readLong();
        } catch (IOException e) {
            throw Error.error(e, ErrorCode.GENERAL_IO_ERROR, "RowInputBinary");
        }
    }

    public int readInt() {

        try {
            return super.readInt();
        } catch (IOException e) {
            throw Error.error(e, ErrorCode.GENERAL_IO_ERROR, "RowInputBinary");
        }
    }

    public short readShort() {

        try {
            return super.readShort();
        } catch (IOException e) {
            throw Error.error(e, ErrorCode.GENERAL_IO_ERROR, "RowInputBinary");
        }
    }

    public char readChar() {

        try {
            return super.readChar();
        } catch (IOException e) {
            throw Error.error(e, ErrorCode.GENERAL_IO_ERROR, "RowInputBinary");
        }
    }

    public byte readByte() {

        try {
            return super.readByte();
        } catch (IOException e) {
            throw Error.error(e, ErrorCode.GENERAL_IO_ERROR, "RowInputBinary");
        }
    }

    public boolean readBoolean() {

        try {
            return super.readBoolean();
        } catch (IOException e) {
            throw Error.error(e, ErrorCode.GENERAL_IO_ERROR, "RowInputBinary");
        }
    }

    public int readType() {
        return readShort();
    }

    public String readString() {

        try {
            int length = readInt();

            if (length < 0) {
                throw Error.error(ErrorCode.GENERAL_IO_ERROR,
                                  "RowInputBinary - negative length");
            }

            String s = StringConverter.readUTF(buffer, pos, length);

            s   = ValuePool.getString(s);
            pos += length;

            return s;
        } catch (IOException e) {
            throw Error.error(e, ErrorCode.GENERAL_IO_ERROR, "RowInputBinary");
        }
    }

    public boolean readNull() {

        int b = readByte();

        return b == 0;
    }

    protected String readChar(Type type) {
        return readString();
    }

    protected Integer readSmallint() {
        return ValuePool.getInt(readShort());
    }

    protected Integer readInteger() {
        return ValuePool.getInt(readInt());
    }

    protected Long readBigint() {
        return ValuePool.getLong(readLong());
    }

    protected Double readReal() {
        return ValuePool.getDouble(readLong());
    }

    protected BigDecimal readDecimal(Type type) {

        byte[]     bytes  = readByteArray();
        int        scale  = readInt();
        BigInteger bigint = new BigInteger(bytes);

        return ValuePool.getBigDecimal(new BigDecimal(bigint, scale));
    }

    protected Boolean readBoole() {
        return readBoolean() ? Boolean.TRUE
                             : Boolean.FALSE;
    }

    protected TimeData readTime(Type type) {

        if (type.typeCode == Types.SQL_TIME) {
            return new TimeData(readInt(), readInt(), 0);
        } else {
            return new TimeData(readInt(), readInt(), readInt());
        }
    }

    protected TimestampData readDate(Type type) {

        long date = readLong();

        return new TimestampData(date);
    }

    protected TimestampData readTimestamp(Type type) {

        if (type.typeCode == Types.SQL_TIMESTAMP) {
            return new TimestampData(readLong(), readInt());
        } else {
            return new TimestampData(readLong(), readInt(), readInt());
        }
    }

    protected IntervalMonthData readYearMonthInterval(Type type) {

        long months = readLong();

        return new IntervalMonthData(months, (IntervalType) type);
    }

    protected IntervalSecondData readDaySecondInterval(Type type) {

        long seconds = readLong();
        int  nanos   = readInt();

        return new IntervalSecondData(seconds, nanos, (IntervalType) type);
    }

    protected Object readOther() {
        return new JavaObjectData(readByteArray());
    }

    protected BinaryData readBit() {

        int    length = readInt();
        byte[] b      = new byte[(length + 7) / 8];

        readFully(b);

        return BinaryData.getBitData(b, length);
    }

    protected BinaryData readBinary() {
        return new BinaryData(readByteArray(), false);
    }

    protected ClobData readClob() {

        long id = readLong();

        return new ClobDataID(id);
    }

    protected BlobData readBlob() {

        long id = readLong();

        return new BlobDataID(id);
    }

    protected Object[] readArray(Type type) {

        type = type.collectionBaseType();

        int      size = readInt();
        Object[] data = new Object[size];

        for (int i = 0; i < size; i++) {
            data[i] = readData(type);
        }

        return data;
    }

    /**
     * Nulls in array are treated as 0
     */
    public int[] readIntArray() throws IOException {

        int   size = readInt();
        int[] data = new int[size];

        for (int i = 0; i < size; i++) {
            if (!readNull()) {
                data[i] = readInt();
            }
        }

        return data;
    }

    public Object[] readData(Type[] colTypes) {

        if (ignoreDataErrors) {
            return new Object[colTypes.length];
        }

        return super.readData(colTypes);
    }

    // helper methods
    public byte[] readByteArray() {

        int    length = readInt();
        byte[] b      = new byte[length];

        readFully(b);

        return b;
    }

    public char[] readCharArray() throws IOException {

        int    length = readInt();
        char[] c      = new char[length];

        if (count - pos < c.length) {
            pos = count;

            throw new EOFException();
        }

        for (int i = 0; i < c.length; i++) {
            int ch1 = buffer[pos++] & 0xff;
            int ch2 = buffer[pos++] & 0xff;

            c[i] = (char) ((ch1 << 8) + (ch2));
        }

        return c;
    }

    /**
     *  Used to reset the row, ready for Result data to be written into the
     *  byte[] buffer by an external routine.
     *
     */
    public void resetRow(int rowsize) {

        if (out != null) {
            out.reset(rowsize);

            buffer = out.getBuffer();
        }

        super.reset();
    }

    /**
     *  Used to reset the row, ready for a new db row to be written into the
     *  byte[] buffer by an external routine.
     *
     */
    public void resetRow(long filepos, int rowsize) {

        if (out != null) {
            out.reset(rowsize);

            buffer = out.getBuffer();
        }

        super.resetRow(filepos, rowsize);
    }
}

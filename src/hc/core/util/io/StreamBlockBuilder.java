package hc.core.util.io;

import hc.core.util.ByteUtil;
import hc.core.util.StringUtil;
import java.io.IOException;
import java.io.InputStream;

public class StreamBlockBuilder {
	public static final String HEADER_SPLITTER = "=";

	public static final String HEADER_FILE_EXTSION = "fileExtension";
	public static final String HEADER_FILE_NAME = "fileName";
	public static final String HEADER_AUDIO_FORMAT = "audioFormat";
	public static final String HEADER_IMAGE_FORMAT = "imageFormat";
	public static final String HEADER_IMAGE_AZIMUTH = "imageAzimuth";
	public static final String HEADER_IMAGE_PITCH = "imagePitch";
	public static final String HEADER_IMAGE_ROLL = "imageRoll";
	public static final String HEADER_IMAGE_LATITUDE = "imageLatitude";
	public static final String HEADER_IMAGE_LONGITUDE = "imageLongitude";
	public static final String HEADER_IMAGE_ALTITUDE = "imageAltitude";

	public static final String HEADER_TAG_CLOSE = "_Hc_Close_Stream";
	public static final String HEADER_TAG_STREAM_CONTENT = "_Hc_Stream_Content";

	public static final byte[] BS_HEADER_FILE_EXTSION = HEADER_FILE_EXTSION
			.getBytes();
	public static final byte[] BS_HEADER_FILE_NAME = HEADER_FILE_NAME
			.getBytes();
	public static final byte[] BS_HEADER_AUDIO_FORMAT = HEADER_AUDIO_FORMAT
			.getBytes();
	public static final byte[] BS_HEADER_IMAGE_FORMAT = HEADER_IMAGE_FORMAT
			.getBytes();
	public static final byte[] BS_HEADER_IMAGE_AZIMUTH = HEADER_IMAGE_AZIMUTH
			.getBytes();
	public static final byte[] BS_HEADER_IMAGE_PITCH = HEADER_IMAGE_PITCH
			.getBytes();
	public static final byte[] BS_HEADER_IMAGE_ROLL = HEADER_IMAGE_ROLL
			.getBytes();
	public static final byte[] BS_HEADER_IMAGE_LATITUDE = HEADER_IMAGE_LATITUDE
			.getBytes();
	public static final byte[] BS_HEADER_IMAGE_LONGITUDE = HEADER_IMAGE_LONGITUDE
			.getBytes();
	public static final byte[] BS_HEADER_IMAGE_ALTITUDE = HEADER_IMAGE_ALTITUDE
			.getBytes();

	private static final byte[] BS_HEADER_CLOSE = HEADER_TAG_CLOSE.getBytes();
	public static final byte[] BS_STREAM_CONTENT = HEADER_TAG_STREAM_CONTENT
			.getBytes();

	private static final byte[] BS_HEADER_SPLITTER = HEADER_SPLITTER.getBytes();

	private static final String[] HEAD_TAGS = { HEADER_TAG_CLOSE,
			HEADER_TAG_STREAM_CONTENT };
	private static final byte[][] HEAD_TAGS_BS = { BS_HEADER_CLOSE,
			BS_STREAM_CONTENT };

	public static final int FOUR_BYTE_LEN = 4;
	public static final int ERR_LEN = -1;

	/**
	 * [four byte len][STREAM_CONTENT][four byte len][data content]
	 * 
	 * @param iobuffer
	 * @param bs
	 * @param offset
	 * @param len
	 * @param lengthBS
	 * @return added length of bytes.
	 */
	public static void addDataBlock(final HCOutputStream osSnap,
			final IOBuffer iobuffer, final IOBuffer iodata,
			final byte[] lengthBS) throws IOException {
		ByteUtil.integerToFourBytes(BS_STREAM_CONTENT.length, lengthBS, 0);
		iobuffer.write(lengthBS, 0, FOUR_BYTE_LEN);
		iobuffer.write(BS_STREAM_CONTENT);

		final int len = iodata.storeIdx;

		if (len > 0) {
			ByteUtil.integerToFourBytes(len, lengthBS, 0);
			iobuffer.write(lengthBS, 0, FOUR_BYTE_LEN);

			iobuffer.write(iodata.buffer, 0, len);
		} else {
			final IOBufferFileStream fs = iodata.stream;
			final int streamLength = fs.size;
			ByteUtil.integerToFourBytes(streamLength, lengthBS, 0);
			iobuffer.write(lengthBS, 0, FOUR_BYTE_LEN);

			osSnap.write(iobuffer.buffer, 0, iobuffer.storeIdx);
			osSnap.flush();
			iobuffer.reset();

			try {
				final InputStream is = fs.is;
				int dataStreamReadLen = 0;
				while ((dataStreamReadLen = is.read(iobuffer.buffer)) > 0) {
					osSnap.write(iobuffer.buffer, 0, dataStreamReadLen);
				}
				osSnap.flush();
				is.close();
			} catch (Throwable e) {
				e.printStackTrace();
			}
			iobuffer.reset();
		}
	}

	public static byte[] getFreeLengthBS() {
		return ByteUtil.byteArrayCacher.getFree(FOUR_BYTE_LEN);
	}

	public static void addHeaderKeyBlock(final IOBuffer iobuffer,
			final byte[] keyBS, final byte[] valueBS, final byte[] lengthBS)
			throws IOException {
		final int len = keyBS.length + BS_HEADER_SPLITTER.length
				+ valueBS.length;
		ByteUtil.integerToFourBytes(len, lengthBS, 0);

		iobuffer.write(lengthBS, 0, FOUR_BYTE_LEN);
		iobuffer.write(keyBS);
		iobuffer.write(BS_HEADER_SPLITTER);
		iobuffer.write(valueBS);
	}

	public static void addCloseHeader(final IOBuffer iobuffer,
			final byte[] lengthBS) throws IOException {
		ByteUtil.integerToFourBytes(BS_HEADER_CLOSE.length, lengthBS, 0);
		iobuffer.write(lengthBS, 0, FOUR_BYTE_LEN);
		iobuffer.write(BS_HEADER_CLOSE);
	}

	/**
	 * -1 means no block or error
	 * 
	 * @param is
	 * @return
	 */
	public static int readBlockLength(final HCInputStream is,
			final byte[] lengthBS) throws IOException {
		is.readFully(lengthBS, 0, FOUR_BYTE_LEN);
		return (int) ByteUtil.fourBytesToLong(lengthBS, 0);
	}

	/**
	 * null means error.
	 * 
	 * @param is
	 * @param lengthBS
	 * @return
	 */
	public static String readHeaderBlock(final HCInputStream is,
			final byte[] lengthBS) throws IOException {
		final int len = readBlockLength(is, lengthBS);

		final byte[] dataBS = ByteUtil.byteArrayCacher.getFree(len);
		is.readFully(dataBS, 0, len);

		for (int i = 0; i < HEAD_TAGS_BS.length; i++) {
			final byte[] headBS = HEAD_TAGS_BS[i];

			if (len == headBS.length) {
				boolean isSame = true;
				for (int j = 0; j < headBS.length; j++) {
					if (headBS[j] != dataBS[j]) {
						isSame = false;
						break;
					}
				}

				if (isSame) {
					return HEAD_TAGS[i];
				}
			}
		}

		final String result = StringUtil.bytesToString(dataBS, 0, len);
		ByteUtil.byteArrayCacher.cycle(dataBS);
		return result;
	}
}

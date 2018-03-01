package hc.server.ui;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;

import hc.core.IConstant;
import hc.core.L;
import hc.core.util.BooleanValue;
import hc.core.util.ByteUtil;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.core.util.StringUtil;
import hc.core.util.io.HCInputStream;
import hc.core.util.io.IOBuffer;
import hc.core.util.io.StreamBlockBuilder;
import hc.core.util.io.StreamBuilder;
import hc.server.data.StoreDirManager;
import hc.server.ui.design.HCPermissionConstant;
import hc.server.ui.design.J2SESession;
import hc.server.util.HCAudioInputStream;
import hc.server.util.HCFileInputStream;
import hc.server.util.HCImageInputStream;
import hc.server.util.HCInputStreamBuilder;
import hc.util.ResourceUtil;
import hc.util.StringBuilderCacher;
import hc.util.ThreadConfig;

/**
 * a {@link ClientSession} instance will be created before
 * {@link ProjectContext#EVENT_SYS_MOBILE_LOGIN}, and will be released by server
 * after {@link ProjectContext#EVENT_SYS_MOBILE_LOGOUT}. <BR>
 * <BR>
 * invoking {@link ProjectContext#getClientSession()} to get this instance for
 * current session. <BR>
 * <BR>
 * it is thread safe for attribute set/get.
 * 
 * @since 7.7
 */
public final class ClientSession {
	private final Hashtable<String, Object> attribute_map = new Hashtable<String, Object>();
	final J2SESession j2seCoreSSMaybeNull;
	final boolean hasLocationOfMobile;
	Thread waitThread;
	boolean isLogout;

	ClientSession(final J2SESession j2seCoreSS, final boolean hasLocationOfMobile) {
		this.j2seCoreSSMaybeNull = j2seCoreSS;// 在Simu下传入null
		if (j2seCoreSSMaybeNull != null) {
			j2seCoreSSMaybeNull.clientSession = this;
		}
		this.hasLocationOfMobile = hasLocationOfMobile;
	}

	/**
	 * 其它工程或线程也需此功能，故锁session level
	 * 
	 * @param ctx
	 */
	final void notifyInputMemberID(final ProjectContext ctx) {
		ServerUIUtil.checkLineOnForAPI(j2seCoreSSMaybeNull);

		final Object threadWait = new Object();

		final BooleanValue memberIDSetStatus = j2seCoreSSMaybeNull.memberIDSetStatus;
		synchronized (memberIDSetStatus) {// 多工程或多线程间同步
			if (memberIDSetStatus.value) {
				return;
			}

			if (j2seCoreSSMaybeNull.addWaitLock(threadWait) == false) {
				return;
			}

			final MemberIDInputDialog inputMemberIDDialog = new MemberIDInputDialog(
					memberIDSetStatus, threadWait);
			ctx.sendDialogWhenInSession(inputMemberIDDialog);

			synchronized (threadWait) {
				try {
					threadWait.wait();
				} catch (final InterruptedException e) {
				}
			}

		}

		j2seCoreSSMaybeNull.removeWaitLock(threadWait);
		ServerUIUtil.checkLineOnForAPI(j2seCoreSSMaybeNull);
	}

	/**
	 * returns the last known latitude.<BR>
	 * <BR>
	 * project permission [<STRONG>location of mobile</STRONG>] is required.
	 * <BR>
	 * <BR>
	 * if no permission (project or mobile) or no GPS signal, then return -1.0;
	 * <BR>
	 * <BR>
	 * to set location updates frequency, see
	 * {@link ProjectContext#setLocationUpdates(long)}.
	 * 
	 * @return
	 * @see #getLocationLongitude()
	 * @see #isWithoutLocation()
	 * @see ProjectContext#EVENT_SYS_MOBILE_LOCATION
	 * @see ProjectContext#addSystemEventListener(hc.server.util.SystemEventListener)
	 * @see #getLocationAltitude()
	 * @see #getLocationCourse()
	 * @see #getLocationSpeed()
	 */
	public final double getLocationLatitude() {// 纬度
		if (hasLocationOfMobile == false) {
			LogManager.err(HCPermissionConstant.NO_PERMISSION_OF_LOCATION_OF_MOBILE);
			return J2SESession.NO_PERMISSION_LOC;
		}

		if (j2seCoreSSMaybeNull == null) {
			return SimuMobile.MOBILE_LATITUDE;
		}
		return j2seCoreSSMaybeNull.location.latitude;
	}

	/**
	 * true means no permission (project or mobile) or no GPS signal.
	 * 
	 * @return
	 * @see #getLocationLatitude()
	 */
	public final boolean isWithoutLocation() {
		return getLocationLatitude() == -1;
	}

	/**
	 * take a photograph from mobile client. <BR>
	 * for image format, see {@link HCImageInputStream#getImageFormat()}. <BR>
	 * <BR>
	 * <STRONG>Know more</STRONG> : <BR>
	 * 1. it will block current thread for result, but not block server
	 * receiving result.<BR>
	 * 2. mobile client will not display alert/question/dialog when taking
	 * photograph.
	 * 
	 * @return null means canceled by user or exception, it returns full stream
	 *         or null, never part. <BR>
	 * 		To get image format, invoke
	 *         {@link HCImageInputStream#getImageFormat()}.
	 * @see #takePhotographWithLocationFromClient()
	 * @since 7.72
	 */
	public final HCImageInputStream takePhotographFromClient() {
		return takePhotographFromClientImpl(false);
	}

	/**
	 * take a photograph with GPS location from mobile client. <BR>
	 * <BR>
	 * to get location, see {@link HCImageInputStream#getImageLongitude()}. <BR>
	 * project permission [location of mobile] is <STRONG>NOT</STRONG> required.
	 * <BR>
	 * <BR>
	 * for image format, see {@link HCImageInputStream#getImageFormat()}. <BR>
	 * <BR>
	 * <STRONG>Know more</STRONG> : <BR>
	 * 1. it will block current thread for result, but not block server
	 * receiving result.<BR>
	 * 2. mobile client will not display alert/question/dialog when taking
	 * photograph.
	 * 
	 * @return null means canceled by user or exception, it returns full stream
	 *         or null, never part. <BR>
	 * 		To get image format, invoke
	 *         {@link HCImageInputStream#getImageFormat()}.
	 * @see #takePhotographFromClient()
	 * @since 7.72
	 */
	public final HCImageInputStream takePhotographWithLocationFromClient() {
		return takePhotographFromClientImpl(true);
	}

	private final HCImageInputStream takePhotographFromClientImpl(final boolean isRequireLocation) {
		if (j2seCoreSSMaybeNull == null) {
			return SimuMobile.buildMobileImageStream();
		}

		ServerUIUtil.checkLineOnForAPI(j2seCoreSSMaybeNull);

		final StreamBuilder streamBuilder = new StreamBuilder(j2seCoreSSMaybeNull);

		final hc.core.util.io.HCInputStream baseIS = (hc.core.util.io.HCInputStream) ServerUIAPIAgent
				.runAndWaitInSysThread(new ReturnableRunnable() {
					@Override
					public Object run() throws Throwable {
						final StringBuilder sb = StringBuilderCacher.getFree();

						sb.append(IConstant.toString(isRequireLocation));
						sb.append(HCURL.FILE_EXTENSION_SPLITTER);
						sb.append(IConstant.toString(isFromMobileDialog()));

						final byte[] bs = sb.toString().getBytes();
						StringBuilderCacher.cycle(sb);
						return streamBuilder.buildInputStream(StreamBuilder.S_CLASS_TRANS_IMAGE, bs,
								0, bs.length, true);
					}
				});

		final byte[] lengthBS = StreamBlockBuilder.getFreeLengthBS();

		try {
			do {
				String block = StreamBlockBuilder.readHeaderBlock(baseIS, lengthBS);
				if (StreamBlockBuilder.HEADER_TAG_CLOSE.equals(block)) {
					break;
				}
				String imageFormat = "";
				float imageAzimuth = 0;
				float imagePitch = 0;
				float imageRoll = 0;
				double imageLatitude = -1.0;
				double imageLongitude = -1.0;
				double imageAltitude = -1.0;

				{
					final String[] keys = StringUtil.splitToArray(block,
							StreamBlockBuilder.HEADER_SPLITTER);
					if (StreamBlockBuilder.HEADER_IMAGE_FORMAT.equals(keys[0])) {
						imageFormat = keys[1];
					}
				}
				{
					block = StreamBlockBuilder.readHeaderBlock(baseIS, lengthBS);
					final String[] keys = StringUtil.splitToArray(block,
							StreamBlockBuilder.HEADER_SPLITTER);
					if (StreamBlockBuilder.HEADER_IMAGE_AZIMUTH.equals(keys[0])) {
						try {
							imageAzimuth = Float.parseFloat(keys[1]);
						} catch (final Exception e) {
							e.printStackTrace();
						}
					}
				}
				{
					block = StreamBlockBuilder.readHeaderBlock(baseIS, lengthBS);
					final String[] keys = StringUtil.splitToArray(block,
							StreamBlockBuilder.HEADER_SPLITTER);
					if (StreamBlockBuilder.HEADER_IMAGE_PITCH.equals(keys[0])) {
						try {
							imagePitch = Float.parseFloat(keys[1]);
						} catch (final Exception e) {
							e.printStackTrace();
						}
					}
				}
				{
					block = StreamBlockBuilder.readHeaderBlock(baseIS, lengthBS);
					final String[] keys = StringUtil.splitToArray(block,
							StreamBlockBuilder.HEADER_SPLITTER);
					if (StreamBlockBuilder.HEADER_IMAGE_ROLL.equals(keys[0])) {
						try {
							imageRoll = Float.parseFloat(keys[1]);
						} catch (final Exception e) {
							e.printStackTrace();
						}
					}
				}
				{
					block = StreamBlockBuilder.readHeaderBlock(baseIS, lengthBS);
					final String[] keys = StringUtil.splitToArray(block,
							StreamBlockBuilder.HEADER_SPLITTER);
					if (StreamBlockBuilder.HEADER_IMAGE_LATITUDE.equals(keys[0])) {
						try {
							imageLatitude = Double.parseDouble(keys[1]);
						} catch (final Exception e) {
							e.printStackTrace();
						}
					}
				}
				{
					block = StreamBlockBuilder.readHeaderBlock(baseIS, lengthBS);
					final String[] keys = StringUtil.splitToArray(block,
							StreamBlockBuilder.HEADER_SPLITTER);
					if (StreamBlockBuilder.HEADER_IMAGE_LONGITUDE.equals(keys[0])) {
						try {
							imageLongitude = Double.parseDouble(keys[1]);
						} catch (final Exception e) {
							e.printStackTrace();
						}
					}
				}
				{
					block = StreamBlockBuilder.readHeaderBlock(baseIS, lengthBS);
					final String[] keys = StringUtil.splitToArray(block,
							StreamBlockBuilder.HEADER_SPLITTER);
					if (StreamBlockBuilder.HEADER_IMAGE_ALTITUDE.equals(keys[0])) {
						try {
							imageAltitude = Double.parseDouble(keys[1]);
						} catch (final Exception e) {
							e.printStackTrace();
						}
					}
				}

				block = StreamBlockBuilder.readHeaderBlock(baseIS, lengthBS);
				if (StreamBlockBuilder.HEADER_TAG_STREAM_CONTENT.equals(block)) {
					final InputStream bis = buildISFromStream(baseIS, lengthBS);
					return HCInputStreamBuilder.buildImageStream(bis, imageAzimuth, imagePitch,
							imageRoll, imageFormat, imageLatitude, imageLongitude, imageAltitude);
				}
			} while (false);
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			ByteUtil.byteArrayCacher.cycle(lengthBS);
			try {
				baseIS.close();
			} catch (final Exception e) {
			}
		}
		ServerUIUtil.checkLineOnForAPI(j2seCoreSSMaybeNull);
		return null;
	}

	private final InputStream buildISFromStream(final HCInputStream baseIS, final byte[] lengthBS)
			throws IOException {
		final int dataLen = StreamBlockBuilder.readBlockLength(baseIS, lengthBS);

		if (IOBuffer.isStreamSize(dataLen)) {
			final ProjectContext projectContext = ProjectContext.getProjectContext();
			final File tmp_sub_for_hc_sys = StoreDirManager
					.getTmpSubForUserManagedByHcSys(projectContext);
			final File tempFile = ResourceUtil.createRandomFileWithExt(tmp_sub_for_hc_sys, null);
			final byte[] bs = ByteUtil.byteArrayCacher.getFree(1024 * 200);
			try {
				int writeCount = 0;
				final FileOutputStream fos = new FileOutputStream(tempFile);
				do {
					final int leftLen = dataLen - writeCount;
					final int onetimeLen = leftLen > bs.length ? bs.length : leftLen;
					baseIS.readFully(bs, 0, onetimeLen);

					fos.write(bs, 0, onetimeLen);
					writeCount += onetimeLen;
				} while (writeCount < dataLen);
				return new FileInputStream(tempFile);
			} finally {
				ByteUtil.byteArrayCacher.cycle(bs);
			}
		} else {
			final byte[] dataBS = new byte[dataLen];
			baseIS.readFully(dataBS, 0, dataLen);

			return new ByteArrayInputStream(dataBS, 0, dataLen);
		}
	}

	/**
	 * record a audio from mobile client. <BR>
	 * <BR>
	 * for audio format, see {@link HCAudioInputStream#getAudioFormat()}.<BR>
	 * 1. Android client, the audio format is <code>3gp</code>.<BR>
	 * 2. iOS client, the audio format is <code>m4a</code>. <BR>
	 * <BR>
	 * <STRONG>Know more</STRONG> : <BR>
	 * 1. it will block current thread for result, but not block server
	 * receiving result.<BR>
	 * 2. mobile client will not display alert/question/dialog when recording
	 * audio.
	 * 
	 * @return null means canceled by user or exception, it returns full stream
	 *         or null, never part. <BR>
	 * 		To get audio format, use
	 *         {@link HCAudioInputStream#getAudioFormat()}.
	 * @see #recordAudioWithLocationFromClient()
	 * @since 7.72
	 */
	public final HCAudioInputStream recordAudioFromClient() {
		return recordAudioFromClientImpl(false);
	}

	/**
	 * record a audio with GPS location from mobile client. <BR>
	 * <BR>
	 * to get location, see {@link HCAudioInputStream#getAudioLongitude()}. <BR>
	 * project permission [location of mobile] is <STRONG>NOT</STRONG> required.
	 * <BR>
	 * <BR>
	 * for audio format, see {@link HCAudioInputStream#getAudioFormat()}.<BR>
	 * 1. Android client, the audio format is <code>3gp</code>.<BR>
	 * 2. iOS client, the audio format is <code>m4a</code>. <BR>
	 * <BR>
	 * <STRONG>Know more</STRONG> : <BR>
	 * 1. it will block current thread for result, but not block server
	 * receiving result.<BR>
	 * 2. mobile client will not display alert/question/dialog when recording
	 * audio.
	 * 
	 * @return null means canceled by user or exception, it returns full stream
	 *         or null, never part.
	 * @see #recordAudioFromClient()
	 * @since 7.72
	 */
	public final HCAudioInputStream recordAudioWithLocationFromClient() {
		return recordAudioFromClientImpl(true);
	}

	private final boolean isFromMobileDialog() {
		if (j2seCoreSSMaybeNull == null) {
			return false;
		} else {
			return j2seCoreSSMaybeNull.uiEventInput.isDialogEvent();
		}
	}

	private final HCAudioInputStream recordAudioFromClientImpl(final boolean isRequireLocation) {
		if (j2seCoreSSMaybeNull == null) {
			return SimuMobile.buildMobileAudioStream();
		}

		ServerUIUtil.checkLineOnForAPI(j2seCoreSSMaybeNull);

		final StreamBuilder streamBuilder = new StreamBuilder(j2seCoreSSMaybeNull);

		final hc.core.util.io.HCInputStream baseIS = (hc.core.util.io.HCInputStream) ServerUIAPIAgent
				.runAndWaitInSysThread(new ReturnableRunnable() {
					@Override
					public Object run() throws Throwable {
						final StringBuilder sb = StringBuilderCacher.getFree();

						sb.append(IConstant.toString(isRequireLocation));
						sb.append(HCURL.FILE_EXTENSION_SPLITTER);
						sb.append(IConstant.toString(isFromMobileDialog()));

						final byte[] bs = sb.toString().getBytes();
						StringBuilderCacher.cycle(sb);
						return streamBuilder.buildInputStream(StreamBuilder.S_CLASS_TRANS_AUDIO, bs,
								0, bs.length, true);
					}
				});

		final byte[] lengthBS = StreamBlockBuilder.getFreeLengthBS();

		try {
			do {
				String block = StreamBlockBuilder.readHeaderBlock(baseIS, lengthBS);
				if (StreamBlockBuilder.HEADER_TAG_CLOSE.equals(block)) {
					break;
				}
				String audioFormat = "";
				double imageLatitude = -1.0;
				double imageLongitude = -1.0;
				double imageAltitude = -1.0;

				{
					final String[] keys = StringUtil.splitToArray(block,
							StreamBlockBuilder.HEADER_SPLITTER);
					if (StreamBlockBuilder.HEADER_AUDIO_FORMAT.equals(keys[0])) {
						audioFormat = keys[1];
						L.V = L.WShop ? false
								: LogManager.log("receive record audio format : " + audioFormat);
					}
				}
				{
					block = StreamBlockBuilder.readHeaderBlock(baseIS, lengthBS);
					final String[] keys = StringUtil.splitToArray(block,
							StreamBlockBuilder.HEADER_SPLITTER);
					if (StreamBlockBuilder.HEADER_IMAGE_LATITUDE.equals(keys[0])) {
						imageLatitude = Double.parseDouble(keys[1]);
						L.V = L.WShop ? false
								: LogManager
										.log("receive record audio latitude : " + imageLatitude);
					}
				}
				{
					block = StreamBlockBuilder.readHeaderBlock(baseIS, lengthBS);
					final String[] keys = StringUtil.splitToArray(block,
							StreamBlockBuilder.HEADER_SPLITTER);
					if (StreamBlockBuilder.HEADER_IMAGE_LONGITUDE.equals(keys[0])) {
						imageLongitude = Double.parseDouble(keys[1]);
						L.V = L.WShop ? false
								: LogManager
										.log("receive record audio longitude : " + imageLongitude);
					}
				}
				{
					block = StreamBlockBuilder.readHeaderBlock(baseIS, lengthBS);
					final String[] keys = StringUtil.splitToArray(block,
							StreamBlockBuilder.HEADER_SPLITTER);
					if (StreamBlockBuilder.HEADER_IMAGE_ALTITUDE.equals(keys[0])) {
						imageAltitude = Double.parseDouble(keys[1]);
						L.V = L.WShop ? false
								: LogManager.log("receive record audio altitude : " + audioFormat);
					}
				}

				block = StreamBlockBuilder.readHeaderBlock(baseIS, lengthBS);
				if (StreamBlockBuilder.HEADER_TAG_STREAM_CONTENT.equals(block)) {
					final InputStream bis = buildISFromStream(baseIS, lengthBS);
					L.V = L.WShop ? false
							: LogManager.log("sucessful receive record audio stream.");
					return HCInputStreamBuilder.buildAudioStream(bis, audioFormat, imageLatitude,
							imageLongitude, imageAltitude);
				}
			} while (false);
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			ByteUtil.byteArrayCacher.cycle(lengthBS);
			try {
				baseIS.close();
			} catch (final Exception e) {
			}
		}
		ServerUIUtil.checkLineOnForAPI(j2seCoreSSMaybeNull);
		return null;
	}

	/**
	 * image file type.
	 * 
	 * @see #getOneFileFromClient(String)
	 * @see #getMultipleFilesFromClient(String)
	 * @see #TYPE_AUDIO
	 * @see #TYPE_VIDEO
	 * @see #TYPE_ALL
	 */
	public static final String TYPE_IMAGE = IConstant.FILE_IMAGE;

	/**
	 * audio file type.
	 * 
	 * @see #getOneFileFromClient(String)
	 * @see #getMultipleFilesFromClient(String)
	 * @see #TYPE_IMAGE
	 * @see #TYPE_VIDEO
	 * @see #TYPE_ALL
	 */
	public static final String TYPE_AUDIO = IConstant.FILE_AUDIO;

	/**
	 * video file type.
	 * 
	 * @see #getOneFileFromClient(String)
	 * @see #getMultipleFilesFromClient(String)
	 * @see #TYPE_IMAGE
	 * @see #TYPE_AUDIO
	 * @see #TYPE_ALL
	 */
	public static final String TYPE_VIDEO = IConstant.FILE_VIDEO;

	/**
	 * any file type.
	 * 
	 * @see #getOneFileFromClient(String)
	 * @see #getMultipleFilesFromClient(String)
	 * @see #TYPE_IMAGE
	 * @see #TYPE_AUDIO
	 * @see #TYPE_VIDEO
	 */
	public static final String TYPE_ALL = IConstant.FILE_ALL;

	/**
	 * open a file from mobile client. <BR>
	 * <BR>
	 * <STRONG>Know more</STRONG> : <BR>
	 * 1. it will block current thread for result, but not block server
	 * receiving result.<BR>
	 * 2. mobile client will not display alert/question/dialog when opening
	 * file.<BR>
	 * 3. in iOS, voice memos can't be accessed.
	 * 
	 * @param type
	 *            file type (recommend, the return file depends on user choice),
	 *            one of {@link #TYPE_IMAGE}, {@link #TYPE_AUDIO},
	 *            {@link #TYPE_VIDEO} and {@link #TYPE_ALL}.
	 * @return null means canceled by user or exception, it returns full stream
	 *         or null, never part. <BR>
	 * 		To get file extension of result, use
	 *         {@link HCFileInputStream#getFileExtension()}.
	 * @see #getOneFileFromClient(String)
	 * @see #openMultipleFilesFromClient(String)
	 * @see #getOneAudioFileMaybeByRecording()
	 * @see #getOneImageFileMaybeByTakingPhotograph()
	 * @see #recordAudioFromClient()
	 * @see #recordAudioWithLocationFromClient()
	 * @see #takePhotographFromClient()
	 * @see #takePhotographWithLocationFromClient()
	 * @since 7.72
	 */
	public final HCFileInputStream openOneFileFromClient(final String type) {
		final Object result = openMultipleFilesFromClient(type, false);
		if (result == null) {
			return null;
		} else if (result instanceof Vector) {
			return (HCFileInputStream) ((Vector) result).elementAt(0);
		} else {
			return (HCFileInputStream) result;
		}
	}

	/**
	 * get a file from mobile client. <BR>
	 * <BR>
	 * it is equals with {@link #openOneFileFromClient(String)}.<BR>
	 * <BR>
	 * <STRONG>Know more</STRONG> : <BR>
	 * 1. it will block current thread for result, but not block server
	 * receiving result.<BR>
	 * 2. mobile client will not display alert/question/dialog when opening
	 * file.<BR>
	 * 3. in iOS, voice memos can't be accessed.
	 * 
	 * @param type
	 *            file type (recommend, the return file depends on user choice),
	 *            one of {@link #TYPE_IMAGE}, {@link #TYPE_AUDIO},
	 *            {@link #TYPE_VIDEO} and {@link #TYPE_ALL}.
	 * @return null means canceled by user or exception, it returns full stream
	 *         or null, never part. <BR>
	 * 		To get file extension of result, use
	 *         {@link HCFileInputStream#getFileExtension()}.
	 * @see #openOneFileFromClient(String)
	 * @see #getOneAudioFileMaybeByRecording()
	 * @see #getOneImageFileMaybeByTakingPhotograph()
	 * @see #recordAudioFromClient()
	 * @see #recordAudioWithLocationFromClient()
	 * @see #takePhotographFromClient()
	 * @see #takePhotographWithLocationFromClient()
	 * @see #openMultipleFilesFromClient(String)
	 * @since 7.72
	 */
	public final HCFileInputStream getOneFileFromClient(final String type) {
		return openOneFileFromClient(type);
	}

	/**
	 * get a audio file from mobile client, user can choose recording or not.
	 * <BR>
	 * <BR>
	 * <STRONG>Know more</STRONG> : <BR>
	 * 1. it will block current thread for result, but not block server
	 * receiving result.<BR>
	 * 2. mobile client will not display alert/question/dialog when opening
	 * file.<BR>
	 * 3. in iOS, voice memos can't be accessed.
	 * 
	 * @return null means canceled by user or exception, it returns full stream
	 *         or null, never part. <BR>
	 * 		To get file extension of result, use
	 *         {@link HCFileInputStream#getFileExtension()}.
	 * @since 7.72
	 */
	public final HCFileInputStream getOneAudioFileMaybeByRecording() {
		if (j2seCoreSSMaybeNull == null) {
			return SimuMobile.buildMobileAudioFileStream();
		}

		ServerUIUtil.checkLineOnForAPI(j2seCoreSSMaybeNull);

		final StringBuilder sb = StringBuilderCacher.getFree();

		sb.append(TYPE_AUDIO);
		sb.append(HCURL.FILE_EXTENSION_SPLITTER);
		sb.append(IConstant.toString(isFromMobileDialog()));

		final String result = sb.toString();
		StringBuilderCacher.cycle(sb);
		return (HCFileInputStream) receiveFileFromClient(
				StreamBuilder.S_CLASS_TRANS_FILE_MAYBE_ACTION, false, result);
	}

	/**
	 * get a image file from mobile client, user can choose taking photograph or
	 * not. <BR>
	 * <BR>
	 * <STRONG>Know more</STRONG> : <BR>
	 * 1. it will block current thread for result, but not block server
	 * receiving result.<BR>
	 * 2. mobile client will not display alert/question/dialog when opening
	 * file.<BR>
	 * 
	 * @return null means canceled by user or exception, it returns full stream
	 *         or null, never part. <BR>
	 * 		To get file extension of result, use
	 *         {@link HCFileInputStream#getFileExtension()}.
	 * @see #getOneFileFromClient(String)
	 * @since 7.72
	 */
	public final HCFileInputStream getOneImageFileMaybeByTakingPhotograph() {
		if (j2seCoreSSMaybeNull == null) {
			return SimuMobile.buildMobileImageFileStream();
		}

		ServerUIUtil.checkLineOnForAPI(j2seCoreSSMaybeNull);

		final StringBuilder sb = StringBuilderCacher.getFree();

		sb.append(TYPE_IMAGE);
		sb.append(HCURL.FILE_EXTENSION_SPLITTER);
		sb.append(IConstant.toString(isFromMobileDialog()));

		final String result = sb.toString();
		StringBuilderCacher.cycle(sb);
		return (HCFileInputStream) receiveFileFromClient(
				StreamBuilder.S_CLASS_TRANS_FILE_MAYBE_ACTION, false, result);
	}

	/**
	 * open multiple files from mobile client. <BR>
	 * <BR>
	 * <STRONG>Know more</STRONG> : <BR>
	 * 1. it will block current thread for result, but not block server
	 * receiving result.<BR>
	 * 2. mobile client will not display alert/question/dialog when opening
	 * file.<BR>
	 * 3. in iOS, voice memos can't be accessed.
	 * 
	 * @param type
	 *            file type (recommend, the return file depends on user choice),
	 *            one of {@link #TYPE_IMAGE}, {@link #TYPE_AUDIO},
	 *            {@link #TYPE_VIDEO} and {@link #TYPE_ALL}.
	 * @return null means canceled by user or exception, it returns full stream
	 *         or null, never part. <BR>
	 * 		To get file extension of result, use
	 *         {@link HCFileInputStream#getFileExtension()}.
	 * @see #openOneFileFromClient(String)
	 * @see #getOneFileFromClient(String)
	 * @see #getOneAudioFileMaybeByRecording()
	 * @see #getOneImageFileMaybeByTakingPhotograph()
	 * @since 7.72
	 */
	public final Vector<HCFileInputStream> openMultipleFilesFromClient(final String type) {
		final Object result = openMultipleFilesFromClient(type, true);
		if (result == null) {
			return null;
		} else if (result instanceof Vector) {
			return (Vector<HCFileInputStream>) result;
		} else if (result instanceof HCFileInputStream) {
			final Vector<HCFileInputStream> v = new Vector<HCFileInputStream>(1);
			v.add((HCFileInputStream) result);
			return v;
		} else {
			return null;
		}
	}

	/**
	 * get multiple files from mobile client. <BR>
	 * <BR>
	 * it is equals with {@link #openMultipleFilesFromClient(String)}.<BR>
	 * <BR>
	 * <STRONG>Know more</STRONG> : <BR>
	 * 1. it will block current thread for result, but not block server
	 * receiving result.<BR>
	 * 2. mobile client will not display alert/question/dialog when opening
	 * file.<BR>
	 * 3. in iOS, voice memos can't be accessed.
	 * 
	 * @param type
	 *            file type (recommend, the return file depends on user choice),
	 *            one of {@link #TYPE_IMAGE}, {@link #TYPE_AUDIO},
	 *            {@link #TYPE_VIDEO} and {@link #TYPE_ALL}.
	 * @return null means canceled by user or exception, it returns full stream
	 *         or null, never part. <BR>
	 * 		To get file extension of result, use
	 *         {@link HCFileInputStream#getFileExtension()}.
	 * @see #getOneFileFromClient(String)
	 * @see #getOneAudioFileMaybeByRecording()
	 * @see #getOneImageFileMaybeByTakingPhotograph()
	 */
	public final Vector<HCFileInputStream> getMultipleFilesFromClient(final String type) {
		return openMultipleFilesFromClient(type);
	}

	/**
	 * 单文件请求，则返回HCFileInputStream；多文件请求，则返回Vector<HCFileInputStream>
	 * 
	 * @param type
	 * @param isMultple
	 * @return
	 */
	private final Object openMultipleFilesFromClient(String type, final boolean isMultple) {// split
																							// is
																							// HCURL.FILE_EXTENSION_SPLITTER
		if (j2seCoreSSMaybeNull == null) {
			if (isMultple) {
				final Vector<HCFileInputStream> result = new Vector<HCFileInputStream>(1);
				result.add(SimuMobile.buildMobileImageFileStream());
				return result;
			} else {
				return SimuMobile.buildMobileImageFileStream();
			}
		}

		ServerUIUtil.checkLineOnForAPI(j2seCoreSSMaybeNull);

		if (type == null) {
			type = TYPE_ALL;
		}

		final StringBuilder sb = StringBuilderCacher.getFree();

		sb.append(type);
		sb.append(HCURL.FILE_EXTENSION_SPLITTER);
		sb.append(IConstant.toString(isMultple));
		sb.append(HCURL.FILE_EXTENSION_SPLITTER);
		sb.append(IConstant.toString(isFromMobileDialog()));

		final String result = sb.toString();
		StringBuilderCacher.cycle(sb);
		return receiveFileFromClient(StreamBuilder.S_CLASS_TRANS_FILE, isMultple, result);
	}

	/**
	 * 单文件请求，则返回HCFileInputStream；多文件请求，则返回Vector<HCFileInputStream>
	 * 
	 * @param eClass
	 * @param isMultple
	 * @param typePass
	 * @return
	 */
	private final Object receiveFileFromClient(final String eClass, final boolean isMultple,
			final String typePass) {
		final byte[] typeBS = ByteUtil.getBytes(typePass, IConstant.UTF_8);
		final StreamBuilder streamBuilder = new StreamBuilder(j2seCoreSSMaybeNull);

		final hc.core.util.io.HCInputStream baseIS = (hc.core.util.io.HCInputStream) ServerUIAPIAgent
				.runAndWaitInSysThread(new ReturnableRunnable() {
					@Override
					public Object run() throws Throwable {
						return streamBuilder.buildInputStream(eClass, typeBS, 0, typeBS.length,
								true);
					}
				});

		final byte[] lengthBS = StreamBlockBuilder.getFreeLengthBS();
		Vector<HCFileInputStream> vectResult = null;

		try {
			do {
				String block = StreamBlockBuilder.readHeaderBlock(baseIS, lengthBS);
				if (StreamBlockBuilder.HEADER_TAG_CLOSE.equals(block)) {
					break;
				}

				String fileType = "";
				String fileName = "";

				{
					final String[] keys = StringUtil.splitToArray(block,
							StreamBlockBuilder.HEADER_SPLITTER);
					if (StreamBlockBuilder.HEADER_FILE_EXTSION.equals(keys[0])) {
						fileType = keys[1];
					}
				}
				{
					block = StreamBlockBuilder.readHeaderBlock(baseIS, lengthBS);
					final String[] keys = StringUtil.splitToArray(block,
							StreamBlockBuilder.HEADER_SPLITTER);
					if (StreamBlockBuilder.HEADER_FILE_NAME.equals(keys[0])) {
						fileName = keys[1];
					}
				}

				block = StreamBlockBuilder.readHeaderBlock(baseIS, lengthBS);
				HCFileInputStream resultOne = null;
				if (StreamBlockBuilder.HEADER_TAG_STREAM_CONTENT.equals(block)) {
					final InputStream bis = buildISFromStream(baseIS, lengthBS);
					resultOne = HCInputStreamBuilder.build(bis, fileType, fileName);
				}
				if (isMultple == false) {
					if (resultOne != null) {
						return resultOne;
					} else {
						return null;
					}
				}

				if (resultOne != null) {
					if (vectResult == null) {
						vectResult = new Vector<HCFileInputStream>(4);
					}
					vectResult.add(resultOne);
				}
			} while (isMultple);

			if (vectResult == null || vectResult.size() == 0) {
				return null;
			}
			return vectResult;
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			ByteUtil.byteArrayCacher.cycle(lengthBS);
			try {
				baseIS.close();
			} catch (final Exception e) {
			}
		}
		ServerUIUtil.checkLineOnForAPI(j2seCoreSSMaybeNull);
		return null;
	}

	/**
	 * scan QR code on client and wait for result. <BR>
	 * <BR>
	 * <STRONG>Know more</STRONG> : <BR>
	 * 1. it will block current thread for result, but not block server
	 * receiving result.<BR>
	 * 2. mobile client will not display alert/question/dialog when scanning
	 * code.
	 * 
	 * @return null means canceled by user or exception.
	 * @since 7.72
	 */
	public final String scanQRCodeFromClient() {
		if (j2seCoreSSMaybeNull == null) {
			return SimuMobile.MOBILE_QR_RESULT;
		}

		ServerUIUtil.checkLineOnForAPI(j2seCoreSSMaybeNull);

		final Thread threadSnap = Thread.currentThread();
		waitThread = threadSnap;// 注意：不考虑多线程并发调用

		ServerUIAPIAgent.runInSysThread(new Runnable() {
			@Override
			public void run() {
				HCURLUtil.sendCmd(j2seCoreSSMaybeNull, HCURL.DATA_CMD_SendPara,
						HCURL.DATA_PARA_SCAN_QR_CODE, IConstant.toString(isFromMobileDialog()));
			}
		});

		synchronized (threadSnap) {
			if (isLogout) {
				return null;
			}
			try {
				threadSnap.wait();
			} catch (final InterruptedException e) {
			}
		}

		ServerUIUtil.checkLineOnForAPI(j2seCoreSSMaybeNull);

		final String result = (String) ThreadConfig.getValue(ThreadConfig.QR_RESULT, true);
		if (HCURL.CANCEL_HC_CMD.equals(result)) {
			LogManager.log("user cancel scan QR code.");
			return null;
		}

		return result;
	}

	final void notifyClientSessionWaitObjectShutdown() {
		isLogout = true;

		notifyClientSessionWaitObject(waitThread);
	}

	private final void notifyClientSessionWaitObject(final Thread threadSnap) {
		if (threadSnap != null) {
			synchronized (threadSnap) {
				threadSnap.notify();
			}
			// threadSnap = null;//不注释可能会导致notifyQRCode出现null情形
		}
	}

	final void notifyQRCode(final String result) {
		final Thread threadSnap = waitThread;
		if (threadSnap != null) {
			ThreadConfig.putValue(threadSnap, ThreadConfig.QR_RESULT, result);

			notifyClientSessionWaitObject(threadSnap);
		} else {
			LogManager.errToLog("Error on notifyQRCode");
		}
	}

	/**
	 * true means GPS; false means otherwise, maybe WiFi.
	 * 
	 * @return
	 * @see #getLocationLongitude()
	 */
	public final boolean isLocationGPS() {
		if (hasLocationOfMobile == false) {
			LogManager.err(HCPermissionConstant.NO_PERMISSION_OF_LOCATION_OF_MOBILE);
			return false;
		}

		if (j2seCoreSSMaybeNull == null) {
			return SimuMobile.MOBILE_GPS;
		}
		return j2seCoreSSMaybeNull.location.isGPS;
	}

	/**
	 * true means fresh; false means otherwise, maybe last known GPS.
	 * 
	 * @return
	 * @see #getLocationLongitude()
	 */
	public final boolean isLocationFresh() {
		if (hasLocationOfMobile == false) {
			LogManager.err(HCPermissionConstant.NO_PERMISSION_OF_LOCATION_OF_MOBILE);
			return false;
		}

		if (j2seCoreSSMaybeNull == null) {
			return SimuMobile.MOBILE_FRESH;
		}
		return j2seCoreSSMaybeNull.location.isFresh;
	}

	/**
	 * returns the last known altitude (unit : meter). <BR>
	 * if no permission (project or mobile) or no GPS signal, then return -1.0;
	 * 
	 * @return
	 * @see #getLocationLongitude()
	 */
	public final double getLocationAltitude() {// 海拔：米
		if (hasLocationOfMobile == false) {
			LogManager.err(HCPermissionConstant.NO_PERMISSION_OF_LOCATION_OF_MOBILE);
			return J2SESession.NO_PERMISSION_LOC;
		}

		if (j2seCoreSSMaybeNull == null) {
			return SimuMobile.MOBILE_ALTITUDE;
		}
		return j2seCoreSSMaybeNull.location.altitude;
	}

	/**
	 * returns the last known course.<BR>
	 * -1.0 means unknown (some Android return 0.0).<BR>
	 * <BR>
	 * 0 means north<BR>
	 * 90 means east<BR>
	 * 180 means south<BR>
	 * 270 means west
	 * 
	 * @return
	 * @see #getLocationLongitude()
	 */
	public final double getLocationCourse() {// 航向：0表示北 90东 180南 270西
		if (hasLocationOfMobile == false) {
			LogManager.err(HCPermissionConstant.NO_PERMISSION_OF_LOCATION_OF_MOBILE);
			return J2SESession.NO_PERMISSION_LOC;
		}

		if (j2seCoreSSMaybeNull == null) {
			return SimuMobile.MOBILE_COURSE;
		}
		return j2seCoreSSMaybeNull.location.course;
	}

	/**
	 * returns the last known speed (unit : meter/second).<BR>
	 * -1.0 means unknown (some Android return 0.0).<BR>
	 * <BR>
	 * 
	 * @return
	 * @see #getLocationLongitude()
	 */
	public final double getLocationSpeed() {// 设备移动速度：米/秒
		if (hasLocationOfMobile == false) {
			LogManager.err(HCPermissionConstant.NO_PERMISSION_OF_LOCATION_OF_MOBILE);
			return J2SESession.NO_PERMISSION_LOC;
		}

		if (j2seCoreSSMaybeNull == null) {
			return SimuMobile.MOBILE_SPEED;
		}
		return j2seCoreSSMaybeNull.location.speed;
	}

	/**
	 * returns the last known longitude.<BR>
	 * <BR>
	 * project permission [<STRONG>location of mobile</STRONG>] is required.
	 * <BR>
	 * <BR>
	 * if no permission (project or mobile) or no GPS signal, then return -1.0;
	 * <BR>
	 * <BR>
	 * to set location updates frequency, see
	 * {@link ProjectContext#setLocationUpdates(long)}.
	 * 
	 * @return
	 * @see #getLocationLatitude()
	 * @see #isWithoutLocation()
	 * @see ProjectContext#EVENT_SYS_MOBILE_LOCATION
	 * @see ProjectContext#addSystemEventListener(hc.server.util.SystemEventListener)
	 * @see #getLocationAltitude()
	 * @see #getLocationCourse()
	 * @see #getLocationSpeed()
	 * @see #isLocationGPS()
	 * @see #isLocationFresh()
	 */
	public final double getLocationLongitude() {// 经度
		if (hasLocationOfMobile == false) {
			LogManager.err(HCPermissionConstant.NO_PERMISSION_OF_LOCATION_OF_MOBILE);
			return J2SESession.NO_PERMISSION_LOC;
		}

		if (j2seCoreSSMaybeNull == null) {
			return SimuMobile.MOBILE_LONGITUDE;
		}
		return j2seCoreSSMaybeNull.location.longitude;
	}

	/**
	 * returns the attribute with the given name, or null if there is no
	 * attribute by that name. <BR>
	 * It is thread safe.
	 * 
	 * @param name
	 * @return the attribute with the <code>name</code>.
	 * @see #getIntAttribute(String)
	 * @see #getBooleanAttribute(String)
	 * @see #getStringAttribute(String)
	 * @since 7.7
	 */
	public final Object getAttribute(final String name) {
		return attribute_map.get(name);
	}

	/**
	 * returns the attribute with the given name, or <code>defaultValue</code>
	 * if there is no attribute by that name.
	 * 
	 * @param name
	 * @param defaultValue
	 *            the default value for name.
	 * @return <code>defaultValue</code> if this map contains no attribute for
	 *         the name
	 * @see #getIntAttribute(String, int)
	 * @see #getBooleanAttribute(String, boolean)
	 * @see #getStringAttribute(String, String)
	 * @since 7.7
	 */
	public final Object getAttribute(final String name, final Object defaultValue) {
		final Object value = attribute_map.get(name);
		if (value == null) {
			return defaultValue;
		} else {
			return value;
		}
	}

	/**
	 * if name is not exists or not Boolean object, then return false
	 * 
	 * @param name
	 * @return
	 * @see #getBooleanAttribute(String, boolean)
	 */
	public final boolean getBooleanAttribute(final String name) {
		final Object obj = attribute_map.get(name);
		if (obj != null && obj instanceof Boolean) {
			return ((Boolean) obj).booleanValue();
		}
		return false;
	}

	/**
	 * if name is not exists or not Boolean object, then return defaultValue.
	 * 
	 * @param name
	 * @param defaultValue
	 * @return the value of attribute <code>name</code>
	 * @see #getBooleanAttribute(String)
	 */
	public final boolean getBooleanAttribute(final String name, final boolean defaultValue) {
		final Object obj = attribute_map.get(name);
		if (obj != null) {
			if (obj instanceof Boolean) {
				return ((Boolean) obj).booleanValue();
			}
		}

		return defaultValue;
	}

	/**
	 * if name is not exists or not convertible object, then return 0
	 * 
	 * @param name
	 * @return
	 * @see #getByteAttribute(String, byte)
	 */
	public final byte getByteAttribute(final String name) {
		final Object obj = attribute_map.get(name);
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

	/**
	 * if name is not exists or not convertible object, then return
	 * defaultValue.
	 * 
	 * @param name
	 * @param defaultValue
	 * @return the value of attribute <code>name</code>
	 * @see #getByteAttribute(String)
	 */
	public final byte getByteAttribute(final String name, final byte defaultValue) {
		final Object obj = attribute_map.get(name);
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

		return defaultValue;
	}

	/**
	 * if name is not exists or not byte[] object, then return null.
	 * 
	 * @param name
	 * @return
	 */
	public final byte[] getByteArrayAttribute(final String name) {
		final Object obj = attribute_map.get(name);
		if (obj != null && obj instanceof byte[]) {
			return (byte[]) obj;
		}

		return null;
	}

	/**
	 * if name is not exists or not byte[] object, then return defaultValue.
	 * 
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public final byte[] getByteArrayAttribute(final String name, final byte[] defaultValue) {
		final Object obj = attribute_map.get(name);
		if (obj != null && obj instanceof byte[]) {
			return (byte[]) obj;
		}

		return defaultValue;
	}

	/**
	 * if name is not exists or not Character object, then return 0
	 * 
	 * @param name
	 * @return
	 * @see #getCharAttribute(String, char)
	 */
	public final char getCharAttribute(final String name) {
		final Object obj = attribute_map.get(name);
		if (obj != null) {
			if (obj instanceof Character) {
				return ((Character) obj);
			}
		}
		return 0;
	}

	/**
	 * if name is not exists or not Character object, then return defaultValue.
	 * 
	 * @param name
	 * @param defaultValue
	 * @return the value of attribute <code>name</code>
	 * @see #getCharAttribute(String)
	 */
	public final char getCharAttribute(final String name, final char defaultValue) {
		final Object obj = attribute_map.get(name);
		if (obj != null) {
			if (obj instanceof Character) {
				return ((Character) obj);
			}
		}

		return defaultValue;
	}

	/**
	 * if name is not exists or not convertible object, then return 0
	 * 
	 * @param name
	 * @return
	 * @see #getShortAttribute(String, short)
	 */
	public final short getShortAttribute(final String name) {
		final Object obj = attribute_map.get(name);
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

	/**
	 * if name is not exists or not convertible object, then return
	 * defaultValue.
	 * 
	 * @param name
	 * @param defaultValue
	 * @return the value of attribute <code>name</code>
	 * @see #getShortAttribute(String)
	 */
	public final short getShortAttribute(final String name, final short defaultValue) {
		final Object obj = attribute_map.get(name);
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

		return defaultValue;
	}

	/**
	 * if name is not exists or not convertible object, then return 0
	 * 
	 * @param name
	 * @return
	 * @see #getIntAttribute(String, int)
	 */
	public final int getIntAttribute(final String name) {
		final Object obj = attribute_map.get(name);
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

	/**
	 * if name is not exists or not convertible object, then return
	 * defaultValue.
	 * 
	 * @param name
	 * @param defaultValue
	 * @return the value of attribute <code>name</code>
	 * @see #getIntAttribute(String)
	 */
	public final int getIntAttribute(final String name, final int defaultValue) {
		final Object obj = attribute_map.get(name);
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

		return defaultValue;
	}

	/**
	 * if name is not exists or not convertible object, then return 0
	 * 
	 * @param name
	 * @return
	 * @see #getLongAttribute(String, long)
	 */
	public final long getLongAttribute(final String name) {
		final Object obj = attribute_map.get(name);
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

	/**
	 * if name is not exists or not convertible object, then return
	 * defaultValue.
	 * 
	 * @param name
	 * @param defaultValue
	 * @return the value of attribute <code>name</code>
	 * @see #getLongAttribute(String)
	 */
	public final long getLongAttribute(final String name, final long defaultValue) {
		final Object obj = attribute_map.get(name);
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

		return defaultValue;
	}

	/**
	 * if name is not exists or not convertible object, then return 0
	 * 
	 * @param name
	 * @return
	 * @see #getFloatAttribute(String, float)
	 */
	public final float getFloatAttribute(final String name) {
		final Object obj = attribute_map.get(name);
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

	/**
	 * if name is not exists or not convertible object, then return
	 * defaultValue.
	 * 
	 * @param name
	 * @param defaultValue
	 * @return the value of attribute <code>name</code>
	 * @see #getFloatAttribute(String)
	 */
	public final float getFloatAttribute(final String name, final float defaultValue) {
		final Object obj = attribute_map.get(name);
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

		return defaultValue;
	}

	/**
	 * if name is not exists or not convertible object, then return 0
	 * 
	 * @param name
	 * @return
	 * @see #getDoubleAttribute(String, double)
	 */
	public final double getDoubleAttribute(final String name) {
		final Object obj = attribute_map.get(name);
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

	/**
	 * if name is not exists or not convertible object, then return
	 * defaultValue.
	 * 
	 * @param name
	 * @param defaultValue
	 * @return the value of attribute <code>name</code>
	 * @see #getDoubleAttribute(String)
	 */
	public final double getDoubleAttribute(final String name, final double defaultValue) {
		final Object obj = attribute_map.get(name);
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
		return defaultValue;
	}

	/**
	 * if name is not exists, return null; if the attribute of name is not
	 * String object, return obj.toString()
	 * 
	 * @param name
	 * @return
	 * @see #getStringAttribute(String, String)
	 */
	public final String getStringAttribute(final String name) {
		final Object obj = attribute_map.get(name);
		if (obj != null) {
			if (obj instanceof String) {
				return (String) obj;
			} else {
				return obj.toString();
			}
		}
		return null;
	}

	/**
	 * if name is not exists, then return defaultValue; if the attribute of name
	 * is not String object, return obj.toString()
	 * 
	 * @param name
	 * @param defaultValue
	 * @return the value of attribute <code>name</code>
	 * @see #getStringAttribute(String)
	 */
	public final String getStringAttribute(final String name, final String defaultValue) {
		final Object obj = attribute_map.get(name);
		if (obj != null) {
			if (obj instanceof String) {
				return (String) obj;
			} else {
				return obj.toString();
			}
		} else {
			return defaultValue;
		}
	}

	/**
	 * set an object to a given attribute name. <BR>
	 * <BR>
	 * it is thread safe. <BR>
	 * <BR>
	 * to set attribute for project (NOT for session), see
	 * {@link ProjectContext#setAttribute(String, Object)}.
	 * 
	 * @param name
	 *            the name of attribute.
	 * @param obj
	 *            the value of the attribute.
	 * @return the previous object of the specified name, or null if it did not
	 *         have one
	 * @see #setIntAttribute(String, int)
	 * @see #setBooleanAttribute(String, boolean)
	 * @see #setStringAttribute(String, String)
	 * @since 7.7
	 */
	public final Object setAttribute(final String name, final Object obj) {
		return attribute_map.put(name, obj);
	}

	/**
	 * set attribute name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setBooleanAttribute(final String name, final boolean value) {
		attribute_map.put(name, Boolean.valueOf(value));
	}

	/**
	 * set attribute name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setByteAttribute(final String name, final byte value) {
		attribute_map.put(name, Byte.valueOf(value));
	}

	/**
	 * set <code>value</code> for the <code>name</code> in attribute.<BR>
	 * to copy to new byte array , please invoke
	 * {@link #setByteArrayAttribute(String, byte[], int, int)}.
	 * 
	 * @param name
	 * @param value
	 *            is NOT copied.
	 */
	public final void setByteArrayAttribute(final String name, final byte[] value) {
		attribute_map.put(name, value);
	}

	/**
	 * set name with new byte array, which copy values from bs.
	 * 
	 * @param name
	 * @param bs
	 *            the values is copied to new byte array.
	 * @param offset
	 * @param length
	 * @see #setByteArrayAttribute(String, byte[])
	 */
	public final void setByteArrayAttribute(final String name, final byte[] bs, final int offset,
			final int length) {
		final byte[] outbs = new byte[length];
		System.arraycopy(bs, offset, outbs, 0, length);
		attribute_map.put(name, outbs);
	}

	/**
	 * set attribute name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setCharAttribute(final String name, final char value) {
		attribute_map.put(name, Character.valueOf(value));
	}

	/**
	 * set attribute name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setShortAttribute(final String name, final short value) {
		attribute_map.put(name, Short.valueOf(value));
	}

	/**
	 * set attribute name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setIntAttribute(final String name, final int value) {
		attribute_map.put(name, Integer.valueOf(value));
	}

	/**
	 * set attribute name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setLongAttribute(final String name, final long value) {
		attribute_map.put(name, Long.valueOf(value));
	}

	/**
	 * set attribute name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setFloatAttribute(final String name, final float value) {
		attribute_map.put(name, Float.valueOf(value));
	}

	/**
	 * set attribute name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setDoubleAttribute(final String name, final double value) {
		attribute_map.put(name, Double.valueOf(value));
	}

	/**
	 * set attribute name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setStringAttribute(final String name, final String value) {
		attribute_map.put(name, value);
	}

	/**
	 * removes the attribute with the given name.
	 * 
	 * @param name
	 *            the name that needs to be removed
	 * @return the attribute to which the name had been mapped, or null if the
	 *         name did not have a mapping
	 * @since 7.7
	 */
	public final Object removeAttribute(final String name) {
		return attribute_map.remove(name);
	}

	/**
	 * It is equals with {@link #removeAttribute(String)}
	 * 
	 * @param name
	 *            the name that needs to be removed
	 * @return the attribute to which the name had been mapped, or null if the
	 *         name did not have a mapping
	 * @since 7.7
	 */
	public final Object clearAttribute(final String name) {
		return removeAttribute(name);
	}

	/**
	 * returns an enumeration containing the attribute names available.
	 * 
	 * @return the enumeration of all attribute names.
	 * @since 7.7
	 */
	public final Enumeration getAttributeNames() {
		final HashSet<String> set = new HashSet<String>();
		synchronized (attribute_map) {
			final Enumeration<String> en = attribute_map.keys();
			try {
				while (en.hasMoreElements()) {
					final String item = en.nextElement();
					set.add(item);
				}
			} catch (final NoSuchElementException e) {
			}
		}

		final Iterator<String> setit = set.iterator();
		return new Enumeration() {
			@Override
			public boolean hasMoreElements() {
				return setit.hasNext();
			}

			@Override
			public Object nextElement() {
				return setit.next();
			}
		};
	}

	/**
	 * returns the number of attribute names.
	 * 
	 * @return
	 * @since 7.7
	 */
	public final int getAttributeSize() {
		return attribute_map.size();
	}

	/**
	 * tests if the specified name is a key in this session.
	 * 
	 * @param name
	 *            possible key
	 * @return
	 * @throws NullPointerException
	 *             if the name is null.
	 * @since 7.7
	 */
	public final boolean containsAttributeName(final Object name) {
		return attribute_map.containsKey(name);
	}

	/**
	 * returns true if one or more names maps to this object.
	 * 
	 * @param object
	 * @return
	 * @throws NullPointerException
	 *             if the value is null
	 * @since 7.7
	 */
	public final boolean containsAttributeObject(final Object object) {
		return attribute_map.contains(object);
	}

	/**
	 * check if these is no names to values.
	 *
	 * @return true if empty.
	 * @since 7.7
	 */
	public final boolean isAttributeEmpty() {
		return attribute_map.isEmpty();
	}
}

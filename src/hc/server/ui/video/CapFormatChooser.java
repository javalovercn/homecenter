package hc.server.ui.video;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import javax.media.CaptureDeviceManager;
import javax.media.Format;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;
import javax.media.format.YUVFormat;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.sun.media.util.JMFI18N;

public class CapFormatChooser extends JPanel implements ItemListener,
		ActionListener {
	private VideoFormat formatOld;
	private Format[] arrSupportedFormats;
	private float[] customFrameRates;
	private Vector vectorContSuppFormats;
	private boolean boolDisplayEnableTrack;
	private ActionListener listenerEnableTrack;
	private boolean boolEnableTrackSaved;
	private JCheckBox checkEnableTrack;
	private JLabel labelEncoding;
	private JComboBox comboEncoding;
	private JLabel labelSize;
	private CapSizeControl controlSize;
	private JLabel labelFrameRate;
	private JComboBox comboFrameRate;
	private JLabel labelExtra;
	private JComboBox comboExtra;
	private int nWidthLabel;
	private int nWidthData;
	private static final int MARGINH = 12;
	private static final int MARGINV = 6;
	private static final float[] standardCaptureRates = { 15.0F, 1.0F, 2.0F,
			5.0F, 7.5F, 10.0F, 12.5F, 20.0F, 24.0F, 25.0F, 30.0F };
	private static final String DEFAULT_STRING = "<default>";

	public CapFormatChooser(Format[] arrFormats, VideoFormat formatDefault,
			float[] frameRates) {
		this(arrFormats, formatDefault, false, null, frameRates);
	}

	public CapFormatChooser(Format[] arrFormats, VideoFormat formatDefault) {
		this(arrFormats, formatDefault, false, null, null);
	}

	public CapFormatChooser(Format[] arrFormats, VideoFormat formatDefault,
			boolean boolDisplayEnableTrack, ActionListener listenerEnableTrack) {
		this(arrFormats, formatDefault, boolDisplayEnableTrack,
				listenerEnableTrack, null);
	}

	public CapFormatChooser(Format[] arrFormats, VideoFormat formatDefault,
			boolean boolDisplayEnableTrack, ActionListener listenerEnableTrack,
			boolean capture) {
		this(arrFormats, formatDefault, boolDisplayEnableTrack,
				listenerEnableTrack, (capture) ? standardCaptureRates : null);
	}

	public CapFormatChooser(Format[] arrFormats, VideoFormat formatDefault,
			boolean boolDisplayEnableTrack, ActionListener listenerEnableTrack,
			float[] frameRates) {
		this.arrSupportedFormats = null;
		this.customFrameRates = null;
		this.vectorContSuppFormats = new Vector();

		this.boolEnableTrackSaved = true;

		this.nWidthLabel = 0;
		this.nWidthData = 0;

		this.arrSupportedFormats = arrFormats;
		this.boolDisplayEnableTrack = boolDisplayEnableTrack;
		this.listenerEnableTrack = listenerEnableTrack;
		this.customFrameRates = frameRates;

		int nCount = this.arrSupportedFormats.length;
		for (int i = 0; i < nCount; ++i)
			if (this.arrSupportedFormats[i] instanceof VideoFormat)
				this.vectorContSuppFormats
						.addElement(this.arrSupportedFormats[i]);

		if (isFormatSupported(formatDefault))
			this.formatOld = formatDefault;
		else
			this.formatOld = null;
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setEnabled(boolean boolEnable) {
		super.setEnabled(boolEnable);

		if (this.checkEnableTrack != null)
			this.checkEnableTrack.setEnabled(boolEnable);
		enableControls(boolEnable);
	}

	public VideoFormat getFormat() {
		VideoFormat formatVideoNew;
		String strYuvType = null;

		VideoFormat formatVideo = null;

		String strEncoding = (String) this.comboEncoding.getSelectedItem();

		int nSize = this.vectorContSuppFormats.size();
		for (int i = 0; i < nSize; ++i) {
			Object objectFormat = this.vectorContSuppFormats.elementAt(i);
			if (!(objectFormat instanceof VideoFormat))
				continue;
			formatVideo = (VideoFormat) objectFormat;

			if (!(isFormatGoodForEncoding(formatVideo)))
				continue;
			if (!(isFormatGoodForVideoSize(formatVideo)))
				continue;
			if (!(isFormatGoodForFrameRate(formatVideo)))
				continue;

			if ((strEncoding.equalsIgnoreCase("rgb"))
					&& (formatVideo instanceof RGBFormat)) {
				RGBFormat formatRGB = (RGBFormat) formatVideo;
				Integer integerBitsPerPixel = new Integer(
						formatRGB.getBitsPerPixel());
				String strBitsPerPixel = integerBitsPerPixel.toString();
				if (this.comboExtra.getSelectedItem().equals(strBitsPerPixel))
					break;
			} else {
				if ((!(strEncoding.equalsIgnoreCase("yuv")))
						|| (!(formatVideo instanceof YUVFormat)))
					break;
				YUVFormat formatYUV = (YUVFormat) formatVideo;
				int nYuvType = formatYUV.getYuvType();
				strYuvType = getYuvType(nYuvType);
				if ((strYuvType != null)
						&& (this.comboExtra.getSelectedItem()
								.equals(strYuvType)))
					break;
			}

		}

		if (formatVideo.getSize() == null) {
			formatVideoNew = new VideoFormat(null,
					this.controlSize.getVideoSize(), -1, null, -1.0F);
			formatVideo = (VideoFormat) formatVideoNew.intersects(formatVideo);
		}
		if ((this.customFrameRates != null) && (formatVideo != null)) {
			formatVideoNew = new VideoFormat(null, null, -1, null,
					getFrameRate());
			formatVideo = (VideoFormat) formatVideoNew.intersects(formatVideo);
		}

		return formatVideo;
	}

	public float getFrameRate() {
		String selection = (String) this.comboFrameRate.getSelectedItem();
		if (selection != null) {
			if (selection.equals(DEFAULT_STRING))
				return -1.0F;
			try {
				float fr = Float.valueOf(selection).floatValue();
				return fr;
			} catch (NumberFormatException nfe) {
			}
		}
		return -1.0F;
	}

	public void setCurrentFormat(VideoFormat formatDefault) {
		if (isFormatSupported(formatDefault))
			this.formatOld = formatDefault;
		updateFields(this.formatOld);
	}

	public void setFrameRate(float frameRate) {
		for (int i = 0; i < this.comboFrameRate.getItemCount(); ++i) {
			float value = Float.valueOf(
					(String) this.comboFrameRate.getItemAt(i)).floatValue();
			if (Math.abs(frameRate - value) < 0.5D) {
				this.comboFrameRate.setSelectedIndex(i);
				return;
			}
		}
	}

	public void setSupportedFormats(Format[] arrFormats,
			VideoFormat formatDefault) {
		this.arrSupportedFormats = arrFormats;

		this.vectorContSuppFormats.removeAllElements();
		int nCount = this.arrSupportedFormats.length;
		for (int i = 0; i < nCount; ++i)
			if (this.arrSupportedFormats[i] instanceof VideoFormat)
				this.vectorContSuppFormats
						.addElement(this.arrSupportedFormats[i]);

		if (isFormatSupported(formatDefault))
			this.formatOld = formatDefault;
		else
			this.formatOld = null;
		setSupportedFormats(this.vectorContSuppFormats);
	}

	public void setSupportedFormats(Vector vectorContSuppFormats) {
		this.vectorContSuppFormats = vectorContSuppFormats;

		if (vectorContSuppFormats.isEmpty()) {
			this.checkEnableTrack.setSelected(false);
			this.checkEnableTrack.setEnabled(false);
			onEnableTrack(true);
			return;
		}

		this.checkEnableTrack.setEnabled(true);
		this.checkEnableTrack.setSelected(this.boolEnableTrackSaved);
		onEnableTrack(true);

		if (!(isFormatSupported(this.formatOld)))
			this.formatOld = null;

		updateFields(this.formatOld);
	}

	public void setTrackEnabled(boolean boolEnable) {
		this.boolEnableTrackSaved = boolEnable;
		if (this.checkEnableTrack == null)
			return;
		this.checkEnableTrack.setSelected(boolEnable);
		onEnableTrack(true);
	}

	public boolean isTrackEnabled() {
		boolean boolEnabled = this.checkEnableTrack.isSelected();
		return boolEnabled;
	}

	public Dimension getPreferredSize() {
		Dimension dim = new Dimension();
		if (this.boolDisplayEnableTrack == true) {
			dimControl = this.checkEnableTrack.getPreferredSize();
			dim.width = Math.max(dim.width, dimControl.width);
			dim.height += dimControl.height + 6;
		}

		Dimension dimLabel = this.labelEncoding.getPreferredSize();
		this.nWidthLabel = Math.max(this.nWidthLabel, dimLabel.width);
		Dimension dimControl = this.comboEncoding.getPreferredSize();
		this.nWidthData = Math.max(this.nWidthData, dimControl.width);
		dim.height += Math.max(dimLabel.height, dimControl.height) + 6;

		dimLabel = this.labelSize.getPreferredSize();
		this.nWidthLabel = Math.max(this.nWidthLabel, dimLabel.width);
		dimControl = this.controlSize.getPreferredSize();
		this.nWidthData = Math.max(this.nWidthData, dimControl.width);
		dim.height += Math.max(dimLabel.height, dimControl.height) + 6;

		dimLabel = this.labelFrameRate.getPreferredSize();
		this.nWidthLabel = Math.max(this.nWidthLabel, dimLabel.width);
		dimControl = this.comboFrameRate.getPreferredSize();
		this.nWidthData = Math.max(this.nWidthData, dimControl.width);
		dim.height += Math.max(dimLabel.height, dimControl.height) + 6;

		dimLabel = this.labelExtra.getPreferredSize();
		this.nWidthLabel = Math.max(this.nWidthLabel, dimLabel.width);
		dimControl = this.comboExtra.getPreferredSize();
		this.nWidthData = Math.max(this.nWidthData, dimControl.width);
		dim.height += Math.max(dimLabel.height, dimControl.height);

		dim.width = Math
				.max(dim.width, this.nWidthLabel + 12 + this.nWidthData);
		return dim;
	}

	Dimension dimControl;

	public void doLayout() {
		getPreferredSize();
		int nOffsetY = 0;
		int nLabelOffsetX = 0;
		int nDataOffsetX = this.nWidthLabel + 12;
		Dimension dimThis = getSize();

		if (this.boolDisplayEnableTrack == true) {
			dimControl = this.checkEnableTrack.getPreferredSize();
			this.checkEnableTrack.setBounds(nLabelOffsetX, nOffsetY,
					dimControl.width, dimControl.height);
			nOffsetY += dimControl.height + 6;
		}

		Dimension dimLabel = this.labelEncoding.getPreferredSize();
		Dimension dimControl = this.comboEncoding.getPreferredSize();
		this.labelEncoding.setBounds(nLabelOffsetX, nOffsetY, this.nWidthLabel,
				dimLabel.height);
		this.comboEncoding.setBounds(nDataOffsetX, nOffsetY, dimThis.width
				- nDataOffsetX, dimControl.height);
		nOffsetY += Math.max(dimLabel.height, dimControl.height) + 6;

		dimLabel = this.labelSize.getPreferredSize();
		dimControl = this.controlSize.getPreferredSize();
		this.labelSize.setBounds(nLabelOffsetX, nOffsetY, this.nWidthLabel,
				dimLabel.height);
		this.controlSize.setBounds(nDataOffsetX, nOffsetY, dimThis.width
				- nDataOffsetX, dimControl.height);
		nOffsetY += Math.max(dimLabel.height, dimControl.height) + 6;

		dimLabel = this.labelFrameRate.getPreferredSize();
		dimControl = this.comboFrameRate.getPreferredSize();
		this.labelFrameRate.setBounds(nLabelOffsetX, nOffsetY,
				this.nWidthLabel, dimLabel.height);
		this.comboFrameRate.setBounds(nDataOffsetX, nOffsetY, dimThis.width
				- nDataOffsetX, dimControl.height);
		nOffsetY += Math.max(dimLabel.height, dimControl.height) + 6;

		dimLabel = this.labelExtra.getPreferredSize();
		dimControl = this.comboExtra.getPreferredSize();
		this.labelExtra.setBounds(nLabelOffsetX, nOffsetY, this.nWidthLabel,
				dimLabel.height);
		this.comboExtra.setBounds(nDataOffsetX, nOffsetY, dimThis.width
				- nDataOffsetX, dimControl.height);
		nOffsetY += Math.max(dimLabel.height, dimControl.height) + 6;
	}

	private void init() throws Exception {
		setLayout(null);

		this.checkEnableTrack = new JCheckBox(
				JMFI18N.getResource("formatchooser.enabletrack"), true);
		this.checkEnableTrack.addItemListener(this);
		if (this.boolDisplayEnableTrack == true) {
			add(this.checkEnableTrack);
		}

		this.labelEncoding = new JLabel("Encoding:", SwingConstants.RIGHT);
		add(this.labelEncoding);
		this.comboEncoding = new JComboBox();
		this.comboEncoding.addItemListener(this);
		add(this.comboEncoding);

		this.labelSize = new JLabel("Video Size:", SwingConstants.RIGHT);
		add(this.labelSize);

		if (this.formatOld == null) {
			this.controlSize = new CapSizeControl();
		} else {
			CapSize sizeVideo = new CapSize(this.formatOld.getSize());
			this.controlSize = new CapSizeControl(sizeVideo);
		}

		this.controlSize.addActionListener(this);
		add(this.controlSize);

		this.labelFrameRate = new JLabel("Frame Rate:", SwingConstants.RIGHT);
		add(this.labelFrameRate);
		this.comboFrameRate = new JComboBox();
		this.comboFrameRate.addItemListener(this);
		add(this.comboFrameRate);

		this.labelExtra = new JLabel("Extra:", SwingConstants.RIGHT);
		this.labelExtra.setVisible(false);
		add(this.labelExtra);
		this.comboExtra = new JComboBox();
		this.comboExtra.setVisible(false);
		add(this.comboExtra);

		updateFields(this.formatOld);
	}

	private void updateFields(VideoFormat formatDefault) {
		String strEncoding;
		String strEncodingPref = null;

		Vector vectorEncoding = new Vector();

		boolean boolEnable = this.comboEncoding.isEnabled();
		this.comboEncoding.setEnabled(false);
		this.comboEncoding.removeAllItems();

		int nSize = this.vectorContSuppFormats.size();
		for (int i = 0; i < nSize; ++i) {
			Object objectFormat = this.vectorContSuppFormats.elementAt(i);
			if (!(objectFormat instanceof VideoFormat))
				continue;
			VideoFormat formatVideo = (VideoFormat) objectFormat;

			strEncoding = formatVideo.getEncoding().toUpperCase();
			if (strEncodingPref == null)
				strEncodingPref = strEncoding;

			//强制关闭选择YUV格式
			if (vectorEncoding.contains(strEncoding) || strEncoding.equalsIgnoreCase("yuv"))
				continue;
			this.comboEncoding.addItem(strEncoding);
			vectorEncoding.addElement(strEncoding);
		}

		if (formatDefault != null) {
			strEncoding = formatDefault.getEncoding().toUpperCase();
			this.comboEncoding.setSelectedItem(strEncoding);
		} else if (strEncodingPref != null) {
			this.comboEncoding.setSelectedItem(strEncodingPref);
		} else if (this.comboEncoding.getItemCount() > 0) {
			this.comboEncoding.setSelectedIndex(0);
		}
		updateFieldsFromEncoding(formatDefault);
		this.comboEncoding.setEnabled(boolEnable);
		//change Encoding
	}

	private void updateFieldsFromEncoding(VideoFormat formatDefault) {
		CapSize sizeVideo;
		Dimension formatVideoSize;
		CapSize sizeVideoPref = null;
		boolean boolVideoSizePref = false;

		boolean boolEnable = this.controlSize.isEnabled();
		this.controlSize.setEnabled(false);
		this.controlSize.removeAll();

		int nSize = this.vectorContSuppFormats.size();
		for (int i = 0; i < nSize; ++i) {
			Object objectFormat = this.vectorContSuppFormats.elementAt(i);
			if (!(objectFormat instanceof VideoFormat))
				continue;
			VideoFormat formatVideo = (VideoFormat) objectFormat;
			if (!(isFormatGoodForEncoding(formatVideo)))
				continue;
			formatVideoSize = formatVideo.getSize();
			if (formatVideoSize == null) {
				sizeVideo = null;
			} else
				sizeVideo = new CapSize(formatVideoSize);
			if (!(boolVideoSizePref)) {
				boolVideoSizePref = true;
				sizeVideoPref = sizeVideo;
			}

			this.controlSize.addItem(sizeVideo);
		}

		if ((formatDefault != null) && (isFormatGoodForEncoding(formatDefault))) {
			formatVideoSize = formatDefault.getSize();
			if (formatVideoSize == null) {
				sizeVideo = null;
			} else
				sizeVideo = new CapSize(formatVideoSize);

			this.controlSize.select(sizeVideo);
		} else if (boolVideoSizePref == true) {
			this.controlSize.select(sizeVideoPref);
		} else if (this.controlSize.getItemCount() > 0) {
			this.controlSize.select(0);
		}
		updateFieldsFromSize(formatDefault);
		this.controlSize.setEnabled(boolEnable);
	}

	private void updateFieldsFromSize(VideoFormat formatDefault) {
		Float floatFrameRate;
		Float floatFrameRatePref = null;

		Vector vectorRates = new Vector();

		boolean boolEnable = this.comboFrameRate.isEnabled();
		this.comboFrameRate.setEnabled(false);
		if (this.customFrameRates == null)
			this.comboFrameRate.removeAllItems();
		else if (this.comboFrameRate.getItemCount() < 1) {
			for (int i = 0; i < this.customFrameRates.length; ++i)
				this.comboFrameRate.addItem(Float
						.toString(this.customFrameRates[i]));
		}

		int nSize = this.vectorContSuppFormats.size();
		for (int i = 0; i < nSize; ++i) {
			Object objectFormat = this.vectorContSuppFormats.elementAt(i);
			if (!(objectFormat instanceof VideoFormat))
				continue;
			VideoFormat formatVideo = (VideoFormat) objectFormat;
			if (!(isFormatGoodForEncoding(formatVideo)))
				continue;
			if (!(isFormatGoodForVideoSize(formatVideo)))
				continue;

			if (this.customFrameRates != null) {
				continue;
			}

			floatFrameRate = new Float(formatVideo.getFrameRate());
			if (floatFrameRatePref == null)
				floatFrameRatePref = floatFrameRate;

			if (vectorRates.contains(floatFrameRate))
				continue;
			if (floatFrameRate.floatValue() == -1.0F)
				this.comboFrameRate.addItem(DEFAULT_STRING);
			else
				this.comboFrameRate.addItem(floatFrameRate.toString());
			vectorRates.addElement(floatFrameRate);
		}

		if ((formatDefault != null) && (this.customFrameRates == null)
				&& (isFormatGoodForEncoding(formatDefault))
				&& (isFormatGoodForVideoSize(formatDefault))) {
			floatFrameRate = new Float(formatDefault.getFrameRate());
			if (floatFrameRate.floatValue() == -1.0F)
				this.comboFrameRate.setSelectedItem(DEFAULT_STRING);
			else
				this.comboFrameRate.setSelectedItem(floatFrameRate.toString());
		} else if (floatFrameRatePref != null) {
			if (floatFrameRatePref.floatValue() == -1.0F)
				this.comboFrameRate.setSelectedItem(DEFAULT_STRING);
			else
				this.comboFrameRate.setSelectedItem(floatFrameRatePref
						.toString());
		} else if (this.comboFrameRate.getItemCount() > 0) {
			this.comboFrameRate.setSelectedIndex(0);
		}
		updateFieldsFromRate(formatDefault);
		this.comboFrameRate.setEnabled(boolEnable);
	}

	private void updateFieldsFromRate(VideoFormat formatDefault) {
		Integer integerBitsPerPixel;
		int nYuvType;
		RGBFormat formatRGB;
		YUVFormat formatYUV;
		String strYuvType = null;

		Vector vectorExtra = new Vector();
		boolean boolRGB = false;
		boolean boolYUV = false;

		String strEncoding = (String) this.comboEncoding.getSelectedItem();
		if (strEncoding == null)
			return;

		if (strEncoding.equalsIgnoreCase("rgb")) {
			this.labelExtra.setText("Bits/Pixel:");
			this.labelExtra.setVisible(true);
			this.comboExtra.setVisible(true);
			boolRGB = true;
		} else if (strEncoding.equalsIgnoreCase("yuv")) {
			this.labelExtra.setText("YUV Type:");
			this.labelExtra.setVisible(true);
			this.comboExtra.setVisible(true);
			boolYUV = true;
		} else {
			this.labelExtra.setVisible(false);
			this.comboExtra.setVisible(false);
			return;
		}

		boolean boolEnable = this.comboExtra.isEnabled();
		this.comboExtra.setEnabled(false);
		this.comboExtra.removeAllItems();

		int nSize = this.vectorContSuppFormats.size();
		for (int i = 0; i < nSize; ++i) {
			Object objectFormat = this.vectorContSuppFormats.elementAt(i);
			if (!(objectFormat instanceof VideoFormat))
				continue;
			VideoFormat formatVideo = (VideoFormat) objectFormat;
			if (!(isFormatGoodForEncoding(formatVideo)))
				continue;
			if (!(isFormatGoodForVideoSize(formatVideo)))
				continue;
			if (!(isFormatGoodForFrameRate(formatVideo)))
				continue;

			if ((boolRGB == true) && (formatVideo instanceof RGBFormat)) {
				formatRGB = (RGBFormat) formatVideo;
				integerBitsPerPixel = new Integer(formatRGB.getBitsPerPixel());
				if (!(vectorExtra.contains(integerBitsPerPixel))) {
					this.comboExtra.addItem(integerBitsPerPixel.toString());
					vectorExtra.addElement(integerBitsPerPixel);
				}
			} else if ((boolYUV == true) && (formatVideo instanceof YUVFormat)) {
				formatYUV = (YUVFormat) formatVideo;
				nYuvType = formatYUV.getYuvType();
				strYuvType = getYuvType(nYuvType);
				if ((strYuvType != null)
						&& (!(vectorExtra.contains(strYuvType)))) {
					this.comboExtra.addItem(strYuvType);
					vectorExtra.addElement(strYuvType);
				}
			}
		}

		if ((formatDefault != null) && (isFormatGoodForEncoding(formatDefault))
				&& (isFormatGoodForVideoSize(formatDefault))
				&& (isFormatGoodForFrameRate(formatDefault))) {
			if ((boolRGB == true) && (formatDefault instanceof RGBFormat)) {
				formatRGB = (RGBFormat) formatDefault;
				integerBitsPerPixel = new Integer(formatRGB.getBitsPerPixel());
				this.comboExtra.setSelectedItem(integerBitsPerPixel.toString());
			} else if ((boolYUV == true)
					&& (formatDefault instanceof YUVFormat)) {
				formatYUV = (YUVFormat) formatDefault;
				nYuvType = formatYUV.getYuvType();
				strYuvType = getYuvType(nYuvType);
				if (strYuvType != null)
					this.comboExtra.setSelectedItem(strYuvType);
			} else if (this.comboExtra.getItemCount() > 0) {
				this.comboExtra.setSelectedIndex(0);
			}
		} else if (this.comboExtra.getItemCount() > 0)
			this.comboExtra.setSelectedIndex(0);

		this.comboExtra.setEnabled(boolEnable);
	}

	private boolean isFormatGoodForEncoding(VideoFormat format) {
		boolean boolResult = false;

		String strEncoding = (String) this.comboEncoding.getSelectedItem();
		if (strEncoding != null)
			boolResult = format.getEncoding().equalsIgnoreCase(strEncoding);

		return boolResult;
	}

	private boolean isFormatGoodForVideoSize(VideoFormat format) {
		boolean boolResult = false;

		CapSize sizeVideo = this.controlSize.getVideoSize();
		Dimension formatVideoSize = format.getSize();
		if (formatVideoSize == null)
			boolResult = true;
		else
			boolResult = sizeVideo.equals(formatVideoSize);

		return boolResult;
	}

	private boolean isFormatGoodForFrameRate(VideoFormat format) {
		boolean boolResult = false;

		if (this.customFrameRates != null)
			return true;

		String strFrameRate = (String) this.comboFrameRate.getSelectedItem();
		if (strFrameRate.equals(DEFAULT_STRING))
			return true;

		float fFrameRate2 = format.getFrameRate();
		if (fFrameRate2 == -1.0F)
			return true;

		if (strFrameRate != null) {
			float fFrameRate1 = Float.valueOf(strFrameRate).floatValue();
			boolResult = fFrameRate1 == fFrameRate2;
		}
		return boolResult;
	}

	private boolean isFormatSupported(VideoFormat format) {
		boolean boolSupported = false;

		if (format == null)
			return boolSupported;

		int nCount = this.vectorContSuppFormats.size();
		for (int i = 0; (i < nCount) && (!(boolSupported)); ++i) {
			VideoFormat formatVideo = (VideoFormat) this.vectorContSuppFormats
					.elementAt(i);
			if (formatVideo.matches(format))
				boolSupported = true;
		}
		return boolSupported;
	}

	public void actionPerformed(ActionEvent event) {
		if (event.getActionCommand().equals("Size Changed"))
			updateFieldsFromSize(this.formatOld);
	}

	public void itemStateChanged(ItemEvent event) {
		Object objectSource = event.getSource();
		if (objectSource == this.checkEnableTrack) {
			this.boolEnableTrackSaved = this.checkEnableTrack.isSelected();
			onEnableTrack(true);
		} else if (objectSource == this.comboEncoding) {
			updateFieldsFromEncoding(this.formatOld);
		} else if (objectSource == this.controlSize) {
			updateFieldsFromSize(this.formatOld);
		} else if (objectSource == this.comboFrameRate) {
			updateFieldsFromRate(this.formatOld);
		}
	}

	private void onEnableTrack(boolean notifyListener) {
		boolean boolEnable = this.checkEnableTrack.isSelected();
		enableControls((boolEnable) && (isEnabled()));

		if ((notifyListener == true) && (this.listenerEnableTrack != null)) {
			ActionEvent event;
			if (boolEnable == true)
				event = new ActionEvent(this, 1001,
						"ACTION_VIDEO_TRACK_ENABLED");
			else
				event = new ActionEvent(this, 1001,
						"ACTION_VIDEO_TRACK_DISABLED");
			this.listenerEnableTrack.actionPerformed(event);
		}
	}

	private void enableControls(boolean boolEnable) {
		this.labelEncoding.setEnabled(boolEnable);
		this.comboEncoding.setEnabled(boolEnable);
		this.labelSize.setEnabled(boolEnable);
		this.controlSize.setEnabled(boolEnable);
		this.labelFrameRate.setEnabled(boolEnable);
		this.comboFrameRate.setEnabled(boolEnable);
		this.labelExtra.setEnabled(boolEnable);
		this.comboExtra.setEnabled(boolEnable);
	}

	private String getYuvType(int nType) {
		String strType = null;

		if ((nType & 0x2) == 2)
			strType = JMFI18N.getResource("formatchooser.yuv.4:2:0");
		else if ((nType & 0x4) == 4)
			strType = JMFI18N.getResource("formatchooser.yuv.4:2:2");
		else if ((nType & 0x20) == 32)
			strType = JMFI18N.getResource("formatchooser.yuv.YUYV");
		else if ((nType & 0x8) == 8)
			strType = JMFI18N.getResource("formatchooser.yuv.1:1:1");
		else if ((nType & 0x1) == 1)
			strType = JMFI18N.getResource("formatchooser.yuv.4:1:1");
		else if ((nType & 0x10) == 16)
			strType = JMFI18N.getResource("formatchooser.yuv.YVU9");
		else
			strType = null;

		return strType;
	}
}
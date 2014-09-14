package hc.server;

//import hc.App;
//import hc.core.IConstant;
//import hc.core.util.ByteUtil;
//import hc.res.ImageSrc;
//import hc.util.PropertiesManager;
//
//import java.awt.BorderLayout;
//import java.awt.Font;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.awt.image.BufferedImage;
//import java.util.Hashtable;
//
//import javax.imageio.ImageIO;
//import javax.swing.ImageIcon;
//import javax.swing.JButton;
//import javax.swing.JLabel;
//import javax.swing.JPanel;
//import javax.swing.JTextArea;
//import javax.swing.border.TitledBorder;
//
//import com.google.zxing.EncodeHintType;
//import com.google.zxing.MultiFormatWriter;
//import com.google.zxing.common.ByteMatrix;

public class QRTool {

//	private static String getCertification64(){
//		final byte[] bs = (byte[])IConstant.getInstance().getObject(IConstant.CertKey);
//		
//		byte andByte = 0;
//		int total = 0;
//		for (int i = 0; i < bs.length; i++) {
//			andByte ^= bs[i];
//			total += (0xFF & bs[i]);
//		}
//		
//		return ByteUtil.toHexEnableZeroBegin(andByte) + ByteUtil.toHexEnableZeroBegin((byte)total) + ByteUtil.toHex(bs);
//	}
//	
//	public static BufferedImage toQRPng(final String qrmsg, final int w, final int h){
//        ByteMatrix matrix = null;
//        com.google.zxing.Writer writer = new MultiFormatWriter();
//        try {
//            Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>(2);
//            hints.put(EncodeHintType.CHARACTER_SET, IConstant.UTF_8);
//            matrix = writer.encode(qrmsg, com.google.zxing.BarcodeFormat.QR_CODE, w, h, hints);
//        } catch (com.google.zxing.WriterException e) {
//        	return null;
//        }
//
//        final int width = matrix.width();
//        final int height = matrix.height();
//        final int BLACK = 0x000000;
//        final int WHITE = 0xFFFFFF;
//        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
//        for (int y = 0; y < height; y++) {
//          for (int x = 0; x < width; x++) {
//            final byte b = matrix.get(y, x);
//			image.setRGB(x, y, b == 0 ? BLACK : WHITE);
//          }
//        }
//        return image;
//	}
//
//	private static void qrCertification() {
//		final ActionListener okListener = new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				PropertiesManager.setValue(PropertiesManager.p_NewCertIsNotTransed, IConstant.FALSE);
//				PropertiesManager.saveFile();
//
//				J2SEContext.flipTransable(false);
//			}
//		};
//		final String base64_cert = getCertification64().toLowerCase();
////				final String msg = "Successful create new certification!";
//		final String copy = "<html><body style=\"width:220\"><strong>Description:</strong>" +
//				"<br>input it to the config of HomeCenter mobile app, " +
//				"or scan right QR image by QR read tool and paste the value to config.</body></html>";
//		
//		final BufferedImage qrImage = toQRPng(base64_cert, 250, 250);
//		final JPanel succPanel = new JPanel(new BorderLayout());
////					succPanel.add(new JLabel(msg, new ImageIcon(ImageIO.read(ImageSrc.OK_ICON)), SwingConstants.LEFT), BorderLayout.NORTH);
//		succPanel.setBorder(new TitledBorder("Certification (" + base64_cert.length() + ")"));
//		final JTextArea jtf = new JTextArea(base64_cert, 10, 16);
//		jtf.setEditable(false);
//		jtf.setLineWrap(true);
//		final Font font = jtf.getFont();
//		jtf.setFont(new Font(font.getName(), Font.BOLD, font.getSize()));
//		succPanel.add(jtf, BorderLayout.CENTER);
//		succPanel.add(new JLabel(copy), BorderLayout.SOUTH);
//
//		final JPanel panel = new JPanel(new BorderLayout());
//		panel.add(succPanel, BorderLayout.CENTER);
//		panel.add(new JLabel(new ImageIcon(qrImage)), BorderLayout.EAST);
//		
//		try {
//			JButton jb_ok = new JButton("I inputed to mobile", new ImageIcon(ImageIO.read(ImageSrc.OK_ICON)));
//			App.showCenterPanel(panel, 0, 0, "Certification String", true, jb_ok, 
//					null, okListener, null, null, false, true);
//		} catch (Exception e1) {
//			e1.printStackTrace();
//		}
//	}

}

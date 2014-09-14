package hc.server.ui.video;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SinglePixelPackedSampleModel;

import javax.media.Buffer;
import javax.media.Codec;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;
import com.sun.media.codec.video.colorspace.JavaRGBConverter;

public class HCBufferToImage {
	public HCBufferToImage(VideoFormat format)
    {
        converter = new JavaRGBConverter();
        Dimension size = format.getSize();
        RGBFormat prefFormat = new RGBFormat(size, size.width * size.height, Format.intArray, format.getFrameRate(), 32, -1, -1, -1, 1, -1, 0, -1);
        if(converter != null && converter.setInputFormat(format) != null)
        {
            Format outputs[] = converter.getSupportedOutputFormats(format);
            if(outputs != null && outputs.length != 0)
            {
                for(int j = 0; j < outputs.length; j++)
                    if(outputs[j].matches(prefFormat))
                    {
                        Format out = converter.setOutputFormat(outputs[j]);
                        if(out != null && out.matches(prefFormat))
                            try
                            {
                            	converter.open();
                            	break;
                            }
                            catch(ResourceUnavailableException rue) { }
                    }

            }
        }

        outputBuffer = new Buffer();
    }

    public Image createImage(Buffer buffer)
    {
        try
        {
            int retVal = converter.process(buffer, outputBuffer);
            if(retVal != 0)
                return null;
        }
        catch(Exception ex)
        {
            System.err.println("Exception " + ex);
            return null;
        }
        return createBufferedImage(outputBuffer);
    }

    private final Codec converter;
    private final Buffer outputBuffer;
    private final AffineTransform affinetransform = new AffineTransform(1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F);
    private final AffineTransformOp affinetransformop = new AffineTransformOp(affinetransform, null);
    int[] ai;
    
    public Image createBufferedImage(Buffer buffer)
    {
        RGBFormat rgbformat = (RGBFormat)buffer.getFormat();
        Object obj = buffer.getData();
        if(ai == null){
	        int i = rgbformat.getRedMask();
	        int j = rgbformat.getGreenMask();
	        int k = rgbformat.getBlueMask();
	        ai = new int[3];
	        ai[0] = i;
	        ai[1] = j;
	        ai[2] = k;
        }
        DataBufferInt databufferint = new DataBufferInt((int[])obj, rgbformat.getLineStride() * rgbformat.getSize().height);
        SinglePixelPackedSampleModel singlepixelpackedsamplemodel = new SinglePixelPackedSampleModel(3, rgbformat.getLineStride(), rgbformat.getSize().height, ai);
        java.awt.image.WritableRaster writableraster = Raster.createWritableRaster(singlepixelpackedsamplemodel, databufferint, new Point(0, 0));
        DirectColorModel directcolormodel = new DirectColorModel(24, ai[0], ai[1], ai[2]);
        BufferedImage bufferedimage = new BufferedImage(directcolormodel, writableraster, true, null);
        BufferedImage bufferedimage1 = affinetransformop.createCompatibleDestImage(bufferedimage, directcolormodel);
        affinetransformop.filter(bufferedimage, bufferedimage1);
        return bufferedimage1;
    }

}

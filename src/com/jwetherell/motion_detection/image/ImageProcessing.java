package com.jwetherell.motion_detection.image;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

/**
 * This abstract class is used to process images.
 * 
 * @author Justin Wetherell <phishman3579@gmail.com>
 */
public abstract class ImageProcessing {
	public static final int A = 0;
	public static final int R = 1;
	public static final int G = 2;
	public static final int B = 3;
	
	public static final int H = 0;
	public static final int S = 1;
	public static final int L = 2;

	//Get RGB from Integer
	public static float[] getARGB(int pixel) {
    	int a = (pixel >> 24) & 0xff;
    	int r = (pixel >> 16) & 0xff;
    	int g = (pixel >> 8) & 0xff;
    	int b = (pixel) & 0xff;
    	return (new float[]{a,r,g,b});
	}
	
	//Get HSL from RGB
    //H is 0-360 (degrees)
    //H and S are 0-100 (percent)
	public static int[] convertToHSL(int r, int g, int b) {
        float red = r / 255;
        float green = g / 255;
        float blue = b / 255;
        
		float minComponent = Math.min(red, Math.min(green, blue));
        float maxComponent = Math.max(red, Math.max(green, blue));
        float range = maxComponent - minComponent;
        float h=0,s=0,l=0;
        
        l = (maxComponent + minComponent) / 2;

        if(range == 0) { // Monochrome image
        	h = s = 0;
        } else {
            s = 	(l > 0.5) ? 
		    			range / (2 - range) 
		    		: 
		    			range / (maxComponent + minComponent);
            
            if(red == maxComponent) {
            	h = (blue - green) / range;
            } else if(green == maxComponent) {
            	h = 2 + (blue - red) / range;
            } else if(blue == maxComponent) {
            	h = 4 +(red - green) / range;
            }
        }
        
        //convert to 0-360 (degrees)
        h *= 60;
        if (h<0) h += 360;
        
        //convert to 0-100 (percent)
        s *= 100;
        l *= 100;
        
        //Since they were converted from float to int
		return (new int[]{(int)h,(int)s,(int)l});
	}

	public static int getBrightnessAtPoint(int pixel) {
		//Get RGB from Integer
		int r = (pixel >> 16) & 0xff;
    	int g = (pixel >> 8) & 0xff;
    	int b = (pixel) & 0xff;

    	//Convert RGB to HSL (not using method above because I don't want to create
    	//an extra float[] for every pixel.
        float red = r;
        float green = g;
        float blue = b;
        red = red / 255;
        green = green / 255;
        blue = blue / 255;

        float h=0,s=0,l=0;
        float minComponent = Math.min(red, Math.min(green, blue));
        float maxComponent = Math.max(red, Math.max(green, blue));
        float range = maxComponent - minComponent;
        
        l = (maxComponent + minComponent) / 2;

        if(range == 0) { // Monochrome image
        	h = s = 0;
        } else {
            s = 	(l > 0.5) ? 
		    			range / (2 - range) 
		    		: 
		    			range / (maxComponent + minComponent);
            if (Float.compare(red,maxComponent)==0) {
            	h = (blue - green) / range;
            } else if(Float.compare(green,maxComponent)==0) {
            	h = 2 + (blue - red) / range;
            } else if(Float.compare(blue,maxComponent)==0) {
            	h = 4 +(red - green) / range;
            } else {
            	Log.e("TAG", "Should not get here!");
            }
        }

        //convert to 0-360
        h = h * 60;
        if (h<0) h = h + 360;
        
        //convert to 0-100
        s = s * 100;
        l = l * 100;

        //Convert the HSL into a single "brightness" representation
        //brightness is between 0-100 using 50% lightness and 50% hue
        int brightness = (int)((l * 0.5) + ((h / 360) * 50)); 
        return brightness;
	}
	
	public static int[][] decodeYUV420SPtoHSL(byte[] yuv420sp, int width, int height) {
		if (yuv420sp==null) return null;
		
		final int frameSize = width * height;
		int[][] hsl = new int[frameSize][3];

	    for (int j = 0, yp = 0; j < height; j++) {
	        int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
	        for (int i = 0; i < width; i++, yp++) {
	            int y = (0xff & ((int) yuv420sp[yp])) - 16;
	            if (y < 0) y = 0;
	            if ((i & 1) == 0) {
	                v = (0xff & yuv420sp[uvp++]) - 128;
	                u = (0xff & yuv420sp[uvp++]) - 128;
	            }
	            int y1192 = 1192 * y;
	            int r = (y1192 + 1634 * v);
	            int g = (y1192 - 833 * v - 400 * u);
	            int b = (y1192 + 2066 * u);

	            if (r < 0) r = 0; else if (r > 262143) r = 262143;
	            if (g < 0) g = 0; else if (g > 262143) g = 262143;
	            if (b < 0) b = 0; else if (b > 262143) b = 262143;

	            hsl[yp] = convertToHSL(r,g,b);
	        }
	    }
	    return hsl;
	}
	
	public static int[] decodeYUV420SPtoRGB(byte[] yuv420sp, int width, int height) {
		if (yuv420sp==null) return null;
		
		final int frameSize = width * height;
		int[] rgb = new int[frameSize];

	    for (int j = 0, yp = 0; j < height; j++) {
	        int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
	        for (int i = 0; i < width; i++, yp++) {
	            int y = (0xff & ((int) yuv420sp[yp])) - 16;
	            if (y < 0) y = 0;
	            if ((i & 1) == 0) {
	                v = (0xff & yuv420sp[uvp++]) - 128;
	                u = (0xff & yuv420sp[uvp++]) - 128;
	            }
	            int y1192 = 1192 * y;
	            int r = (y1192 + 1634 * v);
	            int g = (y1192 - 833 * v - 400 * u);
	            int b = (y1192 + 2066 * u);

	            if (r < 0) r = 0; else if (r > 262143) r = 262143;
	            if (g < 0) g = 0; else if (g > 262143) g = 262143;
	            if (b < 0) b = 0; else if (b > 262143) b = 262143;

	            rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
	        }
	    }
	    return rgb;
	}

	public static Bitmap rgbToBitmap(int[] rgb, int width, int height) {
		if (rgb==null) return null;
		
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		bitmap.setPixels(rgb, 0, width, 0, 0, width, height);
		return bitmap;
	}
	
	public static Bitmap rotate(Bitmap bmp, int degrees) {
		if (bmp==null) return null;
		
        //getting scales of the image  
        int width = bmp.getWidth();  
        int height = bmp.getHeight();  

        //Creating a Matrix and rotating it to 90 degrees   
        Matrix matrix = new Matrix();  
        matrix.postRotate(degrees);  

        //Getting the rotated Bitmap  
        Bitmap rotatedBmp = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotatedBmp.compress(Bitmap.CompressFormat.JPEG, 100, stream); 
        return rotatedBmp;
	}
	
	public static byte[] rotate(byte[] data, int degrees) {
		if (data==null) return null;
		
		//Convert the byte data into a Bitmap
        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);  

        //Getting the rotated Bitmap  
        Bitmap rotatedBmp = rotate(bmp,degrees);
        
        //Get the byte array from the Bitmap
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotatedBmp.compress(Bitmap.CompressFormat.JPEG, 100, stream); 
        return stream.toByteArray();
	}
}

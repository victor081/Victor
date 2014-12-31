package com.futronictech;

import java.io.File;
import java.io.FileOutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

public class FtrScanDemoUsbHostActivity extends Activity {
    /** Called when the activity is first created. */
	private static Button mButtonScan;
	private static Button mButtonStop;
	private Button mButtonSave;
	private static TextView mMessage;
	private static TextView mScannerInfo;
	private static ImageView mFingerImage;
	private CheckBox mCheckFrame;
	private CheckBox mCheckLFD;
	private CheckBox mCheckInvertImage;
	
    public static boolean mStop = false;
	public static boolean mFrame = true;
	public static boolean mLFD = false;
	public static boolean mInvertImage = false;
    
    public static final int MESSAGE_SHOW_MSG = 1;
    public static final int MESSAGE_SHOW_SCANNER_INFO = 2;
    public static final int MESSAGE_SHOW_IMAGE = 3;
    public static final int MESSAGE_ERROR = 4;
    public static final int MESSAGE_TRACE = 5;

    public static byte[] mImageFP = null;    
    public static int mImageWidth = 0;
    public static int mImageHeight = 0;
    private static Bitmap mBitmapFP = null;

    private FPScan mFPScan = null;
    //
    public static boolean mUsbHostMode = true;

    // Intent request codes
    private static final int REQUEST_FILE_FORMAT = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mFrame = true;
    	mLFD = mInvertImage = false;    	
    	mButtonScan = (Button) findViewById(R.id.btnScan);
        mButtonStop = (Button) findViewById(R.id.btnStop);
        mButtonSave = (Button) findViewById(R.id.btnSave);
        mMessage = (TextView) findViewById(R.id.tvMessage);
        mScannerInfo = (TextView) findViewById(R.id.tvScannerInfo);
        mFingerImage = (ImageView) findViewById(R.id.imageFinger);
        mCheckFrame = (CheckBox) findViewById(R.id.cbFrame);
        mCheckLFD = (CheckBox) findViewById(R.id.cbLFD);
        mCheckInvertImage = (CheckBox) findViewById(R.id.cbInvertImage);
               	
        mButtonScan.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {	        		
	        		if( mFPScan != null )
	        		{
	        			mStop = true;
	        			mFPScan.stop();
	        			
	        		}
	        		mStop = false;
	        		if( StartScan() )
	        		{
	        	        mButtonScan.setEnabled(false);
	        	        mButtonSave.setEnabled(false);
	        	        mButtonStop.setEnabled(true);
	        		}
        		}
        });
        
        mButtonStop.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
	        		mStop = true;	       
	        		if( mFPScan != null )
	        		{
	        			mFPScan.stop();
	        			mFPScan = null;
       			
	        		}	        		
	        		mButtonScan.setEnabled(true);
	        		mButtonSave.setEnabled(true);
	        		mButtonStop.setEnabled(false);	        		
        		}
        });
        
        mButtonSave.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {   
            	if( mImageFP != null)
            		SaveImage();
            }
        });
        

        mCheckFrame.setOnCheckedChangeListener(new OnCheckedChangeListener() { 
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) { 
			if (buttonView.isChecked()) 
				mFrame = true;
			else 
			{
				mFrame = false;
				mCheckLFD.setChecked(false);
				mLFD = false;
			}
			} 
        });


        mCheckLFD.setOnCheckedChangeListener(new OnCheckedChangeListener() { 
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) { 
			if (buttonView.isChecked()) 
				mLFD = true;
			else 
				mLFD = false;
			} 
        });
        
        mCheckInvertImage.setOnCheckedChangeListener(new OnCheckedChangeListener() { 
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) { 
			if (buttonView.isChecked()) 
				mInvertImage = true;
			else 
				mInvertImage = false;
			} 
        });

    }
    
    @Override
	protected void onDestroy() {
		super.onDestroy();
		mStop = true;	       
		if( mFPScan != null )
		{
			mFPScan.stop();
			mFPScan = null;
		}	        		
	}

	private boolean StartScan()
    {
		mFPScan = new FPScan(this, mHandler);
		mFPScan.start();
		
		return true;
    }
    
    private void SaveImage()
    {
	    Intent serverIntent = new Intent(this, SelectFileFormatActivity.class);
	    startActivityForResult(serverIntent, REQUEST_FILE_FORMAT);
    }
    
    private void SaveImageByFileFormat(String fileFormat, String fileName)
    {
 	   	if( fileFormat.compareTo("WSQ") == 0 )	//save wsq file
    	{    	
    		Scanner devScan = new Scanner();
    		boolean bRet;
    		if( mUsbHostMode )
    			bRet = devScan.OpenDeviceUsbHost(this, false);
    		else
    			bRet = devScan.OpenDevice();
    		if( !bRet )
    		{
                mMessage.setText(devScan.GetErrorMessage());
                return;    			
    		}
    		byte[] wsqImg = new byte[mImageWidth*mImageHeight];
    		long hDevice = devScan.GetDeviceHandle();
    		ftrWsqAndroidHelper wsqHelper = new ftrWsqAndroidHelper();
    		if( wsqHelper.ConvertRawToWsq(hDevice, mImageWidth, mImageHeight, 2.25f, mImageFP, wsqImg) )
    		{  			
    	        File file = new File(fileName);                
    	        try { 
    	            FileOutputStream out = new FileOutputStream(file);                    
    	            out.write(wsqImg, 0, wsqHelper.mWSQ_size);	// save the wsq_size bytes data to file
    	            out.close();
    	            mMessage.setText("Image is saved as " + fileName);
    	         } catch (Exception e) { 
    	        	 mMessage.setText("Exception in saving file"); 
    	         }     			
    		}
    		else
    			mMessage.setText("Failed to convert the image!");
    		if( mUsbHostMode )
    			devScan.CloseDeviceUsbHost();
    		else
    			devScan.CloseDevice();
    		return;
    	}
    	// 0 - save bitmap file 
        File file = new File(fileName);                
        try { 
            FileOutputStream out = new FileOutputStream(file);                    
            //mBitmapFP.compress(Bitmap.CompressFormat.PNG, 90, out);
            MyBitmapFile fileBMP = new MyBitmapFile(mImageWidth, mImageHeight, mImageFP);
            out.write(fileBMP.toBytes());
            out.close();
            mMessage.setText("Image is saved as " + fileName);
         } catch (Exception e) { 
        	 mMessage.setText("Exception in saving file"); 
         } 
    }
    
    // The Handler that gets information back from the FPScan
	private static final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_SHOW_MSG:            	
            	String showMsg = (String) msg.obj;
                mMessage.setText(showMsg);
                break;
            case MESSAGE_SHOW_SCANNER_INFO:            	
            	String showInfo = (String) msg.obj;
                mScannerInfo.setText(showInfo);
                break;
            case MESSAGE_SHOW_IMAGE:
            	ShowBitmap();
                break;              
            case MESSAGE_ERROR:
           		//mFPScan = null;
            	mButtonScan.setEnabled(true);
            	mButtonStop.setEnabled(false);
            	break;
            }
        }
    };
    
    private static void ShowBitmap()
    {
    	int[] pixels = new int[mImageWidth * mImageHeight];
    	for( int i=0; i<mImageWidth * mImageHeight; i++)
    		pixels[i] = mImageFP[i];
    	Bitmap emptyBmp = Bitmap.createBitmap(pixels, mImageWidth, mImageHeight, Config.RGB_565);

        int width, height; 
        height = emptyBmp.getHeight(); 
        width = emptyBmp.getWidth();     
     
        mBitmapFP = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565); 
        Canvas c = new Canvas(mBitmapFP); 
        Paint paint = new Paint(); 
        ColorMatrix cm = new ColorMatrix(); 
        cm.setSaturation(0); 
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm); 
        paint.setColorFilter(f); 
        c.drawBitmap(emptyBmp, 0, 0, paint); 
        
        mFingerImage.setImageBitmap(mBitmapFP);
    }        

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
         case REQUEST_FILE_FORMAT:
			 if (resultCode == Activity.RESULT_OK) {
			     // Get the file format
				 String[] extraString = data.getExtras().getStringArray(SelectFileFormatActivity.EXTRA_FILE_FORMAT);
				 String fileFormat = extraString[0];
				 String fileName = extraString[1];
				 SaveImageByFileFormat(fileFormat, fileName);
             }
			 else
				 mMessage.setText("Cancelled!");
             break;            
        }
    }
    
}
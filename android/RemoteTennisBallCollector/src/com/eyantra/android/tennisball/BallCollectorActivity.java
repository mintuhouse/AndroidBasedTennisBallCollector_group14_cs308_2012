package com.eyantra.android.tennisball;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.HttpAuthHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.view.View;
import android.view.View.OnClickListener;

public class BallCollectorActivity extends Activity {
	private WebView VideoFrame;
	private ImageButton UpButton,DownButton,LeftButton,RightButton;
	private ImageView imgView;
	private TextView textview;
	private TextView mTitle;
	private String imgURL;
	private ProgressDialog progressDialog;
	private LinearLayout manualcontrols;
	private AlertDialog alertDialog;
	private OpenCV opencv = new OpenCV();
	private Handler tHandler = new Handler();	

	private ToggleButton togglemode;
	private boolean automode;
	
	private static final int PROCESS_ID = Menu.FIRST;
	private static final int HELP_ID = Menu.FIRST + 1;
	private static final int CONNECT_ID = Menu.FIRST + 2;
	
	// Debugging
	protected static final String TAG = BallCollectorActivity.class.getSimpleName();
	private static final boolean D = true;	
	
	private boolean BTConnected;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
	
    /*
     *  Message types sent from the BluetoothChatService Handler
     *  Note: Not Used
     */    
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";	
    
	private Bluetooth mBTClient = null;
	private BluetoothAdapter mBTAdapter = null;   
	
	// Sytem state info
	private int systemState;
	
	public static final int SYSTEM_INITIAL = 1;
	public static final int SYSTEM_BALL_INVIEW = 2;
	public static final int SYSTEM_BALL_INCENTER = 3;
	public static final int SYSTEM_BALL_MOVEDOUT = 4;
	public static final int SYSTEM_BALL_PICKED = 5;
	
	private static final int SENDTIME = 1000;	
	private static final int AUTOTIMER = 2000;	

	byte[] write_buffer = new byte[1];
	
	private boolean backButtonPressed;
    
 	// Open the webpage inline in the application
    private class VideoWebViewClient extends WebViewClient {
        @Override
        public void onReceivedHttpAuthRequest(WebView view,
                HttpAuthHandler handler, String host, String realm) {
            handler.proceed("admin", "1234");
        }
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
        @Override  
        public void onPageFinished(WebView view, String url)  
        {  
            view.loadUrl("javascript:(function() { " +
            		"var t=setTimeout(\"" +
            		"document.getElementById('TD_Img2').style.display = 'none'; " +
            		"document.getElementById('tr2').style.display = 'none'; " +
            		"document.getElementById('cam1').style.width=window.innerWidth; " +
            		"document.getElementById('cam1').style.height=window.innerHeight*0.9;" +
            		"document.getElementById('cam1').onclick='event.cancelBubble = true; return false;'" +
            		"\",1000);" +  
                    "})()");  
        }
    }		
	
    
    /*
     * Open HTML Authentication protected HttpConnection
     * String strURL	: URL of the website
     * return	: InputStream of HttpConnection
     */
	private InputStream OpenHttpConnection(String strURL)
            throws IOException {
        URLConnection conn = null;
        InputStream inputStream = null;
        URL url = new URL(strURL);        
        Authenticator.setDefault(new Authenticator(){
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("admin","1234".toCharArray());
            }});
        conn = url.openConnection();
        HttpURLConnection httpConn = (HttpURLConnection) conn;
        httpConn.setRequestMethod("GET");
        httpConn.connect();
        if (httpConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            inputStream = httpConn.getInputStream();
        }
        return inputStream;
    }
	
	/*
	 * Grab an image from URL and convert it into a 
	 * Drawable (a format for displaying in an ImageView)
	 */
	private Drawable grabImageFromUrl(String url) throws Exception {
		//return Drawable.createFromStream((InputStream)new URL(url).getContent(), "src");
		return Drawable.createFromStream((InputStream)OpenHttpConnection(url),"src");
	}	
	
	/*
	 * Grab an image from URL and convert it into a 
	 * Bitmap (a format for displaying in an ImageView)
	 */
	private Bitmap getBitmapImage(String url) throws Exception {
		return BitmapFactory.decodeStream((InputStream)OpenHttpConnection(url));
	}
	
	/*
	 * Cron Task which updated an image at regular intervals
	 * 
	 */
	private Runnable ProcessImageTask = new Runnable(){
		public void run(){
			long ms = SystemClock.uptimeMillis();
        	String iurl = imgURL+"?dummy="+ ms;
            textview.setText("Displaying "+ iurl);
        	/* Note: Redundant not used
        	 * try {
    		    imgView.setImageDrawable(grabImageFromUrl(iurl));
    		} catch(Exception e) {
    			imgView.setImageResource(R.drawable.notfound);
    		    textview.setText("Error: Exception");
    		}*/
        	try{
	        	Bitmap bitmap = getBitmapImage(imgURL);
				imgView.setImageBitmap(bitmap);
				int width = bitmap.getWidth();
				int height = bitmap.getHeight();
				int[] pixels = new int[width * height];
				bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
				//opencv.setSourceImage(pixels, width, height);
				if(!opencv.setSourceImage(pixels, width, height)){ 
				    Log.d("setSourceIMage:", "Error occurred while setting the source image pixels"); 
				} 
				long start = System.currentTimeMillis();
				int[] result = opencv.locateBall();
				long end = System.currentTimeMillis();
				boolean ballInView = (result[0]==1);
				int centerX=result[1], centerY=result[2], radius=result[3];
				boolean leftOfCenter = (ballInView && (centerX < (width*4)/9));
				boolean rightOfCenter = (ballInView && (centerX > (width*5)/9));
				boolean inCenter = (ballInView && (centerX >= (width*4)/9) && (centerX <= (width*5)/9) );
				/** systemState values
					SYSTEM_INITIAL = 1
					SYSTEM_BALL_INVIEW = 2
					SYSTEM_BALL_INCENTRE = 3
					SYSTEM_BALL_MOVEDOUT = 4
					SYSTEM_BALL_PICKED = 5
				*/
				// If Ball is in Camera's field of view
				if(ballInView){
					switch(systemState) {
					case SYSTEM_INITIAL:
						systemState=SYSTEM_BALL_INVIEW;
					case SYSTEM_BALL_INVIEW:
						if(!inCenter){
							if(leftOfCenter){
								sendMessage('l',SENDTIME);
							} else {
								sendMessage('r',SENDTIME);
							}
							break;
						}
						systemState=SYSTEM_BALL_INCENTER;
					case SYSTEM_BALL_INCENTER:
						if(inCenter) {
							systemState=SYSTEM_BALL_INCENTER;
							sendMessage('F',SENDTIME);
						} else {
							systemState=SYSTEM_BALL_INVIEW;
							if(leftOfCenter){
								sendMessage('l',SENDTIME);
							} else {
								sendMessage('r',SENDTIME);
							}
						}
						break;
					}
				// Ball doesn't appear in its field of view
				} else {
					switch(systemState){
					case SYSTEM_INITIAL:
						sendMessage('l',SENDTIME);
						break;
					default:
						systemState=SYSTEM_BALL_PICKED;
						//Stop execution of code.
						textview.setText("Ball successfully picked!");
					}
				}
				byte[] imageData = opencv.getSourceImage();
				long elapse = end - start;
				textview.setText(elapse+"ms is used to process Image.");
				bitmap = BitmapFactory.decodeByteArray(imageData, 0,imageData.length);
				imgView.setImageBitmap(bitmap);
        	}catch(Exception e) {
    			imgView.setImageResource(R.drawable.notfound);
    		    textview.setText("Error: Exception");
    		}
			tHandler.postAtTime(this, ms+AUTOTIMER);
		}		
	};
	
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	backButtonPressed = false;
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        setupUI();        

        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBTAdapter != null) {
            // Initialize the BluetoothChatService to perform bluetooth connections
        	
        }
        else {
          Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
          finish();
          return;
        }        		
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // Response will be recorded will then be called during onActivityResult
        if ( mBTAdapter != null ) {
            if (!mBTAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
            } else {
            	BTConnect();
            }
        }
        
    }
    
    public void BTConnect(){
		mBTClient = new Bluetooth(this);
		Toast.makeText(this, "Connecting...", Toast.LENGTH_LONG).show();
		try {
			Log.d(TAG, "Initialisation Started...");
			
			/** Bluetooth initialise function returns true if connection is succesful, else false. */
			if(mBTClient.Initialise() == false) 
			{
				Toast.makeText(this, " No connection established ", Toast.LENGTH_SHORT).show();
				return;
			}
			else 
			{
				Toast.makeText(this, " Connection established ", Toast.LENGTH_SHORT).show();
				BTConnected=true;
			}
			Log.d(TAG, "Initialisation Successful");
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "Initialisation Failed");
		}
    }
    
    public void BTDisconnect(){
    	Log.d(TAG,"Disonnect Requested");    
		if(mBTClient != null) mBTClient.free_channel(); /**Free up the BT channel. */
		Toast.makeText(this, " Bluetooth disconnected ", Toast.LENGTH_SHORT).show();		
		BTConnected = false;
    }

    public boolean sendMessage(char send) {
		try {  	
			write_buffer[0] = (byte)send;	
			try {mBTClient.BluetoothSend(write_buffer);   
			}
			catch (Exception e){e.printStackTrace();
			}
			if(D){
				String b = new String (write_buffer);
				//Toast.makeText(this, "Sending..."+b+"s",Toast.LENGTH_LONG).show();
			}
		}
		catch (Exception e){e.printStackTrace();
		}
		Log.d(TAG, "Write on button press successful");  
		return true;
    }
    /** 
     * Send message `send` to Firebird and disable clicks for block seconds
     * @param send  - message to be sent
     * @param block - time for which the System messages are to be blocked.
     * @return whether the message is sent
     */
    public boolean sendMessage(char send, int block){
    	boolean s = sendMessage(send);
		disableClicks(block, 1);
    	return s;
    }
    
    /**
     * Disable clicks for time `time` seconds
     * @param time
     */
    public void disableClicks(int time, int processID){
    	progressDialog.setTitle(R.string.empty);
    	switch(processID){
    	case 1:
    		progressDialog.setMessage("Sending. Please wait ...");
    		break;
    	case 2:
    		progressDialog.setMessage("Processing. Please wait ...");
    		break;
    	}
    	progressDialog.setCancelable(false);
    	progressDialog.show();
    	
    	Handler handler = null;
        handler = new Handler(); 
        handler.postDelayed(new Runnable(){ 
             public void run(){
            	 if(progressDialog.isShowing()){
	                 progressDialog.cancel();
	                 progressDialog.dismiss();
            	 }
             }
        }, time);
    }
    
    /**
     * Setup the UI & Listeners
     */
	private void setupUI() {

        // Open the Video stream in application     
        VideoFrame = (WebView) findViewById(R.id.VideoFrameView);
        VideoFrame.setClickable(false);
        VideoFrame.getSettings().setJavaScriptEnabled(true);
        VideoFrame.loadUrl("http://192.168.137.16/ipcam.asp");
        VideoFrame.setWebViewClient(new VideoWebViewClient());        
        
        //imgURL = "http://www.google.co.in/images/srpr/logo3w.png";
        imgURL = "http://192.168.137.16/snapshot.jpg";
        
        textview = (TextView) findViewById(R.id.textview);
        imgView = (ImageView) findViewById(R.id.imgView);
        manualcontrols = (LinearLayout) findViewById(R.id.JoyStick);

        alertDialog = new AlertDialog.Builder(this).create();   
        progressDialog = new ProgressDialog(this);

        if(!BTConnected) BTConnected=false;
        
        togglemode = (ToggleButton) findViewById(R.id.mode);
        automode = false;        
        togglemode.setOnCheckedChangeListener(modelistener);
        
        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.textview);
        
	}
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, PROCESS_ID, 0, "Process Image");
		menu.add(0, HELP_ID, 0, "Help");
		menu.add(0, CONNECT_ID, 0, "Connect");
		return true;
	}
    
    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {       
        if(BTConnected) 
             menu.findItem(CONNECT_ID).setTitle("Disconnect"); 
        else 
             menu.findItem(CONNECT_ID).setTitle("Connect"); 
        return super.onPrepareOptionsMenu(menu); 
   }
    
    @Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		/**
		 *  Use this only for debugging purposes.
		 *  Will remove 
		 */
		case PROCESS_ID:
        	tHandler.removeCallbacks(ProcessImageTask);
        	try{
        		//TODO: Check if is in AUTOMODE
        		Bitmap bitmap = getBitmapImage(imgURL);
				imgView.setImageBitmap(bitmap);
				int width = bitmap.getWidth();
				int height = bitmap.getHeight();
				int[] pixels = new int[width * height];
				bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
				//opencv.setSourceImage(pixels, width, height);
				if(!opencv.setSourceImage(pixels, width, height)){ 
				    Log.d("setSourceIMage:", "Error occurred while setting the source image pixels"); 
				} 
				long start = System.currentTimeMillis();
				int[] result = opencv.locateBall();
				long end = System.currentTimeMillis();
				byte[] imageData = opencv.getSourceImage();
				long elapse = end - start;
				Toast.makeText(this, "" + elapse + " ms is used to extract features.",
						Toast.LENGTH_LONG).show();
				bitmap = BitmapFactory.decodeByteArray(imageData, 0,
						imageData.length);
				imgView.setImageBitmap(bitmap);
        	} catch(Exception e) {
    			imgView.setImageResource(R.drawable.notfound);
    		    textview.setText("Error: Exception");
    		}
			return true;
		case HELP_ID:
			// sending test string...			
			try {
				sendMessage('H');
			}
			catch (Exception e){e.printStackTrace();
			}
			Log.d(TAG, "Write on button press successful"); 
			return true;
		case CONNECT_ID:
			if(BTConnected)
				BTDisconnect();
			else
				BTConnect();
            return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

    
    /** 
     * Called when the activity is destroyed 
     * @param savedInstanceState
     */
    public void onDestroy(Bundle savedInstanceState){
    	tHandler.removeCallbacks(ProcessImageTask);
    	super.onDestroy();
    	if(mBTClient != null){
			mBTClient.free_channel();}
    }
    
    /**
     * (non-Javadoc)
     * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
     * To Implement Back Button
     */
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && VideoFrame.canGoBack()) {
            if(backButtonPressed) finish();
            else {
            	Toast.makeText(this, "Press once more to exit", Toast.LENGTH_LONG).show();
			    backButtonPressed=true;
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    /** Called when the activity resumes after prompting user to turn ON the bluetooth. 
	 * If turned ON, goes ahead with application, else closes the connection and stops application.
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		Log.d(TAG, "onActivityResult " + resultCode);
		if (requestCode == REQUEST_ENABLE_BT) 
		{
			/** When the request to enable Bluetooth returns. */
			if (resultCode == Activity.RESULT_OK) {
				Log.d(TAG,"BT Enabled");
				Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_LONG).show();
				// Bluetooth is now enabled
			} else {
				// User did not enable Bluetooth or an error occured
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, "Bluetooth was not enabled. Closing application..", Toast.LENGTH_LONG).show();
				finish();  /** Terminate the activity and close application. */
				return;
			}
		}
	}
    
    
    public void addListenerOnJoyStick(){
    	UpButton = (ImageButton) findViewById(R.id.uparrow);
    	/*UpButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
            }
        });*/
    }
    
    public void upButtonAction(View v){
    	sendMessage('F',SENDTIME);
    }
    
    public void downButtonAction(View v){
    	sendMessage('B',SENDTIME);
    }
    
    public void leftButtonAction(View v){
    	sendMessage('L',SENDTIME);
    }
 
    public void rightButtonAction(View v){
    	sendMessage('R',SENDTIME);
    }
    
    public void pickButtonAction(View v){
    	sendMessage('P',SENDTIME);
    }
    
    OnCheckedChangeListener modelistener = new OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked){
            	manualcontrols.setVisibility(LinearLayout.GONE);
            	imgView.setVisibility(ImageView.VISIBLE);
            	tHandler.removeCallbacks(ProcessImageTask);
                systemState = SYSTEM_INITIAL;
                tHandler.postDelayed(ProcessImageTask, AUTOTIMER);
            } else {
            	tHandler.removeCallbacks(ProcessImageTask);
            	imgView.setVisibility(ImageView.GONE);
            	manualcontrols.setVisibility(LinearLayout.VISIBLE);
            }
        }
    };
    
}
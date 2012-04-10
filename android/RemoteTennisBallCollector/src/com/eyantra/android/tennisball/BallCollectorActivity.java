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
	private Handler tHandler = new Handler();
	private ToggleButton togglemode;
	private boolean automode;
	private LinearLayout manualcontrols;
	private AlertDialog alertDialog;
	private OpenCV opencv = new OpenCV();
	private static final int MODE_ID = Menu.FIRST;
	private static final int HELP_ID = Menu.FIRST + 1;
	private static final int CONNECT_ID = Menu.FIRST + 2;
	
	// Debugging
	protected static final String TAG = BallCollectorActivity.class.getSimpleName();
	private static final boolean D = true;	

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
	
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";	
    
    private String mConnectedDeviceName;
    private BluetoothAdapter mBluetoothAdapter;
	private Bluetooth mBTClient;
	private WakeLock wl;
    
	
    private final Handler mHandler = new Handler() {
		@Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case Bluetooth.STATE_NONE:
                	mTitle.setText("Not connected");
                    break;
                case Bluetooth.STATE_CONNECTING:
                	mTitle.setText("Connecting...");
                    break;
                case Bluetooth.STATE_CONNECTED:
                	mTitle.setText("Connected to: " + mConnectedDeviceName);
                    onDeviceConnected();
                    break;
                case Bluetooth.STATE_FAILED:
                	mTitle.setText("Connection lost :-(");
                    break;
                }
                break;
            case MESSAGE_WRITE:
            	if (D) {
                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);
                    Log.d(TAG, "TX: \"" + writeMessage + "\"");
            	}
                break;
            case MESSAGE_READ:
            	if (D) {
                	try {
                		byte[] readBuf = (byte[]) msg.obj;
        				String readMessage = new String(readBuf, 0, msg.arg1);
                        Log.d(TAG, "RX: \"" + readMessage + "\"");
                	}
                	catch ( Exception ex) {
                		Log.e(TAG, ex.getMessage(), ex);
                	}
            	}
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    

	private void onDeviceConnected() {
		//jabberClient.sendReset();
		
		mHandler.postDelayed(new Runnable() {
			public void run() {
				//jabberClient.playSound(1, 1, 1);
				mHandler.postDelayed(new Runnable() {
					public void run() {
						//jabberClient.playSound(1, 255, 2);
						mHandler.postDelayed(new Runnable() {
							public void run() {
								//jabberClient.playSound(3, 255, 1);
							}
						}, 500);
					}
				}, 5000);
			}
		}, 100);
		
		//vVol.setProgress(prefs.getLastVolume());
		//vCtrl.setVolume(prefs.getLastVolume());
	}
	
    
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
	
	private Drawable grabImageFromUrl(String url) throws Exception {
		//return Drawable.createFromStream((InputStream)new URL(url).getContent(), "src");
		return Drawable.createFromStream((InputStream)OpenHttpConnection(url),"src");
	}	

	private Bitmap getBitmapImage(String url) throws Exception {
		return BitmapFactory.decodeStream((InputStream)OpenHttpConnection(url));
	}
	
	private Runnable UpdateImageTask = new Runnable(){
		public void run(){
			long ms = SystemClock.uptimeMillis();
        	String iurl = imgURL+"?dummy="+ ms;
            textview.setText("Displaying "+ iurl);
        	try {
    		    imgView.setImageDrawable(grabImageFromUrl(iurl));
    		} catch(Exception e) {
    			imgView.setImageResource(R.drawable.notfound);
    		    textview.setText("Error: Exception");
    		}
			
			tHandler.postAtTime(this, ms+1000);
		}
		
	};
	
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
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
        //new ReloadImageView(this, 500, imgView, imgURL, textview);
        /*
        tHandler.removeCallbacks(UpdateImageTask);
        tHandler.postDelayed(UpdateImageTask, 100);
        */
        togglemode = (ToggleButton) findViewById(R.id.mode);
        automode = false;
        
        togglemode.setOnCheckedChangeListener(modelistener);

        alertDialog = new AlertDialog.Builder(this).create();
        
        //prefs = new Prefs(this);
		
        // Set up the window layout
        //requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		//setContentView(R.layout.dualstick);
        //getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        //jabberClient = new JabberClient(this);
        
        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.textview);
        //mTitle.setText(R.string.app_name);
        //mTitle = (TextView) findViewById(R.id.title_right_text);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            // Initialize the BluetoothChatService to perform bluetooth connections
        	mBTClient = new Bluetooth(this, mHandler);
        }
        else {
          Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
          finish();
          return;
        }
        
        //PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        //wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
        //wl.acquire();
        
		//setupUI();			
    }
    

    @Override
    protected void onPause() {
    	super.onPause();
        wl.release();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        //jabberClient.sendReset();
        if (mBTClient != null) mBTClient.stop();
        
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if ( mBluetoothAdapter != null ) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
            } else {
            	startConnectDeviceSecure();
            }
        }
    }
    
    
    private void connectDevice(Intent data, boolean secure) {
        if (mBluetoothAdapter != null) {
            // Get the device MAC address
            String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
            // Get the BLuetoothDevice object
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            // Attempt to connect to the device
            mBTClient.connect(device, secure);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE_SECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK)
                connectDevice(data, true);
            break;
        case REQUEST_CONNECT_DEVICE_INSECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK)
                connectDevice(data, false);
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                startConnectDeviceSecure();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.e(TAG, "BT not enabled");
                Toast.makeText(this, "BT not enabled, exiting", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    public void sendMessage(byte[]send) {
    	sendMessage(send, 0, send.length);
    }
    
    public void sendMessage(byte[]send, int len) {
    	sendMessage(send, 0, len);
    }
    
    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(byte[] send, int offset, int length) {
    	if ( mBTClient != null ) {
            // Check that we're actually connected before trying anything
            if (mBTClient.getState() != Bluetooth.STATE_CONNECTED) {
                //Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check that there's actually something to send
            if (send != null && send.length > 0) {
                mBTClient.write(send);
            }
    	}
    }
    

	private void setupUI() {
		/* NOT NEEDED
		Button b1 = (Button)findViewById(R.id.b1);
		b1.setOnClickListener(new OnClickListener() {
			byte[] z = new byte[] { 'r', (byte)1 };
			@Override
			public void onClick(View v) {
				sendMessage(z);
			}
		});*/

	}
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MODE_ID, 0, "Mode");
		menu.add(0, HELP_ID, 0, "Help");
		menu.add(0, CONNECT_ID, 0, "Connect");
		return true;
	}
    
    @Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case MODE_ID:
        	tHandler.removeCallbacks(UpdateImageTask);
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
				opencv.extractSURFFeature();
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
			/*Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			long timeTaken = System.currentTimeMillis();
			mCurrentImagePath = IMAGE_DIRECTORY + "/"
					+ Utility.createName(timeTaken) + ".jpg";
			Log.i(TAG, mCurrentImagePath);
			cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(new File(mCurrentImagePath)));
			startActivityForResult(cameraIntent, ACTIVITY_SELECT_CAMERA);*/
			return true;
		case HELP_ID:
			/*Intent galleryIntent = new Intent(Intent.ACTION_PICK,
					Images.Media.INTERNAL_CONTENT_URI);
			startActivityForResult(galleryIntent, ACTIVITY_SELECT_IMAGE);*/
			return true;
		case CONNECT_ID:
			startConnectDeviceSecure();
            return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}
    
	private void startConnectDeviceSecure() {
		if ( mBluetoothAdapter != null ) {
			Intent serverIntent;
			serverIntent = new Intent(this, DeviceListActivity.class);
			mTitle.setText("Attempting to connect");
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
		}
	}

    
    /** Called when the activity is destroyed */
    public void onDestroy(Bundle savedInstanceState){
    	tHandler.removeCallbacks(UpdateImageTask);
    	super.onDestroy();
    }
    
    /*
     * (non-Javadoc)
     * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
     * To Implement Back Button in WebView
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && VideoFrame.canGoBack()) {
            VideoFrame.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    */
    
    
    public void addListenerOnJoyStick(){
    	UpButton = (ImageButton) findViewById(R.id.uparrow);
    	/*UpButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
            }
        });*/
    }
    
    public void upButtonAction(View v){
    	alertDialog.setTitle("Up Button Pressed.");
    	alertDialog.show();
    }
    
    public void downButtonAction(View v){
    	alertDialog.setTitle("Down Button Pressed.");
    	alertDialog.show();    	
    }
    
    public void leftButtonAction(View v){
    	alertDialog.setTitle("Left Button Pressed.");
    	alertDialog.show();    	
    }
 
    public void rightButtonAction(View v){
    	alertDialog.setTitle("Right Button Pressed.");
    	alertDialog.show();    	
    }
    
    public void pickButtonAction(View v){
    	alertDialog.setTitle("Pick Button Pressed.");
    	alertDialog.show();    	
    }
    
    OnCheckedChangeListener modelistener = new OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked){
            	manualcontrols.setVisibility(LinearLayout.GONE);
            	imgView.setVisibility(ImageView.VISIBLE);
            	tHandler.removeCallbacks(UpdateImageTask);
                tHandler.postDelayed(UpdateImageTask, 100);
            } else {
            	tHandler.removeCallbacks(UpdateImageTask);
            	imgView.setVisibility(ImageView.GONE);
            	manualcontrols.setVisibility(LinearLayout.VISIBLE);
            }
        }
    };
    
}
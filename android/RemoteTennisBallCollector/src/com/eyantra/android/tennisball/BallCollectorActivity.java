package com.eyantra.android.tennisball;


import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.KeyEvent;
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
import android.widget.ToggleButton;
import android.view.View;
import android.view.View.OnClickListener;

public class BallCollectorActivity extends Activity {
	private WebView VideoFrame;
	private ImageButton UpButton,DownButton,LeftButton,RightButton;
	private ImageView imgView;
	private TextView textview;
	private String imgURL;
	private Handler tHandler = new Handler();
	private ToggleButton togglemode;
	private boolean automode;
	private LinearLayout manualcontrols;
	private AlertDialog alertDialog;
	
	
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
        
        /*
        //imgURL = "http://www.google.co.in/images/srpr/logo3w.png";
        imgURL = "http://192.168.137.16/snapshot.jpg";
        */
        textview = (TextView) findViewById(R.id.textview);
        //imgView = (ImageView) findViewById(R.id.imgView);
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
            } else {
            	manualcontrols.setVisibility(LinearLayout.VISIBLE);
            }
        }
    };
    
}
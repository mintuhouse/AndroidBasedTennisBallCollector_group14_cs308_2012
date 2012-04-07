package com.eyantra.android.tennisball;


import java.io.InputStream;
import java.net.URL;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;
import android.view.View.OnClickListener;

public class BallCollectorActivity extends Activity {
	private WebView VideoFrame;
	private Button UpButton,DownButton,LeftButton,RightButton;
	private ImageView imgView;
	private TextView textview;
	private String imgURL;
	private Handler tHandler = new Handler();
	
	/*	
 	 * Open the webpage inline in the application
    private class VideoWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }		
	*/
	
	private Drawable grabImageFromUrl(String url) throws Exception {
		return Drawable.createFromStream((InputStream)new URL(url).getContent(), "src");
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
        
        /*
         * Open the Video stream in application     
        VideoFrame = (WebView) findViewById(R.id.VideoFrameView);
        VideoFrame.getSettings().setJavaScriptEnabled(true);
        VideoFrame.loadUrl("http://www.google.com");        
        VideoFrame.setWebViewClient(new VideoWebViewClient()); 
         */
        
        imgURL = "http://www.google.co.in/images/srpr/logo3w.png";
        textview = (TextView) findViewById(R.id.textview);
        imgView = (ImageView) findViewById(R.id.imgView);
        //new ReloadImageView(this, 500, imgView, imgURL, textview);

        tHandler.removeCallbacks(UpdateImageTask);
        tHandler.postDelayed(UpdateImageTask, 100);
        
        addListenerOnJoyStick();
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
    
    @Override
    public void onConfigurationChanged(Configuration newConfig){        
        super.onConfigurationChanged(newConfig);
    }
    
    public void addListenerOnJoyStick(){
    	UpButton = (Button) findViewById(R.id.uparrow);
    	/*UpButton.setOnClickListener(new OnClickListener() {
    		public void onClick(View arg0) {
    			
    		}
    	});	*/
    }
 
    
}
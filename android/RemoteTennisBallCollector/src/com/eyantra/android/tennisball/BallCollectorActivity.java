package com.eyantra.android.tennisball;


import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;
import android.view.View.OnClickListener;

public class BallCollectorActivity extends Activity {
	WebView VideoFrame;
	Button UpButton,DownButton,LeftButton,RightButton;
	ImageView imgView;
	TextView textview;
	
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
        
        String imgURL = "http://www.google.co.in/images/srpr/logo3w.png";
        textview = (TextView) findViewById(R.id.textview);
        imgView = (ImageView) findViewById(R.id.imgView);
        new ReloadImageView(this, 500, imgView, imgURL, textview);
        
        addListenerOnJoyStick();
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
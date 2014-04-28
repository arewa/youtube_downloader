package com.youtubedownloader.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.SpannableStringBuilder;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.youtubedownloader.R;
import com.youtubedownloader.eula.Eula;
import com.youtubedownloader.utils.AppMode;
import com.youtubedownloader.utils.ParseUtils;

public class MainScreenActivity extends Activity {
	
	private AdView adViewBannerTop;
	private AdView adViewBannerBottom;
	
	private TextView textView;
	private EditText editText;
	private Button button;
	
	private String videoId;
	
	private float TEXT_SIZE = 26;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainscreen);
        
        RelativeLayout layout = (RelativeLayout)findViewById(R.id.mainScreenLayout);
        
        showAdvertising();
        
        textView = new TextView(this);
        textView.setId(R.id.explanationText);
        textView.setText(R.string.mainscreen_video_url_box_label);
        textView.setTextSize(TEXT_SIZE);
        textView.setPadding(0, 20, 0, 0);
        RelativeLayout.LayoutParams textViewLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        textViewLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        textViewLayoutParams.addRule(RelativeLayout.BELOW, R.id.mainScreenTopBanner);
        textView.setLayoutParams(textViewLayoutParams);
        layout.addView(textView);
        
        editText = new EditText(this);
        editText.setId(R.id.videoText);
        RelativeLayout.LayoutParams editTextLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        editTextLayoutParams.addRule(RelativeLayout.BELOW, R.id.explanationText);
        editTextLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        editText.setLayoutParams(editTextLayoutParams);
        registerForContextMenu(editText);
        layout.addView(editText);
        
        button = new Button(this);
        button.setId(R.id.search);
        button.setText(R.string.mainscreen_search_button_label);
        RelativeLayout.LayoutParams buttonLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        buttonLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        buttonLayoutParams.addRule(RelativeLayout.BELOW, R.id.videoText);
        button.setLayoutParams(buttonLayoutParams);
        
        button.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				EditText videoText = (EditText)findViewById(R.id.videoText);
				if (!validateInput(SpannableStringBuilder.valueOf(videoText.getText()).toString())) {
					Toast toast = Toast.makeText(getApplicationContext(), R.string.mainscreen_error_video_input_validation, Toast.LENGTH_SHORT);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
				} else {
					Intent intent = new Intent(v.getContext(), VideoInfoActivity.class);
					intent.putExtra("videoId", videoId);
					startActivity(intent);
				}
			}
        });
        
        layout.addView(button);
        
        Eula.show(this);
    }
    
    @Override
	protected void onResume() {
    	load();
		super.onResume();
	}

    @Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	menu.clear();
    	menu.clearHeader();
		menu.add(R.string.mainscreen_contextmenu_paste);
		
		ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
		MenuItem pasteItem = menu.findItem(0);
		
		if (clipboard.hasText()) {
			pasteItem.setEnabled(true);
		} else {
			pasteItem.setEnabled(false);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		
		ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
		
		if (clipboard.hasText()) {
			EditText videoText = (EditText)findViewById(R.id.videoText);
			videoText.setText(clipboard.getText());
		}
		
		return super.onContextItemSelected(item);
	}
	
	private boolean validateInput(String input) {
		if (input == null || "".equals(input)) {
			return false;
		}
		
		videoId = ParseUtils.getYouTubeVideoIdFromUrl(input);
		if ("".equals(videoId)) {
			return false;
		}
		
		return true;
	}

	private void load() {
    	
    }
	
	@Override
	public void onDestroy() {
		adViewBannerTop.destroy();
		adViewBannerBottom.destroy();
		super.onDestroy();
	}
	
	private void showAdvertising() {
		
		RelativeLayout layout = (RelativeLayout)findViewById(R.id.mainScreenLayout);
		
		// Advertising
        adViewBannerBottom = new AdView(this, AdSize.BANNER, AppMode.AD_PUBLISHER_ID);
        RelativeLayout.LayoutParams adBannerBottomLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        adBannerBottomLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        adViewBannerBottom.setLayoutParams(adBannerBottomLayoutParams);
        layout.addView(adViewBannerBottom);
        
        AdRequest adBannerBottomRequest = new AdRequest();
        if (AppMode.TEST_MODE) {
        	adBannerBottomRequest.addTestDevice(AdRequest.TEST_EMULATOR);// Emulator
        	adBannerBottomRequest.addTestDevice(AppMode.TEST_DEVICE_ID);// Test Android Device
        }
        adViewBannerBottom.loadAd(adBannerBottomRequest);
        
        adViewBannerTop = new AdView(this, AdSize.BANNER, AppMode.AD_PUBLISHER_ID);
        adViewBannerTop.setId(R.id.mainScreenTopBanner);
        RelativeLayout.LayoutParams adBannerTopLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        adBannerTopLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        adViewBannerTop.setLayoutParams(adBannerTopLayoutParams);
        layout.addView(adViewBannerTop);
        
        AdRequest adBannerTopRequest = new AdRequest();
        if (AppMode.TEST_MODE) {
        	adBannerTopRequest.addTestDevice(AdRequest.TEST_EMULATOR);// Emulator
        	adBannerTopRequest.addTestDevice(AppMode.TEST_DEVICE_ID);// Test Android Device
        }
        adViewBannerTop.loadAd(adBannerTopRequest);
	}
}
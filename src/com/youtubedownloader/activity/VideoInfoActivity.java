package com.youtubedownloader.activity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.youtubedownloader.R;
import com.youtubedownloader.model.YouTubeVideoInfo;
import com.youtubedownloader.utils.AppMode;
import com.youtubedownloader.utils.ParseUtils;

public class VideoInfoActivity extends Activity {
	
	private static float TEXT_SIZE = 16;
	
	private static int REQUEST_SAVE = 0;
	
	private ProgressDialog progressVideoInfoDialog;
	private ProgressDialog progressVideoDownloadDialog;
	
	private String videoId;
	private YouTubeVideoInfo videoInfo;
	private String downloadPath;
	private String downloadFileName;
	private String downloadUrl;
	
	private AdView adViewBannerTop;
	private AdView adViewBannerBottom;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.videoinfoscreen);
        
        showAdvertising();
        
        videoId = getIntent().getExtras().getString("videoId");
		
		if (videoId == null || "".equals(videoId)) {
			Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.videoinfoscreen_empty_videoid), Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
			return;
		}
		
		progressVideoInfoDialog = ProgressDialog.show(VideoInfoActivity.this, "", getString(R.string.videoinfoscreen_loading), true);
		
		GetVideoInfoThread videoInfoThread = new GetVideoInfoThread(videoInfoHandler);
		videoInfoThread.start();
	}
	
	@Override
	protected void onResume() {
    	reload();
		super.onResume();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {

			downloadPath = data.getStringExtra(FileDialogActivity.RESULT_PATH);
			
			showDownloadButton();

	    } else if (resultCode == Activity.RESULT_CANCELED) {
	    	
	    	downloadPath = "";
	    	
	    	Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.videoinfoscreen_output_folder_not_selected), Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
	    }
	}
	
	@Override
	public void onDestroy() {
		adViewBannerTop.destroy();
		adViewBannerBottom.destroy();
		super.onDestroy();
	}

	private void reload() {
		
	}
	
	private Handler videoInfoHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        		try {
	        		progressVideoInfoDialog.dismiss();
	        		if (videoInfo == null) {
	        			Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.videoinfoscreen_not_obtain_video_info), Toast.LENGTH_SHORT);
	        			toast.setGravity(Gravity.CENTER, 0, 0);
	        			toast.show();
	        		} else {
	        			showImageInfo();
	        		}
        		} catch (Exception e) {
        			Log.e("E", e.getMessage(), e);
        		}
        }
	};
	
	private Handler videoDownloadHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			try {
				int total = msg.arg1;
				progressVideoDownloadDialog.setProgress(total);
				progressVideoDownloadDialog.setMessage(getString(R.string.videoinfoscreen_downloading) + " (" + msg.arg2 + "KB/s)");
				if (total >= 100) {
					progressVideoDownloadDialog.dismiss();
					
					Spinner formatSpinner = (Spinner)findViewById(R.id.formatSpinner);
					formatSpinner.setEnabled(false);
					
					Button downloadButton = (Button)findViewById(R.id.downloadButtonView);
					downloadButton.setEnabled(false);
					
					Button selectFolderButton = (Button)findViewById(R.id.selectFolderButtonView);
					selectFolderButton.setEnabled(false);
					
					Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.videoinfoscreen_download_complete), Toast.LENGTH_LONG);
	    			toast.setGravity(Gravity.CENTER, 0, 0);
	    			toast.show();
	    			
	    			showBackButton();
				}
			} catch (Exception e) {
				Log.e("E", e.getMessage(), e);
			}
		}
	};
	
	private class GetVideoInfoThread extends Thread {
		private Handler mHandler;
		
		GetVideoInfoThread(Handler h) {
			this.mHandler = h;
		}
		
		public void run() {
			videoInfo = ParseUtils.getVideoInfo(videoId);
			mHandler.sendEmptyMessage(0);
		}
	}
	
	private class VideoDownloadThread extends Thread {
		private Handler mHandler;
		
		private static final int BUFFER_SIZE = 2048;
		private static final double ONE_HUNDRED = 100;
		private static final double KB = 1024;
		
		VideoDownloadThread(Handler h) {
			this.mHandler = h;
		}
		
		public void run() {
			FileOutputStream outstream = null;
			
			try {
				HttpGet httpget = new HttpGet(downloadUrl);
				httpget.setHeader("User-Agent", ParseUtils.USER_AGENT);
				
				HttpClient httpclient = new DefaultHttpClient();
				HttpResponse response = httpclient.execute(httpget);			

				HttpEntity entity = response.getEntity();
				if (entity != null && response.getStatusLine().getStatusCode() == 200) {
					double length = entity.getContentLength();
					if (length <= 0) {
						// Unexpected, but do not divide by zero
						length = 1;
					}
					InputStream instream = entity.getContent();
	
					File outputfile = new File(downloadPath + "/" + downloadFileName);
					
					if (outputfile.exists()) {
						outputfile.delete();
					}
					
					outstream = new FileOutputStream(outputfile);

					byte[] buffer = new byte[BUFFER_SIZE];
					double total = 0;
					int count = -1;
					int progress = 10;
					long start = System.currentTimeMillis();
					while ((count = instream.read(buffer)) != -1) {
						total += count;
						int p = (int) ((total / length) * ONE_HUNDRED);
						if (p >= progress) {
							long now = System.currentTimeMillis();
							double s = (now - start) / 1000;
							int kbpers = (int) ((total / KB) / s);
							Message msg = mHandler.obtainMessage();
			                msg.arg1 = progress;
			                msg.arg2 = kbpers;
			                mHandler.sendMessage(msg);
							progress += 10;
						}
						outstream.write(buffer, 0, count);
					}
					outstream.flush();
				}
				
			} catch (Exception e) {
				Log.e("E", e.getMessage(), e);
			} finally {
				try {
					outstream.close();
				} catch (Exception e) {
					Log.e("E", e.getMessage(), e);
				}
			}
		}
	}
	
	private void showImageInfo() {
		RelativeLayout mainLayout = (RelativeLayout)findViewById(R.id.mainVideoInfoLayout);
		
		ImageView thumbnailImageView = new ImageView(this);
		thumbnailImageView.setImageBitmap(getURLasBitmap(videoInfo.getThumbnailUrl()));
		thumbnailImageView.setPadding(0, 0, 5, 5);
		thumbnailImageView.setId(R.id.videoInfoScreenThumbImage);
		RelativeLayout.LayoutParams thumbnailImageLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		thumbnailImageLayoutParams.addRule(RelativeLayout.BELOW, R.id.videoInfoScreenTopBanner);
		thumbnailImageLayoutParams.addRule(RelativeLayout.ALIGN_LEFT);
		thumbnailImageView.setLayoutParams(thumbnailImageLayoutParams);
		mainLayout.addView(thumbnailImageView);
		
		TextView titleView = new TextView(this);
		titleView.setText(videoInfo.getTitle());
		titleView.setTextSize(TEXT_SIZE);
		titleView.setId(R.id.videoInfoScreenTitle);
		RelativeLayout.LayoutParams titleViewLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		titleViewLayoutParams.addRule(RelativeLayout.BELOW, R.id.videoInfoScreenTopBanner);
		titleViewLayoutParams.addRule(RelativeLayout.RIGHT_OF, R.id.videoInfoScreenThumbImage);
		titleViewLayoutParams.addRule(RelativeLayout.ALIGN_LEFT);
		titleView.setLayoutParams(titleViewLayoutParams);
		mainLayout.addView(titleView);
		
		TextView durationView = new TextView(this);
		durationView.setText(getString(R.string.videoinfoscreen_duration) + " " + videoInfo.getLength() + " " + getString(R.string.videoinfoscreen_sec));
		durationView.setTextSize(TEXT_SIZE);
		durationView.setId(R.id.videoInfoScreenDuration);
		RelativeLayout.LayoutParams durationViewLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		durationViewLayoutParams.addRule(RelativeLayout.RIGHT_OF, R.id.videoInfoScreenThumbImage);
		durationViewLayoutParams.addRule(RelativeLayout.BELOW, R.id.videoInfoScreenTitle);
		durationViewLayoutParams.addRule(RelativeLayout.ALIGN_LEFT);
		durationView.setLayoutParams(durationViewLayoutParams);
		mainLayout.addView(durationView);
		
		Spinner formatView = new Spinner(this);
		formatView.setId(R.id.formatSpinner);
		String formatsArray[] = new String[videoInfo.getAvailableFormats().size()];
		int pos = 0;
		for (String k : videoInfo.getAvailableFormats().keySet()) {
			formatsArray[pos] = videoInfo.getAvailableFormats().get(k).getDescr();
			pos ++;
		}
		@SuppressWarnings({ "rawtypes", "unchecked" })
		ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, formatsArray);
		formatView.setAdapter(adapter);
		
		RelativeLayout.LayoutParams formatViewLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		formatViewLayoutParams.addRule(RelativeLayout.BELOW, R.id.videoInfoScreenDuration);
		formatViewLayoutParams.addRule(RelativeLayout.BELOW, R.id.videoInfoScreenThumbImage);
		formatViewLayoutParams.addRule(RelativeLayout.ALIGN_LEFT);
		formatView.setLayoutParams(formatViewLayoutParams);
		
//		if (AppMode.FREE_MODE && videoInfo.getLength() > AppMode.FREE_MODE_DURATION) {
//			Toast toast = Toast.makeText(getApplicationContext(), 
//					String.format(getString(R.string.videoinfoscreen_free_mode_message), String.valueOf(AppMode.FREE_MODE_DURATION)), 
//					Toast.LENGTH_LONG);
//			toast.setGravity(Gravity.CENTER, 0, 0);
//			toast.show();
//			
//			showBackButton();
//			
//			return;
//		}
		
		formatView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
	        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
	        	if (downloadPath != null && !"".equals(downloadPath)) {
					showDownloadButton();
				}
	        }
	        public void onNothingSelected(AdapterView<?> parent) {
	        	
	        }
	    });
		
		mainLayout.addView(formatView);
		
		Button selectFolderButton = new Button(this);
		selectFolderButton.setText(R.string.videoinfoscreen_select_output_folder);
		selectFolderButton.setId(R.id.selectFolderButtonView);
		RelativeLayout.LayoutParams selectFolderButtonLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		selectFolderButtonLayoutParams.addRule(RelativeLayout.BELOW, R.id.formatSpinner);
		selectFolderButtonLayoutParams.addRule(RelativeLayout.ALIGN_LEFT);
		selectFolderButton.setLayoutParams(selectFolderButtonLayoutParams);
		selectFolderButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(v.getContext(), FileDialogActivity.class);
				intent.putExtra(FileDialogActivity.START_PATH, Environment.getExternalStorageDirectory().getAbsolutePath());
				startActivityForResult(intent, REQUEST_SAVE);
			}
		});
		
		mainLayout.addView(selectFolderButton);
	}
	
	private void showDownloadButton() {
		RelativeLayout mainLayout = (RelativeLayout)findViewById(R.id.mainVideoInfoLayout);
		
		File downloadPathFile = new File(downloadPath);
		
		if (!downloadPathFile.exists()) {
			downloadPathFile.mkdir();
		}
		
		Spinner formatView = (Spinner)findViewById(R.id.formatSpinner);
		String format = String.valueOf(formatView.getSelectedItem());
			
		downloadFileName = ParseUtils.cleanFilename(videoInfo.getTitle()) + "." + ParseUtils.getVideoExtByFormatDescr(format);
		
		String type = ParseUtils.getVideoTypeByFormatDescr(format);
		downloadUrl = videoInfo.getDownloadUrls().get(type);
		
		TextView downloadPathView = (TextView)findViewById(R.id.downloadPathTextView);
		if (downloadPathView == null) {
			downloadPathView = new TextView(this);
			downloadPathView.setTextSize(TEXT_SIZE);
			downloadPathView.setId(R.id.downloadPathTextView);
			RelativeLayout.LayoutParams downloadPathViewLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
			downloadPathViewLayoutParams.addRule(RelativeLayout.BELOW, R.id.selectFolderButtonView);
			downloadPathViewLayoutParams.addRule(RelativeLayout.ALIGN_LEFT);
			downloadPathView.setLayoutParams(downloadPathViewLayoutParams);
			mainLayout.addView(downloadPathView);
		}
		
		downloadPathView.setText(getString(R.string.videoinfoscreen_download_to) + downloadPath + "/" + downloadFileName);
		
		Button downloadButton = (Button)findViewById(R.id.downloadButtonView);
		if (downloadButton == null) {
			downloadButton = new Button(this);
			downloadButton.setId(R.id.downloadButtonView);
			downloadButton.setText(R.string.videoinfoscreen_download);
			RelativeLayout.LayoutParams downloadButtonLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
			downloadButtonLayoutParams.addRule(RelativeLayout.BELOW, R.id.downloadPathTextView);
			downloadButtonLayoutParams.addRule(RelativeLayout.ALIGN_LEFT);
			downloadButton.setLayoutParams(downloadButtonLayoutParams);
			downloadButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					progressVideoDownloadDialog = new ProgressDialog(VideoInfoActivity.this);
					progressVideoDownloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
					progressVideoDownloadDialog.setMessage(getString(R.string.videoinfoscreen_downloading));
					progressVideoDownloadDialog.show();
					
					VideoDownloadThread videoDownloadThread = new VideoDownloadThread(videoDownloadHandler);
					videoDownloadThread.start();
				}
			});
			mainLayout.addView(downloadButton);
		}
	}
	
	private void showBackButton() {
		RelativeLayout mainLayout = (RelativeLayout)findViewById(R.id.mainVideoInfoLayout);
		Button backButton = new Button(this);
		backButton.setText(R.string.videoinfoscreen_back);
		RelativeLayout.LayoutParams backButtonLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		backButtonLayoutParams.addRule(RelativeLayout.BELOW, R.id.downloadPathTextView);
		backButtonLayoutParams.addRule(RelativeLayout.ALIGN_LEFT);
		backButton.setLayoutParams(backButtonLayoutParams);
		
		mainLayout.addView(backButton);
		
		backButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				setResult(RESULT_OK);
				finish();
			}
		});
	}

	private Bitmap getURLasBitmap(String url) {
		Bitmap bmImg;
        try {
             URL bitmapUrl = new URL(url);
             HttpURLConnection conn= (HttpURLConnection) bitmapUrl.openConnection();
             conn.setDoInput(true);
             conn.connect();
             InputStream is = conn.getInputStream();
             
             bmImg = BitmapFactory.decodeStream(is);
        } catch (IOException e) {
        	return null;
        }
        return bmImg;
	}
	
	private void showAdvertising() {
		
		RelativeLayout layout = (RelativeLayout)findViewById(R.id.mainVideoInfoLayout);
		
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
        adViewBannerTop.setId(R.id.videoInfoScreenTopBanner);
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
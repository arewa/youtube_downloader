package com.youtubedownloader.model;

import java.util.HashMap;
import java.util.Map;

public class YouTubeVideoInfo {
	
	private String title;
	
	private Map<String, String> downloadUrls = new HashMap<String, String>();
	
	private Map<String, YouTubeVideoFormat> availableFormats = new HashMap<String, YouTubeVideoFormat>();
	
	private String thumbnailUrl;
	
	private int length = 0;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Map<String, String> getDownloadUrls() {
		return downloadUrls;
	}

	public void setDownloadUrls(Map<String, String> downloadUrls) {
		this.downloadUrls = downloadUrls;
		fillAvailableFormats();
	}
	
	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

	public Map<String, YouTubeVideoFormat> getAvailableFormats() {
		return availableFormats;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	private void fillAvailableFormats() {
		for (String k : downloadUrls.keySet()) {
			if ("5".equals(k)) {
				availableFormats.put(k, YouTubeVideoFormat.FLV_240P);
			} else if ("34".equals(k)) {
				availableFormats.put(k, YouTubeVideoFormat.FLV_360P);
			} else if ("35".equals(k)) {
				availableFormats.put(k, YouTubeVideoFormat.FLV_480P);
			} else if ("18".equals(k)) {
				availableFormats.put(k, YouTubeVideoFormat.MP4_360P);
			} else if ("22".equals(k)) {
				availableFormats.put(k, YouTubeVideoFormat.MP4_720P_HD);
			} else if ("37".equals(k)) {
				availableFormats.put(k, YouTubeVideoFormat.MP4_1080P_HD);
			} else if ("38".equals(k)) {
				availableFormats.put(k, YouTubeVideoFormat.MP4_4K_HD);
			} else if ("43".equals(k)) {
				availableFormats.put(k, YouTubeVideoFormat.WEBM_360P);
			} else if ("44".equals(k)) {
				availableFormats.put(k, YouTubeVideoFormat.WEBM_480P);
			} else if ("45".equals(k)) {
				availableFormats.put(k, YouTubeVideoFormat.WEBM_720P_HD);
			}
		}
	}
}

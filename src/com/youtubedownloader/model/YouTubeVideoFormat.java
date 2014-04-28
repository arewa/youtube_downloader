package com.youtubedownloader.model;

public enum YouTubeVideoFormat {
	
	FLV_240P("5"),
	FLV_360P("34"),
	FLV_480P("35"),
	MP4_360P("18"),
	MP4_720P_HD("22"),
	MP4_1080P_HD("37"),
	MP4_4K_HD("38"),
	WEBM_360P("43"),
	WEBM_480P("44"),
	WEBM_720P_HD("45");
	
	private final String key;
	private final String ext;
	
	private YouTubeVideoFormat(final String key) {
		this.key = key;
		if ("5".equals(key) || "34".equals(key) || "35".equals(key)) {
			this.ext = "flv";
		} else if ("43".equals(key) || "44".equals(key) || "45".equals(key)) {
			this.ext = "webm";
		} else {
			this.ext = "mp4";
		}
	}
	
	public String getKey() {
		return this.key;
	}
	
	public String getExt() {
		return this.ext;
	}
	
	public String getDescr() {
		if ("5".equals(key)) {
			return "FLV 320 x 240";
		} else if ("34".equals(key)) {
			return "FLV 640 x 360";
		} else if ("35".equals(key)) {
			return "FLV 854 x 480";
		} else if ("18".equals(key)) {
			return "MP4 640 x 360";
		} else if ("22".equals(key)) {
			return "MP4 1280 x 720";
		} else if ("37".equals(key)) {
			return "MP4 1920 x 1080";
		} else if ("38".equals(key)) {
			return "MP4 4096 x 1714";
		} else if ("43".equals(key)) {
			return "WEBM 640 x 360";
		} else if ("44".equals(key)) {
			return "WEBM 854 x 480";
		} else if ("45".equals(key)) {
			return "WEBM 1280 x 720";
		}
		
		return "Unknown";
	}
}

package com.youtubedownloader.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.util.Log;

import com.youtubedownloader.model.YouTubeVideoFormat;
import com.youtubedownloader.model.YouTubeVideoInfo;

public class ParseUtils {
	
	public static final String DEFAULT_ENCODING = "UTF-8";
	public static final String SCHEME = "http";
	public static final String HOST = "www.youtube.com";
	public static final String USER_AGENT = "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13";
	private static final char[] ILLEGAL_FILENAME_CHARACTERS = { '/', '\n',
		'\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"',
		':' };
	
	public static String getYouTubeVideoIdFromUrl(String input) {
		
		String videoId = "";
		
		URL url;
		try {
			url = new URL(input);
		} catch (MalformedURLException e) {
			Log.e("E", e.getMessage(), e);
			return "";
		}
		
		List<NameValuePair> queryInfo = new ArrayList<NameValuePair>();
		
		try {
			URLEncodedUtils.parse(queryInfo, new Scanner(url.getQuery()), DEFAULT_ENCODING);
		} catch (Exception e) {
			Log.e("E", e.getMessage(), e);
			return "";
		}
		
		for (NameValuePair queryParam : queryInfo) {
			String pKey = queryParam.getName();
			String pVal = queryParam.getValue();
			
			if ("v".equals(pKey)) {
				videoId = pVal;
				break;
			}
		}
		
		if ("".equals(videoId)) {
			try {
				URLEncodedUtils.parse(queryInfo, new Scanner(url.getRef()), DEFAULT_ENCODING);
			} catch (Exception e) {
				Log.e("E", e.getMessage(), e);
				return "";
			} 
			
			for (NameValuePair queryParam : queryInfo) {
				String pKey = queryParam.getName();
				String pVal = queryParam.getValue();
				
				if ("v".equals(pKey)) {
					videoId = pVal;
					break;
				}
			}
		}
		
		return videoId;
	}
	
	public static YouTubeVideoInfo getVideoInfo(String videoId) {
		YouTubeVideoInfo result = new YouTubeVideoInfo();
		
		Map<String, String> downloadUrls = new HashMap<String, String>();
		String title = videoId;
		String thumbnailUrl = "";
		int length = 0;
		
		List<NameValuePair> qparams = new ArrayList<NameValuePair>();
		qparams.add(new BasicNameValuePair("video_id", videoId));
		
		URI uri;
		
		try {
			uri = getUri("get_video_info", qparams);
		} catch (URISyntaxException e) {
			Log.e("E", e.getMessage(), e);
			return null;
		}
		
		CookieStore cookieStore = new BasicCookieStore();
		HttpContext localContext = new BasicHttpContext();
		localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(uri);
		httpget.setHeader("User-Agent", USER_AGENT);
		
		HttpResponse response = null;
		
		try {
			response = httpclient.execute(httpget, localContext);
		} catch (ClientProtocolException e) {
			Log.e("E", e.getMessage(), e);
			return null;
		} catch (IOException e) {
			Log.e("E", e.getMessage(), e);
			return null;
		}
		
		HttpEntity entity = response.getEntity();
		if (entity != null && response.getStatusLine().getStatusCode() == 200) {
			InputStream instream;
			try {
				instream = entity.getContent();
			} catch (IllegalStateException e) {
				Log.e("E", e.getMessage(), e);
				return null;
			} catch (IOException e) {
				Log.e("E", e.getMessage(), e);
				return null;
			}
			String videoInfo = "";
			try {
				videoInfo = getStringFromInputStream(DEFAULT_ENCODING, instream);
			} catch (UnsupportedEncodingException e) {
				Log.e("E", e.getMessage(), e);
				return null;
			} catch (IOException e) {
				Log.e("E", e.getMessage(), e);
				return null;
			}
			
			if (videoInfo != null && videoInfo.length() > 0) {
				List<NameValuePair> infoMap = new ArrayList<NameValuePair>();
				URLEncodedUtils.parse(infoMap, new Scanner(videoInfo), DEFAULT_ENCODING);
				
				for (NameValuePair pair : infoMap) {
					String key = pair.getName();
					String val = pair.getValue();
					if (key.equals("title")) {
						title = val;
					} else if (key.equals("thumbnail_url")) {
						thumbnailUrl = URLDecoder.decode(val);
					} else if (key.equals("length_seconds")) {
						length = Integer.valueOf(val);
					} else if (key.equals("url_encoded_fmt_stream_map")) {
						String[] urls = val.split(",");
						for (String s : urls) {
							// s => "url=http%3A%2F%2Fo-o.preferred....."
							String encodedUrl = s.substring(4);
							encodedUrl = encodedUrl.replace("%3A", ":");
							encodedUrl = encodedUrl.replace("%2F", "/");
							encodedUrl = encodedUrl.replace("%3F", "?");
							
							URL url = null;
							try {
								url = new URL(encodedUrl);
							} catch (MalformedURLException e) {
								Log.e("E", e.getMessage(), e);
								return null;
							}
							List<NameValuePair> queryInfo = new ArrayList<NameValuePair>();
							URLEncodedUtils.parse(queryInfo, new Scanner(url.getQuery()), DEFAULT_ENCODING);
							String videoType = "";
							String videoUrlQuery = "";
							for (NameValuePair queryParam : queryInfo) {
								// Example of queryInfo keys and values:
								// "sparams=id%2Cexpire%2Cip%2Cipbits%2Citag%2Csource%2Cratebypass%2Ccp&fexp=904510%2C906918&itag=43&ip=91.0.0.0&signature=4446954A7A68846ADAB47B340F65606881A868D5.4769920F37B1D5AC825D407C80F0A7FA60064AF1&sver=3&ratebypass=yes&source=youtube&expire=1327939481&key=yt1&ipbits=8&cp=U0hRTFlOV19NS0NOMV9RSEFFOjdmSXBoYnJTSHBG&id=ca68bf58483ed336" => "null"
								// "quality" => "medium"
								// "fallback_host" => "tc.v5.cache7.c.youtube.com"
								// "type" => "video/webm; codecs="vp8.0, vorbis"
								// "itag" => "43"
								String pKey = queryParam.getName();
								String pVal = queryParam.getValue();
								if ("itag".equals(pKey)) {
									videoType = pVal;
								} else {
									if (!"quality".equals(pKey) && 
										!"fallback_host".equals(pKey) &&
										!"type".equals(pKey)) {
										videoUrlQuery = pKey;
									}
								}
							}
							
							if (videoType != null && !"".equals(videoType) && 
								videoUrlQuery != null && !"".equals(videoUrlQuery) &&
								!downloadUrls.containsKey(videoType)) {
								String videoUrl = url.getProtocol() + "://" + url.getHost() + url.getPath() + "?" + videoUrlQuery;
								downloadUrls.put(videoType, videoUrl);
							}
						}
					}
				}
			}
		}
		
		result.setDownloadUrls(downloadUrls);
		result.setTitle(title);
		result.setThumbnailUrl(thumbnailUrl);
		result.setLength(length);
		
		return result;
	}
	
	public static String getVideoExtByFormatDescr(String d) {
		if ("FLV 320 x 240".equals(d)) {
			return "flv";
		} else if ("FLV 640 x 360".equals(d)) {
			return "flv";
		} else if ("FLV 854 x 480".equals(d)) {
			return "flv";
		} else if ("MP4 640 x 360".equals(d)) {
			return "mp4";
		} else if ("MP4 1280 x 720".equals(d)) {
			return "mp4";
		} else if ("MP4 1920 x 1080".equals(d)) {
			return "mp4";
		} else if ("MP4 4096 x 1714".equals(d)) {
			return "mp4";
		} else if ("WEBM 640 x 360".equals(d)) {
			return "webm";
		} else if ("WEBM 854 x 480".equals(d)) {
			return "webm";
		} else if ("WEBM 1280 x 720".equals(d)) {
			return "webm";
		}
		
		return "Unknown";
	}
	
	public static String getVideoTypeByFormatDescr(String t) {
		if ("FLV 320 x 240".equals(t)) {
			return "5";
		} else if ("FLV 640 x 360".equals(t)) {
			return "34";
		} else if ("FLV 854 x 480".equals(t)) {
			return "35";
		} else if ("MP4 640 x 360".equals(t)) {
			return "18";
		} else if ("MP4 1280 x 720".equals(t)) {
			return "22";
		} else if ("MP4 1920 x 1080".equals(t)) {
			return "37";
		} else if ("MP4 4096 x 1714".equals(t)) {
			return "38";
		} else if ("WEBM 640 x 360".equals(t)) {
			return "43";
		} else if ("WEBM 854 x 480".equals(t)) {
			return "44";
		} else if ("WEBM 1280 x 720".equals(t)) {
			return "45";
		}
		
		return "Unknown";
	}
	
	public static String cleanFilename(String filename) {
		for (char c : ILLEGAL_FILENAME_CHARACTERS) {
			filename = filename.replace(c, '_');
		}
		return filename;
	}
	
	private static URI getUri(String path, List<NameValuePair> qparams)
			throws URISyntaxException {
		URI uri = URIUtils.createURI(SCHEME, HOST, -1, "/" + path,
				URLEncodedUtils.format(qparams, DEFAULT_ENCODING), null);
		return uri;
	}
	
	private static String getStringFromInputStream(String encoding,
			InputStream instream) throws UnsupportedEncodingException,
			IOException {
		Writer writer = new StringWriter();

		char[] buffer = new char[1024];
		try {
			Reader reader = new BufferedReader(new InputStreamReader(instream,
					encoding));
			int n;
			while ((n = reader.read(buffer)) != -1) {
				writer.write(buffer, 0, n);
			}
		} finally {
			instream.close();
		}
		String result = writer.toString();
		return result;
	}
}

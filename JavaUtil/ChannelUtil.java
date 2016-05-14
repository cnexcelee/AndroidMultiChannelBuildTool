package com.ab.activity;

import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public class ChannelUtil {
	private static final String CHANNEL_ID_KEY = "bjchannel_id";
	private static final String CHANNEL_NAME_KEY = "bjchannel_name";
	// private static final String CHANNEL_KEY = "bjchannel";
	private static final String CHANNEL_VERSION_KEY = "bjchannel_version";
	private static ChannelModel mChannel;
	
	/**
	 * 获取渠道信息
	* @Title: getChannel 
	* @param @param context
	* @param @return   
	* @return ChannelModel
	* @Description:
	 */
	public static ChannelModel getChannel(Context context){
		return getChannel(context, getDefaultChannel());
	}
	/**
	 * 返回市场。
	 * 
	 * @param context
	 * @return
	 */
	private static ChannelModel getChannel(Context context, ChannelModel defaultChannel){
		// 内存中获取
		if (mChannel != null && !TextUtils.isEmpty(mChannel.getChannelId())) {
			return mChannel;
		}
		// sp中获取
		mChannel = getChannelBySharedPreferences(context);
		if (mChannel != null && !TextUtils.isEmpty(mChannel.getChannelId())) {
			return mChannel;
		}
		// 从apk中获取
		mChannel = getChannelFromApk(context);
		if (mChannel != null && !TextUtils.isEmpty(mChannel.getChannelId())) {
			// 保存sp中备用
			saveChannelBySharedPreferences(context, mChannel);
			return mChannel;
		} 
		return defaultChannel;
	}

	/**
	 * @Title: setDefaultChannel
	 * @param
	 * @return void
	 * @Description:设置默认渠道信息
	 */
	private static ChannelModel getDefaultChannel() {
		ChannelModel channel = new ChannelModel();
		//默认渠道ID
		channel.setChannelId("0000");
		//默认渠道名称
		channel.setChannelName("BJ");
		return channel;
	}

	/**
	 * 从apk中获取版本信息
	 * 
	 * @param context
	 * @param channelKey
	 * @return
	 */
	private static ChannelModel getChannelFromApk(Context context) {
		// 从apk包中获取
		ApplicationInfo appinfo = context.getApplicationInfo();
		String sourceDir = appinfo.sourceDir;
		// 默认放在meta-inf/里， 所以需要再拼接一下
		String key = "META-INF/bjchannel";
		String ret = "";
		ZipFile zipfile = null;
		try {
			zipfile = new ZipFile(sourceDir);
			Enumeration<?> entries = zipfile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = ((ZipEntry) entries.nextElement());
				String entryName = entry.getName();
				if (entryName.startsWith(key)) {
					ret = entryName;
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (zipfile != null) {
				try {
					zipfile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		String[] split = ret.split("_");
		ChannelModel channelModel = new ChannelModel();
		if (split != null && split.length >= 3) {
			channelModel.setChannelId(split[1]);
			channelModel.setChannelName(split[2]);
		}
		return channelModel;
	}

	/**
	 * 本地保存channel & 对应版本号
	 * 
	 * @param context
	 * @param channel
	 */
	private static void saveChannelBySharedPreferences(Context context, ChannelModel channel) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putString(CHANNEL_ID_KEY, channel.getChannelId());
		editor.putString(CHANNEL_NAME_KEY, channel.getChannelName());
		editor.putInt(CHANNEL_VERSION_KEY, getVersionCode(context));
		editor.commit();
	}

	/**
	 * 从sp中获取channel
	 * 
	 * @param context
	 * @return 为空表示获取异常、sp中的值已经失效、sp中没有此值
	 */
	private static ChannelModel getChannelBySharedPreferences(Context context) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		ChannelModel channel = new ChannelModel();
		int currentVersionCode = getVersionCode(context);
		if (currentVersionCode == -1) {
			// 获取错误
			return null;
		}
		int versionCodeSaved = sp.getInt(CHANNEL_VERSION_KEY, -1);
		if (versionCodeSaved == -1) {
			// 本地没有存储的channel对应的版本号
			// 第一次使用 或者 原先存储版本号异常
			return null;
		}
		if (currentVersionCode != versionCodeSaved) {
			return null;
		}
		channel.setChannelId(sp.getString(CHANNEL_ID_KEY, ""));
		channel.setChannelName(sp.getString(CHANNEL_NAME_KEY, ""));

		return channel;
	}

	/**
	 * 从包信息中获取版本号
	 * 
	 * @param context
	 * @return
	 */
	private static int getVersionCode(Context context) {
		try {
			return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return -1;
	}

	static class ChannelModel {

		/** 渠道Id **/
		private String channelId;
		/** 渠道Name **/
		private String channelName;

		public String getChannelId() {
			return channelId;
		}

		public void setChannelId(String channelId) {
			this.channelId = channelId;
		}

		public String getChannelName() {
			return channelName;
		}

		public void setChannelName(String channelName) {
			this.channelName = channelName;
		}

	}
}

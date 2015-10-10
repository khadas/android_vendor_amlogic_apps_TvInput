package com.droidlogic.utils.tunerinput.tvutil;

import android.util.Log;
import android.content.Context;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Looper;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import com.droidlogic.utils.tunerinput.tvutil.TVChannelParams;
import com.droidlogic.utils.tunerinput.tvutil.TVMultilingualText;
import com.droidlogic.utils.tunerinput.tvutil.MapUtil;
import com.droidlogic.utils.tunerinput.data.ChannelInfo;

import android.amlogic.Tv;

public class Scanner implements Tv.ScannerEventListener{
	private static final String TAG = "Scanner";

	private static final int MSG_SCAN_EVENT = 1000;

	private Handler mHandler;
	private Tv mTv = Tv.open();
	private Context context;
	private String mInputId;

	List<ChannelInfo> mChannels;
	private int channel_count=0;
	private boolean isDTV = true;

	public Scanner(Context mtx, Looper looper) {
		mHandler = new Handler(looper){
			@Override
			public void handleMessage(Message msg) {
				Log.d(TAG, "handleEvt:" + msg.what);
				if (msg.what == MSG_SCAN_EVENT) {
					resolveScanEvent((Tv.ScannerEvent)msg.obj);
				}
			}
		};

		context = mtx;
	}

	public void setInputId(String id) {
		mInputId = id;
	}

	public void startDTV(int start)
	{
		startDTV(start, start, -1);
	}

	public void startDTV()
	{
		startDTV(0, 0, -1);
	}

	public void startDTV(int start, int end, int para){
		Log.d(TAG, "DTV Start Scan");

		stop();

		channel_count = 0;
		mChannels = new ArrayList<ChannelInfo>();

		if (mTv != null) {
			mTv.setScannerListener(this);
			if (start == 0 && end == 0)
				mTv.DtvAutoScan();
			else
				mTv.DtvManualScan(start, end, para);
		}

		onStart();

		isDTV = true;
	}

	public void startATV(int start, int end, int para1, int para2)
	{
		Log.d(TAG, "ATV Start Scan");

		stop();

		channel_count = 0;
		mChannels = new ArrayList<ChannelInfo>();

		if (mTv != null) {
			mTv.setScannerListener(this);
			if (start == 0 && end == 0)
				mTv.AtvAutoScan(Tv.atv_video_std_e.valueOf(para1), Tv.atv_audio_std_e.valueOf(para2));
			else
				mTv.AtvManualScan(start, end, Tv.atv_video_std_e.valueOf(para1), Tv.atv_audio_std_e.valueOf(para2));
		}

		onStart();

		isDTV = false;
	}

	public void stop(){
		if (mTv != null)
			mTv.DtvStopScan();
	}

	@Override
	public void onEvent(Tv.ScannerEvent event) {
		Log.d(TAG, "Channel scan event: " + event.type);
		Message msg = mHandler.obtainMessage(MSG_SCAN_EVENT, event);
		mHandler.sendMessage(msg);
	}

	void resolveScanEvent(Tv.ScannerEvent event) {
		String name = null;
		switch (event.type) {
			case Tv.EVENT_DTV_PROG_DATA: {
				try{
					String composedName = new String(event.programName);
					name = TVMultilingualText.getText(composedName);
					if (name == null || name.isEmpty()) {
						name = TVMultilingualText.getText(composedName, "first");
					}
				}catch (Exception e){
					e.printStackTrace();
					name = "????";
				}

				// collect channels .
				int number = event.serviceID;
				if (number == 0)
					number = channel_count;

				ChannelInfo channel = new ChannelInfo(String.valueOf(number), name,
									null,
									event.orig_net_id, event.ts_id, event.serviceID,
									0, 0,
									event.mode,
									event.srvType,
									event.freq, event.bandwidth,
									event.vid, event.vfmt,
									event.aids, event.afmts,
									event.pcr,
									0,0,0,0 );

				channel_count += 1;

				mChannels.add(channel);

				Log.d(TAG,"STORE_SERVICE: "+channel.toString());

				onProgress(event.precent, event.freq, channel_count, name);
			}
			break;

			case Tv.EVENT_SCAN_PROGRESS: {
				Log.d(TAG, "Progress: " + event.precent + "%" + ", channel no. "+event.serviceID);

				if (event.programName != null) {
					try{
						String composedName = new String(event.programName);
						name = TVMultilingualText.getText(composedName);
						if (name == null || name.isEmpty()) {
							name = TVMultilingualText.getText(composedName, "first");
						}
						Log.d(TAG, "New Program : "+ name + ", type "+ event.srvType);
					}catch (Exception e){
						e.printStackTrace();
					}
				}

				onProgress(event.precent, event.freq, channel_count, name);
			}
			break;

			case Tv.EVENT_ATV_PROG_DATA: {
				ChannelInfo channel = new ChannelInfo("A "+String.valueOf(channel_count), event.programName,
						null,
						0, 0, 0,
						0, 0,
						3,
						event.srvType,
						event.freq,
						0 ,//bandwidth
						0,// videoPID
						0,//videoFormat,
						null,// audioPIDs[],
						null,// audioFormats[],
						0,// pcrPID,
						event.videoStd,
						event.audioStd,
						event.isAutoStd,
						0);
				channel_count += 1;
				mChannels.clear();
				mChannels.add(channel);
				TvContractUtils.updateChannelsATV(context, mInputId, mChannels);
			}
			break;

			case Tv.EVENT_STORE_END:
				Log.d(TAG, "Store end");

				if (isDTV)
					//update channels found.
					TvContractUtils.updateChannelsDTV(context, mInputId, mChannels);

				onStop();
			break;

			case Tv.EVENT_SCAN_END:
				Log.d(TAG, "Scan end");
				stop();
			break;

			default:
			break;
		}
	}

	public void onStart() {
	}

	public void onProgress(int progress, int para1, int para2, String msg) {
	}

	public void onStop() {
	}

}


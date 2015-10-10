/*
 * Copyright 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.droidlogic.tvinput.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.tv.TvContentRating;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputManager.Hardware;
import android.media.tv.TvInputManager.HardwareCallback;
import android.media.tv.TvStreamConfig;
import android.media.tv.TvInputService;
import android.media.tv.TvTrackInfo;
import android.media.tv.TvTrackInfo;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputHardwareInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.CaptioningManager;

import com.droidlogic.utils.tunerinput.tvutil.TvContractUtils;
import com.droidlogic.utils.tunerinput.tvutil.TVChannelParams;
import com.droidlogic.utils.tunerinput.tvutil.TVConst;
import com.droidlogic.utils.tunerinput.tvutil.TVMisc;
import com.droidlogic.utils.tunerinput.data.ChannelInfo;

import com.droidlogic.tvinput.R;
import com.droidlogic.tvclient.TvClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.media.MediaPlayer;
import android.amlogic.Tv;

public class ATVInputService extends TvInputService {

	private static final String TAG = "ATVInputService";

	public static final int ATV_HW_DEVICE_ID = 0;

	private HandlerThread mHandlerThread;
	private Handler mDbHandler;
	private static TvClient client = TvClient.getTvClient();

	private ATVSessionImpl mSession;

	private final BroadcastReceiver mParentalControlsBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (mSession != null) {
				mSession.checkContentBlockNeeded();
			}
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		mHandlerThread = new HandlerThread(getClass().getSimpleName());
		mHandlerThread.start();
		mDbHandler = new Handler(mHandlerThread.getLooper());

		setTheme(android.R.style.Theme_Holo_Light_NoActionBar);

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(TvInputManager.ACTION_BLOCKED_RATINGS_CHANGED);
		intentFilter.addAction(TvInputManager.ACTION_PARENTAL_CONTROLS_ENABLED_CHANGED);
		registerReceiver(mParentalControlsBroadcastReceiver, intentFilter);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mParentalControlsBroadcastReceiver);
		mHandlerThread.quit();
		mHandlerThread = null;
		mDbHandler = null;
	}

	@Override
	public Session onCreateSession(String inputId) {
		mSession = new ATVSessionImpl(this, inputId);
		mSession.setOverlayViewEnabled(true);
		client.curSource = Tv.SourceInput_Type.SOURCE_TYPE_TV;
		return mSession;
	}

	private class ATVSessionImpl extends TvInputService.Session implements Handler.Callback {
		private static final int MSG_PLAY_PROGRAM = 1000;
		private static final int MSG_DEVICE_EVENT = 2000;

		private final Context mContext;
		private final String mInputId;
		private TvInputManager mTvInputManager;
		private Hardware mHardware;
		private TvStreamConfig[] mConfigs;
		private Surface mSurface;
		private float mVolume;
		private ChannelInfo mCurrentChannelInfo;
		private TvContentRating mLastBlockedRating;
		private TvContentRating mCurrentContentRating;
		private String mSelectedSubtitleTrackId;
		private boolean mEpgSyncRequested;
		private final Set<TvContentRating> mUnblockedRatingSet = new HashSet<>();
		private Handler mHandler;
		private PlayCurrentProgramRunnable mPlayCurrentProgramRunnable;
		private Tv mTv = TvClient.getTvInstance();

		private HardwareCallback mHardwareCallback = new HardwareCallback(){
			@Override
				public void onReleased() {
				Log.d(TAG, "====onReleased===");
			}

			@Override
			public void onStreamConfigChanged(TvStreamConfig[] configs) {
				Log.d(TAG, "===onStreamConfigChanged==");
				mConfigs = configs;
			}
		};
		private boolean isTuneNotReady = false;

		protected ATVSessionImpl(Context context, String inputId) {
			super(context);

			mContext = context;
			mInputId = inputId;
			mTvInputManager = (TvInputManager) context.getSystemService(Context.TV_INPUT_SERVICE);
			mHardware = mTvInputManager.acquireTvInputHardware(ATV_HW_DEVICE_ID,
					mHardwareCallback, mTvInputManager.getTvInputInfo(inputId));

			mLastBlockedRating = null;
			mHandler = new Handler(this);

			//TODO: ATV device initial here

		}

		@Override
		public boolean handleMessage(Message msg) {
			if (msg.what == MSG_PLAY_PROGRAM) {
				playProgram((ChannelInfo) msg.obj);
				return true;
			}
			return false;
		}

		@Override
		public void onRelease() {
			Log.d(TAG, "onRelease");

			if (mDbHandler != null) {
				mDbHandler.removeCallbacks(mPlayCurrentProgramRunnable);
			}
			releasePlayer();

			//TODO: ATV device release here
			mHardware.setSurface(null, null);
			mTvInputManager.releaseTvInputHardware(ATV_HW_DEVICE_ID, mHardware);
		}

		@Override
		public View onCreateOverlayView() {
			return null;
		}

		@Override
		public boolean onSetSurface(Surface surface) {
			Log.d(TAG, "onSetSurface: surface["+surface+"]");

			mSurface = surface;

			return true;
		}

		@Override
		public void onSurfaceChanged(int format, int width, int height) {
			Log.d(TAG, "onSurfaceChanged [fmt:"+format+", w:"+width+", h:"+height+"]");

			if (isTuneNotReady) {
				switchToSourceInput();
				isTuneNotReady = false;
			}
		}

		@Override
		public void onSetStreamVolume(float volume) {
			mVolume = volume;
		}

		private void switchToSourceInput() {
			mHardware.setSurface(mSurface, mConfigs[0]);
		}

		private boolean playProgram(ChannelInfo info) {
			Log.d(TAG, "play ch: " + info.number+"-"+info.name);
			Log.d(TAG, "["+info.toString()+"]");


			mCurrentChannelInfo = info;

			//TODO: play atv here
			mTv.PlayATVProgram(info.frequency, info.videoStd, info.audioStd,  info.fineTune);

			checkContentBlockNeeded();

			notifyVideoAvailable();

			return true;
		}

		@Override
		public boolean onTune(Uri channelUri) {
			Log.d(TAG, "onTune: url:" + channelUri.toString());

			if (mSurface == null) {//TvView is not ready
				isTuneNotReady = true;
			} else {
				switchToSourceInput();
			}

			notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);
			mUnblockedRatingSet.clear();

			mDbHandler.removeCallbacks(mPlayCurrentProgramRunnable);
			mPlayCurrentProgramRunnable = new PlayCurrentProgramRunnable(channelUri);
			mDbHandler.post(mPlayCurrentProgramRunnable);
			return true;
		}

		@Override
		public void onSetCaptionEnabled(boolean enabled) {
		}

		@Override
		public boolean onSelectTrack(int type, String trackId) {
			return true;
		}

		@Override
		public void onUnblockContent(TvContentRating rating) {
			Log.d(TAG, "onUnblockContent: rating:" + rating.flattenToString());
			if (rating != null) {
				unblockContent(rating);
			}
		}

		private void releasePlayer() {
			//TODO: stop play
			mTv.StopPlayProgram();
		}

		private void checkContentBlockNeeded() {
			if (mCurrentContentRating == null || !mTvInputManager.isParentalControlsEnabled()
					|| !mTvInputManager.isRatingBlocked(mCurrentContentRating)
					|| mUnblockedRatingSet.contains(mCurrentContentRating)) {
				// Content rating is changed so we don't need to block anymore.
				// Unblock content here explicitly to resume playback.
				unblockContent(null);
				return;
			}

			mLastBlockedRating = mCurrentContentRating;
			// Children restricted content might be blocked by TV app as well,
			// but TIS should do its best not to show any single frame of blocked content.
			releasePlayer();

			Log.d(TAG, "notifyContentBlocked [rating:"+mCurrentContentRating+"]");
			notifyContentBlocked(mCurrentContentRating);
		}

		private void unblockContent(TvContentRating rating) {
			// TIS should unblock content only if unblock request is legitimate.
			if (rating == null || mLastBlockedRating == null
					|| (mLastBlockedRating != null && rating.equals(mLastBlockedRating))) {
				mLastBlockedRating = null;
				if (rating != null) {
					mUnblockedRatingSet.add(rating);
				}
				Log.d(TAG, "notifyContentAllowed");
				notifyContentAllowed();
			}
		}

		private class PlayCurrentProgramRunnable implements Runnable {
			private static final int RETRY_DELAY_MS = 2000;
			private final Uri mChannelUri;

			public PlayCurrentProgramRunnable(Uri channelUri) {
				mChannelUri = channelUri;
			}

			@Override
			public void run() {
				ChannelInfo ch = TvContractUtils.getChannelInfoATV(
							mContext.getContentResolver(), mChannelUri);
				if (ch != null) {
					mHandler.removeMessages(MSG_PLAY_PROGRAM);
					mHandler.obtainMessage(MSG_PLAY_PROGRAM, ch).sendToTarget();
				} else {
					//Log.w(TAG, "Failed to get program info for " + mChannelUri + ". Retry in " +
					//		RETRY_DELAY_MS + "ms.");
					//mDbHandler.postDelayed(mPlayCurrentProgramRunnable, RETRY_DELAY_MS);
					Log.w(TAG, "Failed to get channel info for " + mChannelUri + ".");
				}
			}
		}
	}

	public static final class TvInput {
		public final String displayName;
		public final String name;
		public final String description;
		public final String logoThumbUrl;
		public final String logoBackgroundUrl;

		public TvInput(String displayName,
						String name,
						String description,
						String logoThumbUrl,
						String logoBackgroundUrl) {
			this.displayName = displayName;
			this.name = name;
			this.description = description;
			this.logoThumbUrl = logoThumbUrl;
			this.logoBackgroundUrl = logoBackgroundUrl;
		}
	}

	private SparseArray<TvInputInfo> mInfoList = new SparseArray<>();

	public TvInputInfo onHardwareAdded(TvInputHardwareInfo hardwareInfo) {
		if (hardwareInfo.getDeviceId() != ATV_HW_DEVICE_ID)
			return null;

		Log.d(TAG, "=====onHardwareAdded====="+hardwareInfo.toString());

		TvInputInfo info = null;
		ResolveInfo rInfo = TVMisc.getTvIntputResolveInfo(this, ATVInputService.class.getName());
		if (rInfo != null) {
			try {
				info = TvInputInfo.createTvInputInfo(
						this,
						rInfo,
						hardwareInfo,
						"ATV",
						null);
				mInfoList.put(hardwareInfo.getDeviceId(), info);
			} catch (Exception e) {
			}
		}

		return info;
	}

	public String onHardwareRemoved(TvInputHardwareInfo hardwareInfo) {
		if (hardwareInfo.getType() != TvInputHardwareInfo.TV_INPUT_TYPE_TUNER)
			return null;

		TvInputInfo info = mInfoList.get(hardwareInfo.getDeviceId());
		String id = null;
		if (info != null)
			id = info.getId();

		mInfoList.removeAt(hardwareInfo.getDeviceId());

		Log.d(TAG, "=====onHardwareRemoved===== "+id+" hwinfo:"+hardwareInfo.toString());
		return id;
	}
}

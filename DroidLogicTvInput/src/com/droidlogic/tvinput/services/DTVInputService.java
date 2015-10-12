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
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputHardwareInfo;
import android.net.Uri;
import android.os.Bundle;
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
import com.droidlogic.app.tv.DroidLogicTvInputService;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.tvinput.R;
import com.droidlogic.tvclient.TvClient;
import com.droidlogic.utils.tunerinput.data.ChannelInfo;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.media.MediaPlayer;
import android.amlogic.Tv;

public class DTVInputService extends DroidLogicTvInputService {

	private static final String TAG = "DTVInputService";

	public static final int DTV_HW_DEVICE_ID = 10;

	private HandlerThread mHandlerThread;
	private Handler mDbHandler;
	private static TvClient client = TvClient.getTvClient();

	private DTVSessionImpl mSession;

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
		mSession = new DTVSessionImpl(this, inputId);
		mSession.setOverlayViewEnabled(true);
		registerInputSession(mSession, inputId);
		client.curSource = Tv.SourceInput_Type.SOURCE_TYPE_DTV;
		return mSession;
	}

	public class DTVSessionImpl extends TvInputService.Session implements Handler.Callback {
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

		private Tv mTv = null;

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

		protected DTVSessionImpl(Context context, String inputId) {
			super(context);

			mContext = context;
			mInputId = inputId;
			mTvInputManager = (TvInputManager) context.getSystemService(Context.TV_INPUT_SERVICE);
			mHardware = mTvInputManager.acquireTvInputHardware(DTV_HW_DEVICE_ID,
					mHardwareCallback, mTvInputManager.getTvInputInfo(inputId));

			mLastBlockedRating = null;
			mHandler = new Handler(this);
			mTv = TvClient.getTvInstance();
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

			mHardware.setSurface(null, null);
			mTvInputManager.releaseTvInputHardware(DTV_HW_DEVICE_ID, mHardware);
		}

		@Override
		public void onAppPrivateCommand(String action, Bundle data) {
		    if (TextUtils.equals(DroidLogicTvUtils.ACTION_STOP_TV, action)) {
		        stopTv();
		    }
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

			if (info.type == TVChannelParams.MODE_DTMB)
				mTv.PlayDTVProgram(info.type, info.frequency, info.bandwidth, 0,
						info.videoPID, info.videoFormat,
						info.audioPIDs[0], info.audioFormats[0],
						info.pcrPID);
			else
				Log.d(TAG, "channel type["+info.type+"] not supported yet.");

			checkContentBlockNeeded();

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
				ChannelInfo ch = TvContractUtils.getChannelInfoDTV(
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
		if (hardwareInfo.getDeviceId() != DTV_HW_DEVICE_ID)
			return null;

		Log.d(TAG, "=====onHardwareAdded====="+hardwareInfo.toString());

		TvInputInfo info = null;
		ResolveInfo rInfo = getResolveInfo(DTVInputService.class.getName());
		if (rInfo != null) {
			try {
				info = TvInputInfo.createTvInputInfo(
						this,
						rInfo,
						hardwareInfo,
						"DTV",
						null);
			} catch (Exception e) {
			}
		}
		updateInfoListIfNeededLocked(hardwareInfo, info, false);

		return info;
	}

	public String onHardwareRemoved(TvInputHardwareInfo hardwareInfo) {
		if (hardwareInfo.getType() != TvInputHardwareInfo.TV_INPUT_TYPE_TUNER)
			return null;

		TvInputInfo info = getTvInputInfo(hardwareInfo);
		String id = null;
		if (info != null)
			id = info.getId();

		updateInfoListIfNeededLocked(hardwareInfo, info, true);

		Log.d(TAG, "=====onHardwareRemoved===== "+id);
		return id;
	}
}

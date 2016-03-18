package com.droidlogic.tvinput.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ContentUris;
import android.content.pm.ResolveInfo;
import android.media.tv.TvContentRating;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvTrackInfo;
import android.media.tv.TvContract;
import android.media.tv.TvContract.Channels;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.database.ContentObserver;
import android.database.IContentObserver;

import com.droidlogic.tvinput.Utils;

import com.droidlogic.app.tv.DroidLogicTvInputService;
import com.droidlogic.app.tv.TvDataBaseManager;
import com.droidlogic.app.tv.TVChannelParams;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.ChannelInfo;
import com.droidlogic.app.tv.TvInputBaseSession;
import com.droidlogic.app.tv.Program;
import com.droidlogic.app.tv.TVMultilingualText;
import com.droidlogic.app.tv.TVTime;

import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;

import com.droidlogic.app.tv.TvControlManager;

public class DTVInputService extends DroidLogicTvInputService {

    private static final String TAG = "DTVInputService";

    private static final String PID = "pid";
    private static final String FMT = "fmt";

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

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TvInputManager.ACTION_BLOCKED_RATINGS_CHANGED);
        intentFilter
                .addAction(TvInputManager.ACTION_PARENTAL_CONTROLS_ENABLED_CHANGED);
        registerReceiver(mParentalControlsBroadcastReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mParentalControlsBroadcastReceiver);
    }

    @Override
    public Session onCreateSession(String inputId) {
        super.onCreateSession(inputId);
        if (mSession == null || !TextUtils.equals(inputId, mSession.getInputId())) {
            mSession = new DTVSessionImpl(this, inputId, getHardwareDeviceId(inputId));
            registerInputSession(mSession);
        }
        return mSession;
    }

    public class DTVSessionImpl extends TvInputBaseSession implements TvControlManager.AVPlaybackListener{
        private final Context mContext;
        private TvInputManager mTvInputManager;
        private TvContentRating mLastBlockedRating;
        private TvContentRating mCurrentContentRating;
        private final Set<TvContentRating> mUnblockedRatingSet = new HashSet<>();
        private ChannelInfo mCurrentChannel;

        protected DTVSessionImpl(Context context, String inputId, int deviceId) {
            super(context, inputId, deviceId);

            mContext = context;
            mLastBlockedRating = null;
            mCurrentChannel = null;
        }

        @Override
        public int stopTvPlay() {
            int ret = super.stopTvPlay();
            if (ret == ACTION_SUCCESS) {
                stopSubtitle();
            }
            return ret;
        }

        @Override
        public void doRelease() {
            super.doRelease();
            mSession = null;
            stopSubtitle();
            setEPG(null);
//            releasePlayer();
        }

        @Override
        public int doSurfaceChanged(Uri uri) {
            int ret = super.doTune(uri);
            if (ret == ACTION_SUCCESS) {
                switchToSourceInput(uri);
            }
            return ret;
        }

        @Override
        public int doTune(Uri uri) {
            int ret = super.doTune(uri);
            if (ret == ACTION_SUCCESS) {
                switchToSourceInput(uri);
            }
            return ret;
        }

        @Override
        public void doAppPrivateCmd(String action, Bundle bundle) {
            super.doAppPrivateCmd(action, bundle);
            if (TextUtils.equals(DroidLogicTvUtils.ACTION_STOP_TV, action)) {
                stopTv();
            } else if (TextUtils.equals(DroidLogicTvUtils.ACTION_STOP_PLAY, action)) {
                Log.d(TAG, "do private cmd: STOP_PLAY");
                stopSubtitle();
                setEPG(null);
                releasePlayer();
            }
        }

        @Override
        public void doUnblockContent(TvContentRating rating) {
            super.doUnblockContent(rating);
            if (rating != null) {
                unblockContent(rating);
            }
        }

        private void switchToSourceInput(Uri uri) {
            mUnblockedRatingSet.clear();
            Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@ huhu="+ uri);
            if (Utils.getChannelId(uri) < 0) {
                TvControlManager tcm = TvControlManager.getInstance();
                tcm.PlayDTVProgram(TVChannelParams.MODE_DTMB, 0, 0, 0, 0, 0, -1, -1, 0, 0);
                mCurrentChannel = null;
                return;
            }
            TvDataBaseManager mTvDataBaseManager = new TvDataBaseManager(mContext);
            ChannelInfo ch = mTvDataBaseManager.getChannelInfo(uri);
            if (ch != null) {
                playProgram(ch);
                TvControlManager.getInstance().SetAVPlaybackListener(this);
                notifyTracks(ch);
                mCurrentChannel = ch;
            } else {
                Log.w(TAG, "Failed to get channel info for " + uri);
                TvControlManager.getInstance().SetAVPlaybackListener(null);
            }
        }

        private boolean playProgram(ChannelInfo info) {
            info.print();
            int mode = Utils.type2mode(info.getType());
            if (mode == TVChannelParams.MODE_DTMB) {
                TvControlManager tcm = TvControlManager.getInstance();
                tcm.PlayDTVProgram(
                        mode,
                        info.getFrequency(),
                        info.getBandwidth(),
                        0,
                        info.getVideoPid(),
                        info.getVfmt(),
                        (info.getAudioPids() != null) ? info.getAudioPids()[info.getAudioTrackIndex()] : -1,
                        (info.getAudioFormats() != null) ? info.getAudioFormats()[info.getAudioTrackIndex()] : -1,
                        info.getPcrPid(),
                        info.getAudioCompensation());
                tcm.DtvSetAudioChannleMod(info.getAudioChannel());
            } else
                Log.d(TAG, "channel type[" + info.getType() + "] not supported yet.");

            startSubtitle(info);

            setEPG(info);

            checkContentBlockNeeded();
            return true;
        }

        private void checkContentBlockNeeded() {
            if (mCurrentContentRating == null
                    || !mTvInputManager.isParentalControlsEnabled()
                    || !mTvInputManager.isRatingBlocked(mCurrentContentRating)
                    || mUnblockedRatingSet.contains(mCurrentContentRating)) {
                // Content rating is changed so we don't need to block anymore.
                // Unblock content here explicitly to resume playback.
                unblockContent(null);
                return;
            }

            mLastBlockedRating = mCurrentContentRating;
            // Children restricted content might be blocked by TV app as well,
            // but TIS should do its best not to show any single frame of
            // blocked content.
            stopSubtitle();
            releasePlayer();

            Log.d(TAG, "notifyContentBlocked [rating:" + mCurrentContentRating + "]");
            notifyContentBlocked(mCurrentContentRating);
        }

        private void unblockContent(TvContentRating rating) {
            // TIS should unblock content only if unblock request is legitimate.
            if (rating == null
                    || mLastBlockedRating == null
                    || (mLastBlockedRating != null && rating.equals(mLastBlockedRating))) {
                mLastBlockedRating = null;
                if (rating != null) {
                    mUnblockedRatingSet.add(rating);
                }
                Log.d(TAG, "notifyContentAllowed");
                notifyContentAllowed();
            }
        }

        @Override
        public void onEvent(int msgType, int programID) {
            Log.d(TAG, "AV evt:" + msgType);
            if (msgType == TvControlManager.EVENT_AV_SCRAMBLED)
                mSession.notifySessionEvent(DroidLogicTvUtils.AV_SIG_SCRAMBLED, null);
            else if (msgType == TvControlManager.EVENT_AV_PLAYBACK_NODATA)
                ;
            else if (msgType == TvControlManager.EVENT_AV_PLAYBACK_RESUME)
                notifyVideoAvailable();
        }

        @Override
        public boolean onSelectTrack(int type, String trackId) {
            Log.d(TAG, "onSelectTrack: [type:"+type+"] [id:"+trackId+"]");

            if (mCurrentChannel == null)
                return false;

            if (type == TvTrackInfo.TYPE_AUDIO) {

                TvControlManager tcm = TvControlManager.getInstance();
                Map<String, String> parsedMap = ChannelInfo.stringToMap(trackId);
                tcm.DtvSwitchAudioTrack(Integer.parseInt(parsedMap.get("pid")),
                                        Integer.parseInt(parsedMap.get("fmt")),
                                        0);

                notifyTrackSelected(type, trackId);

                return true;

            } else if (type == TvTrackInfo.TYPE_SUBTITLE) {
                if (trackId == null) {
                    stopSubtitle();
                    notifyTrackSelected(type, trackId);
                    return true;
                }

                TvControlManager tcm = TvControlManager.getInstance();
                Map<String, String> parsedMap = ChannelInfo.stringToMap(trackId);
                startSubtitle(Integer.parseInt(parsedMap.get("type")),
                                Integer.parseInt(parsedMap.get("pid")),
                                Integer.parseInt(parsedMap.get("stype")),
                                Integer.parseInt(parsedMap.get("uid1")),
                                Integer.parseInt(parsedMap.get("uid2")));

                notifyTrackSelected(type, trackId);

                return true;
            }
            return false;
        }

        @Override
        public View onCreateOverlayView() {
            return initSubtitleView();
        }

        @Override
        public void onOverlayViewSizeChanged(int width, int height) {
            Log.d(TAG, "onOverlayViewSizeChanged["+width+","+height+"]");
        }

        private void notifyTracks(ChannelInfo ch) {
            List < TvTrackInfo > tracks = null;
            String AudioSelectedId = null;
            String SubSelectedId = null;

            int[] audioPids = ch.getAudioPids();
            int AudioTracksCount = (audioPids == null) ? 0 : audioPids.length;
            if (AudioTracksCount != 0) {
                Log.d(TAG, "notify audio tracks:");
                String[] audioLanguages = ch.getAudioLangs();
                int[] audioFormats = ch.getAudioFormats();

                if (tracks == null)
                    tracks = new ArrayList<>();

                for (int i=0; i<AudioTracksCount; i++) {
                    boolean isDefault = false;
                    if (ch.getAudioTrackIndex() == i)
                        isDefault = true;

                    Map<String, String> map = new HashMap<String, String>();
                    map.put("id", String.valueOf(i));
                    map.put("pid", String.valueOf(audioPids[i]));
                    map.put("fmt", String.valueOf(audioFormats[i]));
                    //if (isDefault)
                    //    map.put("default", String.valueOf(1));
                    String Id = ChannelInfo.mapToString(map);
                    TvTrackInfo AudioTrack =
                        new TvTrackInfo.Builder(TvTrackInfo.TYPE_AUDIO, Id)
                                       .setLanguage(audioLanguages[i])
                                       .setAudioChannelCount(2)
                                       .build();
                    tracks.add(AudioTrack);
                    if (isDefault)
                        AudioSelectedId = Id;
                    Log.d(TAG, "\t"+i+": ["+audioLanguages[i]+"] [pid:"+audioPids[i]+"] [fmt:"+audioFormats[i]+"]");
                }
            }

            int[] subPids = ch.getSubtitlePids();
            int SubTracksCount = (subPids == null) ? 0 : subPids.length;
            if (SubTracksCount != 0) {
                Log.d(TAG, "notify subtitle tracks:");
                String[] subLanguages = ch.getSubtitleLangs();
                int[] subTypes = ch.getSubtitleTypes();
                int[] subStypes = ch.getSubtitleStypes();
                int[] subId1s = ch.getSubtitleId1s();
                int[] subId2s = ch.getSubtitleId2s();

                if (tracks == null)
                    tracks = new ArrayList<>();

                for (int i=0; i<SubTracksCount; i++) {
                    boolean isDefault = false;
                    if (ch.getSubtitleTrackIndex() == i)
                        isDefault = true;

                    Map<String, String> map = new HashMap<String, String>();
                    map.put("id", String.valueOf(i));
                    map.put("pid", String.valueOf(subPids[i]));
                    map.put("type", String.valueOf(subTypes[i]));
                    map.put("stype", String.valueOf(subStypes[i]));
                    map.put("uid1", String.valueOf(subId1s[i]));
                    map.put("uid2", String.valueOf(subId2s[i]));
                    //if (isDefault)
                    //    map.put("default", String.valueOf(1));
                    String Id = ChannelInfo.mapToString(map);
                    TvTrackInfo SubtitleTrack =
                        new TvTrackInfo.Builder(TvTrackInfo.TYPE_SUBTITLE, Id)
                                       .setLanguage(subLanguages[i])
                                       .build();
                    tracks.add(SubtitleTrack);
                    if (isDefault && subtitleAutoStart)
                        SubSelectedId = Id;
                    Log.d(TAG, "\t"+i+": ["+subLanguages[i]+"] [pid:"+subPids[i]+"] [type:"+subTypes[i]+"]");
                    Log.d(TAG, "\t"+"   [id1:"+subId1s[i]+"] [id2:"+subId2s[i]+"] [stype:"+subStypes[i]+"]");
                }

            }

            if (tracks != null)
                notifyTracksChanged(tracks);
            Log.d(TAG, "\tselected: ["+AudioSelectedId+"]");
            notifyTrackSelected(TvTrackInfo.TYPE_AUDIO, AudioSelectedId);

            Log.d(TAG, "\tselected: ["+SubSelectedId+"]");
            notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, SubSelectedId);
        }


        private DTVSubtitleView mSubtitleView = null;
        private static final int TYPE_DVB_SUBTITLE = 1;
        private static final int TYPE_DTV_TELETEXT = 2;
        private static final int TYPE_ATV_TELETEXT = 3;
        private static final int TYPE_DTV_CC = 4;
        private static final int TYPE_ATV_CC = 5;

        private static final boolean subtitleAutoStart = false;

        private void startSubtitle(ChannelInfo channelInfo) {

            if (!subtitleAutoStart)
                return ;

            if (channelInfo.getSubtitlePids() != null) {
                int idx = channelInfo.getSubtitleTrackIndex();
                startSubtitle(channelInfo.getSubtitleTypes()[idx],
                                channelInfo.getSubtitlePids()[idx],
                                channelInfo.getSubtitleStypes()[idx],
                                channelInfo.getSubtitleId1s()[idx],
                                channelInfo.getSubtitleId2s()[idx]);
            } else {
                stopSubtitle();
            }
        }

        private int getTeletextRegionID(String ttxRegionName){
            final String[] supportedRegions= {"English", "Deutsch", "Svenska/Suomi/Magyar",
                                              "Italiano", "Français", "Português/Español",
                                              "Cesky/Slovencina", "Türkçe", "Ellinika","Alarabia / English"};
            final int[] regionIDMaps = {16, 17, 18, 19, 20, 21, 14, 22, 55 , 64};

            int i;
            for (i=0; i<supportedRegions.length; i++) {
                if (supportedRegions[i].equals(ttxRegionName))
                    break;
            }

            if (i >= supportedRegions.length) {
                Log.d(TAG, "Teletext defaut region " + ttxRegionName +
                    " not found, using 'English' as default!");
                i = 0;
            }

            Log.d(TAG, "Teletext default region id: " + regionIDMaps[i]);
            return regionIDMaps[i];
        }

        private View initSubtitleView() {
            if (mSubtitleView == null) {
                mSubtitleView = new DTVSubtitleView(mContext);
            }
            return mSubtitleView;
        }

        private void startSubtitle(int type, int pid, int stype, int id1, int id2) {
            Log.d(TAG, "start Subtitle");

            initSubtitleView();

            mSubtitleView.stop();

            if (type == TYPE_DVB_SUBTITLE) {

                DTVSubtitleView.DVBSubParams params =
                    new DTVSubtitleView.DVBSubParams(0, pid, id1, id2);
                mSubtitleView.setSubParams(params);

            } else if (type == TYPE_DTV_TELETEXT) {

                int pgno;
                pgno = (id1==0) ? 800 : id1*100;
                pgno += (id2 & 15) + ((id2 >> 4) & 15) * 10 + ((id2 >> 8) & 15) * 100;
                DTVSubtitleView.DTVTTParams params =
                    new DTVSubtitleView.DTVTTParams(0, pid, pgno, 0x3F7F, getTeletextRegionID("English"));
                mSubtitleView.setSubParams(params);

            }
            mSubtitleView.setActive(true);
            mSubtitleView.startSub(); //DVBSub/EBUSub/CC

            setOverlayViewEnabled(true);

        }

        private void stopSubtitle() {
            Log.d(TAG, "stop Subtitle");

            if (mSubtitleView != null) {
                setOverlayViewEnabled(false);
                mSubtitleView.stop();
            }
        }


        private HandlerThread mHandlerThread = null;
        private Handler mHandler;
        private void initThread(String name) {
            if (mHandlerThread == null) {
                mHandlerThread = new HandlerThread(name);
                mHandlerThread.start();
                mHandler = new Handler(mHandlerThread.getLooper(), this);
            }
        }

        private void releaseThread() {
            if (mHandlerThread != null) {
                mHandlerThread.quit();
                mHandlerThread = null;
                mHandler = null;
            }
        }

        private EPGCurrentProgramRunnable mEPGCurrentProgramRunnable;
        private EPGScanner epg = null;

        private static final int EPG_FEND = 0;
        private static final int EPG_DMX = 0;
        private static final String EPG_LANGUAGE = "local eng zho chi chs first";
        private static final String DEF_CODING = "standard";//"GB2312";//force setting for auto-detect fail.

        private class EPGCurrentProgramRunnable implements Runnable {
            private final ChannelInfo mChannel;

            public EPGCurrentProgramRunnable(ChannelInfo channel) {
                mChannel = channel;
            }

            @Override
            public void run() {
                Log.d(TAG, "epg ch: " + mChannel.getDisplayNumber()+"-"+mChannel.getDisplayName());

                if (epg == null) {
                    epg = new EPGScanner(mContext, getInputId(), DEF_CODING);
                    epg.reset(EPG_FEND, EPG_DMX,
                        Utils.type2mode(mChannel.getType()),
                        EPG_LANGUAGE.replaceAll("local", TVMultilingualText.getLocalLang()));
                }

                epg.enterChannel(getTVChannelParams(mChannel), false);
                epg.enterService(mChannel);
            }
        }

        private TVChannelParams getTVChannelParams(ChannelInfo channel) {
            TVChannelParams params = null;
            int mode = Utils.type2mode(channel.getType());

            if (mode == TVChannelParams.MODE_DTMB)
                params = TVChannelParams.dtmbParams(channel.getFrequency(), channel.getBandwidth());

            return params;
        }

        private void setEPG(ChannelInfo channel) {
            if (channel != null) {
                Log.d(TAG, "startEPG");

                initThread("startEpg Thread");
                mHandler.removeCallbacks(mEPGCurrentProgramRunnable);
                mEPGCurrentProgramRunnable = new EPGCurrentProgramRunnable(channel);
                mHandler.post(mEPGCurrentProgramRunnable);
            } else {
                if (epg != null) {
                    epg.destroy();
                    epg = null;
                }
                releaseThread();

                Log.d(TAG, "stopEPG");
            }
        }

        public class EPGScanner {
            private static final String TAG = "EPGScanner";
            private static final int MSG_EPG_EVENT = 1000;

            private HandlerThread mHandlerThread;
            private Handler mHandler;
            private Context mContext;
            private DTVEpgScanner epgScanner;
            private TVChannelParams tvchan = null;
            private ChannelInfo tvservice = null;
            private TvDataBaseManager mTvDataBaseManager = null;
            private TvControlManager mTvControlManager = null;
            private TVTime mTvTime = null;

            private ChannelObserver mChannelObserver;
            private ArrayList<ChannelInfo> channelMap;
            private long maxChannel_ID = 0;
            private String mInputId;

            private int fend = 0;
            private int dmx = 0;
            private int src = 0;
            private String[] languages = null;

            EPGScanner(Context context, String inputId, String coding){
                mContext = context;
                mInputId = inputId;
                mTvDataBaseManager = new TvDataBaseManager(mContext);
                mTvTime = new TVTime(mContext);

                mHandlerThread = new HandlerThread(getClass().getSimpleName());
                mHandlerThread.start();
                mHandler = new Handler(mHandlerThread.getLooper()){
                    @Override
                    public void handleMessage(Message msg) {
                        if (msg.what == MSG_EPG_EVENT) {
                            resolveEPGEvent((DTVEpgScanner.Event)msg.obj);
                        }
                    }
                };

                epgScanner = new DTVEpgScanner(){
                    public void onEvent(DTVEpgScanner.Event event){
                        Log.d(TAG, "send event:"+event.type);
                        mHandler.obtainMessage(MSG_EPG_EVENT, event).sendToTarget();
                    }
                };
                epgScanner.setDvbTextCoding(coding);

                if (mChannelObserver == null)
                    mChannelObserver = new ChannelObserver();
                mContext.getContentResolver().registerContentObserver(TvContract.Channels.CONTENT_URI, true, mChannelObserver);
            }

            private void refreshChannelMap() {
                channelMap = mTvDataBaseManager.getChannelList(mInputId, ChannelInfo.SIMPLE_PROJECTION, null, null);
                if (channelMap != null) {
                    for (ChannelInfo c : channelMap)
                        if (c.getId() > maxChannel_ID)
                            maxChannel_ID = c.getId();
                }
                Log.d(TAG, "channelMap changed. max_ID:"+maxChannel_ID);
            }

            public void reset(int fend, int dmx, int src, String textLanguages){
                refreshChannelMap();

                epgScanner.setSource(fend, dmx, src, textLanguages);
                languages = textLanguages.split(" ");

                this.fend = fend;
                this.dmx = dmx;
                this.src = src;
            }

            public void reset(){
                Log.d(TAG, "epg reset.");

                reset(fend, dmx, src, Arrays.toString(languages).replace("[", "").replace("]", "").replace(","," "));

                enterChannel(tvchan, true);
                enterService(tvservice);
            }

            public void destroy(){
                if (mChannelObserver != null) {
                    mContext.getContentResolver().unregisterContentObserver(mChannelObserver);
                    mChannelObserver = null;
                }
                if (epgScanner != null) {
                    epgScanner.destroy();
                    epgScanner = null;
                }
                if (mHandlerThread != null) {
                    mHandlerThread.quit();
                    mHandlerThread = null;
                }
                mTvDataBaseManager = null;
                mTvTime = null;
            }

            public void enterChannel(TVChannelParams chan, boolean force){
                if (chan == null)
                    epgScanner.leaveChannel();
                else if ((tvchan == null) || !tvchan.equals(chan) || force)
                    epgScanner.enterChannel();
                tvchan = chan;
            }

            public void enterService(ChannelInfo channel){
                if (channel == null)
                    epgScanner.leaveProgram();
                else
                    epgScanner.enterProgram(channel);
                tvservice = channel;
            }

            private List<Program> getChannelPrograms(Uri channelUri, ChannelInfo channel,
                                                            DTVEpgScanner.Event event){
                List<Program> programs = new ArrayList<>();
                for (DTVEpgScanner.Event.Evt evt : event.evts) {
                    if ((channel.getTransportStreamId()== evt.ts_id)
                        && (channel.getServiceId() == evt.srv_id)
                        && (channel.getOriginalNetworkId() == evt.net_id)) {
                        try{
                            long start = evt.start;
                            long end = evt.end;
                            Program p = new Program.Builder()
                                .setChannelId(ContentUris.parseId(channelUri))
                                .setTitle(TVMultilingualText.getText(new String(evt.name), languages))
                                .setDescription(TVMultilingualText.getText(new String(evt.desc), languages))
                                //.setContentRatings(programInfo.contentRatings)
                                //.setCanonicalGenres(programInfo.genres)
                                //.setPosterArtUri(programInfo.posterArtUri)
                                //.setInternalProviderData(TvContractUtils.convertVideoInfoToInternalProviderData(
                                //        programInfo.videoType, programInfo.videoUrl))
                                .setStartTimeUtcMillis(start * 1000)
                                .setEndTimeUtcMillis(end * 1000)
                                .build();
                            programs.add(p);
                            Log.d(TAG, "epg: sid/net/ts["+evt.srv_id+"/"+evt.net_id+"/"+evt.ts_id+"]"
                                +"{"+p.getTitle()+"}"
                                +"["+p.getStartTimeUtcMillis()/1000
                                +"-"+p.getEndTimeUtcMillis()/1000
                                +"]");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                return programs;
            }

            private void resolveEPGEvent(DTVEpgScanner.Event event){
                Log.d(TAG, "Channel epg event: " + event.type);
                switch (event.type) {
                    case DTVEpgScanner.Event.EVENT_PROGRAM_EVENTS_UPDATE:
                        for (int i = 0; (channelMap != null && i < channelMap.size()); ++i) {
                            ChannelInfo channel = (ChannelInfo)channelMap.get(i);
                            Uri channelUri = TvContract.buildChannelUri(channel.getId());

                            List<Program> programs = getChannelPrograms(channelUri, channel, event);
                            mTvDataBaseManager.updatePrograms(channelUri, programs);
                        }
                    break;
                    case DTVEpgScanner.Event.EVENT_TDT_END:
                        Log.d(TAG, "[TDT Update]:"+event.time);
                        if (mTvTime != null)
                            mTvTime.setTime(event.time*1000);
                    break;
                    case DTVEpgScanner.Event.EVENT_PROGRAM_AV_UPDATE:
                        Log.d(TAG, "[AV Update]: ServiceId:"+event.channel.getServiceId()
                            +" Vid:"+event.channel.getVideoPid()
                            +" Pcr:"+event.channel.getPcrPid()
                            +" Aids:"+Arrays.toString(event.channel.getAudioPids())
                            +" Afmts:"+Arrays.toString(event.channel.getAudioFormats())
                            +" Alangs:"+Arrays.toString(event.channel.getAudioLangs())
                        );
                        if (tvservice.getServiceId() == event.channel.getServiceId()) {
                            tvservice.setVideoPid(event.channel.getVideoPid());
                            tvservice.setVfmt(event.channel.getVfmt());
                            tvservice.setPcrPid(event.channel.getPcrPid());
                            tvservice.setAudioPids(event.channel.getAudioPids());
                            tvservice.setAudioFormats(event.channel.getAudioFormats());
                            tvservice.setAudioLangs(event.channel.getAudioLangs());
                            mTvDataBaseManager.updateChannelInfo(tvservice);
                        }
                    break;
                    case DTVEpgScanner.Event.EVENT_PROGRAM_NAME_UPDATE:
                        Log.d(TAG, "[NAME Update]: ServiceId:"+event.channel.getServiceId()
                            +" Network:"+event.channel.getOriginalNetworkId()
                            +" Name:"+event.channel.getDisplayName()
                            +" SdtVersion:"+event.channel.getSdtVersion()
                        );
                        if (tvservice.getServiceId() == event.channel.getServiceId()) {
                            tvservice.setDisplayName(event.channel.getDisplayName());
                            tvservice.setOriginalNetworkId(event.channel.getOriginalNetworkId());
                            tvservice.setSdtVersion(event.channel.getSdtVersion());
                            mTvDataBaseManager.updateChannelInfo(tvservice);
                        }
                    break;
                    case DTVEpgScanner.Event.EVENT_CHANNEL_UPDATE:
                        Log.d(TAG, "[TS Update]: Freq:"+tvservice.getFrequency());
                        Log.d(TAG, "TS changed, need autoscan. not impelement");
                    break;
                    default:
                    break;
                }
            }




            private boolean epg_auto_reset = true;
            private void setEpgAutoReset(boolean enable) {
                epg_auto_reset = enable;
            }

            private final class ChannelObserver extends ContentObserver {
                public ChannelObserver() {
                    super(new Handler());
                }

                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    Log.d(TAG, "channel changed: selfchange:"+selfChange+" uri:"+uri);
                    if ((DroidLogicTvUtils.matchsWhich(uri) == DroidLogicTvUtils.MATCH_CHANNEL)//delete
                        || ((DroidLogicTvUtils.matchsWhich(uri) == DroidLogicTvUtils.MATCH_CHANNEL_ID)
                            && (DroidLogicTvUtils.getChannelId(uri) > maxChannel_ID))){//add
                        if (DroidLogicTvUtils.matchsWhich(uri) == DroidLogicTvUtils.MATCH_CHANNEL) {
                            Log.d(TAG, "channel deleted");
                        } else if ((DroidLogicTvUtils.matchsWhich(uri) == DroidLogicTvUtils.MATCH_CHANNEL_ID)
                            && (DroidLogicTvUtils.getChannelId(uri) > maxChannel_ID)) {
                            Log.d(TAG, "channel added: "+DroidLogicTvUtils.getChannelId(uri)+">"+maxChannel_ID);
                        }
                        if (epg_auto_reset)
                            reset();
                    }
                }

                @Override
                public IContentObserver releaseContentObserver() {
                    // TODO Auto-generated method stub
                    return super.releaseContentObserver();
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

        public TvInput(String displayName, String name, String description,
                String logoThumbUrl, String logoBackgroundUrl) {
            this.displayName = displayName;
            this.name = name;
            this.description = description;
            this.logoThumbUrl = logoThumbUrl;
            this.logoBackgroundUrl = logoBackgroundUrl;
        }
    }

    public TvInputInfo onHardwareAdded(TvInputHardwareInfo hardwareInfo) {
        if (hardwareInfo.getDeviceId() != DroidLogicTvUtils.DEVICE_ID_DTV)
            return null;

        Log.d(TAG, "=====onHardwareAdded=====" + hardwareInfo.getDeviceId());

        TvInputInfo info = null;
        ResolveInfo rInfo = getResolveInfo(DTVInputService.class.getName());
        if (rInfo != null) {
            try {
                info = TvInputInfo.createTvInputInfo(this, rInfo, hardwareInfo,
                        getTvInputInfoLabel(hardwareInfo.getDeviceId()), null);
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

        Log.d(TAG, "=====onHardwareRemoved===== " + id);
        return id;
    }


}

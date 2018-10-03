package com.droidlogic.tvinput.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import com.droidlogic.tvinput.settings.OptionUiManagerT;

/**
 * Created by yu.fang on 2018/6/13.
 */

public class TvScanService extends Service {
    private static final String TAG = "TvScanService";

    private OptionUiManagerT mOptionUiManagerT;
    private RemoteCallbackList<IUpdateUiCallbackListener> mListenerList = new RemoteCallbackList<>();

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            TvMessage message = new TvMessage(msg.arg1, msg.what, (String)(msg.obj));
            try{
                int count = mListenerList.beginBroadcast();
                for (int i = 0; i < count; i++) {
                    mListenerList.getBroadcastItem(i).onRespond(message);
                }
                mListenerList.finishBroadcast();
            } catch (RemoteException e){
                e.printStackTrace();
            }
        }
    };

    private final ITvScanService.Stub mBinder = new ITvScanService.Stub(){

        public void init(Intent intent){
            initT(intent);
        }

        public void setAtsccSearchSys(int value){
            setAtsccSearchSysT(value);
        }

        public void startAutoScan(){
            callAutosearch();
        }

        public void startManualScan(){
            callManualSearch();
        }

        public void setSearchSys (boolean value1, boolean value2){
            setSearchSysT(value1, value2);
        }

        public void setFrequency (String value1, String value2) {
            setFrequencyT(value1, value2);
        }

        public void release() {
            releaseT();
        }

        public void registerListener(IUpdateUiCallbackListener listener) throws RemoteException {
            mListenerList.register(listener);
        }

        public void unregisterListener(IUpdateUiCallbackListener listener) throws RemoteException {
            mListenerList.unregister(listener);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "=====onBind");
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "=====init");
    }

    public void initT(Intent intent){
        Log.d(TAG, "=====init OptionUiManagerT");
        mOptionUiManagerT = new OptionUiManagerT(this, intent, true);
        mOptionUiManagerT.setHandler(mHandler);
    }

    public void setAtsccSearchSysT(int value) {
        Log.d(TAG, "=====init setAtsccSearchSys");
        mOptionUiManagerT.setAtsccSearchSys(value);
    }

    public void callAutosearch() {
        Log.d(TAG, "=====init callAutosearch");
        mOptionUiManagerT.callAutosearch();
    }

    public void callManualSearch() {
        Log.d(TAG, "=====init callManualSearch");
        mOptionUiManagerT.callManualSearch();
    }

    public void setSearchSysT (boolean value1, boolean value2){
        Log.d(TAG, "=====init setSearchSys");
        mOptionUiManagerT.setSearchSys(value1, value2);
    }

    public void setFrequencyT (String value1, String value2) {
        Log.d(TAG, "=====init setFrequency");
        mOptionUiManagerT.setFrequency(value1, value2);
    }

    public void releaseT() {
        Log.d(TAG, "=====init release");
        mOptionUiManagerT.release();
        mOptionUiManagerT = null;
    }

    public void registerListener(IUpdateUiCallbackListener listener) throws RemoteException {
        mListenerList.register(listener);
    }

    public void unregisterListener(IUpdateUiCallbackListener listener) throws RemoteException {
        mListenerList.unregister(listener);
    }
}

package com.droidlogic.tvsource.ui;

import java.util.List;

import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.TvControlManager;
import com.droidlogic.tvsource.R;
import com.droidlogic.tvsource.Utils;
import com.droidlogic.tvsource.ui.SourceButton.OnSourceClickListener;
import com.droidlogic.tvsource.ChannelDataManager;

import android.content.Context;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.widget.LinearLayout;

public class SourceInputListLayout extends LinearLayout implements OnSourceClickListener {
    private static final String TAG = "SourceInputListLayout";
    private Context mContext;
    private LinearLayout mRoot;
    private TvInputManager mTvInputManager;
    private SparseArray<SourceButton> mSourceInputs = new SparseArray<>();
    private int mAvaiableSourceCount = 0;

    private SourceButton defSourceInput;
    private SourceButton curSourceInput;
    private SourceButton dtvSourceInput;

    private onSourceInputClickListener mClickListener;

    private int defaultDeviceId;
    private int defaultAtvChannel;
    private int defaultDtvChannel;
    private boolean defaultDtvType;

    public static int INPUT_NEED_RESET    = 0;
    public static int ACTION_FAILED       = -1;
    public static int ACTION_SUCCESS      = 1;

    public SourceInputListLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    private void init() {
        inflate(mContext, R.layout.source_input_list, this);
        mRoot = (LinearLayout) findViewById(R.id.source_root);
        mTvInputManager = (TvInputManager) mContext.getSystemService(Context.TV_INPUT_SERVICE);
    }

    public int stateChange(String inputId, int state) {
        return ACTION_FAILED;
    }

    public int update(String inputId) {
        return ACTION_FAILED;
    }

    /**
     * invoked when device hot plug-out.
     */
    public int remove(String inputId) {
        Utils.logd(TAG, "==== remove, current id =" + curSourceInput.getInputId());
        if (getSourceCount() == 0 || TextUtils.isEmpty(inputId))
            return ACTION_FAILED;
        int count = getSourceCount();
        int i = 1;
        for (; i < count + 1; i++) {
            SourceButton sb = (SourceButton) mRoot.getChildAt(i);
            if (TextUtils.equals(sb.getInputId(), inputId)) {
                if (sb.isHardware()) {
                    sb.sourceRelease();
                    sb.setState(-1);
                    mAvaiableSourceCount--;
                    if (sb.getDeviceId() == curSourceInput.getDeviceId()) {
                        return INPUT_NEED_RESET;
                    }
                } else {
                    mRoot.removeView(sb);
                    mAvaiableSourceCount--;
                    if (TextUtils.equals(inputId, curSourceInput.getInputId())) {
                        curSourceInput = defSourceInput;
                        curSourceInput.setSelected(true);
                        return INPUT_NEED_RESET;
                    }
                }
                return ACTION_SUCCESS;
            }
        }
        return ACTION_FAILED;
    }

    /**
     * invoked when device hot plug-in.
     * get all devices at first time, otherwise, add device one by one.
     */
    public int add(String inputId) {
        int input_list_size = mTvInputManager.getTvInputList().size();
        int count = getSourceCount();
        Utils.logd(TAG, "==== add, input list size=" + input_list_size + ", count=" + count);
        if (TextUtils.isEmpty(inputId))
            return ACTION_FAILED;
        if (count == 0 && count < input_list_size) {
            Utils.logd(TAG, "update all source input.");
            return refresh();
        }
        TvInputInfo info = mTvInputManager.getTvInputInfo(inputId);
        if (info == null)
            return ACTION_FAILED;

        int device_id = getDeviceId(info);
        if (device_id >= 0) {//hardware input source
            SourceButton sb = mSourceInputs.get(device_id);
            if (sb.getTvInputInfo() != null)//has added
                return ACTION_SUCCESS;
            sb.setTvInputInfo(info);
            sb.setState(TvInputManager.INPUT_STATE_CONNECTED);
            initSourceInput(sb);
            mAvaiableSourceCount++;
        } else if (info.getType() != TvInputInfo.TYPE_HDMI) {
            SourceButton sb = new SourceButton(mContext, info);
            mRoot.addView(sb);
            sb.setOnSourceClickListener(this);
            mAvaiableSourceCount++;
        }
        if (curSourceInput == null && input_list_size == mAvaiableSourceCount) {//all source has been added.
            curSourceInput = defSourceInput;
            curSourceInput.setSelected(true);
            return INPUT_NEED_RESET;
        } else if (curSourceInput != null && device_id == curSourceInput.getDeviceId()) {
            return INPUT_NEED_RESET;
        }
        return ACTION_SUCCESS;
    }

    public int refresh() {
        int device_id = -1;

        if (mRoot.getChildCount() > 1) {//refresh source input list, remove all input
            mRoot.removeViews(1, mRoot.getChildCount() - 1);
            mAvaiableSourceCount = 0;
        }
        ChannelDataManager.clear();
        mSourceInputs.clear();

        for (String id : getAllDeviceIds()) { //init all hardware devices
            device_id = Integer.parseInt(id);
            SourceButton sb = new SourceButton(mContext, device_id);
            if (sb.getSigType() == DroidLogicTvUtils.SIG_INFO_TYPE_DTV) {
                dtvSourceInput = sb;
            }
            if (defaultDeviceId == sb.getDeviceId()) {
                curSourceInput = sb;
                curSourceInput.setSelected(true);
            }
            mSourceInputs.put(sb.getDeviceId(), sb);
            Utils.logd(TAG, "==== refresh, sb = " + sb);
            sb.setOnSourceClickListener(this);
        }
        for (int i = mSourceInputs.size() - 1; i >= 0; i--) { //add hardware input to root
            mRoot.addView(mSourceInputs.valueAt(i), 1);
        }
        List<TvInputInfo> input_list = mTvInputManager.getTvInputList();
        Utils.logd(TAG, "==== refresh, input_list size =" + input_list.size());
        for (TvInputInfo info : input_list) {
            device_id = getDeviceId(info);
            Utils.logd(TAG, "==== device_id =" + device_id);
            if (device_id >= 0) {//hardware device
                SourceButton sb = mSourceInputs.get(device_id);
                sb.setTvInputInfo(info);
                sb.setState(TvInputManager.INPUT_STATE_CONNECTED);
                initSourceInput(sb);
                mAvaiableSourceCount++;
            } else if (info.getType() != TvInputInfo.TYPE_HDMI) {//non-hardware device
                SourceButton sb = new SourceButton(mContext, info);
                mRoot.addView(sb);
                sb.setOnSourceClickListener(this);
                mAvaiableSourceCount++;
            }
        }

        if (defSourceInput == null) {//ATV hasn't been added, return and wait.
            return ACTION_SUCCESS;
        }
        return INPUT_NEED_RESET;
    }

    public SourceButton getSourceInput(TvInputInfo info) {
        int id = getDeviceId(info);
        if (id > 0) {
            return mSourceInputs.get(id);
        }
        return null;
    }

    public SourceButton getSourceInput(int deviceId) {
        return deviceId > 0 ? mSourceInputs.get(deviceId) : null;
    }

    private int getDeviceId(TvInputInfo info) {
        String[] temp = info.getId().split(Utils.DELIMITER_INFO_IN_ID);
        if (temp.length == 3) {
            /*  ignore for HDMI CEC device */
            if (temp[2].contains(Utils.PREFIX_HDMI_DEVICE))
                return -1;
            return Integer.parseInt(temp[2].substring(2));
        } else {
            return -1;
        }
    }

    private String[] getAllDeviceIds() {
        TvControlManager tcm = TvControlManager.getInstance();
        String prop_ids = tcm.GetSourceInputList();
        if (TextUtils.equals(prop_ids, "null")) {
            throw new IllegalArgumentException("source input ids is not set.");
        }
        String[] ids = prop_ids.split(",");
        Utils.logd(TAG, "==== ids length is " + ids.length);
        return ids;
    }

    private void initSourceInput(SourceButton sb) {
        if (sb.getSourceType() == DroidLogicTvUtils.SOURCE_TYPE_ATV && defaultAtvChannel >= 0) {
            sb.moveToChannel(defaultAtvChannel, false);
        } else if (sb.getSourceType() == DroidLogicTvUtils.SOURCE_TYPE_DTV && defaultDtvChannel >= 0) {
            sb.moveToChannel(defaultDtvChannel, defaultDtvType);
        }
    }

    public void setDefaultSourceInfo(int device_id, int atv_channel, int dtv_channel,
                                     boolean is_radio) {
        Utils.logd(TAG, "device_id=" + device_id + ", atv_channel=" + atv_channel
                   + ", dtv_channel=" + dtv_channel + ", is_radio=" + is_radio);
        defaultDeviceId = device_id;
        defaultAtvChannel = atv_channel;
        defaultDtvChannel = dtv_channel;
        defaultDtvType = is_radio;
    }

    public void setCurSourceInput(SourceButton sb) {
        if (curSourceInput != null)
            curSourceInput.setSelected(false);
        curSourceInput = sb;
        curSourceInput.setSelected(true);
    }

    public SourceButton getCurSourceInput() {
        return curSourceInput;
    }

    public int getSourceCount() {
        return mRoot.getChildCount() - 1;
    }

    @Override
    public void onButtonClick(SourceButton sb) {
        Utils.logd(TAG, "==== onButtonClick ====" + sb);
        curSourceInput.setSelected(false);
        curSourceInput = sb;
        curSourceInput.setSelected(true);
        mClickListener.onSourceInputClick();
    }

    public SourceButton getDtvSourceButton() {
        return dtvSourceInput;
    }

    public void setOnSourceInputClickListener(onSourceInputClickListener l) {
        mClickListener = l;
    }

    public interface onSourceInputClickListener {
        void onSourceInputClick();
    }
}

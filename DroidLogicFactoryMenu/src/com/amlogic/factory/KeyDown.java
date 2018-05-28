/**
 * change son item content when pressing key
 *
 * @author ice.fu
 */
package com.amlogic.factory;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.droidlogic.app.tv.TvControlManager;
import com.droidlogic.app.SystemControlManager;
import com.droidlogic.app.tv.TvInSignalInfo;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
//import android.os.SystemProperties;
import android.util.Log;


public class KeyDown {
    public static TvControlManager tv = TvControlManager.getInstance();
    /* son ListView's item save to this List */
    public List<Map<String, String>> mListSubMenuData = null;

    private Context context;
    private SystemControlManager sm;

    public KeyDown(List<Map<String, String>> mListSubMenuData, Context context) {
        this.mListSubMenuData = mListSubMenuData;
        this.context = context;
        sm = new SystemControlManager(context);
    }

    public void adjustCalibrate(boolean left, int step) {
        String value_str = "";
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        switch (FactoryMainActivity.mSubMenuPos) {
            case 0:
                value_str = map.get("sub_value");
                if (value_str.equalsIgnoreCase(context.getString(Constant.FACUI_ADC_PORT_RGB)))
                    map.put("sub_value", context.getString(Constant.FACUI_ADC_PORT_YPBPR));
                else
                    map.put("sub_value", context.getString(Constant.FACUI_ADC_PORT_RGB));
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
    }

    private void setpicparam(Map<String, String> map1,  int is_save) {
        if (FactoryMainActivity.mCurShowModePos != 3) {
            int brightness = sm.GetBrightness();
            int contrast = sm.GetContrast();
            int saturation = sm.GetSaturation();
            int sharpness = sm.GetSharpness();
            FactoryMainActivity.mCurShowModePos = 3;
            sm.SetPQMode(SystemControlManager.PQMode.values()[FactoryMainActivity.mCurShowModePos].toInt(), is_save, 0);
            map1.put("sub_value", context.getString(Constant.mShowModeList[FactoryMainActivity.mCurShowModePos]));
            FactoryMainActivity.sAdapter.notifyDataSetChanged();
            sm.SetBrightness(brightness, is_save);
            sm.SaveBrightness(brightness);
            sm.SetContrast(contrast, is_save);
            sm.SaveContrast(contrast);
            sm.SetSaturation(saturation, is_save);
            sm.SaveSaturation(saturation);
            sm.SetSharpness(sharpness, 0, 0);
            sm.SaveSharpness(sharpness, 1);
        }
    }

    public void adjustPicMode(boolean left, int step) {
        String value_str = "";
        int value_int = 0;
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        Map<String, String> map1 = mListSubMenuData.get(1);
        TvControlManager.SourceInput source = TvControlManager.SourceInput.values()[FactoryMainActivity.mCurInputPortPos];
        int is_save = 1;// if is_save is 1,auto save when setting,if is 0,call set function first,then save
        ShowSubView ssv = new ShowSubView(mListSubMenuData, context);
        switch (FactoryMainActivity.mSubMenuPos) {
            case 0: {
                    int iInputPortCount = Constant.mInputPortList.length;
                    if (0 == FactoryMainActivity.mCurInputPortPos && left)
                        FactoryMainActivity.mCurInputPortPos = iInputPortCount - 1;
                    else
                        FactoryMainActivity.mCurInputPortPos = Math.abs(FactoryMainActivity.mCurInputPortPos + step) % iInputPortCount;
                    map.put("sub_value", context.getString(Constant.mInputPortList[FactoryMainActivity.mCurInputPortPos]));
                    source = TvControlManager.SourceInput.values()[FactoryMainActivity.mCurInputPortPos];
                    for (int i = mListSubMenuData.size() - 1; i > 0; i--)
                        mListSubMenuData.remove(i);
                    ssv.setPicture(source);
                    FactoryMainActivity.sAdapter.notifyDataSetChanged();
                }
                break;
            case 1: {
                    int mCurShowModeSize = Constant.mShowModeList.length;
                    FactoryMainActivity.mCurShowModePos = Math.abs(FactoryMainActivity.mCurShowModePos + step + mCurShowModeSize) % mCurShowModeSize;
                    map.put("sub_value", context.getString(Constant.mShowModeList[FactoryMainActivity.mCurShowModePos]));
                    sm.SetPQMode(SystemControlManager.PQMode.values()[FactoryMainActivity.mCurShowModePos].toInt(), is_save, 0);
                    sm.SavePQMode(SystemControlManager.PQMode.values()[FactoryMainActivity.mCurShowModePos].toInt());
                    for (int i = mListSubMenuData.size() - 1; i > 0; i--)
                        mListSubMenuData.remove(i);
                    ssv.setPicture(source);
                    FactoryMainActivity.sAdapter.notifyDataSetChanged();
                }
                break;
            case 2: {
                    setpicparam(map1, is_save);
                    value_str = map.get("sub_value");
                    if (-1 == step && value_str.equals("0"))
                        value_int = 100;
                    else
                        value_int = Math.abs(Integer.parseInt(value_str) + step) % 101;
                    map.put("sub_value", String.valueOf(value_int));
                    FactoryMainActivity.sAdapter.notifyDataSetChanged();
                    sm.SetBrightness(value_int, is_save);
                    sm.SaveBrightness(value_int);
                }
                break;
            case 3: {
                    setpicparam(map1, is_save);
                    value_str = map.get("sub_value");
                    if (-1 == step && value_str.equals("0"))
                        value_int = 100;
                    else
                        value_int = Math.abs(Integer.parseInt(value_str) + step) % 101;
                    map.put("sub_value", String.valueOf(value_int));
                    FactoryMainActivity.sAdapter.notifyDataSetChanged();
                    sm.SetContrast(value_int, is_save);
                    sm.SaveContrast(value_int);
                }
                break;
            case 4: {
                    setpicparam(map1, is_save);
                    value_str = map.get("sub_value");
                    if (-1 == step && value_str.equals("0"))
                        value_int = 100;
                    else
                        value_int = Math.abs(Integer.parseInt(value_str) + step) % 101;
                    map.put("sub_value", String.valueOf(value_int));
                    FactoryMainActivity.sAdapter.notifyDataSetChanged();
                    sm.SetSaturation(value_int, is_save);
                    sm.SaveSaturation(value_int);
                }
                break;
            case 5: {
                    setpicparam(map1, is_save);
                    value_str = map.get("sub_value");
                    if (-1 == step && value_str.equals("0"))
                        value_int = 100;
                    else
                        value_int = Math.abs(Integer.parseInt(value_str) + step) % 101;
                    map.put("sub_value", String.valueOf(value_int));
                    FactoryMainActivity.sAdapter.notifyDataSetChanged();
                    if (sm.SetSharpness(value_int, 0, 0) == 0) {
                        sm.SaveSharpness(value_int, 1);
                    }
                }
                break;
            case 6: {
                    setpicparam(map1, is_save);
                    value_str = map.get("sub_value");
                    if (-1 == step && value_str.equals("-50"))
                        value_int = 50;
                    else
                        value_int = Math.abs(Integer.parseInt(value_str) + step + 50) % 101 - 50;
                    map.put("sub_value", String.valueOf(value_int));
                    FactoryMainActivity.sAdapter.notifyDataSetChanged();
                    sm.SetHue(value_int, is_save);
                }
                break;
            default:
                break;
        }
    }

    public void adjustWhiteBalance(boolean left, int step) {
        String value_str = "";
        int value_int = 0;
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        TvControlManager.SourceInput source = null;
        ShowSubView ssv = new ShowSubView(mListSubMenuData, context);
        switch (FactoryMainActivity.mSubMenuPos) {
            case 0: {
                    int iInputPortCount = Constant.mInputPortList.length;
                    if (0 == FactoryMainActivity.mCurInputPortPos && left)
                        FactoryMainActivity.mCurInputPortPos = iInputPortCount - 1;
                    else
                        FactoryMainActivity.mCurInputPortPos = Math.abs(FactoryMainActivity.mCurInputPortPos + step) % iInputPortCount;
                    map.put("sub_value", context.getString(Constant.mInputPortList[FactoryMainActivity.mCurInputPortPos]));

                    for (int i = mListSubMenuData.size() - 1; i > 0; i--)
                        mListSubMenuData.remove(i);
                    ssv.setWhite(FactoryMainActivity.mCurInputPortPos, 0);
                    FactoryMainActivity.sAdapter.notifyDataSetChanged();
                }
                break;
            case 1: {
                    int mColorTemperatureListSize = Constant.mColorTemperatureList.length;
                    if (0 == FactoryMainActivity.mColorTemperaturePos && left)
                        FactoryMainActivity.mColorTemperaturePos = mColorTemperatureListSize - 1;
                    else
                        FactoryMainActivity.mColorTemperaturePos = Math.abs(FactoryMainActivity.mColorTemperaturePos + step) % mColorTemperatureListSize;
                    map.put("sub_value", context.getString(Constant.mColorTemperatureList[FactoryMainActivity.mColorTemperaturePos]));
                    for (int i = mListSubMenuData.size() - 1; i > 1; i--)
                        mListSubMenuData.remove(i);
                    // get value according new mode
                    ssv.setWhite2(FactoryMainActivity.mCurInputPortPos, FactoryMainActivity.mColorTemperaturePos);
                    FactoryMainActivity.sAdapter.notifyDataSetChanged();
                }
                break;
            case 2: {
                    value_str = map.get("sub_value");
                    if (-1 == step && value_str.equals("0"))
                        value_int = 2047;
                    else
                        value_int = Math.abs(Integer.parseInt(value_str) + step) % 2048;
                    map.put("sub_value", String.valueOf(value_int));
                    FactoryMainActivity.sAdapter.notifyDataSetChanged();
                    sm.FactoryWhiteBalanceSetRedGain(FactoryMainActivity.mCurInputPortPos, FactoryMainActivity.mColorTemperaturePos, value_int);
                }
                break;
            case 3: {
                    value_str = map.get("sub_value");
                    if (-1 == step && value_str.equals("0"))
                        value_int = 2047;
                    else
                        value_int = Math.abs(Integer.parseInt(value_str) + step) % 2048;
                    map.put("sub_value", String.valueOf(value_int));
                    FactoryMainActivity.sAdapter.notifyDataSetChanged();
                    sm.FactoryWhiteBalanceSetGreenGain(FactoryMainActivity.mCurInputPortPos, FactoryMainActivity.mColorTemperaturePos, value_int);
                }
                break;
            case 4: {
                    value_str = map.get("sub_value");
                    if (-1 == step && value_str.equals("0"))
                        value_int = 2047;
                    else
                        value_int = Math.abs(Integer.parseInt(value_str) + step) % 2048;
                    map.put("sub_value", String.valueOf(value_int));
                    FactoryMainActivity.sAdapter.notifyDataSetChanged();
                    sm.FactoryWhiteBalanceSetBlueGain(FactoryMainActivity.mCurInputPortPos, FactoryMainActivity.mColorTemperaturePos, value_int);
                }
                break;
            case 5: {
                    value_str = map.get("sub_value");
                    if (-1 == step && Integer.parseInt(value_str) == -1023)
                        value_int = 1024;
                    else if (1 == step && Integer.parseInt(value_str) == 1024)
                        value_int = -1023;
                    else
                        value_int = Math.abs(Integer.parseInt(value_str) + step + 1024) % 2049 - 1024;
                    map.put("sub_value", String.valueOf(value_int));
                    FactoryMainActivity.sAdapter.notifyDataSetChanged();
                    sm.FactoryWhiteBalanceSetRedOffset(FactoryMainActivity.mCurInputPortPos, FactoryMainActivity.mColorTemperaturePos, value_int);
                }
                break;
            case 6: {
                    value_str = map.get("sub_value");
                    if (-1 == step && Integer.parseInt(value_str) == -1023)
                        value_int = 1024;
                    else if (1 == step && Integer.parseInt(value_str) == 1024)
                        value_int = -1023;
                    else
                        value_int = Math.abs(Integer.parseInt(value_str) + step + 1024) % 2049 - 1024;
                    map.put("sub_value", String.valueOf(value_int));
                    FactoryMainActivity.sAdapter.notifyDataSetChanged();
                    sm.FactoryWhiteBalanceSetGreenOffset(FactoryMainActivity.mCurInputPortPos, FactoryMainActivity.mColorTemperaturePos, value_int);
                }
                break;
            case 7: {
                    value_str = map.get("sub_value");
                    if (-1 == step && Integer.parseInt(value_str) == -1023)
                        value_int = 1024;
                    else if (1 == step && Integer.parseInt(value_str) == 1024)
                        value_int = -1023;
                    else
                        value_int = Math.abs(Integer.parseInt(value_str) + step + 1024) % 2049 - 1024;
                    map.put("sub_value", String.valueOf(value_int));
                    FactoryMainActivity.sAdapter.notifyDataSetChanged();
                    sm.FactoryWhiteBalanceSetBlueOffset(FactoryMainActivity.mCurInputPortPos, FactoryMainActivity.mColorTemperaturePos, value_int);
                }
                break;
            default:
                break;
        }
    }

    public void adjustSSC(boolean left, int step) {
        String value_str = "";
        int value_int = 0;
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        switch (FactoryMainActivity.mSubMenuPos) {
            case 0: {
                    value_str = map.get("sub_value");
                    if (-1 == step && value_str.equals("0"))
                        value_int = 5;
                    else
                        value_int = Math.abs(Integer.parseInt(value_str) + step) % 6;

                    map.put("sub_value", value_int + "");
                    FactoryMainActivity.sAdapter.notifyDataSetChanged();
                    sm.FactorySetLVDSSSC(value_int);
                }
                break;
            default:
                break;
        }
    }

    public void adjustNolinear(boolean left, int step) {
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        TvControlManager.SourceInput source = TvControlManager.SourceInput.values()[FactoryMainActivity.mCurInputPortPos];
        switch (FactoryMainActivity.mSubMenuPos) {
            case 0:
                int iInputPortCount = Constant.mInputPortList.length;
                if (0 == FactoryMainActivity.mCurInputPortPos && left)
                    FactoryMainActivity.mCurInputPortPos = iInputPortCount - 1;
                else
                    FactoryMainActivity.mCurInputPortPos = Math.abs(FactoryMainActivity.mCurInputPortPos + step) % iInputPortCount;
                map.put("sub_value", context.getString(Constant.mInputPortList[FactoryMainActivity.mCurInputPortPos]));
                source = TvControlManager.SourceInput.values()[FactoryMainActivity.mCurInputPortPos];
                for (int i = mListSubMenuData.size() - 1; i > 0; i--)
                    mListSubMenuData.remove(i);
                ShowSubView ssv = new ShowSubView(mListSubMenuData, context);
                ssv.setNoLine(source);
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
    }

    /*set reShow*/
    private static TvControlManager.SourceInput source = TvControlManager.SourceInput.values()[0];
    private static TvInSignalInfo.SignalFmt fmt = TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_CVBS_NTSC_M;
    private static TvInSignalInfo.TransFmt trans_fmt = TvInSignalInfo.TransFmt.values()[0];

    public void adjustReshow(boolean left, int step) {
        String value_str = "";
        int value_int = 0;
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        ShowSubView ssv = new ShowSubView(mListSubMenuData, context);
        TvControlManager.tvin_cutwin_t cutwin_t = tv.FactoryGetOverscanParams(source, fmt, trans_fmt);
        switch (FactoryMainActivity.mSubMenuPos) {
            case 0:
                int iInputPortCount = Constant.mInputPortList.length;
                FactoryMainActivity.mCurInputPortPos = (FactoryMainActivity.mCurInputPortPos + step + iInputPortCount) % iInputPortCount;
                map.put("sub_value", context.getString(Constant.mInputPortList[FactoryMainActivity.mCurInputPortPos]));
                source = TvControlManager.SourceInput.values()[FactoryMainActivity.mCurInputPortPos];
                if (source == TvControlManager.SourceInput.AV1 || source == TvControlManager.SourceInput.TV ||source == TvControlManager.SourceInput.AV2)
                    fmt = TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_CVBS_NTSC_M;
                else if (source == TvControlManager.SourceInput.HDMI1|| source == TvControlManager.SourceInput.DTV
                         || source == TvControlManager.SourceInput.HDMI2 ||source == TvControlManager.SourceInput.HDMI3 )
                    fmt = TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_HDMI_640X480P_60HZ;
                else if (source == TvControlManager.SourceInput.YPBPR1 ||source == TvControlManager.SourceInput.YPBPR2 )
                    fmt = TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_COMP_480P_60HZ_D000;
                else if (source == TvControlManager.SourceInput.VGA)
                    fmt = TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_VGA_512X384P_60HZ_D147;
                else
                    fmt = TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_HDMI_640X480P_60HZ;
                for (int i = mListSubMenuData.size() - 1; i > 0; i--)
                    mListSubMenuData.remove(i);
                // get new value according new mode
                ssv.setTiming(source, fmt, trans_fmt);
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                break;
            case 1:
                value_int = fmt.toInt() + step;
                if (source == TvControlManager.SourceInput.values()[0] ||
                    source == TvControlManager.SourceInput.values()[1] ||
                    source == TvControlManager.SourceInput.values()[2]) {
                    if (1 == step && value_int > TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_CVBS_MAX.toInt())
                        fmt = TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_CVBS_NTSC_M;
                    else if (-1 == step && value_int < TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_CVBS_NTSC_M.toInt())
                        fmt = TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_CVBS_MAX;
                    else
                        fmt = TvInSignalInfo.SignalFmt.valueOf(value_int);
                } else if (source == TvControlManager.SourceInput.values()[5] ||
                           source == TvControlManager.SourceInput.values()[6] ||
                           source == TvControlManager.SourceInput.values()[7] ||
                           source == TvControlManager.SourceInput.values()[10]) {
                    if (1 == step && value_int > TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_HDMI_MAX.toInt())
                        fmt = TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_HDMI_640X480P_60HZ;
                    else if (-1 == step && value_int < TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_HDMI_640X480P_60HZ.toInt())
                        fmt = TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_HDMI_MAX;
                    else
                        fmt = TvInSignalInfo.SignalFmt.valueOf(value_int);
                } else if (source == TvControlManager.SourceInput.values()[3] ||
                           source == TvControlManager.SourceInput.values()[4]) {
                    if (1 == step && value_int > TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_COMP_MAX.toInt())
                        fmt = TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_COMP_480P_60HZ_D000;
                    else if (-1 == step && value_int < TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_COMP_480P_60HZ_D000.toInt())
                        fmt = TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_COMP_MAX;
                    else
                        fmt = TvInSignalInfo.SignalFmt.valueOf(value_int);
                } else if (source == TvControlManager.SourceInput.values()[8]) {
                    if (1 == step && value_int > TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_VGA_MAX.toInt())
                        fmt = TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_VGA_512X384P_60HZ_D147;
                    else if (-1 == step && value_int < TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_VGA_512X384P_60HZ_D147.toInt())
                        fmt = TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_VGA_MAX;
                    else
                        fmt = TvInSignalInfo.SignalFmt.valueOf(value_int);
                } else {
                 if (1 == step && value_int > TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_HDMI_MAX.toInt())
                     fmt = TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_HDMI_640X480P_60HZ;
                 else if (-1 == step && value_int < TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_HDMI_640X480P_60HZ.toInt())
                     fmt = TvInSignalInfo.SignalFmt.TVIN_SIG_FMT_HDMI_MAX;
                 else
                     fmt = TvInSignalInfo.SignalFmt.valueOf(value_int);
                }
                map.put("sub_value", fmt + "");
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                for (int i = mListSubMenuData.size() - 1; i > 3; i--)
                    mListSubMenuData.remove(i);
                // get new value according new mode
                ssv.setElse(source, fmt, trans_fmt);
                break;
            case 2:
                int reshowTvinCount = Constant.mChongxianTvIn3Dstatus.length;
                if (0 == FactoryMainActivity.reshowTvinPos && left)
                    FactoryMainActivity.reshowTvinPos = reshowTvinCount - 1;
                else
                    FactoryMainActivity.reshowTvinPos = Math.abs(FactoryMainActivity.reshowTvinPos + step) % reshowTvinCount;
                map.put("sub_value", context.getString(Constant.mChongxianTvIn3Dstatus[FactoryMainActivity.reshowTvinPos]));
                for (int i = mListSubMenuData.size() - 1; i > 3; i--)
                    mListSubMenuData.remove(i);
                ssv.setElse(source, fmt, trans_fmt);
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                break;
            case 3:
                int reshowTvinTransFmtCount = Constant.mChongxianTvInTransFmt.length;
                if (0 == FactoryMainActivity.reshowTvinTransFmtPos && left)
                    FactoryMainActivity.reshowTvinTransFmtPos = reshowTvinTransFmtCount - 1;
                else
                    FactoryMainActivity.reshowTvinTransFmtPos = Math.abs(FactoryMainActivity.reshowTvinTransFmtPos + step) % reshowTvinTransFmtCount;
                map.put("sub_value", context.getString(Constant.mChongxianTvInTransFmt[FactoryMainActivity.reshowTvinTransFmtPos]));
                trans_fmt = TvInSignalInfo.TransFmt.values()[FactoryMainActivity.reshowTvinTransFmtPos];
                for (int i = mListSubMenuData.size() - 1; i > 3; i--)
                    mListSubMenuData.remove(i);
                ssv.setElse(source, fmt, trans_fmt);
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                break;
            case 4:
                value_str = map.get("sub_value");
                value_int = Integer.parseInt(value_str) + step;
                map.put("sub_value", value_int + "");
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                cutwin_t.hs = value_int;
                //sm.FactorySetOverscanParams(source, fmt, trans_fmt, cutwin_t);
                break;
            case 5:
                value_str = map.get("sub_value");
                value_int = Integer.parseInt(value_str) + step;
                map.put("sub_value", value_int + "");
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                cutwin_t.vs = value_int;
                //sm.FactorySetOverscanParams(source, fmt, trans_fmt, cutwin_t);
                break;
            case 6:
                value_str = map.get("sub_value");
                value_int = Integer.parseInt(value_str) + step;
                map.put("sub_value", value_int + "");
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                cutwin_t.he = value_int;
                //sm.FactorySetOverscanParams(source, fmt, trans_fmt, cutwin_t);
                break;
            case 7:
                value_str = map.get("sub_value");
                value_int = Integer.parseInt(value_str) + step;
                map.put("sub_value", value_int + "");
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                cutwin_t.ve = value_int;
                //sm.FactorySetOverscanParams(source, fmt, trans_fmt, cutwin_t);
                break;
            default:
                break;
        }
    }

    public void adjustFunction(boolean left, int step) {
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        switch (FactoryMainActivity.mSubMenuPos) {
            case 0:
                int iInputPortCount = Constant.CESHITUARRAY_STRINGS.length;
                if (0 == FactoryMainActivity.ceshituPos && left)
                    FactoryMainActivity.ceshituPos = iInputPortCount - 1;
                else
                    FactoryMainActivity.ceshituPos = Math.abs(FactoryMainActivity.ceshituPos + step) % iInputPortCount;
                map.put("sub_value", context.getString(Constant.CESHITUARRAY_STRINGS[FactoryMainActivity.ceshituPos]));
                tv.FactorySetTestPattern(FactoryMainActivity.ceshituPos);
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
    }

    public void adjustAging(boolean left, int step) {
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        switch (FactoryMainActivity.mSubMenuPos) {
            case 0:
                if (tv.SSMReadAgingMode() == 0)
                    tv.SSMSaveAgingMode(1);
                else
                    tv.SSMSaveAgingMode(0);
                map.put("sub_value", tv.SSMReadAgingMode() == 0 ? context.getString(Constant.FACUI_LAOHUA_OFF) : context.getString(Constant.FACUI_LAOHUA_ON));
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
    }

    public void adjust_dym_bl(boolean left, int step) {
        String value_str = "";
        int value_int = 0;
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        switch (FactoryMainActivity.mSubMenuPos) {
            case 0: {
                    value_str = map.get("sub_value");
                    if (-1 == step && value_str.equals("0"))
                        value_int = 100;
                    else
                        value_int = Math.abs(Integer.parseInt(value_str) + step) % 101;
                    map.put("sub_value", String.valueOf(value_int));
                    FactoryMainActivity.sAdapter.notifyDataSetChanged();
                }
                break;
            case 1: {
                    value_str = map.get("sub_value");
                    if (-1 == step && value_str.equals("0"))
                        value_int = 100;
                    else
                        value_int = Math.abs(Integer.parseInt(value_str) + step) % 101;
                    map.put("sub_value", String.valueOf(value_int));
                    FactoryMainActivity.sAdapter.notifyDataSetChanged();
                }
                break;
            case 2: {
                    value_str = map.get("sub_value");
                    if (-1 == step && value_str.equals("0"))
                        value_int = 100;
                    else
                        value_int = Math.abs(Integer.parseInt(value_str) + step) % 101;
                    map.put("sub_value", String.valueOf(value_int));
                    FactoryMainActivity.sAdapter.notifyDataSetChanged();
                }
                break;
            case 3: {
                    value_str = map.get("sub_value");
                    if (-1 == step && value_str.equals("0"))
                        value_int = 100;
                    else
                        value_int = Math.abs(Integer.parseInt(value_str) + step) % 101;
                    map.put("sub_value", String.valueOf(value_int));
                    FactoryMainActivity.sAdapter.notifyDataSetChanged();
                }
                break;
            case 4: {
                    value_str = map.get("sub_value");
                    if (-1 == step && value_str.equals("0"))
                        value_int = 100;
                    else
                        value_int = Math.abs(Integer.parseInt(value_str) + step) % 101;
                    map.put("sub_value", String.valueOf(value_int));
                    FactoryMainActivity.sAdapter.notifyDataSetChanged();
                }
                break;
            default:
                break;
        }
    }

    public void cvbsSetValue(boolean left, int step) {
        String value_str = "";
        int value_int = 0;
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        switch (FactoryMainActivity.mSubMenuPos) {
            case 0: {
                    value_str = map.get("sub_value");
                    if (-1 == step && value_str.equals("80"))
                        value_int = 130;
                    else
                        value_int = Math.abs(Integer.parseInt(value_str) + step - 80) % 51 + 80;
                    map.put("sub_value", String.valueOf(value_int));
                    FactoryMainActivity.sAdapter.notifyDataSetChanged();
                }
                break;
            default:
                break;
        }
    }

    public void hdcpSetValue() {
        String value_str = "";
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        switch (FactoryMainActivity.mSubMenuPos) {
            case 0:
                value_str = map.get("sub_value");
                if (value_str.equalsIgnoreCase(context.getString(Constant.FACUI_HDCP_DEMOKEY_ON))) {
                    map.put("sub_value", context.getString(Constant.FACUI_HDCP_DEMOKEY_OFF));
                    //tv.SSMSaveUsingDefaultHDCPKeyFlag(0);
                } else if (value_str.equalsIgnoreCase(context.getString(Constant.FACUI_HDCP_DEMOKEY_OFF))) {
                    map.put("sub_value", context.getString(Constant.FACUI_HDCP_DEMOKEY_ON));
                    //tv.SSMSaveUsingDefaultHDCPKeyFlag(1);
                }
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
    }

    public void serialSetValue() {
        String value_str = "";
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        switch (FactoryMainActivity.mSubMenuPos) {
            case 0:
                value_str = map.get("sub_value");
                if (value_str.equalsIgnoreCase(context.getString(Constant.FACUI_SERIAL_CMD_SWITCH_ON))) {
                    map.put("sub_value", context.getString(Constant.FACUI_SERIAL_CMD_SWITCH_OFF));
                    tv.SSMSaveSerialCMDSwitchValue(0);
                } else if (value_str.equalsIgnoreCase(context.getString(Constant.FACUI_SERIAL_CMD_SWITCH_OFF))) {
                    map.put("sub_value", context.getString(Constant.FACUI_SERIAL_CMD_SWITCH_ON));
                    tv.SSMSaveSerialCMDSwitchValue(1);
                }
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
    }

    public void autosavelogSetValue() {
        String value_str = "";
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        Intent logintent = new Intent();
        logintent.setAction("com.amlogic.action.savelogtoudisk");
        switch (FactoryMainActivity.mSubMenuPos) {
            case 0:
                value_str = map.get("sub_value");
                if (value_str.equalsIgnoreCase(context.getString(Constant.FACUI_AUTOSAVELOG_SWITCH_ON))) {
                    context.stopService(logintent);
                    map.put("sub_value", context.getString(Constant.FACUI_AUTOSAVELOG_SWITCH_OFF));
                } else if (value_str.equalsIgnoreCase(context.getString(Constant.FACUI_AUTOSAVELOG_SWITCH_OFF))) {
                    context.startService(logintent);
                    map.put("sub_value", context.getString(Constant.FACUI_AUTOSAVELOG_SWITCH_ON));
                }
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
    }

    public void dynamicbacklightSetValue(boolean left, int step) {
        int value_int = 0;
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        switch (FactoryMainActivity.mSubMenuPos) {
            case 0:
                String value_str = map.get("sub_value");
                if (-1 == step && value_str.equals("0"))
                    value_int = 100;
                else
                    value_int = Math.abs(Integer.parseInt(value_str) + step) % 101;
                map.put("sub_value", String.valueOf(value_int));
                //SystemProperties.set("persist.tv.auto_bl_value", value_int + "");
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                break;
            default:
                break;
        }

    }

}

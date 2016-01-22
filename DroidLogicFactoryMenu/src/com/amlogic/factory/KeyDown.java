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
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.util.Log;

public class KeyDown
{
    public static TvControlManager tv = TvControlManager.open();
    /* son ListView's item save to this List */
    public List<Map<String, String>> mListSubMenuData = null;

    private Context context;

    public KeyDown(List<Map<String, String>> mListSubMenuData, Context context)
    {
        this.mListSubMenuData = mListSubMenuData;
        this.context = context;
    }

    public void adjustCalibrate(boolean left, int step)
    {
        String value_str = "";
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        switch (FactoryMainActivity.mSubMenuPos)
        {
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

    private void setpicparam(Map<String, String> map1, TvControlManager.SourceInput_Type source, int is_save)
    {
        if (FactoryMainActivity.mCurShowModePos != 3)
        {
            int brightness = tv.GetBrightness(source);
            int contrast = tv.GetContrast(source);
            int saturation = tv.GetSaturation(source);
            int sharpness = tv.GetSharpness(source);
            FactoryMainActivity.mCurShowModePos = 3;
            tv.SetPQMode(TvControlManager.Pq_Mode.values()[FactoryMainActivity.mCurShowModePos], source, is_save);
            map1.put("sub_value", context.getString(Constant.mShowModeList[FactoryMainActivity.mCurShowModePos]));
            FactoryMainActivity.sAdapter.notifyDataSetChanged();
            tv.SetBrightness(brightness, source, is_save);
            tv.SaveBrightness(brightness, source);
            tv.SetContrast(contrast, source, is_save);
            tv.SaveContrast(contrast, source);
            tv.SetSaturation(saturation, source, tv.GetCurrentSignalInfo().fmt, is_save);
            tv.SaveSaturation(saturation, source);
            tv.SetSharpness(sharpness, source, 0, 0, 0);
            tv.SaveSharpness(sharpness, source, 1);
        }
    }

    public void adjustPicMode(boolean left, int step)
    {
        String value_str = "";
        int value_int = 0;
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        Map<String, String> map1 = mListSubMenuData.get(1);
        TvControlManager.SourceInput_Type source = TvControlManager.SourceInput_Type.values()[FactoryMainActivity.mCurInputPortPos];
        int is_save = 1;// if is_save is 1,auto save when setting,if is 0,call set function first,then save
        ShowSubView ssv = new ShowSubView(mListSubMenuData, context);
        switch (FactoryMainActivity.mSubMenuPos)
        {
            case 0:
            {
                int iInputPortCount = Constant.mInputPortList.length;
                if (0 == FactoryMainActivity.mCurInputPortPos && left)
                    FactoryMainActivity.mCurInputPortPos = iInputPortCount - 1;
                else
                    FactoryMainActivity.mCurInputPortPos = Math.abs(FactoryMainActivity.mCurInputPortPos + step) % iInputPortCount;
                map.put("sub_value", context.getString(Constant.mInputPortList[FactoryMainActivity.mCurInputPortPos]));
                source = TvControlManager.SourceInput_Type.values()[FactoryMainActivity.mCurInputPortPos];
                for (int i = mListSubMenuData.size() - 1; i > 0; i--)
                    mListSubMenuData.remove(i);
                ssv.setPicture(source);
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
            }
                break;
            case 1:
            {
                int mCurShowModeSize = Constant.mShowModeList.length;
                FactoryMainActivity.mCurShowModePos = Math.abs(FactoryMainActivity.mCurShowModePos + step + mCurShowModeSize) % mCurShowModeSize;
                map.put("sub_value", context.getString(Constant.mShowModeList[FactoryMainActivity.mCurShowModePos]));
                tv.SetPQMode(TvControlManager.Pq_Mode.values()[FactoryMainActivity.mCurShowModePos], source, is_save);
                tv.SavePQMode(TvControlManager.Pq_Mode.values()[FactoryMainActivity.mCurShowModePos], source);
                for (int i = mListSubMenuData.size() - 1; i > 0; i--)
                    mListSubMenuData.remove(i);
                ssv.setPicture(source);
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
            }
                break;
            case 2:
            {
                setpicparam(map1, source, is_save);
                value_str = map.get("sub_value");
                if (-1 == step && value_str.equals("0"))
                    value_int = 100;
                else
                    value_int = Math.abs(Integer.parseInt(value_str) + step) % 101;
                map.put("sub_value", String.valueOf(value_int));
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                tv.SetBrightness(value_int, source, is_save);
                tv.SaveBrightness(value_int, source);
            }
                break;
            case 3:
            {
                setpicparam(map1, source, is_save);
                value_str = map.get("sub_value");
                if (-1 == step && value_str.equals("0"))
                    value_int = 100;
                else
                    value_int = Math.abs(Integer.parseInt(value_str) + step) % 101;
                map.put("sub_value", String.valueOf(value_int));
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                tv.SetContrast(value_int, source, is_save);
                tv.SaveContrast(value_int, source);
            }
                break;
            case 4:
            {
                setpicparam(map1, source, is_save);
                value_str = map.get("sub_value");
                if (-1 == step && value_str.equals("0"))
                    value_int = 100;
                else
                    value_int = Math.abs(Integer.parseInt(value_str) + step) % 101;
                map.put("sub_value", String.valueOf(value_int));
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                tv.SetSaturation(value_int, source, tv.GetCurrentSignalInfo().fmt, is_save);
                tv.SaveSaturation(value_int, source);
            }
                break;
            case 5:
            {
                setpicparam(map1, source, is_save);
                value_str = map.get("sub_value");
                if (-1 == step && value_str.equals("0"))
                    value_int = 100;
                else
                    value_int = Math.abs(Integer.parseInt(value_str) + step) % 101;
                map.put("sub_value", String.valueOf(value_int));
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                if (tv.SetSharpness(value_int, source, 0, 0, 0) == 0)
                {
                    tv.SaveSharpness(value_int, source, 1);
                }
            }
                break;
            case 6:
            {
                setpicparam(map1, source, is_save);
                value_str = map.get("sub_value");
                if (-1 == step && value_str.equals("-50"))
                    value_int = 50;
                else
                    value_int = Math.abs(Integer.parseInt(value_str) + step + 50) % 101 - 50;
                map.put("sub_value", String.valueOf(value_int));
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                tv.SetHue(value_int, source, tv.GetCurrentSignalInfo().fmt, is_save);
            }
                break;
            default:
                break;
        }
    }

    public void adjustWhiteBalance(boolean left, int step)
    {
        String value_str = "";
        int value_int = 0;
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        TvControlManager.SourceInput_Type source = null;
        ShowSubView ssv = new ShowSubView(mListSubMenuData, context);
        switch (FactoryMainActivity.mSubMenuPos)
        {
            case 0:
            {
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
            case 1:
            {
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
            case 2:
            {
                value_str = map.get("sub_value");
                if (-1 == step && value_str.equals("0"))
                    value_int = 2047;
                else
                    value_int = Math.abs(Integer.parseInt(value_str) + step) % 2048;
                map.put("sub_value", String.valueOf(value_int));
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                tv.FactoryWhiteBalanceSetRedGain(FactoryMainActivity.mCurInputPortPos, FactoryMainActivity.mColorTemperaturePos, value_int);
            }
                break;
            case 3:
            {
                value_str = map.get("sub_value");
                if (-1 == step && value_str.equals("0"))
                    value_int = 2047;
                else
                    value_int = Math.abs(Integer.parseInt(value_str) + step) % 2048;
                map.put("sub_value", String.valueOf(value_int));
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                tv.FactoryWhiteBalanceSetGreenGain(FactoryMainActivity.mCurInputPortPos, FactoryMainActivity.mColorTemperaturePos, value_int);
            }
                break;
            case 4:
            {
                value_str = map.get("sub_value");
                if (-1 == step && value_str.equals("0"))
                    value_int = 2047;
                else
                    value_int = Math.abs(Integer.parseInt(value_str) + step) % 2048;
                map.put("sub_value", String.valueOf(value_int));
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                tv.FactoryWhiteBalanceSetBlueGain(FactoryMainActivity.mCurInputPortPos, FactoryMainActivity.mColorTemperaturePos, value_int);
            }
                break;
            case 5:
            {
                value_str = map.get("sub_value");
                if (-1 == step && Integer.parseInt(value_str) == -1023)
                    value_int = 1024;
                else if (1 == step && Integer.parseInt(value_str) == 1024)
                    value_int = -1023;
                else
                    value_int = Math.abs(Integer.parseInt(value_str) + step + 1024) % 2049 - 1024;
                map.put("sub_value", String.valueOf(value_int));
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                tv.FactoryWhiteBalanceSetRedOffset(FactoryMainActivity.mCurInputPortPos, FactoryMainActivity.mColorTemperaturePos, value_int);
            }
                break;
            case 6:
            {
                value_str = map.get("sub_value");
                if (-1 == step && Integer.parseInt(value_str) == -1023)
                    value_int = 1024;
                else if (1 == step && Integer.parseInt(value_str) == 1024)
                    value_int = -1023;
                else
                    value_int = Math.abs(Integer.parseInt(value_str) + step + 1024) % 2049 - 1024;
                map.put("sub_value", String.valueOf(value_int));
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                tv.FactoryWhiteBalanceSetGreenOffset(FactoryMainActivity.mCurInputPortPos, FactoryMainActivity.mColorTemperaturePos, value_int);
            }
                break;
            case 7:
            {
                value_str = map.get("sub_value");
                if (-1 == step && Integer.parseInt(value_str) == -1023)
                    value_int = 1024;
                else if (1 == step && Integer.parseInt(value_str) == 1024)
                    value_int = -1023;
                else
                    value_int = Math.abs(Integer.parseInt(value_str) + step + 1024) % 2049 - 1024;
                map.put("sub_value", String.valueOf(value_int));
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                tv.FactoryWhiteBalanceSetBlueOffset(FactoryMainActivity.mCurInputPortPos, FactoryMainActivity.mColorTemperaturePos, value_int);
            }
                break;
            default:
                break;
        }
    }

    public void adjustSSC(boolean left, int step)
    {
        String value_str = "";
        int value_int = 0;
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        switch (FactoryMainActivity.mSubMenuPos)
        {
            case 0:
            {
                value_str = map.get("sub_value");
                if (-1 == step && value_str.equals("0"))
                    value_int = 5;
                else
                    value_int = Math.abs(Integer.parseInt(value_str) + step) % 6;

                map.put("sub_value", value_int + "");
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                tv.FactorySetLVDSSSC(value_int);
            }
                break;
            default:
                break;
        }
    }

    public void adjustNolinear(boolean left, int step)
    {
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        TvControlManager.SourceInput_Type source = TvControlManager.SourceInput_Type.values()[FactoryMainActivity.mCurInputPortPos];
        switch (FactoryMainActivity.mSubMenuPos)
        {
            case 0:
                int iInputPortCount = Constant.mInputPortList.length;
                if (0 == FactoryMainActivity.mCurInputPortPos && left)
                    FactoryMainActivity.mCurInputPortPos = iInputPortCount - 1;
                else
                    FactoryMainActivity.mCurInputPortPos = Math.abs(FactoryMainActivity.mCurInputPortPos + step) % iInputPortCount;
                map.put("sub_value", context.getString(Constant.mInputPortList[FactoryMainActivity.mCurInputPortPos]));
                source = TvControlManager.SourceInput_Type.values()[FactoryMainActivity.mCurInputPortPos];
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
    private static TvControlManager.SourceInput_Type source = TvControlManager.SourceInput_Type.values()[0];
    private static TvControlManager.tvin_sig_fmt_e fmt = TvControlManager.tvin_sig_fmt_e.TVIN_SIG_FMT_CVBS_NTSC_M;
    private static TvControlManager.Tvin_3d_Status status_3d = TvControlManager.Tvin_3d_Status.values()[0];
    private static TvControlManager.tvin_trans_fmt trans_fmt = TvControlManager.tvin_trans_fmt.values()[0];

    public void adjustReshow(boolean left, int step)
    {
        String value_str = "";
        int value_int = 0;
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        ShowSubView ssv = new ShowSubView(mListSubMenuData, context);
        TvControlManager.tvin_cutwin_t cutwin_t = tv.FactoryGetOverscanParams(source, fmt, status_3d, trans_fmt);
        switch (FactoryMainActivity.mSubMenuPos)
        {
            case 0:
                int iInputPortCount = Constant.mInputPortList.length;
                FactoryMainActivity.mCurInputPortPos = (FactoryMainActivity.mCurInputPortPos + step + iInputPortCount) % iInputPortCount;
                map.put("sub_value", context.getString(Constant.mInputPortList[FactoryMainActivity.mCurInputPortPos]));
                source = TvControlManager.SourceInput_Type.values()[FactoryMainActivity.mCurInputPortPos];
                if (source == TvControlManager.SourceInput_Type.SOURCE_TYPE_AV || source == TvControlManager.SourceInput_Type.SOURCE_TYPE_TV)
                    fmt = TvControlManager.tvin_sig_fmt_e.TVIN_SIG_FMT_CVBS_NTSC_M;
                else if (source == TvControlManager.SourceInput_Type.SOURCE_TYPE_MPEG || source == TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV
                        || source == TvControlManager.SourceInput_Type.SOURCE_TYPE_HDMI)
                    fmt = TvControlManager.tvin_sig_fmt_e.TVIN_SIG_FMT_HDMI_640X480P_60HZ;
                else if (source == TvControlManager.SourceInput_Type.SOURCE_TYPE_COMPONENT)
                    fmt = TvControlManager.tvin_sig_fmt_e.TVIN_SIG_FMT_COMP_480P_60HZ_D000;
                else if (source == TvControlManager.SourceInput_Type.SOURCE_TYPE_VGA)
                    fmt = TvControlManager.tvin_sig_fmt_e.TVIN_SIG_FMT_VGA_512X384P_60HZ_D147;
                for (int i = mListSubMenuData.size() - 1; i > 0; i--)
                    mListSubMenuData.remove(i);
                // get new value according new mode
                ssv.setTiming(source, fmt, status_3d, trans_fmt);
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                break;
            case 1:
                value_int = fmt.toInt() + step;
                if (source == TvControlManager.SourceInput_Type.values()[0] || source == TvControlManager.SourceInput_Type.values()[1])
                {
                    if (1 == step && value_int > TvControlManager.tvin_sig_fmt_e.TVIN_SIG_FMT_CVBS_MAX.toInt())
                        fmt = TvControlManager.tvin_sig_fmt_e.TVIN_SIG_FMT_CVBS_NTSC_M;
                    else if (-1 == step && value_int < TvControlManager.tvin_sig_fmt_e.TVIN_SIG_FMT_CVBS_NTSC_M.toInt())
                        fmt = TvControlManager.tvin_sig_fmt_e.TVIN_SIG_FMT_CVBS_MAX;
                    else
                        fmt = TvControlManager.tvin_sig_fmt_e.valueOf(value_int);
                }
                else if (source == TvControlManager.SourceInput_Type.values()[5] || source == TvControlManager.SourceInput_Type.values()[6] || source == TvControlManager.SourceInput_Type.values()[3])
                {
                    if (1 == step && value_int > TvControlManager.tvin_sig_fmt_e.TVIN_SIG_FMT_HDMI_MAX.toInt())
                        fmt = TvControlManager.tvin_sig_fmt_e.TVIN_SIG_FMT_HDMI_640X480P_60HZ;
                    else if (-1 == step && value_int < TvControlManager.tvin_sig_fmt_e.TVIN_SIG_FMT_HDMI_640X480P_60HZ.toInt())
                        fmt = TvControlManager.tvin_sig_fmt_e.TVIN_SIG_FMT_HDMI_MAX;
                    else
                        fmt = TvControlManager.tvin_sig_fmt_e.valueOf(value_int);
                }
                else if (source == TvControlManager.SourceInput_Type.values()[2])
                {
                    if (1 == step && value_int > TvControlManager.tvin_sig_fmt_e.TVIN_SIG_FMT_COMP_MAX.toInt())
                        fmt = TvControlManager.tvin_sig_fmt_e.TVIN_SIG_FMT_COMP_480P_60HZ_D000;
                    else if (-1 == step && value_int < TvControlManager.tvin_sig_fmt_e.TVIN_SIG_FMT_COMP_480P_60HZ_D000.toInt())
                        fmt = TvControlManager.tvin_sig_fmt_e.TVIN_SIG_FMT_COMP_MAX;
                    else
                        fmt = TvControlManager.tvin_sig_fmt_e.valueOf(value_int);
                }
                else if (source == TvControlManager.SourceInput_Type.values()[4])
                {
                    if (1 == step && value_int > TvControlManager.tvin_sig_fmt_e.TVIN_SIG_FMT_VGA_MAX.toInt())
                        fmt = TvControlManager.tvin_sig_fmt_e.TVIN_SIG_FMT_VGA_512X384P_60HZ_D147;
                    else if (-1 == step && value_int < TvControlManager.tvin_sig_fmt_e.TVIN_SIG_FMT_VGA_512X384P_60HZ_D147.toInt())
                        fmt = TvControlManager.tvin_sig_fmt_e.TVIN_SIG_FMT_VGA_MAX;
                    else
                        fmt = TvControlManager.tvin_sig_fmt_e.valueOf(value_int);
                }
                map.put("sub_value", fmt + "");
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                for (int i = mListSubMenuData.size() - 1; i > 3; i--)
                    mListSubMenuData.remove(i);
                // get new value according new mode
                ssv.setElse(source, fmt, status_3d, trans_fmt);
                break;
            case 2:
                int reshowTvinCount = Constant.mChongxianTvIn3Dstatus.length;
                if (0 == FactoryMainActivity.reshowTvinPos && left)
                    FactoryMainActivity.reshowTvinPos = reshowTvinCount - 1;
                else
                    FactoryMainActivity.reshowTvinPos = Math.abs(FactoryMainActivity.reshowTvinPos + step) % reshowTvinCount;
                map.put("sub_value", context.getString(Constant.mChongxianTvIn3Dstatus[FactoryMainActivity.reshowTvinPos]));
                status_3d = TvControlManager.Tvin_3d_Status.values()[FactoryMainActivity.reshowTvinPos];
                for (int i = mListSubMenuData.size() - 1; i > 3; i--)
                    mListSubMenuData.remove(i);
                ssv.setElse(source, fmt, status_3d, trans_fmt);
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                break;
            case 3:
                int reshowTvinTransFmtCount = Constant.mChongxianTvInTransFmt.length;
                if (0 == FactoryMainActivity.reshowTvinTransFmtPos && left)
                    FactoryMainActivity.reshowTvinTransFmtPos = reshowTvinTransFmtCount - 1;
                else
                    FactoryMainActivity.reshowTvinTransFmtPos = Math.abs(FactoryMainActivity.reshowTvinTransFmtPos + step) % reshowTvinTransFmtCount;
                map.put("sub_value", context.getString(Constant.mChongxianTvInTransFmt[FactoryMainActivity.reshowTvinTransFmtPos]));
                trans_fmt = TvControlManager.tvin_trans_fmt.values()[FactoryMainActivity.reshowTvinTransFmtPos];
                for (int i = mListSubMenuData.size() - 1; i > 3; i--)
                    mListSubMenuData.remove(i);
                ssv.setElse(source, fmt, status_3d, trans_fmt);
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                break;
            case 4:
                value_str = map.get("sub_value");
                value_int = Integer.parseInt(value_str) + step;
                map.put("sub_value", value_int + "");
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                cutwin_t.hs = value_int;
                tv.FactorySetOverscanParams(source, fmt, status_3d, trans_fmt, cutwin_t);
                break;
            case 5:
                value_str = map.get("sub_value");
                value_int = Integer.parseInt(value_str) + step;
                map.put("sub_value", value_int + "");
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                cutwin_t.vs = value_int;
                tv.FactorySetOverscanParams(source, fmt, status_3d, trans_fmt, cutwin_t);
                break;
            case 6:
                value_str = map.get("sub_value");
                value_int = Integer.parseInt(value_str) + step;
                map.put("sub_value", value_int + "");
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                cutwin_t.he = value_int;
                tv.FactorySetOverscanParams(source, fmt, status_3d, trans_fmt, cutwin_t);
                break;
            case 7:
                value_str = map.get("sub_value");
                value_int = Integer.parseInt(value_str) + step;
                map.put("sub_value", value_int + "");
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                cutwin_t.ve = value_int;
                tv.FactorySetOverscanParams(source, fmt, status_3d, trans_fmt, cutwin_t);
                break;
            default:
                break;
        }
    }

    public void adjustFunction(boolean left, int step)
    {
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        switch (FactoryMainActivity.mSubMenuPos)
        {
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

    public void adjustAging(boolean left, int step)
    {
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        switch (FactoryMainActivity.mSubMenuPos)
        {
            case 0:
                if (tv.SSMReadAgingMode() == 0)
                    tv.SSMSaveAgingMode(1);
                else
                    tv.SSMSaveAgingMode(0);
                map.put("sub_value", tv.SSMReadAgingMode()==0?context.getString(Constant.FACUI_LAOHUA_OFF):context.getString(Constant.FACUI_LAOHUA_ON));
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
    }

    public void adjust_dym_bl(boolean left, int step)
    {
        String value_str = "";
        int value_int = 0;
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        switch (FactoryMainActivity.mSubMenuPos)
        {
            case 0:
            {
                value_str = map.get("sub_value");
                if (-1 == step && value_str.equals("0"))
                    value_int = 100;
                else
                    value_int = Math.abs(Integer.parseInt(value_str) + step) % 101;
                map.put("sub_value", String.valueOf(value_int));
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
            }
                break;
            case 1:
            {
                value_str = map.get("sub_value");
                if (-1 == step && value_str.equals("0"))
                    value_int = 100;
                else
                    value_int = Math.abs(Integer.parseInt(value_str) + step) % 101;
                map.put("sub_value", String.valueOf(value_int));
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
            }
                break;
            case 2:
            {
                value_str = map.get("sub_value");
                if (-1 == step && value_str.equals("0"))
                    value_int = 100;
                else
                    value_int = Math.abs(Integer.parseInt(value_str) + step) % 101;
                map.put("sub_value", String.valueOf(value_int));
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
            }
                break;
            case 3:
            {
                value_str = map.get("sub_value");
                if (-1 == step && value_str.equals("0"))
                    value_int = 100;
                else
                    value_int = Math.abs(Integer.parseInt(value_str) + step) % 101;
                map.put("sub_value", String.valueOf(value_int));
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
            }
                break;
            case 4:
            {
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

    public void cvbsSetValue(boolean left, int step)
    {
        String value_str = "";
        int value_int = 0;
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        switch (FactoryMainActivity.mSubMenuPos)
        {
            case 0:
            {
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

    public void hdcpSetValue()
    {
        String value_str = "";
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        switch (FactoryMainActivity.mSubMenuPos)
        {
            case 0:
                value_str = map.get("sub_value");
                if (value_str.equalsIgnoreCase(context.getString(Constant.FACUI_HDCP_DEMOKEY_ON)))
                {
                    map.put("sub_value", context.getString(Constant.FACUI_HDCP_DEMOKEY_OFF));
                    tv.SSMSaveUsingDefaultHDCPKeyFlag(0);
                }
                else if (value_str.equalsIgnoreCase(context.getString(Constant.FACUI_HDCP_DEMOKEY_OFF)))
                {
                    map.put("sub_value", context.getString(Constant.FACUI_HDCP_DEMOKEY_ON));
                    tv.SSMSaveUsingDefaultHDCPKeyFlag(1);
                }
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
    }

    public void serialSetValue()
    {
        String value_str = "";
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        switch (FactoryMainActivity.mSubMenuPos)
        {
            case 0:
                value_str = map.get("sub_value");
                if (value_str.equalsIgnoreCase(context.getString(Constant.FACUI_SERIAL_CMD_SWITCH_ON)))
                {
                    map.put("sub_value", context.getString(Constant.FACUI_SERIAL_CMD_SWITCH_OFF));
                    tv.SSMSaveSerialCMDSwitchValue(0);
                }
                else if (value_str.equalsIgnoreCase(context.getString(Constant.FACUI_SERIAL_CMD_SWITCH_OFF)))
                {
                    map.put("sub_value", context.getString(Constant.FACUI_SERIAL_CMD_SWITCH_ON));
                    tv.SSMSaveSerialCMDSwitchValue(1);
                }
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
    }

    public void adbSetValue()
    {
        String value_str = "";
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        switch (FactoryMainActivity.mSubMenuPos)
        {
            case 0:
                value_str = map.get("sub_value");
                if (value_str.equalsIgnoreCase(context.getString(Constant.FACUI_ADB_SWITCH_ON)))
                {
                    map.put("sub_value", context.getString(Constant.FACUI_ADB_SWITCH_OFF));
                    tv.FactorySetAdbdSwitch(0);
                }
                else if (value_str.equalsIgnoreCase(context.getString(Constant.FACUI_ADB_SWITCH_OFF)))
                {
                    map.put("sub_value", context.getString(Constant.FACUI_ADB_SWITCH_ON));
                    tv.FactorySetAdbdSwitch(1);
                }
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
    }

    public void autosavelogSetValue()
    {
        String value_str = "";
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        Intent logintent = new Intent();
        logintent.setAction("com.amlogic.action.savelogtoudisk");
        switch (FactoryMainActivity.mSubMenuPos)
        {
            case 0:
                value_str = map.get("sub_value");
                if (value_str.equalsIgnoreCase(context.getString(Constant.FACUI_AUTOSAVELOG_SWITCH_ON)))
                {
                    context.stopService(logintent);
                    map.put("sub_value", context.getString(Constant.FACUI_AUTOSAVELOG_SWITCH_OFF));
                }
                else if (value_str.equalsIgnoreCase(context.getString(Constant.FACUI_AUTOSAVELOG_SWITCH_OFF)))
                {
                    context.startService(logintent);
                    map.put("sub_value", context.getString(Constant.FACUI_AUTOSAVELOG_SWITCH_ON));
                }
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
    }

    public void dynamicbacklightSetValue(boolean left, int step)
    {
        int value_int = 0;
        Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        switch (FactoryMainActivity.mSubMenuPos)
        {
            case 0:
                String value_str = map.get("sub_value");
                if (-1 == step && value_str.equals("0"))
                    value_int = 100;
                else
                    value_int = Math.abs(Integer.parseInt(value_str) + step) % 101;
                map.put("sub_value", String.valueOf(value_int));
                SystemProperties.set("persist.tv.auto_bl_value", value_int + "");
                FactoryMainActivity.sAdapter.notifyDataSetChanged();
                break;
            default:
                break;
        }

    }

}

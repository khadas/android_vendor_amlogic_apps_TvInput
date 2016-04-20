/**
 * util used to show son item content
 *
 * @author ice.fu
 */
package com.amlogic.factory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.droidlogic.app.tv.TvControlManager;
import com.droidlogic.app.tv.TvControlManager.NOLINE_PARAMS_TYPE;
import com.droidlogic.app.tv.TvControlManager.SourceInput_Type;
import com.droidlogic.app.tv.TvControlManager.noline_params_t;
import com.droidlogic.app.tv.TvControlManager.tvin_cutwin_t;
import com.droidlogic.app.tv.TVInSignalInfo;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;

public class ShowSubView {
    public static TvControlManager tv = TvControlManager.getInstance();
    /* son ListView's item save to this ArrayList */
    public List<Map<String, String>> mListSubMenuData = null;
    private Context context;
    ActivityManager am;// used to adjust if the auto save log to udisk service is open

    public ShowSubView(List<Map<String, String>> mListSubMenuData, Context context) {
        this.mListSubMenuData = mListSubMenuData;
        this.context = context;
    }

    /* ADC calibration */
    public void show_calibate_submenu() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_ADC_PORT));
        map.put("sub_value", context.getString(Constant.FACUI_ADC_PORT_RGB));
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_ADC_AUTO));
        map.put("sub_value", "");
        mListSubMenuData.add(map);
        FactoryMainActivity.mPage = Constant.PAGE_CALIBRATE;
    }

    /* picture mode */
    public void show_picture_submenu() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_PICMODE_PORT));
        map.put("sub_value", context.getString(Constant.FACUI_PICMODE_PORT_TV));
        mListSubMenuData.add(map);
        setPicture(SourceInput_Type.SOURCE_TYPE_TV);
        FactoryMainActivity.mPage = Constant.PAGE_PICTUREMODE;
    }

    /* set picture mode's parameter at one source */
    public void setPicture(SourceInput_Type source) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_PICMODE_MODE));
        int itemPosition = tv.GetPQMode(source);
        map.put("sub_value", context.getString(Constant.mShowModeList[itemPosition]));
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_PICMODE_BRIGHTNESS));
        int value = tv.GetBrightness(source);
        map.put("sub_value", value + "");
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_PICMODE_CONTRAST));
        value = tv.GetContrast(source);
        map.put("sub_value", value + "");
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_PICMODE_COLOR));
        value = tv.GetSaturation(source);
        map.put("sub_value", value + "");
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_PICMODE_DEFINITION));
        value = tv.GetSharpness(source);
        map.put("sub_value", value + "");
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_PICMODE_TONE));
        value = tv.GetHue(source);
        map.put("sub_value", value + "");
        mListSubMenuData.add(map);
    }

    /* white balance */
    public void show_whitebalance_submenu() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_WHTBLAN_PORT));
        map.put("sub_value", context.getString(Constant.FACUI_WHTBLAN_PORT_TV));
        mListSubMenuData.add(map);
        setWhite(SourceInput_Type.SOURCE_TYPE_TV.toInt(), 0);
        FactoryMainActivity.mPage = Constant.PAGE_WHITEBALANCE;
    }

    /* set white balance's parameter at one source */
    public void setWhite(int source, int colortemp_mode) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_WHTBLAN_MODE));
        map.put("sub_value", context.getString(Constant.FACUI_WHTBLAN_MODE_STANDARD));
        mListSubMenuData.add(map);
        setWhite2(source, colortemp_mode);
    }

    /* set white balance's parameter at one source and mode */
    public void setWhite2(int source, int colortemp_mode) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_WHTBLAN_GAIN_R));
        int i = tv.FactoryWhiteBalanceGetRedGain(source, colortemp_mode);
        map.put("sub_value", "" + i);
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_WHTBLAN_GAIN_G));
        i = tv.FactoryWhiteBalanceGetGreenGain(source, colortemp_mode);
        map.put("sub_value", "" + i);
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_WHTBLAN_GAIN_B));
        i = tv.FactoryWhiteBalanceGetBlueGain(source, colortemp_mode);
        map.put("sub_value", "" + i);
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_WHTBLAN_OFFSET_R));
        i = tv.FactoryWhiteBalanceGetRedOffset(source, colortemp_mode);
        map.put("sub_value", "" + i);
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_WHTBLAN_OFFSET_G));
        i = tv.FactoryWhiteBalanceGetGreenOffset(source, colortemp_mode);
        map.put("sub_value", "" + i);
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_WHTBLAN_OFFSET_B));
        i = tv.FactoryWhiteBalanceGetBlueOffset(source, colortemp_mode);
        map.put("sub_value", "" + i);
        mListSubMenuData.add(map);
    }

    /*SSC*/
    public void show_ssc_submenu() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_LVDS_LVDS));
        int i = tv.FactoryGetLVDSSSC();
        map.put("sub_value", i + "");
        mListSubMenuData.add(map);
        FactoryMainActivity.mPage = Constant.PAGE_SSC;
    }

    // /* NOliner */
    // public void show_nolinear_submenu()
    // {
    // Map<String, String> map = new HashMap<String, String>();
    // map.put("sub_name", context.getString(Constant.FACUI_NOLINEAR_PORT));
    // map.put("sub_value", context.getString(Constant.FACUI_PICMODE_PORT_TV));
    // mListSubMenuData.add(map);
    // setNoLine(SourceInput_Type.SOURCE_TYPE_TV);
    // FactoryMainActivity.mPage = Constant.PAGE_NOLINEAR;
    // }

    /* set NOliner's parameter like brightness ect.*/
    public void setNoLine(SourceInput_Type source) {
        noline_params_t noline_params;
        Map<String, String> map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_NOLINEAR_BRIGHTNESS));
        noline_params = tv.FactoryGetNolineParams(NOLINE_PARAMS_TYPE.NOLINE_PARAMS_TYPE_BRIGHTNESS, source);
        map.put("sub_value", noline_params.osd0 + "     " + noline_params.osd25 + "     " + noline_params.osd50 + "     " + noline_params.osd75
                + "     " + noline_params.osd100);
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_NOLINEAR_CONTRAST));
        noline_params = tv.FactoryGetNolineParams(NOLINE_PARAMS_TYPE.NOLINE_PARAMS_TYPE_CONTRAST, source);
        map.put("sub_value", noline_params.osd0 + "     " + noline_params.osd25 + "     " + noline_params.osd50 + "     " + noline_params.osd75
                + "     " + noline_params.osd100);
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_NOLINEAR_SATURATION));
        noline_params = tv.FactoryGetNolineParams(NOLINE_PARAMS_TYPE.NOLINE_PARAMS_TYPE_SATURATION, source);
        map.put("sub_value", noline_params.osd0 + "     " + noline_params.osd25 + "     " + noline_params.osd50 + "     " + noline_params.osd75
                + "     " + noline_params.osd100);
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_NOLINEAR_DEFINITION));
        noline_params = tv.FactoryGetNolineParams(NOLINE_PARAMS_TYPE.NOLINE_PARAMS_TYPE_HUE, source);
        map.put("sub_value", noline_params.osd0 + "     " + noline_params.osd25 + "     " + noline_params.osd50 + "     " + noline_params.osd75
                + "     " + noline_params.osd100);
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_NOLINEAR_TONE));
        noline_params = tv.FactoryGetNolineParams(NOLINE_PARAMS_TYPE.NOLINE_PARAMS_TYPE_SHARPNESS, source);
        map.put("sub_value", noline_params.osd0 + "     " + noline_params.osd25 + "     " + noline_params.osd50 + "     " + noline_params.osd75
                + "     " + noline_params.osd100);
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_NOLINEAR_VOLUMN));
        noline_params = tv.FactoryGetNolineParams(NOLINE_PARAMS_TYPE.NOLINE_PARAMS_TYPE_VOLUME, source);
        map.put("sub_value", noline_params.osd0 + "     " + noline_params.osd25 + "     " + noline_params.osd50 + "     " + noline_params.osd75
                + "     " + noline_params.osd100);
        mListSubMenuData.add(map);
    }

    /* reShow rate */
    public void show_reshow_submenu() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_CHONGXIAN_PORT));
        map.put("sub_value", context.getString(Constant.FACUI_PICMODE_PORT_TV));
        mListSubMenuData.add(map);
        setTiming(SourceInput_Type.SOURCE_TYPE_TV, TVInSignalInfo.SignalFmt.TVIN_SIG_FMT_CVBS_NTSC_M,
                  TVInSignalInfo.TransFmt.TVIN_TFMT_2D);
        FactoryMainActivity.mPage = Constant.PAGE_OVERSCAN;
    }

    /* set reShow rate's all parameter */
    public void setTiming(SourceInput_Type source, TVInSignalInfo.SignalFmt fmt, TVInSignalInfo.TransFmt trans_fmt) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_CHONGXIAN_TIMING));
        map.put("sub_value", fmt + "");
        mListSubMenuData.add(map);
        //map = new HashMap<String, String>();
        //map.put("sub_name", context.getString(Constant.FACUI_CHONGXIAN_TVIN_3D_STATUS));
        //map.put("sub_value", status_3d + "");
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_CHONGXIAN_TVIN_TRANS_FMT));
        map.put("sub_value", trans_fmt + "");
        mListSubMenuData.add(map);
        setElse(source, fmt, trans_fmt);
    }

    /* set reShow rate's parameter except Timing,TVIN_3D_STATUS,TVIN_TRANS_FMT */
    public void setElse(SourceInput_Type source, TVInSignalInfo.SignalFmt fmt, TVInSignalInfo.TransFmt trans_fmt) {
        tvin_cutwin_t cutwin_t;
        cutwin_t = tv.FactoryGetOverscanParams(source, fmt, trans_fmt);
        Map<String, String> map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_CHONGXIAN_HSTART));
        map.put("sub_value", cutwin_t.hs + "");
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_CHONGXIAN_VSTART));
        map.put("sub_value", cutwin_t.vs + "");
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_CHONGXIAN_HPOS));
        map.put("sub_value", cutwin_t.he + "");
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_CHONGXIAN_VPOS));
        map.put("sub_value", cutwin_t.ve + "");
        mListSubMenuData.add(map);
    }

    /*testing picture*/
    public void show_ceshitu_submenu() {
        Map<String, String> map = new HashMap<String, String>();
        int i = tv.FactoryGetTestPattern();
        if (i > 5)
            i = 0;
        map.put("sub_value", context.getString(Constant.CESHITUARRAY_STRINGS[i]));
        mListSubMenuData.add(map);
        FactoryMainActivity.mPage = Constant.PAGE_TESTPATTERN;
    }

    /*aging mode*/
    public void show_agingmode_submenu() {
        Map<String, String> map = new HashMap<String, String>();
        int i = tv.SSMReadAgingMode();
        map.put("sub_name", "");
        map.put("sub_value", i == 0 ? context.getString(Constant.FACUI_LAOHUA_OFF) : context.getString(Constant.FACUI_LAOHUA_ON));
        mListSubMenuData.add(map);
        FactoryMainActivity.mPage = Constant.PAGE_AGINGMODE;
    }

    /*software info*/
    public void show_softinfo_submenu() {
        Map<String, String> map = new HashMap<String, String>();
        String sub_value = "";
        TvControlManager.version_info tmpInfo = tv.TvMiscGetVersion();
        map.put("soft_name", context.getString(Constant.FACUI_SOFTINFO_NUMBER));
        map.put("soft_value", SystemProperties.get("ro.product.model", "NONE") + " " + SystemProperties.get("ro.build.version.time", "NONE"));
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("soft_name", context.getString(Constant.FACUI_SOFTINFO_BOOTVERSION));
        sub_value = tmpInfo.ubootVer;
        map.put("soft_value", sub_value);
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("soft_name", context.getString(Constant.FACUI_SOFTINFO_KERNELVERSION));
        sub_value = tmpInfo.kernel_ver.linux_ver_info + " " + tmpInfo.kernel_ver.build_usr_info + " " + tmpInfo.kernel_ver.build_time_info;
        map.put("soft_value", sub_value);
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("soft_name", "");
        sub_value = tmpInfo.tvapi_ver.git_branch_info;
        map.put("soft_value", sub_value);
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("soft_name", context.getString(Constant.FACUI_SOFTINFO_TVVERSION));
        sub_value = tmpInfo.tvapi_ver.git_commit_info;
        map.put("soft_value", sub_value);
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("soft_name", "");
        sub_value = tmpInfo.tvapi_ver.last_change_time_info;
        map.put("soft_value", sub_value);
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("soft_name", "");
        sub_value = tmpInfo.dvb_ver.git_branch_info;
        map.put("soft_value", sub_value);
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("soft_name", context.getString(Constant.FACUI_SOFTINFO_DVBVERSION));
        sub_value = tmpInfo.dvb_ver.git_commit_info;
        map.put("soft_value", sub_value);
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("soft_name", "");
        sub_value = tmpInfo.dvb_ver.last_change_time_info;
        map.put("soft_value", sub_value);
        mListSubMenuData.add(map);
        FactoryMainActivity.mPage = Constant.PAGE_SOFTINFO;
    }

    /*HDMI HDCP DemoKey*/
    public void show_hdcp_submenu() {
        Map<String, String> map = new HashMap<String, String>();
        int i = tv.SSMReadUsingDefaultHDCPKeyFlag();
        if (i == 0)
            map.put("sub_value", context.getString(Constant.FACUI_HDCP_DEMOKEY_OFF));
        else
            map.put("sub_value", context.getString(Constant.FACUI_HDCP_DEMOKEY_ON));
        mListSubMenuData.add(map);
        FactoryMainActivity.mPage = Constant.HDMI_HDCP_DEMOKEY;
    }

    public void show_upgrade_fbc() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("sub_value", context.getString(Constant.FACUI_START));
        mListSubMenuData.add(map);
        //FactoryMainActivity.mPage = Constant.PAGE_UPGRADE_FBC;
    }

    /*fbc version info*/
    public void show_fbc_version() {
        Map<String, String> map = null;
        String sub_value = "";

        TvControlManager.FBC_MAINCODE_INFO tmpInfo = tv.FactoryGet_FBC_Get_MainCode_Version();

        map = new HashMap<String, String>();
        map.put("soft_name", "");
        sub_value = tmpInfo.Version;
        map.put("soft_value", sub_value);
        mListSubMenuData.add(map);

        map = new HashMap<String, String>();
        map.put("soft_name", "");
        sub_value = tmpInfo.GitBranch;
        map.put("soft_value", sub_value);
        mListSubMenuData.add(map);

        map = new HashMap<String, String>();
        map.put("soft_name", context.getString(Constant.FACUI_SOFTINFO_FBCVERSION));
        sub_value = tmpInfo.GitVersion;
        map.put("soft_value", sub_value);
        mListSubMenuData.add(map);

        map = new HashMap<String, String>();
        map.put("soft_name", "");
        sub_value = tmpInfo.LastBuild;
        map.put("soft_value", sub_value);
        mListSubMenuData.add(map);

        map = new HashMap<String, String>();
        map.put("soft_name", "");
        sub_value = tv.FactoryGet_FBC_SN_Info();
        map.put("soft_value", sub_value);
        mListSubMenuData.add(map);

        //FactoryMainActivity.mPage = Constant.PAGE_FBC_VERSION;
    }

    /*Serial cmd switch:com debug tool's switch，when open, the com debug tool can't work*/
    public void show_serial_submenu() {
        Map<String, String> map = new HashMap<String, String>();
        int i = tv.SSMReadSerialCMDSwitchValue();
        if (i == 0)
            map.put("sub_value", context.getString(Constant.FACUI_HDCP_DEMOKEY_OFF));
        else if (i == 1)
            map.put("sub_value", context.getString(Constant.FACUI_HDCP_DEMOKEY_ON));
        mListSubMenuData.add(map);
        FactoryMainActivity.mPage = Constant.PAGE_SERIAL_CMD_SWITCH;
    }

    public void show_portprint_submenu() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_PORTPRINT_SWITCH_OFF));
        map.put("sub_value", "");
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_PORTPRINT_SWITCH_ON));
        map.put("sub_value", "");
        mListSubMenuData.add(map);
        FactoryMainActivity.mPage = Constant.PAGE_PORTPRINT_SWITCH;
    }

    public void show_remotecontrol() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_REMOTECONTROL_HAIER));
        map.put("sub_value", "");
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_REMOTECONTROL_HAIERMTC));
        map.put("sub_value", "");
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_REMOTECONTROL_HAIERCVT));
        map.put("sub_value", "");
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", context.getString(Constant.FACUI_REMOTECONTROL_HAIERAML));
        map.put("sub_value", "");
        mListSubMenuData.add(map);
        //FactoryMainActivity.mPage = Constant.PAGE_REMOTECONTROL;
    }

    /* auto save log's switch:when open, the function of auto save log to U disk is on */
    public void show_autosavelog_submenu() {
        Map<String, String> map = new HashMap<String, String>();
        int i = 0;
        am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> infos = am.getRunningAppProcesses();
        for (RunningAppProcessInfo rapi : infos) {
            if (rapi.processName.equals("com.amlogic.logrecord"))
                i = 1;
        }
        if (i == 0)
            map.put("sub_value", context.getString(Constant.FACUI_AUTOSAVELOG_SWITCH_OFF));
        else if (i == 1)
            map.put("sub_value", context.getString(Constant.FACUI_AUTOSAVELOG_SWITCH_ON));
        mListSubMenuData.add(map);
        //FactoryMainActivity.mPage = Constant.PAGE_AUTOSAVELOG_SWITCH;
    }

    public void show_dynamicbacklight_submenu() {
        Map<String, String> map = new HashMap<String, String>();
        String dynamicbacklightvalue = SystemProperties.get("persist.tv.auto_bl_value", "0");
        map.put("sub_name", "");
        map.put("sub_value", dynamicbacklightvalue);
        mListSubMenuData.add(map);
        FactoryMainActivity.mPage = Constant.PAGE_DYNAMIC_BACKLIGHT_VALUE;
    }

    public void show_screen_submenu() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("sub_name", "");
        map.put("sub_value", context.getString(Constant.FACUI_SCREEN_UP));
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", "");
        map.put("sub_value", context.getString(Constant.FACUI_SCREEN_DOWN));
        mListSubMenuData.add(map);
        FactoryMainActivity.mPage = Constant.PAGE_SCREEN;
    }

    public void show_outputmode() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("sub_name", "");
        map.put("sub_value", context.getString(Constant.FACUI_OUTPUT_MODE1));
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", "");
        map.put("sub_value", context.getString(Constant.FACUI_OUTPUT_MODE2));
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", "");
        map.put("sub_value", context.getString(Constant.FACUI_OUTPUT_MODE3));
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", "");
        map.put("sub_value", context.getString(Constant.FACUI_OUTPUT_MODE4));
        mListSubMenuData.add(map);
        map = new HashMap<String, String>();
        map.put("sub_name", "");
        map.put("sub_value", context.getString(Constant.FACUI_OUTPUT_MODE5));
        mListSubMenuData.add(map);
    }
}

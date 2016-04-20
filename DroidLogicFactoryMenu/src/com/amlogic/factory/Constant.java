/**
 * some constant used in factory menu
 *
 * @author ice.fu
 */
package com.amlogic.factory;

import android.R.integer;

public class Constant {
    /* constant used to identify father ListView's content when change son ListView's content */
    public static final int PAGE_CALIBRATE = 0;
    public static final int PAGE_PICTUREMODE = 1;
    public static final int PAGE_WHITEBALANCE = 2;
    public static final int PAGE_SSC = 3;
    // public static final int PAGE_NOLINEAR = 4;
    public static final int PAGE_OVERSCAN = 4;
    public static final int PAGE_TESTPATTERN = 5;
    public static final int PAGE_AGINGMODE = 6;
    public static final int PAGE_FACTORYCHANNEL = 7;
    public static final int PAGE_FACRESET = 8;
    public static final int PAGE_SOFTINFO = 9;
    public static final int HDMI_HDCP_DEMOKEY = 10;
    public static final int PAGE_WRITE_KEY = 11;
    //public static final int PAGE_UPGRADE_FBC = -1;
    //public static final int PAGE_FBC_VERSION = -2;
    public static final int PAGE_SERIAL_CMD_SWITCH = 12;
    public static final int PAGE_PORTPRINT_SWITCH = 13;
    //public static final int PAGE_REMOTECONTROL = 14;
    //public static final int PAGE_ADB_SWITCH = 15;
    public static final int PAGE_UPGRADE = 14;
    //public static final int PAGE_SETDEMOPARAM = 17;
    //public static final int PAGE_AUTOSAVELOG_SWITCH = 18;
    public static final int PAGE_DYNAMIC_BACKLIGHT_VALUE = 15;
    public static final int PAGE_SCREEN = 16;
    //public static final int PAGE_OUTPUTMODE = 21;

    public static String CS_UPGRADE_FBC_FILE_NAME = "/storage/external_storage/udisk0/spi.bin";
    public static final int FACUI_START = R.string.Start;

    public static final int FACUI_ADC = R.string.facui_adc;
    public static final int FACUI_ADC_PORT = R.string.facui_adc_port;
    public static final int FACUI_ADC_PORT_RGB = R.string.facui_adc_port_rgb;
    public static final int FACUI_ADC_PORT_YPBPR = R.string.facui_adc_port_ypbpr;
    public static final int FACUI_ADC_AUTO = R.string.facui_adc_auto;

    public static final int FACUI_PICMODE = R.string.facui_picmode;
    public static final int FACUI_PICMODE_PORT = R.string.facui_picmode_port;
    public static final int FACUI_PICMODE_PORT_TV = R.string.facui_picmode_port_tv;
    public static final int FACUI_PICMODE_PORT_AV = R.string.facui_picmode_port_av;
    public static final int FACUI_PICMODE_PORT_YPBPR = R.string.facui_picmode_port_ypbpr;
    public static final int FACUI_PICMODE_PORT_HDMI = R.string.facui_picmode_port_hdmi;
    public static final int FACUI_PICMODE_PORT_VGA = R.string.facui_picmode_port_vga;
    public static final int FACUI_PICMODE_PORT_MPEG = R.string.facui_picmode_port_mpeg;
    public static final int FACUI_PICMODE_PORT_DTV = R.string.facui_picmode_port_dtv;
    /*picture mode mode's int[]*/
    public static final int[] mInputPortList = {FACUI_PICMODE_PORT_TV, FACUI_PICMODE_PORT_AV, FACUI_PICMODE_PORT_YPBPR, FACUI_PICMODE_PORT_HDMI,
                                                FACUI_PICMODE_PORT_VGA, FACUI_PICMODE_PORT_MPEG, FACUI_PICMODE_PORT_DTV
                                               };
    public static final int FACUI_PICMODE_MODE = R.string.facui_picmode_mode;
    public static final int FACUI_PICMODE_MODE_STANDARD = R.string.facui_picmode_mode_standard;
    public static final int FACUI_PICMODE_MODE_LIGHT = R.string.facui_picmode_mode_light;
    public static final int FACUI_PICMODE_MODE_SOFT = R.string.facui_picmode_mode_soft;
    public static final int FACUI_PICMODE_MODE_USER = R.string.facui_picmode_mode_user;
    /*picture mode mode's int[]*/
    public static final int[] mShowModeList = {FACUI_PICMODE_MODE_STANDARD, FACUI_PICMODE_MODE_LIGHT, FACUI_PICMODE_MODE_SOFT,
                                               FACUI_PICMODE_MODE_USER
                                              };
    public static final int FACUI_PICMODE_BRIGHTNESS = R.string.facui_picmode_brightness;
    public static final int FACUI_PICMODE_CONTRAST = R.string.facui_picmode_contrast;
    public static final int FACUI_PICMODE_COLOR = R.string.facui_picmode_color;
    public static final int FACUI_PICMODE_DEFINITION = R.string.facui_picmode_definition;
    public static final int FACUI_PICMODE_TONE = R.string.facui_picmode_tone;
    public static final int FACUI_PICMODE_RESTORATION = R.string.facui_picmode_restoration;

    public static final int FACUI_WHTBLAN = R.string.facui_whtblan;
    public static final int FACUI_WHTBLAN_PORT = R.string.facui_whtblan_port;
    public static final int FACUI_WHTBLAN_PORT_TV = R.string.facui_whtblan_port_atv;
    public static final int FACUI_WHTBLAN_PORT_AV = R.string.facui_whtblan_port_av;
    public static final int FACUI_WHTBLAN_PORT_YPBPR = R.string.facui_whtblan_port_ypbpr;
    public static final int FACUI_WHTBLAN_PORT_HDMI = R.string.facui_whtblan_port_hdmi;
    public static final int FACUI_WHTBLAN_PORT_MPEG = R.string.facui_whtblan_port_media;
    public static final int FACUI_WHTBLAN_MODE = R.string.facui_whtblan_mode;
    public static final int FACUI_WHTBLAN_MODE_STANDARD = R.string.facui_whtblan_mode_standard;
    public static final int FACUI_WHTBLAN_MODE_COOL = R.string.facui_whtblan_mode_cool;
    public static final int FACUI_WHTBLAN_MODE_WARM = R.string.facui_whtblan_mode_warm;
    public static final int FACUI_WHTBLAN_MODE_USER = R.string.facui_whtblan_mode_user;
    public static final int FACUI_WHTBLAN_MODE_MAX = R.string.facui_whtblan_mode_max;
    public static final int[] mColorTemperatureList = {FACUI_WHTBLAN_MODE_STANDARD, FACUI_WHTBLAN_MODE_WARM, FACUI_WHTBLAN_MODE_COOL};
    public static final int FACUI_WHTBLAN_GAIN_R = R.string.facui_whtblan_gain_r;
    public static final int FACUI_WHTBLAN_GAIN_G = R.string.facui_whtblan_gain_g;
    public static final int FACUI_WHTBLAN_GAIN_B = R.string.facui_whtblan_gain_b;
    public static final int FACUI_WHTBLAN_OFFSET_R = R.string.facui_whtblan_offset_r;
    public static final int FACUI_WHTBLAN_OFFSET_G = R.string.facui_whtblan_offset_g;
    public static final int FACUI_WHTBLAN_OFFSET_B = R.string.facui_whtblan_offset_b;
    public static final int FACUI_WHTBLAN_FBC_MODE = R.string.facui_fbcwhtblan_mode;
    public static final int[] mFbcColorTemperatureList = {FACUI_WHTBLAN_MODE_COOL, FACUI_WHTBLAN_MODE_STANDARD, FACUI_WHTBLAN_MODE_WARM, FACUI_WHTBLAN_MODE_USER, FACUI_WHTBLAN_MODE_MAX};
    public static final int FACUI_WHTBLAN_FBC_INIT = R.string.facui_fbcwhtblan_init;
    public static final int FACUI_WHTBLAN_FBC_GAIN_R = R.string.facui_fbcwhtblan_gain_r;
    public static final int FACUI_WHTBLAN_FBC_GAIN_G = R.string.facui_fbcwhtblan_gain_g;
    public static final int FACUI_WHTBLAN_FBC_GAIN_B = R.string.facui_fbcwhtblan_gain_b;
    public static final int FACUI_WHTBLAN_FBC_OFFSET_R = R.string.facui_fbcwhtblan_offset_r;
    public static final int FACUI_WHTBLAN_FBC_OFFSET_G = R.string.facui_fbcwhtblan_offset_g;
    public static final int FACUI_WHTBLAN_FBC_OFFSET_B = R.string.facui_fbcwhtblan_offset_b;

    public static final int FACUI_SSC = R.string.facui_ssc;
    public static final int FACUI_LVDS_LVDS = R.string.facui_ssc_lvds;

    public static final int FACUI_NOLINEAR = R.string.facui_nolinear;
    public static final int FACUI_NOLINEAR_PORT = R.string.facui_nolinear_port;
    public static final int FACUI_NOLINEAR_BRIGHTNESS = R.string.facui_nolinear_brightness;
    public static final int FACUI_NOLINEAR_CONTRAST = R.string.facui_nolinear_contrast;
    public static final int FACUI_NOLINEAR_SATURATION = R.string.facui_nolinear_saturation;
    public static final int FACUI_NOLINEAR_DEFINITION = R.string.facui_nolinear_definition;
    public static final int FACUI_NOLINEAR_TONE = R.string.facui_nolinear_tone;
    public static final int FACUI_NOLINEAR_VOLUMN = R.string.facui_nolinear_volumn;
    public static final int FACUI_NOLINEAR_OSD0 = R.string.facui_nolinear_osd0;
    public static final int FACUI_NOLINEAR_OSD25 = R.string.facui_nolinear_osd25;
    public static final int FACUI_NOLINEAR_OSD50 = R.string.facui_nolinear_osd50;
    public static final int FACUI_NOLINEAR_OSD75 = R.string.facui_nolinear_osd75;
    public static final int FACUI_NOLINEAR_OSD100 = R.string.facui_nolinear_osd100;

    public static final int FACUI_CHONGXIAN = R.string.facui_chongxian;
    public static final int FACUI_CHONGXIAN_PORT = R.string.facui_chongxian_port;
    public static final int FACUI_CHONGXIAN_TIMING = R.string.facui_chongxian_timing;
    public static final int FACUI_CHONGXIAN_TVIN_3D_STATUS = R.string.facui_chongxian_tvin_3d_status;
    public static final int FACUI_CHONGXIAN_STATUS3D_DISABLE = R.string.facui_chongxian_status3d_disable;
    public static final int FACUI_CHONGXIAN_STATUS3D_AUTO = R.string.facui_chongxian_status3d_auto;
    public static final int FACUI_CHONGXIAN_STATUS3D_2D_TO_3D = R.string.facui_chongxian_status3d_2d_to_3d;
    public static final int FACUI_CHONGXIAN_STATUS3D_LR = R.string.facui_chongxian_status3d_lr;
    public static final int FACUI_CHONGXIAN_STATUS3D_BT = R.string.facui_chongxian_status3d_bt;
    public static final int FACUI_CHONGXIAN_STATUS3D_LINE_ALTERNATIVE = R.string.facui_chongxian_status3d_line_alternative;
    public static final int FACUI_CHONGXIAN_STATUS3D_FRAME_ALTERNATIVE = R.string.facui_chongxian_status3d_frame_alternative;
    public static final int FACUI_CHONGXIAN_STATUS3D_MAX = R.string.facui_chongxian_status3d_max;
    public static final int[] mChongxianTvIn3Dstatus = {FACUI_CHONGXIAN_STATUS3D_DISABLE, FACUI_CHONGXIAN_STATUS3D_AUTO,
                                                        FACUI_CHONGXIAN_STATUS3D_2D_TO_3D, FACUI_CHONGXIAN_STATUS3D_LR, FACUI_CHONGXIAN_STATUS3D_BT, FACUI_CHONGXIAN_STATUS3D_LINE_ALTERNATIVE,
                                                        FACUI_CHONGXIAN_STATUS3D_FRAME_ALTERNATIVE, FACUI_CHONGXIAN_STATUS3D_MAX
                                                       };
    public static final int FACUI_CHONGXIAN_TVIN_TRANS_FMT = R.string.facui_chongxian_tvin_trans_fmt;
    public static final int FACUI_CHONGXIAN_TVIN_TFMT_2D = R.string.facui_chongxian_tvin_tfmt_2d;
    public static final int FACUI_CHONGXIAN_TVIN_TFMT_3D_LRH_OLOR = R.string.facui_chongxian_tvin_tfmt_3d_lrh_olor;
    public static final int FACUI_CHONGXIAN_TVIN_TFMT_3D_LRH_OLER = R.string.facui_chongxian_tvin_tfmt_3d_lrh_oler;
    public static final int FACUI_CHONGXIAN_TVIN_TFMT_3D_LRH_ELOR = R.string.facui_chongxian_tvin_tfmt_3d_lrh_elor;
    public static final int FACUI_CHONGXIAN_TVIN_TFMT_3D_LRH_ELER = R.string.facui_chongxian_tvin_tfmt_3d_lrh_eler;
    public static final int FACUI_CHONGXIAN_TVIN_TFMT_3D_TB = R.string.facui_chongxian_tvin_tfmt_3d_tb;
    public static final int FACUI_CHONGXIAN_TVIN_TFMT_3D_FP = R.string.facui_chongxian_tvin_tfmt_3d_fp;
    public static final int FACUI_CHONGXIAN_TVIN_TFMT_3D_FA = R.string.facui_chongxian_tvin_tfmt_3d_fa;
    public static final int FACUI_CHONGXIAN_TVIN_TFMT_3D_LA = R.string.facui_chongxian_tvin_tfmt_3d_la;
    public static final int FACUI_CHONGXIAN_TVIN_TFMT_3D_LRF = R.string.facui_chongxian_tvin_tfmt_3d_lrf;
    public static final int FACUI_CHONGXIAN_TVIN_TFMT_3D_LD = R.string.facui_chongxian_tvin_tfmt_3d_ld;
    public static final int FACUI_CHONGXIAN_TVIN_TFMT_3D_LDGD = R.string.facui_chongxian_tvin_tfmt_3d_ldgd;
    public static final int FACUI_CHONGXIAN_TVIN_TFMT_3D_DET_TB = R.string.facui_chongxian_tvin_tfmt_3d_det_tb;
    public static final int FACUI_CHONGXIAN_TVIN_TFMT_3D_DET_LR = R.string.facui_chongxian_tvin_tfmt_3d_det_lr;
    public static final int FACUI_CHONGXIAN_TVIN_TFMT_3D_DET_INTERLACE = R.string.facui_chongxian_tvin_tfmt_3d_det_interlace;
    public static final int FACUI_CHONGXIAN_TVIN_TFMT_3D_DET_CHESSBOARD = R.string.facui_chongxian_tvin_tfmt_3d_det_chessboard;
    public static final int FACUI_CHONGXIAN_TVIN_TFMT_3D_MAX = R.string.facui_chongxian_tvin_tfmt_3d_max;
    public static final int[] mChongxianTvInTransFmt = {FACUI_CHONGXIAN_TVIN_TFMT_2D, FACUI_CHONGXIAN_TVIN_TFMT_3D_LRH_OLOR,
                                                        FACUI_CHONGXIAN_TVIN_TFMT_3D_LRH_OLER, FACUI_CHONGXIAN_TVIN_TFMT_3D_LRH_ELOR, FACUI_CHONGXIAN_TVIN_TFMT_3D_LRH_ELER,
                                                        FACUI_CHONGXIAN_TVIN_TFMT_3D_TB, FACUI_CHONGXIAN_TVIN_TFMT_3D_FP, FACUI_CHONGXIAN_TVIN_TFMT_3D_FA, FACUI_CHONGXIAN_TVIN_TFMT_3D_LA,
                                                        FACUI_CHONGXIAN_TVIN_TFMT_3D_LRF, FACUI_CHONGXIAN_TVIN_TFMT_3D_LD, FACUI_CHONGXIAN_TVIN_TFMT_3D_LDGD,
                                                        FACUI_CHONGXIAN_TVIN_TFMT_3D_DET_TB, FACUI_CHONGXIAN_TVIN_TFMT_3D_DET_LR, FACUI_CHONGXIAN_TVIN_TFMT_3D_DET_INTERLACE,
                                                        FACUI_CHONGXIAN_TVIN_TFMT_3D_DET_CHESSBOARD, FACUI_CHONGXIAN_TVIN_TFMT_3D_MAX
                                                       };

    public static final int FACUI_CHONGXIAN_HSTART = R.string.facui_chongxian_hstart;
    public static final int FACUI_CHONGXIAN_VSTART = R.string.facui_chongxian_vstart;
    public static final int FACUI_CHONGXIAN_HPOS = R.string.facui_chongxian_hpos;
    public static final int FACUI_CHONGXIAN_VPOS = R.string.facui_chongxian_vpos;

    public static final int FACUI_GONGNENG_CESHITU = R.string.facui_gongneng_ceshitu;
    public static final int FACUI_GONGNENG_CESHITU_OFF = R.string.facui_gongneng_ceshitu_off;
    public static final int FACUI_GONGNENG_CESHITU_RED = R.string.facui_gongneng_ceshitu_red;
    public static final int FACUI_GONGNENG_CESHITU_GREEN = R.string.facui_gongneng_ceshitu_green;
    public static final int FACUI_GONGNENG_CESHITU_BLUE = R.string.facui_gongneng_ceshitu_blue;
    public static final int FACUI_GONGNENG_CESHITU_WHITE = R.string.facui_gongneng_ceshitu_white;
    public static final int FACUI_GONGNENG_CESHITU_BLACK = R.string.facui_gongneng_ceshitu_black;
    public static final int[] CESHITUARRAY_STRINGS = {FACUI_GONGNENG_CESHITU_OFF, FACUI_GONGNENG_CESHITU_RED, FACUI_GONGNENG_CESHITU_GREEN,
                                                      FACUI_GONGNENG_CESHITU_BLUE, FACUI_GONGNENG_CESHITU_WHITE, FACUI_GONGNENG_CESHITU_BLACK
                                                     };

    public static final int FACUI_LAOHUA = R.string.facui_laohua;
    public static final int FACUI_LAOHUA_ON = R.string.facui_laohua_on;
    public static final int FACUI_LAOHUA_OFF = R.string.facui_laohua_off;

    public static final int FACUI_PINDAOYUSHE = R.string.facui_pindaoyushe;

    public static final int FACUI_FACRESET = R.string.facui_facreset;

    public static final int FACUI_SOFTINFO = R.string.facui_softinfo;
    public static final int FACUI_SOFTINFO_ANDROIDVERSION = R.string.facui_softinfo_androidversion;
    public static final int FACUI_SOFTINFO_NUMBER = R.string.facui_softnum;
    public static final int FACUI_SOFTINFO_BOOTVERSION = R.string.facui_softinfo_bootversion;
    public static final int FACUI_SOFTINFO_KERNELVERSION = R.string.facui_softinfo_kernelversion;
    public static final int FACUI_SOFTINFO_TVVERSION = R.string.facui_softinfo_tvversion;
    public static final int FACUI_SOFTINFO_DVBVERSION = R.string.facui_softinfo_dvbversion;
    public static final int FACUI_SOFTINFO_FBCVERSION = R.string.facui_softinfo_fbcversion;

    public static final int FACUI_HDMI_HDCP_DEMOKEY = R.string.facui_hdcpkey;
    public static final int FACUI_HDCP = R.string.facui_hdcp;
    public static final int FACUI_HDMI = R.string.facui_hdmi;
    public static final int FACUI_HDCP_DEMOKEY_ON = R.string.facui_hdcp_demokey_on;
    public static final int FACUI_HDCP_DEMOKEY_OFF = R.string.facui_hdcp_demokey_off;
    public static final int FACUI_HDMI_DEMOKEY_ON = R.string.facui_hdmi_demokey_on;
    public static final int FACUI_HDMI_DEMOKEY_OFF = R.string.facui_hdmi_demokey_off;

    public static final int FACUI_UPGRADE_FBC = R.string.facui_upgrade_fbc;
    public static final int FACUI_FBC_VERSION = R.string.facui_fbc_version;

    public static final int FACUI_SERIAL_CMD_SWITCH = R.string.facui_cmdswitch;
    public static final int FACUI_SERIAL_CMD_SWITCH_ON = R.string.facui_serial_cmd_switch_on;
    public static final int FACUI_SERIAL_CMD_SWITCH_OFF = R.string.facui_serial_cmd_switch_off;

    public static final int FACUI_PORTPRINT_SWITCH = R.string.facui_portprint_switch;
    public static final int FACUI_PORTPRINT_SWITCH_ON = R.string.facui_portprint_on;
    public static final int FACUI_PORTPRINT_SWITCH_OFF = R.string.facui_portprint_off;

    public static final int FACUI_REMOTECONTROL = R.string.facui_remotecontrol;
    public static final int FACUI_REMOTECONTROL_HAIER = R.string.facui_remotecontrol_haier;
    public static final int FACUI_REMOTECONTROL_HAIERMTC = R.string.facui_remotecontrol_haiermtc;
    public static final int FACUI_REMOTECONTROL_HAIERCVT = R.string.facui_remotecontrol_haiercvt;
    public static final int FACUI_REMOTECONTROL_HAIERAML = R.string.facui_remotecontrol_haieraml;

    public static final int FACUI_ADB_SWITCH = R.string.facui_adbswitch;
    public static final int FACUI_ADB_SWITCH_ON = R.string.facui_adbswitch_on;
    public static final int FACUI_ADB_SWITCH_OFF = R.string.facui_adbswitch_off;

    public static final int FACUI_AUTOSAVELOG_SWITCH = R.string.facui_autosavelog;
    public static final int FACUI_AUTOSAVELOG_SWITCH_ON = R.string.facui_autosavelog_on;
    public static final int FACUI_AUTOSAVELOG_SWITCH_OFF = R.string.facui_autosavelog_off;

    public static final int FACUI_UPGRADE = R.string.facui_upgrade;

    public static final int FACUI_SETDEMOPARAM = R.string.facui_setdemoparam;

    public static final int FACUI_DYNAMIC_BACKLIGHT_VALUE = R.string.facui_dym_bl_value;

    public static final int FACUI_SCREEN = R.string.facui_screen;
    public static final int FACUI_SCREEN_UP = R.string.facui_screen_up;
    public static final int FACUI_SCREEN_DOWN = R.string.facui_screen_down;

    public static final int FACUI_WRITE_HDCP_MAC_KEY = R.string.facui_write_key;

    public static final int FACUI_OUTPUT_MODE = R.string.output_mode;
    public static final int FACUI_OUTPUT_MODE1 = R.string.output_mode1;
    public static final int FACUI_OUTPUT_MODE2 = R.string.output_mode2;
    public static final int FACUI_OUTPUT_MODE3 = R.string.output_mode3;
    public static final int FACUI_OUTPUT_MODE4 = R.string.output_mode4;
    public static final int FACUI_OUTPUT_MODE5 = R.string.output_mode5;
}

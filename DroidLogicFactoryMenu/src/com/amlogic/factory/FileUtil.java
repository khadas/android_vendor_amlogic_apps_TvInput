/**
 * operate file's class
 *
 * @author ice.fu
 */
package com.amlogic.factory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import android.content.Context;
import android.util.Log;

import com.droidlogic.app.tv.TvControlManager;

public class FileUtil {
    private static final String TAG = "WriteKey";
    private static final int MAC_LENTH = 6;
    private static final int HDCP_LENTH = 368;
    private static final String UDISK_PATH = getUdiskPath();
    private static final String MAC_DIR = "/storage/external_storage/" + UDISK_PATH + "/MAC";
    private static final String MAC_BANK_DIR = "/storage/external_storage/" + UDISK_PATH + "/MAC_BANK";
    private static final String HDCP_DIR = "/storage/external_storage/" + UDISK_PATH + "/HDCP";
    private static final String HDCP_BANK_DIR = "/storage/external_storage/" + UDISK_PATH + "/HDCP_BANK";
    private static File hdcpFile = null;
    private static File macFile = null;
    private static Runtime run = Runtime.getRuntime();
    static byte[] macKeyData = new byte[MAC_LENTH];
    static byte[] hdcpKeyData = new byte[HDCP_LENTH];

    static int[] data_1 = new int[MAC_LENTH];
    static int[] data_2 = new int[HDCP_LENTH];

    static final int HDCP_TYPE = 1;
    static final int MAC_TYPE = 2;

    public static String getUdiskPath() {
        File[] files = new File("/storage/external_storage").listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.getName().startsWith("sdcard")) {
                    return file.getName();
                }
            }
        }
        return "udisk0";
    }

    private static boolean ReadKey(String key_path, int type) {
        File keyFileDir = null;
        File[] keyFileList = null;
        keyFileDir = new File(key_path);
        if (keyFileDir.exists()) {
            keyFileList = keyFileDir.listFiles();
            if (keyFileList.length > 0) {
                File curFile = keyFileList[0];
                if (curFile.exists()) {
                    FileInputStream fStream = null;
                    if (type == MAC_TYPE) {
                        macFile = curFile;
                    } else if (type == HDCP_TYPE) {
                        hdcpFile = curFile;
                    }
                    try {
                        fStream = new FileInputStream(curFile);
                        if (fStream != null) {
                            try {
                                if (type == HDCP_TYPE) {
                                    fStream.read(hdcpKeyData, 0, hdcpKeyData.length);
                                } else {
                                    fStream.read(macKeyData, 0, macKeyData.length);
                                }
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                                Log.d(TAG, "=========== Read file error ! ===========");
                            }
                            return true;
                        }
                    } catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } finally {
                        if (fStream != null)
                            try {
                                fStream.close();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                    }
                }
                Log.d(TAG, "=========== key file not exit ===========");
            }
            Log.d(TAG, "=========== path " + key_path + "  not exit ===========");
            return false;
        } else {
            return false;
        }
    }

    private static int writeKey(TvControlManager tv, int[] buffer, int type) {
        if (type == HDCP_TYPE)
            return tv.SSMSaveHDCPKey(buffer);
        else
            return tv.SSMSaveMacAddress(buffer);
    }

    public static boolean moveFile(byte[] buffer, int type) {
        File bankDir = null;
        File newFile = null;
        File oldFile = null;
        String newFileName = null;
        FileOutputStream outStream = null;
        if (type == HDCP_TYPE) { //hdcp
            bankDir = new File(HDCP_BANK_DIR);
            if (hdcpFile.exists()) {
                newFileName = hdcpFile.getName();
                oldFile = hdcpFile;
            }
        } else { // mac
            bankDir = new File(MAC_BANK_DIR);
            if (macFile.exists()) {
                newFileName = macFile.getName();
                oldFile = macFile;
            }
        }

        if (!bankDir.exists()) {
            if (!bankDir.mkdir()) {
                Log.d(TAG, "=========== create bank directory failed ! ===========");
                return false;
            }
        }
        if (newFileName != null) {
            newFile = new File(bankDir + "/" + newFileName);
            Log.d(TAG, "----------- new file path is " + newFile.getPath() + " -------------");
            try {
                if (!newFile.createNewFile()) {
                    Log.d(TAG, "=========== create bank file failed ! ===========");
                    return false;
                }
                outStream = new FileOutputStream(newFile);
                outStream.write(buffer);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                Log.d(TAG, "=========== Write file error ! ===========");
                e.printStackTrace();

            } finally {
                if (outStream != null) {
                    try {
                        outStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

            if (oldFile.delete()) {
                if (type == HDCP_TYPE)
                    hdcpFile = null;
                else
                    macFile = null;
                try {
                    Process p = Runtime.getRuntime().exec("sync");
                    if (p.waitFor() != 0) {
                        if (p.exitValue() == 1)
                            Log.d(TAG, "runSystemCmd sync" + " error!");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            } else {
                Log.d(TAG, "=========== delete file error ! ===========");
                return false;
            }
        } else {
            Log.d(TAG, "=========== move key file not exit ===========");
            return false;
        }
    }

    private static void clearBuffer(int type) {
        if (type == HDCP_TYPE) {
            for (int i = 0; i < HDCP_LENTH; i++) {
                data_2[i] = 0;
                hdcpKeyData[i] = 0;
            }
        } else {
            for (int i = 0; i < MAC_LENTH; i++) {
                data_1[i] = 0;
                macKeyData[i] = 0;
            }
        }
    }

    private static void copyBuffer(int type) {
        StringBuilder b = new StringBuilder();
        if (type == HDCP_TYPE) {
            for (int i = 0; i < HDCP_LENTH; i++) {
                if (i % 10 == 0) {
                    Log.d(TAG, b.toString());
                    b.delete(0, b.length());
                    Log.d(TAG, "-----------------------------------------------");
                }
                data_2[i] = hdcpKeyData[i];
                b.append(data_2[i] & 0xff);
                b.append(" ");
            }
        } else {
            Log.d(TAG, "----------------------mac data-------------------------");
            for (int i = 0; i < MAC_LENTH; i++) {
                data_1[i] = macKeyData[i];
                b.append(data_1[i] & 0xff);
                b.append(" ");
            }
            Log.d(TAG, b.toString());
            b.delete(0, b.length());
            Log.d(TAG, "-----------------------------------------------");
        }
    }

    /**
     * Write both the mac and hdcp key
     * @param tv
     * @return
     */
    public static boolean process(TvControlManager tv) {
        if ( ReadKey(HDCP_DIR, HDCP_TYPE) ) {
            copyBuffer(HDCP_TYPE);
            if ( writeKey(tv, data_2, HDCP_TYPE) == 0 ) {
                if ( ReadKey(MAC_DIR, MAC_TYPE) ) {
                    copyBuffer(MAC_TYPE);
                    if ( writeKey(tv, data_1, MAC_TYPE) == 0 ) {
                        if ( moveFile(hdcpKeyData, HDCP_TYPE) ) {
                            clearBuffer(HDCP_TYPE);
                            if ( moveFile(macKeyData, MAC_TYPE) ) {
                                clearBuffer(MAC_TYPE);
                                return true;
                            }
                        }
                    } else {
                        Log.d(TAG, "=========== write mac key error ! ===========");
                    }
                } else {
                    Log.d(TAG, "=========== read  mac key error ! ===========");
                }
            } else {
                Log.d(TAG, "=========== write hdcp key error ! ===========");
            }
        }
        return false;
    }

    /**
     * Just write mac address
     * @param tv
     * @return
     */
    public static boolean processMac(TvControlManager tv) {
        if (ReadKey(MAC_DIR, MAC_TYPE)) {
            copyBuffer(MAC_TYPE);
            if (writeKey(tv, data_1, MAC_TYPE) == 0) {
                if (moveFile(macKeyData, MAC_TYPE)) {
                    clearBuffer(MAC_TYPE);
                    return true;
                }
            } else {
                Log.d(TAG, "=========== write mac key error ! ===========");
            }
        } else {
            Log.d(TAG, "=========== read  mac key error ! ===========");
        }
        return false;
    }

    /**
     *  Just write HDCP key
     * @param tv
     * @return
     */
    public static boolean processHDCP(TvControlManager tv) {
        if (ReadKey(HDCP_DIR, HDCP_TYPE)) {
            copyBuffer(HDCP_TYPE);
            if (writeKey(tv, data_2, HDCP_TYPE) == 0) {
                copyBuffer(2);
                if (moveFile(hdcpKeyData, HDCP_TYPE)) {
                    clearBuffer(HDCP_TYPE);
                    return true;
                }
            } else {
                Log.d(TAG, "=========== write hdcp key error ! ===========");
            }
        } else {
            Log.d(TAG, "=========== read hdcp key error ! ===========");
        }
        return false;
    }

}

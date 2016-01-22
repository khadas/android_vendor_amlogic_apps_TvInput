/**
 * Factory channel preset's Activity
 *
 * @author ice.fu
 */
package com.amlogic.factory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class FavoriteChannelActivity extends Activity
{
    private FileUtil mFileUtils;
    private ListView mListCenter;
    private String mRecoveryPath;// file path and file name
    private String mDbName;// file name
    public AlertDialog.Builder aBuilder;
    public Dialog mydialog;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.favoritechannel);
        mFileUtils = new FileUtil();

        mListCenter = (ListView) findViewById(R.id.list_center);
        mListCenter.setAdapter(newListAdapter());

        mListCenter.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
            {
                Map<String, Object> item = (Map<String, Object>) arg0.getItemAtPosition(arg2);
                mRecoveryPath = (String) item.get("item_path");
                mDbName = (String) item.get("item_name");
                String msg = getString(R.string.suredb);
                OnClickListener listener = new OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        try
                        {
                            aBuilder = new AlertDialog.Builder(FavoriteChannelActivity.this);
                            aBuilder.setTitle(getString(R.string.facui_pindaoyushe)).setMessage(getString(R.string.ispresetting));
                            mydialog = aBuilder.show();
                            File newFile = new File("/data/data/com.android.providers.tv/databases/" + mDbName);
                            if (newFile.exists())
                                delFile(newFile);
                            String commands = "cp -rf " + mRecoveryPath + " /data/data/com.android.providers.tv/databases/" + mDbName;
                            /* do the copy command */
                            Runtime.getRuntime().exec(commands);
                            BufferedReader input = new BufferedReader(new FileReader(newFile));
                            if (input.read() != -1)
                            {
                                mydialog.dismiss();
                                mydialog.dismiss();
                                aBuilder.setTitle(getString(R.string.facui_pindaoyushe)).setMessage(getString(R.string.finishpresetting))
                                        .setPositiveButton(getString(R.string.confirm), null).show();
                            }
                            input.close();
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                };
                ConfirmDialog(msg, listener);
            }
        });
    }

    /**
    * delete file,only support delete file,not support delete path
    * @param file
    * @throws Exception
    */
    private void delFile(File file) throws Exception
    {
        if (!file.exists())
        {
            throw new Exception(getString(R.string.file) + file.getName() + getString(R.string.noexist));
        }
        if (file.isFile())
        {
            if (file.canWrite())
            {
                file.delete();
            }
            else
            {
                mydialog.dismiss();
                aBuilder.setTitle(getString(R.string.facui_pindaoyushe))
                        .setMessage(getString(R.string.alreadyexist) + file.getName() + getString(R.string.onlyread))
                        .setPositiveButton(getString(R.string.confirm), null).show();
                throw new Exception(getString(R.string.file) + file.getName() + getString(R.string.onlyread));
            }
        }
        else
        {
            mydialog.dismiss();
            aBuilder.setTitle(getString(R.string.facui_pindaoyushe))
                    .setMessage(getString(R.string.file) + file.getName() + getString(R.string.notstandard))
                    .setPositiveButton(getString(R.string.confirm), null).show();
            throw new Exception(getString(R.string.file) + file.getName() + getString(R.string.notstandard));
        }
    }

    /* confirm dialog */
    public void ConfirmDialog(String msg, OnClickListener listener)
    {
        new AlertDialog.Builder(FavoriteChannelActivity.this).setTitle(getString(R.string.confirm)).setMessage(msg)
                .setPositiveButton(getString(R.string.confirm), listener).setNegativeButton(getString(R.string.cancel), null).show();
    }

    private List<Map<String, Object>> getListData()
    {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map;
        String recovery_path = getResources().getString(R.string.config_recovery_usb_path);

        File dir = new File("/storage/external_storage");
        String regex_usb = "udisk([0-9])*";
        String regex = ".+\\.[Dd][Bb]";

        if (dir.exists() && dir.isDirectory())
        {
            File[] files = dir.listFiles(new MyFilenameFilter(regex_usb));
            if (files != null && files.length > 0)
            {
                for (File file : files)
                {
                    if (file.exists() && file.isDirectory())
                    {
                        File[] files4 = file.listFiles(new MyFilenameFilter(regex));
                        if (files4 != null && files4.length > 0)
                        {
                            for (File file4 : files4)
                            {
                                map = new HashMap<String, Object>();
                                map.put("item_icon", R.drawable.item_icon_def);
                                map.put("item_name", file4.getName());
                                map.put("item_path", file4.getAbsolutePath());
                                map.put("item_recovery_path", recovery_path + "/" + file4.getName());

                                String date = new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date(file4.lastModified()));
                                map.put("item_date", date);

                                map.put("item_icon2", R.drawable.item_icon2_def);
                                list.add(map);
                            }
                        }
                    }
                }
            }
        }
        return list;
    }

    private SimpleAdapter newListAdapter()
    {
        return new SimpleAdapter(FavoriteChannelActivity.this, getListData(), R.layout.favoritechannel_list, new String[] {"item_icon", "item_name",
                "item_path", "item_date", "item_icon2"}, new int[] {R.id.item_icon, R.id.item_name, R.id.item_path, R.id.item_date, R.id.item_icon2});
    }

    private class MyFilenameFilter implements FilenameFilter
    {
        private Pattern p;

        public MyFilenameFilter(String regex)
        {
            p = Pattern.compile(regex);
        }

        public boolean accept(File file, String name)
        {
            return p.matcher(name).matches();
        }

    }
}

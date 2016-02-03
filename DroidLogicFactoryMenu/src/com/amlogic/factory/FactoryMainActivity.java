/**
 * Factory Menu MainActivity
 *
 * @author ice.fu
 */
package com.amlogic.factory;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.droidlogic.app.SystemControlManager;
import com.droidlogic.app.tv.TvControlManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class FactoryMainActivity extends Activity implements TvControlManager.UpgradeFBCListener
{
    /* father ListView's adapter */
    private ArrayAdapter<String> fAdapter;
    /* son ListView's adapter */
    public static SimpleAdapter sAdapter;
    /* software infomation's adapter */
    public static SimpleAdapter softAdapter;
    /* father ListView's item save to this ArrayList */
    private ArrayList<String> mListMenuData = null;
    /* son ListView's item save to this List */
    private List<Map<String, String>> mListSubMenuData = null;
    /* software infomation's item save to this List */
    private List<Map<String, String>> softData = null;
    /* father listView and son listView's quote */
    private ListView flv;
    public static ListView slv;
    /* father item's position */
    public static int mPage = -1;
    /* son item's position */
    public static int mSubMenuPos = -1;
    // private View mPreMenu = null;
    /* picture mode port's current position */
    public static int mCurInputPortPos = 0;
    /* picture mode mode's current position */
    public static int mCurShowModePos = 0;
    /* white balance mode's current position */
    public static int mColorTemperaturePos = 0;
    /* test picture's current position */
    public static int ceshituPos = 0;
    /* ReShow rate TVIN_3D_STATUS's current position */
    public static int reshowTvinPos = 0;
    /* ReShow rate TvinTransFmt's current position */
    public static int reshowTvinTransFmtPos = 0;
    public static TvControlManager tv = TvControlManager.getInstance();
    private SystemControlManager mSystemControlManager;

    public AlertDialog.Builder aBuilder;
    public Dialog mydialog;
    private PowerManager pm;
    private EditText demoparam;

    private Handler uiHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        setContentView(R.layout.factorymain);
        mSystemControlManager = new SystemControlManager(this);
        /* father ListView's quote */
        flv = (ListView) findViewById(R.id.left_listView);
        flv.setSelection(0);
        mListMenuData = new ArrayList<String>();
        mListMenuData.clear();
        /* add father ListView's item to this adapter */
        fAdapter = new ArrayAdapter<String>(this, R.layout.factory_menu_item, mListMenuData);
        /* add ListView's content to mListMenuData */
        setListFactoryData();
        /* set adapter to ListView */
        flv.setAdapter(fAdapter);
        /* son ListView's quote */
        slv = (ListView) findViewById(R.id.right_listView);
        mListSubMenuData = new ArrayList<Map<String, String>>();
        softData = new ArrayList<Map<String, String>>();
        /* add son ListView's item to this adapter */
        sAdapter = new SimpleAdapter(this, mListSubMenuData, R.layout.factory_submenu_item, new String[] {"sub_name", "sub_value"}, new int[] {
                R.id.sub_name, R.id.sub_value});
        softAdapter = new SimpleAdapter(this, softData, R.layout.softwareinfo, new String[] {"soft_name", "soft_value"}, new int[] {R.id.soft_name,
                R.id.soft_value});
        slv.setAdapter(sAdapter);
        /* Listener when father listView's item is selected */
        flv.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
            {
                if (arg2 == Constant.PAGE_SOFTINFO/* || arg2 == Constant.PAGE_FBC_VERSION*/)
                {
                    slv.setAdapter(softAdapter);
                }
                else
                {
                    slv.setAdapter(sAdapter);
                }
                // mPreMenu = arg1;
                setFactorySubmenu(arg2);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
            }
        });
        /* Listener when father ListView's item is clicked */
        flv.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
            {
                fokbutton(arg2);
            }
        });
        /* Listener when son listView's item is selected */
        slv.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
            {
                mSubMenuPos = arg2;
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
                mSubMenuPos = -1;
            }
        });
        /* Listener when son listView's item is clicked */
        slv.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
            {
                sokbutton(arg2);
            }
        });

        tv.SetUpgradeFBCListener(this);

        uiHandler = new Handler()
        {

            @Override
            public void handleMessage(Message msg)
            {
                // TODO Auto-generated method stub
                if (msg.what == 1)
                {
                    mydialog.dismiss();
                    aBuilder.setTitle(getString(R.string.facui_writekey)).setMessage(getString(R.string.writesuccess))
                            .setPositiveButton(getString(R.string.confirm), null).show();
                }
                else if (msg.what == 2)
                {
                    mydialog.dismiss();
                    aBuilder.setTitle(getString(R.string.facui_writekey)).setMessage(getString(R.string.writefail))
                            .setPositiveButton(getString(R.string.confirm), null).show();
                }
            }

        };
    }

    @Override
    protected void onStart()
    {
        // TODO Auto-generated method stub
        super.onStart();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mCurInputPortPos = 0;
        mCurShowModePos = 0;
        mColorTemperaturePos = 0;
        ceshituPos = 0;
        reshowTvinPos = 0;
        reshowTvinTransFmtPos = 0;
    }

    @Override
    protected void onStop()
    {
        // TODO Auto-generated method stub
        super.onStop();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch (keyCode)
        {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (slv.hasFocus() == true)
                {
                    processSubMenuDirection(true);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (slv.hasFocus() == true)
                {
                    processSubMenuDirection(false);
                    return true;
                }
                else if (flv.hasFocus() == true)
                {
                    slv.requestFocus();
                    slv.setSelection(0);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_BACK:
                if (slv.hasFocus() == true)
                {
                    flv.requestFocus();
                    flv.setSelection(mPage);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (flv.hasFocus() == true && mPage < mListMenuData.size() - 1)
                {
                    flv.setSelection(++mPage);
                }
                if (flv.hasFocus() == true && mPage == mListMenuData.size() - 1)
                {
                    flv.setSelection(0);
                }
                if (slv.hasFocus() == true)
                {
                    slv.requestFocus();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (flv.hasFocus() == true && mPage > 0)
                {
                    flv.setSelection(--mPage);
                }
                if (flv.hasFocus() == true && mPage == 0)
                {
                    flv.setSelection(mListMenuData.size() - 1);
                }
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    /* confirm dialog */
    public void ConfirmDialog(String msg, OnClickListener listener)
    {
        new AlertDialog.Builder(FactoryMainActivity.this).setTitle(getString(R.string.confirm)).setMessage(msg)
                .setPositiveButton(getString(R.string.confirm), listener).setNegativeButton(getString(R.string.cancel), null).show();
    }

    private void fokbutton(int mPage)
    {
        switch (mPage)
        {
            case Constant.PAGE_FACRESET:
                String msg = getString(R.string.surereset);
                OnClickListener listener = new OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        tv.SSMInitDevice();
                        tv.FactoryCleanAllTableForProgram();
                        sendBroadcast(new Intent("android.intent.action.MASTER_CLEAR"));
                    }
                };
                ConfirmDialog(msg, listener);
                break;
            case Constant.PAGE_FACTORYCHANNEL:
                // Intent intent = new Intent();
                // intent.setAction("android.intent.action.FavoriteChannelActivity");
                // this.startActivity(intent);
                if (tv.TvMiscChannelImport(FileUtil.getUdiskPath() + "tv.db") == 0)
                {
                    Toast.makeText(getApplicationContext(), "set success", Toast.LENGTH_LONG).show();
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "set fail", Toast.LENGTH_LONG).show();
                }
                break;
            case Constant.PAGE_UPGRADE:
                Intent upgradeintent = new Intent();
                upgradeintent.setClassName("com.droidlogic.otaupgrade", "com.droidlogic.otaupgrade.MainActivity");
                FactoryMainActivity.this.startActivity(upgradeintent);
                break;
            // case Constant.PAGE_SETDEMOPARAM:
            // demoparam = new EditText(FactoryMainActivity.this);
            // OnClickListener demolistener = new OnClickListener()
            // {
            // @Override
            // public void onClick(DialogInterface dialog, int which)
            // {
            // if (demoparam.getText().toString().equals("8878") || demoparam.getText().toString().equals("8880"))
            // {
            // mSystemControlManager.setBootenv("ubootenv.var.demod", (demoparam.getText().toString()));
            // Toast.makeText(getApplicationContext(), getString(R.string.setsuccess), Toast.LENGTH_LONG).show();
            // }
            // else
            // {
            // Toast.makeText(getApplicationContext(), getString(R.string.setfail), Toast.LENGTH_LONG).show();
            // }
            //
            // }
            // };
            // new
            // AlertDialog.Builder(this).setTitle(getString(R.string.enterdata)).setIcon(android.R.drawable.ic_dialog_info).setView(demoparam)
            // .setPositiveButton(getString(R.string.confirm),
            // demolistener).setNegativeButton(getString(R.string.cancel), null).show();
            // break;

            case Constant.PAGE_WRITE_KEY:
                Log.d("TEST", "--------------- Constant.PAGE_WRITE_KEY is pressed ------------");
                String msg2 = getString(R.string.writekey);
                OnClickListener listener2 = new OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        aBuilder = new AlertDialog.Builder(FactoryMainActivity.this);
                        aBuilder.setTitle(getString(R.string.facui_writekey)).setMessage(getString(R.string.iswriting));
                        mydialog = aBuilder.show();
                        Thread writeThread = new Thread()
                        {
                            @Override
                            public void run()
                            {
                                // TODO Auto-generated method stub
                                if (FileUtil.process(tv))
                                {
                                    Message msg = uiHandler.obtainMessage(1);
                                    uiHandler.sendMessage(msg);
                                }
                                else
                                {
                                    Message msg2 = uiHandler.obtainMessage(2);
                                    uiHandler.sendMessage(msg2);
                                }
                            }
                        };
                        writeThread.start();
                    }
                };
                ConfirmDialog(msg2, listener2);
                break;
            default:
                break;
        }
    }

    /* cursor's position in noLiner's son dialog */
    int j = 0;

    protected int showAddDialog(final TvControlManager.SourceInput_Type source, final TvControlManager.noline_params_t noline_params)
    {
        LayoutInflater factory = LayoutInflater.from(this);
        View textEntryView = factory.inflate(R.layout.mdialog, null);
        /* EditText backspace's listener */
        OnKeyListener listener = new OnKeyListener()
        {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event)
            {
                int i = ((EditText) v).getText().toString().length();
                switch (keyCode)
                {
                    case KeyEvent.KEYCODE_0:
                    case KeyEvent.KEYCODE_1:
                    case KeyEvent.KEYCODE_2:
                    case KeyEvent.KEYCODE_3:
                    case KeyEvent.KEYCODE_4:
                    case KeyEvent.KEYCODE_5:
                    case KeyEvent.KEYCODE_6:
                    case KeyEvent.KEYCODE_7:
                    case KeyEvent.KEYCODE_8:
                    case KeyEvent.KEYCODE_9:
                        if (event.getAction() == KeyEvent.ACTION_UP)
                            j++;
                        break;
                    case KeyEvent.KEYCODE_CLEAR:
                        if (event.getAction() == KeyEvent.ACTION_DOWN && j > 0 && !"".equals(((EditText) v).getText().toString().trim()))
                        {
                            String str1 = new String(((EditText) v).getText().toString());
                            String str2 = new String(((EditText) v).getText().toString());
                            str1 = str1.substring(0, j - 1);
                            str2 = str2.substring(j, i);
                            String str = str1 + str2;
                            ((EditText) v).setText(str);
                            /*author:fuhao::set cursor position*/
                            ((EditText) v).setSelection(--j);
                        }
                        break;
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        if (event.getAction() == KeyEvent.ACTION_DOWN)
                            if (j > 0)
                                j--;
                        break;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        if (event.getAction() == KeyEvent.ACTION_DOWN)
                            if (j < i)
                                j++;
                        break;
                    default:
                        break;
                }
                return false;
            }
        };
        final EditText editText1 = (EditText) textEntryView.findViewById(R.id.editText1);
        final EditText editText2 = (EditText) textEntryView.findViewById(R.id.editText2);
        final EditText editText3 = (EditText) textEntryView.findViewById(R.id.editText3);
        final EditText editText4 = (EditText) textEntryView.findViewById(R.id.editText4);
        final EditText editText5 = (EditText) textEntryView.findViewById(R.id.editText5);
        Button confirmButton = (Button) textEntryView.findViewById(R.id.confirm);
        Button cancelButton = (Button) textEntryView.findViewById(R.id.cancel);
        cancelButton.requestFocus();
        /*listener when EditText's focus change,to set j as 0*/
        OnFocusChangeListener listener2 = new OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                j = ((EditText) v).getText().toString().length();
            }
        };
        editText1.setOnFocusChangeListener(listener2);
        editText2.setOnFocusChangeListener(listener2);
        editText3.setOnFocusChangeListener(listener2);
        editText4.setOnFocusChangeListener(listener2);
        editText5.setOnFocusChangeListener(listener2);
        /* set listener to editText */
        editText1.setOnKeyListener(listener);
        editText2.setOnKeyListener(listener);
        editText3.setOnKeyListener(listener);
        editText4.setOnKeyListener(listener);
        editText5.setOnKeyListener(listener);
        /* cut soft keyboard */
        Class<EditText> cls = EditText.class;
        try
        {
            Method setShowSoftInputOnFocus = cls.getMethod("setShowSoftInputOnFocus", boolean.class);
            setShowSoftInputOnFocus.setAccessible(false);
            setShowSoftInputOnFocus.invoke(editText1, false);
            setShowSoftInputOnFocus.invoke(editText2, false);
            setShowSoftInputOnFocus.invoke(editText3, false);
            setShowSoftInputOnFocus.invoke(editText4, false);
            setShowSoftInputOnFocus.invoke(editText5, false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        final AlertDialog.Builder ad1 = new AlertDialog.Builder(FactoryMainActivity.this);

        ad1.setTitle(getString(R.string.enterdata));
        ad1.setView(textEntryView);
        final Dialog dialog = ad1.show();// show dialog
        confirmButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (editText1.length() != 0 && editText2.length() != 0 && editText3.length() != 0 && editText4.length() != 0
                        && editText5.length() != 0)
                {
                    noline_params.osd0 = Integer.parseInt(editText1.getText().toString());
                    noline_params.osd25 = Integer.parseInt(editText2.getText().toString());
                    noline_params.osd50 = Integer.parseInt(editText3.getText().toString());
                    noline_params.osd75 = Integer.parseInt(editText4.getText().toString());
                    noline_params.osd100 = Integer.parseInt(editText5.getText().toString());
                    if (noline_params.osd0 >= 0 && noline_params.osd0 < 256 && noline_params.osd25 >= 0 && noline_params.osd25 < 256
                            && noline_params.osd50 >= 0 && noline_params.osd50 < 256 && noline_params.osd75 >= 0 && noline_params.osd75 < 256
                            && noline_params.osd100 >= 0 && noline_params.osd100 < 256)
                    {
                        tv.FactorySetNolineParams(TvControlManager.NOLINE_PARAMS_TYPE.values()[mSubMenuPos - 1], source, noline_params);
                        for (int io = mListSubMenuData.size() - 1; io > 0; io--)
                            mListSubMenuData.remove(io);
                        ShowSubView ssv = new ShowSubView(mListSubMenuData, FactoryMainActivity.this);
                        ssv.setNoLine(source);
                        sAdapter.notifyDataSetChanged();
                        dialog.dismiss();
                    }
                    else
                    {
                        new AlertDialog.Builder(FactoryMainActivity.this).setMessage(getString(R.string.wrongdata))
                                .setPositiveButton(getString(R.string.confirm), null).show();
                    }
                }
                else
                {
                    new AlertDialog.Builder(FactoryMainActivity.this).setMessage(getString(R.string.wrongdata))
                            .setPositiveButton(getString(R.string.confirm), null).show();
                }
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dialog.dismiss();
            }
        });
        return 1;
    }

    /* ADC auto calibration dialog's confirm listener */
    OnClickListener adclistener = new OnClickListener()
    {
        @Override
        public void onClick(DialogInterface dialog, int which)
        {
        }
    };
    public EditText whitebalanceEditText;
    /* White Balance input dialog's confirm listener */
    OnClickListener whitebalancelistener = new OnClickListener()
    {
        @Override
        public void onClick(DialogInterface dialog, int which)
        {
            Map<String, String> map = mListSubMenuData.get(mSubMenuPos);
            int value = Integer.parseInt(whitebalanceEditText.getText().toString());
            TvControlManager.SourceInput_Type source = TvControlManager.SourceInput_Type.values()[mCurInputPortPos];
            // TODO Auto-generated method stub
            switch (mSubMenuPos)
            {
                case 2:
                    if (value >= 0 && value < 2047)
                    {
                        tv.FactoryWhiteBalanceSetRedGain(source.toInt(), mColorTemperaturePos, value);
                        map.put("sub_value", "" + value);
                        sAdapter.notifyDataSetChanged();
                    }
                    break;
                case 3:
                    if (value >= 0 && value < 2047)
                    {
                        tv.FactoryWhiteBalanceSetGreenGain(source.toInt(), mColorTemperaturePos, value);
                        map.put("sub_value", "" + value);
                        sAdapter.notifyDataSetChanged();
                    }
                    break;
                case 4:
                    if (value >= 0 && value < 2047)
                    {
                        tv.FactoryWhiteBalanceSetBlueGain(source.toInt(), mColorTemperaturePos, value);
                        map.put("sub_value", "" + value);
                        sAdapter.notifyDataSetChanged();
                    }
                    break;
                case 5:
                    if (value >= -1023 && value < 1024)
                    {
                        tv.FactoryWhiteBalanceSetRedOffset(source.toInt(), mColorTemperaturePos, value);
                        map.put("sub_value", "" + value);
                        sAdapter.notifyDataSetChanged();
                    }
                    break;
                case 6:
                    if (value >= -1023 && value < 1024)
                    {
                        tv.FactoryWhiteBalanceSetGreenOffset(source.toInt(), mColorTemperaturePos, value);
                        map.put("sub_value", "" + value);
                        sAdapter.notifyDataSetChanged();
                    }
                    break;
                case 7:
                    if (value >= -1023 && value < 1024)
                    {
                        tv.FactoryWhiteBalanceSetBlueOffset(source.toInt(), mColorTemperaturePos, value);
                        map.put("sub_value", "" + value);
                        sAdapter.notifyDataSetChanged();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    public void sokbutton(int mSubMenuPos)
    {
        /* ADC auto calibration */
        if (mPage == Constant.PAGE_CALIBRATE && mSubMenuPos == 1)
        {
            ConfirmDialog(getString(R.string.sureadc), adclistener);
        }
        if (mPage == Constant.PAGE_WHITEBALANCE)
        {
            whitebalanceEditText = new EditText(FactoryMainActivity.this);
            new AlertDialog.Builder(this).setTitle(getString(R.string.enterdata)).setIcon(android.R.drawable.ic_dialog_info)
                    .setView(whitebalanceEditText).setPositiveButton(getString(R.string.confirm), whitebalancelistener)
                    .setNegativeButton(getString(R.string.cancel), null).show();
        }
        if (mPage == Constant.PAGE_PORTPRINT_SWITCH)
        {
            switch (mSubMenuPos)
            {
                case 0:
                    ConfirmDialog(getString(R.string.sure_portprint_off), new OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            mSystemControlManager.setBootenv("ubootenv.var.UART", "off");
                            pm.reboot(null);
                        }
                    });
                    break;
                case 1:
                    ConfirmDialog(getString(R.string.sure_portprint_on), new OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            mSystemControlManager.setBootenv("ubootenv.var.UART", "on");
                            pm.reboot(null);
                        }
                    });
                    break;
                default:
                    break;
            }
        }
        // if (mPage == Constant.PAGE_REMOTECONTROL)
        // {
        // switch (mSubMenuPos)
        // {
        // case 0:
        // ConfirmDialog(getString(R.string.sure_remotecontrol), new OnClickListener()
        // {
        // @Override
        // public void onClick(DialogInterface dialog, int which)
        // {
        // mSystemControlManager.setBootenv("ubootenv.var.remoteid", "0");
        // mSystemControlManager.setBootenv("ubootenv.var.factoryid", "0");
        // pm.reboot(null);
        // }
        // });
        // break;
        // case 1:
        // ConfirmDialog(getString(R.string.sure_remotecontrol), new OnClickListener()
        // {
        // @Override
        // public void onClick(DialogInterface dialog, int which)
        // {
        // mSystemControlManager.setBootenv("ubootenv.var.remoteid", "1");
        // mSystemControlManager.setBootenv("ubootenv.var.factoryid", "1");
        // pm.reboot(null);
        // }
        // });
        // break;
        // case 2:
        // ConfirmDialog(getString(R.string.sure_remotecontrol), new OnClickListener()
        // {
        // @Override
        // public void onClick(DialogInterface dialog, int which)
        // {
        // mSystemControlManager.setBootenv("ubootenv.var.remoteid", "1");
        // mSystemControlManager.setBootenv("ubootenv.var.factoryid", "2");
        // pm.reboot(null);
        // }
        // });
        // break;
        // case 3:
        // ConfirmDialog(getString(R.string.sure_remotecontrol), new OnClickListener()
        // {
        // @Override
        // public void onClick(DialogInterface dialog, int which)
        // {
        // mSystemControlManager.setBootenv("ubootenv.var.remoteid", "1");
        // mSystemControlManager.setBootenv("ubootenv.var.factoryid", "3");
        // pm.reboot(null);
        // }
        // });
        // break;
        // default:
        // break;
        // }
        // }
        if (mPage == Constant.PAGE_SCREEN)
        {
            switch (mSubMenuPos)
            {
                case 0:
                    ConfirmDialog(getString(R.string.sure_screen_up), new OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            mSystemControlManager.setBootenv("ubootenv.var.osd_reverse", "n");
                            mSystemControlManager.setBootenv("ubootenv.var.video_reverse", "n");
                            pm.reboot(null);
                        }
                    });
                    break;
                case 1:
                    ConfirmDialog(getString(R.string.sure_screen_down), new OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            mSystemControlManager.setBootenv("ubootenv.var.osd_reverse", "all,true");
                            mSystemControlManager.setBootenv("ubootenv.var.video_reverse", "1");
                            pm.reboot(null);
                        }
                    });
                    break;
                default:
                    break;
            }
        }
        // if (mPage == Constant.PAGE_UPGRADE_FBC)
        // {
        // if (mUpgradeFBCDoing == 0)
        // {
        // mUpgradeFBCDoing = 1;
        // int up_mode = 0, up_block_size = 0;
        // String path_str, mode_str, up_blk_size_str;
        //
        // Map<String, String> map = mListSubMenuData.get(FactoryMainActivity.mSubMenuPos);
        // if (FactoryMainActivity.mSubMenuPos == 0)
        // {
        // map.put("sub_value", getString(Constant.FACUI_START));
        // FactoryMainActivity.sAdapter.notifyDataSetChanged();
        // path_str = getUpdateFBCPath();
        // mode_str = mSystemControlManager.getPropertyString("tv.upgradefbc.mode", "null");
        // if (mode_str.equalsIgnoreCase("null"))
        // {
        // up_mode = 2;
        // }
        // else
        // {
        // up_mode = Integer.parseInt(mode_str);
        // }
        //
        // if (up_mode < 0 || up_mode > 17)
        // {
        // up_mode = 2;
        // }
        //
        // up_blk_size_str = mSystemControlManager.getPropertyString("tv.upgradefbc.blksize", "null");
        // if (up_blk_size_str.equalsIgnoreCase("null"))
        // {
        // up_block_size = 0x10000;
        // }
        // else
        // {
        // up_block_size = Integer.parseInt(up_blk_size_str, 16);
        // }
        //
        // Log.d("upgradeFBCSetValue: ", "path_str = " + path_str);
        // Log.d("upgradeFBCSetValue: ", "mode_str = " + mode_str);
        // Log.d("upgradeFBCSetValue: ", "up_mode = " + up_mode);
        // Log.d("upgradeFBCSetValue: ", "up_block_size = " + up_block_size);
        // tv.StartUpgradeFBC(path_str, up_mode, up_block_size);
        // }
        // }
        // else
        // {
        // Toast.makeText(getApplicationContext(), "Upgrade FBC doing...", Toast.LENGTH_LONG).show();
        // }
        // }
        // if (mPage == Constant.PAGE_OUTPUTMODE)
        // {
        // switch (mSubMenuPos)
        // {
        // case 0:
        // ConfirmDialog(getString(R.string.sure_confirm), new OnClickListener()
        // {
        // @Override
        // public void onClick(DialogInterface dialog, int which)
        // {
        // mSystemControlManager.setBootenv("ubootenv.var.outputmode", "4k2k30hz");
        // pm.reboot(null);
        // }
        // });
        // break;
        // case 1:
        // ConfirmDialog(getString(R.string.sure_confirm), new OnClickListener()
        // {
        // @Override
        // public void onClick(DialogInterface dialog, int which)
        // {
        // mSystemControlManager.setBootenv("ubootenv.var.outputmode", "4k2k50hz");
        // pm.reboot(null);
        // }
        // });
        // break;
        // case 2:
        // ConfirmDialog(getString(R.string.sure_confirm), new OnClickListener()
        // {
        // @Override
        // public void onClick(DialogInterface dialog, int which)
        // {
        // mSystemControlManager.setBootenv("ubootenv.var.outputmode", "4k2k60hz");
        // pm.reboot(null);
        // }
        // });
        // break;
        // case 3:
        // ConfirmDialog(getString(R.string.sure_confirm), new OnClickListener()
        // {
        // @Override
        // public void onClick(DialogInterface dialog, int which)
        // {
        // mSystemControlManager.setBootenv("ubootenv.var.outputmode", "1080p");
        // pm.reboot(null);
        // }
        // });
        // break;
        // case 4:
        // ConfirmDialog(getString(R.string.sure_confirm), new OnClickListener()
        // {
        // @Override
        // public void onClick(DialogInterface dialog, int which)
        // {
        // mSystemControlManager.setBootenv("ubootenv.var.outputmode", "1080p50hz");
        // pm.reboot(null);
        // }
        // });
        // break;
        // default:
        // break;
        // }
        // }
    }

    private String getUpdateFBCPath()
    {
        String UpdateFBCPath = Constant.CS_UPGRADE_FBC_FILE_NAME;
        File[] files = new File("/storage/external_storage").listFiles();
        if (files != null)
        {
            for (File file : files)
            {

                if (file.getPath().startsWith("/storage/external_storage/sd"))
                {
                    File[] myfiles = new File(file.getPath()).listFiles();
                    if (myfiles != null)
                    {
                        for (File myfile : myfiles)
                        {
                            if (myfile.getName().equals("spi.bin"))
                            {
                                UpdateFBCPath = myfile.getAbsolutePath();
                            }
                        }
                    }
                }
            }
        }
        return UpdateFBCPath;
    }

    private void setListFactoryData()
    {
        mListMenuData.add(getString(Constant.FACUI_ADC));
        mListMenuData.add(getString(Constant.FACUI_PICMODE));
        mListMenuData.add(getString(Constant.FACUI_WHTBLAN));
        mListMenuData.add(getString(Constant.FACUI_SSC));
        mListMenuData.add(getString(Constant.FACUI_CHONGXIAN));
        mListMenuData.add(getString(Constant.FACUI_GONGNENG_CESHITU));
        mListMenuData.add(getString(Constant.FACUI_LAOHUA));
        mListMenuData.add(getString(Constant.FACUI_PINDAOYUSHE));
        mListMenuData.add(getString(Constant.FACUI_FACRESET));
        mListMenuData.add(getString(Constant.FACUI_SOFTINFO));
        mListMenuData.add(getString(Constant.FACUI_HDMI_HDCP_DEMOKEY));
        mListMenuData.add(getString(Constant.FACUI_WRITE_HDCP_MAC_KEY));
        // mListMenuData.add(getString(Constant.FACUI_UPGRADE_FBC));
        // mListMenuData.add(getString(Constant.FACUI_FBC_VERSION));
        mListMenuData.add(getString(Constant.FACUI_SERIAL_CMD_SWITCH));
        mListMenuData.add(getString(Constant.FACUI_PORTPRINT_SWITCH));
        // mListMenuData.add(getString(Constant.FACUI_REMOTECONTROL));
        // mListMenuData.add(getString(Constant.FACUI_ADB_SWITCH));
        mListMenuData.add(getString(Constant.FACUI_UPGRADE));
        // mListMenuData.add(getString(Constant.FACUI_SETDEMOPARAM));
        // mListMenuData.add(getString(Constant.FACUI_AUTOSAVELOG_SWITCH));
        mListMenuData.add(getString(Constant.FACUI_DYNAMIC_BACKLIGHT_VALUE));
        mListMenuData.add(getString(Constant.FACUI_SCREEN));
        // mListMenuData.add(getString(Constant.FACUI_OUTPUT_MODE));
    }

    private void setFactorySubmenu(int position)
    {
        mListSubMenuData.clear();
        softData.clear();
        ShowSubView ssv = new ShowSubView(mListSubMenuData, FactoryMainActivity.this);
        switch (position)
        {
            case Constant.PAGE_CALIBRATE:
                ssv.show_calibate_submenu();
                break;
            case Constant.PAGE_PICTUREMODE:
                ssv.show_picture_submenu();
                break;
            case Constant.PAGE_WHITEBALANCE:// white balance
                ssv.show_whitebalance_submenu();
                break;
            case Constant.PAGE_SSC:
                ssv.show_ssc_submenu();
                break;
            case Constant.PAGE_OVERSCAN:
                ssv.show_reshow_submenu();
                break;
            case Constant.PAGE_TESTPATTERN:
                ssv.show_ceshitu_submenu();
                break;
            case Constant.PAGE_AGINGMODE:
                ssv.show_agingmode_submenu();
                break;
            case Constant.PAGE_FACRESET:
                mPage = Constant.PAGE_FACRESET;
                break;
            case Constant.PAGE_SOFTINFO:
                ssv = new ShowSubView(softData, FactoryMainActivity.this);
                ssv.show_softinfo_submenu();
                break;
            case Constant.HDMI_HDCP_DEMOKEY:
                ssv.show_hdcp_submenu();
                break;
            case Constant.PAGE_WRITE_KEY:
                mPage = Constant.PAGE_WRITE_KEY;
                break;
            // case Constant.PAGE_UPGRADE_FBC:
            // ssv.show_upgrade_fbc();
            // break;
            // case Constant.PAGE_FBC_VERSION:
            // ssv = new ShowSubView(softData, FactoryMainActivity.this);
            // ssv.show_fbc_version();
            // break;
            case Constant.PAGE_SERIAL_CMD_SWITCH:
                ssv.show_serial_submenu();
                break;
            case Constant.PAGE_PORTPRINT_SWITCH:
                ssv.show_portprint_submenu();
                break;
            // case Constant.PAGE_REMOTECONTROL:
            // ssv.show_remotecontrol();
            // break;
            // case Constant.PAGE_ADB_SWITCH:
            // ssv.show_adb_submenu();
            // break;
            case Constant.PAGE_UPGRADE:
                mPage = Constant.PAGE_UPGRADE;
                break;
            // case Constant.PAGE_SETDEMOPARAM:
            // mPage = Constant.PAGE_SETDEMOPARAM;
            // break;
            // case Constant.PAGE_AUTOSAVELOG_SWITCH:
            // ssv.show_autosavelog_submenu();
            // break;
            case Constant.PAGE_DYNAMIC_BACKLIGHT_VALUE:
                ssv.show_dynamicbacklight_submenu();
                break;
            case Constant.PAGE_SCREEN:
                ssv.show_screen_submenu();
                break;
            // case Constant.PAGE_OUTPUTMODE:
            // ssv.show_outputmode();
            // mPage = Constant.PAGE_OUTPUTMODE;
            // break;
            default:
                break;
        }
        sAdapter.notifyDataSetChanged();
    }

    private void processSubMenuDirection(boolean left)
    {
        KeyDown kDown = new KeyDown(mListSubMenuData, FactoryMainActivity.this);
        int step = 0;
        if (left == true)
        {
            step = -1;
        }
        else
        {
            step = 1;
        }
        switch (mPage)
        {
            case Constant.PAGE_CALIBRATE:
                kDown.adjustCalibrate(left, step);
                break;
            case Constant.PAGE_PICTUREMODE:
                kDown.adjustPicMode(left, step);
                break;
            case Constant.PAGE_WHITEBALANCE:
                kDown.adjustWhiteBalance(left, step);
                break;
            case Constant.PAGE_SSC:
                kDown.adjustSSC(left, step);
                break;
            case Constant.PAGE_OVERSCAN:
                kDown.adjustReshow(left, step);
                break;
            case Constant.PAGE_TESTPATTERN:
                kDown.adjustFunction(left, step);
                break;
            case Constant.PAGE_AGINGMODE:
                kDown.adjustAging(left, step);
                break;
            case Constant.HDMI_HDCP_DEMOKEY:
                kDown.hdcpSetValue();
                break;
            case Constant.PAGE_SERIAL_CMD_SWITCH:
                kDown.serialSetValue();
                break;
            // case Constant.PAGE_ADB_SWITCH:
            // kDown.adbSetValue();
            // break;
            // case Constant.PAGE_AUTOSAVELOG_SWITCH:
            // kDown.autosavelogSetValue();
            // break;
            case Constant.PAGE_DYNAMIC_BACKLIGHT_VALUE:
                kDown.dynamicbacklightSetValue(left, step);
                break;
            default:
                break;
        }
    }

    private int mUpgradeFBCDoing = 0;

    @Override
    public void onUpgradeStatus(int state, int param)
    {
        String tmp_str = "";

        if (state == TvControlManager.FBCUpgradeState.STATE_RUNNING.toInt())
        {
            tmp_str = "Upgrade FBC running: progress = " + String.valueOf(param) + " %";
            Toast.makeText(getApplicationContext(), tmp_str, Toast.LENGTH_LONG).show();
        }
        else if (state == TvControlManager.FBCUpgradeState.STATE_FINISHED.toInt())
        {
            tmp_str = "Upgrade FBC finished";
            Toast.makeText(getApplicationContext(), tmp_str, Toast.LENGTH_LONG).show();
            mUpgradeFBCDoing = 0;
        }
        else if (state == TvControlManager.FBCUpgradeState.STATE_ABORT.toInt())
        {
            if (param == TvControlManager.FBCUpgradeErrorCode.ERR_SERIAL_CONNECT.toInt())
            {
                tmp_str = "Upgrade FBC error: Please check serial connect!";
                Toast.makeText(getApplicationContext(), tmp_str, Toast.LENGTH_LONG).show();
            }
            else if (param == TvControlManager.FBCUpgradeErrorCode.ERR_OPEN_BIN_FILE.toInt())
            {
                tmp_str = "Upgrade FBC error: Open upgrade bin file failed!";
                Toast.makeText(getApplicationContext(), tmp_str, Toast.LENGTH_LONG).show();
            }
            else if (param == TvControlManager.FBCUpgradeErrorCode.ERR_BIN_FILE_SIZE.toInt())
            {
                tmp_str = "Upgrade FBC error: upgrade bin file size error!";
                Toast.makeText(getApplicationContext(), tmp_str, Toast.LENGTH_LONG).show();
            }
            mUpgradeFBCDoing = 0;
        }
    }
}

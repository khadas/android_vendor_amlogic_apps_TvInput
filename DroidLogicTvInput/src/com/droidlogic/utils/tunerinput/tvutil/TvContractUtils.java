/*
 * Copyright 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.droidlogic.utils.tunerinput.tvutil;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.tv.TvContentRating;
import android.media.tv.TvContract;
import android.media.tv.TvContract.Channels;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.Pair;
import android.util.SparseArray;

import com.droidlogic.utils.tunerinput.data.ChannelInfo;
import com.droidlogic.utils.tunerinput.tvutil.TVChannelParams;
import com.droidlogic.utils.tunerinput.tvutil.MapUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

/**
 * Static helper methods for working with {@link android.media.tv.TvContract}.
 */
public class TvContractUtils
{
    private static final String TAG = "TvContractUtils";
    private static final boolean DEBUG = true;

    private static final SparseArray<String> VIDEO_HEIGHT_TO_FORMAT_MAP = new SparseArray<String>();

    static
    {
        VIDEO_HEIGHT_TO_FORMAT_MAP.put(480, TvContract.Channels.VIDEO_FORMAT_480P);
        VIDEO_HEIGHT_TO_FORMAT_MAP.put(576, TvContract.Channels.VIDEO_FORMAT_576P);
        VIDEO_HEIGHT_TO_FORMAT_MAP.put(720, TvContract.Channels.VIDEO_FORMAT_720P);
        VIDEO_HEIGHT_TO_FORMAT_MAP.put(1080, TvContract.Channels.VIDEO_FORMAT_1080P);
        VIDEO_HEIGHT_TO_FORMAT_MAP.put(2160, TvContract.Channels.VIDEO_FORMAT_2160P);
        VIDEO_HEIGHT_TO_FORMAT_MAP.put(4320, TvContract.Channels.VIDEO_FORMAT_4320P);
    }

    private static final SparseArray<String> CHANNEL_MODE_TO_TYPE_MAP = new SparseArray<String>();

    static
    {
        CHANNEL_MODE_TO_TYPE_MAP.put(TVChannelParams.MODE_DTMB, Channels.TYPE_DTMB);
        CHANNEL_MODE_TO_TYPE_MAP.put(TVChannelParams.MODE_QPSK, Channels.TYPE_DVB_S);
        CHANNEL_MODE_TO_TYPE_MAP.put(TVChannelParams.MODE_QAM, Channels.TYPE_DVB_C);
        CHANNEL_MODE_TO_TYPE_MAP.put(TVChannelParams.MODE_OFDM, Channels.TYPE_DVB_T);
        CHANNEL_MODE_TO_TYPE_MAP.put(TVChannelParams.MODE_ATSC, Channels.TYPE_ATSC_C);
        CHANNEL_MODE_TO_TYPE_MAP.put(TVChannelParams.MODE_ANALOG, Channels.TYPE_PAL);
        CHANNEL_MODE_TO_TYPE_MAP.put(TVChannelParams.MODE_ISDBT, Channels.TYPE_ISDB_T);
    }

    private static final Map<String, Integer> CHANNEL_TYPE_TO_MODE_MAP = new HashMap<String, Integer>();

    static
    {
        CHANNEL_TYPE_TO_MODE_MAP.put(Channels.TYPE_DTMB, TVChannelParams.MODE_DTMB);
        CHANNEL_TYPE_TO_MODE_MAP.put(Channels.TYPE_DVB_C, TVChannelParams.MODE_QAM);
        CHANNEL_TYPE_TO_MODE_MAP.put(Channels.TYPE_DVB_T, TVChannelParams.MODE_OFDM);
        CHANNEL_TYPE_TO_MODE_MAP.put(Channels.TYPE_PAL, TVChannelParams.MODE_ANALOG);
    }

    public static void deleteChannels(Context context, String inputId)
    {
        Uri channelsUri = TvContract.buildChannelsUriForInput(inputId);
        ContentResolver resolver = context.getContentResolver();
        resolver.delete(channelsUri, Channels._ID + "!=-1", null);
    }

    public static void insertDtvChannel(Context context, String inputId, ChannelInfo channel)
    {
        Uri channelsUri = TvContract.buildChannelsUriForInput(inputId);
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(Channels.COLUMN_INPUT_ID, inputId);
        Map<Uri, String> logos = new HashMap<Uri, String>();
        values.put(Channels.COLUMN_DISPLAY_NUMBER, channel.number);
        values.put(Channels.COLUMN_DISPLAY_NAME, channel.name);
        values.put(Channels.COLUMN_ORIGINAL_NETWORK_ID, channel.originalNetworkId);
        values.put(Channels.COLUMN_TRANSPORT_STREAM_ID, channel.transportStreamId);
        values.put(Channels.COLUMN_SERVICE_ID, channel.serviceId);
        values.putNull(Channels.COLUMN_VIDEO_FORMAT);
        values.put(Channels.COLUMN_TYPE, getChannelType(channel.type));

        if (channel.serviceType == 1)
            values.put(Channels.COLUMN_SERVICE_TYPE, Channels.SERVICE_TYPE_AUDIO_VIDEO);
        else if (channel.serviceType == 2)
            values.put(Channels.COLUMN_SERVICE_TYPE, Channels.SERVICE_TYPE_AUDIO);
        else
            values.put(Channels.COLUMN_SERVICE_TYPE, Channels.SERVICE_TYPE_OTHER);

        Map<String, String> map = new HashMap<String, String>();
        map.put("freq", String.valueOf(channel.frequency));
        map.put("bw", String.valueOf(channel.bandwidth));
        map.put("type", String.valueOf(channel.serviceType));
        map.put("vid", String.valueOf(channel.videoPID));
        map.put("vfmt", String.valueOf(channel.videoFormat));
        map.put("aids", Arrays.toString(channel.audioPIDs));
        map.put("afmts", Arrays.toString(channel.audioFormats));
        map.put("pcr", String.valueOf(channel.pcrPID));
        String output = MapUtil.mapToString(map);
        values.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA, output);
        Uri uri = resolver.insert(TvContract.Channels.CONTENT_URI, values);
        if (!TextUtils.isEmpty(channel.logoUrl))
        {
            logos.put(TvContract.buildChannelLogoUri(uri), channel.logoUrl);
        }
        if (!logos.isEmpty())
        {
            new InsertLogosTask(context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, logos);
        }
    }

    // If a channel exists, update it. If not, insert a new one.
    public static void updateOrinsertDtvChannel(Context context, String inputId, ChannelInfo channel)
    {
        Uri channelsUri = TvContract.buildChannelsUriForInput(inputId);
        ContentResolver resolver = context.getContentResolver();
        String[] projection = {Channels._ID, Channels.COLUMN_SERVICE_ID};

        Cursor cursor = null;

        try
        {
            cursor = resolver.query(channelsUri, projection, null, null, null);
            while (cursor != null && cursor.moveToNext())
            {
                long rowId = cursor.getLong(0);
                int serviceId = cursor.getInt(1);
                if (serviceId == channel.serviceId)
                {
                    ContentValues values = new ContentValues();
                    values.put(Channels.COLUMN_INPUT_ID, inputId);
                    Map<Uri, String> logos = new HashMap<Uri, String>();
                    values.put(Channels.COLUMN_DISPLAY_NUMBER, channel.number);
                    values.put(Channels.COLUMN_DISPLAY_NAME, channel.name);
                    values.put(Channels.COLUMN_ORIGINAL_NETWORK_ID, channel.originalNetworkId);
                    values.put(Channels.COLUMN_TRANSPORT_STREAM_ID, channel.transportStreamId);
                    values.put(Channels.COLUMN_SERVICE_ID, channel.serviceId);
                    values.putNull(Channels.COLUMN_VIDEO_FORMAT);
                    values.put(Channels.COLUMN_TYPE, getChannelType(channel.type));

                    if (channel.serviceType == 1)
                        values.put(Channels.COLUMN_SERVICE_TYPE, Channels.SERVICE_TYPE_AUDIO_VIDEO);
                    else if (channel.serviceType == 2)
                        values.put(Channels.COLUMN_SERVICE_TYPE, Channels.SERVICE_TYPE_AUDIO);
                    else
                        values.put(Channels.COLUMN_SERVICE_TYPE, Channels.SERVICE_TYPE_OTHER);

                    Map<String, String> map = new HashMap<String, String>();
                    map.put("freq", String.valueOf(channel.frequency));
                    map.put("bw", String.valueOf(channel.bandwidth));
                    map.put("type", String.valueOf(channel.serviceType));
                    map.put("vid", String.valueOf(channel.videoPID));
                    map.put("vfmt", String.valueOf(channel.videoFormat));
                    map.put("aids", Arrays.toString(channel.audioPIDs));
                    map.put("afmts", Arrays.toString(channel.audioFormats));
                    map.put("pcr", String.valueOf(channel.pcrPID));
                    String output = MapUtil.mapToString(map);
                    values.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA, output);
                    Uri uri = TvContract.buildChannelUri(rowId);
                    resolver.update(uri, values, null, null);
                    if (!TextUtils.isEmpty(channel.logoUrl))
                    {
                        logos.put(TvContract.buildChannelLogoUri(uri), channel.logoUrl);
                    }
                    if (!logos.isEmpty())
                    {
                        new InsertLogosTask(context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, logos);
                    }
                    return;
                }
            }
        }
        catch (Exception e)
        {
            // TODO: handle exception
        }
        finally
        {
            if (cursor != null)
            {
                cursor.close();
            }
        }

        insertDtvChannel(context, inputId, channel);
    }

    public static void insertAtvChannel(Context context, String inputId, ChannelInfo channel)
    {
        Uri channelsUri = TvContract.buildChannelsUriForInput(inputId);
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(Channels.COLUMN_INPUT_ID, inputId);
        Map<Uri, String> logos = new HashMap<Uri, String>();
        values.put(Channels.COLUMN_DISPLAY_NUMBER, channel.number);
        values.put(Channels.COLUMN_DISPLAY_NAME, channel.name);
        values.put(Channels.COLUMN_TYPE, Channels.TYPE_PAL);// TODO: channel.type -> COLUMN_TYPE (PAL/NTSC/SECAM)?
        if (channel.serviceType == 1)
        {
            values.put(Channels.COLUMN_SERVICE_TYPE, Channels.SERVICE_TYPE_AUDIO_VIDEO);
        }
        else if (channel.serviceType == 2)
        {
            values.put(Channels.COLUMN_SERVICE_TYPE, Channels.SERVICE_TYPE_AUDIO);
        }
        Map<String, String> map = new HashMap<String, String>();
        map.put("type", String.valueOf(channel.type));
        map.put("stype", String.valueOf(channel.serviceType));
        map.put("freq", String.valueOf(channel.frequency));
        map.put("vstd", String.valueOf(channel.videoStd));
        map.put("astd", String.valueOf(channel.audioStd));
        map.put("auto", String.valueOf(channel.isAutoStd));
        map.put("fine", String.valueOf(channel.fineTune));
        String output = MapUtil.mapToString(map);
        values.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA, output);

        Uri uri = resolver.insert(TvContract.Channels.CONTENT_URI, values);
        if (!TextUtils.isEmpty(channel.logoUrl))
        {
            logos.put(TvContract.buildChannelLogoUri(uri), channel.logoUrl);
        }
        if (!logos.isEmpty())
        {
            new InsertLogosTask(context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, logos);
        }
    }

    // If a channel exists, update it. If not, insert a new one.
    public static void updateOrinsertAtvChannel(Context context, String inputId, ChannelInfo channel)
    {
        Uri channelsUri = TvContract.buildChannelsUriForInput(inputId);
        ContentResolver resolver = context.getContentResolver();
        String[] projection = {Channels._ID, Channels.COLUMN_INTERNAL_PROVIDER_DATA};
        Cursor cursor = null;
        try
        {
            cursor = resolver.query(channelsUri, projection, null, null, null);
            while (cursor != null && cursor.moveToNext())
            {
                long rowId = cursor.getLong(0);
                Map<String, String> parsedMap = parseInternalProviderData(cursor.getString(1));
                int frequency = Integer.parseInt(parsedMap.get("freq"));
                if (frequency == channel.frequency)
                {
                    ContentValues values = new ContentValues();
                    values.put(Channels.COLUMN_INPUT_ID, inputId);
                    Map<Uri, String> logos = new HashMap<Uri, String>();
                    values.put(Channels.COLUMN_DISPLAY_NUMBER, channel.number);
                    values.put(Channels.COLUMN_DISPLAY_NAME, channel.name);
                    values.put(Channels.COLUMN_TYPE, Channels.TYPE_PAL);// TODO: channel.type -> COLUMN_TYPE
                                                                        // (PAL/NTSC/SECAM)?
                    if (channel.serviceType == 1)
                    {
                        values.put(Channels.COLUMN_SERVICE_TYPE, Channels.SERVICE_TYPE_AUDIO_VIDEO);
                    }
                    else if (channel.serviceType == 2)
                    {
                        values.put(Channels.COLUMN_SERVICE_TYPE, Channels.SERVICE_TYPE_AUDIO);
                    }
                    Map<String, String> map = new HashMap<String, String>();
                    map.put("type", String.valueOf(channel.type));
                    map.put("stype", String.valueOf(channel.serviceType));
                    map.put("freq", String.valueOf(channel.frequency));
                    map.put("vstd", String.valueOf(channel.videoStd));
                    map.put("astd", String.valueOf(channel.audioStd));
                    map.put("auto", String.valueOf(channel.isAutoStd));
                    map.put("fine", String.valueOf(channel.fineTune));
                    String output = MapUtil.mapToString(map);
                    values.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA, output);
                    Uri uri = TvContract.buildChannelUri(rowId);
                    resolver.update(uri, values, null, null);
                    if (!TextUtils.isEmpty(channel.logoUrl))
                    {
                        logos.put(TvContract.buildChannelLogoUri(uri), channel.logoUrl);
                    }
                    if (!logos.isEmpty())
                    {
                        new InsertLogosTask(context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, logos);
                    }
                    return;
                }
            }
        }
        catch (Exception e)
        {

        }
        finally
        {
            if (cursor != null)
            {
                cursor.close();
            }
        }
        insertAtvChannel(context, inputId, channel);
    }

    private static String getVideoFormat(int videoHeight)
    {
        return VIDEO_HEIGHT_TO_FORMAT_MAP.get(videoHeight);
    }

    private static String getChannelType(int mode)
    {
        return CHANNEL_MODE_TO_TYPE_MAP.get(mode);
    }

    private static int getChannelType(String type)
    {
        return CHANNEL_TYPE_TO_MODE_MAP.get(type);
    }

    public static LongSparseArray<ChannelInfo> buildChannelMap(ContentResolver resolver,
		String inputId, List<ChannelInfo> channels) {
		Uri uri = TvContract.buildChannelsUriForInput(inputId);
		String[] projection = {
			TvContract.Channels._ID,
			TvContract.Channels.COLUMN_DISPLAY_NUMBER
		};

		LongSparseArray<ChannelInfo> channelMap = new LongSparseArray<>();
		Cursor cursor = null;
		try {
			cursor = resolver.query(uri, projection, null, null, null);
			if (cursor == null || cursor.getCount() == 0) {
				return null;
			}

			while (cursor.moveToNext()) {
				long channelId = cursor.getLong(0);
				String channelNumber = cursor.getString(1);
				channelMap.put(channelId, getChannelByNumber(channelNumber, channels));
			}
		} catch (Exception e) {
			Log.d(TAG, "Content provider query: " + e.getStackTrace());
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return channelMap;
	}

    public static ChannelInfo getChannelInfoDTV(ContentResolver resolver, Uri channelUri)
    {
        Uri uri = channelUri;
        String[] projection = {Channels.COLUMN_DISPLAY_NAME, Channels.COLUMN_ORIGINAL_NETWORK_ID, Channels.COLUMN_TRANSPORT_STREAM_ID,
                Channels.COLUMN_SERVICE_ID, Channels.COLUMN_SERVICE_TYPE, Channels.COLUMN_INTERNAL_PROVIDER_DATA, Channels.COLUMN_DISPLAY_NUMBER,
                Channels.COLUMN_TYPE};
        Cursor cursor = null;
        ChannelInfo info = null;
        try
        {
            cursor = resolver.query(uri, projection, null, null, null);
            cursor.moveToNext();
            {
                String name = cursor.getString(0);
                int originalNetworkId = cursor.getInt(1);
                int transportStreamId = cursor.getInt(2);
                int serviceId = cursor.getInt(3);
                int serviceType = cursor.getInt(4);
                Map<String, String> parsedMap = parseInternalProviderData(cursor.getString(5));
                String[] aidStrings = parsedMap.get("aids").replace("[", "").replace("]", "").split(", ");
                String[] afmtStrings = parsedMap.get("afmts").replace("[", "").replace("]", "").split(", ");
                int number = cursor.getInt(6);
                int[] aids = new int[aidStrings.length];
                int[] afmts = new int[afmtStrings.length];
                for (int i = 0; i < aidStrings.length; i++)
                {
                    aids[i] = Integer.parseInt(aidStrings[i]);
                    afmts[i] = Integer.parseInt(afmtStrings[i]);
                }
                info = new ChannelInfo(String.valueOf(number), name, null, originalNetworkId, transportStreamId, serviceId, 0, 0,
                        getChannelType(cursor.getString(7)), Integer.parseInt(parsedMap.get("type")), Integer.parseInt(parsedMap.get("freq")),
                        Integer.parseInt(parsedMap.get("bw")), Integer.parseInt(parsedMap.get("vid")), Integer.parseInt(parsedMap.get("vfmt")), aids,
                        afmts, Integer.parseInt(parsedMap.get("pcr")), 0, 0, 0, 0);
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, "Failed to get channel info from TvProvider.", e);
        }
        finally
        {
            if (cursor != null)
            {
                cursor.close();
            }
        }
        return info;
    }

    public static ChannelInfo getChannelInfoATV(ContentResolver resolver, Uri channelUri)
    {

        // TODO: build atv's channelinfo which player needed.

        Uri uri = channelUri;
        String[] projection = {Channels.COLUMN_DISPLAY_NAME, Channels.COLUMN_SERVICE_TYPE, Channels.COLUMN_INTERNAL_PROVIDER_DATA,
                Channels.COLUMN_DISPLAY_NUMBER,};
        Cursor cursor = null;
        ChannelInfo info = null;
        try
        {
            cursor = resolver.query(uri, projection, null, null, null);
            cursor.moveToNext();
            {
                String name = cursor.getString(0);
                int serviceType = cursor.getInt(1);
                Map<String, String> parsedMap = parseInternalProviderData(cursor.getString(2));
                String number = cursor.getString(3);
                info = new ChannelInfo(number, name, null, 0, 0, 0, 0, 0, 0, Integer.parseInt(parsedMap.get("stype")), Integer.parseInt(parsedMap
                        .get("freq")),
                        0,// bandwidth
                        0,// videoPID
                        0,// videoFormat,
                        null,// audioPIDs[],
                        null,// audioFormats[],
                        0,// pcrPID,
                        Integer.parseInt(parsedMap.get("vstd")), Integer.parseInt(parsedMap.get("astd")), Integer.parseInt(parsedMap.get("auto")),
                        Integer.parseInt(parsedMap.get("fine")));
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, "Failed to get channel info from TvProvider.", e);
        }
        finally
        {
            if (cursor != null)
            {
                cursor.close();
            }
        }
        return info;
    }

    public static Map<String, String> parseInternalProviderData(String internalData)
    {
        return MapUtil.stringToMap(internalData);
    }

    public static void insertUrl(Context context, Uri contentUri, URL sourceUrl)
    {
        if (DEBUG)
        {
            Log.d(TAG, "Inserting " + sourceUrl + " to " + contentUri);
        }
        InputStream is = null;
        OutputStream os = null;
        try
        {
            is = sourceUrl.openStream();
            os = context.getContentResolver().openOutputStream(contentUri);
            copy(is, os);
        }
        catch (IOException ioe)
        {
            Log.e(TAG, "Failed to write " + sourceUrl + "  to " + contentUri, ioe);
        }
        finally
        {
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (IOException e)
                {
                    // Ignore exception.
                }
            }
            if (os != null)
            {
                try
                {
                    os.close();
                }
                catch (IOException e)
                {
                    // Ignore exception.
                }
            }
        }
    }

    public static void copy(InputStream is, OutputStream os) throws IOException
    {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1)
        {
            os.write(buffer, 0, len);
        }
    }

    public static String getServiceNameFromInputId(Context context, String inputId)
    {
        TvInputManager tim = (TvInputManager) context.getSystemService(Context.TV_INPUT_SERVICE);
        for (TvInputInfo info : tim.getTvInputList())
        {
            if (info.getId().equals(inputId))
            {
                return info.getServiceInfo().name;
            }
        }
        return null;
    }

    public static TvContentRating[] stringToContentRatings(String commaSeparatedRatings)
    {
        if (TextUtils.isEmpty(commaSeparatedRatings))
        {
            return null;
        }
        String[] ratings = commaSeparatedRatings.split("\\s*,\\s*");
        TvContentRating[] contentRatings = new TvContentRating[ratings.length];
        for (int i = 0; i < contentRatings.length; ++i)
        {
            contentRatings[i] = TvContentRating.unflattenFromString(ratings[i]);
        }
        return contentRatings;
    }

    public static String contentRatingsToString(TvContentRating[] contentRatings)
    {
        if (contentRatings == null || contentRatings.length == 0)
        {
            return null;
        }
        final String DELIMITER = ",";
        StringBuilder ratings = new StringBuilder(contentRatings[0].flattenToString());
        for (int i = 1; i < contentRatings.length; ++i)
        {
            ratings.append(DELIMITER);
            ratings.append(contentRatings[i].flattenToString());
        }
        return ratings.toString();
    }

    private static ChannelInfo getChannelByNumber(String channelNumber, List<ChannelInfo> channels)
    {
        for (ChannelInfo info : channels)
        {
            if (info.number.equals(channelNumber))
            {
                return info;
            }
        }
        throw new IllegalArgumentException("Unknown channel: " + channelNumber);
    }

    private TvContractUtils()
    {
    }

    public static class InsertLogosTask extends AsyncTask<Map<Uri, String>, Void, Void>
    {
        private final Context mContext;

        InsertLogosTask(Context context)
        {
            mContext = context;
        }

        @Override
        public Void doInBackground(Map<Uri, String>... logosList)
        {
            for (Map<Uri, String> logos : logosList)
            {
                for (Uri uri : logos.keySet())
                {
                    try
                    {
                        insertUrl(mContext, uri, new URL(logos.get(uri)));
                    }
                    catch (MalformedURLException e)
                    {
                        Log.e(TAG, "Can't load " + logos.get(uri), e);
                    }
                }
            }
            return null;
        }
    }
}

package com.droidlogic.tvinput.settings;

import android.app.Fragment;
import android.os.Bundle;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;
import android.widget.ListView;
import android.widget.TextView;

import com.android.internal.util.XmlUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.droidlogic.tvinput.R;

public class ContentFragment extends Fragment {
    private static final String TAG = "ContentFragment";

    interface XmlReaderListener {
        void handleRequestedNode(Context context, XmlResourceParser parser, AttributeSet attrs)
                throws org.xmlpull.v1.XmlPullParserException, IOException;
    }

    static class XmlReader {
        private final Context mContext;
        private final int mXmlResource;
        private final String mRootNodeName;
        private final String mNodeNameRequested;
        private final XmlReaderListener mListener;

        XmlReader(Context context, int xmlResource, String rootNodeName, String nodeNameRequested,
                XmlReaderListener listener) {
            mContext = context;
            mXmlResource = xmlResource;
            mRootNodeName = rootNodeName;
            mNodeNameRequested = nodeNameRequested;
            mListener = listener;
        }

        void read() {
            XmlResourceParser parser = null;
            try {
                parser = mContext.getResources().getXml(mXmlResource);
                AttributeSet attrs = Xml.asAttributeSet(parser);

                int type;
                while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                        && type != XmlPullParser.START_TAG) {
                    // Parse next until start tag is found
                }

                String nodeName = parser.getName();
                if (!mRootNodeName.equals(nodeName)) {
                    throw new RuntimeException("XML document must start with <" + mRootNodeName
                            + "> tag; found" + nodeName + " at " + parser.getPositionDescription());
                }

                Bundle curBundle = null;

                final int outerDepth = parser.getDepth();
                while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                        && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                    if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                        continue;
                    }

                    nodeName = parser.getName();
                    if (mNodeNameRequested.equals(nodeName)) {
                        mListener.handleRequestedNode(mContext, parser, attrs);
                    } else {
                        XmlUtils.skipCurrentTag(parser);
                    }
                }

            } catch (XmlPullParserException e) {
                throw new RuntimeException("Error parsing headers", e);
            } catch (IOException e) {
                throw new RuntimeException("Error parsing headers", e);
            } finally {
                if (parser != null)
                    parser.close();
            }
        }
    }

    public static final String ITEM_NAME = "item_name";
    public static final String ITEM_STATUS = "item_status";

    private Context mContext;
    private int mContentList;
    ArrayList<HashMap<String, Object>> listItem = null;
    private TextView content_title = null;
    private ContentListView content_list = null;

    ContentFragment(int xmlList) {
        mContentList = xmlList;
        listItem = new ArrayList<HashMap<String,Object>>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getActivity();
        View view = (View)inflater.inflate(R.layout.layout_content, container, false);
        content_title = (TextView)view.findViewById(R.id.content_title);
        content_list = (ContentListView)view.findViewById(R.id.content_list);

        listItem.clear();
        new XmlReader(mContext, mContentList, "PreferenceScreen", "Preference", new PreferenceXmlReaderListener()).read();

        Log.d(TAG, "@@@@@@@@@@@@@ size=" + listItem.size());
        ContentAdapter mContentAdapter = new ContentAdapter(mContext, listItem);
        content_list.setAdapter(mContentAdapter);

        return view;
    }

    private class PreferenceXmlReaderListener implements XmlReaderListener {
        PreferenceXmlReaderListener() {
        }

        @Override
        public void handleRequestedNode(Context context, XmlResourceParser parser,
                AttributeSet attrs) throws XmlPullParserException, IOException {
            TypedArray sa = context.getResources().obtainAttributes(attrs,
                    com.android.internal.R.styleable.Preference);

            String key = getStringFromTypedArray(sa,
                    com.android.internal.R.styleable.Preference_key);
            String title = getStringFromTypedArray(sa,
                    com.android.internal.R.styleable.Preference_title);
            sa.recycle();

            //Log.d(TAG, "@@@@@@@@@@@@@@@@ key=" + key + "  title=" + title);
            SettingsManager sm = ((TvSettingsActivity)mContext).getSettingsManager();
            if (key.equals(SettingsManager.KEY_CONTENT_TITLE)) {
                sm.setTag(title);
                content_title.setText(title);
            } else {
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put(ITEM_NAME, title);
                map.put(ITEM_STATUS, sm.getStatus(key));
                listItem.add(map);
            }
        }
    }

    private String getStringFromTypedArray(TypedArray sa, int resourceId) {
        String value = null;
        TypedValue tv = sa.peekValue(resourceId);
        if (tv != null && tv.type == TypedValue.TYPE_STRING) {
            if (tv.resourceId != 0) {
                value = mContext.getString(tv.resourceId);
            } else {
                value = tv.string.toString();
            }
        }
        return value;
    }

    public ArrayList<HashMap<String, Object>> getContentList () {
        return listItem;
    }
}

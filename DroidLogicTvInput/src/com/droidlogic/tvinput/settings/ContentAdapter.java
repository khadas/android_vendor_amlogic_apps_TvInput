package com.droidlogic.tvinput.settings;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import com.droidlogic.tvinput.R;

public class ContentAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    ArrayList<HashMap<String, Object>> mlistItem = null;

    public ContentAdapter(Context context, ArrayList<HashMap<String, Object>> list) {
        this.mInflater = LayoutInflater.from(context);
        mlistItem = list;
    }

    @Override
    public int getCount() {
        return mlistItem.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.layout_content_item, null);
            holder = new ViewHolder();

            holder.name = (TextView) convertView.findViewById(R.id.item_name);
            holder.status = (TextView) convertView.findViewById(R.id.item_status);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder)convertView.getTag();
        }

        holder.name.setText(mlistItem.get(position).get(ContentFragment.ITEM_NAME).toString());
        Object status = mlistItem.get(position).get(ContentFragment.ITEM_STATUS);
        if (status != null)
            holder.status.setText(status.toString());
        else
            holder.status.setText("");
        return convertView;
    }

    private class ViewHolder{
        public TextView name;
        public TextView status;
    }
}

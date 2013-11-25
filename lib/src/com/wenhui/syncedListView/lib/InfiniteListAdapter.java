package com.wenhui.syncedListView.lib;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class InfiniteListAdapter extends BaseAdapter {

	public abstract int getItemCount();
	public abstract Object getItemAt(int position);
	public abstract View getItemView(int position, View convertView, ViewGroup parent);
	
	@Override
	final public int getCount() {
        if( getItemCount() <= 0 ) {
            return 0;
        }
		return Integer.MAX_VALUE;
	}

	@Override
	final public Object getItem(int position) {
		int newPosition = getRealItemPosition(position);
		return getItemAt(newPosition);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	final public View getView(int position, View convertView, ViewGroup parent) {
		int newPosition = getRealItemPosition(position);
		return getItemView(newPosition, convertView, parent);
	}
	
	private int getRealItemPosition(int position){
		if( getItemCount() == 0 ){ return 0; }
		return position % getItemCount();
	}

}

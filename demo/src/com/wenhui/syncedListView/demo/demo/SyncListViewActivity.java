package com.wenhui.syncedListView.demo.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

public class SyncListViewActivity extends FragmentActivity {

	private static final String TAG = "SyncListViewActivity";
	
	public static final void launch(Activity context) {
		Intent i = new Intent(context, SyncListViewActivity.class);
		context.startActivity(i);
	}
	
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);

		SyncListViewContainerFragment fragment;
		if (getSupportFragmentManager().findFragmentByTag(TAG) == null) {
			final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			fragment = SyncListViewContainerFragment.newInstance();
			ft.add(android.R.id.content, fragment, TAG);
			ft.commit();
		}
	}
	

}

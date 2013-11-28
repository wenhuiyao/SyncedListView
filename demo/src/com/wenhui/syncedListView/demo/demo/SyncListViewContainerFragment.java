package com.wenhui.syncedListView.demo.demo;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.wenhui.syncedListView.lib.InfiniteListAdapter;
import com.wenhui.syncedListView.lib.SyncedListLayout;

import java.util.HashSet;

public class SyncListViewContainerFragment extends Fragment{
	
    private SyncedListLayout mLayout;
    private MenuItem mAnimMenu;
	private Toast mToast;

	public static SyncListViewContainerFragment newInstance(){
		SyncListViewContainerFragment frag = new SyncListViewContainerFragment();
		return frag;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mToast = Toast.makeText(getActivity(), "", Toast.LENGTH_SHORT);
        setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View root = inflater.inflate(R.layout.sync_list_fragment, null);
        mLayout = (SyncedListLayout)root;
		final ListView lvLeft = (ListView)root.findViewById(R.id.list_view_left);
		final ListView lvRight = (ListView)root.findViewById(R.id.list_view_right);
		int ivHeightLeft = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_left_height);
		int ivHeightRight = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_right_height);

		final ImageAdapter leftAdapter = new ImageAdapter(getActivity(), ivHeightLeft, Images.imageLeftThumbUrls);
		final ImageAdapter rightAdapter = new ImageAdapter(getActivity(), ivHeightRight, Images.imageRightThumbUrls);

        lvLeft.setAdapter(leftAdapter);
        lvRight.setAdapter(rightAdapter);

		lvLeft.setOnItemClickListener(mLeftListClickListener);
		lvRight.setOnItemClickListener(mRightListClickListener);
		
		lvLeft.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			
			@Override
			public void onGlobalLayout() {
				removeGlobalLayoutListenerWrapper(lvLeft.getViewTreeObserver(),this);
                lvLeft.setSelection(200000);
			}
		});
		
		lvRight.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			
			@Override
			public void onGlobalLayout() {
                removeGlobalLayoutListenerWrapper(lvRight.getViewTreeObserver(),this);
                int velocity = getResources().getDimensionPixelSize(R.dimen.animation_velocity);
                lvRight.setSelection(200000);
                mLayout.setAnimationVelocity(velocity);
                mLayout.startAnimation(100l);


			}
		});

		return root;
	}

    private void removeGlobalLayoutListenerWrapper(ViewTreeObserver observer, OnGlobalLayoutListener listener){
        if(Build.VERSION.SDK_INT >= 16 ){
            observer.removeOnGlobalLayoutListener(listener);
        } else {
            observer.removeGlobalOnLayoutListener(listener);
        }
    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

    @Override
    public void onResume() {
        super.onResume();
        mLayout.startAnimation(10l);
    }

    @Override
    public void onPause() {
        super.onPause();
        mLayout.stopAnimation();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        String title = (mLayout.isAnimating()) ? "Stop anim" : "Start anim";
        mAnimMenu = menu.add(Menu.NONE, R.id.animation, 0, title);
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB  ){
            mAnimMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch( item.getItemId() ){
            case  R.id.animation:
                if( mLayout.isAnimating() ){
                    mLayout.stopAnimation();
                    mAnimMenu.setTitle("Start anim");
                } else {
                    mLayout.startAnimation(100l);
                    mAnimMenu.setTitle("Stop anim");

                }
                return true;


        }
        return super.onOptionsItemSelected(item);
    }

    private OnItemClickListener mLeftListClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int position,
				long arg3) {
            position %= Images.imageLeftThumbUrls.length;
			mToast.setText("Left list item " + position  + " click");
			mToast.show();
			
		}
		
	};
	
	private OnItemClickListener mRightListClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int position,
				long arg3) {
            position %= Images.imageRightThumbUrls.length;
			mToast.setText("Right list item " + position + " click");
			mToast.show();
			
		}
		
	};
	
	private static abstract class ImageBaseAdapter<T> extends InfiniteListAdapter implements
            OnClickListener {

		private T[] mData;
		private Context mContext;
		private android.widget.AbsListView.LayoutParams mImageViewLayoutParams;

		public ImageBaseAdapter(Context context, int imageViewHeight, T[] data) {
			mImageViewLayoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT,
					imageViewHeight);
			mData = data;
			mContext = context;
		}

		@Override
		public int getItemCount() {
			if (mData == null) {
				return 0;
			}
			return mData.length;
		}
		
		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public T getItemAt(int position) {
			if( mData == null ){ return null; }
			return mData[position%mData.length];
		}

		@Override
		public View getItemView(int position, View convertView, ViewGroup parent) {
			ImageView imageView;
			if (convertView == null) { // if it's not recycled, instantiate and
				// initialize
				imageView = new ImageView(mContext);
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				imageView.setLayoutParams(mImageViewLayoutParams);
			} else { 
				imageView = (ImageView) convertView;
			}
			
			T photo = getItemAt(position);
			if( photo == null ){
				imageView.setImageDrawable(null);
			} else {
				Picasso.with(mContext).load(getImageUrl(photo)).into(imageView);


			}
			imageView.setTag(photo);
			return imageView;
		}

		protected abstract String getImageUrl(T data);

	}
	
	private class ImageAdapter extends ImageBaseAdapter<String>{

		public ImageAdapter(Context context, int imageViewHeight, String[] data) {
			super(context, imageViewHeight, data);
		}

		@Override
		protected String getImageUrl(String data) {
			return data;
		}

		@Override
		public void onClick(View v) {
			Toast.makeText(getActivity(), v.getTag().toString(), Toast.LENGTH_SHORT).show();
		}
		
	}

}

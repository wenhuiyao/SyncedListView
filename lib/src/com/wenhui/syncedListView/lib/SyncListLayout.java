package com.wenhui.syncedListView.lib;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.os.Build;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Scroller;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Author: wyao
 */
public class SyncListLayout extends LinearLayout implements Runnable {

	private static final String TAG = "SyncListLayout";

    private static final int DEFAULT_SCROLL_ANIMATION_DURATION = 50;

	private static final int TOUCH_MODE_REST = 0;
	private static final int TOUCH_MODE_SCROLL = 1;
	private static final int TOUCH_MODE_FLING = 2;

    private ListView mListViewLeft;
    private ListView mListViewRight;
    private GestureDetectorCompat gestureDetector;
    private int mLastFlingY = 0;
    private float mRightScrollFactor = 0.6f;
    private float mLeftScrollFactor = 1f;
    private MotionEvent mDownEvent;
    private boolean mAnimating=false;
	private float distance=0;
	private int mTouchMode = TOUCH_MODE_REST;
    private boolean mRequestStopAnim;
    private Runnable mAnimationRunnable;
    private int mAnimationDuration = DEFAULT_SCROLL_ANIMATION_DURATION;
    private int mAnimationDistanceLeft = 0, mAnimationDistanceRight = 0;
    private int mLeftListId=0, mRightListId=0;

    // OverScroller doesn't give us the duration of a fling, bummer
    private Scroller mScroller;

    public SyncListLayout(Context context) {
        super(context);
        init(context, null);
    }

    public SyncListLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public SyncListLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs ) {

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.SyncListLayout);
        try{
            mLeftListId = a.getResourceId(R.styleable.SyncListLayout_left_id, 0);
            mRightListId = a.getResourceId(R.styleable.SyncListLayout_right_id, 0);
            mLeftScrollFactor = a.getFloat(R.styleable.SyncListLayout_left_scroll_factor, 1f);
            mRightScrollFactor = a.getFloat(R.styleable.SyncListLayout_right_scroll_factor, 1f);

        }finally {
            a.recycle();
        }

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        gestureDetector = new GestureDetectorCompat(context, gestureListener);
        mScroller = new Scroller(context);

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ){
            mAnimationRunnable = new AnimationRunnableJb();
            mAnimationDistanceLeft = 3;
            mAnimationDistanceRight = 2;
        } else {
            mAnimationRunnable = new AnimationRunnablePreJb();
            mAnimationDistanceLeft = 2;
            mAnimationDistanceRight = 1;
        }

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();


    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mAnimationRunnable);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mListViewLeft = (ListView) findViewById(mLeftListId);
        mListViewRight = (ListView) findViewById(mRightListId);

        if( mListViewRight == null || mListViewLeft == null ){
            throw new IllegalStateException("Either left list or right list cannot be null");
        }

    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
    	return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	int action = MotionEventCompat.getActionMasked(event);
    	switch( action ){
    	case MotionEvent.ACTION_DOWN:
    	case MotionEvent.ACTION_UP:
    		if( mAnimating ){
    			break;
    		}
    		long delay;
    		switch( mTouchMode ){
    		case TOUCH_MODE_SCROLL:
    			delay = 300l;
    			break;
            case TOUCH_MODE_FLING:
                delay = mScroller.getDuration() + 100l;
                break;
    		default:
    			delay = 50l;
    		}
    		startAnimationInternal(delay);
    		break;
    	}
    	return gestureDetector.onTouchEvent(event);
    }
    
	private void dispatchTouchToList(final MotionEvent e){
		if( mDownEvent == null ){ return; }

        post(new Runnable() {
            @Override
            public void run() {
                int leftListWidth = mListViewLeft.getWidth();

                if ( mDownEvent.getX() <= mListViewLeft.getWidth() ){
                    mListViewLeft.dispatchTouchEvent(mDownEvent);
                    mListViewLeft.dispatchTouchEvent(e);
                } else {
                    // For some reason, this will only recognize x of left list
                    mDownEvent.offsetLocation(-leftListWidth, 0f);
                    e.offsetLocation(-leftListWidth, 0f);
                    mListViewRight.dispatchTouchEvent(mDownEvent);
                    mListViewRight.dispatchTouchEvent(e);
                }

                mDownEvent.recycle();
                mDownEvent = null;
            }
        });

	}

    @Override
    public void computeScroll() {
        if ( mScroller.computeScrollOffset() ){
            int oldY = mLastFlingY;
            int curY = mScroller.getCurrY();

            if( oldY != curY ){
                distance = curY - oldY;
                post(this);
            }
            
            mLastFlingY = curY;
            return;
        }
    }

    public void setLeftListView(ListView left){
        this.mListViewLeft = left;
    }

    public void setRightListView(ListView right){
        this.mListViewRight = right;
    }

    public boolean isAnimating() {
        return mAnimating;
    }

    public void startAnimation(long delay) {
        mRequestStopAnim = false;
        startAnimationInternal(delay);
    }

    private void startAnimationInternal(long delay){
        if(mRequestStopAnim ){ return; }

        if (mListViewLeft == null || mListViewRight == null) {
            postDelayed(mAnimationRunnable, 1000l);
            return;
        }

        if( isAnimating() ){
            return;
        }

        mAnimating = true;
        postDelayed(mAnimationRunnable, delay);
    }

    public void stopAnimation(){
        mRequestStopAnim = true;
        stopAnimationInternal();
    }

    private void stopAnimationInternal(){
    	mAnimating = false;
		removeCallbacks(mAnimationRunnable);
    }


    public void setLeftScrollFactor(float factor){
        this.mLeftScrollFactor = factor;
    }

    public void setRightScrollFactor(float factor){
        this.mRightScrollFactor = factor;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void setAnimationDuration(int duration){
        this.mAnimationDuration = duration;
    }

    public void setAnimationDistanceLeft(int distance){
        this.mAnimationDistanceLeft = distance;
    }

    public void setAnimationDistanceRight(int distance){
        this.mAnimationDistanceRight = distance;
    }

	@Override
	public void run() {
        scrollListtBy(mListViewRight, (int)( distance * mRightScrollFactor ) );
        scrollListtBy(mListViewLeft, (int)( distance * mLeftScrollFactor ) );
	}

    private void scrollListtBy(ListView target, int deltaY) {
        final int firstPosition = target.getFirstVisiblePosition();
        if (firstPosition == ListView.INVALID_POSITION) {
            return;
        }

        final View firstView = target.getChildAt(0);
        if( firstView == null ){ return; }

        final int newTop = firstView.getTop() - deltaY;
        target.setSelectionFromTop(firstPosition, newTop);
    }

    private GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent e) {
			mScroller.forceFinished(true);	
			if( mDownEvent != null ){
				mDownEvent.recycle();
			}
			mDownEvent = MotionEvent.obtain(e);
			removeCallbacks(SyncListLayout.this);
			stopAnimationInternal();
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        	mTouchMode = TOUCH_MODE_FLING;
            mLastFlingY = (int) mListViewRight.getScrollY();
            mScroller.fling(0, mLastFlingY, 0, (int)-velocityY, Integer.MIN_VALUE, Integer.MAX_VALUE,
            		Integer.MIN_VALUE, Integer.MAX_VALUE);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        	mTouchMode = TOUCH_MODE_SCROLL;
        	distance = distanceY;
        	post(SyncListLayout.this);
            return true;
        }

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			dispatchTouchToList(e);
			return false;
		}

    };

    private class AnimationRunnableJb implements Runnable {
        private Method method;

        @Override
        public void run() {
            invokeListViewSmoothScrollBy(mListViewLeft, mAnimationDistanceLeft, mAnimationDuration);
            invokeListViewSmoothScrollBy(mListViewRight, mAnimationDistanceRight, mAnimationDuration );
            postDelayed(this, mAnimationDuration - 5l);
        }

        private void invokeListViewSmoothScrollBy(ListView lv, int distance, int duration){
            //TODO: any alternative? to smoothly (linearly) scroll listview by distance
            try {
                if (method == null) {
                    method = lv.getClass().getSuperclass().getDeclaredMethod("smoothScrollBy",
                            int.class,
                            int.class, boolean.class);
                    method.setAccessible(true);
                }
                method.invoke(lv, distance, duration, true);
                return;
            } catch (InvocationTargetException e) {
            } catch (IllegalAccessException e) {
            } catch (NoSuchMethodException e) {
            }

            lv.smoothScrollBy(distance, duration);
        }
    }

    private class AnimationRunnablePreJb implements Runnable {
        @Override
        public void run() {
            scrollListtBy(mListViewLeft, mAnimationDistanceLeft);
            scrollListtBy(mListViewRight, mAnimationDistanceRight);
            postDelayed(this, 5l);
        }
    }

}

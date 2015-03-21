/*
 * Copyright 2013 Wenhui Yao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wenhui.syncedListView.lib;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.OverScroller;
import android.widget.Scroller;


public class SyncedListLayout extends LinearLayout {

	private static final String TAG = "SyncedListLayout";

    private static final int TOUCH_MODE_FLING = 1;
    private static final int TOUCH_MODE_SCROLL = 2;
    private static final int TOUCH_MODE_REST = 0;

    private static final int DEFAULT_SCROLL_ANIMATION_DURATION = 60*1000; // MINUTE
    private static final int DEFAULT_VELOCITY = 1500;  // PER MINUTE
    private static final long DEFAULT_ANIMATION_DELAY = 10L;

    private ListView mListViewLeft;
    private ListView mListViewRight;
    private GestureDetectorCompat gestureDetector;
    private int mLastFlingY = 0;
    private float mRightScrollFactor = 0.8f;
    private float mLeftScrollFactor = 1.4f;
    private MotionEvent mDownEvent;
    private boolean mAnimating=false;
    private boolean mRequestStopAnim = false;
    private FlingRunnable mFlingRunnable;
    private AnimationRunnable mAnimationRunnable;
    private int mAnimationDuration = DEFAULT_SCROLL_ANIMATION_DURATION;
    private int mAnimationVelocity = DEFAULT_VELOCITY;
    private int mLeftListId=0, mRightListId=0;
    private float mLeftAnimationScrollFactor = 2f, mRightAnimationScrollFactor=1f;
    private Scroller mScroller;
    private int mTouchMode = TOUCH_MODE_REST;


    public SyncedListLayout(Context context) {
        super(context);
        init(context, null);
    }

    public SyncedListLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public SyncedListLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs ) {

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SyncedListLayout);
        try{
            mLeftListId = a.getResourceId(R.styleable.SyncedListLayout_left_id, 0);
            mRightListId = a.getResourceId(R.styleable.SyncedListLayout_right_id, 0);
            mLeftScrollFactor = a.getFloat(R.styleable.SyncedListLayout_left_scroll_factor, 1f);
            mRightScrollFactor = a.getFloat(R.styleable.SyncedListLayout_right_scroll_factor, 1f);
        }finally {
            a.recycle();
        }

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metric = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metric);
        mAnimationVelocity = (int)(DEFAULT_VELOCITY * metric.density);

        gestureDetector = new GestureDetectorCompat(context, gestureListener);
        mScroller = new Scroller(context);

        mAnimationRunnable = new AnimationRunnable(context);
        mFlingRunnable = new FlingRunnable();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
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
        boolean handle = gestureDetector.onTouchEvent(event);
        int action = MotionEventCompat.getActionMasked(event);
        switch( action ){
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "touch mode " + mTouchMode);
                if( mTouchMode != TOUCH_MODE_FLING ){
                    startAnimationInternal(DEFAULT_ANIMATION_DELAY);
                }

                mTouchMode = TOUCH_MODE_REST;
                break;
        }
        return handle;
    }
    
	private void dispatchTouchToList(final MotionEvent e){
		if( mDownEvent == null ){ return; }

        int leftListWidth = mListViewLeft.getWidth();

        if (mDownEvent.getX() <= mListViewLeft.getWidth()) {
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

    @Override
    public void computeScroll() {
        if ( mScroller.computeScrollOffset() ){

            if( mAnimating ){ stopAnimationInternal(); }

            int curY = mScroller.getCurrY();
            int distance = curY - mLastFlingY;
            mLastFlingY = curY;

            if( distance != 0 ){
                mFlingRunnable.setDistance(distance);
                post(mFlingRunnable);
            }

            if ( mScroller.isFinished() || distance == 0 ) {
                startAnimationInternal(DEFAULT_ANIMATION_DELAY);
            }

        }

    }

    public void setLeftListView(ListView left){
        this.mListViewLeft = left;
    }

    public void setRightListView(ListView right){
        this.mListViewRight = right;
    }

    public void setLeftAnimationScrollFactor(float factor){
        this.mLeftAnimationScrollFactor = factor;
    }

    public void setRightAnimationScrollFactor(float factor){
        this.mRightAnimationScrollFactor= factor;
    }

    public boolean isAnimating() {
        return mAnimating;
    }

    public void startAnimation(long delay) {
        mRequestStopAnim = false;
        startAnimationInternal(delay);
    }

    private void startAnimationInternal(long delay){
        if( mAnimating ){ return; }

        if(mRequestStopAnim ){ return; }

        mAnimating = true;
        postDelayed(mAnimationLaunchRunnable, delay);
    }

    private Runnable mAnimationLaunchRunnable = new Runnable() {
        @Override
        public void run() {
            mAnimationRunnable.startAnimation(mAnimationVelocity, mAnimationDuration);
        }
    };

    public void stopAnimation(){
        mRequestStopAnim = true;
        stopAnimationInternal();
    }

    private void stopAnimationInternal(){
        mAnimating = false;
        removeCallbacks(mAnimationLaunchRunnable);
        mAnimationRunnable.cancel();
		removeCallbacks(mAnimationRunnable);
    }

    public void setLeftScrollFactor(float factor){
        this.mLeftScrollFactor = factor;
    }

    public void setRightScrollFactor(float factor){
        this.mRightScrollFactor = factor;
    }

    /**
     *
     * @param velocity  Distance per second
     */
    public void setAnimationVelocity(int velocity){
        this.mAnimationVelocity = velocity * 60;
    }

    private class FlingRunnable implements Runnable {
        private float distance = 0;

        public void setDistance(float distance){
            this.distance = distance;
        }

        @Override
        public void run() {
            scrollListBy(distance, mLeftScrollFactor, mRightScrollFactor);
        }
    }

    private void scrollListBy(float distance, float leftScrollFactor, float rightScrollFactor){
        scrollListBy(mListViewRight, (int) (distance * rightScrollFactor + 0.5f));
        scrollListBy(mListViewLeft, (int) (distance * leftScrollFactor + 0.5f));
    }

    private void scrollListBy(ListView target, int deltaY) {
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
			removeCallbacks(mFlingRunnable);
			stopAnimationInternal();
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            mTouchMode = TOUCH_MODE_FLING;
            mLastFlingY = mListViewRight.getScrollY();
            mScroller.fling(0, mLastFlingY, 0, (int)-velocityY, Integer.MIN_VALUE, Integer.MAX_VALUE,
            		Integer.MIN_VALUE, Integer.MAX_VALUE);

            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            mTouchMode = TOUCH_MODE_SCROLL;
        	mFlingRunnable.setDistance(distanceY);
        	post(mFlingRunnable);
            return true;
        }

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
            dispatchTouchToList(e);
			return false;
		}

    };

    private class AnimationRunnable implements Runnable {
        private int lastY;
        private final OverScroller scroller;
        private int distance, duration;
        private boolean cancelled = false;

        public AnimationRunnable(Context context){
            scroller = new OverScroller(context, new LinearInterpolator());
        }

        public void startAnimation(int distance, int duration){
            cancelled = false;
            this.distance = distance;
            this.duration = duration;
            lastY = mListViewRight.getScrollY();
            animate();
        }

        private void animate(){
            scroller.startScroll(0, lastY, 0, distance, duration);
            post(this);
        }

        public void cancel(){
            cancelled = true;
            scroller.forceFinished(true);
        }

        @Override
        public void run() {
            if( cancelled ){
                return;
            }

            boolean hasMore = scroller.computeScrollOffset();
            int y = scroller.getCurrY();
            int yDiff = y - lastY;
            if( yDiff != 0 ){
                scrollListBy(yDiff, mLeftAnimationScrollFactor, mRightAnimationScrollFactor);
                lastY = y;
            }

            if( hasMore ){
                post(this);
            } else {
                animate();
            }

        }
    }


}

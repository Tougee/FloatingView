package com.touge.floatingview.floating;

import android.content.Context;
import android.graphics.Outline;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;

import com.google.android.cameraview.CameraView;

public class CircleCameraView extends CameraView {
    private static final int MAX_SCALE_RATIO = 3;

    private float mLastDis;
    private float mScaleRatio;
    private long mLastTouchTime;
    private int mTouchCount;

    public CircleCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init() {
        ViewOutlineProvider vop = new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        };
        setOutlineProvider(vop);
        setClipToOutline(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        switch (event.getActionMasked()) {
//            case MotionEvent.ACTION_DOWN:
//                if (event.getPointerCount() == 1) {
//                    return super.onTouchEvent(event);
//                }
//                mLastTouchTime = System.currentTimeMillis();
//                mTouchCount++;
//                break;
//            case MotionEvent.ACTION_POINTER_DOWN:
//                if (event.getPointerCount() == 2) {
//                    getParent().requestDisallowInterceptTouchEvent(true);
//                    mLastDis = distanceBetweenFingers(event);
//                } else {
//                    getParent().requestDisallowInterceptTouchEvent(false);
//                }
//                break;
//            case MotionEvent.ACTION_MOVE:
//                if (event.getPointerCount() == 2) {
//                    final float currDis = distanceBetweenFingers(event);
//                    final boolean shrink = currDis < mLastDis;
//                    if ((shrink && mScaleRatio >= 1) || (!shrink && mScaleRatio <= MAX_SCALE_RATIO)) {
//                        double currRatio = currDis / mLastDis;
//                        mScaleRatio *= currRatio;
//                        if (mScaleRatio < 1) {
//                            mScaleRatio = 1;
//                        } else if (mScaleRatio > MAX_SCALE_RATIO) {
//                            mScaleRatio = MAX_SCALE_RATIO;
//                        }
//
//                        setScaleX(mScaleRatio);
//                        setScaleY(mScaleRatio);
//
//                        mLastDis = currDis;
//                    }
//                }
//                break;
//            case MotionEvent.ACTION_UP:
//                final long upTime = System.currentTimeMillis();
//                final long diff = upTime - mLastTouchTime;
//                if (diff > 300) {
//                    mTouchCount = 0;
//                }
//                if (mTouchCount > 1) {  // double tap
//                    int facing = getFacing();
//                    setFacing(facing == CameraView.FACING_FRONT ?
//                            CameraView.FACING_BACK : CameraView.FACING_FRONT);
//                    mTouchCount = 0;
//                    mLastTouchTime = 0;
//                    return true;
//                }
//                if (mTouchCount > 2) {
//                    mTouchCount = 0;
//                }
//                break;
//            case MotionEvent.ACTION_CANCEL:
//                mTouchCount = 0;
//                break;
//        }
//        return true;
        return super.onTouchEvent(event);
    }

    private float distanceBetweenFingers(MotionEvent event) {
        float disX = Math.abs(event.getX(0) - event.getX(1));
        float disY = Math.abs(event.getY(0) - event.getY(1));
        return (float) Math.sqrt(disX * disX + disY * disY);
    }
}

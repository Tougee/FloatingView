package com.touge.floatingview.floating;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.touge.floatingview.R;
import com.touge.floatingview.Utils;

public class BubbleLayout implements View.OnTouchListener {
    private static final int MAX_SCALE_RATIO = 2;

    private static final int DOCK_NORMAL = 0;
    private static final int DOCK_RESET = 1;
    private static final int DOCK_BACK = 2;

    private static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();
    private static final int DOUBLE_TAP_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();

    private static final int TAP = 0;

    private WindowViewController mWindowViewController;
    private Context mContext;
    private BubbleView mBubbleView;
    private View mContentView;
    private ValueAnimator mDockAnimator;

    private boolean mIsBubbleAddToWindow;
    private boolean mIsContentAddToWindow;
    private boolean mIsBottomAddToWindow;

    private OnBubbleExitListener mOnBubbleExitListener;
    private OnTapListener mOnTapListener;
    private BubbleHandler mHandler;

    private MotionEvent mCurrentDownEvent;
    private MotionEvent mPreviousUpEvent;

    private float mLastDis;
    private float mScaleRatio;
    private FrameLayout mBottomLayout;
    private boolean mResetMode;

    private int mTouchSlop;
    private float mDoubleTapSlopSquare;
    private boolean mStillDown;
    private boolean mDeferConfirmSingleTap;
    private boolean mIsDoubleTapping;
    private boolean mAlwaysInBiggerTapRegion;

    private PointF mLastTouchPos = new PointF();
    private Point mDisplayPoint = new Point();
    private Point mPreResetPoint = new Point();
    private ImageView mCancelView;

    public BubbleLayout(Context ctx) {
        mContext = ctx;
        mHandler = new BubbleHandler();
        mWindowViewController = new WindowViewController((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE));
        mBubbleView = new BubbleView(ctx);
        mBubbleView.setOnTouchListener(this);
        mContentView = createContentView(ctx);
        ViewConfiguration configuration = ViewConfiguration.get(ctx);
        mTouchSlop = configuration.getScaledTouchSlop();
        final int doubleTapSlop = configuration.getScaledDoubleTapSlop();
        mDoubleTapSlopSquare = doubleTapSlop * doubleTapSlop;
        mWindowViewController.getDisplayPoint(mDisplayPoint);
        setOnTapListener(new OnTapListener() {
            @Override
            public boolean onSingleTap() {
                dockBubble(mResetMode ? DOCK_BACK : DOCK_RESET);
                showContent();
                return true;
            }

            @Override
            public boolean onDoubleTap() {
                mBubbleView.switchCamera();
                return true;
            }
        });

        initBottomLayout(ctx);
        showBubble();
    }

    private void initBottomLayout(Context ctx) {
        mBottomLayout = new FrameLayout(ctx);
        WindowManager.LayoutParams bottomParams = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Utils.dp(ctx, 150));
        bottomParams.gravity = Gravity.BOTTOM;
        mBottomLayout.setLayoutParams(bottomParams);
        mBottomLayout.setBackgroundColor(Color.parseColor("#88000000"));
        mCancelView = new ImageView(ctx);
        mCancelView.setImageResource(R.drawable.ic_cancel_white_24dp);
        FrameLayout.LayoutParams delParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        delParams.gravity = Gravity.CENTER;
        mBottomLayout.addView(mCancelView, delParams);
    }

    private void showBubble() {
        if (mIsBubbleAddToWindow) {
            return;
        }
        mIsBubbleAddToWindow = true;
        mWindowViewController.addView(
                Utils.dp(mContext, 70),
                Utils.dp(mContext, 70),
                true,
                mBubbleView
        );
    }

    private void hideBubble() {
        if (!mIsBubbleAddToWindow) {
            return;
        }
        mIsBubbleAddToWindow = false;
        mWindowViewController.removeView(mBubbleView);
        if (mOnBubbleExitListener != null) {
            mOnBubbleExitListener.onBubbleExit();
        }
    }

    private void showBottom() {
        if (mIsBottomAddToWindow) {
            return;
        }
        mIsBottomAddToWindow = true;
        mWindowViewController.addView(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Utils.dp(mContext, 150),
                false,
                mBottomLayout,
                Gravity.BOTTOM
        );
    }

    private void hideBottom() {
        if (!mIsBottomAddToWindow) {
            return;
        }
        mIsBottomAddToWindow = false;
        mWindowViewController.removeView(mBottomLayout);
    }

    public void showContent() {
        if (mIsContentAddToWindow) {
            return;
        }
        mIsContentAddToWindow = true;
        mWindowViewController.addView(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                false,
                mContentView
        );
    }

    public void hideContent() {
        if (!mIsContentAddToWindow) {
            return;
        }
        mIsContentAddToWindow = false;
        mWindowViewController.removeView(mContentView);
    }

    public void removeFromWindow() {
        if (mIsBubbleAddToWindow) {
            mWindowViewController.removeView(mBubbleView);
            mIsBubbleAddToWindow = false;
        }
        if (mIsContentAddToWindow) {
            mWindowViewController.removeView(mContentView);
            mIsContentAddToWindow = false;
        }
    }

    private View createContentView(Context context) {
        return new View(context);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        boolean handled = false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                boolean hadTapMessage = mHandler.hasMessages(TAP);
                if (hadTapMessage) mHandler.removeMessages(TAP);
                if (mCurrentDownEvent != null && mPreviousUpEvent != null && hadTapMessage
                        && isConsideredDoubleTap(mCurrentDownEvent, mPreviousUpEvent, event)) {
                    mIsDoubleTapping = true;
                    mDockAnimator.cancel();
                    mDockAnimator.removeAllUpdateListeners();
                    if (mOnTapListener != null) {
                        handled = mOnTapListener.onDoubleTap();
                    }
                } else {
                    mHandler.sendEmptyMessageDelayed(TAP, DOUBLE_TAP_TIMEOUT);
                }

                if (mCurrentDownEvent != null) {
                    mCurrentDownEvent.recycle();
                }
                mCurrentDownEvent = MotionEvent.obtain(event);
                mStillDown = true;
                mDeferConfirmSingleTap = false;
                mAlwaysInBiggerTapRegion = true;

                mLastTouchPos.set(event.getRawX(), event.getRawY());
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() == 2) {
                    mLastDis = distanceBetweenFingers(event);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() == 1) {
                    final float dx = event.getRawX() - mLastTouchPos.x;
                    final float dy = event.getRawY() - mLastTouchPos.y;
                    if (Math.abs(dx) > mTouchSlop || Math.abs(dy) > mTouchSlop) {
                        mAlwaysInBiggerTapRegion = false;
                        mHandler.removeMessages(TAP);
                        showBottom();
                        Point windowPoint = mWindowViewController.getViewPosition(mBubbleView);
                        mWindowViewController.safeMoveViewTo(mBubbleView,
                                windowPoint.x + (int) dx,
                                windowPoint.y + (int) dy,
                                mDisplayPoint
                        );
                        mLastTouchPos.set(event.getRawX(), event.getRawY());

                        if (mResetMode) {
                            hideContent();
                        }
                    }
                } else if (event.getPointerCount() == 2) {
                    final float currDis = distanceBetweenFingers(event);
                    final boolean shrink = currDis < mLastDis;
                    if ((shrink && mScaleRatio >= 1) || (!shrink && mScaleRatio <= MAX_SCALE_RATIO)) {
                        mScaleRatio = currDis / mLastDis;
                        if (mScaleRatio < 1) {
                            mScaleRatio = 1;
                        } else if (mScaleRatio > MAX_SCALE_RATIO) {
                            mScaleRatio = MAX_SCALE_RATIO;
                        }

                        mWindowViewController.scaleView(mBubbleView, mScaleRatio);
                        mBubbleView.scale(mScaleRatio);

                        mLastDis = currDis;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                MotionEvent currentUpEvent = MotionEvent.obtain(event);
                mStillDown = false;
                if (mDeferConfirmSingleTap && !mIsDoubleTapping && mAlwaysInBiggerTapRegion && mOnTapListener != null) {
                    handled = mOnTapListener.onSingleTap();
                }

                if (mPreviousUpEvent != null) {
                    mPreviousUpEvent.recycle();
                }
                mPreviousUpEvent = currentUpEvent;

                Point bubblePos = mWindowViewController.getViewPosition(mBubbleView);
                Rect bubbleRect = new Rect(bubblePos.x, bubblePos.y, bubblePos.x + mBubbleView.getWidth(), bubblePos.y + mBubbleView.getHeight());
                Rect cancelRect = new Rect();
                mCancelView.getGlobalVisibleRect(cancelRect);
                final int offset = mDisplayPoint.y - mBottomLayout.getHeight();
                cancelRect.top += offset;
                cancelRect.bottom += offset;
                if (Rect.intersects(bubbleRect, cancelRect)) {
                    hideBubble();
                    hideBottom();
                } else {
                    dockBubble();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                mStillDown = false;
                mAlwaysInBiggerTapRegion = false;
                mHandler.removeMessages(TAP);
                dockBubble();
                break;
        }
        return handled;
    }

    private boolean isConsideredDoubleTap(MotionEvent firstDown, MotionEvent firstUp,
                                          MotionEvent secondDown) {
        if (!mAlwaysInBiggerTapRegion) {
            return false;
        }

        final long deltaTime = secondDown.getEventTime() - firstUp.getEventTime();
        Log.d("@@@", "deltaTime: " + deltaTime);
        if (deltaTime > DOUBLE_TAP_TIMEOUT) {
            return false;
        }

        int deltaX = (int) firstDown.getX() - (int) secondDown.getX();
        int deltaY = (int) firstDown.getY() - (int) secondDown.getY();
        return (deltaX * deltaX + deltaY * deltaY < mDoubleTapSlopSquare);
    }

    private float distanceBetweenFingers(MotionEvent event) {
        float disX = Math.abs(event.getX(0) - event.getX(1));
        float disY = Math.abs(event.getY(0) - event.getY(1));
        return (float) Math.sqrt(disX * disX + disY * disY);
    }

    private void dockBubble() {
        dockBubble(DOCK_NORMAL);
    }

    private void dockBubble(final int dockMode) {
        final Point currWindowPos = mWindowViewController.getViewPosition(mBubbleView);
        final int destX, destY;
        if (dockMode == DOCK_BACK) {
            mResetMode = false;
            destX = mPreResetPoint.x;
            destY = mPreResetPoint.y;
        } else {
            destY = 0;
            boolean isLeft = currWindowPos.x + mBubbleView.getWidth() / 2 < mDisplayPoint.x / 2;
            if (dockMode == DOCK_RESET) {
                mResetMode = true;
                mPreResetPoint = currWindowPos;

                destX = mDisplayPoint.x - mBubbleView.getWidth();
            } else {
                destX = isLeft ? 0 : mDisplayPoint.x - mBubbleView.getWidth();
            }
        }
        mDockAnimator = ValueAnimator.ofFloat(0, 1);
        mDockAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int curX = currWindowPos.x + (int) (animation.getAnimatedFraction() * (destX - currWindowPos.x));
                int curY;
                if (dockMode == DOCK_NORMAL) {
                    curY = currWindowPos.y;
                } else {
                    curY = currWindowPos.y + (int) (animation.getAnimatedFraction() * (destY - currWindowPos.y));
                }
                mWindowViewController.safeMoveViewTo(
                        mBubbleView,
                        curX,
                        curY,
                        mDisplayPoint
                );
            }
        });
        mDockAnimator.start();

        hideBottom();
    }

    private class BubbleHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TAP:
                    // If the user's finger is still down, do not count it as a tap
                    if (mOnTapListener != null) {
                        if (!mStillDown) {
                            mOnTapListener.onSingleTap();
                        } else {
                            mDeferConfirmSingleTap = true;
                        }
                    }
                    break;
            }
        }
    }

    public void setOnTapListener(OnTapListener listener) {
        mOnTapListener = listener;
    }

    public interface OnTapListener {
        boolean onSingleTap();

        boolean onDoubleTap();
    }

    public void setOnBubbleExitListener(OnBubbleExitListener listener) {
        mOnBubbleExitListener = listener;
    }

    public interface OnBubbleExitListener {
        void onBubbleExit();
    }
}
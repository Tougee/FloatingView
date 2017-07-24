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
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.touge.floatingview.R;
import com.touge.floatingview.Utils;

import static com.touge.floatingview.floating.BubbleView.BUBBLE_INITIAL_SIZE;

public class BubbleLayout implements View.OnTouchListener {
  private static final int MAX_SCALE_RATIO = 2;
  private static final float DOCK_PER = 0.25f;

  private static final int DOCK_NORMAL = 0;
  private static final int DOCK_RESET = 1;
  private static final int DOCK_BACK = 2;

  private static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();
  private static final int DOUBLE_TAP_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();

  private static final int TAP = 0;

  private WindowViewController mWindowViewController;
  private Context mContext;

  private BubbleView mBubbleView;
  private FrameLayout mBottomLayout;
  private View mContentView;
  private ImageView mCancelView;

  private ValueAnimator mDockAnimator;

  private OnBubbleExitListener mOnBubbleExitListener;
  private OnTapListener mOnTapListener;
  private BubbleHandler mHandler;

  private MotionEvent mCurrentDownEvent;
  private MotionEvent mPreviousUpEvent;

  private boolean mIsBubbleAddToWindow;
  private boolean mIsContentAddToWindow;
  private boolean mIsBottomAddToWindow;

  private float mLastDis;
  private float mScaleRatio = 1;
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

  public BubbleLayout(final Context ctx) {
    mContext = ctx;
    mHandler = new BubbleHandler();
    mWindowViewController =
        new WindowViewController((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE));
    mBubbleView = new BubbleView(ctx);
    mBubbleView.setOnTouchListener(this);
    mContentView = createContentView(ctx);
    ViewConfiguration configuration = ViewConfiguration.get(ctx);
    mTouchSlop = configuration.getScaledTouchSlop();
    final int doubleTapSlop = configuration.getScaledDoubleTapSlop();
    mDoubleTapSlopSquare = doubleTapSlop * doubleTapSlop;
    mWindowViewController.getDisplayPoint(mDisplayPoint);
    setOnTapListener(new OnTapListener() {
      @Override public boolean onSingleTap() {
        dockBubble(mResetMode ? DOCK_BACK : DOCK_RESET);
        showContent();
//        test scale
//        mScaleRatio += 0.3;
//        mWindowViewController.scaleView(mBubbleView, mScaleRatio, Utils.dp(ctx, BUBBLE_INITIAL_SIZE));
//        mBubbleView.scale(mScaleRatio);
//        WindowManager.LayoutParams params =
//            (WindowManager.LayoutParams) mBubbleView.getLayoutParams();
//        Log.d("@@@", "bubble width:"
//            + mBubbleView.getWidth()
//            + ", height:"
//            + mBubbleView.getHeight()
//            + "   window w:"
//            + params.width
//            + ", h:"
//            + params.height
//            + ", x:"
//            + params.x
//            + ", y:"
//            + params.y
//        );
        return true;
      }

      @Override public boolean onDoubleTap() {
        mBubbleView.switchCamera();
        return true;
      }
    });

    initBottomLayout(ctx);
    showBubble();
  }

  private void initBottomLayout(Context ctx) {
    mBottomLayout = new FrameLayout(ctx);
    WindowManager.LayoutParams bottomParams =
        new WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.dp(ctx, 150));
    bottomParams.gravity = Gravity.BOTTOM;
    mBottomLayout.setLayoutParams(bottomParams);
    mBottomLayout.setBackgroundColor(Color.parseColor("#88000000"));
    mCancelView = new ImageView(ctx);
    mCancelView.setImageResource(R.drawable.ic_cancel_white_24dp);
    FrameLayout.LayoutParams delParams =
        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
    delParams.gravity = Gravity.CENTER;
    mBottomLayout.addView(mCancelView, delParams);
  }

  private void showBubble() {
    if (mIsBubbleAddToWindow) {
      return;
    }
    mIsBubbleAddToWindow = true;
    mWindowViewController.addView(Utils.dp(mContext, BUBBLE_INITIAL_SIZE), Utils.dp(mContext, BUBBLE_INITIAL_SIZE), true,
        mBubbleView);
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
    mWindowViewController.addView(ViewGroup.LayoutParams.MATCH_PARENT, Utils.dp(mContext, 150),
        false, mBottomLayout, Gravity.BOTTOM);
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
    mWindowViewController.addView(WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT, false, mContentView);
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

  @Override public boolean onTouch(View v, MotionEvent event) {
    boolean handled = false;
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        Log.d("@@@", "ACTION_DOWN");
        boolean hadTapMessage = mHandler.hasMessages(TAP);
        if (hadTapMessage) mHandler.removeMessages(TAP);
        if (mCurrentDownEvent != null
            && mPreviousUpEvent != null
            && hadTapMessage
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
        Log.d("@@@", "ACTION_POINTER_DOWN event.getPointerCount()=" + event.getPointerCount());
        if (event.getPointerCount() == 2) {
          mLastDis = distanceBetweenFingers(event);
        }
        break;
      case MotionEvent.ACTION_MOVE:
        Log.d("@@@", "ACTION_MOVE");
        if (event.getPointerCount() == 1) {
          final float dx = event.getRawX() - mLastTouchPos.x;
          final float dy = event.getRawY() - mLastTouchPos.y;
          if (Math.abs(dx) > mTouchSlop || Math.abs(dy) > mTouchSlop) {
            mAlwaysInBiggerTapRegion = false;
            mHandler.removeMessages(TAP);
            showBottom();
            Point windowPoint = mWindowViewController.getViewPosition(mBubbleView);
            mWindowViewController.safeMoveViewTo(mBubbleView, windowPoint.x + (int) dx,
                windowPoint.y + (int) dy, mDisplayPoint);
            mLastTouchPos.set(event.getRawX(), event.getRawY());

            if (mResetMode) {
              hideContent();
            }
          }
        } else if (event.getPointerCount() == 2) {
          final float currDis = distanceBetweenFingers(event);
          Log.d("@@@", "2 finger distance:" + currDis);
          final boolean shrink = currDis < mLastDis;
          if ((shrink && mScaleRatio >= 1) || (!shrink && mScaleRatio <= MAX_SCALE_RATIO)) {
            Log.d("@@@", "move scale ratio: " + mScaleRatio);
            mScaleRatio = currDis / mLastDis;
            if (mScaleRatio < 1) {
              mScaleRatio = 1;
            } else if (mScaleRatio > MAX_SCALE_RATIO) {
              mScaleRatio = MAX_SCALE_RATIO;
            }

            mWindowViewController.scaleView(mBubbleView, mScaleRatio, Utils.dp(mContext, BUBBLE_INITIAL_SIZE));
            mBubbleView.scale(mScaleRatio);

            mLastDis = currDis;
          }
        }
        break;
      case MotionEvent.ACTION_UP:
        Log.d("@@@", "ACTION_UP");
        MotionEvent currentUpEvent = MotionEvent.obtain(event);
        mStillDown = false;
        if (mDeferConfirmSingleTap
            && !mIsDoubleTapping
            && mAlwaysInBiggerTapRegion
            && mOnTapListener != null) {
          handled = mOnTapListener.onSingleTap();
        }

        if (mPreviousUpEvent != null) {
          mPreviousUpEvent.recycle();
        }
        mPreviousUpEvent = currentUpEvent;

        Point bubblePos = mWindowViewController.getViewPosition(mBubbleView);
        Rect bubbleRect = new Rect(bubblePos.x, bubblePos.y, bubblePos.x + mBubbleView.getWidth(),
            bubblePos.y + mBubbleView.getHeight());
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
        Log.d("@@@", "ACTION_CANCEL");
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
    Log.d("@@@", "currWindowPos x: " + currWindowPos.x + ", y: " + currWindowPos.y);
    final int destX, destY;
    if (dockMode == DOCK_BACK) {
      mResetMode = false;
      destX = mPreResetPoint.x;
      destY = mPreResetPoint.y;
    } else {
      boolean isLeft = currWindowPos.x + mBubbleView.getWidth() / 2 < mDisplayPoint.x / 2;
      if (dockMode == DOCK_RESET) {
        mResetMode = true;
        mPreResetPoint = currWindowPos;

        destY = 0;
        destX = mDisplayPoint.x - mBubbleView.getWidth();
      } else {
        destY = (int) -(mBubbleView.getHeight() * DOCK_PER);
        destX = isLeft ? (int) (-mBubbleView.getWidth() * DOCK_PER)
            : mDisplayPoint.x - (int) (mBubbleView.getWidth() * (1 - DOCK_PER));
      }
    }
    Log.d("@@@", "destX: " + destX + ", destY: " + destY);
    mDockAnimator = ValueAnimator.ofFloat(0, 1);
    mDockAnimator.setInterpolator(new OvershootInterpolator());
    mDockAnimator.setDuration(500);
    mDockAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override public void onAnimationUpdate(ValueAnimator animation) {
        int curX =
            currWindowPos.x + (int) (animation.getAnimatedFraction() * (destX - currWindowPos.x));
        int curY;
        if (dockMode == DOCK_NORMAL) {
          curY = currWindowPos.y;
        } else {
          curY =
              currWindowPos.y + (int) (animation.getAnimatedFraction() * (destY - currWindowPos.y));
        }
        Log.d("@@@", "currX: " + curX + ", currY: " + curY);
        mWindowViewController.moveViewTo(mBubbleView, curX, curY);
      }
    });
    mDockAnimator.start();

    hideBottom();
  }

  private class BubbleHandler extends Handler {

    @Override public void handleMessage(Message msg) {
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

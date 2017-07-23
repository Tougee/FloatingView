package com.touge.floatingview.floating;

import android.graphics.PixelFormat;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

public class WindowViewController {

  private WindowManager mWindowManager;

  public WindowViewController(@NonNull WindowManager windowManager) {
    mWindowManager = windowManager;
  }

  public void getDisplayPoint(Point p) {
    mWindowManager.getDefaultDisplay().getSize(p);
  }

  public void addView(int width, int height, boolean isTouchable, View view, int gravity) {
    // If this view is untouchable then add the corresponding flag, otherwise set to zero which
    // won't have any effect on the OR'ing of flags.
    int touchableFlag = isTouchable ? 0 : WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;

    int windowType = /*Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : */WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;

    WindowManager.LayoutParams params = new WindowManager.LayoutParams(width, height, windowType,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            | touchableFlag, PixelFormat.TRANSLUCENT);
    params.gravity = gravity;
    params.x = 0;
    params.y = 0;

    mWindowManager.addView(view, params);
  }

  public void addView(int width, int height, boolean isTouchable, @NonNull View view) {
    addView(width, height, isTouchable, view, Gravity.TOP | Gravity.START);
  }

  public void removeView(@NonNull View view) {
    if (null != view.getParent()) {
      mWindowManager.removeView(view);
    }
  }

  public Point getViewPosition(@NonNull View view) {
    WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
    return new Point(params.x, params.y);
  }

  public void scaleView(View view, float ratio) {
    WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
    params.width *= ratio;
    params.height *= ratio;
    mWindowManager.updateViewLayout(view, params);
  }

  public void safeMoveViewTo(View view, int x, int y, Point displayPoint) {
    if (x < 0) {
      x = 0;
    } else if (x > displayPoint.x - view.getWidth()) {
      x = displayPoint.x - view.getWidth();
    }
    if (y < 0) {
      y = 0;
    } else if (y > displayPoint.y - view.getHeight()) {
      y = displayPoint.y - view.getHeight();
    }
    moveViewTo(view, x, y);
  }

  public void moveViewTo(View view, int x, int y) {
    WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
    params.x = x;
    params.y = y;
    mWindowManager.updateViewLayout(view, params);
  }

  public void showView(View view) {
    try {
      WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
      mWindowManager.addView(view, params);
    } catch (IllegalStateException e) {
      // The view is already visible.
    }
  }

  public void hideView(View view) {
    try {
      mWindowManager.removeView(view);
    } catch (IllegalArgumentException e) {
      // The View wasn't visible to begin with.
    }
  }

  public void makeTouchable(View view) {
    WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
    params.flags = params.flags
        & ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        & ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
    mWindowManager.updateViewLayout(view, params);
  }

  public void makeUntouchable(View view) {
    WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
    params.flags = params.flags
        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
    mWindowManager.updateViewLayout(view, params);
  }
}
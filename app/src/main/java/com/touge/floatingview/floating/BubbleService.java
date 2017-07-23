package com.touge.floatingview.floating;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.touge.floatingview.OverlayPermission;

public class BubbleService extends Service implements BubbleLayout.OnBubbleExitListener {
  private static final String TAG = "BubbleService";

  private BubbleLayout mBubbleLayout;
  private boolean mIsRunning;

  @Override public void onCreate() {
    Log.d(TAG, "onCreate()");
    Notification foregroundNotification = getForegroundNotification();
    if (null != foregroundNotification) {
      int notificationId = getForegroundNotificationId();
      startForeground(notificationId, foregroundNotification);
    }
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    // Stop and return immediately if we don't have permission to display things above other
    // apps.
    if (!OverlayPermission.hasRuntimePermissionToDrawOverlay(getApplicationContext())) {
      Log.e(TAG, "Cannot display a Hover menu in a Window without the draw overlay permission.");
      stopSelf();
      return START_NOT_STICKY;
    }

    if (null == intent) {
      Log.e(TAG, "Received null Intent. Not creating bubble.");
      stopSelf();
      return START_NOT_STICKY;
    }

    if (!mIsRunning) {
      Log.d(TAG, "onStartCommand() - showing Hover menu.");
      mIsRunning = true;

      mBubbleLayout = new BubbleLayout(this);
      mBubbleLayout.setOnBubbleExitListener(this);

      initBubbleOuter(intent);
    }

    return START_STICKY;
  }

  @Override public void onDestroy() {
    Log.d(TAG, "onDestroy()");
    if (mIsRunning) {
      mBubbleLayout.removeFromWindow();
      mIsRunning = false;
    }
  }

  @Nullable @Override public IBinder onBind(Intent intent) {
    return null;
  }

  protected void initBubbleOuter(Intent intent) {

  }

  protected int getForegroundNotificationId() {
    // Subclasses should provide their own notification ID if using a notification.
    return 123456789;
  }

  @Nullable protected Notification getForegroundNotification() {
    // If subclass returns a non-null Notification then the Service will be run in
    // the foreground.
    return null;
  }

  public static void showBubble(Context context) {
    context.startService(new Intent(context, BubbleService.class));
  }

  @Override public void onBubbleExit() {
    Log.d(TAG, "Bubble exit requested. Exiting.");
    mIsRunning = false;
    stopSelf();
  }
}

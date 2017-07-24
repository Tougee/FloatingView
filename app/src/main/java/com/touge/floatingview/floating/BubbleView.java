package com.touge.floatingview.floating;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.touge.floatingview.R;
import com.touge.floatingview.Utils;

public class BubbleView extends FrameLayout {
  public static final int BUBBLE_INITIAL_SIZE = 70;

  private CircleCameraView mCameraView;

  public BubbleView(@NonNull Context context) {
    super(context);
    init(context);
  }

  private void init(Context ctx) {
    LayoutInflater.from(ctx).inflate(R.layout.view_camera, this, true);
    mCameraView = (CircleCameraView) findViewById(R.id.camera);
    FrameLayout.LayoutParams cameraParams = (LayoutParams) mCameraView.getLayoutParams();
    cameraParams.width = cameraParams.height = Utils.dp(ctx, BUBBLE_INITIAL_SIZE);
    cameraParams.gravity = Gravity.CENTER;
    mCameraView.setLayoutParams(cameraParams);
  }

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    mCameraView.start();
  }

  public void switchCamera() {
    int facing = mCameraView.getFacing();
    mCameraView.setFacing(facing == com.google.android.cameraview.CameraView.FACING_FRONT
        ? com.google.android.cameraview.CameraView.FACING_BACK
        : com.google.android.cameraview.CameraView.FACING_FRONT);
  }

  public void scale(float ratio) {
    setScaleX(ratio);
    setScaleY(ratio);
//    setTranslationX((getWidth() - mCameraView.getLayoutParams().width) * ratio / 2);
//    setTranslationY((getHeight() - mCameraView.getLayoutParams().height) * ratio / 2);
  }
}


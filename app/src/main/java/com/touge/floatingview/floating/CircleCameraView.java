package com.touge.floatingview.floating;

import android.content.Context;
import android.graphics.Outline;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import com.google.android.cameraview.CameraView;

public class CircleCameraView extends CameraView {

  public CircleCameraView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public void init() {
    ViewOutlineProvider vop = new ViewOutlineProvider() {
      @Override public void getOutline(View view, Outline outline) {
        outline.setOval(0, 0, view.getWidth(), view.getHeight());
      }
    };
    setOutlineProvider(vop);
    setClipToOutline(true);
  }
}

package com.touge.floatingview.floating;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.touge.floatingview.R;

public class BubbleView extends FrameLayout {

    private CircleCameraView mCameraView;

    public BubbleView(@NonNull Context context) {
        super(context);
        init(context);
    }

    private void init(Context ctx) {
        LayoutInflater.from(ctx).inflate(R.layout.view_camera, this, true);
        mCameraView = (CircleCameraView) findViewById(R.id.camera);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mCameraView.start();
    }

    public void switchCamera() {
        int facing = mCameraView.getFacing();
        mCameraView.setFacing(facing == com.google.android.cameraview.CameraView.FACING_FRONT ?
                com.google.android.cameraview.CameraView.FACING_BACK : com.google.android.cameraview.CameraView.FACING_FRONT);
    }

    public void scale(float ratio) {
        setScaleX(ratio);
        setScaleY(ratio);
    }
}


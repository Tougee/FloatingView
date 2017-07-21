package com.touge.floatingview;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class Utils {

    public static int dp(Context context, float dipValue) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }
}

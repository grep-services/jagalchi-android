package com.moon_o.jagalchi.tentoone.util;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.Toast;

/**
 * Created by mucha on 16. 4. 27.
 */
public class ToastWrapper {
    private volatile static ToastWrapper globalToast = null;
    private Toast internalToast;

    private ToastWrapper(Toast toast) {
        if(toast == null)
            throw new NullPointerException("instantaion param is not null");

        internalToast = toast;
    }

    public static ToastWrapper makeText(Context context, CharSequence text, int duration) {
        return new ToastWrapper(Toast.makeText(context, text, duration));
    }

    public static ToastWrapper makeText(Context context, int resId, int duration)
        throws Resources.NotFoundException {
        return new ToastWrapper(Toast.makeText(context, resId, duration));
    }

    public static ToastWrapper makeText(Context context, CharSequence text) {
        return new ToastWrapper(Toast.makeText(context, text, Toast.LENGTH_LONG));
    }

    public static ToastWrapper makeText(Context context, int resId)
        throws Resources.NotFoundException {
        return new ToastWrapper(Toast.makeText(context, resId, Toast.LENGTH_LONG));
    }

    public static void showText(Context context, CharSequence text, int duration) {
        ToastWrapper.makeText(context, text, duration).show();
    }

    public static void showText(Context context, int resId, int duration)
        throws Resources.NotFoundException {
        ToastWrapper.makeText(context, resId, duration).show();
    }

    public static void showText(Context context, CharSequence text) {
        ToastWrapper.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    public static void showText(Context context, int resId) {
        ToastWrapper.makeText(context, resId, Toast.LENGTH_LONG).show();
    }

    public void cancel() {
        internalToast.cancel();
    }

    public void show() {
        show(true);
    }

    public void show(boolean cancelCurrent) {
        if (cancelCurrent && (globalToast != null))
            globalToast.cancel();

        globalToast = this;

        if (internalToast == null || internalToast.getView().getWindowVisibility() != View.VISIBLE)
            internalToast.show();

    }

}

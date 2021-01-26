package com.zhilai.update.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.Toast;

/**
 * Created by yuhao.zhu on 2017/4/25.
 */

public class ToastUtil {

    private static final String TAG = "ToastUtil";

    private static Toast toast = null;

    @SuppressLint("ShowToast")
    public static void setToastMsg(Context mContext, String msg) {
        if (mContext == null) {
            return;
        }
        L.d(TAG, "toast====" + toast);
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(mContext, msg, Toast.LENGTH_SHORT);
        toast.show();
    }

    @SuppressLint("ShowToast")
    public static void setToastMsg(Context mContext, int id) {
        if (mContext == null) {
            return;
        }
        String msg = mContext.getResources().getString(id);
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(mContext, msg, Toast.LENGTH_SHORT);
        toast.show();
    }
}

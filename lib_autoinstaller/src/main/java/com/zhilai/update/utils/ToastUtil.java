package com.zhilai.update.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by yuhao.zhu on 2017/4/25.
 */

public class ToastUtil {

    private static final String TAG = "ToastUtil";

    private static Toast toast = null;

    public static void setToastMsg(Context mContext, String msg) {
        if (toast != null) {
            toast.setText(msg);
            toast.setDuration(Toast.LENGTH_SHORT);
        } else {
            toast = Toast.makeText(mContext, msg, Toast.LENGTH_SHORT);
        }
        toast.show();
    }

    public static void setToastMsg(Context mContext, int id) {
        String msg = mContext.getResources().getString(id);
        if (toast != null) {
            toast.setText(msg);
            toast.setDuration(Toast.LENGTH_SHORT);
        } else {
            toast = Toast.makeText(mContext, msg, Toast.LENGTH_SHORT);
        }
        toast.show();
    }

}

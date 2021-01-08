package com.zhilai.update.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.liulishuo.filedownloader.FileDownloader;
import com.zhilai.update.network.UpdateAppBuilder;
import com.zhilai.update.utils.L;
import com.zhilai.update.utils.Utils;

import java.io.File;

/**
 * 描述：监听apk安装替换卸载广播
 */
public class AppInstallReceiver extends BroadcastReceiver {

    private final String TAG = AppInstallReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
//        PackageManager manager = context.getPackageManager();
            final String savePath = Utils.getCachePath(context) + File.separator;//默认的保存路径
            if (intent.getAction() == null || intent.getData() == null) {
                return;
            }
            if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
                String packageName = intent.getData().getSchemeSpecificPart();
                L.d(TAG, "安装成功" + packageName);
                //安装完成之后；删除下载文件； 和清空db里的数据
                L.d(TAG, "savePath===" + savePath);
                startApp(context, packageName, true, savePath);
            }
            if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
                String packageName = intent.getData().getSchemeSpecificPart();
                L.d(TAG, "卸载成功" + packageName);
            }
            if (intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {
                String packageName = intent.getData().getSchemeSpecificPart();
                L.d(TAG, "替换成功" + packageName);
                //安装完成之后；删除下载文件； 和清空db里的数据
                L.d(TAG, "savePath===" + savePath);
                startApp(context, packageName, false, savePath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startApp(Context context, String packageName, boolean isInstall, String savePath) {
        if (UpdateAppBuilder.isAutoRestart) {
            String currentPageName = Utils.getAppProcessName(context);
            if (currentPageName.equals(packageName)) {
                L.d(TAG, "======重启App======" + currentPageName);
                if (isInstall) {
                    L.d(TAG, "====删除文件并清空db里的数据=====");
                    Utils.deleteFile(new File(savePath));
                    FileDownloader.getImpl().clearAllTaskData();
                } else {
                    L.d(TAG, "====删除文件=====");
                    Utils.deleteFile(new File(savePath));
//                FileDownloader.getImpl().clearAllTaskData();
                }
                Utils.startApp(context, currentPageName);
            }
        } else {
            L.d(TAG, "====不重启App=====");
        }
    }
}

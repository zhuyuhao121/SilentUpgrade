package com.zhilai.update.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.zhilai.update.service.InstallAccessibilityService;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;

/**
 * 描述：class describe
 * 作者：zhuangzeqin
 * 时间: 2017/9/6-16:13
 * 邮箱：zzq@eeepay.cn
 */
public class Utils {

    private static final String TAG = "Utils";

    /**
     * 获取缓存存放路径
     *
     * @return
     */
    public static String getCachePath(Context context) {
        String cachePath;
//        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
//                || !Environment.isExternalStorageRemovable()) {
//            /**获取缓存路径:/sdcard/Android/data/<application package>/cache  */
//            cachePath = context.getExternalCacheDir().getPath() + File.separator + "apk";
//        } else {
//            /**获取缓存路径:/data/data/<application package>/cache   */
//            cachePath = context.getCacheDir().getPath() + File.separator + "apk";
//        }
        cachePath = ("sdcard/" + context.getString(context.getApplicationInfo().labelRes)) + "/apk/";
        Log.d(TAG, "cachePath======" + cachePath);
        return cachePath;
    }

    /**
     * 7.0 的 Intent 离开你的应用，应用失败，并出现 FileUriExposedException 异常。
     *
     * @param context
     * @param apkPath
     */
    public static void installAPK(Context context, String apkPath) {
        L.e(TAG, "==普通安装==");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        // 由于没有在Activity环境下启动Activity,设置下面的标签
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        File file = (new File(apkPath));
        //版本在7.0以上是不能直接通过uri访问的
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //参数1 上下文, 参数2 Provider主机地址 和配置文件中保持一致   参数3  共享的文件
            String packageName = Utils.getAppProcessName(context);
            Uri apkUri = FileProvider.getUriForFile(context, packageName + ".fileprovider", file);
            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(file),
                    "application/vnd.android.package-archive");
        }
        context.startActivity(intent);
    }

    public static void install(Context context, File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // 7.0+以上版本
            Uri apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        }
        context.startActivity(intent);
    }

    /**
     * 通过递归调用删除一个文件夹及下面的所有文件
     *
     * @param file
     */
    /**
     * 删除某目录下所有文件包括子文件夹
     */
    public static void deleteFile(File file) {
        if (!file.exists()) {
            return;
        }
        if (file.isFile()) {
            file.delete();
            return;
        }
        if (file.isDirectory()) {
            File[] childFile = file.listFiles();
            if (childFile == null || childFile.length == 0) {
                file.delete();
                return;
            }
            for (File f : childFile) {
                deleteFile(f);
            }
            file.delete();
        }
    }

    // 此方法工作有误
    @Deprecated
    public static boolean isRooted() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("su");
            OutputStream outputStream = process.getOutputStream();
            InputStream inputStream = process.getInputStream();
            outputStream.write("id\n".getBytes());
            outputStream.flush();
            outputStream.write("exit\n".getBytes());
            outputStream.flush();
            process.waitFor();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String s = bufferedReader.readLine();
            if (s.contains("uid=0")) return true;
        } catch (IOException e) {
            Log.e(TAG, "没有root权限");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (process != null)
                process.destroy();
        }
        return false;
    }

    public static boolean checkRooted() {
        boolean result = false;
        try {
            result = new File("/system/bin/su").exists() || new File("/system/xbin/su").exists();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static Intent getLaunchIntent(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            return packageManager.getLaunchIntentForPackage(context.getPackageName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 检测辅助功能是否开启
     *
     * @param mContext
     * @return
     */
    public static boolean isAccessibilitySettingsOn(Context mContext) {
        int accessibilityEnabled = 0;
        // MyAccessibilityService为对应的服务
        final String service = mContext.getPackageName() + "/" + InstallAccessibilityService.class.getCanonicalName();
        Log.e(TAG, "service:" + service);
        try {
            accessibilityEnabled = Settings.Secure.getInt(mContext.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
            Log.e(TAG, "accessibilityEnabled = " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Error finding setting, default accessibility to not found: " + e.getMessage());
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            Log.e(TAG, "***ACCESSIBILITY IS ENABLED***");
            String settingValue = Settings.Secure.getString(mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();

                    Log.e(TAG, "accessibilityService :: " + accessibilityService + " " + service);
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        Log.e(TAG, "We've found the correct setting - accessibility is switched on!");
                        return true;
                    }
                }
            }
        } else {
            Log.e(TAG, "***ACCESSIBILITY IS DISABLED***");
        }
        return false;
    }

    /**
     * 监测到升级后执行app的启动
     * ,Class<?> cls
     */
    public static void startApp(Context context, String pageName) {
        // 根据包名打开安装的apk
        Intent resolveIntent = context.getPackageManager()
                .getLaunchIntentForPackage(pageName);
        context.startActivity(resolveIntent);

//        // 打开自身 一般用于软件升级
//        Intent intent = new Intent(context, cls);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(intent);
    }

    /**
     * 获取当前应用程序的包名
     *
     * @param context 上下文对象
     * @return 返回包名
     */
    public static String getAppProcessName(Context context) {
        //当前应用pid
        int pid = android.os.Process.myPid();
        //任务管理类
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        //遍历所有应用
        List<ActivityManager.RunningAppProcessInfo> infos = manager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo info : infos) {
            if (info.pid == pid)//得到当前应用
                return info.processName;//返回包名
        }
        return "";
    }

    /**
     * 发送Handler数据
     */
    public static void sendMessage(Handler handler, int what) {
        Message msg = Message.obtain();
        msg.what = what;
        handler.sendMessage(msg);
    }

    /**
     * 发送Handler数据
     */
    public static void sendMessage(Handler handler, int what, Object obj) {
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = obj;
        handler.sendMessage(msg);
    }

    /**
     * 延时发送Handler数据
     */
    public static void sendMessageDelayed(Handler handler, int what, long delayTime) {
        Message msg = Message.obtain();
        msg.what = what;
        handler.sendMessageDelayed(msg, delayTime);
    }
}

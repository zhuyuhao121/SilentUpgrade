package com.zhilai.update;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.zhilai.update.network.UpdateAppBuilder;
import com.zhilai.update.utils.BsPatchUtils;
import com.zhilai.update.utils.CommandUtil;
import com.zhilai.update.utils.L;
import com.zhilai.update.utils.ToastUtil;
import com.zhilai.update.utils.Utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public class AutoInstaller extends Handler {

    private static final String TAG = AutoInstaller.class.getSimpleName();
    private static volatile AutoInstaller mAutoInstaller;
    private Context mContext;
//    private String mTempPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Download";

    public enum MODE {
        ROOT_ONLY, //root安装
        AUTO_ONLY, //服务监听系统界面，模拟点击事件安装
        SYSTEM_SIGN_ONLY,  //系统签名安装
        GENERAL_INSTALL //普通安装
    }

    private MODE mMode = MODE.ROOT_ONLY;

    private AutoInstaller(Context context) {
        mContext = context.getApplicationContext();
    }

    public static AutoInstaller getDefault(Context context) {
        if (mAutoInstaller == null) {
            synchronized (AutoInstaller.class) {
                if (mAutoInstaller == null) {
                    mAutoInstaller = new AutoInstaller(context);
                }
            }
        }
        return mAutoInstaller;
    }


    public interface OnStateChangedListener {
        void onStart();

        void onComplete();

        void onNeed2OpenService();

        void needPermission();
    }

    private OnStateChangedListener mOnStateChangedListener;

    public void setOnStateChangedListener(OnStateChangedListener onStateChangedListener) {
        mOnStateChangedListener = onStateChangedListener;
    }

//    /**
//     * root后安装
//     * @param filePath
//     * @return
//     */
//    private boolean installUseRoot(String filePath) {
//        if (TextUtils.isEmpty(filePath))
//            throw new IllegalArgumentException("Please check apk file path!");
//        boolean result = false;
//        String command = "su; pm install -r " + filePath;
//        List<String> msgList = CommandUtil.execute(command);
//        if (msgList.size() > 0 && !msgList.get(0).toLowerCase().contains("Failure")) {
//            result = true;
//        }
//        return result;
//    }

    private boolean installUseRoot(String filePath) {
        L.e(TAG, "==ROOT安装==");
        boolean result = false;
        String cmd = "pm install -r " + filePath;
        Process process = null;
        DataOutputStream os = null;
        StringBuilder successMsg = null;
        StringBuilder errorMsg = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        try {
            //静默安装需要root权限
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.write(cmd.getBytes());
            os.writeBytes("\n");
            os.writeBytes("wait\n");
            os.flush();
            //执行命令
            CommandUtil.debug("installBySu cmd  = " + cmd);
            process.waitFor();
            //获取返回结果
            successMsg = new StringBuilder();
            errorMsg = new StringBuilder();
            successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String s;
            while ((s = successResult.readLine()) != null) {
                successMsg.append(s);
            }
            while ((s = errorResult.readLine()) != null) {
                errorMsg.append(s);
            }
            CommandUtil.debug("installBySu result : successMsg = " + successMsg + " errorMsg = " + errorMsg);

            int i = process.waitFor();
            if (i == 0) {
                Log.d(TAG, "=======正确获取root权限=======");
//                        Util.setToastMsg(mContext, "=======正确获取root权限=======");
                result = true; // 正确获取root权限
            } else {
                Log.d(TAG, "========没有root权限，或者拒绝获取root权限======");
//                        Util.setToastMsg(mContext, "========没有root权限，或者拒绝获取root权限======");
                result = false; // 没有root权限，或者拒绝获取root权限
            }
        } catch (IOException e) {
            e.printStackTrace();
            L.d(TAG, "e.getMessage()==1==" + e.getMessage());
            if (e.getMessage().contains("Cannot run program \"su\"")) {
                result = false;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null)
                    os.close();
                if (process != null)
                    process.destroy();
                if (successResult != null)
                    successResult.close();
                if (errorResult != null)
                    errorResult.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

//    /**
//     * 系统签名安装
//     *
//     * @param filePath
//     * @return
//     */
//    private boolean installSystemSign(String filePath) {
//        L.e(TAG, "==系统签名安装==");
//        if (TextUtils.isEmpty(filePath))
//            throw new IllegalArgumentException("Please check apk file path!");
//        String[] commands = new String[3];
//        commands[0] = "pm install -r " + filePath;
//
//        commands[1] = "sleep 10";
//
//        Intent launchIntent = Utils.getLaunchIntent(mContext);
//        if (launchIntent != null) {
//            commands[2] = "am start -n " + launchIntent.getPackage()
//                    + "/" + launchIntent.getComponent().getClassName();
//        }
//
//        CommandUtil.debug("installSystemSign commands = " + Arrays.asList(commands));
//
//        boolean result = false;
//        //只安装,目前不支持安装后启动应用
//        List<String> msgList = CommandUtil.execute(commands[0]);
//        if (msgList.size() > 0 && !msgList.get(0).toLowerCase().contains("Failure")) {
//            result = true;
//        }
//        L.d(TAG, "result==" + result);
//        return true;
//    }

    /**
     * 系统签名安装
     *
     * @param filePath 安装包路径
     */
    private void installSystemSign(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            throw new IllegalArgumentException("Please check apk file path!");
        }

        String[] commands = new String[3];

        commands[1] = "sleep 10";

        Intent launchIntent = Utils.getLaunchIntent(mContext);
        if (launchIntent != null) {
            commands[2] = "am start -n " + launchIntent.getPackage()
                    + "/" + launchIntent.getComponent().getClassName();
        }

        CommandUtil.debug("Build.VERSION.SDK_INT = " + Build.VERSION.SDK_INT);

        if (Build.VERSION.SDK_INT >= 24) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                throw new IllegalArgumentException("Please call in main thread");
            }
            //android7.0以上使用以下命令行，且只能在主线程调用安装
            StringBuilder sb = new StringBuilder();
            sb.append("pm install ");
            sb.append(" -r ");
            sb.append(" -i ");
            sb.append(mContext.getPackageName());
            sb.append(" --user 0 ");
            sb.append(" ");
            sb.append(filePath);
            commands[0] = sb.toString();

            CommandUtil.execute(commands[0]);

        } else {
            commands[0] = "pm install -r " + filePath;
            boolean result = false;
            //只安装,目前不支持安装后启动应用
            List<String> msgList = CommandUtil.execute(commands[0]);
            if (msgList.size() > 0 && !msgList.get(0).toLowerCase().contains("Failure")) {
                result = true;
            }
        }
        CommandUtil.debug("installSystemSign commands = " + Arrays.asList(commands));
    }

//    /**
//     * 系统签名安装
//     *
//     * @param filePath 安装包路径
//     *                 这里的“r”指的是“replace”，替换原来的应用；“-d”指的是“downgrade”，降级安装
//     */
//    public void installSystemSign(String filePath) {
//        String cmd = "";
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
//            cmd = "pm install -r -d " + filePath;
//        } else {
//            cmd = "pm install -r -d -i packageName --user 0 " + filePath;
//        }
//        Runtime runtime = Runtime.getRuntime();
//        try {
//            Process process = runtime.exec(cmd);
//            InputStream errorInput = process.getErrorStream();
//            InputStream inputStream = process.getInputStream();
//            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
//            StringBuilder error = new StringBuilder();
//            StringBuilder result = new StringBuilder();
//            String line = "";
//            while ((line = bufferedReader.readLine()) != null) {
//                result.append(line);
//            }
//            bufferedReader = new BufferedReader(new InputStreamReader(errorInput));
//            while ((line = bufferedReader.readLine()) != null) {
//                error.append(line);
//            }
//            if (result.toString().equals("Success")) {
//                if (mContext != null) {
//                    ToastUtil.setToastMsg(mContext, "安装成功");
//                }
//                Log.i(TAG, "install: Success");
//            } else {
//                if (mContext != null) {
//                    ToastUtil.setToastMsg(mContext, "安装失败");
//                }
//                Log.i(TAG, "install: error" + error);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

//    /**
//     * 使用辅助功能安装
//     *
//     * @param filePath
//     */
//    private void installUseAS(String filePath) {
//        // 存储空间
//        if (permissionDenied()) {
//            Log.e(TAG, " ## permissionDenied : ");
//            sendEmptyMessage(4);
//            return;
//        }
//        Log.e(TAG, " ## SDK_INT : " + Build.VERSION.SDK_INT);
//
//        // 允许安装应用
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            boolean b = mContext.getPackageManager().canRequestPackageInstalls();
//            Log.e(TAG, "## canRequestPackageInstalls : " + b + "," + Build.VERSION.SDK_INT);
//            if (!b) {
//                sendEmptyMessage(4);
////                return;
//            }
//        }
//
//        File file = new File(filePath);
//        if (!file.exists()) {
//            Log.e(TAG, "apk file not exists, path: " + filePath);
//            return;
//        }
////        Uri uri = Uri.fromFile(file);
////        Intent intent = new Intent(Intent.ACTION_VIEW);
////        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
////            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
////            Uri contentUri = FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".fileProvider", file);
////            mContext.grantUriPermission(mContext.getPackageName(), contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
////            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
////        } else {
////            intent.setDataAndType(uri, "application/vnd.android.package-archive");
////            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////        }
////        mContext.startActivity(intent);
//
//        startInstall(mContext, filePath);
//
//        if (!isAccessibilitySettingsOn(mContext)) {
//            toAccessibilityService();
//            sendEmptyMessage(3);
//        }
//    }
//
//    /**
//     * android1.x-6.x
//     *
//     * @param path 文件的路径
//     */
//    public void startInstall(Context context, String path) {
//        Intent install = new Intent(Intent.ACTION_VIEW);
//        install.setDataAndType(Uri.parse("file://" + path), "application/vnd.android.package-archive");
//        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(install);
//    }
//
//    private boolean permissionDenied() {
//        if (Build.VERSION.SDK_INT >= 23) {
//            String[] permissions = {
//                    Manifest.permission.READ_EXTERNAL_STORAGE,
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE
//            };
//
//            for (String str : permissions) {
//                if (mContext.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
//                    return true;
//                }
//            }
//        }
//
//        return false;
//    }
//
//    private void toAccessibilityService() {
//        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
//        mContext.startActivity(intent);
//    }
//
//    private boolean isAccessibilitySettingsOn(Context mContext) {
//        int accessibilityEnabled = 0;
//        final String service = mContext.getPackageName() + "/" + InstallAccessibilityService.class.getCanonicalName();
//        try {
//            accessibilityEnabled = Settings.Secure.getInt(
//                    mContext.getApplicationContext().getContentResolver(),
//                    Settings.Secure.ACCESSIBILITY_ENABLED);
//            Log.v(TAG, "accessibilityEnabled = " + accessibilityEnabled);
//        } catch (Settings.SettingNotFoundException e) {
//            Log.e(TAG, "Error finding setting, default accessibility to not found: "
//                    + e.getMessage());
//        }
//        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');
//
//        if (accessibilityEnabled == 1) {
//            Log.v(TAG, "***ACCESSIBILITY IS ENABLED*** -----------------");
//            String settingValue = Settings.Secure.getString(
//                    mContext.getApplicationContext().getContentResolver(),
//                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
//            if (settingValue != null) {
//                mStringColonSplitter.setString(settingValue);
//                while (mStringColonSplitter.hasNext()) {
//                    String accessibilityService = mStringColonSplitter.next();
//
//                    Log.v(TAG, "-------------- > accessibilityService :: " + accessibilityService + " " + service);
//                    if (accessibilityService.equalsIgnoreCase(service)) {
//                        Log.v(TAG, "We've found the correct setting - accessibility is switched on!");
//                        return true;
//                    }
//                }
//            }
//        } else {
//            Log.v(TAG, "***ACCESSIBILITY IS DISABLED***");
//        }
//
//        return false;
//    }

    public void install(final String filePath) {
        if (TextUtils.isEmpty(filePath) || !filePath.endsWith(".apk")) {
//            throw new IllegalArgumentException("not a correct apk file path");
            Utils.sendMessage(this, 5, "not a correct apk file path");
            return;
        }
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
        installType(filePath);
//            }
//        }).start();
    }

    private void installType(String filePath) {
        if (mContext == null) {
            return;
        }
        L.d(TAG, "filePath==000==" + filePath);
        // /storage/emulated/0/Android/data/com.zhilai.silentupgrade/cache/apk/old.apk

        sendEmptyMessage(1);

        if (UpdateAppBuilder.isIncrementalUpdate) {
            L.d(TAG, "增量更新");
            File newFile = new File(mContext.getExternalFilesDir("apk"), "app.apk");
//        File patchFile = new File(mContext.getExternalFilesDir("apk"), "patch.apk");
            File patchFile = new File(filePath);
            Log.e(TAG, "patchFile.exists()===" + patchFile.exists());
            Log.e(TAG, "patchFile.canRead()===" + patchFile.canRead());
            Log.e(TAG, "newFile.getAbsolutePath()===" + newFile.getAbsolutePath());
            Log.e(TAG, "patchFile.getAbsolutePath()===" + patchFile.getAbsolutePath());
            int result = BsPatchUtils.patch(mContext.getApplicationInfo().sourceDir, newFile.getAbsolutePath(),
                    patchFile.getAbsolutePath());
            filePath = newFile.getAbsolutePath();
            if (result != 0) {
                L.e(TAG, "差分包合并失败");
                Utils.sendMessage(this, 5, "差分包合并失败");
                return;
            }
        }
        L.d(TAG, "filePath==111==" + filePath);
        switch (mMode) {
//                    case BOTH:
//                        if (!Utils.checkRooted() || !installUseRoot(filePath)) {
//                            installUseAS(filePath);
//                        }
//                        break;
            case ROOT_ONLY:
                if (!installUseRoot(filePath)) {
                    Utils.installAPK(mContext, filePath);
                }
                break;
            case SYSTEM_SIGN_ONLY:
                installSystemSign(filePath);
                break;
            case AUTO_ONLY:
            case GENERAL_INSTALL:
                Utils.installAPK(mContext, filePath);
                break;
        }
        sendEmptyMessage(0);
    }

//    public void patch(String filePath) {
//        File newFile = new File(mContext.getExternalFilesDir("apk"), "app.apk");
////        File patchFile = new File(mContext.getExternalFilesDir("apk"), "patch.apk");
//        File patchFile = new File(filePath);
//        Log.e(TAG, "patchFile.exists()===" + patchFile.exists());
//        Log.e(TAG, "patchFile.canRead()===" + patchFile.canRead());
//        Log.e(TAG, "newFile.getAbsolutePath()===" + newFile.getAbsolutePath());
//        Log.e(TAG, "patchFile.getAbsolutePath()===" + patchFile.getAbsolutePath());
//        int result = BsPatchUtils.patch(mContext.getApplicationInfo().sourceDir, newFile.getAbsolutePath(),
//                patchFile.getAbsolutePath());
//        if (result == 0) {
//            Utils.install(mContext, newFile);
//        }
//    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
            case 0:
                if (mOnStateChangedListener != null)
                    mOnStateChangedListener.onComplete();
                break;
            case 1:
                if (mOnStateChangedListener != null)
                    mOnStateChangedListener.onStart();
                break;

            case 3:
                if (mOnStateChangedListener != null)
                    mOnStateChangedListener.onNeed2OpenService();
                break;
            case 4:
                if (mOnStateChangedListener != null) {
                    mOnStateChangedListener.needPermission();
                }
                break;
            case 5:
                String errorMsg = (String) msg.obj;
                if (TextUtils.isEmpty(errorMsg) && mContext != null) {
                    ToastUtil.setToastMsg(mContext, errorMsg);
                }
                break;
        }
    }

//    public void install(File file) {
//        if (file == null)
//            throw new IllegalArgumentException("file is null");
//        install(file.getAbsolutePath());
//    }
//
//
//    public void installFromUrl(final String httpUrl) {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                sendEmptyMessage(1);
//                File file = downLoadFile(httpUrl);
//                install(file);
//            }
//        }).start();
//    }
//
//    private File downLoadFile(String httpUrl) {
//        if (TextUtils.isEmpty(httpUrl)) throw new IllegalArgumentException();
//        File file = new File(mTempPath);
//        if (!file.exists()) file.mkdirs();
//        file = new File(mTempPath + File.separator + "update.apk");
//        InputStream inputStream = null;
//        FileOutputStream outputStream = null;
//        HttpURLConnection connection = null;
//        try {
//            URL url = new URL(httpUrl);
//            connection = (HttpURLConnection) url.openConnection();
//            if (connection instanceof HttpsURLConnection) {
//                SSLContext sslContext = getSLLContext();
//                if (sslContext != null) {
//                    SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
//                    ((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);
//                }
//            }
//            connection.setConnectTimeout(60 * 1000);
//            connection.setReadTimeout(60 * 1000);
//            connection.connect();
//            inputStream = connection.getInputStream();
//            outputStream = new FileOutputStream(file);
//            byte[] buffer = new byte[1024];
//            int len = 0;
//            while ((len = inputStream.read(buffer)) > 0) {
//                outputStream.write(buffer, 0, len);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                if (inputStream != null)
//                    inputStream.close();
//                if (outputStream != null)
//                    outputStream.close();
//                if (connection != null)
//                    connection.disconnect();
//            } catch (IOException e) {
//                inputStream = null;
//                outputStream = null;
//            }
//        }
//        return file;
//    }
//
//    private SSLContext getSLLContext() {
//        SSLContext sslContext = null;
//        try {
//            sslContext = SSLContext.getInstance("TLS");
//            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
//                @Override
//                public void checkClientTrusted(X509Certificate[] chain, String authType) {
//                }
//
//                @Override
//                public void checkServerTrusted(X509Certificate[] chain, String authType) {
//                }
//
//                @Override
//                public X509Certificate[] getAcceptedIssuers() {
//                    return new X509Certificate[0];
//                }
//            }}, new SecureRandom());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return sslContext;
//    }

    public static class Builder {

        private MODE mode = MODE.ROOT_ONLY;
        private Context context;
//        private OnStateChangedListener onStateChangedListener;
//        private String directory = Environment.getExternalStorageDirectory().getAbsolutePath();

        public Builder(Context c) {
            context = c;
        }

        public Builder setMode(MODE m) {
            mode = m;
            return this;
        }

//        public Builder setOnStateChangedListener(OnStateChangedListener o) {
//            onStateChangedListener = o;
//            return this;
//        }
//
//        public Builder setCacheDirectory(String path) {
//            directory = path;
//            return this;
//        }

        public AutoInstaller build() {
            AutoInstaller autoInstaller = new AutoInstaller(context);
            autoInstaller.mMode = mode;
//            autoInstaller.mOnStateChangedListener = onStateChangedListener;
//            autoInstaller.mTempPath = directory;
            return autoInstaller;
        }
    }
}

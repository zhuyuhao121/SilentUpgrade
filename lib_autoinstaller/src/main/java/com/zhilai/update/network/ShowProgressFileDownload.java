package com.zhilai.update.network;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloader;
import com.zhilai.update.OnAutoInstallListener;
import com.zhilai.update.utils.L;
import com.zhilai.update.utils.ToastUtil;
import com.zhilai.update.AutoInstaller;
import com.zhilai.update.view.CustomDialog;
import java.net.UnknownHostException;

/**
 * 描述：通知栏显示下载进度条
 * 作者：zhuangzeqin
 * 时间: 2017/9/8-9:28
 * 邮箱：zzq@eeepay.cn
 */
public class ShowProgressFileDownload extends FileDownloadListener {

    private final String TAG = ShowProgressFileDownload.class.getSimpleName();

    private static final int NOTIFICATIONID = 103;//通知栏id

    private final int MININTERVALUPDATESPEED = 400;//最小时间间隔更新下载速度

    private final int CALLBACKPROGRESSTIMES = 300;//回调进度时间

    private Context mContext;//上下文对象

    private String mDownloadUrl;//下载文件路径

    private String mTargetFilePath;//apk 文件路径

    private boolean isWifiRequired = false;//是否wifi 请求下载

    private NotificationCompat.Builder builderProgress;

    private NotificationManager notificationManager;

    private CustomDialog dialog;

    private OnAutoInstallListener onAutoInstallListener;

    public ShowProgressFileDownload setAutoInstallListener(OnAutoInstallListener onAutoInstallListener){
        this.onAutoInstallListener = onAutoInstallListener;
        return this;
    }

    private ShowProgressFileDownload(Context activity)
    {
        this.mContext = activity;
        //显示进度条通知
        builderProgress = new NotificationCompat.Builder(mContext);
        builderProgress.setContentTitle("下载中");
        builderProgress.setSmallIcon(android.R.drawable.stat_sys_download);
        builderProgress.setTicker("进度条通知");
        builderProgress.setProgress(100, 0, false);
        Notification notification = builderProgress.build();
        notificationManager = (NotificationManager) mContext.getSystemService(mContext.NOTIFICATION_SERVICE);
        //发送一个通知
        notificationManager.notify(NOTIFICATIONID, notification);
    }

    /**
     * 设置下载的url
     *
     * @param url
     * @return
     */
    public ShowProgressFileDownload downloadUrl(String url) {
        this.mDownloadUrl = url;
        return this;
    }

    /**
     * 设置apk 文件路径
     * @param targetFilePath
     * @return
     */
    public ShowProgressFileDownload targetFilePath(String targetFilePath)
    {
        this.mTargetFilePath = targetFilePath;
        return this;
    }

    /**
     * 是否wifi 请求下载
     * @param isWifiRequired
     * @return
     */
    public ShowProgressFileDownload isWifiRequired(boolean isWifiRequired)
    {
        this.isWifiRequired = isWifiRequired;
        return this;
    }


    public static ShowProgressFileDownload with(Context activity)
    {
        return new ShowProgressFileDownload(activity);
    }

    /**
     * 开始下载
     */
    public void startDownload()
    {
        FileDownloader.getImpl().
                create(mDownloadUrl).
                setPath(mTargetFilePath).
                setAutoRetryTimes(3).
                setWifiRequired(isWifiRequired).//是否在wifi下载
                setCallbackProgressTimes(CALLBACKPROGRESSTIMES).
                setMinIntervalUpdateSpeed(MININTERVALUPDATESPEED).setListener(this).start();
    }

    @Override
    protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
        //等待，已经进入下载队列	数据库中的soFarBytes与totalBytes
        L.d(TAG, task.getFilename() + "等待，已经进入下载队列");
        if(onAutoInstallListener != null){
            onAutoInstallListener.pending(task, soFarBytes, totalBytes);
        }
    }

    @Override
    protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
        int progress = (int) ((float) soFarBytes / totalBytes * 100);
        L.d(TAG, "progress=====" + progress);
        if(onAutoInstallListener != null){
            onAutoInstallListener.onProgress(progress);
        }
        //更新进度条
        builderProgress.setProgress(100, progress, false);
        //再次通知
        notificationManager.notify(NOTIFICATIONID, builderProgress.build());
    }

    @Override
    protected void completed(BaseDownloadTask task) {
        Log.d(TAG, task.getFilename() + "下载完成");
        if(onAutoInstallListener != null){
            onAutoInstallListener.onProgress(100);
        }
        //进度条退出
        notificationManager.cancel(NOTIFICATIONID);
        showTipDialog(mContext);
    }

    @Override
    protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
        Log.d(TAG, task.getFilename() + "下载暂停Speed:" + task.getSpeed());
        //下载暂停
        if(onAutoInstallListener != null){
            onAutoInstallListener.paused(task, soFarBytes, totalBytes);
        }
    }

    @Override
    protected void error(BaseDownloadTask task, Throwable e) {
        //下载出现错误 java.net.UnknownHostException
        e.printStackTrace();
        if(onAutoInstallListener != null){
            onAutoInstallListener.error(task, e);
        }
        if (e instanceof UnknownHostException) {
            Toast.makeText(mContext, "网络已经断开", Toast.LENGTH_SHORT).show();
//            return;
        }
    }

    @Override
    protected void warn(BaseDownloadTask task) {
        //  在下载队列中(正在等待/正在下载)已经存在相同下载连接与相同存储路径的任务
       // Log.d(TAG, task.getFilename() + "已经下载字节:" + task.getSoFarBytes());
        if(onAutoInstallListener != null){
            onAutoInstallListener.warn(task);
        }
    }

    private void showTipDialog(final Context mContext) {
////        if (dialog!=null)//不知道为什么；网络监听会回调2次？
////            dialog.cancel();
//        dialog = new CustomDialog(mContext);
//        dialog.setTitles("温馨提示").setMessage("新的版本已经下载好，是否需要更新？");
//        dialog.setPositiveButton(mContext.getString(R.string.ok), new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //跳转下载安装的界面
//                Utils.installAPK(mContext, mTargetFilePath);
//            }
//        });
//        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);
//        dialog.setNegativeButton(mContext.getString(R.string.cancel), null);
//        dialog.setCanceledOnTouchOutside(false);
//        dialog.show();



        AutoInstaller.MODE mode = UpdateAppBuilder.mode;

        if(mode == null){
            ToastUtil.setToastMsg(mContext, "请配置安装方式");
            return;
        }

        AutoInstaller installer = new AutoInstaller.Builder(mContext)
                .setMode(mode)
//                .setCacheDirectory(mTargetFilePath)
                .build();

//        installer.setOnStateChangedListener(new AutoInstaller.OnStateChangedListener() {
//            @Override
//            public void onStart() {
//                L.d(TAG, " installer onStart  ");
//                ToastUtil.setToastMsg(mContext, R.string.text_install_start);
//            }
//
//            @Override
//            public void onComplete() {
//                L.d(TAG, " installer onComplete ");
//                ToastUtil.setToastMsg(mContext, R.string.text_install_complete);
//            }
//
//            @Override
//            public void onNeed2OpenService() {
//                L.d(TAG, " installer onNeed2OpenService ");
//            }
//
//            @Override
//            public void needPermission() {
//                L.d(TAG, " installer needPermission ");
//            }
//        });

        installer.install(mTargetFilePath);
    }

}

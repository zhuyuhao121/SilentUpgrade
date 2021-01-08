package com.zhilai.silentupgrade;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.provider.Settings;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.zhilai.update.OnAutoInstallListener;
import com.zhilai.update.utils.L;
import com.zhilai.update.utils.ToastUtil;
import com.zhilai.silentupgrade.permission.Acp;
import com.zhilai.silentupgrade.permission.AcpListener;
import com.zhilai.silentupgrade.permission.AcpOptions;
import com.zhilai.update.AutoInstaller;
import com.zhilai.update.network.UpdateAppBuilder;
import com.zhilai.update.utils.Utils;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Button bt;
    private boolean flag;
//    private String url = "http://zzwandroid.oss-cn-shenzhen.aliyuncs.com/download.apk";
//    private String url = "http://zzwandroid.oss-cn-shenzhen.aliyuncs.com/old.apk";
//    private String url = "http://zzwandroid.oss-cn-shenzhen.aliyuncs.com/patch1.apk";
//    "http://115.29.241.7:8022/nit_admin/profile/file/version/88880101686244059/今天国际快递柜V1.0.0.200926.apk"
    private String url = "http://115.29.241.7:8022/nit_admin/profile/file/version/88880117280653418/今天国际快递柜V1.0.1.201014.apk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyStoragePermissions();
        bt = findViewById(R.id.bt);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (flag) {
                    AutoInstaller.MODE mode = AutoInstaller.MODE.SYSTEM_SIGN_ONLY;
                    download(url, mode);
                } else {
                    ToastUtil.setToastMsg(MainActivity.this, "请先申请权限");
                    verifyStoragePermissions();
                }
            }
        });
    }

    /**
     * 下载App并安装
     *
     * @param url   apk的下载路径
     * @param mode： ROOT_ONLY, root模式下安装
     *              AUTO_ONLY, 开启无障碍服务安装
     *              SYSTEM_SIGN_ONLY, 系统签名的方式安装（针对我们的工控机）
     *              GENERAL_INSTALL; 普通安装（弹提示框自己手动安装）
     */
    private void download(String url, AutoInstaller.MODE mode) {
        UpdateAppBuilder.with(MainActivity.this)
                .apkPath(url)//apk 的下载路径
                .instalMode(mode)
                .isAutoRestart(true)
                .isIncrementalUpdate(false)
                .setAutoInstallListener(new OnAutoInstallListener() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onProgress(int progress) {
                        bt.setText("当前下载进度：" + progress + "%");
                        if (progress == 100) {
                            bt.setText("点击下载");
                        }
                    }

                    @Override
                    public void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                        L.d(TAG, task.getFilename() + "等待，已经进入下载队列");
                    }

                    @Override
                    public void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                        L.d(TAG, task.getFilename() + "下载暂停Speed:" + task.getSpeed());
                    }

                    @Override
                    public void error(BaseDownloadTask task, Throwable e) {
                        L.d(TAG, "下载出现错误");
                    }

                    @Override
                    public void warn(BaseDownloadTask task) {
                        //  在下载队列中(正在等待/正在下载)已经存在相同下载连接与相同存储路径的任务
                        L.d(TAG, task.getFilename() + "已经下载字节:" + task.getSoFarBytes());
                    }
                })
//                        .isForce(false)//是否需要强制升级
//                        .serverVersionName("1.0.1")//服务的版本(会与当前应用的版本号进行比较)
//                        .updateInfo("有新的版本发布啦！赶紧下载体验")//升级版本信息
                .start();//开始下载
    }

    /**
     * 打开无障碍服务界面
     */
    private void silenceAutoInstall() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Utils.isAccessibilitySettingsOn(MainActivity.this)) {
            download(url, AutoInstaller.MODE.AUTO_ONLY);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void verifyStoragePermissions() {
        Acp.getInstance(this).request(new AcpOptions.Builder()
                        .setPermissions(Manifest.permission.READ_EXTERNAL_STORAGE
                                , Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .build(),
                new AcpListener() {
                    @Override
                    public void onGranted() {
                        L.d(TAG, "===已申请到权限===");
                        flag = true;
                    }

                    @Override
                    public void onDenied(List<String> permissions) {
                        L.d(TAG, "===权限拒绝===");
                        flag = false;
                    }
                });
    }
}

package com.zhilai.silentupgrade;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.provider.Settings;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;
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
    private Button downBt;
    private TextView urlTv;
    private EditText urlEt;
    private boolean flag;
    //    private String url = "http://zzwandroid.oss-cn-shenzhen.aliyuncs.com/download.apk";
//    private String url = "http://zzwandroid.oss-cn-shenzhen.aliyuncs.com/old.apk";
//    private String url = "http://zzwandroid.oss-cn-shenzhen.aliyuncs.com/patch1.apk";
//    private String url = "http://115.29.241.7:8022/nit_admin/profile/file/version/88880117280653418/今天国际快递柜V1.0.1.201014.apk";
    private String url = "http://113.57.110.66:8092/nit_admin/profile/file/version/88880251777737753/系统签名升级V1.0.0.210319.apk";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyStoragePermissions();
        urlEt = findViewById(R.id.url_et);
        downBt = findViewById(R.id.down_bt);
        Button scanBt = findViewById(R.id.scan_bt);
        urlTv = findViewById(R.id.url_tv);
        TextView tv = findViewById(R.id.tv);
        scanBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                huaweiScanCode();
            }
        });
        downBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (flag) {
                    url = urlEt.getText().toString();
                    if (TextUtils.isEmpty(url)) {
                        ToastUtil.setToastMsg(MainActivity.this, "安装包下载地址不能为空");
                        return;
                    }
                    download(url, AutoInstaller.MODE.SYSTEM_SIGN_ONLY);
                } else {
                    ToastUtil.setToastMsg(MainActivity.this, "请先申请权限");
                    verifyStoragePermissions();
                }
            }
        });
        tv.setText("V" + getVersion(this));
        urlEt.setText(url);
        urlEt.setSelection(url.length());
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
                .isAutoRestart(true)//是否自动重启
                .isIncrementalUpdate(true)//是否增量更新
                .setAutoInstallListener(new OnAutoInstallListener() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onProgress(int progress) {
                        downBt.setText("当前下载进度：" + progress + "%");
                        if (progress == 100) {
                            downBt.setText("点击下载");
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
                        ToastUtil.setToastMsg(MainActivity.this, "下载出现错误");
                    }

                    @Override
                    public void warn(BaseDownloadTask task) {
                        //  在下载队列中(正在等待/正在下载)已经存在相同下载连接与相同存储路径的任务
                        L.d(TAG, task.getFilename() + "已经下载字节:" + task.getSoFarBytes());
                    }
                })
                .start();//开始下载
    }

    /**
     * 打开无障碍服务界面
     */
    private void silenceAutoInstall() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivityForResult(intent, 1);
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (Utils.isAccessibilitySettingsOn(MainActivity.this)) {
//            download(url, AutoInstaller.MODE.AUTO_ONLY);
//        }
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void verifyStoragePermissions() {
        Acp.getInstance(this).request(new AcpOptions.Builder()
                        .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE
                                , Manifest.permission.CAMERA)
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


    int REQUESTCODE = 121;

    /**
     * 跳转到华为扫码界面
     */
    private void huaweiScanCode() {
//        HmsScanAnalyzerOptions options = new HmsScanAnalyzerOptions.Creator().setHmsScanTypes(HmsScan.QRCODE_SCAN_TYPE, HmsScan.DATAMATRIX_SCAN_TYPE).create();
        HmsScanAnalyzerOptions options = new HmsScanAnalyzerOptions.Creator().create();
        ScanUtil.startScan(this, REQUESTCODE, options);
    }

    @SuppressLint("SetTextI18n")
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        if (requestCode == REQUESTCODE) {
            //导入图片扫描返回结果
            HmsScan obj = data.getParcelableExtra(ScanUtil.RESULT);
            if (obj == null) {
                return;
            }
            //展示解码结果
            String originalValue = obj.getOriginalValue();//原始码值
            int scanType = obj.getScanType();//码制式
            int scanTypeForm = obj.getScanTypeForm();//结构化数据
            Rect borderRect = obj.getBorderRect();//码在图片中的位置
            int left = borderRect.left;
            int top = borderRect.top;
            int right = borderRect.right;
            int bottom = borderRect.bottom;

            L.d(TAG, "原始码值==" + originalValue);
            L.d(TAG, "码制式==" + scanType);
            L.d(TAG, "结构化数据==" + scanTypeForm);
            L.d(TAG, "码在图片中的位置==" + borderRect);
            L.d(TAG, "左==" + left);
            L.d(TAG, "上==" + top);
            L.d(TAG, "右==" + right);
            L.d(TAG, "下==" + bottom);
            url = originalValue;
            urlTv.setText(url);
            urlEt.setText(url);
            urlEt.setSelection(url.length());
        }

        if (Utils.isAccessibilitySettingsOn(MainActivity.this)) {
            if (!TextUtils.isEmpty(url)) {
                download(url, AutoInstaller.MODE.AUTO_ONLY);
            }
        }
    }

    /**
     * 获取版本号
     *
     * @return 当前应用的版本名称；
     */
    public static String getVersion(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            String packageName = context.getPackageName();
            PackageInfo info = pm.getPackageInfo(packageName, 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}

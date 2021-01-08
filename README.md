## 项目介绍

App升级在开发过程中是必不可少的，为了大家方便集成以及统一管理，我将App升级功能封装成了一个仓库，大家只要在项目中添加依赖就可以直接使用
Apk下载下来后，程序会自动安装并重启，重启的我已经在仓库里加上了，所以大家在调用的时候，就不需要再做重启的处理，安装成功后，会将apk文件删除掉，Android高版本的需要通过FileProvider来安装，这些我也处理了，所以大家只需要添加依赖，并调用下面提供的接口即可，其他都不需要处理，如果在测试的时候发现bug可以给我反馈，我会尽快更改

## 依赖添加
在你的项目根目录下的build.gradle文件中加入依赖
``` java
allprojects {
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```
添加依赖
``` java
dependencies {
    implementation 'com.github.zhuyuhao121:SilentUpgrade:1.0.0'
}
```

GitHub地址：https://github.com/zhuyuhao121/SilentUpgrade


## 接口介绍

### 打开无障碍服务界面


``` java
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
```

### 下载App并安装

root模式安装：AutoInstaller.MODE.ROOT_ONLY

系统签名模式安装：AutoInstaller.MODE.SYSTEM_SIGN_ONLY

手机可选择无障碍模式或者普通模式安装：AutoInstaller.MODE.AUTO_ONLY，AutoInstaller.MODE.GENERAL_INSTALL

如果选择root模式安装的话，程序判断工控机没有root权限会自动调普通安装接口

``` java
bt.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View view) {
        AutoInstaller.MODE mode = AutoInstaller.MODE.ROOT_ONLY;
            if(mode == AutoInstaller.MODE.AUTO_ONLY){
                if (Utils.isAccessibilitySettingsOn(MainActivity.this)) {
                    download(url, AutoInstaller.MODE.AUTO_ONLY);
                } else {
                    silenceAutoInstall();
                }
            } else {
                download(url, mode);
            }
    }
});


/**
 * 下载App并安装
 * @param url  apk的下载路径
 * @param mode：
 * ROOT_ONLY, root模式下安装
 * AUTO_ONLY, 开启无障碍服务安装
 * SYSTEM_SIGN_ONLY, 系统签名的方式安装（针对我们的工控机）
 * GENERAL_INSTALL; 普通安装（弹提示框自己手动安装）
 */
private void download(String url, AutoInstaller.MODE mode){
    UpdateAppBuilder.with(MainActivity.this)
        .apkPath(url)//apk 的下载路径
        .instalMode(mode)
        .isIncrementalUpdate(true)//是否增量更新
        .setAutoInstallListener(new OnAutoInstallListener() {
            @Override
            public void onProgress(int progress) {
                bt.setText("当前下载进度：" + progress + "%");
                if (progress == 100) {
                    bt.setText("点击下载");
                }
            }
        })
//                        .isForce(false)//是否需要强制升级
//                        .serverVersionName("1.0.1")//服务的版本(会与当前应用的版本号进行比较)
//                        .updateInfo("有新的版本发布啦！赶紧下载体验")//升级版本信息
        .start();//开始下载
}
```

## 差分包制作方式：

将差分工具包放到某个文件夹下，在当前目录打开cmd，然后执行下面命令，注意：一定要在差分工具包的目录下打开cmd

生成差分包： bsdiff oldfile newfile patchfile

合成：bspatch oldfile 合成后的输出文件 patchfile

如：bsdiff 1.apk 2.apk patch（对比版本1与版本2的apk，生成差分包patch）

bspatch 1.apk 2.apk patch（使用差分包patch与1.apk合并，生成2.apk）


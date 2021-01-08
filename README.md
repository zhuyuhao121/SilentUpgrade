1 项目介绍
App升级在开发过程中是必不可少的，为了大家方便集成以及统一管理，我将App升级功能封装成了一个仓库，大家只要在项目中添加依赖就可以直接使用
大家之前接触比较多的应该是普通升级、强制升级，这次为了与微信、支付宝人脸对接，人家要求我们要做静默升级，静默升级是需要root权限的，目前支付宝提供的工控机是有root权限的，我们公司的工控机暂时没有root权限，不过这个之后可以想办法解决，手机的root权限目前是不太容易获取的，所以静默升级在手机上暂时有问题（每个手机厂家是会对自己内置的手机系统应用开启root权限，其他的不开启），不过可以通过开启无障碍服务的方式辅助解决，这种方式经过测试在部分手机上可以成功，部分手机上不成功，它的原理是通过开启无障碍服务，监听系统界面上的操作，模拟点击事件，大概意思就是，安装的时候会弹出安装提示框，不过不需要人为操作，系统会自己点击安装按钮，然后直到安装成功，有的手机上只会弹出安装提示框，但不会模拟点击，所以这种方式也不是完全能够解决静默升级
Apk下载下来后是存储在包名下的，完整路径是Android/data/自己程序的包名/cache/apk/xxx.apk
Apk下载下来后，程序会自动安装并重启，重启的我已经在仓库里加上了，所以大家在调用的时候，就不需要再做重启的处理，安装成功后，会将apk文件删除掉，Android高版本的需要通过FileProvider来安装，这些我也处理了，所以大家只需要添加依赖，并调用下面提供的接口即可，其他都不需要处理，如果在测试的时候发现bug可以给我反馈，我会尽快更改

以下是开启无障碍服务的截图：



2其他项目如何引用
2.1 添加maven仓库
根目录的build.gradle中添加maven仓库，本地仓库和远程仓库根据需要自行选择，如果你只是对接人，那就直接依赖远程仓库，如果是仓库的打包者，那就先使用本地仓库进行测试，在本地仓库使用没问题的情况下，将aar上传到远程仓库，供其他开发者调用，本地仓库以及远程仓库的创建可参考《ZL-KF-110601人脸支付终端使用手册V1.0.4_20190627》

maven{
    url 'file://D://silentupgrade/'
    // url "https://raw.githubusercontent.com/zhuyuhao121/SilentUpgrade/master"
}

2.2 在app的build.gradle中引用依赖
最新版本查看链接：
https://github.com/zhuyuhao121/SilentUpgrade/tree/master/com/zhilai/update/silentupgrade

implementation 'com.zhilai.update:silentupgrade:1.0.2'

2.3 点击同步按钮下载依赖，下载成功之后就会看到对应的aar包

3 接口介绍
3.1打开无障碍服务界面



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

3.2下载App并安装

支付宝的工控机可选择root模式安装：AutoInstaller.MODE.ROOT_ONLY
A9工控机可选择系统签名模式安装：AutoInstaller.MODE.SYSTEM_SIGN_ONLY
手机可选择无障碍模式或者普通模式安装：
AutoInstaller.MODE.AUTO_ONLY，AutoInstaller.MODE.GENERAL_INSTALL
如果选择root模式安装的话，程序判断工控机没有root权限会自动调普通安装接口


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


差分包制作方式：

将差分工具包放到某个文件夹下，在当前目录打开cmd，然后执行下面命令，注意：一定要在差分工具包的目录下打开cmd

生成差分包： bsdiff oldfile newfile patchfile
合成：bspatch oldfile 合成后的输出文件 patchfile

如：bsdiff 1.apk 2.apk patch
对比版本1与版本2的apk，生成差分包patch

bspatch 1.apk 2.apk patch
使用差分包patch与1.apk合并，生成2.apk
package com.zhilai.update;

import com.liulishuo.filedownloader.BaseDownloadTask;

public interface OnAutoInstallListener {

    void onProgress(int progress);

    void pending(BaseDownloadTask task, int soFarBytes, int totalBytes);

    void paused(BaseDownloadTask task, int soFarBytes, int totalBytes);

    void error(BaseDownloadTask task, Throwable e);

    void warn(BaseDownloadTask task);

}

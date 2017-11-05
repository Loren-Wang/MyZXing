package com.myzxing.android.decode;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;

import com.myzxing.android.in.QrCodeEncode;

/**
 * 创建时间： 0003/2017/11/3 下午 4:22
 * 创建人：王亮（Loren wang）
 * 功能作用：二维码扫描结果处理
 * 功能方法：
 * 思路：
 * 修改人：
 * 修改时间：
 * 备注：
 */
public class ScanResultHandler extends Handler{
    private QrCodeEncode qrCodeEncode;
    public ScanResultHandler(@NonNull QrCodeEncode qrCodeEncode){
        this.qrCodeEncode = qrCodeEncode;
        qrCodeEncode.initScanResultHandler();//回调结果处理初始化
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        qrCodeEncode.handleScanResultMessage(msg);//消息回传
    }

    public void quitSynchronously() {
        qrCodeEncode.quitScanResultSynchronously();//回调结果处理初始化
    }
}

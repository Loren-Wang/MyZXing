package com.myzxing.android.decode;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;

import com.myzxing.android.in.QrCodeEncode;

/**
 * 创建时间： 0003/2017/11/3 下午 4:31
 * 创建人：王亮（Loren wang）
 * 功能作用：解码后对于解码的结果进行处理，就是相当于处理字节数据
 * 功能方法：
 * 思路：
 * 修改人：
 * 修改时间：
 * 备注：
 */
public class DecodeHandler extends Handler {
    private QrCodeEncode qrCodeEncode;
    public DecodeHandler(@NonNull QrCodeEncode qrCodeEncode){
        this.qrCodeEncode = qrCodeEncode;
        qrCodeEncode.initDecodeResultHandler();//回调结果处理初始化
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        qrCodeEncode.handleDecodeResultMessage(msg);//消息回传
    }
}

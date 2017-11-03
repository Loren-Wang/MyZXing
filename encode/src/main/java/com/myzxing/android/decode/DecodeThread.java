package com.myzxing.android.decode;

import com.myzxing.android.in.QrCodeEncode;

/**
 * 创建时间： 0003/2017/11/3 下午 4:27
 * 创建人：王亮（Loren wang）
 * 功能作用：解码线程，用来对于扫描到的图片信息进行处理
 * 功能方法：
 * 思路：
 * 修改人：
 * 修改时间：
 * 备注：
 */
public class DecodeThread extends Thread {
    private QrCodeEncode qrCodeEncode;
    public DecodeThread(QrCodeEncode qrCodeEncode){
        this.qrCodeEncode = qrCodeEncode;
        qrCodeEncode.initDecodeThread();//初始化解码线程回调
    }

    @Override
    public void run() {
        super.run();
        qrCodeEncode.decodeThreadRun();
    }
}

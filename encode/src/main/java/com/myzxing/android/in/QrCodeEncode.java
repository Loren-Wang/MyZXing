package com.myzxing.android.in;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.myzxing.android.manager.BeepManager;
import com.myzxing.android.manager.DecodeFormatManager;
import com.myzxing.android.manager.PreferencesActivity;
import com.myzxing.android.camera.CameraManager;

import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

public abstract class QrCodeEncode implements SurfaceHolder.Callback {

    private String TAG = "QrCodeEncode";
    private Activity activity;
    private boolean hasSurface;//是否有sufaceview
    private boolean isShowLog = false;//是否显示log信息
    private BeepManager beepManager;//声音震动管理
    public CameraManager cameraManager;//相机管理器
    private MultiFormatReader multiFormatReader;//二维码的解析读取工具
    private Map<DecodeHintType,Object> hints;//解析类型

    private final int MSG_WHAT_RESULT_DATA = 0x0000;//接受到扫描后的字节数据
    private final int MSG_WHAT_RESULT_DATA_ENCODE_SUCCESS = 0x0001;//解析成功
    private final int MSG_WHAT_RESULT_DATA_ENCODE_FAIL = 0x0010;//解析失败

    private HandlerThread handlerThread = new HandlerThread(TAG);
    private Handler handlerChildThread;//子线程
    //发送消息到主线程
    private Handler handlerMainThread = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_WHAT_RESULT_DATA://获取到图片数据
                    encodeByteData((byte[]) msg.obj);
                    break;
                case MSG_WHAT_RESULT_DATA_ENCODE_SUCCESS://二维码解析成功
//                    encodeQrCodeSuccess((String) msg.obj);
                    break;
                case MSG_WHAT_RESULT_DATA_ENCODE_FAIL://二维码解析失败
//                    encodeQrCodeFail();
                    break;
                default:
                    break;
            }
        }
    };

    public void onEncodeCreate(Bundle savedInstanceState, Activity activity){
        Window window = activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.activity = activity;

        //初始化异步消息通知体
        handlerThread.start();
        handlerChildThread = new Handler(handlerThread.getLooper());

        //初始化二维码的解析读取
        initMultiFormatReaderHints(null);
        multiFormatReader = new MultiFormatReader();
        multiFormatReader.setHints(hints);

        //初始化默认变量
        hasSurface = false;
        //初始化震动声音
        beepManager = new BeepManager(activity);
        logD("初始化声音震动管理器");

        if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

    }

    public void onEncodeResume(Application application) {
        /** CameraManager must be initialized here, not in onCreate(). This is necessary because we don't
         * want to open the camera driver and measure the screen size if we're going to show the help on
         * first launch. That led to bugs where the scanning rectangle was the wrong size and partially
         * off screen.
         * 必须在这里初始化拍照者，而不是在onCreate()中。这是必要的，因为如果我们要在第一次启动时显示帮助，
         * 我们不希望打开摄像机驱动程序并测量屏幕尺寸。这就导致了错误的地方扫描矩形的大小和屏幕部分都是错误的*/
        cameraManager = new CameraManager(application);
        logD("初始化相机管理器");

        //更新状态
        beepManager.updatePrefs();

        //设置扫描矩形的宽高
        int sCanWidth = getSCanWidth();
        int sCanHeight = getSCanHeight();
        cameraManager.setManualFramingRect(sCanWidth, sCanHeight);
        logD("设置扫描矩形（矩形在屏幕正中心）的宽高，宽：" + sCanWidth + "高：" + sCanHeight);


        //获取sufaceview
        SurfaceView surfaceView = getSurfaceView();
        if (surfaceView == null) {
            throw new RuntimeException("SurfaceView can not be null");
        }
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this);
        }
    }

    public void onEncodePause(Application application) {
        //停止预览
        cameraManager.stopPreview();
        //移除所有消息通知
        Looper.myLooper().quit();
        handlerMainThread.removeMessages(MSG_WHAT_RESULT_DATA);
        handlerMainThread.removeMessages(MSG_WHAT_RESULT_DATA_ENCODE_SUCCESS);
        handlerMainThread.removeMessages(MSG_WHAT_RESULT_DATA_ENCODE_FAIL);

        //关闭相机驱动
        cameraManager.closeDriver();
        //关闭震动生意管理器
        beepManager.close();
        beepManager = null;

        beepManager.close();
        if (!hasSurface) {
            SurfaceView surfaceView = getSurfaceView();
            if (surfaceView == null) {
                throw new RuntimeException("SurfaceView can not be null");
            }
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
    }





    /*******************************************抽象方法********************************************/
    protected abstract int getSCanWidth();//获取扫描矩形的宽，这个扫描矩形会在屏幕的正中心
    protected abstract int getSCanHeight();//获取扫描矩形的高，这个扫描矩形会在屏幕的正中心
    protected abstract int getPreviewWidth();//获取预览窗口宽度，因为是旋转了90度，所以在使用的时候用作高度使用，但是获取的值还是实际窗口的宽度
    protected abstract int getPreviewHeight();//获取预览窗口高度，因为是旋转了90度，所以在使用的时候用作宽度使用，但是获取的值还是实际窗口的高度
    protected abstract SurfaceView getSurfaceView();//获取sufaceview
    protected abstract void cameraStartError();//相机启用异常，相机故障，可能是相关权限拍照和录像未打开，请尝试打开再重试。
    protected abstract void encodeQrCodeSuccess(@NonNull String data);//二维码解析成功
    protected abstract void encodeQrCodeFail();//二维码解析失败







    /******************************************自定义方法*******************************************/

    /**
     * 初始化相机
     * @param surfaceHolder
     */
    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            //开启预览
            cameraManager.startPreview();
            //设置数据返回操作的handler以及what值
            cameraManager.requestPreviewFrame(handlerMainThread,MSG_WHAT_RESULT_DATA);
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            cameraStartError();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
            cameraStartError();
        }
    }

    /**
     * 解析数据
     * @param data
     */
    private void encodeByteData(byte[] data){
        Result rawResult = null;
        PlanarYUVLuminanceSource source = cameraManager.buildLuminanceSource(data, getPreviewHeight(),getPreviewWidth());//宽高在传参时对调
        if (source != null) {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {
                rawResult = multiFormatReader.decodeWithState(bitmap);
            } catch (ReaderException re) {
                // continue
            } finally {
                multiFormatReader.reset();
            }
        }
        Message message = Message.obtain();
        if(rawResult != null){
            message.what = MSG_WHAT_RESULT_DATA_ENCODE_SUCCESS;
            message.obj = rawResult;
            handlerMainThread.sendMessage(message);
            logD("二维码解析成功，解析内容为：" + rawResult);
        }else {
            message.what = MSG_WHAT_RESULT_DATA_ENCODE_FAIL;
            handlerMainThread.sendMessage(message);
            logD("二维码解析失败");
        }
    }

    /**
     * 配置初始化二维码解析读取
     * @param characterSet
     */
    private void initMultiFormatReaderHints(String characterSet){
        hints = new EnumMap<>(DecodeHintType.class);

        // The prefs can't change while the thread is running, so pick them up once here.
        EnumSet<BarcodeFormat> decodeFormats = null;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        decodeFormats = EnumSet.noneOf(BarcodeFormat.class);
        if (prefs.getBoolean(PreferencesActivity.KEY_DECODE_1D_PRODUCT, true)) {
            decodeFormats.addAll(DecodeFormatManager.PRODUCT_FORMATS);
        }
        if (prefs.getBoolean(PreferencesActivity.KEY_DECODE_1D_INDUSTRIAL, true)) {
            decodeFormats.addAll(DecodeFormatManager.INDUSTRIAL_FORMATS);
        }
        if (prefs.getBoolean(PreferencesActivity.KEY_DECODE_QR, true)) {
            decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
        }
        if (prefs.getBoolean(PreferencesActivity.KEY_DECODE_DATA_MATRIX, true)) {
            decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
        }
        if (prefs.getBoolean(PreferencesActivity.KEY_DECODE_AZTEC, false)) {
            decodeFormats.addAll(DecodeFormatManager.AZTEC_FORMATS);
        }
        if (prefs.getBoolean(PreferencesActivity.KEY_DECODE_PDF417, false)) {
            decodeFormats.addAll(DecodeFormatManager.PDF417_FORMATS);
        }

        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);

        if (characterSet != null) {
            hints.put(DecodeHintType.CHARACTER_SET, characterSet);
        }
//        hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, resultPointCallback);
//        Log.i("DecodeThread", "Hints: " + hints);
    }




    /*******************************************回调方法********************************************/

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }



    /**
     * 显示debug
     * @param msg
     */
    private void logD(String msg){
        if(isShowLog){
            Log.d(TAG,msg);
        }
    }

    /**
     * 设置是否显示log信息
     * @param showLog
     * @return
     */
    public QrCodeEncode setShowLog(boolean showLog) {
        isShowLog = showLog;
        return this;
    }
}

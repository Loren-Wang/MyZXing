package com.myzxing.android.in;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
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
import com.myzxing.android.camera.CameraManager;
import com.myzxing.android.camera.PreferencesActivity;
import com.myzxing.android.decode.DecodeFormatManager;
import com.myzxing.android.decode.DecodeHandler;
import com.myzxing.android.decode.DecodeThread;
import com.myzxing.android.decode.ScanResultHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public abstract class QrCodeEncode implements SurfaceHolder.Callback {

    private Activity activity;
    private CameraManager cameraManager;
    private boolean hasSurface;

    public void onCreate(@NonNull Activity activity){
        this.activity = activity;
        // 保持Activity处于唤醒状态
        Window window = activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void onResume(){
        // CameraManager必须在这里初始化，而不是在onCreate()中。
        // 这是必须的，因为当我们第一次进入时需要显示帮助页，我们并不想打开Camera,测量屏幕大小
        // 当扫描框的尺寸不正确时会出现bug
        cameraManager = new CameraManager(activity);

        cameraManager.setManualFramingRect(0.1f,0.1f,0.8f,0.8f);


        SurfaceHolder surfaceHolder = getSurfaceView().getHolder();
        if(hasSurface) {
            // activity在paused时但不会stopped,因此surface仍旧存在；
            // surfaceCreated()不会调用，因此在这里初始化camera
            initCamera(surfaceHolder);
        } else {
            // 重置callback，等待surfaceCreated()来初始化camera
            surfaceHolder.addCallback(this);
        }
    }


    /***************************************自定义方法**********************************************/

    /**
     * 初始化Camera
     *
     * @param surfaceHolder
     */
    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            return;
        }
        try {
            // 打开Camera硬件设备
            cameraManager.openDriver(surfaceHolder);
            // 创建一个handler来打开预览，并抛出一个运行时异常
            if (scanResultHandler == null) {
                scanResultHandler = new ScanResultHandler(this);
            }
        } catch (IOException ioe) {
        } catch (RuntimeException e) {
        }
    }

    public void resetDecode(){
        //重置预览解码的回传
        cameraManager.requestPreviewFrame(decodeHandler,DECODE_DATA);
        try {
            handlerInitLatch.await();
        } catch (InterruptedException ie) {
            // continue?
        }
    }



    /****************************************抽象方法**********************************************/

    protected abstract SurfaceView getSurfaceView();//获取sufaceview
    protected abstract void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor);
    protected abstract void showScanImage(Bitmap bitmap);





    /****************************************回调方法**********************************************/
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        hasSurface = false;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }




    /**************************************扫描结果处理********************************************/
    private ScanResultHandler scanResultHandler;
    private final int SCAN_SUCCESS = 0x0000;//扫描成功
    private final int SCAN_FAIL = 0x0001;//扫描失败
    private final int SCAN_CROP_IMAGE = 0x0002;//扫描到的图片
    /**
     * 结果处理初始化
     */
    public void initScanResultHandler(){
        //初始化解码处理线程
        decodeThread = new DecodeThread(this);
        decodeThread.start();
        cameraManager.startPreview();
        resetDecode();

    }

    /**
     * 结果处理返回信息
     * @param message
     */
    public void handleScanResultMessage(Message message){
        Bundle bundle = message.getData();
        Bitmap barcode = null;
        float scaleFactor = 1.0f;


        switch (message.what){
            case SCAN_SUCCESS:
                if (bundle != null) {
                    byte[] compressedBitmap = bundle
                            .getByteArray(BARCODE_BITMAP);
                    if (compressedBitmap != null) {
                        barcode = BitmapFactory.decodeByteArray(compressedBitmap,
                                0, compressedBitmap.length, null);
                        // Mutable copy:
                        barcode = barcode.copy(Bitmap.Config.ARGB_8888, true);
                    }
                    scaleFactor = bundle
                            .getFloat(BARCODE_SCALED_FACTOR);
                }
                handleDecode((Result) message.obj,barcode,scaleFactor);
                break;
            case SCAN_CROP_IMAGE:
                bundle = message.getData();
                barcode = null;
                if (bundle != null) {
                    byte[] compressedBitmap = bundle
                            .getByteArray(BARCODE_BITMAP);
                    if (compressedBitmap != null) {
                        barcode = BitmapFactory.decodeByteArray(compressedBitmap,
                                0, compressedBitmap.length, null);
                        // Mutable copy:
                        barcode = barcode.copy(Bitmap.Config.ARGB_8888, true);
                    }
                    scaleFactor = bundle
                            .getFloat(BARCODE_SCALED_FACTOR);
                }
                showScanImage(barcode);
                break;
            case SCAN_FAIL:
                resetDecode();
                break;
            default:
                break;

        }
    }











    /****************************************解码处理**********************************************/
    private DecodeThread decodeThread;//解码线程
    private DecodeHandler decodeHandler;//解码结果处理

    private final int DECODE_DATA = 0x0000;

    private Map<DecodeHintType,Object> hints;
    private CountDownLatch handlerInitLatch;
    /**
     * 初始化解码线程
     */
    public void initDecodeThread(){
        handlerInitLatch = new CountDownLatch(1);
        hints = new EnumMap<DecodeHintType,Object>(DecodeHintType.class);

        // The prefs can't change while the thread is running, so pick them up once here.
        EnumSet<BarcodeFormat> decodeFormats = null;
        if (decodeFormats == null || decodeFormats.isEmpty()) {
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
        }
        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);

        Log.i("DecodeThread", "Hints: " + hints);
    }

    public void decodeThreadRun(){
        Looper.prepare();
        decodeHandler = new DecodeHandler(this);
        handlerInitLatch.countDown();
        Looper.loop();
    }





    private MultiFormatReader multiFormatReader;//读取二维码信息
    private boolean running = true;//是否在运行
    public static final String BARCODE_BITMAP = "barcode_bitmap";
    public static final String BARCODE_SCALED_FACTOR = "barcode_scaled_factor";
    /**
     * 初始化解码结果处理
     */
    public void initDecodeResultHandler(){
        multiFormatReader = new MultiFormatReader();
        multiFormatReader.setHints(hints);
    }

    /**
     * 解码消息回传
     * @param message
     */
    public void handleDecodeResultMessage(Message message){
        if (!running) {
            return;
        }
        switch (message.what) {
            case DECODE_DATA:
                decode((byte[]) message.obj, message.arg1, message.arg2);
                break;
//            case R.id.quit:
//                running = false;
//                Looper.myLooper().quit();
//                break;
        }
    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it
     * took. For efficiency, reuse the same reader objects from one decode to
     * the next.
     *解码
     * @param data
     *            The YUV preview frame.
     * @param width
     *            The width of the preview frame.
     * @param height
     *            The height of the preview frame.
     */
    private void decode(byte[] data, int width, int height) {
        long start = System.currentTimeMillis();
        Result rawResult = null;

        /***************竖屏更改3**********************/
        byte[] rotatedData = new byte[data.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++)
                rotatedData[x * height + height - y - 1] = data[x + y * width];
        }
        int tmp = width; // Here we are swapping, that's the difference to #11
        width = height;
        height = tmp;
        data = rotatedData;
        /*************************************/

        PlanarYUVLuminanceSource source = cameraManager
                .buildLuminanceSource(data, width, height);
        if (source != null) {
            Message message = Message.obtain(scanResultHandler, SCAN_CROP_IMAGE, rawResult);
            Bundle bundle = new Bundle();
            bundleThumbnail(source, bundle);
            message.setData(bundle);
            message.sendToTarget();
//
//            Log.d("testtest",source.getWidth() + "+++++" + source.getHeight());

            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {
                rawResult = multiFormatReader.decodeWithState(bitmap);
            } catch (ReaderException re) {
                // continue
            } finally {
                multiFormatReader.reset();
            }
        }

        if (rawResult != null) {
            Message message = Message.obtain(scanResultHandler, SCAN_SUCCESS, rawResult);
            Bundle bundle = new Bundle();
            bundleThumbnail(source, bundle);
            message.setData(bundle);
            message.sendToTarget();
            Log.d("test","解析成功");
        } else {
            Message message = Message.obtain(scanResultHandler, SCAN_FAIL, rawResult);
            message.sendToTarget();
            Log.d("test","解析失败");
        }
    }

    private static void bundleThumbnail(PlanarYUVLuminanceSource source,
                                        Bundle bundle) {
        int[] pixels = source.renderThumbnail();
        int width = source.getThumbnailWidth();
        int height = source.getThumbnailHeight();
        Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height,
                Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
        bundle.putByteArray(BARCODE_BITMAP, out.toByteArray());
        bundle.putFloat(BARCODE_SCALED_FACTOR, (float) width
                / source.getWidth());
    }

}

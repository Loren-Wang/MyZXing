　　在使用官方的框架和第三方的框架的时候发现了一个很大的问题，那就是扫描的区域就只有从屏幕中间扩展出来的扫描区域进行扫描，没有说可以自己定义扫描区域的方法，我很纳闷，也可能是我在读别人的源码的时候忽略掉了，但是仅仅只有这个问题也就算了，但是偏偏实现的逻辑是在activity中，如果仿照这个Activity在自己的项目中使用的话感觉通用性太低了，所以，强迫症犯了，干脆自己写一个工具类来给别人开放出去，让别人调用吧，于是**参考极光的调用方式以及https://github.com/yangxixi88/ZxingLite这个比人写的工具，以及官方代码：https://github.com/zxing/zxing这三个**开始写自己的工具吧。

　　官方的代码和别人的工具代码最终其实就三步，分别是界面创建时的操作、界面获取到焦点与失去焦点的操作，所以我就干脆直接在工具类中创建这三个方法，然后将Activity实例传入，用来给一些需要Activity实例的方法调用，然后Activity的oncreate、onpause、onresume这三个方法中分别调用工具类中的三个方法，用来初始化以及使用二维码扫描等操作。

　　具体使用不是很复杂，就是实例化工具类，调用相关方法就可以，至于具体的思路直接按调用方法一步一步向下走就可以了，入口其实就是onresume方法，从这里按方法向下走就可以，每一步都有解释的！


　　**其中的方法有：**

　　**1. onCreate(Activity activity);//在扫描界面创建的时候调用该方法，用来创建一些必须变量；**

　　**2. onResume();//在界面获得焦点的时候需要调用该方法，用来初始化一些管理器与扫描配置,但是在调用之前需要请求两个权限；**

　　**3. onPause();//在扫描界面的onpause方法中需要调用这个方法，用来在界面失去焦点的时候关闭不必要线程；**

　　**4. getSurfaceView();//获取sufaceview；**

　　**5. handleDecode(Result rawResult, Bitmap barcode, float scaleFactor);//扫描结果回传；**

　　**6. showScanImage(Bitmap bitmap);//当前正在扫描的灰度图；**

　　**7. getLeftPercent();//左侧非扫描界面百分比，范围（0-1）；**

　　**8. getTopPercent();//左侧非扫描界面百分比，范围（0-1）；**

　　**9. getRighttPercent();//左侧非扫描界面百分比，范围（0-1）；**

　　**10. getBottomPercent();//左侧非扫描界面百分比，范围（0-1）；**

　　**11. isRunningScan();//是否要扫描，因为在获得扫描结果后会回传到界面当中，但是，当回传的界面执行onpause方法后会重置扫描，所以需要这个值；**

　　**12. initCamera(SurfaceHolder surfaceHolder);//初始化相机参数；**

　　**13. resetDecode();//重置解码器，使其可以重新进行扫描操作；**

调用demo：

    package com.myzxingtest.android;

    import android.Manifest;
    import android.content.pm.PackageManager;
    import android.graphics.Bitmap;
    import android.os.Bundle;
    import android.support.v4.app.ActivityCompat;
    import android.support.v7.app.AppCompatActivity;
    import android.view.SurfaceView;
    import android.view.View;
    import android.widget.ImageView;
    import android.widget.Toast;

    import com.google.zxing.Result;
    import com.myzxing.android.in.QrCodeEncode;


    public class MainActivity extends AppCompatActivity {

    private SurfaceView surfaceView;
    private ImageView imgView;
    private QrCodeEncode qrCodeEncode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //异常信息收集
        CrashHandler catchHandler = CrashHandler.getInstance();
        catchHandler.init(getApplicationContext());

        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.sufaceView);
        imgView = findViewById(R.id.imgView);

        qrCodeEncode = new QrCodeEncode() {
            @Override
            protected SurfaceView getSurfaceView() {
                return surfaceView;
            }

            @Override
            protected void handleDecode(final Result rawResult, final Bitmap barcode, float scaleFactor) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imgView.setImageBitmap(barcode);
                        Toast.makeText(MainActivity.this, rawResult.getText(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            protected void showScanImage(final Bitmap bitmap) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imgView.setImageBitmap(bitmap);
                    }
                });
            }

            @Override
            protected float getLeftPercent() {
                return 0.4f;
            }

            @Override
            protected float getTopPercent() {
                return 0.4f;
            }

            @Override
            protected float getRighttPercent() {
                return 0.4f;
            }

            @Override
            protected float getBottomPercent() {
                return 0.4f;
            }

            @Override
            protected boolean isRunningScan() {
                return true;
            }
        };
        qrCodeEncode.onCreate(this);

        imgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imgView.setImageDrawable(null);
                qrCodeEncode.resetDecode();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        qrCodeEncode.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        qrCodeEncode.onResume();
    }

    }



代码地址：[https://github.com/Loren-Wang/MyZXing](https://github.com/Loren-Wang/MyZXing "https://github.com/Loren-Wang/MyZXing")
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

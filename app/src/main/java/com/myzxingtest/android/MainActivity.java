package com.myzxingtest.android;

import android.graphics.Bitmap;
import android.os.Bundle;
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
                        Toast.makeText(MainActivity.this,rawResult.getText(),Toast.LENGTH_LONG).show();
                    }
                });
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
//        qrCodeEncode.onEncodePause(getApplication());
    }

    @Override
    protected void onResume() {
        super.onResume();
        qrCodeEncode.onResume();
    }

}

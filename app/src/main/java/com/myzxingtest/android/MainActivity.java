package com.myzxingtest.android;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.Toast;

import com.myzxing.android.in.QrCodeEncode;


public class MainActivity extends AppCompatActivity {

    private SurfaceView surfaceView;
    private ImageView imgView;
    private int sCanWidth;
    private int sCanHeight;
    private QrCodeEncode qrCodeEncode = new QrCodeEncode() {

        @Override
        protected int getPreviewWidth() {
            return 1080;
        }

        @Override
        protected Rect getScanRect() {
            return new Rect(184,440,584,840);
        }

        @Override
        protected int getPreviewHeight() {
            return 1920;
        }

        @Override
        protected SurfaceView getSurfaceView() {
            return surfaceView;
        }

        @Override
        protected void cameraStartError() {

        }

        @Override
        protected void encodeQrCodeSuccess(@NonNull String data) {
            Toast.makeText(MainActivity.this,data,Toast.LENGTH_LONG);
        }

        @Override
        protected void encodeQrCodeFail() {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //异常信息收集
        CrashHandler catchHandler = CrashHandler.getInstance();
        catchHandler.init(getApplicationContext());

        setContentView(R.layout.activity_main);

        qrCodeEncode.onEncodeCreate(savedInstanceState,this);
        qrCodeEncode.setShowLog(true);
        surfaceView = findViewById(R.id.sufaceView);
        imgView = findViewById(R.id.imgView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        qrCodeEncode.onEncodePause(getApplication());
    }

    @Override
    protected void onResume() {
        super.onResume();
        qrCodeEncode.onEncodeResume(getApplication());
    }

}
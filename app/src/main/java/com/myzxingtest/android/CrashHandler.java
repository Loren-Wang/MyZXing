package com.myzxingtest.android;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *  功能：程序崩溃时崩溃log日志的打印，打印地址在 AppCommon.PROJECT_FILE_DIR + "crash/";中
 *  创建时间 2016.1.14
 *  创建人：wangliang_dev
 *
 */
public class CrashHandler implements UncaughtExceptionHandler {
    private String TAG="CrashHandler";

    // 系统默认的UncaughtException处理类
    private UncaughtExceptionHandler mDefaultHandler;
    // CrashHandler实例
    private static CrashHandler instance;
    // 程序的Context对象
    private Context mContext;
    // 用来存储设备信息和异常信息
    private Map infos = new HashMap();

    // 用于格式化日期,作为日志文件名的一部分
    private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");


    private CrashHandler() {
    }


    public static CrashHandler getInstance() {
        if (instance == null)
            instance = new CrashHandler();
        return instance;
    }


    public void init(Context context) {
        mContext = context;
        if(context != null) {
            Log.i(TAG, "CrashHandler init: success");
            // 获取系统默认的UncaughtException处理器
            mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
            // 设置该CrashHandler为程序的默认处理器
            Thread.setDefaultUncaughtExceptionHandler(this);
        }else {
            Log.i(TAG, "CrashHandler init: fail");
        }
    }


    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            // 如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Log.e(TAG, "error : ", e);
            }
            // 退出程序
            android.os.Process.killProcess(android.os.Process.myPid());
            try {
                System.exit(1);
            }catch (Exception e){
            }
        }
    }


    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        // 收集设备参数信息
        collectDeviceInfo(mContext);
//        if (ToolUtils.getInstance(mContext).isNotBackground(mContext)) {
//            // 使用Toast来显示异常信息
//            new Thread() {
//                @Override
//                public void run() {
//                    Looper.prepare();
//                    Toast.makeText(mContext, "很抱歉,程序出现异常,即将退出.", Toast.LENGTH_SHORT).show();
//                    Looper.loop();
//                }
//            }.start();
//        }
        // 保存日志文件
        try {
            saveCatchInfo2File(ex);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }


    public void collectDeviceInfo(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = pi.versionCode + "";
                infos.put("versionName", versionName);
                infos.put("versionCode", versionCode);
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "an error occured when collect package info", e);
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                infos.put(field.getName(), field.get(null).toString());
                Log.d(TAG, field.getName() + " : " + field.get(null));
            } catch (Exception e) {
                Log.e(TAG, "an error occured when collect crash info", e);
            }
        }
    }


    private String saveCatchInfo2File(Throwable ex) {

        StringBuffer sb = new StringBuffer();
        Iterator<Map.Entry> iterator = infos.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry entry = iterator.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            sb.append(key + "=" + value + "\n");
        }


        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);
        try {
            long timestamp = System.currentTimeMillis();
            String time = formatter.format(new Date());
            String fileName = "crash-" + time + "-" + timestamp + ".log";
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                String path = Environment.getExternalStorageDirectory().getPath() + "/zxingtest/";
                File dir = new File(path + fileName);
                IOUtils.writeToFile(dir,sb.toString());
                // 发送给开发人员
//                postErrorLog(path, fileName);
//                sendCrashLog2PM(path,fileName);
            }
            return fileName;
        } catch (Exception e) {
            Log.e(TAG, "an error occured while writing file...", e);
        }
        return null;
    }


    private void sendCrashLog2PM(String path, String fileName) {
        if (!new File(fileName).exists()) {
            Toast.makeText(mContext, "日志文件不存在！", Toast.LENGTH_SHORT).show();
            return;
        }
//        postErrorLog(path,fileName);
//        FileInputStream fis = null;
//        BufferedReader reader = null;
//        String s = null;
//        try {
//            fis = new FileInputStream(fileName);
//            reader = new BufferedReader(new InputStreamReader(fis, "GBK"));
//            while (true) {
//                s = reader.readLine();
//                if (s == null)
//                    break;
//                // 由于目前尚未确定以何种方式发送，所以先打出log日志。
//                Log.i("info", s.toString());
//
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally { // 关闭流
//            try {
//                reader.close();
//                fis.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }
//    private void postErrorLog(final String path, final String fileName) {
//        Handler handler = new Handler(){
//            @Override
//            public void handleMessage(Message msg) {
//                super.handleMessage(msg);
//                String token=(String)msg.obj;
//                CrashHandler.this.post(token, path, fileName);
//            }
//        };
//        long dateToken = SharedPrefUtils.getLong(AppCommon.QINIU_TOKEN_GET_TIME, 0);
//        long dateTokenNow = new Date().getTime();
//        if(dateToken==0l||(dateTokenNow-dateToken)>=7200000l) {
//            HttpStringsRequestUtils.getInstance(MyApplication.getContext()).getQiNiuToken(handler);//<从服务端SDK获取>;
//        }else {
//            String token=SharedPrefUtils.getString(AppCommon.QINIU_TOKEN_VALUE, null);
//            CrashHandler.this.post(token, path, fileName);
//        }
//
//    }
//
//    private void post(String token, String path, String fileName) {
//        // 重用 uploadManager。一般地，只需要创建一个 uploadManager 对象
//        UploadManager uploadManager = new UploadManager();
//        String data = path+fileName;
//        String key = AppCommon.POST_ERROR_LOG_PREFIX + UserModel.getInstance().getUserDto().getUserId() + "/" + fileName;
//        uploadManager.put(data, key, token,
//                new UpCompletionHandler() {
//                    @Override
//                    public void complete(String key, ResponseInfo info, JSONObject res) {
//                        //  res 包含hash、key等信息，具体字段取决于上传策略的设置。
//                    }
//                }, null);
//    }
}
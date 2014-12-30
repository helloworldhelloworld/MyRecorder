package com.xj.myapplication.myrecorder.demo;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.xj.myapplication.myrecorder.demo.R;



public class MainActivity extends Activity {

    private static final String LOG_TAG = "AudioRecordTest";
    //语音文件保存路径
    private String FileName = null;
    private String SUFFIX = ".amr";
    private String PREFIX = "Record_";
    //界面控件
    private Button startRecord;
    private Button startPlay;
    private Button stopRecord;
    private Button stopPlay;
    private Dialog mDialog;
    private boolean isRuning = true;
    private MyThread notifyThread;
    private long startTime;
    private String mBaseDirs = "/MyRecord";
    private File mFileDir = null;
    private File mRecordFile =null;

    //语音操作对象
    private MediaPlayer mPlayer = null;
    private MediaRecorder mRecorder = null;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //开始录音
        startRecord = (Button)findViewById(R.id.startRecord);
        startRecord.setText(R.string.startRecord);
        //绑定监听器
        startRecord.setOnClickListener(new startRecordListener());

        //结束录音
        stopRecord = (Button)findViewById(R.id.stopRecord);
        stopRecord.setText(R.string.stopRecord);
        stopRecord.setOnClickListener(new stopRecordListener());

        //开始播放
        startPlay = (Button)findViewById(R.id.startPlay);
        startPlay.setText(R.string.startPlay);
        //绑定监听器
        startPlay.setOnClickListener(new startPlayListener());

        //结束播放
        stopPlay = (Button)findViewById(R.id.stopPlay);
        stopPlay.setText(R.string.stopPlay);
        stopPlay.setOnClickListener(new stopPlayListener());

        //设置sdcard的路径
        FileName = Environment.getExternalStorageDirectory().getAbsolutePath();
//        FileName += "/audiorecordtest.amr";
        createDirs();
    }

    public String getFileName()
    {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd HH-mm-ss");
        Date curDate = new Date(System.currentTimeMillis());
        String time = formatter.format(curDate);
        System.out.println("curtime:" + time);
        return time;
    }

    public void createDirs()
    {
        String path = FileName+mBaseDirs;
        mFileDir = new File(path);
        if(!mFileDir.exists())
        {
            mFileDir.mkdirs();
            Log.v("录音 ","创建录音文件"+mFileDir.exists());
        }
    }

    //开始录音
    class startRecordListener implements OnClickListener{

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            try {
                startMediaPlayer();
            }catch (Exception e)
            {
                Log.e(LOG_TAG, e.getMessage());
            }

                notifyThread = new MyThread();
                notifyThread.start();
        }

    }

    private void startMediaPlayer() {
        String curTime = getFileName();
        mRecordFile = new File(mFileDir,PREFIX+curTime+SUFFIX);

        if(!mRecordFile.exists())
            try {
                mRecordFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        mRecorder = new MediaRecorder();
        mRecorder.reset();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setOutputFile(mRecordFile.getAbsolutePath());
        isRuning = true;
        try {
            mRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mRecorder.start();
    }

    private Handler handler =new Handler()
    {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what)
            {
                case 0:
                    showDialog();
                    break;

            }
        }
    };

    private void showDialog() {
        stopMediaPlayer();
        if(mDialog != null && mDialog.isShowing())
            return;
        mDialog  = new AlertDialog.Builder(MainActivity.this)
        .setMessage("是否继续录音").setTitle("提示")
        .setPositiveButton("确定", new Dialog.OnClickListener() {


            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                startMediaPlayer();

                synchronized (notifyThread) {

                    startTime = System.currentTimeMillis();
                    notifyThread.notifyAll();

                }

            }
        })
        .setNegativeButton("取消", new Dialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
                isRuning = false;
                startTime = System.currentTimeMillis();
                synchronized (notifyThread) {

                    notifyThread.notifyAll();

                }


            }
        }).create();

        mDialog.show();
    }

    public class MyThread extends Thread
    {
        public void run() {
            startTime = System.currentTimeMillis();
            while(isRuning){

                long longtime = (System.currentTimeMillis()-startTime)/1000;
                if(longtime >= 3)
                {
                    handler.sendEmptyMessage(0);

                    synchronized (this){
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    //停止录音
    class stopRecordListener implements OnClickListener{

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            stopMediaPlayer();
        }

    }

    private void stopMediaPlayer() {
        try{
            if(mRecorder != null) {
                mRecorder.stop();
                mRecorder.reset();
                mRecorder.release();
                mRecorder = null;
                componmentData();
            }
        }catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    private void showFileName()
    {

    }

    //播放录音
    class startPlayListener implements OnClickListener{

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            mPlayer = new MediaPlayer();
            try{
                mPlayer.setDataSource(mRecordFile.getAbsolutePath());
                mPlayer.prepare();
                mPlayer.start();
            }catch(IOException e){
                Log.e(LOG_TAG,"播放失败");
            }
        }

    }
    //停止播放录音
    class stopPlayListener implements OnClickListener{

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            mPlayer.release();
            mPlayer = null;
        }

    }

    private void clearFiles(String fileName)
    {
        for (File recordFile : mFileDir.listFiles()) {
            if(recordFile == null)
                break;
            if(!fileName.equals(recordFile.getName())&&
                    -1 != recordFile.getName().indexOf("amr") &&
                    -1!=recordFile.getName().indexOf(PREFIX)){
                if(recordFile.exists())
                    recordFile.delete();
            }
        }
    }

    private void componmentData() {
        String curTime = getFileName();
        File fileall= new File(mFileDir, PREFIX+curTime+SUFFIX);
        if(!fileall.exists())
            try {
                fileall.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        try {
            FileOutputStream fos = new FileOutputStream(fileall);

            int i = 0;
            for (File recordFile : mFileDir.listFiles()) {
                if(recordFile == null)
                    break;
                FileInputStream fis = null;
                if (!fileall.getName().equals(recordFile.getName()) &&
                        -1 != recordFile.getName().indexOf("amr") &&
                        -1!=recordFile.getName().indexOf(PREFIX)) {
                    fis = new FileInputStream(recordFile);
                    if(fis == null)
                        break;
                    byte[] recordByte = new byte[fis.available()];
                    int length = recordByte.length;
                    if (i == 0) {

                        while (fis.read(recordByte) != -1) {
                            fos.write(recordByte,0,length);
                        }
                    }
                    else
                    {
                        while(fis.read(recordByte) != -1)
                        {
                            fos.write(recordByte,6,length-6);
                        }

                    }
                    fos.flush();
                    fis.close();
                    i++;
                }

            }
            fos.close();
            clearFiles(fileall.getName());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

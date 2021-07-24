package com.example.opencvhand;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener {

    //界面组件定义
    private CameraBridgeViewBase openCvCameraView;
    TextView textView;

    //检测用训练文档载入变量
    private CascadeClassifier cascadeClassifier_play;
    private CascadeClassifier cascadeClassifier_pause;
    private CascadeClassifier cascadeClassifier_previous;
    private CascadeClassifier cascadeClassifier_next;
    private CascadeClassifier cascadeClassifier_forward;
    private CascadeClassifier cascadeClassifier_backoff;

    //消息类型用FLAG
    final int FLAG_CHECK = 0;
    final int FLAG_PLAY = 1;
    final int FLAG_PAUSE = 2;
    final int FLAG_PREVIOUS = 3;
    final int FLAG_NEXT = 4;
    final int FLAG_FORWARD = 5;
    final int FLAG_BACKOFF = 6;
    final int FLAG_REFRESH = 7;

    //定义OpenCV使用的图片类型 变量定义
    private Mat grayscaleImage;
    private int absoluteHandSize;

    //开启Service用Intent
    Intent intent = new Intent("com.example.service.MUSIC_SERVICE");

    //检测用FLAG
    int FLAG_THREAD_RUNNING = 0;//检测线程是否在运行
    int FLAG_PLAYED = 0;//是否播放过
    int FLAG_PLAYING = 0;//是否正在播放
    int FLAG_TIMER = 0;//识别手势的有效时间计数器

    //正确手势次数（减少误判用）
    AtomicInteger correct = new AtomicInteger();

    private MusicService.MyBinder binder;//用于接收Service回调用的binder
    private CheckThread thread= new CheckThread();//检查线程定义

    //Service回调用connection
    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (MusicService.MyBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    //检测音频用线程
    class CheckThread extends Thread
    {
        @Override
        public void run() {
            super.run();
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    myHandler.sendEmptyMessage(FLAG_CHECK);

                }
            }, 0,1000);
        }
    }

    //Camera触发修改UI用handler
    final Handler myHandler = new Handler()
    {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what)
            {
                case FLAG_CHECK:
                    textView.setText(binder.getname());
                    break;


                case FLAG_PLAY:
                    checkThread();

                    if(FLAG_PLAYED == 0){
                        binder.play();
                        FLAG_PLAYED = 1;
                    }else {
                        binder.goplay();
                    }

                    timeInterval();
                    myHandler.sendEmptyMessage(FLAG_CHECK);
                    break;


                case FLAG_PAUSE:
                    checkThread();

                    binder.pause();

                    timeInterval();
                    myHandler.sendEmptyMessage(FLAG_CHECK);
                    break;


                case FLAG_REFRESH:
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            correct.set(0);
                            FLAG_TIMER = 0;
                        }
                    }, 1000);
                    break;

            }
        }
    };

    //检测检测进程是否已经开启
    private void checkThread()
    {
        if(FLAG_THREAD_RUNNING == 0) {
            thread.start();
            FLAG_THREAD_RUNNING = 1;
        }
    }

    //音频操作后刷新时间间隔
    private void timeInterval()
    {
        openCvCameraView.disableView();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                openCvCameraView.enableView();
                correct.set(0);
                FLAG_TIMER = 0;
            }
        }, 3000);

    }

    //加载OpenCV
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    initializeOpenCVDependencies();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    //初始化OpenCV依赖
    private void initializeOpenCVDependencies() {

        try {
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = getResources().openRawResource(R.raw.cascade_play2);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "cascade_play2.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);


            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            // Load the cascade classifier
            cascadeClassifier_play = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading cascade", e);
        }

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //需要在完成生命周期之后才使用binder的方法
        intent.setPackage("com.example.opencvhand");
        bindService(intent, conn, Context.BIND_AUTO_CREATE);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        openCvCameraView = new JavaCameraView(this, -1);
        openCvCameraView = (JavaCameraView)findViewById(R.id.javaCameraView);
        openCvCameraView.setCvCameraViewListener(this);
        openCvCameraView.setRotation(0);//旋转界面
//        openCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);//前置摄像头 CameraBridgeViewBase.CAMERA_ID_BACK为后置摄像头
        // And we are ready to go
        openCvCameraView.enableView();

        //绑定界面
        textView = (TextView)findViewById(R.id.songName);


        //初始化非基本数据类型对象
        correct.set(0);

    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(conn);
        stopService(intent);
    }


    //Camera开启时设置输入图片要转化成的格式
    @Override
    public void onCameraViewStarted(int width, int height) {
        grayscaleImage = new Mat(height, width, CvType.CV_32FC4);


        // The faces will be a 20% of the height of the screen
        absoluteHandSize = (int) (height * 0.2);
    }


    @Override
    public void onCameraViewStopped() {
    }


    //Camera检测过程
    @Override
    public Mat onCameraFrame(Mat aInputFrame) {
        // Create a grayscale image
        Imgproc.cvtColor(aInputFrame, grayscaleImage, Imgproc.COLOR_RGBA2RGB);

        //检测手势
        MatOfRect play = new MatOfRect();

        // Use the classifier to detect hands
        if (cascadeClassifier_play != null) {
            cascadeClassifier_play.detectMultiScale(aInputFrame, play, 1.2, 18, 2,
                    new Size(absoluteHandSize, absoluteHandSize), new Size());
        }

        Rect[] playArray = play.toArray();
        if(playArray.length != 0)
        {
            //开启减少误判计时器
            if(FLAG_TIMER ==0) {
                myHandler.sendEmptyMessage(FLAG_REFRESH);
                FLAG_TIMER = 1;
            }

            //识别次数加一
            correct.getAndIncrement();

            //识别次数足够
            if(correct.get()==5){
                if(FLAG_PLAYING ==0){//未播放状态
                    myHandler.sendEmptyMessage(FLAG_PLAY);
                    FLAG_PLAYING = 1;
                }else if(FLAG_PLAYING == 1){//在播放状态
                    myHandler.sendEmptyMessage(FLAG_PAUSE);
                    FLAG_PLAYING = 0;
                }
            }
        }

        // If there are any faces found, draw a rectangle around it
        for (int i = 0; i <playArray.length; i++){
            Imgproc.rectangle(aInputFrame, playArray[i].tl(), playArray[i].br(), new Scalar(0, 255, 0, 255), 3);
        }


        return aInputFrame;
    }


    //onResume时需要重新依赖
    @Override
    public void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.e("log_wons", "OpenCV init error");
            // Handle initialization error
        }
        initializeOpenCVDependencies();
        //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
    }
}

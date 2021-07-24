package com.example.opencvhand;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.List;

public class MusicService extends Service {

    String[] musicfiles = new String[10];//音乐文件名列表
    MediaPlayer player = new MediaPlayer();//播放器
    int songNum = 0;//在播放的歌曲在数组中的下标

    private MyBinder binder = new MyBinder();

    public class MyBinder extends Binder
    {
        //第一次播放以及播放源配置
        public void play() {
            if (!player.isPlaying()) {
                try {
                    player.reset();//重置多媒体
                    AssetFileDescriptor fd = getAssets().openFd(musicfiles[songNum]);//获取文件
                    //指定参数为音频文件
                    player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    player.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());//设置播放源
                    player.prepare();//准备播放
                    player.start();//开始播放

                    //setOnCompletionListener 当前多媒体对象播放完成时发生的事件
                    player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            next();
                        }
                    });
                } catch (IOException e) {
//                e.printStackTrace();
                    Log.v("MusicService", e.getMessage());
                }
            }
        }


        //继续播放
        public void goplay() {
            int position = getCurrentProgress();
            player.seekTo(position);
            player.start();
        }

        //暂停
        public void pause() {
            if(player != null && player.isPlaying()){
                player.pause();
            }
        }

        //前一首
        public void previous() {
            songNum=(songNum+2)% 3 ;
            play();
        }


        //下一首
        public void next(){
            songNum=(songNum+1)% 3 ;
            play();
        }


        //前进15秒
        public void forward(){

            pause();
            int position;
            if ((position=getCurrentProgress()+15000)>=player.getDuration()) {
                position = player.getDuration();
            }
            else {
                position=getCurrentProgress()+15000;
            }

            player.seekTo(position);
            player.start();

        }


        //后退15秒
        public void backoff(){

            pause();
            int position;
            if ((position=getCurrentProgress())>15000) {
                position = getCurrentProgress()- 15000;
            }
            else {
                position = 0;
            }

            player.seekTo(position);
            player.start();

        }



        //更新播放的歌曲名字
        public String getname()
        {
            return musicfiles[songNum];
        }



        // 获取当前进度
        private int getCurrentProgress() {
            if (player != null & player.isPlaying()) {
                return player.getCurrentPosition();
            }
            else if (player != null & (!player.isPlaying()))
            {
                return player.getCurrentPosition();
            }
            return 0;
        }

    }

    public void onCreate() {
        super.onCreate();

        String[] assetManager = new String[10];
        try {
            assetManager = getResources().getAssets().list("");
        } catch (IOException e) {
            e.printStackTrace();
        }

        int i =0;
        for (String string: assetManager)
        {
            if(string.endsWith(".mp3")) {
                musicfiles[i] = string;
                i++;
            }
        }



    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}

//ссылка на источник кода
//https://inducesmile.com/android-programming/how-to-play-rtsp-stream-in-android-videoview/

package example.com.rtspvideoview;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import example.com.videoviewproject.R;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, IVLCVout.Callback{
    final static int PERMISSION_REQUEST_CODE_INTERNET = 2;
    ConnectivityManager cm;
    EditText etAdress;
    Button btHttpView, btExampleView;
    String videoSource;

    private final static String TAG = "Rtsp Video";
    private String mFilePath;
    private SurfaceView mSurface;
    private SurfaceHolder holder;
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
    private int mVideoWidth;// = 2;
    private int mVideoHeight;// = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        etAdress = findViewById(R.id.etAdress);
        btHttpView = findViewById(R.id.btHttpView);     btHttpView.setOnClickListener(this);
        btExampleView = findViewById(R.id.btExampleView); btExampleView.setOnClickListener(this);

        mSurface = (SurfaceView)findViewById(R.id.surfaceview);
//        mFilePath = "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov";
        Log.d(TAG, "Playing: " + mFilePath);
        holder = mSurface.getHolder();
//работает!!!        etAdress.setText("http://www.kakvse.net/uploads/posts/2019-08/video/raznoe/Voditel'_vovremya_sdal_vpravo_inache_prishlos'_by_vezti_avto_v_remont.mp4");
        etAdress.setText("rtsp://v5.cache1.c.youtube.com/CjYLENy73wIaLQklThqIVp_AsxMYESARFEIJbXYtZ29vZ2xlSARSBWluZGV4YIvJo6nmx9DvSww=/0/0/0/video.3gp");
        requestInternet();

    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setSize(mVideoWidth, mVideoHeight);

        videoSource = "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov";
        createPlayer(videoSource);
    }

    @Override
    protected void onPause() {
        super.onPause();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btHttpView : {
                requestInternet();
                videoSource = etAdress.getText().toString().trim();

                mMediaPlayer.stop();
                Media m = new Media(libvlc, Uri.parse(videoSource));
                mMediaPlayer.setMedia(m);
                mMediaPlayer.play();
                break;
            }
            case R.id.btExampleView : {
                requestInternet();
                videoSource = "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov";

                mMediaPlayer.stop();
                Media m = new Media(libvlc, Uri.parse(videoSource));
                mMediaPlayer.setMedia(m);
                mMediaPlayer.play();
                break;
            }
            default: break;
        }
    }

    /**
     * Used to set size for SurfaceView
     *
     * @param width
     * @param height
     */
    private void setSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        if (mVideoWidth * mVideoHeight <= 1)
            return;

        if (holder == null || mSurface == null)
            return;

        int w = getWindow().getDecorView().getWidth();
        int h = getWindow().getDecorView().getHeight();
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (w > h && isPortrait || w < h && !isPortrait) {
            int i = w;
            w = h;
            h = i;
        }

        float videoAR = (float) mVideoWidth / (float) mVideoHeight;
        float screenAR = (float) w / (float) h;

        if (screenAR < videoAR)
            h = (int) (w / videoAR);
        else
            w = (int) (h * videoAR);

        holder.setFixedSize(mVideoWidth, mVideoHeight);
        ViewGroup.LayoutParams lp = mSurface.getLayoutParams();
        lp.width = w;
        lp.height =  h;
        mSurface.setLayoutParams(lp);
        mSurface.invalidate();
    }

    /**
     * Creates MediaPlayer and plays video
     *
     * @param media
     */
    private void createPlayer(String media) {
        releasePlayer();
        try {
            if (media.length() > 0) {
                Toast toast = Toast.makeText(this, media, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0,
                        0);
                toast.show();
            }

            // Create LibVLC
            // TODO: make this more robust, and sync with audio demo
            ArrayList<String> options = new ArrayList<String>();
            //options.add("--subsdec-encoding <encoding>");
            options.add("--aout=opensles");
            options.add("--audio-time-stretch"); // time stretching
            options.add("-vvv"); // verbosity
            libvlc = new LibVLC(this, options);
            holder.setKeepScreenOn(true);

            // Creating media player
            mMediaPlayer = new MediaPlayer(libvlc);
            mMediaPlayer.setEventListener(mPlayerListener);

            // Seting up video output
            final IVLCVout vout = mMediaPlayer.getVLCVout();
            vout.setVideoView(mSurface);
            //vout.setSubtitlesView(mSurfaceSubtitles);
            vout.addCallback(this);
            vout.attachViews();

            Media m = new Media(libvlc, Uri.parse(media));
            mMediaPlayer.setMedia(m);
            mMediaPlayer.play();
        } catch (Exception e) {
            Toast.makeText(this, "Error in creating player!", Toast
                    .LENGTH_LONG).show();
        }
    }

    private void releasePlayer() {
        if (libvlc == null)
            return;
        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(this);
        vout.detachViews();
        holder = null;
        libvlc.release();
        libvlc = null;

        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    /**
     * Registering callbacks
     */
    private MediaPlayer.EventListener mPlayerListener = new MyPlayerListener(this);

/*
    @Override
    public void onNewLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        if (width * height == 0)
            return;

        // store video size
        mVideoWidth = width;
        mVideoHeight = height;
        setSize(mVideoWidth, mVideoHeight);
    }
*/
    @Override
    public void onSurfacesCreated(IVLCVout vout) {

    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vout) {

    }

/*    @Override
    public void onHardwareAccelerationError(IVLCVout vlcVout) {
        Log.e(TAG, "Error with hardware acceleration");
        this.releasePlayer();
        Toast.makeText(this, "Error with hardware acceleration", Toast.LENGTH_LONG).show();
    }
*/
    private static class MyPlayerListener implements MediaPlayer.EventListener {
        private WeakReference<MainActivity> mOwner;

        public MyPlayerListener(MainActivity owner) {
            mOwner = new WeakReference<MainActivity>(owner);
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
            MainActivity player = mOwner.get();

            switch (event.type) {
                case MediaPlayer.Event.EndReached:
                    Log.d(TAG, "MediaPlayerEndReached");
                    player.releasePlayer();
                    break;
                case MediaPlayer.Event.Playing:
                case MediaPlayer.Event.Paused:
                case MediaPlayer.Event.Stopped:
                default:
                    break;
            }
        }
    }













    //функция ответов на запрос разрешений
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //если пришел ответ на запрос разрешения интернета
        if (requestCode == PERMISSION_REQUEST_CODE_INTERNET && grantResults.length == 2) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Повторите запуск видео из интернета", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Без выдачи разрешения для работы с интернетом, просмотр не возможен", Toast.LENGTH_SHORT).show();

            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    //Запрос разрешения интернета
    private void requestInternet() {
        // Запрос "опасного" разрешения работы с интернетом
        int internetStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
        //если разрешения нет, запрос на получение разрешения
        //ответ на запрос обрабатывается в функции onRequestPermissionsResult()
        if (internetStatus != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.INTERNET},
                    PERMISSION_REQUEST_CODE_INTERNET);
        }
    }
    //проверка связи с Интернетом
    public boolean isOnline() {
        NetworkInfo nInfo = cm.getActiveNetworkInfo();
        if (nInfo != null && nInfo.isConnected()) {
            Toast.makeText(this, "ONLINE", Toast.LENGTH_SHORT).show();
            return true;
        }
        else {
            Toast.makeText(this, "ONLINE", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}


package com.bipedface.ajithvalappil.bipedface;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentValues;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.net.wifi.*;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.AndroidCharacter;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.widget.FrameLayout;
import android.view.OrientationEventListener;
import android.content.Context;
import android.hardware.SensorManager;
import android.widget.ImageView;
import android.os.Environment;
import java.util.*;
import java.io.*;
import 	android.widget.EditText;
import android.widget.VideoView;

import com.bipedface.ajithvalappil.bipedface.util.SystemUiHider;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class FullscreenActivity extends Activity  implements SurfaceHolder.Callback{
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    //Camera
    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    boolean previewing = false;
    int numberOfFaces = 0;
    private FaceOverlayView mFaceView;
    static EditText editText;
    static float left = 0;
    static float top = 0;
    static float right = 0;
    static float bottom = 0;

    static boolean faceFound = false;
    static int headPosVertical = 1500;
    static int minVerPos = 1500;
    static int maxVerPos = 2000;

    static int headPosHorizontal = 1500;
    static int minHorPos = 550;
    static int maxHorPos = 2400;

    // We need the phone orientation to correctly draw the overlay:
    private int mOrientation;
    private int mOrientationCompensation;
    private OrientationEventListener mOrientationEventListener;

    // Let's keep track of the display rotation and orientation also:
    private int mDisplayRotation;
    private int mDisplayOrientation;
    private ImageView imageView;
    private List<String> imgList = new ArrayList<String>();
    private int imgCount = 0;
    boolean showHiFaces = false;
    long photoTime = System.currentTimeMillis();

    static int servoDetailsX = 0;
    static int servoDetailsY = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_fullscreen);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        getWindow().setFormat(PixelFormat.UNKNOWN);

        final View contentView = findViewById(R.id.surfaceView);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }



            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.


        surfaceView = (SurfaceView)findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mFaceView = new FaceOverlayView(this);
        addContentView(mFaceView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT , FrameLayout.LayoutParams.MATCH_PARENT));

        // Create and Start the OrientationListener:
        mOrientationEventListener = new SimpleOrientationEventListener(this);
        mOrientationEventListener.enable();

        editText = (EditText) findViewById(R.id.editText);

        try {
            WifiManager wifiMgr = (WifiManager) getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            int ip = wifiInfo.getIpAddress();
            String ipAddress = android.text.format.Formatter.formatIpAddress(ip);
            //editText.setText("IP:" + ipAddress + ":8080/");

            VideoView videoView = (VideoView)findViewById(R.id.videoView);
            videoView.setVideoPath("/storage/emulated/0/robot/eyes/eyes.mp4");
            videoView.start();
        }catch(Exception ee){

        }

    }

    AutoFocusCallback myAutoFocusCallback = new AutoFocusCallback(){

        @Override
        public void onAutoFocus(boolean arg0, Camera arg1) {
            // TODO Auto-generated method stub
        }};

    ShutterCallback myShutterCallback = new ShutterCallback(){

        @Override
        public void onShutter() {
            // TODO Auto-generated method stub

        }};

    PictureCallback myPictureCallback_RAW = new PictureCallback(){

        @Override
        public void onPictureTaken(byte[] arg0, Camera arg1) {
            // TODO Auto-generated method stub

        }};
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
    PictureCallback myPictureCallback_JPG = new PictureCallback(){

        @Override
        public void onPictureTaken(byte[] arg0, Camera arg1) {
            // TODO Auto-generated method stub
            camera.startPreview();
            camera.startFaceDetection();


            showHiFaces = true;
            File mediaStorageDir = new File("/storage/emulated/0/face");
            File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "face.jpg");

            FileOutputStream imageFileOS;
            try {
                imageFileOS = new FileOutputStream(mediaFile);
                imageFileOS.write(arg0);
                imageFileOS.flush();
                imageFileOS.close();

                System.out.println("Image saved: " + mediaFile.getPath());
                showHiFaces = false;
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        // TODO Auto-generated method stub
        if(previewing){
            camera.stopFaceDetection();
            camera.stopPreview();
            previewing = false;
        }

        if (camera != null){
            try {

                System.out.println(String.valueOf( ">>>Max Face: " + camera.getParameters().getMaxNumDetectedFaces()));
                Camera.Parameters parameters = camera.getParameters();
                List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
                Camera.Size previewSize = previewSizes.get(0);
                // And set them:
                parameters.setPreviewSize(previewSize.width, previewSize.height);
                camera.setParameters(parameters);
                camera.setPreviewDisplay(surfaceHolder);

                // Now set the display orientation for the camera. Can we do this differently?
                mDisplayRotation = Util.getDisplayRotation(FullscreenActivity.this);
                mDisplayOrientation = Util.getDisplayOrientation(mDisplayRotation, 0);
                camera.setDisplayOrientation(mDisplayOrientation);

                if (mFaceView != null) {
                    mFaceView.setDisplayOrientation(mDisplayOrientation);
                }

                camera.startPreview();

                camera.startFaceDetection();
                previewing = true;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        camera = Camera.open(1);
        camera.setFaceDetectionListener(faceDetectionListener);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        camera.stopFaceDetection();
        camera.stopPreview();
        camera.release();
        camera = null;
        previewing = false;
    }

    FaceDetectionListener faceDetectionListener
            = new FaceDetectionListener(){

        @Override
        public void onFaceDetection(Camera.Face[] faces, Camera camera) {
            mFaceView.setFaces(faces);
            if (faces.length == 0){
                System.out.println(" No Face Detected! ");
            }else{
                takePicture(camera);
            }


        }
    };

    /**
     * We need to react on OrientationEvents to rotate the screen and
     * update the views.
     */
    private class SimpleOrientationEventListener extends OrientationEventListener {

        public SimpleOrientationEventListener(Context context) {
            super(context, SensorManager.SENSOR_DELAY_NORMAL);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            // We keep the last known orientation. So if the user first orient
            // the camera then point the camera to floor or sky, we still have
            // the correct orientation.
            if (orientation == ORIENTATION_UNKNOWN) return;
            mOrientation = Util.roundOrientation(orientation, mOrientation);
            // When the screen is unlocked, display rotation may change. Always
            // calculate the up-to-date orientationCompensation.
            int orientationCompensation = mOrientation
                    + Util.getDisplayRotation(FullscreenActivity.this);
            if (mOrientationCompensation != orientationCompensation) {
                mOrientationCompensation = orientationCompensation;
                mFaceView.setOrientation(mOrientationCompensation);
            }
        }
    }

    public void takePicture(final Camera cam){

        Runnable runnable = new Runnable() {
            public void run() {
                /*long endTime = System.currentTimeMillis();
                long currentTime = endTime - photoTime;
                int tTime = (int) (currentTime /1000);
                //System.out.println("tTime:" + tTime);
                if (tTime > 5){*/
                    cam.takePicture(null,null, myPictureCallback_JPG);
                  /*  photoTime = System.currentTimeMillis();
                }*/
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }

}

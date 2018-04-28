package com.bipedface.ajithvalappil.bipedface;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera.Face;
import android.os.StrictMode;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ajithvalappil2 on 3/14/15.
 */
public class FaceOverlayView extends View {

    private Paint mPaint;
    private Paint rPaint;
    private Paint mTextPaint;
    private int mDisplayOrientation;
    private int mOrientation;
    private Face[] mFaces;

    public FaceOverlayView(Context context) {
        super(context);
        initialize();
    }

    private void initialize() {
        // We want a green box around the face:
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.GREEN);
        //mPaint.setAlpha(128);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(5);

        rPaint = new Paint();
        rPaint.setAntiAlias(true);
        rPaint.setDither(true);
        rPaint.setColor(Color.GREEN);
        //mPaint.setAlpha(128);
        rPaint.setStyle(Paint.Style.STROKE);
        rPaint.setStrokeWidth(5);

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setDither(true);
        mTextPaint.setTextSize(20);
        mTextPaint.setColor(Color.GREEN);
        mTextPaint.setStyle(Paint.Style.FILL);
    }

    public void setFaces(Face[] faces) {
        mFaces = faces;
        invalidate();
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    public void setDisplayOrientation(int displayOrientation) {
        mDisplayOrientation = displayOrientation;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        FullscreenActivity.left = (getWidth()/2)- 80;
        FullscreenActivity.top = (getHeight()/2)- 80;
        FullscreenActivity.right = (getWidth()/2) + 80;
        FullscreenActivity.bottom = (getHeight()/2) + 80;

        canvas.drawRect(FullscreenActivity.left, FullscreenActivity.top, FullscreenActivity.right, FullscreenActivity.bottom, rPaint);

        if (mFaces != null && mFaces.length > 0) {
            FullscreenActivity.faceFound = true;
            Matrix matrix = new Matrix();
            Util.prepareMatrix(matrix, true, mDisplayOrientation, getWidth(), getHeight());
            canvas.save();
            matrix.postRotate(mOrientation);
            canvas.rotate(-mOrientation);
            RectF rectF = new RectF();
            for (Face face : mFaces) {
                rectF.set(face.rect);
                matrix.mapRect(rectF);
                canvas.drawRect(rectF, mPaint);
                canvas.drawText("Score " + face.score, rectF.right, rectF.top, mTextPaint);
                //draw x axis
                canvas.drawCircle(rectF.centerX(),rectF.centerY(), 10, mPaint);

                if (FullscreenActivity.left!=0 &&  rectF.centerX() < FullscreenActivity.left){
                    FullscreenActivity.servoDetailsX = 1;
                }else if (FullscreenActivity.right!=0 &&  rectF.centerX() > FullscreenActivity.right){
                    FullscreenActivity.servoDetailsX = -1;
                }else{
                    FullscreenActivity.servoDetailsX = 0;
                }

                if (FullscreenActivity.top!=0 &&  rectF.centerY() < FullscreenActivity.top){
                    FullscreenActivity.servoDetailsY = -1;
                }else if (FullscreenActivity.bottom!=0 &&  rectF.centerY() > FullscreenActivity.bottom){
                    FullscreenActivity.servoDetailsY = 1;
                }else{
                    FullscreenActivity.servoDetailsY = 0;
                }
                FullscreenActivity.editText.setText("servoDetails:" + FullscreenActivity.servoDetailsX + "" + FullscreenActivity.servoDetailsY);


                //send data to raspberrypi
                String dataChar = "";
                if (FullscreenActivity.servoDetailsX == -1){
                    FullscreenActivity.headPosHorizontal = FullscreenActivity.headPosHorizontal + FullscreenActivity.servoDetailsX;
                    if (FullscreenActivity.headPosHorizontal < FullscreenActivity.minHorPos){
                        FullscreenActivity.headPosHorizontal = FullscreenActivity.minHorPos;
                    }
                    dataChar = "r";
                    POST(dataChar + "@" + FullscreenActivity.headPosHorizontal);
                }
                if (FullscreenActivity.servoDetailsX == +1){
                    FullscreenActivity.headPosHorizontal = FullscreenActivity.headPosHorizontal + FullscreenActivity.servoDetailsX;
                    if (FullscreenActivity.headPosHorizontal < FullscreenActivity.maxHorPos){
                        FullscreenActivity.headPosHorizontal = FullscreenActivity.maxHorPos;
                    }
                    dataChar = "l";
                    POST(dataChar + "@" + FullscreenActivity.headPosHorizontal);
                }

                if (FullscreenActivity.servoDetailsY == -1){
                    FullscreenActivity.headPosVertical = FullscreenActivity.headPosVertical + FullscreenActivity.servoDetailsY;
                    if (FullscreenActivity.headPosVertical < FullscreenActivity.minVerPos){
                        FullscreenActivity.headPosVertical = FullscreenActivity.minVerPos;
                    }

                    dataChar = "t";
                    POST(dataChar + "@" + FullscreenActivity.headPosVertical);
                }
                if (FullscreenActivity.servoDetailsY == +1){
                    FullscreenActivity.headPosVertical = FullscreenActivity.headPosVertical + FullscreenActivity.servoDetailsY;
                    if (FullscreenActivity.headPosVertical < FullscreenActivity.maxVerPos){
                        FullscreenActivity.headPosVertical = FullscreenActivity.maxVerPos;
                    }
                    dataChar = "b";
                    POST(dataChar + "@" + FullscreenActivity.headPosVertical);
                }


            }
            canvas.restore();
        }
    }

    public static void POST(String data){
        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

            StrictMode.setThreadPolicy(policy);
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://10.0.1.25:8000/macros/positionhead/" + data);
            HttpResponse response = client.execute(post);
            System.out.println(response.getStatusLine().getStatusCode());
            try {
                //Thread.sleep(1000);
            }catch(Exception e){

            }

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
        }

    }


}

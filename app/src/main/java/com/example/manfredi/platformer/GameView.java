package com.example.manfredi.platformer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by Manfredi on 01/03/2017.
 */

public class GameView extends SurfaceView implements Runnable {
    private static final String TAG = "GameView";
    private static final int BG_COLOR = Color.rgb(135, 206, 235);//sky blue

    private volatile boolean mIsRunning = false;
    private Thread mGameThread = null;
    private Canvas mCanvas;
    private SurfaceHolder mSurfaceHolder;
    Context mContext = null;
    Paint mPaint = null;

    private boolean mDebugging = true;

    FrameTimer mTimer;
    Viewport mCamera;
    LevelManager mLevelManager;


    private static final int METERS_TO_SHOW_X = 16; //TODO: move to xml
    private static final int METERS_TO_SHOW_Y = 9;
    private static final int STAGE_WIDTH = 1920/3;
    private static final int STAGE_HEIGHT = 1080/3;
    private static final boolean SCALE_CONTENT = true;

    public GameView(final Context context) {
        super(context);
        mContext = context;
        mPaint = new Paint();
        mSurfaceHolder = getHolder();
        mTimer = new FrameTimer();
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        if (SCALE_CONTENT) {
            mSurfaceHolder.setFixedSize(STAGE_WIDTH, STAGE_HEIGHT);
            screenWidth = STAGE_WIDTH;
            screenHeight = STAGE_HEIGHT;
        }
        mCamera = new Viewport(screenWidth, screenHeight, METERS_TO_SHOW_X, METERS_TO_SHOW_Y);
        loadLevel("TestLevel");
    }

    private void loadLevel(String levelName) {
        mLevelManager = new LevelManager(mContext, mCamera.getPixelsPerMetreX(), levelName);
        mCamera.setWorldCentre(mLevelManager.mPlayer.mWorldLocation);

    }

    public void pause() {
        mIsRunning = false;
        try {
            mGameThread.join();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        mIsRunning = true;
        mGameThread = new Thread(this);
        try {
            mGameThread.start();
        }
        catch(IllegalThreadStateException e) {
            e.printStackTrace();
        }
    }

    public void destroy() {

    }

    private void update(long dt) {
        for(GameObject go : mLevelManager.mGameObjects) {
            go.update(dt);
            go.mIsVisible = mCamera.inView(go.mWorldLocation, go.mWidth, go.mHeight);
        }
        mCamera.setWorldCentre(mLevelManager.mPlayer.mWorldLocation);
    }

    private void render() {
        if (!lockAndSetCanvas()) {
            return;
        }
        Point screenCoord = new Point();
        mCanvas.drawColor(BG_COLOR);
        mPaint.setColor(Color.WHITE);

        for(GameObject go : mLevelManager.mGameObjects) {
            if(!go.mIsVisible) {
                //continue;
            }
            mCamera.worldToScreen(go.mWorldLocation, screenCoord);
            Bitmap b = mLevelManager.getBitmap(go.mType);
            mCanvas.drawBitmap(b, screenCoord.x, screenCoord.y, mPaint);
        }

        if(mDebugging) {
            doDebugDrawing();
        }
        mSurfaceHolder.unlockCanvasAndPost(mCanvas);
    }

    private void doDebugDrawing() {
        int y = 60;
        int texSize = 32;
        mPaint.setTextSize(texSize);
        mPaint.setTextAlign(Paint.Align.LEFT);
        mPaint.setColor(Color.WHITE);
        mCanvas.drawText("FPS: " + mTimer.getCurrentFPS(), 10, y, mPaint);
        y+=texSize;
        mCanvas.drawText("Sprites: " + mLevelManager.mGameObjects.size(), 10, y, mPaint);
        y+=texSize;
        mCanvas.drawText("Clipped: " + mCamera.getClipCount(), 10, y, mPaint);
        mCamera.resetClipCount();
    }

    private boolean lockAndSetCanvas() {
        if(!mSurfaceHolder.getSurface().isValid()) {
            return false;
        }
        mCanvas = mSurfaceHolder.lockCanvas();
        if(mCanvas == null) {
            return false;
        }
        return true;
    }

    @Override
    public void run() {
        mTimer.reset();
        while(mIsRunning) {
            update(mTimer.onEnterFrame());
            render();
            //mTimer.endFrame();
        }
    }

}

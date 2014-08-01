package de.asideas.overlay.controls.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import de.asideas.overlay.controls.internal.Utils;
import de.asideas.overlay.controls.internal.ccontrols.PieRenderer;
import de.asideas.overlay.controls.internal.ccontrols.RenderOverlay;
import de.asideas.overlay.controls.R;

/**
 * Created by mskrabacz on 28/05/14.
 */
public class InspectorArcService extends Service implements View.OnTouchListener, PieRenderer.PieListener
{
    private static final String TAG = InspectorArcService.class.getSimpleName();

    private static final int LONG_PRESS_EVENT = 0x10;

    private static final String MOTION_EVENT_KEY = "motion_event_key";

    private static final long LONG_PRESS_TIMEOUT = 300;

    private static double MIN_MOVE_DISTANCE = 40.0f;

    private float mOffsetX;

    private float mOffsetY;

    private int mOriginalXPos;

    private int mOriginalYPos;

    private boolean mMoving;

    private WindowManager mWindowManager;

    private ViewGroup mFrame;

    private ViewGroup mPieWrapper;

    private PieRenderer mPieRenderer;

    private Point mCenterPoint;

    private final Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case LONG_PRESS_EVENT:
                    toggleVisibility(View.INVISIBLE);

                    MotionEvent event = msg.getData().getParcelable(MOTION_EVENT_KEY);
                    mPieRenderer.onTouchEvent(event);

                    break;
            }
        }
    };

    private boolean mIsOpened = false;

    private InspectorBinder mBinder = new InspectorBinder();

    public class InspectorBinder extends Binder
    {
        public void show()
        {

        }

        public void hide()
        {

        }

        public void setMainIcon(int resId)
        {
            ((ImageView) mFrame.findViewById(R.id.control_hint)).setImageResource(resId);
        }

        public PieRenderer getRenderer()
        {
            return mPieRenderer;
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        MIN_MOVE_DISTANCE = Utils.dpToPx(this, 64) / 2;

        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        mCenterPoint = Utils.getScreenCenterPoint(this);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;

        mFrame = (ViewGroup) LayoutInflater.from(getApplicationContext()).inflate(R.layout.pie_layout, null, false);
        mFrame.setOnTouchListener(this);

        mWindowManager.addView(mFrame, params);

        createMenu();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        try
        {
            if (mFrame != null)
            {
                mWindowManager.removeViewImmediate(mFrame);
                mFrame = null;
            }
            if (mPieWrapper != null && mIsOpened)
            {
                mWindowManager.removeViewImmediate(mPieWrapper);
                mPieWrapper = null;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private float x;

    private float y;

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        if (mPieRenderer.isVisible())
        {
            return mPieRenderer.onTouchEvent(event);
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            x = event.getRawX();
            y = event.getRawY();

            mMoving = false;

            int[] location = new int[2];
            mFrame.getLocationOnScreen(location);

            mOriginalXPos = location[0];
            mOriginalYPos = location[1];

            mOffsetX = mOriginalXPos - x;
            mOffsetY = mOriginalYPos - y;

            // TODO msq -
            if (mPieRenderer != null && mPieRenderer.isVisible())
            {
                mPieRenderer.hide();
            }

            Message msg = handler.obtainMessage(LONG_PRESS_EVENT);
            Bundle data = new Bundle();
            data.putParcelable(MOTION_EVENT_KEY, MotionEvent.obtain(event));
            msg.setData(data);

            handler.sendMessageDelayed(msg, LONG_PRESS_TIMEOUT);
        }
        else if (event.getAction() == MotionEvent.ACTION_MOVE)
        {
            float dx = event.getRawX() - x;
            float dy = event.getRawY() - y;

            float value = x * dx + y * dy;
            value = value < 0 ? value * -1 : value;
            double distance = Math.sqrt(value);

            if (distance > MIN_MOVE_DISTANCE || distance == Float.NaN)
            {
                x = event.getRawX();
                y = event.getRawY();

                handler.removeMessages(LONG_PRESS_EVENT);

                WindowManager.LayoutParams params = (WindowManager.LayoutParams) mFrame.getLayoutParams();

                int newX = (int) (mOffsetX + x);
                int newY = (int) (mOffsetY + y);

                if (Math.abs(newX - mOriginalXPos) < 1 && Math.abs(newY - mOriginalYPos) < 1 && !mMoving)
                {
                    return false;
                }

                params.x = (int) (x - (mCenterPoint.x / 2));
                params.y = (int) (y - mCenterPoint.y / 2);

                mWindowManager.updateViewLayout(mFrame, params);
                mMoving = true;
            }

            x = event.getRawX();
            y = event.getRawY();
        }
        else if (event.getAction() == MotionEvent.ACTION_UP)
        {
            if (mMoving)
            {
                return true;
            }
            handler.removeMessages(LONG_PRESS_EVENT);
        }

        return false;
    }

    private void createMenu()
    {
        mPieRenderer = new PieRenderer(getApplicationContext());
        mPieRenderer.setPieListener(this);

        mPieWrapper = (ViewGroup) LayoutInflater.from(getApplicationContext()).inflate(R.layout.render_overlay, mFrame, false);
        RenderOverlay renderOverlay = (RenderOverlay) mPieWrapper.findViewById(R.id.render_overlay);

        renderOverlay.addRenderer(mPieRenderer);
    }

    @Override
    public void onPieOpened(int centerX, int centerY)
    {
        if (!mIsOpened)
        {
            addPieLayer();
            mIsOpened = true;
        }
    }

    private void addPieLayer()
    {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;
        mWindowManager.addView(mPieWrapper, params);
    }

    @Override
    public void onPieClosed()
    {
        if (mPieWrapper != null && mIsOpened)
        {
            mWindowManager.removeViewImmediate(mPieWrapper);

            toggleVisibility(View.VISIBLE);
            mIsOpened = false;
        }
    }

    private void toggleVisibility(int visibility)
    {
        mFrame.findViewById(R.id.control_hint).setVisibility(visibility);
    }
}


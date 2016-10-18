package rcfans.com.cmracing;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class ButtonView extends SurfaceView implements Callback,Runnable
{
    private boolean flag;
    private boolean draw;

    public static final String THROTTLEX_CHANGED = "throttlex_changed_to";
    public static final String THROTTLEY_CHANGED = "throttley_changed_to";

    //private int action = NEUTRAL;

    private int screenW;
    private int screenH;

    private Thread th;

    private Canvas canvas;
    private SurfaceHolder sfh;
    private Paint paint;

    private float smallCenterX , smallCenterY, smallCenterR = 20;
    private float BigCenterX , BigCenterY, BigCenterR = 40;
    private float minX, minY, maxX, maxY;

    //private float throttleIncreamental = 10;

    public ButtonView(Context context,AttributeSet attrs)
    {
        super(context,attrs);
        sfh = this.getHolder();
        sfh.addCallback(this);
        paint = new Paint();
        paint.setAntiAlias(true);
        setFocusable(true);
    }

    @Override
    public void run()
    {
        while (flag)
        {
            long start = System.currentTimeMillis();
            if(draw)
            {
                drawButton();
            }
            logic();
            long end = System.currentTimeMillis();
            try {
                if (end - start < 50)
                {
                    Thread.sleep(50 - (end - start));
                }
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void logic()
    {
        Intent xIntent = new Intent(THROTTLEX_CHANGED);
        Intent yIntent = new Intent(THROTTLEY_CHANGED);
        float valx = (smallCenterY - (BigCenterY - BigCenterR))/BigCenterR*100;
        float valy = (smallCenterX - (BigCenterX - BigCenterR))/BigCenterR*100;
        xIntent.putExtra("throttlex", Math.round(valx));
        yIntent.putExtra("throttley", Math.round(valy));
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(xIntent);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(yIntent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        //Action_UP 手指松开
        if (event.getAction() == MotionEvent.ACTION_UP)
        {
            clearView();
            draw = false;
            smallCenterX = BigCenterX;
            smallCenterY = BigCenterY;
        }
        //Action_Down 手指按下
        else if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            float pointX = event.getX();
            float pointY = event.getY();
            if((pointX > minX && pointX < maxY) && (pointY > minY && pointY < maxY))
            {
                draw  = true;
                BigCenterX = event.getX();
                BigCenterY = event.getY();
                smallCenterX = BigCenterX;
                smallCenterY = BigCenterY;
            }
        }
        //Action_Move手指移动
        //Action_Move手指移动
        else if (event.getAction() == MotionEvent.ACTION_MOVE)
        {
            int pointX = (int) event.getX();
            int pointY = (int) event.getY();
            if (Math.sqrt(Math.pow((BigCenterX - (int) event.getX()), 2) + Math.pow((BigCenterY - (int) event.getY()), 2)) <= BigCenterR)
            {
                smallCenterX = pointX;
                smallCenterY = pointY;
            }
            else
            {
                setSmallCircleXY(BigCenterX, BigCenterY, BigCenterR, getRad(BigCenterX, BigCenterY, pointX, pointY));
            }
        }

        return true;
    }

    /**
     * 得到两点之间的弧度
     * @param px1    第一个点的X坐标
     * @param py1    第一个点的Y坐标
     * @param px2    第二个点的X坐标
     * @param py2    第二个点的Y坐标
     * @return
     */
    public double getRad(float px1, float py1, float px2, float py2) {
        //得到两点X的距离
        float x = px2 - px1;
        //得到两点Y的距离
        float y = py1 - py2;
        //算出斜边长
        float Hypotenuse = (float) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
        //得到这个角度的余弦值（通过三角函数中的定理 ：邻边/斜边=角度余弦值）
        float cosAngle = x / Hypotenuse;
        //通过反余弦定理获取到其角度的弧度
        float rad = (float) Math.acos(cosAngle);
        //当触屏的位置Y坐标<摇杆的Y坐标我们要取反值-0~-180
        if (py2 < py1) {
            rad = -rad;
        }
        return rad;
    }

    public void setSmallCircleXY(float centerX, float centerY, float R, double rad)
    {
        smallCenterX = (float) (R * Math.cos(rad)) + centerX;
        smallCenterY = (float) (R * Math.sin(rad)) + centerY;
    }

    public void drawButton()
    {
        try
        {
            canvas = sfh.lockCanvas();
            if (canvas != null) {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                paint.setColor(Color.RED);
                paint.setAlpha(0x77);
                canvas.drawCircle(BigCenterX, BigCenterY, BigCenterR, paint);
                canvas.drawCircle(smallCenterX, smallCenterY, smallCenterR, paint);
            }
        }
        catch (Exception e)
        {
            // TODO: handle exception
        }
        finally
        {
            if (canvas != null)
                sfh.unlockCanvasAndPost(canvas);
        }
    }

    public void clearView()
    {
        try
        {
            canvas = sfh.lockCanvas();
            if (canvas != null)
            {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            }
        }
        catch (Exception e)
        {
            // TODO: handle exception
        }
        finally
        {
            if (canvas != null)
                sfh.unlockCanvasAndPost(canvas);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        screenW = this.getWidth();
        screenH = this.getHeight();

        BigCenterR = screenH/6;
        smallCenterR = screenH/12;

        minX = (BigCenterR)/2;
        maxX = screenW - minX;

        minY = (BigCenterR)/2;
        maxY = screenH - maxY;


        flag = true;
        th = new Thread(this);
        th.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        flag = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }
}

package rcfans.com.cmracing;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class StickView extends SurfaceView implements Callback,Runnable
{
    private boolean flag;
    private boolean draw;

    private int screenW;
    private int screenH;

    private Thread th;

    private Canvas canvas;
    private SurfaceHolder sfh;
    private Paint paint;

    private float smallCenterX=100 , smallCenterY=100, smallCenterR = 20;
    //the size of small circle and the position of the center point
    private float BigCenterX=100 , BigCenterY=100, BigCenterR = 40;

    //smallCenterR and BigCenterR are changed in the following operations
    //as a result smallCenterR is 90 and the BigCenterR is 180

    //the size of big circle and the position of the center point
    private float minX, minY, maxX, maxY;
    //

    public static String STEERX_CHANGED = "steerx_changed_to";
    public static String STEERY_CHANGED = "steery_changed_to";
    public static final String THROTTLEX_CHANGED = "throttlex_changed_to";
    public static final String THROTTLEy_CHANGED = "throttley_changed_to";

    public StickView(Context context,AttributeSet attrs)
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
                drawStick();
            }
            else
            {
               clearView();
            }
            logic();
            long end = System.currentTimeMillis();
            try
            {
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
            //intent 用于Activity之间的通信
            Intent xIntent = new Intent(STEERX_CHANGED);
            Intent yIntent = new Intent(STEERY_CHANGED);

            //Here should keep the value as positive between 0 and 80
            //float valx=smallCenterX-BigCenterX+BigCenterR;
            float valx=(smallCenterX-BigCenterX+BigCenterR)/BigCenterR*100;
            float valy=(smallCenterY-BigCenterY+BigCenterR)/BigCenterR*100;

            //the range of the num is 0-200 and the other num can be worked as sign
            //Here BigCenterR 40 is HEX which means 64
            xIntent.putExtra("steerx",Math.round(valx));
            yIntent.putExtra("steery",Math.round(valy));
            //xIntent.putExtra("steerx",Math.round(smallCenterX));
            //yIntent.putExtra("steery",Math.round(smallCenterY));

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
    public double getRad(float px1, float py1, float px2, float py2)
    {
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
        if (py2 < py1)
        {
            rad = -rad;
        }
        return rad;
    }

    public void setSmallCircleXY(float centerX, float centerY, float R, double rad)
    {
        smallCenterX = (float) (R * Math.cos(rad)) + centerX;
        smallCenterY = (float) (R * Math.sin(rad)) + centerY;
    }

    //
    public void drawStick()
    {
        try
        {
            canvas = sfh.lockCanvas();
            if (canvas != null)
            {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                paint.setColor(Color.GREEN);
                paint.setAlpha(0x77);
                //draw the big circle
                canvas.drawCircle(BigCenterX, BigCenterY, BigCenterR, paint);
                //draw the small circle
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

        minX = BigCenterR;
        maxX = screenW - minX*2;

        minY = BigCenterR;
        maxY = screenH;


        flag = true;
        th = new Thread(this);
        th.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        flag = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }
}

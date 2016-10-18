package rcfans.com.cmracing;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends ActionBarActivity
{
    private int last_st_x;
    private int last_st_y;
    private int last_th_x;
    private int last_th_y;
    private int sensor1=0;
    private int sensor2=0;
    private int sensor3=0;

    //sending steerx,steery,throttlex and throttley to car
    //recieve data from car
    private IntentFilter intentFilter;

    private TextView feed;
    private PlainProtocol mPP;

    //private boolean wifiControlled;

    /*bluetooth*/
    private final static int REQUEST_CONNECT_DEVICE = 1;    //宏定义查询设备句柄

    private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";   //SPP服务UUID号

    private InputStream is;    //输入流，用来接收蓝牙数据
    private TextView dis;       //接收数据显示句柄
    private ScrollView sv;      //翻页句柄
    private String smsg = "";    //显示用数据缓存
    private String fmsg = "";    //保存用数据缓存
    private int mode = 0;

    BluetoothDevice _device = null;     //蓝牙设备
    BluetoothSocket _socket = null;      //蓝牙通信socket
    boolean _discoveryFinished = false;
    boolean bRun = true;
    boolean bThread = false;

    private BluetoothAdapter _bluetooth = BluetoothAdapter.getDefaultAdapter();    //获取本地蓝牙适配器，即蓝牙设备
    /*bluetooth*/

    private boolean connected = false;
    private boolean start=false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mPP = new PlainProtocol();

        SurfaceView sfvStick = (SurfaceView)findViewById(R.id.stick);

        sfvStick.setZOrderOnTop(true);    // necessary

        SurfaceHolder sfhStickHolder = sfvStick.getHolder();

        sfhStickHolder.setFormat(PixelFormat.TRANSPARENT);

        SurfaceView sfvButton = (SurfaceView)findViewById(R.id.button);

        sfvButton.setZOrderOnTop(true);    // necessary

        SurfaceHolder sfhButtonHolder = sfvButton.getHolder();

        sfhButtonHolder.setFormat(PixelFormat.TRANSPARENT);

        intentFilter = new IntentFilter();

        intentFilter.addAction(ButtonView.THROTTLEX_CHANGED);

        intentFilter.addAction(ButtonView.THROTTLEY_CHANGED);

        intentFilter.addAction(StickView.STEERX_CHANGED);

        intentFilter.addAction(StickView.STEERY_CHANGED);

        sv = (ScrollView)findViewById(R.id.receivedDataScrollView);  //得到翻页句柄

        dis = (TextView) findViewById(R.id.in);      //得到数据显示句柄

        Button btn = (Button) findViewById(R.id.connect);

        btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            //连接按键响应函数
            public void onClick (View v)
            {
                if (_bluetooth.isEnabled() == false)
                {  //如果蓝牙服务不可用则提示
                    Toast.makeText(getApplicationContext(), " 打开蓝牙中...", Toast.LENGTH_LONG).show();
                    return;
                }

                //如未连接设备则打开DeviceListActivity进行设备搜索
                Button btn = (Button) findViewById(R.id.connect);
                if (_socket == null)
                {
                    Intent serverIntent = new Intent(getApplicationContext(), DeviceListActivity.class); //跳转程序设置

                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);  //设置返回宏定义
                }
                else
                {
                    //关闭连接socket
                    try
                    {
                        is.close();
                        _socket.close();
                        _socket = null;
                        bRun = false;
                        btn.setText("蓝牙连接");
                        connected = false;
                    }
                    catch (IOException e)
                    {
                    }
                }
                return;
            }
        });

        final Button s1Btn = (Button) findViewById(R.id.sensor1);

        s1Btn.setOnClickListener(new View.OnClickListener()
        {
            public  void onClick(View v)
            {
                if(start)
                {
                    start=false;
                    s1Btn.setText("SENSOR1启动");
                    sensor1=0;
                }
                else
                {
                    start=true;
                    s1Btn.setText("SENSOR1停止");
                    sensor1=1;
                }
            }

        });

        final Button s2Btn = (Button) findViewById(R.id.sensor2);

        s2Btn.setOnClickListener(new View.OnClickListener()
        {
            public  void onClick(View v)
            {
                if(start)
                {
                    start=false;
                    s2Btn.setText("SENSOR2启动");
                    sensor2=0;
                }
                else
                {
                    start=true;
                    s2Btn.setText("SENSOR2停止");
                    sensor2=1;
                }
            }

        });

        final Button s3Btn = (Button) findViewById(R.id.sensor3);

        s3Btn.setOnClickListener(new View.OnClickListener()
        {
            public  void onClick(View v)
            {
                if(start)
                {
                    start=false;
                    s3Btn.setText("SENSOR3启动");
                    sensor3=0;
                }
                else
                {
                    start=true;
                    s3Btn.setText("SENSOR3停止");
                    sensor3=1;
                }
            }

        });

        //如果打开本地蓝牙设备不成功，提示信息，结束程序
        if (_bluetooth == null)
        {
            Toast.makeText(this, "无法打开手机蓝牙，请确认手机是否有蓝牙功能！", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 设置设备可以被搜索
        new Thread()
        {
            public void run()
            {
                if(_bluetooth.isEnabled()==false)
                {
                    _bluetooth.enable();
                }
            }
        }.start();

        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(StickView.STEERX_CHANGED))
                {
                    last_st_x = intent.getIntExtra("steerx", 1024);
                }

                if(intent.getAction().equals(StickView.STEERY_CHANGED))
                {
                    last_st_y = intent.getIntExtra("steery",1024);
                }

                if (intent.getAction().equals(ButtonView.THROTTLEX_CHANGED))
                {
                    last_th_x = intent.getIntExtra("throttlex", 1024);
                }

                if(intent.getAction().equals(ButtonView.THROTTLEY_CHANGED))
                {
                    last_th_y = intent.getIntExtra("throttley", 1024);
                }
            }
        }, intentFilter);

    }

    //消息处理队列
    Handler handler= new Handler()
    {
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);
            dis.setText(smsg);   //显示数据
            sv.scrollTo(0,dis.getMeasuredHeight()); //跳至数据最后一页
        }
    };

    private Handler timerSendDataHandler = new Handler();

    //Send Data
    private Runnable sendDataRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            try {
                OutputStream os = _socket.getOutputStream();   //蓝牙连接输出流
                //String toSend = last_st + "," + last_th + "\r\n";
                byte[] toSend = new byte[8];
                toSend[0] = (byte)255;
                toSend[1] = (byte)(last_st_x);
                toSend[2] = (byte)(last_st_y);
                toSend[3] = (byte)(last_th_x);
                toSend[4] = (byte)(last_th_y);
                toSend[5] = (byte)(sensor1);
                toSend[6] = (byte)(sensor2);
                toSend[7] = (byte)(sensor3);

                os.write(toSend);
            }
            catch (IOException e)
            {
            }
        timerSendDataHandler.postDelayed(this, 20);
        }
    };

    //接收数据线程
    Thread ReadThread=new Thread()
    {
        public void run()
        {
            int num = 0;
            byte[] buffer = new byte[1024];
            byte[] buffer_new = new byte[1024];
            int i = 0;
            int n = 0;
            bRun = true;
            //接收线程
            while(true){
                try{
                    while(is.available()==0)
                    {
                        while(bRun == false)
                        {}
                    }
                    while(true)
                    {
                        num = is.read(buffer);         //读入数据
                        n=0;

                        String s0 = new String(buffer,0,num);
                        fmsg+=s0;    //保存收到数据
                        for(i=0;i<num;i++){
                            if((buffer[i] == 0x0d)&&(buffer[i+1]==0x0a)){
                                buffer_new[n] = 0x0a;
                                i++;
                            }else{
                                buffer_new[n] = buffer[i];
                            }
                            n++;
                        }
                        String s = new String(buffer_new,0,n);
                        smsg+=s;   //写入接收缓存
                        if(is.available()==0)break;  //短时间没有数据才跳出进行显示
                    }
                    //发送显示消息，进行显示刷新
                    handler.sendMessage(handler.obtainMessage());
                }catch(IOException e){
                }
            }
        }
    };

    //接收活动结果，响应startActivityForResult()
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode){
            case REQUEST_CONNECT_DEVICE:     //连接结果，由DeviceListActivity设置返回
                // 响应返回结果
                if (resultCode == Activity.RESULT_OK) {   //连接成功，由DeviceListActivity设置返回
                    // MAC地址，由DeviceListActivity设置返回
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // 得到蓝牙设备句柄
                    _device = _bluetooth.getRemoteDevice(address);

                    // 用服务号得到socket
                    try{
                        _socket = _device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
                    }catch(IOException e){
                        Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
                    }
                    //连接socket
                    Button btn = (Button) findViewById(R.id.connect);
                    try{
                        _socket.connect();
                        Toast.makeText(this, "连接"+_device.getName()+"成功！", Toast.LENGTH_SHORT).show();
                        connected = true;
                        btn.setText("断开蓝牙");
                    }catch(IOException e){
                        try{
                            Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
                            _socket.close();
                            _socket = null;
                        }catch(IOException ee){
                            Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
                        }

                        return;
                    }

                    //打开接收线程
                    try
                    {
                        is = _socket.getInputStream();   //得到蓝牙数据输入流
                    }
                    catch(IOException e){
                        Toast.makeText(this, "接收数据失败！", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(bThread==false)
                    {
                        ReadThread.start();
                        timerSendDataHandler.postDelayed(sendDataRunnable, 20);
                        bThread=true;
                    }
                    else
                    {
                        bRun = true;
                    }
                }
                break;
            default:break;
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if(_socket!=null)  //关闭连接socket
            try
            {
                _socket.close();
            }
            catch(IOException e)
            {}
        _bluetooth.disable();
    }

}

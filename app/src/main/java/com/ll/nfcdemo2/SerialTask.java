package com.ll.nfcdemo2;
import android.os.Bundle;
import android.os.Handler;
import android.content.Context;
import java.io.File;
import java.io.InterruptedIOException;
import java.lang.Thread;

import android.os.Message;
import android.util.Log;
import com.ll.nfcdemo2.RfidData;



/**
 * Created by ll on 2018/6/19.
 */

public class SerialTask extends Thread {
    private static final String TAG = SerialTask.class.getSimpleName();
    public Context context = null;
    public Handler handler = null;
    public Bundle bundle = null;
    public  int fd = -1 ;


    public  final class RfidData2
    {
        public String id ;
        public String data;
        public String type;
       // RfidData tmp = new RfidData();

        public String GetRfidID()
        {
            return  id;
        }
        public String GetRfidData() {return  data;}
        public String GetRfidType(){return  type;}

    }
    public SerialTask(Handler handlerin,Context contextin)
    {
        handler  = handlerin ;
        context = contextin;
        bundle =new Bundle();
        fd = SerialPortOpen();

    }

    public void SerialPortClose()
    {
        SerialPortClose(fd);
    }
    public void run()
    {
        RfidData tmp = null;
        String  ID="";
        int buflen = 0,flag = 0,no=0;
        byte[] rcvbyte = new byte[256];
        byte[] buf = new byte[256];


      //  char[]rcvbyte0  =new char[256];
        while (true)
        {
            try
            {
                Thread.sleep(100);
                if(fd>0)
                {
                    int len = RecvSeralPort(fd, rcvbyte);
                    if(len>0)
                    {
                        String ssss ="";
                        for(int p= 0;p<len;p++)
                        {
                            String hex = Integer.toHexString(rcvbyte[p] & 0xFF);
                            if (hex.length() == 1)
                            {
                                hex = '0' + hex;
                            }
                            ssss = ssss+hex;
                        }
                        Log.i(TAG,ssss);

                        Log.i(TAG, "serial recive len:"+Integer.toString(len)+" serial buf len:"+Integer.toString(buflen));

                        if(len == 56)
                        {
                            buflen = len ;
                            for(int n = 0; n< len;n++)
                            {
                                buf[n] = rcvbyte[n];
                            }
                            no = 0;
                        }
                        if(len<56)
                        {
                            no++;
                            Log.e(TAG,"no="+Integer.toString(no));
                            if(no==3)
                            {
                                buflen = 0;
                                Log.e(TAG,"buflen 清零");
                                no=0;
                            }
                            String ff ="";
                            for(int k = 0; k< len;k++)
                            {
                                buf[buflen+k] = rcvbyte[k];
                                ff = ssss+Integer.toHexString(buf[buflen+k] & 0xFF);
                            }
                            Log.e(TAG,ff);
                            buflen = buflen+ len;
                        }

                            //Log.i(TAG,"serial buf len:"+Integer.toString(buflen));
                            Log.i(TAG, "2-serial recive len:"+Integer.toString(len)+" 2-serial buf len:"+Integer.toString(buflen));

                        if( buflen==56)
                        {
                            if(buf[0]!=0x7b||buf[55]!=0x7d||buf[1]!=0x01)
                            {
                                    no = 0;
                                    buflen =0 ;
                                    Log.e(TAG,"卡片数据格式不对");
                                    continue;
                            }
                            String id = "";
                            for (int i = 3; i < 7; i++ )
                            {
                                  String hex = Integer.toHexString(buf[i] & 0xFF);
                                    if(hex.length() == 1)
                                    {
                                        hex = '0' + hex;
                                    }
                                    id += hex.toUpperCase();
                            }
                            Log.i(TAG, id);
                            tmp = new RfidData();
                            tmp.type = "01";
                            tmp.id = id;


                            String data = "";
                            // int[]c = new int[256];
                            char[]d = new char[56];
                            for (int i = 7; i < 55; i++)
                            {
                                d[i] = 0x00;
                                d[i] =(char)(buf[i] & 0xFF);
                                String s = String.valueOf(d[i]);
                                data += s;
                            }
                            Log.i(TAG, "16hex: "+data);
                            tmp.data=data;
                            flag = 1;
                            buflen =0 ;
                            no = 0;
                        }
                        if(flag==1)
                        {
                            Bundle bundle0 = new Bundle();
                            bundle0.putParcelable("rfiddata", tmp);
                            Message msg1;
                            msg1 = handler.obtainMessage();//每发送一次都要重新获取
                            msg1.setData(bundle0);
                            msg1.arg1 = 88;
                            handler.sendMessage(msg1);//用handler向主线程发送信息
                            flag = 0;
                        }
                        for(int m = 0;m<256;m++)
                            rcvbyte[m]=0x00;
                    }
                }
            }
            catch (InterruptedException e)
            {
                System.out.println("Thread "  + " interrupted.");
                Log.e(TAG,e.toString());
            }
            catch (Exception io)
            {
                System.out.println("Thread "  + " interrupted io.");
                Log.e(TAG,io.toString());
            }

        }

       // SerialPortClose(fd);
    }

    public native int RecvSeralPort(int fd,byte buf[]);
    public native int SerialPortOpen();
    public native int SerialPortClose(int fd);


    static
    {
        System.loadLibrary("serialport");
    }

}

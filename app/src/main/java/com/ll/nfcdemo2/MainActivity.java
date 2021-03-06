package com.ll.nfcdemo2;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private TextView mReadText,mTagIdText;
    public static SerialTask serialtask = null;
    public ActivityHandler handler = null;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //强制横屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mReadText = findViewById(R.id.readBtnView);
        mTagIdText=findViewById(R.id.id_tv);




        String deviceModel = Build.MODEL; // 设备型号
        Log.i(TAG,deviceModel);
        if(deviceModel.indexOf("madest")!=-1)  //a33 不带NFC
        {
            //适用于直板型10寸平板
            handler = new ActivityHandler();
            serialtask = new SerialTask(handler, this);
            int fd = serialtask.fd;
            if (fd > 0)
            {
                Log.i("serial-task", Integer.toString(fd));
                serialtask.start();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("提示");
                builder.setMessage("读卡设备打开失败!");
                builder.setPositiveButton("是", null);
                builder.show();
            }
        }
        else  //带NFC 3288
        {
            mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
            if (mNfcAdapter == null) //不带NFC
            {
                Toast.makeText(this, "该设备不支持nfc", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            if (!mNfcAdapter.isEnabled()) {
                Toast.makeText(this, "请打开nfc开关", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
                startActivity(intent);
            }

            //创建PendingIntent对象，当检查到一个tag标签就会执行此Intent
            mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()), 0);
        }
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        //取出标签
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        String techList[] = tag.getTechList();
        byte[] bytesId = tag.getId();



        String tagId =bytesToHexString(bytesId);
        Log.d(TAG, "这里是tagId:"+tagId);
        mTagIdText.setText(tagId);
        for (String tech : techList)
        {
            System.out.print(tech);
            Log.d(TAG, tech);
        }
        readNdeftag(tag);
//        readMifareUltralight(tag);

        String action=intent.getAction();


    }


    public String readMifareUltralight(Tag tag){
        MifareUltralight mifare=MifareUltralight.get(tag);
        try
        {
            if(mifare == null)
                return null;

            mifare.connect();
            byte[] payload=mifare.readPages(4);
            String data= new String(payload, Charset.forName("GB2312"));
            Log.d(TAG, data);

        } catch (IOException e) {
            Log.e(TAG, "IOException while reading MifareUltralight message...", e);
        } finally {
            if (mifare!=null){
                try {
                    mifare.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing tag...", e);
                }
            }
        }
        return null;
    }

    private String readNdeftag(Tag tag) {
        Ndef ndef = Ndef.get(tag);
        if (ndef != null)
        {
            //获取tag的type是第几
//        String tagType=ndef.getType();
            try
            {
                if(ndef == null)
                    return null;

                ndef.connect();
                NdefMessage ndefMessage = ndef.getNdefMessage();
                if (ndefMessage != null)
                {
                    mReadText.setText(parseTextRecord(ndefMessage.getRecords()[0]));
                    Toast.makeText(this, "成功", Toast.LENGTH_SHORT).show();
                } else
                {
                    mReadText.setText("该标签为空标签");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (FormatException e) {
                e.printStackTrace();
            } finally {
                try {
                    ndef.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }else {
            readMifareUltralight(tag);
        }
        return  null;
    }
    public static String parseTextRecord(NdefRecord ndefRecord) {
        //判断TNF
        if (ndefRecord.getTnf() != NdefRecord.TNF_WELL_KNOWN) {
            return null;
        }
        //判断长度和类型
        if (!Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
            return null;
        }
        try {
            byte[] payload = ndefRecord.getPayload();
            String textEncoding = ((payload[0] & 0X80) == 0) ? "UTF-8" : "UTF-16";
            int languageCodeLength = payload[0] & 0X3f;
            String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            String textRecord = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
            return textRecord;
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mNfcAdapter!=null)
        {
            mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if (mNfcAdapter != null)
        {
            mNfcAdapter.disableForegroundDispatch(this);
        }

    }

    private String bytesToHexString(byte[] src)
    {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        char[] buffer = new char[2];
        for (int i = 0; i < src.length; i++) {
            buffer[0] = Character.forDigit(src[i] >>> 4 & 0X0F, 16);
            buffer[1] = Character.forDigit(src[i] & 0X0F, 16);
            stringBuilder.append(buffer);
        }
        return stringBuilder.toString();

    }


    /**
     * 接受消息，处理消息 ，此Handler会与当前主线程一块运行
     * */
    //自定义handler类
    class ActivityHandler extends Handler {
        @Override
        //接收别的线程的信息并处理
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            try {
                    if (isFinishing() || isDestroyed())
                    {
                        return;
                    }
                    switch (msg.arg1)
                    {
                        case 1:

                            break;
                        case 88:
                            Bundle bundle88 = msg.getData();
                            RfidData tmp = bundle88.getParcelable("rfiddata");
                            Log.i(TAG, tmp.id);
                            mTagIdText.setText(tmp.id);
                            break;
                        default:
                    }
                }
                catch (Exception e)
                {

                }
            }
        }

}

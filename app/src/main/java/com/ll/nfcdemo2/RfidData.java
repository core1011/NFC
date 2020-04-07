package com.ll.nfcdemo2;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by ll on 2018/6/20.
 */

public class RfidData implements Parcelable {

    public String id ;
    public String data;
    public String type;

    public RfidData()
    {
        id = null;
        data = null;
        type =null;

    }
    public  RfidData(Parcel in)
    {
        //顺序要和writeToParcel写的顺序一样
        id = in.readString();
        data =in.readString();
        type = in.readString();


    }
    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }
    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(id);
        dest.writeString(data);;
        dest.writeString(type);
    }

    public static final Creator<RfidData> CREATOR = new Creator<RfidData>() {
        public RfidData createFromParcel(Parcel in) {
            return new RfidData(in);
        }

        public RfidData[] newArray(int size) {
            return new RfidData[size];
        }
    };

}

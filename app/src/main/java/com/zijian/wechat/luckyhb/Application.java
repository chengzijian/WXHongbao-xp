package com.zijian.wechat.luckyhb;

import com.androidnetworking.AndroidNetworking;

/**
 * Created by zijian.cheng on 2018/3/2.
 */
public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidNetworking.initialize(getApplicationContext());
    }
}

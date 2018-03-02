package com.zijian.wechat.luckyhb;


import de.robv.android.xposed.XSharedPreferences;

/**
 * Created by zijian.cheng on 2018.03.02
 */
public class PreferencesUtils {

    private static XSharedPreferences instance = null;
    private static int MAX_COUNT = 3;

    private static XSharedPreferences getInstance() {
        if (instance == null) {
            instance = new XSharedPreferences(PreferencesUtils.class.getPackage().getName(), "user_settings");
            instance.makeWorldReadable();
        } else {
            instance.reload();
        }
        return instance;
    }

    public static boolean open() {
        return getInstance().getBoolean("open", false);
    }

    public static boolean notSelf() {
        return getInstance().getBoolean("not_self", false);
    }

    public static String getMobile() {
        return getInstance().getString("mobile", null);
    }

    public static int getUseCount(){
        String mobile = getMobile();
        return getInstance().getInt(mobile, 0);
    }

    public static boolean canUse() {
        String mobile = getMobile();
        return getInstance().getInt(mobile, 0) < MAX_COUNT;
    }

}



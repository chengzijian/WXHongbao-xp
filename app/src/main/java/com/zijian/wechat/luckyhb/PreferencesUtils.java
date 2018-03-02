package com.zijian.wechat.luckyhb;


import de.robv.android.xposed.XSharedPreferences;

/**
 * Created by zijian.cheng on 2018.03.02
 */
public class PreferencesUtils {

    private static XSharedPreferences instance = null;

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

    public static int getMaxCount() {
        return getInstance().getInt("max_count", 0);
    }

    public static boolean getMeituan() {
        return getInstance().getBoolean("get_meituan", false);
    }

    public static boolean getElem() {
        return getInstance().getBoolean("get_elem", false);
    }

}



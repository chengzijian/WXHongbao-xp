package com.zijian.wechat.luckyhb;

import android.app.AndroidAppHelper;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.zijian.wechat.luckyhb.util.XmlToJson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static android.text.TextUtils.isEmpty;
import static com.zijian.wechat.luckyhb.VersionParam.WECHAT_PACKAGE_NAME;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

/**
 * Created by zijian.cheng on 2018.03.02
 */

public class Main implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private String TAG = "tttttt";
    private static String wechatVersion = "";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals(WECHAT_PACKAGE_NAME)) {
            if (isEmpty(wechatVersion)) {
                Context context = (Context) callMethod(callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread", new Object[0]), "getSystemContext", new Object[0]);
                String versionName = context.getPackageManager().getPackageInfo(lpparam.packageName, 0).versionName;
                log("Found wechat version:" + versionName);
                wechatVersion = versionName;
                new DonateHook().hook(lpparam);
                VersionParam.init(versionName);
            }

            findAndHookMethod("com.tencent.wcdb.database.SQLiteDatabase", lpparam.classLoader, "insert", String.class, String.class, ContentValues.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    ContentValues contentValues = (ContentValues) param.args[2];
                    String tableName = (String) param.args[0];
                    if (TextUtils.isEmpty(tableName) || !tableName.equals("message")) {
                        return;
                    }
                    Integer type = contentValues.getAsInteger("type");
                    if (null == type) {
                        return;
                    }

                    Log.e(TAG, "type:" + type);
                    if (type == 49) {
                        handleLuckyTakeOut(contentValues);
                    }

                }
            });

            new HideModule().hide(lpparam);
        }
    }

    private void handleLuckyTakeOut(ContentValues contentValues) throws XmlPullParserException, IOException, JSONException {
        if (!PreferencesUtils.open()) {
            return;
        }

        int status = contentValues.getAsInteger("status");
        if (status == 4) {
            return;
        }

        int isSend = contentValues.getAsInteger("isSend");
        if (PreferencesUtils.notSelf() && isSend != 0) {
            return;
        }

        String mobile = PreferencesUtils.getMobile();
        if (!mobile.matches("^[1][3,4,5,6,7,8][0-9]{9}$")) {
            Log.e(TAG, "mobile:" + mobile);
            showToast("手机号有误，请检查");
            return;
        }

        String content = contentValues.getAsString("content");
        if (!content.startsWith("<msg")) {
            content = content.substring(content.indexOf("<msg"));
        }

        JSONObject wcpayinfo = new XmlToJson.Builder(content).build()
                .getJSONObject("msg").getJSONObject("appmsg");
        //String type = wcpayinfo.getString("type");
        String url = wcpayinfo.getString("url");

        String key = new SimpleDateFormat("yyyyMMdd", Locale.CHINA).format(new Date()) + mobile;
        final String typeName;
        if (url.contains("h5.ele.me/hongbao")) {
            if (!PreferencesUtils.getElem()) {
                return;
            }
            key += "ele";
            typeName = "饿了么";
        } else if (url.contains("activity.waimai.meituan.com")) {
            if (!PreferencesUtils.getMeituan()) {
                return;
            }
            key += "meituan";
            typeName = "美团";
        } else {
            typeName = "";
        }
        Log.e(TAG, "key:" + key);

        final String keyId = key;
        SharedPreferences pref = AndroidAppHelper.currentApplication()
                .getSharedPreferences("user_settings", Context.MODE_WORLD_READABLE);
        Log.e(TAG, "count:" + pref.getInt(keyId, 0));
        int MAX_COUNT = PreferencesUtils.getMaxCount();
        Log.e(TAG, "MAX_COUNT:" + MAX_COUNT);
        if (pref.getInt(keyId, 0) < MAX_COUNT) {
            AndroidNetworking.post("https://hongbao.xxooweb.com/hongbao")
                    .addBodyParameter("url", url)
                    .addBodyParameter("mobile", mobile)
                    .setTag("post")
                    .setPriority(Priority.HIGH)
                    .build()
                    .getAsJSONArray(new JSONArrayRequestListener() {
                        @Override
                        public void onResponse(JSONArray response) {
                            String result = response.toString();
                            Log.e(TAG, "result 1:" + result);
                            recordResult(result, keyId, typeName);
                        }

                        @Override
                        public void onError(ANError anError) {
                            String result = formatResult(anError.getMessage());
                            Log.e(TAG, "result 2:" + result);
                            recordResult(result, keyId, typeName);
                        }
                    });

        } else {
            showToast("今日已超出" + typeName + "红包领取上限");
        }
    }

    private void recordResult(String result, String keyId, String typeName) {
        boolean isSuccess = false;
        if (result.contains("已失效")) {
            isSuccess = false;
        }

        //这里的判断并没有很严谨，接口如果改了的话再说
        if (result.contains("红包领取完毕")) {
            isSuccess = true;
        }

        String log;
        if (isSuccess) {
            //成功后，次数加一
            SharedPreferences pref = AndroidAppHelper.currentApplication()
                    .getSharedPreferences("user_settings", Context.MODE_WORLD_READABLE);
            SharedPreferences.Editor editor = pref.edit();
            Log.e(TAG, "before>>" + pref.getInt(keyId, 0));
            editor.putInt(keyId, pref.getInt(keyId, 0) + 1).apply();
            Log.e(TAG, "after>>" + pref.getInt(keyId, 0));

            log = String.format("[%1$s][%2$s][%3$s]", "成功", keyId, result);
            showToast("领取" + typeName + "红包成功");
        } else {
            //失败
            log = String.format("[%1$s][%2$s][%3$s]", "失败", keyId, result);
            showToast("领取" + typeName + "红包不成功");
        }

        Log.e(TAG, log);
        XposedBridge.log(log);
    }

    private String formatResult(String str) {
        List<String> ls = new ArrayList<String>();
        Pattern pattern = Pattern.compile("\\{(.+?)\\}");
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            ls.add(matcher.group());
        }
        for (String string : ls) {
            return string;
        }
        return null;
    }


    private void showToast(String msg) {
        // Toasts
        Toast toast = Toast.makeText(AndroidAppHelper.currentApplication(), msg, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM, 0, 200);
        toast.show();
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {

    }
}

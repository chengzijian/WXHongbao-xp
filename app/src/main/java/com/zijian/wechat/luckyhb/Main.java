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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
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

public class Main implements IXposedHookLoadPackage {

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


            SharedPreferences pref =
                    AndroidAppHelper.currentApplication().getSharedPreferences("user_settings", Context.MODE_WORLD_READABLE);
            SharedPreferences.Editor editor = pref.edit();

//            XSharedPreferences prefs = new XSharedPreferences("com.zijian.wechat.luckyhb");
//            String mode = prefs.getString("is_low_ram", "default");
//            Log.e("ttttttt", mode);
//            prefs.edit().putString("is_low_ram", "default22").apply();
//            Log.e("ttttttt", mode);

//            Context context = AndroidAppHelper.currentApplication();
//            int count = PreferencesUtils.getUseCount();
//            SharedPreferences wpref = context.getSharedPreferences("user_settings", Context.MODE_WORLD_READABLE);
//            SharedPreferences.Editor editor = wpref.edit();
//            editor.putInt(PreferencesUtils.getMobile(), count + 1);
//            editor.commit();
//            // Toasts
//            int duration = Toast.LENGTH_SHORT;
//            Toast toast = Toast.makeText(context, count + ">>" + PreferencesUtils.getUseCount(), duration);
//            toast.setGravity(Gravity.TOP, 0, 0);
//            toast.show();
//            Log.e("ttttttt", count + ">>" + PreferencesUtils.getUseCount());

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

        String content = contentValues.getAsString("content");
        if (!content.startsWith("<msg")) {
            content = content.substring(content.indexOf("<msg"));
        }

        JSONObject wcpayinfo = new XmlToJson.Builder(content).build()
                .getJSONObject("msg").getJSONObject("appmsg");
        String type = wcpayinfo.getString("type");
        String url = wcpayinfo.getString("url");

        recordResult(url);
//        AndroidNetworking.post("https://hongbao.xxooweb.com/hongbao")
//                .addBodyParameter("url", url)
//                .addBodyParameter("mobile", PreferencesUtils.getMobile())
//                .setTag("post")
//                .setPriority(Priority.HIGH)
//                .build()
//                .getAsJSONArray(new JSONArrayRequestListener() {
//                    @Override
//                    public void onResponse(JSONArray response) {
//                        recordResult(response.toString());
//                    }
//
//                    @Override
//                    public void onError(ANError anError) {
//                        String result = formatResult(anError.getMessage());
//                        recordResult(result);
//                    }
//                });
    }

    private void recordResult(String result) {
        Context context = AndroidAppHelper.currentApplication();
        SharedPreferences preferences = context.getSharedPreferences("device", Context.MODE_WORLD_READABLE);

        Toast toast = Toast.makeText(context, preferences.getAll().toString(), Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();

        SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");
        String log = String.format("[%1$s][%2$s][%3$s]", date.format(new Date()), PreferencesUtils.getMobile(), result);
        Log.e("ttttttt", log);
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
}

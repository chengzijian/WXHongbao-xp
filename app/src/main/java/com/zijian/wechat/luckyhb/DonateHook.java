package com.zijian.wechat.luckyhb;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static android.text.TextUtils.isEmpty;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * Created by zijian.cheng on 2018.03.02
 */

public class DonateHook {
    public void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam.packageName.equals("com.tencent.mm")) {
            findAndHookMethod("com.tencent.mm.ui.LauncherUI", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    if (activity != null) {
                        Intent intent = activity.getIntent();
                        if (intent != null) {
                            String className = intent.getComponent().getClassName();
                            if (!isEmpty(className) && className.equals("com.tencent.mm.ui.LauncherUI")
                                    && intent.hasExtra("donate")) {
                                Intent donateIntent = new Intent();
                                donateIntent.setClassName(activity, "com.tencent.mm.plugin.collect.reward.ui.QrRewardSelectMoneyUI");
                                donateIntent.putExtra("key_scene", 2);
                                donateIntent.putExtra("key_qrcode_url ", "m0yq;Yt@9pfBkb!C6R@G$L");
                                donateIntent.putExtra("key_channel", 13);
                                donateIntent.removeExtra("donate");
                                activity.startActivity(donateIntent);
                                activity.finish();
                            }
                        }
                    }
                }
            });
            findAndHookMethod("com.tencent.mm.plugin.collect.reward.ui.QrRewardSelectMoneyUI", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    String qrcodeUrl = activity.getIntent().getStringExtra("key_qrcode_url");
                    if (isEmpty(qrcodeUrl)) {
                        activity.getIntent().putExtra("key_qrcode_url", "m0yq;Yt@9pfBkb!C6R@G$L");
                        return;
                    }
                }
            });

        }
    }
}
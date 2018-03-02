package com.zijian.wechat.luckyhb;

import android.annotation.TargetApi;
import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by zijian.cheng on 2018.03.02
 */
public class MainActivity extends AppCompatActivity {

    private SettingsFragment mSettingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            mSettingsFragment = new SettingsFragment();
            replaceFragment(R.id.settings_container, mSettingsFragment);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void replaceFragment(int viewId, android.app.Fragment fragment) {
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(viewId, fragment).commit();
    }

    /**
     * A placeholder fragment containing a settings view.
     */
    public static class SettingsFragment extends PreferenceFragment {

        private EditTextPreference editPre;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName("user_settings");
            getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
            addPreferencesFromResource(R.xml.pref_setting);

            editPre = (EditTextPreference) findPreference("mobile");
            editPre.setSummary(getPreferenceManager().getSharedPreferences().getString("mobile"
                    , ""));
            editPre.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if(newValue != null){
                        Log.e("onPreferenceChange", newValue.toString());
                        String value = newValue.toString();
                        if (value.matches("^[1][3,4,5,6,7,8][0-9]{9}$")) {
                            editPre.setSummary(value);
                            return true;
                        } else {
                            Toast.makeText(getActivity(), "手机号码输入不正确", Toast.LENGTH_SHORT).show();
                            return false;
                        }
                    }
                    return false;
                }
            });

            Preference reset = findPreference("author");
            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference pref) {
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.VIEW");
                    intent.setData(Uri.parse("https://github.com/chengzijian"));
                    startActivity(intent);
                    return true;
                }
            });

            Preference donateAlipay = findPreference("donate_alipay");
            donateAlipay.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference pref) {
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.VIEW");
                    String payUrl = "https://qr.alipay.com/FKX05260KGZEMGG5MN2CB3";
                    intent.setData(Uri.parse("alipayqr://platformapi/startapp?saId=10000007&clientVersion=3.7.0.0718&qrcode=" + payUrl));
                    if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivity(intent);
                    } else {
                        intent.setData(Uri.parse(payUrl));
                        startActivity(intent);
                    }
                    return true;
                }
            });

            Preference donateWechat = findPreference("donate_wechat");
            donateWechat.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference pref) {
                    Intent intent = new Intent();
                    intent.setClassName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI");
                    intent.putExtra("donate", true);
                    startActivity(intent);
                    return true;
                }
            });

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

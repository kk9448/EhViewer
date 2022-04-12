/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.ui;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.hippo.android.resource.AttrResources;
import com.hippo.content.ContextLocalWrapper;
import com.hippo.ehviewer.Analytics;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import java.util.Locale;

// 相比普通的Activity增加了，防止截屏， 安全等方面的启动项
public abstract class EhActivity extends AppCompatActivity {

    @StyleRes
    protected abstract int getThemeResId(int theme);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(getThemeResId(Settings.getTheme()));

        super.onCreate(savedInstanceState);

        // 把Activity放入一个List<Activity>中
        ((EhApplication) getApplication()).registerActivity(this);

        if (Analytics.isEnabled()) {
            FirebaseAnalytics.getInstance(this);
        }
        // LOLLIPOP为Android 5.0, getApplyNavBarThemeColor(), 设置下方的导航条颜色是否跟随主题
        // 如果大于5.0并且跟随主题颜色， 则执行下方语句
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Settings.getApplyNavBarThemeColor()) {
            //设置导航条的颜色为R.attr.colorPrimaryDark
            getWindow().setNavigationBarColor(AttrResources.getAttrColor(this, R.attr.colorPrimaryDark));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        ((EhApplication) getApplication()).unregisterActivity(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(Settings.getEnabledSecurity()){
            //把这个window中的内容看作需要保护的内容,
            //防止被截屏,或防止内容显示在一些不安全的屏幕上
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        }else{
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        Locale locale = null;
        String language = Settings.getAppLanguage();
        // 如果语言和系统设置的语言不一样
        // locale为地区信息
        // new locale(string language, string country, string variant)
        if (language != null && !language.equals("system")) {
            String[] split = language.split("-");
            if (split.length == 1) {
                locale = new Locale(split[0]);
            } else if (split.length == 2) {
                locale = new Locale(split[0], split[1]);
            } else if (split.length == 3) {
                locale = new Locale(split[0], split[1], split[2]);
            }
        }

        if (locale != null) {
            //hippo自定义类，设置Locale
            newBase = ContextLocalWrapper.wrap(newBase, locale);
        }

        super.attachBaseContext(newBase);
    }
}

package com.cybexmobile.activity.setting.help;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.cybex.basemodule.base.BaseActivity;
import com.cybexmobile.R;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class HelpActivity extends BaseActivity{

    @BindView(R.id.help_wv)
    WebView mWebView;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    private Unbinder mUnbinder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        mUnbinder = ButterKnife.bind(this);
        setSupportActionBar(mToolbar);
        initWebViewSetting();
        boolean isNight = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("night_mode", false);
        String url = "http://47.75.154.39:3009/" + (isNight ? "cybexday" : "cybexnight" ) + "?lang=" + Locale.getDefault().getLanguage();
        mWebView.loadUrl(url);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWebView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mWebView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWebView.destroy();
        mUnbinder.unbind();
    }

    @Override
    public void onNetWorkStateChanged(boolean isAvailable) {

    }

    private void initWebViewSetting(){
        WebSettings webSettings = mWebView.getSettings();
        //支持javascript交互
        webSettings.setJavaScriptEnabled(true);
        //将图片调整到适合webview的大小
        webSettings.setUseWideViewPort(true);
        //缩放至屏幕的大小
        webSettings.setLoadWithOverviewMode(true);
        //支持缩放
        webSettings.setSupportZoom(true);
        //设置内置的缩放控件
        webSettings.setBuiltInZoomControls(true);
        //隐藏原生的缩放控件
        webSettings.setDisplayZoomControls(false);
        mWebView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                showLoadDialog();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                hideLoadDialog();
            }
        });
    }
}

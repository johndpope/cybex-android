package com.cybexmobile.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.widget.Toast;

import com.cybexmobile.BuildConfig;
import com.cybexmobile.api.RetrofitFactory;
import com.cybexmobile.base.BaseActivity;
import com.cybexmobile.data.AppVersion;
import com.cybexmobile.R;
import com.cybexmobile.service.WebSocketService;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SplashActivity extends BaseActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Intent intent = new Intent(SplashActivity.this, WebSocketService.class);
        startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(SplashActivity.this, BottomNavigationActivity.class);
                startActivity(i);
                finish();
            }
        }, 2000);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void checkVersion() {
        RetrofitFactory.getInstance()
                .api()
                .checkAppUpdate()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<AppVersion>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(AppVersion appVersion) {
                        if(appVersion.compareVersion(BuildConfig.VERSION_NAME)){
                            AlertDialog.Builder builder = new AlertDialog.Builder(SplashActivity.this)
                                    .setCancelable(false)
                                    .setTitle(R.string.setting_version_update_available)
                                    .setMessage(R.string.setting_version_update_content)
                                    .setPositiveButton(R.string.setting_version_update_now, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent browseIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(appVersion.getUrl()));
                                            startActivity(browseIntent);
                                        }
                                    });
                            if(!appVersion.isForceUpdate()){
                                builder.setNegativeButton(R.string.setting_version_update_next_time, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                            }
                            builder.show();
                        } else {
                            Toast toast = Toast.makeText(getApplicationContext(), R.string.setting_version_is_the_latest, Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        hideLoadDialog();
                    }

                    @Override
                    public void onComplete() {
                        hideLoadDialog();
                    }
                });
    }

}

package com.cybexmobile.activity.main;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatDelegate;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.cybex.basemodule.constant.Constant;
import com.cybex.basemodule.service.WebSocketService;
import com.cybex.eto.fragment.EtoFragment;
import com.cybex.provider.http.response.AppConfigResponse;
import com.cybex.provider.market.WatchlistData;
import com.cybex.provider.websocket.apihk.LimitOrderWrapper;
import com.cybexmobile.BuildConfig;
import com.cybexmobile.activity.markets.MarketsActivity;
import com.cybex.provider.http.RetrofitFactory;
import com.cybex.basemodule.base.BaseActivity;
import com.cybex.provider.http.entity.AppVersion;
import com.cybex.basemodule.dialog.CybexDialog;
import com.cybex.basemodule.event.Event;
import com.cybexmobile.fragment.AccountFragment;
import com.cybexmobile.fragment.ExchangeFragment;
import com.cybexmobile.fragment.WatchlistFragment;
import com.cybexmobile.fragment.main.CybexMainFragment;
import com.cybexmobile.helper.BottomNavigationViewHelper;
import com.cybexmobile.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.Locale;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static com.cybex.basemodule.constant.Constant.ACTION_BUY;
import static com.cybex.basemodule.constant.Constant.ACTION_SELL;
import static com.cybexmobile.activity.markets.MarketsActivity.RESULT_CODE_BACK;
import static com.cybex.basemodule.constant.Constant.INTENT_PARAM_ACTION;
import static com.cybex.basemodule.constant.Constant.INTENT_PARAM_WATCHLIST;

public class BottomNavigationActivity extends BaseActivity implements WatchlistFragment.OnListFragmentInteractionListener {

    private BottomNavigationView mBottomNavigationView;
    private static final String KEY_BOTTOM_NAVIGATION_VIEW_SELECTED_ID = "KEY_BOTTOM_NAVIGATION_VIEW_SELECTED_ID";
    private static final int REQUEST_CODE_BACK = 1;

    private WatchlistFragment mWatchListFragment;
    private AccountFragment mAccountFragment;
    private ExchangeFragment mExchangeFragment;
    private EtoFragment mEtoFragment;
    private CybexMainFragment mCybexMainFragment;
    private Context mContext;

    private String mAction;
    private WatchlistData mWatchlistData;

    private long mLastExitTime;

    private Disposable mDisposableVersionUpdate;
    private Disposable mDisposableAppConfig;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            showFragment(item.getItemId());
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        EventBus.getDefault().register(this);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        setContentView(R.layout.activity_nav_button);
        mBottomNavigationView = findViewById(R.id.navigation);
        BottomNavigationViewHelper.removeShiftMode(mBottomNavigationView);
        mBottomNavigationView.setItemIconTintList(null);
        mBottomNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        initFragment(savedInstanceState);
        loadAppConfig();
        if (BuildConfig.APPLICATION_ID.equals(Constant.APPLICATION_ID)) {
            checkVersionGoogleStore();
        } else {
            checkVersion();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConfigChanged(Event.ConfigChanged event) {
        switch (event.getConfigName()) {
            case "EVENT_REFRESH_LANGUAGE":
                recreate();
                break;
            case "THEME_CHANGED":
                recreate();
                break;
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_BOTTOM_NAVIGATION_VIEW_SELECTED_ID, mBottomNavigationView.getSelectedItemId());
        FragmentManager fm = getSupportFragmentManager();
        if (mWatchListFragment != null && mWatchListFragment.isAdded()) {
            fm.putFragment(outState, WatchlistFragment.class.getSimpleName(), mWatchListFragment);
        }
        if (mExchangeFragment != null && mExchangeFragment.isAdded()) {
            fm.putFragment(outState, ExchangeFragment.class.getSimpleName(), mExchangeFragment);
        }
        if (mEtoFragment != null && mEtoFragment.isAdded()) {
            fm.putFragment(outState, EtoFragment.class.getSimpleName(), mEtoFragment);
        }
        if (mAccountFragment != null && mAccountFragment.isAdded()) {
            fm.putFragment(outState, AccountFragment.class.getSimpleName(), mAccountFragment);
        }
        if (mCybexMainFragment != null && mCybexMainFragment.isAdded()) {
            fm.putFragment(outState, CybexMainFragment.class.getSimpleName(), mCybexMainFragment);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        Intent intentService = new Intent(this, WebSocketService.class);
        stopService(intentService);
        if(mDisposableVersionUpdate != null && !mDisposableVersionUpdate.isDisposed()){
            mDisposableVersionUpdate.dispose();
        }
        if(mDisposableAppConfig != null && !mDisposableAppConfig.isDisposed()){
            mDisposableAppConfig.dispose();
        }
        LimitOrderWrapper.getInstance().disconnect();
    }

    @Override
    public void onNetWorkStateChanged(boolean isAvailable) {
        if (!isAvailable) {
            Toast.makeText(this, getResources().getString(R.string.network_connection_is_not_available), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //register after recreate that activity
        recreate();
    }

    private void initFragment(Bundle savedInstanceState) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        int selectedId = R.id.navigation_main;
        if (savedInstanceState != null) {
            mWatchListFragment = (WatchlistFragment) fragmentManager.getFragment(savedInstanceState, WatchlistFragment.class.getSimpleName());
            mExchangeFragment = (ExchangeFragment) fragmentManager.getFragment(savedInstanceState, ExchangeFragment.class.getSimpleName());
            mEtoFragment = (EtoFragment) fragmentManager.getFragment(savedInstanceState, EtoFragment.class.getSimpleName());
            mAccountFragment = (AccountFragment) fragmentManager.getFragment(savedInstanceState, AccountFragment.class.getSimpleName());
            mCybexMainFragment = (CybexMainFragment) fragmentManager.getFragment(savedInstanceState, CybexMainFragment.class.getSimpleName());
            selectedId = savedInstanceState.getInt(KEY_BOTTOM_NAVIGATION_VIEW_SELECTED_ID, R.id.navigation_watchlist);
        }
        showFragment(selectedId);
    }

    /**
     * 延迟加载fragment
     *
     * @param resId
     */
    private void showFragment(int resId) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (mWatchListFragment != null && mWatchListFragment.isAdded()) {
            transaction.hide(mWatchListFragment);
        }
        if (mExchangeFragment != null && mExchangeFragment.isAdded()) {
            transaction.hide(mExchangeFragment);
        }
        if (mEtoFragment != null && mEtoFragment.isAdded()) {
            transaction.hide(mEtoFragment);
        }
        if (mAccountFragment != null && mAccountFragment.isAdded()) {
            transaction.hide(mAccountFragment);
        }
        if (mCybexMainFragment != null && mCybexMainFragment.isAdded()) {
            transaction.hide(mCybexMainFragment);
        }
        /**
         * fix online crash
         * java.lang.IllegalStateException: Fragment already added
         */
        switch (resId) {
            case R.id.navigation_watchlist:
                if (mWatchListFragment == null) {
                    mWatchListFragment = new WatchlistFragment();
                }
                if (mWatchListFragment.isAdded() || fragmentManager.findFragmentByTag(WatchlistFragment.class.getSimpleName()) != null) {
                    transaction.show(mWatchListFragment);
                } else {
                    transaction.add(R.id.frame_container, mWatchListFragment, WatchlistFragment.class.getSimpleName());
                }
                break;
            case R.id.navigation_exchange:
                if (mExchangeFragment == null) {
                    mExchangeFragment = ExchangeFragment.getInstance(mAction, mWatchlistData);
                }
                if (mExchangeFragment.isAdded() || fragmentManager.findFragmentByTag(ExchangeFragment.class.getSimpleName()) != null) {
                    transaction.show(mExchangeFragment);
                } else {
                    transaction.add(R.id.frame_container, mExchangeFragment, ExchangeFragment.class.getSimpleName());
                }
                break;
            case R.id.navigation_eto:
                if (mEtoFragment == null) {
                    mEtoFragment = EtoFragment.getInstance();
                }
                if (mEtoFragment.isAdded() || fragmentManager.findFragmentByTag(EtoFragment.class.getSimpleName()) != null) {
                    transaction.show(mEtoFragment);
                } else {
                    transaction.add(R.id.frame_container, mEtoFragment, EtoFragment.class.getSimpleName());
                }
                break;
            case R.id.navigation_account:
                if (mAccountFragment == null) {
                    mAccountFragment = new AccountFragment();
                }
                if (mAccountFragment.isAdded() || fragmentManager.findFragmentByTag(AccountFragment.class.getSimpleName()) != null) {
                    transaction.show(mAccountFragment);
                } else {
                    transaction.add(R.id.frame_container, mAccountFragment, AccountFragment.class.getSimpleName());
                }
                break;
            case R.id.navigation_main:
                if (mCybexMainFragment == null) {
                    mCybexMainFragment = new CybexMainFragment();
                }
                if (mCybexMainFragment.isAdded() || fragmentManager.findFragmentByTag(CybexMainFragment.class.getSimpleName()) != null) {
                    transaction.show(mCybexMainFragment);
                } else {
                    transaction.add(R.id.frame_container, mCybexMainFragment, CybexMainFragment.class.getSimpleName());
                }
                break;
        }
        transaction.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - mLastExitTime > 2 * 1000) {
                mLastExitTime = currentTime;
                Toast.makeText(this, getResources().getString(R.string.text_press_again_to_exit), Toast.LENGTH_SHORT).show();
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_BACK && resultCode == RESULT_CODE_BACK) {
            if (data != null) {
                mAction = data.getStringExtra(INTENT_PARAM_ACTION);
                mWatchlistData = (WatchlistData) data.getSerializableExtra(INTENT_PARAM_WATCHLIST);
                EventBus.getDefault().post(new Event.MarketIntentToExchange(mAction, mWatchlistData));
            }
            mBottomNavigationView.setSelectedItemId(R.id.navigation_exchange);
        }
    }

    @Override
    public void onListFragmentInteraction(WatchlistData item) {
        Intent intent = new Intent(BottomNavigationActivity.this, MarketsActivity.class);
        intent.putExtra(INTENT_PARAM_WATCHLIST, item);
        startActivityForResult(intent, REQUEST_CODE_BACK);
    }

    private void loadAppConfig() {
        mDisposableAppConfig = RetrofitFactory.getInstance()
                .api()
                .getSettingConfig()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<AppConfigResponse>() {
                    @Override
                    public void accept(AppConfigResponse appConfigResponse) throws Exception {
                        if (appConfigResponse.isETOEnabled()) {
                            mBottomNavigationView.getMenu().removeItem(R.id.navigation_main);
                            mBottomNavigationView.getMenu().removeItem(R.id.navigation_watchlist);
                            mBottomNavigationView.getMenu().removeItem(R.id.navigation_exchange);
                            mBottomNavigationView.getMenu().removeItem(R.id.navigation_account);
                            mBottomNavigationView.inflateMenu(R.menu.navigation_eto);
                            BottomNavigationViewHelper.removeShiftMode(mBottomNavigationView);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {

                    }
                });
    }

    private void checkVersion() {
        mDisposableVersionUpdate = RetrofitFactory.getInstance()
                .api()
                .checkAppUpdate()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<AppVersion>() {
                    @Override
                    public void accept(AppVersion appVersion) throws Exception {
                        if (appVersion.compareVersion(BuildConfig.VERSION_NAME)) {
                            if (appVersion.isForceUpdate(BuildConfig.VERSION_NAME)) {
                                if (Locale.getDefault().getLanguage().equals("zh")) {
                                    CybexDialog.showVersionUpdateDialogForced(BottomNavigationActivity.this, appVersion.getCnUpdateInfo(), new CybexDialog.ConfirmationDialogClickListener() {
                                        @Override
                                        public void onClick(Dialog dialog) {
                                            goToUpdate(appVersion.getUrl());
                                        }
                                    });
                                } else {
                                    CybexDialog.showVersionUpdateDialogForced(BottomNavigationActivity.this, appVersion.getEnUpdateInfo(), new CybexDialog.ConfirmationDialogClickListener() {
                                        @Override
                                        public void onClick(Dialog dialog) {
                                            goToUpdate(appVersion.getUrl());

                                        }
                                    });
                                }
                            } else {
                                if (Locale.getDefault().getLanguage().equals("zh")) {
                                    CybexDialog.showVersionUpdateDialog(BottomNavigationActivity.this, appVersion.getCnUpdateInfo(), new CybexDialog.ConfirmationDialogClickListener() {
                                        @Override
                                        public void onClick(Dialog dialog) {
                                            goToUpdate(appVersion.getUrl());

                                        }
                                    });
                                } else {
                                    CybexDialog.showVersionUpdateDialog(BottomNavigationActivity.this, appVersion.getEnUpdateInfo(), new CybexDialog.ConfirmationDialogClickListener() {
                                        @Override
                                        public void onClick(Dialog dialog) {
                                            goToUpdate(appVersion.getUrl());

                                        }
                                    });
                                }
                            }
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {

                    }
                });
    }

    private void checkVersionGoogleStore() {
        mDisposableVersionUpdate = RetrofitFactory.getInstance()
                .api()
                .checkAppUpdateGoogleStore()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<AppVersion>() {
                    @Override
                    public void accept(AppVersion appVersion) throws Exception {
                        if (appVersion.compareVersion(BuildConfig.VERSION_NAME)) {
                            if (appVersion.isForceUpdate(BuildConfig.VERSION_NAME)) {
                                if (Locale.getDefault().getLanguage().equals("zh")) {
                                    CybexDialog.showVersionUpdateDialogForced(BottomNavigationActivity.this, appVersion.getCnUpdateInfo(), new CybexDialog.ConfirmationDialogClickListener() {
                                        @Override
                                        public void onClick(Dialog dialog) {
                                            goToUpdate(appVersion.getUrl());
                                        }
                                    });
                                } else {
                                    CybexDialog.showVersionUpdateDialogForced(BottomNavigationActivity.this, appVersion.getEnUpdateInfo(), new CybexDialog.ConfirmationDialogClickListener() {
                                        @Override
                                        public void onClick(Dialog dialog) {
                                            goToUpdate(appVersion.getUrl());
                                        }
                                    });
                                }
                            } else {
                                if (Locale.getDefault().getLanguage().equals("zh")) {
                                    CybexDialog.showVersionUpdateDialog(BottomNavigationActivity.this, appVersion.getCnUpdateInfo(), new CybexDialog.ConfirmationDialogClickListener() {
                                        @Override
                                        public void onClick(Dialog dialog) {
                                            goToUpdate(appVersion.getUrl());
                                        }
                                    });
                                } else {
                                    CybexDialog.showVersionUpdateDialog(BottomNavigationActivity.this, appVersion.getEnUpdateInfo(), new CybexDialog.ConfirmationDialogClickListener() {
                                        @Override
                                        public void onClick(Dialog dialog) {
                                            goToUpdate(appVersion.getUrl());
                                        }
                                    });
                                }
                            }
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {

                    }
                });
    }

    private void goToUpdate(String url) {
        Intent browseIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browseIntent);
    }

}

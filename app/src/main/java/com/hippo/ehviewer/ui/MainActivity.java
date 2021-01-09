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

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.hippo.drawerlayout.DrawerLayout;
import com.hippo.ehviewer.AppConfig;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhTagDatabase;
import com.hippo.ehviewer.client.EhUrlOpener;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.ListUrlBuilder;
import com.hippo.ehviewer.client.parser.GalleryDetailUrlParser;
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser;
//import com.hippo.ehviewer.ui.scene.AnalyticsScene;
import com.hippo.ehviewer.ui.scene.BaseScene;
import com.hippo.ehviewer.ui.scene.CookieSignInScene;
import com.hippo.ehviewer.ui.scene.DownloadsScene;
import com.hippo.ehviewer.ui.scene.FavoritesScene;
import com.hippo.ehviewer.ui.scene.GalleryCommentsScene;
import com.hippo.ehviewer.ui.scene.GalleryDetailScene;
import com.hippo.ehviewer.ui.scene.GalleryInfoScene;
import com.hippo.ehviewer.ui.scene.GalleryListScene;
import com.hippo.ehviewer.ui.scene.GalleryPreviewsScene;
import com.hippo.ehviewer.ui.scene.HistoryScene;
import com.hippo.ehviewer.ui.scene.ProgressScene;
import com.hippo.ehviewer.ui.scene.SecurityScene;
import com.hippo.ehviewer.ui.scene.SelectSiteScene;
import com.hippo.ehviewer.ui.scene.SignInScene;
import com.hippo.ehviewer.ui.scene.SolidScene;
import com.hippo.ehviewer.ui.scene.WarningScene;
import com.hippo.ehviewer.ui.scene.WebViewSignInScene;
import com.hippo.ehviewer.widget.EhDrawerLayout;
import com.hippo.io.UniFileInputStreamPipe;
import com.hippo.network.Network;
import com.hippo.scene.Announcer;
import com.hippo.scene.SceneFragment;
import com.hippo.scene.StageActivity;
import com.hippo.unifile.UniFile;
import com.hippo.util.BitmapUtils;
import com.hippo.util.ClipboardUtil;
import com.hippo.widget.LoadImageView;
import com.hippo.yorozuya.IOUtils;
import com.hippo.yorozuya.SimpleHandler;
import com.hippo.yorozuya.ViewUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class MainActivity extends StageActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 0;

    private static final int REQUEST_CODE_SETTINGS = 0;

    private static final String KEY_NAV_CHECKED_ITEM = "nav_checked_item";
    private static final String KEY_CLIP_TEXT_HASH_CODE = "clip_text_hash_code";

    static {
        registerLaunchMode(SecurityScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(WarningScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
//        registerLaunchMode(AnalyticsScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(SignInScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(WebViewSignInScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(CookieSignInScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(SelectSiteScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(GalleryListScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TOP);
        registerLaunchMode(GalleryDetailScene.class, SceneFragment.LAUNCH_MODE_STANDARD);
        registerLaunchMode(GalleryInfoScene.class, SceneFragment.LAUNCH_MODE_STANDARD);
        registerLaunchMode(GalleryCommentsScene.class, SceneFragment.LAUNCH_MODE_STANDARD);
        registerLaunchMode(GalleryPreviewsScene.class, SceneFragment.LAUNCH_MODE_STANDARD);
        registerLaunchMode(DownloadsScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(FavoritesScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(HistoryScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TOP);
        registerLaunchMode(ProgressScene.class, SceneFragment.LAUNCH_MODE_STANDARD);
    }

    /*---------------
     Whole life cycle
     ---------------*/
    @Nullable
    private EhDrawerLayout mDrawerLayout;
    @Nullable
    private NavigationView mNavView;
    @Nullable
    private MaterialCardView mRightDrawer;
    @Nullable
    private LoadImageView mAvatar;
    @Nullable
    private TextView mDisplayName;
    private int mNavCheckedItem = 0;

    @Override
    public int getContainerViewId() {
        return R.id.fragment_container;
    }

    @Nullable
    @Override
    protected Announcer getLaunchAnnouncer() {
        if (!TextUtils.isEmpty(Settings.getSecurity())) {
            return new Announcer(SecurityScene.class);
        } else if (Settings.getShowWarning()) {
            return new Announcer(WarningScene.class);
//        } else if (Settings.getAskAnalytics()) {
//            return new Announcer(AnalyticsScene.class);
        } else if (EhUtils.needSignedIn(this)) {
            return new Announcer(SignInScene.class);
        } else if (Settings.getSelectSite()) {
            return new Announcer(SelectSiteScene.class);
        } else {
            Bundle args = new Bundle();
            args.putString(GalleryListScene.KEY_ACTION, Settings.getLaunchPageGalleryListSceneAction());
            return new Announcer(GalleryListScene.class).setArgs(args);
        }
    }

    // Sometimes scene can't show directly
    private Announcer processAnnouncer(Announcer announcer) {
        if (0 == getSceneCount()) {
            if (!TextUtils.isEmpty(Settings.getSecurity())) {
                Bundle newArgs = new Bundle();
                newArgs.putString(SecurityScene.KEY_TARGET_SCENE, announcer.getClazz().getName());
                newArgs.putBundle(SecurityScene.KEY_TARGET_ARGS, announcer.getArgs());
                return new Announcer(SecurityScene.class).setArgs(newArgs);
            } else if (Settings.getShowWarning()) {
                Bundle newArgs = new Bundle();
                newArgs.putString(WarningScene.KEY_TARGET_SCENE, announcer.getClazz().getName());
                newArgs.putBundle(WarningScene.KEY_TARGET_ARGS, announcer.getArgs());
                return new Announcer(WarningScene.class).setArgs(newArgs);
//            } else if (Settings.getAskAnalytics()) {
//                Bundle newArgs = new Bundle();
//                newArgs.putString(AnalyticsScene.KEY_TARGET_SCENE, announcer.getClazz().getName());
//                newArgs.putBundle(AnalyticsScene.KEY_TARGET_ARGS, announcer.getArgs());
//                return new Announcer(AnalyticsScene.class).setArgs(newArgs);
            } else if (EhUtils.needSignedIn(this)) {
                Bundle newArgs = new Bundle();
                newArgs.putString(SignInScene.KEY_TARGET_SCENE, announcer.getClazz().getName());
                newArgs.putBundle(SignInScene.KEY_TARGET_ARGS, announcer.getArgs());
                return new Announcer(SignInScene.class).setArgs(newArgs);
            } else if (Settings.getSelectSite()) {
                Bundle newArgs = new Bundle();
                newArgs.putString(SelectSiteScene.KEY_TARGET_SCENE, announcer.getClazz().getName());
                newArgs.putBundle(SelectSiteScene.KEY_TARGET_ARGS, announcer.getArgs());
                return new Announcer(SelectSiteScene.class).setArgs(newArgs);
            }
        }
        return announcer;
    }

    private File saveImageToTempFile(UniFile file) {
        if (null == file) {
            return null;
        }

        Bitmap bitmap = null;
        try {
            bitmap = BitmapUtils.decodeStream(new UniFileInputStreamPipe(file), -1, -1, 500 * 500, false, false, null);
        } catch (OutOfMemoryError e) {
            // Ignore
        }
        if (null == bitmap) {
            return null;
        }

        File temp = AppConfig.createTempFile();
        if (null == temp) {
            return null;
        }

        OutputStream os = null;
        try {
            os = new FileOutputStream(temp);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os);
            return temp;
        } catch (IOException e) {
            return null;
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    private boolean handleIntent(Intent intent) {
        if (intent == null) {
            return false;
        }

        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            Uri uri = intent.getData();
            if (uri == null) {
                return false;
            }
            Announcer announcer = EhUrlOpener.parseUrl(uri.toString());
            if (announcer != null) {
                startScene(processAnnouncer(announcer));
                return true;
            }
        } else if (Intent.ACTION_SEND.equals(action)) {
            String type = intent.getType();
            if ("text/plain".equals(type)) {
                ListUrlBuilder builder = new ListUrlBuilder();
                builder.setKeyword(intent.getStringExtra(Intent.EXTRA_TEXT));
                startScene(processAnnouncer(GalleryListScene.getStartAnnouncer(builder)));
                return true;
            } else if (type.startsWith("image/")) {
                Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (null != uri) {
                    UniFile file = UniFile.fromUri(this, uri);
                    File temp = saveImageToTempFile(file);
                    if (null != temp) {
                        ListUrlBuilder builder = new ListUrlBuilder();
                        builder.setMode(ListUrlBuilder.MODE_IMAGE_SEARCH);
                        builder.setImagePath(temp.getPath());
                        builder.setUseSimilarityScan(true);
                        builder.setShowExpunged(true);
                        startScene(processAnnouncer(GalleryListScene.getStartAnnouncer(builder)));
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    protected void onUnrecognizedIntent(@Nullable Intent intent) {
        Class<?> clazz = getTopSceneClass();
        if (clazz != null && SolidScene.class.isAssignableFrom(clazz)) {
            // TODO the intent lost
            return;
        }

        if (!handleIntent(intent)) {
            boolean handleUrl = false;
            if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
                handleUrl = true;
                Toast.makeText(this, R.string.error_cannot_parse_the_url, Toast.LENGTH_SHORT).show();
            }

            if (0 == getSceneCount()) {
                if (handleUrl) {
                    finish();
                } else {
                    Bundle args = new Bundle();
                    args.putString(GalleryListScene.KEY_ACTION, Settings.getLaunchPageGalleryListSceneAction());
                    startScene(processAnnouncer(new Announcer(GalleryListScene.class).setArgs(args)));
                }
            }
        }
    }

    @Nullable
    @Override
    protected Announcer onStartSceneFromIntent(@NonNull Class<?> clazz, @Nullable Bundle args) {
        return processAnnouncer(new Announcer(clazz).setArgs(args));
    }

    @Override
    protected void onCreate2(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);

        mDrawerLayout = (EhDrawerLayout) ViewUtils.$$(this, R.id.draw_view);
        mNavView = (NavigationView) ViewUtils.$$(this, R.id.nav_view);
        mRightDrawer = (MaterialCardView) ViewUtils.$$(this, R.id.right_drawer);
        View headerLayout = mNavView.getHeaderView(0);
        mAvatar = (LoadImageView) ViewUtils.$$(headerLayout, R.id.avatar);
        mDisplayName = (TextView) ViewUtils.$$(headerLayout, R.id.display_name);
        ViewUtils.$$(headerLayout, R.id.night_mode).setOnClickListener(v -> {
            int theme = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_YES) > 0 ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES;
            AppCompatDelegate.setDefaultNightMode(theme);
            Settings.putTheme(theme);
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        updateProfile();

        if (mNavView != null) {
            mNavView.setNavigationItemSelectedListener(this);
        }

        if (savedInstanceState == null) {
            checkDownloadLocation();
            if (Settings.getCellularNetworkWarning()) {
                checkCellularNetwork();
            }
        } else {
            onRestore(savedInstanceState);
        }

        EhTagDatabase.update(this);
    }

    private void checkDownloadLocation() {
        UniFile uniFile = Settings.getDownloadLocation();
        // null == uniFile for first start
        if (null == uniFile || uniFile.ensureDir()) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.waring)
                .setMessage(R.string.invalid_download_location)
                .setPositiveButton(R.string.get_it, null)
                .show();
    }

    private void checkCellularNetwork() {
        if (Network.getActiveNetworkType(this) == ConnectivityManager.TYPE_MOBILE) {
            showTip(R.string.cellular_network_warning, BaseScene.LENGTH_SHORT);
        }
    }

    private void onRestore(Bundle savedInstanceState) {
        mNavCheckedItem = savedInstanceState.getInt(KEY_NAV_CHECKED_ITEM);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putInt(KEY_NAV_CHECKED_ITEM, mNavCheckedItem);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mDrawerLayout = null;
        mNavView = null;
        mRightDrawer = null;
        mAvatar = null;
        mDisplayName = null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        setNavCheckedItem(mNavCheckedItem);

        checkClipboardUrl();
    }

    @Override
    protected void onTransactScene() {
        super.onTransactScene();

        checkClipboardUrl();
    }

    private void checkClipboardUrl() {
        SimpleHandler.getInstance().postDelayed(() -> {
            if (!isSolid()) {
                checkClipboardUrlInternal();
            }
        }, 300);
    }

    private boolean isSolid() {
        Class<?> topClass = getTopSceneClass();
        return topClass == null || SolidScene.class.isAssignableFrom(topClass);
    }

    @Nullable
    private Announcer createAnnouncerFromClipboardUrl(String url) {
        GalleryDetailUrlParser.Result result1 = GalleryDetailUrlParser.parse(url, false);
        if (result1 != null) {
            Bundle args = new Bundle();
            args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GID_TOKEN);
            args.putLong(GalleryDetailScene.KEY_GID, result1.gid);
            args.putString(GalleryDetailScene.KEY_TOKEN, result1.token);
            return new Announcer(GalleryDetailScene.class).setArgs(args);
        }

        GalleryPageUrlParser.Result result2 = GalleryPageUrlParser.parse(url, false);
        if (result2 != null) {
            Bundle args = new Bundle();
            args.putString(ProgressScene.KEY_ACTION, ProgressScene.ACTION_GALLERY_TOKEN);
            args.putLong(ProgressScene.KEY_GID, result2.gid);
            args.putString(ProgressScene.KEY_PTOKEN, result2.pToken);
            args.putInt(ProgressScene.KEY_PAGE, result2.page);
            return new Announcer(ProgressScene.class).setArgs(args);
        }

        return null;
    }

    private void checkClipboardUrlInternal() {
        String text = ClipboardUtil.getTextFromClipboard();
        int hashCode = text != null ? text.hashCode() : 0;

        if (text != null && hashCode != 0 && Settings.getClipboardTextHashCode() != hashCode) {
            Announcer announcer = createAnnouncerFromClipboardUrl(text);
            if (announcer != null && mDrawerLayout != null) {
                Snackbar snackbar = Snackbar.make(mDrawerLayout, R.string.clipboard_gallery_url_snack_message, Snackbar.LENGTH_SHORT);
                snackbar.setAction(R.string.clipboard_gallery_url_snack_action, v -> startScene(announcer));
                snackbar.show();
            }
        }

        Settings.putClipboardTextHashCode(hashCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length == 1 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.you_rejected_me, Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onSceneViewCreated(SceneFragment scene, Bundle savedInstanceState) {
        super.onSceneViewCreated(scene, savedInstanceState);

        createDrawerView(scene);
    }

    public void createDrawerView(SceneFragment scene) {
        if (scene instanceof BaseScene && mRightDrawer != null && mDrawerLayout != null) {
            BaseScene baseScene = (BaseScene) scene;
            mRightDrawer.removeAllViews();
            View drawerView = baseScene.createDrawerView(
                    baseScene.getLayoutInflater(), mRightDrawer, null);
            if (drawerView != null) {
                mRightDrawer.addView(drawerView);
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.RIGHT);
            } else {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
            }
        }
    }

    @Override
    public void onSceneViewDestroyed(SceneFragment scene) {
        super.onSceneViewDestroyed(scene);

        if (scene instanceof BaseScene) {
            BaseScene baseScene = (BaseScene) scene;
            baseScene.destroyDrawerView();
        }
    }

    public void updateProfile() {
        if (null == mAvatar || null == mDisplayName) {
            return;
        }

        String avatarUrl = Settings.getAvatar();
        if (TextUtils.isEmpty(avatarUrl)) {
            mAvatar.load(R.drawable.default_avatar);
        } else {
            mAvatar.load(avatarUrl, avatarUrl);
        }

        String displayName = Settings.getDisplayName();
        if (TextUtils.isEmpty(displayName)) {
            displayName = getString(R.string.default_display_name);
        }
        mDisplayName.setText(displayName);
    }

    public void addAboveSnackView(View view) {
        if (mDrawerLayout != null) {
            mDrawerLayout.addAboveSnackView(view);
        }
    }

    public void removeAboveSnackView(View view) {
        if (mDrawerLayout != null) {
            mDrawerLayout.removeAboveSnackView(view);
        }
    }

    public int getDrawerLockMode(int edgeGravity) {
        if (mDrawerLayout != null) {
            return mDrawerLayout.getDrawerLockMode(edgeGravity);
        } else {
            return DrawerLayout.LOCK_MODE_UNLOCKED;
        }
    }

    public void setDrawerLockMode(int lockMode, int edgeGravity) {
        if (mDrawerLayout != null) {
            mDrawerLayout.setDrawerLockMode(lockMode, edgeGravity);
        }
    }

    public void openDrawer(int drawerGravity) {
        if (mDrawerLayout != null) {
            mDrawerLayout.openDrawer(drawerGravity);
        }
    }

    public void closeDrawer(int drawerGravity) {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(drawerGravity);
        }
    }

    public void toggleDrawer(int drawerGravity) {
        if (mDrawerLayout != null) {
            if (mDrawerLayout.isDrawerOpen(drawerGravity)) {
                mDrawerLayout.closeDrawer(drawerGravity);
            } else {
                mDrawerLayout.openDrawer(drawerGravity);
            }
        }
    }

    public void setNavCheckedItem(@IdRes int resId) {
        mNavCheckedItem = resId;
        if (mNavView != null) {
            if (resId == 0) {
                mNavView.setCheckedItem(R.id.nav_stub);
            } else {
                mNavView.setCheckedItem(resId);
            }
        }
    }

    public void showTip(@StringRes int id, int length) {
        showTip(getString(id), length);
    }

    /**
     * If activity is running, show snack bar, otherwise show toast
     */
    public void showTip(CharSequence message, int length) {
        if (null != mDrawerLayout) {
            Snackbar.make(mDrawerLayout, message,
                    length == BaseScene.LENGTH_LONG ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, message,
                    length == BaseScene.LENGTH_LONG ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout != null && (mDrawerLayout.isDrawerOpen(Gravity.LEFT) ||
                mDrawerLayout.isDrawerOpen(Gravity.RIGHT))) {
            mDrawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Don't select twice
        if (item.isChecked()) {
            return false;
        }

        int id = item.getItemId();

        if (id == R.id.nav_homepage) {
            Bundle args = new Bundle();
            args.putString(GalleryListScene.KEY_ACTION, GalleryListScene.ACTION_HOMEPAGE);
            startSceneFirstly(new Announcer(GalleryListScene.class)
                    .setArgs(args));
        } else if (id == R.id.nav_subscription) {
            Bundle args = new Bundle();
            args.putString(GalleryListScene.KEY_ACTION, GalleryListScene.ACTION_SUBSCRIPTION);
            startSceneFirstly(new Announcer(GalleryListScene.class)
                    .setArgs(args));
        } else if (id == R.id.nav_whats_hot) {
            Bundle args = new Bundle();
            args.putString(GalleryListScene.KEY_ACTION, GalleryListScene.ACTION_WHATS_HOT);
            startSceneFirstly(new Announcer(GalleryListScene.class)
                    .setArgs(args));
        } else if (id == R.id.nav_toplist) {
            Bundle args = new Bundle();
            args.putString(GalleryListScene.KEY_ACTION, GalleryListScene.ACTION_TOP_LIST);
            startSceneFirstly(new Announcer(GalleryListScene.class)
                    .setArgs(args));
        } else if (id == R.id.nav_favourite) {
            startScene(new Announcer(FavoritesScene.class));
        } else if (id == R.id.nav_history) {
            startScene(new Announcer(HistoryScene.class));
        } else if (id == R.id.nav_downloads) {
            startScene(new Announcer(DownloadsScene.class));
        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, REQUEST_CODE_SETTINGS);
        }

        if (id != R.id.nav_stub && mDrawerLayout != null) {
            mDrawerLayout.closeDrawers();
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SETTINGS:
                if (RESULT_OK == resultCode) {
                    refreshTopScene();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    public void setDrawerGestureBlocker(EhDrawerLayout.GestureBlocker gestureBlocker) {
        if (mDrawerLayout != null) {
            mDrawerLayout.setGestureBlocker(gestureBlocker);
        }
    }

    public boolean isDrawersVisible() {
        if (mDrawerLayout != null) {
            return mDrawerLayout.isDrawersVisible();
        } else {
            return false;
        }
    }
}

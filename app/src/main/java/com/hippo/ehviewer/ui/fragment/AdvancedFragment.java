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

package com.hippo.ehviewer.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hippo.ehviewer.AppConfig;
import com.hippo.ehviewer.BuildConfig;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.GetText;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.scene.BaseScene;
import com.hippo.util.ExceptionUtils;
import com.hippo.util.IoThreadPoolExecutor;
import com.hippo.util.LogCat;
import com.hippo.util.ReadableTime;
import com.hippo.yorozuya.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AdvancedFragment extends BasePreferenceFragment {
    private static final String KEY_DUMP_LOGCAT = "dump_logcat";
    private static final String KEY_CLEAR_MEMORY_CACHE = "clear_memory_cache";
    private static final String KEY_APP_LANGUAGE = "app_language";
    private static final String KEY_IMPORT_DATA = "import_data";
    private static final String KEY_EXPORT_DATA = "export_data";

    ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument(),
            uri -> {
                if (uri != null) {
                    try {
                        // grantUriPermission might throw RemoteException on MIUI
                        requireActivity().grantUriPermission(BuildConfig.APPLICATION_ID, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    } catch (Exception e) {
                        ExceptionUtils.throwIfFatal(e);
                        e.printStackTrace();
                    }
                    try {
                        AlertDialog alertDialog = new MaterialAlertDialogBuilder(requireActivity())
                                .setCancelable(false)
                                .setView(R.layout.preference_dialog_task)
                                .show();
                        IoThreadPoolExecutor.getInstance().execute(() -> {
                            boolean success = EhDB.exportDB(requireActivity(), uri);
                            Activity activity = getActivity();
                            if (activity != null) {
                                activity.runOnUiThread(() -> {
                                    if (alertDialog.isShowing()) {
                                        alertDialog.dismiss();
                                    }
                                    showTip(
                                            (success)
                                                    ? GetText.getString(R.string.settings_advanced_export_data_to, uri.toString())
                                                    : GetText.getString(R.string.settings_advanced_export_data_failed),
                                            BaseScene.LENGTH_SHORT);
                                });
                            }
                        });
                    } catch (Exception e) {
                        showTip(R.string.settings_advanced_export_data_failed, BaseScene.LENGTH_SHORT);
                    }
                }
            });
    ActivityResultLauncher<String> dumpLogcatLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument(),
            uri -> {
                if (uri != null) {
                    try {
                        // grantUriPermission might throw RemoteException on MIUI
                        requireActivity().grantUriPermission(BuildConfig.APPLICATION_ID, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    } catch (Exception e) {
                        ExceptionUtils.throwIfFatal(e);
                        e.printStackTrace();
                    }
                    try {
                        File zipFile = new File(AppConfig.getExternalTempDir(), "logs.zip");
                        if (zipFile.exists()) {
                            //noinspection ResultOfMethodCallIgnored
                            zipFile.delete();
                        }

                        ArrayList<File> files = new ArrayList<>();
                        files.addAll(Arrays.asList(AppConfig.getExternalParseErrorDir().listFiles()));
                        files.addAll(Arrays.asList(AppConfig.getExternalCrashDir().listFiles()));

                        boolean finished = false;

                        BufferedInputStream origin = null;
                        ZipOutputStream out = null;
                        try {
                            FileOutputStream dest = new FileOutputStream(zipFile);
                            out = new ZipOutputStream(new BufferedOutputStream(dest));
                            byte[] bytes = new byte[1024 * 64];

                            for (File file : files) {
                                if (!file.isFile()) {
                                    continue;
                                }
                                try {
                                    FileInputStream fi = new FileInputStream(file);
                                    origin = new BufferedInputStream(fi, bytes.length);

                                    ZipEntry entry = new ZipEntry(file.getName());
                                    out.putNextEntry(entry);
                                    int count;
                                    while ((count = origin.read(bytes, 0, bytes.length)) != -1) {
                                        out.write(bytes, 0, count);
                                    }
                                    origin.close();
                                    origin = null;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            ZipEntry entry = new ZipEntry("logcat-" + ReadableTime.getFilenamableTime(System.currentTimeMillis()) + ".txt");
                            out.putNextEntry(entry);
                            LogCat.save(out);
                            out.closeEntry();
                            out.close();
                            IOUtils.copy(new FileInputStream(zipFile), requireActivity().getContentResolver().openOutputStream(uri));
                            finished = true;
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            if (origin != null) {
                                origin.close();
                            }
                            if (out != null) {
                                out.close();
                            }
                        }
                        if (!finished) {
                            finished = LogCat.save(requireActivity().getContentResolver().openOutputStream(uri));
                        }
                        showTip(
                                finished ? getString(R.string.settings_advanced_dump_logcat_to, uri.toString()) :
                                        getString(R.string.settings_advanced_dump_logcat_failed), BaseScene.LENGTH_SHORT);
                    } catch (Exception e) {
                        showTip(getString(R.string.settings_advanced_dump_logcat_failed), BaseScene.LENGTH_SHORT);
                    }
                }
            });
    ActivityResultLauncher<String[]> importDataLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    try {
                        // grantUriPermission might throw RemoteException on MIUI
                        requireActivity().grantUriPermission(BuildConfig.APPLICATION_ID, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception e) {
                        ExceptionUtils.throwIfFatal(e);
                        e.printStackTrace();
                    }
                    try {
                        AlertDialog alertDialog = new MaterialAlertDialogBuilder(requireActivity())
                                .setCancelable(false)
                                .setView(R.layout.preference_dialog_task)
                                .show();
                        IoThreadPoolExecutor.getInstance().execute(() -> {
                            final String error = EhDB.importDB(requireActivity(), uri);
                            Activity activity = getActivity();
                            if (activity != null) {
                                activity.runOnUiThread(() -> {
                                    if (alertDialog.isShowing()) {
                                        alertDialog.dismiss();
                                    }
                                    if (null == error) {
                                        showTip(getString(R.string.settings_advanced_import_data_successfully), BaseScene.LENGTH_SHORT);
                                    } else {
                                        showTip(error, BaseScene.LENGTH_SHORT);
                                    }
                                });
                            }

                        });
                    } catch (Exception e) {
                        showTip(e.getLocalizedMessage(), BaseScene.LENGTH_SHORT);
                    }
                }
            });

    @Override
    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.advanced_settings);

        Preference dumpLogcat = findPreference(KEY_DUMP_LOGCAT);
        Preference clearMemoryCache = findPreference(KEY_CLEAR_MEMORY_CACHE);
        Preference appLanguage = findPreference(KEY_APP_LANGUAGE);
        Preference importData = findPreference(KEY_IMPORT_DATA);
        Preference exportData = findPreference(KEY_EXPORT_DATA);

        dumpLogcat.setOnPreferenceClickListener(this);
        clearMemoryCache.setOnPreferenceClickListener(this);
        importData.setOnPreferenceClickListener(this);
        exportData.setOnPreferenceClickListener(this);

        appLanguage.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (KEY_DUMP_LOGCAT.equals(key)) {
            try {
                dumpLogcatLauncher.launch("log-" + ReadableTime.getFilenamableTime(System.currentTimeMillis()) + ".zip");
            } catch (Throwable e) {
                ExceptionUtils.throwIfFatal(e);
                showTip(R.string.error_cant_find_activity, BaseScene.LENGTH_SHORT);
            }
            return true;
        } else if (KEY_CLEAR_MEMORY_CACHE.equals(key)) {
            ((EhApplication) requireContext().getApplicationContext()).clearMemoryCache();
            Runtime.getRuntime().gc();
            showTip(R.string.settings_advanced_clear_memory_cache_done, BaseScene.LENGTH_SHORT);
        } else if (KEY_IMPORT_DATA.equals(key)) {
            try {
                importDataLauncher.launch(new String[]{"*/*"});
            } catch (Throwable e) {
                ExceptionUtils.throwIfFatal(e);
                showTip(R.string.error_cant_find_activity, BaseScene.LENGTH_SHORT);
            }
            return true;
        } else if (KEY_EXPORT_DATA.equals(key)) {
            try {
                exportLauncher.launch(ReadableTime.getFilenamableTime(System.currentTimeMillis()) + ".db");
            } catch (Throwable e) {
                ExceptionUtils.throwIfFatal(e);
                showTip(R.string.error_cant_find_activity, BaseScene.LENGTH_SHORT);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (KEY_APP_LANGUAGE.equals(key)) {
            ((EhApplication) getActivity().getApplication()).recreate();
            return true;
        }
        return false;
    }

    @Override
    public int getFragmentTitle() {
        return R.string.settings_advanced;
    }
}

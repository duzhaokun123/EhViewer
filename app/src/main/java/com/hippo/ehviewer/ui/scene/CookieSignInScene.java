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

package com.hippo.ehviewer.ui.scene;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhCookieStore;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.util.ClipboardUtil;
import com.hippo.util.ExceptionUtils;
import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.ViewUtils;

import okhttp3.Cookie;

public class CookieSignInScene extends SolidScene implements EditText.OnEditorActionListener,
        View.OnClickListener {

    /*---------------
     View life cycle
     ---------------*/
    @Nullable
    private TextInputLayout mIpbMemberIdLayout;
    @Nullable
    private TextInputLayout mIpbPassHashLayout;
    @Nullable
    private TextInputLayout mIgneousLayout;
    @Nullable
    private EditText mIpbMemberId;
    @Nullable
    private EditText mIpbPassHash;
    @Nullable
    private EditText mIgneous;
    @Nullable
    private View mOk;
    @Nullable
    private TextView mFromClipboard;

    // false for error
    private static boolean checkIpbMemberId(String id) {
        for (int i = 0, n = id.length(); i < n; i++) {
            char ch = id.charAt(i);
            if (!(ch >= '0' && ch <= '9')) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkIpbPassHash(String hash) {
        if (32 != hash.length()) {
            return false;
        }

        for (int i = 0, n = hash.length(); i < n; i++) {
            char ch = hash.charAt(i);
            if (!(ch >= '0' && ch <= '9') && !(ch >= 'a' && ch <= 'z')) {
                return false;
            }
        }
        return true;
    }

    private static Cookie newCookie(String name, String value, String domain) {
        return new Cookie.Builder().name(name).value(value)
                .domain(domain).expiresAt(Long.MAX_VALUE).build();
    }

    @Override
    public boolean needShowLeftDrawer() {
        return false;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_cookie_sign_in, container, false);
        mIpbMemberIdLayout = (TextInputLayout) ViewUtils.$$(view, R.id.ipb_member_id_layout);
        mIpbMemberId = mIpbMemberIdLayout.getEditText();
        AssertUtils.assertNotNull(mIpbMemberId);
        mIpbPassHashLayout = (TextInputLayout) ViewUtils.$$(view, R.id.ipb_pass_hash_layout);
        mIpbPassHash = mIpbPassHashLayout.getEditText();
        AssertUtils.assertNotNull(mIpbPassHash);
        mIgneousLayout = (TextInputLayout) ViewUtils.$$(view, R.id.igneous_layout);
        mIgneous = mIgneousLayout.getEditText();
        AssertUtils.assertNotNull(mIgneous);
        mOk = ViewUtils.$$(view, R.id.ok);
        mFromClipboard = (TextView) ViewUtils.$$(view, R.id.from_clipboard);

        mFromClipboard.setPaintFlags(mFromClipboard.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);

        mIpbPassHash.setOnEditorActionListener(this);

        mOk.setOnClickListener(this);
        mFromClipboard.setOnClickListener(this);

        // Try to get old version cookie info
        Context context = getContext();
        AssertUtils.assertNotNull(context);
        SharedPreferences sharedPreferences = context.getSharedPreferences("eh_info", 0);
        String ipbMemberId = sharedPreferences.getString("ipb_member_id", null);
        String ipbPassHash = sharedPreferences.getString("ipb_pass_hash", null);
        String igneous = sharedPreferences.getString("igneous", null);
        boolean getIt = false;
        if (!TextUtils.isEmpty(ipbMemberId)) {
            mIpbMemberId.setText(ipbMemberId);
            getIt = true;
        }
        if (!TextUtils.isEmpty(ipbPassHash)) {
            mIpbPassHash.setText(ipbPassHash);
            getIt = true;
        }
        if (!TextUtils.isEmpty(igneous)) {
            mIgneous.setText(igneous);
            getIt = true;
        }
        if (getIt) {
            showTip(R.string.found_cookies, LENGTH_SHORT);
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        showSoftInput(mIpbMemberId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mIpbMemberIdLayout = null;
        mIpbPassHashLayout = null;
        mIgneousLayout = null;
        mIpbMemberId = null;
        mIpbPassHash = null;
        mIgneous = null;
    }

    @Override
    public void onClick(View v) {
        if (mOk == v) {
            enter();
        } else if (mFromClipboard == v) {
            fillCookiesFromClipboard();
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (mIpbPassHash == v) {
            enter();
        }
        return true;
    }

    public void enter() {
        Context context = getContext();
        if (null == context || null == mIpbMemberIdLayout || null == mIpbPassHashLayout ||
                null == mIgneousLayout || null == mIpbMemberId || null == mIpbPassHash ||
                null == mIgneous) {
            return;
        }

        final String ipbMemberId = mIpbMemberId.getText().toString().trim();
        final String ipbPassHash = mIpbPassHash.getText().toString().trim();
        final String igneous = mIgneous.getText().toString().trim();

        if (TextUtils.isEmpty(ipbMemberId)) {
            mIpbMemberIdLayout.setError(getString(R.string.text_is_empty));
            return;
        } else {
            mIpbMemberIdLayout.setError(null);
        }
        if (TextUtils.isEmpty(ipbPassHash)) {
            mIpbPassHashLayout.setError(getString(R.string.text_is_empty));
            return;
        } else {
            mIpbPassHashLayout.setError(null);
        }

        hideSoftInput();

        if (!checkIpbMemberId(ipbMemberId) || !(checkIpbPassHash(ipbPassHash))) {
            new MaterialAlertDialogBuilder(context).setTitle(R.string.waring)
                    .setMessage(R.string.wrong_cookie_warning)
                    .setPositiveButton(R.string.i_will_check_it, null)
                    .setNegativeButton(R.string.i_dont_think_so, (dialog, which) -> {
                        storeCookie(ipbMemberId, ipbPassHash, igneous);
                        setResult(RESULT_OK, null);
                        finish();
                    }).show();
        } else {
            storeCookie(ipbMemberId, ipbPassHash, igneous);
            setResult(RESULT_OK, null);
            finish();
        }
    }

    private void storeCookie(String id, String hash, String igneous) {
        Context context = getContext();
        if (null == context) {
            return;
        }

        EhUtils.signOut(context);

        EhCookieStore store = EhApplication.getEhCookieStore(context);
        store.addCookie(newCookie(EhCookieStore.KEY_IPD_MEMBER_ID, id, EhUrl.DOMAIN_E));
        store.addCookie(newCookie(EhCookieStore.KEY_IPD_MEMBER_ID, id, EhUrl.DOMAIN_EX));
        store.addCookie(newCookie(EhCookieStore.KEY_IPD_PASS_HASH, hash, EhUrl.DOMAIN_E));
        store.addCookie(newCookie(EhCookieStore.KEY_IPD_PASS_HASH, hash, EhUrl.DOMAIN_EX));
        if (!igneous.isEmpty()) {
            store.addCookie(newCookie(EhCookieStore.KEY_IGNEOUS, igneous, EhUrl.DOMAIN_E));
            store.addCookie(newCookie(EhCookieStore.KEY_IGNEOUS, igneous, EhUrl.DOMAIN_EX));
        }
    }

    private void fillCookiesFromClipboard() {
        hideSoftInput();
        String text = ClipboardUtil.getTextFromClipboard();
        if (text == null) {
            showTip(R.string.from_clipboard_error, LENGTH_SHORT);
            return;
        }
        try {
            String[] kvs;
            if (text.contains(";")) {
                kvs = text.split(";");
            } else if (text.contains("\n")) {
                kvs = text.split("\n");
            } else {
                showTip(R.string.from_clipboard_error, LENGTH_SHORT);
                return;
            }
            if (kvs.length < 3) {
                showTip(R.string.from_clipboard_error, LENGTH_SHORT);
                return;
            }
            for (String s : kvs) {
                String[] kv;
                if (s.contains("=")) {
                    kv = s.split("=");
                } else if (s.contains(":")) {
                    kv = s.split(":");
                } else {
                    continue;
                }
                if (kv.length != 2) {
                    continue;
                }
                switch (kv[0].trim().toLowerCase()) {
                    case "ipb_member_id":
                        if (mIpbMemberId != null) {
                            mIpbMemberId.setText(kv[1].trim());
                        }
                        break;
                    case "ipb_pass_hash":
                        if (mIpbPassHash != null) {
                            mIpbPassHash.setText(kv[1].trim());
                        }
                        break;
                    case "igneous":
                        if (mIgneous != null) {
                            mIgneous.setText(kv[1].trim());
                        }
                        break;
                }
            }
            enter();
        } catch (Exception e) {
            ExceptionUtils.throwIfFatal(e);
            e.printStackTrace();
            showTip(R.string.from_clipboard_error, LENGTH_SHORT);
        }
    }
}

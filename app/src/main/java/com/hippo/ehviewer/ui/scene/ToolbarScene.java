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

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;

import com.hippo.ehviewer.R;

public abstract class ToolbarScene extends BaseScene {

    @Nullable
    private Toolbar mToolbar;

    private CharSequence mTempTitle;

    @Nullable
    public View onCreateViewWithToolbar(LayoutInflater inflater,
                                        @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return null;
    }

    @Nullable
    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater,
                                   @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_toolbar, container, false);
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        FrameLayout contentPanel = view.findViewById(R.id.content_panel);

        View contentView = onCreateViewWithToolbar(inflater, contentPanel, savedInstanceState);
        if (contentView == null) {
            return null;
        } else {
            mToolbar = toolbar;
            contentPanel.addView(contentView, 0);
            return view;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mToolbar = null;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mToolbar != null) {
            if (mTempTitle != null) {
                mToolbar.setTitle(mTempTitle);
                mTempTitle = null;
            }

            int menuResId = getMenuResId();
            if (menuResId != 0) {
                mToolbar.inflateMenu(menuResId);
                mToolbar.setOnMenuItemClickListener(ToolbarScene.this::onMenuItemClick);
            }
            mToolbar.setNavigationOnClickListener(v -> onNavigationClick());
        }
    }

    @Override
    public boolean needWhiteStatusBar() {
        return false;
    }


    public int getMenuResId() {
        return 0;
    }

    public boolean onMenuItemClick(MenuItem item) {
        return false;
    }

    public void onNavigationClick() {
    }

    public void setNavigationIcon(@DrawableRes int resId) {
        if (mToolbar != null) {
            mToolbar.setNavigationIcon(resId);
        }
    }

    public void setNavigationIcon(@Nullable Drawable icon) {
        if (mToolbar != null) {
            mToolbar.setNavigationIcon(icon);
        }
    }

    public void setTitle(@StringRes int resId) {
        setTitle(getString(resId));
    }

    public void setTitle(CharSequence title) {
        if (mToolbar != null) {
            mToolbar.setTitle(title);
        } else {
            mTempTitle = title;
        }
    }
}

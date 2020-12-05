package com.hippo.ehviewer.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.hippo.ehviewer.Analytics;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;

/**
 * Created by Mo10 on 2018/2/10.
 */

public class PrivacyFragment extends BaseSettingsFragment {
    private static final String KEY_PATTERN_PROTECTION = "pattern_protection";

    @Override
    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.privacy_settings);
//        Preference enableAnalytics = findPreference(Settings.KEY_ENABLE_ANALYTICS);

//        enableAnalytics.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        setTitle(R.string.settings_privacy);
        Preference patternProtection = findPreference(KEY_PATTERN_PROTECTION);
        patternProtection.setSummary(TextUtils.isEmpty(Settings.getSecurity()) ?
                R.string.settings_privacy_pattern_protection_not_set :
                R.string.settings_privacy_pattern_protection_set);
    }

//    @Override
//    public boolean onPreferenceChange(Preference preference, Object newValue) {
//        String key = preference.getKey();
//        if (Settings.KEY_ENABLE_ANALYTICS.equals(key)) {
//            if (newValue instanceof Boolean && (Boolean) newValue) {
//                Analytics.start(getActivity().getApplication());
//            }
//        }
//        return true;
//    }
}

/*
 * Copyright (C) 2019-2020 ion-OS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ion.ionizer.fragments;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import com.ion.ionizer.R;
import com.ion.ionizer.preferences.SystemSettingMasterSwitchPreference;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class Misc extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener, Indexable {

    public static final String TAG = "Misc";

    private static final String SCREEN_STATE_TOGGLES_ENABLE = "screen_state_toggles_enable_key";
    private static final String PREF_ALTERNATIVE_RECENTS_CATEGORY = "alternative_recents_category";
    private static final String PREF_SWIPE_UP_ENABLED = "swipe_up_enabled_warning";
    private static final String PREF_USE_SLIM_RECENTS = "use_slim_recents";

    private SystemSettingMasterSwitchPreference mEnableScreenStateToggles;
    private SystemSettingMasterSwitchPreference mEnableSlimRecent;
    private PreferenceCategory mAlternativeRecentsCategory;
    private Preference mSwipeUpEnabledWarning;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.ion_settings_misc);

        mEnableScreenStateToggles = (SystemSettingMasterSwitchPreference) findPreference(SCREEN_STATE_TOGGLES_ENABLE);
        mEnableScreenStateToggles.setOnPreferenceChangeListener(this);

        // Alternative recents en-/disabling
        mAlternativeRecentsCategory = (PreferenceCategory) findPreference(PREF_ALTERNATIVE_RECENTS_CATEGORY);
        mSwipeUpEnabledWarning = (Preference) findPreference(PREF_SWIPE_UP_ENABLED);
        mEnableSlimRecent = (SystemSettingMasterSwitchPreference) findPreference(PREF_USE_SLIM_RECENTS);
        mEnableSlimRecent.setOnPreferenceChangeListener(this);

        updatePreferences();
        updateDependencies();
    }

    private void updateDependencies() {
        // Warning for alternative recents when gesture navigation is enabled,
        // which directly controls quickstep (launcher) recents.
        final int navigationMode = getActivity().getResources()
                .getInteger(com.android.internal.R.integer.config_navBarInteractionMode);
        // config_navBarInteractionMode:
        // 0: 3 button mode (supports slim recents)
        // 1: 2 button mode (currently does not support alternative recents)
        // 2: gesture only (currently does not support alternative recents)
        if (navigationMode != 0) {
            mEnableSlimRecent.setEnabled(false);
        } else {
            mAlternativeRecentsCategory.removePreference(mSwipeUpEnabledWarning);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mEnableScreenStateToggles) {
            boolean value = (Boolean) newValue;
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.START_SCREEN_STATE_SERVICE, value ? 1 : 0, UserHandle.USER_CURRENT);
            Intent service = (new Intent())
                .setClassName("com.android.systemui", "com.android.systemui.ion.screenstate.ScreenStateService");
            if (value) {
                getActivity().stopService(service);
                getActivity().startService(service);
            } else {
                getActivity().stopService(service);
            }
            return true;
        } else if (preference == mEnableSlimRecent) {
            boolean value = (Boolean) newValue;
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.USE_SLIM_RECENTS, value ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        }
        return false;
    }

    private void updatePreferences() {
        int enabled = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.START_SCREEN_STATE_SERVICE, 0, UserHandle.USER_CURRENT);
        mEnableScreenStateToggles.setChecked(enabled != 0);

        int useSlimRecent = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.USE_SLIM_RECENTS, 0, UserHandle.USER_CURRENT);
        mEnableSlimRecent.setChecked(useSlimRecent != 0);
    }

    @Override
    public void onPause() {
        super.onPause();

        updatePreferences();
        updateDependencies();
    }

    @Override
    public void onResume() {
        super.onResume();

        updatePreferences();
        updateDependencies();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.ION_IONIZER;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();
                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.ion_settings_misc;
                    result.add(sir);
                    return result;
                }
                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    return result;
                }
    };
}

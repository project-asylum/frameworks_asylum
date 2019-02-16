/*
 * Copyright (C) 2016 The CyanogenMod project
 * Copyright (C) 2017 SlimRoms Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package asylum.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceDataStore;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.asylum.utils.AttributeHelper;

import com.asylum.preference.R;

public class SettingListPreference extends ListPreference {

    private int mSettingType;

    private PreferenceManager mPreferenceManager = PreferenceManager.get();

    private String mListDependency;
    private String[] mListDependencyValues;

    public SettingListPreference(Context context) {
        super(context);
        init(context, null);
    }

    public SettingListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SettingListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        AttributeHelper a = new AttributeHelper(context, attrs, R.styleable.SettingPreference);

        mSettingType = PreferenceManager.getSettingType(a);

        String list = a.getString(R.styleable.SettingPreference_listDependency);
        if (!TextUtils.isEmpty(list)) {
            String[] listParts = list.split(":");
            mListDependency = listParts[0];
            mListDependencyValues = listParts[1].split("\\|");
        }

        boolean hidePreference =
                a.getBoolean(R.styleable.SettingPreference_hidePreference, false);
        int hidePreferenceInt = a.getInt(R.styleable.SettingPreference_hidePreferenceInt, -1);
        int intDep = a.getInt(R.styleable.SettingPreference_hidePreferenceIntDependency, 0);
        if (hidePreference || hidePreferenceInt == intDep) {
            setVisible(false);
        }

        setPreferenceDataStore(new DataStore());
    }

    @Override
    public void onAttached() {
        super.onAttached();
        if (mListDependency != null) {
            mPreferenceManager.registerListDependent(
                    this, mListDependency, mListDependencyValues);
        }
    }

    @Override
    public void onDetached() {
        super.onDetached();
        if (mListDependency != null) {
            mPreferenceManager.unregisterListDependent(this, mListDependency);
        }
    }

    protected boolean putString(String key, String value) {
        if (TextUtils.equals(value, getString(key, null))) {
            return true;
        }
        PreferenceManager.putStringInSettings(getContext(),
                mSettingType, key, value);
        mPreferenceManager.updateDependents(this);
        return true;
    }

    protected String getString(String key, String defaultReturnValue) {
        return PreferenceManager.getStringFromSettings(getContext(), mSettingType, key,
                defaultReturnValue);
    }

    protected boolean isPersisted() {
        // Using getString instead of getInt so we can simply check for null
        // instead of catching an exception. (All values are stored as strings.)
        return PreferenceManager.settingExists(getContext(), mSettingType, getKey());
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        final String value;
        if (!restorePersistedValue || !isPersisted()) {
            if (defaultValue == null) {
                return;
            }
            value = (String) defaultValue;
            if (shouldPersist()) {
                persistString(value);
            }
        } else {
            // Note: the default is not used because to have got here
            // isPersisted() must be true.
            value = getString(getKey(), null /* not used */);
        }
        setValue(value);
    }

    private class DataStore extends PreferenceDataStore {
        @Override
        public void putString(String key, String value) {
            SettingListPreference.this.putString(key, value);
        }

        @Override
        public String getString(String key, String defaultValue) {
            return SettingListPreference.this.getString(key, defaultValue);
        }
    }
}

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
import android.support.v7.preference.PreferenceDataStore;
import android.support.v14.preference.SwitchPreference;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.asylum.utils.AttributeHelper;

import com.asylum.preference.R;

public class SettingSwitchPreference extends SwitchPreference {

    private int mSettingType;

    private PreferenceManager mPreferenceManager = PreferenceManager.get();
    private String mListDependency;
    private String[] mListDependencyValues;

    public SettingSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public SettingSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public SettingSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SettingSwitchPreference(Context context) {
        this(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        AttributeHelper a = new AttributeHelper(context, attrs,
            R.styleable.SettingPreference);

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

    protected boolean putBoolean(String key, boolean value) {
        if (shouldPersist()) {
            if (value == getPersistedBoolean(!value)) {
                return true;
            }
            PreferenceManager.putIntInSettings(getContext(),
                    mSettingType, key, value ? 1 : 0);
            return true;
        }
        return false;
    }

    protected boolean getBoolean(String key, boolean defaultReturnValue) {
        return PreferenceManager.getIntFromSettings(getContext(), mSettingType, key,
                defaultReturnValue ? 1 : 0) != 0;
    }

    private boolean isPersisted() {
        // Using getString instead of getInt so we can simply check for null
        // instead of catching an exception. (All values are stored as strings.)
        return PreferenceManager.settingExists(getContext(), mSettingType, getKey());
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        boolean value;
        if (!restorePersistedValue || !isPersisted()) {
            if (defaultValue == null) {
                return;
            }
            value = (boolean) defaultValue;
            if (shouldPersist()) {
                persistBoolean(value);
            }
        } else {
            value = getPersistedBoolean(false);
        }
        setChecked(value);
    }

    private class DataStore extends PreferenceDataStore {
        @Override
        public void putBoolean(String key, boolean value) {
            SettingSwitchPreference.this.putBoolean(key, value);
        }

        @Override
        public boolean getBoolean(String key, boolean defaultValue) {
            return SettingSwitchPreference.this.getBoolean(key, defaultValue);
        }
    }
}


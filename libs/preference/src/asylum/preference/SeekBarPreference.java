/*
 * Copyright (C) 2015-2017 SlimRoms
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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceDataStore;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.asylum.preference.R;
import com.asylum.utils.AttributeHelper;

/**
 * @hide
 */
public class SeekBarPreference extends Preference
        implements OnSeekBarChangeListener {

    public int mInterval = 5;

    private View mView = null;
    private TextView mMonitorBox;
    private SeekBar mBar;

    int mDefaultValue = 60;
    int mSetDefault = -1;
    int mMultiply = -1;
    int mMinimum = -1;
    boolean mDisableText = false;
    boolean mDisablePercentageValue = false;
    boolean mIsMilliSeconds = false;

    private int mSettingType;

    private PreferenceManager mPreferenceManager = PreferenceManager.get();
    private String mListDependency;
    private String[] mListDependencyValues;

    private OnPreferenceChangeListener mChanger;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setLayoutResource(R.layout.slider_preference);

        AttributeHelper a = new AttributeHelper(context, attrs, R.styleable.SeekBarPreference);

        mSetDefault = a.getInt(R.styleable.SeekBarPreference_defaultValue, mSetDefault);
        mDefaultValue = mSetDefault;
        mIsMilliSeconds = a.getBoolean(R.styleable.SeekBarPreference_useMilliSeconds,
                mIsMilliSeconds);
        mDisablePercentageValue = !a.getBoolean(R.styleable.SeekBarPreference_usePercentage,
                !mDisablePercentageValue);
        mDisableText = a.getBoolean(R.styleable.SeekBarPreference_disableText, mDisableText);
        mInterval = a.getInt(R.styleable.SeekBarPreference_interval, mInterval);
        mMinimum = a.getInt(R.styleable.SeekBarPreference_minValue, mMinimum);
        mMultiply = a.getInt(R.styleable.SeekBarPreference_multiplyValue, mMultiply);

        a = new AttributeHelper(context, attrs, R.styleable.SettingPreference);

        mSettingType = PreferenceManager.getSettingType(a);

        String list = a.getString(R.styleable.SettingPreference_listDependency);
        if (!TextUtils.isEmpty(list)) {
            String[] listParts = list.split(":");
            if (listParts.length == 2) {
                mListDependency = listParts[0];
                mListDependencyValues = listParts[1].split("\\|");
            }
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

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mView = holder.itemView;
        mMonitorBox = (TextView) holder.findViewById(R.id.monitor_box);
        mBar = (SeekBar) holder.findViewById(R.id.seek_bar);
        mBar.setOnSeekBarChangeListener(this);
        int progress = getPersistedInt(mSetDefault);
        if (mMinimum != -1) {
            progress -= mMinimum;
        }
        if (mMultiply > 0) {
            progress = progress / mMultiply;
        }
        mBar.setProgress(progress);
    }

    public void setInitValue(int progress) {
        mDefaultValue = progress;
        if (mBar != null) {
            mBar.setProgress(mDefaultValue);
        }
    }

    @Override
    public void setOnPreferenceChangeListener(
                OnPreferenceChangeListener onPreferenceChangeListener) {
        mChanger = onPreferenceChangeListener;
        super.setOnPreferenceChangeListener(onPreferenceChangeListener);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        progress = Math.round(((float) progress) / mInterval) * mInterval;
        seekBar.setProgress(progress);

        if (mMultiply > 0) {
            progress = progress * mMultiply;
        }

        if (mMinimum != -1) {
            progress += mMinimum;
        }

        if (progress == mSetDefault) {
            mMonitorBox.setText(R.string.default_string);
        } else if (!mDisableText) {
            if (mIsMilliSeconds) {
                mMonitorBox.setText(progress + " ms");
            } else if (!mDisablePercentageValue) {
                mMonitorBox.setText(progress + "%");
            } else {
                mMonitorBox.setText(Integer.toString(progress));
            }
        }
        if (mChanger != null) {
            mChanger.onPreferenceChange(this, Integer.toString(progress));
        }
        persistInt(progress);
    }

    public void disablePercentageValue(boolean disable) {
        mDisablePercentageValue = disable;
    }

    public void disableText(boolean disable) {
        mDisableText = disable;
    }

    public void setInterval(int inter) {
        mInterval = inter;
    }

    public void setDefault(int defaultVal) {
        mSetDefault = defaultVal;
    }

    public void multiplyValue(int val) {
        mMultiply = val;
    }

    public void minimumValue(int val) {
        mMinimum = val;
    }

    public void isMilliseconds(boolean millis) {
        mIsMilliSeconds = millis;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    protected boolean putInt(String key, int value) {
        if (value == getInt(key, -1)) {
            return true;
        }
        PreferenceManager.putIntInSettings(getContext(),
                mSettingType, key, value);
        return true;
    }

    protected int getInt(String key, int defaultReturnValue) {
        return PreferenceManager.getIntFromSettings(getContext(), mSettingType,
                key, defaultReturnValue);
    }

    protected boolean isPersisted() {
        return PreferenceManager.settingExists(getContext(), mSettingType, getKey());
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        int value;
        if (!restorePersistedValue || !isPersisted()) {
            if (defaultValue == null) {
                return;
            }
            value = (int) defaultValue;
            if (shouldPersist()) {
                persistInt(value);
            }
        } else {
            // Note: the default is not used because to have got here
            // isPersisted() must be true.
            value = getInt(getKey(), -1 /* not used */);
        }
        mBar.setProgress(value);
    }

    private class DataStore extends PreferenceDataStore {
        @Override
        public void putInt(String key, int value) {
            SeekBarPreference.this.putInt(key, value);
        }

        @Override
        public int getInt(String key, int defaultValue) {
            return SeekBarPreference.this.getInt(key, defaultValue);
        }
    }
}

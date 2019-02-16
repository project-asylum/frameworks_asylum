package asylum.preference.colorpicker;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceDataStore;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ImageView;

import asylum.preference.PreferenceManager;
import com.asylum.preference.R;
import com.asylum.utils.AttributeHelper;

@SuppressWarnings({"WeakerAccess", "unused"})
public class ColorPickerPreference extends Preference implements
        ColorPickerDialog.ColorSeletectedListener {

    private int mColor;
    private int mDefaultColor = Color.RED;
    private CircleDrawable mDrawable;
    private ColorPickerDialog mDialog;

    private int mSettingType;

    private String mListDependency;
    private String[] mListDependencyValues;
    private PreferenceManager mPreferenceManager = PreferenceManager.get();

    public ColorPickerPreference(Context context) {
        super(context);
        init(context, null);
    }

    @SuppressWarnings("RestrictedApi")
    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            AttributeHelper a =
                    new AttributeHelper(context, attrs, R.styleable.ColorPickerPreference);

            mDefaultColor = a.getInt(R.styleable.ColorPickerPreference_defaultColor,
                    mDefaultColor);
            mColor = mDefaultColor;

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
        }

        setWidgetLayoutResource(R.layout.color_widget);
    }

    public int getDefaultColor() {
        return mDefaultColor;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return mDefaultColor;
    }

    @Override
    public void colorPicked(int color) {
        mColor = color;
        if (mDrawable != null) {
            mDrawable.setColor(color);
        }
        persistInt(mColor);

        if (getOnPreferenceChangeListener() != null) {
            getOnPreferenceChangeListener().onPreferenceChange(this, color);
        }
    }

    @Override
    public void performClick() {
        mDialog = new ColorPickerDialog(getContext(), this);
        mDialog.show();
        mDialog.updateColor(mColor);
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

        float density = getContext().getResources().getDisplayMetrics().density;

        ImageView iv = (ImageView) holder.itemView.findViewById(R.id.color);

        mDrawable = new CircleDrawable(mColor);
        iv.setImageDrawable(mDrawable);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        if (mDialog == null || !mDialog.isShowing()) {
            return superState;
        }

        final SavedState state = new SavedState(superState);
        state.dialogBundle = mDialog.onSaveInstanceState();
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        mDialog = new ColorPickerDialog(getContext(), this);
        if (savedState.dialogBundle != null) {
            mDialog.onRestoreInstanceState(savedState.dialogBundle);
        }
        mDialog.show();
        mDialog.updateColor(mColor);
    }

    private boolean putInt(String key, int value) {
        if (value == getInt(key, -1)) {
            return true;
        }
        PreferenceManager.putIntInSettings(getContext(), mSettingType, key, value);
        return true;
    }

    private int getInt(String key, int defValue) {
        return PreferenceManager.getIntFromSettings(getContext(),
                mSettingType, key, defValue);
    }

    private boolean isPersisted() {
        // Using getString instead of getInt so we can simply check for null
        // instead of catching an exception. (All values are stored as strings.)
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
        colorPicked(value);
    }

    private class DataStore extends PreferenceDataStore {
        @Override
        public void putInt(String key, int value) {
            ColorPickerPreference.this.putInt(key, value);
        }

        @Override
        public int getInt(String key, int defaultValue) {
            return ColorPickerPreference.this.getInt(key, defaultValue);
        }
    }

    private static class SavedState extends BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
        Bundle dialogBundle;

        public SavedState(Parcel source) {
            super(source);
            dialogBundle = source.readBundle(getClass().getClassLoader());
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeBundle(dialogBundle);
        }
    }
}

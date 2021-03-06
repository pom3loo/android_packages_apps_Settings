package com.android.settings.cyanogenmod;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SeekBarDialogPreference;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class PieControl extends SettingsPreferenceFragment
                        implements Preference.OnPreferenceChangeListener {

    private static final int DEFAULT_POSITION = 1 << 1; // this equals Position.BOTTOM.FLAG

    private static final String PIE_CONTROL = "pie_control_checkbox";
    private static final String PIE_SENSITIVITY = "pie_control_sensitivity";
    private static final String PIE_SIZE = "pie_control_size";
    private static final String[] TRIGGER = {
        "pie_control_trigger_left",
        "pie_control_trigger_bottom",
        "pie_control_trigger_right",
        "pie_control_trigger_top"
    };

    private CheckBoxPreference mPieControl;
    private ListPreference mPieSensitivity;
    private SeekBarDialogPreference mPieSize;
    private CheckBoxPreference[] mTrigger = new CheckBoxPreference[4];

    private ContentObserver mPieTriggerObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updatePieTriggers();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pie_control);

        PreferenceScreen prefSet = getPreferenceScreen();
        mPieControl = (CheckBoxPreference) prefSet.findPreference(PIE_CONTROL);
        mPieControl.setOnPreferenceChangeListener(this);
        mPieSensitivity = (ListPreference) prefSet.findPreference(PIE_SENSITIVITY);
        mPieSensitivity.setOnPreferenceChangeListener(this);
        mPieSize = (SeekBarDialogPreference) prefSet.findPreference(PIE_SIZE);

        for (int i = 0; i < TRIGGER.length; i++) {
            mTrigger[i] = (CheckBoxPreference) prefSet.findPreference(TRIGGER[i]);
            mTrigger[i].setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mPieControl) {
            boolean newState = (Boolean) newValue;

            Settings.System.putInt(getContentResolver(),
                    Settings.System.PIE_CONTROLS, newState ? 1 : 0);
            propagatePieControl(newState);

        } else if (preference == mPieSensitivity) {
            String newState = (String) newValue;

            Settings.System.putString(getContentResolver(),
                    Settings.System.PIE_SENSITIVITY, newState);
            mPieSensitivity.setSummary(
                    mPieSensitivity.getEntries()[Integer.parseInt(newState) - 1]);
        } else {
            int triggerSlots = 0;
            for (int i = 0; i < mTrigger.length; i++) {
                boolean checked = preference == mTrigger[i]
                        ? (Boolean) newValue : mTrigger[i].isChecked();
                if (checked) {
                    triggerSlots |= 1 << i;
                }
            }
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PIE_POSITIONS, triggerSlots);
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        mPieControl.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.PIE_CONTROLS, 0) == 1);
        propagatePieControl(mPieControl.isChecked());

        int sensitivity = Settings.System.getInt(getContentResolver(),
                Settings.System.PIE_SENSITIVITY, 3);
        mPieSensitivity.setValue(Integer.toString(sensitivity));

        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.PIE_POSITIONS), true,
                mPieTriggerObserver);

        updatePieTriggers();
        updateSensitivity();
    }

    @Override
    public void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(mPieTriggerObserver);
    }

    private void propagatePieControl(boolean value) {
        for (int i = 0; i < mTrigger.length; i++) {
            mTrigger[i].setEnabled(value);
        }
        mPieSensitivity.setEnabled(value);
        mPieSize.setEnabled(value);
    }

    private void updatePieTriggers() {
        int triggerSlots = Settings.System.getInt(getContentResolver(),
                Settings.System.PIE_POSITIONS, DEFAULT_POSITION);

        for (int i = 0; i < mTrigger.length; i++) {
            if ((triggerSlots & (0x01 << i)) != 0) {
                mTrigger[i].setChecked(true);
            } else {
                mTrigger[i].setChecked(false);
            }
        }
    }

    private void updateSensitivity() {
        int triggerSlots = Settings.System.getInt(getContentResolver(),
                Settings.System.PIE_SENSITIVITY, 3);
        mPieSensitivity.setSummary(mPieSensitivity.getEntry());
    }

}

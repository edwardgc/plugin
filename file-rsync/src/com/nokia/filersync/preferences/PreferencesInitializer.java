package com.nokia.filersync.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.nokia.filersync.FileRsyncPlugin;

public class PreferencesInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = FileRsyncPlugin.getDefault().getPreferenceStore();
        store.setDefault(FileRsyncConstants.KEY_ASK_USER, true);
    }

}

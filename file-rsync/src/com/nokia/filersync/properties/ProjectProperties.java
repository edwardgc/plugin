package com.nokia.filersync.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.BackingStoreException;

import com.nokia.filersync.FileRsyncPlugin;
import com.nokia.filersync.builder.FileRsyncBuilder;
import com.nokia.filersync.command.FileMapping;

public class ProjectProperties implements IPreferenceChangeListener, INodeChangeListener {

    public static final String KEY_CYGWIN_HOME = "cygwinHome";
    public static final String KEY_TARGET_HOST = "targetHost";
    public static final String KEY_TARGET_USERNAME = "targetUsername";

    public static final String KEY_CLEAN_ON_CLEAN_BUILD = "cleanOnCleanBuild";

    public static final String KEY_EXCLUDE_TEAM_FILES = "exludeTeamFiles";

    public static final String KEY_SKIP_NEWER_FILES = "skipNewerFiles";

    public static final String KEY_PROJECT = "project";

    private IProject project;

    private IEclipsePreferences preferences;

    private boolean ignorePreferenceListeners;

    private boolean rebuildPathMap;

    private List<FileMapping> mappings;
    
    private boolean configOutOfSync = false;

    private static Map<IProject, ProjectProperties> projectsToProps = 
    		new HashMap<IProject, ProjectProperties>();

    private final List<IPreferenceChangeListener> prefListeners;

    public synchronized void addPreferenceChangeListener(FileRsyncBuilder listener) {
        if (prefListeners.contains(listener)) {
            return;
        }

        /*
         * it seems that I don't know about real builders lifecycle, but
         * sometimes there are more than one builder instance from same class
         * for the same project.
         */
        String projName = listener.getProject().getName();
        ArrayList<FileRsyncBuilder> oldBuilders = new ArrayList<FileRsyncBuilder>();
        for (int i = 0; i < prefListeners.size(); i++) {
            FileRsyncBuilder ib = (FileRsyncBuilder) prefListeners.get(i);
            if (projName.equals(ib.getProject().getName())) {
                ib.setDisabled(true);
                oldBuilders.add(ib);
            }
        }
        for (int i = 0; i < oldBuilders.size(); i++) {
            prefListeners.remove(oldBuilders.get(i));
        }
        prefListeners.add(listener);
    }

    public List<IPreferenceChangeListener> getProjectPreferenceChangeListeners() {
        return prefListeners;
    }

    /**
     * @param project
     */
    protected ProjectProperties(IProject project) {
        this.project = project;
        initPreferencesStore();
        prefListeners = new ArrayList<IPreferenceChangeListener>();
    }

    private void initPreferencesStore() {
        IScopeContext projectScope = new ProjectScope(project);
        preferences = projectScope.getNode(FileRsyncPlugin.PLUGIN_ID);
        buildPathMap(preferences);
        preferences.addPreferenceChangeListener(this);
        preferences.addNodeChangeListener(this);
    }

    public static ProjectProperties getInstance(IResource resource) {
        // sanity check
        List<IProject> projects = new ArrayList<IProject>(projectsToProps.keySet());
        for (int i = 0; i < projects.size(); i++) {
            IProject project = (IProject) projects.get(i);
            if (project == null || !project.isAccessible()) {
                ProjectProperties props = (ProjectProperties) projectsToProps
                        .get(project);
                props.prefListeners.clear();
                projectsToProps.remove(project);
            }
        }

        if (resource == null) {
            return null;
        }
        IProject project = resource.getProject();
        if (project == null) {
            return null;
        }
        ProjectProperties props = (ProjectProperties) projectsToProps.get(project);
        if (props != null) {
            return props;
        }
        props = new ProjectProperties(project);
        projectsToProps.put(project, props);
        return props;
    }

    public static void removeInstance(IProject project) {
        projectsToProps.remove(project);
    }

    private void buildPathMap(IEclipsePreferences prefs) {
        String[] keys;
        try {
            keys = prefs.keys();
        } catch (BackingStoreException e) {
            FileRsyncPlugin.log("Could not read preferences for project '"
                    + project.getName() + "'", e, IStatus.ERROR);
            return;
        }
        this.ignorePreferenceListeners = true;

        ArrayList<FileMapping> mappingList = new ArrayList<FileMapping>(keys.length);
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].startsWith(FileMapping.FULL_MAP_PREFIX)) {
                FileMapping mapping = new FileMapping(prefs.get(keys[i], null), project
                        .getLocation());

                if (mappingList.contains(mapping)) {
                    FileRsyncPlugin.log("Preferences contains duplicated " + "mapping: '"
                            + mapping + "' for project '" + project.getName() + "'",
                            null, IStatus.WARNING);

                    prefs.remove(keys[i]);
                } else {
                    mappingList.add(mapping);
                }
            }
        }
        mappings = new ArrayList<FileMapping>(mappingList.size());

        while (mappingList.size() > 0) {
            FileMapping fm1 = (FileMapping) mappingList.get(0);
            IPath sourcePath = fm1.getSourcePath();
            IPath destinationPath = fm1.getDestinationPath();
            boolean duplicate = false;
            for (int i = 1; i < mappingList.size(); i++) {
                FileMapping fm2 = (FileMapping) mappingList.get(i);
                if (sourcePath.equals(fm2.getSourcePath())) {
                    if ((destinationPath != null && destinationPath.equals(fm2
                            .getDestinationPath()))
                            || destinationPath == null
                            && fm2.getDestinationPath() == null) {
                        duplicate = true;
                        FileRsyncPlugin.log("Preferences contains duplicated "
                                + "mapping: '" + fm2 + "' for project '"
                                + project.getName() + "'", null, IStatus.WARNING);
                        break;
                    }
                }
            }
            if (!duplicate) {
            	mappings.add(fm1);
            }
            mappingList.remove(0);
        }

        this.ignorePreferenceListeners = false;
        this.rebuildPathMap = false;
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent event) {
        if (!isIgnorePreferenceListeners()) {
            buildPathMap(preferences);
        } else {
            rebuildPathMap = true;
        }
    }

    /**
     * @return Returns the preferences.
     */
    public IEclipsePreferences getPreferences(boolean forceSync) {
        if (forceSync) {
            refreshPreferences();
        }
        return preferences;
    }

    /**
     *
     */
    public void refreshPreferences() {
        this.ignorePreferenceListeners = true;
        try {
            preferences.clear();
            preferences.sync();
            buildPathMap(preferences);
        } catch (BackingStoreException e) {
            FileRsyncPlugin.log("Could not sync to preferences for project:" + project, e,
                    IStatus.ERROR);
        } catch (IllegalStateException e) {
            // settings deleted?
            initPreferencesStore();
        }
        this.ignorePreferenceListeners = false;
    }

    public void refreshPathMap() {
        this.ignorePreferenceListeners = true;
        buildPathMap(preferences);
        this.ignorePreferenceListeners = false;
    }

    /**
     * @param preferences
     *            The preferences to set.
     */
    protected void setPreferences(IEclipsePreferences preferences) {
        this.preferences = preferences;
    }

    /**
     * @return Returns the project.
     */
    public IProject getProject() {
        return project;
    }

    /**
     * @param project
     *            The project to set.
     */
    protected void setProject(IProject project) {
        this.project = project;
    }

    /**
     * @return Returns the mappings.
     */
    public List<FileMapping> getMappings() {
        return mappings;
    }

    /**
     * @param mappings
     *            The mappings to set.
     */
    public void setMappings(List<FileMapping> mappings) {
        this.mappings = mappings;
    }

    @Override
    public void added(NodeChangeEvent event) {
        if (!isIgnorePreferenceListeners()) {
            buildPathMap(preferences);
        } else {
            rebuildPathMap = true;
        }
    }

    @Override
    public void removed(NodeChangeEvent event) {
        try {
            // in case preferences are entirely deleted
            if(event.getParent() == preferences && !preferences.nodeExists("")) {
                // code below throws exception
                // preferences.removeNodeChangeListener(this);
                // preferences.removePreferenceChangeListener(this);
                return;
            }
        } catch (BackingStoreException e) {
            return;
        }
        if (!isIgnorePreferenceListeners()) {
            buildPathMap(preferences);
        } else {
            rebuildPathMap = true;
        }
    }

    /**
     * @return Returns the ignorePreferenceListeners.
     */
    public boolean isIgnorePreferenceListeners() {
        return ignorePreferenceListeners;
    }

    /**
     * @param ignorePreferenceListeners
     *            The ignorePreferenceListeners to set.
     */
    public void setIgnorePreferenceListeners(boolean ignorePreferenceListeners) {
        this.ignorePreferenceListeners = ignorePreferenceListeners;
        if (!ignorePreferenceListeners && rebuildPathMap) {
            buildPathMap(preferences);
            rebuildPathMap = false;
            for (int i = 0; i < prefListeners.size(); i++) {
                IPreferenceChangeListener listener = prefListeners.get(i);
                IEclipsePreferences.PreferenceChangeEvent event = new IEclipsePreferences.PreferenceChangeEvent(
                        preferences, KEY_PROJECT, project, project);
                listener.preferenceChange(event);
            }
        }
    }

    public FileRsyncConfig getSavedConfig() {
    	FileRsyncConfig config = new FileRsyncConfig();
    	config.setMappings(mappings);
    	config.setExludeTeamFiles(preferences.getBoolean(KEY_EXCLUDE_TEAM_FILES, false));
    	config.setSkipNewerFiles(preferences.getBoolean(KEY_SKIP_NEWER_FILES, false));
    	config.setCygwinHome(preferences.get(KEY_CYGWIN_HOME, ""));
    	config.setTargetHost(preferences.get(KEY_TARGET_HOST, ""));
    	config.setTargetUsername(preferences.get(KEY_TARGET_USERNAME, ""));
    	configOutOfSync = false;
    	return config;
    }
    
    /*
     * Identify configuration has been updated before builder initialized
     */
    public boolean isConfigOutOfSync() {
    	return configOutOfSync;
    }
    
    public boolean saveConfig(FileRsyncConfig config) {
    	setIgnorePreferenceListeners(true);
    	configOutOfSync = true;
    	
    	try {
            preferences.clear();
        } catch (BackingStoreException e) {
            FileRsyncPlugin.log("Cannot clear preferences for project '"
                    + project.getName() + "'", e, IStatus.ERROR);
        } catch (IllegalStateException e) {
            FileRsyncPlugin
            .log("FileRsync project preferences (for project '"
                    + project.getName() + "') error: " + e.getMessage(), e,
                    IStatus.ERROR);
            return false;
        }

        int i = 0;
        for (FileMapping mapping : config.getMappings()) {
            preferences.put(FileMapping.FULL_MAP_PREFIX + i++,
                    mapping.encode());
        }

        preferences.put(ProjectProperties.KEY_EXCLUDE_TEAM_FILES, "" + config.isExludeTeamFiles());
        preferences.put(ProjectProperties.KEY_SKIP_NEWER_FILES, "" + config.isSkipNewerFiles());
        
        preferences.put(ProjectProperties.KEY_CYGWIN_HOME, 
        		config.getCygwinHome() == null ? "" : config.getCygwinHome().toString());
        preferences.put(ProjectProperties.KEY_TARGET_HOST, config.getTargetHost());
        preferences.put(ProjectProperties.KEY_TARGET_USERNAME, config.getTargetUsername());
        
        setIgnorePreferenceListeners(false);
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            FileRsyncPlugin.log("Cannot store preferences for project '"
                    + project.getName() + "'", e, IStatus.ERROR);
        }
        return true;
    }
   
}

package com.nokia.filersync.builder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;

import com.nokia.filersync.FileRsyncPlugin;
import com.nokia.filersync.properties.FileRsyncConfig;
import com.nokia.filersync.properties.ProjectProperties;

public class FileRsyncBuilder extends IncrementalProjectBuilder

implements IPreferenceChangeListener {

    public static final String BUILDER_ID = FileRsyncPlugin.PLUGIN_ID + ".FileRsyncBuilder";

    public static final String SETTINGS_DIR = ".settings";

    public static final String SETTINGS_FILE = FileRsyncPlugin.PLUGIN_ID + ".prefs";

    private static final IPath SETTINGS_PATH = new Path(SETTINGS_DIR).append(SETTINGS_FILE);

    public static final int MAPPING_CHANGED_IN_GUI_BUILD = 999;

    public static final Integer MAPPING_CHANGED_IN_GUI = new Integer(MAPPING_CHANGED_IN_GUI_BUILD);
    
	private final int MAX_INCREMENTAL_RESOURCES = 5;

    private boolean wizardNotAvailable;

    private boolean disabled;

    private long modificationStamp;

    private FileRsyncConfig activeConfig;

    volatile boolean ignorePrefChange;

    private CygwinBuilder builder = new CygwinBuilder(new CommandRunner());
    
    public FileRsyncBuilder() {
        super();
    }

    protected IProject getProjectInternal() {
        IProject project = null;
        try {
            project = getProject();
        } catch (NullPointerException e) {
        	FileRsyncPlugin.log("Builder should not be created manually!", null, IStatus.ERROR);
        }
        return project;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public void build(int kind, IProgressMonitor monitor) {
        build(kind, new HashMap<String,String>(), monitor);
    }

    @Override
    protected void clean(IProgressMonitor monitor) throws CoreException {
        build(CLEAN_BUILD, monitor);
    }

    @Override
    protected IProject[] build(int kind, Map<String,String> args, IProgressMonitor monitor) {
        if (isDisabled()) {
            return null;
        }
        if (args == null) {
            args = new HashMap<String,String>();
        }
        ProjectProperties props = ProjectProperties.getInstance(getProjectInternal());

        SyncWizard wizard = new SyncWizard();
        IProject[] result = null;
        try {
            switch (kind) {
            case AUTO_BUILD:		//Fall-Through
            case INCREMENTAL_BUILD:
                result = buildIncremental(args, props, wizard, monitor);
                break;
            case FULL_BUILD:
                result = buildFull(args, props, wizard, monitor);
                break;
            case CLEAN_BUILD:
                // Do nothing
                break;
            case MAPPING_CHANGED_IN_GUI_BUILD:
                args.put(MAPPING_CHANGED_IN_GUI.toString(), MAPPING_CHANGED_IN_GUI.toString());
                result = buildFull(args, props, wizard, monitor);
                break;
            default:
                result = buildFull(args, props, wizard, monitor);
                break;
            }
            wizardNotAvailable = false;
        } catch (IllegalArgumentException e) {
            if (!wizardNotAvailable) {
                FileRsyncPlugin.log("Couldn't run file sync for project '"
                        + getProjectInternal().getName() + "': " + e.getMessage(), e,
                        IStatus.WARNING);
                wizardNotAvailable = true;
            }
            return null;
        } catch (IllegalStateException e) {
            if (!wizardNotAvailable) {
                FileRsyncPlugin.log("Couldn't run file sync for project '"
                        + getProjectInternal().getName() + "': " + e.getMessage(), e,
                        IStatus.WARNING);
                wizardNotAvailable = true;
            }
            return null;
        }

        return result;
    }

    private IProject[] buildFull(Map<String,String> args, ProjectProperties props, SyncWizard wizard,
            IProgressMonitor monitor) {
        IProject currentProject = getProjectInternal();
        if (currentProject != null) {
            fullProjectBuild(args, currentProject, props, wizard, monitor);
        }
        return null;
    }

    /**
     * Incremental build
     * @param args build parameters
     * @param wizard
     * @param monitor progress indicator
     * @return IProject[] related projects list
     */
	private IProject[] buildIncremental(final Map<String, String> args, final ProjectProperties props,
			final SyncWizard wizard, final IProgressMonitor monitor) {
		IProject result[] = null;

		final IProject currentProject = getProjectInternal();
		if (currentProject != null) {
			final IResourceDelta resourceDelta = getDelta(currentProject);
			if (resourceDelta == null) {
				return buildFull(args, props, wizard, monitor);
			}
			if (resourceDelta.getAffectedChildren().length == 0) {
				FileRsyncPlugin.log("nothing happens because delta is empty", null, IStatus.INFO);
			} else {
				FSPropsChecker propsChecker = new FSPropsChecker(monitor, props);
				try {
					resourceDelta.accept(propsChecker, false);
				} catch (CoreException e) {
					FileRsyncPlugin.log("Errors during sync of the resource delta:" + resourceDelta + " for project '"
							+ currentProject.getName() + "'", e, IStatus.ERROR);
				}
				wizard.setProjectProps(props);

				OutputStream os = null;
				try {
					final FSDeltaVisitor visitor = new FSDeltaVisitor(wizard);
					resourceDelta.accept(visitor, getVisitorFlags());
					List<IResource> resources = visitor.getAffectedResources();
					os = FileRsyncPlugin.getConsoleStream();

					if (propsChecker.propsChanged || resources.size() > MAX_INCREMENTAL_RESOURCES) {
						monitor.beginTask("Full file sync", 1);
						builder.buildFull(activeConfig, os);
					} else {
						monitor.beginTask("Incremental file sync", 1);
						builder.buildIncremental(resources, activeConfig, os);
					}
				} catch (CoreException e) {
					FileRsyncPlugin.log("Errors during sync of the resource delta:" + resourceDelta + " for project '"
							+ currentProject + "'", e, IStatus.ERROR);
				} catch (IOException e) {
					FileRsyncPlugin.log(e.getMessage(), e, IStatus.ERROR);
				} finally {
					wizard.cleanUp(monitor);
					monitor.done();
					if (os != null) {
						try {
							os.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
        return result;
    }

    /**
     * Process all files in the project
     * @param project the project
     * @param monitor a progress indicator
     * @param wizard
     */
    protected void fullProjectBuild(Map<String,String> args, final IProject project,
            ProjectProperties props, SyncWizard wizard, final IProgressMonitor monitor) {

        if (!args.containsKey(MAPPING_CHANGED_IN_GUI.toString()) && wizard.getProjectProps() == null) {
            FSPropsChecker propsChecker = new FSPropsChecker(monitor, props);
            try {
                project.accept(propsChecker, IResource.DEPTH_INFINITE, false);
            } catch (CoreException e) {
                FileRsyncPlugin.log("Error during visiting project: " + project.getName(),
                        e, IStatus.ERROR);
            }
        }
        wizard.setProjectProps(props);

		OutputStream os = null;
		try {
			os = FileRsyncPlugin.getConsoleStream();
			builder.buildFull(activeConfig, os);
		} catch (IOException e) {
			FileRsyncPlugin.log(e.getMessage(), e, IStatus.ERROR);
		} finally {
			wizard.cleanUp(monitor);
			monitor.done();
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
    }

    @Override
    protected void startupOnInitialize() {
        super.startupOnInitialize();
        checkSettingsTimestamp(getProject().getFile(SETTINGS_PATH));
        ProjectProperties props = ProjectProperties.getInstance(getProjectInternal());
        props.addPreferenceChangeListener(this);
        activeConfig = props.getSavedConfig();
    }

    private int getVisitorFlags() {
        return activeConfig.isExludeTeamFiles()? IResource.NONE : IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS;
    }

    protected boolean checkSettingsTimestamp(IResource settingsFile) {
        long oldStamp = modificationStamp;
        long localTimeStamp = settingsFile.getLocation().toFile().lastModified();
        boolean changed = oldStamp != 0 && oldStamp != localTimeStamp;
        if (oldStamp == 0 || changed) {
            modificationStamp = localTimeStamp;
        }
        return changed;
    }

    private class FSDeltaVisitor implements IResourceDeltaVisitor {
        private SyncWizard wizard;
        private List<IResource> affectedResources = new ArrayList<IResource>(); 

        public FSDeltaVisitor(SyncWizard wizard) {
            this.wizard = wizard;
        }

        @Override
        public boolean visit(IResourceDelta delta) {
            if (delta == null || affectedResources.size() > MAX_INCREMENTAL_RESOURCES) {
                return false;
            }
            if(isResourceIncluded(affectedResources, delta.getResource()))
            	return true;
            if (delta.getResource().getType() == IResource.PROJECT) {
                return true;
            }
            boolean shouldVisit = wizard.checkResource(delta);
            if (!shouldVisit) {
                // return true, if there children with mappings to visit
            	if(delta.getAffectedChildren().length>0) return true; //continue visit children
                return wizard.hasMappedChildren(delta);
            }
            IResource rt = wizard.sync(delta);
            if(rt != null)
            	affectedResources.add(rt);
            return true;
        }

		public List<IResource> getAffectedResources() {
			return affectedResources;
		}
    }

 
    private class FSPropsChecker implements IResourceVisitor, IResourceDeltaVisitor {
        private final IProgressMonitor monitor;

        private final ProjectProperties props;

        boolean propsChanged;

        public FSPropsChecker(IProgressMonitor monitor, ProjectProperties props) {
            this.monitor = monitor;
            this.props = props;
        }

        @Override
        public boolean visit(IResource resource) {
            if (monitor.isCanceled()) {
                throw new OperationCanceledException();
            }
            if (resource.getType() == IResource.PROJECT) {
                return true;
            }

            boolean continueVisit = isSettingsDir(resource);
            if (continueVisit && isSettingsFile(resource)
                    && checkSettingsTimestamp(resource)) {
                // mappings changed
                ignorePrefChange = true;
                props.refreshPreferences();
                FileRsyncConfig savedConfig = props.getSavedConfig();
                ignorePrefChange = false;
                if (!activeConfig.equals(savedConfig)) {
                    propsChanged = true;
                    continueVisit = false;
                    activeConfig = savedConfig;
                }
            } else {
                continueVisit = true;
            }
            // visit children only from settings directory
            return continueVisit;
        }

        /**
         * @param file
         * @return true if this resource is my own preference file
         */
        private boolean isSettingsFile(IResource file) {
            // the directory is already ok, so we check only for the file name
            IPath relativePath = file.getProjectRelativePath();
            if (relativePath == null) {
                return false;
            }
            return SETTINGS_FILE.equals(relativePath.lastSegment());
        }

        private boolean isSettingsDir(IResource dir) {
            IPath relativePath = dir.getProjectRelativePath();
            if (relativePath == null || relativePath.segmentCount() > 2) {
                return false;
            }
            return SETTINGS_DIR.equals(relativePath.segment(0));
        }

        @Override
        public boolean visit(IResourceDelta delta) {
            return visit(delta.getResource());
        }
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent event) {
        if (ignorePrefChange) {
            return;
        }
        String key = event.getKey();
        if (!ProjectProperties.KEY_PROJECT.equals(key)) {
            return;
        }
        Job[] jobs = Job.getJobManager().find(getClass());
        if (jobs.length == 0) {
            final Job myJob = new Job("Mapping is changed => full project sync") {
                @Override
                public boolean belongsTo(Object family) {
                    return family == FileRsyncBuilder.class;
                }

                @Override
                public IStatus run(IProgressMonitor monitor) {
                	ProjectProperties props = ProjectProperties.getInstance(getProjectInternal());
                    activeConfig = props.getSavedConfig(); 
                    build(MAPPING_CHANGED_IN_GUI_BUILD, monitor);
                    return Status.OK_STATUS;//new JobStatus(IStatus.INFO, 0, this, "", null);
                }
            };
            myJob.setUser(false);
            myJob.schedule();
        }
    }
    
    boolean isResourceIncluded(List<IResource> resources, IResource newResource) {
    	for(IResource res : resources) {
    		if(res.getProjectRelativePath().equals(newResource.getProjectRelativePath())) {
    			return true;
    		}
    	}
    	return false;
    }

}

package com.nokia.filersync.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;

import com.nokia.filersync.FileRsyncPlugin;
import com.nokia.filersync.builder.FileRsyncBuilder;

public class ProjectHelper {
    /**
     * Will be run after workbench is started and w.window is opened
     */
    public ProjectHelper() {
        super();
    }

    public static boolean hasBuilder(IProject project){
        if(!project.isAccessible()){
            return false;
        }
        IProjectDescription desc;
        try {
            desc = project.getDescription();
        } catch (CoreException e) {
            FileRsyncPlugin.log("hasBuilder(): failed for project '"
                    + project.getName() + "'", e, IStatus.INFO);
            return false;
        }
        ICommand[] commands = desc.getBuildSpec();
        boolean found = false;

        for (int i = 0; i < commands.length; i++) {
            String builderName = commands[i].getBuilderName();
            if (FileRsyncBuilder.BUILDER_ID.equals(builderName)) {
                found = true;
                break;
            }
        }
        return found;
    }

    public static boolean isBuilderDisabled(IProject project){
        if(project == null || !project.isAccessible()){
            return false;
        }
        IProjectDescription desc;
        try {
            desc = project.getDescription();
        } catch (CoreException e) {
            FileRsyncPlugin.log("addBuilder(): failed for project '"
                    + project.getName() + "'", e, IStatus.WARNING);
            return false;
        }
        return isBuilderDisabled(project, desc);
    }

    private static boolean isBuilderDisabled(IProject project, IProjectDescription desc){
        ICommand[] commands = desc.getBuildSpec();
        boolean disabled = false;

        for (int i = 0; i < commands.length; i++) {
            String builderName = commands[i].getBuilderName();
            if (FileRsyncBuilder.BUILDER_ID.equals(builderName)) {
                disabled = false;
                break;
            }
            // see ExternalToolBuilder.ID
            if(isBuilderDeactivated(commands[i])) {
                disabled = true;
                break;
            }
        }
        return disabled;
    }

    private static boolean isBuilderDeactivated(ICommand command){
        // see ExternalToolBuilder.ID
        if(command.getBuilderName().equals("org.eclipse.ui.externaltools.ExternalToolBuilder")) {
            /*
             * check for deactivated builder
             */
            Map<String, String> arguments = command.getArguments();
            String externalLaunch = arguments
                    .get("LaunchConfigHandle"); // see BuilderUtils.LAUNCH_CONFIG_HANDLE);
            if(externalLaunch != null
                    && externalLaunch.indexOf(FileRsyncBuilder.BUILDER_ID) >=0){
                return true;
            }
        }
        return false;
    }

    public static boolean addBuilder(IProject project) {
        if(!project.isAccessible()){
            return false;
        }
        if(hasBuilder(project)){
            return true;
        }

        IProjectDescription desc;
        try {
            desc = project.getDescription();
        } catch (CoreException e) {
            FileRsyncPlugin.log("addBuilder(): failed for project '"
                    + project.getName() + "'", e, IStatus.WARNING);
            return false;
        }

        if(isBuilderDisabled(project, desc)){
            removeDisabledBuilder(desc);
        }

        ICommand command = desc.newCommand();
        command.setBuilderName(FileRsyncBuilder.BUILDER_ID);

        ICommand[] commands = desc.getBuildSpec();
        ICommand[] newCommands = new ICommand[commands.length + 1];

        // Add it after other builders.
        System.arraycopy(commands, 0, newCommands, 0, commands.length);
        newCommands[newCommands.length-1] = command;
        desc.setBuildSpec(newCommands);
        try {
            project.setDescription(desc, IResource.FORCE
                    | IResource.KEEP_HISTORY, null);

        } catch (CoreException e) {
            FileRsyncPlugin.log(
                    "addBuilder(): failed to change .project file for project '"
                            + project.getName() + "'", e, IStatus.WARNING);
            return false;
        }
        return true;
    }

    private static void removeDisabledBuilder(IProjectDescription desc) {
        ICommand[] commands = desc.getBuildSpec();

        List<ICommand> list = new ArrayList<ICommand>(commands.length);

        for (int i = 0; i < commands.length; i++) {
            if (!isBuilderDeactivated(commands[i])) {
                list.add(commands[i]);
            }
        }

        ICommand[] newCommands = (ICommand[]) list.toArray(new ICommand[list.size()]);

        desc.setBuildSpec(newCommands);
    }

    public static boolean disableBuilder(IProject project) {
        if(!project.isAccessible()){
            return false;
        }

        IProjectDescription desc;
        try {
            desc = project.getDescription();
        } catch (CoreException e) {
            FileRsyncPlugin.log("hasBuilder(): failed for project '"
                    + project.getName() + "'", e, IStatus.INFO);
            return false;
        }
        if(isBuilderDisabled(project, desc)){
            removeDisabledBuilder(desc);
        }

        ICommand[] commands = desc.getBuildSpec();

        List<ICommand> list = new ArrayList<ICommand>(commands.length);

        for (int i = 0; i < commands.length; i++) {
            String builderName = commands[i].getBuilderName();
            if (!FileRsyncBuilder.BUILDER_ID.equals(builderName)) {
                list.add(commands[i]);
            }
        }

        ICommand[] newCommands = (ICommand[]) list.toArray(new ICommand[list.size()]);

        desc.setBuildSpec(newCommands);
        try {
            project.setDescription(desc, IResource.FORCE
                    | IResource.KEEP_HISTORY, null);
        } catch (CoreException e) {
            FileRsyncPlugin.log(
                    "addBuilder(): failed to change .project file for project '"
                            + project.getName() + "'", e, IStatus.WARNING);
            return false;
        }

        ProjectProperties properties = ProjectProperties.getInstance(project);
        if(properties != null){
            List<IPreferenceChangeListener> listeners = properties.getProjectPreferenceChangeListeners();
            for (int i = 0; i < listeners.size(); i++) {
                FileRsyncBuilder b = (FileRsyncBuilder) listeners.get(i);
                b.setDisabled(true);
            }
        }
        ProjectProperties.removeInstance(project);
        return true;

    }
}
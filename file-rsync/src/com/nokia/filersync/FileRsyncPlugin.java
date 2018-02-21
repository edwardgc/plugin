package com.nokia.filersync;

import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.internal.win32.GESTURECONFIG;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.nokia.filersync.preferences.FileRsyncConstants;

public class FileRsyncPlugin extends AbstractUIPlugin {

    private static FileRsyncPlugin plugin;

    public static final String PLUGIN_ID = "com.nokia.filersync";
    
    private MessageConsole console;
    IConsoleManager manager;

    public FileRsyncPlugin() {
        super();
        if(plugin != null){
            throw new IllegalStateException("FileRsync plugin is singleton!");
        }
        plugin = this;
        
        console = new MessageConsole("File Rsync", null);
        manager = (IConsoleManager) ConsolePlugin.getDefault().getConsoleManager();
        manager.addConsoles(new IConsole[]{console});
    }

    /**
     * Returns the shared instance.
     */
    public static FileRsyncPlugin getDefault() {
        return plugin;
    }

    public static void error(String message, Throwable error) {
        Shell shell = FileRsyncPlugin.getShell();
        if(message == null){
            message = "";
        }
        if (error != null) {
            message = message + " " + error.getMessage();
        }
        IPreferenceStore store = getDefault().getPreferenceStore();
        if (store.getBoolean(FileRsyncConstants.KEY_ASK_USER)) {
            MessageDialog.openError(shell, "FileRsync error", message);
        }
        log(message, error, IStatus.ERROR);
    }

    /**
     * @param statusID
     *            one of IStatus. constants like IStatus.ERROR etc
     * @param error
     */
    public static void log(String messageID, Throwable error, int statusID) {
        if (messageID == null) {
            messageID = error.getMessage();
            if (messageID == null) {
                messageID = error.toString();
            }
        }
        Status status = new Status(statusID, PLUGIN_ID, 0, messageID, error);
        getDefault().getLog().log(status);
        if(getDefault().isDebugging()){
            System.out.println(status);
            MessageConsoleStream os = getConsoleStream();
           	try {
           		getConsoleStream().println(status.toString());
           		os.flush();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
        }
    }
    
    public static MessageConsoleStream getConsoleStream() {
    	FileRsyncPlugin plugin = getDefault();
    	plugin.manager.showConsoleView(plugin.console);  
        return plugin.console.newMessageStream();
    }

    public static Shell getShell() {
        return getDefault().getWorkbench().getActiveWorkbenchWindow().getShell();
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in
     * relative path.
     *
     * @param path
     *            the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        if (path == null) {
            return null;
        }
        ImageRegistry imageRegistry = getDefault().getImageRegistry();
        ImageDescriptor imageDescriptor = imageRegistry.getDescriptor(path);
        if (imageDescriptor == null) {
            imageDescriptor = imageDescriptorFromPlugin(PLUGIN_ID, path);
            imageRegistry.put(path, imageDescriptor);
        }
        return imageDescriptor;
    }

    public static Image getImage(String path) {
        if (path == null) {
            return null;
        }
        ImageDescriptor imageDescriptor = getImageDescriptor(path);
        if (imageDescriptor != null) {
            return getDefault().getImageRegistry().get(path);
        }
        return null;
    }
}

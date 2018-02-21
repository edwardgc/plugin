package com.nokia.filersync.dialogs;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;

import com.nokia.filersync.FileRsyncPlugin;

/**
 * Can be an error, warning, info or OK. For error, info and warning states,
 * a message describes the problem.
 */
public class StatusInfo implements IStatus {

    public static final IStatus OK_STATUS= new StatusInfo();

    private String fStatusMessage;
    private int fSeverity;

    /**
     * Creates a status set to OK (no message)
     */
    public StatusInfo() {
        this(OK, null);
    }

    /**
     * Creates a status .
     * @param severity The status severity: ERROR, WARNING, INFO and OK.
     * @param message The message of the status. Applies only for ERROR,
     * WARNING and INFO.
     */
    public StatusInfo(int severity, String message) {
        fStatusMessage= message;
        fSeverity= severity;
    }

    /**
     *  Returns if the status' severity is OK.
     */
    @Override
    public boolean isOK() {
        return fSeverity == IStatus.OK;
    }

    /**
     *  Returns if the status' severity is WARNING.
     */
    public boolean isWarning() {
        return fSeverity == IStatus.WARNING;
    }

    /**
     *  Returns if the status' severity is INFO.
     */
    public boolean isInfo() {
        return fSeverity == IStatus.INFO;
    }

    /**
     *  Returns if the status' severity is ERROR.
     */
    public boolean isError() {
        return fSeverity == IStatus.ERROR;
    }

    /**
     * @see IStatus#getMessage
     */
    @Override
    public String getMessage() {
        return fStatusMessage;
    }

    public void setError(String errorMessage) {
        Assert.isNotNull(errorMessage);
        fStatusMessage= errorMessage;
        fSeverity= IStatus.ERROR;
    }

    public void setWarning(String warningMessage) {
        Assert.isNotNull(warningMessage);
        fStatusMessage= warningMessage;
        fSeverity= IStatus.WARNING;
    }

    public void setInfo(String infoMessage) {
        Assert.isNotNull(infoMessage);
        fStatusMessage= infoMessage;
        fSeverity= IStatus.INFO;
    }

    public void setOK() {
        fStatusMessage= null;
        fSeverity= IStatus.OK;
    }

    @Override
    public boolean matches(int severityMask) {
        return (fSeverity & severityMask) != 0;
    }

    @Override
    public boolean isMultiStatus() {
        return false;
    }

    @Override
    public int getSeverity() {
        return fSeverity;
    }

    @Override
    public String getPlugin() {
        return FileRsyncPlugin.PLUGIN_ID;
    }

    @Override
    public Throwable getException() {
        return null;
    }

    @Override
    public int getCode() {
        return fSeverity;
    }

    @Override
    public IStatus[] getChildren() {
        return new IStatus[0];
    }

}

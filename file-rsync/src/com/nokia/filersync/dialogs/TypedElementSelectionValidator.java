package com.nokia.filersync.dialogs;

import java.util.Collection;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

/**
 * Implementation of a <code>ISelectionValidator</code> to validate the
 * type of an element.
 * Empty selections are not accepted.
 */
public class TypedElementSelectionValidator implements ISelectionStatusValidator {

    private final IStatus fgErrorStatus= new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
    private final IStatus fgOKStatus= new StatusInfo();

    private final Class<?>[] fAcceptedTypes;
    private final boolean fAllowMultipleSelection;
    private final Collection<Object> fRejectedElements;

    /**
     * @param acceptedTypes The types accepted by the validator.
     * @param allowMultipleSelection If set to <code>true</code>, the validator
     * allows multiple selection.
     */
    public TypedElementSelectionValidator(Class<?>[] acceptedTypes, boolean allowMultipleSelection) {
        this(acceptedTypes, allowMultipleSelection, null);
    }

    /**
     * @param acceptedTypes The types accepted by the validator
     * @param allowMultipleSelection If set to <code>true</code>, the validator
     * allows multiple selection.
     * @param rejectedElements A list of elements that are not accepted
     */
    public TypedElementSelectionValidator(Class<?>[] acceptedTypes, boolean allowMultipleSelection, Collection<Object> rejectedElements) {
        Assert.isNotNull(acceptedTypes);
        fAcceptedTypes= acceptedTypes;
        fAllowMultipleSelection= allowMultipleSelection;
        fRejectedElements= rejectedElements;
    }

    @Override
    public IStatus validate(Object[] elements) {
        if (isValid(elements)) {
            return fgOKStatus;
        }
        return fgErrorStatus;
    }

    private boolean isOfAcceptedType(Object o) {
        for (int i= 0; i < fAcceptedTypes.length; i++) {
            if (fAcceptedTypes[i].isInstance(o)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRejectedElement(Object elem) {
        return (fRejectedElements != null) && fRejectedElements.contains(elem);
    }

    protected boolean isSelectedValid(Object elem) {
        return true;
    }

    private boolean isValid(Object[] selection) {
        if (selection.length == 0) {
            return false;
        }

        if (!fAllowMultipleSelection && selection.length != 1) {
            return false;
        }

        for (int i= 0; i < selection.length; i++) {
            Object o= selection[i];
            if (!isOfAcceptedType(o) || isRejectedElement(o) || !isSelectedValid(o)) {
                return false;
            }
        }
        return true;
    }
}

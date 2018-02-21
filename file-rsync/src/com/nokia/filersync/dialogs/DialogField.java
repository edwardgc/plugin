package com.nokia.filersync.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

/**
 * Base class of all dialog fields.
 * Dialog fields manage controls together with the model
 * from the creation time of the widgets.
 * - support for automated layout.
 * - enable / disable, set focus a concept of the base class.
 *
 * DialogField have a label.
 */
public class DialogField {

    private Label fLabel = null;
    protected String fLabelText = ""; //$NON-NLS-1$
    private boolean fEnabled = true;
    
    private IDialogFieldListener fDialogFieldListener;

    public DialogField() {
    }

    /**
     * Sets the label of the dialog field.
     */
    public void setLabelText(String labeltext) {
        fLabelText= labeltext;
        if (isOkToUse(fLabel)) {
            fLabel.setText(labeltext);
        }
    }

    // ------ change listener
    
    public final void setDialogFieldListener(IDialogFieldListener listener) {
        fDialogFieldListener= listener;
    }

    public void dialogFieldChanged() {
        if (fDialogFieldListener != null) {
            fDialogFieldListener.dialogFieldChanged(this);
        }
    }

    // ------- focus management
    
    /**
     * Tries to set the focus to the dialog field.
     * Returns <code>true</code> if the dialog field can take focus.
     *  To be override by dialog field implementors.
     */
    public boolean setFocus() {
        return false;
    }

    /**
     * Posts <code>setFocus</code> to the display event queue.
     */
    public void postSetFocusOnDialogField(Display display) {
        if (display != null) {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					setFocus();
				}
			});
        }
    }

    // ------- layout helpers

    /**
     * Creates all controls of the dialog field and fills it to a composite.
     * The composite is assumed to have <code>MGridLayout</code> as
     * layout.
     * The dialog field will adjust its controls' spans to the number of columns given.
     *  To be override by dialog field implementors.
     */
    public Control[] doFillIntoGrid(Composite parent, int nColumns) {
        Label label= getLabelControl(parent);
        label.setLayoutData(gridDataForLabel(nColumns));

        return new Control[] { label };
    }

    /**
     * Returns the number of columns of the dialog field.
     *  To be override by dialog field implementors.
     */
    public int getNumberOfControls() {
        return 1;
    }

    protected static GridData gridDataForLabel(int span) {
        GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan= span;
        return gd;
    }

    // ------- ui creation

    /**
     * Creates or returns the created label widget.
     * @param parent The parent composite or <code>null</code> if the widget has
     * already been created.
     */
    public Label getLabelControl(Composite parent) {
        if (fLabel == null) {
            fLabel= new Label(parent, SWT.LEFT | SWT.WRAP);
            fLabel.setFont(parent.getFont());
            fLabel.setEnabled(fEnabled);
            if (fLabelText != null && !"".equals(fLabelText)) { //$NON-NLS-1$
                fLabel.setText(fLabelText);
            } else {
                // to avoid a 16 pixel wide empty label - revisit
                fLabel.setText("."); 
                fLabel.setVisible(false);
            }
        }
        return fLabel;
    }

    /**
     * Tests is the control is not <code>null</code> and not disposed.
     */
    protected final boolean isOkToUse(Control control) {
        return (control != null) && (Display.getCurrent() != null) && !control.isDisposed();
    }

    // --------- enable / disable management

    public final void setEnabled(boolean enabled) {
        if (enabled != fEnabled) {
            fEnabled= enabled;
            updateEnableState();
        }
    }

    protected void updateEnableState() {
        if (fLabel != null && !fLabel.isDisposed()) {
            fLabel.setEnabled(fEnabled);
        }
    }

    public void refresh() {
        updateEnableState();
    }

    public final boolean isEnabled() {
        return fEnabled;
    }

}

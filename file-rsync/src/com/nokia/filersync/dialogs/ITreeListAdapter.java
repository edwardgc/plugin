package com.nokia.filersync.dialogs;

import org.eclipse.swt.events.KeyEvent;

public interface ITreeListAdapter<T> {

    void customButtonPressed(TreeListDialogField<T> field, int index);

    void selectionChanged(TreeListDialogField<T> field);

    void doubleClicked(TreeListDialogField<T> field);

    void keyPressed(TreeListDialogField<T> field, KeyEvent event);

    Object[] getChildren(TreeListDialogField<T> field, Object element);

    Object getParent(TreeListDialogField<T> field, Object element);

    boolean hasChildren(TreeListDialogField<T> field, Object element);

}

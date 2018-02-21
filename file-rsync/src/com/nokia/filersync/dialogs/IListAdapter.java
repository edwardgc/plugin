package com.nokia.filersync.dialogs;

public interface IListAdapter {

    void customButtonPressed(ListDialogField field, int index);

    void selectionChanged(ListDialogField field);

    void doubleClicked(ListDialogField field);

}

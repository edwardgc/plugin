package com.nokia.filersync.properties;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

import com.nokia.filersync.FileRsyncPlugin;
import com.nokia.filersync.dialogs.DialogField;
import com.nokia.filersync.dialogs.IDialogFieldListener;
import com.nokia.filersync.dialogs.IListAdapter;
import com.nokia.filersync.dialogs.LayoutUtil;
import com.nokia.filersync.dialogs.ListDialogField;
import com.nokia.filersync.dialogs.TypedElementSelectionValidator;
import com.nokia.filersync.dialogs.TypedViewerFilter;

public class InclusionExclusionDialog extends StatusDialog {

    private final ListDialogField inclPatternList;

    private final ListDialogField exclPatternList;

    private final PathListElement currElement;

    private IContainer currSourceFolder;

    private static final int IDX_ADD = 0;

    private static final int IDX_ADD_MULTIPLE = 1;

    private static final int IDX_EDIT = 2;

    private static final int IDX_REMOVE = 4;

    private static class ExclusionInclusionLabelProvider extends LabelProvider {

        private final Image elementImage;

        public ExclusionInclusionLabelProvider(String key, ImageDescriptor descriptor) {
            ImageRegistry registry = FileRsyncPlugin.getDefault().getImageRegistry();
            if (registry.getDescriptor(key) == null) {
                registry.put(key, descriptor);
            }
            elementImage = registry.get(key);
        }

        @Override
        public Image getImage(Object element) {
            return elementImage;
        }

        @Override
        public String getText(Object element) {
            return (String) element;
        }

    }

    public InclusionExclusionDialog(Shell parent, PathListElement entryToEdit,
            boolean focusOnExcluded) {
        super(parent);
        setShellStyle(getShellStyle() | SWT.RESIZE);

        currElement = entryToEdit;

        setTitle("Inclusion and Exclusion Patterns");

        IProject currProject = entryToEdit.getProject();
        IWorkspaceRoot root = currProject.getWorkspace().getRoot();
        IResource res = root.findMember(entryToEdit.getPath());
        if (res instanceof IContainer) {
            currSourceFolder = (IContainer) res;
        }

        String excLabel = "E&xclusion patterns:";
        String[] excButtonLabels = new String[] {
                /* IDX_ADD */"&Add...",
                /* IDX_ADD_MULTIPLE */"Add M&ultiple...",
                /* IDX_EDIT */"Edi&t...", null,
        /* IDX_REMOVE */"Rem&ove" };

        String incLabel = "I&nclusion patterns:";
        String[] incButtonLabels = new String[] {
                /* IDX_ADD */"A&dd...",
                /* IDX_ADD_MULTIPLE */"Add &Multiple...",
                /* IDX_EDIT */"&Edit...", null,
        /* IDX_REMOVE */"&Remove" };

        exclPatternList = createListContents(entryToEdit, PathListElement.EXCLUSION,
                excLabel,
                FileRsyncPlugin.getImageDescriptor("icons/remove_from_path.gif"),
                excButtonLabels);
        inclPatternList = createListContents(entryToEdit, PathListElement.INCLUSION,
                incLabel, FileRsyncPlugin.getImageDescriptor("icons/add_to_path.gif"),
                incButtonLabels);
        if (focusOnExcluded) {
            exclPatternList.postSetFocusOnDialogField(parent.getDisplay());
        } else {
            inclPatternList.postSetFocusOnDialogField(parent.getDisplay());
        }
    }

    private ListDialogField createListContents(PathListElement entryToEdit, String key,
            String label, ImageDescriptor descriptor, String[] buttonLabels) {
        ExclusionPatternAdapter adapter = new ExclusionPatternAdapter();

        ListDialogField patternList = new ListDialogField(adapter, buttonLabels,
                new ExclusionInclusionLabelProvider(key, descriptor));
        patternList.setDialogFieldListener(adapter);
        patternList.setLabelText(label);
        patternList.setRemoveButtonIndex(IDX_REMOVE);
        patternList.enableButton(IDX_EDIT, false);

        IPath[] pattern = (IPath[]) entryToEdit.getAttribute(key);

        ArrayList<String> elements = new ArrayList<String>(pattern.length);
        for (int i = 0; i < pattern.length; i++) {
            elements.add(pattern[i].toString());
        }
        patternList.setElements(elements);
        patternList.selectFirstElement();
        patternList.enableButton(IDX_ADD_MULTIPLE, currSourceFolder != null);
        patternList.setViewerComparator(new ViewerComparator());
        return patternList;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        Composite inner = new Composite(composite, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.numColumns = 2;
        inner.setLayout(layout);
        inner.setLayoutData(new GridData(GridData.FILL_BOTH));

        DialogField labelField = new DialogField();
        String name = currElement.getPath().makeRelative().toString();
        labelField.setLabelText(MessageFormat.format(
                "Included and excluded resources for ''{0}''.", new Object[] { name }));
        labelField.doFillIntoGrid(inner, 2);

        inclPatternList.doFillIntoGrid(inner, 3);
        LayoutUtil.setHorizontalSpan(inclPatternList.getLabelControl(null), 2);
        LayoutUtil.setHorizontalGrabbing(inclPatternList.getListControl(null));

        exclPatternList.doFillIntoGrid(inner, 3);
        LayoutUtil.setHorizontalSpan(exclPatternList.getLabelControl(null), 2);
        LayoutUtil.setHorizontalGrabbing(exclPatternList.getListControl(null));

        applyDialogFont(composite);
        return composite;
    }

    protected void doCustomButtonPressed(ListDialogField field, int index) {
        if (index == IDX_ADD) {
            addEntry(field);
        } else if (index == IDX_EDIT) {
            editEntry(field);
        } else if (index == IDX_ADD_MULTIPLE) {
            addMultipleEntries(field);
        }
    }

    protected void doDoubleClicked(ListDialogField field) {
        editEntry(field);
    }

    protected void doSelectionChanged(ListDialogField field) {
        List<String> selected = field.getSelectedElements();
        field.enableButton(IDX_EDIT, canEdit(selected));
    }

    private boolean canEdit(List<String> selected) {
        return selected.size() == 1;
    }

    private void editEntry(ListDialogField field) {

        List<String> selElements = field.getSelectedElements();
        if (selElements.size() != 1) {
            return;
        }
        List<String> existing = field.getElements();
        String entry = (String) selElements.get(0);
        InclusionExclusionEntryDialog dialog = new InclusionExclusionEntryDialog(
                getShell(), isExclusion(field), entry, existing, currElement);
        if (dialog.open() == Window.OK) {
            field.replaceElement(entry, dialog.getExclusionPattern());
        }
    }

    private boolean isExclusion(ListDialogField field) {
        return field == exclPatternList;
    }

    private void addEntry(ListDialogField field) {
        List<String> existing = field.getElements();
        InclusionExclusionEntryDialog dialog = new InclusionExclusionEntryDialog(
                getShell(), isExclusion(field), null, existing, currElement);
        if (dialog.open() == Window.OK) {
            field.addElement(dialog.getExclusionPattern());
        }
    }

    class ExclusionPatternAdapter implements IListAdapter, IDialogFieldListener {

        @Override
        public void customButtonPressed(ListDialogField field, int index) {
            doCustomButtonPressed(field, index);
        }

        @Override
        public void selectionChanged(ListDialogField field) {
            doSelectionChanged(field);
        }

        @Override
        public void doubleClicked(ListDialogField field) {
            doDoubleClicked(field);
        }

        @Override
        public void dialogFieldChanged(DialogField field) {
        }

    }

    private IPath[] getPattern(ListDialogField field) {
        Object[] arr = field.getElements().toArray();
        Arrays.sort(arr);
        IPath[] res = new IPath[arr.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = new Path((String) arr[i]);
        }
        return res;
    }

    public IPath[] getExclusionPattern() {
        return getPattern(exclPatternList);
    }

    public IPath[] getInclusionPattern() {
        return getPattern(inclPatternList);
    }

    private void addMultipleEntries(ListDialogField field) {
        Class<?>[] acceptedClasses = new Class[] { IFolder.class, IFile.class };
        ISelectionStatusValidator validator = new TypedElementSelectionValidator(
                acceptedClasses, true);
        ViewerFilter filter = new TypedViewerFilter(acceptedClasses);

        ILabelProvider lp = new WorkbenchLabelProvider();
        ITreeContentProvider cp = new WorkbenchContentProvider();

        String title, message;
        if (isExclusion(field)) {
            title = "Exclusion Pattern Selection";
            message = "&Choose folders or files to exclude:";
        } else {
            title = "Inclusion Pattern Selection";
            message = "&Choose folders or files to include:";
        }
        ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(),
                lp, cp);
        dialog.setTitle(title);
        dialog.setValidator(validator);
        dialog.setMessage(message);
        dialog.addFilter(filter);
        dialog.setInput(currSourceFolder);
        dialog.setInitialSelection(null);
        dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));

        if (dialog.open() == Window.OK) {
            Object[] objects = dialog.getResult();
            int existingSegments = currSourceFolder.getFullPath().segmentCount();

            for (int i = 0; i < objects.length; i++) {
                IResource curr = (IResource) objects[i];
                IPath path = curr.getFullPath().removeFirstSegments(existingSegments)
                        .makeRelative();
                String res;
                if (curr instanceof IContainer) {
                    res = path.addTrailingSeparator().toString();
                } else {
                    res = path.toString();
                }
                field.addElement(res);
            }
        }
    }

}

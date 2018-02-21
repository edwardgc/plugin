package com.nokia.filersync.properties;

import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.dialogs.NewFolderDialog;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.osgi.service.prefs.BackingStoreException;

import com.nokia.filersync.FileRsyncPlugin;
import com.nokia.filersync.builder.CharOperation;
import com.nokia.filersync.builder.FileRsyncBuilder;
import com.nokia.filersync.command.FileMapping;
import com.nokia.filersync.dialogs.DialogField;
import com.nokia.filersync.dialogs.IDialogFieldListener;
import com.nokia.filersync.dialogs.IStatusChangeListener;
import com.nokia.filersync.dialogs.IStringButtonAdapter;
import com.nokia.filersync.dialogs.ITreeListAdapter;
import com.nokia.filersync.dialogs.LayoutUtil;
import com.nokia.filersync.dialogs.MultipleFolderSelectionDialog;
import com.nokia.filersync.dialogs.PixelConverter;
import com.nokia.filersync.dialogs.SelectionButtonDialogField;
import com.nokia.filersync.dialogs.StatusInfo;
import com.nokia.filersync.dialogs.StringButtonDialogField;
import com.nokia.filersync.dialogs.StringDialogField;
import com.nokia.filersync.dialogs.TreeListDialogField;
import com.nokia.filersync.dialogs.TypedViewerFilter;

public class ProjectSyncPropertyPage extends PropertyPage implements
IStatusChangeListener {
    protected IStatus errorStatus = new StatusInfo(IStatus.ERROR, "Please select one file");

    protected IStatus okStatus = new StatusInfo();

    protected IWorkspaceRoot workspaceRoot;

    protected List<PathListElement> mappingList;

    protected TreeListDialogField<PathListElement> foldersList;

    private StatusInfo destFolderStatus;

    private IProject project;

    private final static int IDX_ADD = 0;

    private final static int IDX_EDIT = 2;

    private final static int IDX_REMOVE = 3;

    private final PathListElementComparator pathComparator = new PathListElementComparator();

    protected SelectionButtonDialogField exludeTeamFilesField;
    protected SelectionButtonDialogField skipNewerFilesField;

    protected StringButtonDialogField cygwinHomeField;
    protected StringDialogField targetHostField;
    protected StringDialogField targetUsernameField;

    private SelectionButtonDialogField enableFileRsyncField;

    protected static Composite createContainer(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);
        GridData gridData = new GridData(GridData.VERTICAL_ALIGN_FILL
                | GridData.HORIZONTAL_ALIGN_FILL);
        gridData.grabExcessHorizontalSpace = true;
        composite.setLayoutData(gridData);
        return composite;
    }

    @Override
    protected Control createContents(Composite parent) {
        TabFolder tabFolder = new TabFolder(parent, SWT.TOP);
        tabFolder.setLayout(new GridLayout(1, true));
        tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

        TabItem tabFilter = new TabItem(tabFolder, SWT.NONE);
        tabFilter.setText("Source and Target Configuration");

        TabItem support = new TabItem(tabFolder, SWT.NONE);
        support.setText("About...");
        Composite supportPanel = createContainer(tabFolder);
        support.setControl(supportPanel);
        SupportPanel.createSupportLinks(supportPanel);


        // ensure the page has no special buttons
        noDefaultAndApplyButton();

        mappingList = new ArrayList<PathListElement>();

        workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

        BuildPathAdapter adapter = new BuildPathAdapter();

        cygwinHomeField = new StringButtonDialogField(adapter);
        cygwinHomeField.setButtonLabel("Browse...");
        cygwinHomeField.setDialogFieldListener(adapter);
        cygwinHomeField.setLabelText("Cygwin Home Directory:");
        
        targetHostField = new StringDialogField();
        targetHostField.setDialogFieldListener(adapter);
        targetHostField.setLabelText("Target Host:");
        
        targetUsernameField = new StringDialogField();
        targetUsernameField.setDialogFieldListener(adapter);
        targetUsernameField.setLabelText("Username on Target Host:");

        destFolderStatus = new StatusInfo();

        project = (IProject) getElement();

        ProjectProperties properties = ProjectProperties.getInstance(project);
        List<IPreferenceChangeListener> listeners = properties.getProjectPreferenceChangeListeners();
        boolean noBuilderInstalled = listeners.isEmpty();

        init(properties);

        PixelConverter converter = new PixelConverter(tabFolder);

        Composite composite = new Composite(tabFolder, SWT.NONE);
        tabFilter.setControl(composite);

        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.numColumns = 1;
        composite.setLayout(layout);

        Composite folder = new Composite(composite, SWT.NONE);
        layout = new GridLayout();
        folder.setLayout(layout);
        folder.setLayoutData(new GridData(GridData.FILL_BOTH));

        initSyncPage();
        initSyncControl(folder);

        Composite editorcomp = new Composite(composite, SWT.NONE);

        DialogField[] editors = new DialogField[] { cygwinHomeField, 
        											targetHostField,
        											targetUsernameField};
        LayoutUtil.doDefaultLayout(editorcomp, editors, noBuilderInstalled, 0, 0);

        int maxFieldWidth = converter.convertWidthInCharsToPixels(40);
        LayoutUtil.setWidthHint(cygwinHomeField.getTextControl(null), maxFieldWidth);
        LayoutUtil.setHorizontalGrabbing(cygwinHomeField.getTextControl(null));

        editorcomp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        statusChanged(destFolderStatus);
        Dialog.applyDialogFont(composite);

        if (Display.getCurrent() != null) {
            updateUI();
        } else {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    updateUI();
                }
            });
        }
        return composite;
    }

    public void initSyncPage() {

        PathContainerAdapter adapter = new PathContainerAdapter();

        boolean disabled = !ProjectHelper.hasBuilder(project)
                || ProjectHelper.isBuilderDisabled(project);
        enableFileRsyncField = new SelectionButtonDialogField(SWT.CHECK);
        enableFileRsyncField.setSelection(!disabled);
        enableFileRsyncField.setLabelText("Enable FileRsync builder for project");
        enableFileRsyncField.setDialogFieldListener(adapter);

        String[] buttonLabels;

        buttonLabels = new String[] {
                /* 0 = IDX_ADDEXIST */"&Add Folder...",
                /* 1 */null,
                /* 2 = IDX_EDIT */"Edit...",
        		/* 3 = IDX_REMOVE */"Remove"};

        foldersList = new TreeListDialogField<PathListElement>(adapter, buttonLabels, new PathListLabelProvider());
        foldersList.setDialogFieldListener(adapter);
        foldersList.setLabelText("Available synchronization mappings:");

        /*
         * the small hack to have all entries sorted in alphab. order except
         * of "include/exclude" branches - they should be inversed
         */
        foldersList.setComparator(new ViewerComparator(new Collator() {
            private final Collator delegate = Collator.getInstance();

            @Override
            public int compare(String source, String target) {
                return PathListLabelProvider.compare(source, target);
            }

            @Override
            public CollationKey getCollationKey(String source) {
                return delegate.getCollationKey(source);
            }

            @Override
            public int hashCode() {
                return delegate.hashCode();
            }
        }));
        foldersList.enableButton(IDX_EDIT, false);

        exludeTeamFilesField = new SelectionButtonDialogField(SWT.CHECK);
        exludeTeamFilesField.setSelection(false);
        exludeTeamFilesField.setLabelText("Exclude team private files (like .svn)");
        exludeTeamFilesField.setDialogFieldListener(adapter);

        skipNewerFilesField = new SelectionButtonDialogField(SWT.CHECK);
        skipNewerFilesField.setSelection(false);
        skipNewerFilesField.setLabelText("Skip newer destination files");
        skipNewerFilesField.setDialogFieldListener(adapter);

        enableInputControls(!disabled);
    }

    protected void init() {
        ArrayList<Object> folders = new ArrayList<Object>();
        for (int i = 0; i < mappingList.size(); i++) {
            PathListElement cpe = (PathListElement) mappingList.get(i);
            folders.add(cpe);
        }

        foldersList.setElements(folders);

        for (int i = 0; i < folders.size(); i++) {
            PathListElement cpe = (PathListElement) folders.get(i);
            IPath[] patterns = (IPath[]) cpe.getAttribute(PathListElement.EXCLUSION);
            boolean hasOutputFolder = (cpe.getAttribute(PathListElement.DESTINATION) != null);
            if (patterns.length > 0 || hasOutputFolder) {
                foldersList.expandElement(cpe, 3);
            }
        }

        IEclipsePreferences preferences = getPreferences(false);
        boolean exludeTeamFiles = preferences.getBoolean(
                ProjectProperties.KEY_EXCLUDE_TEAM_FILES, false);
        exludeTeamFilesField.setSelection(exludeTeamFiles);
        boolean skipNewerFiles = preferences.getBoolean(
                ProjectProperties.KEY_SKIP_NEWER_FILES, false);
        skipNewerFilesField.setSelection(skipNewerFiles);
        
        cygwinHomeField.setText(preferences.get(ProjectProperties.KEY_CYGWIN_HOME, ""));
        targetHostField.setText(preferences.get(ProjectProperties.KEY_TARGET_HOST, ""));
        targetUsernameField.setText(preferences.get(ProjectProperties.KEY_TARGET_USERNAME, ""));
    }

    protected void initSyncControl(Composite parent) {
        PixelConverter converter = new PixelConverter(parent);
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        LayoutUtil.doDefaultLayout(composite, new DialogField[] { enableFileRsyncField,
                foldersList, 
                exludeTeamFilesField,
                skipNewerFilesField }, true, SWT.DEFAULT, SWT.DEFAULT);

        LayoutUtil.setHorizontalGrabbing(foldersList.getTreeControl(null));

        int buttonBarWidth = converter.convertWidthInCharsToPixels(24);
        foldersList.setButtonsMinWidth(buttonBarWidth);

        // expand
        List<Object> elements = foldersList.getElements();
        for (int i = 0; i < elements.size(); i++) {
            PathListElement elem = (PathListElement) elements.get(i);
            IPath[] exclusionPatterns = (IPath[]) elem
                    .getAttribute(PathListElement.EXCLUSION);
            IPath[] inclusionPatterns = (IPath[]) elem
                    .getAttribute(PathListElement.INCLUSION);
            IPath output = (IPath) elem.getAttribute(PathListElement.DESTINATION);
            if (exclusionPatterns.length > 0 || inclusionPatterns.length > 0
                    || output != null) {
                foldersList.expandElement(elem, TreeViewer.ALL_LEVELS);
            }
        }
    }

    protected void pathListKeyPressed(TreeListDialogField<PathListElement> field, KeyEvent event) {
        if (field == foldersList) {
            if (event.character == SWT.DEL && event.stateMask == 0) {
                List<Object> selection = field.getSelectedElements();
                if (canRemove(selection)) {
                    removeEntry();
                }
            }
        }
    }

    protected void pathListDoubleClicked(TreeListDialogField<PathListElement> field) {
        if (field == foldersList) {
            List<Object> selection = field.getSelectedElements();
            if (canEdit(selection)) {
                editEntry();
            }
        }
    }

    private boolean hasMembers(IContainer container) {
        try {
            IResource[] members = container.members();
            for (int i = 0; i < members.length; i++) {
                if (members[i] instanceof IContainer) {
                    return true;
                }
            }
        } catch (CoreException e) {
            // ignore
        }
        return false;
    }

    protected void pathListButtonPressed(DialogField field, int index) {
        if (field != foldersList) {
            return;
        }
        if (index == IDX_ADD) {
            addEntry();
        } else if (index == IDX_EDIT) {
            editEntry();
        } else if (index == IDX_REMOVE) {
            removeEntry();
        }
    }

    private void addEntry() {
        List<PathListElement> elementsToAdd = new ArrayList<PathListElement>(10);
        if (hasMembers(project)) {
            PathListElement[] srcentries = openFolderDialog(null);
            if (srcentries != null) {
                for (int i = 0; i < srcentries.length; i++) {
                    elementsToAdd.add(srcentries[i]);
                }
            }
        } else {
            boolean addRoot = MessageDialog.openQuestion(getShell(), "Project has no folders",
                    "Current project has no folders. Create mapping for project root?");
            if(addRoot){
                PathListElement entry = newFolderElement(project);
                elementsToAdd.add(entry);
            } else {
                PathListElement entry = openNewFolderDialog(null);
                if (entry != null) {
                    elementsToAdd.add(entry);
                }
            }
        }
        if (!elementsToAdd.isEmpty()) {

            HashSet<PathListElement> modifiedElements = new HashSet<PathListElement>();
            askForAddingExclusionPatternsDialog(elementsToAdd, modifiedElements);

            foldersList.addElements(elementsToAdd);
            foldersList.postSetSelection(new StructuredSelection(elementsToAdd));
            for(Object element : elementsToAdd) {
            	foldersList.expandElement(element, TreeViewer.ALL_LEVELS);
            }

            if (!modifiedElements.isEmpty()) {
                for (Object element : modifiedElements) {
                    foldersList.refresh(element);
                    foldersList.expandElement(element, TreeViewer.ALL_LEVELS);
                }
            }
            dialogFieldChanged(foldersList);
        }
    }

    private void editEntry() {
        List<Object> selElements = foldersList.getSelectedElements();
        if (selElements.size() != 1) {
            return;
        }
        Object elem = selElements.get(0);
        if (foldersList.getIndexOfElement(elem) != -1) {
            editElementEntry((PathListElement)elem);
        } else if (elem instanceof PathListElementAttribute) {
            editAttributeEntry((PathListElementAttribute) elem);
        }
    }

    private void editElementEntry(PathListElement elem) {
        PathListElement res = null;

        res = openNewFolderDialog(elem);

        if (res != null) {
            foldersList.replaceElement(elem, res);
        }
    }

    private void editAttributeEntry(PathListElementAttribute elem) {
        String key = elem.getKey();
        if (key.equals(PathListElement.DESTINATION)) {
            IPath path = (IPath) elem.getValue();
            String strPath = path == null ? "" : path.toString();
            InputDialog dialog = new InputDialog(getShell(), "Target directory", "Target directory", strPath.toString(), null);
            if (dialog.open() == Window.OK) {
                elem.getParent().setAttribute(PathListElement.DESTINATION,
                        new Path(dialog.getValue()), "");
                foldersList.refresh();
            }
            dialogFieldChanged(foldersList);
        } else if (key.equals(PathListElement.EXCLUSION)) {
            showExclusionInclusionDialog(elem.getParent(), true);
        } else if (key.equals(PathListElement.INCLUSION)) {
            showExclusionInclusionDialog(elem.getParent(), false);
        }
    }

    private void showExclusionInclusionDialog(PathListElement selElement,
            boolean focusOnExclusion) {
        InclusionExclusionDialog dialog = new InclusionExclusionDialog(getShell(),
                selElement, focusOnExclusion);
        if (dialog.open() == Window.OK) {
            selElement.setAttribute(PathListElement.INCLUSION, dialog
                    .getInclusionPattern(), null);
            selElement.setAttribute(PathListElement.EXCLUSION, dialog
                    .getExclusionPattern(), null);
            foldersList.refresh();
        }
    }

    protected void pathListSelectionChanged(DialogField field) {
        List<Object> selected = foldersList.getSelectedElements();
        foldersList.enableButton(IDX_EDIT, canEdit(selected));
        foldersList.enableButton(IDX_REMOVE, canRemove(selected));
        boolean noAttributes = !hasAttributes(selected);
        foldersList.enableButton(IDX_ADD, noAttributes);
    }

    private boolean hasAttributes(List selElements) {
        if (selElements.size() == 0) {
            return false;
        }
        for (int i = 0; i < selElements.size(); i++) {
            if (selElements.get(i) instanceof PathListElementAttribute) {
                return true;
            }
        }
        return false;
    }

    private void removeEntry() {
        List selElements = foldersList.getSelectedElements();
        for (int i = selElements.size() - 1; i >= 0; i--) {
            Object elem = selElements.get(i);
            if (elem instanceof PathListElementAttribute) {
                PathListElementAttribute attrib = (PathListElementAttribute) elem;
                String key = attrib.getKey();
                Object value = null;
                Object defaultValue = null;
                if (key.equals(PathListElement.EXCLUSION)
                        || key.equals(PathListElement.INCLUSION)) {
                    value = new Path[0];
                    defaultValue = value;
                }
                attrib.getParent().setAttribute(key, value, defaultValue);
                selElements.remove(i);
            }
        }
        if (selElements.isEmpty()) {
            foldersList.refresh();
        } else {
            foldersList.removeElements(selElements);
        }
        dialogFieldChanged(foldersList);
    }

    private boolean canRemove(List selElements) {
        if (selElements.size() == 0) {
            return false;
        }
        for (int i = 0; i < selElements.size(); i++) {
            Object elem = selElements.get(i);
            if (elem instanceof PathListElementAttribute) {
                PathListElementAttribute attrib = (PathListElementAttribute) elem;
                String key = attrib.getKey();
                if (PathListElement.INCLUSION.equals(key)) {
                    if (((IPath[]) attrib.getValue()).length == 0) {
                        return false;
                    }
                } else if (PathListElement.EXCLUSION.equals(key)) {
                    if (((IPath[]) attrib.getValue()).length == 0) {
                        return false;
                    }
                } else if (attrib.getValue() == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean canEdit(List selElements) {
        if (selElements.size() != 1) {
            return false;
        }
        Object elem = selElements.get(0);
        if (elem instanceof PathListElement) {
            return false;
        }
        if (elem instanceof PathListElementAttribute) {
            return true;
        }
        return false;
    }

    protected void pathListDialogFieldChanged(DialogField field) {
        if (project == null) {
            return;
        }

        if (field == foldersList) {
             updatePatternList();
        } else if (field == enableFileRsyncField) {
            boolean ok;
            boolean selected = enableFileRsyncField.isSelected();
            if (selected) {
                ok = ProjectHelper.addBuilder(project);
            } else {
                ok = ProjectHelper.disableBuilder(project);
            }
            if (!ok) {
                String title = "Error";
                String message = "Changing project builder properties failed.";
                MessageDialog.openInformation(getShell(), title, message);
            } else {
                enableInputControls(selected);
            }
        }
    }

    protected void enableInputControls(boolean selected) {
        skipNewerFilesField.setEnabled(selected);
        exludeTeamFilesField.setEnabled(selected);
        cygwinHomeField.setEnabled(selected);
        targetHostField.setEnabled(selected);
        targetUsernameField.setEnabled(selected);
        foldersList.setEnabled(selected);
        if (selected) {
            updateTargetDirectoryStatus();
        } else {
            destFolderStatus.setOK();
        }
        statusChanged(destFolderStatus);
    }

    private void updatePatternList() {
        List srcelements = foldersList.getElements();

        List oldmappings = mappingList;
        List newMappings = new ArrayList(mappingList);
        for (int i = 0; i < oldmappings.size(); i++) {
            PathListElement cpe = (PathListElement) oldmappings.get(i);
            if (!srcelements.contains(cpe)) {
                newMappings.remove(cpe);
            } else {
                // let be only really new elements in the updated list
                srcelements.remove(cpe);
            }
        }
        if (!srcelements.isEmpty()) {
            for (int i = 0; i < srcelements.size(); i++) {
                PathListElement cpe = (PathListElement) srcelements.get(i);
                if (!newMappings.contains(cpe)) {
                    newMappings.add(cpe);
                }
            }
        }
        mappingList = newMappings;
    }

    private PathListElement openNewFolderDialog(PathListElement existing) {
        NewFolderDialog dialog = new NewFolderDialog(getShell(), project);
        dialog.setTitle("Create new folder...");
        if (dialog.open() == Window.OK) {
            IResource createdFolder = (IResource) dialog.getResult()[0];
            return newFolderElement(createdFolder);
        }
        return null;
    }

    private void askForAddingExclusionPatternsDialog(List newEntries, Set modifiedEntries) {
        fixNestingConflicts(newEntries, foldersList.getElements(), modifiedEntries);
        if (!modifiedEntries.isEmpty()) {
            String title = "Folder added";
            String message = "Exclusion filters have been added to nesting folders";
            MessageDialog.openInformation(getShell(), title, message);
        }
    }

    private PathListElement[] openFolderDialog(PathListElement existing) {

        Class[] acceptedClasses = new Class[] { IProject.class, IFolder.class };
        List existingContainers = getExistingContainers(null);

        IProject[] allProjects = workspaceRoot.getProjects();
        ArrayList rejectedElements = new ArrayList(allProjects.length);
        IProject currProject = project;
        for (int i = 0; i < allProjects.length; i++) {
            if (!allProjects[i].equals(currProject)) {
                rejectedElements.add(allProjects[i]);
            }
        }
        ViewerFilter filter = new TypedViewerFilter(acceptedClasses, rejectedElements
                .toArray());

        ILabelProvider lp = new WorkbenchLabelProvider();
        ITreeContentProvider cp = new BaseWorkbenchContentProvider();

        String title = "Folder Selection";
        String message = "&Choose folders to be added to the synchronization mapping:";

        MultipleFolderSelectionDialog dialog = new MultipleFolderSelectionDialog(
                getShell(), lp, cp);
        dialog.setExisting(existingContainers.toArray());
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.addFilter(filter);
        dialog.setInput(project.getParent());
        if (existing == null) {
            dialog.setInitialFocus(project);
        } else {
            dialog.setInitialFocus(existing.getResource());
        }
        if (dialog.open() == Window.OK) {
            Object[] elements = dialog.getResult();
            PathListElement[] res = new PathListElement[elements.length];
            for (int i = 0; i < res.length; i++) {
                IResource elem = (IResource) elements[i];
                res[i] = newFolderElement(elem);
            }
            return res;
        }
        return null;
    }

    private List getExistingContainers(PathListElement existing) {
        List res = new ArrayList();
        List cplist = foldersList.getElements();
        for (int i = 0; i < cplist.size(); i++) {
            PathListElement elem = (PathListElement) cplist.get(i);
            if (elem != existing) {
                IResource resource = elem.getResource();
                if (resource instanceof IContainer) { // defensive code
                    res.add(resource);
                }
            }
        }
        return res;
    }

    private PathListElement newFolderElement(IResource res) {
        Assert.isNotNull(res);
        return new PathListElement(project, res.getFullPath(), res);
    }

    /*
     * @see BuildPathBasePage#getSelection
     */
    public List getSelection() {
        return foldersList.getSelectedElements();
    }

    /*
     * @see BuildPathBasePage#setSelection
     */
    public void setSelection(List selElements) {
        foldersList.selectElements(new StructuredSelection(selElements));
    }

    protected void filterAndSetSelection(List list) {
        ArrayList res = new ArrayList(list.size());
        for (int i = list.size() - 1; i >= 0; i--) {
            Object curr = list.get(i);
            if (curr instanceof PathListElement) {
                res.add(curr);
            }
        }
        setSelection(res);
    }

    protected void fixNestingConflicts(List newEntries, List existing,
            Set modifiedSourceEntries) {
        for (int i = 0; i < newEntries.size(); i++) {
            PathListElement curr = (PathListElement) newEntries.get(i);
            addExclusionPatterns(curr, existing, modifiedSourceEntries);
            existing.add(curr);
        }
    }

    private void addExclusionPatterns(PathListElement newEntry, List existing,
            Set modifiedEntries) {
        IPath entryPath = newEntry.getPath();
        for (int i = 0; i < existing.size(); i++) {
            PathListElement curr = (PathListElement) existing.get(i);

            IPath currPath = curr.getPath();
            if (!currPath.equals(entryPath)) {
                if (currPath.isPrefixOf(entryPath)) {
                    IPath[] exclusionFilters = (IPath[]) curr
                            .getAttribute(PathListElement.EXCLUSION);
                    if (!isExcludedPath(entryPath, exclusionFilters)) {
                        IPath pathToExclude = entryPath.removeFirstSegments(
                                currPath.segmentCount()).addTrailingSeparator();
                        IPath[] newExclusionFilters = new IPath[exclusionFilters.length + 1];
                        System.arraycopy(exclusionFilters, 0, newExclusionFilters, 0,
                                exclusionFilters.length);
                        newExclusionFilters[exclusionFilters.length] = pathToExclude;
                        curr.setAttribute(PathListElement.EXCLUSION, newExclusionFilters,
                                null);
                        modifiedEntries.add(curr);
                    }
                } else if (entryPath.isPrefixOf(currPath)) {
                    IPath[] exclusionFilters = (IPath[]) newEntry
                            .getAttribute(PathListElement.EXCLUSION);

                    if (!isExcludedPath(currPath, exclusionFilters)) {
                        IPath pathToExclude = currPath.removeFirstSegments(
                                entryPath.segmentCount()).addTrailingSeparator();
                        IPath[] newExclusionFilters = new IPath[exclusionFilters.length + 1];
                        System.arraycopy(exclusionFilters, 0, newExclusionFilters, 0,
                                exclusionFilters.length);
                        newExclusionFilters[exclusionFilters.length] = pathToExclude;
                        newEntry.setAttribute(PathListElement.EXCLUSION,
                                newExclusionFilters, null);
                        modifiedEntries.add(newEntry);
                    }
                }
            }
        }
    }

    /**
     * Copy from StatusUtil
     * Applies the status to the status line of a dialog page.
     */
    public static void applyToStatusLine(DialogPage page, IStatus status) {
        String message = status.getMessage();
        switch (status.getSeverity()) {
        case IStatus.OK:
            page.setMessage(message, IMessageProvider.NONE);
            page.setErrorMessage(null);
            break;
        case IStatus.WARNING:
            page.setMessage(message, IMessageProvider.WARNING);
            page.setErrorMessage(null);
            break;
        case IStatus.INFO:
            page.setMessage(message, IMessageProvider.INFORMATION);
            page.setErrorMessage(null);
            break;
        default:
            if (message.length() == 0) {
                message = null;
            }
            page.setMessage(null);
            page.setErrorMessage(message);
            break;
        }
    }

    /**
     * copy from JavaModelUtils
     * @param resourcePath
     * @param exclusionPatterns
     */
    public static boolean isExcludedPath(IPath resourcePath, IPath[] exclusionPatterns) {
        char[] path = resourcePath.toString().toCharArray();
        for (int i = 0, length = exclusionPatterns.length; i < length; i++) {
            char[] pattern = exclusionPatterns[i].toString().toCharArray();
            if (CharOperation.pathMatch(pattern, path, true, '/')) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void performDefaults() {
    }

    @Override
    public boolean performOk() {
        if (!destFolderStatus.isOK()) {
            return false;
        }
        if (!hasChangesInDialog()) {
            return true;
        }
        ProjectProperties properties = ProjectProperties.getInstance(project);
        return properties.saveConfig(getNewConfig());
    }

    protected IEclipsePreferences getPreferences(boolean forceSync) {
        ProjectProperties properties = ProjectProperties.getInstance(project);
        boolean wasDisabled = true;
        if(forceSync){
            List listeners = properties.getProjectPreferenceChangeListeners();
            for (int i = 0; i < listeners.size(); i++) {
                FileRsyncBuilder b = (FileRsyncBuilder) listeners.get(i);
                wasDisabled = b.isDisabled();
                if(!b.isDisabled()) {
                    b.setDisabled(true);
                }
            }
        }
        IEclipsePreferences preferences = properties.getPreferences(forceSync);
        if(forceSync){
            List listeners = properties.getProjectPreferenceChangeListeners();
            for (int i = 0; i < listeners.size(); i++) {
                FileRsyncBuilder b = (FileRsyncBuilder) listeners.get(i);
                if(!wasDisabled) {
                    b.setDisabled(false);
                }
            }
        }
        return preferences;
    }

    @Override
    public void statusChanged(IStatus status) {
        setValid(!status.matches(IStatus.ERROR));
        applyToStatusLine(this, status);
    }

    protected void init(ProjectProperties prop) {
    	List<FileMapping> mappings = prop.getMappings();
        List<PathListElement> newClassPath = new ArrayList<PathListElement>();
        for (FileMapping mapping : mappings) {
            newClassPath.add(new PathListElement(project, mapping));
        }
        Collections.sort(newClassPath, pathComparator);
        mappingList = newClassPath;
        //TODO init dialog
    }

    protected void updateUI() {
        init();
        updateTargetDirectoryStatus();
        doStatusLineUpdate();
    }

    public boolean hasChangesInDialog() {
    	ProjectProperties properties = ProjectProperties.getInstance(project);
        return !getNewConfig().equals(properties.getSavedConfig());
    }

    /**
     * @return Returns the Java project. Can return
     *         <code>null<code> if the page has not
     * been initialized.
     */
    public IProject getProject() {
        return project;
    }

    /**
     * @return Returns the current class path (raw). Note that the entries
     *         returned must not be valid.
     */
    public List<FileMapping> getFileMappings() {
        List<FileMapping> entries = new ArrayList<FileMapping>(mappingList.size());
        for (PathListElement path : mappingList) {
            entries.add(path.getMapping());
        }
        return entries;
    }

    void changeControlPressed(DialogField field) {
        if (field == cygwinHomeField) {
            DirectoryDialog dialog = new DirectoryDialog(getShell());
            dialog.setMessage("Select Cygwin Home directory");
            if (cygwinHomeField.getText() != null) {
                dialog.setFilterPath(cygwinHomeField.getText());
            }
            String absPath = dialog.open();
            if (absPath == null) {
                return;
            }
            IPath destPath = new Path(absPath);
            cygwinHomeField.setText(destPath.toOSString());
        } 
    }

    void dialogFieldChanged(DialogField field) {
    	if(field == foldersList) {
	        updateTargetDirectoryStatus();
	        doStatusLineUpdate();
    	}
    }

    private void doStatusLineUpdate() {
        if (Display.getCurrent() != null) {
            statusChanged(destFolderStatus);
        }
    }

    private void updateTargetDirectoryStatus() {
    	if(!validateTargetDirectory()) {
    		destFolderStatus.setError("Please specify target directory!");
            return;
        }
        destFolderStatus.setOK();
    }
    
    protected boolean validateTargetDirectory() {
        if (mappingList == null) {
            return true;
        }
        for (int i = 0; i < mappingList.size(); i++) {
            PathListElement cpe = (PathListElement) mappingList.get(i);
            Object dest = cpe.getAttribute(PathListElement.DESTINATION);
            boolean noDestFolder = dest == null || dest.toString().trim().length() == 0;
            if (noDestFolder) {
                return false;
            }
        }
        return true;
    }


    class PathContainerAdapter implements ITreeListAdapter<PathListElement>, IDialogFieldListener {

        private final Object[] EMPTY_ARR = new Object[0];

        @Override
        public void customButtonPressed(TreeListDialogField<PathListElement> field, int index) {
            pathListButtonPressed(field, index);
        }

        @Override
        public void selectionChanged(TreeListDialogField<PathListElement> field) {
            pathListSelectionChanged(field);
        }

        @Override
        public void doubleClicked(TreeListDialogField<PathListElement> field) {
            pathListDoubleClicked(field);
        }

        @Override
        public void keyPressed(TreeListDialogField<PathListElement> field, KeyEvent event) {
            pathListKeyPressed(field, event);
        }

        @Override
        public Object[] getChildren(TreeListDialogField<PathListElement> field, Object element) {
            if (element instanceof PathListElement) {
                return ((PathListElement) element).getChildren();
            }
            return EMPTY_ARR;
        }

        @Override
        public Object getParent(TreeListDialogField<PathListElement> field, Object element) {
            if (element instanceof PathListElementAttribute) {
                return ((PathListElementAttribute) element).getParent();
            }
            return null;
        }

        @Override
        public boolean hasChildren(TreeListDialogField<PathListElement> field, Object element) {
            return (element instanceof PathListElement);
        }

        @Override
        public void dialogFieldChanged(DialogField field) {
            pathListDialogFieldChanged(field);
        }
    }

    class BuildPathAdapter implements IStringButtonAdapter, IDialogFieldListener {

        @Override
        public void changeControlPressed(DialogField field) {
            ProjectSyncPropertyPage.this.changeControlPressed(field);
        }

        @Override
        public void dialogFieldChanged(DialogField field) {
            ProjectSyncPropertyPage.this.dialogFieldChanged(field);
        }
    }
    
    public FileRsyncConfig getNewConfig() {
    	FileRsyncConfig config = new FileRsyncConfig();
    	config.setMappings(getFileMappings());
    	config.setExludeTeamFiles(exludeTeamFilesField.isSelected());
    	config.setSkipNewerFiles(skipNewerFilesField.isSelected());
    	config.setCygwinHome(cygwinHomeField.getText());
    	config.setTargetHost(targetHostField.getText());
    	config.setTargetUsername(targetUsernameField.getText());
    	return config;
    } 
}

package com.nokia.filersync.properties;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.nokia.filersync.FileRsyncPlugin;

public class PathListLabelProvider extends LabelProvider {

    private static final String TARGET_FOLDER = "Target directory: ";
    public static final String EXCLUDED = "Excluded: ";
    public static final String INCLUDED = "Included: ";
    
    private static final String NONE = "(None)";
    private static final String ALL = "(All)";

    public PathListLabelProvider() {
        super();
    }

    @Override
    public String getText(Object element) {
        if (element instanceof PathListElement) {
            return getPathListElementText((PathListElement) element);
        } else if (element instanceof PathListElementAttribute) {
            return getPathListElementAttributeText((PathListElementAttribute) element);
        }
        return super.getText(element);
    }

    public static int compare(String label1, String label2){
        int sourceOrder = getOrder(label1);
        int targetOrder = getOrder(label2);
        if(sourceOrder > targetOrder){
            return 1;
        } else if(sourceOrder < targetOrder){
            return -1;
        }
        return 0;
    }

    public static int getOrder(String s){
        if(s.startsWith(INCLUDED)){
            return 1;
        }
        if(s.startsWith(EXCLUDED)){
            return 2;
        }
        if(s.startsWith(TARGET_FOLDER)){
            return 3;
        }
        return 0;
    }

    public String getPathListElementAttributeText(PathListElementAttribute attrib) {
        StringBuffer buf = new StringBuffer();
        String key = attrib.getKey();
        if (key.equals(PathListElement.DESTINATION)) {
            buf.append(TARGET_FOLDER);
            IPath path = (IPath) attrib.getValue();
            if (path != null && !path.isEmpty()) {
                buf.append(path.toString());
            } else {
                buf.append(NONE);
            }
        } else if (key.equals(PathListElement.EXCLUSION)) {
            buf.append(EXCLUDED);
            IPath[] patterns = (IPath[]) attrib.getValue();
            if (patterns != null && patterns.length > 0) {
                for (int i = 0; i < patterns.length; i++) {
                    if (i > 0) {
                        buf.append("; ");
                    }
                    buf.append(patterns[i].toString());
                }
            } else {
                buf.append(NONE);
            }
        } else if (key.equals(PathListElement.INCLUSION)) {
            buf.append(INCLUDED);
            IPath[] patterns = (IPath[]) attrib.getValue();
            if (patterns != null && patterns.length > 0) {
                for (int i = 0; i < patterns.length; i++) {
                    if (i > 0) {
                        buf.append("; ");
                    }
                    buf.append(patterns[i].toString());
                }
            } else {
                buf.append(ALL);
            }
        }
        return buf.toString();
    }

    public String getPathListElementText(PathListElement entry) {
        IPath path = entry.getPath();
        StringBuffer buf = new StringBuffer(path.makeRelative().toString());
        return buf.toString();
    }

    @Override
    public Image getImage(Object element) {
        if (element instanceof PathListElement) {
            PathListElement cpentry = (PathListElement) element;
            String key = null;
            if (cpentry.getPath().segmentCount() == 1) {
                key = IDE.SharedImages.IMG_OBJ_PROJECT;
            } else {
                key = ISharedImages.IMG_OBJ_FOLDER;
            }
            return PlatformUI.getWorkbench().getSharedImages().getImage(key);
        } else if (element instanceof PathListElementAttribute) {
            String key = ((PathListElementAttribute) element).getKey();
            if (key.equals(PathListElement.DESTINATION)) {
                return FileRsyncPlugin.getImage("icons/output_folder.gif");
            } else if (key.equals(PathListElement.EXCLUSION)) {
                return FileRsyncPlugin.getImage("icons/remove_from_path.gif");
            } else if (key.equals(PathListElement.INCLUSION)) {
                return FileRsyncPlugin.getImage("icons/add_to_path.gif");
            }
        }
        return null;
    }

}

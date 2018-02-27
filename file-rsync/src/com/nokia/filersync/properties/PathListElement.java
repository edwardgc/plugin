package com.nokia.filersync.properties;

import java.net.URL;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.nokia.filersync.command.FileMapping;

public class PathListElement {

    public static final String DESTINATION = "output";

    public static final String EXCLUSION = "exclusion";

    public static final String INCLUSION = "inclusion";

    private final IProject fProject;

    private IPath fPath;

    private final IResource fResource;

    private FileMapping cachedMapping;

    private final ArrayList<PathListElementAttribute> fChildren;

    public PathListElement(IProject project, IPath path, IResource res) {
        fProject = project;

        fPath = path;
        fChildren = new ArrayList<PathListElementAttribute>();
        fResource = res;
        cachedMapping = null;

        createAttributeElement(INCLUSION, new Path[0]);
        createAttributeElement(EXCLUSION, new Path[0]);
        createAttributeElement(DESTINATION, null);
    }

    public PathListElement(IProject project, FileMapping mapping) {
        fProject = project;

        fPath = new Path(project.getName());
        fPath = fPath.append(mapping.getSourcePath());

        fChildren = new ArrayList<PathListElementAttribute>();
        fResource = project.getFolder(mapping.getSourcePath());
        cachedMapping = mapping;
        createAttributeElement(INCLUSION, mapping.getInclusionPatterns());
        createAttributeElement(EXCLUSION, mapping.getExclusionPatterns());
        createAttributeElement(DESTINATION, mapping.getDestinationPath());
    }

    public FileMapping getMapping() {
        if (cachedMapping == null) {
            cachedMapping = createMapping();
        }
        return cachedMapping;
    }

    private FileMapping createMapping() {
        IPath outputLocation = (IPath) getAttribute(DESTINATION);
        IPath[] inclusionPattern = (IPath[]) getAttribute(INCLUSION);
        IPath[] exclusionPattern = (IPath[]) getAttribute(EXCLUSION);
        return new FileMapping(getWithoutProject(fPath), fResource.getLocation(), outputLocation, 
                inclusionPattern, exclusionPattern, fProject.getLocation());
    }

    private IPath getWithoutProject(IPath path) {
        if (fProject.getName().equals(path.segment(0))) {
            return path.removeFirstSegments(1);
        }
        return path;
    }

    /**
     * Gets the class path entry path.
     */
    public IPath getPath() {
        return fPath;
    }

    /**
     * Entries without resource are either non existing or a variable entry
     * External jars do not have a resource
     */
    public IResource getResource() {
        return fResource;
    }

    public PathListElementAttribute setAttribute(String key, Object value,
            Object defaultValue) {
        PathListElementAttribute attribute = findAttributeElement(key);
        if (attribute == null) {
            return null;
        }
        attribute.setValue(value);
        attributeChanged(key);
        return attribute;
    }

    private PathListElementAttribute findAttributeElement(String key) {
        for (int i = 0; i < fChildren.size(); i++) {
            Object curr = fChildren.get(i);
            if (curr instanceof PathListElementAttribute) {
                PathListElementAttribute elem = (PathListElementAttribute) curr;
                if (key.equals(elem.getKey())) {
                    return elem;
                }
            }
        }
        return null;
    }

    public Object getAttribute(String key) {
        PathListElementAttribute attrib = findAttributeElement(key);
        if (attrib != null) {
            return attrib.getValue();
        }
        return null;
    }

    private void createAttributeElement(String key, Object value) {
        fChildren.add(new PathListElementAttribute(this, key, value));
    }

    public Object[] getChildren() {
        return fChildren.toArray();
    }

    private void attributeChanged(String key) {
        cachedMapping = null;
    }

    /*
     * @see Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object other) {
        if (other != null && other.getClass().equals(getClass())) {
            PathListElement elem = (PathListElement) other;
            return elem.fPath.equals(fPath) && getMapping().equals(elem.getMapping());
        }
        return false;
    }

    /*
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getMapping().hashCode();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getMapping().toString();
    }

    /**
     * Gets the project.
     * @return Returns a IJavaProject
     */
    public IProject getProject() {
        return fProject;
    }

    public static StringBuffer appendEncodePath(IPath path, StringBuffer buf) {
        if (path != null) {
            String str = path.toString();
            buf.append('[').append(str.length()).append(']').append(str);
        } else {
            buf.append('[').append(']');
        }
        return buf;
    }

    public static StringBuffer appendEncodedURL(URL url, StringBuffer buf) {
        if (url != null) {
            String str = url.toExternalForm();
            buf.append('[').append(str.length()).append(']').append(str);
        } else {
            buf.append('[').append(']');
        }
        return buf;
    }

    public StringBuffer appendEncodedSettings(StringBuffer buf) {
        appendEncodePath(fPath, buf).append(';');
        buf.append("false").append(';');
        IPath output = (IPath) getAttribute(DESTINATION);
        appendEncodePath(output, buf).append(';');
        IPath[] exclusion = (IPath[]) getAttribute(EXCLUSION);
        buf.append('[').append(exclusion.length).append(']');
        for (int i = 0; i < exclusion.length; i++) {
            appendEncodePath(exclusion[i], buf).append(';');
        }
        IPath[] inclusion = (IPath[]) getAttribute(INCLUSION);
        buf.append('[').append(inclusion.length).append(']');
        for (int i = 0; i < inclusion.length; i++) {
            appendEncodePath(inclusion[i], buf).append(';');
        }
        return buf;
    }

}

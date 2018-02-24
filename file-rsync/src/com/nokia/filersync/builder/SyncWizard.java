package com.nokia.filersync.builder;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;

import com.nokia.filersync.command.FileMapping;
import com.nokia.filersync.properties.ProjectProperties;

public class SyncWizard {
    protected static final IContentType TEXT_TYPE = Platform.getContentTypeManager()
            .getContentType("org.eclipse.core.runtime.text"); //$NON-NLS-1$

    /**
     * all known file mappings for this wizard
     */
    private List<FileMapping> mappings;

    private ProjectProperties projectProps;

    public SyncWizard() {
        super();
    }

    public void setProjectProps(ProjectProperties props) throws IllegalArgumentException {
        projectProps = props;
        mappings = props.getMappings();
        if (mappings == null || mappings.size() == 0) {
            throw new IllegalArgumentException("FileRsync synchronization mapping is missing. "
                    + "Please add it in File Rsync setting page.");
        }
    }

    /**
     * @param res
     * @return true, if given resource is known by project mappings and allowed
     * to be synchronized with target.
     */
    public boolean checkResource(IResource res) {
        return matchFilter(res);
    }

    /**
     * @param delta
     * @return true, if resource from given delta is known by project
     * mappings and allowed to be synchronized with target.
     */
    public boolean checkResource(IResourceDelta delta) {
        IResource res = delta.getResource();
        return checkResource(res);
    }

    public boolean hasMappedChildren(IResource resource) {
        if (resource.isPhantom()) {
            // !resource.isAccessible() excludes deleted files - but this is needed here
            return false;
        }
        if (resource.getType() == IResource.PROJECT) {
            return true;
        }
        IPath relativePath = resource.getProjectRelativePath();
        return hasMappedChildren(relativePath, resource.getType() == IResource.FOLDER);
    }

    public boolean hasMappedChildren(IPath path, boolean isFolder) {
        for (FileMapping fm : mappings) {
            if (fm.getSourcePath().isPrefixOf(path)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasMappedChildren(IResourceDelta delta) {
        IResource res = delta.getResource();
        return hasMappedChildren(res);
    }

    /**
     * Performs all required operations to sync given delta with target directory
     * @param delta
     * @return true only if this operation was successful for all mapped files
     */
    public IResource sync(IResourceDelta delta) {
		IResource res = delta.getResource();
		switch (delta.getKind()) {
		case IResourceDelta.ADDED:
			return res;
		case IResourceDelta.REMOVED:
			return res;
		case IResourceDelta.REPLACED: // Fall-Through
		case IResourceDelta.CHANGED:
			if (res.getType() == IResource.FILE) {
				return res;
			}
			break;
		default:
			break;
		}
		return null;
	}

    public void cleanUp(IProgressMonitor monitor) {
        projectProps = null;
        mappings = null;
    }

    /**
     * @param resource
     * @return true if given resource is folder or project
     */
    protected boolean isContainer(IResource resource) {
        return resource.getType() == IResource.FOLDER
                || resource.getType() == IResource.PROJECT;
    }

    /**
     * Check if given resource is in included and not in excluded entries patterns
     * in any one of known project files mappings.
     * @param resource
     * @return true
     */
    protected boolean matchFilter(IResource resource) {
        if (resource.isPhantom() || resource.getType() == IResource.PROJECT) {
            // !resource.isAccessible() excludes deleted files - but this is needed here
            return false;
        }
        IPath relativePath = resource.getProjectRelativePath();
        return matchFilter(relativePath, resource.getType() == IResource.FOLDER);
    }

    /**
     * Check if given path is in included and not in excluded entries patterns
     * in any one of known project files mappings.
     * @param path
     * @param isFolder
     * @return true
     */
    protected boolean matchFilter(IPath path, boolean isFolder) {
        for ( FileMapping fm : mappings) {
            if (fm.getSourcePath().isPrefixOf(path)) {
                char[][] excl = fm.fullExclusionPatternChars();
                char[][] incl = fm.fullInclusionPatternChars();
                boolean ex = isExcluded(path, incl, excl, isFolder);
                if (!ex) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * Copy from org.eclipse.jdt.internal.core.util.Util
     *
     * Returns whether the given resource path matches one of the inclusion/exclusion
     * patterns.
     * NOTE: should not be asked directly using pkg root pathes
     * @see IClasspathEntry#getInclusionPatterns
     * @see IClasspathEntry#getExclusionPatterns
     */
    public final static boolean isExcluded(IPath resourcePath,
            char[][] inclusionPatterns, char[][] exclusionPatterns, boolean isFolderPath) {
        if (inclusionPatterns == null && exclusionPatterns == null) {
            return false;
        }
        return isExcluded(resourcePath.toString().toCharArray(), inclusionPatterns,
                exclusionPatterns, isFolderPath);
    }

    /*
     * Copy from org.eclipse.jdt.internal.compiler.util.Util.isExcluded
     *
     * ToDO (philippe) should consider promoting it to CharOperation
     * Returns whether the given resource path matches one of the inclusion/exclusion
     * patterns.
     * NOTE: should not be asked directly using pkg root pathes
     * @see IClasspathEntry#getInclusionPatterns
     * @see IClasspathEntry#getExclusionPatterns
     */
    public final static boolean isExcluded(char[] path, char[][] inclusionPatterns,
            char[][] exclusionPatterns, boolean isFolderPath) {
        if (inclusionPatterns == null && exclusionPatterns == null) {
            return false;
        }

        inclusionCheck: if (inclusionPatterns != null) {
            for (int i = 0, length = inclusionPatterns.length; i < length; i++) {
                char[] pattern = inclusionPatterns[i];
                char[] folderPattern = pattern;
                if (isFolderPath) {
                    int lastSlash = CharOperation.lastIndexOf('/', pattern);
                    if (lastSlash != -1 && lastSlash != pattern.length - 1) { // trailing slash -> adds '**' for free (see http://ant.apache.org/manual/dirtasks.html)
                        int star = CharOperation.indexOf('*', pattern, lastSlash);
                        if ((star == -1 || star >= pattern.length - 1 || pattern[star + 1] != '*')) {
                            folderPattern = CharOperation.subarray(pattern, 0, lastSlash);
                        }
                    }
                }
                if (CharOperation.pathMatch(folderPattern, path, true, '/')) {
                    break inclusionCheck;
                }
            }
            return true; // never included
        }
        if (isFolderPath) {
            path = CharOperation.concat(path, new char[] { '*' }, '/');
        }
        if (exclusionPatterns != null) {
            for (int i = 0, length = exclusionPatterns.length; i < length; i++) {
                if (CharOperation.pathMatch(exclusionPatterns[i], path, true, '/')) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return Returns the projectProps.
     */
    public ProjectProperties getProjectProps() {
        return projectProps;
    }

}

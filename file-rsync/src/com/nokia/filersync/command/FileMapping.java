package com.nokia.filersync.command;

import java.util.StringTokenizer;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import com.nokia.filersync.FileRsyncPlugin;

public class FileMapping {

    public static final String MAP_PREFIX = "map";

    /**
     * Separator for the different mapping parts like include/exclude etc
     */
    public static final String MAP_SEPARATOR = "|";

    /**
     * Common prefix to identify mapping props, usually should followed by a number
     */
    public static final String FULL_MAP_PREFIX = MAP_PREFIX + MAP_SEPARATOR;

    public static final String PATTERN_SEPARATOR = ";";

    public static final String EMPTY_ENTRY = ",";

    public static final String PROJECT_ROOT = "/";

    private IPath sourcePath;
    private IPath sourceLocation;

    private IPath destinationPath;

    private IPath[] inclusionPatterns;

    private IPath[] exclusionPatterns;

    private char[][] fullCharExclusionPatterns;

    private char[][] fullCharInclusionPatterns;

    private static final char[][] EMPTY_CHARS = new char[0][];

    private static final char[][] ALL_CHARS = new char[][] { "**/*".toCharArray() };

    private IPath projectPath;

    private String encoding;

    private FileMapping() {
    }

    /**
     * @param sourcePath
     * @param destinationPath
     * @param inclusionPatterns
     * @param exclusionPatterns
     */
    public FileMapping(IPath sourcePath, IPath sourceLocation, IPath destinationPath,
            IPath[] inclusionPatterns, IPath[] exclusionPatterns, IPath projectPath) {
        this();
        this.sourcePath = sourcePath;
        this.sourceLocation = sourceLocation;
        this.destinationPath = destinationPath;
        this.inclusionPatterns = inclusionPatterns;
        this.exclusionPatterns = exclusionPatterns;
        this.projectPath = projectPath;
    }

    /**
     * @param fullMapping all required properties for current object
     * @see #encode()
     */
    public FileMapping(String fullMapping, IPath projectPath) {
        this();
        this.projectPath = projectPath;
        decode(fullMapping);
    }

    private void decode(String fullMapping) {
        if (fullMapping == null || fullMapping.indexOf(MAP_SEPARATOR) <= 0) {
            FileRsyncPlugin.log("Path map string is null or broken:" + fullMapping, null,
                    IStatus.WARNING);
            return;
        }
        StringTokenizer st = new StringTokenizer(fullMapping, MAP_SEPARATOR);
        if (!st.hasMoreTokens()) {
            FileRsyncPlugin.log("Path map should contain at least source folder"
                    + fullMapping, null, IStatus.WARNING);
            return;
        }
        String path = st.nextToken();
        if (path == null || path.trim().length() == 0) {
            FileRsyncPlugin.log("Source path couldn't be empty:" + fullMapping, null,
                    IStatus.WARNING);
            sourcePath = null;
            return;
        }
        sourcePath = new Path(path);
        
        path = st.nextToken();
        if (path == null || path.trim().length() == 0) {
        	FileRsyncPlugin.log("Source location couldn't be empty:" + fullMapping, null,
                    IStatus.WARNING);
        	sourceLocation = null;
            return;
        }
        sourceLocation = new Path(path);

        if (st.hasMoreTokens()) {
            path = st.nextToken();
            if (!isEmptyPath(path)) {
                setDestinationPath(new Path(path));
            }
        }
        if (st.hasMoreTokens()) {
            path = st.nextToken();
            if (!isEmptyPath(path)) {
                setInclusionPatterns(decodePatterns(path));
            }
        }
        if (st.hasMoreTokens()) {
            path = st.nextToken();
            if (!isEmptyPath(path)) {
                setExclusionPatterns(decodePatterns(path));
            }
        }
    }

	/**
     * @param path
     */
    private boolean isEmptyPath(String path) {
        if (path == null || path.trim().length() == 0) {
            return true;
        }
        return EMPTY_ENTRY.equals(path);
    }

    /**
     * @return this object, encoded as String
     * @see #decode(String)
     */
    public String encode() {
        StringBuffer sb = new StringBuffer();
        IPath shortSourcePath = getSourcePath();
        if (shortSourcePath.segmentCount() == 0) {
            sb.append(PROJECT_ROOT);
        } else {
            sb.append(shortSourcePath);
        }
        sb.append(MAP_SEPARATOR);
        if (sourceLocation != null) {
            sb.append(sourceLocation);
        } else {
            sb.append(EMPTY_ENTRY);
        }
        sb.append(MAP_SEPARATOR);
        if (getDestinationPath() != null) {
            sb.append(getDestinationPath());
        } else {
            sb.append(EMPTY_ENTRY);
        }
        sb.append(MAP_SEPARATOR);
        sb.append(encodePatterns(getInclusionPatterns()));
        sb.append(MAP_SEPARATOR);
        sb.append(encodePatterns(getExclusionPatterns()));
        return sb.toString();
    }

    private IPath[] decodePatterns(String value) {
        if (value == null) {
            return null;
        }
        StringTokenizer st = new StringTokenizer(value, PATTERN_SEPARATOR);
        int count = st.countTokens();
        if (count == 0) {
            return null;
        }
        IPath[] patterns = new IPath[count];
        while (st.hasMoreTokens()) {
            patterns[--count] = new Path(st.nextToken());
        }
        return patterns;
    }

    private String encodePatterns(IPath[] patterns) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < patterns.length; i++) {
            sb.append(patterns[i].toString());
            if (i < patterns.length - 1) {
                sb.append(PATTERN_SEPARATOR);
            }
        }
        if (patterns.length == 0) {
            sb.append(EMPTY_ENTRY);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return encode();
    }

    /**
     * @return Returns the destinationPath.
     */
    public IPath getDestinationPath() {
        return destinationPath;
    }

    /**
     * @param destinationPath The destinationPath to set.
     */
    public void setDestinationPath(IPath destinationPath) {
        this.destinationPath = destinationPath;
    }

    /**
     * @return Returns the exclusionPatterns, never null.
     */
    public IPath[] getExclusionPatterns() {
        return exclusionPatterns == null ? new IPath[0] : exclusionPatterns;
    }

    /**
     * @param exclusionPatterns The exclusionPatterns to set.
     */
    public void setExclusionPatterns(IPath[] exclusionPatterns) {
        this.exclusionPatterns = exclusionPatterns;
        fullCharExclusionPatterns = null;
    }

    /**
     * @return Returns the inclusionPatterns, never null.
     */
    public IPath[] getInclusionPatterns() {
        return inclusionPatterns == null ? new IPath[0] : inclusionPatterns;
    }

    /**
     * @param inclusionPatterns The inclusionPatterns to set.
     */
    public void setInclusionPatterns(IPath[] inclusionPatterns) {
        this.inclusionPatterns = inclusionPatterns;
        fullCharInclusionPatterns = null;
    }

    /**
     * @return Returns the sourcePath.
     */
    public IPath getSourcePath() {
        return sourcePath;
    }
    
    public IPath getSourceLocaiton() {
    	return sourceLocation;
    }

    /**
     * @return char based representation of the exclusions patterns full path.
     */
    public char[][] fullExclusionPatternChars() {
        if (exclusionPatterns == null || exclusionPatterns.length == 0) {
            return EMPTY_CHARS;
        }
        if (fullCharExclusionPatterns == null) {
            int length = exclusionPatterns.length;
            fullCharExclusionPatterns = new char[length][];
            IPath prefixPath = sourcePath.removeTrailingSeparator();
            for (int i = 0; i < length; i++) {
                if (!sourcePath.isRoot()) {
                    fullCharExclusionPatterns[i] = prefixPath
                            .append(exclusionPatterns[i]).toString().toCharArray();
                } else {
                    fullCharExclusionPatterns[i] = exclusionPatterns[i].toString()
                            .toCharArray();
                }
            }
        }
        return fullCharExclusionPatterns;
    }

    /**
     * @return char based representation of the inclusions patterns full path.
     */
    public char[][] fullInclusionPatternChars() {
        if (inclusionPatterns == null || inclusionPatterns.length == 0) {
            return ALL_CHARS;
        }
        if (fullCharInclusionPatterns == null) {
            int length = inclusionPatterns.length;
            fullCharInclusionPatterns = new char[length][];
            IPath prefixPath = sourcePath.removeTrailingSeparator();
            for (int i = 0; i < length; i++) {
                if (!sourcePath.isRoot()) {
                    fullCharInclusionPatterns[i] = prefixPath
                            .append(inclusionPatterns[i]).toString().toCharArray();
                } else {
                    fullCharInclusionPatterns[i] = inclusionPatterns[i].toString()
                            .toCharArray();
                }
            }
        }
        return fullCharInclusionPatterns;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof FileMapping)) {
            return false;
        }
        String mapAsString = encode();
        return mapAsString.equals(((FileMapping) obj).encode());
    }

    @Override
    public int hashCode() {
        String mapAsString = encode();
        return mapAsString.hashCode();
    }

    /**
     * @param encoding the encoding to set
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * @return the encoding
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * @param projectPath the projectPath to set
     */
    public void setProjectPath(IPath projectPath) {
        this.projectPath = projectPath;
    }

    /**
     * @return the projectPath
     */
    public IPath getProjectPath() {
        return projectPath;
    }
}

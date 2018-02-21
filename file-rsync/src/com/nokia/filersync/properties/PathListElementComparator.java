package com.nokia.filersync.properties;

import java.io.Serializable;
import java.util.Comparator;

public class PathListElementComparator implements Comparator<PathListElement>, Serializable {

    private static final long serialVersionUID = -6143935945692635274L;

    @Override
    public int compare(PathListElement o1, PathListElement o2) {
        if(!(o1 instanceof PathListElement) || !(o2 instanceof PathListElement)){
            return 0;
        }
        PathListElement path1 = (PathListElement) o1;
        PathListElement path2 = (PathListElement) o2;
        if(path1.getPath() != null && path2.getPath() != null){
            return path1.getPath().toString().compareTo(path2.getPath().toString());
        }
        return path1.toString().compareTo(path2.toString());
    }

}

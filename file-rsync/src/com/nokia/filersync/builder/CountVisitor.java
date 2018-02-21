package com.nokia.filersync.builder;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import com.nokia.filersync.FileRsyncPlugin;

public class CountVisitor implements IResourceDeltaVisitor, IResourceVisitor {
    private int count = 0;

    @Override
    public boolean visit(IResourceDelta delta) {
        count++;
        return true;
    }

    @Override
    public boolean visit(IResource resource) {
        count++;
        return true;
    }

	public int getCount() {
		return count;
	}

    public static int countDeltaElement(IResourceDelta delta, int visitorFlags) {
        CountVisitor visitor = new CountVisitor();
        try {
            delta.accept(visitor, visitorFlags);
        } catch (CoreException e) {
            FileRsyncPlugin.log("Exception counting elements in the delta: " + delta, e,
                    IStatus.ERROR);
        }
        return visitor.getCount();
    }
}

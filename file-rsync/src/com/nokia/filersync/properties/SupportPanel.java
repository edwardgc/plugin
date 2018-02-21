package com.nokia.filersync.properties;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import com.nokia.filersync.FileRsyncPlugin;

public class SupportPanel {
    static void createSupportLinks(Composite defPanel) {
        Group commonPanel = new Group(defPanel, SWT.NONE);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginWidth = 0;
        commonPanel.setLayout(layout);
        commonPanel.setLayoutData(gridData);
        commonPanel.setText("About");

        Label label = new Label(commonPanel, SWT.NONE);
        label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        label.setText("Feel free to support FileRsync plugin in the way you like:");

        Font font = JFaceResources.getFontRegistry().getBold(
                JFaceResources.getDialogFont().getFontData()[0].getName());

        Link link = new Link(commonPanel, SWT.NONE);
        link.setFont(font);
        link.setText(" - <a>Visit homepage on Github</a>");
        link.setToolTipText("You need just a sense of humor!");
        link.addListener (SWT.Selection, new Listener () {
            @Override
            public void handleEvent(Event event) {
                handleUrlClick("https://github.com/edwardgc/plugin");
            }
        });

        link = new Link(commonPanel, SWT.NONE);
        link.setFont(font);
        link.setText(" - <a>Report issue or feature request: Chen.A.Guo@nokia-sbell.com</a>");
        link.setToolTipText("Send mail!");
        link.addListener (SWT.Selection, new Listener () {
            @Override
            public void handleEvent(Event event) {
                handleUrlClick("mailto:Chen.A.Guo@nokia-sbell.com");
            }
        });
    }

    private static void handleUrlClick(final String urlStr) {
        try {
            IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
            IWebBrowser externalBrowser = support.getExternalBrowser();
            if(externalBrowser != null){
                externalBrowser.openURL(new URL(urlStr));
            } else {
                IWebBrowser browser = support.createBrowser(urlStr);
                if(browser != null){
                    browser.openURL(new URL(urlStr));
                }
            }
        } catch (PartInitException e) {
            FileRsyncPlugin.log("Failed to open url " + urlStr, e, IStatus.ERROR);
        } catch (MalformedURLException e) {
            FileRsyncPlugin.log("Failed to open url " + urlStr, e, IStatus.ERROR);
        }
    }
}

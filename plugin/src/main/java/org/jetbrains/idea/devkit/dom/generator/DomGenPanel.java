/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.idea.devkit.dom.generator;

import consulo.application.ApplicationPropertiesComponent;
import consulo.application.ui.wm.ApplicationIdeFocusManager;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileTypeDescriptor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;

/**
 * @author Konstantin Bulenkov
 */
public class DomGenPanel {
    private JPanel mainPanel;
    private JTextField myNamespace;
    private JTextField mySuperClass;
    private TextFieldWithBrowseButton mySchemaLocation;
    private JTextField myPackage;
    private TextFieldWithBrowseButton myOutputDir;
    private JTextArea mySkipSchemas;
    private JTextField myAuthor;
    private final Project myProject;

    public DomGenPanel(Project project) {
        myProject = project;
    }

    private void createUIComponents() {
        mySchemaLocation = new TextFieldWithBrowseButton();
        final String title = "Choose XSD or DTD schema";
        mySchemaLocation.addBrowseFolderListener(
            title,
            "Make sure there are only necessary schemes in directory where your XSD or DTD schema is located",
            myProject,
            new FileTypeDescriptor(title, "xsd", "dtd")
        );
        mySchemaLocation.getTextField().setEditable(false);
        mySchemaLocation.addActionListener(e -> {
            final File file = new File(mySchemaLocation.getText());
            if (file.exists() && file.getName().toLowerCase().endsWith(".xsd")) {
                final VirtualFile vf = LocalFileSystem.getInstance().findFileByIoFile(file);
                if (vf != null) {
                    final PsiFile psiFile = PsiManager.getInstance(myProject).findFile(vf);
                    if (psiFile instanceof XmlFile xmlFile) {
                        final XmlDocument xml = xmlFile.getDocument();
                        if (xml != null) {
                            final XmlTag rootTag = xml.getRootTag();
                            if (rootTag != null) {
                                String target = null;
                                ArrayList<String> ns = new ArrayList<>();
                                for (XmlAttribute attr : rootTag.getAttributes()) {
                                    if ("targetNamespace".equals(attr.getName())) {
                                        target = attr.getValue();
                                    }
                                    else if (attr.getName().startsWith("xmlns")) {
                                        ns.add(attr.getValue());
                                    }
                                }

                                ns.remove(target);
                                if (target != null) {
                                    myNamespace.setText(target);
                                }
                                mySkipSchemas.setText(StringUtil.join(ArrayUtil.toStringArray(ns), "\n"));
                            }
                        }
                    }
                }
            }
        });
        myOutputDir = new TextFieldWithBrowseButton();
        myOutputDir.addBrowseFolderListener("Select Output For Generated Files", "", myProject,
            FileChooserDescriptorFactory.getDirectoryChooserDescriptor("Output For Generated Files")
        );
    }

    public JComponent getComponent() {
        return mainPanel;
    }

    public NamespaceDesc getNamespaceDescriptor() {
        return new NamespaceDesc(
            myNamespace.getText().trim(),
            myPackage.getText().trim(),
            mySuperClass.getText().trim(),
            "",
            null,
            null,
            null,
            null
        );
    }

    public String getLocation() {
        return mySchemaLocation.getText();
    }

    public String getOutputDir() {
        return myOutputDir.getText();
    }

    private static final String PREFIX = "DomGenPanel.";

    public void restore() {
        myNamespace.setText(getValue("namespace", ""));
        myPackage.setText(getValue("package", "com.intellij.myframework.model"));
        mySchemaLocation.setText(getValue("schemaLocation", ""));
        mySuperClass.setText(getValue("superClass", "com.intellij.util.xml.DomElement"));
        myOutputDir.setText(getValue("output", ""));
        mySkipSchemas.setText(getValue(
            "skipSchemas",
            "http://www.w3.org/2001/XMLSchema\nhttp://www.w3.org/2001/XMLSchema-instance"
        ));
        myAuthor.setText(getValue("author", ""));
    }

    private static String getValue(String name, String defaultValue) {
        return ApplicationPropertiesComponent.getInstance().getOrInit(PREFIX + name, defaultValue);
    }

    private static void setValue(String name, String value) {
        ApplicationPropertiesComponent.getInstance().setValue(PREFIX + name, value);
    }

    public void saveAll() {
        setValue("namespace", myNamespace.getText());
        setValue("package", myPackage.getText());
        setValue("schemaLocation", mySchemaLocation.getText());
        setValue("superClass", mySuperClass.getText());
        setValue("output", myOutputDir.getText());
        setValue("skipSchemas", mySkipSchemas.getText());
        setValue("author", myAuthor.getText());
    }

    public boolean validate() {
        if (!new File(mySchemaLocation.getText()).exists()) {
            Messages.showErrorDialog(myProject, "Schema location doesn't exist", "Error");
            ApplicationIdeFocusManager.getInstance().getInstanceForProject(myProject).requestFocus(mySchemaLocation, true);
            return false;
        }

        if (!new File(myOutputDir.getText()).exists()) {
            Messages.showErrorDialog(myProject, "Output dir doesn't exist", "Error");
            ApplicationIdeFocusManager.getInstance().getInstanceForProject(myProject).requestFocus(myOutputDir, true);
            return false;
        }

        return true;
    }

    public String[] getSkippedSchemas() {
        final String schemes = mySkipSchemas.getText().replaceAll("\r\n", "\n").trim();
        if (schemes.length() > 0) {
            return schemes.split("\n");
        }
        return new String[0];
    }

    public String getAuthor() {
        return myAuthor.getText();
    }
}

/**
 * @author VISTALL
 * @since 2023-02-10
 */
open module consulo.devkit {
    requires consulo.application.api;
    requires consulo.application.content.api;
    requires consulo.base.icon.library;
    requires consulo.code.editor.api;
    requires consulo.component.api;
    requires consulo.configurable.api;
    requires consulo.datacontext.api;
    requires consulo.diff.api;
    requires consulo.disposer.api;
    requires consulo.document.api;
    requires consulo.execution.api;
    requires consulo.execution.debug.api;
    requires consulo.file.chooser.api;
    requires consulo.file.editor.api;
    requires consulo.file.template.api;
    requires consulo.find.api;
    requires consulo.index.io;
    requires consulo.language.api;
    requires consulo.language.editor.api;
    requires consulo.language.editor.refactoring.api;
    requires consulo.language.editor.ui.api;
    requires consulo.language.impl;
    requires consulo.localize.api;
    requires consulo.logging.api;
    requires consulo.module.api;
    requires consulo.module.content.api;
    requires consulo.module.ui.api;
    requires consulo.navigation.api;
    requires consulo.platform.api;
    requires consulo.process.api;
    requires consulo.project.api;
    requires consulo.project.ui.api;
    requires consulo.ui.api;
    requires consulo.ui.ex.api;
    requires consulo.ui.ex.awt.api;
    requires consulo.undo.redo.api;
    requires consulo.util.collection;
    requires consulo.util.io;
    requires consulo.util.jdom;
    requires consulo.util.lang;
    requires consulo.util.xml.serializer;
    requires consulo.virtual.file.system.api;

    requires consulo.ide.api;

    requires consulo.java;
    requires consulo.java.language.api;
    requires consulo.java.language.impl;
    requires consulo.java.execution.api;
    requires consulo.java.debugger.impl;
    requires consulo.java.coverage.impl;
    requires consulo.java.execution.impl;

    requires com.intellij.xml;
    requires com.intellij.xml.html.api;
    requires org.jetbrains.plugins.yaml;
    requires com.intellij.properties;
    requires org.jetbrains.idea.maven;

    requires org.yaml.snakeyaml;
    requires dtdparser;
    requires consulo.internal.jdi;
    requires com.palantir.javapoet;
    requires java.compiler;

    requires com.ibm.icu;

    requires xercesImpl;

    // TODO remove in future
    requires java.desktop;
    requires forms.rt;

    exports consulo.devkit;
    exports consulo.devkit.action;
    exports consulo.devkit.codeInsight.daemon;
    exports consulo.devkit.codeInsight.generation;
    exports consulo.devkit.dom;
    exports consulo.devkit.dom.impl;
    exports consulo.devkit.icon;
    exports consulo.devkit.inspections;
    exports consulo.devkit.inspections.inject;
    exports consulo.devkit.inspections.internal;
    exports consulo.devkit.inspections.requiredXAction;
    exports consulo.devkit.inspections.requiredXAction.stateResolver;
    exports consulo.devkit.inspections.util.service;
    exports consulo.devkit.inspections.valhalla;
    exports consulo.devkit.intentation;
    exports consulo.devkit.localize.folding;
    exports consulo.devkit.localize.index;
    exports consulo.devkit.localize.inspection;
    exports consulo.devkit.maven;
    exports consulo.devkit.module.extension;
    exports consulo.devkit.requires.dom;
    exports consulo.devkit.run;
    exports consulo.devkit.util;
    exports org.jetbrains.idea.devkit;
    exports org.jetbrains.idea.devkit.build;
    exports org.jetbrains.idea.devkit.dom;
    exports org.jetbrains.idea.devkit.dom.generator;
    exports org.jetbrains.idea.devkit.dom.impl;
    exports org.jetbrains.idea.devkit.inspections;
    exports org.jetbrains.idea.devkit.inspections.internal;
    exports org.jetbrains.idea.devkit.inspections.quickfix;
    exports org.jetbrains.idea.devkit.navigation;
    exports org.jetbrains.idea.devkit.run;
    exports org.jetbrains.idea.devkit.sdk;
    exports org.jetbrains.idea.devkit.util;
}

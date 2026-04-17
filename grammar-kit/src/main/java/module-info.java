/**
 * @author VISTALL
 * @since 2023-02-10
 */
module consulo.devkit.grammar.kit {
    requires consulo.application.api;
    requires consulo.code.editor.api;
    requires consulo.color.scheme.api;
    requires consulo.component.api;
    requires consulo.datacontext.api;
    requires consulo.document.api;
    requires consulo.file.chooser.api;
    requires consulo.file.editor.api;
    requires consulo.language.api;
    requires consulo.language.editor.api;
    requires consulo.language.editor.refactoring.api;
    requires consulo.language.editor.ui.api;
    requires consulo.language.impl;
    requires consulo.localize.api;
    requires consulo.logging.api;
    requires consulo.module.api;
    requires consulo.navigation.api;
    requires consulo.base.icon.library;
    requires consulo.project.api;
    requires consulo.project.ui.api;
    requires consulo.ui.api;
    requires consulo.ui.ex.api;
    requires consulo.ui.ex.awt.api;
    requires consulo.usage.api;
    requires consulo.util.collection;
    requires consulo.util.dataholder;
    requires consulo.util.io;
    requires consulo.util.lang;
    requires consulo.virtual.file.system.api;
    requires asm;
    requires velocity.engine.core;

    requires consulo.devkit.grammar.kit.core;
    requires consulo.java.language.api;
    requires consulo.java.debugger.impl;
    requires consulo.java;
    requires consulo.internal.jdi;

    // TODO remove in future
    requires java.desktop;

    exports consulo.devkit.grammarKit.impl;
    exports org.intellij.grammar.impl;
    exports org.intellij.grammar.impl.actions;
    exports org.intellij.grammar.impl.debugger;
    exports org.intellij.grammar.impl.editor;
    exports org.intellij.grammar.impl.inspection;
    exports org.intellij.grammar.impl.intention;
    exports org.intellij.grammar.impl.java;
    exports org.intellij.grammar.impl.livePreview;
    exports org.intellij.grammar.impl.psi.impl;
    exports org.intellij.grammar.impl.refactor;
}

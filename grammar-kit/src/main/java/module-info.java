/**
 * @author VISTALL
 * @since 10/02/2023
 */
module consulo.devkit.grammar.kit {
    requires consulo.devkit.grammar.kit.core;
    requires consulo.java.language.api;
    requires consulo.java.debugger.impl;
    requires consulo.java;
    requires consulo.internal.jdi;

    // TODO remove in future
    requires java.desktop;
    requires consulo.ide.impl;

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
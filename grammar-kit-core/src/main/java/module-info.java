/**
 * @author VISTALL
 * @since 2023-02-10
 */
module consulo.devkit.grammar.kit.core {
    requires consulo.language.api;
    requires consulo.language.impl;

    exports consulo.devkit.grammarKit.generator;
    exports consulo.devkit.grammarKit.icon;
    exports consulo.devkit.grammarKit.localize;
    exports org.intellij.grammar;
    exports org.intellij.grammar.analysis;
    exports org.intellij.grammar.config;
    exports org.intellij.grammar.generator;
    exports org.intellij.grammar.java;
    exports org.intellij.grammar.parser;
    exports org.intellij.grammar.psi;
    exports org.intellij.grammar.psi.impl;
}
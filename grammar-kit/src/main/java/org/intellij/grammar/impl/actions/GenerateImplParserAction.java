package org.intellij.grammar.impl.actions;

import consulo.annotation.component.ActionImpl;
import consulo.devkit.grammarKit.generator.GenerateTarget;
import consulo.devkit.grammarKit.localize.BnfLocalize;

import java.util.EnumSet;

/**
 * @author VISTALL
 * @since 2026-02-13
 */
@ActionImpl(id = "grammar.GenerateParser.Impl")
public class GenerateImplParserAction extends GenerateAction {
    public GenerateImplParserAction() {
        super(BnfLocalize.actionTextGenerateParserImpl(), EnumSet.of(GenerateTarget.Impl));
    }
}

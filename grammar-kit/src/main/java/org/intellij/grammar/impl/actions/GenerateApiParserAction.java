package org.intellij.grammar.impl.actions;

import consulo.annotation.component.ActionImpl;
import consulo.devkit.grammarKit.generator.GenerateTarget;

import java.util.EnumSet;

/**
 * @author VISTALL
 * @since 2026-02-13
 */
@ActionImpl(id = "grammar.GenerateParser.API")
public class GenerateApiParserAction extends GenerateAction {
    public GenerateApiParserAction() {
        super("Generate Parser [API]", EnumSet.of(GenerateTarget.API));
    }
}

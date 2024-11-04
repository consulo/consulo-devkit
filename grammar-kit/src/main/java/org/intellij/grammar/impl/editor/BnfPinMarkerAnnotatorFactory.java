package org.intellij.grammar.impl.editor;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.language.Language;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.AnnotatorFactory;
import jakarta.annotation.Nullable;
import org.intellij.grammar.BnfLanguage;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2023-02-11
 */
@ExtensionImpl
public class BnfPinMarkerAnnotatorFactory implements AnnotatorFactory, DumbAware {
    @Nullable
    @Override
    public Annotator createAnnotator() {
        return new BnfPinMarkerAnnotator();
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return BnfLanguage.INSTANCE;
    }
}

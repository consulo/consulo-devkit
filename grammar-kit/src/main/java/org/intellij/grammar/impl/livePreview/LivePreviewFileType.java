/*
 * Copyright 2011-present Greg Shrago
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

package org.intellij.grammar.impl.livePreview;

import consulo.devkit.grammarKit.icon.GrammarKitIconGroup;
import consulo.language.file.LanguageFileType;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.fileType.FileType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author gregsh
 */
public class LivePreviewFileType extends LanguageFileType {
    public static final FileType INSTANCE = new LivePreviewFileType();

    protected LivePreviewFileType() {
        super(LivePreviewLanguage.BASE_INSTANCE);
    }

    @Nonnull
    @Override
    public String getId() {
        return "Grammar Live Preview";
    }

    @Nonnull
    @Override
    public LocalizeValue getDescription() {
        return LocalizeValue.localizeTODO("Grammar Live Preview");
    }

    @Nonnull
    @Override
    public String getDefaultExtension() {
        return "preview";
    }

    @Nullable
    @Override
    public Image getIcon() {
        return GrammarKitIconGroup.grammarfile();
    }
}

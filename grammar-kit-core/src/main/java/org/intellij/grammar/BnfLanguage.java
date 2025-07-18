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
package org.intellij.grammar;

import consulo.devkit.grammarKit.localize.BnfLocalize;
import consulo.language.Language;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

/**
 * Created by IntelliJ IDEA.
 * User: gregory
 * Date: 13.07.11
 * Time: 22:44
 */
public class BnfLanguage extends Language {
    public static final BnfLanguage INSTANCE = new BnfLanguage();

    protected BnfLanguage() {
        super("BNF");
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return BnfLocalize.bnfLanguageDisplayName();
    }
}

/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package org.jetbrains.idea.devkit.dom.impl;

import com.intellij.java.impl.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider;
import com.intellij.java.impl.util.xml.ExtendClass;
import com.intellij.java.impl.util.xml.PsiClassConverter;
import com.intellij.java.language.psi.PsiClass;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.xml.util.xml.ConvertContext;
import consulo.xml.util.xml.GenericDomValue;

import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
public class PluginPsiClassConverter extends PsiClassConverter {
    @Override
    protected GlobalSearchScope getScope(@Nonnull ConvertContext context) {
        return GlobalSearchScope.allScope(context.getProject());
    }

    @Override
    protected JavaClassReferenceProvider createClassReferenceProvider(
        GenericDomValue<PsiClass> genericDomValue,
        ConvertContext context,
        ExtendClass extendClass
    ) {
        final JavaClassReferenceProvider provider = super.createClassReferenceProvider(genericDomValue, context, extendClass);
        provider.setOption(JavaClassReferenceProvider.JVM_FORMAT, Boolean.TRUE);
        provider.setOption(JavaClassReferenceProvider.ALLOW_DOLLAR_NAMES, Boolean.TRUE);
        return provider;
    }
}

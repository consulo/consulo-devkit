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

package org.intellij.grammar.java;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.language.impl.psi.FakePsiElement;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReferenceProvider;
import consulo.navigation.NavigationItem;
import consulo.util.collection.SmartList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * @author gregsh
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class JavaHelper {
    public static class TypeParameterInfo {
        private final String name;
        private final List<String> extendsList;

        public TypeParameterInfo(@Nonnull String name) {
            this.name = name;
            this.extendsList = new SmartList<>();
        }

        public String getName() {
            return name;
        }

        public List<String> getExtendsList() {
            return extendsList;
        }
    }

    protected static class MyElement<T> extends FakePsiElement implements NavigatablePsiElement {
        private final T myDelegate;

        protected MyElement(T delegate) {
            myDelegate = delegate;
        }

        @Override
        public PsiElement getParent() {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            MyElement element = (MyElement)o;

            if (!myDelegate.equals(element.myDelegate)) {
                return false;
            }

            return true;
        }

        public T getDelegate() {
            return myDelegate;
        }

        @Override
        public int hashCode() {
            return myDelegate.hashCode();
        }

        @Override
        public String toString() {
            return myDelegate.toString();
        }
    }

    public enum MethodType {
        STATIC,
        INSTANCE,
        CONSTRUCTOR
    }

    @Nonnull
    public static JavaHelper getJavaHelper(@Nonnull PsiElement context) {
        return context.getProject().getInstance(JavaHelper.class);
    }

    @Nullable
    public NavigatablePsiElement findClass(@Nullable String className) {
        return null;
    }

    @Nonnull
    public List<NavigatablePsiElement> findClassMethods(
        @Nullable String version,
        @Nullable String className,
        @Nonnull MethodType methodType,
        @Nullable String methodName,
        int paramCount,
        String... paramTypes
    ) {
        return Collections.emptyList();
    }

    public List<TypeParameterInfo> getGenericParameters(NavigatablePsiElement method) {
        return Collections.emptyList();
    }

    public List<String> getExceptionList(NavigatablePsiElement method) {
        return Collections.emptyList();
    }

    @Nullable
    public String getSuperClassName(@Nullable String className) {
        return null;
    }

    @Nonnull
    public List<String> getMethodTypes(String version, @Nullable NavigatablePsiElement method) {
        return Collections.emptyList();
    }

    @Nonnull
    public String getDeclaringClass(@Nullable NavigatablePsiElement method) {
        return "";
    }

    public boolean isPublic(@Nullable NavigatablePsiElement element) {
        return true;
    }

    @Nonnull
    public List<String> getParameterAnnotations(@Nullable NavigatablePsiElement method, int paramIndex) {
        return Collections.emptyList();
    }

    @Nonnull
    public List<String> getAnnotations(@Nullable NavigatablePsiElement element) {
        return Collections.emptyList();
    }

    @Nullable
    public PsiReferenceProvider getClassReferenceProvider() {
        return null;
    }

    @Nullable
    public NavigationItem findPackage(@Nullable String packageName) {
        return null;
    }

    protected static boolean acceptsName(@Nullable String expected, @Nullable String actual) {
        return "*".equals(expected) || expected != null && expected.equals(actual);
    }
}

/*
 * Copyright 2013-2017 consulo.io
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

package consulo.devkit.inspections.internal;

import com.intellij.java.language.psi.JavaElementVisitor;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiMethodCallExpression;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.localize.DevKitLocalize;
import consulo.devkit.util.PluginModuleUtil;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.util.collection.MultiMap;
import jakarta.annotation.Nonnull;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;

import java.util.Collection;

/**
 * @author VISTALL
 * @since 2017-08-23
 */
@ExtensionImpl
public class PlatformErrorInspection extends InternalInspection {
    private final MultiMap<String, String> myRestrictedMethodList = MultiMap.create();

    public PlatformErrorInspection() {
        myRestrictedMethodList.putValue(System.class.getName(), "getProperties");
        myRestrictedMethodList.putValue(System.class.getName(), "getProperty");
        myRestrictedMethodList.putValue(System.class.getName(), "getenv");
        myRestrictedMethodList.putValue(System.class.getName(), "lineSeparator");
        myRestrictedMethodList.putValue(System.class.getName(), "setProperties");
        myRestrictedMethodList.putValue(System.class.getName(), "setProperty");
        myRestrictedMethodList.putValue(System.class.getName(), "clearProperty");

        myRestrictedMethodList.putValue(Boolean.class.getName(), "getBoolean");
        myRestrictedMethodList.putValue(Integer.class.getName(), "getInteger");
        myRestrictedMethodList.putValue(Long.class.getName(), "getLong");
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return DevKitLocalize.platformErrorInspectionDisplayName();
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
        if (PluginModuleUtil.searchClassInFileUseScope(holder.getFile(), Platform.class.getName()) == null) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        return new JavaElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitMethodCallExpression(@Nonnull PsiMethodCallExpression expression) {
                PsiMethod method = expression.resolveMethod();
                if (method != null && myRestrictedMethodList.containsScalarValue(method.getName())) {
                    PsiClass containingClass = method.getContainingClass();
                    if (containingClass != null) {
                        Collection<String> strings = myRestrictedMethodList.get(containingClass.getQualifiedName());
                        if (strings.contains(method.getName())) {
                            holder.newProblem(DevKitLocalize.platformErrorInspectionMessage())
                                .range(expression, expression.getMethodExpression().getRangeInElement())
                                .highlightType(ProblemHighlightType.GENERIC_ERROR)
                                .create();
                        }
                    }
                }
            }
        };
    }
}

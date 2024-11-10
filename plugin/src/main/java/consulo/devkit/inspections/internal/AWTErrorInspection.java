/*
 * Copyright 2013-2016 consulo.io
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

import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.util.PsiUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.localize.DevKitLocalize;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;

/**
 * @author VISTALL
 * @since 2016-06-22
 */
@ExtensionImpl
public class AWTErrorInspection extends InternalInspection {
    private static final String[] ourErrorPackages = {
        "java.awt",
        "javax.swing"
    };

    @Nonnull
    @Override
    public String getDisplayName() {
        return DevKitLocalize.awtErrorInspectionDisplayName().get();
    }

    @Override
    public boolean isEnabledByDefault() {
        return false;
    }

    @Override
    public PsiElementVisitor buildInternalVisitor(@Nonnull final ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitTypeElement(@Nonnull PsiTypeElement type) {
                checkType(type, type.getType());
            }

            @Override
            public void visitNewExpression(@Nonnull PsiNewExpression expression) {
                final PsiJavaCodeReferenceElement classReference = expression.getClassReference();
                if (classReference == null) {
                    return;
                }
                checkType(classReference, expression.getType());
            }

            private void checkType(PsiElement owner, PsiType psiType) {
                PsiClass psiClass = PsiUtil.resolveClassInType(psiType);
                if (psiClass != null) {
                    final String qualifiedName = psiClass.getQualifiedName();
                    if (qualifiedName == null) {
                        return;
                    }
                    for (String errorPackage : ourErrorPackages) {
                        if (StringUtil.startsWith(qualifiedName, errorPackage)) {
                            holder.newProblem(DevKitLocalize.awtErrorInspectionMessage())
                                .range(owner)
                                .create();
                        }
                    }
                }
            }
        };
    }

    @RequiredReadAction
    @Override
    protected boolean isAllowed(ProblemsHolder holder) {
        Module module = ModuleUtilCore.findModuleForPsiElement(holder.getFile());
        if (module == null) {
            return false;
        }

        // allow AWT & Swing inside desktop modules
        return !module.getName().startsWith("consulo-desktop-awt-");
    }
}

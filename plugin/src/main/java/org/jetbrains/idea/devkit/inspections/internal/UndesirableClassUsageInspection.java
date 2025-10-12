/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package org.jetbrains.idea.devkit.inspections.internal;

import com.intellij.java.language.psi.JavaElementVisitor;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiJavaCodeReferenceElement;
import com.intellij.java.language.psi.PsiNewExpression;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.query.QueryExecutor;
import consulo.devkit.localize.DevKitLocalize;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import consulo.project.util.query.QueryExecutorBase;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.table.JBTable;
import consulo.ui.ex.awt.tree.Tree;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

@ExtensionImpl
public class UndesirableClassUsageInspection extends InternalInspection {
    private static final Map<String, String> CLASSES = new HashMap<>();

    static {
        CLASSES.put(JList.class.getName(), JBList.class.getName());
        CLASSES.put(JTable.class.getName(), JBTable.class.getName());
        CLASSES.put(JTree.class.getName(), Tree.class.getName());
        CLASSES.put(JScrollPane.class.getName(), JBScrollPane.class.getName());
        CLASSES.put(QueryExecutor.class.getName(), QueryExecutorBase.class.getName());
        CLASSES.put(BufferedImage.class.getName(), "UIUtil.createImage()");
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return DevKitLocalize.undesirableClassUsageInspectionDisplayName();
    }

    @Override
    @Nonnull
    public PsiElementVisitor buildInternalVisitor(@Nonnull final ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitNewExpression(@Nonnull PsiNewExpression expression) {
                PsiJavaCodeReferenceElement ref = expression.getClassReference();
                if (ref == null) {
                    return;
                }

                if (!(ref.resolve() instanceof PsiClass psiClass)) {
                    return;
                }

                String name = psiClass.getQualifiedName();
                if (name == null) {
                    return;
                }

                String replacement = CLASSES.get(name);
                if (replacement == null) {
                    return;
                }

                holder.newProblem(DevKitLocalize.undesirableClassUsageInspectionMessage(replacement))
                    .range(expression)
                    .highlightType(ProblemHighlightType.LIKE_DEPRECATED)
                    .create();
            }
        };
    }
}

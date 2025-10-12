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

package consulo.devkit.inspections;

import com.intellij.java.analysis.impl.codeInsight.intention.AddAnnotationFix;
import com.intellij.java.analysis.impl.codeInspection.AnnotateMethodFix;
import com.intellij.java.indexing.search.searches.OverridingMethodsSearch;
import com.intellij.java.language.psi.JavaElementVisitor;
import com.intellij.java.language.psi.PsiIdentifier;
import com.intellij.java.language.psi.PsiMethod;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.query.Query;
import consulo.devkit.inspections.requiredXAction.CallStateType;
import consulo.devkit.localize.DevKitLocalize;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import consulo.util.lang.StringUtil;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2015-05-18
 */
@ExtensionImpl
public class PlaceXActionAnnotationInspection extends InternalInspection {
    private static class MyAnnotateMethodFix extends AnnotateMethodFix {
        public MyAnnotateMethodFix(String fqn, String... annotationsToRemove) {
            super(fqn, annotationsToRemove);
        }

        @Nonnull
        @Override
        protected String getPreposition() {
            return "as";
        }

        @Override
        protected boolean annotateOverriddenMethods() {
            return true;
        }

        @Override
        protected boolean annotateSelf() {
            return false;
        }
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return DevKitLocalize.placeXactionAnnotationInspectionDisplayName();
    }

    @Nonnull
    @Override
    public PsiElementVisitor buildInternalVisitor(@Nonnull final ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitMethod(@Nonnull PsiMethod method) {
                if (method.isConstructor()) {
                    return;
                }

                PsiIdentifier nameIdentifier = method.getNameIdentifier();
                if (nameIdentifier == null) {
                    return;
                }

                CallStateType selfActionType = CallStateType.findSelfActionType(method);
                if (selfActionType != CallStateType.NONE) {
                    Query<PsiMethod> query = OverridingMethodsSearch.search(method);
                    for (PsiMethod itMethod : query) {
                        if (CallStateType.findSelfActionType(itMethod) == CallStateType.NONE) {
                            String actionClass = selfActionType.getActionClass();
                            holder.newProblem(DevKitLocalize.placeXactionAnnotationInspectionMessageForOverridden(
                                    StringUtil.getShortName(actionClass)
                                ))
                                .range(nameIdentifier)
                                .withFix(new MyAnnotateMethodFix(actionClass))
                                .create();
                            break;
                        }
                    }
                }
                else {
                    PsiMethod[] superMethods = method.findSuperMethods();
                    for (PsiMethod superMethod : superMethods) {
                        CallStateType superActionType = CallStateType.findSelfActionType(superMethod);
                        if (superActionType != CallStateType.NONE) {
                            String actionClass = superActionType.getActionClass();
                            holder.newProblem(DevKitLocalize.placeXactionAnnotationInspectionMessage(StringUtil.getShortName(actionClass)))
                                .range(nameIdentifier)
                                .withFix(new AddAnnotationFix(actionClass, method))
                                .create();
                        }
                    }
                }
            }
        };
    }
}

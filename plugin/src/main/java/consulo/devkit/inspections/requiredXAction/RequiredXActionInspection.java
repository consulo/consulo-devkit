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
package consulo.devkit.inspections.requiredXAction;

import com.intellij.java.analysis.impl.codeInspection.AnnotateMethodFix;
import com.intellij.java.language.psi.*;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.inspections.requiredXAction.stateResolver.*;
import consulo.devkit.localize.DevKitLocalize;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import consulo.util.lang.StringUtil;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2015-02-18
 */
@ExtensionImpl
public class RequiredXActionInspection extends InternalInspection {
    public static class RequiredXActionVisitor extends JavaElementVisitor {
        private static final CompositeStateResolver CALL_EXPRESSION_STATE_RESOLVER =
            new CompositeStateResolver(LambdaStateResolver.INSTANCE, AnonymousClassStateResolver.INSTANCE);

        private final ProblemsHolder myHolder;

        public RequiredXActionVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        @RequiredReadAction
        public void visitMethodReferenceExpression(PsiMethodReferenceExpression expression) {
            if (expression.resolve() instanceof PsiMethod method) {
                validate(expression, method, MethodReferenceResolver.INSTANCE);
            }
        }

        @Override
        @RequiredReadAction
        public void visitCallExpression(PsiCallExpression expression) {
            PsiMethod method = expression.resolveMethod();
            if (method != null) {
                validate(expression, method, CALL_EXPRESSION_STATE_RESOLVER);
            }
        }

        @RequiredReadAction
        private void validate(PsiExpression expression, PsiMethod method, StateResolver stateResolver) {
            CallStateType actionType = CallStateType.findActionType(method);
            if (actionType != CallStateType.NONE && stateResolver.resolveState(actionType, expression) == Boolean.FALSE) {
                reportError(expression, actionType);
            }
        }

        private void reportError(@Nonnull PsiExpression expression, @Nonnull CallStateType type) {
            LocalizeValue text = switch (type) {
                case READ, WRITE ->
                    DevKitLocalize.inspectionsAnnotation0IsRequiredAtOwnerOrAppRun(StringUtil.capitalize(type.name().toLowerCase()));
                case UI_ACCESS -> DevKitLocalize.inspectionsAnnotation0IsRequiredAtOwnerOrAppRunUi();
                default -> throw new IllegalArgumentException();
            };
            myHolder.newProblem(text)
                .range(expression)
                .withFix(new AnnotateMethodFix(type.getActionClass()))
                .create();
        }
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return DevKitLocalize.invocationStateValidateInspectionDisplayName();
    }

    @Nonnull
    @Override
    public PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
        return new RequiredXActionVisitor(holder);
    }
}

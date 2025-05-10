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

import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiMethod;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.language.editor.WriteCommandAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.Module;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2016-06-22
 */
public enum CallStateType {
    NONE(null, null),
    @SuppressWarnings("deprecation")
    READ(
        RequiredReadAction.class.getName(),
        new AcceptableMethodCallCheck(Application.class, Set.of("runReadAction")),
        new AcceptableMethodCallCheck(ReadAction.class, Set.of("run", "compute"))
    ) {
        @Override
        @RequiredReadAction
        public boolean isAcceptableActionType(@Nonnull CallStateType type, @Nonnull PsiElement context) {
            if (type == READ) {
                return true;
            }
            // in new data lock model ui thread not provide read lock
            if (type == UI_ACCESS) {
                return !isNewDataLockModel(context);
            }
            return type == WRITE;
        }

        @RequiredReadAction
        private boolean isNewDataLockModel(PsiElement context) {
            Module module = context.getModule();
            //noinspection SimplifiableIfStatement
            if (module == null) {
                return false;
            }
            return JavaPsiFacade.getInstance(context.getProject())
                .findClass("consulo.application.concurrent.DataLock", GlobalSearchScope.moduleWithDependenciesScope(module)) != null;
        }
    },
    @SuppressWarnings("deprecation")
    WRITE(
        RequiredWriteAction.class.getName(),
        new AcceptableMethodCallCheck(Application.class, Set.of("runWriteAction")),
        new AcceptableMethodCallCheck(WriteAction.class, Set.of("run", "compute")),
        new AcceptableMethodCallCheck(WriteCommandAction.class, Set.of("runWriteCommandAction"))
    ),
    UI_ACCESS(
        RequiredUIAccess.class.getName(),
        new AcceptableMethodCallCheck(UIAccess.class, Set.of("give", "giveIfNeed", "giveAndWait", "giveAndWaitIfNeed")),
        new AcceptableMethodCallCheck(Application.class, Set.of("invokeLater", "invokeAndWait")),
        new AcceptableMethodCallCheck(UIUtil.class, Set.of("invokeAndWaitIfNeeded", "invokeLaterIfNeeded")),
        new AcceptableMethodCallCheck(SwingUtilities.class, Set.of("invokeAndWait", "invokeLater"))
    ) {
        @Override
        public boolean isAcceptableActionType(@Nonnull CallStateType type, @Nonnull PsiElement context) {
            // write actions required call from dispatch thread, and it inherit dispatch state
            return type == UI_ACCESS || type == WRITE;
        }
    };

    @Nullable
    private final String myActionClass;
    @Nonnull
    private final AcceptableMethodCallCheck[] myAcceptableMethodCallChecks;

    @SuppressWarnings("NullableProblems")
    CallStateType(@Nullable String actionClass, AcceptableMethodCallCheck... methodCallChecks) {
        myActionClass = actionClass;
        myAcceptableMethodCallChecks = methodCallChecks;
    }

    @Nonnull
    public static CallStateType findSelfActionType(@Nonnull PsiMethod method) {
        for (CallStateType actionType : values()) {
            String actionClass = actionType.myActionClass;
            if (actionClass == null) {
                continue;
            }

            if (AnnotationUtil.isAnnotated(method, actionClass, 0)) {
                return actionType;
            }
        }
        return NONE;
    }

    @Nonnull
    public AcceptableMethodCallCheck[] getAcceptableMethodCallChecks() {
        return myAcceptableMethodCallChecks;
    }

    @Nonnull
    public String getActionClass() {
        assert myActionClass != null;
        return myActionClass;
    }

    @Nonnull
    public static CallStateType findActionType(@Nonnull PsiMethod method) {
        for (CallStateType actionType : values()) {
            String actionClass = actionType.myActionClass;
            if (actionClass == null) {
                continue;
            }

            if (AnnotationUtil.isAnnotated(method, actionClass, 0)) {
                return actionType;
            }
        }
        return NONE;
    }

    public boolean isAcceptableActionType(@Nonnull CallStateType type, @Nonnull PsiElement context) {
        return type == this;
    }
}

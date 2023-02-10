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

package consulo.devkit.inspections.requiredXAction.stateResolver;

import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.psi.*;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.function.CommonProcessors;
import consulo.application.util.function.Computable;
import consulo.application.util.function.ThrowableComputable;
import consulo.devkit.inspections.requiredXAction.AcceptableMethodCallCheck;
import consulo.devkit.inspections.requiredXAction.CallStateType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.search.ReferencesSearch;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.function.ThrowableRunnable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 01-Oct-16
 */
public abstract class StateResolver {
  @Nullable
  @RequiredReadAction
  public abstract Boolean resolveState(CallStateType actionType, PsiExpression expression);

  protected static Map<String, Class[]> ourInterfaces = new HashMap<String, Class[]>() {
    {
      put("compute", new Class[]{
        Computable.class,
        ThrowableComputable.class
      });
      put("run", new Class[]{
        Runnable.class,
        ThrowableRunnable.class
      });
    }
  };

  protected static boolean resolveByMaybeParameterListOrVariable(PsiElement maybeParameterListOrVariable, CallStateType actionType) {
    // Runnable run = new Runnable() {};
    // ApplicationManager.getApplication().runReadAction(run);
    if (maybeParameterListOrVariable instanceof PsiVariable) {
      CommonProcessors.CollectProcessor<PsiReference> processor = new CommonProcessors.CollectProcessor<>();
      ReferencesSearch.search(maybeParameterListOrVariable).forEach(processor);

      Collection<PsiReference> results = processor.getResults();
      if (results.isEmpty()) {
        return false;
      }

      boolean weFoundRunAction = false;
      for (PsiReference result : results) {
        if (result instanceof PsiReferenceExpression) {
          PsiElement maybeExpressionList = ((PsiReferenceExpression)result).getParent();
          if (maybeExpressionList instanceof PsiExpressionList) {
            if (acceptActionTypeFromCall((PsiExpressionList)maybeExpressionList, actionType)) {
              weFoundRunAction = true;
              break;
            }
          }
        }
      }

      if (weFoundRunAction) {
        return true;
      }
    }
    // ApplicationManager.getApplication().runReadAction(new Runnable() {});
    else if (maybeParameterListOrVariable instanceof PsiExpressionList) {
      if (acceptActionTypeFromCall((PsiExpressionList)maybeParameterListOrVariable, actionType)) {
        return true;
      }
    }
    return false;
  }

  protected static boolean acceptActionTypeFromCall(@Nonnull PsiExpressionList expressionList, @Nonnull CallStateType actionType) {
    for (CallStateType type : CallStateType.values()) {
      if (actionType.isAcceptableActionType(type)) {
        PsiElement parent = expressionList.getParent();

        for (AcceptableMethodCallCheck acceptableMethodCallCheck : type.getAcceptableMethodCallChecks()) {
          if (acceptableMethodCallCheck.accept(parent)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  @RequiredReadAction
  protected static boolean isAllowedFunctionCall(@Nonnull PsiFunctionalExpression functionalExpression, @Nonnull CallStateType actionType) {
    PsiElement superParent = functionalExpression.getParent();
    if (superParent instanceof PsiVariable) {
      return checkVariableType(actionType, (PsiVariable)superParent, ((PsiVariable)superParent).getType());
    }
    else {
      if (!(superParent instanceof PsiExpressionList)) {
        return false;
      }

      if (!(superParent.getParent() instanceof PsiMethodCallExpression)) {
        return false;
      }

      PsiExpression[] expressions = ((PsiExpressionList)superParent).getExpressions();
      int i = ArrayUtil.indexOf(expressions, functionalExpression);
      if (i == -1) {
        return false;
      }

      PsiElement target = ((PsiMethodCallExpression)superParent.getParent()).getMethodExpression().resolve();
      if (!(target instanceof PsiParameterListOwner)) {
        return false;
      }

      PsiParameter[] parameters = ((PsiParameterListOwner)target).getParameterList().getParameters();
      if (i >= parameters.length) {
        return false;
      }

      PsiParameter parameter = parameters[i];

      PsiType type = parameter.getType();
      if (checkVariableType(actionType, parameter, type)) {
        return true;
      }
    }
    return false;
  }

  private static boolean checkVariableType(@Nonnull CallStateType actionType, PsiVariable variable, PsiType type) {
    if (type instanceof PsiClassType) {
      PsiClass psiClass = ((PsiClassType)type).resolve();
      if (psiClass != null && psiClass.isInterface()) {
        // check if target variable type can use lambda
        List<HierarchicalMethodSignature> signatureList = LambdaUtil.findFunctionCandidates(psiClass);
        if (signatureList != null && signatureList.size() == 1) {
          HierarchicalMethodSignature signature = signatureList.get(0);

          for (CallStateType callStateType : CallStateType.values()) {
            if (actionType.isAcceptableActionType(callStateType)) {
              // if parameter of method is annotated - or annotated lambda abstract method
              if (AnnotationUtil.isAnnotated(variable,
                                             callStateType.getActionClass(),
                                             false) || AnnotationUtil.isAnnotated(signature.getMethod(),
                                                                                  callStateType.getActionClass(),
                                                                                  false)) {
                return true;
              }
            }
          }
        }
      }
    }
    return false;
  }
}

/*
 * Copyright 2013-2016 must-be.org
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;
import org.mustbe.consulo.RequiredReadAction;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;

/**
 * @author VISTALL
 * @since 22-Jun-16
 */
public class AWTErrorInspection extends InternalInspection
{
	private static final String[] ourErrorPackages = {
			"java.awt",
			"javax.swing"
	};

	@Override
	public PsiElementVisitor buildInternalVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly)
	{
		return new JavaElementVisitor()
		{
			@Override
			public void visitTypeElement(PsiTypeElement type)
			{
				checkType(type, type.getType());
			}

			@Override
			public void visitNewExpression(PsiNewExpression expression)
			{
				final PsiJavaCodeReferenceElement classReference = expression.getClassReference();
				if(classReference == null)
				{
					return;
				}
				checkType(classReference, expression.getType());
			}

			private void checkType(PsiElement owner, PsiType psiType)
			{
				if(psiType instanceof PsiClassType)
				{
					final PsiClass psiClass = ((PsiClassType) psiType).resolve();
					if(psiClass != null)
					{
						final String qualifiedName = psiClass.getQualifiedName();
						if(qualifiedName == null)
						{
							return;
						}
						for(String errorPackage : ourErrorPackages)
						{
							if(StringUtil.startsWith(qualifiedName, errorPackage))
							{
								holder.registerProblem(owner, "AWT & Swing implementation can not be used. Please visit guide for writing UI");
							}
						}
					}
				}
			}
		};
	}

	@RequiredReadAction
	@Override
	protected boolean isAllowed(ProblemsHolder holder)
	{
		if(isAllowedByDefault())
		{
			return true;
		}

		if(!super.isAllowed(holder))
		{
			return false;
		}

		Module module = ModuleUtilCore.findModuleForPsiElement(holder.getFile());
		assert module != null;
		final Module[] dependencies = ModuleRootManager.getInstance(module).getDependencies(false);
		for(Module dependency : dependencies)
		{
			// module-marker for desktop impl
			if(dependency.getName().equals("desktop-ui-impl"))
			{
				return false;
			}
		}
		return true;
	}
}

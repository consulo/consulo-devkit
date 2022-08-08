/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.idea.devkit.inspections;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

public enum DescriptionType
{
	INTENTION(Set.of("com.intellij.codeInsight.intention.IntentionAction", "consulo.language.editor.intention.IntentionAction"), "intentionDescriptions", true),
	INSPECTION(Set.of("com.intellij.codeInspection.InspectionProfileEntry", "consulo.language.editor.inspection.scheme.InspectionProfileEntry"),
			"inspectionDescriptions", false),
	POSTFIX_TEMPLATES(Set.of("com.intellij.codeInsight.template.postfix.templates.PostfixTemplate", "consulo.language.editor.postfixTemplate.PostfixTemplate"),
			"postfixTemplates", true);

	private final Set<String> myClassNames;
	private final String myDescriptionFolder;
	private final boolean myFixedDescriptionFilename;

	DescriptionType(Set<String> classes, String descriptionFolder, boolean fixedDescriptionFilename)
	{
		myClassNames = classes;
		myDescriptionFolder = descriptionFolder;
		myFixedDescriptionFilename = fixedDescriptionFilename;
	}

	@Nonnull
	public Set<String> getClassNames()
	{
		return myClassNames;
	}

	public String getDescriptionFolder()
	{
		return myDescriptionFolder;
	}

	public boolean isFixedDescriptionFilename()
	{
		return myFixedDescriptionFilename;
	}

	@Nullable
	public PsiClass findClass(@Nonnull Project project, @Nonnull GlobalSearchScope scope)
	{
		JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
		for(String className : myClassNames)
		{
			PsiClass aClass = psiFacade.findClass(className, scope);
			if(aClass != null)
			{
				return aClass;
			}
		}
		return null;
	}

	public boolean isInheritor(PsiClass psiClass)
	{
		for(String className : myClassNames)
		{
			if(InheritanceUtil.isInheritor(psiClass, className))
			{
				return true;
			}
		}
		return false;
	}
}

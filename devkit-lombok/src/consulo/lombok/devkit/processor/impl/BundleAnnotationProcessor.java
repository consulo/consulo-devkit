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
package consulo.lombok.devkit.processor.impl;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.light.LightMethodBuilder;
import com.intellij.psi.impl.light.LightParameter;
import consulo.java.module.util.JavaClassNames;
import consulo.lombok.processors.LombokSelfClassProcessor;

/**
 * @author VISTALL
 * @since 1:49/22.10.13
 */
public class BundleAnnotationProcessor extends LombokSelfClassProcessor
{
	public BundleAnnotationProcessor(String annotationClass)
	{
		super(annotationClass);
	}

	@Override
	public void processElement(@NotNull PsiClass parent, @NotNull PsiClass psiClass, @NotNull List<PsiElement> result)
	{
		JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(psiClass.getProject());

		PsiJavaParserFacade parserFacade = javaPsiFacade.getParserFacade();

		PsiAnnotation affectedAnnotation = getAffectedAnnotation(psiClass);

		String value = calcBundleMessageFilePath(affectedAnnotation, psiClass);

		createMessage0(parent, psiClass, result, parserFacade, affectedAnnotation, value);
		createMessage1(parent, psiClass, result, parserFacade, affectedAnnotation, value);
	}

	private static void createMessage0(PsiClass parent,
			PsiClass psiClass,
			List<PsiElement> result,
			PsiJavaParserFacade parserFacade,
			PsiAnnotation affectedAnnotation,
			String value)
	{
		LightMethodBuilder builder = new LightMethodBuilder(parent.getManager(), "message");
		builder.setContainingClass(psiClass);
		builder.setModifiers(PsiModifier.STATIC, PsiModifier.FINAL, PsiModifier.PUBLIC);
		builder.setNavigationElement(affectedAnnotation);
		builder.setMethodReturnType(JavaClassNames.JAVA_LANG_STRING);


		final PsiParameter parameterFromText = parserFacade.createParameterFromText("@org.jetbrains.annotations.PropertyKey(" +
				value +
				") java.lang.String key", psiClass);

		builder.addParameter(new LightParameter("key", JavaPsiFacade.getElementFactory(parent.getProject()).createTypeByFQClassName(JavaClassNames
				.JAVA_LANG_STRING), builder, JavaLanguage.INSTANCE)
		{
			@NotNull
			@Override
			public PsiModifierList getModifierList()
			{
				return parameterFromText.getModifierList();
			}
		});

		result.add(builder);
	}

	private static void createMessage1(PsiClass parent,
			PsiClass psiClass,
			List<PsiElement> result,
			PsiJavaParserFacade parserFacade,
			PsiAnnotation affectedAnnotation,
			String value)
	{
		LightMethodBuilder builder = new LightMethodBuilder(parent.getManager(), "message");
		builder.setContainingClass(psiClass);
		builder.setModifiers(PsiModifier.STATIC, PsiModifier.FINAL, PsiModifier.PUBLIC);
		builder.setNavigationElement(affectedAnnotation);
		builder.setMethodReturnType(JavaClassNames.JAVA_LANG_STRING);


		final PsiParameter parameterFromText = parserFacade.createParameterFromText("@org.jetbrains.annotations.PropertyKey(" +
				value +
				") java.lang.String key", psiClass);

		builder.addParameter(new LightParameter("key", JavaPsiFacade.getElementFactory(parent.getProject()).createTypeByFQClassName(JavaClassNames
				.JAVA_LANG_STRING), builder, JavaLanguage.INSTANCE)
		{
			@NotNull
			@Override
			public PsiModifierList getModifierList()
			{
				return parameterFromText.getModifierList();
			}
		});

		PsiClassType javaLangObject = JavaPsiFacade.getElementFactory(parent.getProject()).createTypeByFQClassName(JavaClassNames.JAVA_LANG_OBJECT);

		builder.addParameter("args", new PsiEllipsisType(javaLangObject), true);

		result.add(builder);
	}

	@NotNull
	public static String calcBundleMessageFilePath(@NotNull PsiAnnotation annotation, @NotNull PsiClass psiClass)
	{
		JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(psiClass.getProject());

		String result = null;
		PsiElement value = annotation.findAttributeValue(PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME);
		if(value instanceof PsiExpression)
		{
			Object o = javaPsiFacade.getConstantEvaluationHelper().computeConstantExpression(value);
			if(o instanceof String)
			{
				result = (String) o;
			}
		}

		if(StringUtil.isEmpty(result))
		{
			result = "messages." + psiClass.getName();
		}

		return result;
	}

	@NotNull
	@Override
	public Class<? extends PsiElement> getCollectorPsiElementClass()
	{
		return PsiMethod.class;
	}
}

/*
 * Copyright 2011-present Greg Shrago
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

package org.intellij.grammar.editor;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import org.intellij.grammar.BnfIcons;
import org.intellij.grammar.KnownAttribute;
import org.intellij.grammar.generator.ParserGeneratorUtil;
import org.intellij.grammar.generator.RuleGraphHelper;
import org.intellij.grammar.java.JavaHelper;
import org.intellij.grammar.psi.BnfExpression;
import org.intellij.grammar.psi.BnfFile;
import org.intellij.grammar.psi.BnfRule;
import org.intellij.grammar.psi.impl.GrammarUtil;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author gregsh
 */
public class BnfRuleLineMarkerProvider extends RelatedItemLineMarkerProvider
{

	@Override
	public void collectNavigationMarkers(List<PsiElement> elements,
										 Collection<? super RelatedItemLineMarkerInfo> result,
										 boolean forNavigation)
	{
		Set<PsiElement> visited = forNavigation ? new HashSet<PsiElement>() : null;
		for(PsiElement element : elements)
		{
			PsiElement parent = element.getParent();
			boolean isRuleId = parent instanceof BnfRule && (forNavigation || element == ((BnfRule) parent).getId());
			if(!(isRuleId || forNavigation && element instanceof BnfExpression))
			{
				continue;
			}
			List<PsiElement> items = new ArrayList<PsiElement>();
			NavigatablePsiElement method = getMethod(element);
			if(method != null && (!forNavigation || visited.add(method)))
			{
				items.add(method);
			}
			boolean hasPSI = false;
			if(isRuleId)
			{
				BnfRule rule = RuleGraphHelper.getSynonymTargetOrSelf(null, (BnfRule) parent);
				if(RuleGraphHelper.hasPsiClass(rule))
				{
					hasPSI = true;
					JavaHelper javaHelper = JavaHelper.getJavaHelper(rule);
					Couple<String> qualifiedRuleClassNames = ParserGeneratorUtil.getQualifiedRuleClassName(rule);
					for(String className : new String[]{
							qualifiedRuleClassNames.getFirst(),
							qualifiedRuleClassNames.getSecond()
					})
					{
						NavigatablePsiElement aClass = javaHelper.findClass(className);
						if(aClass != null && (!forNavigation || visited.add(aClass)))
						{
							items.add(aClass);
						}
					}
				}
			}
			if(!items.isEmpty())
			{
				AnAction action = ActionManager.getInstance().getAction("GotoRelated");
				String tooltipAd = "";
				String popupTitleAd = "";
				if(action != null)
				{
					String shortcutText = KeymapUtil.getFirstKeyboardShortcutText(action);
					String actionText = StringUtil.isEmpty(shortcutText) ? "'" + action.getTemplatePresentation().getText() + "' action" : shortcutText;
					tooltipAd = "\nGo to sub-expression code via " + actionText;
					popupTitleAd = " (for sub-expressions use " + actionText + ")";
				}
				String title = "parser " + (hasPSI ? "and PSI " : "") + "code";
				NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(BnfIcons.RELATED_METHOD).
						setTargets(items).
						setTooltipText("Click to navigate to " + title + tooltipAd).
						setPopupTitle(StringUtil.capitalize(title) + popupTitleAd);
				result.add(builder.createLineMarkerInfo(element));
			}
		}
	}

	@Nullable
	private static NavigatablePsiElement getMethod(PsiElement element)
	{
		BnfRule rule = PsiTreeUtil.getParentOfType(element, BnfRule.class);
		if(rule == null)
		{
			return null;
		}
		String parserClass = ParserGeneratorUtil.getAttribute(null, rule, KnownAttribute.PARSER_CLASS);
		if(StringUtil.isEmpty(parserClass))
		{
			return null;
		}
		JavaHelper helper = JavaHelper.getJavaHelper(element);
		PsiFile containingFile = rule.getContainingFile();
		String version = containingFile instanceof BnfFile ? ((BnfFile) containingFile).getVersion() : null;
		List<NavigatablePsiElement> methods = helper.findClassMethods(version, parserClass, JavaHelper.MethodType.STATIC, GrammarUtil.getMethodName(rule, element), -1);
		return ContainerUtil.getFirstItem(methods);
	}
}

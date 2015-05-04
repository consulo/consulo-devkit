/*
 * Copyright 2013-2015 must-be.org
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

package org.mustbe.consulo.devkit.navigation;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;

import org.consulo.lombok.annotations.Bundle;
import org.consulo.lombok.devkit.processor.impl.BundleAnnotationProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.ide.actions.OpenFileAction;
import com.intellij.lang.properties.PropertiesFileType;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaToken;
import com.intellij.util.ConstantFunction;

/**
 * @author VISTALL
 * @since 04.05.2015
 */
public class BundleItemLineMarkerProvider implements LineMarkerProvider
{
	@Nullable
	@Override
	public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element)
	{
		if(element instanceof PsiJavaToken && ((PsiJavaToken) element).getTokenType() == JavaTokenType.IDENTIFIER && element.getParent() instanceof
				PsiClass)
		{
			PsiClass psiClass = (PsiClass) element.getParent();
			PsiAnnotation annotation = AnnotationUtil.findAnnotation(psiClass, Bundle.class.getName());
			if(annotation != null)
			{
				return new LineMarkerInfo<PsiElement>(element, element.getTextRange(), PropertiesFileType.INSTANCE.getIcon(),
						Pass.LINE_MARKERS, new ConstantFunction<PsiElement, String>("Navigate to file"),
						new GutterIconNavigationHandler<PsiElement>()
				{
					@Override
					public void navigate(MouseEvent e, PsiElement elt)
					{
						PsiClass parent = (PsiClass) elt.getParent();
						PsiAnnotation annotation = AnnotationUtil.findAnnotation(parent, Bundle.class.getName());
						if(annotation != null)
						{
							String filePath = BundleAnnotationProcessor.calcBundleMessageFilePath(annotation, parent);
							filePath = filePath.replace(".", "/") + PropertiesFileType.DOT_DEFAULT_EXTENSION;
							VirtualFile[] contentRoots = ProjectRootManager.getInstance(elt.getProject()).getContentSourceRoots();
							for(VirtualFile contentRoot : contentRoots)
							{
								VirtualFile fileByRelativePath = contentRoot.findFileByRelativePath(filePath);
								if(fileByRelativePath != null)
								{
									OpenFileAction.openFile(fileByRelativePath, elt.getProject());
								}
							}
						}

					}
				}, GutterIconRenderer.Alignment.LEFT);
			}
		}
		return null;
	}

	@Override
	public void collectSlowLineMarkers(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result)
	{

	}
}

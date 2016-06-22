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

package consulo.lombok.devkit.navigation;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;

import org.consulo.lombok.annotations.Bundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.RequiredDispatchThread;
import org.mustbe.consulo.RequiredReadAction;
import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.ide.actions.OpenFileAction;
import com.intellij.lang.properties.PropertiesFileType;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.util.ConstantFunction;
import consulo.lombok.devkit.processor.impl.BundleAnnotationProcessor;

/**
 * @author VISTALL
 * @since 04.05.2015
 */
public class BundleItemLineMarkerProvider implements LineMarkerProvider
{
	@RequiredReadAction
	@Nullable
	@Override
	public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element)
	{
		return null;
	}

	@RequiredReadAction
	@Override
	public void collectSlowLineMarkers(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result)
	{
		for(PsiElement temp : elements)
		{
			if(temp instanceof PsiClass)
			{
				PsiClass psiClass = (PsiClass) temp;
				PsiIdentifier nameIdentifier = psiClass.getNameIdentifier();
				if(nameIdentifier == null)
				{
					continue;
				}

				PsiAnnotation annotation = AnnotationUtil.findAnnotation(psiClass, Bundle.class.getName());
				if(annotation != null)
				{
					LineMarkerInfo<PsiElement> markerInfo = new LineMarkerInfo<PsiElement>(nameIdentifier, nameIdentifier.getTextRange(), PropertiesFileType.INSTANCE.getIcon(),
							Pass.UPDATE_OVERRIDEN_MARKERS, new ConstantFunction<PsiElement, String>("Navigate to bundle"), new GutterIconNavigationHandler<PsiElement>()
					{
						@RequiredDispatchThread
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
										return;
									}
								}
							}

							Messages.showErrorDialog(parent.getProject(), "Bundle did not found", "Error");
						}
					}, GutterIconRenderer.Alignment.LEFT);
					result.add(markerInfo);
				}
			}
		}
	}
}

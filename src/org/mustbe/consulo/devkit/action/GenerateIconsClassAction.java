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

package org.mustbe.consulo.devkit.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.devkit.action.icons.GenerateDirNode;
import org.mustbe.consulo.devkit.action.icons.IconClassBuilder;
import org.mustbe.consulo.roots.ContentFolderScopes;
import org.mustbe.consulo.roots.impl.ProductionResourceContentFolderTypeProvider;
import com.intellij.application.options.ModuleListCellRenderer;
import com.intellij.ide.actions.OpenFileAction;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.QualifiedName;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.ui.UIUtil;
import lombok.val;

/**
 * @author VISTALL
 * @since 02.02.15
 */
public class GenerateIconsClassAction extends AnAction
{
	public static class SettingsDialog extends DialogWrapper
	{
		private JPanel myRootPanel;
		private ComboBox myModuleComboBox;
		private JBTextField myClassTextField;

		public SettingsDialog(@NotNull final Project project)
		{
			super(project);
			setTitle("Generate Icons Class");
			myRootPanel = new JPanel(new VerticalFlowLayout());
			myModuleComboBox = new ComboBox(ApplicationManager.getApplication().runReadAction(new Computable<Module[]>()
			{
				@Override
				public Module[] compute()
				{
					return ModuleManager.getInstance(project).getSortedModules();
				}
			}));
			myModuleComboBox.setRenderer(new ModuleListCellRenderer());
			myRootPanel.add(LabeledComponent.left(myModuleComboBox, "Module"));
			myClassTextField = new JBTextField("org.unknown.SomeIcons");
			myRootPanel.add(LabeledComponent.left(myClassTextField, "File"));
			init();
		}

		public String getClassQName()
		{
			return myClassTextField.getText();
		}

		@NotNull
		public Module getModule()
		{
			return (Module) myModuleComboBox.getSelectedItem();
		}

		@Nullable
		@Override
		protected String getDimensionServiceKey()
		{
			setSize(400, 150);

			return getClass().getName();
		}

		@Nullable
		@Override
		protected JComponent createCenterPanel()
		{
			return myRootPanel;
		}
	}

	private static final String ourIconsDirName = "icons";

	@Override
	public void actionPerformed(AnActionEvent anActionEvent)
	{
		final Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
		assert project != null;

		val settingsDialog = new SettingsDialog(project);

		if(!settingsDialog.showAndGet())
		{
			return;
		}

		UIUtil.invokeAndWaitIfNeeded(new Runnable()
		{
			@Override
			public void run()
			{
				addFile(settingsDialog.getModule(), settingsDialog.getClassQName());
			}
		});
	}

	private static void addFile(final Module module, final String classQName)
	{
		val project = module.getProject();
		try
		{

			DirectoryIndex directoryIndex = DirectoryIndex.getInstance(module.getProject());

			ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);

			VirtualFile[] contentFolderFiles = moduleRootManager.getContentFolderFiles(ContentFolderScopes.of
					(ProductionResourceContentFolderTypeProvider.getInstance()));

			List<VirtualFile> iconsDirs = ContainerUtil.map(contentFolderFiles, new Function<VirtualFile, VirtualFile>()
			{
				@Override
				public VirtualFile fun(VirtualFile virtualFile)
				{
					return virtualFile.findChild(ourIconsDirName);
				}
			});

			final List<VirtualFile> icons = new ArrayList<VirtualFile>();
			for(VirtualFile iconDir : iconsDirs)
			{
				VfsUtil.visitChildrenRecursively(iconDir, new VirtualFileVisitor()
				{
					@Override
					public boolean visitFile(@NotNull VirtualFile file)
					{
						if("png".equalsIgnoreCase(file.getExtension()))
						{
							icons.add(file);
						}
						return super.visitFile(file);
					}
				});
			}

			MultiMap<String, VirtualFile> map = new MultiMap<String, VirtualFile>();

			for(VirtualFile iconFile : icons)
			{
				VirtualFile parentFile = iconFile.getParent();
				String packageName = directoryIndex.getPackageName(parentFile);

				assert packageName != null;
				if(packageName.length() == ourIconsDirName.length())
				{
					map.putValue("", iconFile);
				}
				else
				{
					map.putValue(packageName.substring(ourIconsDirName.length() + 1, packageName.length()), iconFile);
				}
			}

			QualifiedName p = QualifiedName.fromDottedString(classQName);

			final String packageName = p.getParent().toString();
			final String className = p.getLastComponent();

			IconClassBuilder iconClassBuilder = new IconClassBuilder(packageName, className);

			for(Map.Entry<String, Collection<VirtualFile>> entry : map.entrySet())
			{
				String key = entry.getKey();
				QualifiedName qualifiedName = QualifiedName.fromDottedString(key);

				GenerateDirNode generateDirNode = iconClassBuilder.getOrCreate(qualifiedName);

				for(VirtualFile virtualFile : entry.getValue())
				{
					generateDirNode.addIcon(key, virtualFile);
				}
			}

			val classText = iconClassBuilder.build().toString();

			String fileName = className + JavaFileType.DOT_DEFAULT_EXTENSION;
			val psiFile = PsiFileFactory.getInstance(project).createFileFromText(fileName, JavaFileType.INSTANCE, classText);
			ApplicationManager.getApplication().runWriteAction(new Runnable()
			{
				@Override
				public void run()
				{
					CodeStyleManager.getInstance(project).reformat(psiFile);
				}
			});

			contentFolderFiles = moduleRootManager.getContentFolderFiles(ContentFolderScopes.onlyProduction());

			if(contentFolderFiles.length == 0)
			{
				Messages.showErrorDialog(project, "No production directories", "Error");
				return;
			}

			VirtualFile directoryForAdd = contentFolderFiles[0];
			QualifiedName qualifiedName = QualifiedName.fromDottedString(packageName);
			for(val part : qualifiedName.getComponents())
			{
				val finalDirectoryForAdd = directoryForAdd;
				VirtualFile temp = finalDirectoryForAdd.findChild(part);

				if(temp == null || !temp.isDirectory())
				{
					temp = ApplicationManager.getApplication().runWriteAction(new ThrowableComputable<VirtualFile, Throwable>()
					{
						@Override
						public VirtualFile compute() throws IOException
						{
							return finalDirectoryForAdd.createChildDirectory(null, part);
						}
					});
				}
				directoryForAdd = temp;
			}

			val temp = directoryForAdd;
			VirtualFile file = ApplicationManager.getApplication().runWriteAction(new ThrowableComputable<VirtualFile, Throwable>()
			{
				@Override
				public VirtualFile compute() throws IOException
				{
					val child = temp.findOrCreateChildData(null, psiFile.getName());

					child.setBinaryContent(psiFile.getText().getBytes(CharsetToolkit.UTF8_CHARSET));
					return child;
				}
			});
			OpenFileAction.openFile(file, project);
		}
		catch(Throwable throwable)
		{
			Messages.showErrorDialog(project, ExceptionUtil.getThrowableText(throwable), "Error");
		}
	}
}

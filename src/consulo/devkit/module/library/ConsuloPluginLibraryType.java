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

package consulo.devkit.module.library;

import java.io.File;
import java.io.IOException;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.icons.AllIcons;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.RepositoryHelper;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.DummyLibraryProperties;
import com.intellij.openapi.roots.libraries.LibraryType;
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration;
import com.intellij.openapi.roots.libraries.PersistentLibraryKind;
import com.intellij.openapi.roots.libraries.ui.LibraryEditorComponent;
import com.intellij.openapi.roots.libraries.ui.LibraryPropertiesEditor;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.LibraryEditor;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.io.ZipUtil;
import consulo.annotations.RequiredDispatchThread;
import consulo.ide.updateSettings.UpdateSettings;
import consulo.roots.types.BinariesOrderRootType;
import consulo.vfs.util.ArchiveVfsUtil;

/**
 * @author VISTALL
 * @since 29-Sep-16
 */
public class ConsuloPluginLibraryType extends LibraryType<DummyLibraryProperties>
{
	public static final String LIBRARY_PREFIX = "consulo-plugin: ";
	public static final String DEP_LIBRARY = "dep";

	public static final PersistentLibraryKind<DummyLibraryProperties> KIND = new PersistentLibraryKind<DummyLibraryProperties>("consulo-plugin")
	{
		@NotNull
		@Override
		public DummyLibraryProperties createDefaultProperties()
		{
			return new DummyLibraryProperties();
		}
	};

	private ConsuloPluginLibraryType()
	{
		super(KIND);
	}

	@Nullable
	@Override
	public String getCreateActionName()
	{
		return "From Consulo plugin repository";
	}

	@Nullable
	@Override
	public NewLibraryConfiguration createNewLibrary(@NotNull JComponent jComponent, @Nullable VirtualFile virtualFile, @NotNull Project project)
	{
		DialogBuilder builder = new DialogBuilder();
		builder.title("Enter Plugin ID");
		ChoosePluginPanel centerPanel = new ChoosePluginPanel(project);
		builder.centerPanel(centerPanel);
		builder.setPreferredFocusComponent(centerPanel.getTextField());
		builder.dimensionKey("ConsuloPluginLibraryType#ChoosePluginPanel");

		if(!builder.showAndGet())
		{
			return null;
		}

		IdeaPluginDescriptor pluginDescriptor = centerPanel.getPluginDescriptor();
		if(pluginDescriptor == null)
		{
			Messages.showErrorDialog(project, "Plugin is not found", ApplicationInfo.getInstance().getFullVersion());
			return null;
		}

		Ref<VirtualFile> libRef = Ref.create();

		String pluginId = pluginDescriptor.getPluginId().getIdString();

		new Task.Modal(project, "Downloading plugin: " + pluginDescriptor.getPluginId().getIdString(), false)
		{
			@Override
			public void run(@NotNull ProgressIndicator progressIndicator)
			{
				String url = RepositoryHelper.buildUrlForDownload(UpdateSettings.getInstance().getChannel(), pluginId, null, false);

				try
				{
					File tempFile = FileUtil.createTempFile("download", "zip");

					HttpRequests.request(url).saveToFile(tempFile, progressIndicator);

					File depDir = new File(project.getBasePath(), DEP_LIBRARY);
					depDir.mkdir();

					ZipUtil.extract(tempFile, depDir, null);

					tempFile.delete();

					libRef.set(LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(depDir, pluginId + "/lib")));
				}
				catch(IOException e)
				{
					//
				}
			}
		}.queue();

		VirtualFile libDirectory = libRef.get();
		if(libDirectory == null)
		{
			Messages.showErrorDialog(project, "Plugin directory is not found", ApplicationInfo.getInstance().getFullVersion());
			return null;
		}

		return new NewLibraryConfiguration(LIBRARY_PREFIX + pluginId, this, DummyLibraryProperties.INSTANCE)
		{
			@Override
			public void addRoots(@NotNull LibraryEditor libraryEditor)
			{
				for(VirtualFile file : libDirectory.getChildren())
				{
					if(file.isDirectory())
					{
						continue;
					}
					VirtualFile localVirtualFileByPath = ArchiveVfsUtil.getArchiveRootForLocalFile(file);
					if(localVirtualFileByPath == null)
					{
						continue;
					}
					libraryEditor.addRoot(localVirtualFileByPath, BinariesOrderRootType.getInstance());
				}
			}
		};
	}

	@Nullable
	@Override
	public LibraryPropertiesEditor createPropertiesEditor(@NotNull LibraryEditorComponent<DummyLibraryProperties> libraryEditorComponent)
	{
		return new LibraryPropertiesEditor()
		{
			@NotNull
			@Override
			public JComponent createComponent()
			{
				return new JPanel();
			}

			@Override
			public void apply()
			{

			}

			@RequiredDispatchThread
			@Override
			public boolean isModified()
			{
				return false;
			}

			@RequiredDispatchThread
			@Override
			public void reset()
			{

			}
		};
	}

	@Nullable
	@Override
	public Icon getIcon()
	{
		return AllIcons.Icon16;
	}
}

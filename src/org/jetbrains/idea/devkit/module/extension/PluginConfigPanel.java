/*
 * Copyright 2013 Consulo.org
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
package org.jetbrains.idea.devkit.module.extension;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;

import org.consulo.module.extension.ui.ModuleExtensionWithSdkPanel;
import org.jetbrains.annotations.Nullable;
import com.intellij.ide.highlighter.JarArchiveFileType;
import com.intellij.ide.plugins.IdeaPluginDescriptorImpl;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.ArchiveFileSystem;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.CheckBoxListListener;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.Function;
import com.intellij.util.ui.UIUtil;

/**
 * @author VISTALL
 * @since 13:36/13.06.13
 */
public class PluginConfigPanel extends JPanel
{

	private static final String LIBRARY_PREFIX = "consulo:";
	private final PluginMutableModuleExtension myMutableModuleExtension;
	private final ModifiableRootModel myRootModel;
	private final Runnable myUpdateOnCheck;
	private JPanel myRoot;
	private ModuleExtensionWithSdkPanel myModuleExtensionWithSdkPanel;
	private CheckBoxList<IdeaPluginDescriptorImpl> myBundledPluginsList;
	private JTabbedPane myTabbedPane1;
	private CheckBoxList<IdeaPluginDescriptorImpl> myCustomPluginList;
	private TextFieldWithBrowseButton myCustomPluginPath;

	public PluginConfigPanel(PluginMutableModuleExtension mutableModuleExtension, ModifiableRootModel rootModel, Runnable updateOnCheck)
	{
		myMutableModuleExtension = mutableModuleExtension;
		myRootModel = rootModel;
		myUpdateOnCheck = updateOnCheck;

		updateBundledPluginList();
		updateCustomBundledPluginList();
	}

	private void createUIComponents()
	{
		myRoot = this;
		myModuleExtensionWithSdkPanel = new ModuleExtensionWithSdkPanel(myMutableModuleExtension, new Runnable()
		{
			@Override
			public void run()
			{
				if(myUpdateOnCheck != null)
				{
					myUpdateOnCheck.run();
				}

				updateBundledPluginList();
			}
		});

		myCustomPluginPath = new TextFieldWithBrowseButton();
		myCustomPluginPath.setEditable(true);
		Project project = myMutableModuleExtension.getModule().getProject();
		myCustomPluginPath.addBrowseFolderListener("Choose Plugin Dir", null, project, new FileChooserDescriptor(false, true, false, false, false, false));
		myCustomPluginPath.setText(myMutableModuleExtension.getCustomPluginDirPresentableUrl());
		myCustomPluginPath.getTextField().getDocument().addDocumentListener(new DocumentAdapter()
		{
			@Override
			protected void textChanged(DocumentEvent documentEvent)
			{
				myMutableModuleExtension.setCustomPluginDirUrl(myCustomPluginPath.getText());

				updateCustomBundledPluginList();
			}
		});

		myBundledPluginsList = new CheckBoxList<IdeaPluginDescriptorImpl>();
		myBundledPluginsList.setCheckBoxListListener(new MyCheckBoxListListener(myBundledPluginsList));

		myCustomPluginList = new CheckBoxList<IdeaPluginDescriptorImpl>();
		myCustomPluginList.setCheckBoxListListener(new MyCheckBoxListListener(myCustomPluginList));
	}

	private void updateBundledPluginList()
	{
		ApplicationManager.getApplication().executeOnPooledThread(new Runnable()
		{
			@Override
			public void run()
			{
				updateBusy(myBundledPluginsList, true, Collections.<IdeaPluginDescriptorImpl>emptyList());

				List<IdeaPluginDescriptorImpl> items = null;
				final Sdk sdk = myMutableModuleExtension.getSdk();
				if(sdk == null)
				{
					items = Collections.emptyList();
				}
				else
				{

					final String pluginPath = sdk.getHomePath() + "/plugins";

					final int pluginCount = PluginManager.countPlugins(pluginPath);

					items = new ArrayList<IdeaPluginDescriptorImpl>(pluginCount);
					PluginManager.loadDescriptors(pluginPath, items, null, pluginCount);
					final Iterator<IdeaPluginDescriptorImpl> iterator = items.iterator();

					// remove Core plugin - because it added to SDK
					while(iterator.hasNext())
					{
						final IdeaPluginDescriptorImpl next = iterator.next();
						if(next.getPluginId().getIdString().equals(PluginManager.CORE_PLUGIN_ID))
						{
							iterator.remove();
							break;
						}
					}
				}

				updateBusy(myBundledPluginsList, false, items);
			}
		});
	}

	private void updateCustomBundledPluginList()
	{
		ApplicationManager.getApplication().executeOnPooledThread(new Runnable()
		{
			@Override
			public void run()
			{
				updateBusy(myCustomPluginList, true, Collections.<IdeaPluginDescriptorImpl>emptyList());

				List<IdeaPluginDescriptorImpl> items = null;

				String customPluginDirPresentableUrl = myMutableModuleExtension.getCustomPluginDirPresentableUrl();
				if(customPluginDirPresentableUrl == null)
				{
					items = Collections.emptyList();
				}
				else
				{
					final int pluginCount = PluginManager.countPlugins(customPluginDirPresentableUrl);

					items = new ArrayList<IdeaPluginDescriptorImpl>(pluginCount);
					PluginManager.loadDescriptors(customPluginDirPresentableUrl, items, null, pluginCount);
				}

				updateBusy(myCustomPluginList, false, items);
			}
		});
	}

	private void updateBusy(final CheckBoxList<IdeaPluginDescriptorImpl> list, final boolean val, final List<IdeaPluginDescriptorImpl> items)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				list.setItems(items, IdeaPluginDescriptorToString.ourInstance);
				list.setPaintBusy(val);

				if(!val)
				{
					for(IdeaPluginDescriptorImpl item : items)
					{
						list.setItemSelected(item, findLibrary(item) != null);
					}
				}
			}
		});
	}

	@Nullable
	private Library findLibrary(IdeaPluginDescriptorImpl ideaPluginDescriptor)
	{
		final String pluginName = LIBRARY_PREFIX + ideaPluginDescriptor.getName();
		final Iterator<Library> libraryIterator = myRootModel.getModuleLibraryTable().getLibraryIterator();
		while(libraryIterator.hasNext())
		{
			final Library next = libraryIterator.next();
			final String name = next.getName();
			if(name != null && name.equals(pluginName))
			{
				return next;
			}
		}
		return null;
	}

	private static class IdeaPluginDescriptorToString implements Function<IdeaPluginDescriptorImpl, String>
	{
		private static final IdeaPluginDescriptorToString ourInstance = new IdeaPluginDescriptorToString();

		@Override
		public String fun(IdeaPluginDescriptorImpl ideaPluginDescriptor)
		{
			return ideaPluginDescriptor.getName();
		}
	}

	private class MyCheckBoxListListener implements CheckBoxListListener
	{
		private final CheckBoxList<IdeaPluginDescriptorImpl> myBoxList;

		private MyCheckBoxListListener(CheckBoxList<IdeaPluginDescriptorImpl> boxList)
		{
			myBoxList = boxList;
		}

		@Override
		public void checkBoxSelectionChanged(int index, boolean value)
		{
			final Object itemAt = myBoxList.getItemAt(index);
			if(itemAt != null)
			{
				final LibraryTable moduleLibraryTable = myRootModel.getModuleLibraryTable();
				final IdeaPluginDescriptorImpl ideaPluginDescriptor = (IdeaPluginDescriptorImpl) itemAt;
				if(value)
				{
					final List<File> classPath = ideaPluginDescriptor.getClassPath();

					final Library library = moduleLibraryTable.createLibrary(LIBRARY_PREFIX + ideaPluginDescriptor.getName());
					final Library.ModifiableModel modifiableModel = library.getModifiableModel();

					ArchiveFileSystem jarFileSystem = JarArchiveFileType.INSTANCE.getFileSystem();
					for(File file : classPath)
					{
						if(file.isDirectory())
						{
							continue;
						}
						modifiableModel.addRoot(jarFileSystem.findLocalVirtualFileByPath(file.getPath()), OrderRootType.CLASSES);
					}

					modifiableModel.commit();
				}
				else
				{
					final Library library = findLibrary(ideaPluginDescriptor);
					if(library != null)
					{
						moduleLibraryTable.removeLibrary(library);
					}
				}
				UIUtil.invokeLaterIfNeeded(myUpdateOnCheck);
			}
		}
	}
}

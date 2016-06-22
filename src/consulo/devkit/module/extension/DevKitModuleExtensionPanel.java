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

package consulo.devkit.module.extension;

import java.awt.BorderLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;

import org.consulo.module.extension.ui.ModuleExtensionSdkBoxBuilder;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.devkit.module.extension.PluginMutableModuleExtension;
import org.mustbe.consulo.RequiredDispatchThread;
import com.intellij.ide.highlighter.JarArchiveFileType;
import com.intellij.ide.plugins.IdeaPluginDescriptorImpl;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.types.BinariesOrderRootType;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.ArchiveFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.CheckBoxListListener;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.TabbedPaneWrapper;
import com.intellij.util.Function;
import com.intellij.util.ui.UIUtil;

/**
 * @author VISTALL
 * @since 22.03.2015
 */
public class DevKitModuleExtensionPanel extends JPanel implements Disposable
{
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
				final LibraryTable moduleLibraryTable = myMutableModuleExtension.getModuleRootLayer().getModuleLibraryTable();
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
						VirtualFile localVirtualFileByPath = jarFileSystem.findLocalVirtualFileByPath(file.getPath());
						if(localVirtualFileByPath == null)
						{
							continue;
						}
						modifiableModel.addRoot(localVirtualFileByPath, BinariesOrderRootType.getInstance());
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

	private static final String LIBRARY_PREFIX = "consulo:";

	private final PluginMutableModuleExtension myMutableModuleExtension;
	private final Runnable myUpdateOnCheck;

	private CheckBoxList<IdeaPluginDescriptorImpl> myBundledPluginsList;
	private CheckBoxList<IdeaPluginDescriptorImpl> myCustomPluginList;

	@RequiredDispatchThread
	public DevKitModuleExtensionPanel(PluginMutableModuleExtension mutableModuleExtension, Runnable updateOnCheck)
	{
		super(new VerticalFlowLayout(true, true));
		myMutableModuleExtension = mutableModuleExtension;
		myUpdateOnCheck = updateOnCheck;

		ModuleExtensionSdkBoxBuilder boxBuilder = ModuleExtensionSdkBoxBuilder.createAndDefine(mutableModuleExtension, new Runnable()
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

		add(boxBuilder.build());

		myBundledPluginsList = new CheckBoxList<IdeaPluginDescriptorImpl>();
		myBundledPluginsList.setCheckBoxListListener(new MyCheckBoxListListener(myBundledPluginsList));

		myCustomPluginList = new CheckBoxList<IdeaPluginDescriptorImpl>();
		myCustomPluginList.setCheckBoxListListener(new MyCheckBoxListListener(myCustomPluginList));

		final TextFieldWithBrowseButton customPluginPathTextField = new TextFieldWithBrowseButton();
		customPluginPathTextField.setEditable(true);
		Project project = myMutableModuleExtension.getModule().getProject();
		customPluginPathTextField.addBrowseFolderListener("Choose Plugin Dir", null, project, new FileChooserDescriptor(false, true, false, false,
				false, false));
		customPluginPathTextField.setText(myMutableModuleExtension.getCustomPluginDirPresentableUrl());
		customPluginPathTextField.getTextField().getDocument().addDocumentListener(new DocumentAdapter()
		{
			@Override
			protected void textChanged(DocumentEvent documentEvent)
			{
				String fileUrl = StringUtil.isEmptyOrSpaces(customPluginPathTextField.getText()) ? null : VirtualFileManager.constructUrl("file",
						FileUtil.toSystemIndependentName(customPluginPathTextField.getText()));

				myMutableModuleExtension.setCustomPluginDirUrl(fileUrl);

				updateCustomPluginList();
			}
		});

		JPanel customPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0,0, true, true));
		customPanel.add(customPluginPathTextField);
		customPanel.add(ScrollPaneFactory.createScrollPane(myCustomPluginList, true));

		TabbedPaneWrapper paneWrapper = new TabbedPaneWrapper(this);
		paneWrapper.addTab("Bundled", ScrollPaneFactory.createScrollPane(myBundledPluginsList, true));
		paneWrapper.addTab("Custom", customPanel);

		JPanel paneWithHeader = new JPanel(new BorderLayout());
		paneWithHeader.setBorder(IdeBorderFactory.createTitledBorder("Plugins", false));
		paneWithHeader.add(paneWrapper.getComponent(), BorderLayout.CENTER);

		add(paneWithHeader);

		updateBundledPluginList();
		updateCustomPluginList();
	}

	@Nullable
	private Library findLibrary(IdeaPluginDescriptorImpl ideaPluginDescriptor)
	{
		final String pluginName = LIBRARY_PREFIX + ideaPluginDescriptor.getName();
		final Iterator<Library> libraryIterator = myMutableModuleExtension.getModuleRootLayer().getModuleLibraryTable().getLibraryIterator();
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

	private void updateCustomPluginList()
	{
		ApplicationManager.getApplication().executeOnPooledThread(new Runnable()
		{
			@Override
			public void run()
			{
				updateBusy(myCustomPluginList, true, Collections.<IdeaPluginDescriptorImpl>emptyList());

				List<IdeaPluginDescriptorImpl> items = null;

				VirtualFile customPluginDir = myMutableModuleExtension.getCustomPluginDir();
				if(customPluginDir == null || !customPluginDir.exists())
				{
					items = Collections.emptyList();
				}
				else
				{
					File file = VfsUtil.virtualToIoFile(customPluginDir);
					int pluginCount = PluginManager.countPlugins(file.getAbsolutePath());

					items = new ArrayList<IdeaPluginDescriptorImpl>(pluginCount);
					PluginManager.loadDescriptors(file.getAbsolutePath(), items, null, pluginCount);
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

	@Override
	public void dispose()
	{

	}
}

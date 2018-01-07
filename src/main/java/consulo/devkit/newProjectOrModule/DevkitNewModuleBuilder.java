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

package consulo.devkit.newProjectOrModule;

import java.io.File;
import java.util.Collections;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.devkit.run.PluginConfigurationType;
import com.intellij.compiler.impl.javaCompiler.JavaCompilerConfiguration;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.elements.ArtifactRootElement;
import com.intellij.packaging.impl.artifacts.PlainArtifactType;
import com.intellij.packaging.impl.elements.ArtifactRootElementImpl;
import com.intellij.packaging.impl.elements.DirectoryPackagingElement;
import com.intellij.packaging.impl.elements.moduleContent.ModuleOutputPackagingElementImpl;
import com.intellij.packaging.impl.elements.moduleContent.ProductionModuleOutputElementType;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.uiDesigner.GuiDesignerConfiguration;
import consulo.annotations.DeprecationInfo;
import consulo.devkit.run.ConsuloRunConfiguration;
import consulo.ide.impl.UnzipNewModuleBuilderProcessor;
import consulo.ide.newProject.NewModuleBuilder;
import consulo.ide.newProject.NewModuleContext;
import consulo.java.module.extension.JavaMutableModuleExtension;
import consulo.module.extension.MutableModuleExtensionWithSdk;
import consulo.packaging.impl.elements.ZipArchivePackagingElement;
import consulo.packaging.impl.elements.moduleContent.ProductionResourceModuleOutputElementType;
import consulo.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider;
import consulo.roots.impl.ProductionContentFolderTypeProvider;
import consulo.roots.impl.ProductionResourceContentFolderTypeProvider;
import consulo.util.pointers.NamedPointer;

/**
 * @author VISTALL
 * @since 26.11.14
 */
@Deprecated
@DeprecationInfo("After full migration to maven, plugin factory will be replaced by maven archtype")
public class DevkitNewModuleBuilder implements NewModuleBuilder
{
	private static final String DEFAULT_JAVA_SDK_NAME = "1.8";
	private static final String DEFAULT_CONSULO_SDK_NAME = "Consulo SNAPSHOT";

	@Override
	public void setupContext(@NotNull NewModuleContext context)
	{
		NewModuleContext.Group group = context.createGroup("consulo-plugin", "Consulo Plugin");
		group.add("Simple Plugin", AllIcons.Nodes.Plugin, new UnzipNewModuleBuilderProcessor("/moduleTemplates/#ConsuloDevkitSimplePlugin.zip")
		{
			@NotNull
			@Override
			public JComponent createConfigurationPanel()
			{
				return new JPanel();
			}

			@Override
			public void setupModule(@NotNull JComponent panel, @NotNull ContentEntry contentEntry, @NotNull ModifiableRootModel modifiableRootModel)
			{
				unzip(modifiableRootModel);

				JavaMutableModuleExtension<?> javaExtension = modifiableRootModel.getExtensionWithoutCheck("java");
				MutableModuleExtensionWithSdk<?> devkitExtension = modifiableRootModel.getExtensionWithoutCheck("consulo-plugin");

				assert javaExtension != null;
				assert devkitExtension != null;

				javaExtension.setEnabled(true);
				javaExtension.setBytecodeVersion("1.8");
				devkitExtension.setEnabled(true);

				modifiableRootModel.addModuleExtensionSdkEntry(javaExtension);
				modifiableRootModel.addModuleExtensionSdkEntry(devkitExtension);

				javaExtension.getInheritableLanguageLevel().set(null, LanguageLevel.JDK_1_8);
				javaExtension.getInheritableSdk().set(null, DEFAULT_JAVA_SDK_NAME);

				devkitExtension.getInheritableSdk().set(null, DEFAULT_CONSULO_SDK_NAME);

				VirtualFile file = contentEntry.getFile();
				if(file != null)
				{
					FileUtil.createDirectory(new File(file.getPath(), "src"));

					VirtualFile temp = file.findFileByRelativePath("src");
					if(temp != null)
					{
						contentEntry.addFolder(temp, ProductionContentFolderTypeProvider.getInstance());
					}
					temp = file.findFileByRelativePath("resources");
					if(temp != null)
					{
						contentEntry.addFolder(temp, ProductionResourceContentFolderTypeProvider.getInstance());
					}
				}

				Project project = modifiableRootModel.getProject();

				JavaCompilerConfiguration compilerConfiguration = JavaCompilerConfiguration.getInstance(project);

				compilerConfiguration.setAddNotNullAssertions(true);
				compilerConfiguration.setProjectBytecodeTarget("1.8");

				GuiDesignerConfiguration.getInstance(project).COPY_FORMS_RUNTIME_TO_OUTPUT = false;

				Artifact distArtifact = ArtifactManager.getInstance(project).addArtifact("dist", PlainArtifactType.getInstance(), createDistRootElement(modifiableRootModel));

				RunManagerEx runManager = RunManagerEx.getInstanceEx(project);

				RunnerAndConfigurationSettings runConfiguration = runManager.createRunConfiguration("Run Consulo In Sandbox", PluginConfigurationType.getInstance().getConfigurationFactories()[0]);

				ConsuloRunConfiguration configuration = (ConsuloRunConfiguration) runConfiguration.getConfiguration();
				configuration.setArtifactName("dist");
				configuration.setJavaSdkName(DEFAULT_JAVA_SDK_NAME);
				configuration.setConsuloSdkName(DEFAULT_CONSULO_SDK_NAME);

				runConfiguration.setSingleton(true);
				runManager.addConfiguration(runConfiguration, true, Collections.emptyList(), true);

				runManager.makeStable(runConfiguration);
				runManager.setSelectedConfiguration(runConfiguration);

				BuildArtifactsBeforeRunTaskProvider.setBuildArtifactBeforeRun(project, configuration, distArtifact);
			}

			@NotNull
			private ArtifactRootElement<?> createDistRootElement(ModifiableRootModel modifiableRootModel)
			{
				String moduleName = modifiableRootModel.getModule().getName();
				NamedPointer<Module> pointer = ModuleUtil.createPointer(modifiableRootModel.getModule());

				ArtifactRootElementImpl rootElement = new ArtifactRootElementImpl();
				DirectoryPackagingElement modulePluginDirElement = new DirectoryPackagingElement(moduleName);
				rootElement.addFirstChild(modulePluginDirElement);

				DirectoryPackagingElement libPluginDir = new DirectoryPackagingElement("lib");
				modulePluginDirElement.addFirstChild(libPluginDir);

				ZipArchivePackagingElement zipArchivePackagingElement = new ZipArchivePackagingElement(moduleName + ".jar");
				zipArchivePackagingElement.addFirstChild(new ModuleOutputPackagingElementImpl(ProductionModuleOutputElementType.getInstance(), modifiableRootModel.getProject(), pointer,
						ProductionContentFolderTypeProvider.getInstance()));
				zipArchivePackagingElement.addFirstChild(new ModuleOutputPackagingElementImpl(ProductionResourceModuleOutputElementType.getInstance(), modifiableRootModel.getProject(), pointer,
						ProductionResourceContentFolderTypeProvider.getInstance()));
				libPluginDir.addFirstChild(zipArchivePackagingElement);
				return rootElement;
			}
		});
	}
}

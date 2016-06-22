/*
 * Copyright 2013-2014 must-be.org
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
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.consulo.module.extension.MutableModuleExtensionWithSdk;
import org.consulo.util.pointers.NamedPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.devkit.run.PluginConfigurationType;
import consulo.devkit.run.ConsuloRunConfiguration;
import org.mustbe.consulo.ide.impl.NewModuleBuilder;
import org.mustbe.consulo.ide.impl.NewModuleContext;
import org.mustbe.consulo.ide.impl.UnzipNewModuleBuilderProcessor;
import org.mustbe.consulo.java.module.extension.JavaMutableModuleExtension;
import org.mustbe.consulo.roots.impl.ProductionContentFolderTypeProvider;
import org.mustbe.consulo.roots.impl.ProductionResourceContentFolderTypeProvider;
import com.intellij.compiler.impl.javaCompiler.JavaCompilerConfiguration;
import com.intellij.compiler.options.CompileStepBeforeRun;
import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
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
import com.intellij.packaging.artifacts.ArtifactPointerUtil;
import com.intellij.packaging.elements.ArtifactRootElement;
import com.intellij.packaging.impl.artifacts.PlainArtifactType;
import com.intellij.packaging.impl.elements.ArtifactPackagingElement;
import com.intellij.packaging.impl.elements.ArtifactRootElementImpl;
import com.intellij.packaging.impl.elements.DirectoryCopyPackagingElement;
import com.intellij.packaging.impl.elements.DirectoryPackagingElement;
import com.intellij.packaging.impl.elements.ZipArchivePackagingElement;
import com.intellij.packaging.impl.elements.moduleContent.ModuleOutputPackagingElementImpl;
import com.intellij.packaging.impl.elements.moduleContent.ProductionModuleOutputElementType;
import com.intellij.packaging.impl.elements.moduleContent.ProductionResourceModuleOutputElementType;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTask;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.uiDesigner.GuiDesignerConfiguration;

/**
 * @author VISTALL
 * @since 26.11.14
 */
public class DevkitNewModuleBuilder implements NewModuleBuilder
{
	private static final String DEFAULT_JAVA_SDK_NAME = "1.6";
	private static final String DEFAULT_CONSULO_SDK_NAME = "Consulo 1.SNAPSHOT";

	@Override
	public void setupContext(@NotNull NewModuleContext context)
	{
		context.addItem("#ConsuloDevkit", "Consulo Plugin", AllIcons.Icon16);
		context.addItem("#ConsuloDevkitSimplePlugin", "Simple Plugin", AllIcons.Nodes.Plugin);

		context.setupItem(new String[]{
				"#ConsuloDevkit",
				"#ConsuloDevkitSimplePlugin"
		}, new UnzipNewModuleBuilderProcessor("/moduleTemplates/#ConsuloDevkitSimplePlugin.zip")
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
				devkitExtension.setEnabled(true);

				modifiableRootModel.addModuleExtensionSdkEntry(javaExtension);
				modifiableRootModel.addModuleExtensionSdkEntry(devkitExtension);

				javaExtension.getInheritableLanguageLevel().set(null, LanguageLevel.JDK_1_6);
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
				compilerConfiguration.setProjectBytecodeTarget("1.6");

				GuiDesignerConfiguration.getInstance(project).COPY_FORMS_RUNTIME_TO_OUTPUT = false;

				Artifact distArtifact = ArtifactManager.getInstance(project).addArtifact("dist", PlainArtifactType.getInstance(),
						createDistRootElement(modifiableRootModel));

				Artifact pluginArtifact = ArtifactManager.getInstance(project).addArtifact("plugin", PlainArtifactType.getInstance(),
						createPluginRootElement(modifiableRootModel, distArtifact));

				RunManagerEx runManager = RunManagerEx.getInstanceEx(project);

				RunnerAndConfigurationSettings runConfiguration = runManager.createRunConfiguration("Run Consulo In Sandbox",
						PluginConfigurationType.getInstance().getConfigurationFactories()[0]);


				ConsuloRunConfiguration configuration = (ConsuloRunConfiguration) runConfiguration.getConfiguration();
				configuration.setArtifactName("plugin");
				configuration.setJavaSdkName(DEFAULT_JAVA_SDK_NAME);
				configuration.setConsuloSdkName(DEFAULT_CONSULO_SDK_NAME);

				List<BeforeRunTask> tasks = new ArrayList<BeforeRunTask>(2);
				tasks.add(BeforeRunTaskProvider.getProvider(project, CompileStepBeforeRun.ID).createTask(configuration));

				BuildArtifactsBeforeRunTask artifactsBeforeRunTask = BeforeRunTaskProvider.getProvider(project,
						BuildArtifactsBeforeRunTaskProvider.ID).createTask(configuration);

				artifactsBeforeRunTask.addArtifact(distArtifact);
				artifactsBeforeRunTask.addArtifact(pluginArtifact);

				tasks.add(artifactsBeforeRunTask);

				runConfiguration.setSingleton(true);
				runManager.addConfiguration(runConfiguration, true, tasks, true);

				runManager.makeStable(runConfiguration);
				runManager.setSelectedConfiguration(runConfiguration);
			}

			@NotNull
			private ArtifactRootElement<?> createPluginRootElement(ModifiableRootModel modifiableRootModel, Artifact distArtifact)
			{
				ArtifactRootElementImpl rootElement = new ArtifactRootElementImpl();
				rootElement.addFirstChild(new ArtifactPackagingElement(modifiableRootModel.getProject(),
						ArtifactPointerUtil.getPointerManager(modifiableRootModel.getProject()).create(distArtifact)));
				rootElement.addFirstChild(new DirectoryCopyPackagingElement(modifiableRootModel.getModule().getModuleDirPath() + "/dep"));
				return rootElement;
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
				zipArchivePackagingElement.addFirstChild(new ModuleOutputPackagingElementImpl(ProductionModuleOutputElementType.getInstance(),
						modifiableRootModel.getProject(), pointer, ProductionContentFolderTypeProvider.getInstance()));
				zipArchivePackagingElement.addFirstChild(new ModuleOutputPackagingElementImpl(ProductionResourceModuleOutputElementType.getInstance
						(), modifiableRootModel.getProject(), pointer, ProductionResourceContentFolderTypeProvider.getInstance()));
				libPluginDir.addFirstChild(zipArchivePackagingElement);
				return rootElement;
			}
		});
	}
}

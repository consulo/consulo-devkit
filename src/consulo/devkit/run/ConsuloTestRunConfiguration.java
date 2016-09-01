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

package consulo.devkit.run;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.diagnostic.logging.LogConfigurationPanel;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.JavaExecutionUtil;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.configurations.coverage.CoverageConfigurable;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.SettingsEditorGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaPackage;
import com.intellij.psi.search.GlobalSearchScope;
import consulo.annotations.RequiredDispatchThread;

/**
 * @author VISTALL
 * @since 12.04.2015
 */
public class ConsuloTestRunConfiguration extends ConsuloRunConfigurationBase
{
	public TargetType getTargetType()
	{
		return myTargetType;
	}

	public void setTargetType(TargetType targetType)
	{
		myTargetType = targetType;
	}

	public static enum TargetType
	{
		CLASS,
		PACKAGE
	}

	private TargetType myTargetType = TargetType.CLASS;
	public String CLASS_NAME;
	public String PACKAGE_NAME;

	public ConsuloTestRunConfiguration(Project project, ConfigurationFactory factory, String name)
	{
		super(project, factory, name);
	}

	@Override
	public void readExternal(Element element) throws InvalidDataException
	{
		super.readExternal(element);
		myTargetType = TargetType.valueOf(element.getAttributeValue("target-type", "CLASS"));
	}

	@Override
	public void writeExternal(Element element) throws WriteExternalException
	{
		super.writeExternal(element);
		element.setAttribute("target-type", myTargetType.name());
	}

	@NotNull
	@Override
	@SuppressWarnings("unchecked")
	public SettingsEditor<? extends RunConfiguration> getConfigurationEditor()
	{
		SettingsEditorGroup settingsEditorGroup = new SettingsEditorGroup<RunConfiguration>();
		settingsEditorGroup.addEditor("General", new ConsuloTestRunConfigurationEditor(getProject()));
		settingsEditorGroup.addEditor("Coverage", new CoverageConfigurable(this));
		settingsEditorGroup.addEditor("Log", new LogConfigurationPanel<ConsuloRunConfigurationBase>());
		return settingsEditorGroup;
	}

	@NotNull
	@Override
	@RequiredDispatchThread
	public ConsuloSandboxRunState createState(Executor executor,
			@NotNull ExecutionEnvironment env,
			@NotNull Sdk javaSdk,
			@NotNull String consuloHome,
			@Nullable Artifact artifact) throws ExecutionException
	{
		ConsuloTestRunConfiguration runProfile = (ConsuloTestRunConfiguration) env.getRunProfile();
		switch(getTargetType())
		{
			case CLASS:
				if(StringUtil.isEmptyOrSpaces(runProfile.CLASS_NAME))
				{
					throw new ExecutionException("'Class Name' cant be empty");
				}
				break;
		}
		return new ConsuloTestRunState(env, javaSdk, consuloHome, artifact);
	}

	@Override
	public String suggestedName()
	{
		switch(myTargetType)
		{
			case CLASS:
				return ProgramRunnerUtil.shortenName(JavaExecutionUtil.getShortClassName(CLASS_NAME), 0);
			case PACKAGE:
				return ProgramRunnerUtil.shortenName(JavaExecutionUtil.getShortClassName(PACKAGE_NAME), 0);
		}
		return null;
	}

	@Override
	@RequiredDispatchThread
	public void checkConfiguration() throws RuntimeConfigurationException
	{
		Module[] modules = getModules();
		GlobalSearchScope globalSearchScope = GlobalSearchScope.EMPTY_SCOPE;
		for(Module module : modules)
		{
			globalSearchScope = globalSearchScope.union(module.getModuleWithDependenciesAndLibrariesScope(true));
		}
		switch(myTargetType)
		{
			case CLASS:
				if(StringUtil.isEmptyOrSpaces(CLASS_NAME))
				{
					throw new RuntimeConfigurationException("Test class can't be empty");
				}
				PsiClass aClass = JavaPsiFacade.getInstance(getProject()).findClass(CLASS_NAME, globalSearchScope);
				if(aClass == null)
				{
					throw new RuntimeConfigurationException("Test class is not found");
				}
				break;
			case PACKAGE:
				PsiJavaPackage aPackage = JavaPsiFacade.getInstance(getProject()).findPackage(StringUtil.notNullize(PACKAGE_NAME));
				if(aPackage == null)
				{
					throw new RuntimeConfigurationException("Test package is not found");
				}
				break;
		}
	}
}


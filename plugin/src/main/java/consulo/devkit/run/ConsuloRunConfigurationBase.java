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

package consulo.devkit.run;

import consulo.annotation.access.RequiredReadAction;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkUtil;
import consulo.devkit.localize.DevKitLocalize;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.LegacyRunConfigurationConverter;
import consulo.execution.configuration.ProgramParametersAware;
import consulo.execution.configuration.fragment.ProgramParametersFragmentState;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.LocatableConfigurationBase;
import consulo.execution.configuration.log.LogFileOptions;
import consulo.execution.configuration.log.PredefinedLogFile;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.java.debugger.impl.GenericDebugRunnerConfiguration;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.util.lang.Comparing;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2015-04-12
 */
public abstract class ConsuloRunConfigurationBase extends LocatableConfigurationBase implements GenericDebugRunnerConfiguration, ProgramParametersAware {
    public static final PredefinedLogFile CONSULO_LOG = new PredefinedLogFile("CONSULO_LOG", true);

    public static final String LOG_FILE = "/system/log/consulo.log";
    private static final String JAVA_SDK = "java-sdk";

    public ConsuloRunConfigurationBase(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    @Override
    protected Class<ConsuloRunConfigurationOptions> getOptionsClass() {
        return ConsuloRunConfigurationOptions.class;
    }

    @Override
    public ConsuloRunConfigurationOptions getOptions() {
        return (ConsuloRunConfigurationOptions) super.getOptions();
    }

    @Override
    protected void convertLegacyState(LegacyRunConfigurationConverter converter) {
        super.convertLegacyState(converter);

        ConsuloRunConfigurationLegacyState old = converter.readXml(ConsuloRunConfigurationLegacyState.class);
        ConsuloRunConfigurationOptions options = new ConsuloRunConfigurationOptions();
        options.setVmParameters(old.VM_PARAMETERS);
        options.setJava9Modules(old.ENABLED_JAVA9_MODULES);
        options.setPluginsHomePath(old.PLUGINS_HOME_PATH);
        options.setConsuloSdkPath(old.ALT_CONSULO_SDK_PATH);
        Element javaSdk = converter.getElement().getChild(JAVA_SDK);
        options.setJavaSdk(javaSdk == null ? null : javaSdk.getAttributeValue("name"));
        converter.setOptions(options);

        converter.addFragment(new ProgramParametersFragmentState(old.PROGRAM_PARAMETERS));
    }

    @Override
    public List<RunConfigurationFragment<?>> createFragments() {
        return List.of(
            new ConsuloSdkFragment<>(),
            new ConsuloVmParametersFragment<>(),
            new ConsuloPluginsHomePathFragment<>(),
            new ConsuloJava9ModulesFragment<>()
        );
    }

    @Nonnull
    public String getSandboxPath() {
        return getProject().getBasePath() + "/" + Project.DIRECTORY_STORE_FOLDER + "/sandbox";
    }

    @Nullable
    @Override
    public LogFileOptions getOptionsForPredefinedLogFile(PredefinedLogFile predefinedLogFile) {
        if (CONSULO_LOG.equals(predefinedLogFile)) {
            String sandboxPath = getSandboxPath();
            return new LogFileOptions("consulo.log", sandboxPath + LOG_FILE, true, false, true);
        }
        else {
            return null;
        }
    }

    @Nullable
    public String getConsuloSdkHome() {
        return getOptions().getConsuloSdkPath();
    }

    public void setConsuloSdkHome(@Nullable String path) {
        getOptions().setConsuloSdkPath(path);
    }

    @Nullable
    public String getPluginsHomePath() {
        return getOptions().getPluginsHomePath();
    }

    public void setPluginsHomePath(@Nullable String path) {
        getOptions().setPluginsHomePath(path);
    }

    @Nullable
    public String getVmParameters() {
        return getOptions().getVmParameters();
    }

    public void setVmParameters(@Nullable String vmParameters) {
        getOptions().setVmParameters(vmParameters);
    }

    public boolean isEnabledJava9Modules() {
        return getOptions().isJava9Modules();
    }

    public void setEnabledJava9Modules(boolean enabled) {
        getOptions().setJava9Modules(enabled);
    }

    @Nullable
    @Override
    public final ConsuloSandboxRunState getState(@Nonnull Executor executor, @Nonnull ExecutionEnvironment env) throws ExecutionException {
        String javaSdkName = getJavaSdkName();
        final Sdk javaSdk = javaSdkName == null ? null : SdkUtil.createPointer(javaSdkName).get();
        if (javaSdk == null) {
            throw new ExecutionException(DevKitLocalize.runConfigurationNoJavaSdk().get());
        }

        final String consuloSdkHome = getConsuloSdkHome();
        if (consuloSdkHome == null) {
            throw new ExecutionException(DevKitLocalize.runConfigurationNoConsuloSdk().get());
        }

        return createState(executor, env, javaSdk, consuloSdkHome, getPluginsHomePath());
    }

    @Nonnull
    public abstract ConsuloSandboxRunState createState(
        Executor executor,
        @Nonnull ExecutionEnvironment env,
        @Nonnull Sdk javaSdk,
        @Nonnull String consuloHome,
        @Nullable String pluginsHomePath
    ) throws ExecutionException;



    @Override
    public boolean isGeneratedName() {
        return Comparing.equal(getName(), suggestedName());
    }

    @Nullable
    public String getJavaSdkName() {
        return getOptions().getJavaSdk();
    }

    public void setJavaSdkName(@Nullable String name) {
        getOptions().setJavaSdk(name);
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public Module[] getModules() {
        return ModuleManager.getInstance(getProject()).getModules();
    }
}

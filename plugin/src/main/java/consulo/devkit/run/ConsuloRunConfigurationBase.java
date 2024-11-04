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
import consulo.component.util.pointer.NamedPointer;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkPointerManager;
import consulo.content.bundle.SdkUtil;
import consulo.devkit.localize.DevKitLocalize;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.LocatableConfigurationBase;
import consulo.execution.configuration.log.LogFileOptions;
import consulo.execution.configuration.log.PredefinedLogFile;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.ide.ServiceManager;
import consulo.java.debugger.impl.GenericDebugRunnerConfiguration;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.DefaultJDOMExternalizer;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2015-04-12
 */
public abstract class ConsuloRunConfigurationBase extends LocatableConfigurationBase implements GenericDebugRunnerConfiguration {
    public static final PredefinedLogFile CONSULO_LOG = new PredefinedLogFile("CONSULO_LOG", true);

    public static final String LOG_FILE = "/system/log/consulo.log";
    private static final String JAVA_SDK = "java-sdk";
    private static final String CONSULO_SDK = "consulo-sdk";

    public String VM_PARAMETERS;
    public boolean ENABLED_JAVA9_MODULES;
    public String PROGRAM_PARAMETERS;
    public String PLUGINS_HOME_PATH;
    protected NamedPointer<Sdk> myJavaSdkPointer;
    public boolean USE_ALT_CONSULO_SDK;
    public String ALT_CONSULO_SDK_PATH;

    public ConsuloRunConfigurationBase(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
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

    public String getConsuloSdkHome() {
        if (StringUtil.isEmpty(ALT_CONSULO_SDK_PATH)) {
            return null;
        }
        return ALT_CONSULO_SDK_PATH;
    }

    @Nullable
    @Override
    public final ConsuloSandboxRunState getState(@Nonnull Executor executor, @Nonnull ExecutionEnvironment env) throws ExecutionException {
        final Sdk javaSdk = myJavaSdkPointer == null ? null : myJavaSdkPointer.get();
        if (javaSdk == null) {
            throw new ExecutionException(DevKitLocalize.runConfigurationNoJavaSdk().get());
        }

        final String consuloSdkHome = getConsuloSdkHome();
        if (consuloSdkHome == null) {
            throw new ExecutionException(DevKitLocalize.runConfigurationNoConsuloSdk().get());
        }

        return createState(executor, env, javaSdk, consuloSdkHome, PLUGINS_HOME_PATH);
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
    public void readExternal(Element element) throws InvalidDataException {
        DefaultJDOMExternalizer.readExternal(this, element);

        myJavaSdkPointer = PluginRunXmlConfigurationUtil.readPointer(
            JAVA_SDK,
            element,
            () -> ServiceManager.getService(SdkPointerManager.class)
        );

        super.readExternal(element);
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        DefaultJDOMExternalizer.writeExternal(this, element);

        PluginRunXmlConfigurationUtil.writePointer(JAVA_SDK, element, myJavaSdkPointer);

        super.writeExternal(element);
    }

    @Override
    public boolean isGeneratedName() {
        return Comparing.equal(getName(), suggestedName());
    }

    @Nullable
    public String getJavaSdkName() {
        return myJavaSdkPointer == null ? null : myJavaSdkPointer.getName();
    }

    public void setJavaSdkName(@Nullable String name) {
        myJavaSdkPointer = name == null ? null : SdkUtil.createPointer(name);
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public Module[] getModules() {
        return ModuleManager.getInstance(getProject()).getModules();
    }
}

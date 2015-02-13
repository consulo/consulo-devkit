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

package org.mustbe.consulo.devkit.run;

import org.consulo.util.pointers.NamedPointer;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.sdk.SdkUtil;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactPointerUtil;

/**
 * @author VISTALL
 * @since 29.05.14
 */
public class ConsuloJUnitData
{
	public static enum Model
	{
		artifact,
		sdk,
		none
	}

	public static final Key<ConsuloJUnitData> KEY = Key.create("consulo-junit-data");

	private Model myConsuloModel = Model.none;
	private NamedPointer<?> myConsuloPointer;
	private NamedPointer<Artifact> myPluginPointer;

	public static void read(RunConfigurationBase conf, Element element)
	{
		ConsuloJUnitData data = new ConsuloJUnitData();
		conf.putUserData(KEY, data);

		Model consuloModel = Model.valueOf(element.getAttributeValue("consulo-model", "none"));
		String consuloName = element.getAttributeValue("consulo-name");
		if(!StringUtil.isEmpty(consuloName))
		{
			switch(consuloModel)
			{
				case artifact:
					data.myConsuloPointer = ArtifactPointerUtil.getPointerManager(conf.getProject()).create(consuloName);
					break;
				case sdk:
					data.myConsuloPointer = SdkUtil.createPointer(consuloName);
					break;
			}
		}
		String pluginName = element.getAttributeValue("plugin-name");
		data.myPluginPointer = StringUtil.isEmpty(pluginName) ? null : ArtifactPointerUtil.getPointerManager(conf.getProject()).create(pluginName);
	}

	public static void write(RunConfigurationBase conf, Element element)
	{
		ConsuloJUnitData consuloJUnitData = conf.getUserData(KEY);
		if(consuloJUnitData == null)
		{
			return;
		}

		if(consuloJUnitData.myConsuloPointer != null)
		{
			element.setAttribute("consulo-model", consuloJUnitData.myConsuloModel.name());
			element.setAttribute("consulo-name", consuloJUnitData.myConsuloPointer.getName());
		}

		if(consuloJUnitData.myPluginPointer != null)
		{
			element.setAttribute("plugin-name", consuloJUnitData.myPluginPointer.getName());
		}
	}

	@Nullable
	public NamedPointer<?> getConsuloPointer()
	{
		return myConsuloPointer;
	}

	public void setPluginPointer(NamedPointer<Artifact> pluginPointer)
	{
		myPluginPointer = pluginPointer;
	}

	public void setConsuloPointer(Model model, NamedPointer<?> consuloPointer)
	{
		myConsuloModel = model;
		myConsuloPointer = consuloPointer;
	}

	@Nullable
	public NamedPointer<Artifact> getPluginPointer()
	{
		return myPluginPointer;
	}

	@Nullable
	public String getConsuloPath()
	{
		if(myConsuloPointer == null)
		{
			return null;
		}
		switch(myConsuloModel)
		{
			case artifact:
				Artifact artifact = (Artifact) myConsuloPointer.get();
				return artifact == null ? null : artifact.getOutputPath();
			case sdk:
				Sdk sdk = (Sdk) myConsuloPointer.get();
				return sdk == null ? null : sdk.getHomePath();
			default:
				return null;
		}
	}

	@Nullable
	public String getPluginPath()
	{
		Artifact artifact = myPluginPointer == null ? null : myPluginPointer.get();
		return artifact == null ? null : artifact.getOutputPath();
	}
}

/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.mustbe.consulo.devkit.test;

import org.jetbrains.annotations.NonNls;
import org.junit.runner.JUnitCore;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;

public class ConsuloTestApplication
{
	@NonNls
	public static final String IDEA_IS_INTERNAL_PROPERTY = "idea.is.internal";

	private static final Logger LOG = Logger.getInstance(ConsuloTestApplication.class);

	private static ConsuloTestApplication ourInstance;

	public static ConsuloTestApplication getInstance()
	{
		return ourInstance;
	}

	public ConsuloTestApplication(String[] args)
	{
		LOG.assertTrue(ourInstance == null);
		ourInstance = this;

		boolean isInternal = Boolean.getBoolean(IDEA_IS_INTERNAL_PROPERTY);

		ApplicationManagerEx.createApplication(isInternal, true, true, true, ApplicationManagerEx.IDEA_APPLICATION, null);
	}

	public void start()
	{
		try
		{
			ApplicationEx app = ApplicationManagerEx.getApplicationEx();
			app.load(PathManager.getOptionsPath());

			PluginId id = PluginId.getId("org.mustbe.consulo.csharp");
			IdeaPluginDescriptor plugin = PluginManager.getPlugin(id);
			if(plugin == null)
			{
				System.out.println("Plugin id is wrong");
				System.exit(-1);
			}

			Class<?> aClass = plugin.getPluginClassLoader().loadClass("org.musbe.consulo.csharp.parsing.CSharpParsingTest");

			JUnitCore core = new JUnitCore();
			core.addListener(new SMTestSender());
			core.run(aClass);
			System.exit(0);
		}
		catch(Throwable e)
		{
			e.printStackTrace();
			System.exit(-1);
		}
	}
}

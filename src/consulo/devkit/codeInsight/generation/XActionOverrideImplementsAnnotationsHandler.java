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

package consulo.devkit.codeInsight.generation;

import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.RequiredDispatchThread;
import org.mustbe.consulo.RequiredReadAction;
import org.mustbe.consulo.RequiredWriteAction;
import com.intellij.codeInsight.generation.OverrideImplementsAnnotationsHandler;
import com.intellij.openapi.project.Project;
import com.intellij.util.ArrayUtil;
import consulo.devkit.codeInsight.ConsuloUI;

/**
 * @author VISTALL
 * @since 10.06.2015
 */
public class XActionOverrideImplementsAnnotationsHandler implements OverrideImplementsAnnotationsHandler
{
	private static final String[] ourAnnotations = new String[]{
			RequiredReadAction.class.getName(),
			RequiredWriteAction.class.getName(),
			RequiredDispatchThread.class.getName(),
			ConsuloUI.RequiredUIAccess
	};

	@Override
	public String[] getAnnotations(Project project)
	{
		return ourAnnotations;
	}

	@NotNull
	@Override
	public String[] annotationsToRemove(Project project, @NotNull String s)
	{
		if(ArrayUtil.contains(s, ourAnnotations))
		{
			return ourAnnotations;
		}
		return ArrayUtil.EMPTY_STRING_ARRAY;
	}
}
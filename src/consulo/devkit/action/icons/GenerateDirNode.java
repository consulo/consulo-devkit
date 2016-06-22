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

package consulo.devkit.action.icons;

import java.awt.Image;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.ImageIcon;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ImageLoader;

/**
 * @author VISTALL
 * @since 02.02.15
 */
public class GenerateDirNode extends GenerateNode
{
	private Map<String, GenerateDirNode> myDirs = new TreeMap<String, GenerateDirNode>();

	private Map<String, GenerateIconNode> myFiles = new TreeMap<String, GenerateIconNode>();

	public GenerateDirNode(String name, String path)
	{
		super(name, path);
	}

	public GenerateDirNode getOrCreate(String name)
	{
		name = StringUtil.capitalize(name);

		GenerateDirNode generateDirNode = myDirs.get(name);
		if(generateDirNode == null)
		{
			myDirs.put(name, generateDirNode = new GenerateDirNode(name, "//"));
		}
		return generateDirNode;
	}

	public Map<String, GenerateDirNode> getDirs()
	{
		return myDirs;
	}

	public Map<String, GenerateIconNode> getFiles()
	{
		return myFiles;
	}

	public void addIcon(String parent, VirtualFile virtualFile)
	{
		String nameWithoutExtension = virtualFile.getNameWithoutExtension();
		if(StringUtil.endsWith(nameWithoutExtension, "@2x") ||
				StringUtil.endsWith(nameWithoutExtension, "@2x_dark") ||
				StringUtil.endsWith(nameWithoutExtension, "_dark"))
		{
			return;
		}

		nameWithoutExtension = nameWithoutExtension.replace("-", "_");
		nameWithoutExtension = StringUtil.capitalize(nameWithoutExtension);

		try
		{
			String pathToLoad = "/icons/";
			if(!StringUtil.isEmpty(parent))
			{
				pathToLoad += parent.replace(".", "/");
				pathToLoad += "/";
			}
			pathToLoad += virtualFile.getName();

			Image image = ImageLoader.loadFromStream(virtualFile.getInputStream());
			myFiles.put(nameWithoutExtension, new GenerateIconNode(nameWithoutExtension, pathToLoad, new ImageIcon(image)));
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}

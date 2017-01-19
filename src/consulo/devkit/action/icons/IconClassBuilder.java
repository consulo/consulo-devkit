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

package consulo.devkit.action.icons;

import javax.swing.Icon;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.util.QualifiedName;

/**
 * @author VISTALL
 * @since 02.02.15
 */
public class IconClassBuilder
{
	private StringBuilder myBuilder = new StringBuilder();

	private GenerateDirNode myRootNode = new GenerateDirNode("", "/");

	public IconClassBuilder(String packageName, String name)
	{
		myBuilder.append("package ").append(packageName).append(";\n\n");
		myBuilder.append("import com.intellij.openapi.util.IconLoader;\n");
		myBuilder.append("import consulo.ui.migration.SwingImageRef;\n\n");

		myBuilder.append("// Generated Consulo DevKit plugin \n");
		myBuilder.append("public interface ").append(name).append(" {\n");
	}

	public GenerateDirNode getOrCreate(QualifiedName qualifiedName)
	{
		GenerateDirNode root = myRootNode;
		for(int i = 0; i < qualifiedName.getComponentCount(); i++)
		{
			root = root.getOrCreate(qualifiedName.getComponents().get(i));
		}
		return root;
	}

	private void build(int indentSize, GenerateDirNode node)
	{
		for(GenerateDirNode entry : node.getDirs().values())
		{
			indent(indentSize).append("interface ").append(entry.getName()).append(" {\n");
			build(indentSize + 1, entry);
			indent(indentSize).append("}\n\n");
		}

		for(GenerateIconNode iconNode : node.getFiles().values())
		{
			Icon icon = iconNode.getIcon();
			indent(indentSize).append("SwingImageRef ").append(iconNode.getName()).append(" = IconLoader.getIcon(\"").append(iconNode
					.getPath()).append("\"); ").append(" // ").append(icon.getIconWidth()).append("x").append(icon.getIconHeight()).append("\n");
		}
	}

	private StringBuilder indent(int size)
	{
		myBuilder.append(StringUtil.repeat("  ", size));
		return myBuilder;
	}

	public IconClassBuilder build()
	{
		build(1, myRootNode);

		myBuilder.append("}");
		return this;
	}

	@Override
	public String toString()
	{
		return myBuilder.toString();
	}
}

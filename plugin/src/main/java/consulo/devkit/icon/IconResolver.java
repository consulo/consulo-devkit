package consulo.devkit.icon;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.impl.jar.JarFileSystemImpl;
import com.intellij.ui.IconDeferrer;
import com.intellij.util.io.URLUtil;
import consulo.disposer.Disposable;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author VISTALL
 * @since 2020-10-15
 */
public class IconResolver implements Disposable
{
	public static IconResolver getInstance(@Nonnull Project project)
	{
		return ServiceManager.getService(project, IconResolver.class);
	}

	private final Map<URL, Image> myImageCache = new ConcurrentHashMap<>();

	@Nonnull
	public Image getImage(Collection<VirtualFile> files)
	{
		VirtualFile file = null;
		for(VirtualFile virtualFile : files)
		{
			if(Objects.equals(virtualFile.getExtension(), "svg"))
			{
				file = virtualFile;
				break;
			}
		}

		if(file == null)
		{
			file = files.iterator().next();
		}

		try
		{
			URL fileUrl;
			if(file.isInLocalFileSystem())
			{
				fileUrl = VfsUtil.virtualToIoFile(file).toURI().toURL();
			}
			else if(file.getFileSystem() instanceof JarFileSystemImpl)
			{
				String presentableUrl = FileUtil.toSystemIndependentName(file.getPresentableUrl());

				Pair<String, String> parts = URLUtil.splitJarUrl(presentableUrl);
				if(parts == null)
				{
					return Image.empty(Image.DEFAULT_ICON_SIZE);
				}

				String filePath = parts.getFirst();
				String entryPath = parts.getSecond();

				fileUrl = URLUtil.getJarEntryURL(new File(filePath), entryPath);
			}
			else
			{
				return Image.empty(Image.DEFAULT_ICON_SIZE);
			}

			Image image = myImageCache.get(fileUrl);
			if(image != null)
			{
				return image;
			}

			return IconDeferrer.getInstance().defer(Image.empty(Image.DEFAULT_ICON_SIZE), fileUrl, url ->
			{
				try
				{
					Image targetImage = ImageEffects.resize(Image.fromUrl(url), Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE);
					myImageCache.put(url, targetImage);
					return targetImage;
				}
				catch(IOException e)
				{
					return Image.empty(Image.DEFAULT_ICON_SIZE);
				}
			}) ;
		}
		catch(IOException e)
		{
			return Image.empty(Image.DEFAULT_ICON_SIZE);
		}
	}

	@Override
	public void dispose()
	{
		myImageCache.clear();
	}
}

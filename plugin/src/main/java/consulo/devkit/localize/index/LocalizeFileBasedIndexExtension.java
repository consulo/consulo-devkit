package consulo.devkit.localize.index;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.intellij.util.io.VoidDataExternalizer;
import consulo.logging.Logger;
import org.jetbrains.yaml.YAMLFileType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;

/**
 * @author VISTALL
 * @since 2020-06-01
 */
public class LocalizeFileBasedIndexExtension extends FileBasedIndexExtension<String, Void>
{
	private static final Logger LOG = Logger.getInstance(LocalizeFileBasedIndexExtension.class);

	public static final ID<String, Void> INDEX = ID.create("consulo.localize.file.index");

	@Nonnull
	@Override
	public ID<String, Void> getName()
	{
		return INDEX;
	}

	@Nonnull
	@Override
	public DataIndexer<String, Void, FileContent> getIndexer()
	{
		return fileContent ->
		{
			VirtualFile file = fileContent.getFile();

			VirtualFile localizeDirectory = findLocalizeDirectory(file);
			if(localizeDirectory == null)
			{
				return Collections.emptyMap();
			}

			String relativeLocation = VfsUtil.getRelativeLocation(file.getParent(), localizeDirectory);

			if(relativeLocation == null)
			{
				return Collections.emptyMap();
			}

			// com/intellij/images

			relativeLocation = relativeLocation.replace("/", ".");

			String id = relativeLocation + ".localize." + file.getNameWithoutExtension();

			return Collections.singletonMap(id, null);
		};
	}

	@Nonnull
	@Override
	public KeyDescriptor<String> getKeyDescriptor()
	{
		return EnumeratorStringDescriptor.INSTANCE;
	}

	@Nonnull
	@Override
	public DataExternalizer<Void> getValueExternalizer()
	{
		return VoidDataExternalizer.INSTANCE;
	}

	@Override
	public int getVersion()
	{
		return 3;
	}

	@Nonnull
	@Override
	public FileBasedIndex.InputFilter getInputFilter()
	{
		return (project, file) ->
		{
			if(file.getFileType() != YAMLFileType.YML)
			{
				return false;
			}

			CharSequence nameSequence = file.getNameSequence();
			if(!StringUtil.endsWith(nameSequence, "Localize.yaml"))
			{
				return false;
			}

			VirtualFile localizeDirectory = findLocalizeDirectory(file);
			if(localizeDirectory != null)
			{
				VirtualFile idTxt = localizeDirectory.findChild("id.txt");
				if(idTxt == null)
				{
					return false;
				}

				try
				{
					String text = VfsUtil.loadText(idTxt);
					if("en".equals(text))
					{
						return true;
					}
				}
				catch(IOException e)
				{
					LOG.error(e);
				}
			}
			return false;
		};
	}

	@Nullable
	private static VirtualFile findLocalizeDirectory(VirtualFile file)
	{
		VirtualFile parent = file;
		while((parent = parent.getParent()) != null)
		{
			if(StringUtil.equals(parent.getNameSequence(), "localize"))
			{
				return parent;
			}
		}

		return null;
	}

	@Override
	public boolean dependsOnFileContent()
	{
		return false;
	}
}

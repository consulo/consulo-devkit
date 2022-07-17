package consulo.devkit;

import com.intellij.refactoring.migration.PredefinedMigrationProvider;

import javax.annotation.Nonnull;
import java.net.URL;

/**
 * @author VISTALL
 * @since 05-May-22
 */
public class Consulo3MigrationProvider implements PredefinedMigrationProvider
{
	@Nonnull
	@Override
	public URL getMigrationMap()
	{
		return getClass().getResource("Consulo2-Consulo3.xml");
	}
}


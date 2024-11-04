package consulo.devkit;

import com.intellij.java.impl.refactoring.migration.PredefinedMigrationProvider;
import consulo.annotation.component.ExtensionImpl;

import jakarta.annotation.Nonnull;
import java.net.URL;

/**
 * @author VISTALL
 * @since 05-May-22
 */
@ExtensionImpl
public class Consulo3MigrationProvider implements PredefinedMigrationProvider {
    @Nonnull
    @Override
    public URL getMigrationMap() {
        return getClass().getResource("Consulo2-Consulo3.xml");
    }
}


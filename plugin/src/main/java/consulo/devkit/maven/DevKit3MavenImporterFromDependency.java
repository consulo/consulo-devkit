package consulo.devkit.maven;

import consulo.annotation.component.ExtensionImpl;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 09-Aug-22
 */
@ExtensionImpl
public class DevKit3MavenImporterFromDependency extends DevKitMavenImporterFromDependency {
  @Inject
  public DevKit3MavenImporterFromDependency() {
    super("consulo", "consulo-component-api");
  }
}

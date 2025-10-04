package consulo.devkit.maven;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.ContentFolderTypeProvider;
import consulo.devkit.module.extension.PluginModuleExtension;
import consulo.maven.importing.MavenImporterFromDependency;
import consulo.module.Module;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.jetbrains.idea.maven.importing.MavenModifiableModelsProvider;
import org.jetbrains.idea.maven.importing.MavenRootModelAdapter;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectChanges;
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask;
import org.jetbrains.idea.maven.project.MavenProjectsTree;

import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2022-08-09
 */
@ExtensionImpl
public class DevKit3MavenImporterFromDependency extends MavenImporterFromDependency {
    @Inject
    public DevKit3MavenImporterFromDependency() {
        super("consulo", "consulo-component-api");
    }

    @Override
    public void preProcess(
        Module module,
        MavenProject mavenProject,
        MavenProjectChanges mavenProjectChanges,
        MavenModifiableModelsProvider mavenModifiableModelsProvider
    ) {
    }

    @Override
    public void process(MavenModifiableModelsProvider mavenModifiableModelsProvider, Module module, MavenRootModelAdapter mavenRootModelAdapter, MavenProjectsTree mavenProjectsTree, MavenProject mavenProject, MavenProjectChanges mavenProjectChanges, Map<MavenProject, String> map, List<MavenProjectsProcessorTask> list) {
        enableModuleExtension(module, mavenModifiableModelsProvider, PluginModuleExtension.class);
    }

    @Override
    public boolean isExcludedGenerationSourceFolder(@Nonnull MavenProject mavenProject, @Nonnull String sourcePath, @Nonnull ContentFolderTypeProvider typeProvider) {
        return super.isExcludedGenerationSourceFolder(mavenProject, sourcePath, typeProvider);
    }
}

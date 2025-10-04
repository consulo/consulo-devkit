package consulo.devkit.maven;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.ContentFolderTypeProvider;
import consulo.maven.importing.MavenImporterFromBuildPlugin;
import consulo.module.Module;
import jakarta.annotation.Nonnull;
import org.jetbrains.idea.maven.importing.MavenModifiableModelsProvider;
import org.jetbrains.idea.maven.importing.MavenRootModelAdapter;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectChanges;
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask;
import org.jetbrains.idea.maven.project.MavenProjectsTree;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2025-10-04
 */
@ExtensionImpl
public class MavenConsuloPluginImporter extends MavenImporterFromBuildPlugin {
    public MavenConsuloPluginImporter() {
        super("consulo.maven", "maven-consulo-plugin");
    }

    @Override
    public void preProcess(Module module, MavenProject mavenProject, MavenProjectChanges mavenProjectChanges, MavenModifiableModelsProvider mavenModifiableModelsProvider) {
    }

    @Override
    public void process(MavenModifiableModelsProvider mavenModifiableModelsProvider, Module module, MavenRootModelAdapter mavenRootModelAdapter, MavenProjectsTree mavenProjectsTree, MavenProject mavenProject, MavenProjectChanges mavenProjectChanges, Map<MavenProject, String> map, List<MavenProjectsProcessorTask> list) {
    }

    @Override
    public void collectExcludedFolders(MavenProject mavenProject, Consumer<String> result) {
        result.accept(mavenProject.getDirectory() + "/sandbox");
    }

    @Override
    public boolean isExcludedGenerationSourceFolder(@Nonnull MavenProject mavenProject,
                                                    @Nonnull String sourcePath,
                                                    @Nonnull ContentFolderTypeProvider typeProvider) {
        Path target = Path.of(sourcePath);
        Path localizePath = Path.of(mavenProject.getDirectory(), "target/generated-sources/localize");
        if (target.equals(localizePath)) {
            return true;
        }
        return super.isExcludedGenerationSourceFolder(mavenProject, sourcePath, typeProvider);
    }
}

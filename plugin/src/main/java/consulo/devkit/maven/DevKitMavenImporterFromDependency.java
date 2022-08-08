package consulo.devkit.maven;

import com.intellij.openapi.module.Module;
import consulo.devkit.module.extension.PluginModuleExtension;
import consulo.maven.importing.MavenImporterFromDependency;
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
 * @since 09-Aug-22
 */
public class DevKitMavenImporterFromDependency extends MavenImporterFromDependency
{
	public DevKitMavenImporterFromDependency()
	{
		super("consulo", "consulo-core-api");
	}

	public DevKitMavenImporterFromDependency(String groupId, String artifactId)
	{
		super(groupId, artifactId);
	}

	@Override
	public void preProcess(Module module, MavenProject mavenProject, MavenProjectChanges mavenProjectChanges, MavenModifiableModelsProvider mavenModifiableModelsProvider)
	{

	}

	@Override
	public void process(MavenModifiableModelsProvider mavenModifiableModelsProvider,
						Module module,
						MavenRootModelAdapter mavenRootModelAdapter,
						MavenProjectsTree mavenProjectsTree,
						MavenProject mavenProject,
						MavenProjectChanges mavenProjectChanges,
						Map<MavenProject, String> map,
						List<MavenProjectsProcessorTask> list)
	{
		enableModuleExtension(module, mavenModifiableModelsProvider, PluginModuleExtension.class);
	}
}

package consulo.devkit.inspections.util.service;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.impl.ExtensionAreaId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ObjectUtil;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.DomService;
import consulo.annotation.access.RequiredReadAction;
import org.jetbrains.idea.devkit.dom.Extension;
import org.jetbrains.idea.devkit.dom.Extensions;
import org.jetbrains.idea.devkit.dom.IdeaPlugin;
import org.jetbrains.idea.devkit.util.DescriptorUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author VISTALL
 * @since 2018-08-16
 */
public class ServiceLocator
{
	private static Map<String, ExtensionAreaId> ourExtensionToArea = new HashMap<>();

	static
	{
		ourExtensionToArea.put("applicationService", ExtensionAreaId.APPLICATION);
		ourExtensionToArea.put("projectService", ExtensionAreaId.PROJECT);
		ourExtensionToArea.put("moduleService", ExtensionAreaId.MODULE);
	}

	@Nullable
	@RequiredReadAction
	public static ServiceInfo findAnyService(@Nonnull PsiClass psiClass)
	{
		String qualifiedName = psiClass.getQualifiedName();
		if(qualifiedName == null)
		{
			return null;
		}

		List<ServiceInfo> services = ServiceLocator.getServices(psiClass.getProject());
		for(ServiceInfo service : services)
		{
			if(qualifiedName.equals(service.getImplementation()) || qualifiedName.equals(service.getInterface()))
			{
				return service;
			}
		}
		return null;
	}

	@Nullable
	@RequiredReadAction
	public static ServiceInfo findImplementationService(@Nonnull PsiClass psiClass)
	{
		String qualifiedName = psiClass.getQualifiedName();
		if(qualifiedName == null)
		{
			return null;
		}

		List<ServiceInfo> services = ServiceLocator.getServices(psiClass.getProject());
		for(ServiceInfo service : services)
		{
			if(qualifiedName.equals(service.getImplementation()))
			{
				return service;
			}
		}
		return null;
	}

	@RequiredReadAction
	public static List<ServiceInfo> getServices(Project project)
	{
		return CachedValuesManager.getManager(project).getCachedValue(project, () ->
				CachedValueProvider.Result.create(getServicesImpl(project), PsiModificationTracker.MODIFICATION_COUNT));
	}

	@Nonnull
	@RequiredReadAction
	private static List<ServiceInfo> getServicesImpl(Project project)
	{
		Collection<VirtualFile> candidates = DomService.getInstance().getDomFileCandidates(IdeaPlugin.class, project, GlobalSearchScope.projectScope(project));

		List<ServiceInfo> list = new ArrayList<>();
		PsiManager manager = PsiManager.getInstance(project);
		for(VirtualFile candidate : candidates)
		{
			PsiFile file = manager.findFile(candidate);
			if(!(file instanceof XmlFile))
			{
				continue;
			}

			DomFileElement<IdeaPlugin> consuloPlugin = DescriptorUtil.getConsuloPlugin((XmlFile) file);
			if(consuloPlugin == null)
			{
				continue;
			}

			IdeaPlugin rootElement = consuloPlugin.getRootElement();

			for(Extensions extensions : rootElement.getExtensions())
			{
				String extensionNs = extensions.getDefaultExtensionNs().getStringValue();
				if(!PluginManagerCore.CORE_PLUGIN_ID.equals(extensionNs))
				{
					continue;
				}

				XmlTag extensionsXmlTag = extensions.getXmlTag();

				for(XmlTag tag : extensionsXmlTag.getSubTags())
				{
					Extension extension = ObjectUtil.tryCast(DomManager.getDomManager(project).getDomElement(tag), Extension.class);
					if(extension == null)
					{
						continue;
					}

					String name = tag.getName();

					ExtensionAreaId area = ourExtensionToArea.get(name);
					if(area == null)
					{
						continue;
					}

					String serviceInterface = tag.getAttributeValue("serviceInterface");
					String serviceImplementation = tag.getAttributeValue("serviceImplementation");
					if(serviceInterface == null)
					{
						serviceInterface = serviceImplementation;
					}
					list.add(new ServiceInfo(area, serviceInterface, serviceImplementation, tag));
				}
			}
		}

		return list;
	}
}

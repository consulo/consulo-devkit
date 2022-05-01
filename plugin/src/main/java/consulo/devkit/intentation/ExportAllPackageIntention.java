package consulo.devkit.intentation;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.IncorrectOperationException;
import consulo.annotation.access.RequiredReadAction;
import consulo.devkit.util.PluginModuleUtil;
import consulo.java.module.extension.JavaModuleExtension;
import consulo.psi.PsiPackage;
import consulo.psi.PsiPackageManager;
import consulo.roots.ContentFolderScopes;
import consulo.ui.annotation.RequiredUIAccess;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * @author VISTALL
 * @since 15/01/2022
 */
public class ExportAllPackageIntention implements IntentionAction
{
	@Nls
	@Nonnull
	@Override
	public String getText()
	{
		return "Export all packages";
	}

	@Nls
	@Nonnull
	@Override
	public String getFamilyName()
	{
		return "Java Modules";
	}

	@Override
	@RequiredUIAccess
	public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile psiFile)
	{
		Module module = ModuleUtilCore.findModuleForFile(psiFile);
		return findModule(editor, psiFile) != null && PluginModuleUtil.isConsuloOrPluginProject(project, module);
	}

	@Override
	@RequiredUIAccess
	public void invoke(@Nonnull Project project, Editor editor, PsiFile psiFile) throws IncorrectOperationException
	{
		PsiJavaModule javaModule = findModule(editor, psiFile);
		if(javaModule == null)
		{
			return;
		}

		Module module = ModuleUtilCore.findModuleForFile(psiFile);
		if(module == null)
		{
			return;
		}

		ContentFolder[] folders = ModuleRootManager.getInstance(module).getContentFolders(ContentFolderScopes.production());

		List<VirtualFile> packageDirectories = new ArrayList<>();
		for(ContentFolder folder : folders)
		{
			VirtualFile contentFile = folder.getFile();
			if(contentFile == null)
			{
				continue;
			}

			VfsUtilCore.visitChildrenRecursively(contentFile, new VirtualFileVisitor<>()
			{
				@Override
				public boolean visitFile(@Nonnull VirtualFile file)
				{
					if(file.isDirectory() && file != contentFile)
					{
						packageDirectories.add(file);
					}
					return true;
				}
			});
		}

		PsiManager psiManager = PsiManager.getInstance(project);

		Set<String> alreadyExported = new HashSet<>();
		for(PsiPackageAccessibilityStatement statement : javaModule.getExports())
		{
			alreadyExported.add(statement.getPackageName());
		}

		Set<String> packages = new TreeSet<>(Comparator.reverseOrder());
		for(VirtualFile packageDirectory : packageDirectories)
		{
			PsiDirectory directory = psiManager.findDirectory(packageDirectory);
			if(directory == null)
			{
				continue;
			}

			PsiPackage psiPackage = PsiPackageManager.getInstance(project).findPackage(directory, JavaModuleExtension.class);
			if(psiPackage instanceof PsiJavaPackage psiJavaPackage)
			{
				PsiClass[] classes = psiJavaPackage.getClasses(GlobalSearchScope.moduleScope(module));
				if(classes.length == 0 || alreadyExported.contains(psiPackage.getQualifiedName()))
				{
					continue;
				}

				packages.add(psiPackage.getQualifiedName());
			}
		}

		PsiElementFactory psiElementFactory = PsiElementFactory.getInstance(project);
		WriteAction.run(() -> {
			PsiElement anchor = getLastItem(javaModule.getExports());
			if(anchor == null)
			{
				ASTNode lbrace = javaModule.getNode().findChildByType(JavaTokenType.RBRACE);
				anchor = lbrace == null ? null : lbrace.getPsi();
			}

			if(anchor == null)
			{
				return;
			}

			for(String aPackage : packages)
			{
				PsiStatement statement = psiElementFactory.createModuleStatementFromText("exports " + aPackage + ";", psiFile);

				anchor = javaModule.addBefore(statement, anchor);
			}
		});
	}

	private static <T> T getLastItem(Iterable<T> iterable)
	{
		T element = null;
		for(T t : iterable)
		{
			element = t;
		}
		return element;
	}

	@RequiredReadAction
	private PsiJavaModule findModule(Editor editor, PsiFile psiFile)
	{
		PsiElement element = psiFile.getViewProvider().findElementAt(editor.getCaretModel().getOffset());
		if(element == null || PsiUtilCore.getElementType(element) != JavaTokenType.IDENTIFIER)
		{
			return null;
		}
		return PsiTreeUtil.getParentOfType(element, PsiJavaModule.class);
	}

	@Override
	public boolean startInWriteAction()
	{
		return false;
	}
}

package consulo.devkit.intentation;

import com.intellij.java.language.psi.*;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.WriteAction;
import consulo.codeEditor.Editor;
import consulo.devkit.util.PluginModuleUtil;
import consulo.java.language.module.extension.JavaModuleExtension;
import consulo.language.ast.ASTNode;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.psi.*;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ContentFolder;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * @author VISTALL
 * @since 2022-01-15
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "consulo.devkit.export.all.packages", fileExtensions = "java", categories = {"Java", "Consulo DevKit"})
public class ExportAllPackageIntention implements IntentionAction {
    @Nls
    @Nonnull
    @Override
    public String getText() {
        return "Export all packages";
    }

    @Override
    @RequiredUIAccess
    public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile psiFile) {
        Module module = ModuleUtilCore.findModuleForFile(psiFile);
        return findModule(editor, psiFile) != null && PluginModuleUtil.isConsuloOrPluginProject(project, module);
    }

    @Override
    @RequiredUIAccess
    public void invoke(@Nonnull Project project, Editor editor, PsiFile psiFile) throws IncorrectOperationException {
        PsiJavaModule javaModule = findModule(editor, psiFile);
        if (javaModule == null) {
            return;
        }

        Module module = ModuleUtilCore.findModuleForFile(psiFile);
        if (module == null) {
            return;
        }

        ContentFolder[] folders = ModuleRootManager.getInstance(module).getContentFolders(LanguageContentFolderScopes.production());

        List<VirtualFile> packageDirectories = new ArrayList<>();
        for (ContentFolder folder : folders) {
            VirtualFile contentFile = folder.getFile();
            if (contentFile == null) {
                continue;
            }

            VirtualFileUtil.visitChildrenRecursively(contentFile, new VirtualFileVisitor<>() {
                @Override
                public boolean visitFile(@Nonnull VirtualFile file) {
                    if (file.isDirectory() && file != contentFile) {
                        packageDirectories.add(file);
                    }
                    return true;
                }
            });
        }

        PsiManager psiManager = PsiManager.getInstance(project);

        Set<String> alreadyExported = new HashSet<>();
        for (PsiPackageAccessibilityStatement statement : javaModule.getExports()) {
            alreadyExported.add(statement.getPackageName());
        }

        Set<String> packages = new TreeSet<>(Comparator.reverseOrder());
        for (VirtualFile packageDirectory : packageDirectories) {
            PsiDirectory directory = psiManager.findDirectory(packageDirectory);
            if (directory == null) {
                continue;
            }

            PsiPackage psiPackage = PsiPackageManager.getInstance(project).findPackage(directory, JavaModuleExtension.class);
            if (psiPackage instanceof PsiJavaPackage psiJavaPackage) {
                PsiClass[] classes = psiJavaPackage.getClasses(GlobalSearchScope.moduleScope(module));
                if (classes.length == 0 || alreadyExported.contains(psiPackage.getQualifiedName())) {
                    continue;
                }

                packages.add(psiPackage.getQualifiedName());
            }
        }

        com.intellij.java.language.psi.PsiElementFactory psiElementFactory =
            com.intellij.java.language.psi.PsiElementFactory.getInstance(project);
        WriteAction.run(() -> {
            PsiElement anchor = getLastItem(javaModule.getExports());
            if (anchor == null) {
                ASTNode lbrace = javaModule.getNode().findChildByType(JavaTokenType.RBRACE);
                anchor = lbrace == null ? null : lbrace.getPsi();
            }

            if (anchor == null) {
                return;
            }

            for (String aPackage : packages) {
                PsiStatement statement = psiElementFactory.createModuleStatementFromText("exports " + aPackage + ";", psiFile);

                anchor = javaModule.addBefore(statement, anchor);
            }
        });
    }

    private static <T> T getLastItem(Iterable<T> iterable) {
        T element = null;
        for (T t : iterable) {
            element = t;
        }
        return element;
    }

    @RequiredReadAction
    private PsiJavaModule findModule(Editor editor, PsiFile psiFile) {
        PsiElement element = psiFile.getViewProvider().findElementAt(editor.getCaretModel().getOffset());
        if (element == null || PsiUtilCore.getElementType(element) != JavaTokenType.IDENTIFIER) {
            return null;
        }
        return PsiTreeUtil.getParentOfType(element, PsiJavaModule.class);
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}

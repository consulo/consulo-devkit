package consulo.devkit.localize.folding;

import com.intellij.java.language.JavaLanguage;
import com.intellij.java.language.psi.*;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.matcher.NameUtil;
import consulo.devkit.localize.index.LocalizeFileIndexExtension;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.editor.folding.FoldingBuilder;
import consulo.language.editor.folding.FoldingDescriptor;
import consulo.language.editor.folding.NamedFoldingDescriptor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.util.LanguageCachedValueUtil;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.yaml.psi.*;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @author UNV
 * @since 2020-06-01
 */
@ExtensionImpl
public class LocalizeFoldingBuilder implements FoldingBuilder {
    @RequiredReadAction
    @Nonnull
    @Override
    public FoldingDescriptor[] buildFoldRegions(@Nonnull ASTNode astNode, @Nonnull Document document) {
        PsiElement psi = astNode.getPsi();

        List<FoldingDescriptor> foldings = new ArrayList<>();

        psi.accept(new JavaRecursiveElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitMethodCallExpression(@Nonnull PsiMethodCallExpression methodCall) {
                super.visitMethodCallExpression(methodCall);

                String value = matchLocalizeKeyOf(methodCall);
                if (value == null) {
                    value = matchLocalizeKeyGetValue(methodCall);
                }
                if (value == null) {
                    value = matchLocalizeClassMethodCall(methodCall);
                }
                if (value == null) {
                    return;
                }

                foldings.add(new NamedFoldingDescriptor(methodCall.getNode(), methodCall.getTextRange(), null, value));
            }
        });

        return foldings.toArray(FoldingDescriptor.EMPTY);
    }

    /**
     * Checks if provided method call expression represents {@code LocalizeKey} definition {@code LocalizeKey.of(ID, "localize.key")}.
     *
     * @param methodCall Method call expression to analyze.
     * @return LocalizeValue text extracted by method call expression or null.
     */
    @Nullable
    @RequiredReadAction
    private String matchLocalizeKeyOf(@Nonnull PsiMethodCallExpression methodCall) {
        PsiReferenceExpression methodExpr = methodCall.getMethodExpression();

        if ("of".equals(methodExpr.getReferenceName())
            && methodExpr.getQualifierExpression() instanceof PsiReferenceExpression qualRefExpr
            && "LocalizeKey".equals(qualRefExpr.getReferenceName())) {

            PsiExpression[] args = methodCall.getArgumentList().getExpressions();
            if (args.length >= 2
                && args[1] instanceof PsiLiteralExpression keyExpr
                && keyExpr.getValue() instanceof String key
                && args[0] instanceof PsiReferenceExpression fileNameExpr
                && fileNameExpr.resolve() instanceof PsiField fileNameField) {

                Map<String, String> map = findLocalizeMap(methodExpr, fileNameField.getContainingClass());

                return map == null ? null : map.get(key);
            }
        }

        return null;
    }

    /**
     * Checks if provided method call expression represents getting {@code LocalizeKey} value {@code LOCALIZE_KEY_CONSTANT.getValue()}.
     *
     * @param methodCall Method call expression to analyze.
     * @return LocalizeValue text extracted by method call expression or null.
     */
    @Nullable
    @RequiredReadAction
    private String matchLocalizeKeyGetValue(@Nonnull PsiMethodCallExpression methodCall) {
        PsiReferenceExpression methodExpr = methodCall.getMethodExpression();

        if ("getValue".equals(methodExpr.getReferenceName())
            && methodExpr.getQualifierExpression() instanceof PsiReferenceExpression qualRefExpr
            && qualRefExpr.resolve() instanceof PsiField field
            && field.isStatic() && field.isFinal()) {
            PsiClass psiClass = field.getContainingClass();
            String className = psiClass == null ? null : psiClass.getName();
            if (className != null && className.endsWith("Localize")) {
                LocalizeResolveInfo localizeInfo = findLocalizeInfo(methodExpr, psiClass, field.getName());
                return localizeInfo == null ? null : localizeInfo.value();
            }
        }

        return null;
    }

    /**
     * Checks if provided method call expression represents getting {@code LocalizeValue} constant {@code XxLocalize.localizeKey()}.
     *
     * @param methodCall Method call expression to analyze.
     * @return LocalizeValue text extracted by method call expression or null.
     */
    @Nullable
    @RequiredReadAction
    private String matchLocalizeClassMethodCall(@Nonnull PsiMethodCallExpression methodCall) {
        PsiReferenceExpression methodExpr = methodCall.getMethodExpression();

        if (methodExpr.getQualifierExpression() instanceof PsiReferenceExpression qualRefExpr) {
            String className = qualRefExpr.getReferenceName();

            if (className != null && className.endsWith("Localize")) {
                PsiElement possibleClass = qualRefExpr.resolve();

                LocalizeResolveInfo info = findLocalizeInfo(methodExpr, possibleClass, methodExpr.getReferenceName());
                if (info != null) {
                    return info.value();
                }
            }
        }
        return null;
    }

    @Nullable
    @RequiredReadAction
    public static LocalizeResolveInfo findLocalizeInfo(PsiElement scope, PsiElement possibleClass, String memberName) {
        YAMLFile yamlFile = findYAMLFile(scope, possibleClass);
        if (yamlFile == null) {
            return null;
        }

        Map<String, String> map = buildLocalizeCache(yamlFile);

        String key = replaceCamelCase(memberName);
        String value = map.get(key);

        return value == null ? null : new LocalizeResolveInfo(yamlFile, key, value);
    }

    @Nullable
    @RequiredReadAction
    private static Map<String, String> findLocalizeMap(PsiElement scope, PsiElement possibleClass) {
        YAMLFile yamlFile = findYAMLFile(scope, possibleClass);
        return yamlFile == null ? null : buildLocalizeCache(yamlFile);
    }

    @Nullable
    @RequiredReadAction
    private static YAMLFile findYAMLFile(PsiElement scope, PsiElement possibleClass) {
        if (!(possibleClass instanceof PsiClass psiClass)) {
            return null;
        }

        String qualifiedName = psiClass.getQualifiedName();

        if (qualifiedName == null) {
            return null;
        }

        Collection<VirtualFile> containingFiles = FileBasedIndex.getInstance().getContainingFiles(
            LocalizeFileIndexExtension.INDEX,
            qualifiedName,
            scope.getResolveScope()
        );

        if (containingFiles.isEmpty()) {
            return null;
        }

        VirtualFile item = ContainerUtil.getFirstItem(containingFiles);
        assert item != null;

        PsiFile file = PsiManager.getInstance(scope.getProject()).findFile(item);
        return file instanceof YAMLFile yamlFile ? yamlFile : null;
    }

    public static Map<String, String> buildLocalizeCache(YAMLFile yamlFile) {
        return LanguageCachedValueUtil.getCachedValue(
            yamlFile,
            () -> {
                List<YAMLDocument> documents = yamlFile.getDocuments();

                Map<String, String> map = new HashMap<>();

                for (YAMLDocument document : documents) {
                    if (document.getTopLevelValue() instanceof YAMLMapping topLevelMapping) {
                        for (YAMLKeyValue value : topLevelMapping.getKeyValues()) {
                            if (value.getValue() instanceof YAMLMapping valueMapping) {
                                YAMLKeyValue text = valueMapping.getKeyValueByKey("text");
                                if (text != null) {
                                    String key = value.getKeyText();
                                    map.put(key.toLowerCase(Locale.ROOT), text.getValueText());
                                }
                            }
                        }
                    }
                }

                return CachedValueProvider.Result.create(map, yamlFile);
            }
        );
    }

    private static String replaceCamelCase(String camelCaseString) {
        return Arrays.stream(NameUtil.splitNameIntoWords(camelCaseString))
            .map(s -> s.toLowerCase(Locale.US))
            .collect(Collectors.joining("."));
    }

    @RequiredReadAction
    @Nullable
    @Override
    public String getPlaceholderText(@Nonnull ASTNode astNode) {
        return null;
    }

    @RequiredReadAction
    @Override
    public boolean isCollapsedByDefault(@Nonnull ASTNode astNode) {
        return true;
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return JavaLanguage.INSTANCE;
    }
}

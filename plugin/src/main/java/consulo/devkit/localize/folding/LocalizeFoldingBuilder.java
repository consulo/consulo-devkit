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
import consulo.util.lang.Couple;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.yaml.psi.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author VISTALL
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
            public void visitMethodCallExpression(@Nonnull PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);

                PsiReferenceExpression methodExpression = expression.getMethodExpression();

                if ("getValue".equals(methodExpression.getReferenceName())) {
                    if (methodExpression.getQualifierExpression() instanceof PsiReferenceExpression referenceExpression
                        && referenceExpression.resolve() instanceof PsiField field
                        && field.hasModifierProperty(PsiModifier.STATIC)
                        && field.hasModifierProperty(PsiModifier.FINAL)) {
                        PsiClass psiClass = field.getContainingClass();
                        String className = psiClass.getName();
                        if (className != null && className.endsWith("Localize")) {
                            LocalizeResolveInfo localizeInfo = findLocalizeInfo(methodExpression, psiClass, field.getName());
                            if (localizeInfo == null) {
                                return;
                            }

                            foldings.add(new NamedFoldingDescriptor(
                                expression.getNode(),
                                expression.getTextRange(),
                                null,
                                localizeInfo.value()
                            ));
                        }
                    }
                }
                else {
                    Couple<String> localizeInfo = findLocalizeInfo(methodExpression);
                    if (localizeInfo == null) {
                        return;
                    }

                    foldings.add(new NamedFoldingDescriptor(
                        expression.getNode(),
                        expression.getTextRange(),
                        null,
                        localizeInfo.getSecond()
                    ));
                }
            }
        });

        return foldings.toArray(FoldingDescriptor.EMPTY);
    }

    @RequiredReadAction
    @Nullable
    private Couple<String> findLocalizeInfo(@Nullable PsiReferenceExpression expression) {
        if (expression == null) {
            return null;
        }

        PsiExpression qualifierExpression = expression.getQualifierExpression();
        if (qualifierExpression instanceof PsiReferenceExpression referenceExpression) {
            String className = referenceExpression.getReferenceName();

            if (className != null && className.endsWith("Localize")) {
                PsiElement possibleClass = referenceExpression.resolve();

                LocalizeResolveInfo info = findLocalizeInfo(expression, possibleClass, expression.getReferenceName());
                if (info != null) {
                    return Couple.of(info.key(), info.value());
                }
            }
        }
        return null;
    }

    @Nullable
    @RequiredReadAction
    public static LocalizeResolveInfo findLocalizeInfo(PsiElement scope, PsiElement possibleClass, String memberName) {
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
        if (file instanceof YAMLFile yamlFile) {
            Map<String, String> map = buildLocalizeCache(yamlFile);

            String key = replaceCamelCase(memberName);

            String value = map.get(key);
            if (value == null) {
                return null;
            }

            return new LocalizeResolveInfo(yamlFile, key, value);
        }

        return null;
    }

    private static Map<String, String> buildLocalizeCache(YAMLFile yamlFile) {
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

    public static String replaceCamelCase(String camelCaseString) {
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

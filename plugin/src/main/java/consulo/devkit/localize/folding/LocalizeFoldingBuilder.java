package consulo.devkit.localize.folding;

import com.intellij.java.language.JavaLanguage;
import com.intellij.java.language.psi.*;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.matcher.NameUtil;
import consulo.devkit.localize.index.LocalizeFileBasedIndexExtension;
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
import consulo.util.lang.StringUtil;
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
      public void visitMethodCallExpression(PsiMethodCallExpression expression) {
        PsiReferenceExpression methodExpression = expression.getMethodExpression();

        super.visitMethodCallExpression(expression);

        if ("getValue".equals(methodExpression.getReferenceName())) {
          PsiExpression qualifierExpression = methodExpression.getQualifierExpression();

          if (qualifierExpression instanceof PsiMethodCallExpression) {
            Couple<String> localizeInfo = findLocalizeInfo(((PsiMethodCallExpression)qualifierExpression).getMethodExpression(), true);
            if (localizeInfo == null) {
              return;
            }

            foldings.add(new NamedFoldingDescriptor(expression.getNode(), expression.getTextRange(), null, localizeInfo.getSecond()));
          }
        }
        else {
          PsiElement parent = expression.getParent();
          if (parent instanceof PsiReferenceExpression && "getValue".equals(((PsiReferenceExpression)parent).getReferenceName())) {
            return;
          }

          Couple<String> localizeInfo = findLocalizeInfo(methodExpression, true);
          if (localizeInfo == null) {
            return;
          }

          foldings.add(new NamedFoldingDescriptor(expression.getNode(), expression.getTextRange(), null, localizeInfo.getSecond()));
        }
      }
    });

    return foldings.toArray(FoldingDescriptor.EMPTY);
  }

  @RequiredReadAction
  @Nullable
  private Couple<String> findLocalizeInfo(@Nullable PsiReferenceExpression expression, boolean resolve) {
    if (expression == null) {
      return null;
    }

    PsiExpression qualifierExpression = expression.getQualifierExpression();
    if (!(qualifierExpression instanceof PsiReferenceExpression)) {
      return null;
    }

    String referenceName = ((PsiReferenceExpression)qualifierExpression).getReferenceName();

    if (referenceName != null && StringUtil.endsWith(referenceName, "Localize")) {
      PsiElement element = ((PsiReferenceExpression)qualifierExpression).resolve();

      if (element instanceof PsiClass) {
        String qualifiedName = ((PsiClass)element).getQualifiedName();

        if (qualifiedName == null) {
          return null;
        }

        Collection<VirtualFile> containingFiles = FileBasedIndex.getInstance()
                                                                .getContainingFiles(LocalizeFileBasedIndexExtension.INDEX,
                                                                                    qualifiedName,
                                                                                    expression.getResolveScope());

        if (containingFiles.isEmpty()) {
          return null;
        }

        if (resolve) {
          VirtualFile item = ContainerUtil.getFirstItem(containingFiles);
          assert item != null;

          PsiFile file = PsiManager.getInstance(expression.getProject()).findFile(item);
          if (file instanceof YAMLFile) {
            Map<String, String> map = buildLocalizeCache((YAMLFile)file);

            String key = replaceCamelCase(expression.getReferenceName());

            String value = map.get(key);
            if (value == null) {
              return null;
            }

            return Couple.of(key, value);
          }
        }
        else {
          return Couple.of("", "");
        }
      }
    }

    return null;
  }

  private static Map<String, String> buildLocalizeCache(YAMLFile yamlFile) {
    return LanguageCachedValueUtil.getCachedValue(yamlFile, () ->
    {
      List<YAMLDocument> documents = yamlFile.getDocuments();

      Map<String, String> map = new HashMap<>();

      for (YAMLDocument document : documents) {
        YAMLValue topLevelValue = document.getTopLevelValue();
        if (topLevelValue instanceof YAMLMapping) {
          for (YAMLKeyValue value : ((YAMLMapping)topLevelValue).getKeyValues()) {
            String key = value.getKeyText();

            YAMLValue yamlValue = value.getValue();
            if (yamlValue instanceof YAMLMapping) {
              YAMLKeyValue text = ((YAMLMapping)yamlValue).getKeyValueByKey("text");
              if (text != null) {
                map.put(key.toLowerCase(Locale.ROOT), text.getValueText());
              }
            }
          }
        }
      }

      return CachedValueProvider.Result.create(map, yamlFile);
    });
  }

  private static String replaceCamelCase(String camelCaseString) {
    String[] strings = NameUtil.splitNameIntoWords(camelCaseString);
    return Arrays.stream(strings).map(s -> s.toLowerCase(Locale.US)).collect(Collectors.joining("."));
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

package consulo.devkit.localize.inspection;

import com.intellij.java.language.impl.psi.impl.JavaConstantExpressionEvaluator;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.java.language.psi.search.PsiShortNamesCache;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.WriteAction;
import consulo.component.util.localize.AbstractBundle;
import consulo.language.editor.inspection.LocalQuickFixOnPsiElement;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;

import javax.lang.model.SourceVersion;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:nikolay@yurchenko.su">Nikolay Yurchenko</a>
 * @since 2024-06-07
 */
@ExtensionImpl
public class BundleMessageToLocalizeInspection extends InternalInspection {
  @SuppressWarnings("deprecation")

  @Nonnull
  @Override
  public String getDisplayName() {
    return "XxxBundle.message() to XxxLocalize inspection";
  }

  @Nonnull
  @Override
  public PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
    return new BundleCallVisitor(holder);
  }

  public static String getString(PsiExpression expr) {
    return (String)getObject(expr);
  }

  private static Object getObject(PsiExpression expr) {
    return JavaConstantExpressionEvaluator.computeConstantExpression(expr, true);
  }

  private static class BundleCallVisitor extends JavaElementVisitor {
    private static final String MESSAGE = "message";
    private static final String ABSTRACT_BUNDLE_CLASS_NAME = AbstractBundle.class.getName();
    private static final Pattern BUNDLE_SUFFIX_REGEX = Pattern.compile("^(.*?)Bundle$");

    private final ProblemsHolder holder;

    private BundleCallVisitor(ProblemsHolder holder) {
      this.holder = holder;
    }

    @Override
    @RequiredReadAction
    public void visitMethodCallExpression(@Nonnull PsiMethodCallExpression expression) {
      BundleCallVisit bundleCallVisit = new BundleCallVisit(expression);

      if (!bundleCallVisit.isApplicable()) {
        return;
      }

      bundleCallVisit.registerProblem();
    }

    @SuppressWarnings("RequiredXAction")
    private class BundleCallVisit {
      @Nonnull
      private final PsiMethodCallExpression expression;
      @Nonnull
      private final PsiReferenceExpression methodExpression;
      private final PsiElement method;
      private final PsiClass psiClass;
      @Nonnull
      private final String className;

      public BundleCallVisit(@Nonnull PsiMethodCallExpression expression) {
        this.expression = expression;
        this.methodExpression = expression.getMethodExpression();
        this.method = methodExpression.resolve();
        PsiElement parent = (this.method == null) ? null : method.getParent();
        this.psiClass = (parent instanceof PsiClass psiClass) ? psiClass : null;
        this.className = StringUtil.notNullize(this.psiClass == null ? null : this.psiClass.getName());
      }

      public boolean isApplicable() {
        return methodNameIsMessage() && classNameEndsWithBundle() && classExtendsAbstractBundle();
      }

      public boolean methodNameIsMessage() {
        return MESSAGE.equals(methodExpression.getReferenceName());
      }

      public boolean classNameEndsWithBundle() {
        return BUNDLE_SUFFIX_REGEX.matcher(className).matches();
      }

      public boolean classExtendsAbstractBundle() {
        PsiReferenceList extendsList = psiClass == null ? null : psiClass.getExtendsList();
        return extendsList != null
          && Arrays.asList(extendsList.getReferenceElements()).stream()
          .anyMatch(p -> ABSTRACT_BUNDLE_CLASS_NAME.equals(p.getQualifiedName()));
      }

      public void registerProblem() {
        String localizeClassName = BUNDLE_SUFFIX_REGEX.matcher(className).replaceAll("$1Localize");

        Project project = expression.getProject();
        PsiClass[] classes = PsiShortNamesCache.getInstance(project)
          .getClassesByName(localizeClassName, GlobalSearchScope.projectScope(project));
        if (classes.length != 1) {
          return;
        }

        String localizeClassQualifiedName = classes[0].getQualifiedName();

        PsiExpression[] argExpressions = expression.getArgumentList().getExpressions();
        if (argExpressions.length < 1) {
          return;
        }

        String key = getString(argExpressions[0]);
        String localizeMethodName = BundleKeyTransformer.toLocalizeMethodName(key);

        StringBuilder codeBlock = new StringBuilder()
          .append(localizeClassQualifiedName)
          .append(".").append(localizeMethodName).append('(');

        for (int i = 1, n = argExpressions.length; i < n; i++) {
          PsiExpression argExpression = argExpressions[i];
          if (i > 1) codeBlock.append(", ");
          codeBlock.append(argExpression.getText());
        }

        codeBlock.append(").get()");

        holder.registerProblem(
          expression,
          LocalizeValue.localizeTODO("Replace with " + localizeClassQualifiedName + "." + localizeMethodName).get(),
          new Fix(expression, codeBlock.toString())
        );
      }
    }
  }

  private static class Fix extends LocalQuickFixOnPsiElement {
    private final String replacement;

    protected Fix(@Nonnull PsiElement element, String replacement) {
      super(element);
      this.replacement = replacement;
    }

    @Nonnull
    @Override
    public String getText() {
      return LocalizeValue.localizeTODO("Replace with XxxLocalize").get();
    }

    @Override
    public void invoke(
      @Nonnull Project project,
      @Nonnull PsiFile psiFile,
      @Nonnull PsiElement expression,
      @Nonnull PsiElement endElement

    ) {
      PsiExpression newExpression = JavaPsiFacade.getElementFactory(project)
        .createExpressionFromText(replacement, expression);

      WriteAction.run(() -> {
        PsiElement newElement = expression.replace(newExpression);

        JavaCodeStyleManager.getInstance(project).shortenClassReferences(newElement);
      });
    }

    @Nls
    @Nonnull
    @Override
    public String getFamilyName() {
      return "DevKit";
    }
  }

  private static interface BundleKeyTransformer {
    public static String toLocalizeMethodName(String key) {
      return normalizeName(capitalizeByDot(key));
    }

    private static String normalizeName(String text) {
      char c = text.charAt(0);
      if (c == '0') {
        return "zero" + text.substring(1, text.length());
      }
      else if (c == '1') {
        return "one" + text.substring(1, text.length());
      }
      else if (c == '2') {
        return "two" + text.substring(1, text.length());
      }
      return escapeString(text);
    }

    private static String escapeString(String name) {
      return SourceVersion.isName(name) ? name : "_" + name;
    }

    private static String capitalizeByDot(String key) {
      String[] split = key.replace(" ", ".").split("\\.");

      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < split.length; i++) {
        if (i != 0) {
          builder.append(StringUtil.capitalize(split[i]));
        }
        else {
          builder.append(split[i]);
        }
      }

      return builder.toString();
    }
  }
}

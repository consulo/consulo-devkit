package consulo.devkit.localize.inspection;

import com.intellij.java.language.impl.psi.impl.JavaConstantExpressionEvaluator;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.java.language.psi.search.PsiShortNamesCache;
import com.intellij.java.language.psi.util.InheritanceUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.WriteAction;
import consulo.component.util.localize.AbstractBundle;
import consulo.devkit.localize.DevKitLocalize;
import consulo.language.editor.inspection.LocalQuickFixOnPsiElement;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;

/**
 * @author <a href="mailto:nikolay@yurchenko.su">Nikolay Yurchenko</a>
 * @since 2024-06-07
 */
@ExtensionImpl
public class BundleMessageToLocalizeInspection extends InternalInspection {

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
    private final ProblemsHolder holder;

    private BundleCallVisitor(ProblemsHolder holder) {
      this.holder = holder;
    }

    @Override
    @RequiredReadAction
    public void visitMethodCallExpression(@Nonnull PsiMethodCallExpression expression) {
      new TransformToLocalizeInspector(expression).registerProblem();
    }

    private class TransformToLocalizeInspector extends LocalizeClassExistsChecker {
      protected String replacementCodeBlock;
      protected LocalizeValue inspectionName;

      protected TransformToLocalizeInspector(@Nonnull PsiMethodCallExpression expression) {
        super(expression);
      }

      public void registerProblem() {
        if (!isApplicable()) {
          return;
        }

        initReplacementCodeBlock();

        inspectionName = DevKitLocalize.inspectionsReplaceWithXxxlocalize(localizeClassName, localizeMethodName);

        holder.registerProblem(
          expression,
          inspectionName.get(),
          new TransformToLocalizeFix(expression, inspectionName, replacementCodeBlock)
        );
      }

      @SuppressWarnings("RequiredXAction")
      private void initReplacementCodeBlock() {
        StringBuilder codeBlock = new StringBuilder()
          .append(localizeClassQualifiedName)
          .append('.').append(localizeMethodName).append('(');

        for (int i = 1, n = argExpressions.length; i < n; i++) {
          PsiExpression argExpression = argExpressions[i];
          if (i > 1) codeBlock.append(", ");
          codeBlock.append(argExpression.getText());
        }

        codeBlock.append(").get()");

        replacementCodeBlock = codeBlock.toString();
      }
    }
  }

  private static class TransformToLocalizeFix extends LocalQuickFixOnPsiElement {
    protected final LocalizeValue inspectionName;
    protected final String replacementCodeBlock;

    protected TransformToLocalizeFix(
      @Nonnull PsiElement element,
      LocalizeValue inspectionName,
      String replacementCodeBlock
    ) {
      super(element);
      this.inspectionName = inspectionName;
      this.replacementCodeBlock = replacementCodeBlock;
    }

    @Nonnull
    @Override
    public String getText() {
      return inspectionName.get();
    }

    @Override
    public void invoke(
      @Nonnull Project project,
      @Nonnull PsiFile psiFile,
      @Nonnull PsiElement expression,
      @Nonnull PsiElement endElement
    ) {
      PsiExpression newExpression = JavaPsiFacade.getElementFactory(project)
        .createExpressionFromText(replacementCodeBlock, expression);

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

  private abstract static class MethodCallExpressionChecker {
    @Nonnull
    protected final PsiMethodCallExpression expression;
    @Nonnull
    protected final PsiReferenceExpression methodExpression;
    @Nonnull
    protected Project project;

    protected MethodCallExpressionChecker(@Nonnull PsiMethodCallExpression expression) {
      this.expression = expression;
      this.methodExpression = expression.getMethodExpression();
      this.project = expression.getProject();
    }

    abstract boolean isApplicable();
  }

  private static class CallsBundleMessageChecker extends MethodCallExpressionChecker {
    protected PsiExpression[] argExpressions;

    protected CallsBundleMessageChecker(@Nonnull PsiMethodCallExpression expression) {
      super(expression);
    }

    @Override
    @SuppressWarnings("RequiredXAction")
    boolean isApplicable() {
      if (!"message".equals(methodExpression.getReferenceName())) {
        return false;
      }

      PsiElement qualifier = methodExpression.getQualifier();
      if (qualifier == null || !qualifier.getText().endsWith("Bundle")) {
        return false;
      }

      argExpressions = expression.getArgumentList().getExpressions();
      return argExpressions.length >= 1;
    }
  }

  private static class ClassExtendsAbstractBundleChecker extends CallsBundleMessageChecker {
    @SuppressWarnings("deprecation")
    private static final String ABSTRACT_BUNDLE_CLASS_NAME = AbstractBundle.class.getName();

    protected PsiElement method;
    protected PsiClass psiClass;

    protected ClassExtendsAbstractBundleChecker(@Nonnull PsiMethodCallExpression expression) {
      super(expression);
    }

    @Override
    @SuppressWarnings("RequiredXAction")
    boolean isApplicable() {
      if (!super.isApplicable()) {
        return false;
      }

      this.method = methodExpression.resolve();
      PsiElement parent = (this.method == null) ? null : method.getParent();
      this.psiClass = (parent instanceof PsiClass psiClass) ? psiClass : null;

      return psiClass != null && InheritanceUtil.isInheritor(psiClass, ABSTRACT_BUNDLE_CLASS_NAME);
    }
  }

  private static class LocalizeClassExistsChecker extends ClassExtendsAbstractBundleChecker {
    protected String className, localizeClassName, localizeClassQualifiedName, localizeMethodName;

    protected LocalizeClassExistsChecker(@Nonnull PsiMethodCallExpression expression) {
      super(expression);
    }

    @Override
    @SuppressWarnings({"RequiredXAction", "ConstantConditions"})
    boolean isApplicable() {
      if (!super.isApplicable()) {
        return false;
      }

      this.className = this.psiClass.getName();

      this.localizeClassName =
        className.substring(0, className.length() - "Bundle".length()) + "Localize";

      PsiClass[] classes = PsiShortNamesCache.getInstance(project)
        .getClassesByName(localizeClassName, expression.getResolveScope());
      if (classes.length != 1) {
        return false;
      }

      this.localizeClassQualifiedName = classes[0].getQualifiedName();

      String key = getString(argExpressions[0]);
      localizeMethodName = normalizeName(capitalizeByDot(key));

      return true;
    }

    private String normalizeName(String text) {
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

    private String escapeString(String name) {
      return PsiNameHelper.getInstance(project).isIdentifier(name) ? name : "_" + name;
    }

    private String capitalizeByDot(String key) {
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

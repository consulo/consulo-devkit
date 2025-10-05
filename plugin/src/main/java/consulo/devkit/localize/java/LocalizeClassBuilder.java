package consulo.devkit.localize.java;

import com.ibm.icu.text.MessageFormat;
import com.intellij.java.language.impl.psi.impl.light.LightPsiClassBuilder;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiModifier;
import com.intellij.java.language.psi.PsiType;
import consulo.devkit.localize.LocalizeUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.resolve.PsiScopeProcessor;
import consulo.language.psi.resolve.ResolveState;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;

import java.text.Format;
import java.util.List;
import java.util.Locale;

/**
 * @author VISTALL
 * @since 2025-10-02
 */
public class LocalizeClassBuilder extends LightPsiClassBuilder {
    private final String myQualifiedName;

    @Nonnull
    private final YAMLFile myYamlFile;

    private boolean myInitialized;

    public LocalizeClassBuilder(@Nonnull YAMLFile yamlFile,
                                @Nonnull String qualifiedName) {
        super(yamlFile, StringUtil.getShortName(qualifiedName));
        myYamlFile = yamlFile;

        getModifierList().addModifier(PsiModifier.PUBLIC);

        setNavigationElement(yamlFile);

        myQualifiedName = qualifiedName;
    }

    private void buildMethods() {
        if (myInitialized) {
            return;
        }

        myInitialized = true;

        List<YAMLDocument> documents = myYamlFile.getDocuments();

        PsiManager manager = getManager();

        GlobalSearchScope resolveScope = myYamlFile.getResolveScope();

        Project project = myYamlFile.getProject();

        for (YAMLDocument document : documents) {
            if (document.getTopLevelValue() instanceof YAMLMapping topLevelMapping) {
                for (YAMLKeyValue value : topLevelMapping.getKeyValues()) {
                    if (value.getValue() instanceof YAMLMapping valueMapping) {
                        YAMLKeyValue text = valueMapping.getKeyValueByKey("text");
                        if (text != null) {
                            String key = value.getKeyText();
                            String valueText = text.getValueText();

                            String localizeText = StringUtil.notNullize(valueText);

                            MessageFormat format = new MessageFormat(localizeText, Locale.US);

                            Format[] formatsByArgumentIndex = format.getFormatsByArgumentIndex();

                            String methodName = LocalizeUtil.formatMethodName(project, key);

                            LocalizeMethodBuilder builder = new LocalizeMethodBuilder(this, value, methodName, localizeText);

                            if (formatsByArgumentIndex.length > 0) {
                                for (int i = 0; i < formatsByArgumentIndex.length; i++) {
                                    builder.addParameter("p" + i, PsiType.getJavaLangObject(manager, resolveScope));
                                }
                            }

                            addMethod(builder);
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean processDeclarations(@Nonnull PsiScopeProcessor processor, @Nonnull ResolveState state, PsiElement lastParent, @Nonnull PsiElement place) {
        buildMethods();
        return super.processDeclarations(processor, state, lastParent, place);
    }

    @Nonnull
    @Override
    public PsiMethod[] getMethods() {
        buildMethods();
        return super.getMethods();
    }

    @Override
    public PsiFile getContainingFile() {
        return myYamlFile;
    }

    @Nullable
    @Override
    public String getQualifiedName() {
        return myQualifiedName;
    }
}

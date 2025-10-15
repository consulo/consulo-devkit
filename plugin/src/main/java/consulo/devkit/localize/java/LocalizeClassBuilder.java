package consulo.devkit.localize.java;

import com.ibm.icu.text.MessageFormat;
import com.intellij.java.language.impl.psi.impl.light.LightFieldBuilder;
import com.intellij.java.language.impl.psi.impl.light.LightMethodBuilder;
import com.intellij.java.language.impl.psi.impl.light.LightPsiClassBuilder;
import com.intellij.java.language.psi.*;
import consulo.devkit.localize.LocalizeUtil;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;

import java.text.Format;
import java.util.ArrayList;
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

    private List<PsiMethod> myMethods = List.of();

    private PsiField myIdField;

    public LocalizeClassBuilder(@Nonnull YAMLFile yamlFile,
                                @Nonnull String qualifiedName) {
        super(yamlFile, StringUtil.getShortName(qualifiedName));
        
        myYamlFile = yamlFile;

        PsiClassType stringType = PsiType.getJavaLangString(yamlFile.getManager(), yamlFile.getResolveScope());

        LightFieldBuilder idBuilder = new LightFieldBuilder("ID", stringType, yamlFile);
        idBuilder.setContainingClass(this);
        idBuilder.setModifiers(PsiModifier.PUBLIC, PsiModifier.STATIC, PsiModifier.FINAL);

        myIdField = idBuilder;

        getModifierList().addModifier(PsiModifier.PUBLIC);

        setNavigationElement(yamlFile);

        myQualifiedName = qualifiedName;
    }

    private void buildMethods() {
        if (myInitialized) {
            return;
        }

        myMethods = collectMethods();

        for (PsiMethod method : myMethods) {
            if (method instanceof LightMethodBuilder) {
                ((LightMethodBuilder) method).setContainingClass(this);
            }
        }

        myInitialized = true;
    }

    private List<PsiMethod> collectMethods() {
        List<PsiMethod> methods = new ArrayList<>();

        List<YAMLDocument> documents = myYamlFile.getDocuments();

        PsiManager manager = getManager();

        GlobalSearchScope resolveScope = myYamlFile.getResolveScope();

        Project project = myYamlFile.getProject();

        PsiClassType type = PsiElementFactory.getInstance(project).createTypeByFQClassName(LocalizeValue.class.getName(), resolveScope);

        PsiClassType javaLangObject = PsiType.getJavaLangObject(manager, resolveScope);

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

                            LocalizeMethodBuilder builder = new LocalizeMethodBuilder(this, value, methodName, localizeText, type);

                            if (formatsByArgumentIndex.length > 0) {
                                for (int i = 0; i < formatsByArgumentIndex.length; i++) {
                                    builder.addParameter("p" + i, javaLangObject);
                                }
                            }

                            methods.add(builder);
                        }
                    }
                }
            }
        }
        
        return methods;
    }

    @Nonnull
    @Override
    public PsiField[] getFields() {
        return new PsiField[]{myIdField};
    }

    @Nonnull
    @Override
    public PsiMethod[] getMethods() {
        buildMethods();

        return myMethods.toArray(PsiMethod.EMPTY_ARRAY);
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

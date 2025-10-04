package consulo.devkit.localize.java;

import com.ibm.icu.text.MessageFormat;
import com.intellij.java.language.impl.psi.impl.light.LightPsiClassBuilder;
import com.intellij.java.language.psi.PsiType;
import consulo.devkit.localize.LocalizeUtil;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
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
    private final String myPackageName;
    @Nonnull
    private final YAMLFile myYamlFile;

    public LocalizeClassBuilder(@Nonnull YAMLFile yamlFile,
                                @Nonnull String qualifiedName) {
        super(yamlFile, StringUtil.getShortName(qualifiedName));
        myYamlFile = yamlFile;
        setNavigationElement(yamlFile);

        myPackageName = qualifiedName;

        List<YAMLDocument> documents = yamlFile.getDocuments();

        PsiManager manager = getManager();
        GlobalSearchScope resolveScope = yamlFile.getResolveScope();

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

                            String methodName = LocalizeUtil.formatMethodName(yamlFile.getProject(), key);

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
    public PsiFile getContainingFile() {
        return myYamlFile;
    }

    @Nullable
    @Override
    public String getQualifiedName() {
        return myPackageName;
    }
}

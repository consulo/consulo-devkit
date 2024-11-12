package consulo.devkit.localize;

import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiNameHelper;
import consulo.annotation.access.RequiredReadAction;
import consulo.localize.LocalizeManager;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.Locale;

/**
 * @author VISTALL
 * @since 2024-09-08
 */
public class LocalizeUtil {
    public static final Locale DEFAULT_LOCALE = Locale.US;
    private static final String DEFAULT_LOCALE_STR = DEFAULT_LOCALE.toString();
    private static final String LOCALIZE_LIB_DIR = "LOCALIZE-LIB";

    private static final String ZERO_PREFIX = "zero";
    private static final String ONE_PREFIX = "one";
    private static final String TWO_PREFIX = "two";

    public static final String TEXT_KEY = "text";

    @RequiredReadAction
    @Nullable
    public static PsiMethod findMethodByYAMLKey(YAMLKeyValue element) {
        String keyText = element.getKeyText();

        VirtualFile file = element.getContainingFile().getVirtualFile();

        String fileName = file.getNameWithoutExtension();
        String packageName = StringUtil.getPackageName(fileName);
        String className = packageName + ".localize." + StringUtil.getShortName(fileName);

        Project project = element.getProject();
        PsiClass localizeClass = JavaPsiFacade.getInstance(project).findClass(className, element.getResolveScope());
        if (localizeClass != null) {
            PsiMethod[] methods = localizeClass.findMethodsByName(LocalizeUtil.formatMethodName(project, keyText), false);
            if (methods.length > 0) {
                return methods[0];
            }
        }

        return null;
    }

    @Nullable
    public static Locale extractLocaleFromFile(@Nullable VirtualFile file) {
        if (file == null || file.getFileType() != YAMLFileType.YML) {
            return null;
        }

        CharSequence nameSequence = file.getNameSequence();

        if (StringUtil.endsWith(nameSequence, "Localize.yaml")) {
            VirtualFile parentDir = file.getParent();
            if (parentDir != null) {
                Locale locale = null;
                try {
                    locale = LocalizeManager.get().parseLocale(parentDir.getName());
                }
                catch (Exception e) {
                    return null;
                }

                VirtualFile localizeLibParent = parentDir.getParent();
                if (localizeLibParent != null && LOCALIZE_LIB_DIR.equals(localizeLibParent.getName())) {
                    return locale;
                }
            }
        }
        return null;
    }

    public static boolean isDefaultLocalizeFile(@Nullable VirtualFile file) {
        if (file == null || file.getFileType() != YAMLFileType.YML) {
            return false;
        }

        CharSequence nameSequence = file.getNameSequence();

        if (StringUtil.endsWith(nameSequence, "Localize.yaml")) {
            VirtualFile parentDir = file.getParent();
            if (parentDir != null && DEFAULT_LOCALE_STR.equals(parentDir.getName())) {
                VirtualFile localizeLibParent = parentDir.getParent();
                if (localizeLibParent != null && LOCALIZE_LIB_DIR.equals(localizeLibParent.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String formatMethodName(Project project, String key) {
        return normalizeName(project, capitalizeByDot(key));
    }

    private static String capitalizeByDot(String key) {
        String[] split = key.toLowerCase(Locale.ROOT).replace(" ", ".").split("\\.");

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

    private static String normalizeName(Project project, String text) {
        char c = text.charAt(0);
        if (c == '0') {
            return ZERO_PREFIX + text.substring(1, text.length());
        }
        else if (c == '1') {
            return ONE_PREFIX + text.substring(1, text.length());
        }
        else if (c == '2') {
            return TWO_PREFIX + text.substring(1, text.length());
        }
        return escapeString(project, text);
    }

    private static String escapeString(Project project, String name) {
        return PsiNameHelper.getInstance(project).isIdentifier(name) ? name : "_" + name;
    }
}

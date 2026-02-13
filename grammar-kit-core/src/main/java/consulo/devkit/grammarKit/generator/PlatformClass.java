package consulo.devkit.grammarKit.generator;

/**
 * @author VISTALL
 * @since 2022-03-25
 */
public enum PlatformClass {
    PSI_ELEMENT("consulo.language.psi.PsiElement"),
    PSI_TREE_UTIL("consulo.language.psi.util.PsiTreeUtil"),
    AST_NODE("consulo.language.ast.ASTNode"),
    AST_WRAPPER_PSI_ELEMENT("consulo.language.impl.psi.ASTWrapperPsiElement"),
    COMPOSITE_PSI_ELEMENT("consulo.language.impl.psi.CompositePsiElement"),
    TOKEN_SET("consulo.language.ast.TokenSet"),
    LANGUAGE("consulo.language.Language"),
    LANGUAGE_VERSION("consulo.language.version.LanguageVersion"),
    PSI_BUILDER("consulo.language.parser.PsiBuilder"),
    PSI_PARSER("consulo.language.parser.PsiParser"),
    PSI_ELEMENT_VISITOR("consulo.language.psi.PsiElementVisitor"),
    STUB_BASED_PSI_ELEMENT("consulo.language.psi.StubBasedPsiElement"),
    STUB_BASED_PSI_ELEMENT_BASE("com.intellij.extapi.psi.StubBasedPsiElementBase"),
    ISTUB_ELEMENT_TYPE("consulo.language.psi.stub.IStubElementType"),
    GENERATED_PARSER_UTIL_BASE("consulo.language.impl.parser.GeneratedParserUtilBase"),
    IELEMENT_TYPE("consulo.language.ast.IElementType");

    private final String myClassNameV3;

    PlatformClass(String v3) {
        myClassNameV3 = v3;
    }

    public String select(String version) {
        return myClassNameV3;
    }
}

package consulo.devkit.grammarKit.generator;

/**
 * @author VISTALL
 * @since 25-Mar-22
 */
public enum PlatformClass
{
	PSI_ELEMENT("com.intellij.psi.PsiElement", "consulo.language.psi.PsiElement"),
	PSI_TREE_UTIL("com.intellij.psi.util.PsiTreeUtil", "consulo.language.psi.util.PsiTreeUtil"),
	AST_NODE("com.intellij.lang.ASTNode", "consulo.language.ast.ASTNode"),
	AST_WRAPPER_PSI_ELEMENT("com.intellij.extapi.psi.ASTWrapperPsiElement", "consulo.language.impl.psi.ASTWrapperPsiElement"),
	COMPOSITE_PSI_ELEMENT("com.intellij.psi.impl.source.tree.CompositePsiElement", "consulo.language.impl.psi.CompositePsiElement"),
	TOKEN_SET("com.intellij.psi.tree.TokenSet", "consulo.language.ast.TokenSet"),
	LANGUAGE("com.intellij.lang.Language", "consulo.language.Language"),
	LANGUAGE_VERSION("consulo.lang.LanguageVersion", "consulo.language.version.LanguageVersion"),
	PSI_BUILDER("com.intellij.lang.PsiBuilder", "consulo.language.parser.PsiBuilder"),
	PSI_PARSER("com.intellij.lang.PsiParser", "consulo.language.parser.PsiParser"),
	PSI_ELEMENT_VISITOR("com.intellij.psi.PsiElementVisitor", "consulo.language.psi.PsiElementVisitor"),
	STUB_BASED_PSI_ELEMENT("com.intellij.psi.StubBasedPsiElement", "consulo.language.psi.StubBasedPsiElement"),
	STUB_BASED_PSI_ELEMENT_BASE("com.intellij.extapi.psi.StubBasedPsiElementBase", "com.intellij.extapi.psi.StubBasedPsiElementBase"),
	ISTUB_ELEMENT_TYPE("com.intellij.psi.stubs.IStubElementType", "consulo.language.psi.stub.IStubElementType"),
	GENERATED_PARSER_UTIL_BASE("com.intellij.lang.parser.GeneratedParserUtilBase", "consulo.language.impl.parser.GeneratedParserUtilBase"),
	IELEMENT_TYPE("com.intellij.psi.tree.IElementType", "consulo.language.ast.IElementType");

	private final String myClassNameV2;
	private final String myClassNameV3;

	PlatformClass(String v2, String v3)
	{
		myClassNameV2 = v2;
		myClassNameV3 = v3;
	}

	public String getClassNameV2()
	{
		return myClassNameV2;
	}

	public String getClassNameV3()
	{
		return myClassNameV3;
	}

	public String select(String version)
	{
		if("3".equals(version))
		{
			return myClassNameV3;
		}

		return myClassNameV2;
	}
}

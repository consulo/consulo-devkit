package consulo.devkit.localize.folding;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.lang.folding.NamedFoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndex;
import consulo.annotation.access.RequiredReadAction;
import consulo.devkit.localize.index.LocalizeFileBasedIndexExtension;
import org.jetbrains.yaml.psi.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 2020-06-01
 */
public class LocalizeFoldingBuilder implements FoldingBuilder
{
	@RequiredReadAction
	@Nonnull
	@Override
	public FoldingDescriptor[] buildFoldRegions(@Nonnull ASTNode astNode, @Nonnull Document document)
	{
		PsiElement psi = astNode.getPsi();

		List<FoldingDescriptor> foldings = new ArrayList<>();

		psi.accept(new JavaRecursiveElementVisitor()
		{
			@Override
			@RequiredReadAction
			public void visitMethodCallExpression(PsiMethodCallExpression expression)
			{
				PsiReferenceExpression methodExpression = expression.getMethodExpression();

				super.visitMethodCallExpression(expression);

				if("getValue".equals(methodExpression.getReferenceName()))
				{
					PsiExpression qualifierExpression = methodExpression.getQualifierExpression();

					if(qualifierExpression instanceof PsiMethodCallExpression)
					{
						Couple<String> localizeInfo = findLocalizeInfo(((PsiMethodCallExpression) qualifierExpression).getMethodExpression(), true);
						if(localizeInfo == null)
						{
							return;
						}

						foldings.add(new NamedFoldingDescriptor(expression.getNode(), expression.getTextRange(), null, localizeInfo.getSecond()));
					}
				}
				else
				{
					PsiElement parent = expression.getParent();
					if(parent instanceof PsiReferenceExpression && "getValue".equals(((PsiReferenceExpression) parent).getReferenceName()))
					{
						return;
					}

					Couple<String> localizeInfo = findLocalizeInfo(methodExpression, true);
					if(localizeInfo == null)
					{
						return;
					}

					foldings.add(new NamedFoldingDescriptor(expression.getNode(), expression.getTextRange(), null, localizeInfo.getSecond()));
				}
			}
		});

		return ContainerUtil.toArray(foldings, FoldingDescriptor.EMPTY);
	}

	@RequiredReadAction
	@Nullable
	private Couple<String> findLocalizeInfo(@Nullable PsiReferenceExpression expression, boolean resolve)
	{
		if(expression == null)
		{
			return null;
		}

		PsiExpression qualifierExpression = expression.getQualifierExpression();
		if(!(qualifierExpression instanceof PsiReferenceExpression))
		{
			return null;
		}

		String referenceName = ((PsiReferenceExpression) qualifierExpression).getReferenceName();

		if(referenceName != null && StringUtil.endsWith(referenceName, "Localize"))
		{
			PsiElement element = ((PsiReferenceExpression) qualifierExpression).resolve();

			if(element instanceof PsiClass)
			{
				String qualifiedName = ((PsiClass) element).getQualifiedName();

				if(qualifiedName == null)
				{
					return null;
				}

				Collection<VirtualFile> containingFiles = FileBasedIndex.getInstance().getContainingFiles(LocalizeFileBasedIndexExtension.INDEX, qualifiedName, expression.getResolveScope());

				if(containingFiles.isEmpty())
				{
					return null;
				}

				if(resolve)
				{
					VirtualFile item = ContainerUtil.getFirstItem(containingFiles);
					assert item != null;

					PsiFile file = PsiManager.getInstance(expression.getProject()).findFile(item);
					if(file instanceof YAMLFile)
					{
						Map<String, String> map = buildLocalizeCache((YAMLFile) file);

						String key = replaceCamelCase(expression.getReferenceName());

						String value = map.get(key);
						if(value == null)
						{
							return null;
						}

						return Couple.of(key, value);
					}
				}
				else
				{
					return Couple.of("", "");
				}
			}
		}

		return null;
	}

	private static Map<String, String> buildLocalizeCache(YAMLFile yamlFile)
	{
		return CachedValuesManager.getCachedValue(yamlFile, () ->
		{
			List<YAMLDocument> documents = yamlFile.getDocuments();

			Map<String, String> map = new HashMap<>();

			for(YAMLDocument document : documents)
			{
				YAMLValue topLevelValue = document.getTopLevelValue();
				if(topLevelValue instanceof YAMLMapping)
				{
					for(YAMLKeyValue value : ((YAMLMapping) topLevelValue).getKeyValues())
					{
						String key = value.getKeyText();

						YAMLValue yamlValue = value.getValue();
						if(yamlValue instanceof YAMLMapping)
						{
							YAMLKeyValue text = ((YAMLMapping) yamlValue).getKeyValueByKey("text");
							if(text != null)
							{
								map.put(key, text.getValueText());
							}
						}
					}
				}
			}

			return CachedValueProvider.Result.create(map, yamlFile);
		});
	}

	private static String replaceCamelCase(String camelCaseString)
	{
		String[] strings = NameUtil.splitNameIntoWords(camelCaseString);
		return Arrays.stream(strings).map(s -> s.toLowerCase(Locale.US)).collect(Collectors.joining("."));
	}

	@RequiredReadAction
	@Nullable
	@Override
	public String getPlaceholderText(@Nonnull ASTNode astNode)
	{
		return null;
	}

	@RequiredReadAction
	@Override
	public boolean isCollapsedByDefault(@Nonnull ASTNode astNode)
	{
		return true;
	}
}

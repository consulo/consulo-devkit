package consulo.devkit.icon.references;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.icon.IconResolver;
import consulo.devkit.icon.index.IconFileBasedIndexExtension;
import consulo.language.Language;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.*;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.util.ProcessingContext;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.patterns.XmlAttributeValuePattern;
import consulo.xml.patterns.XmlPatterns;
import consulo.xml.psi.xml.XmlAttributeValue;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author VISTALL
 * @since 2020-10-15
 */
@ExtensionImpl
public class IconPsiReferenceContributor extends PsiReferenceContributor {
  @Override
  public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
    final XmlAttributeValuePattern pluginXml = XmlPatterns.xmlAttributeValue().withLocalName("icon");

    registrar.registerReferenceProvider(pluginXml, new PsiReferenceProvider() {
      @Nonnull
      @Override
      public PsiReference[] getReferencesByElement(@Nonnull final PsiElement element, @Nonnull ProcessingContext context) {
        return new PsiReference[]{
          new PsiReferenceBase<PsiElement>(element, true) {
            @RequiredReadAction
            @Override
            public PsiElement resolve() {
              PsiElement psiElement = getElement();
              String value = ((XmlAttributeValue)psiElement).getValue();
              if (value.contains("@")) {
                Collection<VirtualFile> containingFiles =
                  FileBasedIndex.getInstance().getContainingFiles(IconFileBasedIndexExtension.INDEX, value, GlobalSearchScope
                    .allScope
                      (psiElement.getProject()));

                if (!containingFiles.isEmpty()) {
                  PsiManager psiManager = PsiManager.getInstance(psiElement.getProject());

                  PsiFile file = psiManager.findFile(containingFiles.iterator().next());
                  if (file != null) {
                    return file;
                  }
                }
              }

              return null;
            }

            @RequiredReadAction
            @Nonnull
            @Override
            public Object[] getVariants() {
              FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();
              IconResolver iconResolver = IconResolver.getInstance(getElement().getProject());

              Collection<String> keys = fileBasedIndex.getAllKeys(IconFileBasedIndexExtension.INDEX, getElement().getProject());

              List<Object> list = new ArrayList<>(keys.size());

              for (String key : keys) {
                String[] args = key.split("@");

                String groupId = args[0];
                String imageId = args[1];

                LookupElementBuilder lookup = LookupElementBuilder.create(key);
                lookup = lookup.withPresentableText(imageId);
                lookup = lookup.withTypeText(groupId, true);

                Collection<VirtualFile> files =
                  fileBasedIndex.getContainingFiles(IconFileBasedIndexExtension.INDEX, key, GlobalSearchScope.allScope(getElement()
                                                                                                                         .getProject()));

                if (!files.isEmpty()) {
                  Image image = iconResolver.getImage(files);
                  lookup = lookup.withIcon(image);
                }

                list.add(lookup);
              }
              return list.toArray(Object[]::new);
            }
          }
        };
      }
    });
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return XMLLanguage.INSTANCE;
  }
}

package consulo.devkit.grammarKit.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.language.icon.IconDescriptor;
import consulo.language.icon.IconDescriptorUpdater;
import consulo.language.psi.PsiElement;
import consulo.ui.image.Image;
import consulo.util.lang.Comparing;
import org.intellij.grammar.BnfIcons;
import org.intellij.grammar.psi.BnfAttr;
import org.intellij.grammar.psi.BnfAttrs;
import org.intellij.grammar.psi.BnfModifier;
import org.intellij.grammar.psi.BnfRule;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 12.09.13.
 */
@ExtensionImpl
public class BnfIconDescriptorUpdater implements IconDescriptorUpdater {
  @RequiredReadAction
  @Override
  public void updateIcon(@Nonnull IconDescriptor iconDescriptor, @Nonnull PsiElement element, int flags) {
    if (element instanceof BnfRule rule) {
      final Image base = hasModifier(rule, "external") ? BnfIcons.EXTERNAL_RULE : BnfIcons.RULE;
      final Image visibility = hasModifier(rule, "private") ? AllIcons.Nodes.C_private : AllIcons.Nodes.C_public;

      iconDescriptor.setMainIcon(base);
      iconDescriptor.setRightIcon(visibility);
    }
    else if (element instanceof BnfAttr) {
      iconDescriptor.setMainIcon(BnfIcons.ATTRIBUTE);
    }
    else if (element instanceof BnfAttrs) {
      iconDescriptor.setMainIcon(AllIcons.Nodes.Package);
    }
  }

  private static boolean hasModifier(BnfRule bnfRule, String modifier) {
    for (BnfModifier bnfModifier : bnfRule.getModifierList()) {
      if (Comparing.equal(bnfModifier.getText(), modifier)) {
        return true;
      }
    }
    return false;
  }
}

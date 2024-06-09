/**
 * @author VISTALL
 * @since 10/02/2023
 */
open module consulo.devkit {
  requires consulo.java;
  requires com.intellij.xml;
  requires org.jetbrains.plugins.yaml;
  requires com.intellij.properties;
  requires org.jetbrains.idea.maven;

  requires consulo.java.coverage.impl;
  requires consulo.java.execution.impl;

  requires org.yaml.snakeyaml;
  requires dtdparser;
  requires consulo.internal.jdi;

  requires xercesImpl;

  // TODO remove in future
  requires java.desktop;
  requires forms.rt;

  exports consulo.devkit;
  exports consulo.devkit.action;
  exports consulo.devkit.codeInsight.daemon;
  exports consulo.devkit.codeInsight.generation;
  exports consulo.devkit.dom;
  exports consulo.devkit.dom.impl;
  exports consulo.devkit.icon;
  exports consulo.devkit.inspections;
  exports consulo.devkit.inspections.inject;
  exports consulo.devkit.inspections.internal;
  exports consulo.devkit.inspections.requiredXAction;
  exports consulo.devkit.inspections.requiredXAction.stateResolver;
  exports consulo.devkit.inspections.util.service;
  exports consulo.devkit.inspections.valhalla;
  exports consulo.devkit.intentation;
  exports consulo.devkit.localize.folding;
  exports consulo.devkit.localize.index;
  exports consulo.devkit.localize.inspection;
  exports consulo.devkit.maven;
  exports consulo.devkit.module.extension;
  exports consulo.devkit.requires.dom;
  exports consulo.devkit.run;
  exports consulo.devkit.util;
  exports org.jetbrains.idea.devkit;
  exports org.jetbrains.idea.devkit.actions;
  exports org.jetbrains.idea.devkit.build;
  exports org.jetbrains.idea.devkit.dom;
  exports org.jetbrains.idea.devkit.dom.generator;
  exports org.jetbrains.idea.devkit.dom.impl;
  exports org.jetbrains.idea.devkit.inspections;
  exports org.jetbrains.idea.devkit.inspections.internal;
  exports org.jetbrains.idea.devkit.inspections.quickfix;
  exports org.jetbrains.idea.devkit.navigation;
  exports org.jetbrains.idea.devkit.run;
  exports org.jetbrains.idea.devkit.sdk;
  exports org.jetbrains.idea.devkit.util;
}
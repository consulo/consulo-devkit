/*
 * Copyright 2011-present Greg Shrago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.intellij.grammar.impl.livePreview;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.devkit.grammarKit.localize.BnfLocalize;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.internal.org.objectweb.asm.*;
import consulo.language.Language;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.pointer.VirtualFilePointer;
import consulo.virtualFileSystem.pointer.VirtualFilePointerManager;
import jakarta.annotation.Nullable;
import org.intellij.grammar.psi.BnfFile;

import jakarta.annotation.Nonnull;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static consulo.internal.org.objectweb.asm.Opcodes.*;

/**
 * @author gregsh
 */
public class LivePreviewLanguage extends Language {
    private final VirtualFilePointer myFilePointer;
    private final SoftReference<BnfFile> myBnfFile;
    public static final Language BASE_INSTANCE = new Language("BNF_LP") {
        @Override
        public LocalizeValue getDisplayName() {
            return BnfLocalize.bnfPreviewLanguageDisplayName();
        }
    };

    private static final MyClassLoader ourClassLoader = new MyClassLoader();

    protected LivePreviewLanguage(@Nonnull BnfFile grammarFile) {
        super(BASE_INSTANCE, ObjectUtil.assertNotNull(grammarFile.getVirtualFile()).getPath());
        VirtualFile virtualFile = ObjectUtil.assertNotNull(grammarFile.getVirtualFile());
        Application app = grammarFile.getProject().getApplication();
        if (app.isUnitTestMode()) {
            myBnfFile = new SoftReference<>(grammarFile);
            myFilePointer = null;
        }
        else {
            myFilePointer = VirtualFilePointerManager.getInstance().create(virtualFile, app, null);
            myBnfFile = null;
        }
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        VirtualFile file = getGrammarFile();
        return LocalizeValue.localizeTODO(file == null ? getID() : "'" + file.getName() + "' grammar");
    }

    @Nullable
    public VirtualFile getGrammarFile() {
        if (myBnfFile != null) {
            BnfFile file = myBnfFile.get();
            return file == null ? null : file.getVirtualFile();
        }
        else {
            return myFilePointer.getFile();
        }
    }

    @Nullable
    @RequiredReadAction
    public BnfFile getGrammar(@Nullable Project project) {
        if (myBnfFile != null) {
            return myBnfFile.get();
        }
        VirtualFile file = project == null ? null : getGrammarFile();
        PsiFile psiFile = file == null ? null : PsiManager.getInstance(project).findFile(file);
        return psiFile instanceof BnfFile ? (BnfFile)psiFile : null;
    }

    @Nonnull
    public static LivePreviewLanguage newInstance(PsiFile psiFile) {
        try {
            return (LivePreviewLanguage)ourClassLoader.createClass().getDeclaredConstructors()[0].newInstance(psiFile);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public static LivePreviewLanguage findInstance(PsiFile psiFile) {
        VirtualFile vFile = psiFile.getVirtualFile();
        if (vFile == null) {
            return null;
        }
        for (Language language : Language.getRegisteredLanguages()) {
            if (language instanceof LivePreviewLanguage &&
                vFile.equals(((LivePreviewLanguage)language).getGrammarFile())) {
                return (LivePreviewLanguage)language;
            }
        }
        return null;
    }

    @Nonnull
    public List<Editor> getGrammarEditors(@Nonnull Project project) {
        VirtualFile file = getGrammarFile();
        if (file == null) {
            return Collections.emptyList();
        }
        FileEditor[] editors = FileEditorManager.getInstance(project).getAllEditors(file);
        if (editors.length == 0) {
            return Collections.emptyList();
        }
        List<Editor> result = new ArrayList<>();
        for (FileEditor editor : editors) {
            if (editor instanceof TextEditor) {
                result.add(((TextEditor)editor).getEditor());
            }
        }
        return result;
    }

    @Nonnull
    @RequiredReadAction
    public List<Editor> getPreviewEditors(@Nonnull Project project) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        VirtualFile[] files = fileEditorManager.getOpenFiles();
        if (files.length == 0) {
            return Collections.emptyList();
        }
        List<Editor> result = new ArrayList<>();
        PsiManager psiManager = PsiManager.getInstance(project);
        for (VirtualFile file : files) {
            PsiFile psiFile = psiManager.findFile(file);
            Language language = psiFile == null ? null : psiFile.getLanguage();
            if (language == this) {
                for (FileEditor editor : fileEditorManager.getAllEditors(file)) {
                    if (editor instanceof TextEditor textEditor) {
                        result.add(textEditor.getEditor());
                    }
                }
            }
        }
        return result;
    }

    private static class MyClassLoader extends ClassLoader {
        private final AtomicInteger myCounter = new AtomicInteger();

        MyClassLoader() {
            super(LivePreviewHelper.class.getClassLoader());
        }

        Class<LivePreviewLanguage> createClass() throws Exception {
            int index = myCounter.incrementAndGet();
            byte[] b = dump(index);
            return (Class<LivePreviewLanguage>)defineClass(LivePreviewLanguage.class.getName() + index, b, 0, b.length);
        }

        public static byte[] dump(int index) throws Exception {
            // TG there's ASM Bytecode plugin!
            ClassWriter cw = new ClassWriter(0);
            FieldVisitor fv;
            MethodVisitor mv;
            AnnotationVisitor av0;

            cw.visit(
                V1_6,
                ACC_PUBLIC + ACC_SUPER,
                "org/intellij/grammar/livePreview/LivePreviewLanguage" + index,
                null,
                "org/intellij/grammar/livePreview/LivePreviewLanguage",
                null
            );

            cw.visitSource("LivePreviewLanguage1.java", null);

            {
                mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(Lorg/intellij/grammar/psi/BnfFile;)V", null, null);
                {
                    av0 = mv.visitParameterAnnotation(0, "Lorg/jetbrains/annotations/NotNull;", false);
                    av0.visitEnd();
                }
                mv.visitCode();
                Label l0 = new Label();
                mv.visitLabel(l0);
                mv.visitVarInsn(ALOAD, 1);
                Label l1 = new Label();
                mv.visitJumpInsn(IFNONNULL, l1);
                mv.visitTypeInsn(NEW, "java/lang/IllegalArgumentException");
                mv.visitInsn(DUP);
                mv.visitLdcInsn(
                    "Argument 0 for @NotNull parameter of org/intellij/grammar/livePreview/LivePreviewLanguage1.<init>" +
                        " must not be null"
                );
                mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V");
                mv.visitInsn(ATHROW);
                mv.visitLabel(l1);
                mv.visitLineNumber(11, l1);
                mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(
                    INVOKESPECIAL,
                    "org/intellij/grammar/livePreview/LivePreviewLanguage",
                    "<init>",
                    "(Lorg/intellij/grammar/psi/BnfFile;)V"
                );
                Label l2 = new Label();
                mv.visitLabel(l2);
                mv.visitLineNumber(12, l2);
                mv.visitInsn(RETURN);
                Label l3 = new Label();
                mv.visitLabel(l3);
                mv.visitLocalVariable(
                    "this",
                    "Lorg/intellij/grammar/livePreview/LivePreviewLanguage1;",
                    null,
                    l0,
                    l3,
                    0
                );
                mv.visitLocalVariable("grammarFile", "Lorg/intellij/grammar/psi/BnfFile;", null, l0, l3, 1);
                mv.visitMaxs(3, 2);
                mv.visitEnd();
            }
            cw.visitEnd();

            return cw.toByteArray();
        }
    }
}

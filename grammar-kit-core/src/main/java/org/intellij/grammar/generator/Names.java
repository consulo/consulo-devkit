/*
 * Copyright 2011-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.grammar.generator;

import consulo.application.Application;

import jakarta.annotation.Nonnull;

/**
 * @author greg
 */
class Names {
    public final String builder;
    public final String version;
    public final String level;
    public final String marker;
    public final String pinned;
    public final String result;
    public final String pos;
    public final String root;
    public final String priority;
    public final String metaParamPrefix;
    public final String psiLocal = "p";

    private Names(
        String builder,
        String version,
        String level,
        String marker,
        String pinned,
        String result,
        String pos,
        String root,
        String priority,
        String metaParamPrefix
    ) {
        this.builder = builder;
        this.version = version;
        this.level = level;
        this.marker = marker;
        this.pinned = pinned;
        this.result = result;
        this.pos = pos;
        this.root = root;
        this.priority = priority;
        this.metaParamPrefix = metaParamPrefix;
    }

    public static Names classicNames() {
        return new Names(
            "builder_",
            "version_",
            "level_",
            "marker_",
            "pinned_",
            "result_",
            "pos_",
            "root_",
            "priority_",
            ""
        );
    }

    public static Names longNames() {
        return new Names(
            "builder",
            "version",
            "level",
            "marker",
            "pinned",
            "result",
            "pos",
            "type",
            "priority",
            "a"
        );
    }

    public static Names shortNames() {
        return new Names(
            "b",
            "v",
            "l",
            "m",
            "p",
            "r",
            "c",
            "t",
            "g",
            "_"
        );
    }

    @Nonnull
    public static Names forName(String name) {
        if ("long".equals(name)) {
            return longNames();
        }
        if ("short".equals(name)) {
            return shortNames();
        }
        if ("classic".equals(name)) {
            return classicNames();
        }
        return Application.get().isUnitTestMode() ? classicNames() : shortNames();
    }
}

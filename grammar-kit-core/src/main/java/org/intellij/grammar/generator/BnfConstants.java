/*
 * Copyright 2011-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.grammar.generator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author gregsh
 */
public interface BnfConstants
{
	String BNF_DISPLAY_NAME = "Grammar-Kit BNF";
	String LP_DISPLAY_NAME = "Grammar-Kit Live Preview";

	String GENERATION_GROUP = "Grammar Generator";

	String CLASS_HEADER_DEF = "// This is a generated file. Not intended for manual editing.";

	String REGEXP_PREFIX = "regexp:";

	String OVERRIDE_ANNO = "@java.lang.Override";
	String NOTNULL_ANNO = "@" + Nonnull.class.getName();
	String NULLABLE_ANNO = "@" + Nullable.class.getName();
	String SUPPRESS_WARNINGS_ANNO = "@java.lang.SuppressWarnings";

	String RECOVER_AUTO = "#auto";

	String TOKEN_SET_HOLDER_NAME = "TokenSets";
}

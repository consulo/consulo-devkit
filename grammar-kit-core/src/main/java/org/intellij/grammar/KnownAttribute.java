/*
 * Copyright 2011-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.grammar;

import consulo.devkit.grammarKit.generator.PlatformClass;
import consulo.devkit.grammarKit.generator.PlatformClassKnownAttribute;
import consulo.util.io.FileUtil;
import consulo.util.lang.Pair;
import org.intellij.grammar.generator.BnfConstants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author gregsh
 */
@SuppressWarnings("StaticVariableOfConcreteClass")
public class KnownAttribute<T> {
    private static final Map<String, KnownAttribute> ourAttributes = new TreeMap<>();

    @Nonnull
    public static Collection<KnownAttribute> getAttributes() {
        return Collections.unmodifiableCollection(ourAttributes.values());
    }

    @Nullable
    public static KnownAttribute getAttribute(@Nullable String name) {
        return name == null ? null : ourAttributes.get(name);
    }

    private static final ListValue EMPTY_LIST = new ListValue();

    public static final KnownAttribute<String> CLASS_HEADER =
        create(true, String.class, "classHeader", BnfConstants.CLASS_HEADER_DEF);
    public static final KnownAttribute<ListValue> GENERATE = create(true, ListValue.class, "generate", EMPTY_LIST);
    public static final KnownAttribute<Boolean> GENERATE_PSI = create(true, Boolean.class, "generatePsi", true);
    public static final KnownAttribute<Boolean> GENERATE_TOKENS = create(true, Boolean.class, "generateTokens", true);
    public static final KnownAttribute<Boolean> GENERATE_TOKEN_ACCESSORS =
        create(true, Boolean.class, "generateTokenAccessors", false);
    public static final KnownAttribute<Integer> GENERATE_FIRST_CHECK =
        create(true, Integer.class, "generateFirstCheck", 2);
    public static final KnownAttribute<Boolean> EXTENDED_PIN = create(true, Boolean.class, "extendedPin", true);

    public static final KnownAttribute<ListValue> PARSER_IMPORTS = create(true, ListValue.class, "parserImports", EMPTY_LIST);
    public static final KnownAttribute<String> VERSION = create(true, String.class, "version", "");
    public static final KnownAttribute<String> PSI_CLASS_PREFIX = create(true, String.class, "psiClassPrefix", "");
    public static final KnownAttribute<String> PSI_IMPL_CLASS_SUFFIX =
        create(true, String.class, "psiImplClassSuffix", "Impl");
    public static final KnownAttribute<String> PSI_TREE_UTIL_CLASS =
        create(true, String.class, "psiTreeUtilClass", PlatformClass.PSI_TREE_UTIL);
    public static final KnownAttribute<String> PSI_PACKAGE =
        create(true, String.class, "psiPackage", "generated.psi");
    public static final KnownAttribute<String> PSI_IMPL_PACKAGE =
        create(true, String.class, "psiImplPackage", "generated.psi.impl");
    public static final KnownAttribute<String> PSI_VISITOR_NAME =
        create(true, String.class, "psiVisitorName", "Visitor");
    public static final KnownAttribute<String> PSI_IMPL_UTIL_CLASS =
        create(true, String.class, "psiImplUtilClass", (String)null);
    public static final KnownAttribute<String> ELEMENT_TYPE_CLASS =
        create(true, String.class, "elementTypeClass", PlatformClass.IELEMENT_TYPE);
    public static final KnownAttribute<String> TOKEN_TYPE_CLASS =
        create(true, String.class, "tokenTypeClass", PlatformClass.IELEMENT_TYPE);
    public static final KnownAttribute<String> PARSER_CLASS =
        create(true, String.class, "parserClass", "generated.GeneratedParser");
    public static final KnownAttribute<String> PARSER_UTIL_CLASS =
        create(true, String.class, "parserUtilClass", PlatformClass.GENERATED_PARSER_UTIL_BASE);
    public static final KnownAttribute<String> ELEMENT_TYPE_HOLDER_CLASS =
        create(true, String.class, "elementTypeHolderClass", "generated.GeneratedTypes");
    public static final KnownAttribute<String> ELEMENT_TYPE_PREFIX =
        create(true, String.class, "elementTypePrefix", "");
    public static final KnownAttribute<String> ELEMENT_TYPE_FACTORY =
        create(true, String.class, "elementTypeFactory", (String)null);
    public static final KnownAttribute<String> TOKEN_TYPE_FACTORY =
        create(true, String.class, "tokenTypeFactory", (String)null);

    public static final KnownAttribute<String> EXTENDS =
        create(false, String.class, "extends", PlatformClass.AST_WRAPPER_PSI_ELEMENT);
    public static final KnownAttribute<ListValue> IMPLEMENTS =
        create(false, ListValue.class, "implements", ListValue.singleValue(null, PlatformClass.PSI_ELEMENT));
    public static final KnownAttribute<String> ELEMENT_TYPE = create(false, String.class, "elementType", (String)null);
    public static final KnownAttribute<Object> PIN = create(false, Object.class, "pin", (Object)(-1));
    public static final KnownAttribute<String> MIXIN = create(false, String.class, "mixin", (String)null);
    public static final KnownAttribute<String> RECOVER_WHILE = create(false, String.class, "recoverWhile", (String)null);
    public static final KnownAttribute<String> NAME = create(false, String.class, "name", (String)null);

    public static final KnownAttribute<Boolean> EXTRA_ROOT = create(false, Boolean.class, "extraRoot", false);
    public static final KnownAttribute<Boolean> RIGHT_ASSOCIATIVE =
        create(false, Boolean.class, "rightAssociative", false);
    public static final KnownAttribute<String> CONSUME_TOKEN_METHOD =
        create(false, String.class, "consumeTokenMethod", "consumeToken");

    public static final KnownAttribute<String> STUB_CLASS = create(false, String.class, "stubClass", (String)null);

    public static final KnownAttribute<ListValue> METHODS = create(false, ListValue.class, "methods", EMPTY_LIST);
    public static final KnownAttribute<ListValue> HOOKS = create(false, ListValue.class, "hooks", EMPTY_LIST);
    public static final KnownAttribute<ListValue> TOKENS = create(true, ListValue.class, "tokens", EMPTY_LIST);

    private final boolean myGlobal;
    private final String myName;
    private final Class<T> myClazz;
    private final T myDefaultValue;

    public static <T> KnownAttribute<T> create(Class<T> clazz, String name, @Nullable T defaultValue) {
        return new KnownAttribute<>(name, clazz, defaultValue);
    }

    public static KnownAttribute<String> create(boolean global, Class<String> clazz, String name, @Nonnull PlatformClass platformClass) {
        if (clazz != String.class) {
            throw new IllegalArgumentException(clazz.toString());
        }
        return new PlatformClassKnownAttribute(global, name, platformClass);
    }

    private static <T> KnownAttribute<T> create(boolean global, Class<T> clazz, String name, @Nullable T defaultValue) {
        return new KnownAttribute<>(global, name, clazz, defaultValue);
    }

    protected KnownAttribute(String name, Class<T> clazz, T defaultValue) {
        myName = name;
        myClazz = clazz;
        myDefaultValue = defaultValue;
        myGlobal = false;
    }

    protected KnownAttribute(boolean global, String name, Class<T> clazz, T defaultValue) {
        myName = name;
        myClazz = clazz;
        myDefaultValue = defaultValue;
        myGlobal = global;
        KnownAttribute prev = ourAttributes.put(name, this);
        assert prev == null : name + " attribute already defined";
    }

    @Nonnull
    public String getName() {
        return myName;
    }

    public boolean isGlobal() {
        return myGlobal;
    }

    public T getDefaultValue(String version) {
        return myDefaultValue;
    }

    public T ensureValue(Object o, String version) {
        if (o == null) {
            return getDefaultValue(version);
        }
        if (myClazz == ListValue.class && o instanceof String) {
            return (T)ListValue.singleValue(null, (String)o);
        }
        if (myClazz.isInstance(o)) {
            return (T)o;
        }
        return getDefaultValue(version);
    }

    @Override
    public String toString() {
        return myName;
    }

    public String getDescription() {
        try {
            InputStream resourceAsStream = getClass().getResourceAsStream("/messages/attributeDescriptions/" + getName() + ".html");
            return resourceAsStream == null ? null : FileUtil.loadTextAndClose(resourceAsStream);
        }
        catch (IOException e) {
            return null;
        }
    }

    // returns a non-registered attribute for migration purposes
    @Nonnull
    public KnownAttribute<T> alias(String deprecatedName) {
        return new KnownAttribute<>(deprecatedName, myClazz, null);
    }

    @Nullable
    public static KnownAttribute getCompatibleAttribute(String name) {
        return getAttribute(name);
    }

    public static class ListValueObject {
        private final Object myValue;

        public ListValueObject(@Nonnull Object value) {
            myValue = value;
        }

        public String getStringValue(String version) {
            if (myValue instanceof PlatformClass) {
                return ((PlatformClass)myValue).select(version);
            }
            return (String)myValue;
        }
    }

    public static class ListValue extends LinkedList<Pair<ListValueObject, ListValueObject>> {
        @Nonnull
        public static ListValue singleValue(Object s1, Object s2) {
            ListValue t = new ListValue();
            t.add(Pair.create(s1 == null ? null : new ListValueObject(s1), s2 == null ? null : new ListValueObject(s2)));
            return t;
        }

        @Nonnull
        public List<String> asStrings(String version) {
            List<String> t = new ArrayList<>();
            for (Pair<ListValueObject, ListValueObject> pair : this) {
                if (pair.first != null) {
                    t.add(pair.first.getStringValue(version));
                }
                else if (pair.second != null) {
                    t.add(pair.second.getStringValue(version));
                }
            }
            return t;
        }

        @Nonnull
        public Map<String, String> asMap(String version) {
            return asMap(version, false);
        }

        @Nonnull
        public Map<String, String> asInverseMap(String version) {
            return asMap(version, true);
        }

        @Nonnull
        private Map<String, String> asMap(String version, boolean inverse) {
            Map<String, String> t = new LinkedHashMap<>();
            for (Pair<ListValueObject, ListValueObject> pair : this) {
                ListValueObject v1 = inverse ? pair.second : pair.first;
                String key = v1 == null ? null : v1.getStringValue(version);
                ListValueObject v2 = inverse ? pair.first : pair.second;
                String value = v2 == null ? null : v2.getStringValue(version);
                if (key != null) {
                    t.put(key, value);
                }
            }
            return Collections.unmodifiableMap(t);
        }
    }
}

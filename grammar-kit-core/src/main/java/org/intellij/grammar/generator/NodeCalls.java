/*
 * Copyright 2011-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.grammar.generator;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.intellij.grammar.generator.ParserGeneratorUtil.getWrapperParserMetaMethodName;

/**
 * @author Daniil Ovchinnikov
 */
public class NodeCalls
{
	private NodeCalls()
	{
	}

	interface NodeCall
	{

		@Nonnull
		String render(@Nonnull Names names);
	}

	interface NodeArgument
	{

		default boolean referencesMetaParameter()
		{
			return false;
		}

		@Nonnull
		String render();
	}

	static class ConsumeTokenCall implements NodeCall
	{

		final ParserGeneratorUtil.ConsumeType consumeType;
		final String token;

		ConsumeTokenCall(@Nonnull ParserGeneratorUtil.ConsumeType consumeType, @Nonnull String token)
		{
			this.consumeType = consumeType;
			this.token = token;
		}

		@Nonnull
		@Override
		public String render(@Nonnull Names names)
		{
			return String.format("%s(%s, %s)", consumeType.getMethodName(), names.builder, token);
		}
	}

	static class ConsumeTokenChoiceCall implements NodeCall
	{

		final ParserGeneratorUtil.ConsumeType consumeType;
		final String tokenSetName;

		ConsumeTokenChoiceCall(@Nonnull ParserGeneratorUtil.ConsumeType consumeType, @Nonnull String tokenSetName)
		{
			this.consumeType = consumeType;
			this.tokenSetName = tokenSetName;
		}

		@Nonnull
		@Override
		public String render(@Nonnull Names names)
		{
			return String.format("%s(%s, %s)", consumeType.getMethodName(), names.builder, tokenSetName);
		}
	}

	static class ConsumeTokensCall implements NodeCall
	{

		final String methodName;
		final int pin;
		final List<String> tokens;

		ConsumeTokensCall(@Nonnull String methodName, int pin, @Nonnull List<String> tokens)
		{
			this.methodName = methodName;
			this.pin = pin;
			this.tokens = Collections.unmodifiableList(tokens);
		}

		@Nonnull
		@Override
		public String render(@Nonnull Names names)
		{
			return String.format("%s(%s, %d, %s)", methodName, names.builder, pin, StringUtil.join(tokens, ", "));
		}
	}

	static class ExpressionMethodCall implements NodeCall
	{

		final String methodName;
		final int priority;

		ExpressionMethodCall(@Nonnull String methodName, int priority)
		{
			this.methodName = methodName;
			this.priority = priority;
		}

		@Nonnull
		@Override
		public String render(@Nonnull Names names)
		{
			return String.format("%s(%s, %s + 1, %d)", methodName, names.builder, names.level, priority);
		}
	}

	static class MetaMethodCall extends MethodCallWithArguments
	{

		final
		@Nullable
		String targetClassName;

		MetaMethodCall(@Nullable String targetClassName, @Nonnull String methodName, @Nonnull List<NodeArgument> arguments)
		{
			super(methodName, arguments);
			this.targetClassName = targetClassName;
		}

		boolean referencesMetaParameter()
		{
			return arguments.stream().anyMatch(NodeArgument::referencesMetaParameter);
		}

		@Nullable
		String getTargetClassName()
		{
			return targetClassName;
		}

		@Nonnull
		@Override
		protected String getMethodRef()
		{
			String ref = super.getMethodRef();
			return targetClassName == null ? ref : String.format("%s.%s", targetClassName, ref);
		}
	}

	static class MetaMethodCallArgument implements NodeArgument
	{

		final MetaMethodCall call;

		MetaMethodCallArgument(@Nonnull MetaMethodCall call)
		{
			this.call = call;
		}

		@Override
		public boolean referencesMetaParameter()
		{
			return true;
		}

		@Nonnull
		private String getMethodRef()
		{
			String ref = getWrapperParserMetaMethodName(call.methodName);
			String className = call.getTargetClassName();
			return className == null ? ref : String.format("%s.%s", className, ref);
		}

		@Nonnull
		@Override
		public String render()
		{
			String arguments = String.join(", ", ContainerUtil.map(call.arguments, NodeArgument::render));
			return String.format("%s(%s)", getMethodRef(), arguments);
		}
	}

	static class MetaParameterCall implements NodeCall
	{

		final String metaParameterName;

		MetaParameterCall(@Nonnull String metaParameterName)
		{
			this.metaParameterName = metaParameterName;
		}

		@Nonnull
		@Override
		public String render(@Nonnull Names names)
		{
			return String.format("%s.parse(%s, %s)", metaParameterName, names.builder, names.level);
		}
	}

	static class MethodCall implements NodeCall
	{

		final boolean renderClass;
		final String className;
		final String methodName;

		MethodCall(boolean renderClass, @Nonnull String className, @Nonnull String methodName)
		{
			this.renderClass = renderClass;
			this.className = className;
			this.methodName = methodName;
		}

		@Nonnull
		String getMethodName()
		{
			return methodName;
		}

		@Nonnull
		String getClassName()
		{
			return className;
		}

		@Nonnull
		public String render(@Nonnull Names names)
		{
			if(renderClass)
			{
				return String.format("%s.%s(%s, %s + 1)", className, methodName, names.builder, names.level);
			}
			else
			{
				return String.format("%s(%s, %s + 1)", methodName, names.builder, names.level);
			}
		}
	}

	static class MethodCallWithArguments implements NodeCall
	{

		final String methodName;
		final List<NodeArgument> arguments;

		MethodCallWithArguments(@Nonnull String methodName, @Nonnull List<NodeArgument> arguments)
		{
			this.methodName = methodName;
			this.arguments = Collections.unmodifiableList(arguments);
		}

		@Nonnull
		protected String getMethodRef()
		{
			return methodName;
		}

		@Nonnull
		@Override
		public String render(@Nonnull Names names)
		{
			String argumentStr = arguments.stream()
					.map(NodeArgument::render)
					.map(it -> ", " + it)
					.collect(Collectors.joining());
			return String.format("%s(%s, %s + 1%s)", getMethodRef(), names.builder, names.level, argumentStr);
		}
	}


	static class TextArgument implements NodeArgument
	{

		final String text;

		TextArgument(@Nonnull String text)
		{
			this.text = text;
		}

		@Nonnull
		@Override
		public String render()
		{
			return text;
		}
	}

	static class MetaParameterArgument extends TextArgument
	{

		MetaParameterArgument(@Nonnull String text)
		{
			super(text);
		}

		@Override
		public boolean referencesMetaParameter()
		{
			return true;
		}
	}
}

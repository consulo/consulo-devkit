package com.intellij.codeInsight.completion;

import javax.annotation.Nonnull;

/**
 * @author peter
 */
public abstract class PrefixMatcher
{
	protected final String myPrefix;

	protected PrefixMatcher(String prefix)
	{
		myPrefix = prefix;
	}

	public boolean isStartMatch(String name)
	{
		return prefixMatches(name);
	}

	public abstract boolean prefixMatches(@Nonnull String name);

	@Nonnull
	public final String getPrefix()
	{
		return myPrefix;
	}

	@Nonnull
	public abstract PrefixMatcher cloneWithPrefix(@Nonnull String prefix);

	public int matchingDegree(String string)
	{
		return 0;
	}
}

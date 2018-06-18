package com.intellij.codeInsight.completion.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.TestOnly;
import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.codeStyle.MinusculeMatcher;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.util.containers.FList;
import com.intellij.util.text.CharArrayUtil;

/**
 * @author peter
 */
public class CamelHumpMatcher extends PrefixMatcher
{
	private final MinusculeMatcher myMatcher;
	private final MinusculeMatcher myCaseInsensitiveMatcher;
	private final boolean myCaseSensitive;
	private static boolean ourForceStartMatching;

	public CamelHumpMatcher(@Nonnull final String prefix)
	{
		this(prefix, true);
	}

	public CamelHumpMatcher(String prefix, boolean caseSensitive)
	{
		super(prefix);
		myCaseSensitive = caseSensitive;
		myMatcher = createMatcher(myCaseSensitive);
		myCaseInsensitiveMatcher = createMatcher(false);
	}

	@Override
	public boolean isStartMatch(String name)
	{
		return myMatcher.isStartMatch(name);
	}


	private static int skipUnderscores(@Nonnull String name)
	{
		return CharArrayUtil.shiftForward(name, 0, "_");
	}

	@Override
	public boolean prefixMatches(@Nonnull final String name)
	{
		return myMatcher.matches(name);
	}

	@Override
	@Nonnull
	public PrefixMatcher cloneWithPrefix(@Nonnull final String prefix)
	{
		return new CamelHumpMatcher(prefix, myCaseSensitive);
	}

	private MinusculeMatcher createMatcher(final boolean caseSensitive)
	{
		String prefix = applyMiddleMatching(myPrefix);

		if(!caseSensitive)
		{
			return NameUtil.buildMatcher(prefix, NameUtil.MatchingCaseSensitivity.NONE);
		}

		return NameUtil.buildMatcher(prefix, NameUtil.MatchingCaseSensitivity.NONE);
	}

	public static String applyMiddleMatching(String prefix)
	{
		if(!prefix.isEmpty() && !ourForceStartMatching)
		{
			return "*" + StringUtil.replace(prefix, ".", ". ").trim();
		}
		return prefix;
	}

	@Override
	public String toString()
	{
		return myPrefix;
	}

	/**
	 * In an ideal world, all tests would use the same settings as production, i.e. middle matching.
	 * If you see a usage of this method which can be easily removed (i.e. it's easy to make a test pass without it
	 * by modifying test expectations slightly), please do it
	 */
	@TestOnly
	@Deprecated
	public static void forceStartMatching(Disposable parent)
	{
		ourForceStartMatching = true;
		Disposer.register(parent, new Disposable()
		{
			@Override
			public void dispose()
			{
				//noinspection AssignmentToStaticFieldFromInstanceMethod
				ourForceStartMatching = false;
			}
		});
	}

	@Override
	public int matchingDegree(String string)
	{
		return matchingDegree(string, matchingFragments(string));
	}

	public FList<TextRange> matchingFragments(String string)
	{
		return myMatcher.matchingFragments(string);
	}

	public int matchingDegree(String string, @Nullable FList<TextRange> fragments)
	{
		int underscoreEnd = skipUnderscores(string);
		if(underscoreEnd > 0)
		{
			FList<TextRange> ciRanges = myCaseInsensitiveMatcher.matchingFragments(string);
			if(ciRanges != null && !ciRanges.isEmpty())
			{
				int matchStart = ciRanges.get(0).getStartOffset();
				if(matchStart > 0 && matchStart <= underscoreEnd)
				{
					return myCaseInsensitiveMatcher.matchingDegree(string.substring(matchStart), true) - 1;
				}
			}
		}

		return myMatcher.matchingDegree(string, true, fragments);
	}
}

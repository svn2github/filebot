
package net.sourceforge.filebot.similarity;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses( { SeriesNameMatcherTest.class, SeasonEpisodeMatcherTest.class, NameSimilarityMetricTest.class, NumericSimilarityMetricTest.class, SeasonEpisodeSimilarityMetricTest.class })
public class SimilarityTestSuite {
	
}

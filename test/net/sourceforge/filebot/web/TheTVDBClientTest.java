
package net.sourceforge.filebot.web;


import static org.junit.Assert.*;

import java.net.URL;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import net.sf.ehcache.CacheManager;
import net.sourceforge.filebot.web.TheTVDBClient.BannerProperty;
import net.sourceforge.filebot.web.TheTVDBClient.MirrorType;
import net.sourceforge.filebot.web.TheTVDBClient.TheTVDBSearchResult;


public class TheTVDBClientTest {
	
	private TheTVDBClient thetvdb = new TheTVDBClient("BA864DEE427E384A");
	
	
	@Test
	public void search() throws Exception {
		// test default language and query escaping (blanks)
		List<SearchResult> results = thetvdb.search("babylon 5");
		
		assertEquals(1, results.size());
		
		TheTVDBSearchResult first = (TheTVDBSearchResult) results.get(0);
		
		assertEquals("Babylon 5", first.getName());
		assertEquals(70726, first.getSeriesId());
	}
	
	
	@Test
	public void searchGerman() throws Exception {
		List<SearchResult> results = thetvdb.search("Buffy the Vampire Slayer", Locale.GERMAN);
		
		assertEquals(2, results.size());
		
		TheTVDBSearchResult first = (TheTVDBSearchResult) results.get(0);
		
		assertEquals("Buffy the Vampire Slayer", first.getName());
		assertEquals(70327, first.getSeriesId());
	}
	
	
	@Test
	public void getEpisodeListAll() throws Exception {
		List<Episode> list = thetvdb.getEpisodeList(new TheTVDBSearchResult("Buffy the Vampire Slayer", 70327));
		
		assertTrue(list.size() >= 144);
		
		// check ordinary episode
		Episode first = list.get(0);
		assertEquals("Buffy the Vampire Slayer", first.getSeriesName());
		assertEquals("1997-03-10", first.getSeriesStartDate().toString());
		assertEquals("Welcome to the Hellmouth (1)", first.getTitle());
		assertEquals("1", first.getEpisode().toString());
		assertEquals("1", first.getSeason().toString());
		assertEquals("1", first.getAbsolute().toString());
		assertEquals("1997-03-10", first.airdate().toString());
		
		// check special episode
		Episode last = list.get(list.size() - 1);
		assertEquals("Buffy the Vampire Slayer", last.getSeriesName());
		assertEquals("Unaired Pilot", last.getTitle());
		assertEquals("1", last.getSeason().toString());
		assertEquals(null, last.getEpisode());
		assertEquals(null, last.getAbsolute());
		assertEquals("1", last.getSpecial().toString());
		assertEquals(null, last.airdate());
	}
	
	
	@Test
	public void getEpisodeListSingleSeason() throws Exception {
		List<Episode> list = thetvdb.getEpisodeList(new TheTVDBSearchResult("Wonderfalls", 78845), 1);
		
		assertEquals(14, list.size());
		
		Episode first = list.get(0);
		
		assertEquals("Wonderfalls", first.getSeriesName());
		assertEquals("2004-03-12", first.getSeriesStartDate().toString());
		assertEquals("Wax Lion", first.getTitle());
		assertEquals("1", first.getEpisode().toString());
		assertEquals("1", first.getSeason().toString());
		assertEquals(null, first.getAbsolute()); // should be "1" but data has not yet been entered
		assertEquals("2004-03-12", first.airdate().toString());
	}
	
	
	@Test
	public void getEpisodeListNumbering() throws Exception {
		List<Episode> list = thetvdb.getEpisodeList(new TheTVDBSearchResult("Firefly", 78874), 1);
		
		assertEquals(14, list.size());
		
		Episode first = list.get(0);
		assertEquals("Firefly", first.getSeriesName());
		assertEquals("2002-09-20", first.getSeriesStartDate().toString());
		assertEquals("Serenity", first.getTitle());
		assertEquals("1", first.getEpisode().toString());
		assertEquals("1", first.getSeason().toString());
		assertEquals("1", first.getAbsolute().toString());
		assertEquals("2002-12-20", first.airdate().toString());
	}
	
	
	@Test
	public void getEpisodeListLink() {
		assertEquals("http://www.thetvdb.com/?tab=seasonall&id=78874", thetvdb.getEpisodeListLink(new TheTVDBSearchResult("Firefly", 78874)).toString());
	}
	
	
	@Test
	public void getEpisodeListLinkSingleSeason() {
		assertEquals("http://www.thetvdb.com/?tab=season&seriesid=73965&seasonid=6749", thetvdb.getEpisodeListLink(new TheTVDBSearchResult("Roswell", 73965), 3).toString());
	}
	
	
	@Test
	public void getMirror() throws Exception {
		assertNotNull(thetvdb.getMirror(MirrorType.XML));
		assertNotNull(thetvdb.getMirror(MirrorType.BANNER));
		assertNotNull(thetvdb.getMirror(MirrorType.ZIP));
	}
	
	
	@Test
	public void resolveTypeMask() {
		// no flags set
		assertEquals(EnumSet.noneOf(MirrorType.class), MirrorType.fromTypeMask(0));
		
		// xml and zip flags set
		assertEquals(EnumSet.of(MirrorType.ZIP, MirrorType.XML), MirrorType.fromTypeMask(5));
		
		// all flags set
		assertEquals(EnumSet.allOf(MirrorType.class), MirrorType.fromTypeMask(7));
	}
	
	
	@Test
	public void getBanner() throws Exception {
		Map<BannerProperty, Object> banner = thetvdb.getBanner(new TheTVDBSearchResult("Buffy the Vampire Slayer", 70327), "season", "seasonwide", 7, "en");
		
		assertEquals(857660, (Double) banner.get(BannerProperty.id), 0);
		assertEquals("season", banner.get(BannerProperty.BannerType));
		assertEquals("seasonwide", banner.get(BannerProperty.BannerType2));
		assertEquals("http://thetvdb.com/banners/seasonswide/70327-7.jpg", banner.get(BannerProperty.BannerPath).toString());
		assertEquals(99712, WebRequest.fetch((URL) banner.get(BannerProperty.BannerPath)).remaining(), 0);
	}
	
	
	@Test
	public void getBannerDescriptor() throws Exception {
		List<Map<BannerProperty, Object>> banners = thetvdb.getBannerDescriptor(70327);
		
		assertEquals(106, banners.size());
		assertEquals("fanart", banners.get(0).get(BannerProperty.BannerType));
		assertEquals("1280x720", banners.get(0).get(BannerProperty.BannerType2));
		assertEquals(486993, WebRequest.fetch((URL) banners.get(0).get(BannerProperty.BannerPath)).remaining(), 0);
	}
	
	
	@BeforeClass
	@AfterClass
	public static void clearCache() {
		CacheManager.getInstance().clearAll();
	}
	
}

package net.filebot.web;

import static net.filebot.CachedResource.*;
import static org.junit.Assert.*;

import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Ignore;
import org.junit.Test;

import net.filebot.Cache;
import net.filebot.CacheType;
import net.filebot.CachedResource;
import net.filebot.web.TMDbClient.Artwork;
import net.filebot.web.TMDbClient.MovieInfo;

public class TMDbClientTest {

	static TMDbClient tmdb = new TMDbClient("66308fb6e3fd850dde4c7d21df2e8306");

	@Test
	public void searchByName() throws Exception {
		List<Movie> result = tmdb.searchMovie("Serenity", Locale.CHINESE);
		Movie movie = result.get(0);

		assertEquals("冲出宁静号", movie.getName());
		assertEquals(2005, movie.getYear());
		assertEquals(-1, movie.getImdbId());
		assertEquals(16320, movie.getTmdbId());
	}

	@Test
	public void searchByNameWithYearShortName() throws Exception {
		List<Movie> result = tmdb.searchMovie("Up 2009", Locale.ENGLISH);
		Movie movie = result.get(0);

		assertEquals("Up", movie.getName());
		assertEquals(2009, movie.getYear());
		assertEquals(-1, movie.getImdbId());
		assertEquals(14160, movie.getTmdbId());
	}

	@Test
	public void searchByNameWithYearNumberName() throws Exception {
		List<Movie> result = tmdb.searchMovie("9 (2009)", Locale.ENGLISH);
		Movie movie = result.get(0);

		assertEquals("9", movie.getName());
		assertEquals(2009, movie.getYear());
		assertEquals(-1, movie.getImdbId());
		assertEquals(12244, movie.getTmdbId());
	}

	@Test
	public void searchByNameGermanResults() throws Exception {
		List<Movie> result = tmdb.searchMovie("East of Eden", Locale.GERMAN);
		Movie movie = result.get(0);

		assertEquals("Jenseits von Eden", movie.getName());
		assertEquals(1955, movie.getYear());
		assertEquals(Arrays.asList("Jenseits von Eden (1955)", "East of Eden (1955)"), movie.getEffectiveNames());
	}

	@Test
	public void searchByIMDB() throws Exception {
		Movie movie = tmdb.getMovieDescriptor(new Movie(null, 0, 418279, -1), Locale.ENGLISH);

		assertEquals("Transformers", movie.getName());
		assertEquals(2007, movie.getYear(), 0);
		assertEquals(418279, movie.getImdbId(), 0);
		assertEquals(1858, movie.getTmdbId(), 0);
	}

	@Test
	public void getMovieInfo() throws Exception {
		MovieInfo movie = tmdb.getMovieInfo(new Movie(null, 0, 418279, -1), Locale.ENGLISH, true);

		assertEquals("Transformers", movie.getName());
		assertEquals("2007-06-27", movie.getReleased().toString());
		assertEquals("PG-13", movie.getCertification());
		assertEquals("[es, en]", movie.getSpokenLanguages().toString());
		assertEquals("Shia LaBeouf", movie.getActors().get(0));
		assertEquals("Michael Bay", movie.getDirector());
	}

	@Test
	public void getArtwork() throws Exception {
		List<Artwork> artwork = tmdb.getArtwork("tt0418279");
		assertEquals("backdrops", artwork.get(0).getCategory());
		assertEquals("https://image.tmdb.org/t/p/original/ac0HwGJIU3GxjjGujlIjLJmAGPR.jpg", artwork.get(0).getUrl().toString());
	}

	SearchResult buffy = new SearchResult(95, "Buffy the Vampire Slayer");
	SearchResult wonderfalls = new SearchResult(1982, "Wonderfalls");
	SearchResult firefly = new SearchResult(1437, "Firefly");

	@Test
	public void search() throws Exception {
		// test default language and query escaping (blanks)
		List<SearchResult> results = tmdb.search("babylon 5", Locale.ENGLISH);

		assertEquals(1, results.size());

		assertEquals("Babylon 5", results.get(0).getName());
		assertEquals(3137, results.get(0).getId());
	}

	@Test
	public void getEpisodeListAll() throws Exception {
		List<Episode> list = tmdb.getEpisodeList(buffy, SortOrder.Airdate, Locale.ENGLISH);

		assertTrue(list.size() >= 144);

		// check ordinary episode
		Episode first = list.get(0);
		assertEquals("Buffy the Vampire Slayer", first.getSeriesName());
		assertEquals("1997-03-10", first.getSeriesInfo().getStartDate().toString());
		assertEquals("Welcome to the Hellmouth (1)", first.getTitle());
		assertEquals("1", first.getEpisode().toString());
		assertEquals("1", first.getSeason().toString());
		assertEquals("1", first.getAbsolute().toString());
		assertEquals("1997-03-10", first.getAirdate().toString());

		// check special episode
		Episode last = list.get(list.size() - 1);
		assertEquals("Buffy the Vampire Slayer", last.getSeriesName());
		assertEquals("Unaired Buffy the Vampire Slayer pilot", last.getTitle());
		assertEquals(null, last.getSeason());
		assertEquals(null, last.getEpisode());
		assertEquals(null, last.getAbsolute());
		assertEquals("1", last.getSpecial().toString());
		assertEquals(null, last.getAirdate());
	}

	@Test
	public void getEpisodeListSingleSeason() throws Exception {
		List<Episode> list = tmdb.getEpisodeList(wonderfalls, SortOrder.Airdate, Locale.ENGLISH);

		Episode first = list.get(0);
		assertEquals("Wonderfalls", first.getSeriesName());
		assertEquals("2004-03-12", first.getSeriesInfo().getStartDate().toString());
		assertEquals("Wax Lion", first.getTitle());
		assertEquals("1", first.getEpisode().toString());
		assertEquals("1", first.getSeason().toString());
		assertEquals("1", first.getAbsolute().toString());
		assertEquals("2004-03-12", first.getAirdate().toString());
	}

	@Ignore
	@Test
	public void floodLimit() throws Exception {
		for (Locale it : Locale.getAvailableLocales()) {
			List<Movie> results = tmdb.searchMovie("Serenity", it);
			assertEquals(16320, results.get(0).getTmdbId());
		}
	}

	@Ignore
	@Test
	public void etag() throws Exception {
		Cache cache = Cache.getCache("test", CacheType.Persistent);
		Cache etagStorage = Cache.getCache("etag", CacheType.Persistent);
		CachedResource<String, byte[]> resource = cache.bytes("http://devel.squid-cache.org/old_projects.html#etag", URL::new).fetch(fetchIfNoneMatch(etagStorage::get, etagStorage::put)).expire(Duration.ZERO);
		assertArrayEquals(resource.get(), resource.get());
	}

}

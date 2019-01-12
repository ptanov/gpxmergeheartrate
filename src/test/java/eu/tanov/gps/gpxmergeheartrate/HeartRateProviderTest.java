package eu.tanov.gps.gpxmergeheartrate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.junit.Test;

import eu.tanov.gps.gpxmergeheartrate.HeartRateProvider.HeartRateRow;
import eu.tanov.gps.gpxmergeheartrate.HeartRateProvider.Statistics;

public class HeartRateProviderTest {

	private static final String DEFAULT_HEADER = "a\n";

	private static final String DEFAULT_DATA = DEFAULT_HEADER

			+ "01.01.2000 00:00:00,1,1%\n"

			+ "02.01.2000 00:00:00,2,1%\n"

			+ "03.01.2000 00:00:00,3,1%\n"

			+ "04.01.2000 00:00:00,4,1%\n"

			+ "05.01.2000 00:00:00,5,1%\n"

			+ "06.01.2000 00:00:00,6,1%\n"

			+ "07.01.2000 00:00:00,7,1%\n"

			+ "08.01.2000 00:00:00,8,1%\n"

			+ "09.01.2000 00:00:00,9,1%";

	private static final String DEFAULT_SMALL_FIRST_LINE_DATE = "10.11.2018 23:15:36";
	private static final int DEFAULT_SMALL_FIRST_LINE_RATE = 91;
	private static final int DEFAULT_SMALL_FIRST_LINE_ZONE = 47;
	private static final String DEFAULT_SMALL_FIRST_LINE = DEFAULT_SMALL_FIRST_LINE_DATE + "," + DEFAULT_SMALL_FIRST_LINE_RATE
			+ "," + DEFAULT_SMALL_FIRST_LINE_ZONE + "%";
	private static final String DEFAULT_SMALL = DEFAULT_HEADER + DEFAULT_SMALL_FIRST_LINE;

	/**
	 * Like in Mockito spy - we assert findClosest() arguments
	 */
	private static class AssertableHeartRateProvider extends HeartRateProvider {
		private int invocation = 0;
		private final int[] expectedFromIndex;
		private final int[] expectedToIndex;
		private final int[] returns;

		public AssertableHeartRateProvider(InputStream source, int[] expectedFromIndex, int[] expectedToIndex, int[] returns) {
			super(source);
			assertEquals(expectedFromIndex.length, expectedToIndex.length);
			assertEquals(expectedFromIndex.length, returns.length);

			this.expectedFromIndex = expectedFromIndex;
			this.expectedToIndex = expectedToIndex;
			this.returns = returns;
		}

		@Override
		protected int findClosest(HeartRateRow[] heartRates, int fromIndex, int toIndex, OffsetDateTime dateTime) {
			assertTrue(invocation < returns.length);

			final int lastInvocation = invocation;
			invocation++;

			assertEquals(expectedFromIndex[lastInvocation], fromIndex);
			assertEquals(expectedToIndex[lastInvocation], toIndex);
			return returns[lastInvocation];
		}

		public void assertAllInvocations() {
			assertEquals(returns.length, invocation);
		}
	}

	@Test
	public void souldParseHeartRateLine() {
		final HeartRateProvider heartRateProvider = new HeartRateProvider(toInputStream(DEFAULT_SMALL));

		final HeartRateRow actual = heartRateProvider.parseLine(DEFAULT_SMALL_FIRST_LINE);
		assertEquals(parseDateTime(DEFAULT_SMALL_FIRST_LINE_DATE), actual.getDateTime());
		assertEquals(DEFAULT_SMALL_FIRST_LINE_RATE, actual.getRate());
		assertEquals(DEFAULT_SMALL_FIRST_LINE_ZONE, actual.getRateZone());
	}

	private static OffsetDateTime parseDateTime(String dateTime) {
		return LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")).atZone(ZoneId.systemDefault())
				.toOffsetDateTime();
	}

	@Test
	public void shouldHandleEqualsTwiceNotFound() {
		final HeartRateProvider heartRateProvider = new HeartRateProvider(toInputStream(DEFAULT_SMALL));
		assertFalse(heartRateProvider.getHrForTime(parseDateTime("11.11.2018 23:16:36")).isPresent());
		assertFalse(heartRateProvider.getHrForTime(parseDateTime("11.11.2018 23:16:36")).isPresent());
		final Statistics actual = heartRateProvider.getStatistics();
		assertEquals(2, actual.getCountFailed());
		assertEquals(0, actual.getCountSucceed());
	}

	@Test
	public void shouldReturnOnlyLessThanMaxDifference() {
		assertEquals(91, new HeartRateProvider(toInputStream(DEFAULT_SMALL)).getHrForTime(parseDateTime("10.11.2018 23:16:36"))
				.get().intValue());
		assertEquals(91, new HeartRateProvider(toInputStream(DEFAULT_SMALL)).getHrForTime(parseDateTime("10.11.2018 23:14:36"))
				.get().intValue());

		assertFalse(new HeartRateProvider(toInputStream(DEFAULT_SMALL)).getHrForTime(parseDateTime("10.11.2018 23:16:37"))
				.isPresent());
		assertFalse(new HeartRateProvider(toInputStream(DEFAULT_SMALL)).getHrForTime(parseDateTime("10.11.2018 23:14:35"))
				.isPresent());
	}

	@Test
	public void shouldHandleNewLineAtTheEnd() {
		assertEquals(91, new HeartRateProvider(toInputStream(DEFAULT_SMALL))
				.getHrForTime(parseDateTime(DEFAULT_SMALL_FIRST_LINE_DATE)).get().intValue());
		assertEquals(91, new HeartRateProvider(toInputStream(DEFAULT_SMALL + "\n"))
				.getHrForTime(parseDateTime(DEFAULT_SMALL_FIRST_LINE_DATE)).get().intValue());
	}

	@Test
	public void shouldReturnClosestHrForTime() {
		final String heartRateFileOdd = DEFAULT_HEADER

				+ "02.01.2000 00:00:00,1,1%\n"

				+ "02.01.2000 00:00:05,2,1%\n"

				+ "02.01.2000 00:00:10,3,1%";

		assertEquals(1, new HeartRateProvider(toInputStream(heartRateFileOdd)).getHrForTime(parseDateTime("02.01.2000 00:00:00"))
				.get().intValue());
		assertEquals(1, new HeartRateProvider(toInputStream(heartRateFileOdd)).getHrForTime(parseDateTime("02.01.2000 00:00:01"))
				.get().intValue());
		assertEquals(1, new HeartRateProvider(toInputStream(heartRateFileOdd)).getHrForTime(parseDateTime("02.01.2000 00:00:02"))
				.get().intValue());
		assertEquals(2, new HeartRateProvider(toInputStream(heartRateFileOdd)).getHrForTime(parseDateTime("02.01.2000 00:00:03"))
				.get().intValue());
		assertEquals(2, new HeartRateProvider(toInputStream(heartRateFileOdd)).getHrForTime(parseDateTime("02.01.2000 00:00:04"))
				.get().intValue());
		assertEquals(2, new HeartRateProvider(toInputStream(heartRateFileOdd)).getHrForTime(parseDateTime("02.01.2000 00:00:05"))
				.get().intValue());
		assertEquals(2, new HeartRateProvider(toInputStream(heartRateFileOdd)).getHrForTime(parseDateTime("02.01.2000 00:00:06"))
				.get().intValue());
		assertEquals(2, new HeartRateProvider(toInputStream(heartRateFileOdd)).getHrForTime(parseDateTime("02.01.2000 00:00:07"))
				.get().intValue());
		assertEquals(3, new HeartRateProvider(toInputStream(heartRateFileOdd)).getHrForTime(parseDateTime("02.01.2000 00:00:08"))
				.get().intValue());
		assertEquals(3, new HeartRateProvider(toInputStream(heartRateFileOdd)).getHrForTime(parseDateTime("02.01.2000 00:00:09"))
				.get().intValue());
		assertEquals(3, new HeartRateProvider(toInputStream(heartRateFileOdd)).getHrForTime(parseDateTime("02.01.2000 00:00:10"))
				.get().intValue());

		final String heartRateFileEven = DEFAULT_HEADER

				+ "02.01.2000 00:00:00,1,1%\n"

				+ "02.01.2000 00:00:04,2,1%\n"

				+ "02.01.2000 00:00:08,3,1%";

		assertEquals(1, new HeartRateProvider(toInputStream(heartRateFileEven)).getHrForTime(parseDateTime("02.01.2000 00:00:00"))
				.get().intValue());
		assertEquals(1, new HeartRateProvider(toInputStream(heartRateFileEven)).getHrForTime(parseDateTime("02.01.2000 00:00:01"))
				.get().intValue());
		assertEquals(1, new HeartRateProvider(toInputStream(heartRateFileEven)).getHrForTime(parseDateTime("02.01.2000 00:00:02"))
				.get().intValue());
		assertEquals(2, new HeartRateProvider(toInputStream(heartRateFileEven)).getHrForTime(parseDateTime("02.01.2000 00:00:03"))
				.get().intValue());
		assertEquals(2, new HeartRateProvider(toInputStream(heartRateFileEven)).getHrForTime(parseDateTime("02.01.2000 00:00:04"))
				.get().intValue());
		assertEquals(2, new HeartRateProvider(toInputStream(heartRateFileEven)).getHrForTime(parseDateTime("02.01.2000 00:00:05"))
				.get().intValue());
		assertEquals(2, new HeartRateProvider(toInputStream(heartRateFileEven)).getHrForTime(parseDateTime("02.01.2000 00:00:06"))
				.get().intValue());
		assertEquals(3, new HeartRateProvider(toInputStream(heartRateFileEven)).getHrForTime(parseDateTime("02.01.2000 00:00:07"))
				.get().intValue());
		assertEquals(3, new HeartRateProvider(toInputStream(heartRateFileEven)).getHrForTime(parseDateTime("02.01.2000 00:00:08"))
				.get().intValue());

	}

	@Test
	public void shouldReturnHrForTime() {
		assertFalse(new HeartRateProvider(toInputStream(DEFAULT_DATA)).getHrForTime(parseDateTime("07.01.2000 23:58:00"))
				.isPresent());
		assertEquals(8, new HeartRateProvider(toInputStream(DEFAULT_DATA)).getHrForTime(parseDateTime("07.01.2000 23:59:00"))
				.get().intValue());
		assertEquals(8, new HeartRateProvider(toInputStream(DEFAULT_DATA)).getHrForTime(parseDateTime("08.01.2000 00:00:00"))
				.get().intValue());
		assertEquals(8, new HeartRateProvider(toInputStream(DEFAULT_DATA)).getHrForTime(parseDateTime("08.01.2000 00:01:00"))
				.get().intValue());
		assertFalse(new HeartRateProvider(toInputStream(DEFAULT_DATA)).getHrForTime(parseDateTime("08.01.2000 00:02:00"))
				.isPresent());

		assertEquals(1, new HeartRateProvider(toInputStream(DEFAULT_DATA)).getHrForTime(parseDateTime("31.12.1999 23:59:00"))
				.get().intValue());
		assertEquals(1, new HeartRateProvider(toInputStream(DEFAULT_DATA)).getHrForTime(parseDateTime("01.01.2000 00:00:00"))
				.get().intValue());
		assertEquals(1, new HeartRateProvider(toInputStream(DEFAULT_DATA)).getHrForTime(parseDateTime("01.01.2000 00:01:00"))
				.get().intValue());

		assertEquals(9, new HeartRateProvider(toInputStream(DEFAULT_DATA)).getHrForTime(parseDateTime("08.01.2000 23:59:00"))
				.get().intValue());
		assertEquals(9, new HeartRateProvider(toInputStream(DEFAULT_DATA)).getHrForTime(parseDateTime("09.01.2000 00:00:00"))
				.get().intValue());
		assertEquals(9, new HeartRateProvider(toInputStream(DEFAULT_DATA)).getHrForTime(parseDateTime("09.01.2000 00:01:00"))
				.get().intValue());
		assertFalse(new HeartRateProvider(toInputStream(DEFAULT_DATA)).getHrForTime(parseDateTime("09.01.2000 00:02:00"))
				.isPresent());

		final HeartRateProvider heartRateProviderIncreasing = new HeartRateProvider(toInputStream(DEFAULT_DATA));
		final Statistics actualStatisticsEmpty = heartRateProviderIncreasing.getStatistics();
		assertEquals(0, actualStatisticsEmpty.getCountSucceed());
		assertEquals(0, actualStatisticsEmpty.getCountFailed());

		assertEquals(9, heartRateProviderIncreasing.getHrForTime(parseDateTime("09.01.2000 00:00:00")).get().intValue());
		assertEquals(8, heartRateProviderIncreasing.getHrForTime(parseDateTime("08.01.2000 00:00:00")).get().intValue());
		assertEquals(7, heartRateProviderIncreasing.getHrForTime(parseDateTime("07.01.2000 00:00:00")).get().intValue());
		assertEquals(6, heartRateProviderIncreasing.getHrForTime(parseDateTime("06.01.2000 00:00:00")).get().intValue());
		assertEquals(5, heartRateProviderIncreasing.getHrForTime(parseDateTime("05.01.2000 00:00:00")).get().intValue());
		assertEquals(4, heartRateProviderIncreasing.getHrForTime(parseDateTime("04.01.2000 00:00:00")).get().intValue());
		assertEquals(3, heartRateProviderIncreasing.getHrForTime(parseDateTime("03.01.2000 00:00:00")).get().intValue());
		assertEquals(2, heartRateProviderIncreasing.getHrForTime(parseDateTime("02.01.2000 00:00:00")).get().intValue());
		assertEquals(1, heartRateProviderIncreasing.getHrForTime(parseDateTime("01.01.2000 00:00:00")).get().intValue());
		assertEquals(1, heartRateProviderIncreasing.getHrForTime(parseDateTime("31.12.1999 23:59:00")).get().intValue());

		final Statistics actualStatisticsIncreasing = heartRateProviderIncreasing.getStatistics();
		assertEquals(0, actualStatisticsIncreasing.getCountFailed());
		assertEquals(10, actualStatisticsIncreasing.getCountSucceed());

		final HeartRateProvider heartRateProviderDecreasing = new HeartRateProvider(toInputStream(DEFAULT_DATA));
		assertEquals(1, heartRateProviderDecreasing.getHrForTime(parseDateTime("01.01.2000 00:00:00")).get().intValue());
		assertEquals(2, heartRateProviderDecreasing.getHrForTime(parseDateTime("02.01.2000 00:00:00")).get().intValue());
		assertEquals(3, heartRateProviderDecreasing.getHrForTime(parseDateTime("03.01.2000 00:00:00")).get().intValue());
		assertEquals(4, heartRateProviderDecreasing.getHrForTime(parseDateTime("04.01.2000 00:00:00")).get().intValue());
		assertEquals(5, heartRateProviderDecreasing.getHrForTime(parseDateTime("05.01.2000 00:00:00")).get().intValue());
		assertEquals(6, heartRateProviderDecreasing.getHrForTime(parseDateTime("06.01.2000 00:00:00")).get().intValue());
		assertEquals(7, heartRateProviderDecreasing.getHrForTime(parseDateTime("07.01.2000 00:00:00")).get().intValue());
		assertEquals(8, heartRateProviderDecreasing.getHrForTime(parseDateTime("08.01.2000 00:00:00")).get().intValue());
		assertEquals(9, heartRateProviderDecreasing.getHrForTime(parseDateTime("09.01.2000 00:00:00")).get().intValue());
		assertEquals(9, heartRateProviderDecreasing.getHrForTime(parseDateTime("09.01.2000 00:01:00")).get().intValue());

		final Statistics actualStatisticsDecreasing = heartRateProviderDecreasing.getStatistics();
		assertEquals(0, actualStatisticsDecreasing.getCountFailed());
		assertEquals(10, actualStatisticsDecreasing.getCountSucceed());

		final HeartRateProvider heartRateProviderRandom = new HeartRateProvider(toInputStream(DEFAULT_DATA));
		assertEquals(7, heartRateProviderRandom.getHrForTime(parseDateTime("07.01.2000 00:00:00")).get().intValue());
		assertEquals(2, heartRateProviderRandom.getHrForTime(parseDateTime("02.01.2000 00:00:00")).get().intValue());
		assertEquals(2, heartRateProviderRandom.getHrForTime(parseDateTime("02.01.2000 00:00:01")).get().intValue());
		assertEquals(8, heartRateProviderRandom.getHrForTime(parseDateTime("08.01.2000 00:00:00")).get().intValue());
		assertEquals(6, heartRateProviderRandom.getHrForTime(parseDateTime("06.01.2000 00:00:00")).get().intValue());
		assertEquals(3, heartRateProviderRandom.getHrForTime(parseDateTime("03.01.2000 00:00:00")).get().intValue());
		assertEquals(4, heartRateProviderRandom.getHrForTime(parseDateTime("04.01.2000 00:00:00")).get().intValue());
		assertEquals(1, heartRateProviderRandom.getHrForTime(parseDateTime("01.01.2000 00:00:00")).get().intValue());
		assertEquals(9, heartRateProviderRandom.getHrForTime(parseDateTime("09.01.2000 00:00:00")).get().intValue());
		assertEquals(5, heartRateProviderRandom.getHrForTime(parseDateTime("05.01.2000 00:00:00")).get().intValue());
		assertEquals(6, heartRateProviderRandom.getHrForTime(parseDateTime("06.01.2000 00:00:00")).get().intValue());
		assertEquals(8, heartRateProviderRandom.getHrForTime(parseDateTime("08.01.2000 00:00:00")).get().intValue());

		final Statistics actualStatisticsRandom = heartRateProviderRandom.getStatistics();
		assertEquals(0, actualStatisticsRandom.getCountFailed());
		assertEquals(12, actualStatisticsRandom.getCountSucceed());
	}

	@Test
	public void shouldSearchInSmallerRegion() {
		final AssertableHeartRateProvider heartRateProvider = new AssertableHeartRateProvider(toInputStream(DEFAULT_DATA),

				new int[] { 0, 7, 0, 0, 0, 0, 3 },

				new int[] { 8, 8, 8, 8, 5, 4, 8 },

				new int[] { 7, 8, 0, 5, 4, 3, 6 });

		assertEquals(8, heartRateProvider.getHrForTime(parseDateTime("08.01.2000 00:00:00")).get().intValue());
		// this is same so it is skipped and not send to findClosest():
		assertEquals(8, heartRateProvider.getHrForTime(parseDateTime("08.01.2000 00:00:00")).get().intValue());

		assertEquals(9, heartRateProvider.getHrForTime(parseDateTime("09.01.2000 00:00:00")).get().intValue());
		assertEquals(1, heartRateProvider.getHrForTime(parseDateTime("01.01.2000 00:00:00")).get().intValue());
		assertEquals(6, heartRateProvider.getHrForTime(parseDateTime("06.01.2000 00:00:00")).get().intValue());
		assertEquals(5, heartRateProvider.getHrForTime(parseDateTime("05.01.2000 00:00:00")).get().intValue());
		assertEquals(4, heartRateProvider.getHrForTime(parseDateTime("04.01.2000 00:00:00")).get().intValue());
		assertEquals(7, heartRateProvider.getHrForTime(parseDateTime("07.01.2000 00:00:00")).get().intValue());

		heartRateProvider.assertAllInvocations();
	}

	@Test
	public void shouldSortHeartRates() {
		final String heartRateFile = DEFAULT_HEADER

				+ "09.01.2000 00:00:00,9,1%\n"

				+ "08.01.2000 00:00:00,8,1%\n"

				+ "07.01.2000 00:00:00,7,1%\n"

				+ "06.01.2000 00:00:00,6,1%\n"

				+ "05.01.2000 00:00:00,5,1%\n"

				+ "04.01.2000 00:00:00,4,1%\n"

				+ "03.01.2000 00:00:00,3,1%\n"

				+ "02.01.2000 00:00:00,2,1%\n"

				+ "01.01.2000 00:00:00,1,1%";

		final HeartRateProvider heartRateProvider = new HeartRateProvider(toInputStream(heartRateFile));
		final HeartRateRow[] heartRates = heartRateProvider.heartRates;
		HeartRateRow previous = null;
		for (final HeartRateRow next : heartRates) {
			final HeartRateRow beforePrevious = previous;
			previous = next;
			if (beforePrevious == null) {
				continue;
			}
			assertTrue(previous.getDateTime().compareTo(beforePrevious.getDateTime()) > 0);
		}
		assertEquals(6, heartRateProvider.getHrForTime(parseDateTime("06.01.2000 00:00:00")).get().intValue());
	}

	private static InputStream toInputStream(String s) {
		return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
	}
}

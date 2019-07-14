package eu.tanov.gps.gpxmergeheartrate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class HeartRateProvider {
	private static final Duration MAX_DIFFERENCE = Duration.ofMinutes(1);

	public static class Statistics {
		private final int countSucceed;
		private final int countFailed;

		public Statistics(int countSucceed, int countFailed) {
			this.countSucceed = countSucceed;
			this.countFailed = countFailed;
		}

		public int getCountSucceed() {
			return countSucceed;
		}

		public int getCountFailed() {
			return countFailed;
		}

		@Override
		public String toString() {
			return "Statistics [countSucceed=" + countSucceed + ", countFailed=" + countFailed + "]";
		}

	}

	protected static class HeartRateRow {
		private final OffsetDateTime dateTime;
		private final int rate;
		private final int rateZone;

		public HeartRateRow(OffsetDateTime dateTime, int rate, int rateZone) {
			this.dateTime = Objects.requireNonNull(dateTime);
			if (rate <= 0) {
				throw new IllegalArgumentException("Rate must be positive, but was: " + rate);
			}
			this.rate = rate;
			if (rateZone < 0 || rateZone > 150) {
				throw new IllegalArgumentException("Ratezone must be [0; 150], but was: " + rateZone);
			}
			this.rateZone = rateZone;
		}

		@Override
		public String toString() {
			return "HeartRateRow [dateTime=" + dateTime + ", rate=" + rate + ", rateZone=" + rateZone + "]";
		}

		public OffsetDateTime getDateTime() {
			return dateTime;
		}

		public int getRate() {
			return rate;
		}

		@SuppressWarnings("unused")
		public int getRateZone() {
			return rateZone;
		}

	}

	protected final HeartRateRow[] heartRates;
	private OffsetDateTime lastDateTime;
	private int lastIndex;

	private int countSucceed = 0;
	private int countFailed = 0;

	public HeartRateProvider(InputStream source) {
		try (Stream<String> stream = new BufferedReader(new InputStreamReader(source)).lines()) {
			heartRates = stream.skip(1).filter(a -> !a.isEmpty()).map(this::parseLine)
					.sorted((a, b) -> a.getDateTime().compareTo(b.getDateTime())).toArray(HeartRateRow[]::new);
		}

		if (heartRates.length == 0) {
			throw new IllegalArgumentException("No records in the input stream");
		}

		lastIndex = heartRates.length - 1;
	}

	protected HeartRateRow parseLine(String line) {
		final String[] splitted = line.split(",");
		if (splitted.length != 3) {
			throw new IllegalArgumentException("3 expected, but was: " + line);
		}

		final OffsetDateTime dateTime = LocalDateTime.parse(splitted[0], DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
				.atZone(ZoneId.systemDefault()).toOffsetDateTime();

		final int rate = Integer.parseInt(splitted[1]);
		final int rateZone = Integer.parseInt(splitted[2].substring(0, splitted[2].length() - 1));

		return new HeartRateRow(dateTime, rate, rateZone);
	}

	public Statistics getStatistics() {
		return new Statistics(countSucceed, countFailed);
	}

	public Optional<Integer> getHrForTime(OffsetDateTime dateTime) {
		lastIndex = findClosest(dateTime);
		lastDateTime = dateTime;
		final HeartRateRow closest = heartRates[lastIndex];

		if (MAX_DIFFERENCE.compareTo(Duration.between(dateTime, closest.getDateTime()).abs()) >= 0) {
			countSucceed++;
			return Optional.of(closest.getRate());
		}

		countFailed++;
		return Optional.empty();
	}

	private int findClosest(OffsetDateTime dateTime) {
		final int fromIndex;
		final int toIndex;

		if (lastDateTime != null) {
			final int compareResult = lastDateTime.compareTo(dateTime);
			if (compareResult == 0) {
				// the same as last
				return lastIndex;
			} else if (compareResult > 0) {
				fromIndex = 0;
				// not -1 (with check max(fromIndex)) because we need to compare with the last
				// result
				toIndex = lastIndex;
			} else {
				// not +1 (with check min(toIndex)) because we need to compare with the last
				// result
				fromIndex = lastIndex;
				toIndex = heartRates.length - 1;
			}
		} else {
			fromIndex = 0;
			toIndex = heartRates.length - 1;
		}
		return findClosest(heartRates, fromIndex, toIndex, dateTime);
	}

	protected int findClosest(HeartRateRow[] heartRates, int fromIndex, int toIndex, OffsetDateTime dateTime) {
		if (dateTime.isBefore(heartRates[fromIndex].getDateTime())) {
			return fromIndex;
		}
		if (dateTime.isAfter(heartRates[toIndex].getDateTime())) {
			return toIndex;
		}

		while (fromIndex <= toIndex) {
			final int middle = (toIndex + fromIndex) / 2;

			if (dateTime.isBefore(heartRates[middle].getDateTime())) {
				toIndex = middle - 1;
			} else if (dateTime.isAfter(heartRates[middle].getDateTime())) {
				fromIndex = middle + 1;
			} else {
				return middle;
			}
		}

		final Duration durationFrom = Duration.between(heartRates[fromIndex].getDateTime(), dateTime);
		final Duration durationTo = Duration.between(dateTime, heartRates[toIndex].getDateTime());
		if (durationFrom.compareTo(durationTo) > 0) {
			return fromIndex;
		} else {
			return toIndex;
		}
	}
}
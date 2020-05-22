package eu.tanov.gps.gpxmergeheartrate;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import eu.tanov.gps.gpxmergeheartrate.HeartRateProvider.Statistics;

public class GpxMergeHeartRateTest {

	@Test
	public void testProcessMiBandTools() throws Exception {
		final GpxMergeHeartRate gpxMergeHeartRate = new GpxMergeHeartRate();
		try (final InputStream gpxInputFile = getClass().getResourceAsStream("input.gpx")) {
			try (final InputStream heartRateFile = getClass().getResourceAsStream("input-mibandtools.csv")) {
				final String expectedResult = IOUtils.toString(getClass().getResourceAsStream("output.gpx"));

				final ByteArrayOutputStream actualOutput = new ByteArrayOutputStream();

				final Statistics actualStatistics =
						gpxMergeHeartRate.process(gpxInputFile, heartRateFile, actualOutput, Optional.empty());

				assertEquals(1, actualStatistics.getCountSucceed());
				assertEquals(0, actualStatistics.getCountFailed());

				assertEquals(expectedResult, actualOutput.toString());
			}
		}
	}

	@Test
	public void testProcessNotifyFitnessForMiBand() throws Exception {
		final GpxMergeHeartRate gpxMergeHeartRate = new GpxMergeHeartRate();
		try (final InputStream gpxInputFile = getClass().getResourceAsStream("input.gpx")) {
			try (final InputStream heartRateFile = getClass().getResourceAsStream("input-notifyfitnessformiband.csv")) {
				final String expectedResult = IOUtils.toString(getClass().getResourceAsStream("output.gpx"));

				final ByteArrayOutputStream actualOutput = new ByteArrayOutputStream();

				final Statistics actualStatistics =
						gpxMergeHeartRate.process(gpxInputFile, heartRateFile, actualOutput, Optional.empty());

				assertEquals(1, actualStatistics.getCountSucceed());
				assertEquals(0, actualStatistics.getCountFailed());

				assertEquals(expectedResult, actualOutput.toString());
			}
		}
	}

}

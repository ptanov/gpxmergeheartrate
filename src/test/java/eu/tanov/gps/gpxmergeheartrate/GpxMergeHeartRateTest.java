package eu.tanov.gps.gpxmergeheartrate;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import eu.tanov.gps.gpxmergeheartrate.HeartRateProvider.Statistics;

public class GpxMergeHeartRateTest {

	private void assertResult(String inputGpx, String inputHeartRate, String output) throws Exception {
		final GpxMergeHeartRate gpxMergeHeartRate = new GpxMergeHeartRate();
		try (final InputStream gpxInputFile = getClass().getResourceAsStream(inputGpx)) {
			try (final InputStream heartRateFile = getClass().getResourceAsStream(inputHeartRate)) {
				final String expectedResult = IOUtils.toString(getClass().getResourceAsStream(output));

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
	public void testProcessMiBandTools() throws Exception {
		assertResult("input.gpx", "input-mibandtools.csv", "output.gpx");
		assertResult("inputExtensions.gpx", "input-mibandtools.csv", "outputExtensions.gpx");
		assertResult("inputTrackPointExtension.gpx", "input-mibandtools.csv", "outputTrackPointExtension.gpx");
	}

	@Test
	public void testProcessNotifyFitnessForMiBand() throws Exception {
		assertResult("input.gpx", "input-notifyfitnessformiband.csv", "output.gpx");
		assertResult("inputExtensions.gpx", "input-notifyfitnessformiband.csv", "outputExtensions.gpx");
		assertResult("inputTrackPointExtension.gpx", "input-notifyfitnessformiband.csv", "outputTrackPointExtension.gpx");
	}

}

package eu.tanov.gps.gpxmergeheartrate;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import eu.tanov.gps.gpxmergeheartrate.HeartRateProvider.Statistics;
import eu.tanov.gps.gpxmergeheartrate.parsestates.IdentatingStateHandler;
import eu.tanov.gps.gpxmergeheartrate.parsestates.OutsideTrkptStateHandler;
import eu.tanov.gps.gpxmergeheartrate.parsestates.StateHandler;

public class GpxMergeHeartRate {
	public static final String NAMESPACE_GPX = "http://www.topografix.com/GPX/1/1";
	public static final String EXTENSION_PREFIX = "gpxtpx";
	private static final String EXTENSION_URI = "http://www.garmin.com/xmlschemas/TrackPointExtension/v1";

	public static void main(String[] args) throws Exception {
		if (args.length != 3 && args.length != 4) {
			System.err.println(
					"3 or 4 parameters expected: <input gpx file> <input csv heart rate file> <result gpx file> [<max heart rate>]");
			System.exit(1);
		}
		final String gpxInputFile = args[0];
		final String heartRateFile = args[1];
		final String resultFile = args[2];
		final Optional<Integer> maximumHeartRate = Optional.ofNullable(args.length == 4 ? Integer.valueOf(args[3]) : null);
		final GpxMergeHeartRate gpxMergeHeartRate = new GpxMergeHeartRate();
		try (FileInputStream gpxInputStream = new FileInputStream(gpxInputFile)) {
			try (FileInputStream heartRateStream = new FileInputStream(heartRateFile)) {
				try (FileOutputStream resultStream = new FileOutputStream(resultFile)) {
					final Statistics statistics =
							gpxMergeHeartRate.process(gpxInputStream, heartRateStream, resultStream, maximumHeartRate);

					System.out.printf("Processed: %d/%d\n", statistics.getCountSucceed(),
							(statistics.getCountSucceed() + statistics.getCountFailed()));
				}
			}
		}
	}

	public Statistics process(InputStream gpxInputFile, InputStream heartRateFile, OutputStream resultFile,
			Optional<Integer> maximumHeartRate) throws Exception {
		final HeartRateProvider heartRateProvider = maximumHeartRate.map(a -> new HeartRateProvider(heartRateFile, a))
				.orElseGet(() -> new HeartRateProvider(heartRateFile));

		final XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty("javax.xml.stream.isCoalescing", true);

		final XMLEventReader reader = factory.createXMLEventReader(gpxInputFile);
		final XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter(resultFile);

		final XMLEventFactory eventFactory = XMLEventFactory.newInstance();

		StateHandler currentState =
				new OutsideTrkptStateHandler(heartRateProvider, IdentatingStateHandler.createInitialData(eventFactory, writer));
		while (reader.hasNext()) {
			final XMLEvent event = (XMLEvent) reader.next();
			currentState = currentState.handleEvent(addNamespacesIfRootElement(eventFactory, event));
		}

		reader.close();
		writer.flush();
		writer.close();

		return heartRateProvider.getStatistics();
	}

	private XMLEvent addNamespacesIfRootElement(XMLEventFactory eventFactory, XMLEvent event) {
		if (event.isStartElement() && event.asStartElement().getName().getLocalPart().equals("gpx")) {
			return addNamespaceToRootElement(eventFactory, event.asStartElement());
		}
		return event;
	}

	private XMLEvent addNamespaceToRootElement(XMLEventFactory eventFactory, StartElement startElement) {
		final String prefix = startElement.getName().getPrefix();
		final String namespaceUri = startElement.getName().getNamespaceURI();
		final String localName = startElement.getName().getLocalPart();
		final Iterator<?> attributes = startElement.getAttributes();
		@SuppressWarnings("unchecked")
		final Iterator<Namespace> oldNamespaces = startElement.getNamespaces();

		final List<Namespace> namespaces = enchanceNamespaces(eventFactory, oldNamespaces);

		final NamespaceContext context = startElement.getNamespaceContext();
		return eventFactory.createStartElement(prefix, namespaceUri, localName, attributes, namespaces.iterator(), context);
	}

	private List<Namespace> enchanceNamespaces(XMLEventFactory eventFactory, final Iterator<Namespace> oldNamespaces) {
		final List<Namespace> result = new ArrayList<>();
		oldNamespaces.forEachRemaining(result::add);
		final Optional<Namespace> previous = result.stream().filter(a -> a.getPrefix().equals(EXTENSION_PREFIX)).findFirst();
		if (previous.isPresent()) {
			if (!previous.get().getNamespaceURI().equals(EXTENSION_URI)) {
				throw new IllegalArgumentException("Different URI for " + EXTENSION_PREFIX + ", expecting " + EXTENSION_URI
						+ " but found " + previous.get().getNamespaceURI());
			}
			return result;
		}

		result.add(eventFactory.createNamespace(EXTENSION_PREFIX, EXTENSION_URI));
		return result;
	}

}
package eu.tanov.gps.gpxmergeheartrate.parsestates;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import eu.tanov.gps.gpxmergeheartrate.GpxMergeHeartRate;
import eu.tanov.gps.gpxmergeheartrate.HeartRateProvider;

public class InsideTrkptStateHandler extends IdentatingStateHandler {
	private static final String ELEMENT_HR = "hr";
	private static final String ELEMENT_TRACK_POINT_EXTENSION = "TrackPointExtension";
	private static final String ELEMENT_EXTENSIONS = "extensions";

	private static final Map<String, BiFunction<XMLEventFactory, Integer, Stream<XMLEvent>>> SUPPLIERS = initSuppliers();

	private final List<XMLEvent> events = new ArrayList<>();
	private final HeartRateProvider heartRateProvider;

	public InsideTrkptStateHandler(HeartRateProvider heartRateProvider, IdentatingStateHandler identatingStateHandler,
			XMLEvent event) {
		super(identatingStateHandler);
		this.heartRateProvider = heartRateProvider;
		events.add(event);
	}

	private static Map<String, BiFunction<XMLEventFactory, Integer, Stream<XMLEvent>>> initSuppliers() {
		final Map<String, BiFunction<XMLEventFactory, Integer, Stream<XMLEvent>>> result = new HashMap<>();
		result.put(ELEMENT_TRACK_POINT_EXTENSION,
				(eventFactory, heartRate) -> of(
						eventFactory.createStartElement(GpxMergeHeartRate.EXTENSION_PREFIX, "", ELEMENT_HR),
						eventFactory.createCharacters(String.valueOf(heartRate)),
						eventFactory.createEndElement(GpxMergeHeartRate.EXTENSION_PREFIX, "", ELEMENT_HR)));

		result.put(ELEMENT_EXTENSIONS, (eventFactory, heartRate) -> concat(
				concat(of(eventFactory.createStartElement(GpxMergeHeartRate.EXTENSION_PREFIX, "", ELEMENT_TRACK_POINT_EXTENSION)),
						result.get(ELEMENT_TRACK_POINT_EXTENSION).apply(eventFactory, heartRate)),
				of(eventFactory.createEndElement(GpxMergeHeartRate.EXTENSION_PREFIX, "", ELEMENT_TRACK_POINT_EXTENSION))));

		result.put(OutsideTrkptStateHandler.ELEMENT_TRACK_POINT,
				(eventFactory, heartRate) -> concat(
						concat(of(eventFactory.createStartElement("", GpxMergeHeartRate.NAMESPACE_GPX, ELEMENT_EXTENSIONS)),
								result.get(ELEMENT_EXTENSIONS).apply(eventFactory, heartRate)),
						of(eventFactory.createEndElement("", GpxMergeHeartRate.NAMESPACE_GPX, ELEMENT_EXTENSIONS))));
		return unmodifiableMap(result);
	}

	@Override
	public StateHandler handleEvent(XMLEvent event) throws XMLStreamException {
		events.add(event);
		if (event.isEndElement()
				&& event.asEndElement().getName().getLocalPart().equals(OutsideTrkptStateHandler.ELEMENT_TRACK_POINT)) {
			handleEvents();
			return new OutsideTrkptStateHandler(heartRateProvider, this);
		}

		return this;
	}

	private void handleEvents() throws XMLStreamException {
		final Optional<OffsetDateTime> time = getTimeFromEvents();

		final Optional<Integer> hrForTime = time.map(heartRateProvider::getHrForTime).orElse(Optional.empty());

		for (final XMLEvent event : hrForTime.map(this::addHeartRateToEvents).orElse(events)) {
			handleIdent(event);
		}
	}

	private List<XMLEvent> addHeartRateToEvents(int heartRate) {
		if (events.stream().anyMatch(a -> a.isStartElement() && a.asStartElement().getName().getLocalPart().equals(ELEMENT_HR))) {
			throw new IllegalArgumentException("Already have '" + ELEMENT_HR + "'");
		}
		final String lastAvailable = addHeartRateToEventsLastAvailable();

		final List<XMLEvent> result = new ArrayList<>(events.size() + 5);

		for (final XMLEvent next : events) {
			if (next.isEndElement() && next.asEndElement().getName().getLocalPart().equals(lastAvailable)) {
				SUPPLIERS.get(lastAvailable).apply(eventFactory, heartRate).forEach(result::add);
			}
			result.add(next);
		}

		return result;
	}

	private String addHeartRateToEventsLastAvailable() {
		final List<String> options = asList(ELEMENT_TRACK_POINT_EXTENSION, ELEMENT_EXTENSIONS);
		for (final String next : options) {
			if (events.stream().anyMatch(a -> a.isStartElement() && a.asStartElement().getName().getLocalPart().equals(next))) {
				return next;
			}
		}
		return OutsideTrkptStateHandler.ELEMENT_TRACK_POINT;
	}

	private Optional<OffsetDateTime> getTimeFromEvents() {
		boolean found = false;
		for (final XMLEvent event : events) {
			if (found) {
				if (event.isCharacters()) {
					return Optional.of(OffsetDateTime.parse(event.asCharacters().getData()));
				}
				throw new IllegalStateException("XML file is not supported - can't read time");
			}

			if (event.isStartElement() && (event.asStartElement().getName().getLocalPart().equals("time"))) {
				found = true;
			}
		}
		return Optional.empty();
	}

}
package eu.tanov.gps.gpxmergeheartrate.parsestates;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import eu.tanov.gps.gpxmergeheartrate.HeartRateProvider;
import eu.tanov.gps.gpxmergeheartrate.GpxMergeHeartRate;

public class InsideTrkptStateHandler extends IdentatingStateHandler {
	private final List<XMLEvent> events = new ArrayList<>();
	private final HeartRateProvider heartRateProvider;

	public InsideTrkptStateHandler(HeartRateProvider heartRateProvider, IdentatingStateHandler identatingStateHandler,
			XMLEvent event) {
		super(identatingStateHandler);
		this.heartRateProvider = heartRateProvider;
		events.add(event);
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
		if (events.stream()
				.anyMatch(a -> a.isStartElement() && a.asStartElement().getName().getLocalPart().equals("extensions"))) {
			throw new UnsupportedOperationException("Having 'extensions' in trkpt is not supported yet, file an issue, please");
		}
		final List<XMLEvent> result = new ArrayList<>(events.size() + 5);

		for (final XMLEvent next : events) {
			if (next.isEndElement()
					&& next.asEndElement().getName().getLocalPart().equals(OutsideTrkptStateHandler.ELEMENT_TRACK_POINT)) {
				result.add(eventFactory.createStartElement("", GpxMergeHeartRate.NAMESPACE_GPX, "extensions"));
				result.add(eventFactory.createStartElement(GpxMergeHeartRate.EXTENSION_PREFIX, "", "TrackPointExtension"));

				result.add(eventFactory.createStartElement(GpxMergeHeartRate.EXTENSION_PREFIX, "", "hr"));

				result.add(eventFactory.createCharacters(String.valueOf(heartRate)));

				result.add(eventFactory.createEndElement(GpxMergeHeartRate.EXTENSION_PREFIX, "", "hr"));

				result.add(eventFactory.createEndElement(GpxMergeHeartRate.EXTENSION_PREFIX, "", "TrackPointExtension"));
				result.add(eventFactory.createEndElement("", GpxMergeHeartRate.NAMESPACE_GPX, "extensions"));

			}
			result.add(next);
		}

		return result;
	}

	private Optional<OffsetDateTime> getTimeFromEvents() {
		boolean found = false;
		for (final XMLEvent event : events) {
			if (found) {
				if (event.isCharacters()) {
					return Optional.of(OffsetDateTime.parse(event.asCharacters().getData()));
				} else {
					throw new IllegalStateException("XML file is not supported - can't read time");
				}
			}

			if (event.isStartElement() && (event.asStartElement().getName().getLocalPart().equals("time"))) {
				found = true;
			}
		}
		return Optional.empty();
	}

}
package eu.tanov.gps.gpxmergeheartrate.parsestates;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import eu.tanov.gps.gpxmergeheartrate.HeartRateProvider;

public class OutsideTrkptStateHandler extends IdentatingStateHandler {
	protected static final String ELEMENT_TRACK_POINT = "trkpt";

	private final HeartRateProvider heartRateProvider;

	public OutsideTrkptStateHandler(HeartRateProvider heartRateProvider, IdentatingStateHandler identatingStateHandler) {
		super(identatingStateHandler);
		this.heartRateProvider = heartRateProvider;
	}

	@Override
	public StateHandler handleEvent(XMLEvent event) throws XMLStreamException {
		if (event.isStartElement() && (event.asStartElement().getName().getLocalPart().equals(ELEMENT_TRACK_POINT))) {
			return new InsideTrkptStateHandler(heartRateProvider, this, event);
		}
		handleIdent(event);

		return new OutsideTrkptStateHandler(heartRateProvider, this);
	}

}
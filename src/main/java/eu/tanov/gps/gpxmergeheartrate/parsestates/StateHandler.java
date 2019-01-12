package eu.tanov.gps.gpxmergeheartrate.parsestates;

import javax.xml.stream.events.XMLEvent;

public interface StateHandler {
	StateHandler handleEvent(XMLEvent event) throws Exception;
}

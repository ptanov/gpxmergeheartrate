package eu.tanov.gps.gpxmergeheartrate.parsestates;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

public abstract class IdentatingStateHandler implements StateHandler {
	protected final XMLEventFactory eventFactory;
	private final XMLEventWriter writer;

	private boolean isPreviousCharacter;
	private int ident;

	protected IdentatingStateHandler(IdentatingStateHandler handler) {
		this(handler.eventFactory, handler.writer, handler.ident, handler.isPreviousCharacter);
	}

	protected IdentatingStateHandler(XMLEventFactory eventFactory, XMLEventWriter writer, int ident,
			boolean isPreviousCharacter) {
		this.eventFactory = eventFactory;
		this.writer = writer;
		this.ident = ident;
		this.isPreviousCharacter = isPreviousCharacter;
	}

	private void addIdentation(XMLEvent event) throws XMLStreamException {
		final int effectiveIdent = ident - (event.isEndElement() ? 1 : 0);
		if (effectiveIdent == 0 || isPreviousCharacter || event.isCharacters()) {
			return;
		}

		final StringBuilder result = new StringBuilder(effectiveIdent + 1);

		result.append("\n");

		for (int i = 0; i < effectiveIdent; i++) {
			result.append("\t");
		}
		writer.add(eventFactory.createCharacters(result.toString()));

	}

	protected void handleIdent(XMLEvent event) throws XMLStreamException {
		addIdentation(event);
		if (event.isStartElement()) {
			ident++;
		} else if (event.isEndElement()) {
			ident--;
		}
		isPreviousCharacter = event.isCharacters();
		writer.add(event);
	}

	public static IdentatingStateHandler createInitialData(XMLEventFactory eventFactory, XMLEventWriter writer) {
		return new IdentatingStateHandler(eventFactory, writer, 0, false) {

			@Override
			public StateHandler handleEvent(XMLEvent event) throws Exception {
				throw new IllegalStateException("MUST not be called - used only for initial creation");
			}

		};

	}
}
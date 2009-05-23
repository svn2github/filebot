
package net.sourceforge.filebot.ui.panel.rename;


import static java.util.Collections.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "history")
class History {
	
	@XmlElement(name = "sequence")
	private List<Sequence> sequences;
	
	
	public History() {
		this.sequences = new ArrayList<Sequence>();
	}
	

	public History(Collection<Sequence> sequences) {
		this.sequences = new ArrayList<Sequence>(sequences);
	}
	
	
	public static class Sequence {
		
		@XmlAttribute(name = "date", required = true)
		private Date date;
		
		@XmlElement(name = "rename", required = true)
		private List<Element> elements;
		
		
		private Sequence() {
			// hide constructor
		}
		

		public Date date() {
			return date;
		}
		

		public List<Element> elements() {
			return unmodifiableList(elements);
		}
		

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Sequence) {
				Sequence other = (Sequence) obj;
				return date.equals(other.date) && elements.equals(other.elements);
			}
			
			return false;
		}
	}
	

	public static class Element {
		
		@XmlAttribute(name = "dir", required = true)
		private File dir;
		
		@XmlAttribute(name = "from", required = true)
		private String from;
		
		@XmlAttribute(name = "to", required = true)
		private String to;
		
		
		private Element() {
			// hide constructor
		}
		

		public File dir() {
			return dir;
		}
		

		public String from() {
			return from;
		}
		

		public String to() {
			return to;
		}
		

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Element) {
				Element element = (Element) obj;
				return to.equals(element.to) && from.equals(element.from) && dir.getPath().equals(element.dir.getPath());
			}
			
			return false;
		}
	}
	
	
	public List<Sequence> sequences() {
		return unmodifiableList(sequences);
	}
	

	public void add(Iterable<Entry<File, File>> elements) {
		Sequence sequence = new Sequence();
		
		sequence.date = new Date();
		sequence.elements = new ArrayList<Element>();
		
		for (Entry<File, File> entry : elements) {
			File from = entry.getKey();
			File to = entry.getValue();
			
			// sanity check, parent folder must be the same for both files
			if (!from.getParentFile().equals(to.getParentFile())) {
				throw new IllegalArgumentException(String.format("Illegal entry: ", entry));
			}
			
			Element element = new Element();
			
			element.dir = from.getParentFile();
			element.from = from.getName();
			element.to = to.getName();
			
			sequence.elements.add(element);
		}
		
		add(sequence);
	}
	

	public void add(Sequence sequence) {
		this.sequences.add(sequence);
	}
	

	public void addAll(Collection<Sequence> sequences) {
		this.sequences.addAll(sequences);
	}
	

	public void merge(History history) {
		for (Sequence sequence : history.sequences()) {
			if (!sequences.contains(sequence)) {
				add(sequence);
			}
		}
	}
	

	public void clear() {
		sequences.clear();
	}
	

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof History) {
			History other = (History) obj;
			return sequences.equals(other.sequences);
		}
		
		return false;
	}
	

	public static void exportHistory(History history, File file) throws IOException {
		try {
			Marshaller marshaller = JAXBContext.newInstance(History.class).createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			
			marshaller.marshal(history, file);
		} catch (JAXBException e) {
			throw new IOException(e);
		}
	}
	

	public static History importHistory(File file) throws IOException {
		try {
			Unmarshaller unmarshaller = JAXBContext.newInstance(History.class).createUnmarshaller();
			
			return ((History) unmarshaller.unmarshal(file));
		} catch (JAXBException e) {
			throw new IOException(e);
		}
	}
	
}

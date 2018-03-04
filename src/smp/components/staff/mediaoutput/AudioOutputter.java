package smp.components.staff.mediaoutput;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import com.sun.media.sound.AudioSynthesizer;

import smp.components.InstrumentIndex;
import smp.components.Values;
import smp.components.staff.Staff;
import smp.components.staff.sequences.StaffNote;
import smp.components.staff.sequences.StaffNoteLine;
import smp.stateMachine.StateMachine;

/**
 * 
 * @since v1.1.1
 * @author J
 *
 */
public class AudioOutputter {

	/** The instruments and their keys that are playing. */
	private ArrayList<ArrayList<Integer>> notesOn = new ArrayList<>();

	/**
	 * The instruments playing at the current timePosition. Tells us to not stop
	 * the instrument's notes.
	 */
	private boolean[] instrumentsOnNow = new boolean[InstrumentIndex.values().length];

	/**
	 * The timePositionExact rounded to the nearest microsecond for shortMessage
	 * timestamps.
	 */
	private long timePosition = 0;

	/** The exact representation of time in microseconds used for accuracy. */
	private double timePositionExact = 0;

	/** The exact increment of time in microseconds. */
	private double timeIncrementExact = 0;

	private Staff theStaff;

	// AUDIO OUTPUT MEMBERS
	private AudioFormat audioFormat;
	private AudioSynthesizer audioSynthesizer;
	private Map<String, Object> infoMap;
	private AudioInputStream audioInputStream;
	private File soundbankFile;
	private Soundbank soundbank;
	private MidiChannel[] midiChannels;
	private ShortMessage msg;
	private Receiver recv;

	public AudioOutputter(Staff staff) {

		theStaff = staff;

		for(int i = 0; i < Values.NUMINSTRUMENTS; i++)
			notesOn.add(new ArrayList<Integer>());
		
		try {
			System.out.println(" * getting synthesizer... ");
			audioFormat = new AudioFormat(44100, 16, 2, true, false);
			audioSynthesizer = (AudioSynthesizer) MidiSystem.getSynthesizer();

			System.out.println(" * opening stream... ");
			infoMap = new HashMap<String, Object>();
			infoMap.put("resampletType", "sinc");
			infoMap.put("maxPolyphony", "1024");
			audioInputStream = audioSynthesizer.openStream(audioFormat, infoMap);

			System.out.println(" * getting SoundBank");
			soundbankFile = new File("soundset3.sf2");
			soundbank = MidiSystem.getSoundbank(soundbankFile);

			System.out.println(" * loading SoundBank");
			audioSynthesizer.loadAllInstruments(soundbank);

			System.out.println(" * initializing instruments");
			midiChannels = audioSynthesizer.getChannels();

			// somehow this gets the synth to start rolling?!.. bug?!
			for (MidiChannel mch : midiChannels) {
				mch.programChange(0); // set instrument (0 = piano by default)
				mch.controlChange(Values.REVERB, 0); // MARIO PAINT SPECIFIC
			}

			System.out.println(" * sending short MIDI messages... ");
			msg = new ShortMessage();
			recv = audioSynthesizer.getReceiver();
			
		} catch (IOException | MidiUnavailableException | InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}

	// TODO:
	public void processArrangement() throws InvalidMidiDataException {
		
	}
	
	public void processSong() throws InvalidMidiDataException {
		// 60000000 is one minute
		// the formula is microseconds/bpm = microseconds per beat
		timeIncrementExact = 60000000 / StateMachine.getTempo();
		int lastLine = theStaff.findLastLine();// theStaff.getLastLine();//temp TODO: make it getLastLine without having to find it...
		int stopLine = (int) (Math.ceil((lastLine + 1) / 4.0) * 4);
		for (int i = 0; i < stopLine; i++) {
			processLine(i);
			seekNext();
		}
	}

	/**
	 * Passes all notes on the specified line to the audioInputStream.
	 * 
	 * @param i
	 *            The index of the line
	 * @throws InvalidMidiDataException
	 */
	private void processLine(int i) throws InvalidMidiDataException {
		StaffNoteLine line = theStaff.getSequence().getLine(i);
		for (StaffNote n : line.getNotes()) {
			
			int instrument = n.getInstrument().ordinal();
			int key = Values.staffNotes[n.getPosition()].getKeyNum() + n.getAccidental();
			
			switch (n.muteNoteVal()) {
			case 2:
				stopInstrument(instrument);
				break;
			case 1:
				stopNote(instrument, key);
				break;
			case 0:
				playNote(instrument, key, line.getVolume());
				break;
			}
		}
	}

	/**
	 * https://sourceforge.net/p/rasmusdsp/discussion/602492/thread/87f58786/
	 * 
	 * @throws IOException
	 * @throws MidiUnavailableException
	 * @throws InvalidMidiDataException
	 * @throws InterruptedException
	 */
	public void test() throws IOException, MidiUnavailableException, InvalidMidiDataException, InterruptedException {
		System.out.println(" * getting synthesizer... ");
		AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
		AudioSynthesizer synthesizer = (AudioSynthesizer) MidiSystem.getSynthesizer();

		System.out.println(" * opening stream... ");
		Map<String, Object> infoMap = new HashMap<String, Object>();
		infoMap.put("resampletType", "sinc");
		infoMap.put("maxPolyphony", "1024");
		AudioInputStream stream = synthesizer.openStream(format, infoMap);

		System.out.println(" * getting SoundBank");
		File soundbankFile = new File("soundset3.sf2");
		Soundbank soundbank = MidiSystem.getSoundbank(soundbankFile);

		System.out.println(" * loading SoundBank");
		synthesizer.loadAllInstruments(soundbank);

		System.out.println(" * initializing instruments");
		MidiChannel[] mChans = synthesizer.getChannels();

		// somehow this gets the synth to start rolling?!.. bug?!
		for (MidiChannel mch : mChans) {
			mch.programChange(0); // set instrument (0 = piano by default)
			mch.controlChange(Values.REVERB, 0);// MARIO PAINT SPECIFIC
		}

		System.out.println(" * sending short MIDI messages... ");
		ShortMessage msg = new ShortMessage();
		Receiver recv = synthesizer.getReceiver();
		msg.setMessage(ShortMessage.PROGRAM_CHANGE, 0, 13, 0);
		recv.send(msg, 0);
		msg.setMessage(ShortMessage.NOTE_ON, 0, 60, 64);
		recv.send(msg, 0);
		msg.setMessage(ShortMessage.NOTE_ON, 1, 64, 64);
		recv.send(msg, 0);
		msg.setMessage(ShortMessage.PROGRAM_CHANGE, 0, 02, 0);
		recv.send(msg, 0);
		msg.setMessage(ShortMessage.NOTE_ON, 0, 40, 64);
		recv.send(msg, 0);
		// msg.setMessage(ShortMessage.PROGRAM_CHANGE, 0, 13, 0);
		// recv.send(msg, 0);
		msg.setMessage(ShortMessage.NOTE_ON, 0, 72, 64);
		recv.send(msg, 0);
		msg.setMessage(ShortMessage.NOTE_OFF, 0, 40, 64);
		recv.send(msg, 250000);
		msg.setMessage(ShortMessage.NOTE_OFF, 0, 50, 64);
		recv.send(msg, 500000);
		msg.setMessage(ShortMessage.NOTE_ON, 0, 50, 64);
		recv.send(msg, 500000);
		msg.setMessage(ShortMessage.NOTE_OFF, 0, 50, 64);
		recv.send(msg, 750000);
		msg.setMessage(ShortMessage.NOTE_ON, 0, 50, 64);
		recv.send(msg, 750000);
		msg.setMessage(ShortMessage.NOTE_OFF, 0, 50, 64);
		recv.send(msg, 1000000);
		msg.setMessage(ShortMessage.NOTE_ON, 0, 50, 64);
		recv.send(msg, 1000000);
		msg.setMessage(ShortMessage.NOTE_OFF, 0, 50, 64);
		recv.send(msg, 1250000);
		msg.setMessage(ShortMessage.NOTE_ON, 0, 72, 64);
		recv.send(msg, 1250000);
		msg.setMessage(ShortMessage.NOTE_OFF, 0, 72, 0);
		recv.send(msg, 3000000);
		msg.setMessage(ShortMessage.NOTE_ON, 0, 72, 64);
		recv.send(msg, 5000000);

		System.out.println(" * stream has bytes available: " + stream.available());
		byte[] b = new byte[stream.available()];
		stream.read(b);
		// Thread.sleep(500);
		System.out.println(" * stream has bytes available: " + stream.available());
		b = new byte[stream.available()];
		stream.read(b);
		// Thread.sleep(500);
		System.out.println(" * stream has bytes available: " + stream.available());

		System.out.println(" * calc length for save to file... ");
		long len = (long) (format.getFrameRate() * 10);

		System.out.println(" * writing to file... ");
		stream = new AudioInputStream(stream, format, len);
		AudioSystem.write(stream, AudioFileFormat.Type.WAVE, new File("output.wav"));

		System.out.println(" * done with MIDI!");
		System.exit(0);
	}

	/**
	 * Plays note with specified instrument, key, and volume. Stops all other
	 * notes with the same instrument if instrument is not on in noteExtensions.
	 * 
	 * @param instrument
	 *            The instrument channel
	 * @param key
	 *            The note's key
	 * @param volume
	 *            The note's volume
	 * @throws InvalidMidiDataException
	 */
	public void playNote(int instrument, int key, int volume) throws InvalidMidiDataException {
		boolean noteExtended = StateMachine.getNoteExtensions()[instrument];
		if(!noteExtended && !instrumentsOnNow[instrument])
			stopInstrument(instrument);
		
		msg.setMessage(ShortMessage.PROGRAM_CHANGE, instrument % 16, instrument, 0);
		recv.send(msg, timePosition);
		msg.setMessage(ShortMessage.NOTE_ON, instrument % 16, key, volume);
		recv.send(msg, timePosition);
		
		notesOn.get(instrument).add(key);
		instrumentsOnNow[instrument] = true;
	}

	/**
	 * Stops note with specified instrument and key. Used when there's a mute
	 * note.
	 * 
	 * @param instruments
	 *            The instrument channel
	 * @param key
	 *            The note's key
	 * @throws InvalidMidiDataException
	 */
	public void stopNote(int instrument, int key) throws InvalidMidiDataException {
		ArrayList<Integer> instrumentKeysOn = notesOn.get(instrument);
		for(int i = 0; i < instrumentKeysOn.size(); i++) {
			 if(instrumentKeysOn.get(i) == key) {
				 msg.setMessage(ShortMessage.PROGRAM_CHANGE, instrument % 16, instrument, 0);
				 recv.send(msg, timePosition);
				 msg.setMessage(ShortMessage.NOTE_OFF, instrument % 16, key, 0);
				 recv.send(msg, timePosition);
				 
				 instrumentKeysOn.remove(i);
				 break;
			 }
		}
	}

	/**
	 * Stops all notes with specified instrument. Used when there's a
	 * mute-instrument note.
	 * 
	 * @param instrument
	 *            The instrument channel
	 * @throws InvalidMidiDataException
	 */
	public void stopInstrument(int instrument) throws InvalidMidiDataException {
		ArrayList<Integer> instrumentKeysOn = notesOn.get(instrument);
		while(instrumentKeysOn.size() > 0) {
			int key = instrumentKeysOn.get(0);
			stopNote(instrument, key);
		}
	}

	/**
	 * Increments the timePosition to the time of the next line.
	 */
	public void seekNext() {
		timePositionExact += timeIncrementExact;
		timePosition = Math.round(timePositionExact);
		for (int i = 0; i < instrumentsOnNow.length; i++)
			instrumentsOnNow[i] = false;
	}

	public void finish() throws IOException {
		System.out.println(" * stream has bytes available: " + audioInputStream.available());
		byte[] b = new byte[audioInputStream.available()];
		audioInputStream.read(b);

		System.out.println(" * calc length for save to file... ");
		double lengthExactPlusOne = timePositionExact / 1000000 + 1;
		System.out.println(lengthExactPlusOne);
		long lengthFrames = (long) (audioFormat.getFrameRate() * lengthExactPlusOne);//lengthSeconds);

		System.out.println(" * writing to " + theStaff.getSequenceName() + ".wav... ");
		audioInputStream = new AudioInputStream(audioInputStream, audioFormat, lengthFrames);
		AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, new File(theStaff.getSequenceName() + ".wav"));

		System.out.println(" * done with MIDI!");
	}
}

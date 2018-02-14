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

import smp.components.Values;

public class AudioOutputter {
	
	/**
	 * This is the list of lists, arranged by instrument channel, of notes
	 * (keys) that are playing.
	 */
	private ArrayList<ArrayList<Integer>> notesOn;

    /** This tells us which channels just started playing notes again. */
    private ArrayList<Boolean> channelRestarted;
    
    long timePosition;
    
	public AudioOutputter() {
		try {
			test();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MidiUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidMidiDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		////// System.out.println(" * getting synthesizer... ");
		////AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
		////AudioSynthesizer synthesizer = (AudioSynthesizer) MidiSystem.getSynthesizer();
        ////
		////// System.out.println(" * opening stream... ");
		////Map<String, Object> infomap = new HashMap<String, Object>();
		////infomap.put("resampletType", "sinc");
		////infomap.put("maxPolyphony", "1024");
		////AudioInputStream stream = synthesizer.openStream(format, infomap);
        ////
		////// System.out.println(" * getting SoundBank");
		////File soundbank_file = new File("soundset3.sf2");
		////Soundbank soundbank = MidiSystem.getSoundbank(soundbank_file);
        ////
		////// System.out.println(" * loading SoundBank");
		////synthesizer.loadAllInstruments(soundbank);
        ////
		////// System.out.println(" * initializing instruments");
		////MidiChannel[] mchans = synthesizer.getChannels();
        ////
		////// somehow this gets the synth to start rolling?!.. bug?!
		////for (MidiChannel mch : mchans) {
		////	mch.programChange(7); // set instrument (0 = piano by default)
		////}
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
		Map<String, Object> infomap = new HashMap<String, Object>();
		infomap.put("resampletType", "sinc");
		infomap.put("maxPolyphony", "1024");
		AudioInputStream stream = synthesizer.openStream(format, infomap);

		System.out.println(" * getting SoundBank");
		File soundbank_file = new File("soundset3.sf2");
		Soundbank soundbank = MidiSystem.getSoundbank(soundbank_file);

		System.out.println(" * loading SoundBank");
		synthesizer.loadAllInstruments(soundbank);

		System.out.println(" * initializing instruments");
		MidiChannel[] mchans = synthesizer.getChannels();

		// somehow this gets the synth to start rolling?!.. bug?!
		for (MidiChannel mch : mchans) {
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
//		msg.setMessage(ShortMessage.PROGRAM_CHANGE, 0, 13, 0);
//		recv.send(msg, 0);
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
//		Thread.sleep(500);
		System.out.println(" * stream has bytes available: " + stream.available());
		b = new byte[stream.available()];
		stream.read(b);
//		Thread.sleep(500);
		System.out.println(" * stream has bytes available: " + stream.available());

		System.out.println(" * calc length for save to file... ");
		long len = (long) (format.getFrameRate() * 10);

		System.out.println(" * writing to file... ");
		stream = new AudioInputStream(stream, format, len);
		AudioSystem.write(stream, AudioFileFormat.Type.WAVE, new File("output.wav"));

		System.out.println(" * done with MIDI!");
		System.exit(0);
	}
	
	public void addNote(int instrument, int key, int volume) {
		// if instrument is not noteExtension and !channelRestarted(instrument)
		//		for(noteKey : notesOn(instrument))
		//			stopNote(instrument, noteKey)
		//		notesOn(instrument).clear()
		// 
		// msg.setMessage(ShortMessage.PROGRAM_CHANGE, 0, instrument, 0);
		// recv.send(msg, timePosition);
		// msg.setMessage(NOTE ON,0,key,volume)
		// recv.send(msg, timePosition)
		//
		// notesOn[instrument].add(key);
		// channelRestarted[isntrumemnt] = true
	}
	
	/**
	 * Stops note with specified instrument and key.
	 * @param instrument
	 * @param key
	 */
	public void stopNote(int instrument, int key) {
		// msg.setMessage(ShortMessage.PROGRAM_CHANGE, 0, instrument, 0);
		// recv.send(msg, timePosition);
		// msg.setMessage(ShortMessage.NOTE_OFF, 0, key, 0);
		// recv.send(msg, timePosition);
	}
	
	public void seekNext() {
		// timePosition += (tempo function whatever)
		// channelRestarted[] = false 
	}
	
}

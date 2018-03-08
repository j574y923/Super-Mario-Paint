package smp.components.staff.mediaoutput;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import smp.components.staff.Staff;

public class AudioOutputterA {
	
	Staff theStaff;
	
	public AudioOutputterA(Staff staff)  {
		
		theStaff = staff;
	}
	
	public void outputSong() throws MidiUnavailableException, InvalidMidiDataException, IOException {
		AudioOutputter pass0 = new AudioOutputter(theStaff, 0);
		pass0.processSong();
		byte[] result0 = pass0.finishSong();
		
		AudioOutputter pass1 = new AudioOutputter(theStaff, 1);
		pass1.processSong();
		byte[] result1 = pass1.finishSong();
		
		for(int i = 44; i < result0.length; i++)
			result0[i] += result1[i];
		
		AudioFormat audioFormat = pass0.getAudioFormat();
		double lengthExactPlusPadding = pass0.getTimePositionExact() / 1000000 + AudioOutputter.AUDIO_PADDING_END;
		long lengthFrames = (long) (audioFormat.getFrameRate() * lengthExactPlusPadding);
		AudioInputStream audioInputStream = new AudioInputStream(new ByteArrayInputStream(result0), audioFormat, lengthFrames);
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, new File(theStaff.getSequenceName() + ".wav"));
        audioInputStream.close();
	}
	
	public void outputArr() {
		
	}
}

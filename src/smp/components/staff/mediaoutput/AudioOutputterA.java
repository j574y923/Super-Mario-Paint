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
	
	public void outputArr() throws MidiUnavailableException, InvalidMidiDataException, IOException {
		AudioOutputter pass0 = new AudioOutputter(theStaff, 0);
		pass0.processArrangement();
		byte[] result0 = pass0.finishArr();
		
		AudioOutputter pass1 = new AudioOutputter(theStaff, 1);
		pass1.processArrangement();
		byte[] result1 = pass1.finishArr();
		
		for(int i = 44; i < result0.length; i+=2) {
	        short buf0A = result0[i+1];
	        short buf0B = result0[i];
	        buf0A = (short) ((buf0A & 0xff) << 8);
	        buf0B = (short) (buf0B & 0xff);

	        short buf1A = result1[i+1];
	        short buf1B = result1[i];
	        buf1A = (short) ((buf1A & 0xff) << 8);
	        buf1B = (short) (buf1B & 0xff);
	        
	        short buf0C = (short) (buf0A + buf0B);
	        short buf1C = (short) (buf1A + buf1B);
	        
	        int why0 = buf0C;
	        int why1 = buf1C;
	        
	        short res;
			if (why0 + why1 > Short.MAX_VALUE) {
				res = Short.MAX_VALUE;
			} else if (why0 + why1 < Short.MIN_VALUE) {
				res = Short.MIN_VALUE;
			} else {
				res = (short) (buf0C + buf1C);
			}
	        
	        result0[i] = (byte) res;
	        result0[i+1] = (byte) (res >> 8);
		}
		
		AudioFormat audioFormat = pass0.getAudioFormat();
		double lengthExactPlusPadding = pass0.getTimePositionExact() / 1000000 + AudioOutputter.AUDIO_PADDING_END;
		long lengthFrames = (long) (audioFormat.getFrameRate() * lengthExactPlusPadding);
		AudioInputStream audioInputStream = new AudioInputStream(new ByteArrayInputStream(result0), audioFormat, lengthFrames);
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, new File(theStaff.getArrangementName() + ".wav"));
        audioInputStream.close();
	}
}

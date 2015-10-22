package ua.com.mysechko.cyberbeatbox;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class BeatBox {

	JPanel mainPanel;
	JFrame theFrame;

	/* All checkboxes stored in the array */
	ArrayList<JCheckBox> checkboxList;
	Sequencer sequencer;
	Sequence sequence;
	Track track;

	/* Name for the drum-sounds */
	String[] instrumentNames = { "Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal", "Hand CLap", "High Tom", "Hi Bongo",
			"Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low-mid Tom", "High Agogo", "Open Hi Congo" };

	/* Drum <keys> */
	int[] instruments = { 35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63 };

	public static void main(String[] args) {
		new BeatBox().buidGUI();
	}

	public void buidGUI() {
		theFrame = new JFrame("Cyber BeatBox");
		theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		BorderLayout layout = new BorderLayout();
		JPanel background = new JPanel(layout);
		background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		/* Box contains buttons Start, Stop, TempoUp, TempoDown */
		Box buttonsBox = new Box(BoxLayout.Y_AXIS);

		JButton start = new JButton("Start");
		start.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				buildTrackAndStart();
			}
		});
		buttonsBox.add(start);

		JButton stop = new JButton("Stop");
		stop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sequencer.stop();
			}
		});
		buttonsBox.add(stop);

		JButton tempoUp = new JButton("TempoUp");
		start.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				float tempoUp = sequencer.getTempoFactor();
				sequencer.setTempoFactor((float) (tempoUp * 1.03));
			}
		});
		buttonsBox.add(tempoUp);

		JButton tempoDown = new JButton("TempoDown");
		start.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				float tempoDown = sequencer.getTempoFactor();
				sequencer.setTempoFactor((float) (tempoDown * 0.97));
			}
		});
		buttonsBox.add(tempoDown);
		
		JButton serialize = new JButton("Save schema");
		serialize.addActionListener(new MySendListener());
		buttonsBox.add(serialize);
		
		JButton deserialize = new JButton("Load schema");
		deserialize.addActionListener(new MyReadInListener());
		buttonsBox.add(deserialize);

		Box nameBox = new Box(BoxLayout.Y_AXIS);

		/* Add all drum-sound names to the list */
		for (int i = 0; i < instrumentNames.length; i++) {
			nameBox.add(new JLabel(instrumentNames[i]));
			nameBox.add(Box.createRigidArea(new Dimension(5, 6)));
		}

		background.add(BorderLayout.EAST, buttonsBox);
		background.add(BorderLayout.WEST, nameBox);

		theFrame.getContentPane().add(background);
		;

		/* Create new grid 16x16 for the checkboxes */
		GridLayout grid = new GridLayout(16, 16);
		grid.setHgap(2);
		grid.setVgap(1);
		JPanel mainPanel = new JPanel(grid);
		background.add(BorderLayout.CENTER, mainPanel);

		checkboxList = new ArrayList<>();

		/* Add all the 256 checkboxes to the ArrayList and to the panel */
		for (int j = 0; j < 256; j++) {
			JCheckBox chb = new JCheckBox();
			chb.setSelected(false);
			checkboxList.add(chb);
			mainPanel.add(chb);
		}

		setUpMidi();

		theFrame.setMinimumSize(new Dimension(600, 411));
		theFrame.setLocationRelativeTo(null);
		theFrame.pack();
		theFrame.setVisible(true);
	}

	public void setUpMidi() {
		try {
			sequencer = MidiSystem.getSequencer();
			sequencer.open();
			sequence = new Sequence(Sequence.PPQ, 4);
			track = sequence.createTrack();
			sequencer.setTempoInBPM(120);
		} catch (MidiUnavailableException | InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}

	public void buildTrackAndStart() {
		int[] tracklist = null;

		sequence.deleteTrack(track);
		track = sequence.createTrack();

		for (int i = 0; i < 16; i++) {
			tracklist = new int[16];

			int key = instruments[i];

			/* checking the row for the current drum-sound */
			for (int j = 0; j < 16; j++) {
				JCheckBox c = (JCheckBox) checkboxList.get(j + (16 * i));
				if (c.isSelected()) {
					tracklist[j] = key;
				} else {
					tracklist[j] = 0;
				}
			}
			makeTracks(tracklist);
			 track.add(makeEvent(176, 1, 127, 0, 16));
		}

		/* We must be sure that every pattern contains track #16 (but from 0 to 15 - so 15-th) */
		track.add(makeEvent(192, 9, 1, 0, 15));

		try {
			sequencer.setSequence(sequence);
			sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
			sequencer.start();
			sequencer.setTempoInBPM(120);
		} catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}

	/*method create sequence for every tracklist for every drum-sound and add it to the track*/
	public void makeTracks(int[] list) {
		for (int i = 0; i < 16; i++) {
			int key = list[i];

			if (key != 0) {
				track.add(makeEvent(144, 9, key, 100, i));
				track.add(makeEvent(128, 9, key, 100, i + 1));
			}
		}
	}

	/*Method make <event> to be added to the track - for every-drum sound from 1 to 16 tick*/
	public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
		MidiEvent event = null;
		try {
			ShortMessage message = new ShortMessage();
			message.setMessage(comd, chan, one, two);
			event = new MidiEvent(message, tick);
		} catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
		return event;
	}
	
	/*Listener to attach it to the <serialize> button and to serialize music schema*/
	public class MySendListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			boolean[] checkboxStateSer = new boolean[256];
			
			for(int i = 0; i < 256; i++){
				JCheckBox testBox = (JCheckBox) checkboxList.get(i);
				if(testBox.isSelected()){
					checkboxStateSer[i] = true;
				}
			}
			
			JFileChooser fileSaved = new JFileChooser();
			fileSaved.showSaveDialog(theFrame);
			serializeIntoFile( fileSaved.getSelectedFile(), checkboxStateSer);		
		}
	}
	
	/*Method to serialize music schema from <array> into the specified <file>*/
	private void serializeIntoFile(File file, boolean[] array){
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
			oos.writeObject(array);
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*Listener to attach it to the <deserialize> button*/
	public class MyReadInListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser fileOpened = new JFileChooser();
			fileOpened.showOpenDialog(theFrame);
			boolean [] tempArray = deserializeMusicSchema(fileOpened.getSelectedFile());
			
			for (int j = 0; j < 256; j++){
				JCheckBox check = (JCheckBox) checkboxList.get(j);
				if(tempArray[j]){
					check.setSelected(true);
				} else {
					check.setSelected(false);
				}
			}
			sequencer.stop();
			buildTrackAndStart();
		}
	}
	
	/*Method to deserialize boolean array with music schema from specified file*/
	private boolean [] deserializeMusicSchema(File file){
		boolean [] checkboxStateDsr = null;
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
			checkboxStateDsr = (boolean []) ois.readObject();
			ois.close();
		} catch (FileNotFoundException e) {
			System.out.println("File: " + file.getName() + " not found.");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return checkboxStateDsr;
	}
}

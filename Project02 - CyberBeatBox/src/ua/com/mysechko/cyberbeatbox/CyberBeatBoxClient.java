package ua.com.mysechko.cyberbeatbox;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class CyberBeatBoxClient {

	JPanel mainPanel;
	JFrame theFrame;
	JList incomingList;
	JTextField userMessage;

	Vector<String> listWithUsers = new Vector<>();
	int nextUser;
	static String generalName;
	String userName;
	private static final String DEFAULT_USER = "Default User#";
	HashMap<String, boolean[]> otherSeqsMap = new HashMap<>();

	/* All checkboxes stored in the array */
	ArrayList<JCheckBox> checkboxList;
	Sequencer sequencer;
	Sequence sequence;
	Track track;

	/* Object input and output streams to send/receive serializible objects */
	ObjectOutputStream out;
	ObjectInputStream oit;

	/* Name for the drum-sounds */
	String[] instrumentNames = { "Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal", "Hand CLap", "High Tom", "Hi Bongo",
			"Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low-mid Tom", "High Agogo", "Open Hi Congo" };

	/* Drum <keys> */
	int[] instruments = { 35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63 };

	public static void main(String[] args) {
		if ((generalName = args[0]) != null) {
			new CyberBeatBoxClient().startUp(args[0]);
		} else {
			new CyberBeatBoxClient().startUp(DEFAULT_USER);
		}
	}

	/* Establish the communication with server */
	public void startUp(String name) {
		userName = name;
		try {
			Socket clientSocket = new Socket("127.0.0.1", 5000);
			out = new ObjectOutputStream(clientSocket.getOutputStream());
			oit = new ObjectInputStream(clientSocket.getInputStream());

			Thread thread = new Thread(new RemoteReader());
			thread.start();

			setUpMidi();
			buildGUI();

		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.out.println("Sorry, connection couldn`t be stablished.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void buildGUI() {
		theFrame = new JFrame("Cyber BeatBox");
		theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		BorderLayout layout = new BorderLayout();
		JPanel background = new JPanel(layout);
		background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JMenuBar menuBar = new JMenuBar();
		
		JMenu file = new JMenu("File");
		menuBar.add(file);

		JMenuItem open = new JMenuItem("Open");
		open.addActionListener(new MyReadInListener ());
		file.add(open);

		JMenuItem save = new JMenuItem("Save");
		save.addActionListener(new MySaveListener());
		file.add(save);
		
		theFrame.setJMenuBar(menuBar);

		/* Box contains buttons Start, Stop, TempoUp, TempoDown */
		Box buttonsBox = new Box(BoxLayout.Y_AXIS);

		JButton start = new JButton("Start");
		start.addActionListener(new MyStartActionListener());
		buttonsBox.add(start);

		JButton stop = new JButton("Stop");
		stop.addActionListener(new MyStopActionListener());
		buttonsBox.add(stop);

		JButton tempoUp = new JButton("TempoUp");
		tempoUp.addActionListener(new MyTempoUpActionListener());
		buttonsBox.add(tempoUp);

		JButton tempoDown = new JButton("TempoDown");
		tempoDown.addActionListener(new MyTempoDownActionListener());
		buttonsBox.add(tempoDown);

		JButton send = new JButton("Send");
		send.addActionListener(new MySendListener());
		buttonsBox.add(send);

		userMessage = new JTextField();
		buttonsBox.add(userMessage);
		
		/*List with data from server - music schemes*/
		incomingList = new JList<>();
		incomingList.addListSelectionListener(new MyListListener());
		incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane listScroll = new JScrollPane(incomingList);
		buttonsBox.add(listScroll);
		incomingList.setListData(listWithUsers);
		
		Box nameBox = new Box(BoxLayout.Y_AXIS);

		/* Add all drum-sound names to the list */
		for (int i = 0; i < instrumentNames.length; i++) {
			nameBox.add(new JLabel(instrumentNames[i]));
			nameBox.add(Box.createRigidArea(new Dimension(5, 6)));
		}

		background.add(BorderLayout.EAST, buttonsBox);
		background.add(BorderLayout.WEST, nameBox);

		theFrame.getContentPane().add(background);

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

	/* method create sequence for every tracklist for every drum-sound and add it to the track */
	public void makeTracks(int[] list) {
		for (int i = 0; i < 16; i++) {
			int key = list[i];

			if (key != 0) {
				track.add(makeEvent(144, 9, key, 100, i));
				track.add(makeEvent(128, 9, key, 100, i + 1));
			}
		}
	}

	/* Method make <event> to be added to the track - for every-drum sound from 1 to 16 tick */
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
	
	public class MyStartActionListener implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent e) {
			buildTrackAndStart();
		}
	}
	
	public class MyStopActionListener implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent e) {
			sequencer.stop();
		}
	}
	
	public class MyTempoUpActionListener implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent e) {
			float tempoUp = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float) (tempoUp * 1.03));
		}
	}
	
	public class MyTempoDownActionListener implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent e) {
			float tempoDown = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float) (tempoDown * 0.97));
		}
	}

	/* Listener to attach it to the <Save> menu item and to save music schema itno the serialized file */
	public class MySaveListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			boolean[] checkboxStateSer = new boolean[256];

			for (int i = 0; i < 256; i++) {
				JCheckBox testBox = (JCheckBox) checkboxList.get(i);
				if (testBox.isSelected()) {
					checkboxStateSer[i] = true;
				}
			}

			JFileChooser fileSaved = new JFileChooser();
			fileSaved.showSaveDialog(theFrame);
			serializeIntoFile(fileSaved.getSelectedFile(), checkboxStateSer);
		}
	}

	/*A listener to send serialized files to the server (output stream)*/
	public class MySendListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			boolean[] checkboxState = new boolean[256];

			for (int i = 0; i < 256; i++) {
				JCheckBox testBox = (JCheckBox) checkboxList.get(i);
				if (testBox.isSelected()) {
					checkboxState[i] = true;
				}
			}
			try {
				out.writeObject(userName + nextUser++ + ": " + userMessage.getText());
				out.writeObject(checkboxState);
			} catch (IOException e1) {
				System.out.println("Couldn`t send a message");;
			}
			userMessage.setText("");
		}
	}
	
	/*Listener to read the choosen value from the list*/
	public class MyListListener implements ListSelectionListener{
		@Override
		public void valueChanged(ListSelectionEvent e) {
			if(!e.getValueIsAdjusting()){
				String keyWithName = (String) incomingList.getSelectedValue();
				if(keyWithName != null){
					boolean[] checks = (boolean[]) otherSeqsMap.get(keyWithName);
					changeSequence(checks);
					sequencer.stop();
					buildTrackAndStart();
				}
			}
		}
	}

	/* Method to serialize music schema from <array> into the specified <file> */
	private void serializeIntoFile(File file, boolean[] array) {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
			oos.writeObject(array);
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* Listener to attach it to the <deserialize> button */
	public class MyReadInListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser fileOpened = new JFileChooser();
			fileOpened.showOpenDialog(theFrame);
			boolean[] tempArray = deserializeMusicSchema(fileOpened.getSelectedFile());

			for (int j = 0; j < 256; j++) {
				JCheckBox check = (JCheckBox) checkboxList.get(j);
				if (tempArray[j]) {
					check.setSelected(true);
				} else {
					check.setSelected(false);
				}
			}
			sequencer.stop();
			buildTrackAndStart();
		}
	}

	/* Method to deserialize boolean array with music schema from specified file */
	private boolean[] deserializeMusicSchema(File file) {
		boolean[] checkboxStateDsr = null;
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
			checkboxStateDsr = (boolean[]) ois.readObject();
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

	/* Object of this class reads incoming message and save if into the defined data structures */
	public class RemoteReader implements Runnable {
		boolean[] checkBoxState;
		String newName;
		Object obj;

		@Override
		public void run() {
			try {
				while ((obj = oit.readObject()) != null) {
					System.out.println("Read objects from the server. First object: " + obj.getClass());
					newName = (String) obj;
					checkBoxState = (boolean[]) oit.readObject();
					otherSeqsMap.put(newName, checkBoxState);
					listWithUsers.add(newName);
					incomingList.setListData(listWithUsers);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/* Apply new sequence chosen from the list */
	public void changeSequence(boolean[] checkBoxState) {
		for (int i = 0; i < 256; i++) {
			JCheckBox check = (JCheckBox) checkboxList.get(i);
			if (checkBoxState[i]) {
				check.setSelected(true);
			} else {
				check.setSelected(false);
			}
		}
	}
}

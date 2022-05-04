package com.apixio.converters.raps.gui;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;

import com.apixio.converters.raps.RAPSParser;
import com.apixio.model.patient.Patient;
import com.apixio.model.utility.PatientJSONParser;

public class RAPSReportExporterTool {

	private JFrame frmConvertRapsFiles;
	private JTextField inputFolder;
	private String selectedFolderName;
	private File selectedOutputFolder;
	
	// jdk-6 backport fix - required because jdk 6 has untyped
	// swing libraries.
	private DefaultListModel listModel = new DefaultListModel();

	// Create a file chooser
	private final JFileChooser fc = new JFileChooser();
	private JLabel lblFileList;
	private JLabel statusLabel;
	private JProgressBar fileProgressBar;
	private JTextField outputFolderTextField;
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					RAPSReportExporterTool window = new RAPSReportExporterTool();
					window.frmConvertRapsFiles.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public RAPSReportExporterTool() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		final RAPSReportExporterTool currentToolWindow = this;

		frmConvertRapsFiles = new JFrame();
		frmConvertRapsFiles.setTitle("Convert RAPS Files to Patient Objects");
		frmConvertRapsFiles.setBounds(100, 100, 588, 471);
		frmConvertRapsFiles.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 1.0, 0.0, 0.0,
				Double.MIN_VALUE };
		frmConvertRapsFiles.getContentPane().setLayout(gridBagLayout);

		JPanel inputPanel = new JPanel();
		GridBagConstraints gbc_inputPanel = new GridBagConstraints();
		gbc_inputPanel.fill = GridBagConstraints.BOTH;
		gbc_inputPanel.insets = new Insets(0, 0, 5, 5);
		gbc_inputPanel.gridx = 0;
		gbc_inputPanel.gridy = 0;
		frmConvertRapsFiles.getContentPane().add(inputPanel, gbc_inputPanel);
		inputPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		JLabel lblChooseFolder = new JLabel("Choose Folder");
		inputPanel.add(lblChooseFolder);

		inputFolder = new JTextField();
		lblChooseFolder.setLabelFor(inputFolder);
		inputPanel.add(inputFolder);
		inputFolder.setColumns(30);

		JButton btnNewButton = new JButton("Open");
		btnNewButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				// this happens when we have a new click.
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int retVal = fc
						.showOpenDialog(currentToolWindow.frmConvertRapsFiles);
				if (retVal == JFileChooser.APPROVE_OPTION) {
					File selectedFile = fc.getSelectedFile();
					currentToolWindow.inputFolder.setText(selectedFile
							.getAbsolutePath());
					currentToolWindow.setSelectedFolderName(selectedFile
							.getAbsolutePath());
					currentToolWindow.populateFileGrid();
				}
			}
		});
		inputPanel.add(btnNewButton);

		JPanel processPanel = new JPanel();
		GridBagConstraints gbc_processPanel = new GridBagConstraints();
		gbc_processPanel.fill = GridBagConstraints.BOTH;
		gbc_processPanel.insets = new Insets(0, 0, 5, 0);
		gbc_processPanel.gridwidth = 4;
		gbc_processPanel.anchor = GridBagConstraints.NORTHWEST;
		gbc_processPanel.gridx = 0;
		gbc_processPanel.gridy = 1;
		frmConvertRapsFiles.getContentPane()
				.add(processPanel, gbc_processPanel);
		processPanel.setLayout(new BorderLayout(0, 0));

		lblFileList = new JLabel("Files List");
		processPanel.add(lblFileList, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane();
		processPanel.add(scrollPane, BorderLayout.CENTER);

		// jdk-6 backport fix - required because jdk 6 has untyped
		// swing libraries.
		JList filesList = new JList(listModel);
		scrollPane.setViewportView(filesList);

		JPanel outputPanel = new JPanel();
		GridBagConstraints gbc_outputPanel = new GridBagConstraints();
		gbc_outputPanel.gridwidth = 0;
		gbc_outputPanel.gridheight = 0;
		gbc_outputPanel.fill = GridBagConstraints.BOTH;
		gbc_outputPanel.gridx = 0;
		gbc_outputPanel.gridy = 2;
		frmConvertRapsFiles.getContentPane().add(outputPanel, gbc_outputPanel);
		outputPanel.setLayout(new BorderLayout(0, 0));

		JSplitPane splitPane = new JSplitPane();
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		outputPanel.add(splitPane);

		JPanel topPanel = new JPanel();
		splitPane.setLeftComponent(topPanel);
		topPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		JLabel lblNewLabel = new JLabel("Choose Output Folder");
		topPanel.add(lblNewLabel);

		outputFolderTextField = new JTextField();
		topPanel.add(outputFolderTextField);
		outputFolderTextField.setColumns(20);

		JButton btnNewButton_1 = new JButton("Choose");
		btnNewButton_1.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int readValue = fc
						.showOpenDialog(currentToolWindow.frmConvertRapsFiles);
				if (readValue == JFileChooser.APPROVE_OPTION) {
					selectedOutputFolder = fc.getSelectedFile();
					currentToolWindow.outputFolderTextField
							.setText(selectedOutputFolder.getAbsolutePath());
				}
			}
		});
		topPanel.add(btnNewButton_1);

		JPanel bottomPanel = new JPanel();
		splitPane.setRightComponent(bottomPanel);

		statusLabel = new JLabel("");
		bottomPanel.add(statusLabel);

		fileProgressBar = new JProgressBar();
		bottomPanel.add(fileProgressBar);

		JButton btnExport = new JButton("Export");
		bottomPanel.add(btnExport);
		btnExport.addActionListener(new ActionListener() {

			// start the export operation by running through all the files that
			// are
			// present in the selected folder.
			public void actionPerformed(ActionEvent e) {
				currentToolWindow.startExport();
			}
		});
	}

	protected void startExport() {

		if (StringUtils.isBlank(getSelectedFolderName())) {
			JOptionPane.showMessageDialog(frmConvertRapsFiles,
					"Please select input directory before proceeding");
			return;
		}

		File inputDir = new File(getSelectedFolderName());
		File outputDir = getSelectedOutputFolder();

		if (outputDir == null) {
			JOptionPane.showMessageDialog(frmConvertRapsFiles,
					"Please select output directory before proceeding");
			return;
		}

		String[] inputFileNames = inputDir.list();
		int n = 0;
		for (String inputFileName : inputFileNames) {
			File inputFile = new File(getSelectedFolderName() + "/"
					+ inputFileName);

			// skip inner directories.
			if (inputFile.isDirectory())
				continue;
			PrintWriter writer = null;
			// generate patient objects (.apo Files)
			try {
				statusLabel.setText("Processing file " + inputFileName);
				InputStream is = new FileInputStream(inputFile);
				writer = new PrintWriter(new FileWriter(
						outputDir.getAbsolutePath() + "/" + inputFileName
								+ ".apos"));
				RAPSParser parser = new RAPSParser();
				parser.parse(is, null);
				for (Patient p : parser.getPatients()) {
					writer.println(new PatientJSONParser().toJSON(p));//.toGsonJSON(p));
				}
			} catch (Exception e) {
				System.err.println("Error while processing " + inputFileName
						+ " : " + e.getMessage());
			} finally {
				if(writer != null)
					writer.close();
			}
			
			// update the progress bar.
			++n;
			fileProgressBar.setValue(n);
		}
	}

	protected void populateFileGrid() {
		File f = new File(getSelectedFolderName());
		String[] fileNames = f.list();
		int nFiles = 0;
		long nBytes = 0;

		for (String fileName : fileNames) {
			File innerFile = new File(getSelectedFolderName() + "/" + fileName);
			if (!innerFile.isDirectory()) {
				++nFiles;
				listModel.addElement(String.format("%s - %d bytes (%s)",
						innerFile.getAbsolutePath(), innerFile.length(),
						new DateTime(innerFile.lastModified()).toString()));
				nBytes += innerFile.length();
			}
		}
		// update the label.
		lblFileList.setText(String.format(
				"Files List - Found %d files (Total Size : %dMB)", nFiles,
				(nBytes / (1024 * 1024))));
		fileProgressBar.setMaximum(nFiles);
		fileProgressBar.setValue(0);
	}

	String getSelectedFolderName() {
		return selectedFolderName;
	}

	void setSelectedFolderName(String selectedFolderName) {
		this.selectedFolderName = selectedFolderName;
	}

	File getSelectedOutputFolder() {
		return selectedOutputFolder;
	}

	void setSelectedOutputFolder(File selectedOutputFolder) {
		this.selectedOutputFolder = selectedOutputFolder;
	}

}

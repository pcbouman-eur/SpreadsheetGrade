package nl.eur.ese.spreadsheettest;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import javax.xml.bind.JAXBException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class XMLTestGUI extends JFrame implements ActionListener
{
	private String noFile = "(No File Selected)";
	
	private JTextField xml, ref, test;
	private File lastDir;
	private File xmlFile, refFile, testFile;
	private JButton setXML, setReference, setTest, run;
	private JTextArea log;
	
	public XMLTestGUI()
	{
		super();
		setTitle("Excel Autograder");
		setSize(800,600);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		init();
		setVisible(true);
		setLocationRelativeTo(null);
	}
	
	private void init()
	{
		JPanel bottom, temp, temp2;
		
		bottom = new JPanel();
		bottom.setLayout(new GridLayout(4,1));
		
		temp = new JPanel();
		temp.setLayout(new BorderLayout());
		xml = new JTextField();
		xml.setText(noFile);
		xml.setEditable(false);
		temp.add(xml, BorderLayout.CENTER);
		temp2 = new JPanel();
		setXML = new JButton("Choose");
		setXML.addActionListener(this);
		temp2.add(setXML);
		temp.add(temp2, BorderLayout.EAST);
		temp.setBorder(BorderFactory.createTitledBorder("XML Exercise"));
		bottom.add(temp);
		
		temp = new JPanel();
		temp.setLayout(new BorderLayout());
		ref = new JTextField();
		ref.setText(noFile);
		ref.setEditable(false);
		temp.add(ref, BorderLayout.CENTER);
		temp2 = new JPanel();
		setReference = new JButton("Choose");
		setReference.addActionListener(this);
		temp2.add(setReference);
		temp.add(temp2, BorderLayout.EAST);
		temp.setBorder(BorderFactory.createTitledBorder("Reference Solution .xlsx"));
		bottom.add(temp);
		
		temp = new JPanel();
		temp.setLayout(new BorderLayout());
		test = new JTextField();
		test.setText(noFile);
		test.setEditable(false);
		temp.add(test, BorderLayout.CENTER);
		temp2 = new JPanel();
		setTest = new JButton("Choose");
		setTest.addActionListener(this);
		temp2.add(setTest);
		temp.add(temp2, BorderLayout.EAST);
		temp.setBorder(BorderFactory.createTitledBorder("Solution to test .xlsx"));
		bottom.add(temp);
		
		temp = new JPanel();
		run = new JButton("Run");
		run.addActionListener(this);
		temp.add(run);
		temp.setBorder(BorderFactory.createTitledBorder("Control"));
		bottom.add(temp);
		
		setLayout(new BorderLayout());
		add(bottom, BorderLayout.SOUTH);
		
		log = new JTextArea();
		log.setEditable(false);
		log.setFont(new Font("monospaced", Font.PLAIN, 12));
		JScrollPane center = new JScrollPane(log);
		add(center, BorderLayout.CENTER);
		
		lastDir = new File(System.getProperty("user.dir"));
	}

	@Override
	public void actionPerformed(ActionEvent ae)
	{
		if (ae.getSource() == setXML)
		{
			File f = chooseFile(".xml", "XML Exercise");
			if (f != null)
			{
				xmlFile = f;
				xml.setText(f.toString());
			}
		}
		if (ae.getSource() == setReference)
		{
			File f = chooseFile(".xlsx", "Excel File");
			if (f != null)
			{
				refFile = f;
				ref.setText(f.toString());
			}
		}
		if (ae.getSource() == setTest)
		{
			File f = chooseFile(".xlsx", "Excel File");
			if (f != null)
			{
				testFile = f;
				test.setText(f.toString());
			}
		}
		if (ae.getSource() == run)
		{
			if (xmlFile == null || refFile == null || testFile == null)
			{
				JOptionPane.showMessageDialog(this, "You must select all three files before a test can be run!");
				return;
			}
			
			XMLTest exercise;
			long time = System.currentTimeMillis();
			try
			{
				exercise = new XMLTest(xmlFile, true);
			}
			catch (JAXBException je)
			{
				JOptionPane.showMessageDialog(this, "An error occurred while reading the XML file. Please view the log.");
				log.setText("An error occurred while reading the XML file. \n\n"+getStackTrace(je));
				return;
			}
			
			try ( XSSFWorkbook refBook = new XSSFWorkbook(refFile); XSSFWorkbook testBook = new XSSFWorkbook(testFile) )
			{
				try
				{
					exercise.testAll(refBook, testBook);
					time = System.currentTimeMillis() - time;
				}
				catch (Exception e)
				{
					e.printStackTrace();
					JOptionPane.showMessageDialog(this, "An error occurred while running the tests. This should not happen! ");
					log.setText("An error occurred while running the tests.\nPlease notify Paul about this\n\n"+getStackTrace(e));
					return;
				}
				
				String errors = exercise.getErrorTrace();
				StringBuilder sb = new StringBuilder();
				sb.append("The test ran without problems. Finished in "+time+" ms.\n\n");
				if (errors.length() > 0)
				{
					sb.append("Some errors occurred while evaluating the Excel sheets.\n");
					sb.append("Detailed error logging information (not visible to the student)\n");
					sb.append(errors);
					sb.append("\n\n");
				}
				sb.append("Output for the Student\n----------------------\n\n");
				sb.append(exercise.makeReport(true));
				
				log.setText(sb.toString());
				
			} catch (InvalidFormatException e)
			{
				JOptionPane.showMessageDialog(this, "The format of the Excel files is invalid. Please view the log.");
				log.setText("The format of the Excel files is invalid. \n\n"+getStackTrace(e));
				return;
			}
			catch (IOException e)
			{
				JOptionPane.showMessageDialog(this, "An error occurred while reading the Excel files. Please view the log.");
				log.setText("An error occurred while reading the Excel files. \n\n"+getStackTrace(e));
				return;
			}
			
		}
	}
	
	private File chooseFile(String ext, String desc)
	{
		JFileChooser jfc = new JFileChooser();
		if (lastDir != null)
		{
			jfc.setCurrentDirectory(lastDir);
		}
		jfc.setMultiSelectionEnabled(false);
		jfc.setFileFilter(
					new FileFilter(){
						@Override
						public boolean accept(File f)
						{
							return f.isDirectory() || (f.isFile() && f.getName().toLowerCase().endsWith(ext.toLowerCase()));
						}

						@Override
						public String getDescription()
						{
							return desc +"(*"+ext+")";
						}
					});
		
		int res = jfc.showOpenDialog(this);
		if (res == JFileChooser.APPROVE_OPTION)
		{
			File f = jfc.getSelectedFile();
			if (!f.isFile())
			{
				JOptionPane.showMessageDialog(this, "That is not a valid file!");
				return null;
			}
			lastDir = f.getParentFile();
			return f;
		}
		return null;
	}
	
	public static void main(String [] args)
	{
		DatabaseFunctions.register();
		ModernFunctions.register();
		new XMLTestGUI();
	}
	
	public static String getStackTrace(Exception e)
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(bos);
		e.printStackTrace(pw);
		pw.flush();
		return bos.toString();
	}
	
}

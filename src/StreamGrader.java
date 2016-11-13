import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.bind.JAXBException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.InvalidOperationException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class StreamGrader
{
	private long timeout;
	private DataInputStream input;
	private DataOutputStream output;
	
	public static void main(String [] args)
	{
		long to = Long.parseLong(args[0]);
		StreamGrader sg = new StreamGrader(to, System.in, System.out);
		sg.process();
	}

	public StreamGrader(long timeout, InputStream is, OutputStream os)
	{
		this.timeout = timeout;
		this.input = new DataInputStream(is);
		this.output = new DataOutputStream(os);
	}
	
	public void process()
	{
		while (true)
		{
			long jobID;
			try
			{
				jobID = input.readLong();
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
				return;
			}
			try
			{
				round(jobID);
			}
			catch (EOFException e)
			{
				System.exit(0);
			}
			catch (IOException | JAXBException e)
			{
				TestResults tr = new TestResults();
				tr.addError("Unexpected error. Please contact us. (Error details: "+e.getMessage()+")");
				try {
					output.writeLong(jobID);
					output.writeBoolean(false);
					tr.sendTestResults(output);
					output.flush();
				}
				catch (IOException ex)
				{
					ex.printStackTrace();
				}
			}
		}
	}
	
	public void round(long jobID) throws IOException, JAXBException
	{		
		int numBytes = input.readInt();
		byte [] xmlData = new byte[numBytes];
		input.readFully(xmlData, 0, numBytes);
		
		numBytes = input.readInt();
		byte [] refData = new byte[numBytes];
		input.readFully(refData, 0, numBytes);
		
		numBytes = input.readInt();
		byte [] testData = new byte[numBytes];
		input.readFully(testData, 0, numBytes);
		
		// Feed the data to the tester here

		File tmpXml = File.createTempFile("excelgrade", ".xml");
		tmpXml.deleteOnExit();
		Files.write(tmpXml.toPath(), xmlData);
		
		File tmpRef = File.createTempFile("excelgrade-ref", ".xlsx");
		tmpRef.deleteOnExit();
		Files.write(tmpRef.toPath(), refData);
		
		File tmpTest = File.createTempFile("excelgrade-test", ".xlsx");
		tmpTest.deleteOnExit();
		Files.write(tmpTest.toPath(), testData);
		
		XMLTest test = new XMLTest(tmpXml);
		Timeout to = new Timeout(jobID);
		Thread toThread = new Thread(to);
		toThread.start();
		
		TestResults tr;
		try (XSSFWorkbook refBook = new XSSFWorkbook(tmpRef);
				XSSFWorkbook testBook = new XSSFWorkbook(tmpTest))
		{
			test.testAll(refBook, testBook);
			tr = test.makeReport();
		}
		catch (InvalidFormatException|InvalidOperationException ife)
		{
			tr = new TestResults();
			tr.addError("Error reading submitted file. Make sure you submit an Excel file.");
		}
		to.deactivate();
		
		
		
		tmpXml.delete();
		tmpRef.delete();
		tmpTest.delete();
		
		// Send output back to calling process
		output.writeLong(jobID);
		output.writeBoolean(true);
		tr.sendTestResults(output);
		output.flush();		
	}

	private class Timeout implements Runnable
	{
		private long jobID;
		private boolean active = true;
		
		public Timeout(long jid)
		{
			jobID = jid;
		}
		
		@Override
		public void run()
		{
			
			try
			{
				Thread.sleep(timeout);
			}
			catch (InterruptedException e)
			{
				stopProcess("An unexpected error occurred. Please contact us. (Error details: "+e.getMessage() +
						")");
			}
			if (active)
			{
				stopProcess("Excel grader took longer than the maximum time of "+timeout+"ms. "
						+ "Try again later and contact us if the problem persists.");
			}
		}
		
		public synchronized void deactivate()
		{
			active = false;
		}
		
		public synchronized void stopProcess(String msg)
		{
			TestResults tr = new TestResults();
			tr.addError(msg);
			try {
				output.writeLong(jobID);
				output.writeBoolean(false);
				tr.sendTestResults(output);
				output.flush();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			System.exit(0);
		}
		
	}
	
	
}

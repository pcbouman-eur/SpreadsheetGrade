import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import nl.eur.spreadsheettest.xml.Exercise;
import nl.eur.spreadsheettest.xml.InputType;
import nl.eur.spreadsheettest.xml.TestcaseType;

public class XMLTest
{
	private final static double eps = 10e-8;
	private final static String report = "%5d";
	private final static int maxFullGrid = 5;
	private final static long seed = 54321;
	
	private int numRandomExtreme = 128;
	private int numRandomSample = 128;
	private Exercise exercise;
	
	private Map<String,Integer> testSucceed;
	private Map<String,Integer> testFailed;
	private Map<String,Integer> totalTest;
	private Map<String,Integer> errors;
	
	public XMLTest()
	{
		testSucceed = new TreeMap<>();
		testFailed = new TreeMap<>();
		totalTest = new TreeMap<>();
		errors = new TreeMap<>();
	}
	
	public static void main(String [] args)
	{
		if (args.length != 3)
		{
			System.out.println("This program requires three arguments: ");
			System.out.println(" Argument 1 : An xml file that contains information on the exercise");
			System.out.println(" Argument 2 : An xlsx file that contains the reference solution to the exercise");
			System.out.println(" Argument 3 : An xlsx file that needs to be checked for correctness");
			System.exit(0);
		}
		
		try ( XSSFWorkbook ref = new XSSFWorkbook(args[1]); XSSFWorkbook handin = new XSSFWorkbook(args[2]) )
		{
			XMLTest test = new XMLTest();
			test.readExercise(new File(args[0]));
			test.testRanges(ref.getSheetAt(0), handin.getSheetAt(0), eps, new Random(seed));
			System.out.println(test.makeReport(true));
		}
		catch (Exception e)
		{
			System.out.println("Something went wrong while trying to run the tests...");
			e.printStackTrace();
		}
	}
	
	public void testRanges(Sheet ref, Sheet test, double eps, Random ran)
	{
		if (exercise == null)
		{
			throw new IllegalStateException("Cannot iterate over tests if no assignment file has been loaded.");
		}
		for (TestcaseType tc : exercise.getTestcases().getTestcase())
		{
			String descr = tc.getDescription().trim().replaceAll("\n", " "); //.replaceAll("\\w+", " ");
			
			List<Range> outputs = tc.getOutput().stream().map(ot -> new Range(ot.getRange())).collect(Collectors.toList());
			ExcelTest et = new ExcelTest(ref, test, eps, outputs);
			
			int cellCount = 0;
			List<InputRangeDouble> irds = new ArrayList<>();
			for (InputType i : tc.getInput())
			{
				String range = i.getRange();
				double lb = i.getLb();
				double ub = i.getUb();
				double precision = i.getPrecision();
				InputRangeDouble ird = new InputRangeDouble(range,lb,ub,precision);
				irds.add(ird);
				cellCount += ird.getCellCount();
			}
			
			if (cellCount <= maxFullGrid)
			{
				String curTest = descr + " (all combinations of extreme values)";
				
				List<Assignment> l = new ArrayList<>();
				for (InputRangeDouble ird : irds)
				{
					l = ird.expandAssignments(l);
				}
				
				for (Assignment a : l)
				{
					try
					{
						reportTest(curTest,et.runTest(a));
					}
					catch (Exception e)
					{
						reportError(e.getClass().getSimpleName()+" during "+curTest);
					}
				}
				
			}
			else
			{
				String curTest = descr + " (random combinations of extreme values)";
				for (int k=0; k < numRandomExtreme; k++)
				{
					Assignment a = null;
					for (InputRangeDouble ird : irds)
					{
						a = ird.expandRandomUBAssignment(ran, a);
					}
					
					try
					{
						reportTest(curTest,et.runTest(a));
					}
					catch (Exception e)
					{
						reportError(e.getClass().getSimpleName()+" during "+curTest);
					}
					
				}
			}
			
			String curTest = descr + " (random values)";
			for (int k=0; k < numRandomSample; k++)
			{
				Assignment a = null;
				for (InputRangeDouble ird : irds)
				{
					a = ird.expandRandomAssignment(ran, a);
					
					try
					{
						reportTest(curTest,et.runTest(a));
					}
					catch (Exception e)
					{
						reportError(e.getClass().getSimpleName()+" during "+curTest);
					}
				}
			}
		}
	}
	
	public String makeReport(boolean json)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Results for exercise "+exercise.getTitle());
		sb.append("\n");
		if (testSucceed.size() > 0)
		{
			sb.append("\n");
			sb.append("****************\n");
			sb.append("* PASSED TESTS *\n");
			sb.append("****************\n");
			for (String s : testSucceed.keySet())
			{
				String success = String.format(report, testSucceed.get(s));
				String total = String.format(report, totalTest.get(s));
				sb.append("[ "+success+" out of "+total+" ] \t: "+s+"\n");
			}
		}
		
		if (testFailed.size() > 0)
		{
			sb.append("\n");
			sb.append("****************\n");
			sb.append("* FAILED TESTS *\n");
			sb.append("****************\n");
			for (String s : testFailed.keySet())
			{
				String failed = String.format(report, testFailed.get(s));
				String total = String.format(report, totalTest.get(s));
				sb.append("[ "+failed+" out of "+total+" ] : "+s+"\n");
			}
		}
		
		if (errors.size() > 0)
		{
			sb.append("\n");
			sb.append("****************\n");
			sb.append("* TEST  ERRORS *\n");
			sb.append("****************\n");
			for (String s : errors.keySet())
			{
				String err = String.format(report, errors.get(s));
				sb.append("[ "+err+" error(s) ] : "+s+"\n");
			}
		}
		
		double score = Math.floor(100*(testSucceed.size()*1d)/(totalTest.size()*1d));
		
		if (json)
		{
			sb.append("\n");
			sb.append("{\"scores\": {\""+exercise.getShorttitle()+"\": "+score+"} }");

		}
		
		return sb.toString();
	}
	
	
	private void readExercise(File f) throws JAXBException
	{
		
		JAXBContext jc = JAXBContext.newInstance( Exercise.class );
		Unmarshaller u = jc.createUnmarshaller();
		exercise = (Exercise) u.unmarshal(f);
	}
	
	private void reportTest(String test, boolean b)
	{
		totalTest.merge(test, 1, (i,j) -> i+j);
		if (b)
		{
			testSucceed.merge(test, 1, (i,j) -> i+j);
		}
		else
		{
			testFailed.merge(test, 1, (i,j) -> i+j);
		}
	}
	
	private void reportError(String error)
	{
		errors.merge(error, 1, (i,j) -> i+j);
	}
}

package nl.eur.ese.spreadsheettest;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.poi.ss.formula.FormulaParser;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.formula.eval.NotImplementedFunctionException;
import org.apache.poi.ss.formula.ptg.AbstractFunctionPtg;
import org.apache.poi.ss.formula.ptg.AttrPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.RefPtgBase;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
//import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFEvaluationWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import nl.eur.spreadsheettest.xml.AbsoluteType;
import nl.eur.spreadsheettest.xml.CompareType;
import nl.eur.spreadsheettest.xml.Exercise;
import nl.eur.spreadsheettest.xml.FunctionConstraint;
import nl.eur.spreadsheettest.xml.InputType;
import nl.eur.spreadsheettest.xml.StyleType;
import nl.eur.spreadsheettest.xml.TestcaseType;

public class XMLTest extends XMLTestBase {

	private XMLTest()
	{
		super();
	}
	
	public XMLTest(File f) throws JAXBException
	{
		this();
		readExercise(f);
	}
	
	public XMLTest(File f, boolean traceErrors) throws JAXBException
	{
		this(f);
		this.traceErrors = traceErrors;
	}
	
	public static void main(String [] args)
	{
		DatabaseFunctions.register();
		ModernFunctions.register();
		
		if (args.length != 3)
		{
			System.out.println("This program requires three arguments: ");
			System.out.println(" Argument 1 : An xml file that contains information on the exercise");
			System.out.println(" Argument 2 : An xlsx file that contains the reference solution to the exercise");
			System.out.println(" Argument 3 : An xlsx file that needs to be checked for correctness");
			System.exit(0);
		}
		
		try (XSSFWorkbook ref = new XSSFWorkbook(new FileInputStream(args[1]));
			 XSSFWorkbook handin = new XSSFWorkbook(new FileInputStream(args[2])) )
		{
			XMLTest test = new XMLTest();
			test.readExercise(new File(args[0]));
			test.testAll(ref, handin);
			System.out.println(test.makeReport(true));
		}
		catch (Exception e)
		{
			System.out.println("Something went wrong while trying to run the tests...");
			e.printStackTrace();
		}
	}

	public String getErrorTrace()
	{
		if (!traceErrors)
		{
			throw new IllegalStateException("This XMLTest does not trace errors");
		}
		return errorTrace.toString();
	}
	
	public TestResults makeReport()
	{
		TestResults tr = new TestResults();
		for (Entry<String,Integer> e : testSucceed.entrySet())
		{
			tr.addPass(e.getKey(),e.getValue());
		}
		for (Entry<String,Integer> e : testFailed.entrySet())
		{
			tr.addFail(e.getKey(), e.getValue());
		}
		for (Entry<String,Integer> e : errors.entrySet())
		{
			tr.addError(e.getKey(), e.getValue());
		}
		for (Entry<String,Integer> e : notImpl.entrySet())
		{
			tr.addError("Unsupported function(s) '"+e.getKey()
				+"' found. Please let your TA check your work. ", e.getValue());
		}
		for (Entry<String,Integer> e : missing.entrySet())
		{
			tr.addError("Unexpected empty cell(s) found: "+e.getKey(), e.getValue());
		}
		return tr;
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
				sb.append("[ "+success+" out of "+total+" ] : "+s+"\n");
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
		
		
		double score = Math.floor(100*(totalTest.size()-testFailed.size()*1d)/(totalTest.size()*1d));
		
		int numErr = errors.size() + notImpl.size() + missing.size();
		
		if (numErr > 0)
		{
			sb.append("\n");
			sb.append("****************\n");
			sb.append("* TEST  ERRORS *\n");
			sb.append("****************\n");
			if (errors.size() > 0)
			{
				for (String s : errors.keySet())
				{
					String err = String.format(report, errors.get(s));
					sb.append("[ "+err+" error(s) ] : "+s+"\n");
				}
				sb.append("\n");
			}
			if (notImpl.size() > 0)
			{
				sb.append("There was a problem during grading, because not all Excel functions\n");
				sb.append("are implemented in the grading system. We ran into these functions:\n");
				for (String s : notImpl.keySet())
				{
					String err = String.format(report, notImpl.get(s));
					sb.append(" ("+err+" times) : "+s);
				}
				sb.append("\n");
			}
			if (missing.size() > 0)
			{
				sb.append("There was a problem during grading, because some cells were empty.\n");
				for (String s : missing.keySet())
				{
					String err = String.format(report, missing.get(s));
					sb.append(" ("+err+" times) : "+s);
				}
				sb.append("\n");
			}
			
			// Force score to zero if there were any errors
			score = 0;
		}
		
		
		
		if (json)
		{
			sb.append("\n");
			sb.append("{\"scores\": {\""+exercise.getShorttitle()+"\": "+score+"} }");

		}
		
		return sb.toString();
	}


	public static enum CompareResult
	{
		OKAY,
		DIFFERENT_VALUES,
		DIFFERENT_TYPES,
		UNSUPPORTED_TYPE
	}
}

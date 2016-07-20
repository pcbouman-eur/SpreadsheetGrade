import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.poi.ss.formula.FormulaParser;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.ptg.AbstractFunctionPtg;
import org.apache.poi.ss.formula.ptg.AttrPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.RefPtgBase;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFEvaluationWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import nl.eur.spreadsheettest.xml.AbsoluteType;
import nl.eur.spreadsheettest.xml.Exercise;
import nl.eur.spreadsheettest.xml.FunctionConstraint;
import nl.eur.spreadsheettest.xml.InputType;
import nl.eur.spreadsheettest.xml.StyleType;
import nl.eur.spreadsheettest.xml.TestcaseType;

public class XMLTest
{
	private String report = "%4d";
	private int maxFullGrid = 5;
	private long seed = 54321;
	
	private Exercise exercise;
	
	private Map<String,Integer> testSucceed;
	private Map<String,Integer> testFailed;
	private Map<String,Integer> totalTest;
	private Map<String,Integer> errors;
	
	private boolean traceErrors;
	private StringBuilder errorTrace;
	
	private XMLTest()
	{
		this.testSucceed = new TreeMap<>();
		this.testFailed = new TreeMap<>();
		this.totalTest = new TreeMap<>();
		this.errors = new TreeMap<>();
		this.traceErrors = false;
		this.errorTrace = new StringBuilder();
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
			test.testAll(ref, handin);
			System.out.println(test.makeReport(true));
		}
		catch (Exception e)
		{
			System.out.println("Something went wrong while trying to run the tests...");
			e.printStackTrace();
		}
	}
	
	public void testAll(XSSFWorkbook ref, XSSFWorkbook test)
	{
		testRanges(ref, test);
		testStyle(test);
	}
	
	public void testStyle(XSSFWorkbook test)
	{
		if (exercise == null)
		{
			throw new IllegalStateException("Cannot iterate over tests if no assignment file has been loaded.");
		}
		
		if (exercise.getStyles() == null)
		{
			return;
		}

		XSSFEvaluationWorkbook ewb = XSSFEvaluationWorkbook.create(test);
		
		for (StyleType style : exercise.getStyles().getStyle())
		{
			// Grab the relevant sheet
			String sn = style.getSheetName();
			Sheet sheet;
			if (sn != null)
			{
				sheet = test.getSheet(sn);
				if (sheet == null)
				{
					reportError("We could not find a sheet with name '"+sn+"' in your solution."
							+ " Make sure you have a sheet with that exact name.");
					continue;
				}
			}
			else
			{
				sheet = test.getSheetAt(0);
			}
			int sheetIndex = test.getSheetIndex(sheet);
			String sheetString = "";
			if (sn != null)
			{
					sheetString = " in sheet '"+sn+"'";
			}
			
			// Setup data structures for checking
			Set<CellReference> absRefs = new HashSet<>();
		
			Set<String> functionsUsed = new HashSet<>();
			
			for (AbsoluteType abs : style.getAbsolute())
			{
				Range r = new Range(abs.getRange());
				for (CellReference cr : r)
				{
					absRefs.add(cr);
				}
			}
			
			for (Row row : sheet)
			{
				for (Cell c : row)
				{
					if (c.getCellType() == Cell.CELL_TYPE_FORMULA)
					{
						Ptg[] tokens = FormulaParser.parse(c.getCellFormula(), ewb, FormulaType.CELL, sheetIndex);
						functionsUsed.addAll(getFunctions(tokens));
						checkReferences(tokens, absRefs, sheetString);
					}
				}
			}
			
			for (FunctionConstraint req : style.getRequired())
			{
				String fname = req.getFunction().trim().toUpperCase();
				reportTest("The function '"+fname+"' is used at least once"+sheetString+"", functionsUsed.contains(fname));
			}
			
			for (FunctionConstraint req : style.getForbidden())
			{
				String fname = req.getFunction().trim().toUpperCase();
				reportTest("The function '"+fname+"' is never used"+sheetString+"", !functionsUsed.contains(fname));
			}
		}
		
		
	}
	
	private void checkReferences(Ptg[] tokens, Set<CellReference> absRefs, String sheetString)
	{
		for (Ptg token : tokens)
		{
			if (token instanceof RefPtgBase)
			{
				RefPtgBase rp = (RefPtgBase) token;
				CellReference cr = new CellReference(rp.getRow(), rp.getColumn());
				if (absRefs.contains(cr))
				{
					boolean test = !rp.isColRelative() && !rp.isRowRelative();
					reportTest("References to cell "+cr.formatAsString()+sheetString+" are absolute", test);
				}
			}
		}
	}

	public void testRanges(XSSFWorkbook ref, XSSFWorkbook test)
	{

		if (exercise == null)
		{
			throw new IllegalStateException("Cannot iterate over tests if no assignment file has been loaded.");
		}
		
		Random ran = new Random(seed);
		report = "%"+exercise.getReportDigits()+"d";
		seed = exercise.getSeed();
		
		
		outerLoop:
		for (TestcaseType tc : exercise.getTestcases().getTestcase())
		{
			Sheet refSheet, testSheet;
			
			if (tc.getSheetName() != null)
			{
				String sn = tc.getSheetName();
				refSheet = ref.getSheet(sn);
				testSheet = test.getSheet(sn);
				if (refSheet == null)
				{
					reportError("There is a problem with the assignment: the sheet with name '"+ sn
							+"' could not be found in the reference solution. Please contact us!");
					continue;
				}
				if (testSheet == null)
				{
					reportError("We could not find a sheet with name '"+sn+"' in your solution."
							+ " Make sure you have a sheet with that exact name.");
					continue;
				}
			}
			else
			{
				refSheet = ref.getSheetAt(0);
				testSheet = test.getSheetAt(0);
			}
			
			
			String descr = tc.getDescription().trim().replaceAll("\n", " "); //.replaceAll("\\w+", " ");
			
			List<Range> outputs = tc.getOutput().stream().map(ot -> new Range(ot.getRange())).collect(Collectors.toList());
			ExcelTest et = new ExcelTest(refSheet, testSheet, tc.getEps(), outputs);
			
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
				if (!ird.isTight())
				{
					cellCount += ird.getCellCount();
				}
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
						if (traceErrors)
						{
							errorTrace.append("While running "+curTest+" the following Exception was raised\n");
							errorTrace.append(getStackTrace(e));
							errorTrace.append("\n");
						}
						continue outerLoop;
					}
				}
				
			}
			else
			{
				String curTest = descr + " (random combinations of extreme values)";
				for (int k=0; k < tc.getRandomCombinations(); k++)
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
						if (traceErrors)
						{
							errorTrace.append("While running "+curTest+" the following Exception was raised\n");
							errorTrace.append(getStackTrace(e));
							errorTrace.append("\n");
						}
						continue outerLoop;
					}
					
				}
			}
			
			String curTest = descr + " (random values)";
			for (int k=0; k < tc.getRandomDraws(); k++)
			{
				Assignment a = null;
				for (InputRangeDouble ird : irds)
				{
					a = ird.expandRandomAssignment(ran, a);
				}
				try
				{
					reportTest(curTest,et.runTest(a));
				}
				catch (Exception e)
				{
					reportError(e.getClass().getSimpleName()+" during "+curTest);
					if (traceErrors)
					{
						errorTrace.append("While running "+curTest+" the following Exception was raised\n");
						errorTrace.append(getStackTrace(e));
						errorTrace.append("\n");
					}
					continue outerLoop;
				}
			}
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
	
	public static Set<String> getFunctions(Ptg[] tokens)
	{
		Set<String> functions = new HashSet<>();
		for (Ptg token : tokens)
		{
			if (token instanceof AttrPtg)
			{
				AttrPtg ap = (AttrPtg) token;
				if (ap.isSum())
				{
					functions.add("SUM");
				}
				if (ap.isOptimizedIf())
				{
					functions.add("IF");
				}
				if (ap.isOptimizedChoose())
				{
					functions.add("CHOOSE");
				}
			}
			if (token instanceof AbstractFunctionPtg)
			{
				AbstractFunctionPtg fvp = (AbstractFunctionPtg) token;
				functions.add(fvp.getName().trim().toUpperCase());
			}
		}
		return functions;
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

package nl.eur.ese.spreadsheettest;
import java.io.ByteArrayOutputStream;
import java.io.File;
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
	private Map<String,Integer> notImpl;
	private Map<String,Integer> missing;
	
	private boolean traceErrors;
	private StringBuilder errorTrace;
	
	private XMLTest()
	{
		this.testSucceed = new TreeMap<>();
		this.testFailed = new TreeMap<>();
		this.totalTest = new TreeMap<>();
		this.errors = new TreeMap<>();
		this.notImpl = new TreeMap<>();
		this.missing = new TreeMap<>();
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
		ModernFunctions.addModernFunctions(ref);
		ModernFunctions.addModernFunctions(test);
		testComparisons(ref, test);
		testStyle(test);
		testRanges(ref, test);
	}
	
	public void testComparisons(XSSFWorkbook ref, XSSFWorkbook test)
	{
		if (exercise.getComparisons() == null || exercise.getComparisons().getComparison() == null)
		{
			return;
		}
		outerLoop:
		for (CompareType comp : exercise.getComparisons().getComparison())
		{
			String descr = comp.getDescription().trim().replaceAll("\n", " ");
			String sn = comp.getSheetName();
			Range range = new Range(comp.getRange().trim().replaceAll("\n", " "));
			Sheet refSheet, testSheet;
			
			if (sn != null)
			{
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
			
			for (CellReference cr : range)
			{
				int row = cr.getRow();
				int col = cr.getCol();
				if (refSheet.getRow(row) == null || refSheet.getRow(row).getCell(col) == null)
				{
					reportError("There is a problem with the assignment: cell "+cr.formatAsString()
							+ " in sheet '"+ refSheet.getSheetName() + "' could not be found in the "
							+ "reference solution. Please contact us!");
					continue outerLoop;
				}
				if (testSheet.getRow(row) == null || testSheet.getRow(row).getCell(col) == null)
				{
					reportError("We could not find cell "+cr.formatAsString()+" within sheet '"+sn
							+"' in your solution. Make sure you follow the assignment and do "
							+" something with this cell.");
					continue outerLoop;
				}
				
				Cell refCell = refSheet.getRow(row).getCell(col);
				Cell testCell = testSheet.getRow(row).getCell(col);
				CompareResult res = compare(refCell, testCell, comp.getEps(), comp.isTextStrict());
				if (res == CompareResult.UNSUPPORTED_TYPE)
				{
					reportError("We could not compare the value in cell "+cr.formatAsString()+" within "
							+ "sheet '"+sn+"' to the reference because it holds an unexpected type of "
							+ "data. Please contact us. ");
					continue outerLoop;
				}
				if (res == CompareResult.DIFFERENT_TYPES)
				{
					if (refCell.getCellType() == CellType.FORMULA)
					{
						reportTest("The cell "+cr.formatAsString()+" within sheet '"+sn+"' of your solution "+
								"should be computed using a formula, but it holds static data. Make sure you "+
								"use a formula where appropiate.", false);						
					}
					else
					{
						reportTest("The cell "+cr.formatAsString()+" within sheet '"+sn+"' of your solution "+
									"holds the wrong type of data. Check the assignment to see what type of "+
								    "should be stored in the cell. ", false);
					}
					continue outerLoop;
				}
				reportTest(descr, res == CompareResult.OKAY);
			}
		}
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
			Map<Integer,String> colRefs = new HashMap<>();
			Set<Integer> rowRefs = new HashSet<>();
			
		
			Set<String> functionsUsed = new HashSet<>();
			
			for (AbsoluteType abs : style.getAbsolute())
			{
				if (abs.getRange() != null)
				{
					Range r = new Range(abs.getRange());
					for (CellReference cr : r)
					{
						absRefs.add(cr);
					}
				}
				if (abs.getCol() != null)
				{
					String colName = abs.getCol().trim().toUpperCase();
					int col = new CellReference(colName+"1").getCol();
					colRefs.put(col, colName);
				}
				if (abs.getRow() != null)
				{
					int row = abs.getRow();
					rowRefs.add(row);
				}
			}
			
			for (Row row : sheet)
			{
				for (Cell c : row)
				{
					//if (c.getCellTypeEnum() == CellType.FORMULA)
					if (c.getCellType() == CellType.FORMULA)
					{
						Ptg[] tokens = FormulaParser.parse(c.getCellFormula(), ewb, FormulaType.CELL, sheetIndex);
						functionsUsed.addAll(getFunctions(tokens));
						checkReferences(tokens, absRefs, colRefs, rowRefs, sheetString);
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
	
	private void checkReferences(Ptg[] tokens, Set<CellReference> absRefs, Map<Integer,String> colRefs, Set<Integer> rowRefs, String sheetString)
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
				if (colRefs.containsKey(rp.getColumn()))
				{
					String colName = colRefs.get(rp.getColumn());
					boolean test = !rp.isColRelative();
					reportTest("References to column "+colName+sheetString+" are absolute", test);
				}
				if (rowRefs.contains(rp.getRow()))
				{
					boolean test = !rp.isRowRelative();
					reportTest("References to row "+rp.getRow()+" are absolute", test);
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
		
		if (exercise.getTestcases() == null)
		{
			return;
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
			ExcelTest et = new ExcelTest(refSheet, testSheet, tc.getEps(), outputs, tc.isTextStrict());
			
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
					catch (NotImplementedFunctionException nie)
					{
						notImpl.merge("Usage of function '"+nie.getFunctionName()+"'", 1, (i,j) -> i+j);
					}
					catch (NotImplementedException nie)
					{
						if (nie.getCause() instanceof NotImplementedFunctionException)
						{
							NotImplementedFunctionException nife = (NotImplementedFunctionException) nie.getCause();
							String msg = "Usage of unknown function '"+nife.getFunctionName()+"'";
							msg += " caused by "+nie.getMessage();
							notImpl.merge(msg, 1, (i,j) -> i+j);						
						}
						else
						{
							notImpl.merge(nie.getMessage(), 1, (i,j) -> i+j);
						}
					}
					catch (NoCellException nce)
					{
						missing.merge(nce.getMessage(), 1, (i,j) -> i+j);
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
					catch (NotImplementedFunctionException nie)
					{
						notImpl.merge("Usage of function '"+nie.getFunctionName()+"'", 1, (i,j) -> i+j);
					}
					catch (NotImplementedException nie)
					{
						if (nie.getCause() instanceof NotImplementedFunctionException)
						{
							NotImplementedFunctionException nife = (NotImplementedFunctionException) nie.getCause();
							String msg = "Usage of unknown function '"+nife.getFunctionName()+"'";
							msg += " caused by "+nie.getMessage();
							notImpl.merge(msg, 1, (i,j) -> i+j);						
						}
						else
						{
							notImpl.merge(nie.getMessage(), 1, (i,j) -> i+j);
						}
					}
					catch (NoCellException nce)
					{
						missing.merge(nce.getMessage(), 1, (i,j) -> i+j);
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
				catch (NotImplementedFunctionException nie)
				{
					notImpl.merge("Usage of function '"+nie.getFunctionName()+"'", 1, (i,j) -> i+j);
				}
				catch (NotImplementedException nie)
				{
					if (nie.getCause() instanceof NotImplementedFunctionException)
					{
						NotImplementedFunctionException nife = (NotImplementedFunctionException) nie.getCause();
						String msg = "Usage of unknown function '"+nife.getFunctionName()+"'";
						msg += " caused by "+nie.getMessage();
						notImpl.merge(msg, 1, (i,j) -> i+j);						
					}
					else
					{
						notImpl.merge(nie.getMessage(), 1, (i,j) -> i+j);
					}
				}
				catch (NoCellException nce)
				{
					missing.merge(nce.getMessage(), 1, (i,j) -> i+j);
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
	
	public static CompareResult compare(Cell ref, Cell test, double eps, boolean textStrict)
	{
		if (ref.getCellType() != test.getCellType())
		{
			CellType ct = ref.getCellType();
			CellType ct2 = test.getCellType();
			return CompareResult.DIFFERENT_TYPES;
		}
		CellType ct = ref.getCellType();
		if (ct == CellType.FORMULA)
		{
			if (ref.getCachedFormulaResultType() != test.getCachedFormulaResultType())
			{
				return CompareResult.DIFFERENT_TYPES;
			}
			ct = ref.getCachedFormulaResultType();
		}
		
		if (ct == CellType.STRING)
		{
			String t1 = ref.getStringCellValue();
			String t2 = test.getStringCellValue();
			if (textStrict)
			{
				return t1.equals(t2) ? CompareResult.OKAY : CompareResult.DIFFERENT_VALUES;
			}
			else
			{
				return t1.trim().toLowerCase().equals(t2.trim().toLowerCase()) ?
						CompareResult.OKAY : CompareResult.DIFFERENT_VALUES;
			}
		}
		if (ct == CellType.NUMERIC)
		{
			double d1 = ref.getNumericCellValue();
			double d2 = test.getNumericCellValue();
			if (Math.abs(d1 - d2) <= eps)
			{
				return CompareResult.OKAY;
			}
			else
			{
				return CompareResult.DIFFERENT_VALUES;
			}
		}
		if (ct == CellType.BOOLEAN)
		{
			if (ref.getBooleanCellValue() == test.getBooleanCellValue())
			{
				return CompareResult.OKAY;
			}
			return CompareResult.DIFFERENT_VALUES;
		}
		if (ct == CellType.BLANK)
		{
			return CompareResult.OKAY;
		}
		
		return CompareResult.UNSUPPORTED_TYPE;
	}
	
	public static enum CompareResult
	{
		OKAY,
		DIFFERENT_VALUES,
		DIFFERENT_TYPES,
		UNSUPPORTED_TYPE
	}
}

package nl.eur.ese.spreadsheettest;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;

public class ExcelTest
{
	private final static boolean localEval = true;
	
	private double eps;
	
	private Sheet reference;
	private Sheet test;
	
	private List<Range> outputs;
	private boolean strict;
	
	public ExcelTest(Sheet ref, Sheet test, double eps, List<Range> output)
	{
		this(ref, test, eps, output, true);
	}
	
	public ExcelTest(Sheet ref, Sheet test, double eps, List<Range> output, boolean strict)
	{
		this.reference = ref;
		this.test = test;
		
		this.eps = eps;
		this.outputs = new ArrayList<>(output);
		this.strict = strict;
	}
	
	public boolean runTest(Assignment a)
	{
		if (a != null)
		{
			a.assign(reference);
			a.assign(test);
		}
		return testAll();
	}
	
	private boolean testAll()
	{
		FormulaEvaluator refEval = reference.getWorkbook().getCreationHelper().createFormulaEvaluator();
		FormulaEvaluator testEval = test.getWorkbook().getCreationHelper().createFormulaEvaluator();
		
		if (!localEval)
		{
			refEval.evaluateAll();
			testEval.evaluateAll();
		}
			
		for (Range or : outputs)
		{
			for (CellReference cr : or)
			{
				if (localEval)
				{
					Row refRow = reference.getRow(cr.getRow());
					if (refRow == null)
					{
						throw new NoCellException(cr, true);
					}
					Cell refCell = refRow.getCell(cr.getCol());
					if (refCell == null)
					{
						throw new NoCellException(cr, true);
					}
					refEval.evaluate(refCell);
					Row testRow = test.getRow(cr.getRow());
					if (testRow == null)
					{
						throw new NoCellException(cr, false);
					}
					Cell testCell = testRow.getCell(cr.getCol());
					if (testCell == null)
					{
						throw new NoCellException(cr, false);
					}
					testEval.evaluate(testCell);
				}
				
				//System.out.println("comparing.. "+cr);
				if (!compare(cr))
				{
					return false;
				}
			}
		}
		return true;
	}
	
	private CellValue getRefValue(FormulaEvaluator fe, CellReference cellRef, Sheet sh)
	{
		Row row = sh.getRow(cellRef.getRow());
		if (row == null)
		{
			throw new NoCellException(cellRef, row == null);
		}
		Cell cell = sh.getRow(cellRef.getRow()).getCell(cellRef.getCol());
		if (cell == null)
		{
			throw new NoCellException(cellRef, cell == null);
		}
		
		CellType type = cell.getCellType();
		
		if (type == CellType.BOOLEAN)
		{
			return CellValue.valueOf(cell.getBooleanCellValue());
		}
		else if (type == CellType.NUMERIC)
		{
			return new CellValue(cell.getNumericCellValue());
		}
		else if (type == CellType.STRING)
		{
			return new CellValue(cell.getStringCellValue());
		}
		else if (type == CellType.ERROR)
		{
			return CellValue.getError(cell.getErrorCellValue());
		}
		else if (type == CellType.FORMULA)
		{
			// compare evaluated values!!
			CellValue val = fe.evaluate(cell);
			
			CellType valType = val.getCellType();
			
			if (valType == CellType.BOOLEAN)
			{
				return CellValue.valueOf(val.getBooleanValue());
			}
			else if (valType == CellType.NUMERIC)
			{
				return new CellValue(val.getNumberValue());
			}
			else if (valType == CellType.STRING)
			{
				return new CellValue(val.getStringValue());
			}
			else if (valType == CellType.ERROR)
			{
				return CellValue.getError(val.getErrorValue());
			}
		}
		return null;
	}
	
	
	
	private boolean compare(CellReference cell)
	{
		FormulaEvaluator refEval = reference.getWorkbook().getCreationHelper().createFormulaEvaluator();
		FormulaEvaluator testEval = test.getWorkbook().getCreationHelper().createFormulaEvaluator();

		
		CellValue refVal = getRefValue(refEval, cell, reference);
		CellValue testVal = getRefValue(testEval, cell, test);
		
		if ( refVal == null || testVal == null)
		{
			return refVal == testVal;
		}
		
		CellType refType = refVal.getCellType();
		CellType testType = testVal.getCellType();
		
		if (refType != testType)
		{
			return false;
		}
		

		if (refType == CellType.BOOLEAN)
		{
			return refVal.getBooleanValue() == testVal.getBooleanValue();
		}
		else if (refType == CellType.NUMERIC)
		{
			return Math.abs(refVal.getNumberValue() - testVal.getNumberValue()) <= eps;
		}
		else if (refType == CellType.STRING)
		{
			if (strict)
			{
				return refVal.getStringValue().equals(testVal.getStringValue());
			}
			else
			{
				return refVal.getStringValue().trim().toLowerCase().equals(
							testVal.getStringValue().trim().toLowerCase()
						);
			}
		}
		return false;
	}
	
	private void copyData(CellReference cell)
	{
		Cell refCell = reference.getRow(cell.getRow()).getCell(cell.getCol());
		Cell testCell = test.getRow(cell.getRow()).getCell(cell.getCol());
		
		CellType type = refCell.getCellType();
		
		if (type == CellType.BLANK)
		{
			testCell.setCellType(CellType.BLANK);
		}
		else if (type == CellType.BOOLEAN)
		{
			testCell.setCellType(CellType.BOOLEAN);
			testCell.setCellValue(refCell.getBooleanCellValue());
		}
		else if (type == CellType.ERROR)
		{
			testCell.setCellType(CellType.ERROR);
			testCell.setCellErrorValue(refCell.getErrorCellValue());
		}
		else if (type == CellType.FORMULA)
		{
			testCell.setCellType(CellType.FORMULA);
			testCell.setCellFormula(refCell.getCellFormula());
		}
		else if (type == CellType.NUMERIC)
		{
			testCell.setCellType(CellType.NUMERIC);
			testCell.setCellValue(refCell.getNumericCellValue());
		}
		else if (type == CellType.STRING)
		{
			testCell.setCellType(CellType.STRING);
			testCell.setCellValue(refCell.getStringCellValue());
		}
	}
	
	
}

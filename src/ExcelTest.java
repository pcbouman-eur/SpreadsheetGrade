import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;

public class ExcelTest
{
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
		else
		{
			//
		}
		return testAll();
	}
	
	private boolean testAll()
	{
		FormulaEvaluator refEval = reference.getWorkbook().getCreationHelper().createFormulaEvaluator();
		FormulaEvaluator testEval = test.getWorkbook().getCreationHelper().createFormulaEvaluator();
		
		refEval.evaluateAll();
		testEval.evaluateAll();
		
		for (Range or : outputs)
		{
			for (CellReference cr : or)
			{
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
		
		int type = cell.getCellType();
		
		if (type == Cell.CELL_TYPE_BOOLEAN)
		{
			return CellValue.valueOf(cell.getBooleanCellValue());
		}
		else if (type == Cell.CELL_TYPE_NUMERIC)
		{
			return new CellValue(cell.getNumericCellValue());
		}
		else if (type == Cell.CELL_TYPE_STRING)
		{
			return new CellValue(cell.getStringCellValue());
		}
		else if (type == Cell.CELL_TYPE_ERROR)
		{
			return CellValue.getError(cell.getErrorCellValue());
		}
		else if (type == Cell.CELL_TYPE_FORMULA)
		{
			// compare evaluated values!!
			CellValue val = fe.evaluate(cell);
			
			int valType = val.getCellType();
			
			if (valType == Cell.CELL_TYPE_BOOLEAN)
			{
				return CellValue.valueOf(val.getBooleanValue());
			}
			else if (valType == Cell.CELL_TYPE_NUMERIC)
			{
				return new CellValue(val.getNumberValue());
			}
			else if (valType == Cell.CELL_TYPE_STRING)
			{
				return new CellValue(val.getStringValue());
			}
			else if (valType == Cell.CELL_TYPE_ERROR)
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
		
		int refType = refVal.getCellType();
		int testType = testVal.getCellType();
		
		if (refType != testType)
		{
			return false;
		}
		

		if (refType == Cell.CELL_TYPE_BOOLEAN)
		{
			return refVal.getBooleanValue() == testVal.getBooleanValue();
		}
		else if (refType == Cell.CELL_TYPE_NUMERIC)
		{
			return Math.abs(refVal.getNumberValue() - testVal.getNumberValue()) <= eps;
		}
		else if (refType == Cell.CELL_TYPE_STRING)
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
		
		int type = refCell.getCellType();
		
		if (type == Cell.CELL_TYPE_BLANK)
		{
			testCell.setCellType(Cell.CELL_TYPE_BLANK);
		}
		else if (type == Cell.CELL_TYPE_BOOLEAN)
		{
			testCell.setCellType(Cell.CELL_TYPE_BOOLEAN);
			testCell.setCellValue(refCell.getBooleanCellValue());
		}
		else if (type == Cell.CELL_TYPE_ERROR)
		{
			testCell.setCellType(Cell.CELL_TYPE_ERROR);
			testCell.setCellErrorValue(refCell.getErrorCellValue());
		}
		else if (type == Cell.CELL_TYPE_FORMULA)
		{
			testCell.setCellType(Cell.CELL_TYPE_FORMULA);
			testCell.setCellFormula(refCell.getCellFormula());
		}
		else if (type == Cell.CELL_TYPE_NUMERIC)
		{
			testCell.setCellType(Cell.CELL_TYPE_NUMERIC);
			testCell.setCellValue(refCell.getNumericCellValue());
		}
		else if (type == Cell.CELL_TYPE_STRING)
		{
			testCell.setCellType(Cell.CELL_TYPE_STRING);
			testCell.setCellValue(refCell.getStringCellValue());
		}
	}
	
	
}

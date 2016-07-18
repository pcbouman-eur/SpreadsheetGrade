import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;

public class ExcelTest
{
	private double eps;
	
	private Sheet reference;
	private Sheet test;
	
	private List<Range> outputs;
	
	public ExcelTest(Sheet ref, Sheet test, double eps, List<Range> output)
	{
		this.reference = ref;
		this.test = test;
		
		this.eps = eps;
		this.outputs = new ArrayList<>(output);
	}
	
	public boolean runTest(Assignment a)
	{
		a.assign(reference);
		a.assign(test);
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
	
	private boolean compare(CellReference cell)
	{
		FormulaEvaluator refEval = reference.getWorkbook().getCreationHelper().createFormulaEvaluator();
		FormulaEvaluator testEval = test.getWorkbook().getCreationHelper().createFormulaEvaluator();
		
		Cell refCell = reference.getRow(cell.getRow()).getCell(cell.getCol());
		Cell testCell = test.getRow(cell.getRow()).getCell(cell.getCol());
		
		int type = refCell.getCellType();
		int testType = testCell.getCellType();
		
		if (testType != type)
		{
			return false;
		}
		
		else if (type == Cell.CELL_TYPE_BOOLEAN)
		{
			return refCell.getBooleanCellValue() == testCell.getBooleanCellValue();
		}
		else if (type == Cell.CELL_TYPE_FORMULA)
		{
			// compare evaluated values!!
			CellValue refVal = refEval.evaluate(refCell);
			CellValue testVal = testEval.evaluate(testCell);
			
			if (refVal.getCellType() != testVal.getCellType())
			{
				return false;
			}
			
			int valType = refVal.getCellType();
			
			if (valType == Cell.CELL_TYPE_BOOLEAN)
			{
				return refVal.getBooleanValue() == testVal.getBooleanValue();
			}
			else if (valType == Cell.CELL_TYPE_NUMERIC)
			{
				//System.out.println(""+refVal.getNumberValue()+"  vs "+testVal.getNumberValue());
				return Math.abs(refVal.getNumberValue() - testVal.getNumberValue()) <= eps;
			}
			else if (valType == Cell.CELL_TYPE_STRING)
			{
				return refVal.getStringValue().equals(testVal.getStringValue());
			}
			else if (valType == Cell.CELL_TYPE_BLANK)
			{
				return true;
			}
			else if (valType == Cell.CELL_TYPE_ERROR)
			{
				return refVal.getErrorValue() == testVal.getErrorValue();
			}
			
		}
		else if (type == Cell.CELL_TYPE_NUMERIC)
		{
			return Math.abs(refCell.getNumericCellValue() - testCell.getNumericCellValue()) <= eps;
		}
		else if (type == Cell.CELL_TYPE_STRING)
		{
			return refCell.getStringCellValue().equals(testCell.getStringCellValue());
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

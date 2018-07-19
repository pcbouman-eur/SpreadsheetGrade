import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.formula.FormulaParser;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.eval.FunctionEval;
import org.apache.poi.ss.formula.ptg.AbstractFunctionPtg;
import org.apache.poi.ss.formula.ptg.AttrPtg;
import org.apache.poi.ss.formula.ptg.NamePtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.RefPtg;
import org.apache.poi.ss.formula.ptg.RefPtgBase;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFEvaluationWorkbook;
import org.apache.poi.xssf.usermodel.XSSFName;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class NamedRefTest
{

	public static void main(String [] args) throws IOException
	{
		
		DatabaseFunctions.register();
		
		System.out.println(FunctionEval.getSupportedFunctionNames());
		System.out.println(FunctionEval.getNotSupportedFunctionNames());
		
		try (XSSFWorkbook ref = new XSSFWorkbook("namedref.xlsx"))
		{
			CellReference cr = new CellReference(3,2,true,false);
			CellReference cr2 = new CellReference(3,2,true,false);
			
			XSSFEvaluationWorkbook ewb = XSSFEvaluationWorkbook.create(ref);
			
			Sheet s = ref.getSheetAt(0);
			
			for (int k=0; k < ref.getNumberOfNames(); k++)
			{
				XSSFName name = ref.getNameAt(k);
				System.out.println(name.getNameName() +" -> "+name.getRefersToFormula());
			}
			
			for (Row r : s)
			{
				for (Cell c : r)
				{
					if (c.getCellType() == Cell.CELL_TYPE_FORMULA)
					{
						String formula = c.getCellFormula();
						Ptg[] tokens = FormulaParser.parse(formula, ewb, FormulaType.CELL, ref.getSheetIndex(s));
						for (Ptg token : tokens)
						{
							System.out.println(token);
							if (token instanceof NamePtg)
							{
								NamePtg name = (NamePtg) token;
								// This is a name pointer; should be fine.
							}
							if (token instanceof RefPtgBase)
							{
								RefPtgBase rp = (RefPtgBase) token;
								System.out.println("Col relative : "+rp.isColRelative());
								System.out.println("Row relative : "+rp.isRowRelative());
							}
							if (token instanceof AttrPtg)
							{
								AttrPtg ap = (AttrPtg) token;
								System.out.println(ap.isSum());
							}
							if (token instanceof AbstractFunctionPtg)
							{
								AbstractFunctionPtg fvp = (AbstractFunctionPtg) token;
								System.out.println(fvp.getName());
							}
						}
					}
				}
			}
		}
	}
	

	
	public static boolean containsFunctionCall(Ptg[] tokens, Collection<String> fnames)
	{
		Set<String> fns = fnames.stream().map(s -> s.trim().toUpperCase()).collect(Collectors.toSet());
		boolean checkSum = fns.contains("SUM");
		boolean checkIf = fns.contains("IF"); 
		boolean checkChoose = fns.contains("CHOOSE");
		
		for (Ptg token : tokens)
		{
			if (token instanceof AttrPtg)
			{
				AttrPtg ap = (AttrPtg) token;
				if ((ap.isSum() && checkSum) || (ap.isOptimizedIf() && checkIf) || (ap.isOptimizedChoose() && checkChoose))
				{
					return true;
				}
			}
			if (token instanceof AbstractFunctionPtg)
			{
				AbstractFunctionPtg fvp = (AbstractFunctionPtg) token;
				if (fns.contains(fvp.getName().trim().toUpperCase()))
				{
					return true;
				}
			}
		}
		return false;
	}
}

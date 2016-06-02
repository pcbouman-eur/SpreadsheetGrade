import java.io.IOException;
import java.util.Collections;
import java.util.Random;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class Main
{
	public static void main(String [] args) throws IOException
	{
		try ( XSSFWorkbook input = new XSSFWorkbook("input.xlsx"); XSSFWorkbook output = new XSSFWorkbook("output.xlsx") )
		{
		
			InputRangeDouble ird = new InputRangeDouble("A1",0,100);
			Range outputRange = new Range("B1");
			
			ExcelTest et = new ExcelTest(input.getSheetAt(0), output.getSheetAt(0), 10e-12, Collections.singletonList(outputRange));
			
			for (Assignment a : ird.getAssignments())
			{
				boolean res = et.runTest(a);
				System.out.println(a +" -> "+res);
				if (!res)
				{
					System.out.println("ERROR ERROR");;
					break;
				}
			}
			
			Random r = new Random(12345);
			
			for (int k=0; k < 10; k++)
			{
				Assignment a = ird.getRandomAssignment(r);
			
				boolean res = et.runTest(a);
				System.out.println(a +" -> "+res);
				if (!res)
				{
					System.out.println("ERROR ERROR");;
					break;
				}
			
			}
		}
	}
}

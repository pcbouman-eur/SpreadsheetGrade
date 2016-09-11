import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.functions.Function;
import org.apache.poi.ss.formula.functions.Rank;
import org.apache.poi.ss.formula.udf.DefaultUDFFinder;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * The purpose of this class is to easily add a bunch of functions to Excel
 * workbooks that are not recognized or implemented by POI.
 * It uses some reflective magic for that, but only once.
 * 
 * @author Paul Bouman
 */

public final class ModernFunctions
{
	@ExcelFunction("_xlfn.RANK.EQ")
	private static FreeRefFunction RANK_EQ = convert(new Rank());

	
	public static void addModernFunctions(Workbook wb)
	{
		if (toolpack == null)
		{
			toolpack = buildUDFFinder();
		}
		wb.addToolPack(toolpack);
			
	}
	
	private static DefaultUDFFinder toolpack;
	
	private static FreeRefFunction convert(Function f)
	{
		return (args, ec) -> f.evaluate(args, ec.getRowIndex(), ec.getColumnIndex());
	}
	
	private static DefaultUDFFinder buildUDFFinder()
	{
		List<String> names = new ArrayList<>();
		List<FreeRefFunction> funcs = new ArrayList<>();
		for (Method m : ModernFunctions.class.getDeclaredMethods())
		{
			if (m.isAnnotationPresent(ExcelFunction.class))
			{
				if (m.getParameterTypes().length == 0 &&
					m.getReturnType().isAssignableFrom(FreeRefFunction.class) &&
				    Modifier.isStatic(m.getModifiers()))
				{
					try
					{
						names.add(m.getAnnotation(ExcelFunction.class).value());
						Object o = m.invoke(null, new Object [0]);
						FreeRefFunction fun = (FreeRefFunction) o;
						funcs.add(fun);
					}
					catch (Exception ex)
					{
						String msg = "An exception occured while adding the annotated method "
								   + m +"\n. The original exception was\n"+ex.getMessage();
						throw new IllegalStateException(msg);
					}
					
				}
			}
		}
		for (Field f : ModernFunctions.class.getDeclaredFields())
		{
			if (f.isAnnotationPresent(ExcelFunction.class))
			{
				if (f.getType().isAssignableFrom(FreeRefFunction.class) &&
					Modifier.isStatic(f.getModifiers()))
				{
					try
					{
						names.add(f.getAnnotation(ExcelFunction.class).value());
						Object o = f.get(null);
						FreeRefFunction fun = (FreeRefFunction) o;
						funcs.add(fun);					
					}
					catch (Exception ex)
					{
						String msg = "An exception occured while adding the annotated field "
								   + f +"\n. The original exception was\n"+ex.getMessage();
						throw new IllegalStateException(msg);
					}
				}
			}
		}
		String [] nameArray = names.toArray(new String[names.size()]);
		FreeRefFunction [] funArray = funcs.toArray(new FreeRefFunction[funcs.size()]);
		
		return new DefaultUDFFinder(nameArray, funArray); 
	}

	@Retention(RetentionPolicy.RUNTIME)
	private @interface ExcelFunction
	{
		String value();
	}
}

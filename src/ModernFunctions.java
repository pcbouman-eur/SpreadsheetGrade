import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;

import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.NumericValueEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.Countif;
import org.apache.poi.ss.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.functions.Function;
import org.apache.poi.ss.formula.functions.IPMT;
import org.apache.poi.ss.formula.functions.Rank;
import org.apache.poi.ss.formula.functions.Sumif;
import org.apache.poi.ss.formula.udf.DefaultUDFFinder;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * The purpose of this class is to easily add a bunch of functions to Excel
 * workbooks that are not recognized or implemented by POI.
 * It uses some reflective magic for that.
 * 
 * @author Paul Bouman
 */

public final class ModernFunctions
{
	@ExcelFunction("CUMIPMT")
	private static FreeRefFunction CUMIPMT = getCumIPMT();

	@ExcelFunction("_xlfn.RANK.EQ")
	private static FreeRefFunction RANK_EQ = convert(new Rank());

	@ExcelFunction("AVERAGEIF")
	private static FreeRefFunction AVERAGEIF = convert(combine(
									new Sumif(),
									new Countif(),
									(v1, v2) -> v1 instanceof NumericValueEval &&
									            v2 instanceof NumericValueEval
									          ? ( value(v2) != 0
									            ? new NumberEval(value(v1)/value(v2))
									            : ErrorEval.DIV_ZERO)
									          : ErrorEval.VALUE_INVALID));
	
	
	private static FreeRefFunction getCumIPMT()
	{
		final IPMT ipmt = new IPMT();
		
		return (args, ec) -> 
		{
			if (args.length != 6)
			{
				return ErrorEval.NA;
			}
			for (ValueEval ve : args)
			{
				if (!(ve instanceof NumericValueEval))
				{
					return ErrorEval.VALUE_INVALID;
				}
			}
			NumericValueEval [] nves = new NumericValueEval[args.length];
			for (int t=0; t < args.length; t++)
			{
				nves[t] = (NumericValueEval) args[t];
			}
			
			double rate = nves[0].getNumberValue();
			double nper = nves[1].getNumberValue();
			double pv = nves[2].getNumberValue();
			int start_per = (int)Math.floor(nves[3].getNumberValue());
			int end_per  = (int)Math.floor(nves[4].getNumberValue());
			double type = nves[5].getNumberValue();
			
			if (rate <= 0 || nper <= 0 || pv <= 0)
			{
				return ErrorEval.NUM_ERROR;
			}
			if (start_per < 1 || end_per < 1 || start_per > end_per)
			{
				return ErrorEval.NUM_ERROR;
			}
			if (type != 0 && type != 1)
			{
				return ErrorEval.NUM_ERROR;
			}
			
			ValueEval [] newArgs = new ValueEval[6];
			newArgs[0] = nves[0];
			newArgs[2] = nves[1];
			newArgs[3] = nves[2];
			newArgs[4] = new NumberEval(0);
			newArgs[5] = nves[5];
			
			double sum = 0;
			for (int t=start_per; t <= end_per; t++)
			{
				newArgs[1] = new NumberEval(t);
				try
				{
					sum += ipmt.eval(newArgs, ec.getRowIndex(), ec.getColumnIndex());
				}
				catch (EvaluationException ee)
				{
					//TODO: return error type?
				}
			}
			
			return new NumberEval(sum);
		};
	}
	
	public static void addModernFunctions(Workbook wb)
	{
		if (toolpack == null)
		{
			toolpack = buildUDFFinder();
		}
		wb.addToolPack(toolpack);
			
	}
	
	private static DefaultUDFFinder toolpack;
	
	private static Double value(ValueEval ve)
	{
		if (!(ve instanceof NumericValueEval))
		{
			throw new IllegalArgumentException("Argument must be of type NumericValueEval");
		}
		return ((NumericValueEval)ve).getNumberValue();
	}
	
	private static Function combine(Function f1, Function f2, BinaryOperator<ValueEval> op)
	{
		
		return (arg0, arg1, arg2) -> op.apply(
					f1.evaluate(arg0, arg1, arg2),
					f2.evaluate(arg0, arg1, arg2)
				);
	}
	
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
						System.out.println("Adding function "+names.get(names.size()-1));
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
						System.out.println("Adding function "+names.get(names.size()-1));
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

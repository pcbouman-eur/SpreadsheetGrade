import org.apache.poi.ss.formula.eval.BlankEval;
import org.apache.poi.ss.formula.eval.FunctionEval;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.NumericValueEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.IDStarAlgorithm;

public class DatabaseFunctions
{
	// DMAX, DSUM, DPRODUCT, DSTDEV, DVAR, DVARP 
	
	public static void register()
	{
		FunctionEval.registerFunction("DMAX", new DStarRunner(() -> new DMax()));
		FunctionEval.registerFunction("DSUM", new DStarRunner(() -> new DSum()));
		FunctionEval.registerFunction("DPRODUCT", new DStarRunner(() -> new DProduct()));
		FunctionEval.registerFunction("DCOUNT", new DStarRunner(() -> new DCount(false)));
		FunctionEval.registerFunction("DCOUNTA", new DStarRunner(() -> new DCount(true)));
		FunctionEval.registerFunction("DAVERAGE", new DStarRunner(() -> new DAverage()));
	}
	
	public static final class DMax implements IDStarAlgorithm
	{
	    private ValueEval maxValue;
	    @Override
	    public boolean processMatch(ValueEval eval) {
	        if(eval instanceof NumericValueEval) {
	            if(maxValue == null) { // First match, just set the value.
	                maxValue = eval;
	            } else { // There was a previous match, find the new minimum.
	                double currentValue = ((NumericValueEval)eval).getNumberValue();
	                double oldValue = ((NumericValueEval)maxValue).getNumberValue();
	                if(currentValue > oldValue) {
	                    maxValue = eval;
	                }
	            }
	        }

	        return true;
	    }

	    @Override
	    public ValueEval getResult() {
	        if(maxValue == null) {
	            return NumberEval.ZERO;
	        } else {
	            return maxValue;
	        }
	    }
	} 
	
	public static final class DSum implements IDStarAlgorithm
	{
	    private double sum = 0;

	    @Override
	    public boolean processMatch(ValueEval eval) {
	        if(eval instanceof NumericValueEval) {
	                double currentValue = ((NumericValueEval)eval).getNumberValue();
	                sum += currentValue;
	        }

	        return true;
	    }

	    @Override
	    public ValueEval getResult() {
	            return new NumberEval(sum);
	    }
	} 
	
	public static final class DProduct implements IDStarAlgorithm
	{
		// Excel defines the product of an empty list as 0
	    private Double prod;

	    @Override
	    public boolean processMatch(ValueEval eval) {
	        if(eval instanceof NumericValueEval) {
	                double currentValue = ((NumericValueEval)eval).getNumberValue();
	                if (prod == null)
	                {
	                	prod = currentValue;
	                }
	                else
	                {
	                	prod *= currentValue;
	                }
	        }

	        return true;
	    }

	    @Override
	    public ValueEval getResult() {
	        if (prod == null)
	        {
	        	return NumberEval.ZERO;
	        }
	    	return new NumberEval(prod);
	    }
	}
	
	public static final class DCount implements IDStarAlgorithm
	{
		// Excel defines the product of an empty list as 0
	    private int count;
	    private boolean countAll;
	    
	    public DCount(boolean a)
	    {
	    	countAll = true;
	    	count = 0;
	    }

	    @Override
	    public boolean processMatch(ValueEval eval) {
	    	
	    	if (countAll && !(eval instanceof BlankEval))
	    	{
	    		count++;
	    	}
	    	else if (eval instanceof NumericValueEval)
	    	{
	    		count++;
	    	}
	    	
	        return true;
	    }

	    @Override
	    public ValueEval getResult() {
	       return new NumberEval(count);
	    }
	}
	
	public static final class DAverage implements IDStarAlgorithm
	{
		// Excel defines the product of an empty list as 0
	    private int count;
	    private double sum;
	    
	    public DAverage()
	    {
	    	count = 0;
	    }

	    @Override
	    public boolean processMatch(ValueEval eval) {
	    	
	    	if (eval instanceof NumericValueEval)
	    	{
	    		NumericValueEval number = (NumericValueEval) eval;
	    		count++;
	    		sum += number.getNumberValue();
	    	}
	    	
	        return true;
	    }

	    @Override
	    public ValueEval getResult() {
	       return new NumberEval(sum/count);
	    }
	}
	
	
}

import org.apache.poi.ss.formula.atp.AnalysisToolPak;
import org.apache.poi.ss.formula.eval.FunctionEval;

public class Bla
{
	public static void main(String [] args)
	{
		DatabaseFunctions.register();
		ModernFunctions.register();
		
		System.out.println("Supported regular functions");
		System.out.println(FunctionEval.getSupportedFunctionNames());
		System.out.println("Supported ATP functions");
		System.out.println(AnalysisToolPak.getSupportedFunctionNames());
		System.out.println();
		System.out.println("Unsupported regular functions");
		System.out.println(FunctionEval.getNotSupportedFunctionNames());
		System.out.println("Unsupported ATP functions");
		System.out.println(AnalysisToolPak.getNotSupportedFunctionNames());
		System.out.println();
	}
}

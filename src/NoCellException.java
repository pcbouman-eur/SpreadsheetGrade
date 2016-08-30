import org.apache.poi.ss.util.CellReference;

public class NoCellException extends RuntimeException
{
	
	private static final long serialVersionUID = 581129633301367621L;
	private final CellReference cell;
	private final boolean reference;

	public NoCellException(CellReference cell, boolean b)
	{
		this.cell = cell;
		this.reference = b;
	}
	
	public CellReference getCellReference()
	{
		return cell;
	}
	
	public boolean inReferenceSheet()
	{
		return reference;
	}
	
	@Override
	public String getMessage()
	{
		String c = cell.formatAsString();
		if (reference)
		{
			return "Cell "+c+" was not defined in the reference solution. Please contact us.";
		}
		else
		{
			return "Cell "+c+" was not defined in your solution and therefore it could not be checked.";
		}
	}

}

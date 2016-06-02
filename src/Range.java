import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.ss.util.CellReference;

public class Range implements Iterable<CellReference>
{
	
	private CellReference from;
	private CellReference to;

	public Range(String s)
	{
		if (s.contains(":"))
		{
			String [] spl = s.split(":");
			if (spl.length != 2)
			{
				throw new IllegalArgumentException("Expecting either a cell or a cell range");
			}
			from = new CellReference(spl[0]);
			to = new CellReference(spl[1]);
		}
		else
		{
			from = new CellReference(s);
			to = new CellReference(s);
		}
	}
	
	public List<CellReference> getCells()
	{
		int minRow = Math.min(from.getRow(), to.getRow());
		int maxRow = Math.max(from.getRow(), to.getRow());
		int minCol = Math.min(from.getCol(), to.getCol());
		int maxCol = Math.max(from.getCol(), to.getCol());

		List<CellReference> cells = new ArrayList<>((1+maxRow-minRow)*(1+maxCol-minCol));
		for (int r=minRow; r <= maxRow; r++)
		{
			for (int c = minCol; c <= maxCol; c++)
			{
				cells.add(new CellReference(r,c));
			}
		}
		
		return cells;
	}
	
	@Override
	public Iterator<CellReference> iterator()
	{
		return getCells().iterator();
	}
	
}

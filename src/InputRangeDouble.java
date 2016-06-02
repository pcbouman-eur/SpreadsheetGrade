import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.poi.ss.util.CellReference;

public class InputRangeDouble implements Iterable<CellReference>
{
	private Range range;
	
	private double lowerBound;
	private double upperBound;
	
	public InputRangeDouble(String r, double lower, double upper)
	{
		range = new Range(r);
		lowerBound = Math.min(lower, upper);
		upperBound = Math.max(lower, upper);
	}
	
	public double getRandom(Random r)
	{
		return lowerBound + r.nextDouble()*(upperBound-lowerBound);
	}
	
	public List<Assignment> getAssignments()
	{
		return expandAssignments(Collections.emptyList());
	}
	
	public List<Assignment> expandAssignments(List<Assignment> input)
	{
		ArrayList<Assignment> result = new ArrayList<>(input);
		for (CellReference cr : this)
		{
			if (result.isEmpty())
			{
				result.add(new Assignment(cr,lowerBound));
				result.add(new Assignment(cr,upperBound));
				continue;
			}
			ArrayList<Assignment> newResult = new ArrayList<>(result.size()*2);
			for (Assignment a : result)
			{
				newResult.add(a.expand(cr, lowerBound));
				newResult.add(a.expand(cr, upperBound));
			}
			result = newResult;
		}
		return result;
	}
	
	public Assignment getRandomAssignment(Random r)
	{
		return expandRandomAssignment(r,null);
	}
	
	public Assignment expandRandomAssignment(Random r, Assignment in)
	{
		Assignment cur = in;
		for (CellReference cr : this)
		{
			if (cur == null)
			{
				cur = new Assignment(cr, getRandom(r));
			}
			else
			{
				cur = cur.expand(cr, getRandom(r));
			}
		}
		return cur;
	}
	
	@Override
	public Iterator<CellReference> iterator()
	{
		return range.iterator();
	}

}

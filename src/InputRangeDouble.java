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
	private double precision;
	
	public InputRangeDouble(String r, double lower, double upper)
	{
		this(r, lower, upper, 0.001);
	}
	
	public InputRangeDouble(String r, double lower, double upper, double prec)
	{
		range = new Range(r);
		lowerBound = Math.min(lower, upper);
		upperBound = Math.max(lower, upper);
		precision = prec;
	}
	
	public double getRandomUB(Random r)
	{
		if (r.nextBoolean())
		{
			return lowerBound;
		}
		return upperBound;
	}
	
	public double getRandom(Random r)
	{
		double draw = lowerBound + r.nextDouble()*(upperBound-lowerBound);
		if (precision >= 1 && precision > 0)
		{
			draw = Math.round(draw * (1/precision)) * precision;
		}
		if (draw < lowerBound)
		{
			return lowerBound;
		}
		if (draw > upperBound)
		{
			return upperBound;
		}
		return draw;
	}
	
	public List<Assignment> getAssignments()
	{
		return expandAssignments(Collections.emptyList());
	}
	
	public List<Assignment> expandAssignments(List<Assignment> input)
	{
		ArrayList<Assignment> result = new ArrayList<>(input);
		boolean tight = isTight();
		for (CellReference cr : this)
		{
			if (result.isEmpty())
			{
				result.add(new Assignment(cr,lowerBound));
				if (!tight)
				{
					result.add(new Assignment(cr,upperBound));
				}
				continue;
			}
			ArrayList<Assignment> newResult;
			if (!tight)
			{
				newResult = new ArrayList<>(result.size()*2);
			}
			else
			{
				newResult = new ArrayList<>(result.size());
			}
			for (Assignment a : result)
			{
				newResult.add(a.expand(cr, lowerBound));
				if (!tight)
				{
					newResult.add(a.expand(cr, upperBound));
				}
			}
			result = newResult;
		}
		return result;
	}
	
	public Assignment getRandomAssignment(Random r)
	{
		return expandRandomAssignment(r,null);
	}
	
	public Assignment getUBRandomAssignment(Random r)
	{
		return expandRandomUBAssignment(r,null);
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
	
	public Assignment expandRandomUBAssignment(Random r, Assignment in)
	{
		Assignment cur = in;
		for (CellReference cr : this)
		{
			if (cur == null)
			{
				cur = new Assignment(cr, getRandomUB(r));
			}
			else
			{
				cur = cur.expand(cr, getRandomUB(r));
			}
		}
		return cur;
	}
	
	@Override
	public Iterator<CellReference> iterator()
	{
		return range.iterator();
	}

	public boolean isTight()
	{
		return Math.abs(upperBound - lowerBound) <= precision;
	}
	
	public int getCellCount()
	{
		return range.getNumberOfCells();
	}

}

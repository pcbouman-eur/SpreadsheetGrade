import org.apache.poi.ss.usermodel.Cell;
//import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;

public class Assignment
{
	private CellReference cr;
	private int type;
	//private CellType type;
	private double numValue;
	private String stringValue;
	private boolean boolValue;
	
	private Assignment next;
	
	public Assignment(CellReference cr, double value)
	{
		this(cr,value,null);
	}
	
	public Assignment(CellReference cr, double value, Assignment a)
	{
		super();
		this.cr = cr;
		//this.type = CellType.NUMERIC;
		this.type = Cell.CELL_TYPE_NUMERIC;
		this.numValue = value;
		this.next = a;
	}
	
	public Assignment(CellReference cr, String value)
	{
		this(cr,value,null);
	}
	
	public Assignment(CellReference cr, String value, Assignment a)
	{
		super();
		this.cr = cr;
		//this.type = CellType.STRING;
		this.type = Cell.CELL_TYPE_STRING;
		this.stringValue = value;
		this.next = a;
	}
	
	public Assignment(CellReference cr, boolean value)
	{
		this(cr,value,null);
	}
	
	public Assignment(CellReference cr, boolean value, Assignment a)
	{
		super();
		this.cr = cr;
		//this.type = CellType.BOOLEAN;
		this.type = Cell.CELL_TYPE_BOOLEAN;
		this.boolValue = value;
		this.next = a;
	}
	
	public Assignment expand(CellReference cr, double value)
	{
		return new Assignment(cr,value,this);
	}
	
	public Assignment expand(CellReference cr, String value)
	{
		return new Assignment(cr,value,this);
	}
	
	public Assignment expand(CellReference cr, boolean value)
	{
		return new Assignment(cr,value,this);
	}
	
	public void assign(Sheet s)
	{
		Cell c = s.getRow(cr.getRow()).getCell(cr.getCol());
		c.setCellType(type);
		//if (type == CellType.BOOLEAN)
		if (type == Cell.CELL_TYPE_BOOLEAN)
		{
			c.setCellValue(boolValue);
		}
		//else if (type == CellType.NUMERIC)
		else if (type == Cell.CELL_TYPE_NUMERIC)
		{
			c.setCellValue(numValue);
		}
		//else if (type == CellType.STRING)
		else if (type == Cell.CELL_TYPE_STRING)
		{
			
			c.setCellValue(stringValue);
		}
		if (next != null)
		{
			next.assign(s);
		}
	}
	
	public void concat(Assignment a)
	{
		if (next == null)
		{
			next = a;
		}
		next.concat(a);
	}
}

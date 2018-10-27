package nl.eur.ese.spreadsheettest;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class TestResults
{
	private Map<String, Integer> pass;
	private Map<String, Integer> fail;
	private Map<String, Integer> error;
	private Map<String, Integer> total;
	
	public TestResults()
	{
		pass = new TreeMap<>();
		fail = new TreeMap<>();
		error = new TreeMap<>();
		total = new TreeMap<>();
	}
	
	private void inc(Map<String,Integer> map, String msg)
	{
		inc(map,msg,1);
	}
	
	private void inc(Map<String,Integer> map, String msg, int count)
	{
		map.merge(msg, count, (i,j) -> i+j);
		total.merge(msg, count, (i,j) -> i+j);
	}
	
	public void test(String msg, boolean b)
	{
		if (b)
		{
			addPass(msg);
		}
		else
		{
			addFail(msg);
		}
	}
	
	public void addPass(String msg)
	{
		inc(pass,msg);
	}
	
	public void addFail(String msg)
	{
		inc(fail,msg);
	}
	
	public void addError(String msg)
	{
		inc(error,msg);
	}
	
	public void addPass(String msg, int num)
	{
		inc(pass,msg,num);
	}
	
	public void addFail(String msg, int num)
	{
		inc(fail,msg,num);
	}
	
	public void addError(String msg, int num)
	{
		inc(error,msg,num);
	}
	
	public Map<String, Integer> getPass()
	{
		return Collections.unmodifiableMap(pass);
	}

	public Map<String, Integer> getFail()
	{
		return Collections.unmodifiableMap(fail);
	}

	public Map<String, Integer> getError()
	{
		return Collections.unmodifiableMap(error);
	}

	public Map<String, Integer> getTotal()
	{
		return Collections.unmodifiableMap(total);
	}
	
	public static TestResults readTestResults(DataInput in) throws IOException
	{
		TestResults tr = new TestResults();
		int number = in.readInt();
		for (int t=0; t < number; t++)
		{
			tr.inc(tr.pass, in.readUTF(), in.readInt());
		}
		number = in.readInt();
		for (int t=0; t < number; t++)
		{
			tr.inc(tr.fail, in.readUTF(), in.readInt());
		}
		number = in.readInt();
		for (int t=0; t < number; t++)
		{
			tr.inc(tr.error, in.readUTF(), in.readInt());
		}		
		return tr;
	}
	
	public void sendTestResults(DataOutput out) throws IOException
	{
		out.writeInt(pass.size());
		for (Entry<String,Integer> e : pass.entrySet())
		{
			out.writeUTF(e.getKey());
			out.writeInt(e.getValue());
		}
		out.writeInt(fail.size());
		for (Entry<String,Integer> e : fail.entrySet())
		{
			out.writeUTF(e.getKey());
			out.writeInt(e.getValue());
		}
		out.writeInt(error.size());
		for (Entry<String,Integer> e : error.entrySet())
		{
			out.writeUTF(e.getKey());
			out.writeInt(e.getValue());
		}
	}

	public boolean didPass()
	{
		return fail.size() == 0 && error.size() == 0;
	}
	
}

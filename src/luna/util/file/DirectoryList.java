package luna.util.file;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

// Display a directory listing using regular expressions.

public class DirectoryList
{
	private DirectoryFilter filter;

	public DirectoryList(String regex)
	{
		filter = new DirectoryFilter(regex);
	}

	public File [] list(String path)
	{
		return new File(path).listFiles(filter);
	}

	public static void main(String[] args)
	{

		File [] list;
		DirectoryList dl = new DirectoryList(".*\\.java");
		list = dl.list("./src/luna/");

		System.out.println(list.length);

		for (File file : list)
		{
			System.out.println(file.getAbsolutePath());
		}

	}
}

class DirectoryFilter implements FilenameFilter
{
	private Pattern pattern;

	public DirectoryFilter(String regex)
	{
		this.pattern = Pattern.compile(regex);
	}

	@Override
	public boolean accept(File dir, String name)
	{
		return pattern.matcher(name).matches();
	}

}
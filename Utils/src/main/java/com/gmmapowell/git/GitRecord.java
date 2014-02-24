package com.gmmapowell.git;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;

public class GitRecord {
	private final File source;
	private File generates;
	private boolean missing;
	private boolean dirty;
	private boolean error;
	private boolean committed = false;
	private List<File> dirtyFiles = new ArrayList<File>();

	GitRecord(File file) {
		this.source = file;
	}

	public void setError() {
		error = true;
	}

	public boolean isFileDirty(File f) {
		return missing || dirtyFiles.contains(f);
	}

	/** Call commit no matter what.
	 * If there are any errors, both files will be deleted.
	 * Otherwise, if there is only one file, that is saved;
	 * if both were needed, the old one is unlinked and the new one copied across  
	 */
	public void commit()
	{
		if (committed)
			return;
		// handle the error case first
		if (error)
		{
//			System.out.println("Error - removing " + source + " and " + generates);
			if (source.exists())
				source.delete();
			if (generates.exists())
				generates.delete();
		}
		else if (source.exists() && generates.exists()) // the typical something might have changed case
		{
			if (dirty)
			{ // they are different, move ...
//				System.out.println("Built - moving " + generates + " to " + source);
				boolean fd = source.delete();
				if (!fd)
					throw new UtilException("Could not delete the file " + source + " when renaming " + generates);
				boolean renameWorked = generates.renameTo(source);
				if (!renameWorked)
					throw new UtilException("Could not rename " + generates + " to " + source);
			}
			else
			{
//				System.out.println("No change - removing " +generates);
				// they're the same, the new one is uninteresting
				generates.delete();
			}
		}
		else if (!source.exists() && generates.exists()) // we didn't have a comparison file
		{
//			System.out.println("Clean build - saving " + generates + " as " + source);
			boolean renameWorked = generates.renameTo(source);
			if (!renameWorked)
				throw new UtilException("Could not rename " + generates + " to " + source);
		}
		else
			throw new UtilException("I don't think this case should be able to happen: Comparing " + source + (source.exists()?"(exists)":"(missing)") + " " + generates + (generates.exists()?"(exists)":"(missing)"));
		committed = true;
	}

	public void revert() {
		if (committed)
			return;
//		System.out.println("Removing " + source + " and " + generates);
		if (source.exists())
			source.delete();
		if (generates.exists())
			generates.delete();
	}

	boolean sourceExists() {
		return source.exists();
	}

	void setDirty() {
		dirty = true;
		source.delete();
	}

	void generates(File newFile) {
		generates = newFile;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void dirtyFile(File f) {
		dirtyFiles.add(f);
		if (!dirty)
			setDirty();
	}

	public void fileMissing() {
		missing = true;
	}

	@Override
	public String toString() {
		return "GitRecord[" + source + "]";
	}
}
